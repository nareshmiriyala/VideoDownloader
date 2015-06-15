package com.dellnaresh.videodownload;

import com.dellnaresh.videodownload.config.ConfigReader;
import com.dellnaresh.videodownload.info.VideoInfo;
import com.dellnaresh.videodownload.info.VideoParser;
import com.dellnaresh.videodownload.vhs.YouTubeQParser;
import com.dellnaresh.wget.info.DownloadInfo;
import com.dellnaresh.wget.info.DownloadInfo.Part;
import com.dellnaresh.wget.info.DownloadInfo.Part.States;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppManagedDownloadTest {

    VideoInfo info;
    long last;
    String url;
    String path;
    String vimeoUrl;
    private final Logger logger = LoggerFactory.getLogger(AppManagedDownloadTest.class);

    public void run(String url, File path) {
        try {
            AtomicBoolean stop = new AtomicBoolean(false);
            Runnable notify = new Runnable() {

                @Override
                public void run() {
                    VideoInfo i1 = info;
                    DownloadInfo i2 = i1.getDownloadInfo();

                    // notify app or save downloadVideo state
                    // you can extractDownloadInfo information from DownloadInfo info;
                    switch (i1.getState()) {
                        case EXTRACTING:
                        case EXTRACTING_DONE:
                        case DONE:
                            logger.info("Download state {},Downloaded video quality {}", i1.getState(), i1.getVideoQuality());
                            break;
                        case RETRYING:
                            logger.info("Retrying downloadVideo with delay {} having state {}", i1.getDelay(), i1.getState());
                            break;
                        case STOP:
                            logger.error("Stopping download");
                            System.exit(0);
                            break;
                        case ERROR:
                            logger.error("Error during download");
                            System.exit(0);
                            break;
                        case DOWNLOADING:
                            long now = System.currentTimeMillis();
                            if (now - 1000 > last) {
                                last = now;

                                String parts = "";

                                List<Part> pp = i2.getParts();
                                if (pp != null) {
                                    // isMultiPart downloadVideo
                                    for (Part p : pp) {
                                        if (p.getState().equals(States.DOWNLOADING)) {
                                            parts += String.format("Part#%d(%.2f) ", p.getNumber(), p.getCount()
                                                    / (float) p.getLength());
                                        }
                                    }
                                }
                                logger.info(String.format("%s %.2f %s", i1.getState(),
                                        i2.getCount() / (float) i2.getLength(), parts));
                            }
                            break;
                        default:
                            break;
                    }
                }
            };

            info = new VideoInfo(new URL(url));

            // [OPTIONAL] limit maximum quality, or do not call this function if
            // you wish maximum quality available.
            //
            // if youtube does not have video with requested quality, program
            // will raise en exception.
            VideoParser user;

            // create simple youtube request
            //user = new YouTubeParser(info.getWebUrl());
            // downloadVideo maximum video quality
            user = new YouTubeQParser(info.getWebUrl(), VideoInfo.VideoQuality.p360);
            // downloadVideo non webm only
            //user = new YouTubeMPGParser(info.getWebUrl(), VideoQuality.p480);

            VideoDownload v = new VideoDownload(info, path);

            // [OPTIONAL] call v.extractDownloadInfo() only if you d like to get video title
            // or downloadVideo url link
            // before start downloadVideo. or just skip it.
            v.extractVideo(user, stop, notify);

            logger.info("Title: {} ", info.getTitle());
            logger.info("Download URL: {} ", info.getDownloadInfo().getSource());

            v.downloadVideo(user, stop, notify);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setUp() throws Exception {
        // ex: http://www.youtube.com/watch?v=Nj6PFaDmp6c
        ConfigReader instance = ConfigReader.getInstance();

        path = instance.getPropertyValue("download.directory");
        vimeoUrl = instance.getPropertyValue("vimeo.download.url");
        // ex: /Users/axet/Downloads/
        url = instance.getPropertyValue("youtube.download.url");

    }

    @Test
    public void testAppManagedDownload() throws FileNotFoundException {
        AppManagedDownloadTest e = new AppManagedDownloadTest();

        e.run(url, new File(path));
    }
}