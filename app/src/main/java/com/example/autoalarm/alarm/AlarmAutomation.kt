package com.example.AutoAlarm.alarm

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import android.util.Log
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class AlarmAutomation(private val context: Context) {
    private var pendingAlarms = mutableListOf<Calendar>()
    private var currentAlarmId = 0
    private val dateFormat = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())

    fun setAlarms(startTime: Calendar, interval: Int, count: Int) {
        pendingAlarms.clear()
        
        // Prepara tutte le sveglie
        for (i in 0 until count) {
            val alarmTime = (startTime.clone() as Calendar).apply {
                add(Calendar.MINUTE, i * interval)
                
                // Se l'ora è già passata, imposta per domani
                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            pendingAlarms.add(alarmTime)
            
            val timeString = dateFormat.format(alarmTime.time)
            Log.d("AlarmAutomation", "Sveglia #${i+1} programmata per: $timeString")
        }
        
        // Inizia a impostare le sveglie
        currentAlarmId = 0
        createNextAlarmInClockApp()
    }

    fun continueSettingAlarms() {
        // Chiamato da onResume di MainActivity per continuare a impostare le sveglie
        if (currentAlarmId < pendingAlarms.size) {
            createNextAlarmInClockApp()
        } else if (pendingAlarms.isNotEmpty()) {
            // Tutte le sveglie sono state impostate
            Toast.makeText(context, "Tutte le ${pendingAlarms.size} sveglie sono state impostate", Toast.LENGTH_SHORT).show()
            pendingAlarms.clear()
        }
    }
    
    // Metodo per impostare una singola sveglia (usato da BootReceiver)
    fun setSingleAlarm(alarmId: Int, alarmTime: Calendar) {
        Log.d("AlarmAutomation", "Impostazione singola sveglia #$alarmId per: ${dateFormat.format(alarmTime.time)}")
        
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, alarmTime.get(Calendar.HOUR_OF_DAY))
            putExtra(AlarmClock.EXTRA_MINUTES, alarmTime.get(Calendar.MINUTE))
            putExtra(AlarmClock.EXTRA_MESSAGE, "AutoAlarm #$alarmId")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("AlarmAutomation", "Errore durante l'impostazione della sveglia: ${e.message}")
                Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Nessuna app Orologio trovata sul dispositivo", Toast.LENGTH_LONG).show()
            Log.e("AlarmAutomation", "Nessuna app orologio compatibile trovata")
        }
    }

    private fun createNextAlarmInClockApp() {
        if (currentAlarmId >= pendingAlarms.size) return
        
        val alarmTime = pendingAlarms[currentAlarmId]
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, alarmTime.get(Calendar.HOUR_OF_DAY))
            putExtra(AlarmClock.EXTRA_MINUTES, alarmTime.get(Calendar.MINUTE))
            putExtra(AlarmClock.EXTRA_MESSAGE, "AutoAlarm #${currentAlarmId + 1}")
            // Se TRUE, imposta la sveglia senza mostrare l'UI, ma potrebbe richiedere permessi aggiuntivi
            // Se FALSE, mostra l'UI dell'app orologio per conferma manuale
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivity(intent)
                val timeString = dateFormat.format(alarmTime.time)
                Log.d("AlarmAutomation", "Aperta app orologio per sveglia #${currentAlarmId + 1}: $timeString")
                currentAlarmId++
            } catch (e: Exception) {
                Log.e("AlarmAutomation", "Errore durante l'impostazione della sveglia: ${e.message}")
                Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                // Prova a continuare con la prossima sveglia
                currentAlarmId++
                continueSettingAlarms()
            }
        } else {
            Toast.makeText(context, "Nessuna app Orologio trovata sul dispositivo", Toast.LENGTH_LONG).show()
            Log.e("AlarmAutomation", "Nessuna app orologio compatibile trovata")
        }
    }
} 