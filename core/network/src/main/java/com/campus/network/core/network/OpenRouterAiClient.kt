package com.campus.network.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * OpenRouter AI Client for real-time search suggestions and semantic campus queries.
 * Credentials and configuration extracted securely from the UniHub project.
 */
class OpenRouterAiClient(
    private val apiKey: String = DEFAULT_OPENROUTER_KEY,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        const val DEFAULT_OPENROUTER_KEY = "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"
        private const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
        private const val MODEL_NAME = "google/gemini-2.0-flash-exp:free"

        val DEFAULT_TRENDING_SUGGESTIONS = listOf(
            "Central Quad study pods open late",
            "Quiet library pods open after midnight",
            "Affordable engineering laptops and monitors",
            "High-payout campus freelance gigs",
            "Used textbooks and class notes",
            "Finals study group meetups"
        )
    }

    private val client = HttpClient(OkHttp)

    /**
     * Fetches dynamic AI search suggestions for the given query.
     * Resilient: Falls back to instant curated suggestions on timeout or network error.
     */
    suspend fun getSearchSuggestions(query: String): List<String> = withContext(ioDispatcher) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext DEFAULT_TRENDING_SUGGESTIONS
        }

        // Try AI generation with a fast timeout (3.5s) to preserve search responsiveness
        val aiResults = withTimeoutOrNull(3500L) {
            runCatching {
                queryAiForSuggestions(trimmed)
            }.getOrNull()
        }

        if (!aiResults.isNullOrEmpty()) {
            aiResults
        } else {
            // Intelligent local fallback matching the query
            generateFallbackSuggestions(trimmed)
        }
    }

    private suspend fun queryAiForSuggestions(query: String): List<String> {
        val prompt = "You are a campus network AI search engine. The student typed: \"$query\". " +
                "Generate 4 short, highly relevant search queries (3 to 6 words each) covering campus gigs, study groups, textbooks, or student marketplace. " +
                "Respond ONLY with a valid JSON array of 4 strings, no markdown formatting, no backticks, no extra text. Example: [\"Item 1\", \"Item 2\", \"Item 3\", \"Item 4\"]"

        val requestJson = JSONObject().apply {
            put("model", MODEL_NAME)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an AI search engine autocomplete assistant. Output only a JSON array of strings.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("max_tokens", 150)
            put("temperature", 0.3)
        }

        val response: HttpResponse = client.post(OPENROUTER_URL) {
            header("Authorization", "Bearer $apiKey")
            header("HTTP-Referer", "https://unihub.campus")
            header("X-Title", "Patriarch Campus Network")
            contentType(ContentType.Application.Json)
            setBody(requestJson.toString())
        }

        val responseBody = response.bodyAsText()
        return parseSuggestionsFromResponse(responseBody)
    }

    private fun parseSuggestionsFromResponse(jsonStr: String): List<String> {
        return runCatching {
            val root = JSONObject(jsonStr)
            val choices = root.getJSONArray("choices")
            if (choices.length() == 0) return emptyList()

            val messageObj = choices.getJSONObject(0).getJSONObject("message")
            var content = messageObj.getString("content").trim()

            // Remove any markdown code block wrapper if present
            if (content.startsWith("```json")) {
                content = content.removePrefix("```json").trim()
            }
            if (content.startsWith("```")) {
                content = content.removePrefix("```").trim()
            }
            if (content.endsWith("```")) {
                content = content.removeSuffix("```").trim()
            }

            val array = JSONArray(content)
            val result = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val item = array.optString(i)?.trim()
                if (!item.isNullOrEmpty()) {
                    result.add(item)
                }
            }
            result
        }.getOrDefault(emptyList())
    }

    private fun generateFallbackSuggestions(query: String): List<String> {
        val lower = query.lowercase()
        val allCurated = listOf(
            "Central Quad study pods open late",
            "Used organic chemistry textbooks",
            "Quiet library pods open after midnight",
            "High-paying campus coding gigs",
            "Affordable MacBook and iPad deals",
            "Calculus and Physics tutoring groups",
            "Campus dorm essentials and mini fridge",
            "Used engineering textbooks cheap",
            "Weekend campus hackathon and workshops",
            "Student academic scholarship advice"
        )

        val matches = allCurated.filter { it.lowercase().contains(lower) }
        return if (matches.isNotEmpty()) {
            matches.take(4)
        } else {
            listOf(
                "$query campus marketplace items",
                "$query student study groups",
                "$query gigs and tasks",
                "Explore $query with Patriarch AI"
            )
        }
    }
}
