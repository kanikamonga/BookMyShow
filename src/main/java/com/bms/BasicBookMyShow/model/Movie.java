package com.bms.BasicBookMyShow.model;

public class Movie {
    private final String id;

    private String title;

    private String language;

    private String genre;

    private String releaseDate;

    private int duration;

    public Movie(String title, String language, String genre, String releaseDate, int duration) {
        this.id = IdGenerator.generateId();
        this.title = title;
        this.language = language;
        this.genre = genre;
        this.releaseDate = releaseDate;
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
