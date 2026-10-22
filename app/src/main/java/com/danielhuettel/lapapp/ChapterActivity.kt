package com.danielhuettel.lapapp

import android.content.Intent
import android.os.Bundle
import android.view.View

class ChapterActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapter)

        val mode = intent.getStringExtra("MODE") ?: "READ"

        val buttons = listOf(
            findViewById<View>(R.id.btnA) to "A",
            findViewById<View>(R.id.btnB) to "B",
            findViewById<View>(R.id.btnC) to "C",
            findViewById<View>(R.id.btnD) to "D",
            findViewById<View>(R.id.btnE) to "E",
            findViewById<View>(R.id.btnF) to "F"
        )

        buttons.forEach { (button, chapter) ->
            button?.setOnClickListener {
                val nextActivity = when (mode) {
                    "READ" -> DetailActivity::class.java
                    else -> QuizActivity::class.java
                }
                
                val intent = Intent(this, nextActivity)
                intent.putExtra("CHAPTER", chapter)
                intent.putExtra("QUIZ_MODE", if (mode == "LEARN") "LEARN" else "LAP_CHAPTER")
                startActivity(intent)
            }
        }
    }
}
