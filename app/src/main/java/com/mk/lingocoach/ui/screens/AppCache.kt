package com.mk.lingocoach.ui.screens

import android.content.Context
import com.google.gson.Gson
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.AssessmentResponse
import com.mk.lingocoach.network.CurrentLearningPathResponse
import com.mk.lingocoach.network.LearningPathRequest
import com.mk.lingocoach.network.Mistake
import com.mk.lingocoach.network.DailyStats
import com.mk.lingocoach.network.ProgressMetrics

/**
 * In-memory + SharedPreferences cache for data that changes infrequently.
 * Each entry has a TTL. Screens show stale data immediately, then refresh
 * in the background — eliminating spinners on every navigation visit.
 */
object AppCache {

    private val gson = Gson()
    private val completedSublessonIds = linkedSetOf<String>()

    // ── Learning path (5-min TTL) ─────────────────────────────────────────────
    var learningPath: CurrentLearningPathResponse? = null
    var learningPathAt: Long = 0L
    private const val LP_TTL = 5 * 60 * 1000L          // 5 minutes

    // ── Mistakes list (10-min TTL) ────────────────────────────────────────────
    var mistakes: List<Mistake>? = null
    var mistakesAt: Long = 0L
    private const val MK_TTL = 10 * 60 * 1000L         // 10 minutes

    // Analytics are shown immediately on return and refreshed in the background.
    var weeklyStats: List<DailyStats>? = null
    var progressMetrics: ProgressMetrics? = null
    var analyticsAt: Long = 0L
    private const val ANALYTICS_TTL = 2 * 60 * 1000L

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun isLearningPathStale() =
        System.currentTimeMillis() - learningPathAt > LP_TTL

    fun isMistakesStale() =
        System.currentTimeMillis() - mistakesAt > MK_TTL

    fun isAnalyticsStale() =
        System.currentTimeMillis() - analyticsAt > ANALYTICS_TTL

    fun invalidateLearningPath() {
        learningPath = null
        learningPathAt = 0L
    }

    fun invalidateLocalizedLearningPath(context: Context) {
        invalidateLearningPath()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PATH)
            .remove(KEY_PATH_AT)
            .remove("learning_path_json")
            .apply()
    }

    fun regenerateLocalizedLearningPath(context: Context, languageCode: String) {
        invalidateLocalizedLearningPath(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val assessmentJson = prefs.getString("assessment_response_json", null) ?: return
        val assessment = try {
            gson.fromJson(assessmentJson, AssessmentResponse::class.java)
        } catch (_: Exception) {
            return
        }
        val locale = if (languageCode == "system") java.util.Locale.getDefault()
            else java.util.Locale.forLanguageTag(languageCode)
        val request = LearningPathRequest(
            session_id = assessment.session_id,
            user_name = prefs.getString("display_name", "") ?: "",
            tier = assessment.assigned_tier ?: "B2 Upper-Intermediate",
            grammar_score = assessment.grammar_score.toInt(),
            vocabulary_score = assessment.vocabulary_score.toInt(),
            coherence_score = assessment.coherence_score.toInt(),
            structural_break = assessment.structural_break,
            detected_strength = assessment.detected_strength ?: "",
            detected_weakness = assessment.detected_weakness ?: "",
            recommended_focus = assessment.recommended_focus ?: "",
            user_goal = prefs.getString("user_goal", "general") ?: "general",
            user_level_self_reported = prefs.getString("user_level", "intermediate") ?: "intermediate",
            output_language = locale.getDisplayLanguage(java.util.Locale.ENGLISH).ifBlank { "English" }
        )
        prefs.edit().putString("learning_path_generation_state", "generating").apply()
        AssessmentApi.getLearningPath(request) { generated ->
            if (generated == null) {
                prefs.edit().putString("learning_path_generation_state", "fallback").apply()
                return@getLearningPath
            }
            AssessmentApi.getCurrentLearningPath(assessment.session_id) { current ->
                if (current != null && current.isBackendLearningPathReady()) {
                    learningPath = applyLocalLearningPathProgress(current)
                    learningPathAt = System.currentTimeMillis()
                    saveToDisk(context)
                    prefs.edit().putString("learning_path_generation_state", "ready").apply()
                } else {
                    prefs.edit().putString("learning_path_generation_state", "fallback").apply()
                }
            }
        }
    }

    fun rememberCompletedSublesson(sublessonId: String) {
        if (sublessonId.isBlank()) return
        completedSublessonIds.add(sublessonId)
        learningPath = learningPath?.withCompletedSublesson(sublessonId)
        learningPathAt = System.currentTimeMillis()
    }

    fun applyLocalLearningPathProgress(path: CurrentLearningPathResponse): CurrentLearningPathResponse =
        completedSublessonIds.fold(path.normalizedLearningPath()) { updatedPath, sublessonId ->
            updatedPath.withCompletedSublesson(sublessonId)
        }

    fun invalidateMistakes() {
        mistakes = null
        mistakesAt = 0L
    }

    fun invalidateAll() {
        invalidateLearningPath()
        invalidateMistakes()
        weeklyStats = null
        progressMetrics = null
        analyticsAt = 0L
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
                ?.let { applyLocalLearningPathProgress(it) }
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
