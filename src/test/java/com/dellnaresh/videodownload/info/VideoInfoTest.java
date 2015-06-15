package com.dellnaresh.videodownload.info;

import com.dellnaresh.videodownload.vhs.YouTubeParser;
import com.dellnaresh.wget.info.ex.DownloadError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(PowerMockRunner.class)
@PrepareForTest({YouTubeParser.class})
public class VideoInfoTest {
    URL mockYoutubeURL;
    @Mock
    private VideoParser mockVideoParser;
    @Mock
    private AtomicBoolean mockAtomicBoolean;
    @Mock
    private Runnable mockRunnable;
    private VideoInfo videoInfo;

    @Before
    public void setup() throws Exception {
        mockYoutubeURL = new URL("https://www.youtube.com/watch?v=1Rk5fM02FCE");
        videoInfo = new VideoInfo(mockYoutubeURL);
    }

    @Test(expected = DownloadError.class)
    public void testExtract() throws Exception {
        videoInfo.extractDownloadInfo(mockVideoParser, mockAtomicBoolean, mockRunnable);

    }
}