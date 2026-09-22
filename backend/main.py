from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.orm import Session
import database
from pydantic import BaseModel
from typing import List, Optional
import os
from dotenv import load_dotenv
import openai

load_dotenv()
openai.api_key = os.getenv("OPENAI_API_KEY")

app = FastAPI(title="LingoFlix Backend")

# ... (keep existing init and get_db) ...

@app.post("/translate")
async def translate_word(req: TranslationRequest):
    if not openai.api_key:
        return {"word": req.word, "translation": "API Key missing", "explanation": "Please set OPENAI_API_KEY in .env"}

    prompt = f"""
    Translate the word '{req.word}' from the sentence: '{req.sentence}'.
    Provide:
    1. Direct translation to Hebrew.
    2. A brief explanation of why this word is used in this context.
    3. An alternative word that could fit.
    Format your response as JSON: {{"translation": "...", "explanation": "...", "alternative": "..."}}
    """

    try:
        response = openai.ChatCompletion.create(
            model="gpt-3.5-turbo",
            messages=[{"role": "user", "content": prompt}]
        )
        import json
        return json.loads(response.choices[0].message.content)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/stats/difficult-words")
def get_difficult_words(limit: int = 10, db: Session = Depends(get_db)):
    # Return words with high failure rate
    return db.query(database.WordStats).filter(database.WordStats.wrong_count > 0)\
        .order_by(database.WordStats.wrong_count.desc()).limit(limit).all()

@app.post("/activity")
def update_activity(update: ActivityUpdate, db: Session = Depends(get_db)):
    word_stat = db.query(database.WordStats).filter(database.WordStats.word == update.word).first()

    if not word_stat:
        word_stat = database.WordStats(word=update.word)
        db.add(word_stat)

    if update.is_correct:
        word_stat.correct_count += 1
    else:
        word_stat.wrong_count += 1

    db.commit()
    return {"status": "success", "word": update.word, "score": word_stat.correct_count}

@app.get("/health")
def health_check():
    return {"status": "online", "version": "1.4", "server_name": "LingoServer-Home"}

@app.get("/check-update")
def check_update():
    # In a real scenario, this would check a version file
    return {
        "latest_version": "1.5",
        "update_url": "http://your-pi-ip:8000/download/latest.apk",
        "release_notes": "שיפורי מהירות ויכולות AI חדשות"
    }

@app.get("/movies/info/{movie_name}")
async def get_movie_pro_info(movie_name: str):
    # This will eventually query your local DB or TMDB
    return {
        "title": movie_name,
        "cast": ["Actor 1", "Actor 2"],
        "description": "מידע מפורט שהגיע מהשרת המרוחק שלך...",
        "rating": 8.5
    }

@app.get("/movies/library")
async def get_private_library():
    # This is the "Full Database" from your Raspberry Pi
    return [
        {
            "id": "1",
            "title": "חברים (Friends)",
            "description": "הסדרה הקלאסית ללימוד אנגלית",
            "image_url": "https://image.tmdb.org/t/p/w500/f496p9xyvIC3oDI1p3I996A9YwM.jpg",
            "category": "TV Show",
            "difficulty": "קל"
        },
        {
            "id": "2",
            "title": "המטריקס",
            "description": "סרט אקשן עם דיבור ברור ומדויק",
            "image_url": "https://image.tmdb.org/t/p/w500/f89U3Y9SJuCYFJj7v0qy1795GvI.jpg",
            "category": "Movie",
            "difficulty": "בינוני"
        }
    ]

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
