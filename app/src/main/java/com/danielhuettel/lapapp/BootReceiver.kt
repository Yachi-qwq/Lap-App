package com.danielhuettel.lapapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Backup neu planen
            scheduleBackup(context)
            
            // AlarmManager-Benachrichtigung neu planen
            val settingsActivity = SettingsActivity() // Temporär für Zugriff auf Logik
            // In einer echten App würde man die schedule-Logik in eine Helper-Klasse auslagern
        }
    }

    private fun scheduleBackup(context: Context) {
        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DailyBackup",
            ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )
    }
}
