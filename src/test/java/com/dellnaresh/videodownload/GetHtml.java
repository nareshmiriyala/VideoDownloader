package com.dellnaresh.videodownload;

import com.dellnaresh.videodownload.info.VideoParser;
import com.dellnaresh.videodownload.vhs.YouTubeParser;
import com.dellnaresh.wget.WGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetHtml {
    final static String UTF8 = "UTF-8";

    public static void main(String[] args) {
        try {
            System.out.println("Enter download url:");
            // ex: http://www.youtube.com/watch?v=Nj6PFaDmp6c
            BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
            String urlStr = bufferRead.readLine();
            URL url1 = new URL(urlStr);
            YouTubeParser youTubeParser=new YouTubeParser(url1);

            String h = WGet.getHtml(url1);
            List<VideoParser.VideoDownload> sNextVideoURL = new ArrayList<>();
            Map<String, String> queryMap = YouTubeParser.getQueryMap(h);
            String urlEncodedFmtStreamMap = queryMap.get("url_encoded_fmt_stream_map");
            Document doc = Jsoup.connect(urlStr).get();
            Elements select = doc.select(urlEncodedFmtStreamMap);
            String url_encoded_fmt_stream_map=null;
            if(urlEncodedFmtStreamMap!=null) {
                 url_encoded_fmt_stream_map= URLDecoder.decode(urlEncodedFmtStreamMap, UTF8);

            }
//            youTubeParser.extractUrlEncodedVideos(sNextVideoURL, url_encoded_fmt_stream_map);

            System.out.println(h);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
