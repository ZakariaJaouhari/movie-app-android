package com.example.moviesapp_jaouhari;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class movie_item_list extends AppCompatActivity {

    private static final String TMDB_API_KEY = "ce601ad5b7a468bf65223a10b811735b";
    private static final String TAG = "GenreListActivity";

    private GenreGridAdapter adapter;
    private final List<MyMovieData> items = new ArrayList<>();
    private RequestQueue queue;
    private GridLayoutManager layoutManager;

    private int genreId;
    private boolean isTV;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_item_list);

        genreId = getIntent().getIntExtra("genreId", -1);
        String genreName = getIntent().getStringExtra("genreName");
        isTV = getIntent().getBooleanExtra("isTV", false);

        // Header wiring
        ImageView backButton = findViewById(R.id.backButton);
        ImageView searchIcon = findViewById(R.id.imageSearchIcon);
        ImageView logoutIcon = findViewById(R.id.logoutIcon);
        TextView genreTitle = findViewById(R.id.genreTitle);

        genreTitle.setText(genreName != null ? genreName : "");
        backButton.setOnClickListener(v -> finish());
        searchIcon.setOnClickListener(v -> {
            // Search not active on this page
        });
        logoutIcon.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        RecyclerView recyclerView = findViewById(R.id.genreListRecyclerView);
        layoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new GenreGridAdapter(items, this, isTV);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (!isLoading && hasMore
                        && layoutManager.findLastVisibleItemPosition() >= layoutManager.getItemCount() - 9) {
                    currentPage++;
                    fetchPage(currentPage);
                }
            }
        });

        queue = Volley.newRequestQueue(this);
        fetchPage(1);
    }

    private void fetchPage(int page) {
        if (isLoading || !hasMore) return;
        isLoading = true;

        String baseUrl = isTV
                ? "https://api.themoviedb.org/3/discover/tv"
                : "https://api.themoviedb.org/3/discover/movie";
        String url = baseUrl + "?api_key=" + TMDB_API_KEY
                + "&with_genres=" + genreId
                + "&sort_by=popularity.desc"
                + "&language=fr-FR"
                + "&page=" + page;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() == 0) hasMore = false;
                        List<MyMovieData> newItems = new ArrayList<>();
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
                            newItems.add(item);
                        }
                        adapter.addItems(newItems);
                        isLoading = false;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error: " + e.getMessage());
                        isLoading = false;
                    }
                }, error -> {
            Log.e(TAG, "Error: " + error.getMessage());
            isLoading = false;
        });
        queue.add(req);
    }
}
