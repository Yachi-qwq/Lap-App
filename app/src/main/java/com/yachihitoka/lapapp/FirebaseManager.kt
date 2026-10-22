package com.yachihitoka.lapapp

import android.content.Context

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source

object FirebaseManager {

    private const val TAG = "FirebaseSync"
    private const val COLLECTION_USERS = "users"
    private const val FIELD_PROGRESS = "progress"

    private val db: FirebaseFirestore by lazy {
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        firestore.firestoreSettings = settings
        firestore
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Lädt den gesamten lokalen Fortschritt in die Cloud hoch.
     */
    fun saveProgressToCloud(context: Context, onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onComplete(false)
            return
        }

        val prefs = context.getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        val progressMap = mutableMapOf<String, Int>()
        
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("status_") && value is Int) {
                progressMap[key.removePrefix("status_")] = value
            }
        }

        if (progressMap.isEmpty()) {
            onComplete(true) // Nichts zu tun
            return
        }

        Log.d(TAG, "Speichere ${progressMap.size} Einträge in Cloud...")
        db.collection(COLLECTION_USERS).document(user.uid)
            .set(mapOf(FIELD_PROGRESS to progressMap), SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Upload erfolgreich.")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Upload fehlgeschlagen: ${e.message}")
                onComplete(false)
            }
    }

    /**
     * Lädt den neuesten Stand vom Server und aktualisiert die lokale Datenbank.
     */
    fun loadProgressFromCloud(context: Context, onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onComplete(false)
            return
        }

        Log.d(TAG, "Lade Daten vom Server (Source.SERVER)...")
        db.collection(COLLECTION_USERS).document(user.uid)
            .get(Source.SERVER)
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val progress = document.get(FIELD_PROGRESS) as? Map<*, *>
                    updateLocalData(context, progress)
                    Log.d(TAG, "Download und lokales Update erfolgreich.")
                    onComplete(true)
                } else {
                    Log.d(TAG, "Kein Dokument auf dem Server vorhanden.")
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Download fehlgeschlagen: ${e.message}")
                onComplete(false)
            }
    }

    private fun updateLocalData(context: Context, progress: Map<*, *>?) {
        if (progress == null) return
        
        val prefs = context.getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // Optional: Vor dem Import löschen oder einfach überschreiben (ConflictStrategy = Replace Logik)
        progress.forEach { (id, status) ->
            if (id is String) {
                val cloudStatus = when(status) {
                    is Long -> status.toInt()
                    is Int -> status
                    else -> 0
                }
                editor.putInt("status_$id", cloudStatus)
            }
        }
        editor.apply()
    }

    // Veraltete Methoden für Kompatibilität oder Entfernung
    @Deprecated("Nutze saveProgressToCloud und loadProgressFromCloud", ReplaceWith("saveProgressToCloud(context) {}"))
    fun startCloudSync(context: Context, ivCloudStatus: ImageView?) { 
        ivCloudStatus?.setImageResource(android.R.drawable.stat_sys_upload_done)
        ivCloudStatus?.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_blue_light))
    }
    
    @Deprecated("Nicht mehr benötigt bei manuellem Sync")
    fun stopCloudSync() {}

    @Deprecated("Nutze saveProgressToCloud")
    fun saveQuestionStatus(questionId: String, status: Int) {
        val user = auth.currentUser ?: return
        val progressUpdate = mapOf(FIELD_PROGRESS to mapOf(questionId to status))
        db.collection(COLLECTION_USERS).document(user.uid).set(progressUpdate, SetOptions.merge())
    }

    @Deprecated("Nutze loadProgressFromCloud")
    fun syncProgressPriority(context: Context, onComplete: (Boolean) -> Unit) {
        loadProgressFromCloud(context, onComplete)
    }
}
