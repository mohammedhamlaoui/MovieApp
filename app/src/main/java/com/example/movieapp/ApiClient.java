package com.example.movieapp;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static Retrofit retrofit;

    private ApiClient() {
    }

    public static TmdbApi getTmdbApi() {
        if (retrofit == null) {
            Interceptor apiKeyInterceptor = chain -> {
                Request originalRequest = chain.request();
                HttpUrl updatedUrl = originalRequest.url()
                        .newBuilder()
                        .addQueryParameter("api_key", Config.TMDB_API_KEY)
                        .build();

                Request updatedRequest = originalRequest.newBuilder()
                        .url(updatedUrl)
                        .build();

                return chain.proceed(updatedRequest);
            };

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(apiKeyInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(Config.TMDB_BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit.create(TmdbApi.class);
    }
}
