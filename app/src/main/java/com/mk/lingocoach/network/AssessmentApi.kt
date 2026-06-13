package com.mk.lingocoach.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class SessionResponse(
    val session_id: String,
    val current_step: Int,
    val next_question: String
)

data class AssessmentResponse(
    val session_id: String,
    val assessment_complete: Boolean,
    val next_question: String?,
    val assigned_tier: String?,
    val current_step: Int,
    val grammar_score: Float,
    val vocabulary_score: Float,
    val coherence_score: Float,
    val structural_break: Boolean,
    val detected_strength: String?,
    val detected_weakness: String?,
    val recommended_focus: String?,
    val transcribed_text: String?
)

// ─── Current Learning Path Models ───────────────────────────────────────────

data class ContentBlock(
    val type: String,       // "explanation", "example", "tip"
    val text: String
)

data class Exercise(
    val id: String,
    val type: String,       // "multiple_choice", "fill_in_the_blank", "pronunciation", "free_speech"
    val stimulus: String,
    val instruction: String,
    val options: List<String>?,
    val correct_answer: String?
)

data class CurrentSublesson(
    val id: String,
    val title: String,
    val order: Int,
    val status: String,
    val content_blocks: List<ContentBlock>,
    val exercises: List<Exercise>
)

data class CurrentLesson(
    val id: String,
    val title: String,
    val description: String,
    val order: Int,
    val status: String,
    @SerializedName("xp_reward") val xpReward: Int,
    val sublessons: List<CurrentSublesson>
)

data class CurrentModule(
    val id: String,
    val title: String,
    val description: String,
    val level: String,
    val order: Int,
    val status: String,
    @SerializedName("xp_reward") val xpReward: Int,
    val lessons: List<CurrentLesson>
)

data class CurrentLearningPathResponse(
    val user_id: String,
    val tier: String,
    val xp: Int,
    val streak: Int,
    val modules: List<CurrentModule>
)

data class SublessonDetail(
    val id: String,
    val title: String,
    val lesson_id: String,
    val module_id: String,
    val order: Int,
    val status: String?,
    val content_blocks: List<ContentBlock>,
    val exercises: List<Exercise>
)

data class CompleteExerciseRequest(
    val user_id: String,
    val sublesson_id: String,
    val exercise_id: String,
    val user_answer: String,
    val audio_transcription: String = ""
)

data class CompleteExerciseResponse(
    val exercise_id: String,
    val is_correct: Boolean,
    val score: Int,
    val feedback: String,
    val xp_earned: Int,
    val streak_updated: Boolean,
    val new_streak: Int,
    val mistake_logged: Boolean,
    val mistake_type: String?
)

data class Mistake(
    val id: String,
    val user_id: String,
    val word: String,
    val mistake_type: String,
    val user_sentence: String,
    val correct_sentence: String,
    val explanation: String,
    val times_missed: Int,
    val mastered: Boolean,
    val mastery_score: Int,
    val created_at: String,
    val last_reviewed: String?
)

// ─────────────────────────────────────────────────────────────────────────────
object AssessmentApi {
    private const val BASE_URL = "https://lingoai-backend-zej0.onrender.com"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun createSession(onResult: (SessionResponse?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/sessions")
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to create session", e)
                onResult(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("AssessmentApi", "Create session unsuccessful: ${response.code}")
                        onResult(null)
                        return
                    }
                    val bodyString = response.body?.string()
                    Log.d("AssessmentApi", "Create session response: $bodyString")
                    try {
                        val session = gson.fromJson(bodyString, SessionResponse::class.java)
                        onResult(session)
                    } catch (e: Exception) {
                        Log.e("AssessmentApi", "Failed to parse session response", e)
                        onResult(null)
                    }
                }
            }
        })
    }

    fun submitTextAnswer(sessionId: String, answer: String, onResult: (AssessmentResponse?) -> Unit) {
        val json = gson.toJson(mapOf("session_id" to sessionId, "user_answer" to answer))
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/assess")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to submit text answer", e)
                onResult(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("AssessmentApi", "Submit text answer unsuccessful: ${response.code}")
                        onResult(null)
                        return
                    }
                    val bodyString = response.body?.string()
                    Log.d("AssessmentApi", "Submit text response: $bodyString")
                    try {
                        val result = gson.fromJson(bodyString, AssessmentResponse::class.java)
                        onResult(result)
                    } catch (e: Exception) {
                        Log.e("AssessmentApi", "Failed to parse text answer response", e)
                        onResult(null)
                    }
                }
            }
        })
    }

    fun submitVoiceAnswer(sessionId: String, audioFile: File, onResult: (AssessmentResponse?) -> Unit) {
        val fileBody = audioFile.asRequestBody("audio/mp4".toMediaType())
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("session_id", sessionId)
            .addFormDataPart("audio_file", audioFile.name, fileBody)
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/assess/voice")
            .post(multipartBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to submit voice answer", e)
                onResult(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("AssessmentApi", "Submit voice answer unsuccessful: ${response.code}")
                        onResult(null)
                        return
                    }
                    val bodyString = response.body?.string()
                    Log.d("AssessmentApi", "Submit voice response: $bodyString")
                    try {
                        val result = gson.fromJson(bodyString, AssessmentResponse::class.java)
                        onResult(result)
                    } catch (e: Exception) {
                        Log.e("AssessmentApi", "Failed to parse voice answer response", e)
                        onResult(null)
                    }
                }
            }
        })
    }

    fun getLearningPath(requestBody: LearningPathRequest, onResult: (LearningPathResponse?) -> Unit) {
        val json = gson.toJson(requestBody)
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/learning-path")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .header("accept", "application/json")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to fetch learning path", e)
                onResult(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("AssessmentApi", "Fetch learning path unsuccessful: ${response.code}")
                        onResult(null)
                        return
                    }
                    val bodyString = response.body?.string()
                    Log.d("AssessmentApi", "Learning path response: $bodyString")
                    try {
                        val result = gson.fromJson(bodyString, LearningPathResponse::class.java)
                        onResult(result)
                    } catch (e: Exception) {
                        Log.e("AssessmentApi", "Failed to parse learning path response", e)
                        onResult(null)
                    }
                }
            }
        })
    }

    fun getCurrentLearningPath(userId: String, onResult: (CurrentLearningPathResponse?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/learning-path/current?user_id=$userId")
            .get()
            .header("accept", "application/json")
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to get current learning path", e)
                onResult(null)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) { Log.e("AssessmentApi", "Get learning path error: ${response.code}"); onResult(null); return }
                    val body = response.body?.string()
                    Log.d("AssessmentApi", "Current learning path: $body")
                    try { onResult(gson.fromJson(body, CurrentLearningPathResponse::class.java)) }
                    catch (e: Exception) { Log.e("AssessmentApi", "Parse error", e); onResult(null) }
                }
            }
        })
    }

    fun getSublesson(sublessonId: String, onResult: (SublessonDetail?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/sublessons/$sublessonId")
            .get()
            .header("accept", "application/json")
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to get sublesson", e); onResult(null)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) { onResult(null); return }
                    val body = response.body?.string()
                    try { onResult(gson.fromJson(body, SublessonDetail::class.java)) }
                    catch (e: Exception) { onResult(null) }
                }
            }
        })
    }

    fun completeExercise(request: CompleteExerciseRequest, onResult: (CompleteExerciseResponse?) -> Unit) {
        val json = gson.toJson(request)
        val httpRequest = Request.Builder()
            .url("$BASE_URL/api/v1/exercise/complete")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .header("accept", "application/json")
            .build()
        client.newCall(httpRequest).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to complete exercise", e); onResult(null)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) { onResult(null); return }
                    val body = response.body?.string()
                    try { onResult(gson.fromJson(body, CompleteExerciseResponse::class.java)) }
                    catch (e: Exception) { onResult(null) }
                }
            }
        })
    }

    fun getMistakes(userId: String, onResult: (List<Mistake>?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/mistakes?user_id=$userId")
            .get()
            .header("accept", "application/json")
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to get mistakes", e); onResult(null)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) { onResult(null); return }
                    val body = response.body?.string()
                    try {
                        val type = object : com.google.gson.reflect.TypeToken<List<Mistake>>() {}.type
                        onResult(gson.fromJson(body, type))
                    } catch (e: Exception) { onResult(null) }
                }
            }
        })
    }

    fun getVocabBookmarks(userId: String, onResult: (List<VocabBookmark>?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/vocab/bookmarks?user_id=$userId")
            .get()
            .header("accept", "application/json")
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to get vocab bookmarks", e); onResult(null)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) { onResult(null); return }
                    val body = response.body?.string()
                    try {
                        val type = object : com.google.gson.reflect.TypeToken<List<VocabBookmark>>() {}.type
                        onResult(gson.fromJson(body, type))
                    } catch (e: Exception) { onResult(null) }
                }
            }
        })
    }

    fun getFlashcards(userId: String, onResult: (List<Flashcard>?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/flashcards/review?user_id=$userId")
            .get()
            .header("accept", "application/json")
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to get flashcards", e); onResult(null)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) { onResult(null); return }
                    val body = response.body?.string()
                    try {
                        val type = object : com.google.gson.reflect.TypeToken<List<Flashcard>>() {}.type
                        onResult(gson.fromJson(body, type))
                    } catch (e: Exception) { onResult(null) }
                }
            }
        })
    }

    fun reviewFlashcard(cardId: String, rating: Int, onResult: (Boolean) -> Unit) {
        val json = gson.toJson(ReviewRatingRequest(cardId, rating))
        val httpRequest = Request.Builder()
            .url("$BASE_URL/api/v1/flashcards/review")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .header("accept", "application/json")
            .build()
        client.newCall(httpRequest).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to review flashcard", e); onResult(false)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    onResult(response.isSuccessful)
                }
            }
        })
    }
}

data class VocabBookmark(
    val id: String,
    val user_id: String,
    val word: String,
    val context_sentence: String?,
    val corrected_sentence: String?,
    val definition: String?,
    val pronunciation_url: String?,
    val mastery_score: Int
)

data class Flashcard(
    val id: String,
    val user_id: String,
    val card_type: String,
    val front: String,
    val back: String,
    val interval: Int,
    val ease_factor: Float,
    val repetitions: Int,
    val streak: Int,
    val due_date: String
)

data class ReviewRatingRequest(
    val card_id: String,
    val rating: Int
)

data class SubLearningPath(
    val title: String,
    val description: String,
    val status: String,
    val duration_minutes: Int?,
    val estimated_days: Int?
)

data class Module(
    val level: String,
    val title: String,
    val status: String,
    val description: String,
    val sub_learning_path: List<SubLearningPath>
)

data class LearningPathResponse(
    val session_id: String,
    val tier: String,
    val overall_summary: String?,
    val grammar_improvement_plan: String?,
    val vocabulary_improvement_plan: String?,
    val coherence_improvement_plan: String?,
    val recommended_resources: List<String>?,
    val practice_exercises: List<String>?,
    val daily_study_routine: String?,
    val modules: List<Module>?
)

data class LearningPathRequest(
    val session_id: String,
    val tier: String,
    val grammar_score: Int,
    val vocabulary_score: Int,
    val coherence_score: Int,
    val structural_break: Boolean,
    val detected_strength: String,
    val detected_weakness: String,
    val recommended_focus: String
)
