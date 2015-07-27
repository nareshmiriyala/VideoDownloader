package com.dellnaresh.wget;

import com.dellnaresh.util.Constants;
import com.dellnaresh.wget.info.DownloadInfo;
import com.dellnaresh.wget.info.ex.DownloadInterruptedError;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
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
    private File target = null;
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
        info.setCount(0);
        OutputStream outputStream=new FileOutputStream(target);
        InputStream inputStream = info.getSource().openStream();
        IOUtils.copy(inputStream,outputStream);

        if (stop.get())
            throw new DownloadInterruptedError(Constants.ERRORS.STOPPED);
        if (Thread.interrupted())
            throw new DownloadInterruptedError(Constants.ERRORS.INTERRUPTED);

//        FileUtils.copyURLToFile(info.getSource(), target);
    }

    public File getTarget() {
        return target;
    }

    public void setTarget(File target) {
        this.target = target;
    }
}
