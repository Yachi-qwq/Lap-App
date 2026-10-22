package com.danielhuettel.lapapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DetailActivity : BaseActivity() {

    private val TAG = "DEBUG_DETAIL"
    private lateinit var adapter: QuestionAdapter
    private var allQuestions: List<Question> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val selectedChapter = intent.getStringExtra("CHAPTER") ?: ""
        
        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.rvQuestions)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = QuestionAdapter(listOf())
        recyclerView.adapter = adapter

        // Daten laden
        loadData(selectedChapter)

        // Suche
        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })
    }

    private fun loadData(chapter: String) {
        val fullPath = AssetProvider.getQuestionsPath()
        Log.d("DEBUG_PATH", "Pfad: $fullPath")
        
        try {
            val jsonString = AssetProvider.loadJSONFromAsset(this, fullPath)
            if (jsonString == null) {
                throw Exception("Datei konnte nicht gefunden werden.")
            }

            val type = object : TypeToken<List<Question>>() {}.type
            val rawList: List<Question> = Gson().fromJson(jsonString, type)
            
            // Filter nach Kapitel (analog zu QuizActivity)
            val filteredList = if (chapter.isEmpty()) {
                rawList
            } else {
                rawList.filter { it.id.trim().startsWith(chapter, ignoreCase = true) }
            }
            
            allQuestions = filteredList
            
            runOnUiThread {
                if (allQuestions.isEmpty()) {
                    Toast.makeText(this, "Keine Fragen für Kapitel $chapter gefunden!", Toast.LENGTH_SHORT).show()
                }
                adapter.updateList(allQuestions)
            }

        } catch (e: Exception) {
            Log.e("PFAD_FEHLER", "Fehler beim Laden von $fullPath: ${e.message}")
            Toast.makeText(this, "Fehler beim Laden der Fragen", Toast.LENGTH_LONG).show()
        }
    }
}
