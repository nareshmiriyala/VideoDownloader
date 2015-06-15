package com.dellnaresh.wget;

import com.dellnaresh.wget.info.ex.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RetryWrap {

    public static final int RETRY_DELAY = 10;

    static <T> void moved(AtomicBoolean stop, WrapReturn<T> r, DownloadMoved e) {
        if (stop.get())
            throw new DownloadInterruptedError("stop");

        if (Thread.currentThread().isInterrupted())
            throw new DownloadInterruptedError("interrrupted");

        r.moved(e.getMoved());
    }

    static <T> void retry(AtomicBoolean stop, WrapReturn<T> r, RuntimeException e) {
        for (int i = RETRY_DELAY; i >= 0; i--) {
            r.retry(i, e);

            if (stop.get())
                throw new DownloadInterruptedError("stop");

            if (Thread.currentThread().isInterrupted())
                throw new DownloadInterruptedError("interrrupted");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                throw new DownloadInterruptedError(e1);
            }
        }
    }

    public static <T> T run(AtomicBoolean stop, WrapReturn<T> r) {
        while (true) {
            if (stop.get())
                throw new DownloadInterruptedError("stop");
            if (Thread.currentThread().isInterrupted())
                throw new DownloadInterruptedError("interrupted");

            try {
                try {

                    return r.download();
                } catch (SocketException e) {
                    // enumerate all retry exceptions
                    throw new DownloadRetry(e);
                } catch (ProtocolException e) {
                    // enumerate all retry exceptions
                    throw new DownloadRetry(e);
                } catch (HttpRetryException e) {
                    // enumerate all retry exceptions
                    throw new DownloadRetry(e);
                } catch (InterruptedIOException e) {
                    // enumerate all retry exceptions
                    throw new DownloadRetry(e);
                } catch (UnknownHostException e) {
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

    public static void check(HttpURLConnection c) throws IOException {
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

    public interface WrapReturn<T> {
        public void retry(int delay, Throwable e);

        public void moved(URL url);

        public T download() throws IOException;
    }

    public interface Wrap {
        public void retry(int delay, Throwable e);

        public void moved(URL url);

        public void download() throws IOException;
    }
}
