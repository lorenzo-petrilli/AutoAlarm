package com.example.autoalarm

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri

class MainActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var editTextInterval: EditText
    private lateinit var editTextCount: EditText
    private lateinit var buttonSetAlarms: Button
    private lateinit var tvGithubLink: TextView
    private lateinit var switchTimeFormat: SwitchCompat
    private lateinit var alarmAutomation: AlarmAutomation
    private lateinit var sharedPreferences: SharedPreferences
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    // Link al repository GitHub (modifica con il tuo URL)
    private val githubUrl = "https://github.com/lorenzo-petrilli/AutoAlarm"
    
    // Chiave per salvare la preferenza del formato orario
    private val KEY_TIME_FORMAT = "time_format_24h"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Inizializzazione delle SharedPreferences
        sharedPreferences = getSharedPreferences("autoalarm_prefs", MODE_PRIVATE)

        timePicker = findViewById(R.id.timePicker)
        editTextInterval = findViewById(R.id.editTextInterval)
        editTextCount = findViewById(R.id.editTextCount)
        buttonSetAlarms = findViewById(R.id.buttonSetAlarms)
        tvGithubLink = findViewById(R.id.tvGithubLink)
        switchTimeFormat = findViewById(R.id.switchTimeFormat)

        alarmAutomation = AlarmAutomation(this)
        
        // Ripristina l'ultima preferenza di formato orario salvata
        val is24HourFormat = sharedPreferences.getBoolean(KEY_TIME_FORMAT, true)
        switchTimeFormat.isChecked = is24HourFormat
        setTimePickerFormat(is24HourFormat)
        
        // Imposta il listener per il cambio di formato orario
        switchTimeFormat.setOnCheckedChangeListener { _, isChecked ->
            setTimePickerFormat(isChecked)
            // Salva la preferenza
            sharedPreferences.edit().putBoolean(KEY_TIME_FORMAT, isChecked).apply()
        }

        buttonSetAlarms.setOnClickListener {
            setAlarms()
        }
        
        // Configurazione click sul link GitHub
        tvGithubLink.setOnClickListener {
            openGithubPage()
        }
    }
    
    private fun setTimePickerFormat(is24HourFormat: Boolean) {
        timePicker.setIs24HourView(is24HourFormat)
    }
    
    override fun onResume() {
        super.onResume()
        // Quando torniamo dall'app orologio, continua a impostare le prossime sveglie se ce ne sono
        alarmAutomation.continueSettingAlarms()
    }
    
    private fun openGithubPage() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Impossibile aprire il browser: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "Errore durante l'apertura del browser: ${e.message}")
        }
    }

    private fun setAlarms() {
        try {
            val startTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timePicker.hour)
                set(Calendar.MINUTE, timePicker.minute)
                set(Calendar.SECOND, 0)
                
                // Aggiungi automaticamente un giorno se l'ora è già passata oggi
                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val interval = editTextInterval.text.toString().toIntOrNull()
            val count = editTextCount.text.toString().toIntOrNull()

            if (interval == null || count == null || interval <= 0 || count <= 0) {
                Toast.makeText(this, "Inserisci valori validi positivi per intervallo e numero di sveglie", Toast.LENGTH_SHORT).show()
                return
            }
            
            val timeString = timeFormat.format(startTime.time)
            
            AlertDialog.Builder(this)
                .setTitle("Impostazione Sveglie")
                .setMessage("Verranno impostate $count sveglie nell'app Orologio, a partire dalle $timeString con intervallo di $interval minuti.\n\nDovrai confermare ogni sveglia quando appare l'app Orologio.")
                .setPositiveButton("Procedi") { _, _ ->
                    alarmAutomation.setAlarms(startTime, interval, count)
                }
                .setNegativeButton("Annulla", null)
                .show()
        } catch (e: Exception) {
            Log.e("SetAlarms", "Errore durante l'impostazione delle sveglie", e)
            Toast.makeText(this, "Si è verificato un errore: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}