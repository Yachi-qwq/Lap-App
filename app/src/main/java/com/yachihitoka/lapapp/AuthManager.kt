package com.yachihitoka.lapapp

import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Gibt den aktuell angemeldeten Firebase-User zurück.
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Prüft, ob ein User angemeldet ist.
     */
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    /**
     * Liest den Anzeigenamen des aktuellen Users aus.
     */
    fun getUserDisplayName(): String? = auth.currentUser?.displayName

    /**
     * Liest die URL des Profilbilds des aktuellen Users aus.
     */
    fun getUserProfilePictureUrl(): String? = auth.currentUser?.photoUrl?.toString()

    /**
     * Meldet den aktuellen User ab.
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Meldet den User anonym an (für den ersten Start).
     */
    fun signInAnonymously(onComplete: (Boolean) -> Unit) {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    /**
     * Meldet den User mit einem Google-Credential an.
     * Nutzt die neue Credential Manager API.
     */
    fun signInWithGoogle(credential: CustomCredential, onComplete: (Boolean, String?) -> Unit) {
        try {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                handleFirebaseSignIn(firebaseCredential, onComplete)
            } else {
                onComplete(false, "Unerwarteter Credential-Typ")
            }
        } catch (e: Exception) {
            onComplete(false, e.message)
        }
    }

    /**
     * Behandelt den Firebase-Login. Falls der User anonym ist, wird der Account verknüpft.
     */
    private fun handleFirebaseSignIn(credential: AuthCredential, onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user != null && user.isAnonymous) {
            // Anonymen Account mit dem neuen Provider verknüpfen (Linking)
            user.linkWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onComplete(true, null)
                    } else {
                        // Falls Linking fehlschlägt (z.B. Google Account existiert schon), normal einloggen
                        loginWithCredential(credential, onComplete)
                    }
                }
        } else {
            loginWithCredential(credential, onComplete)
        }
    }

    /**
     * Führt einen einfachen Login mit Credentials durch.
     */
    private fun loginWithCredential(credential: AuthCredential, onComplete: (Boolean, String?) -> Unit) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    /**
     * Verknüpft den aktuellen Account mit E-Mail und Passwort.
     */
    fun linkWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        val credential = EmailAuthProvider.getCredential(email, password)
        auth.currentUser?.linkWithCredential(credential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) onComplete(true, null)
            else onComplete(false, task.exception?.message)
        }
    }

    /**
     * Meldet einen bestehenden User mit E-Mail und Passwort an.
     */
    fun loginWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) onComplete(true, null)
            else onComplete(false, task.exception?.message)
        }
    }

    /**
     * Sendet eine E-Mail zum Zurücksetzen des Passworts.
     */
    fun sendPasswordResetEmail(email: String, onComplete: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) onComplete(true, null)
            else onComplete(false, task.exception?.message)
        }
    }

    /**
     * Sichert den Fortschritt in Firestore.
     */
    fun backupProgressToCloud(progressMap: Map<String, Int>) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).set(mapOf("progress" to progressMap), SetOptions.merge())
    }
}
