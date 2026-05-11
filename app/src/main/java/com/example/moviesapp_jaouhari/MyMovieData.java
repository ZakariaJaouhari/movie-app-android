package com.example.moviesapp_jaouhari;

import java.util.List;

public class MyMovieData {
    private String movieName;
    private String movieDate;
    private String movieImage;
    private String movieDescription;
    private int movieId;
    private List<Integer> genreIds;
    private String genreNames;
    private String backdropPath;

    public MyMovieData(int movieId, String movieName, String movieDate, String movieImage) {
        this.movieName = movieName;
        this.movieDate = movieDate;
        this.movieImage = movieImage;
        this.movieId = movieId;
    }

    public MyMovieData(int movieId, String movieName, String movieDate, String movieImage, List<Integer> genreIds) {
        this.movieName = movieName;
        this.movieDate = movieDate;
        this.movieImage = movieImage;
        this.movieId = movieId;
        this.genreIds = genreIds;
    }

    public int getMovieId() {
        return movieId;
    }

    public String getMovieName() {
        return movieName;
    }

    public String getMovieDate() {
        return movieDate;
    }

    public String getMovieImage() {
        return movieImage;
    }

    public String getMovieDescription() {
        return movieDescription;
    }

    public List<Integer> getGenreIds() {
        return genreIds;
    }

    public void setGenreIds(List<Integer> genreIds) {
        this.genreIds = genreIds;
    }

    public String getGenreNames() {
        return genreNames;
    }

    public void setGenreNames(String genreNames) {
        this.genreNames = genreNames;
    }

    public String getBackdropPath() {
        return backdropPath != null ? backdropPath : "";
    }

    public void setBackdropPath(String backdropPath) {
        this.backdropPath = backdropPath;
    }
}