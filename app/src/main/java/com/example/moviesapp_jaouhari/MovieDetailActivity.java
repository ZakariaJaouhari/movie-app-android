package com.example.moviesapp_jaouhari;

import static android.content.ContentValues.TAG;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieDetailActivity extends AppCompatActivity {
    private TextView descriptionTextView;
    private TextView Name;
    private TextView releaseDateTextView;
    private TextView adultRatingTextView;
    private TextView voteCountTextView;
    private RatingBar ratingBar;
    private ImageView img;
    private String trailerKey;
    private RequestQueue requestQueue;
    private Button playButton;
    private MaterialButton addToMyListButton;
    private RecyclerView recyclerViewSimilar;
    private SimilarMoviesAdapter similarMoviesAdapter;
    private List<MyMovieData> similarMoviesList = new ArrayList<>();

    // TV seasons & episodes
    private android.widget.LinearLayout seasonsSection;
    private RecyclerView seasonsRecyclerView;
    private TextView episodesTitle;
    private TextView episodeCountBadge;
    private RecyclerView episodesRecyclerView;
    private SeasonAdapter seasonAdapter;
    private EpisodeAdapter episodeAdapter;
    private final List<TvSeason> seasonsList = new ArrayList<>();
    private final List<TvEpisode> episodesList = new ArrayList<>();
    private int currentSeasonNumber = 1;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private int currentMovieId;
    private String currentMovieName;
    private String currentMovieDate;
    private String currentPosterPath;
    private boolean isInMyList = false;
    private boolean isTV = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        descriptionTextView = findViewById(R.id.Details);
        img = findViewById(R.id.imageview);
        Name = findViewById(R.id.textName);
        releaseDateTextView = findViewById(R.id.releaseDate);
        adultRatingTextView = findViewById(R.id.adultRating);
        voteCountTextView = findViewById(R.id.voteCount);
        ratingBar = findViewById(R.id.movieRating);
        playButton = findViewById(R.id.playButton);
        addToMyListButton = findViewById(R.id.addToMyListButton);
        recyclerViewSimilar = findViewById(R.id.recyclerViewSimilarMovies);

        recyclerViewSimilar.setLayoutManager(new GridLayoutManager(this, 3));
        similarMoviesAdapter = new SimilarMoviesAdapter(similarMoviesList, this);
        recyclerViewSimilar.setAdapter(similarMoviesAdapter);

        // TV seasons & episodes
        seasonsSection = findViewById(R.id.seasonsSection);
        seasonsRecyclerView = findViewById(R.id.seasonsRecyclerView);
        episodesTitle = findViewById(R.id.episodesTitle);
        episodeCountBadge = findViewById(R.id.episodeCountBadge);
        episodesRecyclerView = findViewById(R.id.episodesRecyclerView);

        seasonsRecyclerView.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(this,
                        androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        seasonAdapter = new SeasonAdapter(seasonsList, (season, position) -> {
            showEpisodesFor(season);
        });
        seasonsRecyclerView.setAdapter(seasonAdapter);

        episodesRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        episodeAdapter = new EpisodeAdapter(episodesList, episode -> fetchEpisodeTrailer(episode.getEpisodeNumber()));
        episodesRecyclerView.setAdapter(episodeAdapter);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        requestQueue = Volley.newRequestQueue(this);

        isTV = getIntent().getBooleanExtra("isTV", false);
        currentMovieId = getIntent().getIntExtra("movieId", -1);
        if (currentMovieId != -1) {
            fetchMovieDetails(currentMovieId);
            fetchSimilarMovies(currentMovieId);
            checkIfInMyList(currentMovieId);
        } else {
            descriptionTextView.setText("No movie ID provided");
        }

        playButton.setOnClickListener(v -> {
            if (trailerKey != null && !trailerKey.isEmpty()) {
                String trailerUrl = "https://www.youtube.com/watch?v=" + trailerKey;
                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(trailerUrl));
                startActivity(intent);
            } else {
                Toast.makeText(MovieDetailActivity.this, "Bande-annonce non disponible", Toast.LENGTH_SHORT).show();
            }
        });

        addToMyListButton.setOnClickListener(v -> toggleMyList());
    }

    private void checkIfInMyList(int movieId) {
        if (currentUser == null) return;
        db.collection("users")
                .document(currentUser.getUid())
                .collection("myList")
                .document(String.valueOf(movieId))
                .get()
                .addOnSuccessListener(document -> {
                    isInMyList = document.exists();
                    updateMyListButton();
                });
    }

    private void toggleMyList() {
        if (currentUser == null) {
            Toast.makeText(this, "Connectez-vous pour gérer votre liste", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isInMyList) {
            removeFromMyList();
        } else {
            addToMyList();
        }
    }

    private void addToMyList() {
        Map<String, Object> movieData = new HashMap<>();
        movieData.put("movieId", currentMovieId);
        movieData.put("movieName", currentMovieName);
        movieData.put("movieDate", currentMovieDate);
        movieData.put("posterPath", currentPosterPath);
        movieData.put("addedAt", com.google.firebase.Timestamp.now());

        db.collection("users")
                .document(currentUser.getUid())
                .collection("myList")
                .document(String.valueOf(currentMovieId))
                .set(movieData)
                .addOnSuccessListener(aVoid -> {
                    isInMyList = true;
                    updateMyListButton();
                    Toast.makeText(this, currentMovieName + " ajouté à Ma Liste", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeFromMyList() {
        db.collection("users")
                .document(currentUser.getUid())
                .collection("myList")
                .document(String.valueOf(currentMovieId))
                .delete()
                .addOnSuccessListener(aVoid -> {
                    isInMyList = false;
                    updateMyListButton();
                    Toast.makeText(this, currentMovieName + " retiré de Ma Liste", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateMyListButton() {
        if (isInMyList) {
            addToMyListButton.setText(getString(R.string.remove_from_my_list));
            addToMyListButton.setStrokeColorResource(R.color.netflix_red);
            addToMyListButton.setTextColor(getResources().getColor(R.color.netflix_red, null));
            addToMyListButton.setIconTintResource(R.color.netflix_red);
        } else {
            addToMyListButton.setText(getString(R.string.add_to_my_list));
            addToMyListButton.setStrokeColorResource(android.R.color.white);
            addToMyListButton.setTextColor(getResources().getColor(android.R.color.white, null));
            addToMyListButton.setIconTintResource(android.R.color.white);
        }
    }

    private void fetchMovieDetails(int movieId) {
        String TMDB_API_KEY = "ce601ad5b7a468bf65223a10b811735b";
        String mediaType = isTV ? "tv" : "movie";
        String movieDetailsUrl = "https://api.themoviedb.org/3/" + mediaType + "/" + movieId + "?api_key=" + TMDB_API_KEY + "&language=fr-FR";
        String movieVideosUrl = "https://api.themoviedb.org/3/" + mediaType + "/" + movieId + "/videos?api_key=" + TMDB_API_KEY;

        JsonObjectRequest movieDetailsRequest = new JsonObjectRequest(Request.Method.GET, movieDetailsUrl, null,
                response -> {
                    currentMovieName = isTV
                            ? response.optString("name", response.optString("title", ""))
                            : response.optString("title", "");
                    String movieDescription = response.optString("overview", "");
                    currentMovieDate = isTV
                            ? response.optString("first_air_date", response.optString("release_date", "N/A"))
                            : response.optString("release_date", "N/A");
                    boolean isAdult = response.optBoolean("adult", false);
                    double voteAverage = response.optDouble("vote_average", 0.0);
                    int voteCount = response.optInt("vote_count", 0);
                    currentPosterPath = response.optString("poster_path", "");
                    String imageUrl = "https://image.tmdb.org/t/p/w500" + currentPosterPath;

                    Name.setText(currentMovieName);
                    descriptionTextView.setText(movieDescription);
                    releaseDateTextView.setText(currentMovieDate);

                    ratingBar.setRating((float) (voteAverage / 2.0));
                    voteCountTextView.setText(getString(R.string.vote_count_format, voteCount));

                    if (isAdult) {
                        adultRatingTextView.setText("+18");
                        adultRatingTextView.setBackgroundResource(android.R.color.holo_red_dark);
                    } else {
                        adultRatingTextView.setText("PUBLIC");
                        adultRatingTextView.setBackgroundResource(android.R.color.holo_green_dark);
                    }

                    if (!isFinishing()) {
                        Glide.with(MovieDetailActivity.this).load(imageUrl).into(img);
                    }

                    // Parse seasons for TV shows
                    if (isTV) {
                        try {
                            JSONArray seasonsArray = response.optJSONArray("seasons");
                            if (seasonsArray != null) {
                                seasonsList.clear();
                                for (int i = 0; i < seasonsArray.length(); i++) {
                                    JSONObject s = seasonsArray.getJSONObject(i);
                                    int seasonNum = s.optInt("season_number", 0);
                                    int epCount = s.optInt("episode_count", 0);
                                    if (seasonNum == 0) continue; // ignorer les épisodes spéciaux
                                    seasonsList.add(new TvSeason(
                                            seasonNum,
                                            epCount,
                                            s.optString("name", "Saison " + seasonNum),
                                            s.optString("poster_path", ""),
                                            s.optString("air_date", "")));
                                }
                                seasonAdapter.notifyDataSetChanged();
                                seasonsSection.setVisibility(android.view.View.VISIBLE);
                                if (!seasonsList.isEmpty()) {
                                    showEpisodesFor(seasonsList.get(0));
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Seasons parse error: " + e.getMessage());
                        }
                    }
                }, error -> Log.e(TAG, "Error: " + error.getMessage()));

        JsonObjectRequest movieVideosRequest = new JsonObjectRequest(Request.Method.GET, movieVideosUrl, null,
                response -> {
                    try {
                        if (response.has("results")) {
                            JSONArray results = response.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject video = results.getJSONObject(i);
                                if (video.getString("type").equals("Trailer") && video.getString("site").equals("YouTube")) {
                                    trailerKey = video.getString("key");
                                    break;
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Log.e(TAG, "Error: " + error.getMessage()));

        requestQueue.add(movieDetailsRequest);
        requestQueue.add(movieVideosRequest);
    }

    private void fetchSimilarMovies(int movieId) {
        String TMDB_API_KEY = "ce601ad5b7a468bf65223a10b811735b";
        String mediaTypeRec = isTV ? "tv" : "movie";
        String similarMoviesUrl = "https://api.themoviedb.org/3/" + mediaTypeRec + "/" + movieId + "/recommendations?api_key=" + TMDB_API_KEY + "&language=fr-FR";

        JsonObjectRequest similarMoviesRequest = new JsonObjectRequest(Request.Method.GET, similarMoviesUrl, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        similarMoviesList.clear();
                        for (int i = 0; i < Math.min(results.length(), 12); i++) {
                            JSONObject movieObj = results.getJSONObject(i);
                            int id = movieObj.getInt("id");
                            String title = isTV
                                    ? movieObj.optString("name", movieObj.optString("title", ""))
                                    : movieObj.optString("title", "");
                            String date = isTV
                                    ? movieObj.optString("first_air_date", movieObj.optString("release_date", ""))
                                    : movieObj.optString("release_date", "");
                            String posterPath = movieObj.optString("poster_path", "");
                            MyMovieData item = new MyMovieData(id, title, date, posterPath);
                            item.setTV(isTV);
                            similarMoviesList.add(item);
                        }
                        similarMoviesAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Log.e(TAG, "Error fetching similar movies: " + error.getMessage()));

        requestQueue.add(similarMoviesRequest);
    }

    // ─── Episode trailer ─────────────────────────────────────────────────────

    private void fetchEpisodeTrailer(int episodeNumber) {
        String TMDB_API_KEY = "ce601ad5b7a468bf65223a10b811735b";
        String url = "https://api.themoviedb.org/3/tv/" + currentMovieId
                + "/season/" + currentSeasonNumber
                + "/episode/" + episodeNumber
                + "/videos?api_key=" + TMDB_API_KEY + "&language=fr-FR";

        com.android.volley.toolbox.JsonObjectRequest req =
                new com.android.volley.toolbox.JsonObjectRequest(
                        com.android.volley.Request.Method.GET, url, null,
                        response -> {
                            try {
                                JSONArray results = response.getJSONArray("results");
                                String key = null;
                                for (int i = 0; i < results.length(); i++) {
                                    JSONObject v = results.getJSONObject(i);
                                    if ("YouTube".equals(v.optString("site"))
                                            && ("Trailer".equals(v.optString("type"))
                                            || "Teaser".equals(v.optString("type")))) {
                                        key = v.optString("key");
                                        break;
                                    }
                                }
                                if (key != null && !key.isEmpty()) {
                                    String ytUrl = "https://www.youtube.com/watch?v=" + key;
                                    startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(ytUrl)));
                                } else {
                                    Toast.makeText(this, "Bande-annonce non disponible", Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException ex) {
                                Log.e(TAG, "Episode trailer error: " + ex.getMessage());
                            }
                        },
                        error -> Toast.makeText(this, "Bande-annonce non disponible", Toast.LENGTH_SHORT).show());
        requestQueue.add(req);
    }

    // ─── Seasons / Episodes ──────────────────────────────────────────────────

    private void showEpisodesFor(TvSeason season) {
        currentSeasonNumber = season.getSeasonNumber();
        String label = season.getName() + " • " + season.getEpisodeCount() + " épisodes";
        episodesTitle.setText(label);
        episodesTitle.setVisibility(android.view.View.VISIBLE);
        episodesRecyclerView.setVisibility(android.view.View.VISIBLE);
        episodeCountBadge.setText(season.getEpisodeCount() + " épisodes");
        fetchEpisodes(currentMovieId, season.getSeasonNumber());
    }

    private void fetchEpisodes(int tvId, int seasonNumber) {
        String TMDB_API_KEY = "ce601ad5b7a468bf65223a10b811735b";
        String url = "https://api.themoviedb.org/3/tv/" + tvId + "/season/" + seasonNumber
                + "?api_key=" + TMDB_API_KEY + "&language=fr-FR";

        com.android.volley.toolbox.JsonObjectRequest req =
                new com.android.volley.toolbox.JsonObjectRequest(
                        com.android.volley.Request.Method.GET, url, null,
                        response -> {
                            try {
                                JSONArray eps = response.getJSONArray("episodes");
                                episodesList.clear();
                                for (int i = 0; i < eps.length(); i++) {
                                    JSONObject e = eps.getJSONObject(i);
                                    episodesList.add(new TvEpisode(
                                            e.optInt("episode_number", i + 1),
                                            e.optString("name", ""),
                                            e.optString("overview", ""),
                                            e.optString("still_path", ""),
                                            e.optInt("runtime", 0),
                                            e.optDouble("vote_average", 0.0)));
                                }
                                episodeAdapter.notifyDataSetChanged();
                            } catch (JSONException ex) {
                                Log.e(TAG, "Episodes error: " + ex.getMessage());
                            }
                        },
                        error -> Log.e(TAG, "Episodes fetch error: " + error.getMessage()));
        requestQueue.add(req);
    }

    private class SimilarMoviesAdapter extends RecyclerView.Adapter<SimilarMoviesAdapter.ViewHolder> {
        private List<MyMovieData> movies;
        private Context context;

        public SimilarMoviesAdapter(List<MyMovieData> movies, Context context) {
            this.movies = movies;
            this.context = context;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_similar_movie, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MyMovieData movie = movies.get(position);
            Glide.with(context)
                    .load("https://image.tmdb.org/t/p/w500" + movie.getMovieImage())
                    .into(holder.imageView);

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

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.similarMoviePoster);
            }
        }
    }
}
