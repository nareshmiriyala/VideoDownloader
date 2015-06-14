package com.dellnaresh.videodownload;

import com.dellnaresh.videodownload.info.VideoParser;
import com.dellnaresh.videodownload.vhs.YouTubeParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nareshm on 13/06/2015.
 */
public class FindGoogleVideo {
    public static void main(String[] args) {
        String url = "https://www.youtube.com/watch?v=Fr3lIhSIZZE";
        System.out.println("Fetching %s..."+ url);
        String msg=null;

        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean found=false;
        int i=0;
        while(!found) {
            Element script = doc.select("script").get(i); // Get the script part
            i++;
            String pattern = "\"url_encoded_fmt_stream_map\":(.+?),";
            Pattern p = Pattern.compile(pattern); // Regex for the value of the key
            Matcher m = p.matcher(script.html()); // you have to use html here and NOT text! Text will drop the 'key' part


            while (m.find()) {
//                System.out.println(m.group()); // the whole key ('key = value')
//                System.out.println(m.group(1)); // value only
                msg=m.group(1);
                System.out.println("Found Message is:"+msg);
                found=true;
            }
        }
        try {
            YouTubeParser youTubeParser=new YouTubeParser(new URL(url));

        try {
            String url_encoded_fmt_stream_map= URLDecoder.decode(msg, "UTF-8");

            System.out.println("Value of URL:"+url_encoded_fmt_stream_map);
            List<VideoParser.VideoDownload> sNextVideoURL = new ArrayList<>();
            try {
//                youTubeParser.extractUrlEncodedVideos(sNextVideoURL, url_encoded_fmt_stream_map);
                String[] urlStrings = url_encoded_fmt_stream_map.split("url=");
                String urlString = StringEscapeUtils.unescapeJava(urlStrings[1]);

                String urlFull = URLDecoder.decode(urlString, "UTF-8");
                URL url1 = new URL(urlStrings[1]);
                System.out.println("ULR1 value:"+url1);
                FileUtils.copyURLToFile(url1, new File("C:\\\\Naresh Data\\\\Development Software\\\\Videos\\\\Android\\ali.webm"));
//                ReadableByteChannel rbc = Channels.newChannel(url1.openStream());
//                FileOutputStream fos = new FileOutputStream("C:\\\\Naresh Data\\\\Development Software\\\\Videos\\\\Android\\information.webm");
//                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
//                System.out.println("Map value:"+sNextVideoURL.get(0));
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
