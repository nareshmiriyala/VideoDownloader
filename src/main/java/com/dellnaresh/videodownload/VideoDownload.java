package com.dellnaresh.videodownload;

import com.dellnaresh.videodownload.info.VideoInfo;
import com.dellnaresh.videodownload.info.VideoParser;
import com.dellnaresh.wget.*;
import com.dellnaresh.wget.info.DownloadInfo;
import com.dellnaresh.wget.info.DownloadInfo.Part;
import com.dellnaresh.wget.info.ex.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoDownload {

    private static final Logger logger = LoggerFactory.getLogger(VideoDownload.class);
    private VideoInfo videoInfo;
    private File targetDir;
    private File targetForce = null;
    private File targetFile = null;

    /**
     * extractDownloadInfo video information constructor
     *
     * @param source
     */
    public VideoDownload(URL source) {
        this(new VideoInfo(source), null);
    }

    public VideoDownload(URL source, File targetDir) {
        this(new VideoInfo(source), targetDir);
    }

    public VideoDownload(VideoInfo videoInfo, File targetDir) {
        this.videoInfo = videoInfo;
        this.targetDir = targetDir;
    }

    /**
     * Drop all forbidden characters from filename
     *
     * @param fileName input file name
     * @return normalized file name
     */
    static String replaceBadChars(String fileName) {
        String replace = " ";
        fileName = fileName.replaceAll("/", replace);
        fileName = fileName.replaceAll("\\\\", replace);
        fileName = fileName.replaceAll(":", replace);
        fileName = fileName.replaceAll("\\?", replace);
        fileName = fileName.replaceAll("\"", replace);
        fileName = fileName.replaceAll("\\*", replace);
        fileName = fileName.replaceAll("<", replace);
        fileName = fileName.replaceAll(">", replace);
        fileName = fileName.replaceAll("\\|", replace);
        fileName = fileName.trim();
        fileName = StringUtils.removeEnd(fileName, ".");
        fileName = fileName.trim();

        String ff;
        while (!(ff = fileName.replaceAll("  ", " ")).equals(fileName)) {
            fileName = ff;
        }

        return fileName;
    }

    static String maxFileNameLength(String fileName) {
        int max = 255;
        if (fileName.length() > max)
            fileName = fileName.substring(0, max);
        return fileName;
    }

    public void setTargetDir(File targetDir) {
        this.targetDir = targetDir;
    }

    /**
     * get output file on local file system
     *
     * @return
     */
    public File getTarget() {
        return targetFile;
    }

    public void setTarget(File file) {
        targetForce = file;
    }

    public VideoInfo getVideo() {
        return videoInfo;
    }

    public void downloadVideo() {
        downloadVideo(null, new AtomicBoolean(false), new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    public void download(VideoParser videoParser) {
        downloadVideo(videoParser, new AtomicBoolean(false), new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    boolean done(AtomicBoolean stop) {
        if (stop.get())
            throw new DownloadInterruptedError("stop");
        if (Thread.currentThread().isInterrupted())
            throw new DownloadInterruptedError("interrupted");

        return true;
    }

    void retryDownload(VideoParser videoParser, AtomicBoolean stop, Runnable notify, Throwable e) {
        logger.info("Starting retryDownload of video ");
        boolean retracted = false;

        while (!retracted) {
            for (int i = RetryWrap.RETRY_DELAY; i >= 0; i--) {
                if (stop.get())
                    throw new DownloadInterruptedError("stop");
                if (Thread.currentThread().isInterrupted())
                    throw new DownloadInterruptedError("interrupted");

                videoInfo.setDelay(i, e);
                notify.run();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ee) {
                    throw new DownloadInterruptedError(ee);
                }
            }

            try {
                // if we continue to downloadVideo from old source, and this proxy
                // server is
                // down we have to try to extractDownloadInfo new videoInfo and try to resumeDownload
                // downloadVideo

                DownloadInfo oldDownloadInfo = videoInfo.getDownloadInfo();
                videoInfo.extractDownloadInfo(videoParser, stop, notify);
                DownloadInfo newDownloadInfo = videoInfo.getDownloadInfo();

                if (oldDownloadInfo != null && oldDownloadInfo.resumeDownload(newDownloadInfo)) {
                    newDownloadInfo.copy(oldDownloadInfo);
                } else {
                    if (targetFile != null) {
                        FileUtils.deleteQuietly(targetFile);
                        targetFile = null;
                    }
                }

                retracted = true;
            } catch (DownloadIOCodeError ee) {
                if (retry(ee)) {
                    videoInfo.setState(VideoInfo.States.RETRYING, ee);
                    notify.run();
                } else {
                    throw ee;
                }
            } catch (DownloadRetry ee) {
                videoInfo.setState(VideoInfo.States.RETRYING, ee);
                notify.run();
            }
        }
    }

    void createTargetFile(DownloadInfo downloadInfo) {
        if (targetForce != null) {
            targetFile = targetForce;

            if (downloadInfo.isMultiPart()) {
                if (!DirectMultipart.canResume(downloadInfo, targetFile))
                    targetFile = null;
            } else if (downloadInfo.getRange()) {
                if (!DirectRange.canResume(downloadInfo, targetFile))
                    targetFile = null;
            } else {
                if (!DirectSingle.canResume(downloadInfo, targetFile))
                    targetFile = null;
            }
        }

        if (targetFile == null) {
            File f;

            Integer idupcount = 0;

            String sfilename = replaceBadChars(videoInfo.getTitle());

            sfilename = maxFileNameLength(sfilename);

            String contentType = downloadInfo.getContentType();
            if (contentType == null)
                throw new DownloadRetry("null content type");
            if (contentType.contains("text/html")) {
                String result = sfilename.replaceAll("[-+.^:,]", "");

                f = new File(targetDir, result + ".mp4");
            } else {
                String ext = contentType.replaceFirst("video/", "").replaceAll("x-", "");

                do {
                    String add = idupcount > 0 ? " (".concat(idupcount.toString()).concat(")") : "";

                    f = new File(targetDir, sfilename + add + "." + ext);
                    idupcount += 1;
                } while (f.exists());
            }

            targetFile = f;

            // if we dont have resumeDownload file (targetForce==null) then we shall
            // start over.
            downloadInfo.reset();
        }
    }

    boolean retry(Throwable e) {
        if (e == null)
            return true;

        if (e instanceof DownloadIOCodeError) {
            DownloadIOCodeError c = (DownloadIOCodeError) e;
            switch (c.getCode()) {
                case HttpURLConnection.HTTP_FORBIDDEN:
                case 416:
                    return true;
                default:
                    return false;
            }
        }

        return false;
    }

    /**
     * return status of downloadVideo information. subclassing for VideoInfo.empty();
     *
     * @return
     */
    public boolean empty() {
        return getVideo().empty();
    }

    public void extractVideo() {
        extractVideo(new AtomicBoolean(false), new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    public void extractVideo(AtomicBoolean stop, Runnable notify) {
        extractVideo(null, stop, notify);
    }

    /**
     * extractDownloadInfo video information, retryDownload until success
     *
     * @param stop
     * @param notify
     */
    public void extractVideo(VideoParser videoParser, AtomicBoolean stop, Runnable notify) {
        logger.info("Starting extraction of video {}", videoInfo.getTitle());
        while (done(stop)) {
            try {
                if (videoInfo.empty()) {
                    videoInfo.setState(VideoInfo.States.EXTRACTING);
                    videoInfo.extractDownloadInfo(videoParser, stop, notify);
                    videoInfo.setState(VideoInfo.States.EXTRACTING_DONE);
                    notify.run();
                }
                return;
            } catch (DownloadRetry | DownloadIOError e) {
                retryDownload(videoParser, stop, notify, e);
            } catch (DownloadMultipartError e) {
                checkFileNotFound(e);
                checkRetry(e);
                retryDownload(videoParser, stop, notify, e);
            } catch (DownloadIOCodeError e) {
                if (retry(e))
                    retryDownload(videoParser, stop, notify, e);
                else
                    throw e;
            }
        }
    }

    void checkRetry(DownloadMultipartError e) {
        for (Part ee : e.getInfo().getParts()) {
            if (!retry(ee.getException())) {
                throw e;
            }
        }
    }

    /**
     * checkConnection if all parts has the same filenotfound exception. if so throw DownloadError.FilenotFoundexcepiton
     *
     * @param e
     */
    void checkFileNotFound(DownloadMultipartError e) {
        FileNotFoundException f = null;
        for (Part ee : e.getInfo().getParts()) {
            // no error for this part? skip it
            if (ee.getException() == null)
                continue;
            // this exception has no cause? then it is not FileNotFound
            // excpetion. then do noting. this is checking function. do not
            // rethrow
            if (ee.getException().getCause() == null)
                return;
            if (ee.getException().getCause() instanceof FileNotFoundException) {
                // our first filenotfoundexception?
                if (f == null) {
                    // save it for later checks
                    f = (FileNotFoundException) ee.getException().getCause();
                } else {
                    // checkConnection filenotfound error message is it the same?
                    FileNotFoundException ff = (FileNotFoundException) ee.getException().getCause();
                    if (!ff.getMessage().equals(f.getMessage())) {
                        // if the filenotfound exception message is not the
                        // same. then we cannot retrhow filenotfound exception.
                        // return and continue checks
                        return;
                    }
                }
            } else {
                break;
            }
        }
        if (f != null)
            throw new DownloadError(f);
    }

    public void download(final AtomicBoolean stop, final Runnable notify) {
        downloadVideo(null, stop, notify);
    }

    public void downloadVideo(VideoParser user, final AtomicBoolean stop, final Runnable notify) {
        logger.info("Starting downloadVideo of video {}", videoInfo.getTitle());
        if (targetFile == null && targetForce == null && targetDir == null) {
            throw new RuntimeException("Set downloadVideo file or directory first");
        }

        try {
            if (empty()) {
                extractVideo(user, stop, notify);
            }

            while (done(stop)) {
                try {
                    final DownloadInfo dinfo = videoInfo.getDownloadInfo();

//                    if (dinfo.getContentType() == null || !dinfo.getContentType().contains("video/")) {
//                        throw new DownloadRetry("unable to downloadVideo video, bad content");
//                    }

                    createTargetFile(dinfo);

                    Direct direct;

                    if (dinfo.isMultiPart()) {
                        // multi part? overwrite.
                        direct = new DirectMultipart(dinfo, targetFile);
                    } else if (dinfo.getRange()) {
                        // range downloadVideo? try to resumeDownload downloadVideo from last
                        // position
                        if (targetFile.exists() && targetFile.length() != dinfo.getCount())
                            targetFile = null;
                        direct = new DirectRange(dinfo, targetFile);
                    } else {
                        // single downloadVideo? overwrite file
                        direct = new DirectSingle(dinfo, targetFile);
                    }

                    direct.download(stop, new Runnable() {
                        @Override
                        public void run() {
                            switch (dinfo.getState()) {
                                case DOWNLOADING:
                                    videoInfo.setState(VideoInfo.States.DOWNLOADING);
                                    notify.run();
                                    break;
                                case RETRYING:
                                    videoInfo.setDelay(dinfo.getDelay(), dinfo.getException());
                                    notify.run();
                                    break;
                                default:
                                    // we can safely skip all statues. (extracting -
                                    // already
                                    // pased, STOP / ERROR / DONE i will catch up
                                    // here
                            }
                        }
                    });

                    videoInfo.setState(VideoInfo.States.DONE);
                    notify.run();

                    // break while()
                    return;
                } catch (DownloadRetry | DownloadIOError e) {
                    retryDownload(user, stop, notify, e);
                } catch (DownloadMultipartError e) {
                    checkFileNotFound(e);
                    checkRetry(e);
                    retryDownload(user, stop, notify, e);
                } catch (DownloadIOCodeError e) {
                    if (retry(e))
                        retryDownload(user, stop, notify, e);
                    else
                        throw e;
                }
            }
        } catch (DownloadInterruptedError e) {
            videoInfo.setState(VideoInfo.States.STOP, e);
            notify.run();

            throw e;
        } catch (RuntimeException e) {
            videoInfo.setState(VideoInfo.States.ERROR, e);
            notify.run();

            throw e;
        }
    }
}
