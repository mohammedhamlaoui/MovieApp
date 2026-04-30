package com.example.movieapp;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class MovieResponse {

    @SerializedName("results")
    private List<Movie> results;

    @SerializedName("total_pages")
    private int totalPages;

    public List<Movie> getResults() {
        return results == null ? new ArrayList<>() : results;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
