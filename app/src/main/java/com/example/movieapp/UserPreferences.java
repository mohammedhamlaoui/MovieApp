package com.example.movieapp;

import android.content.Context;
import android.content.SharedPreferences;

public final class UserPreferences {

    public static final String KEY_IS_KID_MODE = "is_kid_mode";
    public static final String KEY_USER_AGE = "user_age";

    private static final String PREFS_NAME = "movie_app_prefs";

    private UserPreferences() {
    }

    public static void clearUserProfile(Context context) {
        getPrefs(context)
                .edit()
                .remove(KEY_USER_AGE)
                .remove(KEY_IS_KID_MODE)
                .apply();
    }

    public static boolean isKidMode(Context context) {
        return getPrefs(context).getBoolean(KEY_IS_KID_MODE, false);
    }

    public static void saveUserProfile(Context context, int age) {
        getPrefs(context)
                .edit()
                .putInt(KEY_USER_AGE, age)
                .putBoolean(KEY_IS_KID_MODE, age < 18)
                .apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
