package com.dellnaresh.videodownload;

import com.dellnaresh.videodownload.config.ConfigReader;
import com.dellnaresh.videodownload.info.VideoInfo;
import com.dellnaresh.videodownload.info.VideoParser;
import com.dellnaresh.videodownload.vhs.YouTubeQParser;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadInfo.Part;
import com.github.axet.wget.info.DownloadInfo.Part.States;
import org.junit.Before;
import org.junit.Test;

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

    public void run(String url, File path) {
        try {
            AtomicBoolean stop = new AtomicBoolean(false);
            Runnable notify = () -> {
                VideoInfo i1 = info;
                DownloadInfo i2 = i1.getInfo();

                // notify app or save download state
                // you can extract information from DownloadInfo info;
                switch (i1.getState()) {
                    case EXTRACTING:
                    case EXTRACTING_DONE:
                    case DONE:
                        System.out.println(i1.getState() + " " + i1.getVideoQuality());
                        break;
                    case RETRYING:
                        System.out.println(i1.getState() + " " + i1.getDelay());
                        break;
                    case DOWNLOADING:
                        long now = System.currentTimeMillis();
                        if (now - 1000 > last) {
                            last = now;

                            String parts = "";

                            List<Part> pp = i2.getParts();
                            if (pp != null) {
                                // multipart download
                                for (Part p : pp) {
                                    if (p.getState().equals(States.DOWNLOADING)) {
                                        parts += String.format("Part#%d(%.2f) ", p.getNumber(), p.getCount()
                                                / (float) p.getLength());
                                    }
                                }
                            }

                            System.out.println(String.format("%s %.2f %s", i1.getState(),
                                    i2.getCount() / (float) i2.getLength(), parts));
                        }
                        break;
                    default:
                        break;
                }
            };

            info = new VideoInfo(new URL(url));

            // [OPTIONAL] limit maximum quality, or do not call this function if
            // you wish maximum quality available.
            //
            // if youtube does not have video with requested quality, program
            // will raise en exception.
            VideoParser user = null;

            // create simple youtube request
            //user = new YouTubeParser(info.getWeb());
            // download maximum video quality
            user = new YouTubeQParser(info.getWeb(), VideoInfo.VideoQuality.p360);
            // download non webm only
            //user = new YouTubeMPGParser(info.getWeb(), VideoQuality.p480);

            VideoDownload v = new VideoDownload(info, path);

            // [OPTIONAL] call v.extract() only if you d like to get video title
            // or download url link
            // before start download. or just skip it.
            v.extract(user, stop, notify);

            System.out.println("Title: " + info.getTitle());
            System.out.println("Download URL: " + info.getInfo().getSource());

            v.download(user, stop, notify);
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