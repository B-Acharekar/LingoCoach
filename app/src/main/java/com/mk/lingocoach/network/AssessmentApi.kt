package com.mk.lingocoach.network

import android.util.Log
import com.google.gson.Gson
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
}

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
