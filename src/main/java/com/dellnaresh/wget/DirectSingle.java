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

public class DirectSingle extends Direct {

    private static Logger logger= LoggerFactory.getLogger(DirectSingle.class);
    /**
     * @param downloadInfo   downloadVideo file information
     * @param target target file
     */
    public DirectSingle(DownloadInfo downloadInfo, File target) {
        super(downloadInfo, target);
    }

    /**
     * checkConnection existing file for downloadVideo resumeDownload. for single downloadVideo it will
     * checkConnection file dose not exist or zero size. so we can resumeDownload downloadVideo.
     *
     * @param downloadInfo
     * @param targetFile
     * @return return true - if all ok, false - if downloadVideo can not be restored.
     */
    public static boolean canResume(DownloadInfo downloadInfo, File targetFile) {
        logger.info("Calling canResume method");
        if (downloadInfo.getCount() != 0)
            return false;

        if (targetFile.exists()) {
            if (targetFile.length() != 0)
                return false;
        }

        return true;
    }

    void downloadFile(DownloadInfo info,AtomicBoolean stop,Runnable notify) throws IOException{
        logger.info("Calling downloadFile method");
        if (stop.get())
            throw new DownloadInterruptedError(Constants.ERRORS.STOPPED);
        if (Thread.interrupted())
            throw new DownloadInterruptedError(Constants.ERRORS.INTERRUPTED);

        FileUtils.copyURLToFile(info.getSource(), target);
    }

    void downloadPart(DownloadInfo info, AtomicBoolean stop, Runnable notify) throws IOException {
        logger.info("Calling downloadPart method");
        RandomAccessFile fos = null;

        try {
            URL url = info.getSource();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            conn.setRequestProperty(Constants.USER_AGENT, info.getUserAgent());
            if (info.getReferrer() != null)
                conn.setRequestProperty(Constants.REFERRER, info.getReferrer().toExternalForm());

            File f = target;
            info.setCount(0);
            f.createNewFile();

            fos = new RandomAccessFile(f, "rw");

            byte[] bytes = new byte[BUF_SIZE];
            int read = 0;

            RetryWrap.checkConnection(conn);

            BufferedInputStream binaryreader = new BufferedInputStream(conn.getInputStream());

            while ((read = binaryreader.read(bytes)) > 0) {
                fos.write(bytes, 0, read);

                info.setCount(info.getCount() + read);
                notify.run();

                if (stop.get())
                    throw new DownloadInterruptedError(Constants.ERRORS.STOPPED);
                if (Thread.interrupted())
                    throw new DownloadInterruptedError(Constants.ERRORS.INTERRUPTED);
            }

            binaryreader.close();
        } finally {
            if (fos != null)
                fos.close();
        }
    }

    @Override
    public void download(final AtomicBoolean stop, final Runnable notify) {
        logger.info("Calling download method");
        downloadInfo.setState(URLInfo.States.DOWNLOADING);
        notify.run();

        try {
            RetryWrap.wrap(stop, new RetryWrap.Wrap() {
                @Override
                public void download() throws IOException {
                    downloadInfo.setState(URLInfo.States.DOWNLOADING);
                    notify.run();
                    downloadFile(downloadInfo,stop,notify);
//                    downloadPart(downloadInfo, stop, notify);
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
