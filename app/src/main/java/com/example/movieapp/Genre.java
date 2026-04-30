package com.example.movieapp;

import com.google.gson.annotations.SerializedName;

public class Genre {

    @SerializedName("name")
    private String name;

    public String getName() {
        return name == null ? "" : name;
    }
}
