package com.example.appinstagramreelsblocker

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("blocker_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SCHEDULE_HOUR = "schedule_hour"
        private const val KEY_SCHEDULE_MINUTE = "schedule_minute"
        private const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
        private const val KEY_BLOCKING_ENABLED = "blocking_enabled"
        private const val ALLOWED_DURATION_MINUTES = 10
    }

    /**
     * Establece el horario de inicio de la ventana de 10 minutos
     */
    fun setSchedule(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_SCHEDULE_HOUR, hour)
            .putInt(KEY_SCHEDULE_MINUTE, minute)
            .putBoolean(KEY_SCHEDULE_ENABLED, true)
            .apply()
    }

    /**
     * Obtiene la hora programada
     */
    fun getScheduledHour(): Int {
        return prefs.getInt(KEY_SCHEDULE_HOUR, 20) // Default: 8 PM
    }

    /**
     * Obtiene el minuto programado
     */
    fun getScheduledMinute(): Int {
        return prefs.getInt(KEY_SCHEDULE_MINUTE, 0) // Default: en punto
    }

    /**
     * Verifica si el horario está configurado
     */
    fun isScheduleEnabled(): Boolean {
        return prefs.getBoolean(KEY_SCHEDULE_ENABLED, false)
    }

    /**
     * Habilita o deshabilita el bloqueo general
     */
    fun setBlockingEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_BLOCKING_ENABLED, enabled)
            .apply()
    }

    /**
     * Verifica si el bloqueo está habilitado
     */
    fun isBlockingEnabled(): Boolean {
        return prefs.getBoolean(KEY_BLOCKING_ENABLED, true)
    }

    /**
     * Verifica si actualmente estamos en la ventana permitida de 10 minutos
     */
    fun isInAllowedTimeWindow(): Boolean {
        if (!isScheduleEnabled()) return false

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)

        val scheduledHour = getScheduledHour()
        val scheduledMinute = getScheduledMinute()

        // Convertir todo a minutos desde medianoche para facilitar la comparación
        val currentMinutesFromMidnight = currentHour * 60 + currentMinute
        val scheduledMinutesFromMidnight = scheduledHour * 60 + scheduledMinute
        val endMinutesFromMidnight = scheduledMinutesFromMidnight + ALLOWED_DURATION_MINUTES

        return currentMinutesFromMidnight in scheduledMinutesFromMidnight until endMinutesFromMidnight
    }

    /**
     * Obtiene los minutos restantes en la ventana permitida
     */
    fun getRemainingMinutesInWindow(): Int {
        if (!isInAllowedTimeWindow()) return 0

        val now = Calendar.getInstance()
        val currentMinutesFromMidnight = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val scheduledMinutesFromMidnight = getScheduledHour() * 60 + getScheduledMinute()
        val endMinutesFromMidnight = scheduledMinutesFromMidnight + ALLOWED_DURATION_MINUTES

        return endMinutesFromMidnight - currentMinutesFromMidnight
    }

    /**
     * Obtiene el tiempo formateado del próximo período permitido
     */
    fun getNextAllowedTime(): String {
        val hour = getScheduledHour()
        val minute = getScheduledMinute()
        return String.format("%02d:%02d", hour, minute)
    }
}