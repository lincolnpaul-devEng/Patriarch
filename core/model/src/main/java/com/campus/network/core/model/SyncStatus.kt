package com.campus.network.core.model

/** Sync lifecycle status for offline-first writes. */
enum class SyncStatus {
    PENDING,   // Written locally, awaiting remote sync
    SYNCED,    // Successfully synced to remote
    FAILED     // Remote sync failed; user action needed
}
