package com.example.movieapp;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class SearchResponse {

    @SerializedName("results")
    private List<SearchItem> results;

    public List<SearchItem> getResults() {
        return results == null ? new ArrayList<>() : results;
    }
}
