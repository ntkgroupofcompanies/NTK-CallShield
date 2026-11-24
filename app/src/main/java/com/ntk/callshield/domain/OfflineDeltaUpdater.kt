package com.ntk.callshield.domain

/**
 * Pure differential snapshot engine implementing:
 * Snapshot_new = (Snapshot_old UNION Delta_additions) MINUS Delta_deletions
 */
object OfflineDeltaUpdater {

    data class SnapshotRecord(
        val normalizedNumber: String,
        val callerName: String,
        val riskScore: Int,
        val category: ThreatCategory,
        val lineType: LineType,
        val carrier: String,
        val region: String
    )

    data class DeltaPack(
        val regionId: String,
        val regionName: String,
        val targetVersion: String,
        val additions: List<SnapshotRecord>,
        val deletions: Set<String>
    )

    data class AppliedSnapshot(
        val records: Map<String, SnapshotRecord>,
        val result: DeltaUpdateResult
    )

    fun applyDelta(
        currentSnapshot: Map<String, SnapshotRecord>,
        deltaPack: DeltaPack,
        nowMillis: Long = System.currentTimeMillis()
    ): AppliedSnapshot {
        val mutable = currentSnapshot.toMutableMap()
        val previousCount = mutable.size

        var addedCount = 0
        for (record in deltaPack.additions) {
            val norm = PhoneNumberNormalizer.normalize(record.normalizedNumber)
            if (norm.isNotEmpty()) {
                if (!mutable.containsKey(norm)) addedCount++
                mutable[norm] = record.copy(normalizedNumber = norm)
            }
        }

        var deletedCount = 0
        for (delRaw in deltaPack.deletions) {
            val norm = PhoneNumberNormalizer.normalize(delRaw)
            if (mutable.remove(norm) != null) {
                deletedCount++
            }
        }

        val result = DeltaUpdateResult(
            regionId = deltaPack.regionId,
            regionName = deltaPack.regionName,
            previousCount = previousCount,
            additionsCount = addedCount,
            deletionsCount = deletedCount,
            newCount = mutable.size,
            newVersion = deltaPack.targetVersion,
            updatedAtMillis = nowMillis
        )
        return AppliedSnapshot(records = mutable, result = result)
    }

    fun buildRegionalDeltaPack(regionId: String, currentVersion: String): DeltaPack {
        val nextPatch = (currentVersion.substringAfterLast('.').toIntOrNull() ?: 1) + 1
        val nextVersion = currentVersion.substringBeforeLast('.') + ".$nextPatch"

        return when (regionId) {
            "WA" -> DeltaPack(
                regionId = "WA",
                regionName = "West Africa",
                targetVersion = nextVersion,
                additions = listOf(
                    SnapshotRecord("+2347009042211", "Fake BVN Verification Line", 97, ThreatCategory.BANK_IMPERSONATION, LineType.VOIP, "Unverified Gateway", "West Africa"),
                    SnapshotRecord("+224620991044", "Conakry Callback Fraud", 95, ThreatCategory.INTERNATIONAL_FRAUD, LineType.MOBILE, "West Africa Trunk", "West Africa"),
                    SnapshotRecord("+23276441908", "Freetown Prize Scam", 93, ThreatCategory.INTERNATIONAL_FRAUD, LineType.MOBILE, "Sierra Trunk", "West Africa")
                ),
                deletions = setOf("+2348010000099")
            )
            "NA" -> DeltaPack(
                regionId = "NA",
                regionName = "North America",
                targetVersion = nextVersion,
                additions = listOf(
                    SnapshotRecord("+18889042211", "Fake EZ-Pass Toll Center", 96, ThreatCategory.DELIVERY_SCAM, LineType.VOIP, "Bandwidth VoIP", "North America"),
                    SnapshotRecord("+18334109920", "Medicare Benefit Robocall", 91, ThreatCategory.ROBOCALL, LineType.TOLL_FREE, "Inteliquent", "North America"),
                    SnapshotRecord("+14159028831", "Spoofed Chase Fraud Desk", 98, ThreatCategory.BANK_IMPERSONATION, LineType.VOIP, "Onvoy VoIP", "North America")
                ),
                deletions = setOf("+18005559901")
            )
            "EU" -> DeltaPack(
                regionId = "EU",
                regionName = "Europe",
                targetVersion = nextVersion,
                additions = listOf(
                    SnapshotRecord("+442079468812", "HMRC Tax Refund Scam", 95, ThreatCategory.BANK_IMPERSONATION, LineType.VOIP, "Gamma Telecom", "Europe"),
                    SnapshotRecord("+375291004488", "Minsk One-Ring Callback", 94, ThreatCategory.INTERNATIONAL_FRAUD, LineType.MOBILE, "Beltelecom Trunk", "Europe")
                ),
                deletions = setOf("+442079460999")
            )
            else -> DeltaPack(
                regionId = "AS",
                regionName = "Asia",
                targetVersion = nextVersion,
                additions = listOf(
                    SnapshotRecord("+81355509182", "Courier Customs Impersonator", 93, ThreatCategory.DELIVERY_SCAM, LineType.VOIP, "SIP Japan", "Asia"),
                    SnapshotRecord("+82269501124", "Crypto Exchange Spoof", 96, ThreatCategory.TECH_SUPPORT_SCAM, LineType.VOIP, "CloudSIP KR", "Asia")
                ),
                deletions = setOf("+81355500001")
            )
        }
    }
}
