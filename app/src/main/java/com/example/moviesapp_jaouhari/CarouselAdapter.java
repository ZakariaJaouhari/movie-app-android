package com.example.moviesapp_jaouhari;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.ViewHolder> {
    private List<MyMovieData> movieDataList;
    private Context context;

    public CarouselAdapter(List<MyMovieData> movieDataList, Context context) {
        this.movieDataList = movieDataList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.carousel_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyMovieData movieData = movieDataList.get(position);
        Glide.with(context)
                .load("https://image.tmdb.org/t/p/w500" + movieData.getMovieImage())
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return Math.min(movieDataList.size(), 10); // Limit to 10 movies
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.carouselImage);
        }
    }
}