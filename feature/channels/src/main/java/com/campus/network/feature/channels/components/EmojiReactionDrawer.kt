package com.campus.network.feature.channels.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.campus.network.core.ui.components.GiphyGif
import com.campus.network.core.ui.components.HybridEmojiReactionDrawer as CoreHybridEmojiReactionDrawer

typealias GiphyGif = com.campus.network.core.ui.components.GiphyGif
typealias EmojiDrawerTab = com.campus.network.core.ui.components.EmojiDrawerTab

@Composable
fun HybridEmojiReactionDrawer(
    onEmojiPicked: (String) -> Unit,
    onGifPicked: (GiphyGif) -> Unit,
    modifier: Modifier = Modifier
) {
    CoreHybridEmojiReactionDrawer(
        onEmojiPicked = onEmojiPicked,
        onGifPicked = onGifPicked,
        modifier = modifier
    )
}
