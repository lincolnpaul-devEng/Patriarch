package com.campus.network.core.network

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class CloudinaryUploadResult(
    val url: String,
    val bytes: Long,
    val publicId: String?
)

class CloudinaryUploader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Uploads an image and returns the Cloudinary secure URL.
     * Also registers the asset against the signed-in user's Patriarch storage quota.
     */
    suspend fun uploadFromUri(
        context: Context,
        uri: Uri,
        category: String = "other"
    ): Result<String> = uploadDetailedFromUri(context, uri, category).map { it.url }

    suspend fun uploadDetailedFromUri(
        context: Context,
        uri: Uri,
        category: String = "other"
    ): Result<CloudinaryUploadResult> = withContext(Dispatchers.IO) {
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
                uploadDetailedBytes(
                    bytes,
                    fileName = "device_upload_${System.currentTimeMillis()}.$extension",
                    mimeType = mimeType,
                    category = category
                )
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun uploadBytes(
        bytes: ByteArray,
        fileName: String = "image_${System.currentTimeMillis()}.jpg",
        mimeType: String = "image/jpeg",
        category: String = "other"
    ): Result<String> = uploadDetailedBytes(bytes, fileName, mimeType, category).map { it.url }

    suspend fun uploadDetailedBytes(
        bytes: ByteArray,
        fileName: String = "image_${System.currentTimeMillis()}.jpg",
        mimeType: String = "image/jpeg",
        category: String = "other"
    ): Result<CloudinaryUploadResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(bytes.size <= 10 * 1024 * 1024) { "Image exceeds 10 MB" }
            require(mimeType in listOf("image/jpeg", "image/png", "image/webp", "image/gif")) {
                "Unsupported image type"
            }
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

            val uploaded = client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorMsg = runCatching {
                        JSONObject(responseBody).optJSONObject("error")?.optString("message")
                    }.getOrNull()?.takeIf(String::isNotBlank) ?: "HTTP ${response.code}: $responseBody"
                    throw IllegalStateException("Cloudinary upload failed: $errorMsg")
                }
                val json = JSONObject(responseBody)
                val url = json.optString("secure_url").takeIf { it.isNotBlank() }
                    ?: json.optString("url").takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Cloudinary response missing secure_url")
                CloudinaryUploadResult(
                    url = url,
                    bytes = json.optLong("bytes").takeIf { it > 0L } ?: bytes.size.toLong(),
                    publicId = json.optString("public_id").takeIf { it.isNotBlank() }
                )
            }

            registerMediaAsset(
                url = uploaded.url,
                bytes = uploaded.bytes,
                category = category,
                publicId = uploaded.publicId
            )
            uploaded
        }
    }

    private fun registerMediaAsset(
        url: String,
        bytes: Long,
        category: String,
        publicId: String?
    ) {
        runCatching {
            val accessToken = SupabaseSessionStore.accessToken() ?: return
            val payload = JSONObject().apply {
                put("p_asset_url", url)
                put("p_bytes", bytes)
                put("p_category", category)
                put("p_public_id", publicId)
            }.toString()
            val request = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/rpc/register_user_media_asset")
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(
                        TAG,
                        "register_user_media_asset failed HTTP ${response.code}: ${response.body?.string().orEmpty()}"
                    )
                }
            }
        }.onFailure { error ->
            Log.w(TAG, "register_user_media_asset error: ${error.message}")
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

    companion object {
        private const val TAG = "CloudinaryUploader"
    }
}
