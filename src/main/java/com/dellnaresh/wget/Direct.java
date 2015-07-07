package com.dellnaresh.wget;

import com.dellnaresh.util.Constants;
import com.dellnaresh.wget.info.DownloadInfo;
import com.dellnaresh.wget.info.ex.DownloadInterruptedError;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
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
    static final int BUF_SIZE = 4 * 1024;
    File target = null;
    final DownloadInfo downloadInfo;

    /**
     * @param downloadInfo downloadVideo file information
     * @param target       target file
     */
    Direct(DownloadInfo downloadInfo, File target) {
        this.target = target;
        this.downloadInfo = downloadInfo;
    }

    abstract public void download(AtomicBoolean stop, Runnable notify);

    protected void downloadFile(DownloadInfo info, AtomicBoolean stop, Runnable notify) throws IOException {
        if (stop.get())
            throw new DownloadInterruptedError(Constants.ERRORS.STOPPED);
        if (Thread.interrupted())
            throw new DownloadInterruptedError(Constants.ERRORS.INTERRUPTED);

        FileUtils.copyURLToFile(info.getSource(), target);
    }

}
