package com.dellnaresh.wget;

import com.dellnaresh.util.Constants;
import com.dellnaresh.videodownload.config.ConfigReader;
import com.dellnaresh.wget.info.DownloadInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by nareshm on 12/07/2015.
 */
@RunWith(PowerMockRunner.class)
public class DirectMultipartTest {

    private DirectMultipart directMultipart;
    @Mock
    private DownloadInfo mockDownloadInfo;

    private File mockFile;

    @Mock
    private AtomicBoolean mockAtomicBoolean;

    @Mock
    private Runnable mockRunnable;

    private URL mockURL;
    @Mock
    private List<DownloadInfo.Part> parts=new ArrayList<>();

    @Mock
    private  DownloadInfo.Part mockPart;

    @Before
    public void setUp() throws Exception {
        mockURL=new URL(Constants.SMALL_FILE_URL);
        when(mockDownloadInfo.getSource()).thenReturn(mockURL);
        when(mockPart.getState()).thenReturn(DownloadInfo.Part.States.RETRYING);
        parts.add(mockPart);

        when(mockDownloadInfo.getParts()).thenReturn(parts);
        mockFile=new File(ConfigReader.getInstance().getPropertyValue(Constants.DOWNLOAD_DIRECTORY_CONFIG)+"\\small.mp4");
        directMultipart=new DirectMultipart(mockDownloadInfo,mockFile);

    }

    @After
    public void tearDown() throws Exception {

    }


    @Test
    public void testDownload() throws Exception {
        directMultipart.download(mockAtomicBoolean,mockRunnable);
    }
}