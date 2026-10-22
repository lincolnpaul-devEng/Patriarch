package com.campus.network.core.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Tracks per-table [lastSyncedAt] epoch-ms watermarks so incremental (delta) syncs
 * only pull rows changed since the last successful sync.
 *
 * Stored in SharedPreferences under key "campus_sync_watermarks". Each entry is keyed
 * by the Supabase table name (e.g. "servers", "campus_market_items") and stores the
 * latest `updated_at` / `created_at` timestamp seen from the remote API.
 */
object SyncWatermarkStore {
    private const val PREFS = "campus_sync_watermarks"
    private const val KEY_PREFIX = "watermark_"

    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    private fun p(): SharedPreferences = prefs ?: throw IllegalStateException("SyncWatermarkStore not initialized")

    /** Returns the last synced epoch-ms for [table], or 0L if never synced. */
    fun getLastSyncedAt(table: String): Long {
        return p().getLong(KEY_PREFIX + table, 0L)
    }

    /** Persists the new watermark for [table]. Should be called after a successful delta sync. */
    fun putLastSyncedAt(table: String, timestampMs: Long) {
        p().edit().putLong(KEY_PREFIX + table, timestampMs).apply()
    }

    /** Clears the watermark for [table] (forces a full re-sync on next delta sync). */
    fun clearWatermark(table: String) {
        p().edit().remove(KEY_PREFIX + table).apply()
    }

    /** Clears all watermarks (forces full re-sync of all tables). */
    fun clearAllWatermarks() {
        p().edit().clear().apply()
    }
}
