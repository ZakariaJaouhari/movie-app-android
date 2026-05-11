package com.example.moviesapp_jaouhari;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SeasonAdapter extends RecyclerView.Adapter<SeasonAdapter.ViewHolder> {

    public interface OnSeasonClickListener {
        void onSeasonClick(TvSeason season, int position);
    }

    private final List<TvSeason> seasons;
    private final OnSeasonClickListener listener;
    private int selectedPosition = 0;

    public SeasonAdapter(List<TvSeason> seasons, OnSeasonClickListener listener) {
        this.seasons = seasons;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int prev = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(prev);
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_season, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TvSeason season = seasons.get(position);
        holder.label.setText(season.getName());

        boolean selected = position == selectedPosition;
        holder.label.setTextColor(selected ? Color.WHITE : Color.parseColor("#AAAAAA"));
        holder.selectedBar.setVisibility(selected ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;
            setSelectedPosition(pos);
            listener.onSeasonClick(seasons.get(pos), pos);
        });
    }

    @Override
    public int getItemCount() {
        return seasons.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView label;
        View selectedBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.seasonLabel);
            selectedBar = itemView.findViewById(R.id.seasonSelectedBar);
        }
    }
}
