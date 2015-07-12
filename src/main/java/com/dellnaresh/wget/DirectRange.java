package com.dellnaresh.wget;

import com.dellnaresh.util.Constants;
import com.dellnaresh.wget.info.DownloadInfo;
import com.dellnaresh.wget.info.URLInfo;
import com.dellnaresh.wget.info.ex.DownloadInterruptedError;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class DirectRange extends Direct {
    private static final Logger logger = LoggerFactory.getLogger(DirectRange.class);

    public DirectRange(DownloadInfo downloadInfo, File target) {
        super(downloadInfo, target);
    }

    /**
     * checkConnection existing file for downloadVideo resumeDownload. for range downloadVideo it will checkConnection
     * file size and inside state. they sould be equal.
     *
     * @param downloadInfo
     * @param targetFile
     * @return return true - if all ok, false - if downloadVideo can not be restored.
     */
    public static boolean canResume(DownloadInfo downloadInfo, File targetFile) {
        logger.info("Calling canResume method");
        if (targetFile.exists()) {
            if (downloadInfo.getCount() != targetFile.length())
                return false;
        } else {
            if (downloadInfo.getCount() > 0)
                return false;
        }
        return true;
    }

    private void downloadPart(DownloadInfo info, AtomicBoolean stop, Runnable notify) throws IOException {
        logger.info("Calling download Part method");
        RandomAccessFile fos = null;
        BufferedInputStream binaryreader = null;
        HttpURLConnection conn=null;

        try {
            URL url = info.getSource();

            conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            conn.setRequestProperty(Constants.USER_AGENT, info.getUserAgent());
            if (info.getReferrer() != null)
                conn.setRequestProperty(Constants.REFERRER, info.getReferrer().toExternalForm());

            File f = target;
            if (!f.exists())
                f.createNewFile();
            info.setCount(FileUtils.sizeOf(f));

            if (info.getLength()!=null && info.getCount() >= info.getLength()) {
                notify.run();
                return;
            }

            fos = new RandomAccessFile(f, "rw");

            if (info.getCount() > 0) {
                conn.setRequestProperty("Range", "bytes=" + info.getCount() + "-");
                fos.seek(info.getCount());
            }

            byte[] bytes = new byte[BUF_SIZE];
            int read;

            RetryWrap.checkConnection(conn);

            binaryreader = new BufferedInputStream(conn.getInputStream());

            while ((read = binaryreader.read(bytes)) > 0) {
                fos.write(bytes, 0, read);

                info.setCount(info.getCount() + read);
                notify.run();

                if (stop.get()) {
                    logger.error("DownloadInterruptedError called stop");
                    throw new DownloadInterruptedError(Constants.ERRORS.STOPPED);
                }
                if (Thread.interrupted()) {
                    logger.error("DownloadInterruptedError thread interrupted");
                    throw new DownloadInterruptedError(Constants.ERRORS.INTERRUPTED);
                }
            }

        } finally {
            if (fos != null)
                fos.close();
            if (binaryreader != null)
                binaryreader.close();
            if(conn!=null)
                conn.disconnect();
        }
    }

    @Override
    public void download(final AtomicBoolean stop, final Runnable notify) {
        logger.info("Called download method");
        downloadInfo.setState(URLInfo.States.DOWNLOADING);
        notify.run();

        try {
            RetryWrap.wrap(stop, new RetryWrap.Wrap() {
                @Override
                public void download() throws IOException {
                    downloadInfo.setState(URLInfo.States.DOWNLOADING);
                    notify.run();
//                    downloadFile(downloadInfo,stop,notify);
                    downloadPart(downloadInfo, stop, notify);
                }

                @Override
                public void retry(int delay, Throwable e) {
                    downloadInfo.setDelay(delay, e);
                    notify.run();
                }

                @Override
                public void moved(URL url) {
                    downloadInfo.setState(URLInfo.States.RETRYING);
                    notify.run();
                }
            });

            downloadInfo.setState(URLInfo.States.DONE);
            notify.run();
        } catch (DownloadInterruptedError e) {
            downloadInfo.setState(URLInfo.States.STOP);
            notify.run();

            throw e;
        } catch (RuntimeException e) {
            downloadInfo.setState(URLInfo.States.ERROR);
            notify.run();

            throw e;
        }
    }
}
