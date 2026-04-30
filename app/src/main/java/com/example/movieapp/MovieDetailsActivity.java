package com.example.movieapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailsActivity extends AppCompatActivity {

    private static final String STREAM_MOVIE_BASE_URL = "https://vidsrc.to/embed/movie/";

    private ImageView backdropImageView;
    private View loadingStateView;
    private ImageView posterImageView;
    private ProgressBar loadingIndicator;
    private ScrollView detailsScrollView;
    private TextView errorTextView;
    private TextView genresTextView;
    private TextView metaTextView;
    private TextView overviewTextView;
    private TextView ratingTextView;
    private TextView titleTextView;
    private TextView yearTextView;
    private int currentMediaId = -1;
    private String currentTitle = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_movie_details);

        bindViews();
        setupInsets();
        setupActions();
        loadDetails();
    }

    private void bindDetails(MediaDetails details) {
        loadingStateView.setVisibility(View.GONE);
        errorTextView.setVisibility(View.GONE);
        detailsScrollView.setVisibility(View.VISIBLE);

        currentTitle = details.getDisplayTitle();
        titleTextView.setText(currentTitle);
        yearTextView.setText(details.getDisplayYear());
        ratingTextView.setText(String.format(Locale.US, "%.1f", details.getVoteAverage()));
        metaTextView.setText(buildMetaText(details));
        genresTextView.setText(buildGenresText(details.getGenres()));

        String overview = details.getOverview();
        overviewTextView.setText(overview.isEmpty() ? getString(R.string.details_no_description) : overview);

        Glide.with(this)
                .load(details.getPosterOrBackdropUrl())
                .placeholder(R.drawable.movie_poster_placeholder)
                .error(R.drawable.movie_poster_placeholder)
                .centerCrop()
                .into(posterImageView);
    }

    private void bindViews() {
        backdropImageView = findViewById(R.id.details_backdrop);
        loadingStateView = findViewById(R.id.details_loading_state);
        posterImageView = findViewById(R.id.details_poster);
        loadingIndicator = findViewById(R.id.details_loading_indicator);
        detailsScrollView = findViewById(R.id.details_scroll_view);
        errorTextView = findViewById(R.id.details_error_text);
        titleTextView = findViewById(R.id.details_title);
        yearTextView = findViewById(R.id.details_year);
        ratingTextView = findViewById(R.id.details_rating);
        metaTextView = findViewById(R.id.details_meta);
        genresTextView = findViewById(R.id.details_genres);
        overviewTextView = findViewById(R.id.details_overview);
    }

    private String buildGenresText(List<Genre> genres) {
        List<String> names = new ArrayList<>();
        for (Genre genre : genres) {
            if (genre != null && !genre.getName().isEmpty()) {
                names.add(genre.getName());
            }
        }
        return names.isEmpty() ? getString(R.string.details_genres_placeholder) : TextUtils.join(" • ", names);
    }

    private String buildMetaText(MediaDetails details) {
        if (details.getRuntime() > 0) {
            return getString(R.string.details_runtime_format, details.getRuntime());
        }
        return getString(R.string.details_runtime_placeholder);
    }

    private void loadDetails() {
        int mediaId = getIntent().getIntExtra("id", -1);
        currentMediaId = mediaId;

        if (mediaId <= 0) {
            showError();
            return;
        }

        showLoading();

        Call<MediaDetails> call = ApiClient.getTmdbApi().getMovieDetails(mediaId);

        call.enqueue(new Callback<MediaDetails>() {
            @Override
            public void onResponse(@NonNull Call<MediaDetails> call, @NonNull Response<MediaDetails> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showError();
                    return;
                }
                bindDetails(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<MediaDetails> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                showError();
            }
        });
    }

    private void setupActions() {
        findViewById(R.id.details_back_button).setOnClickListener(v -> finish());
        Button playNowButton = findViewById(R.id.details_play_button);
        playNowButton.setOnClickListener(v -> openPlayer());
    }

    private void openPlayer() {
        if (currentMediaId <= 0) {
            Toast.makeText(this, "Video not available yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String streamUrl = STREAM_MOVIE_BASE_URL + currentMediaId;

        Intent intent = new Intent(MovieDetailsActivity.this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_VIDEO_URL, streamUrl);
        intent.putExtra(PlayerActivity.EXTRA_BUNNY_VIDEO_ID, String.valueOf(currentMediaId));
        intent.putExtra(PlayerActivity.EXTRA_TITLE, currentTitle);
        startActivity(intent);
    }

    private void setupInsets() {
        View contentContainer = findViewById(R.id.details_content_container);
        final int contentLeft = contentContainer.getPaddingLeft();
        final int contentTop = contentContainer.getPaddingTop();
        final int contentRight = contentContainer.getPaddingRight();
        final int contentBottom = contentContainer.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.details_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            contentContainer.setPadding(
                    contentLeft + systemBars.left,
                    contentTop + systemBars.top,
                    contentRight + systemBars.right,
                    contentBottom + systemBars.bottom
            );
            return insets;
        });
    }

    private void showError() {
        loadingStateView.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.GONE);
        detailsScrollView.setVisibility(View.GONE);
        errorTextView.setVisibility(View.VISIBLE);
    }

    private void showLoading() {
        loadingStateView.setVisibility(View.VISIBLE);
        loadingIndicator.setVisibility(View.VISIBLE);
        detailsScrollView.setVisibility(View.GONE);
        errorTextView.setVisibility(View.GONE);
    }
}
