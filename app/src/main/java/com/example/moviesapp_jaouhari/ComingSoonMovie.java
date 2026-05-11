package com.example.moviesapp_jaouhari;

import java.util.List;

public class ComingSoonMovie {
    private int id;
    private String title;
    private String overview;
    private String releaseDate;
    private String backdropPath;
    private String posterPath;
    private List<Integer> genreIds;

    public ComingSoonMovie(int id, String title, String overview, String releaseDate,
                           String backdropPath, String posterPath, List<Integer> genreIds) {
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.backdropPath = backdropPath;
        this.posterPath = posterPath;
        this.genreIds = genreIds;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getOverview() { return overview; }
    public String getReleaseDate() { return releaseDate; }
    public String getBackdropPath() { return backdropPath; }
    public String getPosterPath() { return posterPath; }
    public List<Integer> getGenreIds() { return genreIds; }
}
