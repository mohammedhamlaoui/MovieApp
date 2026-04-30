package com.example.movieapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SeeAllActivity extends AppCompatActivity {

    public static final String EXTRA_SECTION = "section";
    public static final String EXTRA_TITLE = "title";
    public static final String SECTION_POPULAR = "popular";
    public static final String SECTION_TOP_RATED = "top_rated";
    public static final String SECTION_TRENDING = "trending";

    private static final int GRID_COLUMNS = 2;
    private static final int MAX_TMDB_PAGE = 500;
    private static final int NEXT_PAGE_THRESHOLD = 8;

    private final List<Movie> allMovies = new ArrayList<>();
    private final Set<Integer> loadedMovieIds = new HashSet<>();

    private GridLayoutManager layoutManager;
    private MovieAdapter adapter;
    private ProgressBar loadingView;
    private RecyclerView recyclerView;
    private String section;

    private boolean isLoading;
    private boolean hasMorePages = true;
    private int currentPage = 0;
    private int totalPages = MAX_TMDB_PAGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_see_all);

        bindViews();
        setupActions();
        setupPagination();
        loadNextPage();
    }

    private void bindViews() {
        TextView titleTextView = findViewById(R.id.see_all_title);
        recyclerView = findViewById(R.id.see_all_recycler_view);
        loadingView = findViewById(R.id.see_all_loading);
        section = getIntent().getStringExtra(EXTRA_SECTION);

        titleTextView.setText(getIntent().getStringExtra(EXTRA_TITLE));
        adapter = new MovieAdapter(this::openMovieDetails);
        layoutManager = new GridLayoutManager(this, GRID_COLUMNS);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    private Call<MovieResponse> createCall(String section, int page) {
        if (UserPreferences.isKidMode(this)) {
            return KidsContent.discoverKidsMovies(section, page);
        }
        if (SECTION_POPULAR.equals(section)) {
            return ApiClient.getTmdbApi().getPopularMovies(page);
        }
        if (SECTION_TOP_RATED.equals(section)) {
            return ApiClient.getTmdbApi().getTopRatedMovies(page);
        }
        return ApiClient.getTmdbApi().getTrendingMovies(page);
    }

    private void loadNextPage() {
        if (isLoading || !hasMorePages || currentPage >= MAX_TMDB_PAGE || currentPage >= totalPages) {
            return;
        }

        isLoading = true;
        loadingView.setVisibility(View.VISIBLE);
        int nextPage = currentPage + 1;

        createCall(section, nextPage).enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                isLoading = false;
                loadingView.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(SeeAllActivity.this, R.string.home_error_message, Toast.LENGTH_SHORT).show();
                    return;
                }

                currentPage = nextPage;
                totalPages = Math.min(response.body().getTotalPages(), MAX_TMDB_PAGE);
                appendUniqueMovies(response.body().getResults());
                recyclerView.setVisibility(View.VISIBLE);
                hasMorePages = currentPage < totalPages;
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                isLoading = false;
                loadingView.setVisibility(View.GONE);
                Toast.makeText(SeeAllActivity.this, R.string.home_error_message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void appendUniqueMovies(List<Movie> movies) {
        List<Movie> uniqueMovies = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie != null && loadedMovieIds.add(movie.getId())) {
                uniqueMovies.add(movie);
            }
        }

        allMovies.addAll(uniqueMovies);
        if (currentPage == 1) {
            adapter.submitList(allMovies);
        } else {
            adapter.appendList(uniqueMovies);
        }
    }

    private void openMovieDetails(Movie movie) {
        Intent intent = new Intent(SeeAllActivity.this, MovieDetailsActivity.class);
        intent.putExtra("id", movie.getId());
        intent.putExtra("media_type", "movie");
        startActivity(intent);
    }

    private void setupActions() {
        findViewById(R.id.see_all_back_button).setOnClickListener(v -> finish());
    }

    private void setupPagination() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0) {
                    return;
                }

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                boolean isNearBottom = visibleItemCount + firstVisibleItemPosition + NEXT_PAGE_THRESHOLD >= totalItemCount;
                if (isNearBottom) {
                    loadNextPage();
                }
            }
        });
    }
}
