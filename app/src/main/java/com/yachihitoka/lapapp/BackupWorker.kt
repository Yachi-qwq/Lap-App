package com.yachihitoka.lapapp

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream

class BackupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            backupProgress()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun backupProgress() {
        val prefs = applicationContext.getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        val allEntries = prefs.all
        val backupMap = mutableMapOf<String, Int>()

        for ((key, value) in allEntries) {
            if (key.startsWith("status_") && value is Int) {
                backupMap[key] = value
            }
        }

        val jsonString = Gson().toJson(backupMap)
        val backupFile = File(applicationContext.filesDir, "lernfortschritt_backup.json")
        
        FileOutputStream(backupFile).use { fos ->
            fos.write(jsonString.toByteArray())
        }
    }
}
