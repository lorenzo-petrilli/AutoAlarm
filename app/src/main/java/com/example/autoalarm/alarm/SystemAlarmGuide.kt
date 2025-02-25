package com.example.AutoAlarm.alarm

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.AlarmClock
import android.util.Log
import java.util.Calendar

class SystemAlarmGuide(private val context: Context) {
    private val manufacturerAlarmPackages = mapOf(
        "google" to "com.google.android.deskclock",
        "samsung" to "com.samsung.android.app.clockface",
        "xiaomi" to "com.android.deskclock",
        "oppo" to "com.oppo.clock",
        "motorola" to "com.motorola.timeweatherwidget",
        "huawei" to "com.android.deskclock",
        "honor" to "com.android.deskclock",
        "generic" to "com.android.deskclock"
    )

    fun guideAlarmCreation(startTime: Calendar, interval: Int, count: Int): List<Boolean> {
        val results = mutableListOf<Boolean>()
        for (i in 0 until count) {
            try {
                val alarmTime = (startTime.clone() as Calendar).apply {
                    add(Calendar.MINUTE, i * interval)
                }
                val success = openSystemAlarmApp(alarmTime)
                results.add(success)
                if (!success) {
                    Log.w("SystemAlarmGuide", "Impossibile impostare la sveglia $i")
                }
            } catch (e: Exception) {
                Log.e("SystemAlarmGuide", "Errore nell'impostazione della sveglia $i", e)
                results.add(false)
            }
        }
        return results
    }

    private fun openSystemAlarmApp(time: Calendar): Boolean {
        // Prova prima l'intent generico
        if (openGenericAlarmApp(time)) return true

        // Poi prova con il package specifico del produttore
        val manufacturer = Build.MANUFACTURER.toLowerCase()
        Log.d("SystemAlarmGuide", "Produttore rilevato: $manufacturer")
        val packageName = manufacturerAlarmPackages[manufacturer] ?: manufacturerAlarmPackages["generic"] ?: "com.android.deskclock"
        Log.d("SystemAlarmGuide", "Tentativo con package: $packageName")

        if (openSpecificAlarmApp(time, packageName)) return true

        // Se tutto fallisce, prova ad aprire l'app sveglia senza impostare l'allarme
        return openAlarmAppWithoutSetting()
    }

    private fun openGenericAlarmApp(time: Calendar): Boolean {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, time.get(Calendar.HOUR_OF_DAY))
            putExtra(AlarmClock.EXTRA_MINUTES, time.get(Calendar.MINUTE))
            putExtra(AlarmClock.EXTRA_MESSAGE, "Sveglia impostata da AutoAlarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }

        return tryStartActivity(intent, "generica")
    }

    private fun openSpecificAlarmApp(time: Calendar, packageName: String): Boolean {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, time.get(Calendar.HOUR_OF_DAY))
            putExtra(AlarmClock.EXTRA_MINUTES, time.get(Calendar.MINUTE))
            putExtra(AlarmClock.EXTRA_MESSAGE, "Sveglia impostata da AutoAlarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            setPackage(packageName)
        }

        return tryStartActivity(intent, "specifica")
    }

    private fun openAlarmAppWithoutSetting(): Boolean {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        return tryStartActivity(intent, "senza impostazione")
    }

    private fun tryStartActivity(intent: Intent, type: String): Boolean {
        return try {
            if (isIntentAvailable(intent)) {
                context.startActivity(intent)
                Log.d("SystemAlarmGuide", "App sveglia $type aperta con successo")
                true
            } else {
                Log.e("SystemAlarmGuide", "Nessuna app sveglia $type trovata")
                false
            }
        } catch (e: Exception) {
            Log.e("SystemAlarmGuide", "Errore nell'apertura dell'app sveglia $type", e)
            false
        }
    }

    private fun isIntentAvailable(intent: Intent): Boolean {
        val activities = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        Log.d("SystemAlarmGuide", "Numero di attività trovate per l'intent: ${activities.size}")
        activities.forEach {
            Log.d("SystemAlarmGuide", "Attività trovata: ${it.activityInfo.packageName}")
        }
        return activities.isNotEmpty()
    }
} 