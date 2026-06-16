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

// ─── AILab Data Models ──────────────────────────────────────────────────────

data class AILabSessionStartRequest(
    val user_id: String,
    val topic: String?,
    val voice_gender: String,
    val tone: String
)

data class AILabSessionStartResponse(
    val session_id: String,
    val status: String,
    val message: String,
    val voice_gender: String,
    val tone: String,
    val opening_message: String = ""   // AI's first line — display immediately
)

data class AILabStatusResponse(
    val sessions_used_today: Int,
    val sessions_limit: Int,
    val bonus_sessions: Int,
    val sessions_remaining: Int
)

data class AILabMistake(
    val wrong: String,
    val correct: String,
    val explanation: String,
    val mistake_type: String
)

data class AILabChatResponse(
    val ai_response: String,
    val mistakes: List<AILabMistake> = emptyList()
)

data class AILabEndSessionRequest(
    val session_id: String
)

data class AILabEndSessionResponse(
    val vocabulary_learned: Int,
    val grammar_mistakes: Int,
    val strengths: String,
    val weaknesses: String
)

// ─── AILab API Client ───────────────────────────────────────────────────────

object AILabApi {
    private const val BASE_URL = "https://lingoai-backend-zej0.onrender.com"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun startSession(
        userId: String,
        topic: String,
        voiceGender: String,
        tone: String,
        onResult: (AILabSessionStartResponse?) -> Unit
    ) {
        val requestBody = AILabSessionStartRequest(userId, topic, voiceGender, tone)
        val json = gson.toJson(requestBody)

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/ailab/start")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AILabApi", "Failed to start session", e)
                onResult(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e("AILabApi", "Start session unsuccessful: ${it.code}")
                        onResult(null)
                        return
                    }
                    val bodyString = it.body?.string()
                    try {
                        val result = gson.fromJson(bodyString, AILabSessionStartResponse::class.java)
                        onResult(result)
                    } catch (e: Exception) {
                        Log.e("AILabApi", "Failed to parse start session response", e)
                        onResult(null)
                    }
                }
            }
        })
    }

    fun submitChat(
        userId: String,
        sessionId: String,
        message: String?,
        audioFile: File?,
        onResult: (AILabChatResponse?) -> Unit
    ) {
        val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("user_id", userId)
            .addFormDataPart("session_id", sessionId)

        if (message != null) {
            multipartBuilder.addFormDataPart("message", message)
        }
        if (audioFile != null) {
            val fileBody = audioFile.asRequestBody("audio/mp4".toMediaType())
            multipartBuilder.addFormDataPart("audio_file", audioFile.name, fileBody)
        }

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/ailab/chat")
            .post(multipartBuilder.build())
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AILabApi", "Failed to submit chat", e)
                onResult(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e("AILabApi", "Submit chat unsuccessful: ${it.code}")
                        onResult(null)
                        return
                    }
                    val bodyString = it.body?.string()
                    try {
                        val result = gson.fromJson(bodyString, AILabChatResponse::class.java)
                        onResult(result)
                    } catch (e: Exception) {
                        Log.e("AILabApi", "Failed to parse chat response", e)
                        onResult(null)
                    }
                }
            }
        })
    }

    fun endSession(sessionId: String, onResult: (AILabEndSessionResponse?) -> Unit) {
        val requestBody = AILabEndSessionRequest(sessionId)
        val json = gson.toJson(requestBody)

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/ailab/end")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AILabApi", "Failed to end session", e)
                onResult(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e("AILabApi", "End session unsuccessful: ${it.code}")
                        onResult(null)
                        return
                    }
                    val bodyString = it.body?.string()
                    try {
                        val result = gson.fromJson(bodyString, AILabEndSessionResponse::class.java)
                        onResult(result)
                    } catch (e: Exception) {
                        Log.e("AILabApi", "Failed to parse end session response", e)
                        onResult(null)
                    }
                }
            }
        })
    }

    fun getStatus(userId: String, onResult: (AILabStatusResponse?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/ailab/status?user_id=$userId")
            .get()
            .header("accept", "application/json")
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("AILabApi", "Failed to get status", e)
                onResult(null)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) { onResult(null); return }
                    try {
                        onResult(gson.fromJson(it.body?.string(), AILabStatusResponse::class.java))
                    } catch (e: Exception) { onResult(null) }
                }
            }
        })
    }
}
