package com.danielhuettel.lapapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.io.IOException

class LapActivity : AppCompatActivity() {

    private lateinit var rvQuestions: RecyclerView
    private lateinit var tvCurrentId: TextView
    private lateinit var btnShowAnswer: Button
    private lateinit var layoutRatings: LinearLayout
    private lateinit var btnNextQuestion: Button
    
    private var allQuestions: List<Question> = listOf()
    private var currentId: String = ""
    private var currentGroup: List<Question> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lap)

        rvQuestions = findViewById(R.id.rvLapQuestions)
        tvCurrentId = findViewById(R.id.tvCurrentId)
        btnShowAnswer = findViewById(R.id.btnShowAnswer)
        layoutRatings = findViewById(R.id.layoutRatings)
        btnNextQuestion = findViewById(R.id.btnNextQuestion)
        
        allQuestions = loadAllQuestions()
        loadRandomQuestionGroup()

        btnShowAnswer.setOnClickListener {
            showAnswers()
        }

        btnNextQuestion.setOnClickListener {
            loadRandomQuestionGroup()
        }

        findViewById<Button>(R.id.btnGood).setOnClickListener { saveStatus(3) }
        findViewById<Button>(R.id.btnMedium).setOnClickListener { saveStatus(2) }
        findViewById<Button>(R.id.btnBad).setOnClickListener { saveStatus(1) }
    }

    private fun loadAllQuestions(): List<Question> {
        val list = mutableListOf<Question>()
        val path = AssetProvider.getQuestionsPath()
        try {
            val jsonString = assets.open(path).bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                
                val id = obj.getString("id")
                val frage = obj.getString("frage")
                val antwort = obj.getString("antwort")
                
                val bilderList = mutableListOf<String>()
                if (obj.has("bilder")) {
                    val bilderArray = obj.getJSONArray("bilder")
                    for (j in 0 until bilderArray.length()) {
                        bilderList.add(bilderArray.getString(j))
                    }
                }
                
                list.add(Question(id, frage, antwort, bilderList))
            }
        } catch (e: IOException) { 
            Log.e("LAP_ACTIVITY", "Fehler beim Laden der Fragen", e)
        }
        return list
    }

    private fun loadRandomQuestionGroup() {
        if (allQuestions.isEmpty()) return

        val uniqueIds = allQuestions.map { it.id }.distinct()
        currentId = uniqueIds.random()
        currentGroup = allQuestions.filter { it.id == currentId }

        tvCurrentId.text = "Frage $currentId"
        layoutRatings.visibility = View.GONE
        btnShowAnswer.visibility = View.VISIBLE
        
        setupRecyclerView(false)
    }

    private fun showAnswers() {
        setupRecyclerView(true)
        btnShowAnswer.visibility = View.GONE
        layoutRatings.visibility = View.VISIBLE
    }

    private fun setupRecyclerView(showAnswers: Boolean) {
        rvQuestions.layoutManager = LinearLayoutManager(this)
        rvQuestions.adapter = LapQuestionAdapter(currentGroup, showAnswers, showImagesInItem = true)
    }

    private fun saveStatus(status: Int) {
        val prefs = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("status_$currentId", status).apply()
        loadRandomQuestionGroup()
    }
}
