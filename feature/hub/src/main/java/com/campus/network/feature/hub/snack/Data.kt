package com.campus.network.feature.hub.snack

import androidx.compose.ui.graphics.Color

enum class Cat { Choco, Chips, Candy }

data class Product(
    val id: String,
    val name: String,
    val brand: String,
    val cat: Cat,
    val label: String,
    val price: Int,
    val bg: Color,
    val bag: Color,
    val top: Color? = null,
    val lb: String,
    val tc: Color,
    val ac: Color
)

val Products = listOf(
    Product("smiths", "Smiths Chips", "Smiths", Cat.Chips, "Chips", 7, Color(0xFFF9E8A8), Color(0xFFF2A21B), null, "SMITHS", Color(0xFFD4201C), Color(0xFFFFF3D0)),
    Product("dang", "Coconut Chips", "Dang", Cat.Chips, "Chips", 6, Color(0xFFD9EFC3), Color(0xFF8CC63F), null, "dang", Color.White, Color(0xFF8A5A3A)),
    Product("russet", "Dark Russet", "Idahoan", Cat.Chips, "Chips", 8, Color(0xFFEAD9D3), Color(0xFF47433D), null, "IDAHO", Color.White, Color(0xFFB8935F)),
    Product("ruffles", "Regular Nature", "Ruffles", Cat.Chips, "Chips", 6, Color(0xFFD8D5F7), Color(0xFF3F8DE0), Color(0xFFD92C27), "Ruffles", Color.White, Color(0xFFF1E4A0)),
    Product("twister", "Twister Chips", "Twisties", Cat.Chips, "Chips", 4, Color(0xFFF7D3D8), Color(0xFFE53527), null, "TWISTIES", Color(0xFFFFD23F), Color(0xFFF09A2B)),
    Product("river", "Deep River", "Deep River", Cat.Chips, "Chips", 4, Color(0xFFD8CDF3), Color(0xFF6A55B8), null, "DEEP RIVER", Color(0xFFF3E66A), Color(0xFFF0D75B)),
    Product("good", "Good Source", "Campus Tech", Cat.Choco, "Chocolates", 8, Color(0xFFF8D9E3), Color(0xFFF1F0EE), Color(0xFFE98AA8), "GOOD SOURCE", Color(0xFF222222), Color(0xFF5A3A2E)),
    Product("perfect", "Perfect Edition", "Study Hub", Cat.Choco, "Chocolates", 6, Color(0xFFD9D0F3), Color(0xFFF4F1FB), Color(0xFF7B5BD1), "PERFECT", Color(0xFFD9362B), Color(0xFF5A3A2E)),
    Product("unreal", "Unreal Muffins", "Unreal", Cat.Candy, "Chocolates", 6, Color(0xFFE3F0A0), Color(0xFF8CC63F), null, "UNREAL", Color.White, Color(0xFFB4472C)),
    Product("dark", "Dark Chocolate", "Campus Store", Cat.Candy, "Chocolates", 8, Color(0xFFE6DCF7), Color(0xFFF4F1FB), Color(0xFF4A3B6B), "DARK", Color(0xFF4A3B6B), Color(0xFF3B2A22))
)

data class CartLine(val id: String, val q: Int)
