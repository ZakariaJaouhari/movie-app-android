package com.example.moviesapp_jaouhari;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    public interface OnEpisodeClickListener {
        void onEpisodeClick(TvEpisode episode);
    }

    private final List<TvEpisode> episodes;
    private final OnEpisodeClickListener listener;

    public EpisodeAdapter(List<TvEpisode> episodes, OnEpisodeClickListener listener) {
        this.episodes = episodes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_episode, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TvEpisode ep = episodes.get(position);

        holder.episodeNumber.setText("Ép. " + ep.getEpisodeNumber());
        holder.episodeTitle.setText(ep.getName());

        if (ep.getRuntime() > 0) {
            holder.episodeRuntime.setText(ep.getRuntime() + " min");
            holder.episodeRuntime.setVisibility(View.VISIBLE);
        } else {
            holder.episodeRuntime.setVisibility(View.GONE);
        }

        String overview = ep.getOverview();
        if (overview != null && !overview.isEmpty()) {
            holder.episodeOverview.setText(overview);
            holder.episodeOverview.setVisibility(View.VISIBLE);
        } else {
            holder.episodeOverview.setVisibility(View.GONE);
        }

        String still = ep.getStillPath();
        if (still != null && !still.isEmpty()) {
            Glide.with(holder.episodeStill.getContext())
                    .load("https://image.tmdb.org/t/p/w300" + still)
                    .centerCrop()
                    .into(holder.episodeStill);
        } else {
            holder.episodeStill.setImageDrawable(null);
            holder.episodeStill.setBackgroundColor(0xFF1E1E1E);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEpisodeClick(ep);
        });
    }

    @Override
    public int getItemCount() {
        return episodes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView episodeStill;
        TextView episodeNumber;
        TextView episodeTitle;
        TextView episodeRuntime;
        TextView episodeOverview;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            episodeStill = itemView.findViewById(R.id.episodeStill);
            episodeNumber = itemView.findViewById(R.id.episodeNumber);
            episodeTitle = itemView.findViewById(R.id.episodeTitle);
            episodeRuntime = itemView.findViewById(R.id.episodeRuntime);
            episodeOverview = itemView.findViewById(R.id.episodeOverview);
        }
    }
}
