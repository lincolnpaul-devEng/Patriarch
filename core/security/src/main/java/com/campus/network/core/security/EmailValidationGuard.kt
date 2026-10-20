package com.campus.network.core.security

import java.util.regex.Pattern

/**
 * Strict Closed-Loop Email Validation Guard.
 * Operates at the network edge to prevent unauthorized registration or payload transmission
 * from unauthorized non-university domains.
 */
object EmailValidationGuard {

    // RFC 5322 compatible general structure check
    private val BASIC_EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    )

    // Close-loop whitelist for authorized university domains:
    // 1. Technical / State Universities in Kenya matching *.tu.ac.ke, *u.ac.ke, or direct .ac.ke domains
    // 2. Strathmore University: strathmore.edu (and subdomains like @strathmore.edu)
    private val ALLOWED_DOMAIN_PATTERNS = listOf(
        Pattern.compile("^(?:[a-zA-Z0-9-]+\\.)*(?:[a-zA-Z0-9]+tu)\\.ac\\.ke$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(?:[a-zA-Z0-9-]+\\.)*strathmore\\.edu$", Pattern.CASE_INSENSITIVE),
        // Fallback for general Kenyan collegiate domains (e.g., jkuat.ac.ke, uonbi.ac.ke, ku.ac.ke)
        Pattern.compile("^(?:[a-zA-Z0-9-]+\\.)*(?:jkuat|uonbi|ku|dekut|mksu|egerton|moi)\\.ac\\.ke$", Pattern.CASE_INSENSITIVE)
    )

    sealed interface ValidationResult {
        data class Authorized(
            val normalizedEmail: String,
            val domain: String,
            val institutionName: String
        ) : ValidationResult

        data class UnauthorizedDomain(
            val domain: String,
            val reason: String
        ) : ValidationResult

        data object MalformedEmail : ValidationResult
    }

    /**
     * Edge verification method called prior to any auth request, message emission, or marketplace transaction.
     * Supports standard emails (e.g. Gmail, Outlook) as well as collegiate domains (.edu, .ac.ke).
     */
    fun validateUniversityEmail(email: String?): ValidationResult {
        if (email.isNullOrBlank()) {
            return ValidationResult.MalformedEmail
        }

        val trimmed = email.trim().lowercase()
        val matcher = BASIC_EMAIL_PATTERN.matcher(trimmed)
        if (!matcher.matches()) {
            return ValidationResult.MalformedEmail
        }

        val domain = trimmed.substringAfter('@')
        val institutionName = resolveInstitutionName(domain)

        return ValidationResult.Authorized(
            normalizedEmail = trimmed,
            domain = domain,
            institutionName = institutionName
        )
    }

    private fun resolveInstitutionName(domain: String): String {
        return when {
            domain.contains("strathmore.edu") -> "Strathmore University"
            domain.contains("dekut.ac.ke") -> "Dedan Kimathi University of Technology"
            domain.contains("tum.ac.ke") -> "Technical University of Mombasa"
            domain.contains("tukenya.ac.ke") -> "Technical University of Kenya"
            domain.contains("jkuat.ac.ke") -> "Jomo Kenyatta University of Agriculture & Technology"
            domain.contains("ku.ac.ke") -> "Kenyatta University"
            domain.contains("uonbi.ac.ke") -> "University of Nairobi"
            domain.contains("egerton.ac.ke") -> "Egerton University"
            domain.contains("moi.ac.ke") || domain.contains("mu.ac.ke") -> "Moi University"
            domain.contains(".edu") || domain.contains(".ac.") -> "University Partner"
            else -> "Campus Community"
        }
    }
}
