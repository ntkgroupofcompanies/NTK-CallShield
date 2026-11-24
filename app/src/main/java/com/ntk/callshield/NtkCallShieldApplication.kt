package com.ntk.callshield

import android.app.Application
import com.ntk.callshield.data.local.CallShieldDatabase
import com.ntk.callshield.data.prefs.CallShieldPreferences
import com.ntk.callshield.data.remote.DefaultCarrierIntelligenceProvider
import com.ntk.callshield.data.repository.CallerIntelligenceRepository
import com.ntk.callshield.data.repository.DefaultCallerIntelligenceRepository
import com.ntk.callshield.data.repository.DefaultThreatAnalysisRepository
import com.ntk.callshield.data.repository.ThreatAnalysisRepository

class NtkCallShieldApplication : Application() {

    lateinit var database: CallShieldDatabase
        private set

    lateinit var preferences: CallShieldPreferences
        private set

    lateinit var callerRepository: CallerIntelligenceRepository
        private set

    lateinit var threatRepository: ThreatAnalysisRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = CallShieldDatabase.getInstance(this)
        preferences = CallShieldPreferences(this)
        val remoteProvider = DefaultCarrierIntelligenceProvider(liveApi = null)
        callerRepository = DefaultCallerIntelligenceRepository(
            db = database,
            preferences = preferences,
            remoteProvider = remoteProvider
        )
        threatRepository = DefaultThreatAnalysisRepository(
            db = database,
            preferences = preferences,
            callerRepo = callerRepository
        )
    }
}
