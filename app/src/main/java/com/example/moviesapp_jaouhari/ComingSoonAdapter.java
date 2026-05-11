package com.example.moviesapp_jaouhari;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ComingSoonAdapter extends RecyclerView.Adapter<ComingSoonAdapter.ViewHolder> {

    private static final Map<Integer, String> GENRE_MAP = new HashMap<>();

    static {
        GENRE_MAP.put(28, "Action");
        GENRE_MAP.put(12, "Aventure");
        GENRE_MAP.put(16, "Animation");
        GENRE_MAP.put(35, "Comédie");
        GENRE_MAP.put(80, "Crime");
        GENRE_MAP.put(99, "Documentaire");
        GENRE_MAP.put(18, "Drame");
        GENRE_MAP.put(10751, "Famille");
        GENRE_MAP.put(14, "Fantastique");
        GENRE_MAP.put(36, "Histoire");
        GENRE_MAP.put(27, "Horreur");
        GENRE_MAP.put(10402, "Musique");
        GENRE_MAP.put(9648, "Mystère");
        GENRE_MAP.put(10749, "Romance");
        GENRE_MAP.put(878, "Science-Fiction");
        GENRE_MAP.put(10770, "Téléfilm");
        GENRE_MAP.put(53, "Thriller");
        GENRE_MAP.put(10752, "Guerre");
        GENRE_MAP.put(37, "Western");
    }

    private final List<ComingSoonMovie> movies;
    private final Context context;
    private final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.FRENCH);
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.FRENCH);
    private final SimpleDateFormat releaseLabelFormat = new SimpleDateFormat("d MMMM yyyy", Locale.FRENCH);

    public ComingSoonAdapter(List<ComingSoonMovie> movies, Context context) {
        this.movies = movies;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_coming_soon, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComingSoonMovie movie = movies.get(position);

        // Parse date for the left column
        try {
            Date date = inputFormat.parse(movie.getReleaseDate());
            if (date != null) {
                holder.textMonth.setText(monthFormat.format(date).toUpperCase(Locale.FRENCH));
                holder.textDay.setText(dayFormat.format(date));
                holder.textReleaseLabel.setText("Disponible le " + releaseLabelFormat.format(date));
            }
        } catch (ParseException e) {
            holder.textMonth.setText("");
            holder.textDay.setText("?");
            holder.textReleaseLabel.setText(movie.getReleaseDate());
        }

        holder.textTitle.setText(movie.getTitle());
        holder.textOverview.setText(movie.getOverview());

        // Build genre string
        StringBuilder genres = new StringBuilder();
        if (movie.getGenreIds() != null) {
            for (int i = 0; i < Math.min(movie.getGenreIds().size(), 3); i++) {
                String genre = GENRE_MAP.get(movie.getGenreIds().get(i));
                if (genre != null) {
                    if (genres.length() > 0) genres.append(" • ");
                    genres.append(genre);
                }
            }
        }
        holder.textGenres.setText(genres.toString());

        // Load backdrop image, fallback to poster
        String imageUrl;
        if (movie.getBackdropPath() != null && !movie.getBackdropPath().isEmpty()) {
            imageUrl = "https://image.tmdb.org/t/p/w780" + movie.getBackdropPath();
        } else if (movie.getPosterPath() != null && !movie.getPosterPath().isEmpty()) {
            imageUrl = "https://image.tmdb.org/t/p/w500" + movie.getPosterPath();
        } else {
            imageUrl = "";
        }

        Glide.with(context)
                .load(imageUrl)
                .placeholder(android.R.color.darker_gray)
                .into(holder.backdropImage);

        holder.btnInfo.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra("movieId", movie.getId());
            context.startActivity(intent);
        });

        holder.btnRemind.setOnClickListener(v ->
                Toast.makeText(context, "Rappel activé pour " + movie.getTitle(), Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textMonth, textDay, textTitle, textReleaseLabel, textGenres, textOverview;
        ImageView backdropImage;
        LinearLayout btnRemind, btnInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textMonth = itemView.findViewById(R.id.textMonth);
            textDay = itemView.findViewById(R.id.textDay);
            textTitle = itemView.findViewById(R.id.textTitle);
            textReleaseLabel = itemView.findViewById(R.id.textReleaseLabel);
            textGenres = itemView.findViewById(R.id.textGenres);
            textOverview = itemView.findViewById(R.id.textOverview);
            backdropImage = itemView.findViewById(R.id.backdropImage);
            btnRemind = itemView.findViewById(R.id.btnRemind);
            btnInfo = itemView.findViewById(R.id.btnInfo);
        }
    }
}
