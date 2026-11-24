package com.ntk.callshield.domain

object PhoneNumberNormalizer {

    /**
     * Normalizes an arbitrary phone handle (e.g., "+1 (800) 555-0199", "tel:+234-700-555-019")
     * into a canonical E.164-like string (e.g., "+18005550199").
     */
    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val trimmed = raw.trim().removePrefix("tel:")
        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = buildString(trimmed.length) {
            for (ch in trimmed) {
                if (ch.isDigit()) append(ch)
            }
        }
        if (digitsOnly.isEmpty()) return ""
        return when {
            hasPlus -> "+$digitsOnly"
            digitsOnly.length == 10 -> "+1$digitsOnly"
            digitsOnly.length == 11 && digitsOnly.startsWith("1") -> "+$digitsOnly"
            else -> "+$digitsOnly"
        }
    }

    fun isHiddenOrPrivate(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return true
        val lower = raw.trim().lowercase()
        if (lower in setOf("private", "unknown", "anonymous", "restricted", "payphone", "unavailable")) {
            return true
        }
        return normalize(raw).isEmpty()
    }

    fun extractCountryCode(normalized: String): String {
        if (!normalized.startsWith("+") || normalized.length < 3) return ""
        val digits = normalized.substring(1)
        return when {
            digits.startsWith("1") -> "+1"
            digits.startsWith("44") -> "+44"
            digits.startsWith("33") -> "+33"
            digits.startsWith("49") -> "+49"
            digits.startsWith("81") -> "+81"
            digits.startsWith("82") -> "+82"
            digits.startsWith("86") -> "+86"
            digits.startsWith("91") -> "+91"
            digits.startsWith("224") -> "+224"
            digits.startsWith("232") -> "+232"
            digits.startsWith("233") -> "+233"
            digits.startsWith("234") -> "+234"
            digits.startsWith("254") -> "+254"
            digits.startsWith("353") -> "+353"
            digits.startsWith("372") -> "+372"
            digits.startsWith("375") -> "+375"
            digits.startsWith("881") -> "+881"
            else -> "+${digits.take(2)}"
        }
    }

    fun extractUsAreaCode(normalized: String): String {
        if (normalized.startsWith("+1") && normalized.length >= 5) {
            return normalized.substring(2, 5)
        }
        return ""
    }

    /**
     * Matches normalized phone numbers against user-defined patterns such as "+234-700-*",
     * "+1-800-*", or exact country/area prefixes.
     */
    fun matchesPattern(normalized: String, pattern: String): Boolean {
        if (normalized.isEmpty() || pattern.isBlank()) return false
        val cleanedPattern = pattern.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        val hasWildcard = cleanedPattern.contains("*")
        val prefix = cleanedPattern.substringBefore("*")
        val normalizedPrefix = if (prefix.startsWith("+")) prefix else "+$prefix"

        return if (hasWildcard) {
            normalized.startsWith(normalizedPrefix) ||
                normalized.removePrefix("+").startsWith(prefix.removePrefix("+"))
        } else {
            normalized.startsWith(normalizedPrefix) ||
                normalized == normalizedPrefix ||
                normalized.removePrefix("+1").startsWith(prefix.removePrefix("+"))
        }
    }

    fun formatForDisplay(raw: String): String {
        val norm = normalize(raw)
        if (norm.isEmpty()) return "Private Number"
        if (norm.startsWith("+1") && norm.length == 12) {
            val area = norm.substring(2, 5)
            val mid = norm.substring(5, 8)
            val end = norm.substring(8, 12)
            return "+1 ($area) $mid-$end"
        }
        return norm
    }
}
