package com.example.moviesapp_jaouhari;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.ViewHolder> {

    private final List<MyMovieData> movies;
    private final Context context;

    public TrendingAdapter(List<MyMovieData> movies, Context context) {
        this.movies = movies;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trending, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyMovieData movie = movies.get(position);

        holder.number.setText(String.valueOf(position + 1));

        // Stroke effect on the number (outline only, no fill)
        holder.number.getPaint().setStyle(Paint.Style.STROKE);
        holder.number.getPaint().setStrokeWidth(4f);

        Glide.with(context)
                .load("https://image.tmdb.org/t/p/w342" + movie.getMovieImage())
                .placeholder(android.R.color.darker_gray)
                .into(holder.poster);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra("movieId", movie.getMovieId());
            intent.putExtra("isTV", movie.isTV());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(movies.size(), 10);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView number;
        ImageView poster;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.trendingNumber);
            poster = itemView.findViewById(R.id.trendingPoster);
        }
    }
}
