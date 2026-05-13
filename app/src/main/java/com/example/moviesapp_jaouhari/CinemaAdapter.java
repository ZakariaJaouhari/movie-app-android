package com.example.moviesapp_jaouhari;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CinemaAdapter extends RecyclerView.Adapter<CinemaAdapter.ViewHolder> {

    public interface OnCinemaClickListener {
        void onCinemaClick(Cinema cinema);
    }

    private final List<Cinema> cinemas;
    private final Context context;
    private final OnCinemaClickListener listener;

    public CinemaAdapter(List<Cinema> cinemas, Context context, OnCinemaClickListener listener) {
        this.cinemas = cinemas;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cinema, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cinema cinema = cinemas.get(position);
        holder.name.setText(cinema.name);
        holder.address.setText(cinema.address);
        if (cinema.rating > 0) {
            holder.rating.setText("★ " + cinema.rating);
        } else {
            holder.rating.setText("");
        }
        holder.itemView.setOnClickListener(v -> listener.onCinemaClick(cinema));
    }

    @Override
    public int getItemCount() {
        return cinemas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, rating, address;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.cinemaName);
            rating = itemView.findViewById(R.id.cinemaRating);
            address = itemView.findViewById(R.id.cinemaAddress);
        }
    }
}
