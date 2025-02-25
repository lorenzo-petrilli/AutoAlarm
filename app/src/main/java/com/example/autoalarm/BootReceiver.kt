package com.example.autoalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            Log.d("BootReceiver", "Ricevuto BOOT_COMPLETED, ripristino sveglie")
            
            // Nota: questa funzionalità è disabilitata nella versione attuale
            // poiché la nostra app ora solo imposta sveglie nell'app Orologio
            // e non gestisce la persistenza delle sveglie
            
            /*
            val persistence = AlarmPersistence(context)
            val automation = AlarmAutomation(context)
            
            persistence.getAlarmIds().forEach { id ->
                // Non implementato in questa versione
            }
            */
        }
    }
} 