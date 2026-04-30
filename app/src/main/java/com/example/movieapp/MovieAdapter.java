package com.example.movieapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    public interface OnMovieClickListener {
        void onMovieClick(Movie movie);
    }

    private final List<Movie> movies = new ArrayList<>();
    private final OnMovieClickListener onMovieClickListener;

    public MovieAdapter(OnMovieClickListener onMovieClickListener) {
        this.onMovieClickListener = onMovieClickListener;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movies.get(position);
        holder.titleTextView.setText(movie.getDisplayTitle());
        holder.ratingTextView.setText(movie.getVoteAverageText());
        holder.yearTextView.setText(movie.getReleaseYear());

        Glide.with(holder.posterImageView.getContext())
                .load(movie.getPosterUrl())
                .centerCrop()
                .placeholder(R.drawable.movie_poster_placeholder)
                .error(R.drawable.movie_poster_placeholder)
                .into(holder.posterImageView);

        holder.itemView.setOnClickListener(v -> onMovieClickListener.onMovieClick(movie));
        holder.moreButton.setOnClickListener(v -> showMovieActions(holder.moreButton, movie));
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    public void submitList(List<Movie> newMovies) {
        movies.clear();
        movies.addAll(newMovies);
        notifyDataSetChanged();
    }

    public void appendList(List<Movie> newMovies) {
        int startPosition = movies.size();
        movies.addAll(newMovies);
        notifyItemRangeInserted(startPosition, newMovies.size());
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final ImageButton moreButton;
        final ImageView posterImageView;
        final TextView ratingTextView;
        final TextView titleTextView;
        final TextView yearTextView;

        MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.movie_card);
            moreButton = itemView.findViewById(R.id.movie_more_button);
            posterImageView = itemView.findViewById(R.id.movie_poster);
            ratingTextView = itemView.findViewById(R.id.movie_rating);
            titleTextView = itemView.findViewById(R.id.movie_title);
            yearTextView = itemView.findViewById(R.id.movie_year);
        }
    }

    private void showMovieActions(View anchor, Movie movie) {
        PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
        popupMenu.getMenu().add("Add to Watchlist");
        popupMenu.getMenu().add("Add to Favorites");
        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Add to Watchlist".equals(title)) {
                MovieLibrary.addWatchlist(anchor.getContext(), movie);
                Toast.makeText(anchor.getContext(), "Added to watchlist", Toast.LENGTH_SHORT).show();
                return true;
            }
            MovieLibrary.addFavorite(anchor.getContext(), movie);
            Toast.makeText(anchor.getContext(), "Added to favorites", Toast.LENGTH_SHORT).show();
            return true;
        });
        popupMenu.show();
    }
}
