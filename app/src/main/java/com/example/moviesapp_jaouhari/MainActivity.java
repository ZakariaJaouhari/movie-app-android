package com.example.moviesapp_jaouhari;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TMDB_API_KEY = "ce601ad5b7a468bf65223a10b811735b";
    private static final String BASE_URL = "https://api.themoviedb.org/3/discover/movie";
    private static final String TAG = "MainActivity";

    // Views
    private RecyclerView recyclerView;
    private RecyclerView carouselRecyclerView;
    private RecyclerView actionRecyclerView;
    private RecyclerView topRatedRecyclerView;
    private RecyclerView trendingRecyclerView;
    private EditText searchEditText;
    private ImageView searchIcon;
    private ImageView logoutIcon;
    private Spinner genreSpinner;
    private BottomNavigationView bottomNavigationView;
    private NestedScrollView homeContentScrollView;
    private LinearLayout myListView;
    private TextView myListEmptyText;
    private RecyclerView myListRecyclerView;
    private LinearLayout comingSoonView;
    private RecyclerView comingSoonRecyclerView;
    private TextView popularTitle;
    private LinearLayout trendingSection;
    private LinearLayout top10Section;
    private LinearLayout actionSection;

    // Adapters
    private MyMovieAdapter myMovieAdapter;
    private CarouselAdapter carouselAdapter;
    private ActionAdapter actionAdapter;
    private ActionAdapter topRatedAdapter;
    private TrendingAdapter trendingAdapter;
    private MyListAdapter myListAdapter;
    private ComingSoonAdapter comingSoonAdapter;

    // Data lists
    private final List<MyMovieData> heroMovies = new ArrayList<>();
    private final List<MyMovieData> trendingMovies = new ArrayList<>();
    private final List<MyMovieData> myListMovies = new ArrayList<>();
    private final List<ComingSoonMovie> comingSoonMovies = new ArrayList<>();

    // State
    private RequestQueue queue;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private int currentPage = 1;
    private boolean isLoading = false;
    private final List<Genre> genreList = new ArrayList<>();
    private int selectedGenreId = -1;
    private boolean comingSoonLoaded = false;
    private final Handler heroAutoScrollHandler = new Handler(Looper.getMainLooper());
    private int heroCurrentPos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupAdapters();

        queue = Volley.newRequestQueue(this);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        bottomNavigationView.setItemActiveIndicatorEnabled(false);

        fetchNowPlayingMovies();
        fetchTrendingMovies();
        fetchMovies(currentPage);
        fetchTopRatedMovies();
        fetchActionMovies();
        fetchGenres();
        setupCarouselEffect();
        setupSearchBar();
        setupScrollPagination();
        setupNavigation();
        setupHeroAutoScroll();
    }

    private void bindViews() {
        searchEditText = findViewById(R.id.editTextSearch);
        searchIcon = findViewById(R.id.imageSearchIcon);
        logoutIcon = findViewById(R.id.logoutIcon);
        genreSpinner = findViewById(R.id.genreSpinner);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        homeContentScrollView = findViewById(R.id.homeContentScrollView);
        myListView = findViewById(R.id.myListView);
        myListEmptyText = findViewById(R.id.myListEmptyText);
        myListRecyclerView = findViewById(R.id.myListRecyclerView);
        comingSoonView = findViewById(R.id.comingSoonView);
        comingSoonRecyclerView = findViewById(R.id.comingSoonRecyclerView);
        carouselRecyclerView = findViewById(R.id.carouselRecyclerView);
        topRatedRecyclerView = findViewById(R.id.topRatedRecyclerView);
        actionRecyclerView = findViewById(R.id.actionRecyclerView);
        trendingRecyclerView = findViewById(R.id.trendingRecyclerView);
        recyclerView = findViewById(R.id.recyclerView);
        popularTitle = findViewById(R.id.popularTitle);
        trendingSection = findViewById(R.id.trendingSection);
        top10Section = findViewById(R.id.top10Section);
        actionSection = findViewById(R.id.actionSection);
    }

    private void setupAdapters() {
        // Hero carousel (full-bleed, no snap helper — use PagerSnapHelper)
        carouselRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        new PagerSnapHelper().attachToRecyclerView(carouselRecyclerView);
        carouselAdapter = new CarouselAdapter(heroMovies, this);
        carouselAdapter.setAddToListListener(this::addHeroMovieToList);
        carouselRecyclerView.setAdapter(carouselAdapter);

        // Trending
        trendingRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        trendingAdapter = new TrendingAdapter(trendingMovies, this);
        trendingRecyclerView.setAdapter(trendingAdapter);

        // Top Rated
        topRatedRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Action
        actionRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Popular vertical list
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // My List
        myListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        myListAdapter = new MyListAdapter(myListMovies, this);
        myListRecyclerView.setAdapter(myListAdapter);

        // Coming Soon
        comingSoonRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        comingSoonAdapter = new ComingSoonAdapter(comingSoonMovies, this);
        comingSoonRecyclerView.setAdapter(comingSoonAdapter);
    }

    // ─── Hero auto-scroll every 4 seconds ───────────────────────────────────

    private void setupHeroAutoScroll() {
        heroAutoScrollHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (heroMovies.isEmpty()) {
                    heroAutoScrollHandler.postDelayed(this, 4000);
                    return;
                }
                heroCurrentPos = (heroCurrentPos + 1) % Math.min(heroMovies.size(), 8);
                carouselRecyclerView.smoothScrollToPosition(heroCurrentPos);
                heroAutoScrollHandler.postDelayed(this, 4000);
            }
        }, 4000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        heroAutoScrollHandler.removeCallbacksAndMessages(null);
    }

    private void showAllSections() {
        trendingSection.setVisibility(View.VISIBLE);
        top10Section.setVisibility(View.VISIBLE);
        actionSection.setVisibility(View.VISIBLE);
        carouselRecyclerView.setVisibility(View.VISIBLE);
    }

    private void hideSectionsForFilter() {
        trendingSection.setVisibility(View.GONE);
        top10Section.setVisibility(View.GONE);
        actionSection.setVisibility(View.GONE);
        carouselRecyclerView.setVisibility(View.GONE);
    }

    // ─── Search ──────────────────────────────────────────────────────────────

    private void setupSearchBar() {
        searchIcon.setOnClickListener(v -> {
            if (searchEditText.getVisibility() == View.GONE) {
                searchEditText.setVisibility(View.VISIBLE);
                searchEditText.requestFocus();
            } else {
                searchEditText.setVisibility(View.GONE);
                searchEditText.setText("");
            }
        });

        logoutIcon.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (myMovieAdapter != null) myMovieAdapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ─── Scroll pagination ────────────────────────────────────────────────────

    private void setupScrollPagination() {
        LinearLayoutManager mgr = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (!isLoading && mgr != null
                        && mgr.findLastVisibleItemPosition() >= mgr.getItemCount() - 5) {
                    currentPage++;
                    fetchMovies(currentPage);
                    isLoading = true;
                }
            }
        });
    }

    // ─── Bottom navigation ────────────────────────────────────────────────────

    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                homeContentScrollView.setVisibility(View.VISIBLE);
                comingSoonView.setVisibility(View.GONE);
                myListView.setVisibility(View.GONE);
                return true;
            } else if (item.getItemId() == R.id.nav_coming_soon) {
                homeContentScrollView.setVisibility(View.GONE);
                comingSoonView.setVisibility(View.VISIBLE);
                myListView.setVisibility(View.GONE);
                if (!comingSoonLoaded) {
                    fetchComingSoonMovies();
                    comingSoonLoaded = true;
                }
                return true;
            } else if (item.getItemId() == R.id.nav_my_list) {
                homeContentScrollView.setVisibility(View.GONE);
                comingSoonView.setVisibility(View.GONE);
                myListView.setVisibility(View.VISIBLE);
                loadMyList();
                return true;
            }
            return false;
        });
    }

    // ─── Carousel zoom effect ────────────────────────────────────────────────

    private void setupCarouselEffect() {
        carouselRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm != null) {
                    heroCurrentPos = lm.findFirstVisibleItemPosition();
                }
            }
        });
    }

    // ─── Add hero movie to Firebase list ────────────────────────────────────

    private void addHeroMovieToList(MyMovieData movie) {
        if (currentUser == null) {
            Toast.makeText(this, "Connectez-vous pour gérer votre liste", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("movieId", movie.getMovieId());
        data.put("movieName", movie.getMovieName());
        data.put("movieDate", movie.getMovieDate());
        data.put("posterPath", movie.getMovieImage());
        data.put("addedAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(currentUser.getUid())
                .collection("myList").document(String.valueOf(movie.getMovieId()))
                .set(data)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, movie.getMovieName() + " ajouté à Ma Liste", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ─── Fetch: Now Playing (hero) ────────────────────────────────────────────

    private void fetchNowPlayingMovies() {
        String url = "https://api.themoviedb.org/3/movie/now_playing?api_key=" + TMDB_API_KEY
                + "&language=fr-FR&page=1";

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        heroMovies.clear();
                        for (int i = 0; i < Math.min(results.length(), 8); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            int id = obj.getInt("id");
                            String title = obj.optString("title", "");
                            String date = obj.optString("release_date", "");
                            String poster = obj.optString("poster_path", "");
                            String backdrop = obj.optString("backdrop_path", "");

                            JSONArray genreArray = obj.optJSONArray("genre_ids");
                            List<Integer> genreIds = new ArrayList<>();
                            if (genreArray != null) {
                                for (int j = 0; j < genreArray.length(); j++) {
                                    genreIds.add(genreArray.getInt(j));
                                }
                            }
                            MyMovieData movie = new MyMovieData(id, title, date, poster, genreIds);
                            movie.setBackdropPath(backdrop);
                            heroMovies.add(movie);
                        }
                        carouselAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Log.e(TAG, "Hero error: " + e.getMessage());
                    }
                }, error -> Log.e(TAG, "Hero error: " + error.getMessage()));
        queue.add(req);
    }

    // ─── Fetch: Trending ────────────────────────────────────────────────────

    private void fetchTrendingMovies() {
        String url = "https://api.themoviedb.org/3/trending/movie/week?api_key=" + TMDB_API_KEY
                + "&language=fr-FR";

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        trendingMovies.clear();
                        for (int i = 0; i < Math.min(results.length(), 10); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            int id = obj.getInt("id");
                            String title = obj.optString("title", "");
                            String date = obj.optString("release_date", "");
                            String poster = obj.optString("poster_path", "");
                            trendingMovies.add(new MyMovieData(id, title, date, poster));
                        }
                        trendingAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Log.e(TAG, "Trending error: " + e.getMessage());
                    }
                }, error -> Log.e(TAG, "Trending error: " + error.getMessage()));
        queue.add(req);
    }

    // ─── Fetch: Popular (vertical list) ──────────────────────────────────────

    private void fetchMovies(int page) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String url = BASE_URL + "?api_key=" + TMDB_API_KEY
                + "&page=" + page
                + "&sort_by=primary_release_date.desc"
                + "&primary_release_date.lte=" + today
                + "&vote_count.gte=50";
        if (selectedGenreId != -1) url += "&with_genres=" + selectedGenreId;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        List<MyMovieData> list = new ArrayList<>();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            list.add(new MyMovieData(
                                    obj.getInt("id"),
                                    obj.getString("title"),
                                    obj.optString("release_date", "N/A"),
                                    obj.optString("poster_path", "")));
                        }
                        if (myMovieAdapter == null) {
                            myMovieAdapter = new MyMovieAdapter(list.toArray(new MyMovieData[0]), this);
                            recyclerView.setAdapter(myMovieAdapter);
                        } else {
                            myMovieAdapter.addMovies(list);
                        }
                        isLoading = false;
                    } catch (JSONException e) {
                        isLoading = false;
                        Log.e(TAG, "Movies error: " + e.getMessage());
                    }
                }, error -> {
            isLoading = false;
            Log.e(TAG, "Movies error: " + error.getMessage());
        });
        queue.add(req);
    }

    // ─── Fetch: Action ───────────────────────────────────────────────────────

    private void fetchActionMovies() {
        String url = BASE_URL + "?api_key=" + TMDB_API_KEY + "&with_genres=28&sort_by=popularity.desc";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        List<MyMovieData> list = new ArrayList<>();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            list.add(new MyMovieData(obj.getInt("id"), obj.optString("title", ""),
                                    obj.optString("release_date", ""), obj.optString("poster_path", "")));
                        }
                        actionAdapter = new ActionAdapter(list, this);
                        actionRecyclerView.setAdapter(actionAdapter);
                    } catch (JSONException e) {
                        Log.e(TAG, "Action error: " + e.getMessage());
                    }
                }, error -> Log.e(TAG, "Action error: " + error.getMessage()));
        queue.add(req);
    }

    // ─── Fetch: Top Rated ────────────────────────────────────────────────────

    private void fetchTopRatedMovies() {
        String url = BASE_URL + "?api_key=" + TMDB_API_KEY
                + "&primary_release_year=2026&sort_by=vote_average.desc&vote_count.gte=10";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        List<MyMovieData> list = new ArrayList<>();
                        for (int i = 0; i < Math.min(results.length(), 10); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            list.add(new MyMovieData(obj.getInt("id"), obj.optString("title", ""),
                                    obj.optString("release_date", ""), obj.optString("poster_path", "")));
                        }
                        topRatedAdapter = new ActionAdapter(list, this);
                        topRatedRecyclerView.setAdapter(topRatedAdapter);
                    } catch (JSONException e) {
                        Log.e(TAG, "TopRated error: " + e.getMessage());
                    }
                }, error -> Log.e(TAG, "TopRated error: " + error.getMessage()));
        queue.add(req);
    }

    // ─── Fetch: Genres ───────────────────────────────────────────────────────

    private void fetchGenres() {
        String url = "https://api.themoviedb.org/3/genre/movie/list?api_key=" + TMDB_API_KEY;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray genres = response.getJSONArray("genres");
                        genreList.clear();
                        genreList.add(new Genre(-1, "Toutes les catégories"));
                        for (int i = 0; i < genres.length(); i++) {
                            JSONObject g = genres.getJSONObject(i);
                            genreList.add(new Genre(g.getInt("id"), g.getString("name")));
                        }
                        ArrayAdapter<Genre> adapter = new ArrayAdapter<Genre>(
                                this, R.layout.spinner_item, genreList) {
                            @NonNull @Override
                            public View getView(int pos, @Nullable View cv, @NonNull ViewGroup p) {
                                View v = super.getView(pos, cv, p);
                                ((TextView) v).setTextColor(Color.WHITE);
                                return v;
                            }
                        };
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        genreSpinner.setAdapter(adapter);
                        genreSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                                Genre g = (Genre) parent.getItemAtPosition(pos);
                                if (g.getId() != -1) {
                                    selectedGenreId = g.getId();
                                    hideSectionsForFilter();
                                    popularTitle.setText("Résultats : " + g.getName());
                                } else {
                                    selectedGenreId = -1;
                                    showAllSections();
                                    popularTitle.setText("Populaires");
                                }
                                currentPage = 1;
                                myMovieAdapter = null;
                                fetchMovies(currentPage);
                            }
                            @Override public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    } catch (JSONException e) {
                        Log.e(TAG, "Genre error: " + e.getMessage());
                    }
                }, error -> Log.e(TAG, "Genre error: " + error.getMessage()));
        queue.add(req);
    }

    // ─── Fetch: Coming Soon ──────────────────────────────────────────────────

    private void fetchComingSoonMovies() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String url = "https://api.themoviedb.org/3/movie/upcoming?api_key=" + TMDB_API_KEY
                + "&language=fr-FR&page=1&region=FR";

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        comingSoonMovies.clear();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            String releaseDate = obj.optString("release_date", "");
                            if (releaseDate.compareTo(today) < 0) continue;

                            JSONArray genreArray = obj.optJSONArray("genre_ids");
                            List<Integer> genreIds = new ArrayList<>();
                            if (genreArray != null) {
                                for (int j = 0; j < genreArray.length(); j++) {
                                    genreIds.add(genreArray.getInt(j));
                                }
                            }
                            comingSoonMovies.add(new ComingSoonMovie(
                                    obj.getInt("id"),
                                    obj.optString("title", ""),
                                    obj.optString("overview", ""),
                                    releaseDate,
                                    obj.optString("backdrop_path", ""),
                                    obj.optString("poster_path", ""),
                                    genreIds));
                        }
                        comingSoonMovies.sort((a, b) -> a.getReleaseDate().compareTo(b.getReleaseDate()));
                        comingSoonAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Log.e(TAG, "ComingSoon error: " + e.getMessage());
                    }
                }, error -> Log.e(TAG, "ComingSoon error: " + error.getMessage()));
        queue.add(req);
    }

    // ─── Load My List from Firestore ─────────────────────────────────────────

    private void loadMyList() {
        if (currentUser == null) {
            myListEmptyText.setVisibility(View.VISIBLE);
            myListRecyclerView.setVisibility(View.GONE);
            return;
        }
        db.collection("users").document(currentUser.getUid()).collection("myList")
                .orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    myListMovies.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Long idLong = doc.getLong("movieId");
                        if (idLong == null) continue;
                        myListMovies.add(new MyMovieData(
                                idLong.intValue(),
                                doc.getString("movieName"),
                                doc.getString("movieDate"),
                                doc.getString("posterPath")));
                    }
                    myListAdapter.notifyDataSetChanged();
                    boolean empty = myListMovies.isEmpty();
                    myListEmptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
                    myListRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Impossible de charger la liste", Toast.LENGTH_SHORT).show());
    }
}
