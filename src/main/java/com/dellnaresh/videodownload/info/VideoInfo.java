package com.dellnaresh.videodownload.info;

import com.dellnaresh.videodownload.vhs.VimeoParser;
import com.dellnaresh.videodownload.vhs.YouTubeParser;
import com.dellnaresh.wget.info.DownloadInfo;
import com.dellnaresh.wget.info.ex.DownloadError;
import com.dellnaresh.wget.info.ex.DownloadInterruptedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoInfo {

    private final Logger logger = LoggerFactory.getLogger(VideoInfo.class);
    // user friendly url (not direct video stream url)
    private URL webUrl;
    private VideoQuality videoQuality;
    private DownloadInfo downloadInfo;
    private String title;
    private URL icon;
    // states, three variables
    private States state;
    private Throwable exception;
    private int delay;

    /**
     * @param webUrl user firendly url
     */
    public VideoInfo(URL webUrl) {
        this.setWebUrl(webUrl);

        reset();
    }

    /**
     * checkConnection if we have call extractDownloadInfo()
     *
     * @return true - if extractDownloadInfo() already been called
     */
    public boolean empty() {
        return downloadInfo == null;
    }

    /**
     * reset videoinfo state. make it simialar as after calling constructor
     */
    private void reset() {
        setState(States.QUEUE);

        downloadInfo = null;
        videoQuality = null;
        title = null;
        icon = null;
        exception = null;
        delay = 0;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    public void setDownloadInfo(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    /**
     * get current video quality. holds actual videoquality ready for downloadVideo
     *
     * @return videoquality of requested URL
     */
    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    /**
     * @param vq video quality
     */
    public void setVideoQuality(VideoQuality vq) {
        this.videoQuality = vq;
    }

    public URL getWebUrl() {
        return webUrl;
    }

    private void setWebUrl(URL source) {
        this.webUrl = source;
    }

    public void extractDownloadInfo(VideoParser ei, AtomicBoolean stop, Runnable notify) {

        if (ei == null && YouTubeParser.probe(webUrl)) {
            logger.info("Initialized YouTubeParser");
            ei = new YouTubeParser(webUrl);
        }

        if (ei == null && VimeoParser.probe(webUrl))
            ei = new VimeoParser(webUrl);

        if (ei == null)
            throw new RuntimeException("unsupported webUrl site");

        try {
            DownloadInfo dinfo = ei.extractDownloadInfo(this, stop, notify);
            if (dinfo == null) {
                logger.error("Not able to extractDownloadInfo downloadVideo info");
                throw new DownloadError("DownloadInfo object is null");
            }

            this.setDownloadInfo(dinfo);

            downloadInfo.setReferrer(webUrl);

            downloadInfo.extract(stop, notify);
        } catch (DownloadInterruptedError e) {
            logger.error("Download Interrupted Error {}", e);
            setState(States.STOP, e);

            throw e;
        } catch (RuntimeException e) {
            logger.error("Runtime Exception {}", e);
            setState(States.ERROR, e);

            throw e;
        }
    }

    public States getState() {
        return state;
    }

    public void setState(States state) {
        this.state = state;
        this.exception = null;
        this.delay = 0;
    }

    public void setState(States state, Throwable e) {
        this.state = state;
        this.exception = e;
        this.delay = 0;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay, Throwable e) {
        this.delay = delay;
        this.exception = e;
        this.state = States.RETRYING;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public URL getIcon() {
        return icon;
    }

    public void setIcon(URL icon) {
        this.icon = icon;
    }

    // keep it in order hi->lo
    public enum VideoQuality {
        p3072, p2304, p1080, p720, p520, p480, p360, p270, p240, p224, p144
    }

    public enum States {
        QUEUE, EXTRACTING, EXTRACTING_DONE, DOWNLOADING, RETRYING, DONE, ERROR, STOP
    }

    @Override
    public String toString() {
        return "VideoInfo{" +
                "webUrl=" + webUrl +
                ", videoQuality=" + videoQuality +
                ", title='" + title + '\'' +
                ", icon=" + icon +
                ", delay=" + delay +
                ", downloadInfo=" + downloadInfo +
                '}';
    }
}