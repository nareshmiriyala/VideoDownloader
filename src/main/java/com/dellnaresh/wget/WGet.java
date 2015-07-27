package com.dellnaresh.wget;

import com.dellnaresh.util.Constants;
import com.dellnaresh.wget.info.DownloadInfo;
import com.dellnaresh.wget.info.ex.DownloadInterruptedError;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WGet {
    private static final Logger logger = LoggerFactory.getLogger(WGet.class);
    private Direct direct;
    private File targetFile;
    private DownloadInfo info;

    /**
     * downloadVideo with events control.
     *
     * @param source
     * @param target
     */
    public WGet(URL source, File target) {
        create(source, target);
    }

    /**
     * application controlled downloadVideo / resumeDownload. you should specify targetfile
     * name exactly. since you are choise resumeDownload / isMultiPart downloadVideo.
     * application unable to control file name choise / creation.
     *
     * @param downloadInfo
     * @param targetFile
     */
    public WGet(DownloadInfo downloadInfo, File targetFile) {
        this.info = downloadInfo;
        this.targetFile = targetFile;
        create();
    }

    public static File calcName(URL source, File target) {
        DownloadInfo downloadInfo = new DownloadInfo(source);
        downloadInfo.extract();

        return calcName(downloadInfo, target);
    }

    private static File calcName(DownloadInfo downloadInfo, File target) {
        logger.info("Calling calculate name");
        // target -
        // 1) can point to directory.
        // - generate exclusive (1) name.
        // 2) to exisiting file
        // 3) to non existing file

        String name;

        name = downloadInfo.getContentFilename();

        if (name == null)
            name = new File(downloadInfo.getSource().getPath()).getName();

        try {
            name = URLDecoder.decode(name, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String nameNoExt = FilenameUtils.removeExtension(name);
        String ext = FilenameUtils.getExtension(name);

        File targetFile;

        if (target.isDirectory()) {
            targetFile = FileUtils.getFile(target, name);
            int i = 1;
            while (targetFile.exists()) {
                targetFile = FileUtils.getFile(target, nameNoExt + " (" + i + ")." + ext);
                i++;
            }
        } else {
            try {
                FileUtils.forceMkdir(new File(target.getParent()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            targetFile = target;
        }

        return targetFile;
    }

    public static String getHtml(URL source) {
        logger.info("Calling getHtml");
        return getHtml(source, new HtmlLoader() {
            @Override
            public void notifyRetry(int delay, Throwable e) {
            }

            @Override
            public void notifyDownloading() {
            }

            @Override
            public void notifyMoved() {
            }
        }, new AtomicBoolean(false));
    }

    public static String getHtml(final URL source, final HtmlLoader load, final AtomicBoolean stop) {
        return getHtml(new DownloadInfo(source), load, stop);
    }

    private static String getHtml(final DownloadInfo source, final HtmlLoader load, final AtomicBoolean stop) {

        return RetryWrap.wrap(stop, new WrapReturn<String>() {
            DownloadInfo info = source;

            @Override
            public void retry(int delay, Throwable e) {
                load.notifyRetry(delay, e);
            }

            @Override
            public String download() throws IOException {
                HttpURLConnection conn;

                conn = (HttpURLConnection) info.getSource().openConnection();

                conn.setConnectTimeout(Direct.CONNECT_TIMEOUT);
                conn.setReadTimeout(Direct.READ_TIMEOUT);

                conn.setRequestProperty(Constants.USER_AGENT, info.getUserAgent());
                if (info.getReferrer() != null)
                    conn.setRequestProperty(Constants.REFERRER, info.getReferrer().toExternalForm());

//                RetryWrap.checkConnection(conn);

                InputStream is = conn.getInputStream();

                String enc = conn.getContentEncoding();

                if (enc == null) {
                    Pattern p = Pattern.compile("charset=(.*)");
                    Matcher m = p.matcher(conn.getHeaderField("Content-Type"));
                    if (m.find()) {
                        enc = m.group(1);
                    }
                }

                if (enc == null)
                    enc = "UTF-8";

                BufferedReader br = new BufferedReader(new InputStreamReader(is, enc));

                String line;

                StringBuilder contents = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    contents.append(line);
                    contents.append("\n");

                    if (stop.get())
                        throw new DownloadInterruptedError(Constants.ERRORS.STOPPED);
                }
                is.close();
                br.close();
                return contents.toString();
            }

            @Override
            public void moved(URL url) {
                DownloadInfo old = info;
                info = new DownloadInfo(url);
                info.setReferrer(old.getReferrer());

                load.notifyMoved();
            }

        });
    }

    private void create(URL source, File target) {
        info = new DownloadInfo(source);
        info.extract();
        create(target);
    }

    private void create(File target) {
        targetFile = calcName(info, target);
        create();
    }

    private void create() {
        direct = createDirect();
    }

    private Direct createDirect() {
        if (info.isMultiPart()) {
            return new DirectMultipart(info, targetFile);
        } else if (info.getRange()) {
            return new DirectRange(info, targetFile);
        } else {
            return new DirectSingle(info, targetFile);
        }
    }

    public void download() {
        download(new AtomicBoolean(false), new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    private void download(AtomicBoolean stop, Runnable notify) {
        direct.download(stop, notify);
    }

    public DownloadInfo getInfo() {
        return info;
    }

    public interface HtmlLoader {
        /**
         * some socket problem, retyring
         *
         * @param delay
         * @param e
         */
        void notifyRetry(int delay, Throwable e);

        /**
         * start downloading
         */
        void notifyDownloading();

        /**
         * document moved, relocating
         */
        void notifyMoved();
    }
}
