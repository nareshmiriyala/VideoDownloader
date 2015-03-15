package com.dellnaresh.videodownload;

import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.dellnaresh.videodownload.info.VideoParser;
import com.dellnaresh.videodownload.info.VideoInfo;
import com.dellnaresh.videodownload.vhs.YouTubeParser;

public class ExtractDownloadLinks {

    public static void main(String[] args) {
        try {
            // ex: http://www.youtube.com/watch?v=Nj6PFaDmp6c
            String url = args[0];

            VideoInfo info = new VideoInfo(new URL(url));

            YouTubeParser parser = new YouTubeParser(info.getWeb());

            List<VideoParser.VideoDownload> list = parser.extractLinks(info, new AtomicBoolean(), new Runnable() {
                @Override
                public void run() {
                }
            });

            for (VideoParser.VideoDownload d : list) {
                System.out.println(d.vq + " " + d.url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
