# LingoCoach — Complete System Guide
> Full implementation reference for the Android app (`D:\work\LingoCoach`) and the FastAPI backend (`D:\work\LingoAI_backend`).
> Written after full source analysis of both codebases. Keep this file up to date as features are added.

---

## 1. System Architecture

```
Android App (Kotlin / Jetpack Compose)
   │  OkHttp REST  ·  Base URL: https://lingoai-backend-zej0.onrender.com
   ▼
FastAPI Backend (Python 3.13 · Uvicorn)
   ├── Google Cloud Firestore   ← all persistent data
   ├── Groq Whisper             ← voice transcription
   └── HuggingFace / Groq LLM  ← Llama-3.1-8B (eval, chat, learning path)
```

| Layer | Technology |
|---|---|
| Android | Kotlin, Jetpack Compose, Material3, OkHttp 4, Gson |
| Push | OneSignal 5.1.20 |
| Backend | FastAPI + Uvicorn |
| Database | Google Cloud Firestore |
| LLM primary | HuggingFace Router → `meta-llama/Llama-3.1-8B-Instruct` |
| LLM fallback | Groq → `llama-3.1-8b-instant` |
| Voice STT | Groq Whisper `whisper-large-v3` |
| Vocab data | JSON assets bundled in APK (A1–C2, ~1 000+ words) |
| Local cache | `vocab_progress.json` + `vocab_mistakes.json` in `filesDir` |

---

## 2. Onboarding & Navigation Flow

```
App launch
 └─ SplashScreen (2 s)
     ├─ [new user] LanguageSelectionScreen
     │   └─ WelcomeAboardScreen (3-slide carousel)
     │       └─ UserProfileSetupScreen  (4 steps)
     │           Step 1: What's your name?           → saves display_name
     │           Step 2: What is your goal?          → saves user_goal
     │           Step 3: What is your current level? → saves user_level
     │           Step 4: Speaking Assessment intro   → "Start Assessment"
     │               └─ AssessmentScreen (5 questions, voice or text)
     │                   └─ AssessmentResultScreen
     │                       └─ LearningPathScreen
     │                           └─ HomeScreen
     └─ [returning user] HomeScreen  (direct)
```

### SharedPreferences flags (key `"LingoCoachPrefs"`)
| Key | Type | Meaning |
|---|---|---|
| `lang_selected` | Boolean | Language chosen |
| `onboarding_completed` | Boolean | WelcomeAboard seen |
| `profile_setup_done` | Boolean | UserProfileSetup finished |
| `assessment_completed` | Boolean | Assessment + learning path done |
| `display_name` | String | User's display name |
| `session_id` | String | **Permanent user ID** (UUID from assessment session) |
| `user_goal` | String | Goal selected in setup (e.g. "business") |
| `user_level` | String | Self-reported level (e.g. "intermediate") |
| `target_fluency` | String | Target fluency level (settings) |
| `native_language` | String | Native language (settings) |
| `voice_profile` | String | AI tutor voice (settings) |
| `daily_reminder` | Boolean | Push notification toggle |
| `vocab_last_sync` | String | ISO timestamp of last vocab sync |
| `cache_current_path_json` | String | Cached `CurrentLearningPathResponse` JSON |
| `cache_current_path_at` | Long | Timestamp of last path cache |
| `selected_coach` | String | Coach voice name |
| `learning_path_json` | String | Initial `LearningPathResponse` from generation |
| `assessment_response_json` | String | Full `AssessmentResponse` JSON |

---

## 3. User Identity — session_id as user_id

1. Before assessment, user enters their name in `UserProfileSetupScreen` step 1 → saved to `display_name`.
2. `POST /api/v1/sessions { "user_name": "<name>" }` → backend generates a UUID, stores it in `assessment_sessions` Firestore doc with the name.
3. On success, Android saves the UUID to `SharedPreferences("session_id")`.
4. **This UUID is the permanent `user_id` for every API call across the entire app** — learning path, AI Lab, vocab sync, mistakes, flashcards, analytics.
5. When the learning path is generated (`POST /api/v1/learning-path`), the backend writes `users/{session_id}` with `display_name`, tier, xp, streak, weak_areas, etc.
6. Logout clears all flags → next launch runs full onboarding again and generates a new session UUID.

---

## 4. Assessment Flow

### API sequence
```
POST /api/v1/sessions { "user_name": "Sarah" }
  → { session_id, current_step: 1, next_question, user_name }

POST /api/v1/assess { session_id, user_answer }        (× 5 text rounds)
POST /api/v1/assess/voice (multipart: session_id + audio mp4) (× 5 voice rounds)
  → AssessmentResponse each round
  → On step 5: assessment_complete: true, assigned_tier, grammar_score, vocabulary_score, coherence_score

POST /api/v1/learning-path { session_id, user_name, tier, scores, goal, level }
  → LearningPathResponse + seeds user_curriculum in Firestore
```

### Assessment questions (5 steps, A1 → C1)
| Step | Level | Topic |
|---|---|---|
| 1 | A1 | Name, origin, hobby |
| 2 | A2 | Typical day |
| 3 | B1 | Recent event |
| 4 | B2 | Hypothetical city tour |
| 5 | C1 | Opinion on language technology |

### Tier assignment
| Score | Tier |
|---|---|
| ≤ 45 or early structural break | BEGINNER |
| 46–78 | INTERMEDIATE |
| ≥ 79, no structural breaks | ADVANCED |

### Assessment result UI
- Arc gauge (score/100)
- CEFR tier pill
- Grade text + completion timestamp
- 3 Strength cards: Grammar, Coherence, Fluency (each with score bar)
- Radar chart: Grammar, Vocabulary, Pronunciation, Fluency, Coherence
- 2 Areas to Improve cards (lowest scores) with tips
- Recommended Actions card from `recommended_focus`
- "Continue to Lessons" CTA

---

## 5. Learning Path (Core Feature)

### Generation
```
POST /api/v1/learning-path
  → LLM generates personalised modules for user's tier + goal + level
  → Writes to Firestore:
      users/{user_id}                              ← profile
      user_curriculum/{user_id}/modules/{id}       ← per-user curriculum
      user_curriculum/{user_id}/lessons/{id}
      user_curriculum/{user_id}/sublessons/{id}
  → Returns LearningPathResponse (flat summary for initial display)
```

### Live progress
```
GET /api/v1/learning-path/current?user_id={id}
  → Queries user_curriculum/{user_id}/... + user_progress/{user_id}
  → Returns CurrentLearningPathResponse with computed statuses:
      "completed" | "current" | "locked"
```

### Tier → starting module
| Tier | Module 1 | Module 2 | Module 3 | Module 4 |
|---|---|---|---|---|
| BEGINNER | current | locked | locked | locked |
| INTERMEDIATE | completed | current | locked | locked |
| ADVANCED | completed | completed | current | locked |

### Caching (Android)
- `AppCache.learningPath` — in-memory, TTL 5 min
- `SharedPreferences("cache_current_path_json")` — disk fallback
- Load from disk on screen open → show immediately → refresh if stale in background
- Invalidated after: exercise complete, lesson complete, XP award

### Lesson structure
```
Module
  └─ Lesson (xp_reward: 20–50)
      └─ Sublesson
          ├─ content_blocks: [explanation, example, tip]
          └─ exercises: [multiple_choice, fill_in_the_blank, pronunciation, free_speech]
```

### Exercise completion
```
POST /api/v1/exercise/complete { user_id, sublesson_id, exercise_id, user_answer }
  → Evaluates answer (exact match for MC/FITB, overlap for pronunciation)
  → Awards XP, updates streak
  → If wrong: logs mistake + creates SRS flashcard
  → Triggers adaptive remediation check (background)
  → Updates daily_stats
```

---

## 6. AI Lab

### Session flow
```
GET  /api/v1/ailab/status?user_id=     → { sessions_used_today, sessions_limit: 3, bonus_sessions, sessions_remaining }
POST /api/v1/ailab/start { user_id, topic, voice_gender, tone }
  → Checks daily limit (3 + bonus)
  → Generates opening message via LLM
  → Returns { session_id, opening_message }
POST /api/v1/ailab/chat (multipart: user_id, session_id, message? | audio_file?)
  → Grammar checks user message
  → Generates AI response
  → Returns { ai_response, mistakes: [{ wrong, correct, explanation, mistake_type }] }
POST /api/v1/ailab/end { session_id }
  → Summarises conversation
  → Returns { vocabulary_learned, grammar_mistakes, strengths, weaknesses }
```

### Daily limit rules
- Default: **3 sessions/day** per user (resets at UTC midnight)
- Bonus: earning **≥ 100 XP in a day** grants **+1 free session** (once per day, non-transferable)
- XP is only awarded, never withdrawn

### Conversation flow
- AI sends the first message immediately after session starts (no waiting for user)
- AI responds conversationally and asks follow-up questions each turn
- Mistakes detected inline → shown as correction chips under user bubble

---

## 7. Vocab Builder

### Data source
- JSON assets bundled in APK: `assets/vocab-builder/{a1,a2,b1,b2,c1_c2}/*.json`
- Loaded into memory by `VocabTracker.init(context)` at first use
- ~1 000+ words across A1–C2 levels

### Mastery rules
| Score | Status | Drill behaviour |
|---|---|---|
| 0–79 | Learning | Appears in drills |
| 80–99 | Mastered | Excluded from new sessions |
| 100 | Fully mastered | Excluded from all drills |

- Correct answer: `+20` score
- Wrong answer: `-10` score, logs mistake to vault
- Score clamped 0–100

### Backend sync
```
POST /api/v1/vocab/sync?user_id=   { client_time, changes: [{word, mastery_score, is_bookmarked, updated_at}] }
  → Last-write-wins merge into user_vocab_progress Firestore collection
GET  /api/v1/vocab/sync?user_id=&last_sync_time=
  → Returns updates since last_sync_time
```
Sync fires on `VocabBuilderScreen` open (background). Last sync time stored in SharedPreferences.

---

## 8. Timely Duel

### Gameplay
- 8 questions per session, 4 question types: Fill-in-blank (MC), Spelling, Pronunciation, Sentence
- 4 difficulty levels: Beginner (30s), Intermediate (20s), Advanced (15s), Master (10s)
- Countdown timer per question — timeout = wrong

### XP rules
| Difficulty | Right | Wrong |
|---|---|---|
| Beginner | +5 | -2 |
| Intermediate | +10 | -5 |
| Advanced | +20 | -10 |
| Master | +50 | -25 |

### Backend tracking
```
POST /api/v1/progress/xp { user_id, xp_delta, source: "timely_duel" }
  → Updates users/{id}.xp + xp_earned_today
  → If xp_earned_today first crosses 100 today → bonus_ailab_sessions_today = 1
POST /api/v1/mistakes  (wrong answers)
  → Creates SRS flashcard for each missed word
```

---

## 9. Mistake Vault

### Sources
| Feature | mistake_type |
|---|---|
| Learning Path exercise | grammar / tense / articles / pronunciation |
| AI Lab conversation | grammar / pronunciation / vocabulary |
| Vocab Builder drill | VOCAB_DRILL |
| Timely Duel | TIMELY_DUEL |
| Flashcard retest (Again) | FLASHCARD_RETEST |

### Backend schema (`mistakes` collection)
```json
{
  "id":               "md5_hash",
  "user_id":          "uuid",
  "word":             "negotiate",
  "mistake_type":     "grammar",
  "user_sentence":    "I negotiate a deal yesterday",
  "correct_sentence": "I negotiated a deal yesterday",
  "explanation":      "Past simple required",
  "times_missed":     3,
  "mastered":         false,
  "mastery_score":    0,
  "created_at":       "ISO",
  "last_reviewed":    null
}
```

### Retry / relearn
- Smart Review session — mic-based retest, "Got It" / "Again" marking
- Flashcards — SRS deck auto-created for every mistake

---

## 10. Flashcards (SRS)

```
GET  /api/v1/flashcards/review?user_id=    → due cards (next_review <= now)
POST /api/v1/flashcards/review { card_id, rating }   → SM-2 update
```
Rating scale: `0` = Again, `2` = Hard, `4` = Good, `5` = Easy (SM-2 algorithm).
Cards created automatically when mistakes are logged. Mastery of the underlying vocab/mistake updated on each review.

---

## 11. Daily Analytics

### Firestore collection `daily_stats`
Document ID: `{user_id}_{YYYY-MM-DD}`
```json
{
  "user_id":              "uuid",
  "date":                 "2025-06-16",
  "lessons_completed":    2,
  "exercises_attempted":  10,
  "exercises_correct":    8,
  "mistakes_logged":      3,
  "vocab_drills_done":    1,
  "vocab_words_mastered": 5,
  "ai_lab_sessions":      1,
  "ai_lab_minutes":       12,
  "duel_sessions":        2,
  "duel_correct":         14,
  "xp_earned":            85,
  "streak_day":           7
}
```

### API
```
GET /api/v1/analytics/weekly?user_id=
  → Returns last 7 daily_stats docs, missing days filled with zeros
```

### Android usage
- `HomeScreen` fetches weekly analytics and drives the bar chart with real `xp_earned` values
- "Mistakes logged today" and "Lessons done this week" counters are real data
- `update_daily_stats(db, user_id, field, increment)` is called from: `lesson_manager`, `mistake_service`, `ailab_service`, `progress/xp` endpoint

---

## 12. Caching Strategy (Android)

```kotlin
object AppCache {
    var learningPath: CurrentLearningPathResponse?  // TTL 5 min
    var mistakes: List<Mistake>?                    // TTL 10 min
    // weeklyStats fetched fresh each HomeScreen visit (lightweight)
    fun invalidateLearningPath()
    fun invalidateMistakes()
    fun invalidateAll()
    fun loadFromDisk(context)   // reads SharedPrefs JSON
    fun saveToDisk(context)     // writes SharedPrefs JSON
}
```

**Pattern:** Load from disk immediately → show stale data → refresh in background if TTL expired.

**Invalidation triggers:**
- Exercise completed → `invalidateLearningPath()`
- Mistake logged → `invalidateMistakes()`
- AI Lab session ended → `invalidateLearningPath()`
- Duel finished → `invalidateLearningPath()`

---

## 13. Firestore Collections

| Collection | Doc ID | Purpose |
|---|---|---|
| `assessment_sessions` | UUID | Assessment in-progress state |
| `users` | `{user_id}` | Profile, tier, XP, streak, weak_areas, bonus_ailab |
| `user_progress` | `{user_id}` | completed_sublessons/lessons/modules, injected remediations |
| `user_curriculum/{user_id}/modules` | module_id | Per-user personalised modules |
| `user_curriculum/{user_id}/lessons` | lesson_id | Per-user personalised lessons |
| `user_curriculum/{user_id}/sublessons` | sub_id | Per-user sublessons with content + exercises |
| `curriculum_modules` | mod_id | Shared default modules (fallback) |
| `curriculum_lessons` | les_id | Shared default lessons |
| `curriculum_sublessons` | sub_id | Shared default sublessons |
| `user_vocab_progress` | `{user_id}_{word}` | Word mastery + bookmark per user |
| `vocab_bookmarks` | `{user_id}_{word}` | Bookmarked words with definition + IPA |
| `mistakes` | MD5 hash | All mistakes from all features |
| `flashcards` | `{user_id}_{type}_{ref}` | SRS cards for vocab + mistakes |
| `ai_lab_sessions` | UUID | AI Lab conversation history + summary |
| `daily_stats` | `{user_id}_{YYYY-MM-DD}` | Per-day activity counters |

---

## 14. Complete API Reference

**Base URL:** `https://lingoai-backend-zej0.onrender.com`

### Assessment
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/sessions` | Create session `{ user_name }` → `{ session_id, next_question, user_name }` |
| `POST` | `/api/v1/assess` | Text answer `{ session_id, user_answer }` → AssessmentResponse |
| `POST` | `/api/v1/assess/voice` | Voice answer (multipart) → AssessmentResponse |

### Learning Path
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/learning-path` | Generate path from scores → LearningPathResponse |
| `GET` | `/api/v1/learning-path/current?user_id=` | Live progress → CurrentLearningPathResponse |

### Lessons
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/sublessons/{id}` | Sublesson detail with exercises |
| `POST` | `/api/v1/exercise/complete` | Submit answer → XP + feedback |

### Mistakes
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/mistakes?user_id=` | All mistakes for user |
| `POST` | `/api/v1/mistakes` | Direct mistake log `{ user_id, word, mistake_type, ... }` |
| `POST` | `/api/v1/mistakes/analyze` | AI analysis of free-form text |

### Vocab
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/vocab/bookmarks?user_id=` | Get bookmarked words |
| `POST` | `/api/v1/vocab/bookmarks` | Bookmark a word (AI enriches with IPA + definition) |
| `POST` | `/api/v1/vocab/sync?user_id=` | Push local progress |
| `GET` | `/api/v1/vocab/sync?user_id=&last_sync_time=` | Pull server updates |

### Flashcards
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/flashcards/review?user_id=` | Due SRS cards |
| `POST` | `/api/v1/flashcards/review?user_id=` | Submit rating (0/2/4/5) |

### Progress
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/progress?user_id=` | User state (tier, XP, streak) |
| `POST` | `/api/v1/progress/update` | Manual tier/XP/streak override |
| `POST` | `/api/v1/progress/xp` | Award XP `{ user_id, xp_delta, source }` |
| `PATCH` | `/api/v1/progress/users/{user_id}` | Update profile fields from Settings |

### AI Lab
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/ailab/status?user_id=` | Daily limit status |
| `POST` | `/api/v1/ailab/start` | Start session → `{ session_id, opening_message }` |
| `POST` | `/api/v1/ailab/chat` | Send message/audio → `{ ai_response, mistakes }` |
| `POST` | `/api/v1/ailab/transcribe` | Audio → text only |
| `POST` | `/api/v1/ailab/end` | End session → summary |

### Analytics
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/analytics/weekly?user_id=` | Last 7 days daily stats |

### Health
| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | Returns `{ "status": "ok" }` |

---

## 15. Backend Architecture

```
D:\work\LingoAI_backend\
├── app/
│   ├── main.py              ← FastAPI app + router registration
│   ├── config.py            ← Settings (HF_TOKEN, GROQ_API_KEY, Firestore creds)
│   ├── api/
│   │   ├── assessment.py    ← /sessions, /assess, /assess/voice
│   │   ├── aiLearningPath.py← /learning-path (POST + GET current)
│   │   ├── lessons.py       ← /sublessons, /exercise/complete
│   │   ├── mistakes.py      ← /mistakes
│   │   ├── vocab.py         ← /vocab/bookmarks, /vocab/sync
│   │   ├── flashcards.py    ← /flashcards/review
│   │   ├── progress.py      ← /progress, /progress/xp, /progress/users/{id}
│   │   ├── ailab.py         ← /ailab/*
│   │   └── analytics.py     ← /analytics/weekly
│   ├── services/
│   │   ├── ailab_service.py     ← Session CRUD, daily limit, opening message
│   │   ├── adaptive_engine.py   ← LLM-generated remediation lesson injection
│   │   ├── evaluator.py         ← Local score fallback
│   │   ├── groq_client.py       ← Whisper STT via Groq
│   │   ├── hf_client.py         ← LLM calls (HuggingFace primary, Groq fallback)
│   │   ├── lesson_manager.py    ← Curriculum CRUD, per-user seed, XP/streak
│   │   ├── mistake_service.py   ← LLM mistake detection, SRS card creation
│   │   ├── prompt_builder.py    ← Assessment prompts
│   │   ├── scorer.py            ← Tier calculation from scores
│   │   ├── srs_engine.py        ← SM-2 flashcard algorithm
│   │   └── vocab_service.py     ← Bookmark + vocab progress sync
│   ├── database/
│   │   ├── db.py            ← Firestore client factory
│   │   └── models.py        ← AssessmentSession, UserVocabProgress dataclasses
│   ├── schemas/             ← Pydantic request/response models
│   └── constants/
│       └── questions.py     ← 5 assessment questions (A1–C1)
└── requirements.txt
```

### LLM routing
1. If `HF_TOKEN` set → HuggingFace Router (`meta-llama/Llama-3.1-8B-Instruct`, JSON mode)
2. Fallback → Groq (`llama-3.1-8b-instant`, JSON mode)
3. Fallback → local heuristic evaluator (assessment scoring only)

---

## 16. Environment Variables (`.env`)

```env
# LLM
HF_TOKEN=hf_...                     # HuggingFace API token
HF_MODEL=meta-llama/Llama-3.1-8B-Instruct
GROQ_API_KEY=gsk_...               # Groq API key (LLM + Whisper)
GROQ_AUDIO_URL=https://api.groq.com/openai/v1/audio/transcriptions

# Firestore
FIRESTORE_CREDENTIALS_JSON={"type":"service_account",...}  # inline JSON (Render)
# OR
FIRESTORE_CREDENTIALS_PATH=/path/to/key.json               # local dev
FIRESTORE_PROJECT_ID=lingocoach-xxxxx
FIRESTORE_DATABASE_ID=default

# Flags
USE_LOCAL_EVALUATOR=false           # set true for offline dev/testing
```

---

## 17. Deployment (Render)

1. Root dir: `D:\work\LingoAI_backend`
2. Build command: `pip install -r requirements.txt`
3. Start command: `uvicorn app.main:app --host 0.0.0.0 --port $PORT`
4. Set all env vars listed above in Render dashboard
5. Free tier spins down after 15 min inactivity → first request ~30 s cold start

---

## 18. Known Issues / Critical Bugs Fixed

| # | Bug | Fix applied |
|---|---|---|
| 1 | `"test_user_123"` hardcoded as user_id in AI Lab | T-02: reads real `session_id` from SharedPrefs |
| 2 | Display name never sent to backend | T-01: `POST /sessions { user_name }` |
| 3 | HomeScreen greeted "Alex" always | T-01/T-16: real name + time-of-day greeting |
| 4 | All users got the same generic curriculum | T-04: per-user `seed_user_curriculum` based on tier |
| 5 | Learning path caused spinner on every navigation | T-05: AppCache with 5-min TTL |
| 6 | AI Lab had no daily limit | T-08: 3/day + 100 XP bonus unlock |
| 7 | AI waited for user to speak first | T-07: opening message generated on session start |
| 8 | Mastered vocab words reappeared in drills | T-09: filter score ≥ 80 from drill candidates |
| 9 | Duel XP never saved to Firestore | T-10: `POST /api/v1/progress/xp` |
| 10 | Duel/Vocab/Flashcard mistakes never reached backend | T-11/T-12: logMistake fired from all 5 sources |
| 11 | HomeScreen bar chart 100% hardcoded | T-13: real weekly analytics from Firestore |
| 12 | Settings changes never persisted to Firestore | T-15: `PATCH /api/v1/progress/users/{id}` |
| 13 | user_goal/user_level ignored in learning path | T-17: included in LLM prompt |
