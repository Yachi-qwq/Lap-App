package com.yachihitoka.lapapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class QuizActivity : AppCompatActivity() {

    private val TAG = "QUIZ_CRASH"

    private lateinit var rvQuestions: RecyclerView
    private var btnShowAnswer: View? = null
    private var layoutRatings: View? = null
    private var btnNextQuestion: View? = null
    private var btnPrevQuestion: View? = null

    private var btnBad: MaterialButton? = null
    private var btnMedium: MaterialButton? = null
    private var btnGood: MaterialButton? = null
    private lateinit var adapter: LapQuestionAdapter

    private var allQuestions: List<Question> = listOf()
    private var filteredIds: List<String> = listOf()
    private var currentId: String = ""
    private var currentIdIndex: Int = -1
    
    private var quizMode: String = ""
    private var selectedChapter: String = ""
    private var drillStatus: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_quiz)

            quizMode = intent.getStringExtra("QUIZ_MODE") ?: "LAP_ALL"
            selectedChapter = intent.getStringExtra("CHAPTER") ?: ""
            drillStatus = intent.getIntExtra("DRILL_STATUS", -1)
            
            Log.d(TAG, "Activity gestartet. Modus: $quizMode, Kapitel: $selectedChapter, Drill: $drillStatus")

            // Toolbar Setup
            findViewById<TextView>(R.id.tvToolbarTitle)?.text = when(quizMode) {
                "LEARN" -> "Lernen: Kapitel $selectedChapter"
                "LAP_CHAPTER" -> "LAP: Kapitel $selectedChapter"
                "DRILL_DOWN" -> "Fokus: Kapitel $selectedChapter"
                else -> "LAP: Alle Kapitel"
            }
            findViewById<ImageButton>(R.id.btnHome)?.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }

            // View Binding
            rvQuestions = findViewById(R.id.rvLapQuestions)
            btnShowAnswer = findViewById(R.id.btnShowAnswer)
            layoutRatings = findViewById(R.id.layoutRatings)
            btnNextQuestion = findViewById(R.id.btnNextQuestion)
            btnPrevQuestion = findViewById(R.id.btnPrevQuestion)
            btnBad = findViewById(R.id.btnBad)
            btnMedium = findViewById(R.id.btnMedium)
            btnGood = findViewById(R.id.btnGood)

            // Adapter initialisieren (Bilder im Item JETZT AKTIVIERT)
            adapter = LapQuestionAdapter(listOf(), false, showImagesInItem = true) { id, status ->
                if (id == currentId) {
                    updateRatingButtonsHighlight()
                }
            }
            rvQuestions.layoutManager = LinearLayoutManager(this)
            rvQuestions.adapter = adapter

            // Daten laden
            loadData()
            
            // Listener
            btnShowAnswer?.setOnClickListener { showAnswers() }
            btnNextQuestion?.setOnClickListener { nextQuestion() }
            btnPrevQuestion?.setOnClickListener { previousQuestion() }

            btnGood?.setOnClickListener { v -> v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); saveStatus(3) }
            btnMedium?.setOnClickListener { v -> v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); saveStatus(2) }
            btnBad?.setOnClickListener { v -> v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); saveStatus(1) }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler in onCreate: ", e)
            Toast.makeText(this, "Fehler beim Starten des Quiz: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun loadData() {
        val prefs = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        val rawList = AssetProvider.loadQuestions(this)
        
        var filteredList = if (quizMode == "LAP_ALL") {
            rawList
        } else {
            rawList.filter { it.id.trim().startsWith(selectedChapter, ignoreCase = true) }
        }
        
        if (quizMode == "DRILL_DOWN" && drillStatus != -1) {
            filteredList = filteredList.filter { 
                prefs.getInt("status_${it.id}", 0) == drillStatus
            }
        }
        
        allQuestions = filteredList

        runOnUiThread {
            if (allQuestions.isEmpty()) {
                val msg = if (quizMode == "DRILL_DOWN") "Keine Fragen mit diesem Status gefunden." else "Keine Fragen gefunden."
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                finish()
            } else {
                prepareQuiz()
                loadQuestionAtCurrentIndex()
            }
        }
    }

    private fun prepareQuiz() {
        val allIds = allQuestions.map { it.id }.distinct()
        
        filteredIds = if (quizMode == "LEARN" || quizMode == "DRILL_DOWN") {
            sortIds(allIds)
        } else {
            allIds.shuffled()
        }

        if (quizMode == "LEARN") {
            val prefs = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
            val lastSavedId = prefs.getString("last_id_$selectedChapter", "") ?: ""
            currentIdIndex = filteredIds.indexOf(lastSavedId)
            if (currentIdIndex == -1) currentIdIndex = 0
        } else {
            currentIdIndex = 0
        }
    }

    private fun sortIds(ids: List<String>): List<String> {
        return ids.sortedWith(compareBy(
            { it.filter { char -> char.isLetter() } },
            { it.filter { char -> char.isDigit() }.ifEmpty { "0" }.toInt() }
        ))
    }

    private fun loadQuestionAtCurrentIndex() {
        if (filteredIds.isEmpty()) return
        
        currentId = filteredIds[currentIdIndex]

        if (quizMode == "LEARN") {
            getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE).edit()
                .putString("last_id_$selectedChapter", currentId).apply()
        }

        val group = allQuestions.filter { it.id == currentId }
        
        findViewById<TextView>(R.id.tvCurrentId)?.text = "Frage $currentId"

        layoutRatings?.visibility = View.GONE
        btnShowAnswer?.visibility = View.VISIBLE
        
        btnPrevQuestion?.isEnabled = currentIdIndex > 0
        btnNextQuestion?.isEnabled = currentIdIndex < filteredIds.size - 1

        adapter.updateList(group)
        adapter.setShowAnswers(false)
        
        updateRatingButtonsHighlight()
    }

    private fun nextQuestion() {
        if (currentIdIndex < filteredIds.size - 1) {
            currentIdIndex++
            loadQuestionAtCurrentIndex()
        }
    }

    private fun previousQuestion() {
        if (currentIdIndex > 0) {
            currentIdIndex--
            loadQuestionAtCurrentIndex()
        }
    }

    private fun showAnswers() {
        adapter.setShowAnswers(true)
        btnShowAnswer?.visibility = View.GONE
        layoutRatings?.visibility = View.VISIBLE
    }

    private fun saveStatus(status: Int) {
        getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE).edit()
            .putInt("status_$currentId", status).apply()
        updateRatingButtonsHighlight()
        adapter.notifyDataSetChanged()
    }

    private fun updateRatingButtonsHighlight() {
        val prefs = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        val status = prefs.getInt("status_$currentId", 0)

        btnBad?.let { resetButtonStyle(it) }
        btnMedium?.let { resetButtonStyle(it) }
        btnGood?.let { resetButtonStyle(it) }

        when (status) {
            1 -> btnBad?.let { highlightButton(it, "#F44336") }
            2 -> btnMedium?.let { highlightButton(it, "#FF9800") }
            3 -> btnGood?.let { highlightButton(it, "#4CAF50") }
        }
    }

    private fun highlightButton(button: MaterialButton, colorHex: String) {
        button.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(colorHex))
        button.setTextColor(Color.WHITE)
        button.iconTint = android.content.res.ColorStateList.valueOf(Color.WHITE)
        button.scaleX = 1.1f
        button.scaleY = 1.1f
    }

    private fun resetButtonStyle(button: MaterialButton) {
        button.backgroundTintList = null
        button.scaleX = 1.0f
        button.scaleY = 1.0f
        val color = when(button.id) {
            R.id.btnBad -> "#F44336"
            R.id.btnMedium -> "#FF9800"
            R.id.btnGood -> "#4CAF50"
            else -> "#FFFFFF"
        }
        button.setTextColor(Color.parseColor(color))
        button.iconTint = android.content.res.ColorStateList.valueOf(Color.parseColor(color))
    }
}
