package com.dellnaresh.videodownload;

import java.io.File;
import java.net.URL;

public class DirectDownload {

    public static void main(String[] args) {
        try {
            // ex: http://www.youtube.com/watch?v=Nj6PFaDmp6c
            String url = args[0];
            // ex: "/Users/axet/Downloads"
            String path = args[1];

            VideoDownload v = new VideoDownload(new URL(url), new File(path));

            v.download();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
