package com.example.appinstagramreelsblocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BlockerOverlayActivity : AppCompatActivity() {

    private lateinit var passwordManager: PasswordManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var passwordInput: EditText
    private lateinit var unlockButton: Button
    private lateinit var messageText: TextView

    private val hideReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar la ventana para que aparezca sobre otras apps
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_blocker_overlay)

        passwordManager = PasswordManager(this)
        settingsManager = SettingsManager(this)

        initViews()
        setupListeners()
        updateMessage()

        // Registrar el receiver para ocultar el overlay
        val filter = IntentFilter("com.tuapp.reelsblocker.HIDE_OVERLAY")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(hideReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(hideReceiver, filter)
        }
    }

    private fun initViews() {
        passwordInput = findViewById(R.id.passwordInput)
        unlockButton = findViewById(R.id.unlockButton)
        messageText = findViewById(R.id.messageText)
    }

    private fun setupListeners() {
        unlockButton.setOnClickListener {
            handleUnlock()
        }
    }

    private fun updateMessage() {
        val nextAllowedTime = settingsManager.getNextAllowedTime()
        val message = getString(R.string.reels_blocked_message) +
                "\n\nPróxima ventana permitida: $nextAllowedTime"
        messageText.text = message
    }

    private fun handleUnlock() {
        val password = passwordInput.text.toString()

        if (password.isEmpty()) {
            Toast.makeText(this, "Ingresa la contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        if (passwordManager.verifyPassword(password)) {
            // Contraseña correcta - cerrar el overlay
            Toast.makeText(this, "Desbloqueado", Toast.LENGTH_SHORT).show()

            // Desactivar el bloqueo temporalmente
            settingsManager.setBlockingEnabled(false)

            finish()
        } else {
            // Contraseña incorrecta
            Toast.makeText(this, getString(R.string.incorrect_password), Toast.LENGTH_SHORT).show()
            passwordInput.text.clear()

            // Agregar un pequeño delay para evitar ataques de fuerza bruta
            unlockButton.isEnabled = false
            unlockButton.postDelayed({
                unlockButton.isEnabled = true
            }, 2000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(hideReceiver)
        } catch (e: Exception) {
            // Receiver ya fue desregistrado
        }
    }

    override fun onBackPressed() {
        // Prevenir que se pueda salir con el botón de atrás
        // No hacer nada
    }
}