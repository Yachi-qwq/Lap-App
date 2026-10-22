package com.danielhuettel.lapapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StatisticsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val allQuestions = loadAllQuestions()
        val prefs = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)

        val statsByChapter = calculateStats(allQuestions, prefs)
        displayStats(statsByChapter)
    }

    private fun loadAllQuestions(): List<Question> {
        val fullPath = AssetProvider.getQuestionsPath()
        return try {
            val jsonString = AssetProvider.loadJSONFromAsset(this, fullPath)
            if (jsonString == null) return listOf()
            val type = object : TypeToken<List<Question>>() {}.type
            Gson().fromJson(jsonString, type)
        } catch (e: Exception) {
            Log.e("PFAD_FEHLER", "Fehler beim Laden von $fullPath", e)
            listOf()
        }
    }

    private fun calculateStats(questions: List<Question>, prefs: android.content.SharedPreferences): Map<Char, ChapterStats> {
        val statsMap = mutableMapOf<Char, ChapterStats>()
        val chapters = listOf('A', 'B', 'C', 'D', 'E', 'F')
        chapters.forEach { statsMap[it] = ChapterStats() }

        val groupedQuestions = questions.groupBy { it.id.trim() }

        groupedQuestions.forEach { (id, _) ->
            if (id.isEmpty()) return@forEach
            val chapter = id.first().uppercaseChar()
            if (statsMap.containsKey(chapter)) {
                val status = prefs.getInt("status_$id", 0)
                val stats = statsMap[chapter]!!
                stats.total++
                when (status) {
                    1 -> stats.red++
                    2 -> stats.orange++
                    3 -> stats.green++
                    else -> stats.unanswered++
                }
            }
        }
        return statsMap
    }

    private fun displayStats(statsMap: Map<Char, ChapterStats>) {
        val layoutContainer = findViewById<LinearLayout>(R.id.layoutChapterStats)
        layoutContainer.removeAllViews()
        
        var totalQ = 0
        var totalG = 0
        var totalO = 0
        var totalR = 0
        var totalU = 0

        statsMap.forEach { (chapter, stats) ->
            totalQ += stats.total
            totalG += stats.green
            totalO += stats.orange
            totalR += stats.red
            totalU += stats.unanswered
            
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_chapter_stats, layoutContainer, false)
            itemView.findViewById<TextView>(R.id.tvChapterName).text = "Kapitel $chapter"
            val answeredCount = stats.green + stats.orange + stats.red
            val chapterPercent = if (stats.total > 0) (answeredCount * 100 / stats.total) else 0
            itemView.findViewById<TextView>(R.id.tvChapterDetails).text = "$answeredCount / ${stats.total} ($chapterPercent%)"

            if (stats.total > 0) {
                itemView.findViewById<View>(R.id.viewGreen).layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, stats.green.toFloat())
                itemView.findViewById<View>(R.id.viewOrange).layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, stats.orange.toFloat())
                itemView.findViewById<View>(R.id.viewRed).layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, stats.red.toFloat())
                itemView.findViewById<View>(R.id.viewUnanswered).layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, stats.unanswered.toFloat())
            }

            val expandable = itemView.findViewById<LinearLayout>(R.id.layoutExpandable)
            itemView.findViewById<View>(R.id.cardChapterStats).setOnClickListener {
                expandable.visibility = if (expandable.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }

            val redP = if (stats.total > 0) (stats.red * 100 / stats.total) else 0
            val orangeP = if (stats.total > 0) (stats.orange * 100 / stats.total) else 0
            val greenP = if (stats.total > 0) (stats.green * 100 / stats.total) else 0
            val unansweredP = if (stats.total > 0) (stats.unanswered * 100 / stats.total) else 0

            itemView.findViewById<TextView>(R.id.tvCountUnanswered).text = "${stats.unanswered} ($unansweredP%)"
            itemView.findViewById<TextView>(R.id.tvCountRed).text = "${stats.red} ($redP%)"
            itemView.findViewById<TextView>(R.id.tvCountOrange).text = "${stats.orange} ($orangeP%)"
            itemView.findViewById<TextView>(R.id.tvCountGreen).text = "${stats.green} ($greenP%)"

            layoutContainer.addView(itemView)
        }

        // --- Update Total Progress Card ---
        val totalPercent = if (totalQ > 0) (totalG * 100 / totalQ) else 0
        findViewById<TextView>(R.id.tvTotalPercent).text = "$totalPercent%"
        findViewById<CircularProgressIndicator>(R.id.pbTotalCircular).progress = totalPercent
        findViewById<TextView>(R.id.tvTotalQuestionsInfo).text = "Du hast $totalG von $totalQ Fragen korrekt beantwortet."

        // Total Expandable Logic
        val layoutTotalExpandable = findViewById<LinearLayout>(R.id.layoutTotalExpandable)
        val ivExpandArrow = findViewById<ImageView>(R.id.ivExpandArrow)
        findViewById<View>(R.id.cardTotalProgress).setOnClickListener {
            val isVisible = layoutTotalExpandable.visibility == View.VISIBLE
            layoutTotalExpandable.visibility = if (isVisible) View.GONE else View.VISIBLE
            ivExpandArrow.rotation = if (isVisible) 0f else 90f
        }

        // Fill Total Details
        val greenP = if (totalQ > 0) (totalG * 100 / totalQ) else 0
        val orangeP = if (totalQ > 0) (totalO * 100 / totalQ) else 0
        val redP = if (totalQ > 0) (totalR * 100 / totalQ) else 0
        val unansweredP = if (totalQ > 0) (totalU * 100 / totalQ) else 0

        findViewById<TextView>(R.id.tvTotalDetailGreen).text = "Gut: $totalG ($greenP%)"
        findViewById<TextView>(R.id.tvTotalDetailOrange).text = "Mittel: $totalO ($orangeP%)"
        findViewById<TextView>(R.id.tvTotalDetailRed).text = "Schlecht: $totalR ($redP%)"
        findViewById<TextView>(R.id.tvTotalDetailUnanswered).text = "Offen: $totalU ($unansweredP%)"
    }

    data class ChapterStats(
        var total: Int = 0,
        var green: Int = 0,
        var orange: Int = 0,
        var red: Int = 0,
        var unanswered: Int = 0
    )
}
