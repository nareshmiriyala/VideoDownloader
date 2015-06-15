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

    public abstract List<VideoDownload> extractLinks(final VideoInfo videoInfo, final AtomicBoolean stop,
                                                     final Runnable notify);

    public DownloadInfo extractDownloadInfo(final VideoInfo videoInfo, final AtomicBoolean stop, final Runnable notify) {
        List<VideoDownload> sNextVideoURL = extractLinks(videoInfo, stop, notify);

        if (sNextVideoURL.size() == 0) {
            // rare error:
            //
            // The live recording you're trying to play is still being processed
            // and will be available soon. Sorry, please try again later.
            //
            // retry. since youtube may already rendrered propertly quality.
            throw new DownloadRetry("empty video downloadVideo list," + " wait until youtube will process the video");
        }

        Collections.sort(sNextVideoURL, new VideoContentFirst());

        for (VideoDownload v : sNextVideoURL) {
            videoInfo.setVideoQuality(v.videoQuality);
            DownloadInfo downloadInfo = new DownloadInfo(v.url);
            videoInfo.setDownloadInfo(downloadInfo);
            return downloadInfo;
        }

        // throw downloadVideo stop if user choice not maximum quality and we have no
        // video rendered by youtube

        throw new DownloadError("no video with required quality found,"
                + " increase VideoInfo.setVq to the maximum and retry downloadVideo");
    }

    static public class VideoDownload {
        public final VideoInfo.VideoQuality videoQuality;
        public final URL url;

        public VideoDownload(VideoInfo.VideoQuality videoQuality, URL url) {
            this.videoQuality = videoQuality;
            this.url = url;
        }
    }

    static public class VideoContentFirst implements Comparator<VideoDownload> {

        @Override
        public int compare(VideoDownload o1, VideoDownload o2) {
            Integer i1 = o1.videoQuality.ordinal();
            Integer i2 = o2.videoQuality.ordinal();

            return i1.compareTo(i2);
        }

    }
}
