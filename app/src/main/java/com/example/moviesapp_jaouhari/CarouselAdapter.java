package com.example.moviesapp_jaouhari;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.ViewHolder> {

    private static final Map<Integer, String> GENRE_MAP = new HashMap<>();

    static {
        GENRE_MAP.put(28, "Action");
        GENRE_MAP.put(12, "Aventure");
        GENRE_MAP.put(16, "Animation");
        GENRE_MAP.put(35, "Comédie");
        GENRE_MAP.put(80, "Crime");
        GENRE_MAP.put(18, "Drame");
        GENRE_MAP.put(10751, "Famille");
        GENRE_MAP.put(14, "Fantastique");
        GENRE_MAP.put(27, "Horreur");
        GENRE_MAP.put(10749, "Romance");
        GENRE_MAP.put(878, "Sci-Fi");
        GENRE_MAP.put(53, "Thriller");
        GENRE_MAP.put(37, "Western");
    }

    private final List<MyMovieData> movieDataList;
    private final Context context;
    private OnAddToListClickListener addToListListener;

    public interface OnAddToListClickListener {
        void onAddToList(MyMovieData movie);
    }

    public CarouselAdapter(List<MyMovieData> movieDataList, Context context) {
        this.movieDataList = movieDataList;
        this.context = context;
    }

    public void setAddToListListener(OnAddToListClickListener listener) {
        this.addToListListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.carousel_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyMovieData movie = movieDataList.get(position);

        // Load backdrop image (w780), fallback to poster
        String imageUrl;
        if (!movie.getBackdropPath().isEmpty()) {
            imageUrl = "https://image.tmdb.org/t/p/w780" + movie.getBackdropPath();
        } else {
            imageUrl = "https://image.tmdb.org/t/p/w500" + movie.getMovieImage();
        }
        Glide.with(context).load(imageUrl).centerCrop().into(holder.heroBackdrop);

        holder.heroTitle.setText(movie.getMovieName());

        // Genre tags
        holder.heroGenreTags.removeAllViews();
        if (movie.getGenreIds() != null) {
            int shown = 0;
            for (Integer genreId : movie.getGenreIds()) {
                if (shown >= 3) break;
                String name = GENRE_MAP.get(genreId);
                if (name == null) continue;
                if (shown > 0) {
                    TextView dot = new TextView(context);
                    dot.setText(" · ");
                    dot.setTextColor(Color.parseColor("#CCCCCC"));
                    dot.setTextSize(12f);
                    holder.heroGenreTags.addView(dot);
                }
                TextView tag = new TextView(context);
                tag.setText(name);
                tag.setTextColor(Color.parseColor("#CCCCCC"));
                tag.setTextSize(12f);
                holder.heroGenreTags.addView(tag);
                shown++;
            }
        }

        // Play button → open detail
        holder.heroPlayBtn.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra("movieId", movie.getMovieId());
            context.startActivity(intent);
        });

        // My List button
        holder.heroMyListBtn.setOnClickListener(v -> {
            if (addToListListener != null) {
                addToListListener.onAddToList(movie);
            }
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(movieDataList.size(), 8);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView heroBackdrop;
        TextView heroTitle;
        LinearLayout heroGenreTags;
        MaterialButton heroPlayBtn;
        MaterialButton heroMyListBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            heroBackdrop = itemView.findViewById(R.id.heroBackdrop);
            heroTitle = itemView.findViewById(R.id.heroTitle);
            heroGenreTags = itemView.findViewById(R.id.heroGenreTags);
            heroPlayBtn = itemView.findViewById(R.id.heroPlayBtn);
            heroMyListBtn = itemView.findViewById(R.id.heroMyListBtn);
        }
    }
}
