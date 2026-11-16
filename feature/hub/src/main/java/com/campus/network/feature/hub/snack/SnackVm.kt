package com.campus.network.feature.hub.snack

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

enum class SnackScreen { Grid, Home, Detail, Cart }

class SnackVm : ViewModel() {
    var screen by mutableStateOf(SnackScreen.Grid)
    var prev by mutableStateOf(SnackScreen.Home)
    var tab by mutableStateOf<Cat?>(Cat.Choco)       // null = All
    var current by mutableStateOf("dang")
    var qty by mutableIntStateOf(1)
    var addEvents by mutableIntStateOf(0)

    val cart = mutableStateListOf(
        CartLine("dark", 1),
        CartLine("good", 1),
        CartLine("dang", 3),
        CartLine("smiths", 1)
    )

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
    }
}
