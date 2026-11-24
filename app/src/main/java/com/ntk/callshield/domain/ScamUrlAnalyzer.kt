package com.ntk.callshield.domain

import java.net.URI

/**
 * Pure on-device heuristic & NLP phishing analyzer for URLs and SMS text.
 * Runs 100% locally without internet access.
 */
object ScamUrlAnalyzer {

    private val urlRegex = Regex(
        pattern = """((https?://|www\.)[^\s<>"]+)|(\b[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)+(/[^\s<>"]*)?\b)""",
        option = RegexOption.IGNORE_CASE
    )

    private val ipHostRegex = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")
    private val otpRegex = Regex("""\b(?<!\d)(\d{6}|\d{4}|\d{3}-\d{3})(?!\d)\b""")

    private val shortenerDomains = setOf(
        "bit.ly", "tinyurl.com", "t.co", "rb.gy", "cutt.ly", "is.gd",
        "ow.ly", "shorturl.at", "short.link", "tiny.cc", "rebrand.ly", "goo.gl"
    )

    private val suspiciousTlds = setOf(
        "xyz", "top", "click", "tk", "ml", "ga", "cf", "gq", "buzz",
        "loan", "work", "zip", "mov", "kim", "country", "vip", "icu", "cfd"
    )

    private val trustedOfficialDomains = setOf(
        "chase.com", "bankofamerica.com", "wellsfargo.com", "paypal.com",
        "amazon.com", "apple.com", "google.com", "microsoft.com",
        "usps.com", "ups.com", "fedex.com", "dhl.com", "irs.gov",
        "netflix.com", "citibank.com", "stripe.com", "gtbank.com", "barclays.co.uk"
    )

    private val impersonatedBrandTokens = mapOf(
        "paypal" to "paypal.com",
        "paypa1" to "paypal.com",
        "chase" to "chase.com",
        "ch4se" to "chase.com",
        "amazon" to "amazon.com",
        "amaz0n" to "amazon.com",
        "apple" to "apple.com",
        "app1e" to "apple.com",
        "usps" to "usps.com",
        "fedex" to "fedex.com",
        "fedx" to "fedex.com",
        "ups" to "ups.com",
        "dhl" to "dhl.com",
        "bankofamerica" to "bankofamerica.com",
        "wellsfargo" to "wellsfargo.com",
        "gtbank" to "gtbank.com",
        "irs" to "irs.gov",
        "netflix" to "netflix.com",
        "coinbase" to "coinbase.com",
        "binance" to "binance.com"
    )

    private val urgencyPhrases = listOf(
        "account suspended" to 22,
        "verify immediately" to 22,
        "urgent action required" to 20,
        "final notice" to 18,
        "unpaid toll" to 24,
        "package held" to 18,
        "delivery suspended" to 20,
        "click here to avoid" to 18,
        "within 24 hours" to 14,
        "unauthorized login" to 16,
        "confirm your identity" to 18,
        "tax refund" to 18,
        "wire transfer" to 16,
        "gift card" to 20,
        "prize winner" to 22,
        "claim your reward" to 18,
        "frozen account" to 22,
        "reactivate now" to 18
    )

    private val otpKeywords = listOf(
        "verification code", "otp", "one-time", "security code", "passcode", "login code", "2fa", "auth code"
    )

    private val promoKeywords = listOf(
        "unsubscribe", "% off", "promo code", "flash sale", "limited time offer", "shop now", "deal ends", "discount"
    )

    fun extractUrls(text: String): List<String> {
        return urlRegex.findAll(text)
            .map { it.value.trimEnd('.', ',', '!', '?', ')', ']') }
            .filter { candidate ->
                val lower = candidate.lowercase()
                lower.startsWith("http://") ||
                    lower.startsWith("https://") ||
                    lower.startsWith("www.") ||
                    (candidate.contains(".") && !candidate.all { it.isDigit() || it == '.' })
            }
            .distinct()
            .toList()
    }

    fun extractOtp(text: String): String? {
        val lower = text.lowercase()
        val hasOtpContext = otpKeywords.any { lower.contains(it) } ||
            lower.contains("code is") ||
            lower.contains("code:")
        if (!hasOtpContext) return null
        return otpRegex.find(text)?.value
    }

    fun analyze(input: String): UrlRiskAnalysis {
        val trimmed = input.trim()
        val urls = extractUrls(trimmed)
        val otp = extractOtp(trimmed)
        val factors = mutableListOf<UrlRiskFactor>()
        val lowerText = trimmed.lowercase()

        for (rawUrl in urls) {
            val normalizedUrl = if (rawUrl.startsWith("http://", true) || rawUrl.startsWith("https://", true)) {
                rawUrl
            } else {
                "https://$rawUrl"
            }

            val uri = runCatching { URI(normalizedUrl) }.getOrNull()
            val host = uri?.host?.lowercase()?.removePrefix("www.") ?: ""
            val path = uri?.path?.lowercase().orEmpty()
            if (host.isEmpty()) continue

            val isOfficialDomain = trustedOfficialDomains.any { official ->
                host == official || host.endsWith(".$official")
            }

            if (ipHostRegex.matches(host)) {
                factors.add(
                    UrlRiskFactor(
                        title = "Numeric IP Web Address",
                        detail = "Link uses a raw IP number ($host) instead of a real company website.",
                        weight = 42
                    )
                )
            }

            if (host.contains("xn--")) {
                factors.add(
                    UrlRiskFactor(
                        title = "Disguised Lookalike Characters",
                        detail = "Website '$host' uses hidden international encoding to mimic a real brand.",
                        weight = 40
                    )
                )
            }

            val isShortener = shortenerDomains.any { host == it || host.endsWith(".$it") }
            if (isShortener) {
                factors.add(
                    UrlRiskFactor(
                        title = "Hidden Shortened Link",
                        detail = "Short link '$host' hides where the button actually takes you.",
                        weight = 25
                    )
                )
            }

            val tld = host.substringAfterLast('.', "")
            if (tld in suspiciousTlds) {
                factors.add(
                    UrlRiskFactor(
                        title = "High-Risk Website Ending (.$tld)",
                        detail = "The ending '.$tld' is commonly used by scam text campaigns.",
                        weight = 26
                    )
                )
            }

            if (!isOfficialDomain) {
                for ((brandToken, officialDomain) in impersonatedBrandTokens) {
                    val suspiciousPathBrand = path.contains(brandToken) &&
                        (isShortener || host.contains("verify") || host.contains("secure") || host.contains("login"))
                    if (host.contains(brandToken) || suspiciousPathBrand) {
                        factors.add(
                            UrlRiskFactor(
                                title = "Fake Bank / Brand Website",
                                detail = "Pretends to be '$brandToken' on an unofficial site '$host' (real site: $officialDomain).",
                                weight = 44
                            )
                        )
                        break
                    }
                }
            }

            if (rawUrl.contains("@")) {
                factors.add(
                    UrlRiskFactor(
                        title = "Deceptive '@' Web Trick",
                        detail = "Uses '@' inside the link to hide the real destination website.",
                        weight = 35
                    )
                )
            }

            if (rawUrl.startsWith("http://", ignoreCase = true) && !isOfficialDomain) {
                factors.add(
                    UrlRiskFactor(
                        title = "Unencrypted Connection",
                        detail = "Link uses plain http:// without modern security protection.",
                        weight = 16
                    )
                )
            }

            val dotCount = host.count { it == '.' }
            if (dotCount >= 3 && !isOfficialDomain) {
                factors.add(
                    UrlRiskFactor(
                        title = "Unusually Long Web Address",
                        detail = "Chains $dotCount website layers to disguise the real owner.",
                        weight = 18
                    )
                )
            }
        }

        var urgencyMatched = 0
        for ((phrase, weight) in urgencyPhrases) {
            if (lowerText.contains(phrase)) {
                urgencyMatched++
                if (urgencyMatched <= 3) {
                    factors.add(
                        UrlRiskFactor(
                            title = "Scam Pressure Language",
                            detail = "Tries to rush you with the phrase: \"$phrase\".",
                            weight = weight
                        )
                    )
                }
            }
        }

        val rawScore = factors.sumOf { it.weight }
        val finalScore = when {
            rawScore == 0 && urls.isNotEmpty() && urls.all { u ->
                val h = runCatching {
                    URI(if (u.startsWith("http", true)) u else "https://$u").host?.lowercase()?.removePrefix("www.")
                }.getOrNull().orEmpty()
                trustedOfficialDomains.any { h == it || h.endsWith(".$it") }
            } -> 4
            else -> rawScore.coerceIn(0, 100)
        }

        val severity = when {
            finalScore >= 70 -> "High Scam Risk"
            finalScore >= 40 -> "Caution · Looks Suspicious"
            finalScore >= 15 -> "Low Risk · Double Check"
            else -> "Safe · No Scam Signs Found"
        }

        val recommendation = when {
            finalScore >= 70 -> "Do not tap this link or share codes. Delete this message."
            finalScore >= 40 -> "Avoid tapping the link. Open your bank or delivery app directly instead."
            finalScore >= 15 -> "Make sure you recognize the sender before clicking."
            else -> "Looks clean based on our on-device safety check."
        }

        return UrlRiskAnalysis(
            inputText = trimmed,
            extractedUrls = urls,
            extractedOtp = otp,
            riskScore = finalScore,
            severity = severity,
            factors = factors,
            recommendedAction = recommendation
        )
    }

    fun categorizeSms(sender: String, body: String): Pair<SmsCategory, UrlRiskAnalysis> {
        val analysis = analyze(body)
        val lowerBody = body.lowercase()
        val category = when {
            analysis.riskScore >= 45 -> SmsCategory.BLOCKED_SCAMS
            analysis.extractedOtp != null ||
                lowerBody.contains("transaction") ||
                lowerBody.contains("charged $") ||
                lowerBody.contains("debited") ||
                lowerBody.contains("statement ready") -> SmsCategory.CODES_ALERTS
            promoKeywords.any { lowerBody.contains(it) } -> SmsCategory.PROMOS
            else -> SmsCategory.PERSONAL
        }
        return category to analysis
    }
}
