package com.example.movieapp;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class MediaDetails {

    @SerializedName("backdrop_path")
    private String backdropPath;

    @SerializedName("first_air_date")
    private String firstAirDate;

    @SerializedName("genres")
    private List<Genre> genres;

    @SerializedName("name")
    private String name;

    @SerializedName("number_of_seasons")
    private int numberOfSeasons;

    @SerializedName("overview")
    private String overview;

    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("runtime")
    private int runtime;

    @SerializedName("title")
    private String title;

    @SerializedName("vote_average")
    private double voteAverage;

    public String getBackdropOrPosterUrl() {
        String path = backdropPath;
        if (path == null || path.isEmpty()) {
            path = posterPath;
        }
        return path == null || path.isEmpty() ? null : Config.TMDB_IMAGE_BASE_URL + path;
    }

    public String getDisplayTitle() {
        String value = title;
        if (value == null || value.trim().isEmpty()) {
            value = name;
        }
        return value == null || value.trim().isEmpty() ? "Untitled" : value.trim();
    }

    public String getDisplayYear() {
        String date = releaseDate;
        if (date == null || date.isEmpty()) {
            date = firstAirDate;
        }
        return date != null && date.length() >= 4 ? date.substring(0, 4) : "N/A";
    }

    public List<Genre> getGenres() {
        return genres == null ? new ArrayList<>() : genres;
    }

    public String getOverview() {
        return overview == null || overview.trim().isEmpty() ? "" : overview.trim();
    }

    public String getPosterOrBackdropUrl() {
        String path = posterPath;
        if (path == null || path.isEmpty()) {
            path = backdropPath;
        }
        return path == null || path.isEmpty() ? null : Config.TMDB_IMAGE_BASE_URL + path;
    }

    public int getNumberOfSeasons() {
        return numberOfSeasons;
    }

    public int getRuntime() {
        return runtime;
    }

    public double getVoteAverage() {
        return voteAverage;
    }
}
