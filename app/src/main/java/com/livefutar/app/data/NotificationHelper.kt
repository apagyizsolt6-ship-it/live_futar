package com.livefutar.app.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID = "live_futar_favorites"
    private const val CHANNEL_NAME = "Kedvenc meccsek"

    const val EXTRA_MATCH_ID = "extra_match_id"
    const val ACTION_OPEN_MATCH = "com.livefutar.app.OPEN_MATCH"

    private var nextNotificationId = 1000

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Értesítés kedvenc csapatod góljáról vagy meccskezdéséről"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun notifyGoal(
        context: Context,
        title: String,
        message: String,
        matchId: Long? = null
    ) {
        show(context, title, message, matchId)
    }

    fun notifyKickoff(
        context: Context,
        title: String,
        message: String,
        matchId: Long? = null
    ) {
        show(context, title, message, matchId)
    }

    private fun show(
        context: Context,
        title: String,
        message: String,
        matchId: Long?
    ) {
        if (!hasPermission(context)) return
        ensureChannel(context)

        val launchIntent = Intent().apply {
            setClassName(context.packageName, "com.livefutar.app.MainActivity")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = ACTION_OPEN_MATCH
            if (matchId != null) {
                putExtra(EXTRA_MATCH_ID, matchId)
            }
        }

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }

        val contentIntent = PendingIntent.getActivity(
            context,
            (matchId ?: nextNotificationId.toLong()).toInt(),
            launchIntent,
            pendingFlags
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(nextNotificationId++, notification)
    }
}
