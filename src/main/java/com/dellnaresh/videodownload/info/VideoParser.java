package com.dellnaresh.videodownload.info;

import com.dellnaresh.wget.info.DownloadInfo;
import com.dellnaresh.wget.info.ex.DownloadError;
import com.dellnaresh.wget.info.ex.DownloadRetry;

import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class VideoParser {

    public abstract List<VideoDownload> extractLinks(final VideoInfo info, final AtomicBoolean stop,
                                                     final Runnable notify);

    public DownloadInfo extract(final VideoInfo vinfo, final AtomicBoolean stop, final Runnable notify) {
        List<VideoDownload> sNextVideoURL = extractLinks(vinfo, stop, notify);

        if (sNextVideoURL.size() == 0) {
            // rare error:
            //
            // The live recording you're trying to play is still being processed
            // and will be available soon. Sorry, please try again later.
            //
            // retry. since youtube may already rendrered propertly quality.
            throw new DownloadRetry("empty video download list," + " wait until youtube will process the video");
        }

        Collections.sort(sNextVideoURL, new VideoContentFirst());

        for (VideoDownload v : sNextVideoURL) {
            vinfo.setVideoQuality(v.vq);
            DownloadInfo info = new DownloadInfo(v.url);
            vinfo.setInfo(info);
            return info;
        }

        // throw download stop if user choice not maximum quality and we have no
        // video rendered by youtube

        throw new DownloadError("no video with required quality found,"
                + " increace VideoInfo.setVq to the maximum and retry download");
    }

    static public class VideoDownload {
        public VideoInfo.VideoQuality vq;
        public URL url;

        public VideoDownload(VideoInfo.VideoQuality vq, URL u) {
            this.vq = vq;
            this.url = u;
        }
    }

    static public class VideoContentFirst implements Comparator<VideoDownload> {

        @Override
        public int compare(VideoDownload o1, VideoDownload o2) {
            Integer i1 = o1.vq.ordinal();
            Integer i2 = o2.vq.ordinal();

            return i1.compareTo(i2);
        }

    }
}
