package com.dellnaresh.videodownload;

import com.dellnaresh.util.Constants;
import com.dellnaresh.videodownload.config.ConfigReader;
import com.dellnaresh.videodownload.info.VideoInfo;
import com.dellnaresh.videodownload.info.VideoParser;
import com.dellnaresh.wget.info.DownloadInfo;
import com.thoughtworks.xstream.mapper.Mapper;
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

import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by nareshm on 12/07/2015.
 */
@RunWith(PowerMockRunner.class)
public class VideoDownloadTest {
    private VideoDownload videoDownload;
    private URL mockURL;
    @Mock
    private Runnable mockRunnable;
    @Mock
    private DownloadInfo mockDownloadInfo;
    @Mock
    private VideoInfo mockVideoInfo;

    private File mockFile;
    @Mock
    private AtomicBoolean mockAtomicBoolean;
    @Mock
    private VideoParser mockVideoParser;

    @Before
    public void setUp() throws Exception {
        mockURL = new URL(Constants.SMALL_FILE_URL);
        mockFile = new File(ConfigReader.getInstance().getPropertyValue(Constants.DOWNLOAD_DIRECTORY_CONFIG) );
        when(mockDownloadInfo.getSource()).thenReturn(mockURL);
        when(mockDownloadInfo.getContentType()).thenReturn("text/html");
        when(mockVideoInfo.getTitle()).thenReturn("Hello_World");
        videoDownload = PowerMockito.spy(new VideoDownload(mockVideoInfo,mockFile));
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testReplaceBadChars() throws Exception {

    }

    @Test
    public void testMaxFileNameLength() throws Exception {

    }

    @Test
    public void testDownloadVideo() throws Exception {

    }

    @Test
    public void testDownload() throws Exception {

    }

    @Test
    public void testDone() throws Exception {

    }

    @Test
    public void testRetryDownload() throws Exception {

    }

    @Test
    public void testCreateTargetFile() throws Exception {

        videoDownload.createTargetFile(mockDownloadInfo);
    }

    @Test
    public void testExtractVideo() throws Exception {
        videoDownload.extractVideo();
    }

    @Test
    public void testExtractVideo1() throws Exception {

    }

    @Test
    public void testExtractVideo2() throws Exception {

    }

    @Test
    public void testCheckRetry() throws Exception {

    }

    @Test
    public void testCheckFileNotFound() throws Exception {

    }

    @Test(expected = NullPointerException.class)
    public void testDownload1() throws Exception {
        when(mockVideoInfo.getDownloadInfo()).thenReturn(mockDownloadInfo);
        videoDownload.downloadVideo(mockVideoParser,mockAtomicBoolean,mockRunnable);
    }

    @Test
    public void testDownloadVideo1() throws Exception {

    }
}