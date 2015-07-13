package com.dellnaresh.wget;

import com.dellnaresh.util.Constants;
import com.dellnaresh.videodownload.config.ConfigReader;
import com.dellnaresh.wget.info.DownloadInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by nareshm on 12/07/2015.
 */
@RunWith(PowerMockRunner.class)
public class WGetTest {
    private WGet wGet;

    private DownloadInfo mockDownloadInfo;
    private AtomicBoolean mockAtomicBoolean;

    private File mockFile;
    private URL mockURL;
    @Mock
    private WGet.HtmlLoader mockHtmlLoader;

    @Before
    public void setUp() throws Exception {
        mockURL = new URL(Constants.SMALL_FILE_URL);
        mockFile = new File(ConfigReader.getInstance().getPropertyValue(Constants.DOWNLOAD_DIRECTORY_CONFIG) + "\\small.mp4");
        mockDownloadInfo = new DownloadInfo(mockURL);
        wGet = PowerMockito.spy(new WGet(mockDownloadInfo, mockFile));

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCalcName() throws Exception {

    }

    @Test
    public void testGetHtml() throws Exception {
        mockAtomicBoolean = new AtomicBoolean();
        wGet.getHtml(mockURL, mockHtmlLoader, mockAtomicBoolean);
    }

    @Test
    public void testGetHtml1() throws Exception {

    }

    @Test
    public void testDownload() throws Exception {

    }

    @Test
    public void testGetInfo() throws Exception {

    }
}