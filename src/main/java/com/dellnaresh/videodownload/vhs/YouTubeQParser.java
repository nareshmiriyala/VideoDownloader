package com.dellnaresh.videodownload.vhs;

import com.dellnaresh.videodownload.info.VideoInfo;
import com.dellnaresh.wget.info.DownloadInfo;
import com.dellnaresh.wget.info.ex.DownloadError;
import com.dellnaresh.wget.info.ex.DownloadRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class YouTubeQParser extends YouTubeParser {
    private static final Logger logger = LoggerFactory.getLogger(YouTubeParser.class);
    VideoInfo.VideoQuality q;

    public YouTubeQParser(URL input, VideoInfo.VideoQuality q) {
        super(input);

        this.q = q;
    }

    public DownloadInfo extractDownloadInfo(final VideoInfo videoInfo, final AtomicBoolean stop, final Runnable notify) {
        logger.info("Starting extraction of video {}", videoInfo.getTitle());
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
            boolean found;

            found = q.equals(v.videoQuality);

            if (found) {
                videoInfo.setVideoQuality(v.videoQuality);
                DownloadInfo info = new DownloadInfo(v.url);
                videoInfo.setDownloadInfo(info);
                return info;
            }
        }

        // throw downloadVideo stop if user choice not maximum quality and we have no
        // video rendered by youtube

        throw new DownloadError("no video user quality found");
    }

}
