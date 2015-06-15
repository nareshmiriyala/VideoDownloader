package com.dellnaresh.wget;

import com.dellnaresh.wget.info.DownloadInfo;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;


public abstract class Direct {

    /**
     * connect socket timeout
     */
    static public final int CONNECT_TIMEOUT = 10000;
    /**
     * read socket timeout
     */
    static public final int READ_TIMEOUT = 10000;
    /**
     * size of read buffer
     */
    static public final int BUF_SIZE = 4 * 1024;
    File target = null;
    final DownloadInfo downloadInfo;

    /**
     * @param downloadInfo downloadVideo file information
     * @param target       target file
     */
    public Direct(DownloadInfo downloadInfo, File target) {
        this.target = target;
        this.downloadInfo = downloadInfo;
    }

    abstract public void download(AtomicBoolean stop, Runnable notify);

}
