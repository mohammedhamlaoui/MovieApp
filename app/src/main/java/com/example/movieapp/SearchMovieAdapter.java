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

import java.util.ArrayList;
import java.util.List;

public class SearchMovieAdapter extends RecyclerView.Adapter<SearchMovieAdapter.SearchMovieViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(SearchItem item);
    }

    private final List<SearchItem> items = new ArrayList<>();
    private final OnItemClickListener onItemClickListener;

    public SearchMovieAdapter(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public SearchMovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_movie, parent, false);
        return new SearchMovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchMovieViewHolder holder, int position) {
        SearchItem item = items.get(position);
        holder.titleTextView.setText(item.getDisplayTitle());
        holder.dateTextView.setText(item.getDisplayDate());
        holder.ratingTextView.setText(item.getVoteAverageText());

        Glide.with(holder.posterImageView.getContext())
                .load(item.getPosterUrl())
                .centerCrop()
                .placeholder(R.drawable.movie_poster_placeholder)
                .error(R.drawable.movie_poster_placeholder)
                .into(holder.posterImageView);

        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(item));
        holder.moreButton.setOnClickListener(v -> showMovieActions(holder.moreButton, item.toMovie()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(List<SearchItem> searchItems) {
        items.clear();
        items.addAll(searchItems);
        notifyDataSetChanged();
    }

    static class SearchMovieViewHolder extends RecyclerView.ViewHolder {
        final TextView dateTextView;
        final ImageButton moreButton;
        final ImageView posterImageView;
        final TextView ratingTextView;
        final TextView titleTextView;

        SearchMovieViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImageView = itemView.findViewById(R.id.search_movie_poster);
            moreButton = itemView.findViewById(R.id.search_movie_more_button);
            titleTextView = itemView.findViewById(R.id.search_movie_title);
            dateTextView = itemView.findViewById(R.id.search_movie_date);
            ratingTextView = itemView.findViewById(R.id.search_movie_rating);
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
