package com.example.moviesapp_jaouhari;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MoodScanActivity extends AppCompatActivity {

    private static final String BACKEND_URL = "http://localhost:8000";
    private static final int CAMERA_REQUEST = 3001;
    private static final int CAMERA_PERMISSION_REQUEST = 3002;

    private LinearLayout stateInitial, stateLoading, stateResult;
    private TextView loadingText, moodEmoji, moodLabel, moodMessage;
    private ImageView selfiePreview;
    private RecyclerView moodFilmsRecyclerView;
    private Button btnTakeSelfie, btnRetry;

    private RequestQueue queue;
    private Bitmap currentSelfie;
    private final List<MyMovieData> moodFilms = new ArrayList<>();
    private ActionAdapter filmsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_scan);

        queue = Volley.newRequestQueue(this);

        bindViews();
        setupRecyclerView();
        setupListeners();
    }

    private void bindViews() {
        stateInitial = findViewById(R.id.stateInitial);
        stateLoading = findViewById(R.id.stateLoading);
        stateResult  = findViewById(R.id.stateResult);
        loadingText  = findViewById(R.id.loadingText);
        moodEmoji    = findViewById(R.id.moodEmoji);
        moodLabel    = findViewById(R.id.moodLabel);
        moodMessage  = findViewById(R.id.moodMessage);
        selfiePreview= findViewById(R.id.selfiePreview);
        moodFilmsRecyclerView = findViewById(R.id.moodFilmsRecyclerView);
        btnTakeSelfie= findViewById(R.id.btnTakeSelfie);
        btnRetry     = findViewById(R.id.btnRetry);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        moodFilmsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        filmsAdapter = new ActionAdapter(moodFilms, this);
        moodFilmsRecyclerView.setAdapter(filmsAdapter);
    }

    private void setupListeners() {
        btnTakeSelfie.setOnClickListener(v -> openCamera());
        btnRetry.setOnClickListener(v -> openCamera());
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(intent, CAMERA_REQUEST);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Caméra non disponible", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK && data != null) {
            currentSelfie = (Bitmap) data.getExtras().get("data");
            if (currentSelfie != null) {
                selfiePreview.setImageBitmap(currentSelfie);
                analyzeMood(currentSelfie);
            }
        }
    }

    private void analyzeMood(Bitmap bitmap) {
        showState(stateLoading);
        loadingText.setText("Analyse de ton humeur...");

        // Redimensionner à 224x224 (taille d'entrée du modèle ViT) et compresser
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

        try {
            JSONObject body = new JSONObject();
            body.put("image", base64Image);

            JsonObjectRequest req = new JsonObjectRequest(
                    com.android.volley.Request.Method.POST,
                    BACKEND_URL + "/analyze-mood",
                    body,
                    response -> handleMoodResponse(response),
                    error -> {
                        showState(stateInitial);
                        String msg = "Impossible de joindre le backend.";
                        if (error.networkResponse != null) {
                            msg += " Code: " + error.networkResponse.statusCode;
                        } else if (error instanceof com.android.volley.TimeoutError) {
                            msg = "Délai dépassé (modèle trop lent). Réessaie.";
                        } else if (error instanceof com.android.volley.NoConnectionError) {
                            msg = "Pas de connexion au backend (" + BACKEND_URL + ")";
                        }
                        android.util.Log.e("MoodScan", "Volley error: " + error.getClass().getSimpleName() + " - " + msg, error);
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    });

            req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    120000, 0,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(req);

        } catch (JSONException e) {
            showState(stateInitial);
            Toast.makeText(this, "Erreur d'encodage de l'image", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleMoodResponse(JSONObject response) {
        try {
            String mood    = response.getString("mood");
            String emoji   = response.getString("emoji");
            String message = response.getString("message");
            JSONArray films = response.getJSONArray("films");

            // Afficher mood
            moodEmoji.setText(emoji);
            moodLabel.setText(mood.substring(0, 1).toUpperCase() + mood.substring(1));
            moodMessage.setText(message);

            // Remplir la liste de films
            moodFilms.clear();
            for (int i = 0; i < films.length(); i++) {
                JSONObject f = films.getJSONObject(i);
                moodFilms.add(new MyMovieData(
                        f.getInt("id"),
                        f.getString("title"),
                        "",
                        f.optString("poster_path", "")));
            }
            filmsAdapter.notifyDataSetChanged();

            showState(stateResult);

        } catch (JSONException e) {
            showState(stateInitial);
            Toast.makeText(this, "Erreur de traitement de la réponse", Toast.LENGTH_SHORT).show();
        }
    }

    private void showState(LinearLayout target) {
        stateInitial.setVisibility(View.GONE);
        stateLoading.setVisibility(View.GONE);
        stateResult.setVisibility(View.GONE);
        target.setVisibility(View.VISIBLE);
    }
}
