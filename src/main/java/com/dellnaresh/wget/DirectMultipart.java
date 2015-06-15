package com.dellnaresh.wget;

import com.dellnaresh.threads.LimitThreadPool;
import com.dellnaresh.util.Constants;
import com.dellnaresh.wget.info.DownloadInfo;
import com.dellnaresh.wget.info.URLInfo;
import com.dellnaresh.wget.info.ex.DownloadInterruptedError;
import com.dellnaresh.wget.info.ex.DownloadMultipartError;
import com.dellnaresh.wget.info.ex.DownloadRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;


public class DirectMultipart extends Direct {

    private Logger logger= LoggerFactory.getLogger(DirectMultipart.class);

    static public final int THREAD_COUNT = 3;
    static public final int RETRY_DELAY = 10;

    LimitThreadPool limitThreadPool = new LimitThreadPool(THREAD_COUNT);

    boolean fatal = false;

    Object lock = new Object();

    /**
     * @param info   downloadVideo file information
     * @param target target file
     */
    public DirectMultipart(DownloadInfo info, File target) {
        super(info, target);
    }

    /**
     * checkConnection existing file for downloadVideo resumeDownload. for isMultiPart downloadVideo it may
     * checkConnection all parts CRC
     *
     * @param inputDownloadInfo
     * @param targetFile
     * @return return true - if all ok, false - if downloadVideo can not be restored.
     */
    public static boolean canResume(DownloadInfo inputDownloadInfo, File targetFile) {
        if (!targetFile.exists())
            return false;

        return targetFile.length() >= inputDownloadInfo.getCount();

    }

    /**
     * downloadVideo part.
     * <p>
     * if returns normally - part is fully donwloaded. other wise - it throws
     * RuntimeException or DownloadRetry or DownloadError
     *
     * @param part
     */
    void downloadPart(DownloadInfo.Part part, AtomicBoolean stop, Runnable notify) throws IOException {
        logger.info("Downloading video part {}",part.getNumber());
        RandomAccessFile fos = null;
        BufferedInputStream binaryreader = null;

        try {
            URL url = downloadInfo.getSource();

            long start = part.getStart() + part.getCount();
            long end = part.getEnd();

            // fully downloaded already?
            if (end - start + 1 == 0)
                return;

            HttpURLConnection conn;
            conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            conn.setRequestProperty(Constants.USER_AGENT, downloadInfo.getUserAgent());
            if (downloadInfo.getReferrer() != null)
                conn.setRequestProperty(Constants.REFERRER, downloadInfo.getReferrer().toExternalForm());

            File f = target;

            fos = new RandomAccessFile(f, "rw");

            conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
            fos.seek(start);

            byte[] bytes = new byte[BUF_SIZE];
            int read = 0;

            RetryWrap.checkConnection(conn);

            binaryreader = new BufferedInputStream(conn.getInputStream());

            boolean localStop = false;

            while ((read = binaryreader.read(bytes)) > 0) {
                // ensure we do not downloadVideo more then part size.
                // if so cut bytes and stop downloadVideo
                long partEnd = part.getLength() - part.getCount();
                if (read > partEnd) {
                    read = (int) partEnd;
                    localStop = true;
                }

                fos.write(bytes, 0, read);
                part.setCount(part.getCount() + read);
                downloadInfo.calculateMultipartDownloadProgress();
                notify.run();

                if (stop.get()) {
                    logger.error("DownloadInterruptedError called stop");
                    throw new DownloadInterruptedError(Constants.ERRORS.STOPPED);
                }
                if (Thread.interrupted()) {
                    logger.error("DownloadInterruptedError thread interrupted");
                    throw new DownloadInterruptedError(Constants.ERRORS.INTERRUPTED);
                }
                if (fatal())
                    throw new DownloadInterruptedError(Constants.ERRORS.FATAL);

                // do not throw exception here. we normally done downloading.
                // just took a little bit more
                if (localStop)
                    return;
            }

            if (part.getCount() != part.getLength())
                throw new DownloadRetry("EOF before end of part");
        } finally {
            if (binaryreader != null)
                binaryreader.close();
            if (fos != null)
                fos.close();
        }

    }

    boolean fatal() {
        synchronized (lock) {
            return fatal;
        }
    }

    void fatal(boolean b) {
        synchronized (lock) {
            fatal = b;
        }
    }

    String trimLen(String str, int len) {
        if (str.length() > len)
            return str.substring(0, len / 2) + "..." + str.substring(str.length() - len / 2, str.length());
        else
            return str;
    }

    void downloadWorker(final DownloadInfo.Part part, final AtomicBoolean stop, final Runnable notify) throws InterruptedException {
        logger.info("Called Download Worker");
        limitThreadPool.blockExecute(new Runnable() {
            @Override
            public void run() {
                {
                    String f = "%s - Part: %d";
                    Thread thread = Thread.currentThread();
                    thread.setName(String.format(f, trimLen(downloadInfo.getSource().toString(), 64), part.getNumber()));
                }

                try {
                    RetryWrap.wrap(stop, new RetryWrap.Wrap() {

                        @Override
                        public void download() throws IOException {
                            part.setState(DownloadInfo.Part.States.DOWNLOADING);
                            notify.run();

                            downloadPart(part, stop, notify);
                        }

                        @Override
                        public void retry(int delay, Throwable e) {
                            part.setDelay(delay, e);
                            notify.run();
                        }

                        @Override
                        public void moved(URL url) {
                            part.setState(DownloadInfo.Part.States.RETRYING);
                            notify.run();
                        }

                    });
                    part.setState(DownloadInfo.Part.States.DONE);
                    notify.run();
                } catch (DownloadInterruptedError e) {
                    part.setState(DownloadInfo.Part.States.STOP, e);
                    notify.run();

                    fatal(true);
                } catch (RuntimeException e) {
                    part.setState(DownloadInfo.Part.States.ERROR, e);
                    notify.run();

                    fatal(true);
                }
            }
        });

        part.setState(DownloadInfo.Part.States.DOWNLOADING);
    }

    /**
     * return next part to downloadVideo. ensure this part is not done() and not
     * currently downloading
     *
     * @return
     */
    DownloadInfo.Part getPart() {
        for (DownloadInfo.Part p : downloadInfo.getParts()) {
            if (!p.getState().equals(DownloadInfo.Part.States.QUEUED))
                continue;
            return p;
        }

        return null;
    }

    /**
     * return true, when thread pool empty, and here is no unfinished parts to
     * downloadVideo
     *
     * @return true - done. false - not done yet
     * @throws InterruptedException
     */
    boolean done(AtomicBoolean stop) {
        if (stop.get())
            throw new DownloadInterruptedError(Constants.ERRORS.STOPPED);
        if (Thread.interrupted())
            throw new DownloadInterruptedError(Constants.ERRORS.INTERRUPTED);
        if (limitThreadPool.active())
            return false;
        return getPart() == null;

    }

    @Override
    public void download(AtomicBoolean stop, Runnable notify) {
        logger.info("Calling Download");
        for (DownloadInfo.Part p : downloadInfo.getParts()) {
            if (p.getState().equals(DownloadInfo.Part.States.DONE))
                continue;
            p.setState(DownloadInfo.Part.States.QUEUED);
        }
        downloadInfo.setState(URLInfo.States.DOWNLOADING);
        notify.run();

        try {
            while (!done(stop)) {
                DownloadInfo.Part p = getPart();
                if (p != null) {
                    downloadWorker(p, stop, notify);
                } else {
                    // we have no parts left.
                    //
                    // wait until task ends and checkConnection again if we have to retry.
                    // we have to checkConnection if last part back to queue in case of
                    // RETRY state
                    limitThreadPool.waitUntilNextTaskEnds();
                }

                // if we start to receive errors. stop add new tasks and wait
                // until all active tasks be emptied
                if (fatal()) {
                    limitThreadPool.waitUntilTermination();

                    // checkConnection if all parts finished with interrupted, throw one
                    // interrupted
                    {
                        boolean interrupted = true;
                        for (DownloadInfo.Part pp : downloadInfo.getParts()) {
                            Throwable e = pp.getException();
                            if (e == null)
                                continue;
                            if (e instanceof DownloadInterruptedError)
                                continue;
                            interrupted = false;
                        }
                        if (interrupted) {
                            logger.info("Multipart Download interrupted");
                            throw new DownloadInterruptedError("isMultiPart all interrupted");
                        }
                    }

                    // ok all thread stopped. now throw the exception and let
                    // app deal with the errors
                    throw new DownloadMultipartError(downloadInfo);
                }
            }

            downloadInfo.setState(URLInfo.States.DONE);
            notify.run();
        } catch (InterruptedException e) {
            downloadInfo.setState(URLInfo.States.STOP);
            notify.run();

            throw new DownloadInterruptedError(e);
        } catch (DownloadInterruptedError e) {
            downloadInfo.setState(URLInfo.States.STOP);
            notify.run();

            throw e;
        } catch (RuntimeException e) {
            downloadInfo.setState(URLInfo.States.ERROR);
            notify.run();

            throw e;
        } finally {
            limitThreadPool.shutdown();
        }
    }
}
