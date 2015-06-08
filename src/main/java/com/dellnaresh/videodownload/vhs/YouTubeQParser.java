package com.dellnaresh.videodownload.vhs;

import com.dellnaresh.videodownload.info.VideoInfo;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadError;
import com.github.axet.wget.info.ex.DownloadRetry;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class YouTubeQParser extends YouTubeParser {

    VideoInfo.VideoQuality q;

    public YouTubeQParser(URL input, VideoInfo.VideoQuality q) {
        super(input);

        this.q = q;
    }

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
            boolean found;

            found = q.equals(v.vq);

            if (found) {
                vinfo.setVideoQuality(v.vq);
                DownloadInfo info = new DownloadInfo(v.url);
                vinfo.setInfo(info);
                return info;
            }
        }

        // throw download stop if user choice not maximum quality and we have no
        // video rendered by youtube

        throw new DownloadError("no video user quality found");
    }

}
