package com.example.appinstagramreelsblocker

import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var passwordManager: PasswordManager
    private lateinit var settingsManager: SettingsManager

    private lateinit var statusText: TextView
    private lateinit var enableAccessibilityButton: Button
    private lateinit var enableOverlayButton: Button
    private lateinit var setPasswordButton: Button
    private lateinit var setScheduleButton: Button
    private lateinit var blockingSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        passwordManager = PasswordManager(this)
        settingsManager = SettingsManager(this)

        initViews()
        setupListeners()
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        enableAccessibilityButton = findViewById(R.id.enableAccessibilityButton)
        enableOverlayButton = findViewById(R.id.enableOverlayButton)
        setPasswordButton = findViewById(R.id.setPasswordButton)
        setScheduleButton = findViewById(R.id.setScheduleButton)
        blockingSwitch = findViewById(R.id.blockingSwitch)
    }

    private fun setupListeners() {
        enableAccessibilityButton.setOnClickListener {
            openAccessibilitySettings()
        }

        enableOverlayButton.setOnClickListener {
            requestOverlayPermission()
        }

        setPasswordButton.setOnClickListener {
            showPasswordDialog()
        }

        setScheduleButton.setOnClickListener {
            showScheduleDialog()
        }

        blockingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                // Si intenta desactivar, pedir contraseña
                showPasswordVerificationDialog {
                    settingsManager.setBlockingEnabled(false)
                    Toast.makeText(this, "Bloqueo desactivado", Toast.LENGTH_SHORT).show()
                }
            } else {
                settingsManager.setBlockingEnabled(true)
                Toast.makeText(this, "Bloqueo activado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStatus() {
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val overlayEnabled = Settings.canDrawOverlays(this)
        val passwordSet = passwordManager.isPasswordSet()
        val scheduleSet = settingsManager.isScheduleEnabled()

        var status = "Estado de configuración:\n\n"
        status += "✓ Servicio de Accesibilidad: ${if (accessibilityEnabled) "Habilitado" else "Deshabilitado"}\n"
        status += "✓ Permiso de Superposición: ${if (overlayEnabled) "Concedido" else "Denegado"}\n"
        status += "✓ Contraseña: ${if (passwordSet) "Configurada" else "No configurada"}\n"
        status += "✓ Horario: ${if (scheduleSet) settingsManager.getNextAllowedTime() else "No configurado"}\n"

        statusText.text = status

        blockingSwitch.isChecked = settingsManager.isBlockingEnabled()

        // Actualizar texto de botones
        setPasswordButton.text = if (passwordSet)
            getString(R.string.change_password)
        else
            getString(R.string.set_password)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(this, ReelsBlockerService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedComponentName) {
                return true
            }
        }
        return false
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(
            this,
            "Busca 'Reels Blocker' y actívalo",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            Toast.makeText(this, "Permiso ya concedido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password, null)
        val passwordInput = dialogView.findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = dialogView.findViewById<EditText>(R.id.confirmPasswordInput)

        AlertDialog.Builder(this)
            .setTitle(if (passwordManager.isPasswordSet()) "Cambiar Contraseña" else "Establecer Contraseña")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val password = passwordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()

                if (password.isEmpty()) {
                    Toast.makeText(this, "La contraseña no puede estar vacía", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (password != confirmPassword) {
                    Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (password.length < 4) {
                    Toast.makeText(this, "La contraseña debe tener al menos 4 caracteres", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (passwordManager.setPassword(password)) {
                    Toast.makeText(this, getString(R.string.password_set_success), Toast.LENGTH_SHORT).show()
                    updateStatus()
                } else {
                    Toast.makeText(this, "Error al guardar la contraseña", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showScheduleDialog() {
        // Primero pedir contraseña
        if (!passwordManager.isPasswordSet()) {
            Toast.makeText(this, "Primero debes establecer una contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        showPasswordVerificationDialog {
            // Si la contraseña es correcta, mostrar el selector de horario
            val currentHour = settingsManager.getScheduledHour()
            val currentMinute = settingsManager.getScheduledMinute()

            TimePickerDialog(
                this,
                { _, hour, minute ->
                    settingsManager.setSchedule(hour, minute)
                    Toast.makeText(
                        this,
                        getString(R.string.schedule_set_success) + " ${String.format("%02d:%02d", hour, minute)}",
                        Toast.LENGTH_LONG
                    ).show()
                    updateStatus()
                },
                currentHour,
                currentMinute,
                true
            ).show()
        }
    }

    private fun showPasswordVerificationDialog(onSuccess: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_verify_password, null)
        val passwordInput = dialogView.findViewById<EditText>(R.id.passwordInput)

        AlertDialog.Builder(this)
            .setTitle("Verificar Contraseña")
            .setMessage("Ingresa tu contraseña para desactivar el bloqueo")
            .setView(dialogView)
            .setPositiveButton("Verificar") { _, _ ->
                val password = passwordInput.text.toString()
                if (passwordManager.verifyPassword(password)) {
                    onSuccess()
                } else {
                    Toast.makeText(this, getString(R.string.incorrect_password), Toast.LENGTH_SHORT).show()
                    blockingSwitch.isChecked = true
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                blockingSwitch.isChecked = true
            }
            .show()
    }
}
