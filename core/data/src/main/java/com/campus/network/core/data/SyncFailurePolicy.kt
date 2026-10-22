package com.campus.network.core.data

import com.campus.network.core.model.SyncStatus
import com.campus.network.core.network.SupabaseHttpException
import java.io.IOException

internal fun Result<String>.requireSavedRow(): Result<String> = mapCatching { body ->
    check(org.json.JSONArray(body).length() > 0) {
        "Remote save was accepted without returning a saved record"
    }
    body
}

internal fun syncStatusFor(result: Result<*>): SyncStatus {
    if (result.isSuccess) return SyncStatus.SYNCED
    var error = result.exceptionOrNull()
        ?: IllegalStateException("Remote sync failed without an error")
    while (true) {
        when (error) {
            is SupabaseHttpException -> return if (error.isRetryable) SyncStatus.PENDING else SyncStatus.FAILED
            is IOException -> return SyncStatus.PENDING
        }
        error = error.cause ?: return SyncStatus.FAILED
    }
}
