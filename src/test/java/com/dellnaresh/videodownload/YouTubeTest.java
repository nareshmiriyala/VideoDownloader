package com.dellnaresh.videodownload;

import org.junit.Test;

import java.io.File;

public class YouTubeTest extends AppManagedDownloadTest {

    @Test
    public void testDownload() throws Exception {
        AppManagedDownloadTest e = new AppManagedDownloadTest();
        e.run(url, new File(path));
    }

}
