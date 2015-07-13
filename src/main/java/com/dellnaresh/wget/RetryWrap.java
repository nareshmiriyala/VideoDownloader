package com.dellnaresh.wget;

import com.dellnaresh.util.Constants;
import com.dellnaresh.wget.info.ex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RetryWrap {
    public static final int RETRY_DELAY = 5;
    private static final Logger logger = LoggerFactory.getLogger(RetryWrap.class);

    private static <T> void moved(AtomicBoolean stop, WrapReturn<T> r, DownloadMoved e) {
        logger.info("Calling moved method");
        hadleExceptions(stop);

        r.moved(e.getMoved());
    }

    private static void hadleExceptions(AtomicBoolean stop) {
        if (stop.get())
            throw new DownloadInterruptedError(Constants.ERRORS.STOPPED);

        if (Thread.currentThread().isInterrupted())
            throw new DownloadInterruptedError(Constants.ERRORS.INTERRUPTED);
    }

    private static <T> void retry(AtomicBoolean stop, WrapReturn<T> r, RuntimeException e) {
        logger.info("Calling retry method");
        for (int i = RETRY_DELAY; i >= 0; i--) {
            r.retry(i, e);

            hadleExceptions(stop);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                throw new DownloadInterruptedError(e1);
            }
        }
    }

    private static <T> T run(AtomicBoolean stop, WrapReturn<T> r) {
        logger.info("calling run");
        while (true) {

            hadleExceptions(stop);

            try {
                try {

                    return r.download();
                } catch (HttpRetryException | UnknownHostException e) {
                    // enumerate all retry exceptions
                    throw new DownloadRetry(e);
                } catch (FileNotFoundException e) {
                    throw new DownloadError(e);
                } catch (IOException e) {
                    throw new DownloadIOError(e);
                }
            } catch (DownloadMoved e) {
                moved(stop, r, e);
            } catch (DownloadRetry e) {
                retry(stop, r, e);
            }
        }
    }

    public static <T> T wrap(AtomicBoolean stop, WrapReturn<T> r) {
        return com.dellnaresh.wget.RetryWrap.run(stop, r);
    }

    public static void wrap(AtomicBoolean stop, final Wrap r) {
        logger.info("Calling wrap");
        WrapReturn<Object> rr = new WrapReturn<Object>() {

            @Override
            public Object download() throws IOException {
                r.download();

                return null;
            }

            @Override
            public void retry(int delay, Throwable e) {
                r.retry(delay, e);
            }

            @Override
            public void moved(URL url) {
                r.moved(url);
            }
        };

        com.dellnaresh.wget.RetryWrap.run(stop, rr);
    }

    public static void checkConnection(HttpURLConnection c) throws IOException {
        logger.info("calling check connection");
        int code = c.getResponseCode();
        switch (code) {
            case HttpURLConnection.HTTP_OK:
            case HttpURLConnection.HTTP_PARTIAL:
                return;
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case HttpURLConnection.HTTP_MOVED_PERM:
                // the user agent MUST NOT automatically redirect the request unless it can be confirmed by the user
                throw new DownloadMoved(c);
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw new DownloadIOCodeError(HttpURLConnection.HTTP_FORBIDDEN);
            case 416:
                // HTTP Error 416 - Requested Range Not Satisfiable
                throw new DownloadIOCodeError(416);
        }
    }
    public interface Wrap {
        void retry(int delay, Throwable e);

        void moved(URL url);

        void download() throws IOException;
    }
}
