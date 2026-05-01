package com.example.moviesapp_jaouhari;

import static android.content.ContentValues.TAG;
import android.content.Context;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MovieDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private SupportMapFragment mapFragment;
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
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private GoogleMap mMap;
    private List<LatLng> cinemaLocations = new ArrayList<>();

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

        requestQueue = Volley.newRequestQueue(this);

        int movieId = getIntent().getIntExtra("movieId", -1);
        if (movieId != -1) {
            fetchMovieDetails(movieId);
        } else {
            descriptionTextView.setText("No movie ID provided");
        }

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (trailerKey != null && !trailerKey.isEmpty()) {
                    String trailerUrl = "https://www.youtube.com/watch?v=" + trailerKey;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl));
                    startActivity(intent);
                } else {
                    Toast.makeText(MovieDetailActivity.this, "Bande-annonce non disponible", Toast.LENGTH_SHORT).show();
                }
            }
        });

        cinemaLocations.add(new LatLng(33.596460, -7.615480));
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void fetchMovieDetails(int movieId) {
        String TMDB_API_KEY = "ce601ad5b7a468bf65223a10b811735b";
        String movieDetailsUrl = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + TMDB_API_KEY;
        String movieVideosUrl = "https://api.themoviedb.org/3/movie/" + movieId + "/videos?api_key=" + TMDB_API_KEY;

        JsonObjectRequest movieDetailsRequest = new JsonObjectRequest(Request.Method.GET, movieDetailsUrl, null,
                response -> {
                    try {
                        String movieName = response.getString("title");
                        String movieDescription = response.getString("overview");
                        String releaseDate = response.optString("release_date", "N/A");
                        boolean isAdult = response.optBoolean("adult", false);
                        double voteAverage = response.optDouble("vote_average", 0.0);
                        int voteCount = response.optInt("vote_count", 0);
                        String imageUrl = "https://image.tmdb.org/t/p/w500" + response.getString("poster_path");

                        Name.setText(movieName);
                        descriptionTextView.setText(movieDescription);
                        releaseDateTextView.setText(releaseDate);
                        
                        // Set Rating and Vote Count
                        ratingBar.setRating((float) (voteAverage / 2.0));
                        voteCountTextView.setText(getString(R.string.vote_count_format, voteCount));
                        
                        // Rating bar color is set to white in XML
                        
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            LatLng cinemaLocation = new LatLng(33.596460, -7.615480);
            addCinemaMarker(cinemaLocation);
            moveToCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void addCinemaMarker(LatLng cinemaLocation) {
        mMap.addMarker(new MarkerOptions().position(cinemaLocation).title("Cinema").snippet("Location of the cinema"));
    }

    private void moveToCurrentLocation() {
        if (mMap == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = null;
            try {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } catch (SecurityException e) {
                e.printStackTrace();
                return;
            }
            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            moveToCurrentLocation();
        }
    }
}