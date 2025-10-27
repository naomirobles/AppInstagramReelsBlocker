package com.example.appinstagramreelsblocker

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

class PasswordManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                "password_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback a SharedPreferences normal si hay error
            context.getSharedPreferences("password_prefs", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_PASSWORD_SET = "password_set"
    }

    /**
     * Establece la contraseña inicial
     */
    fun setPassword(password: String): Boolean {
        return try {
            val hash = hashPassword(password)
            prefs.edit()
                .putString(KEY_PASSWORD_HASH, hash)
                .putBoolean(KEY_PASSWORD_SET, true)
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Verifica si la contraseña es correcta
     */
    fun verifyPassword(password: String): Boolean {
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        val inputHash = hashPassword(password)
        return storedHash == inputHash
    }

    /**
     * Verifica si ya se estableció una contraseña
     */
    fun isPasswordSet(): Boolean {
        return prefs.getBoolean(KEY_PASSWORD_SET, false)
    }

    /**
     * Cambia la contraseña (requiere la contraseña actual)
     */
    fun changePassword(oldPassword: String, newPassword: String): Boolean {
        if (!verifyPassword(oldPassword)) {
            return false
        }
        return setPassword(newPassword)
    }

    /**
     * Hashea la contraseña con SHA-256
     */
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Resetea la contraseña (solo para casos de emergencia - requiere reinstalación)
     */
    fun resetPassword() {
        prefs.edit()
            .remove(KEY_PASSWORD_HASH)
            .remove(KEY_PASSWORD_SET)
            .apply()
    }
}