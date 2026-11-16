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
    val name = cleanDisplayName(this)
    return name.isNotEmpty() &&
        '@' !in name &&
        name.lowercase() !in nonProfileDisplayNames
}

fun cleanDisplayName(name: String?): String {
    if (name.isNullOrBlank()) return ""
    var cleaned = name
        .replace(Regex("\\s*\\(Verified Student Seller\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s*\\(Verified Student\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s*\\(Verified\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s*\\(Seller\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s*\\(Member\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("_+verified_student_seller_?$", RegexOption.IGNORE_CASE), "")
        .replace(Regex("_+verified_student_?$", RegexOption.IGNORE_CASE), "")
        .replace(Regex("_+verified_?$", RegexOption.IGNORE_CASE), "")
        .trim()
    if (cleaned.contains('_') && !cleaned.contains(' ')) {
        cleaned = cleaned.split('_').filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
    return cleaned.trim()
}

fun String.cleanProfileDisplayName(): String = cleanDisplayName(this)
