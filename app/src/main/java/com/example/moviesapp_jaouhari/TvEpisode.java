package com.example.moviesapp_jaouhari;

public class TvEpisode {
    private final int episodeNumber;
    private final String name;
    private final String overview;
    private final String stillPath;
    private final int runtime;
    private final double voteAverage;

    public TvEpisode(int episodeNumber, String name, String overview, String stillPath, int runtime, double voteAverage) {
        this.episodeNumber = episodeNumber;
        this.name = name;
        this.overview = overview;
        this.stillPath = stillPath;
        this.runtime = runtime;
        this.voteAverage = voteAverage;
    }

    public int getEpisodeNumber() { return episodeNumber; }
    public String getName() { return name; }
    public String getOverview() { return overview; }
    public String getStillPath() { return stillPath; }
    public int getRuntime() { return runtime; }
    public double getVoteAverage() { return voteAverage; }
}
