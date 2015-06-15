package com.dellnaresh.videodownload;

import com.dellnaresh.wget.info.ex.DownloadError;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by NARESHM on 15/06/2015.
 */
public class JsoupTest {
    private static final Logger logger = LoggerFactory.getLogger(JsoupTest.class);
    private static String SEARCH_PATTERN = "\"url_encoded_fmt_stream_map\":(.+?),";
    private static String URL_PATTERN = "url=(.+?),";
    public static void main(String[] args) {
        try {
            Document document= Jsoup.connect("https://www.youtube.com/watch?v=hHUbLv4ThOo").get();
//            System.out.println(document.html());
            getSearchMessage(document,SEARCH_PATTERN);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getSearchMessage(Document doc, String SEARCH_PATTERN) {
        boolean found=false;
        String foundMessage=null;
        int i=0;
        while(!found) {
            try {
                Element script = doc.select("script").get(i); // Get the script part

                i++;
                Pattern p = Pattern.compile(SEARCH_PATTERN); // Regex for the value of the key
                Matcher m = p.matcher(script.html()); // you have to use html here and NOT text! Text will drop the 'key' part
                while (m.find()) {
//                    logger.info("Found script message {}", URLDecoder.decode(script.html(), "UTF-8"));
                    foundMessage = m.group(1);
//                    logger.info("Found search message {}", foundMessage);
                    findLinks(script.html());
                    found = true;
                }
            }catch (Exception e){
                throw new DownloadError("Error during downloadVideo");
            }
        }
        return foundMessage;
    }

    private static void findLinks(String script) {
        Pattern p = Pattern.compile(URL_PATTERN); // Regex for the value of the key
        Matcher m = p.matcher(script);
        while (m.find()) {
            logger.info("Found link message {}", m.group(1));
        }
    }
}
