package com.campus.network.core.network

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Cloudinary Image Uploader Service for Campus Network.
 * Handles fast, direct-from-device image uploads (avatars, server banners, media attachments)
 * using signed HMAC-SHA1 requests against the Cloudinary REST API.
 */
class CloudinaryUploader(
    private val cloudName: String = BuildConfig.CLOUDINARY_CLOUD_NAME,
    private val apiKey: String = BuildConfig.CLOUDINARY_API_KEY,
    private val apiSecret: String = BuildConfig.CLOUDINARY_API_SECRET
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Upload an image from an Android content Uri (e.g. from photo picker).
     * Returns the HTTPS secure URL on Cloudinary.
     */
    suspend fun uploadFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
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
            uploadBytes(bytes, fileName = "device_upload_${System.currentTimeMillis()}.$extension", mimeType = mimeType).getOrThrow()
        }
    }

    /**
     * Upload raw image bytes directly to Cloudinary.
     */
    suspend fun uploadBytes(
        bytes: ByteArray,
        fileName: String = "image_${System.currentTimeMillis()}.jpg",
        mimeType: String = "image/jpeg"
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            // Cloudinary signed upload signature: SHA-1 of sorted parameters concatenated with api_secret
            val toSign = "timestamp=$timestamp$apiSecret"
            val signature = sha1(toSign)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", apiKey)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("signature", signature)
                .addFormDataPart(
                    name = "file",
                    filename = fileName,
                    body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = runCatching {
                    JSONObject(responseBody).optJSONObject("error")?.optString("message")
                }.getOrNull() ?: "HTTP ${response.code}: $responseBody"
                throw IllegalStateException("Cloudinary upload failed: $errorMsg")
            }

            val json = JSONObject(responseBody)
            json.optString("secure_url").ifEmpty {
                json.optString("url")
            }.takeIf { it.isNotBlank() } ?: throw IllegalStateException("Cloudinary response missing secure_url: $responseBody")
        }
    }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
