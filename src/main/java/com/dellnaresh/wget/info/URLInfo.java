package com.dellnaresh.wget.info;

import com.dellnaresh.wget.Direct;
import com.dellnaresh.wget.RetryWrap;
import com.dellnaresh.wget.WrapReturn;
import com.dellnaresh.wget.info.ex.DownloadRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URLInfo - keep all information about source in one place. Thread safe.
 */
public class URLInfo extends BrowserInfo {
    private final Logger logger = LoggerFactory.getLogger(URLInfo.class);
    /**
     * source url
     */
    private final URL source;

    /**
     * have been extracted?
     */
    private boolean extract = false;

    /**
     * null if size is unknown, which means we unable to restore downloads or do
     * multi thread downlaods
     */
    private Long length;

    /**
     * does server support for the range param?
     */
    private boolean range;

    /**
     * null if here is no such file or other error
     */
    private String contentType;

    /**
     * come from Content-Disposition: attachment; filename="fname.ext"
     */
    private String contentFilename;
    /**
     * downloadVideo state
     */
    private States state;
    /**
     * downloading error / retry error
     */
    private Throwable exception;
    /**
     * retrying delay;
     */
    private int delay;

    URLInfo(URL source) {
        this.source = source;
    }

    public void extract() {
        extract(new AtomicBoolean(false), new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    void extract(final AtomicBoolean stop, final Runnable notify) {
        logger.info("Called extract");
        HttpURLConnection conn;
        try {
            conn = RetryWrap.wrap(stop, new WrapReturn<HttpURLConnection>() {
                URL url = source;

                @Override
                public HttpURLConnection download() throws IOException {
                    setState(States.EXTRACTING);
                    notify.run();

                    try {
                        return extractRange(url);
                    } catch (DownloadRetry e) {
                        throw e;
                    } catch (RuntimeException e) {
                        return extractNormal(url);
                    }
                }

                @Override
                public void retry(int d, Throwable ee) {
                    setDelay(d, ee);
                    notify.run();
                }

                @Override
                public void moved(URL u) {
                    setReferrer(url);

                    url = u;

                    setState(States.RETRYING);
                    notify.run();
                }
            });

            setContentType(conn.getContentType());

            String contentDisposition = conn.getHeaderField("Content-Disposition");
            if (contentDisposition != null) {
                // i support for two forms with and without quotes:
                //
                // 1) contentDisposition="attachment;filename="ap61.ram"";
                // 2) contentDisposition="attachment;filename=ap61.ram";

                Pattern cp = Pattern.compile("filename=[\"]*([^\"]*)[\"]*");
                Matcher cm = cp.matcher(contentDisposition);
                if (cm.find())
                    setContentFilename(cm.group(1));
            }

            setEmpty(true);

            setState(States.EXTRACTING_DONE);
            notify.run();
        } catch (RuntimeException e) {
            setState(States.ERROR, e);

            throw e;
        }
    }

    synchronized boolean empty() {
        return !extract;
    }

    private synchronized void setEmpty(boolean b) {
        extract = b;
    }

    // if range failed - do plain downloadVideo with no retrys's
    private HttpURLConnection extractRange(URL source) throws IOException {
        logger.info("Checking if its extra range download");
        HttpURLConnection conn = (HttpURLConnection) source.openConnection();

        conn.setConnectTimeout(Direct.CONNECT_TIMEOUT);
        conn.setReadTimeout(Direct.READ_TIMEOUT);

        conn.setRequestProperty("User-Agent", getUserAgent());
        if (getReferrer() != null)
            conn.setRequestProperty("Referer", getReferrer().toExternalForm());

        // may raise an exception if not supported by server
        conn.setRequestProperty("Range", "bytes=" + 0 + "-" + 0);

        RetryWrap.checkConnection(conn);

        String range = conn.getHeaderField("Content-Range");
        if (range == null) {
            logger.info("Extra Range not supported");
            throw new RuntimeException("range not supported");
        }

        Pattern p = Pattern.compile("bytes \\d+-\\d+/(\\d+)");
        Matcher m = p.matcher(range);
        if (m.find()) {
            setLength(new Long(m.group(1)));
        } else {
            logger.info("Extra Range not supported");
            throw new RuntimeException("range not supported");
        }

        this.setRange(true);

        return conn;
    }

    // if range failed - do plain downloadVideo with no retrys's
    private HttpURLConnection extractNormal(URL source) throws IOException {
        logger.info("Checking if its normal download");
        HttpURLConnection conn = (HttpURLConnection) source.openConnection();

        conn.setConnectTimeout(Direct.CONNECT_TIMEOUT);
        conn.setReadTimeout(Direct.READ_TIMEOUT);

        conn.setRequestProperty("User-Agent", getUserAgent());
        if (getReferrer() != null)
            conn.setRequestProperty("Referer", getReferrer().toExternalForm());

        setRange(false);

        RetryWrap.checkConnection(conn);

        int len = conn.getContentLength();
        if (len >= 0) {
            setLength((long) len);
        }

        return conn;
    }

    synchronized public String getContentType() {
        return contentType;
    }

    private synchronized void setContentType(String ct) {
        contentType = ct;
    }

    synchronized public Long getLength() {
        return length;
    }

    private synchronized void setLength(Long l) {
        length = l;
    }

    synchronized public URL getSource() {
        return source;
    }

    synchronized public String getContentFilename() {
        return contentFilename;
    }

    private synchronized void setContentFilename(String f) {
        contentFilename = f;
    }

    synchronized public States getState() {
        return state;
    }

    synchronized public void setState(States state) {
        this.state = state;
        this.exception = null;
        this.delay = 0;
    }

    private synchronized void setState(States state, Throwable e) {
        this.state = state;
        this.exception = e;
        this.delay = 0;
    }

    synchronized public Throwable getException() {
        return exception;
    }

    synchronized protected void setException(Throwable exception) {
        this.exception = exception;
    }

    synchronized public int getDelay() {
        return delay;
    }

    synchronized public void setDelay(int delay, Throwable e) {
        this.delay = delay;
        this.exception = e;
        this.state = com.dellnaresh.wget.info.URLInfo.States.RETRYING;
    }

    synchronized public boolean getRange() {
        return range;
    }

    private synchronized void setRange(boolean range) {
        this.range = range;
    }

    /**
     * Notify States
     */
    public enum States {
        EXTRACTING, EXTRACTING_DONE, DOWNLOADING, RETRYING, STOP, ERROR, DONE
    }

}
