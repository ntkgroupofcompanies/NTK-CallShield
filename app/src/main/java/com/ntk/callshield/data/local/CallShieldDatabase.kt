package com.ntk.callshield.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PhoneIntelEntity::class,
        CallLogEntity::class,
        BlockingRuleEntity::class,
        ContactEntity::class,
        SmsMessageEntity::class,
        OfflineRegionEntity::class,
        PrivacyAuditEntity::class,
        MissionProgressEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CallShieldDatabase : RoomDatabase() {
    abstract fun phoneIntelDao(): PhoneIntelDao
    abstract fun callLogDao(): CallLogDao
    abstract fun blockingRuleDao(): BlockingRuleDao
    abstract fun contactDao(): ContactDao
    abstract fun smsMessageDao(): SmsMessageDao
    abstract fun offlineRegionDao(): OfflineRegionDao
    abstract fun privacyAuditDao(): PrivacyAuditDao
    abstract fun missionDao(): MissionDao

    companion object {
        @Volatile
        private var INSTANCE: CallShieldDatabase? = null

        fun getInstance(context: Context): CallShieldDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CallShieldDatabase::class.java,
                    "ntk_callshield_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
