package com.example.AutoAlarm.ui.main

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import android.widget.Toast
import android.widget.TextView
import android.widget.LinearLayout
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.view.LayoutInflater
import com.example.AutoAlarm.R
import com.example.AutoAlarm.alarm.AlarmAutomation

class MainActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var editTextInterval: EditText
    private lateinit var editTextCount: EditText
    private lateinit var buttonSetAlarms: Button
    private lateinit var switchTimeFormat: SwitchCompat
    private lateinit var lastAlarmLayout: LinearLayout
    private lateinit var tvLastAlarmTime: TextView
    private lateinit var alarmAutomation: AlarmAutomation
    private lateinit var sharedPreferences: SharedPreferences
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
    
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
        switchTimeFormat = findViewById(R.id.switchTimeFormat)
        lastAlarmLayout = findViewById(R.id.lastAlarmLayout)
        tvLastAlarmTime = findViewById(R.id.tvLastAlarmTime)

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
            // Aggiorna la visualizzazione dell'ultima sveglia
            updateLastAlarmDisplay()
        }
        
        // Aggiungi listener per i campi di input
        timePicker.setOnTimeChangedListener { _, _, _ -> updateLastAlarmDisplay() }
        
        editTextInterval.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateLastAlarmDisplay() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        editTextCount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateLastAlarmDisplay() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        buttonSetAlarms.setOnClickListener {
            setAlarms()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showAboutDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
        
        // Configura il listener per il link GitHub nel dialog
        dialogView.findViewById<Button>(R.id.tvAboutGithubLink).setOnClickListener {
            openGithubPage()
        }
        
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.close)) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
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
            Toast.makeText(this, getString(R.string.error_opening_browser, e.message), Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "Errore durante l'apertura del browser: ${e.message}")
        }
    }
    
    private fun updateLastAlarmDisplay() {
        try {
            val interval = editTextInterval.text.toString().toIntOrNull() ?: 0
            val count = editTextCount.text.toString().toIntOrNull() ?: 0
            
            if (interval <= 0 || count <= 0) {
                lastAlarmLayout.visibility = View.GONE
                return
            }
            
            val startTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timePicker.hour)
                set(Calendar.MINUTE, timePicker.minute)
                set(Calendar.SECOND, 0)
                
                // Aggiungi automaticamente un giorno se l'ora è già passata oggi
                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            
            // Calcola l'orario dell'ultima sveglia
            val lastAlarmTime = (startTime.clone() as Calendar).apply {
                add(Calendar.MINUTE, (count - 1) * interval)
            }
            
            // Mostra l'orario dell'ultima sveglia
            tvLastAlarmTime.text = dateTimeFormat.format(lastAlarmTime.time)
            lastAlarmLayout.visibility = View.VISIBLE
            
        } catch (e: Exception) {
            Log.e("LastAlarmDisplay", "Errore nel calcolo dell'ultima sveglia", e)
            lastAlarmLayout.visibility = View.GONE
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
                Toast.makeText(this, getString(R.string.enter_valid_values), Toast.LENGTH_SHORT).show()
                return
            }
            
            val timeString = timeFormat.format(startTime.time)
            
            // Calcola l'orario dell'ultima sveglia
            val lastAlarmTime = (startTime.clone() as Calendar).apply {
                add(Calendar.MINUTE, (count - 1) * interval)
            }
            val lastAlarmTimeString = dateTimeFormat.format(lastAlarmTime.time)
            
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.alarm_setup_title))
                .setMessage(getString(R.string.alarm_setup_message, count, timeString, interval, lastAlarmTimeString))
                .setPositiveButton(getString(R.string.proceed)) { _, _ ->
                    alarmAutomation.setAlarms(startTime, interval, count)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } catch (e: Exception) {
            Log.e("SetAlarms", "Errore durante l'impostazione delle sveglie", e)
            Toast.makeText(this, getString(R.string.error_occurred, e.message), Toast.LENGTH_SHORT).show()
        }
    }
} 