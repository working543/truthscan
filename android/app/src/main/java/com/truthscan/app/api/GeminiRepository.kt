package com.truthscan.app.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.truthscan.app.BuildConfig
import com.truthscan.app.service.AnalysisResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun analyzeImage(
        imageBase64: String,
        mimeType: String,
        callback: (Result<AnalysisResult>) -> Unit
    ) {
        scope.launch {
            try {
                val requestBody = AnalyzeRequest(
                    imageBase64 = imageBase64,
                    mimeType = mimeType
                )

                val jsonBody = gson.toJson(requestBody)
                val request = Request.Builder()
                    .url("${BuildConfig.BACKEND_URL}/api/analyze")
                    .header("Content-Type", "application/json")
                    .header("x-app-secret", BuildConfig.APP_SHARED_SECRET)
                    .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: throw Exception("Empty response")
                    val apiResponse = gson.fromJson(responseBody, ApiResponse::class.java)

                    if (apiResponse.success && apiResponse.data != null) {
                        val result = AnalysisResult(
                            credibility = apiResponse.data.credibility,
                            verdict = apiResponse.data.verdict,
                            reason = apiResponse.data.reason,
                            suggestions = apiResponse.data.suggestions
                        )
                        callback(Result.success(result))
                    } else {
                        callback(Result.failure(Exception(apiResponse.error ?: "Unknown error")))
                    }
                } else {
                    val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                    callback(Result.failure(Exception("API error: $errorBody")))
                }
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

    data class AnalyzeRequest(
        @SerializedName("imageBase64")
        val imageBase64: String,
        @SerializedName("mimeType")
        val mimeType: String
    )

    data class ApiResponse(
        @SerializedName("success")
        val success: Boolean,
        @SerializedName("data")
        val data: AnalysisData?,
        @SerializedName("error")
        val error: String?
    )

    data class AnalysisData(
        @SerializedName("credibility")
        val credibility: Int,
        @SerializedName("verdict")
        val verdict: String,
        @SerializedName("reason")
        val reason: List<String>,
        @SerializedName("suggestions")
        val suggestions: List<String>
    )
}
