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

public class ActionAdapter extends RecyclerView.Adapter<ActionAdapter.ViewHolder> {
    private List<MyMovieData> movieDataList;
    private Context context;

    public ActionAdapter(List<MyMovieData> movieDataList, Context context) {
        this.movieDataList = movieDataList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.action_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyMovieData movieData = movieDataList.get(position);
        Glide.with(context)
                .load("https://image.tmdb.org/t/p/w500" + movieData.getMovieImage())
                .into(holder.imageView);
        
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra("movieId", movieData.getMovieId());
            intent.putExtra("isTV", movieData.isTV());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return movieDataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.actionImage);
        }
    }
}