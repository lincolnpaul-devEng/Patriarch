package com.campus.network.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Simple, non-compose connectivity checks for ViewModels and repositories.
 *
 * Mirrors the validated-capability logic used by the app's network callback so the
 * UI can distinguish "saved offline" from "the server rejected the request".
 */
object ConnectivityMonitor {

    fun isCurrentlyOnline(context: Context): Boolean {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return@runCatching false
            val activeNetwork = cm.activeNetwork ?: return@runCatching false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return@runCatching false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }.getOrDefault(false)
    }
}
