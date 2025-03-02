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
    private val alarmPersistence = AlarmPersistence(context)

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
            
            // Verifica se esiste già una sveglia a questo orario
            if (!alarmExistsAtTime(alarmTime)) {
                pendingAlarms.add(alarmTime)
                val timeString = dateFormat.format(alarmTime.time)
                Log.d("AlarmAutomation", "Sveglia #${i+1} programmata per: $timeString")
            } else {
                val timeString = dateFormat.format(alarmTime.time)
                Log.d("AlarmAutomation", "Sveglia alle $timeString già esistente, saltata")
                Toast.makeText(context, "Sveglia alle $timeString già esistente", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Inizia a impostare le sveglie
        currentAlarmId = 0
        if (pendingAlarms.isEmpty()) {
            Toast.makeText(context, "Nessuna nuova sveglia da impostare. Tutte le sveglie sono già impostate.", Toast.LENGTH_SHORT).show()
        } else {
            createNextAlarmInClockApp()
        }
    }

    // Verifica se esiste già una sveglia a questo orario
    private fun alarmExistsAtTime(time: Calendar): Boolean {
        val hour = time.get(Calendar.HOUR_OF_DAY)
        val minute = time.get(Calendar.MINUTE)
        
        // Controlla tutte le sveglie salvate
        val alarmIds = alarmPersistence.getAlarmIds()
        for (id in alarmIds) {
            val alarmTime = Calendar.getInstance().apply {
                timeInMillis = alarmPersistence.getAlarmTime(id)
            }
            
            // Confronta solo ore e minuti
            if (alarmTime.get(Calendar.HOUR_OF_DAY) == hour && 
                alarmTime.get(Calendar.MINUTE) == minute) {
                return true
            }
        }
        return false
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
        
        // Verifica se esiste già una sveglia a questo orario
        if (alarmExistsAtTime(alarmTime)) {
            Log.d("AlarmAutomation", "Sveglia già esistente a questo orario, operazione annullata")
            Toast.makeText(context, "Sveglia già esistente a questo orario", Toast.LENGTH_SHORT).show()
            return
        }
        
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
                // Salva la sveglia creata
                saveAlarm(alarmId, alarmTime)
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
                
                // Salva la sveglia creata
                saveAlarm(currentAlarmId + 1, alarmTime)
                
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
    
    // Salva la sveglia creata
    private fun saveAlarm(alarmId: Int, alarmTime: Calendar) {
        alarmPersistence.saveAlarm(alarmId, alarmTime.timeInMillis)
    }
} 