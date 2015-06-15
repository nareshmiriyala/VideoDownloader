package com.dellnaresh.videodownload.vhs;

import com.dellnaresh.videodownload.info.VideoInfo;
import com.dellnaresh.videodownload.info.VideoParser;
import com.dellnaresh.wget.WGet;
import com.dellnaresh.wget.info.ex.DownloadError;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeParser extends VideoParser {

    final static String UTF8 = "UTF-8";
    private static final Logger logger = LoggerFactory.getLogger(YouTubeParser.class);
    static final Map<Integer, VideoInfo.VideoQuality> itagMap = new HashMap<Integer, VideoInfo.VideoQuality>() {
        private static final long serialVersionUID = -6925194111122038477L;

        {
            put(120, VideoInfo.VideoQuality.p720); // flv
            put(102, VideoInfo.VideoQuality.p720); // webm
            put(101, VideoInfo.VideoQuality.p360); // webm
            put(100, VideoInfo.VideoQuality.p360); // webm
            put(85, VideoInfo.VideoQuality.p520); // mp4
            put(84, VideoInfo.VideoQuality.p720); // mp4
            put(83, VideoInfo.VideoQuality.p240); // mp4
            put(82, VideoInfo.VideoQuality.p360); // mp4
            put(46, VideoInfo.VideoQuality.p1080); // webm
            put(45, VideoInfo.VideoQuality.p720); // webm
            put(44, VideoInfo.VideoQuality.p480); // webm
            put(43, VideoInfo.VideoQuality.p360); // webm
            put(38, VideoInfo.VideoQuality.p3072); // mp4
            put(37, VideoInfo.VideoQuality.p1080); // mp4
            put(36, VideoInfo.VideoQuality.p240); // 3gp
            put(35, VideoInfo.VideoQuality.p480); // flv
            put(34, VideoInfo.VideoQuality.p360); // flv
            put(22, VideoInfo.VideoQuality.p720); // mp4
            put(18, VideoInfo.VideoQuality.p360); // mp4
            put(17, VideoInfo.VideoQuality.p144); // 3gp
            put(6, VideoInfo.VideoQuality.p270); // flv
            put(5, VideoInfo.VideoQuality.p240); // flv
        }
    };
    public static final String VIDEO_URL = "http://www.youtube.com/watch?v=%s";
    URL source;

    public YouTubeParser(URL input) {
        this.source = input;
    }

    public static boolean probe(URL url) {
        return url.toString().contains("youtube.com");
    }

    public static String extractId(URL url) {
        {
            logger.debug("extracting id");
            Pattern u = Pattern.compile("youtube.com/watch?.*v=([^&]*)");
            Matcher um = u.matcher(url.toString());
            if (um.find())
                return um.group(1);
        }

        {
            Pattern u = Pattern.compile("youtube.com/v/([^&]*)");
            Matcher um = u.matcher(url.toString());
            if (um.find())
                return um.group(1);
        }

        return null;
    }

    public static Map<String, String> getQueryMap(String qs) {
        try {
            qs = qs.trim();
            List<NameValuePair> list;
            list = URLEncodedUtils.parse(new URI(null, null, null, -1, null, qs, null), UTF8);
            HashMap<String, String> map = new HashMap<>();
            for (NameValuePair p : list) {
                map.put(p.getName(), p.getValue());
            }
            return map;
        } catch (URISyntaxException e) {
            throw new RuntimeException(qs, e);
        }
    }

    @Override
    public List<VideoDownload> extractLinks(final VideoInfo videoInfo, final AtomicBoolean stop, final Runnable notify) {
        logger.info("Extracting links of video ", videoInfo.getTitle());
        try {
            try {

                return extractEmbedded(videoInfo, stop, notify);
            } catch (EmbeddingDisabled e) {
                    logger.warn("Cant extractDownloadInfo any link using Embedded method ,trying extractHTML");
                List<VideoDownload> videoDownloads = extractHTML(videoInfo, stop, notify);
                if(videoDownloads.size()==0){
                    videoDownloads=streamCpature(videoInfo, stop, notify);
                }
                return videoDownloads;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * do not allow to downloadVideo age restricted videos
     *
     * @param info
     * @param stop
     * @param notify
     * @throws Exception
     */
    List<VideoDownload> streamCpature(final VideoInfo info, final AtomicBoolean stop, final Runnable notify)
            throws Exception {
        logger.info("starting streamCapture of video ",info.getTitle());
        List<VideoDownload> sNextVideoURL = new ArrayList<>();

        String html;
        html = WGet.getHtml(info.getWebUrl(), new WGet.HtmlLoader() {
            @Override
            public void notifyRetry(int delay, Throwable e) {
                info.setDelay(delay, e);
                notify.run();
            }

            @Override
            public void notifyDownloading() {
                info.setState(VideoInfo.States.DOWNLOADING);
                notify.run();
            }

            @Override
            public void notifyMoved() {
                info.setState(VideoInfo.States.RETRYING);
                notify.run();
            }
        }, stop);
        extractHtmlInfo(sNextVideoURL, info, html, stop, notify);
        extractIcon(info, html);

        return sNextVideoURL;
    }

    /**
     * Add resolution video for specific youtube link.
     *
     * @param url downloadVideo source url
     * @throws MalformedURLException
     */
    void addVideo(List<VideoDownload> sNextVideoURL, String itag, URL url) {
        Integer i = Integer.decode(itag);
        VideoInfo.VideoQuality vd = itagMap.get(i);

        sNextVideoURL.add(new VideoDownload(vd, url));
    }
    List<VideoDownload> extractHTML(final VideoInfo info, final AtomicBoolean stop, final Runnable notify)
            throws Exception {
        logger.info("starting extractHTML of video ",info.getTitle());
        List<VideoDownload> sNextVideoURL = new ArrayList<>();

        String id = extractId(source);
        if (id == null) {
            throw new RuntimeException("unknown url");
        }
        String url = String.format(VIDEO_URL, id);
        Document doc = Jsoup.connect(url).get();
        String foundMessage = null;
        String SEARCH_PATTERN = "\"url_encoded_fmt_stream_map\":(.+?),";
        String title = doc.select("title").first().html();
        info.setTitle(title);
        foundMessage = getSearchMessage(doc, SEARCH_PATTERN);
        String url_encoded_fmt_stream_map= URLDecoder.decode(foundMessage, "UTF-8");
        String[] urlStrings = url_encoded_fmt_stream_map.split("url=");
        URL urlValue = new URL(urlStrings[1]);
        logger.info("Value of url to be downloaded {}",urlStrings[1]);
        VideoInfo.VideoQuality vd = itagMap.get(101);
        sNextVideoURL.add(new VideoDownload(vd, urlValue));
//        extractUrlEncodedVideos(sNextVideoURL, url_encoded_fmt_stream_map);
        return sNextVideoURL;
    }

    private String getSearchMessage(Document doc, String SEARCH_PATTERN) {
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
                    foundMessage = m.group(1);
                    logger.debug("Found search message {}",foundMessage);
                    found = true;
                }
            }catch (Exception e){
                throw new DownloadError("Error during downloadVideo");
            }
        }
        return foundMessage;
    }

    /**
     * allows to downloadVideo age restricted videos
     *
     * @param info
     * @param stop
     * @param notify
     * @throws Exception
     */
    List<VideoDownload> extractEmbedded(final VideoInfo info, final AtomicBoolean stop, final Runnable notify)
            throws Exception {
        logger.info("starting extractEmbedded of video ",info.getTitle());
        List<VideoDownload> sNextVideoURL = new ArrayList<>();

        String id = extractId(source);
        if (id == null) {
            throw new RuntimeException("unknown url");
        }

        info.setTitle(String.format("http://www.youtube.com/watch?v=%s", id));

//        String get = String.format("http://www.youtube.com/get_video_info?authuser=0&video_id=%s&el=embedded", id);
        String get = String.format("http://www.youtube.com/get_video_info?video_id=%s&el=embedded&ps=default&eurl=&gl=US&hl=en", id);

            URL url = new URL(get);

            String qs = WGet.getHtml(url, new WGet.HtmlLoader() {
                @Override
                public void notifyRetry(int delay, Throwable e) {
                    info.setDelay(delay, e);
                    notify.run();
                }

                @Override
                public void notifyDownloading() {
                    info.setState(VideoInfo.States.DOWNLOADING);
                    notify.run();
                }

                @Override
                public void notifyMoved() {
                    info.setState(VideoInfo.States.RETRYING);
                    notify.run();
                }
            }, stop);

        Map<String, String> map = getQueryMap(qs);

        if (map.get("status").equals("fail")) {
            String r = URLDecoder.decode(map.get("reason"), UTF8);
            if (map.get("errorcode").equals("150"))
                throw new EmbeddingDisabled("error code 150");
            if (map.get("errorcode").equals("100"))
                throw new VideoDeleted("error code 100");

            throw new DownloadError(r);
            // throw new PrivateVideoException(r);
        }

        info.setTitle(URLDecoder.decode(map.get("title"), UTF8));

        // String fmt_list = URLDecoder.decode(map.get("fmt_list"), UTF8);
        // String[] fmts = fmt_list.split(",");

        String url_encoded_fmt_stream_map = URLDecoder.decode(map.get("url_encoded_fmt_stream_map"), UTF8);

        extractUrlEncodedVideos(sNextVideoURL, url_encoded_fmt_stream_map);

        // 'iurlmaxresæ or 'iurlsd' or 'thumbnail_url'
        String icon = map.get("thumbnail_url");
        icon = URLDecoder.decode(icon, UTF8);
        info.setIcon(new URL(icon));

        return sNextVideoURL;
    }

    void extractIcon(VideoInfo info, String html) {
        try {
            Pattern title = Pattern.compile("itemprop=\"thumbnailUrl\" href=\"(.*)\"");
            Matcher titleMatch = title.matcher(html);
            if (titleMatch.find()) {
                String sline = titleMatch.group(1);
                sline = StringEscapeUtils.unescapeHtml4(sline);
                info.setIcon(new URL(sline));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void extractHtmlInfo(List<VideoDownload> sNextVideoURL, VideoInfo info, String html, AtomicBoolean stop,
                         Runnable notify) throws Exception {
        {
            Pattern age = Pattern.compile("(verify_age)");
            Matcher ageMatch = age.matcher(html);
            if (ageMatch.find())
                throw new AgeException();
        }

        {
            Pattern age = Pattern.compile("(unavailable-player)");
            Matcher ageMatch = age.matcher(html);
            if (ageMatch.find())
                throw new VideoUnavailablePlayer();
        }

        {
            Pattern urlencod = Pattern.compile("\"url_encoded_fmt_stream_map\": \"([^\"]*)\"");
            Matcher urlencodMatch = urlencod.matcher(html);
            if (urlencodMatch.find()) {
                String url_encoded_fmt_stream_map;
                url_encoded_fmt_stream_map = urlencodMatch.group(1);

                // normal embedded video, unable to grab age restricted videos
                Pattern encod = Pattern.compile("url=(.*)");
                Matcher encodMatch = encod.matcher(url_encoded_fmt_stream_map);
                if (encodMatch.find()) {
                    String sline = encodMatch.group(1);

                    extractUrlEncodedVideos(sNextVideoURL, sline);
                }

                // stream video
                Pattern encodStream = Pattern.compile("stream=(.*)");
                Matcher encodStreamMatch = encodStream.matcher(url_encoded_fmt_stream_map);
                if (encodStreamMatch.find()) {
                    String sline = encodStreamMatch.group(1);

                    String[] urlStrings = sline.split("stream=");

                    for (String urlString : urlStrings) {
                        urlString = StringEscapeUtils.unescapeJava(urlString);

                        Pattern link = Pattern.compile("(sparams.*)&itag=(\\d+)&.*&conn=rtmpe(.*),");
                        Matcher linkMatch = link.matcher(urlString);
                        if (linkMatch.find()) {

                            String sparams = linkMatch.group(1);
                            String itag = linkMatch.group(2);
                            String url = linkMatch.group(3);

                            url = "http" + url + "?" + sparams;

                            url = URLDecoder.decode(url, UTF8);

                            addVideo(sNextVideoURL, itag, new URL(url));
                        }
                    }
                }
            }
        }

        {
            Pattern title = Pattern.compile("<meta name=\"title\" content=(.*)");
            Matcher titleMatch = title.matcher(html);
            if (titleMatch.find()) {
                String sline = titleMatch.group(1);
                String name = sline.replaceFirst("<meta name=\"title\" content=", "").trim();
                name = StringUtils.strip(name, "\">");
                name = StringEscapeUtils.unescapeHtml4(name);
                info.setTitle(name);
            }
        }
    }

    // http://en.wikipedia.org/wiki/YouTube#Quality_and_codecs

    public void extractUrlEncodedVideos(List<VideoDownload> sNextVideoURL, String sline) throws Exception {
        logger.info("Extracting url encoded videos");
        String[] urlStrings = sline.split("url=");

        for (String urlString : urlStrings) {
            urlString = StringEscapeUtils.unescapeJava(urlString);

            String urlFull = URLDecoder.decode(urlString, UTF8);

            // universal request
            {
                String url = null;
                {
                    Pattern link = Pattern.compile("([^&,]*)[&,]");
                    Matcher linkMatch = link.matcher(urlString);
                    if (linkMatch.find()) {
                        url = linkMatch.group(1);
                        url = URLDecoder.decode(url, UTF8);
                    }
                }

                String itag = null;
                {
                    Pattern link = Pattern.compile("itag=(\\d+)");
                    Matcher linkMatch = link.matcher(urlFull);
                    if (linkMatch.find()) {
                        itag = linkMatch.group(1);
                    }
                }

                String sig = null;

                if (sig == null) {
                    Pattern link = Pattern.compile("&signature=([^&,]*)");
                    Matcher linkMatch = link.matcher(urlFull);
                    if (linkMatch.find()) {
                        sig = linkMatch.group(1);
                    }
                }

                if (sig == null) {
                    Pattern link = Pattern.compile("sig=([^&,]*)");
                    Matcher linkMatch = link.matcher(urlFull);
                    if (linkMatch.find()) {
                        sig = linkMatch.group(1);
                    }
                }

                if (sig == null) {
                    Pattern link = Pattern.compile("[&,]s=([^&,]*)");
                    Matcher linkMatch = link.matcher(urlFull);
                    if (linkMatch.find()) {
                        sig = linkMatch.group(1);

                        DecryptSignature ss = new DecryptSignature(sig);
                        sig = ss.decrypt();
                    }
                }

                if (url != null && itag != null && sig != null) {
                    try {
                        url += "&signature=" + sig;

                        addVideo(sNextVideoURL, itag, new URL(url));
                    } catch (MalformedURLException e) {
                        // ignore bad urls
                    }
                }
            }
        }
    }

    static class DecryptSignature {
        String sig;

        public DecryptSignature(String signature) {
            this.sig = signature;
        }

        String s(int b, int e) {
            return sig.substring(b, e);
        }

        String s(int b) {
            return sig.substring(b, b + 1);
        }

        String se(int b) {
            return s(b, sig.length());
        }

        String s(int b, int e, int step) {
            String str = "";

            while (b != e) {
                str += sig.charAt(b);
                b += step;
            }
            return str;
        }

        // https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/youtube.py
        String decrypt() {
            switch (sig.length()) {
                case 93:
                    return s(86, 29, -1) + s(88) + s(28, 5, -1);
                case 92:
                    return s(25) + s(3, 25) + s(0) + s(26, 42) + s(79) + s(43, 79) + s(91) + s(80, 83);
                case 91:
                    return s(84, 27, -1) + s(86) + s(26, 5, -1);
                case 90:
                    return s(25) + s(3, 25) + s(2) + s(26, 40) + s(77) + s(41, 77) + s(89) + s(78, 81);
                case 89:
                    return s(84, 78, -1) + s(87) + s(77, 60, -1) + s(0) + s(59, 3, -1);
                case 88:
                    return s(7, 28) + s(87) + s(29, 45) + s(55) + s(46, 55) + s(2) + s(56, 87) + s(28);
                case 87:
                    return s(6, 27) + s(4) + s(28, 39) + s(27) + s(40, 59) + s(2) + se(60);
                case 86:
                    return s(80, 72, -1) + s(16) + s(71, 39, -1) + s(72) + s(38, 16, -1) + s(82) + s(15, 0, -1);
                case 85:
                    return s(3, 11) + s(0) + s(12, 55) + s(84) + s(56, 84);
                case 84:
                    return s(78, 70, -1) + s(14) + s(69, 37, -1) + s(70) + s(36, 14, -1) + s(80) + s(0, 14, -1);
                case 83:
                    return s(80, 63, -1) + s(0) + s(62, 0, -1) + s(63);
                case 82:
                    return s(80, 37, -1) + s(7) + s(36, 7, -1) + s(0) + s(6, 0, -1) + s(37);
                case 81:
                    return s(56) + s(79, 56, -1) + s(41) + s(55, 41, -1) + s(80) + s(40, 34, -1) + s(0) + s(33, 29, -1)
                            + s(34) + s(28, 9, -1) + s(29) + s(8, 0, -1) + s(9);
                case 80:
                    return s(1, 19) + s(0) + s(20, 68) + s(19) + s(69, 80);
                case 79:
                    return s(54) + s(77, 54, -1) + s(39) + s(53, 39, -1) + s(78) + s(38, 34, -1) + s(0) + s(33, 29, -1)
                            + s(34) + s(28, 9, -1) + s(29) + s(8, 0, -1) + s(9);
            }

            throw new RuntimeException("Unable to decrypt signature, key length " + sig.length()
                    + " not supported; retrying might work");
        }
    }

    public static class VideoUnavailablePlayer extends DownloadError {
        private static final long serialVersionUID = 10905065542230199L;

        public VideoUnavailablePlayer() {
            super("unavailable-player");
        }
    }

    public static class AgeException extends DownloadError {
        private static final long serialVersionUID = 1L;

        public AgeException() {
            super("Age restriction, account required");
        }
    }

    public static class PrivateVideoException extends DownloadError {
        private static final long serialVersionUID = 1L;

        public PrivateVideoException() {
            super("Private video");
        }

        public PrivateVideoException(String s) {
            super(s);
        }
    }

    public static class EmbeddingDisabled extends DownloadError {
        private static final long serialVersionUID = 1L;

        public EmbeddingDisabled(String msg) {
            super(msg);
        }
    }

    public static class VideoDeleted extends DownloadError {
        private static final long serialVersionUID = 1L;

        public VideoDeleted(String msg) {
            super(msg);
        }
    }
}
