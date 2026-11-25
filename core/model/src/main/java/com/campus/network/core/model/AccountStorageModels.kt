package com.campus.network.core.model

/**
 * Real account cloud-storage usage for Patriarch student quotas.
 * Sourced from campusprofile + campus_user_media_assets (not mock UI data).
 */
data class AccountStorageUsage(
    val usedBytes: Long = 0L,
    val quotaBytes: Long = DEFAULT_QUOTA_BYTES,
    val categories: List<StorageCategoryUsage> = emptyList(),
    val pendingByteUrls: List<String> = emptyList()
) {
    val percentage: Float
        get() = if (quotaBytes > 0L) {
            (usedBytes.toFloat() / quotaBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    companion object {
        const val DEFAULT_QUOTA_BYTES: Long = 1_073_741_824L
        val Empty = AccountStorageUsage()
    }
}

data class StorageCategoryUsage(
    val category: String,
    val bytes: Long,
    val itemCount: Int
) {
    val displayTitle: String
        get() = when (category) {
            "avatar" -> "Profile Avatar"
            "market" -> "Marketplace Listings"
            "chat_media" -> "Chat Media & GIFs"
            "server" -> "Server Avatars & Banners"
            "stream" -> "Live Stream Backdrops"
            "document" -> "Documents & Files"
            "cache" -> "Cached Media"
            else -> "Other Media"
        }

    val displaySubtitle: String
        get() = when {
            itemCount <= 0 -> "No files tracked"
            itemCount == 1 -> "1 uploaded file"
            else -> "$itemCount uploaded files"
        }
}
