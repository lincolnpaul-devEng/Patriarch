package com.campus.network.core.network

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CloudinaryUploader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun uploadFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        val uploadData = runCatching {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Could not open input stream from uri: $uri")
            val bytes = inputStream.use { it.readBytes() }
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                mimeType.contains("gif") -> "gif"
                else -> "jpg"
            }
            Triple(bytes, mimeType, extension)
        }
        uploadData.fold(
            onSuccess = { (bytes, mimeType, extension) ->
                uploadBytes(
                    bytes,
                    fileName = "device_upload_${System.currentTimeMillis()}.$extension",
                    mimeType = mimeType
                )
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun uploadBytes(
        bytes: ByteArray,
        fileName: String = "image_${System.currentTimeMillis()}.jpg",
        mimeType: String = "image/jpeg"
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(bytes.size <= 10 * 1024 * 1024) { "Image exceeds 10 MB" }
            require(mimeType in listOf("image/jpeg", "image/png", "image/webp", "image/gif")) { "Unsupported image type" }
            val signature = requestUploadSignature()

            val requestBody = MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("api_key", signature.apiKey)
                .addFormDataPart("timestamp", signature.timestamp)
                .addFormDataPart("folder", signature.folder)
                .addFormDataPart("signature", signature.signature)
                .addFormDataPart("file", fileName, bytes.toRequestBody(mimeType.toMediaTypeOrNull()))
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/${signature.cloudName}/image/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorMsg = runCatching {
                        JSONObject(responseBody).optJSONObject("error")?.optString("message")
                    }.getOrNull()?.takeIf(String::isNotBlank) ?: "HTTP ${response.code}: $responseBody"
                    throw IllegalStateException("Cloudinary upload failed: $errorMsg")
                }
                val json = JSONObject(responseBody)
                json.optString("secure_url").takeIf(String::isNotBlank)
                    ?: json.optString("url").takeIf(String::isNotBlank)
                    ?: throw IllegalStateException("Cloudinary response missing secure_url")
            }
            response
        }
    }

    private suspend fun requestUploadSignature(): UploadSignature {
        val accessToken = SupabaseSessionStore.validAccessToken(
            BuildConfig.SUPABASE_URL,
            BuildConfig.SUPABASE_ANON_KEY
        )
        val request = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}/functions/v1/cloudinary-signature")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $accessToken")
            .post(ByteArray(0).toRequestBody(null))
            .build()

        return client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Could not prepare image upload: HTTP ${response.code}")
            }
            val json = JSONObject(responseBody)
            UploadSignature(
                cloudName = json.getString("cloudName"),
                apiKey = json.getString("apiKey"),
                folder = json.getString("folder"),
                timestamp = json.getString("timestamp"),
                signature = json.getString("signature")
            )
        }
    }

    private data class UploadSignature(
        val cloudName: String,
        val apiKey: String,
        val folder: String,
        val timestamp: String,
        val signature: String
    )
}
