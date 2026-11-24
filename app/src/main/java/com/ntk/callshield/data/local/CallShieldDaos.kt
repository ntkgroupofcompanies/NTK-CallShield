package com.ntk.callshield.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneIntelDao {
    @Query("SELECT * FROM phone_intel ORDER BY riskScore DESC, callerName ASC")
    fun observeAll(): Flow<List<PhoneIntelEntity>>

    @Query("SELECT * FROM phone_intel")
    suspend fun getAllSnapshot(): List<PhoneIntelEntity>

    @Query("SELECT * FROM phone_intel WHERE normalizedNumber = :normalized LIMIT 1")
    suspend fun findByNumber(normalized: String): PhoneIntelEntity?

    @Query("SELECT * FROM phone_intel WHERE normalizedNumber LIKE '%' || :query || '%' OR callerName LIKE '%' || :query || '%' OR communityTagsCsv LIKE '%' || :query || '%' LIMIT 25")
    suspend fun search(query: String): List<PhoneIntelEntity>

    @Query("SELECT COUNT(*) FROM phone_intel")
    suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM phone_intel LIMIT 1)")
    suspend fun hasAnyRecords(): Boolean

    @Query("SELECT normalizedNumber FROM phone_intel WHERE riskScore >= 75 OR isBlacklisted = 1")
    suspend fun getHighRiskNumbers(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PhoneIntelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PhoneIntelEntity>)

    @Query("DELETE FROM phone_intel WHERE normalizedNumber = :normalized")
    suspend fun deleteByNumber(normalized: String)
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE normalizedNumber = :normalized AND (disposition = 'BLOCKED' OR disposition = 'SILENCED' OR disposition = 'MISSED') ORDER BY timestampMillis DESC LIMIT 5")
    suspend fun getRecentBlockedOrSilenced(normalized: String): List<CallLogEntity>

    @Query("SELECT COUNT(*) FROM call_logs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CallLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<CallLogEntity>)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM call_logs ORDER BY timestampMillis DESC")
    suspend fun getAllSnapshot(): List<CallLogEntity>

    @Query("DELETE FROM call_logs")
    suspend fun deleteAll()
}

@Dao
interface BlockingRuleDao {
    @Query("SELECT * FROM blocking_rules ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<BlockingRuleEntity>>

    @Query("SELECT * FROM blocking_rules WHERE enabled = 1")
    suspend fun getEnabledRules(): List<BlockingRuleEntity>

    @Query("SELECT COUNT(*) FROM blocking_rules")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: BlockingRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<BlockingRuleEntity>)

    @Update
    suspend fun update(rule: BlockingRuleEntity)

    @Delete
    suspend fun delete(rule: BlockingRuleEntity)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY isFavorite DESC, displayName ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts")
    suspend fun getAllSnapshot(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE normalizedNumber = :normalized LIMIT 1")
    suspend fun findByNumber(normalized: String): ContactEntity?

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Update
    suspend fun update(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}

@Dao
interface SmsMessageDao {
    @Query("SELECT * FROM sms_messages ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<SmsMessageEntity>>

    @Query("SELECT COUNT(*) FROM sms_messages")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sms: SmsMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<SmsMessageEntity>)

    @Query("DELETE FROM sms_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sms_messages")
    suspend fun deleteAll()
}

@Dao
interface OfflineRegionDao {
    @Query("SELECT * FROM offline_regions ORDER BY regionName ASC")
    fun observeAll(): Flow<List<OfflineRegionEntity>>

    @Query("SELECT * FROM offline_regions WHERE regionId = :regionId LIMIT 1")
    suspend fun getById(regionId: String): OfflineRegionEntity?

    @Query("SELECT COUNT(*) FROM offline_regions")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(region: OfflineRegionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(regions: List<OfflineRegionEntity>)
}

@Dao
interface PrivacyAuditDao {
    @Query("SELECT * FROM privacy_audit_logs ORDER BY timestampMillis DESC LIMIT 60")
    fun observeRecent(): Flow<List<PrivacyAuditEntity>>

    @Query("SELECT COUNT(*) FROM privacy_audit_logs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PrivacyAuditEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<PrivacyAuditEntity>)

    @Query("DELETE FROM privacy_audit_logs")
    suspend fun clearAll()
}

@Dao
interface MissionDao {
    @Query("SELECT * FROM guardian_missions ORDER BY completed ASC, rewardPoints DESC")
    fun observeAll(): Flow<List<MissionProgressEntity>>

    @Query("SELECT * FROM guardian_missions WHERE missionId = :missionId LIMIT 1")
    suspend fun getById(missionId: String): MissionProgressEntity?

    @Query("SELECT COUNT(*) FROM guardian_missions")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mission: MissionProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(missions: List<MissionProgressEntity>)
}
