package com.example.movieapp;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TmdbApi {

    @GET("movie/popular")
    Call<MovieResponse> getPopularMovies();

    @GET("movie/popular")
    Call<MovieResponse> getPopularMovies(@Query("page") int page);

    @GET("trending/movie/week")
    Call<MovieResponse> getTrendingMovies();

    @GET("trending/movie/week")
    Call<MovieResponse> getTrendingMovies(@Query("page") int page);

    @GET("movie/top_rated")
    Call<MovieResponse> getTopRatedMovies();

    @GET("movie/top_rated")
    Call<MovieResponse> getTopRatedMovies(@Query("page") int page);

    @GET("discover/movie")
    Call<MovieResponse> discoverMovies(
            @Query("certification_country") String certificationCountry,
            @Query("certification.lte") String certificationLte,
            @Query("include_adult") boolean includeAdult,
            @Query("page") int page,
            @Query("sort_by") String sortBy,
            @Query("with_genres") String withGenres
    );

    @GET("search/movie")
    Call<MovieResponse> searchMovies(
            @Query("query") String query,
            @Query("include_adult") boolean includeAdult
    );

    @GET("search/multi")
    Call<SearchResponse> searchMulti(
            @Query("query") String query,
            @Query("include_adult") boolean includeAdult
    );

    @GET("movie/{id}")
    Call<MediaDetails> getMovieDetails(@Path("id") int id);

    @GET("tv/{id}")
    Call<MediaDetails> getTvDetails(@Path("id") int id);
}
