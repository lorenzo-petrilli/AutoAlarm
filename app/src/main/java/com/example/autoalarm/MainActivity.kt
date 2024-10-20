package com.example.autoalarm

import AlarmAutomation
import SystemAlarmGuide
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var editTextInterval: EditText
    private lateinit var editTextCount: EditText
    private lateinit var buttonSetAlarms: Button
    private lateinit var alarmAutomation: AlarmAutomation

    private lateinit var systemAlarmGuide: SystemAlarmGuide


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        systemAlarmGuide = SystemAlarmGuide(this)

        // Aggiungi questo dopo aver inizializzato le tue viste esistenti
        val setAlarmsButton: Button = findViewById(R.id.buttonSetAlarms)
        setAlarmsButton.setOnClickListener { setAlarms() }
        // Inizializza timePicker
        timePicker = findViewById(R.id.timePicker)
        editTextInterval = findViewById(R.id.editTextInterval)
        editTextCount = findViewById(R.id.editTextCount)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Se il permesso non è stato concesso, guida l'utente nelle impostazioni per abilitarlo
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }

        // Controllo per Android 13 (API 33+) per il permesso di POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2)
            }
        }
    }

    private fun setAlarms() {
        try {
            val startTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timePicker.hour)
                set(Calendar.MINUTE, timePicker.minute)
                set(Calendar.SECOND, 0)
            }
            val interval = editTextInterval.text.toString().toIntOrNull()
            val count = editTextCount.text.toString().toIntOrNull()

            if (interval == null || count == null) {
                Toast.makeText(this, "Per favore inserisci valori validi per intervallo e numero di sveglie", Toast.LENGTH_SHORT).show()
                return
            }

            val message = "Stai per impostare $count sveglie a partire dalle ${startTime.get(Calendar.HOUR_OF_DAY)}:${startTime.get(Calendar.MINUTE)} con un intervallo di $interval minuti. Vuoi procedere?"

            AlertDialog.Builder(this)
                .setTitle("Conferma impostazione sveglie")
                .setMessage(message)
                .setPositiveButton("Sì") { _, _ ->
                    systemAlarmGuide.guideAlarmCreation(startTime, interval, count)
                }
                .setNegativeButton("No", null)
                .show()

        } catch (e: Exception) {
            Log.e("MainActivity", "Errore nell'impostazione delle sveglie", e)
            Toast.makeText(this, "Si è verificato un errore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                1 -> Toast.makeText(this, "Permesso di impostare sveglie concesso", Toast.LENGTH_SHORT).show()
                2 -> Toast.makeText(this, "Permesso di inviare notifiche concesso", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Permesso negato. Alcune funzionalità potrebbero non funzionare.", Toast.LENGTH_SHORT).show()
        }
    }
}