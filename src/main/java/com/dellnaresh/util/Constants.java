package com.dellnaresh.util;

/**
 * Created by NARESHM on 15/06/2015.
 */
public class Constants {
    public static final String USER_AGENT = "User-Agent";
    public static final String REFERRER = "Referrer";

    public static class ERRORS {
        public static final String STOPPED = "stop";
        public static final String FATAL = "fatal";
        public static final String INTERRUPTED = "interrupted";
    }

    public static final String SMALL_FILE_URL = "https://www.youtube.com/watch?v=7M-jsjLB20Y";
    public static final String DOWNLOAD_DIRECTORY_CONFIG = "download.directory";
    public static final String YOUTUBE_URL_START = "http://www.youtube.com/watch?v=";

    /**
     * any – Do not filter video search results based on their duration. This is the default value.
     * long – Only include videos longer than 20 minutes.
     * medium – Only include videos that are between four and 20 minutes long (inclusive).
     * short – Only include videos that are less than four minutes long.
     */
    public enum VIDEO_LENGTH {
        ANY("any"), LONG("long"), MEDIUM("medium"), SHORT("short");
        private String length;

        VIDEO_LENGTH(String length) {
            this.length = length;
        }

        public String getLength() {
            return length;
        }

        public void setLength(String length) {
            this.length = length;
        }

    }

    /**
     * any – Return all videos.
     * episode – Only retrieve episodes of shows.
     * movie – Only retrieve movies.
     */
    public enum VIDEO_TYPE {
        ANY("any"), EPISODE("episode"), MOVIE("movie");
        private String videoType;

        VIDEO_TYPE(String videoType) {
            this.videoType = videoType;
        }

        public String getVideoType() {
            return videoType;
        }

        public void setVideoType(String videoType) {
            this.videoType = videoType;
        }
    }

    /**
     * any – Return all videos, regardless of their resolution.
     * high – Only retrieve HD videos.
     * standard – Only retrieve videos in standard definition.
     */
    public enum VIDEO_DEFINITION {
        ANY("any"), HIGH("high"), STANDARD("standard");
        private String value;

        VIDEO_DEFINITION(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * moderate – YouTube will filter some content from search results and, at the least, will filter content that is restricted in your locale. Based on their content, search results could be removed from search results or demoted in search results. This is the default parameter value.
     * none – YouTube will not filter the search result set.
     * strict – YouTube will try to exclude all restricted content from the search result set. Based on their content, search results could be removed from search results or demoted in search results.
     */
    public enum SAFE_SEARCH {
        MODERATE("moderate"), NONE("none"), STRICT("strict");
        private String value;

        SAFE_SEARCH(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Acceptable values are:
     * any – Return all videos, regardless of which license they have, that match the query parameters.
     * creativeCommon – Only return videos that have a Creative Commons license. Users can reuse videos with this license in other videos that they create. Learn more.
     * youtube – Only return videos that have the standard YouTube license.
     */
    public enum VIDEO_LICENCE {
        ANY("any"), CREATIVE_COMMON("creativeCommon"), YOUTUBE("youtube");
        private String value;

        VIDEO_LICENCE(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * date – Resources are sorted in reverse chronological order based on the date they were created.
     * rating – Resources are sorted from highest to lowest rating.
     * relevance – Resources are sorted based on their relevance to the search query. This is the default value for this parameter.
     * title – Resources are sorted alphabetically by title.
     * videoCount – Channels are sorted in descending order of their number of uploaded videos.
     * viewCount – Resources are sorted from highest to lowest number of views.
     */
    public enum ORDER {
        DATE("date"), RATING("rating "), RELEVANCE("relevance"), TITLE("title"), VIDEO_COUNT("videoCount"), VIEW_COUNT("viewCount");
        private String value;

        ORDER(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public enum RELEVANCE_LANGUAGE {
        EN("en"), TE("te");
        private String value;

        RELEVANCE_LANGUAGE(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
