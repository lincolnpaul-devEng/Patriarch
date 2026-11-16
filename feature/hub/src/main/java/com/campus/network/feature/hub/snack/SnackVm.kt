package com.campus.network.feature.hub.snack

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SnackScreen { Grid, Home, Detail, Cart }

class SnackVm : ViewModel() {
    var screen by mutableStateOf(SnackScreen.Grid)
    var prev by mutableStateOf(SnackScreen.Home)
    var tab by mutableStateOf<Cat?>(Cat.Choco)       // null = All
    var current by mutableStateOf("dang")
    var qty by mutableIntStateOf(1)
    var addEvents by mutableIntStateOf(0)

    // Session cart starts empty
    val cart = mutableStateListOf<CartLine>()

    // Auto-dismissing cart bottom sheet / floating bar visibility
    var isCartBarVisible by mutableStateOf(false)
    private var cartBarDismissJob: Job? = null

    fun go(s: SnackScreen) {
        if (s == SnackScreen.Detail) prev = screen
        screen = s
    }

    fun add(id: String, q: Int) {
        val i = cart.indexOfFirst { it.id == id }
        if (i >= 0) {
            cart[i] = cart[i].copy(q = cart[i].q + q)
        } else {
            cart.add(CartLine(id, q))
        }
        addEvents++
        showCartBarTemporarily()
    }

    fun showCartBarTemporarily(durationMs: Long = 4500L) {
        if (cart.isEmpty()) {
            isCartBarVisible = false
            return
        }
        isCartBarVisible = true
        cartBarDismissJob?.cancel()
        cartBarDismissJob = viewModelScope.launch {
            delay(durationMs)
            isCartBarVisible = false
        }
    }

    fun toggleCartBar() {
        if (isCartBarVisible) {
            cartBarDismissJob?.cancel()
            isCartBarVisible = false
        } else {
            showCartBarTemporarily()
        }
    }

    fun dismissCartBar() {
        cartBarDismissJob?.cancel()
        isCartBarVisible = false
    }
}
