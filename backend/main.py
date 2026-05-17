from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import base64
import io
import requests
from PIL import Image
from transformers import pipeline

app = FastAPI(title="MoviesApp Mood API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

TMDB_KEY = "ce601ad5b7a468bf65223a10b811735b"

# Chargement du modèle au démarrage (une seule fois)
print("Chargement du modèle d'émotions faciales...")
emotion_classifier = pipeline(
    "image-classification",
    model="trpakov/vit-face-expression",
    top_k=7,
)
print("Modèle prêt !")

MOOD_CONFIG = {
    "happy": {
        "genres": "28,12,35",
        "sort_by": "popularity.desc",
        "message": "T'as l'air en forme ! Voici des films pour en profiter",
        "emoji": "😄",
    },
    "sad": {
        "genres": "35,10749,18",
        "sort_by": "vote_average.desc",
        "message": "Un peu melancolique ? Ces films vont te remonter le moral",
        "emoji": "😢",
    },
    "angry": {
        "genres": "28,53,80",
        "sort_by": "popularity.desc",
        "message": "Energie a revendre ! Ces films sont faits pour toi",
        "emoji": "😤",
    },
    "fear": {
        "genres": "35,16,14",
        "sort_by": "popularity.desc",
        "message": "Besoin de legerete ? Voici des films feel-good",
        "emoji": "😨",
    },
    "neutral": {
        "genres": "878,9648,53",
        "sort_by": "vote_average.desc",
        "message": "D'humeur neutre ? Quelque chose d'intrigant s'impose",
        "emoji": "😐",
    },
    "surprise": {
        "genres": "9648,878,14",
        "sort_by": "popularity.desc",
        "message": "Curieux(se) ? Ces films vont t'epater",
        "emoji": "😲",
    },
    "disgust": {
        "genres": "35,16,10751",
        "sort_by": "vote_average.desc",
        "message": "Besoin de bonne humeur ? Ces films vont te changer les idees",
        "emoji": "😊",
    },
}


class ImageRequest(BaseModel):
    image: str  # base64


@app.get("/health")
def health():
    return {"status": "ok", "model": "trpakov/vit-face-expression"}


@app.post("/analyze-mood")
async def analyze_mood(request: ImageRequest):
    # 1. Décoder l'image base64
    image_bytes = base64.b64decode(request.image)
    img = Image.open(io.BytesIO(image_bytes)).convert("RGB")

    # 2. Détecter l'émotion avec le modèle ViT
    dominant = "neutral"
    scores = {}
    try:
        results = emotion_classifier(img)
        dominant = results[0]["label"].lower()
        scores = {r["label"].lower(): round(r["score"] * 100, 1) for r in results}
    except Exception as e:
        print(f"Emotion detection error: {e}")

    # 3. Config humeur → genres TMDB
    config = MOOD_CONFIG.get(dominant, MOOD_CONFIG["neutral"])

    # 4. Films depuis TMDB
    tmdb_url = (
        f"https://api.themoviedb.org/3/discover/movie"
        f"?api_key={TMDB_KEY}"
        f"&with_genres={config['genres']}"
        f"&sort_by={config['sort_by']}"
        f"&vote_count.gte=300"
        f"&language=fr-FR"
    )
    tmdb_res = requests.get(tmdb_url, timeout=10)
    films_raw = tmdb_res.json().get("results", [])[:10]

    films = [
        {
            "id": f["id"],
            "title": f["title"],
            "poster_path": f.get("poster_path", ""),
            "vote_average": f.get("vote_average", 0),
            "overview": f.get("overview", ""),
        }
        for f in films_raw
    ]

    return {
        "mood": dominant,
        "emoji": config["emoji"],
        "message": config["message"],
        "emotion_scores": scores,
        "films": films,
    }
