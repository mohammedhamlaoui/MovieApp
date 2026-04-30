package com.example.movieapp;

import retrofit2.Call;

public final class KidsContent {

    private static final String FAMILY_GENRES = "12,16,35,10751,14";
    private static final String G_RATED = "G";
    private static final String POPULAR_SORT = "popularity.desc";
    private static final String TOP_RATED_SORT = "vote_average.desc";
    private static final String TRENDING_SORT = "primary_release_date.desc";
    private static final String US_CERTIFICATION_COUNTRY = "US";

    private KidsContent() {
    }

    public static Call<MovieResponse> discoverKidsMovies(String section, int page) {
        String sortBy = POPULAR_SORT;
        if (SeeAllActivity.SECTION_TOP_RATED.equals(section)) {
            sortBy = TOP_RATED_SORT;
        } else if (SeeAllActivity.SECTION_TRENDING.equals(section)) {
            sortBy = TRENDING_SORT;
        }

        return ApiClient.getTmdbApi().discoverMovies(
                US_CERTIFICATION_COUNTRY,
                G_RATED,
                false,
                page,
                sortBy,
                FAMILY_GENRES
        );
    }
}
