package com.example.movieapp;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;

public class SearchItem {

    @SerializedName("id")
    private int id;

    @SerializedName("first_air_date")
    private String firstAirDate;

    @SerializedName("media_type")
    private String mediaType;

    @SerializedName("name")
    private String name;

    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("title")
    private String title;

    @SerializedName("vote_average")
    private double voteAverage;

    public static SearchItem fromMovie(Movie movie) {
        SearchItem item = new SearchItem();
        item.id = movie.getId();
        item.mediaType = "movie";
        item.posterPath = movie.getPosterPath();
        item.releaseDate = movie.getReleaseDate();
        item.title = movie.getDisplayTitle();
        item.voteAverage = movie.getVoteAverage();
        return item;
    }

    public String getDisplayDate() {
        String date = isTv() ? firstAirDate : releaseDate;
        return date == null || date.isEmpty() ? "N/A" : date;
    }

    public int getId() {
        return id;
    }

    public String getDisplayTitle() {
        String value = isTv() ? name : title;
        return value == null || value.trim().isEmpty() ? "Untitled" : value.trim();
    }

    public String getMediaType() {
        return mediaType == null ? "" : mediaType;
    }

    public String getPosterUrl() {
        return posterPath == null || posterPath.isEmpty() ? null : Config.TMDB_IMAGE_BASE_URL + posterPath;
    }

    public String getVoteAverageText() {
        return String.format(Locale.US, "%.1f", voteAverage);
    }

    public Movie toMovie() {
        String date = isTv() ? firstAirDate : releaseDate;
        return Movie.createSaved(
                id,
                getDisplayTitle(),
                posterPath,
                null,
                date,
                voteAverage,
                getMediaType()
        );
    }

    public boolean isDisplayable() {
        return "movie".equals(getMediaType());
    }

    private boolean isTv() {
        return "tv".equals(getMediaType());
    }
}
