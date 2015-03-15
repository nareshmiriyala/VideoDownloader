package com.dellnaresh.videodownload.info;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import com.dellnaresh.videodownload.vhs.VimeoParser;
import com.dellnaresh.videodownload.vhs.YouTubeParser;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadInterruptedError;

public class VideoInfo {

    // keep it in order hi->lo
    public enum VideoQuality {
        p3072, p2304, p1080, p720, p520, p480, p360, p270, p240, p224, p144
    }

    public enum States {
        QUEUE, EXTRACTING, EXTRACTING_DONE, DOWNLOADING, RETRYING, DONE, ERROR, STOP
    }

    // user friendly url (not direct video stream url)
    private URL web;

    private VideoQuality vq;
    private DownloadInfo info;
    private String title;
    private URL icon;

    // states, three variables
    private States state;
    private Throwable exception;
    private int delay;

    /**
     * 
     * @param vq
     *            max video quality to download
     * @param web
     *            user firendly url
     * @param video
     *            video stream url
     * @param title
     *            video title
     */
    public VideoInfo(URL web) {
        this.setWeb(web);

        reset();
    }

    /**
     * check if we have call extract()
     * 
     * @return true - if extract() already been called
     */
    public boolean empty() {
        return info == null;
    }

    /**
     * reset videoinfo state. make it simialar as after calling constructor
     */
    public void reset() {
        setState(States.QUEUE);

        info = null;
        vq = null;
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

    public DownloadInfo getInfo() {
        return info;
    }

    public void setInfo(DownloadInfo info) {
        this.info = info;
    }

    /**
     * get current video quality. holds actual videoquality ready for download
     * 
     * @return videoquality of requested URL
     */
    public VideoQuality getVideoQuality() {
        return vq;
    }

    /**
     * 
     * @param vq
     *            video quality
     */
    public void setVideoQuality(VideoQuality vq) {
        this.vq = vq;
    }

    public URL getWeb() {
        return web;
    }

    public void setWeb(URL source) {
        this.web = source;
    }

    public void extract(VideoParser user, AtomicBoolean stop, Runnable notify) {
        VideoParser ei = user;

        if (ei == null && YouTubeParser.probe(web))
            ei = new YouTubeParser(web);

        if (ei == null && VimeoParser.probe(web))
            ei = new VimeoParser(web);

        if (ei == null)
            throw new RuntimeException("unsupported web site");

        try {
            DownloadInfo dinfo = ei.extract(this, stop, notify);

            this.setInfo(dinfo);

            info.setReferer(web);

            info.extract(stop, notify);
        } catch (DownloadInterruptedError e) {
            setState(States.STOP, e);

            throw e;
        } catch (RuntimeException e) {
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

}