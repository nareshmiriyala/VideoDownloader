package com.dellnaresh.wget.info.ex;


import com.dellnaresh.wget.info.DownloadInfo;

public class DownloadMultipartError extends DownloadError {

    private static final long serialVersionUID = 7835308901669107488L;

    final DownloadInfo info;

    public DownloadMultipartError(DownloadInfo info) {
        super("Multipart error");

        this.info = info;
    }

    public DownloadInfo getInfo() {
        return info;
    }

}
