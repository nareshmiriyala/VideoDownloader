package com.dellnaresh.videodownload;

import com.dellnaresh.videodownload.config.ConfigReader;
import com.dellnaresh.videodownload.info.VideoInfo;
import com.dellnaresh.videodownload.info.VideoParser;
import com.dellnaresh.videodownload.vhs.YouTubeParser;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExtractDownloadLinksTest {
    String url;
    String path;

    @Before
    public void setUp() throws Exception {
        // ex: http://www.youtube.com/watch?v=Nj6PFaDmp6c
        ConfigReader instance = ConfigReader.getInstance();
        path = instance.getPropertyValue("download.directory");
        url = instance.getPropertyValue("youtube.download.url");

    }

    @Test
    public void testExtractDownloadLinks() throws Exception {
        try {

            VideoInfo info = new VideoInfo(new URL(url));

            YouTubeParser parser = new YouTubeParser(info.getWebUrl());

            List<VideoParser.VideoDownload> list = parser.extractLinks(info, new AtomicBoolean(), new Runnable() {

                @Override
                public void run() {
                }
            });

            for (VideoParser.VideoDownload d : list) {
                System.out.println(d.videoQuality + " " + d.url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
