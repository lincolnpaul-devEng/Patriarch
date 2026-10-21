package com.campus.network.core.model

private val nonProfileDisplayNames = setOf(
    "campus peer",
    "campus member",
    "campus student",
    "friend",
    "guest",
    "member",
    "new user",
    "peer",
    "student",
    "student peer",
    "student user",
    "unknown",
    "unknown user",
    "verified student",
    "you"
)

fun String.isRealProfileDisplayName(): Boolean {
    val name = trim()
    return name.isNotEmpty() &&
        '@' !in name &&
        name.lowercase() !in nonProfileDisplayNames
}
