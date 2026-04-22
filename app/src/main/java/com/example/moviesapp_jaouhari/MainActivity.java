package com.example.moviesapp_jaouhari;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TMDB_API_KEY = "ce601ad5b7a468bf65223a10b811735b";
    private static final String BASE_URL = "https://api.themoviedb.org/3/discover/movie";
    private static final String GENRE_URL = "https://api.themoviedb.org/3/genre/movie/list";
    private static final String TAG = "MainActivity";
    
    private RecyclerView recyclerView;
    private RecyclerView carouselRecyclerView;
    private RecyclerView actionRecyclerView;
    private RecyclerView topRatedRecyclerView;
    
    private MyMovieAdapter myMovieAdapter;
    private CarouselAdapter carouselAdapter;
    private ActionAdapter actionAdapter;
    private ActionAdapter topRatedAdapter;
    
    private EditText searchEditText;
    private ImageView searchIcon;
    private Spinner genreSpinner;
    private RequestQueue queue;
    
    private int currentPage = 1;
    private boolean isLoading = false;
    private List<Genre> genreList = new ArrayList<>();
    private int selectedGenreId = -1;

    private LinearLayout carouselSection, top10Section, actionSection;
    private TextView popularTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchEditText = findViewById(R.id.editTextSearch);
        searchIcon = findViewById(R.id.imageSearchIcon);
        genreSpinner = findViewById(R.id.genreSpinner);
        
        carouselSection = findViewById(R.id.carouselSection);
        top10Section = findViewById(R.id.top10Section);
        actionSection = findViewById(R.id.actionSection);
        popularTitle = findViewById(R.id.popularTitle);
        
        // Setup Carousel
        carouselRecyclerView = findViewById(R.id.carouselRecyclerView);
        carouselRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        new PagerSnapHelper().attachToRecyclerView(carouselRecyclerView);
        
        // Setup Top Rated List
        topRatedRecyclerView = findViewById(R.id.topRatedRecyclerView);
        topRatedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Setup Action List
        actionRecyclerView = findViewById(R.id.actionRecyclerView);
        actionRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        
        // Setup Vertical List
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager verticalLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(verticalLayoutManager);
        
        queue = Volley.newRequestQueue(this);

        fetchGenres();
        fetchMovies(currentPage);
        fetchTopRatedMovies();
        fetchActionMovies();

        setupCarouselEffect();

        searchIcon.setOnClickListener(v -> {
            if (searchEditText.getVisibility() == View.GONE) {
                searchEditText.setVisibility(View.VISIBLE);
                searchEditText.requestFocus();
            } else {
                searchEditText.setVisibility(View.GONE);
                searchEditText.setText("");
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!isLoading) {
                    if (verticalLayoutManager.findLastVisibleItemPosition() >= verticalLayoutManager.getItemCount() - 5) {
                        currentPage++;
                        fetchMovies(currentPage);
                        isLoading = true;
                    }
                }
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (myMovieAdapter != null) {
                    myMovieAdapter.getFilter().filter(s);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        genreSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Genre selectedGenre = (Genre) parent.getItemAtPosition(position);
                if (selectedGenre.getId() != -1) {
                    selectedGenreId = selectedGenre.getId();
                    toggleFilterMode(true);
                    currentPage = 1;
                    myMovieAdapter = null;
                    fetchMovies(currentPage);
                } else {
                    selectedGenreId = -1;
                    toggleFilterMode(false);
                    currentPage = 1;
                    myMovieAdapter = null;
                    fetchMovies(currentPage);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void toggleFilterMode(boolean isFilterMode) {
        int visibility = isFilterMode ? View.GONE : View.VISIBLE;
        carouselSection.setVisibility(visibility);
        top10Section.setVisibility(visibility);
        actionSection.setVisibility(visibility);
        
        if (isFilterMode) {
            Genre selected = (Genre) genreSpinner.getSelectedItem();
            popularTitle.setText("Résultats pour : " + selected.getName());
        } else {
            popularTitle.setText("Populaires");
        }
    }

    private void fetchGenres() {
        String url = GENRE_URL + "?api_key=" + TMDB_API_KEY;
        JsonObjectRequest genreRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray genres = response.getJSONArray("genres");
                        genreList.clear();
                        genreList.add(new Genre(-1, "Catégories"));
                        for (int i = 0; i < genres.length(); i++) {
                            JSONObject genreObj = genres.getJSONObject(i);
                            genreList.add(new Genre(genreObj.getInt("id"), genreObj.getString("name")));
                        }
                        ArrayAdapter<Genre> adapter = new ArrayAdapter<Genre>(MainActivity.this, 
                                R.layout.spinner_item, genreList) {
                            @NonNull
                            @Override
                            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                                View v = super.getView(position, convertView, parent);
                                ((TextView) v).setTextColor(Color.WHITE);
                                return v;
                            }
                        };
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        genreSpinner.setAdapter(adapter);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Log.e(TAG, "Genre Error: " + error.getMessage()));
        queue.add(genreRequest);
    }

    private void setupCarouselEffect() {
        carouselRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int centerX = recyclerView.getWidth() / 2;
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    int childCenterX = (child.getLeft() + child.getRight()) / 2;
                    int diff = centerX - childCenterX;
                    int distanceFromCenter = Math.abs(diff);
                    float scale = 1.0f - (distanceFromCenter / (float) recyclerView.getWidth()) * 0.4f;
                    child.setScaleX(scale);
                    child.setScaleY(scale);
                    float translationX = diff * 0.4f; 
                    child.setTranslationX(translationX);
                    float alpha = 0.4f + (scale - 0.6f) / 0.4f * 0.6f;
                    child.setAlpha(Math.max(0.4f, alpha));
                    child.setZ(scale * 10);
                }
            }
        });
    }

    private void fetchMovies(int page) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String url = BASE_URL + "?api_key=" + TMDB_API_KEY 
                + "&page=" + page 
                + "&sort_by=primary_release_date.desc"
                + "&primary_release_date.lte=" + today
                + "&vote_count.gte=50";
        
        if (selectedGenreId != -1) {
            url += "&with_genres=" + selectedGenreId;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        List<MyMovieData> moviesList = new ArrayList<>();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject movieObject = results.getJSONObject(i);
                            int id = movieObject.getInt("id");
                            String title = movieObject.getString("title");
                            String releaseDate = movieObject.optString("release_date", "N/A");
                            String imageUrl = movieObject.optString("poster_path", "");
                            moviesList.add(new MyMovieData(id, title, releaseDate, imageUrl));
                        }

                        if (myMovieAdapter == null) {
                            myMovieAdapter = new MyMovieAdapter(moviesList.toArray(new MyMovieData[0]), MainActivity.this);
                            recyclerView.setAdapter(myMovieAdapter);
                            
                            // Load carousel only if not filtered
                            if (selectedGenreId == -1) {
                                carouselAdapter = new CarouselAdapter(moviesList, MainActivity.this);
                                carouselRecyclerView.setAdapter(carouselAdapter);
                                carouselRecyclerView.post(() -> carouselRecyclerView.scrollBy(1, 0));
                            }
                        } else {
                            myMovieAdapter.addMovies(moviesList);
                        }
                        isLoading = false;
                    } catch (JSONException e) {
                        e.printStackTrace();
                        isLoading = false;
                    }
                }, error -> {
            Log.e(TAG, "Error: " + error.getMessage());
            isLoading = false;
        });
        queue.add(jsonObjectRequest);
    }

    private void fetchActionMovies() {
        String url = BASE_URL + "?api_key=" + TMDB_API_KEY + "&with_genres=28&sort_by=popularity.desc";

        JsonObjectRequest actionRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        List<MyMovieData> actionMovies = new ArrayList<>();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject movieObject = results.getJSONObject(i);
                            int id = movieObject.getInt("id");
                            String title = movieObject.getString("title");
                            String releaseDate = movieObject.optString("release_date", "N/A");
                            String imageUrl = movieObject.optString("poster_path", "");
                            actionMovies.add(new MyMovieData(id, title, releaseDate, imageUrl));
                        }
                        actionAdapter = new ActionAdapter(actionMovies, MainActivity.this);
                        actionRecyclerView.setAdapter(actionAdapter);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Log.e(TAG, "Action Error: " + error.getMessage()));
        queue.add(actionRequest);
    }

    private void fetchTopRatedMovies() {
        String url = BASE_URL + "?api_key=" + TMDB_API_KEY 
                + "&primary_release_year=2026"
                + "&sort_by=vote_average.desc"
                + "&vote_count.gte=10";

        JsonObjectRequest topRatedRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        List<MyMovieData> topMovies = new ArrayList<>();
                        int limit = Math.min(results.length(), 10);
                        for (int i = 0; i < limit; i++) {
                            JSONObject movieObject = results.getJSONObject(i);
                            int id = movieObject.getInt("id");
                            String title = movieObject.getString("title");
                            String releaseDate = movieObject.optString("release_date", "N/A");
                            String imageUrl = movieObject.optString("poster_path", "");
                            topMovies.add(new MyMovieData(id, title, releaseDate, imageUrl));
                        }
                        topRatedAdapter = new ActionAdapter(topMovies, MainActivity.this);
                        topRatedRecyclerView.setAdapter(topRatedAdapter);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Log.e(TAG, "Top Rated Error: " + error.getMessage()));
        queue.add(topRatedRequest);
    }
}