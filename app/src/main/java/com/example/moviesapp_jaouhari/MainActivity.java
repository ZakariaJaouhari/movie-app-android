package com.example.moviesapp_jaouhari;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
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
    private static final String TAG = "MainActivity";

    // Film genre definitions
    private static final int[] MOVIE_GENRE_IDS = {28, 35, 18, 27, 878, 53, 12, 16};
    private static final String[] MOVIE_GENRE_NAMES = {
        "Action", "Comédie", "Drame", "Horreur",
        "Science-Fiction", "Thriller", "Aventure", "Animation"
    };

    // TV genre definitions
    private static final int[] TV_GENRE_IDS = {18, 35, 10759, 9648, 80, 10765};
    private static final String[] TV_GENRE_NAMES = {
        "Drame", "Comédie", "Action & Aventure",
        "Mystère", "Crime", "Sci-Fi & Fantastique"
    };

    // Views
    private RecyclerView carouselRecyclerView;
    private RecyclerView topRatedRecyclerView;
    private RecyclerView trendingRecyclerView;
    private EditText searchEditText;
    private ImageView searchIcon;
    private ImageView logoutIcon;
    private BottomNavigationView bottomNavigationView;
    private NestedScrollView homeContentScrollView;
    private LinearLayout myListView;
    private TextView myListEmptyText;
    private RecyclerView myListRecyclerView;
    private LinearLayout comingSoonView;
    private RecyclerView comingSoonRecyclerView;
    private LinearLayout genreSectionsContainer;
    private LinearLayout tabAndFilterRow;

    // Series views
    private NestedScrollView seriesContentScrollView;
    private RecyclerView tvCarouselRecyclerView;
    private RecyclerView tvTrendingRecyclerView;
    private RecyclerView tvTopRatedRecyclerView;
    private LinearLayout tvGenreSectionsContainer;
    private TextView btnFilms;
    private TextView btnSeries;

    // Adapters
    private CarouselAdapter carouselAdapter;
    private ActionAdapter topRatedAdapter;
    private TrendingAdapter trendingAdapter;
    private MyListAdapter myListAdapter;
    private ComingSoonAdapter comingSoonAdapter;

    // Series adapters
    private CarouselAdapter tvCarouselAdapter;
    private TrendingAdapter tvTrendingAdapter;
    private ActionAdapter tvTopRatedAdapter;

    // Data lists
    private final List<MyMovieData> heroMovies = new ArrayList<>();
    private final List<MyMovieData> trendingMovies = new ArrayList<>();
    private final List<MyMovieData> myListMovies = new ArrayList<>();
    private final List<ComingSoonMovie> comingSoonMovies = new ArrayList<>();
    private final List<MyMovieData> tvHeroShows = new ArrayList<>();
    private final List<MyMovieData> tvTrendingShows = new ArrayList<>();

    // Media tab state
    private enum MediaTab { FILMS, SERIES }
    private MediaTab currentMediaTab = MediaTab.FILMS;
    private boolean seriesLoaded = false;
    private boolean movieGenresLoaded = false;
    private boolean tvGenresLoaded = false;

    // State
    private RequestQueue queue;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
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
        fetchTopRatedMovies();
        setupCarouselEffect();
        setupSearchBar();
        setupNavigation();
        setupTabButtons();
        setupHeroAutoScroll();
        createMovieGenreSections();
    }

    private void bindViews() {
        searchEditText = findViewById(R.id.editTextSearch);
        searchIcon = findViewById(R.id.imageSearchIcon);
        logoutIcon = findViewById(R.id.logoutIcon);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        homeContentScrollView = findViewById(R.id.homeContentScrollView);
        myListView = findViewById(R.id.myListView);
        myListEmptyText = findViewById(R.id.myListEmptyText);
        myListRecyclerView = findViewById(R.id.myListRecyclerView);
        comingSoonView = findViewById(R.id.comingSoonView);
        comingSoonRecyclerView = findViewById(R.id.comingSoonRecyclerView);
        carouselRecyclerView = findViewById(R.id.carouselRecyclerView);
        topRatedRecyclerView = findViewById(R.id.topRatedRecyclerView);
        trendingRecyclerView = findViewById(R.id.trendingRecyclerView);
        genreSectionsContainer = findViewById(R.id.genreSectionsContainer);
        tabAndFilterRow = findViewById(R.id.tabAndFilterRow);
        seriesContentScrollView = findViewById(R.id.seriesContentScrollView);
        tvCarouselRecyclerView = findViewById(R.id.tvCarouselRecyclerView);
        tvTrendingRecyclerView = findViewById(R.id.tvTrendingRecyclerView);
        tvTopRatedRecyclerView = findViewById(R.id.tvTopRatedRecyclerView);
        tvGenreSectionsContainer = findViewById(R.id.tvGenreSectionsContainer);
        btnFilms = findViewById(R.id.btnFilms);
        btnSeries = findViewById(R.id.btnSeries);
    }

    private void setupAdapters() {
        carouselRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        new PagerSnapHelper().attachToRecyclerView(carouselRecyclerView);
        carouselAdapter = new CarouselAdapter(heroMovies, this);
        carouselAdapter.setAddToListListener(this::addHeroMovieToList);
        carouselRecyclerView.setAdapter(carouselAdapter);

        trendingRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        trendingAdapter = new TrendingAdapter(trendingMovies, this);
        trendingRecyclerView.setAdapter(trendingAdapter);

        topRatedRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        myListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        myListAdapter = new MyListAdapter(myListMovies, this);
        myListRecyclerView.setAdapter(myListAdapter);

        comingSoonRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        comingSoonAdapter = new ComingSoonAdapter(comingSoonMovies, this);
        comingSoonRecyclerView.setAdapter(comingSoonAdapter);

        setupTvAdapters();
    }

    // ─── Hero auto-scroll ────────────────────────────────────────────────────

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
    }

    // ─── Bottom navigation ────────────────────────────────────────────────────

    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                showHomeContent();
                return true;
            } else if (item.getItemId() == R.id.nav_coming_soon) {
                homeContentScrollView.setVisibility(View.GONE);
                seriesContentScrollView.setVisibility(View.GONE);
                comingSoonView.setVisibility(View.VISIBLE);
                myListView.setVisibility(View.GONE);
                tabAndFilterRow.setVisibility(View.GONE);
                if (!comingSoonLoaded) {
                    fetchComingSoonMovies();
                    comingSoonLoaded = true;
                }
                return true;
            } else if (item.getItemId() == R.id.nav_my_list) {
                homeContentScrollView.setVisibility(View.GONE);
                seriesContentScrollView.setVisibility(View.GONE);
                comingSoonView.setVisibility(View.GONE);
                myListView.setVisibility(View.VISIBLE);
                tabAndFilterRow.setVisibility(View.GONE);
                loadMyList();
                return true;
            }
            return false;
        });
    }

    // ─── Carousel scroll tracking ────────────────────────────────────────────

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

    // ─── Add to Firebase list ────────────────────────────────────────────────

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
                            trendingMovies.add(new MyMovieData(
                                    obj.getInt("id"),
                                    obj.optString("title", ""),
                                    obj.optString("release_date", ""),
                                    obj.optString("poster_path", "")));
                        }
                        trendingAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Log.e(TAG, "Trending error: " + e.getMessage());
                    }
                }, error -> Log.e(TAG, "Trending error: " + error.getMessage()));
        queue.add(req);
    }

    // ─── Fetch: Top Rated ────────────────────────────────────────────────────

    private void fetchTopRatedMovies() {
        String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + TMDB_API_KEY
                + "&primary_release_year=2026&sort_by=popularity.desc";
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

    // ─── Tab Films / Séries ──────────────────────────────────────────────────

    private void setupTabButtons() {
        btnFilms.setOnClickListener(v -> {
            if (currentMediaTab != MediaTab.FILMS) {
                currentMediaTab = MediaTab.FILMS;
                showHomeContent();
                updateTabButtons();
            }
        });
        btnSeries.setOnClickListener(v -> {
            if (currentMediaTab != MediaTab.SERIES) {
                currentMediaTab = MediaTab.SERIES;
                showHomeContent();
                updateTabButtons();
            }
        });
    }

    private void showHomeContent() {
        comingSoonView.setVisibility(View.GONE);
        myListView.setVisibility(View.GONE);
        tabAndFilterRow.setVisibility(View.VISIBLE);
        if (currentMediaTab == MediaTab.SERIES) {
            homeContentScrollView.setVisibility(View.GONE);
            seriesContentScrollView.setVisibility(View.VISIBLE);
            if (!seriesLoaded) {
                fetchTvHeroAndTrending();
                fetchTopRatedTvShows();
                seriesLoaded = true;
            }
            if (!tvGenresLoaded) {
                createTvGenreSections();
                tvGenresLoaded = true;
            }
        } else {
            homeContentScrollView.setVisibility(View.VISIBLE);
            seriesContentScrollView.setVisibility(View.GONE);
        }
    }

    private void updateTabButtons() {
        if (currentMediaTab == MediaTab.FILMS) {
            btnFilms.setBackgroundResource(R.drawable.bg_tab_pill_selected);
            btnFilms.setTextColor(Color.BLACK);
            btnSeries.setBackgroundResource(R.drawable.bg_tab_pill);
            btnSeries.setTextColor(Color.WHITE);
        } else {
            btnSeries.setBackgroundResource(R.drawable.bg_tab_pill_selected);
            btnSeries.setTextColor(Color.BLACK);
            btnFilms.setBackgroundResource(R.drawable.bg_tab_pill);
            btnFilms.setTextColor(Color.WHITE);
        }
    }

    // ─── Series adapters setup ───────────────────────────────────────────────

    private void setupTvAdapters() {
        tvCarouselRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        new PagerSnapHelper().attachToRecyclerView(tvCarouselRecyclerView);
        tvCarouselAdapter = new CarouselAdapter(tvHeroShows, this);
        tvCarouselAdapter.setAddToListListener(this::addHeroMovieToList);
        tvCarouselRecyclerView.setAdapter(tvCarouselAdapter);

        tvTrendingRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tvTrendingAdapter = new TrendingAdapter(tvTrendingShows, this);
        tvTrendingRecyclerView.setAdapter(tvTrendingAdapter);

        tvTopRatedRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    // ─── Fetch: Trending TV ──────────────────────────────────────────────────

    private void fetchTvHeroAndTrending() {
        String url = "https://api.themoviedb.org/3/trending/tv/week?api_key=" + TMDB_API_KEY
                + "&language=fr-FR";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        tvHeroShows.clear();
                        tvTrendingShows.clear();
                        for (int i = 0; i < Math.min(results.length(), 10); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            int id = obj.getInt("id");
                            String title = obj.optString("name", obj.optString("title", ""));
                            String date = obj.optString("first_air_date", obj.optString("release_date", ""));
                            String poster = obj.optString("poster_path", "");
                            String backdrop = obj.optString("backdrop_path", "");
                            JSONArray genreArray = obj.optJSONArray("genre_ids");
                            List<Integer> genreIds = new ArrayList<>();
                            if (genreArray != null) {
                                for (int j = 0; j < genreArray.length(); j++) genreIds.add(genreArray.getInt(j));
                            }
                            MyMovieData show = new MyMovieData(id, title, date, poster, genreIds);
                            show.setBackdropPath(backdrop);
                            show.setTV(true);
                            tvTrendingShows.add(show);
                            if (i < 8) tvHeroShows.add(show);
                        }
                        tvCarouselAdapter.notifyDataSetChanged();
                        tvTrendingAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Log.e(TAG, "TV trending error: " + e.getMessage());
                    }
                }, error -> Log.e(TAG, "TV trending error: " + error.getMessage()));
        queue.add(req);
    }

    // ─── Fetch: Top Rated TV ─────────────────────────────────────────────────

    private void fetchTopRatedTvShows() {
        String url = "https://api.themoviedb.org/3/discover/tv?api_key=" + TMDB_API_KEY
                + "&first_air_date_year=2026&sort_by=popularity.desc&language=fr-FR";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        List<MyMovieData> list = new ArrayList<>();
                        for (int i = 0; i < Math.min(results.length(), 10); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            MyMovieData show = new MyMovieData(
                                    obj.getInt("id"),
                                    obj.optString("name", obj.optString("title", "")),
                                    obj.optString("first_air_date", obj.optString("release_date", "")),
                                    obj.optString("poster_path", ""));
                            show.setTV(true);
                            list.add(show);
                        }
                        tvTopRatedAdapter = new ActionAdapter(list, this);
                        tvTopRatedRecyclerView.setAdapter(tvTopRatedAdapter);
                    } catch (JSONException e) {
                        Log.e(TAG, "TV top rated error: " + e.getMessage());
                    }
                }, error -> Log.e(TAG, "TV top rated error: " + error.getMessage()));
        queue.add(req);
    }

    // ─── Genre sections ──────────────────────────────────────────────────────

    private void createMovieGenreSections() {
        if (movieGenresLoaded) return;
        movieGenresLoaded = true;
        for (int i = 0; i < MOVIE_GENRE_IDS.length; i++) {
            createGenreSection(genreSectionsContainer, MOVIE_GENRE_IDS[i], MOVIE_GENRE_NAMES[i], false);
        }
    }

    private void createTvGenreSections() {
        for (int i = 0; i < TV_GENRE_IDS.length; i++) {
            createGenreSection(tvGenreSectionsContainer, TV_GENRE_IDS[i], TV_GENRE_NAMES[i], true);
        }
    }

    private void createGenreSection(LinearLayout container, int genreId, String genreName, boolean isTV) {
        LinearLayout section = new LinearLayout(this);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sectionParams.setMargins(0, 0, 0, dpToPx(20));
        section.setLayoutParams(sectionParams);
        section.setOrientation(LinearLayout.VERTICAL);

        // Header: title + "Voir tout"
        LinearLayout header = new LinearLayout(this);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dpToPx(12), 0, dpToPx(12), dpToPx(10));

        TextView titleView = new TextView(this);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleView.setLayoutParams(titleParams);
        titleView.setText(genreName);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(17);
        titleView.setTypeface(null, Typeface.BOLD);

        TextView seeAll = new TextView(this);
        seeAll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        seeAll.setText("Voir tout");
        seeAll.setTextColor(Color.parseColor("#E50914"));
        seeAll.setTextSize(13);
        seeAll.setOnClickListener(v -> {
            Intent intent = new Intent(this, movie_item_list.class);
            intent.putExtra("genreId", genreId);
            intent.putExtra("genreName", genreName);
            intent.putExtra("isTV", isTV);
            startActivity(intent);
        });

        header.addView(titleView);
        header.addView(seeAll);

        // Horizontal RecyclerView
        RecyclerView rv = new RecyclerView(this);
        LinearLayout.LayoutParams rvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(170));
        rv.setLayoutParams(rvParams);
        rv.setPadding(dpToPx(12), 0, dpToPx(12), 0);
        rv.setClipToPadding(false);
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        section.addView(header);
        section.addView(rv);
        container.addView(section);

        fetchGenreItems(genreId, isTV, rv);
    }

    private void fetchGenreItems(int genreId, boolean isTV, RecyclerView rv) {
        String baseUrl = isTV
                ? "https://api.themoviedb.org/3/discover/tv"
                : "https://api.themoviedb.org/3/discover/movie";
        String url = baseUrl + "?api_key=" + TMDB_API_KEY
                + "&with_genres=" + genreId
                + "&sort_by=popularity.desc"
                + "&language=fr-FR";

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        List<MyMovieData> list = new ArrayList<>();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            String title = isTV
                                    ? obj.optString("name", obj.optString("title", ""))
                                    : obj.optString("title", "");
                            String date = isTV
                                    ? obj.optString("first_air_date", "")
                                    : obj.optString("release_date", "");
                            MyMovieData item = new MyMovieData(
                                    obj.getInt("id"), title, date, obj.optString("poster_path", ""));
                            item.setTV(isTV);
                            list.add(item);
                        }
                        ActionAdapter adapter = new ActionAdapter(list, this);
                        rv.setAdapter(adapter);
                    } catch (JSONException e) {
                        Log.e(TAG, "Genre items error: " + e.getMessage());
                    }
                }, error -> Log.e(TAG, "Genre items error: " + error.getMessage()));
        queue.add(req);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
