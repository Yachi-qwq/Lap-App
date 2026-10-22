package com.yachihitoka.lapapp

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SettingsActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var tvCurrentTime: TextView
    private lateinit var authManager: AuthManager
    private lateinit var ivSyncStatus: ImageView
    private lateinit var btnManualSync: Button
    private lateinit var btnLogout: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            scheduleNotification()
        } else {
            findViewById<MaterialSwitch>(R.id.switchNotifications).isChecked = false
            Toast.makeText(this, "Berechtigung abgelehnt", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        authManager = AuthManager()
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        ivSyncStatus = findViewById(R.id.iv_sync_status)
        btnManualSync = findViewById(R.id.btn_manual_sync)
        btnLogout = findViewById(R.id.btnLogout)

        updateAccountUI()

        btnManualSync.setOnClickListener {
            performManualSync()
        }

        btnLogout.setOnClickListener {
            authManager.logout()
            updateAccountUI()
            Toast.makeText(this, "Abgemeldet", Toast.LENGTH_SHORT).show()
        }

        // Notification Switch
        val switchNotifications = findViewById<MaterialSwitch>(R.id.switchNotifications)
        switchNotifications.isChecked = prefs.getBoolean("daily_reminder", true)

        updateTimeDisplay()

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("daily_reminder", isChecked).apply()
            if (isChecked) {
                checkPermissionAndSchedule()
            } else {
                cancelNotification()
            }
        }

        findViewById<Button>(R.id.btnSetTime).setOnClickListener {
            showTimePicker()
        }

        findViewById<Button>(R.id.btnResetStats).setOnClickListener {
            showPinDialog()
        }
    }

    private fun updateAccountUI() {
        val user = authManager.getCurrentUser()
        val tvStatus = findViewById<TextView>(R.id.tvAccountStatus)
        val tvEmail = findViewById<TextView>(R.id.tvAccountEmail)
        val btnAction = findViewById<Button>(R.id.btnAccountAction)
        val ivProfilePic = findViewById<ImageView>(R.id.ivProfilePic)

        if (user != null && !user.isAnonymous) {
            tvStatus.text = "Status: Synchronisiert"
            tvEmail.text = user.displayName ?: user.email
            
            val photoUrl = authManager.getUserProfilePictureUrl()
            if (photoUrl != null) {
                ivProfilePic.visibility = View.VISIBLE
                ivProfilePic.load(photoUrl) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            } else {
                ivProfilePic.visibility = View.GONE
            }

            btnManualSync.visibility = View.VISIBLE
            btnManualSync.isEnabled = true
            ivSyncStatus.visibility = View.VISIBLE
            btnLogout.visibility = View.VISIBLE
            
            btnAction.visibility = View.GONE // Hide login button when logged in
        } else {
            tvStatus.text = "Status: Nicht gesichert"
            tvEmail.text = "Sichere deinen Lernfortschritt in der Cloud"
            ivProfilePic.visibility = View.GONE
            
            btnManualSync.visibility = View.VISIBLE
            btnManualSync.isEnabled = false // Disabled if not logged in
            ivSyncStatus.visibility = View.GONE
            btnLogout.visibility = View.GONE
            
            btnAction.visibility = View.VISIBLE
            btnAction.text = "Konto erstellen / Anmelden"
            btnAction.setOnClickListener {
                showAuthDialog()
            }
        }
    }

    private fun performManualSync() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(this, "Bitte logge dich zuerst ein, um zu synchronisieren!", Toast.LENGTH_SHORT).show()
            return
        }

        btnManualSync.isEnabled = false
        btnManualSync.text = "Synchronisiere..."
        
        // Wolke auf Blau setzen (Vorgang läuft)
        ivSyncStatus.setImageResource(android.R.drawable.stat_notify_sync)
        ivSyncStatus.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_blue_light))

        FirebaseManager.saveProgressToCloud(this) { uploadSuccess ->
            if (uploadSuccess) {
                FirebaseManager.loadProgressFromCloud(this) { downloadSuccess ->
                    runOnUiThread {
                        btnManualSync.isEnabled = true
                        btnManualSync.text = "Fortschritt synchronisieren"
                        
                        if (downloadSuccess) {
                            Toast.makeText(this, "Synchronisierung abgeschlossen!", Toast.LENGTH_SHORT).show()
                            ivSyncStatus.setImageResource(android.R.drawable.stat_sys_upload_done)
                            ivSyncStatus.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_blue_light))
                        } else {
                            showSyncError("Download-Fehler")
                        }
                    }
                }
            } else {
                runOnUiThread {
                    btnManualSync.isEnabled = true
                    btnManualSync.text = "Fortschritt synchronisieren"
                    showSyncError("Upload-Fehler")
                }
            }
        }
    }

    private fun showSyncError(error: String) {
        Toast.makeText(this, "Fehler bei der Synchronisierung: $error", Toast.LENGTH_LONG).show()
        ivSyncStatus.setImageResource(android.R.drawable.stat_notify_error)
        ivSyncStatus.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_light))
        Log.e("SYNC", "Manueller Sync in Settings fehlgeschlagen: $error")
    }

    private fun showAuthDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_auth, null)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val btnGoogle = view.findViewById<Button>(R.id.btnGoogleLogin)
        val btnForgot = view.findViewById<Button>(R.id.btnForgotPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .create()

        dialog.show()

        btnForgot.setOnClickListener {
            val email = etEmail.text.toString()
            if (email.isNotEmpty()) {
                authManager.sendPasswordResetEmail(email) { success, error ->
                    if (success) Toast.makeText(this, "Reset-Link gesendet!", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this, "Fehler: $error", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Bitte E-Mail für Reset eingeben", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoogle.setOnClickListener {
            val webClientId = getString(R.string.default_web_client_id)
            if (webClientId.isEmpty()) {
                Toast.makeText(this, "Google Client ID nicht konfiguriert!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val credentialManager = CredentialManager.create(this)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val result = credentialManager.getCredential(this@SettingsActivity, request)
                    val credential = result.credential
                    if (credential is CustomCredential) {
                        authManager.signInWithGoogle(credential) { success, error ->
                            if (success) {
                                Toast.makeText(this@SettingsActivity, "Erfolgreich mit Google angemeldet!", Toast.LENGTH_SHORT).show()
                                updateAccountUI()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(this@SettingsActivity, "Google-Fehler: $error", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@SettingsActivity, "Unerwarteter Credential-Typ", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@SettingsActivity, "Login abgebrochen oder Fehler", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString()
            val pass = etPassword.text.toString()
            if (email.isNotEmpty() && pass.length >= 6) {
                authManager.linkWithEmail(email, pass) { success, error ->
                    if (success) {
                        Toast.makeText(this, "Erfolgreich gesichert!", Toast.LENGTH_SHORT).show()
                        updateAccountUI()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Fehler: $error", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Email & PW (min. 6) benötigt", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val pass = etPassword.text.toString()
            if (email.isNotEmpty() && pass.isNotEmpty()) {
                authManager.loginWithEmail(email, pass) { success, error ->
                    if (success) {
                        Toast.makeText(this, "Willkommen zurück!", Toast.LENGTH_SHORT).show()
                        updateAccountUI()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Fehler: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateTimeDisplay() {
        val hour = prefs.getInt("reminder_hour", 19)
        val minute = prefs.getInt("reminder_minute", 0)
        tvCurrentTime.text = String.format(Locale.getDefault(), "Aktuelle Zeit: %02d:%02d", hour, minute)
    }

    private fun showTimePicker() {
        val hour = prefs.getInt("reminder_hour", 19)
        val minute = prefs.getInt("reminder_minute", 0)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            prefs.edit()
                .putInt("reminder_hour", selectedHour)
                .putInt("reminder_minute", selectedMinute)
                .apply()
            
            updateTimeDisplay()
            if (prefs.getBoolean("daily_reminder", true)) {
                checkPermissionAndSchedule()
            }
        }, hour, minute, true).show()
    }

    private fun checkPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                scheduleNotification()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            scheduleNotification()
        }
    }

    private fun scheduleNotification() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hour = prefs.getInt("reminder_hour", 19)
        val minute = prefs.getInt("reminder_minute", 0)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        Toast.makeText(this, String.format(Locale.getDefault(), "Erinnerung geplant für %02d:%02d", hour, minute), Toast.LENGTH_SHORT).show()
    }

    private fun cancelNotification() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Toast.makeText(this, "Erinnerung deaktiviert", Toast.LENGTH_SHORT).show()
    }

    private fun showPinDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input.hint = "PIN eingeben"
        
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val margin = (24 * resources.displayMetrics.density).toInt()
        params.setMargins(margin, (8 * resources.displayMetrics.density).toInt(), margin, 0)
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle("PIN erforderlich")
            .setMessage("Bitte gib den PIN ein, um die Statistik zurückzusetzen.")
            .setView(container)
            .setPositiveButton("Bestätigen") { _, _ ->
                if (input.text.toString() == "1111") {
                    showResetConfirmationDialog()
                } else {
                    Toast.makeText(this, "Falscher PIN!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Statistik zurücksetzen")
            .setMessage("Bist du sicher, dass du alle gelernten Fortschritte löschen möchtest?")
            .setPositiveButton("Löschen") { _, _ ->
                prefs.edit().clear().apply()
                // Hier eventuell Firestore-Daten löschen, falls gewünscht
                updateAccountUI()
                Toast.makeText(this, "Statistik gelöscht", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
}
