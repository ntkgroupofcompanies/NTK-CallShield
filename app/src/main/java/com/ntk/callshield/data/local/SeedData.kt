package com.ntk.callshield.data.local

import com.ntk.callshield.domain.BlockAction
import com.ntk.callshield.domain.CallDisposition
import com.ntk.callshield.domain.CarrierVerificationStatus
import com.ntk.callshield.domain.LineType
import com.ntk.callshield.domain.RuleType
import com.ntk.callshield.domain.SmsCategory
import com.ntk.callshield.domain.ThreatCategory

object SeedData {

    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 86_400_000L

    /**
     * 46 realistic pre-seeded records spanning verified banks, delivery couriers, hospitals,
     * telemarketers, VoIP scammers, spoofed bank numbers, international fraud prefixes (+224/+232/+375/+234),
     * and community-tagged contacts.
     */
    fun phoneIntelList(now: Long = System.currentTimeMillis()): List<PhoneIntelEntity> = listOf(
        // --- Verified Banks, Hospitals & Delivery Couriers (13) ---
        intel("+18009359935", "+1 (800) 935-9935", "Chase Bank Fraud Desk", 2, LineType.TOLL_FREE, "Verizon Business", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BANK, 0, "Verified Bank,Official Line", "Low", "None", "Low", true, true, now),
        intel("+18004321000", "+1 (800) 432-1000", "Bank of America Support", 3, LineType.TOLL_FREE, "AT&T Enterprise", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BANK, 0, "Verified Bank,Official Support", "Low", "None", "Low", true, true, now),
        intel("+18008693557", "+1 (800) 869-3557", "Wells Fargo Client Care", 4, LineType.TOLL_FREE, "Lumen Telecom", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BANK, 0, "Verified Bank,Trusted", "Low", "None", "Low", true, true, now),
        intel("+2347004822665", "+234 700 482 2665", "GTBank Official Helpdesk", 4, LineType.TOLL_FREE, "MTN Enterprise NG", "West Africa", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BANK, 0, "Verified Bank,West Africa", "Low", "None", "Low", true, true, now),
        intel("+442079460100", "+44 20 7946 0100", "Barclays Premier Desk UK", 3, LineType.LANDLINE, "BT Business", "Europe", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BANK, 0, "Verified Bank,UK Official", "Low", "None", "Low", true, true, now),
        intel("+18004633339", "+1 (800) 463-3339", "FedEx Priority Courier", 4, LineType.TOLL_FREE, "Verizon Business", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BUSINESS, 1, "Delivery Courier,Verified", "Low", "None", "Low", true, false, now),
        intel("+18007425877", "+1 (800) 742-5877", "UPS Package Dispatch", 4, LineType.TOLL_FREE, "AT&T Enterprise", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BUSINESS, 0, "Delivery Courier,Verified", "Low", "None", "Low", true, false, now),
        intel("+18002255345", "+1 (800) 225-5345", "DHL Express Delivery", 5, LineType.TOLL_FREE, "T-Mobile Business", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BUSINESS, 0, "Delivery Courier,Verified", "Low", "None", "Low", true, false, now),
        intel("+14155550101", "+1 (415) 555-0101", "UCSF Medical Center", 2, LineType.LANDLINE, "Sonic Fiber Voice", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BUSINESS, 0, "Hospital,Appointments,Verified", "Low", "None", "Low", true, false, now),
        intel("+15075550199", "+1 (507) 555-0199", "Mayo Clinic Care Line", 1, LineType.LANDLINE, "Lumen Healthcare", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BUSINESS, 0, "Hospital,Medical Clinic", "Low", "None", "Low", true, false, now),
        intel("+18002752273", "+1 (800) 275-2273", "Apple Care Support", 2, LineType.TOLL_FREE, "AT&T Enterprise", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BUSINESS, 0, "Tech Support,Official", "Low", "None", "Low", true, false, now),
        intel("+12125550148", "+1 (212) 555-0148", "Stripe Merchant Risk", 2, LineType.LANDLINE, "Twilio Enterprise", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BUSINESS, 0, "Payments,Verified", "Low", "None", "Low", true, true, now),
        intel("+18002211212", "+1 (800) 221-1212", "Delta Air Lines Desk", 3, LineType.TOLL_FREE, "AT&T Enterprise", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BUSINESS, 0, "Airline,Travel", "Low", "None", "Low", true, false, now),

        // --- Known Scam Robocalls & Spoofed Bank Impersonators (14) ---
        intel("+18885550194", "+1 (888) 555-0194", "Auto-Warranty Scam Bot", 96, LineType.VOIP, "Onvoy Spectrum VoIP", "North America", CarrierVerificationStatus.SPOOF_WARNING, ThreatCategory.ROBOCALL, 4820, "Scam Robocall,Auto Warranty", "High", "Active Forwarding", "High", false, false, now),
        intel("+18005550142", "+1 (800) 555-0142", "Fake IRS Tax Arrest Center", 98, LineType.VOIP, "Bandwidth VoIP", "North America", CarrierVerificationStatus.SPOOF_WARNING, ThreatCategory.BANK_IMPERSONATION, 6310, "Tax Scam,Impersonator", "High", "SIP Spoof", "High", false, false, now),
        intel("+13475550176", "+1 (347) 555-0176", "Spoofed Chase Fraud Line", 95, LineType.VOIP, "Peerless Network", "North America", CarrierVerificationStatus.SPOOF_WARNING, ThreatCategory.BANK_IMPERSONATION, 2190, "Fake Bank,OTP Theft", "High", "Active Forwarding", "High", false, false, now),
        intel("+16465550154", "+1 (646) 555-0154", "Spoofed Bank Alert Desk", 89, LineType.VOIP, "Voxbone SIP", "North America", CarrierVerificationStatus.SPOOF_WARNING, ThreatCategory.BANK_IMPERSONATION, 1450, "Spoofed Caller,Bank Scam", "Elevated", "Detected", "Elevated", false, false, now),
        intel("+18445550188", "+1 (844) 555-0188", "Social Security Scam Bot", 97, LineType.TOLL_FREE, "ThinQ SIP", "North America", CarrierVerificationStatus.SPOOF_WARNING, ThreatCategory.ROBOCALL, 5412, "Robocall,Gov Scam", "High", "Unknown", "Moderate", false, false, now),
        intel("+12025550165", "+1 (202) 555-0165", "Fake Federal Grant Desk", 92, LineType.VOIP, "Sinch Voice", "North America", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.ROBOCALL, 1844, "Robocall,Grant Scam", "Moderate", "None", "Moderate", false, false, now),
        intel("+17025550133", "+1 (702) 555-0133", "Crypto Wallet Scam Center", 95, LineType.VOIP, "Telnyx VoIP", "North America", CarrierVerificationStatus.SPOOF_WARNING, ThreatCategory.TECH_SUPPORT_SCAM, 3120, "Crypto Scam,Seed Phish", "High", "Active Forwarding", "High", false, false, now),
        intel("+18775550129", "+1 (877) 555-0129", "Fake GeekSquad Renewal", 91, LineType.TOLL_FREE, "Inteliquent", "North America", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.TECH_SUPPORT_SCAM, 2740, "Tech Support Scam,Invoice Fraud", "Moderate", "None", "Moderate", false, false, now),
        intel("+13055550181", "+1 (305) 555-0181", "Unpaid Road Toll Phisher", 93, LineType.VOIP, "Onvoy VoIP", "North America", CarrierVerificationStatus.SPOOF_WARNING, ThreatCategory.DELIVERY_SCAM, 3980, "Toll Scam,Smishing Ring", "High", "Detected", "High", false, false, now),
        intel("+14695550122", "+1 (469) 555-0122", "Fake Loan Forgiveness Bot", 88, LineType.VOIP, "Bandwidth VoIP", "North America", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.ROBOCALL, 2033, "Robocall,Loan Scam", "Moderate", "None", "Low", false, false, now),
        intel("+18555550117", "+1 (855) 555-0117", "Fake Medicare Card Center", 90, LineType.TOLL_FREE, "Flowroute SIP", "North America", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.ROBOCALL, 1790, "Healthcare Scam,Robocall", "Moderate", "None", "Low", false, false, now),
        intel("+19175550190", "+1 (917) 555-0190", "Fake Sheriff Warrant Scam", 95, LineType.VOIP, "Unverified SIP", "North America", CarrierVerificationStatus.SPOOF_WARNING, ThreatCategory.BANK_IMPERSONATION, 2890, "Warrant Scam,Spoofed ID", "High", "Active Forwarding", "Elevated", false, false, now),
        intel("+14045550166", "+1 (404) 555-0166", "Fake Power Shutoff Desk", 87, LineType.VOIP, "Peerless Network", "North America", CarrierVerificationStatus.SPOOF_WARNING, ThreatCategory.BANK_IMPERSONATION, 1210, "Utility Scam,Zelle Fraud", "Moderate", "None", "Moderate", false, false, now),
        intel("+18005559901", "+1 (800) 555-9901", "Retired Sweepstakes Dialer", 84, LineType.TOLL_FREE, "Legacy SIP", "North America", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.ROBOCALL, 940, "Retired Campaign,Spam", "Low", "None", "Low", false, false, now),

        // --- International Fraud Prefixes (+224, +232, +375, +234, +372, +881) (8) ---
        intel("+224620550194", "+224 620 550 194", "Guinea One-Ring Trap", 94, LineType.MOBILE, "West Africa Trunk", "West Africa", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.INTERNATIONAL_FRAUD, 1850, "One-Ring Scam,+224 Prefix", "High", "None", "High", false, false, now),
        intel("+23276550118", "+232 76 550 118", "Sierra Leone Callback Ring", 93, LineType.MOBILE, "Freetown Carrier", "West Africa", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.INTERNATIONAL_FRAUD, 1590, "Wangiri,+232 Prefix", "High", "None", "High", false, false, now),
        intel("+375295501420", "+375 29 550 1420", "Belarus Premium Toll Scam", 95, LineType.MOBILE, "Minsk Transit", "Europe", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.INTERNATIONAL_FRAUD, 2240, "Premium Callback,+375 Prefix", "High", "Active Forwarding", "High", false, false, now),
        intel("+2347005550199", "+234 700 555 0199", "Fake BVN / NIN Update Desk", 96, LineType.VOIP, "Unverified VoIP Gateway", "West Africa", CarrierVerificationStatus.SPOOF_WARNING, ThreatCategory.BANK_IMPERSONATION, 3110, "BVN Scam,Fake Bank", "High", "SIM Box Detected", "High", false, false, now),
        intel("+2348010000099", "+234 801 000 0099", "Lagos Advance Fee Ring", 94, LineType.MOBILE, "Unverified Gateway", "West Africa", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.INTERNATIONAL_FRAUD, 2310, "International Scam", "High", "SIM Box Detected", "High", false, false, now),
        intel("+442079460999", "+44 20 7946 0999", "London Trading Boiler Room", 91, LineType.VOIP, "UK SIP Transit", "Europe", CarrierVerificationStatus.SPOOF_WARNING, ThreatCategory.INTERNATIONAL_FRAUD, 1620, "Investment Scam,UK Spoof", "High", "Active Forwarding", "Moderate", false, false, now),
        intel("+81355500001", "+81 3 5550 0001", "Fake Embassy Visa Desk", 89, LineType.VOIP, "SIP Japan Cloud", "Asia", CarrierVerificationStatus.SPOOF_WARNING, ThreatCategory.INTERNATIONAL_FRAUD, 1140, "Consulate Scam,Robocall", "Elevated", "Detected", "Moderate", false, false, now),
        intel("+918045550192", "+91 80 4555 0192", "Fake Windows Refund Desk", 92, LineType.VOIP, "Offshore SIP Trunk", "Asia", CarrierVerificationStatus.SPOOF_WARNING, ThreatCategory.TECH_SUPPORT_SCAM, 3450, "Remote Access Scam", "High", "Active Forwarding", "Elevated", false, false, now),

        // --- VoIP Telemarketers & Sales Centers (7) ---
        intel("+14155550182", "+1 (415) 555-0182", "Apex Solar Lead Center", 64, LineType.VOIP, "RingCentral VoIP", "North America", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.TELEMARKETER, 412, "Telemarketer,Solar Sales,Internet VoIP", "Low", "None", "Low", false, false, now),
        intel("+15125550149", "+1 (512) 555-0149", "Prime Insurance Quotes", 58, LineType.VOIP, "Five9 Cloud Dialer", "North America", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.TELEMARKETER, 318, "Telemarketer,Insurance", "Low", "None", "Low", false, false, now),
        intel("+13125550173", "+1 (312) 555-0173", "Midwest Roof Sales", 52, LineType.VOIP, "Twilio Programmable", "North America", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.TELEMARKETER, 196, "Telemarketer,Cold Call", "Low", "None", "Low", false, false, now),
        intel("+12135550191", "+1 (213) 555-0191", "Pacific Timeshare Resales", 71, LineType.VOIP, "Bandwidth VoIP", "North America", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.TELEMARKETER, 624, "Aggressive Sales,Timeshare", "Moderate", "None", "Low", false, false, now),
        intel("+16025550128", "+1 (602) 555-0128", "Summit Debt Relief", 68, LineType.VOIP, "Onvoy VoIP", "North America", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.TELEMARKETER, 509, "Debt Relief,Telemarketer", "Low", "None", "Low", false, false, now),
        intel("+16175550139", "+1 (617) 555-0139", "Civic Opinion Pollster", 46, LineType.LANDLINE, "Verizon Landline", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.TELEMARKETER, 140, "Survey,Polling", "Low", "None", "Low", false, false, now),
        intel("+14085550177", "+1 (408) 555-0177", "Tech Recruiting Outreach", 42, LineType.VOIP, "Zoom Phone", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.TELEMARKETER, 68, "Recruiter,Cold Call", "Low", "None", "Low", false, false, now),

        // --- Community-Tagged Safe Locals & Urgent Callers (4) ---
        intel("+14155550134", "+1 (415) 555-0134", "Local Courier Driver", 12, LineType.MOBILE, "T-Mobile US", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BUSINESS, 0, "Delivery Driver,Community Safe", "Low", "None", "Low", false, false, now),
        intel("+14155550155", "+1 (415) 555-0155", "Dr. Hannah Park (On-Call)", 6, LineType.MOBILE, "Verizon Wireless", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.PERSONAL, 0, "Doctor,Urgent Callback", "Low", "None", "Low", false, false, now),
        intel("+15105550192", "+1 (510) 555-0192", "Oakridge School Nurse", 4, LineType.LANDLINE, "AT&T Landline", "North America", CarrierVerificationStatus.VERIFIED, ThreatCategory.VERIFIED_BUSINESS, 0, "School,Emergency Contact", "Low", "None", "Low", true, false, now),
        intel("+13105550168", "+1 (310) 555-0168", "Urgent Pharmacy Callback", 60, LineType.VOIP, "Google Voice", "North America", CarrierVerificationStatus.UNVERIFIED, ThreatCategory.UNKNOWN, 14, "Unverified,Repeat Test", "Low", "None", "Low", false, false, now)
    )

    private fun intel(
        normalized: String,
        display: String,
        name: String,
        risk: Int,
        lineType: LineType,
        carrier: String,
        region: String,
        verification: CarrierVerificationStatus,
        category: ThreatCategory,
        reports: Int,
        tags: String,
        simSwap: String,
        forwarding: String,
        smsPumping: String,
        verified: Boolean,
        isFinancial: Boolean,
        now: Long
    ) = PhoneIntelEntity(
        normalizedNumber = normalized,
        displayNumber = display,
        callerName = name,
        riskScore = risk,
        lineType = lineType.name,
        carrier = carrier,
        region = region,
        verificationDefault = verification.name,
        category = category.name,
        communityReports = reports,
        communityTagsCsv = tags,
        simSwapRisk = simSwap,
        callForwardingRisk = forwarding,
        smsPumpingRisk = smsPumping,
        isVerifiedBusiness = verified,
        isFinancialInstitution = isFinancial,
        isWhitelisted = false,
        isBlacklisted = false,
        updatedAtMillis = now
    )

    fun initialCallLogs(now: Long = System.currentTimeMillis()): List<CallLogEntity> = listOf(
        CallLogEntity(
            normalizedNumber = "+18885550194",
            displayNumber = "+1 (888) 555-0194",
            callerName = "Auto-Warranty Scam Bot",
            disposition = CallDisposition.BLOCKED.name,
            riskScore = 96,
            category = ThreatCategory.ROBOCALL.name,
            lineType = LineType.VOIP.name,
            carrier = "Onvoy Spectrum VoIP",
            verificationStatus = CarrierVerificationStatus.SPOOF_WARNING.name,
            matchedRule = "Offline Spam Shield + Bloom Index (96/100)",
            plainReason = "Blocked automatically from your offline scam list.",
            responseFlagsSummary = "disallowCall=true, rejectCall=true, silenceCall=false, skipCallLog=true, skipNotification=true",
            screeningLatencyMs = 4,
            simSlot = 1,
            timestampMillis = now - 22 * 60_000L
        ),
        CallLogEntity(
            normalizedNumber = "+18009359935",
            displayNumber = "+1 (800) 935-9935",
            callerName = "Chase Bank Fraud Desk",
            disposition = CallDisposition.ALLOWED.name,
            riskScore = 2,
            category = ThreatCategory.VERIFIED_BANK.name,
            lineType = LineType.TOLL_FREE.name,
            carrier = "Verizon Business",
            verificationStatus = CarrierVerificationStatus.VERIFIED.name,
            matchedRule = "Carrier Verified Business",
            plainReason = "Carrier Verified ✓ trusted organization.",
            responseFlagsSummary = "disallowCall=false, rejectCall=false, silenceCall=false, skipCallLog=false, skipNotification=false",
            screeningLatencyMs = 3,
            simSlot = 1,
            timestampMillis = now - 2 * HOUR_MS
        ),
        CallLogEntity(
            normalizedNumber = "+16465550154",
            displayNumber = "+1 (646) 555-0154",
            callerName = "Spoofed Bank Alert Desk",
            disposition = CallDisposition.SILENCED.name,
            riskScore = 89,
            category = ThreatCategory.BANK_IMPERSONATION.name,
            lineType = LineType.VOIP.name,
            carrier = "Voxbone SIP",
            verificationStatus = CarrierVerificationStatus.SPOOF_WARNING.name,
            matchedRule = "Balanced Mode · Spoof Warning Silenced",
            plainReason = "Spoof Warning ⚠️ — silenced ringtone and flagged on screen.",
            responseFlagsSummary = "disallowCall=false, rejectCall=false, silenceCall=true, skipCallLog=false, skipNotification=false",
            screeningLatencyMs = 6,
            simSlot = 2,
            timestampMillis = now - 5 * HOUR_MS
        ),
        CallLogEntity(
            normalizedNumber = "+14155550182",
            displayNumber = "+1 (415) 555-0182",
            callerName = "Apex Solar Lead Center",
            disposition = CallDisposition.MISSED.name,
            riskScore = 64,
            category = ThreatCategory.TELEMARKETER.name,
            lineType = LineType.VOIP.name,
            carrier = "RingCentral VoIP",
            verificationStatus = CarrierVerificationStatus.UNVERIFIED.name,
            matchedRule = "Phase-2 Caller ID Warning (64/100)",
            plainReason = "Unverified internet caller with telemarketing reports.",
            responseFlagsSummary = "disallowCall=false, rejectCall=false, silenceCall=false, skipCallLog=false, skipNotification=false",
            screeningLatencyMs = 5,
            simSlot = 1,
            timestampMillis = now - 1 * DAY_MS - 3 * HOUR_MS
        ),
        CallLogEntity(
            normalizedNumber = "+224620550194",
            displayNumber = "+224 620 550 194",
            callerName = "Guinea One-Ring Trap",
            disposition = CallDisposition.BLOCKED.name,
            riskScore = 94,
            category = ThreatCategory.INTERNATIONAL_FRAUD.name,
            lineType = LineType.MOBILE.name,
            carrier = "West Africa Trunk",
            verificationStatus = CarrierVerificationStatus.UNVERIFIED.name,
            matchedRule = "Custom Rule: High-Risk International (+224)",
            plainReason = "Blocked by your international fraud prefix rule (+224).",
            responseFlagsSummary = "disallowCall=true, rejectCall=true, silenceCall=false, skipCallLog=true, skipNotification=true",
            screeningLatencyMs = 4,
            simSlot = 1,
            timestampMillis = now - 2 * DAY_MS
        ),
        CallLogEntity(
            normalizedNumber = "+14155550101",
            displayNumber = "+1 (415) 555-0101",
            callerName = "UCSF Medical Center",
            disposition = CallDisposition.ALLOWED.name,
            riskScore = 2,
            category = ThreatCategory.VERIFIED_BUSINESS.name,
            lineType = LineType.LANDLINE.name,
            carrier = "Sonic Fiber Voice",
            verificationStatus = CarrierVerificationStatus.VERIFIED.name,
            matchedRule = "Carrier Verified Business",
            plainReason = "Carrier Verified ✓ hospital appointment line.",
            responseFlagsSummary = "disallowCall=false, rejectCall=false, silenceCall=false, skipCallLog=false, skipNotification=false",
            screeningLatencyMs = 2,
            simSlot = 2,
            timestampMillis = now - 3 * DAY_MS
        ),
        CallLogEntity(
            normalizedNumber = "+375295501420",
            displayNumber = "+375 29 550 1420",
            callerName = "Belarus Premium Toll Scam",
            disposition = CallDisposition.BLOCKED.name,
            riskScore = 95,
            category = ThreatCategory.INTERNATIONAL_FRAUD.name,
            lineType = LineType.MOBILE.name,
            carrier = "Minsk Transit",
            verificationStatus = CarrierVerificationStatus.UNVERIFIED.name,
            matchedRule = "Custom Rule: High-Risk Country (+375)",
            plainReason = "Blocked by your international fraud prefix rule (+375).",
            responseFlagsSummary = "disallowCall=true, rejectCall=true, silenceCall=false, skipCallLog=true, skipNotification=true",
            screeningLatencyMs = 4,
            simSlot = 1,
            timestampMillis = now - 4 * DAY_MS
        ),
        CallLogEntity(
            normalizedNumber = "+17025550133",
            displayNumber = "+1 (702) 555-0133",
            callerName = "Crypto Wallet Scam Center",
            disposition = CallDisposition.BLOCKED.name,
            riskScore = 95,
            category = ThreatCategory.TECH_SUPPORT_SCAM.name,
            lineType = LineType.VOIP.name,
            carrier = "Telnyx VoIP",
            verificationStatus = CarrierVerificationStatus.SPOOF_WARNING.name,
            matchedRule = "Offline Spam Shield + Bloom Index (95/100)",
            plainReason = "Blocked automatically from your offline scam list.",
            responseFlagsSummary = "disallowCall=true, rejectCall=true, silenceCall=false, skipCallLog=true, skipNotification=true",
            screeningLatencyMs = 3,
            simSlot = 2,
            timestampMillis = now - 5 * DAY_MS
        )
    )

    fun initialBlockingRules(): List<BlockingRuleEntity> = listOf(
        BlockingRuleEntity(
            label = "Guinea Callback Fraud (+224)",
            ruleType = RuleType.COUNTRY_CODE.name,
            pattern = "+224",
            action = BlockAction.BLOCK_SILENTLY.name,
            enabled = true
        ),
        BlockingRuleEntity(
            label = "Sierra Leone Callback (+232)",
            ruleType = RuleType.COUNTRY_CODE.name,
            pattern = "+232",
            action = BlockAction.BLOCK_SILENTLY.name,
            enabled = true
        ),
        BlockingRuleEntity(
            label = "Belarus Premium Toll (+375)",
            ruleType = RuleType.COUNTRY_CODE.name,
            pattern = "+375",
            action = BlockAction.BLOCK_SILENTLY.name,
            enabled = true
        ),
        BlockingRuleEntity(
            label = "Fake Toll-Free Prefix (+234-700-*)",
            ruleType = RuleType.PREFIX_PATTERN.name,
            pattern = "+234-700-*",
            action = BlockAction.BLOCK_SHOW_MISSED.name,
            enabled = false
        ),
        BlockingRuleEntity(
            label = "844 Robocall Prefix",
            ruleType = RuleType.PREFIX_PATTERN.name,
            pattern = "+1-844-555-*",
            action = BlockAction.BLOCK_SHOW_MISSED.name,
            enabled = true
        )
    )

    fun initialContacts(): List<ContactEntity> = listOf(
        ContactEntity(displayName = "Elena Vance", normalizedNumber = "+14155550201", displayNumber = "+1 (415) 555-0201", roleOrLabel = "Sister · Family", avatarColorHex = 0xFF0EA5A4, isWhitelisted = true, isFavorite = true, notes = "Always allow"),
        ContactEntity(displayName = "Elena Vance (Mobile)", normalizedNumber = "+14155550201", displayNumber = "+1 (415) 555-0201", roleOrLabel = "SIM 2 Import", avatarColorHex = 0xFF0284C7, isWhitelisted = true, isFavorite = false, notes = "Duplicate SIM entry"),
        ContactEntity(displayName = "Marcus Chen", normalizedNumber = "+14155550202", displayNumber = "+1 (415) 555-0202", roleOrLabel = "Work Partner", avatarColorHex = 0xFF6366F1, isWhitelisted = true, isFavorite = true, notes = "Architecture team"),
        ContactEntity(displayName = "Marcus Chen", normalizedNumber = "+14155550202", displayNumber = "+1 (415) 555-0202", roleOrLabel = "Backup Contact", avatarColorHex = 0xFF4F46E5, isWhitelisted = true, isFavorite = false, notes = "Old card"),
        ContactEntity(displayName = "Priya Nair", normalizedNumber = "+16505550203", displayNumber = "+1 (650) 555-0203", roleOrLabel = "Family · Emergency", avatarColorHex = 0xFFEC4899, isWhitelisted = true, isFavorite = true, notes = "Emergency contact"),
        ContactEntity(displayName = "Priya Nair - Home", normalizedNumber = "+16505550203", displayNumber = "+1 (650) 555-0203", roleOrLabel = "Duplicate Sync", avatarColorHex = 0xFFDB2777, isWhitelisted = true, isFavorite = false, notes = ""),
        ContactEntity(displayName = "Dr. Samuel Osei", normalizedNumber = "+14155550204", displayNumber = "+1 (415) 555-0204", roleOrLabel = "Family Doctor", avatarColorHex = 0xFF10B981, isWhitelisted = true, isFavorite = true, notes = "UCSF Clinic"),
        ContactEntity(displayName = "Sofia Lindqvist", normalizedNumber = "+442079460205", displayNumber = "+44 20 7946 0205", roleOrLabel = "Design Lead · London", avatarColorHex = 0xFFF59E0B, isWhitelisted = true, isFavorite = false, notes = "UK office"),
        ContactEntity(displayName = "David Morales", normalizedNumber = "+12125550206", displayNumber = "+1 (212) 555-0206", roleOrLabel = "Attorney", avatarColorHex = 0xFF8B5CF6, isWhitelisted = true, isFavorite = false, notes = "NYC"),
        ContactEntity(displayName = "Kenji Takahashi", normalizedNumber = "+81355500207", displayNumber = "+81 3 5550 0207", roleOrLabel = "Engineering · Tokyo", avatarColorHex = 0xFF14B8A6, isWhitelisted = true, isFavorite = false, notes = "Partner")
    )

    fun initialSmsMessages(now: Long = System.currentTimeMillis()): List<SmsMessageEntity> = listOf(
        SmsMessageEntity(
            sender = "Chase Security",
            body = "Chase: Your one-time verification code is 482916. Never share this security code with anyone. It expires in 10 minutes.",
            category = SmsCategory.CODES_ALERTS.name,
            riskScore = 0,
            extractedOtp = "482916",
            extractedUrl = null,
            riskSummary = "Safe · Official OTP code detected",
            timestampMillis = now - 18 * 60_000L
        ),
        SmsMessageEntity(
            sender = "+1 (305) 555-0181",
            body = "URGENT ACTION REQUIRED: You have an unpaid toll of $6.99. Your account suspended within 24 hours unless settled at http://usps-toll-pay.xyz/verify-now",
            category = SmsCategory.BLOCKED_SCAMS.name,
            riskScore = 94,
            extractedOtp = null,
            extractedUrl = "http://usps-toll-pay.xyz/verify-now",
            riskSummary = "High Scam Risk · Fake brand + .xyz ending + urgency",
            timestampMillis = now - 95 * 60_000L
        ),
        SmsMessageEntity(
            sender = "Elena Vance",
            body = "Hey! Just landed safely. Are we still meeting for dinner at 6?",
            category = SmsCategory.PERSONAL.name,
            riskScore = 0,
            extractedOtp = null,
            extractedUrl = null,
            riskSummary = "Safe · Personal message",
            timestampMillis = now - 3 * HOUR_MS
        ),
        SmsMessageEntity(
            sender = "Stripe Alerts",
            body = "Your Stripe 2FA security code is 903412. Authorized sign-in from your mobile browser.",
            category = SmsCategory.CODES_ALERTS.name,
            riskScore = 0,
            extractedOtp = "903412",
            extractedUrl = null,
            riskSummary = "Safe · Official 2FA code",
            timestampMillis = now - 6 * HOUR_MS
        ),
        SmsMessageEntity(
            sender = "Nordic Roasters",
            body = "Flash Sale! Enjoy 25% off single-origin beans today with promo code SHIELD25. Shop now or reply STOP to unsubscribe.",
            category = SmsCategory.PROMOS.name,
            riskScore = 4,
            extractedOtp = null,
            extractedUrl = null,
            riskSummary = "Promotional message",
            timestampMillis = now - 1 * DAY_MS
        ),
        SmsMessageEntity(
            sender = "+1 (646) 555-0154",
            body = "PayPa1 Security: Unauthorized login detected. Verify immediately to avoid frozen account: https://bit.ly/paypa1-recover",
            category = SmsCategory.BLOCKED_SCAMS.name,
            riskScore = 89,
            extractedOtp = null,
            extractedUrl = "https://bit.ly/paypa1-recover",
            riskSummary = "High Scam Risk · Hidden link + fake brand",
            timestampMillis = now - 2 * DAY_MS
        )
    )

    fun initialOfflineRegions(now: Long = System.currentTimeMillis()): List<OfflineRegionEntity> = listOf(
        OfflineRegionEntity(
            regionId = "WA",
            regionName = "West Africa",
            coverageDescription = "Nigeria, Ghana, Guinea (+224) & Sierra Leone (+232) fraud protection",
            version = "2026.11.4",
            entryCount = 68_400,
            sizeMb = 1.9,
            lastSyncedMillis = now - 4 * HOUR_MS,
            isDownloaded = true
        ),
        OfflineRegionEntity(
            regionId = "NA",
            regionName = "North America",
            coverageDescription = "US, Canada & toll-free robocall + VoIP offline protection",
            version = "2026.11.5",
            entryCount = 148_420,
            sizeMb = 4.2,
            lastSyncedMillis = now - 6 * HOUR_MS,
            isDownloaded = true
        ),
        OfflineRegionEntity(
            regionId = "EU",
            regionName = "Europe",
            coverageDescription = "UK, EU, +375 callback & cross-border scam numbers",
            version = "2026.11.2",
            entryCount = 92_150,
            sizeMb = 2.8,
            lastSyncedMillis = now - 1 * DAY_MS,
            isDownloaded = true
        ),
        OfflineRegionEntity(
            regionId = "AS",
            regionName = "Asia",
            coverageDescription = "Japan, South Korea, India & APAC delivery scam protection",
            version = "2026.11.1",
            entryCount = 74_310,
            sizeMb = 2.3,
            lastSyncedMillis = now - 2 * DAY_MS,
            isDownloaded = false
        )
    )

    fun initialPrivacyAudits(now: Long = System.currentTimeMillis()): List<PrivacyAuditEntity> = listOf(
        PrivacyAuditEntity(
            eventType = "ON-DEVICE CALL CHECK",
            detail = "Checked +1 (888) 555-0194 against your offline protection list in 4ms. Nothing left your phone.",
            leftDevice = false,
            timestampMillis = now - 22 * 60_000L
        ),
        PrivacyAuditEntity(
            eventType = "CONTACTS PROTECTION",
            detail = "Contacts remained 100% on your phone. Zero names or numbers uploaded.",
            leftDevice = false,
            timestampMillis = now - 2 * HOUR_MS
        ),
        PrivacyAuditEntity(
            eventType = "OFFLINE SHIELD UPDATE",
            detail = "Updated West Africa & North America offline scam lists cleanly.",
            leftDevice = false,
            timestampMillis = now - 4 * HOUR_MS
        )
    )

    fun initialMissions(): List<MissionProgressEntity> = listOf(
        MissionProgressEntity(
            missionId = "mission_report_spam",
            title = "Scam Spotter",
            description = "Report 2 suspicious numbers to help protect the community",
            currentProgress = 1,
            targetProgress = 2,
            rewardPoints = 150,
            completed = false
        ),
        MissionProgressEntity(
            missionId = "mission_scan_url",
            title = "Link Checker",
            description = "Scan 2 suspicious links or texts with the Scam Checker",
            currentProgress = 1,
            targetProgress = 2,
            rewardPoints = 120,
            completed = false
        ),
        MissionProgressEntity(
            missionId = "mission_delta_sync",
            title = "Offline Ready",
            description = "Update one regional offline scam list so protection works without internet",
            currentProgress = 0,
            targetProgress = 1,
            rewardPoints = 100,
            completed = false
        ),
        MissionProgressEntity(
            missionId = "mission_test_lab",
            title = "Shield Explorer",
            description = "Try 3 scenarios in Test My Shield to see live blocking in action",
            currentProgress = 0,
            targetProgress = 3,
            rewardPoints = 200,
            completed = false
        ),
        MissionProgressEntity(
            missionId = "mission_zero_telemetry",
            title = "Privacy First",
            description = "Keep Private Offline Mode active with zero contacts uploaded",
            currentProgress = 1,
            targetProgress = 1,
            rewardPoints = 100,
            completed = true
        )
    )
}
