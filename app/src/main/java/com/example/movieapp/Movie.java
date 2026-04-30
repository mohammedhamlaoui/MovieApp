package com.example.movieapp;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;

public class Movie {

    @SerializedName("backdrop_path")
    private String backdropPath;

    @SerializedName("id")
    private int id;

    @SerializedName("media_type")
    private String mediaType;

    @SerializedName("overview")
    private String overview;

    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("title")
    private String title;

    @SerializedName("vote_average")
    private double voteAverage;

    public String getBackdropUrl() {
        return backdropPath == null || backdropPath.isEmpty() ? null : Config.TMDB_IMAGE_BASE_URL + backdropPath;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    public String getDisplayTitle() {
        return title == null || title.trim().isEmpty() ? "Untitled" : title.trim();
    }

    public int getId() {
        return id;
    }

    public String getMediaType() {
        return "movie";
    }

    public String getOverview() {
        return overview == null ? "" : overview;
    }

    public String getPosterUrl() {
        return posterPath == null || posterPath.isEmpty() ? null : Config.TMDB_IMAGE_BASE_URL + posterPath;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getReleaseYear() {
        return releaseDate != null && releaseDate.length() >= 4 ? releaseDate.substring(0, 4) : "N/A";
    }

    public String getVoteAverageText() {
        return String.format(Locale.US, "%.1f", voteAverage);
    }

    public double getVoteAverage() {
        return voteAverage;
    }

    public static Movie createSaved(
            int id,
            String title,
            String posterPath,
            String backdropPath,
            String releaseDate,
            double voteAverage,
            String mediaType
    ) {
        Movie movie = new Movie();
        movie.id = id;
        movie.title = title;
        movie.posterPath = posterPath;
        movie.backdropPath = backdropPath;
        movie.releaseDate = releaseDate;
        movie.voteAverage = voteAverage;
        movie.mediaType = mediaType;
        return movie;
    }
}
