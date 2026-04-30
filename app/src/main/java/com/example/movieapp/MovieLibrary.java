package com.example.movieapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class MovieLibrary {

    private static final String KEY_FAVORITES = "favorites";
    private static final String KEY_WATCHLIST = "watchlist";
    private static final String PREFS_NAME = "movie_library";

    private MovieLibrary() {
    }

    public static void addFavorite(Context context, Movie movie) {
        addMovie(context, KEY_FAVORITES, movie);
    }

    public static void addWatchlist(Context context, Movie movie) {
        addMovie(context, KEY_WATCHLIST, movie);
    }

    public static List<Movie> getFavorites(Context context) {
        return getMovies(context, KEY_FAVORITES);
    }

    public static List<Movie> getWatchlist(Context context) {
        return getMovies(context, KEY_WATCHLIST);
    }

    private static void addMovie(Context context, String key, Movie movie) {
        List<Movie> movies = getMovies(context, key);
        for (Movie savedMovie : movies) {
            if (savedMovie.getId() == movie.getId()) {
                return;
            }
        }
        movies.add(0, movie);
        saveMovies(context, key, movies);
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static List<Movie> getMovies(Context context, String key) {
        List<Movie> movies = new ArrayList<>();
        String rawJson = getPreferences(context).getString(key, "[]");
        try {
            JSONArray jsonArray = new JSONArray(rawJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                movies.add(Movie.createSaved(
                        object.optInt("id"),
                        object.optString("title"),
                        emptyToNull(object.optString("posterPath")),
                        emptyToNull(object.optString("backdropPath")),
                        emptyToNull(object.optString("releaseDate")),
                        object.optDouble("voteAverage"),
                        object.optString("mediaType", "movie")
                ));
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        return movies;
    }

    private static String emptyToNull(String value) {
        return TextUtils.isEmpty(value) ? null : value;
    }

    private static void saveMovies(Context context, String key, List<Movie> movies) {
        JSONArray jsonArray = new JSONArray();
        for (Movie movie : movies) {
            JSONObject object = new JSONObject();
            try {
                object.put("id", movie.getId());
                object.put("title", movie.getDisplayTitle());
                object.put("posterPath", movie.getPosterPath());
                object.put("backdropPath", movie.getBackdropPath());
                object.put("releaseDate", movie.getReleaseDate());
                object.put("voteAverage", movie.getVoteAverage());
                object.put("mediaType", movie.getMediaType());
                jsonArray.put(object);
            } catch (JSONException ignored) {
                // Skip malformed local entries so one bad item never breaks the whole list.
            }
        }
        getPreferences(context).edit().putString(key, jsonArray.toString()).apply();
    }
}
