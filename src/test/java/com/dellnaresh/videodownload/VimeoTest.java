package com.dellnaresh.videodownload;

import org.junit.Test;

import java.io.File;

public class VimeoTest extends AppManagedDownloadTest {
    @Test
    public void testVimeoDownload() throws Exception {
        AppManagedDownloadTest e = new AppManagedDownloadTest();
        e.run(vimeoUrl, new File(path));
    }

}
