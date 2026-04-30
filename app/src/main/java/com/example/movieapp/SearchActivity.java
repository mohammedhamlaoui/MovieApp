package com.example.movieapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private static final long SEARCH_DEBOUNCE_MS = 500L;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());

    private Call<MovieResponse> currentSearchCall;
    private EditText searchInput;
    private ProgressBar loadingIndicator;
    private RecyclerView resultsRecyclerView;
    private Runnable pendingSearchRunnable;
    private SearchMovieAdapter adapter;
    private TextView stateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);

        bindViews();
        setupInsets();
        setupRecyclerView();
        setupActions();
        showMessageState(R.string.search_prompt);
    }

    private void bindViews() {
        searchInput = findViewById(R.id.search_input);
        loadingIndicator = findViewById(R.id.search_loading_indicator);
        resultsRecyclerView = findViewById(R.id.search_results_recycler_view);
        stateTextView = findViewById(R.id.search_state_text);
    }

    private void cancelPendingSearch() {
        if (pendingSearchRunnable != null) {
            searchHandler.removeCallbacks(pendingSearchRunnable);
        }
        if (currentSearchCall != null) {
            currentSearchCall.cancel();
        }
    }

    private void openHome() {
        Intent intent = new Intent(SearchActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        cancelPendingSearch();
        super.onDestroy();
    }

    private void performSearch(String query) {
        loadingIndicator.setVisibility(View.VISIBLE);
        resultsRecyclerView.setVisibility(View.GONE);
        stateTextView.setVisibility(View.GONE);

        currentSearchCall = ApiClient.getTmdbApi().searchMovies(query, false);
        currentSearchCall.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                loadingIndicator.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    showMessageState(R.string.search_results_error);
                    return;
                }

                List<SearchItem> movieItems = toSearchItems(response.body().getResults());
                if (movieItems.isEmpty()) {
                    showMessageState(R.string.no_results_found);
                    return;
                }

                adapter.submitList(movieItems);
                resultsRecyclerView.setVisibility(View.VISIBLE);
                stateTextView.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                loadingIndicator.setVisibility(View.GONE);
                showMessageState(R.string.search_results_error);
            }
        });
    }

    private void scheduleSearch(String query) {
        cancelPendingSearch();

        if (query.trim().isEmpty()) {
            adapter.submitList(new ArrayList<>());
            loadingIndicator.setVisibility(View.GONE);
            resultsRecyclerView.setVisibility(View.GONE);
            showMessageState(R.string.search_prompt);
            return;
        }

        pendingSearchRunnable = () -> performSearch(query.trim());
        searchHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
    }

    private void setupActions() {
        findViewById(R.id.search_back_button).setOnClickListener(v -> openHome());
        findViewById(R.id.search_nav_home).setOnClickListener(v -> openHome());
        findViewById(R.id.search_nav_profile).setOnClickListener(v -> openProfile());
        findViewById(R.id.search_nav_search).setOnClickListener(v -> {
            if (searchInput != null) {
                searchInput.requestFocus();
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                scheduleSearch(editable.toString());
            }
        });
    }

    private void setupInsets() {
        View contentContainer = findViewById(R.id.search_content_container);
        View bottomNavigation = findViewById(R.id.search_bottom_navigation);

        final int contentLeft = contentContainer.getPaddingLeft();
        final int contentTop = contentContainer.getPaddingTop();
        final int contentRight = contentContainer.getPaddingRight();
        final int contentBottom = contentContainer.getPaddingBottom();
        final int navLeft = bottomNavigation.getPaddingLeft();
        final int navTop = bottomNavigation.getPaddingTop();
        final int navRight = bottomNavigation.getPaddingRight();
        final int navBottom = bottomNavigation.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.search_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            contentContainer.setPadding(
                    contentLeft + systemBars.left,
                    contentTop + systemBars.top,
                    contentRight + systemBars.right,
                    contentBottom
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

    private void setupRecyclerView() {
        adapter = new SearchMovieAdapter(this::openDetails);
        resultsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        resultsRecyclerView.setAdapter(adapter);
    }

    private void showMessageState(int messageResId) {
        stateTextView.setText(messageResId);
        stateTextView.setVisibility(View.VISIBLE);
        resultsRecyclerView.setVisibility(View.GONE);
    }

    private List<SearchItem> toSearchItems(List<Movie> movies) {
        List<SearchItem> items = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie != null) {
                items.add(SearchItem.fromMovie(movie));
            }
        }
        return items;
    }

    private void openDetails(SearchItem item) {
        Intent intent = new Intent(SearchActivity.this, MovieDetailsActivity.class);
        intent.putExtra("id", item.getId());
        intent.putExtra("media_type", "movie");
        startActivity(intent);
    }

    private void openProfile() {
        startActivity(new Intent(SearchActivity.this, ProfileActivity.class));
    }
}
