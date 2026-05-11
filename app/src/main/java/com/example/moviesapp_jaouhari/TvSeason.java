package com.example.moviesapp_jaouhari;

public class TvSeason {
    private final int seasonNumber;
    private final int episodeCount;
    private final String name;
    private final String posterPath;
    private final String airDate;

    public TvSeason(int seasonNumber, int episodeCount, String name, String posterPath, String airDate) {
        this.seasonNumber = seasonNumber;
        this.episodeCount = episodeCount;
        this.name = name;
        this.posterPath = posterPath;
        this.airDate = airDate;
    }

    public int getSeasonNumber() { return seasonNumber; }
    public int getEpisodeCount() { return episodeCount; }
    public String getName() { return name; }
    public String getPosterPath() { return posterPath; }
    public String getAirDate() { return airDate; }
}
