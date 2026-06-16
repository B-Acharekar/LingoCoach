# LingoCoach — Implementation Task List
> Full scope covering Android app + FastAPI backend.
> Work through ONE task at a time. Do not start the next until the current is verified.
> Status: ⬜ todo | 🔄 in progress | ✅ done

---

## PHASE 1 — Identity & Onboarding (Foundation for everything else)

### T-01 ⬜ Name → Session ID → Permanent User ID
**Why first:** Every other feature (AI Lab, Learning Path, Mistakes, Analytics) uses `user_id`. Right now it's a random UUID the user never sees, and the display name is never tied to it.

**Full scope:**

**Backend (`D:\work\LingoAI_backend`):**
- `app/api/assessment.py` → `POST /api/v1/sessions`: accept optional body `{ "user_name": "string" }` instead of `{}`. Store `user_name` in the `assessment_sessions` Firestore document.
- `app/database/models.py` → Add `user_name: str = ""` field to `AssessmentSession` dataclass.
- `app/api/aiLearningPath.py` → When writing the `users/{session_id}` doc during learning path generation, include `display_name` from the session doc.

**Android (`D:\work\LingoCoach`):**
- `network/AssessmentApi.kt` → `createSession(userName: String, onResult)` — send `{ "user_name": userName }` body. Add `user_name` to `SessionResponse` data class.
- `ui/screens/AssessmentScreen.kt` → Read `display_name` from `SharedPreferences` before calling `createSession`. Pass it in.
- `ui/screens/HomeScreen.kt` → Replace `"Good morning, Alex"` with `sharedPrefs.getString("display_name", "there")`.
- Everywhere `session_id` is used as `userId` (HomeScreen, FlashcardScreen, MistakeVaultScreen, LessonScreen, AILabScreen) — confirm they all read from `SharedPreferences("session_id")`. Fix any that don't.

**Done when:** User types "Sarah" in step 1 → assessment creates a session with `user_name: "Sarah"` → HomeScreen shows "Good morning, Sarah" → all API calls use the real session UUID as user_id.

**Files:**
- `app/api/assessment.py`, `app/database/models.py`, `app/api/aiLearningPath.py`
- `network/AssessmentApi.kt`, `ui/screens/AssessmentScreen.kt`, `ui/screens/HomeScreen.kt`
- `ui/screens/AILabScreen.kt`, `ui/screens/FlashcardScreen.kt`, `ui/screens/MistakeVaultScreen.kt`

---

### T-02 ⬜ Fix hardcoded `"test_user_123"` in AI Lab
**Why:** All AI Lab conversations, mistakes caught during conversations, and session summaries are saved in Firestore under a fake user ID. Nothing links back to the real user.

**Scope:** Single file, two occurrences.
- `ui/screens/AILabScreen.kt` → `ToneSelectionStep` and `ChatStep` (voice submit and text submit):
  - Add `val context = LocalContext.current` and `val prefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)`.
  - Replace both `"test_user_123"` with `prefs.getString("session_id", "") ?: ""`.

**Done when:** Starting an AI Lab session and checking Firestore `ai_lab_sessions` shows the real user UUID.

**Files:** `ui/screens/AILabScreen.kt`

---

---

## PHASE 2 — Assessment Result Screen

### T-03 ⬜ Assessment result UI — match target screenshot exactly
**Why:** Current result screen is functional but plain. Target design (from provided screenshot) is the standard every feature should meet.

**Target UI elements (from screenshot):**
```
┌─────────────────────────────┐
│  ← Assessment Result    ↺   │
│                             │
│    ╭───────────────╮        │
│    │   92  / 100   │  ← arc gauge, purple stroke
│    │  Proficiency  │
│    ╰───────────────╯        │
│       [C1 Advanced]  ← pill │
│   "Excellent performance! 🎉"│
│   "Strong language skills…" │
│   📅 May 12, 2025  10:30 AM │
│                             │
│  ⭐ Strengths               │
│  ┌──────┐ ┌──────┐ ┌──────┐ │
│  │Grammar│ │Coher.│ │Fluency│ ← 3 cards with score + bar
│  │ 95/100│ │92/100│ │90/100 │
│  └──────┘ └──────┘ └──────┘ │
│                             │
│  📊 Performance Overview    │
│     [Radar chart 5 axes]    │
│  Grammar 95 • Vocab 78      │
│  Pronunc. 75 • Fluency 90   │
│  Coherence 92               │
│                             │
│  🔵 Areas to Improve        │
│  ┌─────────┐ ┌─────────┐   │
│  │Vocabulary│ │Pronunciat│  │
│  │  78/100  │ │  75/100  │  │
│  │ Use wider│ │Work on   │  │
│  │ range… 💡│ │clearer…💡│  │
│  └─────────┘ └─────────┘   │
│                             │
│  ✅ Recommended Actions     │
│  ┌─────────────────────────┐│
│  │ Business Vocabulary Pack ││
│  │ 15 min/day   [Start Now→]││
│  └─────────────────────────┘│
│                             │
│  [  Continue to Lessons   ] │
└─────────────────────────────┘
```

**Backend changes:**
- `app/schemas/response.py` → Add to `AssessmentResponse`: `fluency_score: float` (= avg of coherence+grammar/2), `pronunciation_score: float` (= avg of vocabulary+coherence/2), `completed_at: str` (ISO timestamp).
- `app/api/assessment.py` → In `_process_assessment`, compute and return the two new scores + timestamp when `assessment_complete == True`.

**Android changes — rewrite `AssessmentResultView` in `AssessmentScreen.kt`:**
1. **Top section:** Arc gauge (270° sweep, purple, score/100), CEFR tier pill, grade text from score, completion date.
2. **Strengths row:** 3 cards side by side — Grammar, Coherence, Fluency — each shows score/100 and a colored linear progress bar (green/blue/amber).
3. **Performance Overview:** Existing radar chart kept but restyled — white card background, labels outside, score legend list to the right.
4. **Areas to Improve:** 2 cards for the 2 lowest scores (vocab + pronunciation). Each card: icon, name, score/100, 1-line description, amber "Tip:" line.
5. **Recommended Actions:** Single card derived from `recommended_focus`. Shows a briefcase icon, title "Business Vocabulary Pack" style (customised to focus area), "Estimated Time: 15 min/day", purple "Start Now →" button (navigates to lessons).
6. **Bottom CTA:** Full-width purple "Continue to Lessons" button.
- `network/AssessmentApi.kt` → Add `fluency_score`, `pronunciation_score`, `completed_at` to `AssessmentResponse` data class.

**Done when:** Result screen looks like the screenshot with real scores from the backend.

**Files:**
- `app/schemas/response.py`, `app/api/assessment.py`
- `network/AssessmentApi.kt`, `ui/screens/AssessmentScreen.kt`

---

## PHASE 3 — Learning Path (Core Feature — most complex)

### T-04 ⬜ Backend: Per-user learning path generated & stored in Firestore
**Why:** The biggest gap in the app. `LessonManager.seed_curriculum()` writes ONE shared curriculum for all users. Every user sees the same modules regardless of their CEFR tier. The learning path is not personalised.

**Full scope:**

**New flow after assessment:**
```
POST /api/v1/learning-path (with tier + scores + goal + level)
  → LLM generates personalised module/lesson plan
  → Parse response
  → Write to Firestore:
      users/{user_id}                    ← profile
      user_progress/{user_id}           ← progress tracker
      user_curriculum/{user_id}/modules/{mod_id}
      user_curriculum/{user_id}/lessons/{les_id}
      user_curriculum/{user_id}/sublessons/{sub_id}
  → Return CurrentLearningPathResponse immediately
```

**Backend changes:**
- `app/api/aiLearningPath.py`:
  - Add `user_goal: str` and `user_level: str` to `LearningPathRequest` schema.
  - Incorporate goal/level into `build_learning_path_prompt`.
  - After parsing, call new `LessonManager(db).seed_user_curriculum(user_id, parsed_modules)` instead of the shared `seed_curriculum()`.
  - Return the full `CurrentLearningPathResponse` (not just the flat `LearningPathResponse`) so the app can use it immediately.

- `app/services/lesson_manager.py` — add `seed_user_curriculum(user_id, modules_data)`:
  - Write each module to `user_curriculum/{user_id}/modules/{mod_id}` with a Firestore-generated ID.
  - Write each lesson to `user_curriculum/{user_id}/lessons/{les_id}`.
  - Write each sublesson to `user_curriculum/{user_id}/sublessons/{sub_id}` — include generated `content_blocks` and `exercises` (use LLM or template fallback per sublesson topic).
  - Set initial statuses: first sublesson of first lesson = `"current"`, everything else = `"locked"`.

- `app/api/lessons.py` → `get_current_learning_path(user_id)`:
  - Query from `user_curriculum/{user_id}/modules` (not shared `curriculum_modules`).
  - Fall back to shared seed only if user has no personal curriculum.

**Tier → content mapping:**
| Tier | Modules unlocked at start |
|---|---|
| BEGINNER | Foundations (current), Coffee Chat (locked), Professional Pitch (locked), Master (locked) |
| INTERMEDIATE | Foundations (completed), Coffee Chat (current), Professional Pitch (locked), Master (locked) |
| ADVANCED | Foundations (completed), Coffee Chat (completed), Professional Pitch (current), Master (locked) |

**Done when:** Two users with BEGINNER and ADVANCED tiers each get a different personalised curriculum stored in their own Firestore path.

**Files:**
- `app/api/aiLearningPath.py`, `app/schemas/learning_path.py`
- `app/services/lesson_manager.py`, `app/api/lessons.py`

---

### T-05 ⬜ Android: Learning path cache — instant load, background refresh
**Why:** HomeScreen calls `GET /api/v1/learning-path/current` on every single visit. On Render's free tier this causes a 2-5 second spinner every time.

**Cache rules:**
- On-disk: `SharedPreferences("current_path_json")` — full `CurrentLearningPathResponse` JSON.
- In-memory: `AppCache.learningPath` (singleton object, see T-14).
- TTL: 5 minutes. After 5 min, refresh in background — show old data, swap when new arrives.
- Force refresh triggers: completing an exercise, finishing a sublesson, app cold start.

**New file — `ui/screens/LearningPathCache.kt`:**
```kotlin
object LearningPathCache {
    var data: CurrentLearningPathResponse? = null
    var fetchedAt: Long = 0L
    val TTL = 5 * 60 * 1000L

    fun isStale() = System.currentTimeMillis() - fetchedAt > TTL

    fun loadFromDisk(context: Context) { /* read SharedPrefs */ }
    fun saveToDisk(context: Context) { /* write SharedPrefs */ }
    fun refresh(userId: String, context: Context, onDone: (CurrentLearningPathResponse?) -> Unit) {
        AssessmentApi.getCurrentLearningPath(userId) { path ->
            if (path != null) { data = path; fetchedAt = System.currentTimeMillis(); saveToDisk(context) }
            onDone(path)
        }
    }
    fun invalidate() { fetchedAt = 0L }
}
```

**HomeScreen.kt changes:**
- `LaunchedEffect(userId)`: call `LearningPathCache.loadFromDisk(context)` first → set `learningPath = LearningPathCache.data` immediately (no spinner if cached). Then if `LearningPathCache.isStale()`, call `LearningPathCache.refresh(...)` in background.

**LessonScreen.kt changes:**
- After `completeExercise` success: call `LearningPathCache.invalidate()`.

**Done when:** Navigating to HomeScreen a second time within 5 minutes shows data instantly with no network call.

**Files:**
- New `ui/screens/LearningPathCache.kt`
- `ui/screens/HomeScreen.kt`, `ui/screens/LessonScreen.kt`

---

### T-06 ⬜ Android: Rebuild LearningPathScreen to match target UI
**Why:** Current screen uses the old `LearningPathResponse` (flat module list from initial generation). It needs to use live `CurrentLearningPathResponse` with real progress statuses, and look like the target screenshot.

**Target UI (from screenshot):**
```
← LingoCoach                     👤
─────────────────────────────────
MODULE 1:
Foundation Basics          [📋]
12/15 Lessons              80% ▓▓▓▓▓▓▓▓░░

  ● Greetings               ✅
  ● Numbers & Time          ✅
  ● Ordering Food           ✅
  ▶ Daily Routines [CURRENT]    →
  🔒 Common Objects

─────────────────────────────────
MODULE 2: [locked]
─────────────────────────────────
[  Continue Learning  →  ]
─────────────────────────────────
HOME | AI LAB | VOCAB | VAULT
```

**Changes:**
- Data source: Switch from `SharedPreferences("learning_path_json")` to `LearningPathCache.data` (from T-05).
- Remove the zigzag timeline layout entirely.
- New layout:
  - For each module: header card with title, level badge, lesson count, progress bar.
  - Inside each module: list of lessons with ✅/▶/🔒 icon. Current lesson has a purple "CURRENT" badge and right-arrow button that navigates to the active sublesson.
  - Locked modules show only the header, greyed out.
  - Background: use `background.png` like every other screen.
- Keep coach selection at the bottom.
- Bottom nav bar (tab 0 = Home selected from this screen perspective).

**Files:** `ui/screens/LearningPathScreen.kt`

---

---

## PHASE 4 — AI Lab (Active Language Learning)

### T-07 ⬜ AI Lab: AI starts the conversation + conversation flow
**Why:** Currently the user has to type first. The AI is passive. Real language learning means the AI drives the conversation — asks questions, responds to answers, keeps the flow going.

**Desired flow:**
```
User taps "Start Conversation"
  → POST /api/v1/ailab/start { user_id, topic, voice_gender, tone }
  → Backend calls LLM: "Start a natural {tone} conversation about {topic}.
                         Open with 1-2 sentences as an English tutor."
  → Returns { session_id, opening_message: "Hi! I'm your AI tutor today..." }
  → App shows AI bubble immediately (no API call needed)
  → User replies → POST /api/v1/ailab/chat
  → AI responds conversationally, continues the topic
  → During chat: AI silently checks grammar, returns mistakes[] inline
  → Mistakes shown as correction chips under user's message
  → When user ends session → summary dialog
```

**Backend changes (`app/services/ailab_service.py`):**
- `start_session`: after saving session doc, generate opening message via LLM. Return it in response.
- `process_chat`: AI response should be conversational and advance the topic. Prompt: include conversation history (last 10 messages), topic, tone persona, instruction to keep it brief and ask a follow-up question.
- `app/schemas/ailab.py` → Add `opening_message: str` to `SessionStartResponse`.

**Android changes (`ui/screens/AILabScreen.kt`):**
- `AILabApi.kt` → Add `opening_message` to `AILabSessionStartResponse`.
- `ChatStep`: `LaunchedEffect(sessionId)` — when session first loads and messages are empty, add the `opening_message` as the first AI `ChatMessage` bubble immediately (no API call).

**Done when:** Opening a session shows an AI greeting. AI asks a follow-up after each user message. Mistakes appear as correction chips inline.

**Files:**
- `app/services/ailab_service.py`, `app/schemas/ailab.py`
- `network/AILabApi.kt`, `ui/screens/AILabScreen.kt`

---

### T-08 ⬜ AI Lab: Daily limit (3 sessions/day) + XP bonus unlock (100 XP = +1 free)
**Why:** No limit currently exists. Core game mechanic: limit creates value; XP reward loop drives engagement across all features.

**Rules:**
- Default: 3 AI Lab sessions per day per user (resets at UTC midnight).
- Bonus: if user earns ≥ 100 XP in a day (from lessons + duel + vocab drills combined), they get 1 additional free session that day. This bonus can only trigger once per day. It cannot be saved or carried over.
- XP is only awarded, never withdrawn (once earned today = earned).

**Backend changes:**
- `app/services/ailab_service.py → start_session`:
  - Count today's sessions: query `ai_lab_sessions` where `user_id == user_id AND created_at >= UTC_today_start`. Count docs.
  - Check `users/{user_id}.bonus_ailab_sessions_today` (0 or 1).
  - If `count >= 3 + bonus`, raise `ValueError("Daily AI Lab limit reached")`.
- `app/schemas/ailab.py` → new `AILabStatusResponse`: `{ sessions_used_today, sessions_limit, bonus_sessions, sessions_remaining }`.
- `app/api/ailab.py` → `GET /api/v1/ailab/status?user_id=`.
- XP bonus logic lives in `POST /api/v1/progress/xp` (T-10) — when `xp_earned_today` first crosses 100, set `users/{id}.bonus_ailab_sessions_today = 1`.

**Android changes (`ui/screens/AILabScreen.kt`):**
- `HomeStep`: on load, call `GET /api/v1/ailab/status`. Display:
  - `"Sessions today: 2/3"` counter badge.
  - If `sessions_remaining == 0 AND bonus_sessions == 0`: show locked state — grey out Start button, show message `"Earn 100 XP today to unlock a free bonus session"`.
  - If `sessions_remaining == 0 AND bonus_sessions == 1`: show `"1 bonus session available (earned today's XP goal)"`.

**Done when:** 4th session attempt shows locked UI; earning 100 XP unlocks it; Firestore confirms limit.

**Files:**
- `app/services/ailab_service.py`, `app/api/ailab.py`, `app/schemas/ailab.py`
- `ui/screens/AILabScreen.kt`, `network/AILabApi.kt`

---

## PHASE 5 — Vocab Builder

### T-09 ⬜ Vocab Builder: Backend mastery tracking + sync + no-repeat rule
**Why:** Vocab progress is local-only. Mastered words reappear in drills. No cross-device sync. Backend has the `vocab/sync` endpoints but the app never calls them.

**Mastery rules:**
- Score 0–79: still learning → appears in drills.
- Score 80–99: mastered → excluded from NEW drill sessions. Still visible in "All Words".
- Score 100: fully mastered → permanently excluded everywhere except browser.
- Score goes up +20 per correct answer, -10 per wrong, clamped 0–100.
- If user gets a word wrong: `mastered = false`, log to mistake vault.
- If user gets a word right when score was < 80: `mastered` stays false until score hits 80.

**Android changes:**

`VocabTracker.kt`:
- `generateDrillSession`: filter out words where `progressMap[word]?.masteryScore ?: 0 >= 80`.
- Add `syncToBackend(userId: String, context: Context)` suspend function:
  1. Read all `progressMap` entries.
  2. `POST /api/v1/vocab/sync?user_id={userId}` with `{ client_time: ISO, changes: [{word, mastery_score, is_bookmarked, last_reviewed, updated_at}] }`.
  3. Then `GET /api/v1/vocab/sync?user_id={userId}&last_sync_time={lastSync}` — merge returned updates into `progressMap` (server wins if server `updated_at` > local).
  4. Save `SharedPreferences("vocab_last_sync")` with current ISO time.
- `addLocalMistake` function: also call `POST /api/v1/mistakes` (fire-and-forget coroutine) when a drill answer is wrong.

`VocabBuilderScreen.kt`:
- On screen open: launch background coroutine calling `VocabTracker.syncToBackend(userId, context)`.

**Done when:** Mastered words don't reappear; wrong answers appear in Mistake Vault backend; progress syncs to Firestore.

**Files:**
- `ui/screens/VocabTracker.kt`, `ui/screens/VocabBuilderScreen.kt`
- `network/AssessmentApi.kt` (add `logMistake` and `syncVocab`)

---

## PHASE 6 — Timely Duel

### T-10 ⬜ Timely Duel: XP saved to backend + 100 XP AI Lab unlock trigger
**Why:** XP from duels never reaches Firestore. The 100 XP → AI Lab bonus mechanic is entirely missing.

**Backend changes:**
- `app/api/progress.py` → new endpoint `POST /api/v1/progress/xp`:
  ```json
  Request: { "user_id": "...", "xp_delta": 50, "source": "timely_duel" }
  ```
  - Call `LessonManager.update_user_xp_and_streak(user_id, xp_delta)` (already exists).
  - Also increment `users/{id}.xp_earned_today` by `xp_delta`.
  - If `xp_earned_today` crosses 100 for the first time today: set `users/{id}.bonus_ailab_sessions_today = 1`.
  - `xp_earned_today` resets to 0 on each new day (check `last_active_date` vs today, reset if different).

**Android changes:**
- `ui/screens/TimelyDuelScreen.kt` → `DuelResultScreen`: fire `POST /api/v1/progress/xp` with `xpDelta` and source `"timely_duel"` in a background coroutine after showing results. Don't block the UI — it's fire-and-forget.
- `AppCache.invalidateLearningPath()` after XP save (streak/XP shown on HomeScreen needs to update).

**Done when:** Duel XP appears in Firestore `users` doc. Earning 100 total XP in a day unlocks the AI Lab bonus. Firestore `xp_earned_today` resets next day.

**Files:**
- `app/api/progress.py`, `app/schemas/progress.py`, `app/services/lesson_manager.py`
- `ui/screens/TimelyDuelScreen.kt`

---

### T-11 ⬜ Timely Duel: Wrong answers → Mistake Vault (backend + local)
**Why:** Wrong duel answers only go to the local `vocab_mistakes.json`. They don't generate SRS flashcards and don't appear in the backend Mistake Vault.

**Android changes (`ui/screens/TimelyDuelScreen.kt`):**
- `DuelGameScreen → onWrong(word)`: after `VocabTracker.addLocalMistake(...)`, launch background coroutine:
  ```
  POST /api/v1/mistakes {
    user_id, word: word.word,
    mistake_type: "TIMELY_DUEL",
    user_sentence: "(missed in timed duel)",
    correct_sentence: word.word,
    explanation: "Meaning: ${word.meaning}"
  }
  ```
- `network/AssessmentApi.kt`: add `logMistake(userId, word, mistakeType, userSentence, correctSentence, explanation, onResult)` function.

**Done when:** Losing a duel question creates a Firestore `mistakes` doc + SRS flashcard. The mistake appears in MistakeVaultScreen's server tab.

**Files:**
- `ui/screens/TimelyDuelScreen.kt`, `network/AssessmentApi.kt`

---

---

## PHASE 7 — Mistake Vault (Universal logging)

### T-12 ⬜ Mistake Vault: Log mistakes from ALL 5 features to backend
**Why:** Currently only Learning Path exercises and AI Lab conversations log mistakes to Firestore. Vocab Builder and Timely Duel only log locally. Flashcard retests log nothing anywhere on the backend.

**Sources that must log to `POST /api/v1/mistakes`:**
| Source | Trigger | mistake_type |
|---|---|---|
| Learning Path exercise | wrong answer in `LessonScreen` | already works via `complete_exercise` |
| AI Lab conversation | every chat message | already works via `ailab_service` |
| Vocab Builder drill | wrong answer in `ContextualDrillView` | ⬜ MISSING — add in T-09 |
| Timely Duel | wrong answer / timeout | ⬜ MISSING — add in T-11 |
| Flashcard retest | rating == 0 (Again) in `FlashcardScreen` | ⬜ MISSING — add here |

**This task covers Flashcards only** (the others are covered in T-09 and T-11):
- `ui/screens/FlashcardScreen.kt` → `RatingButton("Again")` onClick:
  - After `submitReview(cardId, 0, ...)`, also call `AssessmentApi.logMistake(userId, cardFront, "FLASHCARD_RETEST", ...)`.

**Backend changes:**
- `app/schemas/mistakes.py` → Add `source: str = "unknown"` field to the mistake document schema.
- `app/services/mistake_service.py → _log_mistake`: accept `source` param, store it in Firestore doc.
- This `source` field enables the "Past Logs" tab in MistakeVaultScreen to filter by feature.

**Done when:** Mistakes from all 5 sources appear in Firestore with correct `source` labels. MistakeVaultScreen shows mistakes from every feature.

**Files:**
- `app/services/mistake_service.py`, `app/schemas/mistakes.py`
- `ui/screens/FlashcardScreen.kt`

---

## PHASE 8 — Daily Analytics & Progress Tracking

### T-13 ⬜ Backend: Daily stats collection + weekly analytics endpoint
**Why:** HomeScreen bar chart is 100% hardcoded. Accuracy %, mistakes corrected count, lessons done count — all fake. There is zero time-series data in Firestore.

**New Firestore collection: `daily_stats`**
Document ID: `{user_id}_{YYYY-MM-DD}`
```json
{
  "user_id": "...",
  "date": "2025-06-16",
  "lessons_completed": 0,
  "exercises_attempted": 0,
  "exercises_correct": 0,
  "mistakes_logged": 0,
  "vocab_drills_done": 0,
  "vocab_words_mastered": 0,
  "ai_lab_sessions": 0,
  "ai_lab_minutes": 0,
  "duel_sessions": 0,
  "duel_correct": 0,
  "xp_earned": 0,
  "streak_day": 0
}
```

**Backend — new helper `update_daily_stats(db, user_id, field, increment=1)`:**
- Called from:
  - `lesson_manager.complete_sublesson` → increment `lessons_completed`, `xp_earned`
  - `lessons.complete_exercise` → increment `exercises_attempted`, `exercises_correct` (if correct)
  - `mistake_service._log_mistake` → increment `mistakes_logged`
  - `ailab_service.end_session` → increment `ai_lab_sessions`, `ai_lab_minutes` (from session duration)
  - `progress/xp` endpoint → increment `xp_earned`, `duel_sessions` (if source == "timely_duel")
  - `vocab_service.sync_client_progress` → increment `vocab_drills_done`, `vocab_words_mastered`

**New endpoint `GET /api/v1/analytics/weekly?user_id=`:**
- Returns last 7 `daily_stats` docs as array, sorted by date ascending.
- Add to `app/api/analytics.py` (new file), register in `main.py`.

**Android changes:**
- `network/AssessmentApi.kt` → add `getWeeklyAnalytics(userId, onResult)`.
- `ui/screens/HomeScreen.kt → HomeSpeakingStats`:
  - `LaunchedEffect(userId)`: fetch analytics, cache in `AppCache.weeklyStats` (TTL 60 min).
  - Replace hardcoded `heights` with normalized `xp_earned` values across 7 days.
  - Replace `"85%"` accuracy with `exercises_correct / exercises_attempted` (today's stats).
  - Replace `"42"` mistakes corrected with `mistakes_logged` (today or week total).
  - Replace `"12"` lessons done with `lessons_completed` (this week total).

**Done when:** Bar chart shows real daily activity. Accuracy and lesson counts are real numbers from Firestore.

**Files:**
- New `app/api/analytics.py`, registered in `app/main.py`
- `app/services/lesson_manager.py`, `app/services/ailab_service.py`, `app/services/mistake_service.py`
- `network/AssessmentApi.kt`, `ui/screens/HomeScreen.kt`

---

## PHASE 9 — App-Wide Cache

### T-14 ⬜ Android: AppCache singleton — instant loads, background sync
**Why:** Every screen independently calls the API on every navigation visit. HomeScreen, MistakeVaultScreen, and FlashcardScreen all re-fetch on every open. With Render cold starts this causes 5–15 second waits.

**New file: `ui/screens/AppCache.kt`**
```kotlin
object AppCache {
    // Learning path (5 min TTL) — also backed by SharedPrefs
    var learningPath: CurrentLearningPathResponse? = null
    var learningPathAt: Long = 0L

    // Mistakes list (10 min TTL)
    var mistakes: List<Mistake>? = null
    var mistakesAt: Long = 0L

    // Weekly analytics (60 min TTL)
    var weeklyStats: List<DailyStats>? = null
    var weeklyStatsAt: Long = 0L

    // AI Lab status (5 min TTL)
    var aiLabStatus: AILabStatus? = null
    var aiLabStatusAt: Long = 0L

    fun isStale(fetchedAt: Long, ttlMs: Long) =
        System.currentTimeMillis() - fetchedAt > ttlMs

    fun invalidateLearningPath() { learningPath = null; learningPathAt = 0L }
    fun invalidateMistakes()     { mistakes = null;     mistakesAt = 0L }
    fun invalidateAll()          { invalidateLearningPath(); invalidateMistakes() }
}
```

**Cache-then-refresh pattern (used in each screen):**
```kotlin
// 1. Show cached data immediately (no spinner)
learningPath = AppCache.learningPath ?: loadFromSharedPrefs()
isLoading = (learningPath == null)  // spinner only on true cold start

// 2. Refresh in background if stale
if (AppCache.isStale(AppCache.learningPathAt, 5 * 60_000)) {
    scope.launch(Dispatchers.IO) {
        AssessmentApi.getCurrentLearningPath(userId) { fresh ->
            AppCache.learningPath = fresh
            AppCache.learningPathAt = System.currentTimeMillis()
            scope.launch(Dispatchers.Main) { learningPath = fresh }
        }
    }
}
```

**Invalidation triggers:**
- Exercise completed → `AppCache.invalidateLearningPath()`
- Mistake logged → `AppCache.invalidateMistakes()`
- AI Lab session ended → `AppCache.invalidateLearningPath()` (XP/streak changed)
- Duel finished → `AppCache.invalidateLearningPath()`

**Done when:** Home → AI Lab → Home shows path data instantly on second visit. No API call fires if TTL has not expired.

**Files:**
- New `ui/screens/AppCache.kt`
- `ui/screens/HomeScreen.kt`, `ui/screens/MistakeVaultScreen.kt`
- `ui/screens/LessonScreen.kt`, `ui/screens/AILabScreen.kt`

---

---

## PHASE 10 — Settings & Profile

### T-15 ⬜ Settings: Reads/writes real Firestore user data
**Why:** SettingsScreen only touches `SharedPreferences`. Changes never reach the backend. If a user reinstalls the app, all settings are lost.

**Backend changes:**
- `app/api/progress.py` → add `PATCH /api/v1/users/{user_id}` accepting:
  ```json
  { "display_name": "...", "native_language": "...",
    "target_fluency": "...", "voice_profile": "...", "daily_reminder": true }
  ```
  Merges into `users/{user_id}` Firestore doc.

**Android changes (`ui/screens/SettingsScreen.kt`):**
- `LaunchedEffect(Unit)`: fetch `GET /api/v1/progress?user_id=` to get `tier`, `xp`, `streak`. Show these in a stats row at the top of the settings profile card.
- Whenever `saveStr(key, value)` is called, also fire `PATCH /api/v1/users/{userId}` in a background coroutine with the single changed field.
- On logout, write `display_name`, `target_fluency`, `native_language`, `voice_profile` to Firestore before clearing SharedPrefs (so data is safe if user logs back in).

**Done when:** Changing display name in Settings persists to Firestore. Reinstalling app and logging back in shows the same settings pulled from backend.

**Files:**
- `app/api/progress.py`, `app/schemas/progress.py`
- `ui/screens/SettingsScreen.kt`, `network/AssessmentApi.kt`

---

## PHASE 11 — Polish & Completion

### T-16 ⬜ Home screen: Real name + time-of-day greeting
**Scope:** Two-line change in `HomeScreen.kt`.
- Replace `"Good morning, Alex"` with dynamic greeting:
  ```kotlin
  val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
  val greeting = when {
      hour < 12 -> "Good morning"
      hour < 17 -> "Good afternoon"
      else      -> "Good evening"
  }
  val name = sharedPrefs.getString("display_name", "there") ?: "there"
  // Display: "$greeting, $name"
  ```
**Files:** `ui/screens/HomeScreen.kt`

---

### T-17 ⬜ Pass user_goal + user_level into learning path generation
**Why:** The LLM-generated learning path prompt doesn't know WHY the user is learning (Job Interviews, Travel, Business) or their self-reported starting level. This limits personalisation.

**Android (`ui/screens/AssessmentScreen.kt`):**
- In `onContinue` (learning path generation): read `user_goal` and `user_level` from SharedPrefs and add to `LearningPathRequest`:
  ```kotlin
  LearningPathRequest(
      ...existing fields...,
      user_goal = sharedPrefs.getString("user_goal", "general") ?: "general",
      user_level_self_reported = sharedPrefs.getString("user_level", "intermediate") ?: "intermediate"
  )
  ```

**Backend (`app/schemas/learning_path.py`, `app/api/aiLearningPath.py`):**
- Add `user_goal: str = "general"` and `user_level_self_reported: str = "intermediate"` to `LearningPathRequest`.
- Add to `build_learning_path_prompt`: `"Student's stated learning goal: {payload.user_goal}. Self-reported starting level: {payload.user_level_self_reported}."` — place in the profile section of the prompt.

**Done when:** A user who selected "Business Communication" as their goal gets business-focused lesson titles in their learning path.

**Files:**
- `app/schemas/learning_path.py`, `app/api/aiLearningPath.py`
- `network/AssessmentApi.kt`, `ui/screens/AssessmentScreen.kt`

---

### T-18 ⬜ Complete LINGOCOACH_SYSTEM_GUIDE.md
**Scope:** Write all remaining sections of the system guide. Currently only Section 1 (Architecture) is done. Covers:
- Section 2: Onboarding flow diagram
- Section 3: User identity / session_id lifecycle
- Section 4: Assessment API flow + result fields
- Section 5: Assessment result UI spec (matches T-03)
- Section 6: Learning path full spec (matches T-04/T-05/T-06)
- Section 7: AI Lab full spec (matches T-07/T-08)
- Section 8: Vocab Builder spec (matches T-09)
- Section 9: Timely Duel spec (matches T-10/T-11)
- Section 10: Mistake Vault spec (matches T-12)
- Section 11: Flashcards SRS spec
- Section 12: Daily analytics spec (matches T-13)
- Section 13: Caching spec (matches T-14)
- Section 14: Full Firestore schema (every collection, every field)
- Section 15: Complete API reference (every endpoint)
- Section 16: Backend architecture (services, LLM routing, Groq)
- Section 17: Bug registry (critical issues, root cause, fix)
- Section 18: Environment variables + Render deployment

**Files:** `LINGOCOACH_SYSTEM_GUIDE.md`

---

## Summary Table

| # | Task | Feature | Priority | Backend | Android |
|---|------|---------|----------|---------|---------|
| T-01 | Name → session → user_id everywhere | Identity | 🔴 Critical | ✏️ | ✏️ |
| T-02 | Fix test_user_123 in AI Lab | Identity | 🔴 Critical | — | ✏️ |
| T-03 | Assessment result UI (match screenshot) | Assessment | 🔴 Critical | ✏️ | ✏️ |
| T-04 | Per-user learning path in Firestore | Learning Path | 🔴 Critical | ✏️ | — |
| T-05 | Learning path cache (instant loads) | Learning Path | 🟠 High | — | ✏️ |
| T-06 | LearningPathScreen UI rebuild | Learning Path | 🟠 High | — | ✏️ |
| T-07 | AI starts conversation + flow | AI Lab | 🟠 High | ✏️ | ✏️ |
| T-08 | AI Lab daily limit + XP bonus unlock | AI Lab | 🟠 High | ✏️ | ✏️ |
| T-09 | Vocab mastery sync + no-repeat rule | Vocab Builder | 🟠 High | ✏️ | ✏️ |
| T-10 | Duel XP → backend + AI Lab unlock | Timely Duel | 🟡 Medium | ✏️ | ✏️ |
| T-11 | Duel mistakes → backend + flashcards | Timely Duel | 🟡 Medium | — | ✏️ |
| T-12 | Mistake logging from all 5 features | Mistake Vault | 🟡 Medium | ✏️ | ✏️ |
| T-13 | Daily analytics backend + real charts | Analytics | 🟡 Medium | ✏️ | ✏️ |
| T-14 | App-wide cache layer (AppCache) | Caching | 🟡 Medium | — | ✏️ |
| T-15 | Settings reads/writes Firestore | Settings | 🟡 Medium | ✏️ | ✏️ |
| T-16 | Dynamic time-of-day greeting | Polish | 🟢 Low | — | ✏️ |
| T-17 | user_goal + user_level in learning path | Polish | 🟢 Low | ✏️ | ✏️ |
| T-18 | Complete system guide | Docs | 🟢 Low | — | — |

---

## How to use this list

Say **`start T-01`** and the task will be fully implemented (both backend + Android files changed, tested, built) before moving on.

No task is started until you explicitly ask. No task is marked done until verified.

**Recommended order:** T-01 → T-02 → T-03 → T-04 → T-05 → T-06 → T-07 → T-08 → T-09 → T-10 → T-11 → T-12 → T-13 → T-14 → T-15 → T-16 → T-17 → T-18
