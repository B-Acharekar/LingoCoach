package com.mk.lingocoach.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.mk.lingocoach.config.AppConfig
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
    val next_question: String,
    val user_name: String = "",   // echoed back from backend
    val username: String = ""
)

data class UsernameLookupResponse(
    val session_id: String,
    val username: String,
    val user_name: String = ""
)

data class UsernameAvailabilityResponse(
    val username: String,
    val available: Boolean
)

data class ProfilePatchResult(
    val ok: Boolean,
    val statusCode: Int = 0,
    val message: String = ""
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

data class FullAssessmentAnswer(
    val step: Int,
    val question: String,
    val answer: String
)

data class FullAssessmentRequest(
    val session_id: String,
    val answers: List<FullAssessmentAnswer>
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
    val audio_transcription: String = "",
    val sublesson_complete: Boolean = false
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
    val last_reviewed: String?,
    val source: String = "unknown"
)

// ─────────────────────────────────────────────────────────────────────────────
object AssessmentApi {
    private val baseUrl: String
        get() = AppConfig.backendBaseUrl
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private const val LEARNING_PATH_TIMEOUT_SECONDS = 310L

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val learningPathClient = client.newBuilder()
        .callTimeout(LEARNING_PATH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(LEARNING_PATH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun createSession(
        userName: String = "",
        username: String = "",
        onResult: (SessionResponse?) -> Unit
    ) {
        val body = gson.toJson(mapOf("user_name" to userName, "username" to username))
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/sessions")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
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

    fun findUserByUsername(username: String, onResult: (UsernameLookupResponse?) -> Unit) {
        val normalized = username.trim().lowercase()
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/users/by-username/$normalized")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to find user by username", e)
                onResult(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("AssessmentApi", "Find user unsuccessful: ${response.code}")
                        onResult(null)
                        return
                    }
                    val bodyString = response.body?.string()
                    Log.d("AssessmentApi", "Find user response: $bodyString")
                    try {
                        val user = gson.fromJson(bodyString, UsernameLookupResponse::class.java)
                        onResult(user)
                    } catch (e: Exception) {
                        Log.e("AssessmentApi", "Failed to parse find user response", e)
                        onResult(null)
                    }
                }
            }
        })
    }

    fun checkUsernameAvailability(username: String, onResult: (UsernameAvailabilityResponse?) -> Unit) {
        val normalized = username.trim().lowercase()
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/usernames/$normalized/availability")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to check username availability", e)
                onResult(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("AssessmentApi", "Username availability unsuccessful: ${response.code}")
                        onResult(null)
                        return
                    }
                    val bodyString = response.body?.string()
                    Log.d("AssessmentApi", "Username availability response: $bodyString")
                    try {
                        val availability = gson.fromJson(bodyString, UsernameAvailabilityResponse::class.java)
                        onResult(availability)
                    } catch (e: Exception) {
                        Log.e("AssessmentApi", "Failed to parse username availability response", e)
                        onResult(null)
                    }
                }
            }
        })
    }

    fun submitTextAnswer(sessionId: String, answer: String, onResult: (AssessmentResponse?) -> Unit) {
        val json = gson.toJson(mapOf("session_id" to sessionId, "user_answer" to answer))
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/assess")
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
            .url("${baseUrl}/api/v1/assess/voice")
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

    fun transcribeVoiceAnswer(sessionId: String, audioFile: File, onResult: (AssessmentResponse?) -> Unit) {
        val fileBody = audioFile.asRequestBody("audio/mp4".toMediaType())
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("session_id", sessionId)
            .addFormDataPart("transcribe_only", "true")
            .addFormDataPart("audio_file", audioFile.name, fileBody)
            .build()

        val request = Request.Builder()
            .url("${baseUrl}/api/v1/assess/voice")
            .post(multipartBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to transcribe voice answer", e)
                onResult(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("AssessmentApi", "Transcribe voice unsuccessful: ${response.code}")
                        onResult(null)
                        return
                    }
                    val bodyString = response.body?.string()
                    Log.d("AssessmentApi", "Transcribe voice response: $bodyString")
                    try {
                        val result = gson.fromJson(bodyString, AssessmentResponse::class.java)
                        onResult(result)
                    } catch (e: Exception) {
                        Log.e("AssessmentApi", "Failed to parse transcription response", e)
                        onResult(null)
                    }
                }
            }
        })
    }

    fun submitFullAssessment(requestBody: FullAssessmentRequest, onResult: (AssessmentResponse?) -> Unit) {
        val json = gson.toJson(requestBody)
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/assess/final")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .header("accept", "application/json")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to submit full assessment", e)
                onResult(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("AssessmentApi", "Submit full assessment unsuccessful: ${response.code}")
                        onResult(null)
                        return
                    }
                    val bodyString = response.body?.string()
                    Log.d("AssessmentApi", "Submit full assessment response: $bodyString")
                    try {
                        val result = gson.fromJson(bodyString, AssessmentResponse::class.java)
                        onResult(result)
                    } catch (e: Exception) {
                        Log.e("AssessmentApi", "Failed to parse full assessment response", e)
                        onResult(null)
                    }
                }
            }
        })
    }

    fun getLearningPath(requestBody: LearningPathRequest, onResult: (LearningPathResponse?) -> Unit) {
        val json = gson.toJson(requestBody)
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/learning-path")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .header("accept", "application/json")
            .header("Content-Type", "application/json")
            .build()

        learningPathClient.newCall(request).enqueue(object : okhttp3.Callback {
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

    fun getLearningPathStream(
        requestBody: LearningPathRequest,
        onProgress: (String) -> Unit,
        onResult: (LearningPathResponse?) -> Unit
    ) {
        val json = gson.toJson(requestBody)
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/learning-path/stream")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .header("accept", "text/event-stream")
            .header("Content-Type", "application/json")
            .build()

        learningPathClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to stream learning path", e)
                onResult(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("AssessmentApi", "Stream learning path unsuccessful: ${response.code}")
                        onResult(null)
                        return
                    }

                    val source = response.body?.source()
                    if (source == null) {
                        onResult(null)
                        return
                    }

                    var eventName = "message"
                    val dataLines = mutableListOf<String>()
                    var completed = false

                    fun dispatchEvent() {
                        if (dataLines.isEmpty()) return
                        val data = dataLines.joinToString("\n")
                        try {
                            when (eventName) {
                                "progress" -> {
                                    val message = JsonParser.parseString(data)
                                        .asJsonObject
                                        .get("message")
                                        ?.asString
                                    if (!message.isNullOrBlank()) onProgress(message)
                                }
                                "token" -> onProgress("Receiving your roadmap...")
                                "complete" -> {
                                    completed = true
                                    val result = gson.fromJson(data, LearningPathResponse::class.java)
                                    onResult(result)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AssessmentApi", "Failed to parse learning path stream event", e)
                        } finally {
                            eventName = "message"
                            dataLines.clear()
                        }
                    }

                    try {
                        while (true) {
                            val line = source.readUtf8Line() ?: break
                            when {
                                line.isEmpty() -> dispatchEvent()
                                line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                                line.startsWith("data:") -> dataLines.add(line.removePrefix("data:").trim())
                            }
                        }
                        dispatchEvent()
                        if (!completed) onResult(null)
                    } catch (e: IOException) {
                        Log.e("AssessmentApi", "Learning path stream interrupted", e)
                        onResult(null)
                    }
                }
            }
        })
    }
    fun getCurrentLearningPath(userId: String, onResult: (CurrentLearningPathResponse?) -> Unit) {
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/learning-path/current?user_id=$userId")
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
            .url("${baseUrl}/api/v1/sublessons/$sublessonId")
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
            .url("${baseUrl}/api/v1/exercise/complete")
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
            .url("${baseUrl}/api/v1/mistakes?user_id=$userId")
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

    fun markMistakeResolved(
        userId: String,
        mistakeId: String,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/mistakes/mark-resolved/$mistakeId?user_id=$userId")
            .post(ByteArray(0).toRequestBody(null))
            .header("accept", "application/json")
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to mark mistake resolved", e); onResult?.invoke(false)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { onResult?.invoke(it.isSuccessful) }
            }
        })
    }

    fun getVocabBookmarks(userId: String, onResult: (List<VocabBookmark>?) -> Unit) {
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/vocab/bookmarks?user_id=$userId")
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
            .url("${baseUrl}/api/v1/flashcards/review?user_id=$userId")
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
            .url("${baseUrl}/api/v1/flashcards/review")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .header("accept", "application/json")
            .build()
        client.newCall(httpRequest).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to review flashcard", e); onResult(false)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { onResult(response.isSuccessful) }
            }
        })
    }

    // ── Vocab sync ────────────────────────────────────────────────────────────

    fun syncVocab(
        userId: String,
        clientTime: String,
        changes: List<Map<String, Any>>,
        onResult: (Map<String, Any>?) -> Unit
    ) {
        val body = gson.toJson(mapOf("client_time" to clientTime, "changes" to changes))
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/vocab/sync?user_id=$userId")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .header("accept", "application/json")
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Vocab sync push failed", e); onResult(null)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) { onResult(null); return }
                    try {
                        val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                        onResult(gson.fromJson(it.body?.string(), type))
                    } catch (e: Exception) { onResult(null) }
                }
            }
        })
    }

    fun getVocabServerUpdates(
        userId: String,
        lastSyncTime: String?,
        onResult: (List<Map<String, Any>>?) -> Unit
    ) {
        val url = if (lastSyncTime != null)
            "${baseUrl}/api/v1/vocab/sync?user_id=$userId&last_sync_time=$lastSyncTime"
        else
            "${baseUrl}/api/v1/vocab/sync?user_id=$userId"
        val request = Request.Builder().url(url).get()
            .header("accept", "application/json").build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Vocab sync pull failed", e); onResult(null)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) { onResult(null); return }
                    try {
                        val raw  = gson.fromJson(it.body?.string(),
                            object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                        ) as? Map<*, *>
                        @Suppress("UNCHECKED_CAST")
                        onResult(raw?.get("updates") as? List<Map<String, Any>>)
                    } catch (e: Exception) { onResult(null) }
                }
            }
        })
    }

    fun getWeeklyAnalytics(userId: String, onResult: (List<DailyStats>?) -> Unit) {
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/analytics/weekly?user_id=$userId")
            .get()
            .header("accept", "application/json")
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to get weekly analytics", e); onResult(null)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) { onResult(null); return }
                    try {
                        val type = object : com.google.gson.reflect.TypeToken<List<DailyStats>>() {}.type
                        onResult(gson.fromJson(it.body?.string(), type))
                    } catch (e: Exception) { onResult(null) }
                }
            }
        })
    }

    // ── XP award ──────────────────────────────────────────────────────────────

    fun getProgressMetrics(userId: String, onResult: (ProgressMetrics?) -> Unit) {
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/progress/metrics?user_id=$userId")
            .get()
            .header("accept", "application/json")
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to get progress metrics", e); onResult(null)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) { onResult(null); return }
                    try {
                        onResult(gson.fromJson(it.body?.string(), ProgressMetrics::class.java))
                    } catch (e: Exception) { onResult(null) }
                }
            }
        })
    }

    fun awardXp(
        userId: String,
        xpDelta: Int,
        source: String,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        val body = gson.toJson(mapOf(
            "user_id"   to userId,
            "xp_delta"  to xpDelta,
            "source"    to source
        ))
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/progress/xp")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .header("accept", "application/json")
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to award XP", e); onResult?.invoke(false)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { onResult?.invoke(it.isSuccessful) }
            }
        })
    }

    // ── Settings profile patch ────────────────────────────────────────────────

    fun patchUserProfile(
        userId: String,
        fields: Map<String, Any?>,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        patchUserProfileDetailed(userId, fields) { result ->
            onResult?.invoke(result.ok)
        }
    }

    fun patchUserProfileDetailed(
        userId: String,
        fields: Map<String, Any?>,
        onResult: (ProfilePatchResult) -> Unit
    ) {
        val nonNull = fields.filterValues { it != null }
        if (nonNull.isEmpty()) {
            onResult(ProfilePatchResult(ok = true))
            return
        }
        val body = gson.toJson(nonNull)
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/progress/users/$userId")
            .patch(body.toRequestBody(JSON_MEDIA_TYPE))
            .header("accept", "application/json")
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Profile patch failed", e)
                onResult(ProfilePatchResult(ok = false, message = "Could not connect. Please try again."))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val bodyString = it.body?.string().orEmpty()
                    if (it.isSuccessful) {
                        onResult(ProfilePatchResult(ok = true, statusCode = it.code))
                        return
                    }

                    val detail = runCatching {
                        JsonParser.parseString(bodyString).asJsonObject.get("detail")?.asString
                    }.getOrNull().orEmpty()
                    val fallback = when (it.code) {
                        409 -> "This username is already taken."
                        400 -> "Please check the details and try again."
                        404 -> "Could not find your profile. Please sign in again."
                        else -> "Could not save your profile. Please try again."
                    }
                    onResult(ProfilePatchResult(ok = false, statusCode = it.code, message = detail.ifBlank { fallback }))
                }
            }
        })
    }

    // -- Direct mistake log ────────────────────────────────────────────────────

    fun logMistake(
        userId: String,
        word: String,
        mistakeType: String,
        userSentence: String,
        correctSentence: String,
        explanation: String,
        source: String = "unknown",
        onResult: ((Boolean) -> Unit)? = null
    ) {
        val body = gson.toJson(mapOf(
            "user_id"          to userId,
            "word"             to word,
            "mistake_type"     to mistakeType,
            "user_sentence"    to userSentence,
            "correct_sentence" to correctSentence,
            "explanation"      to explanation,
            "source"           to source
        ))
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/mistakes")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .header("accept", "application/json")
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AssessmentApi", "Failed to log mistake", e); onResult?.invoke(false)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { onResult?.invoke(it.isSuccessful) }
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
    val user_name: String = "",
    val tier: String,
    val grammar_score: Int,
    val vocabulary_score: Int,
    val coherence_score: Int,
    val structural_break: Boolean,
    val detected_strength: String,
    val detected_weakness: String,
    val recommended_focus: String,
    val user_goal: String = "general",
    val user_level_self_reported: String = "intermediate"
)

data class DailyStats(
    val user_id: String = "",
    val date: String = "",
    val lessons_completed: Int = 0,
    val exercises_attempted: Int = 0,
    val exercises_correct: Int = 0,
    val pronunciation_attempts: Int = 0,
    val pronunciation_score_total: Int = 0,
    val mistakes_logged: Int = 0,
    val vocab_drills_done: Int = 0,
    val vocab_words_mastered: Int = 0,
    val ai_lab_sessions: Int = 0,
    val ai_lab_minutes: Int = 0,
    val duel_sessions: Int = 0,
    val duel_correct: Int = 0,
    val xp_earned: Int = 0,
    val streak_day: Int = 0
)

data class ProgressActivity(
    val exercise_attempts: Int = 0,
    val exercise_correct: Int = 0,
    val vocab_words_mastered: Int = 0,
    val ai_lab_sessions: Int = 0,
    val ai_lab_minutes: Int = 0,
    val pronunciation_attempts: Int = 0,
    val pronunciation_score_total: Int = 0
)

data class ProgressMetrics(
    val user_id: String = "",
    val grammar_score: Int = 0,
    val vocabulary_score: Int = 0,
    val listening_score: Int = 0,
    val pronunciation_score: Int = 0,
    val coherence_score: Int = 0,
    val streak: Int = 0,
    val last_active_date: String? = null,
    val xp: Int = 0,
    val activity: ProgressActivity = ProgressActivity()
)


