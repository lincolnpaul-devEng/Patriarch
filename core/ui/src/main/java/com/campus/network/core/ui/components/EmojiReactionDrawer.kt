package com.campus.network.core.ui.components

import android.view.ContextThemeWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Giphy GIF model representation.
 */
data class GiphyGif(
    val id: String,
    val title: String,
    val previewUrl: String,
    val originalUrl: String,
    val width: Int = 0,
    val height: Int = 0
)

/**
 * High-performance Giphy Proxy Client using OkHttp with modern TLS 1.3 / HTTP2 support.
 * Connects to the Supabase Edge Function proxy configured in velo-desktop-video-editor.
 */
object GiphyProxyClient {
    private const val SUPABASE_URL = "https://mgurbmoubtqzsgfdlgur.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1ndXJibW91YnRxenNnZmRsZ3VyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODcwMjYyMTAsImV4cCI6MjEwMjYwMjIxMH0.fcjlhwoln44uisxPfddIlWD1zmLm9nJaoYIfTYTMaV0"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetchGifs(query: String = "", limit: Int = 24): List<GiphyGif> = withContext(Dispatchers.IO) {
        try {
            val isSearch = query.trim().isNotBlank()
            val action = if (isSearch) "search" else "trending"
            val qParam = if (isSearch) "&q=" + URLEncoder.encode(query.trim(), "UTF-8") else ""
            val endpoint = "$SUPABASE_URL/functions/v1/giphy-proxy?action=$action$qParam&limit=$limit"
            android.util.Log.d("GiphyProxy", "Requesting GIPHY via OkHttp: $endpoint")

            val request = Request.Builder()
                .url(endpoint)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .header("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            android.util.Log.d("GiphyProxy", "OkHttp Response code: $code")

            if (!response.isSuccessful) {
                android.util.Log.e("GiphyProxy", "Error response: ${response.body?.string()}")
                return@withContext emptyList()
            }

            val bodyString = response.body?.string() ?: return@withContext emptyList()
            val root = JSONObject(bodyString)
            val dataArray = root.optJSONArray("data") ?: JSONArray()
            android.util.Log.d("GiphyProxy", "Loaded ${dataArray.length()} GIF items from GIPHY")

            val result = mutableListOf<GiphyGif>()
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val id = item.optString("id", "")
                val title = item.optString("title", "GIF")
                val images = item.optJSONObject("images")

                val fixedHeight = images?.optJSONObject("fixed_height")?.optString("url")
                val fixedWidth = images?.optJSONObject("fixed_width")?.optString("url")
                val original = images?.optJSONObject("original")?.optString("url")
                val small = images?.optJSONObject("fixed_height_small")?.optString("url")

                val preview = fixedHeight ?: fixedWidth ?: original ?: small ?: ""
                val full = original ?: fixedHeight ?: preview
                val w = images?.optJSONObject("fixed_height")?.optInt("width", 0) ?: 0
                val h = images?.optJSONObject("fixed_height")?.optInt("height", 0) ?: 0

                if (preview.isNotBlank()) {
                    result.add(
                        GiphyGif(
                            id = id,
                            title = title,
                            previewUrl = preview,
                            originalUrl = full,
                            width = w,
                            height = h
                        )
                    )
                }
            }
            result
        } catch (e: Exception) {
            android.util.Log.e("GiphyProxy", "OkHttp fetch exception", e)
            emptyList()
        }
    }
}

/**
 * Hybrid Emoji & GIF Drawer:
 * - Tab 1: All Emojis (Google AndroidX Unicode 15.1+ EmojiPicker with category navigation)
 * - Tab 2: GIFs (Live GIPHY Search & Trending via Supabase Edge Function Proxy)
 */
@Composable
fun HybridEmojiReactionDrawer(
    onEmojiPicked: (String) -> Unit,
    onGifPicked: (GiphyGif) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(EmojiDrawerTab.ALL_EMOJIS) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE2E8F0))

        // Top Drawer Switcher: [ All Emojis ] [ GIFs ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmojiTabPill(
                title = "All Emojis",
                icon = Icons.Default.SentimentSatisfiedAlt,
                isSelected = selectedTab == EmojiDrawerTab.ALL_EMOJIS,
                onClick = { selectedTab = EmojiDrawerTab.ALL_EMOJIS },
                modifier = Modifier.weight(1f)
            )

            EmojiTabPill(
                title = "GIFs",
                icon = Icons.Default.Gif,
                isSelected = selectedTab == EmojiDrawerTab.GIFS,
                onClick = { selectedTab = EmojiDrawerTab.GIFS },
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFF1F5F9))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                EmojiDrawerTab.ALL_EMOJIS -> {
                    // Google AndroidX Official EmojiPickerView with themed category icons
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val themedContext = ContextThemeWrapper(
                                ctx,
                                com.campus.network.core.ui.R.style.Theme_Patriarch
                            )
                            EmojiPickerView(themedContext).apply {
                                setOnEmojiPickedListener { item ->
                                    onEmojiPicked(item.emoji)
                                }
                            }
                        }
                    )
                }

                EmojiDrawerTab.GIFS -> {
                    // Live GIPHY Search and Trending Grid
                    GiphyPanel(onGifPicked = onGifPicked)
                }
            }
        }
    }
}

/**
 * GIPHY Panel with search bar and responsive 2-column GIF grid.
 */
@Composable
private fun GiphyPanel(
    onGifPicked: (GiphyGif) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var gifList by remember { mutableStateOf<List<GiphyGif>>(emptyList()) }

    // Debounce search query input (350ms)
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            debouncedQuery = ""
        } else {
            delay(350)
            debouncedQuery = searchQuery.trim()
        }
    }

    // Trigger API fetch on debounced query change
    LaunchedEffect(debouncedQuery) {
        isLoading = true
        gifList = GiphyProxyClient.fetchGifs(debouncedQuery)
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Search Input Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search GIFs",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 13.5.sp,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Normal
                    ),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search GIPHY...",
                                fontSize = 13.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { searchQuery = "" }
                    )
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color(0xFF007AFF),
                    strokeWidth = 2.5.dp
                )
            }
        } else if (gifList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No trending GIFs found" else "No GIFs found for \"$searchQuery\"",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(gifList, key = { it.id }) { gif ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .clickable { onGifPicked(gif) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(gif.previewUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = gif.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiTabPill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF007AFF) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF007AFF) else Color(0xFFE2E8F0)),
        modifier = modifier
            .height(32.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF334155)
            )
        }
    }
}

enum class EmojiDrawerTab {
    ALL_EMOJIS,
    GIFS
}
