package com.dellnaresh.videodownload.vhs;

import java.net.URL;
import java.util.List;

import com.dellnaresh.videodownload.info.VideoParser;
import com.dellnaresh.videodownload.info.VideoInfo;

public class YouTubeMPGParser extends YouTubeParser {

    public YouTubeMPGParser(URL input, VideoInfo.VideoQuality q) {
        super(input);
    }

    void addVideo(List<VideoParser.VideoDownload> sNextVideoURL, String itag, URL url) {
        Integer i = Integer.parseInt(itag);

        // get rid of webm
        switch (i) {
        case 102:
        case 101:
        case 100:
        case 46:
        case 45:
        case 44:
        case 43:
            return;
        }

        super.addVideo(sNextVideoURL, itag, url);
    }

}
