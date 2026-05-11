package com.example.moviesapp_jaouhari;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class GenreGridAdapter extends RecyclerView.Adapter<GenreGridAdapter.ViewHolder> {

    private final List<MyMovieData> items;
    private final Context context;
    private final boolean isTV;

    public GenreGridAdapter(List<MyMovieData> items, Context context, boolean isTV) {
        this.items = items;
        this.context = context;
        this.isTV = isTV;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_genre_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyMovieData item = items.get(position);
        if (!item.getMovieImage().isEmpty()) {
            Glide.with(context)
                    .load("https://image.tmdb.org/t/p/w185" + item.getMovieImage())
                    .centerCrop()
                    .placeholder(android.R.color.darker_gray)
                    .into(holder.image);
        } else {
            holder.image.setImageDrawable(null);
        }
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra("movieId", item.getMovieId());
            intent.putExtra("isTV", isTV);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItems(List<MyMovieData> newItems) {
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.genreItemImage);
        }
    }
}
