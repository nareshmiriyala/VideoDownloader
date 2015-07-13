package com.dellnaresh.wget.info;

import com.dellnaresh.util.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by nareshm on 12/07/2015.
 */
@RunWith(PowerMockRunner.class)
public class URLInfoTest {

    private URLInfo urlInfo;
    private URL mockURL;
    @Mock
    private Runnable mockRunnable;
    @Mock
    private AtomicBoolean mockAtomicBoolean;

    @Before
    public void setUp() throws Exception {
        mockURL = new URL(Constants.SMALL_FILE_URL);
        urlInfo = PowerMockito.spy(new URLInfo(mockURL));
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testExtract() throws Exception {
        urlInfo.extract(mockAtomicBoolean,mockRunnable);
    }

    @Test
    public void testExtract1() throws Exception {

    }
}