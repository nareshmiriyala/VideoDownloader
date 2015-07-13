package com.dellnaresh.wget;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.net.HttpURLConnection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by nareshm on 12/07/2015.
 */
@RunWith(PowerMockRunner.class)
public class RetryWrapTest {
    private RetryWrap retryWrap;
    @Mock
    private HttpURLConnection mockConnection;

    @Mock
    private AtomicBoolean mockAtomicBoolean;

    @Mock
    private WrapReturn mockWrapReturn;
    @Before
    public void setUp() throws Exception {
        retryWrap= PowerMockito.spy(new RetryWrap());
    }

    @After
    public void tearDown() throws Exception {

    }


    @Test
    public void testWrap1() throws Exception {

    }

    @Test
    public void testCheckConnection() throws Exception {
        retryWrap.checkConnection(mockConnection);

    }
    @Test
    public void testRun() throws Exception{
        Whitebox.invokeMethod(retryWrap,"run",mockAtomicBoolean,mockWrapReturn);
    }
}