package com.ntk.callshield.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.ntk.callshield.R
import com.ntk.callshield.domain.OverlayStyle
import com.ntk.callshield.domain.ScreeningDecision
import java.lang.ref.WeakReference

/**
 * Renders the non-intrusive floating Caller ID popup (via WindowManager when SYSTEM_ALERT_WINDOW
 * is granted) and posts privacy-safe notifications linking to AfterCallActivity without suppressing
 * the system dialer UI via CallStyle.
 */
object NtkCallerIdOverlay {

    private const val CHANNEL_ID = "ntk_callshield_caller_id"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeOverlayRef: WeakReference<View>? = null
    private var pendingDismissRunnable: Runnable? = null

    fun show(
        context: Context,
        decision: ScreeningDecision,
        style: OverlayStyle,
        verticalPosition: Float
    ) {
        val appContext = context.applicationContext
        postScreeningNotification(appContext, decision)

        if (!Settings.canDrawOverlays(appContext)) return

        mainHandler.post {
            runCatching {
                val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                dismissActiveOverlay(windowManager)

                val density = appContext.resources.displayMetrics.density
                val screenHeight = appContext.resources.displayMetrics.heightPixels

                val accentHex = when {
                    decision.riskScore >= 70 -> "#EF4444"
                    decision.riskScore >= 35 -> "#F59E0B"
                    else -> "#0D9488"
                }

                val container = LinearLayout(appContext).apply {
                    orientation = LinearLayout.VERTICAL
                    val padH = (16 * density).toInt()
                    val padV = (12 * density).toInt()
                    setPadding(padH, padV, padH, padV)
                    background = GradientDrawable().apply {
                        cornerRadius = 18f * density
                        setColor(if (style == OverlayStyle.ALERT_BANNER) Color.parseColor(accentHex) else Color.parseColor("#0B1220"))
                        setStroke((1.5f * density).toInt(), Color.parseColor(accentHex))
                    }
                }

                val titleView = TextView(appContext).apply {
                    text = "${decision.displayName}  ·  ${decision.verificationStatus.plainLabel}"
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, if (style == OverlayStyle.COMPACT_PILL) 13f else 15f)
                }
                container.addView(titleView)

                if (style != OverlayStyle.COMPACT_PILL) {
                    val subView = TextView(appContext).apply {
                        text = "${decision.category.label} · ${decision.lineType.label} · ${decision.plainReason}"
                        setTextColor(Color.parseColor("#E2E8F0"))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    }
                    container.addView(subView)
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = (screenHeight * verticalPosition.coerceIn(0.08f, 0.72f)).toInt()
                }

                container.setOnClickListener {
                    dismissActiveOverlay(windowManager)
                }
                windowManager.addView(container, params)
                activeOverlayRef = WeakReference(container)

                val dismissTask = Runnable {
                    dismissActiveOverlay(windowManager)
                }
                pendingDismissRunnable = dismissTask
                mainHandler.postDelayed(dismissTask, 9000L)
            }
        }
    }

    private fun dismissActiveOverlay(windowManager: WindowManager) {
        pendingDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingDismissRunnable = null
        activeOverlayRef?.get()?.let { existing ->
            runCatching {
                if (existing.isAttachedToWindow) {
                    windowManager.removeViewImmediate(existing)
                }
            }
        }
        activeOverlayRef = null
    }

    private fun postScreeningNotification(context: Context, decision: ScreeningDecision) {
        val appContext = context.applicationContext
        if (!RoleAndPermissionHelper.hasNotificationPermission(appContext)) return
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "NTK CallShield Caller ID",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows caller identity and post-call safety options."
        }
        manager.createNotificationChannel(channel)

        val afterCallIntent = AfterCallActivity.createIntent(
            context = context,
            number = decision.normalizedNumber,
            callerName = decision.displayName,
            riskScore = decision.riskScore,
            disposition = decision.disposition.badgeLabel,
            plainReason = decision.plainReason,
            verificationLabel = decision.verificationStatus.plainLabel,
            lineAndCarrier = "${decision.lineType.label} · ${decision.carrier}"
        )
        val pendingIntent = PendingIntent.getActivity(
            context,
            decision.normalizedNumber.hashCode(),
            afterCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("${decision.displayName} (${decision.disposition.badgeLabel})")
            .setContentText("${decision.verificationStatus.plainLabel} · ${decision.plainReason}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            manager.notify(decision.normalizedNumber.hashCode(), notification)
        }
    }
}
