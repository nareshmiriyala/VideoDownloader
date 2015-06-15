package com.dellnaresh.videodownload;

import com.dellnaresh.videodownload.config.ConfigReader;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

public class DirectDownloadTest {
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
    public void testDirectDownload() throws Exception {
        try {
            VideoDownload v = new VideoDownload(new URL(url), new File(path));
            v.downloadVideo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
