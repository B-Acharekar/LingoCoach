package com.mk.lingocoach.ui.screens

import android.content.Context
import com.google.gson.Gson
import com.mk.lingocoach.network.CurrentLearningPathResponse
import com.mk.lingocoach.network.Mistake

/**
 * In-memory + SharedPreferences cache for data that changes infrequently.
 * Each entry has a TTL. Screens show stale data immediately, then refresh
 * in the background — eliminating spinners on every navigation visit.
 */
object AppCache {

    private val gson = Gson()

    // ── Learning path (5-min TTL) ─────────────────────────────────────────────
    var learningPath: CurrentLearningPathResponse? = null
    var learningPathAt: Long = 0L
    private const val LP_TTL = 5 * 60 * 1000L          // 5 minutes

    // ── Mistakes list (10-min TTL) ────────────────────────────────────────────
    var mistakes: List<Mistake>? = null
    var mistakesAt: Long = 0L
    private const val MK_TTL = 10 * 60 * 1000L         // 10 minutes

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun isLearningPathStale() =
        System.currentTimeMillis() - learningPathAt > LP_TTL

    fun isMistakesStale() =
        System.currentTimeMillis() - mistakesAt > MK_TTL

    fun invalidateLearningPath() {
        learningPath = null
        learningPathAt = 0L
    }

    fun invalidateMistakes() {
        mistakes = null
        mistakesAt = 0L
    }

    fun invalidateAll() {
        invalidateLearningPath()
        invalidateMistakes()
    }

    // ── Disk persistence (SharedPreferences) ─────────────────────────────────

    private const val KEY_PATH = "cache_current_path_json"
    private const val KEY_PATH_AT = "cache_current_path_at"
    private const val PREFS = "LingoCoachPrefs"

    fun loadFromDisk(context: Context) {
        if (learningPath != null) return   // already in memory
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY_PATH, null) ?: return
        val ts    = prefs.getLong(KEY_PATH_AT, 0L)
        try {
            learningPath  = gson.fromJson(json, CurrentLearningPathResponse::class.java)
            learningPathAt = ts
        } catch (_: Exception) { /* corrupt cache — ignore */ }
    }

    fun saveToDisk(context: Context) {
        val path = learningPath ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PATH, gson.toJson(path))
            .putLong(KEY_PATH_AT, learningPathAt)
            .apply()
    }
}
