package com.hw.movie.model;

public class MovieBean {
    private String title;
    private String url;

    public MovieBean(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public String getTitle() {
        return title == null ? "" : title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url == null ? "" : url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
