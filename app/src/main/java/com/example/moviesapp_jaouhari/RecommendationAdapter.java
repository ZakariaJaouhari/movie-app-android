package com.example.moviesapp_jaouhari;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.ViewHolder> {

    private final List<MyMovieData> movies;
    private final List<String> explanations;
    private final Context context;

    public RecommendationAdapter(List<MyMovieData> movies, List<String> explanations, Context context) {
        this.movies = movies;
        this.explanations = explanations;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyMovieData movie = movies.get(position);

        if (!movie.getMovieImage().isEmpty()) {
            Glide.with(context)
                    .load("https://image.tmdb.org/t/p/w185" + movie.getMovieImage())
                    .centerCrop()
                    .placeholder(android.R.color.darker_gray)
                    .into(holder.poster);
        }

        if (position < explanations.size()) {
            holder.explanation.setText(explanations.get(position));
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra("movieId", movie.getMovieId());
            intent.putExtra("isTV", movie.isTV());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView poster;
        TextView explanation;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.recoPoster);
            explanation = itemView.findViewById(R.id.recoExplanation);
        }
    }
}
