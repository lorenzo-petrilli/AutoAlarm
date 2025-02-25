package com.example.AutoAlarm.alarm

import android.content.Context

class AlarmPersistence(context: Context) {
    private val prefs = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)

    fun saveAlarm(alarmId: Int, time: Long) {
        prefs.edit().putLong("alarm_$alarmId", time).apply()
    }

    fun getAlarmIds(): List<Int> {
        return prefs.all.keys
            .filter { it.startsWith("alarm_") }
            .map { it.removePrefix("alarm_").toIntOrNull() ?: 0 }
    }
    
    fun getAlarmTime(alarmId: Int): Long {
        return prefs.getLong("alarm_$alarmId", 0)
    }
    
    fun deleteAlarm(alarmId: Int) {
        prefs.edit().remove("alarm_$alarmId").apply()
    }
    
    fun clearAllAlarms() {
        prefs.edit().clear().apply()
    }
} 