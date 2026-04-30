package com.example.movieapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private TextView accountAgeTextView;
    private TextView accountEmailTextView;
    private TextView accountModeTextView;
    private TextView accountNameTextView;
    private TextView emailTextView;
    private FirebaseAuth firebaseAuth;
    private MovieAdapter favoritesAdapter;
    private RecyclerView favoritesRecyclerView;
    private TextView favoritesEmptyTextView;
    private TextView modeBadgeTextView;
    private TextView nameTextView;
    private MovieAdapter watchlistAdapter;
    private RecyclerView watchlistRecyclerView;
    private TextView watchlistEmptyTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        firebaseAuth = FirebaseAuth.getInstance();

        bindViews();
        setupRecyclerViews();
        setupInsets();
        setupActions();
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedMovies();
    }

    private void bindProfile(String name, String email, String ageText, String modeText) {
        nameTextView.setText(name);
        emailTextView.setText(email);
        modeBadgeTextView.setText(modeText);
        accountNameTextView.setText(name);
        accountEmailTextView.setText(email);
        accountAgeTextView.setText(ageText);
        accountModeTextView.setText(modeText);
    }

    private void bindViews() {
        nameTextView = findViewById(R.id.profile_name);
        emailTextView = findViewById(R.id.profile_email);
        modeBadgeTextView = findViewById(R.id.profile_mode_badge);
        accountNameTextView = findViewById(R.id.profile_account_name);
        accountEmailTextView = findViewById(R.id.profile_account_email);
        accountAgeTextView = findViewById(R.id.profile_account_age);
        accountModeTextView = findViewById(R.id.profile_account_mode);
        watchlistEmptyTextView = findViewById(R.id.profile_watchlist_empty);
        watchlistRecyclerView = findViewById(R.id.profile_watchlist_recycler_view);
        favoritesEmptyTextView = findViewById(R.id.profile_favorites_empty);
        favoritesRecyclerView = findViewById(R.id.profile_favorites_recycler_view);
    }

    private String getModeText(int age) {
        return age < 18 ? getString(R.string.kids_mode_label) : getString(R.string.standard_mode_label);
    }

    private void loadProfile() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            openLogin();
            return;
        }

        String fallbackEmail = currentUser.getEmail() == null ? "" : currentUser.getEmail();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> bindFirestoreProfile(documentSnapshot, fallbackEmail))
                .addOnFailureListener(exception ->
                        bindProfile(getString(R.string.profile_default_name), fallbackEmail, "-", getString(R.string.standard_mode_label))
                );
    }

    private void loadSavedMovies() {
        bindMovieList(MovieLibrary.getWatchlist(this), watchlistAdapter, watchlistRecyclerView, watchlistEmptyTextView);
        bindMovieList(MovieLibrary.getFavorites(this), favoritesAdapter, favoritesRecyclerView, favoritesEmptyTextView);
    }

    private void bindMovieList(List<Movie> movies, MovieAdapter adapter, RecyclerView recyclerView, TextView emptyTextView) {
        adapter.submitList(movies);
        boolean isEmpty = movies.isEmpty();
        emptyTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void bindFirestoreProfile(DocumentSnapshot documentSnapshot, String fallbackEmail) {
        if (!documentSnapshot.exists()) {
            bindProfile(getString(R.string.profile_default_name), fallbackEmail, "-", getString(R.string.standard_mode_label));
            return;
        }

        String name = documentSnapshot.getString("name");
        String email = documentSnapshot.getString("email");
        Integer age = readAge(documentSnapshot.get("age"));

        if (TextUtils.isEmpty(name)) {
            name = getString(R.string.profile_default_name);
        }

        if (TextUtils.isEmpty(email)) {
            email = fallbackEmail;
        }

        if (age == null) {
            bindProfile(name, email, "-", getString(R.string.standard_mode_label));
            return;
        }

        UserPreferences.saveUserProfile(this, age);
        bindProfile(name, email, String.valueOf(age), getModeText(age));
    }

    private void logout() {
        firebaseAuth.signOut();
        UserPreferences.clearUserProfile(this);
        openLogin();
    }

    private void openHome() {
        Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void openLogin() {
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openSearch() {
        startActivity(new Intent(ProfileActivity.this, SearchActivity.class));
    }

    private void openMovieDetails(Movie movie) {
        Intent intent = new Intent(ProfileActivity.this, MovieDetailsActivity.class);
        intent.putExtra("id", movie.getId());
        intent.putExtra("media_type", "movie");
        startActivity(intent);
    }

    private Integer readAge(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private void setupActions() {
        findViewById(R.id.profile_nav_home).setOnClickListener(v -> openHome());
        findViewById(R.id.profile_nav_search).setOnClickListener(v -> openSearch());
        findViewById(R.id.profile_logout_button).setOnClickListener(v -> logout());
    }

    private void setupRecyclerViews() {
        watchlistAdapter = new MovieAdapter(this::openMovieDetails);
        favoritesAdapter = new MovieAdapter(this::openMovieDetails);

        watchlistRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        watchlistRecyclerView.setAdapter(watchlistAdapter);
        favoritesRecyclerView.setAdapter(favoritesAdapter);
    }

    private void setupInsets() {
        View contentContainer = findViewById(R.id.profile_content_container);
        View bottomNavigation = findViewById(R.id.profile_bottom_navigation);

        final int contentLeft = contentContainer.getPaddingLeft();
        final int contentTop = contentContainer.getPaddingTop();
        final int contentRight = contentContainer.getPaddingRight();
        final int contentBottom = contentContainer.getPaddingBottom();
        final int navLeft = bottomNavigation.getPaddingLeft();
        final int navTop = bottomNavigation.getPaddingTop();
        final int navRight = bottomNavigation.getPaddingRight();
        final int navBottom = bottomNavigation.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_root), (v, insets) -> {
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
}
