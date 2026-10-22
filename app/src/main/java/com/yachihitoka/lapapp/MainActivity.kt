package com.yachihitoka.lapapp

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class MainActivity : BaseActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            scheduleDailyNotification()
        } else {
            Toast.makeText(this, "Benachrichtigungen sind deaktiviert.", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var ivCloudStatus: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ivCloudStatus = findViewById(R.id.ivCloudStatusMain)

        // Initialer Cloud-Sync beim Start (nur Laden)
        performInitialSync()

        // Debug-Listing der Assets beim Start
        Log.d("ASSETS_CHECK", "Starte Asset-Überprüfung...")
        AssetProvider.listAssets(this)

        checkNotificationPermission()

        // Click-Listener
        findViewById<View>(R.id.btnReadQuestions).setOnClickListener {
            val intent = Intent(this, ChapterActivity::class.java)
            intent.putExtra("MODE", "READ")
            startActivity(intent)
        }

        findViewById<View>(R.id.btnLearnMode).setOnClickListener {
            val intent = Intent(this, ChapterActivity::class.java)
            intent.putExtra("MODE", "LEARN")
            startActivity(intent)
        }

        findViewById<View>(R.id.btnLapMode).setOnClickListener {
            showLapSelectionDialog()
        }

        findViewById<View>(R.id.btnStats).setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performInitialSync() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            FirebaseManager.loadProgressFromCloud(this) { success ->
                runOnUiThread {
                    if (success) {
                        Log.d("SYNC", "Initialer Sync erfolgreich.")
                        ivCloudStatus.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_blue_light))
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    scheduleDailyNotification()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            scheduleDailyNotification()
        }
    }

    private fun scheduleDailyNotification() {
        val prefs = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        if (!prefs.contains("daily_reminder")) {
            prefs.edit().putBoolean("daily_reminder", true).apply()
        }

        if (!prefs.getBoolean("daily_reminder", true)) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun showLapSelectionDialog() {
        val options = arrayOf("Zufall aus allen Kapiteln", "Zufall aus bestimmtem Kapitel")
        AlertDialog.Builder(this)
            .setTitle("LAP Modus auswählen")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, QuizActivity::class.java)
                        intent.putExtra("QUIZ_MODE", "LAP_ALL")
                        startActivity(intent)
                    }
                    1 -> {
                        val intent = Intent(this, ChapterActivity::class.java)
                        intent.putExtra("MODE", "LAP_CHAPTER")
                        startActivity(intent)
                    }
                }
            }
            .show()
    }
}
