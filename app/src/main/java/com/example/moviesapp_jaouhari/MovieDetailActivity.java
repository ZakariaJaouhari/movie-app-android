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

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private int currentMovieId;
    private String currentMovieName;
    private String currentMovieDate;
    private String currentPosterPath;
    private boolean isInMyList = false;

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

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        requestQueue = Volley.newRequestQueue(this);

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
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl));
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
        String movieDetailsUrl = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + TMDB_API_KEY;
        String movieVideosUrl = "https://api.themoviedb.org/3/movie/" + movieId + "/videos?api_key=" + TMDB_API_KEY;

        JsonObjectRequest movieDetailsRequest = new JsonObjectRequest(Request.Method.GET, movieDetailsUrl, null,
                response -> {
                    try {
                        currentMovieName = response.getString("title");
                        String movieDescription = response.getString("overview");
                        currentMovieDate = response.optString("release_date", "N/A");
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
                    } catch (JSONException e) {
                        e.printStackTrace();
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
        String similarMoviesUrl = "https://api.themoviedb.org/3/movie/" + movieId + "/similar?api_key=" + TMDB_API_KEY;

        JsonObjectRequest similarMoviesRequest = new JsonObjectRequest(Request.Method.GET, similarMoviesUrl, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        similarMoviesList.clear();
                        for (int i = 0; i < Math.min(results.length(), 12); i++) {
                            JSONObject movieObj = results.getJSONObject(i);
                            int id = movieObj.getInt("id");
                            String title = movieObj.getString("title");
                            String date = movieObj.optString("release_date", "");
                            String posterPath = movieObj.optString("poster_path", "");

                            similarMoviesList.add(new MyMovieData(id, title, date, posterPath));
                        }
                        similarMoviesAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Log.e(TAG, "Error fetching similar movies: " + error.getMessage()));

        requestQueue.add(similarMoviesRequest);
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
