package com.dellnaresh.wget;

import com.dellnaresh.util.Constants;
import com.dellnaresh.videodownload.config.ConfigReader;
import com.dellnaresh.wget.info.DownloadInfo;
import com.dellnaresh.wget.info.URLInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by nareshm on 12/07/2015.
 */
@RunWith(PowerMockRunner.class)
public class DirectRangeTest {
    private DirectRange directRange;

    private DownloadInfo mockDownloadInfo;

    private File mockFile;

    @Mock
    private AtomicBoolean mockAtomicBoolean;

    @Mock
    private Runnable mockRunnable;

    private URL mockURL;

    @Before
    public void setUp() throws Exception {
        mockURL=new URL(Constants.SMALL_FILE_URL);
        mockFile=new File(ConfigReader.getInstance().getPropertyValue(Constants.DOWNLOAD_DIRECTORY_CONFIG)+"\\small.mp4");
        mockDownloadInfo=new DownloadInfo(mockURL);
        directRange=new DirectRange(mockDownloadInfo,mockFile);

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCanResume() throws Exception {
    DirectRange.canResume(mockDownloadInfo, mockFile);
    }

    @Test
    public void testDownload() throws Exception {
        directRange.download(mockAtomicBoolean,mockRunnable);
        assertEquals(mockDownloadInfo.getState(),URLInfo.States.DONE);
    }
}