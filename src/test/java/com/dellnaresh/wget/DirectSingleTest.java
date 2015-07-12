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

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by nareshm on 12/07/2015.
 */
@RunWith(PowerMockRunner.class)
public class DirectSingleTest {

    private DirectSingle directSingle;

    @Mock
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
        directSingle=new DirectSingle(mockDownloadInfo,mockFile);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCanResume() throws Exception {
       DirectSingle.canResume(mockDownloadInfo,mockFile);
    }

    @Test
    public void testDownloadPart() throws Exception {
        when(mockDownloadInfo.getSource()).thenReturn(mockURL);
        directSingle.downloadPart(mockDownloadInfo, mockAtomicBoolean, mockRunnable);
    }
    @Test
    public void testDownload() throws Exception{
        when(mockDownloadInfo.getSource()).thenReturn(mockURL);
        directSingle.download(mockAtomicBoolean, mockRunnable);
        Mockito.verify(mockDownloadInfo,times(1)).setState(URLInfo.States.DONE);
    }
}