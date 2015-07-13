package com.dellnaresh.wget;

import java.io.IOException;
import java.net.URL;

/**
 * Created by nareshm on 12/07/2015.
 */
public interface WrapReturn<T> {
    void retry(int delay, Throwable e);

    void moved(URL url);

    T download() throws IOException;
}
