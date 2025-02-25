package com.example.AutoAlarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = "autoalarm_channel"
        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("AutoAlarm")
            .setContentText("È ora di svegliarsi!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(notificationSound)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AutoAlarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche per le sveglie automatiche"
            }
            notificationManager.createNotificationChannel(channel)
        }

        try {
            // Controllo permessi prima di mostrare la notifica
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
                } else {
                    Log.e("AlarmReceiver", "Permesso notifiche non concesso")
                }
            } else {
                notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Errore nell'invio della notifica: ${e.message}")
        }
    }
} 