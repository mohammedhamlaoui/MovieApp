package com.example.movieapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private MovieAdapter popularAdapter;
    private MovieAdapter topRatedAdapter;
    private MovieAdapter trendingAdapter;

    private ImageView featuredBackdropImageView;
    private LinearLayout errorStateLayout;
    private LinearLayout featuredMetaContainer;
    private LinearLayout loadingStateLayout;
    private ProgressBar loadingProgressBar;
    private ScrollView homeScrollView;
    private TextView errorMessageTextView;
    private TextView featuredOverviewTextView;
    private TextView featuredRatingTextView;
    private TextView featuredTitleTextView;
    private TextView featuredYearTextView;

    private int featuredPriority = Integer.MAX_VALUE;
    private int pendingRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        bindViews();
        setupInsets();
        setupRecyclerViews();
        setupActions();
        loadHomeContent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        if (UserPreferences.isKidMode(this)) {
            Toast.makeText(this, R.string.kids_mode_enabled, Toast.LENGTH_SHORT).show();
        }
    }

    private void bindViews() {
        homeScrollView = findViewById(R.id.home_scroll_view);
        loadingStateLayout = findViewById(R.id.home_loading_state);
        loadingProgressBar = findViewById(R.id.home_loading_indicator);
        errorStateLayout = findViewById(R.id.home_error_state);
        errorMessageTextView = findViewById(R.id.home_error_message);
        featuredBackdropImageView = findViewById(R.id.featured_backdrop);
        featuredTitleTextView = findViewById(R.id.featured_title);
        featuredOverviewTextView = findViewById(R.id.featured_overview);
        featuredRatingTextView = findViewById(R.id.featured_rating);
        featuredYearTextView = findViewById(R.id.featured_year);
        featuredMetaContainer = findViewById(R.id.featured_meta_container);
    }

    private void completeRequest() {
        pendingRequests--;
        if (pendingRequests <= 0) {
            loadingStateLayout.setVisibility(View.GONE);
            if (!hasAnyContent()) {
                errorMessageTextView.setText(R.string.home_error_message);
                errorStateLayout.setVisibility(View.VISIBLE);
                homeScrollView.setVisibility(View.GONE);
            }
        }
    }

    private void fetchPopularMovies() {
        Call<MovieResponse> call = UserPreferences.isKidMode(this)
                ? KidsContent.discoverKidsMovies(SeeAllActivity.SECTION_POPULAR, 1)
                : ApiClient.getTmdbApi().getPopularMovies();
        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                handleMovieResponse(response, popularAdapter, 2);
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                completeRequest();
            }
        });
    }

    private void fetchTopRatedMovies() {
        Call<MovieResponse> call = UserPreferences.isKidMode(this)
                ? KidsContent.discoverKidsMovies(SeeAllActivity.SECTION_TOP_RATED, 1)
                : ApiClient.getTmdbApi().getTopRatedMovies();
        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                handleMovieResponse(response, topRatedAdapter, 3);
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                completeRequest();
            }
        });
    }

    private void fetchTrendingMovies() {
        Call<MovieResponse> call = UserPreferences.isKidMode(this)
                ? KidsContent.discoverKidsMovies(SeeAllActivity.SECTION_TRENDING, 1)
                : ApiClient.getTmdbApi().getTrendingMovies();
        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                handleMovieResponse(response, trendingAdapter, 1);
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                completeRequest();
            }
        });
    }

    private void handleMovieResponse(Response<MovieResponse> response, MovieAdapter adapter, int priority) {
        if (response.isSuccessful() && response.body() != null) {
            List<Movie> movies = response.body().getResults();
            if (!movies.isEmpty()) {
                adapter.submitList(movies);
                updateFeaturedMovie(movies.get(0), priority);
                homeScrollView.setVisibility(View.VISIBLE);
                errorStateLayout.setVisibility(View.GONE);
                loadingStateLayout.setVisibility(View.GONE);
            }
        }

        completeRequest();
    }

    private boolean hasAnyContent() {
        return trendingAdapter.getItemCount() > 0
                || popularAdapter.getItemCount() > 0
                || topRatedAdapter.getItemCount() > 0;
    }

    private void loadHomeContent() {
        pendingRequests = 3;
        featuredPriority = Integer.MAX_VALUE;
        loadingStateLayout.setVisibility(View.VISIBLE);
        errorStateLayout.setVisibility(View.GONE);
        homeScrollView.setVisibility(View.GONE);
        trendingAdapter.submitList(java.util.Collections.emptyList());
        popularAdapter.submitList(java.util.Collections.emptyList());
        topRatedAdapter.submitList(java.util.Collections.emptyList());
        featuredMetaContainer.setVisibility(View.GONE);
        featuredTitleTextView.setText("");
        featuredOverviewTextView.setText("");

        fetchTrendingMovies();
        fetchPopularMovies();
        fetchTopRatedMovies();
    }

    private void setupActions() {
        Button retryButton = findViewById(R.id.home_retry_button);
        View headerProfileButton = findViewById(R.id.home_header_profile_button);
        View searchNavigation = findViewById(R.id.home_nav_search);
        View profileNavigation = findViewById(R.id.home_nav_profile);
        EditText searchInput = findViewById(R.id.home_search_input);

        retryButton.setOnClickListener(v -> loadHomeContent());
        headerProfileButton.setOnClickListener(v -> openProfile());
        searchNavigation.setOnClickListener(v -> openSearch());
        profileNavigation.setOnClickListener(v -> openProfile());
        findViewById(R.id.trending_see_all).setOnClickListener(v ->
                openSeeAll(SeeAllActivity.SECTION_TRENDING, getString(R.string.trending_now))
        );
        findViewById(R.id.popular_see_all).setOnClickListener(v ->
                openSeeAll(SeeAllActivity.SECTION_POPULAR, getString(R.string.popular_movies))
        );
        findViewById(R.id.top_rated_see_all).setOnClickListener(v ->
                openSeeAll(SeeAllActivity.SECTION_TOP_RATED, getString(R.string.top_rated))
        );
        searchInput.setOnClickListener(v -> openSearch());
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                openSearch();
            }
        });
    }

    private void setupInsets() {
        View contentContainer = findViewById(R.id.home_content_container);
        View bottomNavigation = findViewById(R.id.bottom_navigation_container);

        final int contentLeft = contentContainer.getPaddingLeft();
        final int contentTop = contentContainer.getPaddingTop();
        final int contentRight = contentContainer.getPaddingRight();
        final int contentBottom = contentContainer.getPaddingBottom();
        final int navLeft = bottomNavigation.getPaddingLeft();
        final int navTop = bottomNavigation.getPaddingTop();
        final int navRight = bottomNavigation.getPaddingRight();
        final int navBottom = bottomNavigation.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            contentContainer.setPadding(
                    contentLeft + systemBars.left,
                    contentTop + systemBars.top,
                    contentRight + systemBars.right,
                    contentBottom + systemBars.bottom
            );

            bottomNavigation.setPadding(
                    navLeft + systemBars.left,
                    navTop,
                    navRight + systemBars.right,
                    navBottom + systemBars.bottom
            );

            return insets;
        });
    }

    private void setupRecyclerViews() {
        RecyclerView trendingRecyclerView = findViewById(R.id.trending_recycler_view);
        RecyclerView popularRecyclerView = findViewById(R.id.popular_recycler_view);
        RecyclerView topRatedRecyclerView = findViewById(R.id.top_rated_recycler_view);

        trendingAdapter = new MovieAdapter(this::openMovieDetails);
        popularAdapter = new MovieAdapter(this::openMovieDetails);
        topRatedAdapter = new MovieAdapter(this::openMovieDetails);

        trendingRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        popularRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        topRatedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        trendingRecyclerView.setAdapter(trendingAdapter);
        popularRecyclerView.setAdapter(popularAdapter);
        topRatedRecyclerView.setAdapter(topRatedAdapter);
    }

    private void updateFeaturedMovie(Movie movie, int priority) {
        if (movie == null || priority > featuredPriority) {
            return;
        }

        featuredPriority = priority;
        featuredTitleTextView.setText(movie.getDisplayTitle());
        featuredOverviewTextView.setText(movie.getOverview());
        featuredRatingTextView.setText(movie.getVoteAverageText());
        featuredYearTextView.setText(movie.getReleaseYear());
        featuredMetaContainer.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(movie.getBackdropUrl() != null ? movie.getBackdropUrl() : movie.getPosterUrl())
                .centerCrop()
                .placeholder(R.drawable.movie_poster_placeholder)
                .error(R.drawable.movie_poster_placeholder)
                .into(featuredBackdropImageView);
    }

    private void openSearch() {
        startActivity(new Intent(HomeActivity.this, SearchActivity.class));
    }

    private void openSeeAll(String section, String title) {
        Intent intent = new Intent(HomeActivity.this, SeeAllActivity.class);
        intent.putExtra(SeeAllActivity.EXTRA_SECTION, section);
        intent.putExtra(SeeAllActivity.EXTRA_TITLE, title);
        startActivity(intent);
    }

    private void openProfile() {
        startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
    }

    private void openMovieDetails(Movie movie) {
        Intent intent = new Intent(HomeActivity.this, MovieDetailsActivity.class);
        intent.putExtra("id", movie.getId());
        intent.putExtra("media_type", "movie");
        startActivity(intent);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
