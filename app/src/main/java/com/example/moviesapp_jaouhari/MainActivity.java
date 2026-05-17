package com.example.moviesapp_jaouhari;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.speech.RecognizerIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.graphics.Typeface;
import android.net.Uri;
import android.widget.ScrollView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.recyclerview.widget.GridLayoutManager;
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

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TMDB_API_KEY = "ce601ad5b7a468bf65223a10b811735b";
    private static final String TAG = "MainActivity";
    private static final int VOICE_SEARCH_REQUEST_CODE = 1001;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;
    private static final String MAPS_API_KEY = "AIzaSyCk-lkzx1GeEAqTm1M9v8YYqkAK1biU530";
    private static final String GROQ_API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String GROQ_RECO_PROMPT =
        "Tu analyses une liste de films ou séries aimés par un utilisateur et tu retournes UNIQUEMENT un tableau JSON " +
        "de recommandations, sans markdown ni texte autour.\n\n" +
        "Format strict de chaque élément :\n" +
        "{\n" +
        "  \"explanation\": \"Parce que tu aimes [titre précis ou genre récurrent de la liste]\",\n" +
        "  \"media_type\": \"movie\" ou \"tv\",\n" +
        "  \"with_genres\": \"53,9648\",\n" +
        "  \"sort_by\": \"popularity.desc\",\n" +
        "  \"vote_average_gte\": 7.0,\n" +
        "  \"with_original_language\": \"en\"\n" +
        "}\n\n" +
        "Règles :\n" +
        "- Génère exactement le nombre de catégories demandé dans le message utilisateur\n" +
        "- Chaque catégorie doit avoir des genres différents des autres\n" +
        "- Tous les éléments doivent avoir le même media_type que demandé\n" +
        "- explanation : phrase courte (ex: \"Parce que tu aimes Inception\", \"Parce que tu adores les thrillers\")\n" +
        "- with_genres : IDs TMDB séparés par virgule, max 2 genres\n" +
        "- sort_by : \"popularity.desc\" ou \"vote_average.desc\"\n" +
        "- vote_average_gte : optionnel, entre 6.0 et 8.0\n" +
        "- with_original_language : optionnel\n\n" +
        "Genres films: 28=Action,12=Aventure,16=Animation,35=Comédie,80=Crime,18=Drame,14=Fantastique,27=Horreur,9648=Mystère,10749=Romance,878=Science-Fiction,53=Thriller,10752=Guerre,37=Western\n" +
        "Genres séries: 10759=Action,16=Animation,35=Comédie,80=Crime,18=Drame,9648=Mystère,10765=Sci-Fi & Fantastique";
    private static final String GROQ_SYSTEM_PROMPT =
        "Tu analyses des requêtes de recherche de films/séries et retournes UNIQUEMENT un objet JSON valide, sans markdown ni texte autour.\n\n" +
        "Champs disponibles (tous optionnels) :\n" +
        "- use_search: boolean (true=chercher par titre exact, false=filtrer par paramètres)\n" +
        "- query: string (si use_search=true, mots-clés pour TMDB)\n" +
        "- media_type: \"movie\" ou \"tv\"\n" +
        "- with_genres: string (IDs genres séparés par virgule)\n" +
        "- year: number (année de sortie)\n" +
        "- with_original_language: string (code ISO: ja, fr, en, ko, es, it, hi)\n" +
        "- sort_by: \"popularity.desc\" | \"vote_average.desc\" | \"release_date.desc\"\n" +
        "- vote_average_gte: number\n\n" +
        "Genres films: 28=Action,12=Aventure,16=Animation,35=Comédie,80=Crime,18=Drame,14=Fantastique,27=Horreur,9648=Mystère,10749=Romance,878=Science-Fiction,53=Thriller,10752=Guerre,37=Western\n" +
        "Genres séries: 10759=Action,16=Animation,35=Comédie,80=Crime,18=Drame,9648=Mystère,10765=Sci-Fi & Fantastique,37=Western\n\n" +
        "Exemples :\n" +
        "\"film d'horreur japonais\" → {\"use_search\":false,\"media_type\":\"movie\",\"with_genres\":\"27\",\"with_original_language\":\"ja\",\"sort_by\":\"popularity.desc\"}\n" +
        "\"comédie romantique française des années 2000\" → {\"use_search\":false,\"media_type\":\"movie\",\"with_genres\":\"35,10749\",\"with_original_language\":\"fr\",\"year\":2000,\"sort_by\":\"popularity.desc\"}\n" +
        "\"Inception\" → {\"use_search\":true,\"query\":\"Inception\"}";

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
    private ImageView micIcon;
    private ImageView moodIcon;
    private ImageView logoutIcon;
    private BottomNavigationView bottomNavigationView;
    private NestedScrollView homeContentScrollView;
    private NestedScrollView myListView;
    private ScrollView profileView;
    private TextView profileAvatar;
    private TextView profileName;
    private TextView profileEmail;
    private android.widget.Button btnLogout;
    private android.widget.Button btnDeleteAccount;
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

    // Home recommendations
    private LinearLayout homeRecommendationsContainer;
    private LinearLayout tvRecommendationsContainer;
    private final java.util.Set<String> lastMovieTitles = new java.util.HashSet<>();
    private final java.util.Set<String> lastTvTitles = new java.util.HashSet<>();

    // Cinema
    private LinearLayout cinemaView;
    private com.google.android.gms.maps.MapView mapView;
    private RecyclerView cinemaRecyclerView;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private com.google.android.gms.location.LocationCallback locationCallback;
    private boolean cinemaMapInitialized = false;
    private final List<Cinema> cinemaList = new ArrayList<>();
    private CinemaAdapter cinemaAdapter;

    // Search
    private LinearLayout searchBarContainer;
    private LinearLayout searchResultsView;
    private RecyclerView searchResultsRecyclerView;
    private TextView searchStatusText;
    private final List<MyMovieData> searchResults = new ArrayList<>();
    private GenreGridAdapter searchAdapter;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    interface ClaudeCallback {
        void onSuccess(JSONObject params);
        void onError();
    }

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (mapView != null) mapView.onCreate(savedInstanceState);

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
        setupProfile();
    }

    private void bindViews() {
        cinemaView = findViewById(R.id.cinemaView);
        mapView = findViewById(R.id.mapView);
        cinemaRecyclerView = findViewById(R.id.cinemaRecyclerView);
        searchBarContainer = findViewById(R.id.searchBarContainer);
        searchEditText = findViewById(R.id.editTextSearch);
        searchIcon = findViewById(R.id.imageSearchIcon);
        micIcon = findViewById(R.id.imageMicIcon);
        moodIcon = findViewById(R.id.imageMoodIcon);
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
        searchResultsView = findViewById(R.id.searchResultsView);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        searchStatusText = findViewById(R.id.searchStatusText);
        homeRecommendationsContainer = findViewById(R.id.homeRecommendationsContainer);
        tvRecommendationsContainer = findViewById(R.id.tvRecommendationsContainer);
        profileView = findViewById(R.id.profileView);
        profileAvatar = findViewById(R.id.profileAvatar);
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
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


        cinemaRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        cinemaAdapter = new CinemaAdapter(cinemaList, this, cinema -> {
            if (googleMap != null) {
                LatLng pos = new LatLng(cinema.lat, cinema.lng);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
            }
        });
        cinemaRecyclerView.setAdapter(cinemaAdapter);

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
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        loadHomeRecommendations();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        heroAutoScrollHandler.removeCallbacksAndMessages(null);
        if (locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    // ─── Search ──────────────────────────────────────────────────────────────

    private void setupSearchBar() {
        searchAdapter = new GenreGridAdapter(searchResults, this, false);
        searchResultsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        searchResultsRecyclerView.setAdapter(searchAdapter);

        searchIcon.setOnClickListener(v -> {
            if (searchBarContainer.getVisibility() == View.GONE) {
                searchBarContainer.setVisibility(View.VISIBLE);
                searchEditText.requestFocus();
            } else {
                searchBarContainer.setVisibility(View.GONE);
                searchEditText.setText("");
                hideSearchResults();
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    hideSearchResults();
                    return;
                }
                searchRunnable = () -> performSearch(query);
                searchHandler.postDelayed(searchRunnable, 400);
            }
        });

        micIcon.setOnClickListener(v -> startVoiceSearch());

        moodIcon.setOnClickListener(v ->
                startActivity(new Intent(this, MoodScanActivity.class)));


        logoutIcon.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez pour rechercher...");
        try {
            startActivityForResult(intent, VOICE_SEARCH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Reconnaissance vocale non disponible sur cet appareil", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_SEARCH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            java.util.ArrayList<String> results =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spoken = results.get(0);
                searchBarContainer.setVisibility(View.VISIBLE);
                searchEditText.setText(spoken);
                searchEditText.setSelection(spoken.length());
            }
        }
    }

    private void performSearch(String query) {
        showSearchResults();
        setSearchStatus("IA en cours d'analyse...", true);
        callGroqForParams(query, new ClaudeCallback() {
            @Override
            public void onSuccess(JSONObject params) {
                try {
                    boolean useSearch = params.optBoolean("use_search", true);
                    boolean isTV = "tv".equals(params.optString("media_type", "movie"));
                    String url = buildTmdbUrl(params);
                    Log.d(TAG, "TMDB URL: " + url);
                    fetchSearchResults(url, useSearch, isTV);
                } catch (JSONException e) {
                    Log.e(TAG, "Claude params error: " + e.getMessage());
                    fallbackSearch(query);
                }
            }

            @Override
            public void onError() {
                Toast.makeText(MainActivity.this, "IA indisponible, recherche classique...", Toast.LENGTH_SHORT).show();
                fallbackSearch(query);
            }
        });
    }

    private void callGroqForParams(String query, ClaudeCallback callback) {
        String url = "https://api.groq.com/openai/v1/chat/completions";
        try {
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", GROQ_SYSTEM_PROMPT);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", query);

            JSONObject body = new JSONObject();
            body.put("model", "llama-3.1-8b-instant");
            body.put("max_tokens", 256);
            body.put("temperature", 0);
            body.put("messages", new JSONArray().put(systemMsg).put(userMsg));

            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, body,
                    response -> {
                        try {
                            String text = response
                                    .getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content").trim();
                            Log.d(TAG, "Groq raw response: " + text);
                            text = text.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
                            callback.onSuccess(new JSONObject(text));
                        } catch (JSONException e) {
                            Log.e(TAG, "Groq parse error: " + e.getMessage());
                            callback.onError();
                        }
                    },
                    error -> {
                        String details = error.toString();
                        if (error.networkResponse != null) {
                            details += " | HTTP " + error.networkResponse.statusCode;
                            if (error.networkResponse.data != null)
                                details += " | " + new String(error.networkResponse.data);
                        }
                        Log.e(TAG, "Groq API error: " + details);
                        callback.onError();
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + GROQ_API_KEY);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            queue.add(req);
        } catch (JSONException e) {
            Log.e(TAG, "Groq request build error: " + e.getMessage());
            callback.onError();
        }
    }

    private String buildTmdbUrl(JSONObject params) throws JSONException {
        boolean useSearch = params.optBoolean("use_search", true);
        String mediaType = params.optString("media_type", "movie");
        boolean isTV = "tv".equals(mediaType);

        if (useSearch) {
            String q = params.optString("query", "");
            return "https://api.themoviedb.org/3/search/multi?api_key=" + TMDB_API_KEY
                    + "&language=fr-FR&query=" + Uri.encode(q);
        }

        String base = isTV
                ? "https://api.themoviedb.org/3/discover/tv"
                : "https://api.themoviedb.org/3/discover/movie";

        StringBuilder url = new StringBuilder(base)
                .append("?api_key=").append(TMDB_API_KEY)
                .append("&language=fr-FR")
                .append("&sort_by=").append(params.optString("sort_by", "popularity.desc"));

        if (params.has("with_genres"))
            url.append("&with_genres=").append(params.getString("with_genres").replace(" ", ""));
        if (params.has("year") && !isTV)
            url.append("&primary_release_year=").append(params.getInt("year"));
        if (params.has("year") && isTV)
            url.append("&first_air_date_year=").append(params.getInt("year"));
        if (params.has("with_original_language"))
            url.append("&with_original_language=").append(params.getString("with_original_language"));
        if (params.has("vote_average_gte"))
            url.append("&vote_average.gte=").append(params.getDouble("vote_average_gte"));

        return url.toString();
    }

    private void fetchSearchResults(String url, boolean isMultiSearch, boolean isTV) {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        searchResults.clear();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            boolean tv;
                            if (isMultiSearch) {
                                String mediaType = obj.optString("media_type", "movie");
                                if ("person".equals(mediaType)) continue;
                                tv = "tv".equals(mediaType);
                            } else {
                                tv = isTV;
                            }
                            String title = tv
                                    ? obj.optString("name", obj.optString("title", ""))
                                    : obj.optString("title", obj.optString("name", ""));
                            String date = tv
                                    ? obj.optString("first_air_date", "")
                                    : obj.optString("release_date", "");
                            MyMovieData item = new MyMovieData(
                                    obj.getInt("id"), title, date, obj.optString("poster_path", ""));
                            item.setTV(tv);
                            searchResults.add(item);
                        }
                        searchAdapter.notifyDataSetChanged();
                        if (searchResults.isEmpty()) {
                            setSearchStatus("Aucun résultat trouvé", true);
                        } else {
                            setSearchStatus("", false);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Fetch results error: " + e.getMessage());
                        setSearchStatus("Erreur lors de la recherche", true);
                    }
                }, error -> {
            Log.e(TAG, "Fetch results error: " + error.getMessage());
            setSearchStatus("Erreur lors de la recherche", true);
        });
        queue.add(req);
    }

    private void fallbackSearch(String query) {
        String url = "https://api.themoviedb.org/3/search/multi?api_key=" + TMDB_API_KEY
                + "&language=fr-FR&query=" + Uri.encode(query);
        fetchSearchResults(url, true, false);
    }

    private void setSearchStatus(String message, boolean visible) {
        if (visible && !message.isEmpty()) {
            searchStatusText.setText(message);
            searchStatusText.setVisibility(View.VISIBLE);
            searchResultsRecyclerView.setVisibility(View.GONE);
        } else {
            searchStatusText.setVisibility(View.GONE);
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showSearchResults() {
        searchResultsView.setVisibility(View.VISIBLE);
        homeContentScrollView.setVisibility(View.GONE);
        seriesContentScrollView.setVisibility(View.GONE);
        comingSoonView.setVisibility(View.GONE);
        myListView.setVisibility(View.GONE);
    }

    private void hideSearchResults() {
        searchResultsView.setVisibility(View.GONE);
        showHomeContent();
    }

    // ─── Bottom navigation ────────────────────────────────────────────────────

    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                cinemaView.setVisibility(View.GONE);
                showHomeContent();
                return true;
            } else if (item.getItemId() == R.id.nav_cinema) {
                homeContentScrollView.setVisibility(View.GONE);
                seriesContentScrollView.setVisibility(View.GONE);
                comingSoonView.setVisibility(View.GONE);
                myListView.setVisibility(View.GONE);
                profileView.setVisibility(View.GONE);
                tabAndFilterRow.setVisibility(View.GONE);
                cinemaView.setVisibility(View.VISIBLE);
                if (!cinemaMapInitialized) {
                    cinemaMapInitialized = true;
                    initCinemaMap();
                }
                return true;
            } else if (item.getItemId() == R.id.nav_coming_soon) {
                homeContentScrollView.setVisibility(View.GONE);
                seriesContentScrollView.setVisibility(View.GONE);
                comingSoonView.setVisibility(View.VISIBLE);
                myListView.setVisibility(View.GONE);
                profileView.setVisibility(View.GONE);
                cinemaView.setVisibility(View.GONE);
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
                profileView.setVisibility(View.GONE);
                cinemaView.setVisibility(View.GONE);
                myListView.setVisibility(View.VISIBLE);
                tabAndFilterRow.setVisibility(View.GONE);
                loadMyList();
                return true;
            } else if (item.getItemId() == R.id.nav_profile) {
                homeContentScrollView.setVisibility(View.GONE);
                seriesContentScrollView.setVisibility(View.GONE);
                comingSoonView.setVisibility(View.GONE);
                myListView.setVisibility(View.GONE);
                cinemaView.setVisibility(View.GONE);
                profileView.setVisibility(View.VISIBLE);
                tabAndFilterRow.setVisibility(View.GONE);
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
        data.put("isTV", movie.isTV());
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

    // ─── Profil ──────────────────────────────────────────────────────────────

    private void setupProfile() {
        if (currentUser == null) return;

        // Infos utilisateur
        String email = currentUser.getEmail() != null ? currentUser.getEmail() : "";
        String displayName = currentUser.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        }
        profileName.setText(displayName);
        profileEmail.setText(email);

        // Avatar : initiale du nom
        String initial = displayName.isEmpty() ? "?" : String.valueOf(displayName.charAt(0)).toUpperCase();
        profileAvatar.setText(initial);

        // Déconnexion
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Suppression du compte
        btnDeleteAccount.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Supprimer le compte")
                    .setMessage("Es-tu sûr(e) de vouloir supprimer ton compte ? Toutes tes données seront perdues. Cette action est irréversible.")
                    .setPositiveButton("Supprimer", (dialog, which) -> deleteAccount())
                    .setNegativeButton("Annuler", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });
    }

    private void deleteAccount() {
        if (currentUser == null) return;
        String uid = currentUser.getUid();

        // Supprimer les données Firestore puis le compte Auth
        db.collection("users").document(uid).collection("myList")
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (QueryDocumentSnapshot doc : snapshots) doc.getReference().delete();
                    db.collection("users").document(uid).delete();
                    currentUser.delete()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Compte supprimé", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Erreur : reconnecte-toi et réessaie", Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Delete account error: " + e.getMessage());
                            });
                });
    }

    // ─── Recommandations IA sur l'accueil ────────────────────────────────────

    private void loadHomeRecommendations() {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid()).collection("myList")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<String> movieTitles = new ArrayList<>();
                    List<String> tvTitles = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String name = doc.getString("movieName");
                        if (name == null) continue;
                        Boolean isTv = doc.getBoolean("isTV");
                        if (Boolean.TRUE.equals(isTv)) tvTitles.add(name);
                        else movieTitles.add(name);
                    }

                    java.util.Set<String> newMovieTitles = new java.util.HashSet<>(movieTitles);
                    java.util.Set<String> newTvTitles = new java.util.HashSet<>(tvTitles);

                    boolean moviesChanged = !newMovieTitles.equals(lastMovieTitles);
                    boolean tvChanged = !newTvTitles.equals(lastTvTitles);

                    lastMovieTitles.clear();
                    lastMovieTitles.addAll(newMovieTitles);
                    lastTvTitles.clear();
                    lastTvTitles.addAll(newTvTitles);

                    if (!movieTitles.isEmpty() && moviesChanged) {
                        int count = Math.min(movieTitles.size(), 3);
                        callGroqForCategory(movieTitles, false, count, homeRecommendationsContainer);
                    }
                    if (!tvTitles.isEmpty() && tvChanged) {
                        int count = Math.min(tvTitles.size(), 3);
                        callGroqForCategory(tvTitles, true, count, tvRecommendationsContainer);
                    }
                });
    }

    private void callGroqForCategory(List<String> titles, boolean isTV, int count, LinearLayout container) {
        String listStr = String.join(", ", titles.subList(0, Math.min(titles.size(), 10)));
        String mediaLabel = isTV ? "séries" : "films";
        String url = "https://api.groq.com/openai/v1/chat/completions";
        try {
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", GROQ_RECO_PROMPT);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content",
                "Ma liste de " + mediaLabel + " : " + listStr + "\n" +
                "Génère exactement " + count + " catégorie(s) de type \"" + (isTV ? "tv" : "movie") + "\".");

            JSONObject body = new JSONObject();
            body.put("model", "llama-3.1-8b-instant");
            body.put("max_tokens", 500);
            body.put("temperature", 0.3);
            body.put("messages", new JSONArray().put(systemMsg).put(userMsg));

            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, body,
                    response -> {
                        try {
                            String text = response
                                    .getJSONArray("choices").getJSONObject(0)
                                    .getJSONObject("message").getString("content").trim();
                            text = text.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
                            Log.d(TAG, "Groq reco (" + mediaLabel + "): " + text);
                            JSONArray categories = new JSONArray(text);
                            container.setVisibility(View.VISIBLE);
                            container.removeAllViews();
                            for (int i = 0; i < categories.length(); i++) {
                                buildRecommendationSection(categories.getJSONObject(i), container);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Groq reco parse error: " + e.getMessage());
                        }
                    },
                    error -> Log.e(TAG, "Groq reco error: " + error.toString())) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + GROQ_API_KEY);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            queue.add(req);
        } catch (JSONException e) {
            Log.e(TAG, "Groq reco request error: " + e.getMessage());
        }
    }

    private void buildRecommendationSection(JSONObject category, LinearLayout container) throws JSONException {
        String explanation = category.optString("explanation", "Pour vous");
        boolean isTV = "tv".equals(category.optString("media_type", "movie"));

        // Section container
        LinearLayout section = new LinearLayout(this);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sectionParams.setMargins(0, 0, 0, dpToPx(20));
        section.setLayoutParams(sectionParams);
        section.setOrientation(LinearLayout.VERTICAL);

        // Titre = explication
        TextView titleView = new TextView(this);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleView.setLayoutParams(titleParams);
        titleView.setText(explanation);
        titleView.setTextColor(android.graphics.Color.WHITE);
        titleView.setTextSize(15);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setPadding(dpToPx(12), 0, dpToPx(12), dpToPx(10));

        // RecyclerView horizontal
        RecyclerView rv = new RecyclerView(this);
        LinearLayout.LayoutParams rvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(220));
        rv.setLayoutParams(rvParams);
        rv.setPadding(dpToPx(12), 0, dpToPx(12), 0);
        rv.setClipToPadding(false);
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        section.addView(titleView);
        section.addView(rv);
        container.addView(section);

        // Charger les 10 films depuis TMDB discover
        fetchRecommendationCategory(category, isTV, rv);
    }

    private void fetchRecommendationCategory(JSONObject category, boolean isTV, RecyclerView rv) {
        String base = isTV
                ? "https://api.themoviedb.org/3/discover/tv"
                : "https://api.themoviedb.org/3/discover/movie";

        StringBuilder url = new StringBuilder(base)
                .append("?api_key=").append(TMDB_API_KEY)
                .append("&language=fr-FR")
                .append("&sort_by=").append(category.optString("sort_by", "popularity.desc"))
                .append("&page=1");

        if (category.has("with_genres"))
            url.append("&with_genres=").append(category.optString("with_genres", "").replace(" ", ""));
        if (category.has("with_original_language"))
            url.append("&with_original_language=").append(category.optString("with_original_language"));
        if (category.has("vote_average_gte"))
            url.append("&vote_average.gte=").append(category.optDouble("vote_average_gte", 6.0));

        Log.d(TAG, "Reco category URL: " + url);

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url.toString(), null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        List<MyMovieData> items = new ArrayList<>();
                        for (int i = 0; i < Math.min(results.length(), 10); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            String title = isTV
                                    ? obj.optString("name", obj.optString("title", ""))
                                    : obj.optString("title", obj.optString("name", ""));
                            String date = isTV
                                    ? obj.optString("first_air_date", "")
                                    : obj.optString("release_date", "");
                            MyMovieData item = new MyMovieData(
                                    obj.getInt("id"), title, date, obj.optString("poster_path", ""));
                            item.setTV(isTV);
                            items.add(item);
                        }
                        ActionAdapter adapter = new ActionAdapter(items, this);
                        rv.setAdapter(adapter);
                    } catch (JSONException e) {
                        Log.e(TAG, "Reco category fetch error: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "Reco category fetch error: " + error.toString()));
        queue.add(req);
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
        profileView.setVisibility(View.GONE);
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

    // ─── Cinéma ──────────────────────────────────────────────────────────────

    private void initCinemaMap() {
        if (mapView != null) mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            getUserLocationAndSearchCinemas();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getUserLocationAndSearchCinemas() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        // Toujours demander une position fraîche — getLastLocation() retourne du cache (ex: San Francisco sur émulateur)
        com.google.android.gms.location.LocationRequest request =
                com.google.android.gms.location.LocationRequest.create()
                        .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setNumUpdates(1)
                        .setInterval(0)
                        .setFastestInterval(0);

        locationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult result) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
                android.location.Location loc = result.getLastLocation();
                if (loc != null) {
                    onLocationObtained(loc);
                } else {
                    Toast.makeText(MainActivity.this,
                            "Impossible d'obtenir la localisation. Vérifiez que le GPS est activé.",
                            Toast.LENGTH_LONG).show();
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Location error: " + e.getMessage());
        }
    }

    private void onLocationObtained(android.location.Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (googleMap != null) {
            // Zoom pays (6) pour voir tous les cinémas du pays
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 6f));
            googleMap.addMarker(new MarkerOptions()
                    .position(userLatLng)
                    .title("Vous êtes ici")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }
        fetchNearbyCinemas(location.getLatitude(), location.getLongitude());
    }

    private void fetchNearbyCinemas(double lat, double lng) {
        // Overpass API via POST — évite les limites de taille d'URL et les erreurs serveur
        final String query = "[out:json][timeout:30];"
                + "(node[\"amenity\"=\"cinema\"](around:300000," + lat + "," + lng + ");"
                + "way[\"amenity\"=\"cinema\"](around:300000," + lat + "," + lng + "););"
                + "out center;";

        com.android.volley.toolbox.StringRequest req = new com.android.volley.toolbox.StringRequest(
                Request.Method.POST,
                "https://overpass-api.de/api/interpreter",
                response -> parseCinemaResponse(response, lat, lng),
                error -> {
                    Log.e(TAG, "Cinema fetch error: " + error.toString());
                    // Essayer un miroir alternatif en cas d'échec
                    fetchCinemasFromMirror(query, lat, lng);
                }) {
            @Override
            public byte[] getBody() {
                String body = "data=" + Uri.encode(query);
                return body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };

        req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                30000, 0, com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(req);
    }

    private void fetchCinemasFromMirror(String query, double lat, double lng) {
        // Miroir de secours si le serveur principal échoue
        com.android.volley.toolbox.StringRequest req = new com.android.volley.toolbox.StringRequest(
                Request.Method.POST,
                "https://lz4.overpass-api.de/api/interpreter",
                response -> parseCinemaResponse(response, lat, lng),
                error -> {
                    Log.e(TAG, "Mirror error: " + error.toString());
                    Toast.makeText(this, "Impossible de charger les cinémas, réessayez plus tard.", Toast.LENGTH_LONG).show();
                }) {
            @Override
            public byte[] getBody() {
                String body = "data=" + Uri.encode(query);
                return body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };
        req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                30000, 0, com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(req);
    }

    private void parseCinemaResponse(String response, double lat, double lng) {
        try {
            JSONArray elements = new JSONObject(response).getJSONArray("elements");
            cinemaList.clear();
            googleMap.clear();

            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, lng))
                    .title("Vous êtes ici")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            for (int i = 0; i < elements.length(); i++) {
                JSONObject obj = elements.getJSONObject(i);
                JSONObject tags = obj.optJSONObject("tags");
                if (tags == null) continue;

                String name = tags.optString("name", "");
                if (name.isEmpty()) name = "Cinéma";

                double cLat, cLng;
                if ("way".equals(obj.optString("type"))) {
                    JSONObject center = obj.optJSONObject("center");
                    if (center == null) continue;
                    cLat = center.getDouble("lat");
                    cLng = center.getDouble("lon");
                } else {
                    if (!obj.has("lat") || !obj.has("lon")) continue;
                    cLat = obj.getDouble("lat");
                    cLng = obj.getDouble("lon");
                }

                String city = tags.optString("addr:city",
                        tags.optString("addr:town", tags.optString("addr:state", "")));
                cinemaList.add(new Cinema(name, city, cLat, cLng, 0));

                googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(cLat, cLng))
                        .title(name)
                        .snippet(city.isEmpty() ? null : city)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            }
            cinemaAdapter.notifyDataSetChanged();
            if (cinemaList.isEmpty()) {
                Toast.makeText(this, "Aucun cinéma trouvé dans ce pays", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Cinema parse error: " + e.getMessage());
            Toast.makeText(this, "Erreur de traitement des données", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (googleMap != null) {
                    try {
                        googleMap.setMyLocationEnabled(true);
                    } catch (SecurityException ignored) {}
                    getUserLocationAndSearchCinemas();
                }
            } else {
                Toast.makeText(this, "Permission de localisation refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
