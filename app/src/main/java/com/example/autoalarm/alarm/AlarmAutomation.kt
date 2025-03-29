package com.example.AutoAlarm.alarm

// import android.app.AlarmManager // Rimosso se non più usato altrove
// import android.app.PendingIntent // Rimosso se non più usato altrove
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import android.util.Log
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.AutoAlarm.R

class AlarmAutomation(private val context: Context) {
    // Unificata la lista delle sveglie da impostare
    private var alarmsToSet = mutableListOf<Calendar>()
    // Indice della prossima sveglia da processare
    private var currentAlarmIndex = 0
    private val dateFormat = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
    private val alarmPersistence = AlarmPersistence(context)

    fun setAlarms(startTime: Calendar, interval: Int, count: Int) {
        alarmsToSet.clear() // Pulisce la lista per una nuova impostazione

        // Prepara tutte le sveglie richieste
        for (i in 0 until count) {
            val alarmTime = (startTime.clone() as Calendar).apply {
                add(Calendar.MINUTE, i * interval)

                // Se l'ora è già passata oggi, imposta per domani
                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            alarmsToSet.add(alarmTime)
            val timeString = dateFormat.format(alarmTime.time)
            Log.d("AlarmAutomation", "Sveglia #${i + 1} richiesta per: $timeString")
        }

        // Inizia il processo di impostazione
        currentAlarmIndex = 0

        if (alarmsToSet.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.no_alarms_to_set), Toast.LENGTH_SHORT).show()
        } else {
            val totalMsg = context.getString(R.string.starting_alarm_setup, alarmsToSet.size)
            Toast.makeText(context, totalMsg, Toast.LENGTH_LONG).show()
            // Inizia a processare la prima sveglia
            processNextAlarm()
        }
    }

    // Rimosse le funzioni: activateNextExistingAlarm, ensureAlarmIsEnabled, alarmExistsAtTime
    // perché ora gestiamo tutto tramite un unico flusso.

    // Funzione chiamata da onResume di MainActivity per continuare il processo
    fun continueSettingAlarms() {
        if (currentAlarmIndex < alarmsToSet.size) {
            // Se ci sono ancora sveglie da processare, continua
            Log.d("AlarmAutomation", "Continuazione: processando sveglia ${currentAlarmIndex + 1}/${alarmsToSet.size}")
            processNextAlarm()
        } else if (alarmsToSet.isNotEmpty()) {
            // Tutte le sveglie sono state processate (l'utente è tornato dopo l'ultima)
            val totalMessage = context.getString(R.string.operation_completed, alarmsToSet.size)
            Toast.makeText(context, totalMessage, Toast.LENGTH_LONG).show()
            Log.d("AlarmAutomation", "Tutte le ${alarmsToSet.size} sveglie sono state processate.")
            // Pulisce la lista dopo il completamento
            alarmsToSet.clear()
            currentAlarmIndex = 0 // Resetta per future operazioni
        }
        // Se alarmsToSet è vuota, non fare nulla (potrebbe essere chiamata onResume senza un processo attivo)
    }

    // Metodo per impostare una singola sveglia (es. da BootReceiver)
    fun setSingleAlarm(alarmId: Int, alarmTime: Calendar) {
        Log.d("AlarmAutomation", "Richiesta impostazione singola sveglia #$alarmId per: ${dateFormat.format(alarmTime.time)}")

        // Invia direttamente l'intento per impostare/attivare la sveglia
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, alarmTime.get(Calendar.HOUR_OF_DAY))
            putExtra(AlarmClock.EXTRA_MINUTES, alarmTime.get(Calendar.MINUTE))
            // Potresti voler usare un messaggio specifico o generico qui
            putExtra(AlarmClock.EXTRA_MESSAGE, "AutoAlarm #$alarmId")
            putExtra(AlarmClock.EXTRA_VIBRATE, true) // Assicura attivazione
            putExtra(AlarmClock.EXTRA_SKIP_UI, false) // Mostra UI per conferma
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivity(intent)
                val formattedTime = String.format("%02d:%02d", alarmTime.get(Calendar.HOUR_OF_DAY), alarmTime.get(Calendar.MINUTE))
                Toast.makeText(context, context.getString(R.string.confirm_alarm, 1, 1, formattedTime), Toast.LENGTH_SHORT).show()
                // Salva comunque la sveglia per riferimento interno
                saveAlarm(alarmId, alarmTime)
                Log.d("AlarmAutomation", "Intento inviato per sveglia singola #$alarmId.")
            } catch (e: Exception) {
                Log.e("AlarmAutomation", "Errore durante l'impostazione della sveglia singola: ${e.message}")
                Toast.makeText(context, context.getString(R.string.error_occurred, e.message), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, context.getString(R.string.no_clock_app), Toast.LENGTH_LONG).show()
            Log.e("AlarmAutomation", "Nessuna app orologio compatibile trovata per sveglia singola.")
        }
    }

    // Processa la prossima sveglia nella lista `alarmsToSet`
    private fun processNextAlarm() {
        // Controlla se ci sono ancora sveglie da impostare
        if (currentAlarmIndex >= alarmsToSet.size) {
            Log.d("AlarmAutomation", "Processo terminato, indice $currentAlarmIndex >= dimensione ${alarmsToSet.size}")
            // Non dovrebbe arrivare qui se chiamato da continueSettingAlarms, ma è una sicurezza.
            // Il messaggio finale viene dato in continueSettingAlarms quando l'utente torna dopo l'ultima sveglia.
            return
        }

        val alarmTime = alarmsToSet[currentAlarmIndex]
        val timeString = dateFormat.format(alarmTime.time)
        val userFriendlyTime = String.format("%02d:%02d", alarmTime.get(Calendar.HOUR_OF_DAY), alarmTime.get(Calendar.MINUTE))

        Log.d("AlarmAutomation", "Processando sveglia ${currentAlarmIndex + 1}/${alarmsToSet.size} per le $userFriendlyTime")

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, alarmTime.get(Calendar.HOUR_OF_DAY))
            putExtra(AlarmClock.EXTRA_MINUTES, alarmTime.get(Calendar.MINUTE))
            putExtra(AlarmClock.EXTRA_MESSAGE, "AutoAlarm") // Messaggio generico
            putExtra(AlarmClock.EXTRA_VIBRATE, true) // Assicura che sia attiva
            putExtra(AlarmClock.EXTRA_SKIP_UI, false) // Richiede conferma utente
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            try {
                val message = context.getString(R.string.confirm_alarm, 
                    currentAlarmIndex + 1, alarmsToSet.size, userFriendlyTime)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                context.startActivity(intent)
                Log.d("AlarmAutomation", "Aperta app orologio per sveglia #${currentAlarmIndex + 1}: $timeString")

                // Salva la sveglia (o il tentativo) per riferimento interno
                // L'ID qui potrebbe essere semplicemente l'indice + 1
                saveAlarm(currentAlarmIndex + 1, alarmTime)

                // Incrementa l'indice per la prossima chiamata a continueSettingAlarms
                currentAlarmIndex++

            } catch (e: Exception) {
                Log.e("AlarmAutomation", "Errore durante l'invio intento per sveglia ${currentAlarmIndex + 1}: ${e.message}")
                Toast.makeText(context, context.getString(R.string.error_occurred, e.message), Toast.LENGTH_SHORT).show()
                // Prova comunque a passare alla prossima sveglia al ritorno dell'utente
                currentAlarmIndex++
                // Non chiamare continueSettingAlarms qui per evitare loop in caso di errore rapido
            }
        } else {
            Toast.makeText(context, context.getString(R.string.no_clock_app), Toast.LENGTH_LONG).show()
            Log.e("AlarmAutomation", "Nessuna app orologio compatibile trovata.")
            // Interrompi il processo se l'app orologio non è disponibile
            alarmsToSet.clear()
            currentAlarmIndex = 0
        }
    }

    // Salva la sveglia (o il tentativo di impostarla) per riferimento interno
    private fun saveAlarm(alarmId: Int, alarmTime: Calendar) {
        // L'ID usato qui è più un riferimento sequenziale dell'operazione corrente
        alarmPersistence.saveAlarm(alarmId, alarmTime.timeInMillis)
        Log.d("AlarmAutomation", "Salvato riferimento per sveglia #$alarmId (${dateFormat.format(alarmTime.time)})")
    }

    // Funzione di verifica esistenza, non usata nel flusso principale ma potrebbe servire altrove
    private fun alarmExistsAtTime(time: Calendar): Boolean {
        val hour = time.get(Calendar.HOUR_OF_DAY)
        val minute = time.get(Calendar.MINUTE)

        val alarmIds = alarmPersistence.getAlarmIds()
        for (id in alarmIds) {
            try {
                val savedTimeMillis = alarmPersistence.getAlarmTime(id)
                // Se il tempo salvato è 0 o negativo, ignora questo record
                 if (savedTimeMillis <= 0) continue

                val alarmTime = Calendar.getInstance().apply {
                    timeInMillis = savedTimeMillis
                }
                // Confronta solo ore e minuti
                if (alarmTime.get(Calendar.HOUR_OF_DAY) == hour &&
                    alarmTime.get(Calendar.MINUTE) == minute) {
                    Log.d("AlarmAutomation", "Trovata corrispondenza in persistenza per ${String.format("%02d:%02d", hour, minute)}")
                    return true
                }
            } catch (e: Exception) {
                 Log.e("AlarmAutomation", "Errore durante il controllo di esistenza per ID $id: ${e.message}")
                 // Potrebbe accadere se un record è corrotto, continua con il prossimo
            }
        }
        Log.d("AlarmAutomation", "Nessuna corrispondenza in persistenza per ${String.format("%02d:%02d", hour, minute)}")
        return false
    }
    
    // Funzione per cancellare tutti i riferimenti interni alle sveglie
    fun clearSavedAlarms() {
        try {
            val alarmIds = alarmPersistence.getAlarmIds()
            if (alarmIds.isEmpty()) {
                Log.d("AlarmAutomation", "Nessuna sveglia salvata da eliminare")
                return
            }
            
            val count = alarmIds.size
            for (id in alarmIds) {
                alarmPersistence.deleteAlarm(id)
            }
            
            Log.d("AlarmAutomation", "Eliminati $count riferimenti interni a sveglie")
        } catch (e: Exception) {
            Log.e("AlarmAutomation", "Errore durante la cancellazione dei riferimenti alle sveglie: ${e.message}")
        }
    }
} 