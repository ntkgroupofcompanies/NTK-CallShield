package com.ntk.callshield.telecom

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.ntk.callshield.NtkCallShieldApplication
import com.ntk.callshield.domain.CallScreeningDecisionEngine
import com.ntk.callshield.domain.CarrierVerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * OS-Compliant Two-Phase CallScreeningService for Android 10+ (API 29-36).
 *
 * Phase 1 (Synchronous / Sub-500ms):
 *   Normalizes handle, checks strictly on-device Safe List & local Contacts (protecting saved
 *   contacts even when Android triggers onScreenCall for them), evaluates carrier verification
 *   status, active protection preset, custom rules, and offline Room/Bloom spam index.
 *
 * Phase 2 (Asynchronous Enrichment & Overlay):
 *   For allowed/silenced calls, launches NtkCallerIdOverlay and privacy-safe notifications without
 *   suppressing the system dialer UI via CallStyle.
 */
class NtkCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallScreeningDecisionEngine.allowFlags(silence = false).toAndroidCallResponse())
            return
        }

        val rawHandle = callDetails.handle?.schemeSpecificPart
        val verificationStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            CarrierVerificationStatus.fromConnectionVerification(callDetails.callerNumberVerificationStatus)
        } else {
            CarrierVerificationStatus.UNVERIFIED
        }

        val app = application as? NtkCallShieldApplication
        if (app == null) {
            respondToCall(callDetails, CallScreeningDecisionEngine.allowFlags(silence = false).toAndroidCallResponse())
            return
        }

        val decision = runBlocking {
            withTimeoutOrNull(420L) {
                val systemContactName = RoleAndPermissionHelper.lookupSystemContactName(
                    this@NtkCallScreeningService,
                    rawHandle
                )
                app.callerRepository.screenIncomingCall(
                    rawHandle = rawHandle,
                    verificationStatus = verificationStatus,
                    simSlot = 1,
                    systemContactName = systemContactName,
                    persistLog = true
                )
            }
        }

        if (decision == null) {
            respondToCall(callDetails, CallScreeningDecisionEngine.allowFlags(silence = false).toAndroidCallResponse())
            return
        }

        try {
            val response = decision.responseFlags.toAndroidCallResponse()
            respondToCall(callDetails, response)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Prevented invalid CallResponse combination", e)
            respondToCall(callDetails, CallScreeningDecisionEngine.allowFlags(silence = false).toAndroidCallResponse())
            return
        }

        if (decision.showFloatingOverlay) {
            serviceScope.launch {
                val settings = app.preferences.getSnapshot()
                NtkCallerIdOverlay.show(
                    context = applicationContext,
                    decision = decision,
                    style = settings.overlayStyle,
                    verticalPosition = settings.overlayVerticalPosition
                )
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "NtkCallScreeningService"
    }
}
