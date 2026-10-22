package com.yachihitoka.lapapp

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class QuestionAdapter(private var questions: List<Question>) :
    RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    private var originalQuestions: List<Question> = questions

    class QuestionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.questionCard)
        val tvId: TextView = view.findViewById(R.id.tvQuestionId)
        val containerImages: LinearLayout = view.findViewById(R.id.containerImages)
        val tvQuestion: TextView = view.findViewById(R.id.tvQuestionText)
        val tvAnswer: TextView = view.findViewById(R.id.tvAnswerText)
        val divider: View = view.findViewById(R.id.divider)
    }

    fun updateList(newList: List<Question>) {
        this.questions = newList
        this.originalQuestions = newList
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        questions = if (query.isEmpty()) {
            originalQuestions
        } else {
            originalQuestions.filter { 
                it.id.contains(query, ignoreCase = true) || it.frage.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = questions[position]
        val context = holder.itemView.context
        
        holder.tvId.text = question.id
        holder.tvQuestion.text = question.frage
        holder.tvAnswer.text = question.antwort

        holder.tvAnswer.visibility = View.VISIBLE
        holder.divider.visibility = View.VISIBLE

        // Bilder nur beim ersten Element (position 0) anzeigen, damit sie nicht doppelt erscheinen
        // In diesem Adapter (Lesemodus) sind Fragen oft einzeln, aber wir prüfen trotzdem die Position.
        if (position == 0 || (position > 0 && questions[position-1].id != question.id)) {
            AssetProvider.loadImagesForQuestion(context, question, holder.containerImages, horizontal = false)
            holder.containerImages.visibility = if (holder.containerImages.childCount > 0) View.VISIBLE else View.GONE
        } else {
            holder.containerImages.removeAllViews()
            holder.containerImages.visibility = View.GONE
        }

        val prefs = context.getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        val status = prefs.getInt("status_${question.id}", 0)

        val color = when (status) {
            1 -> Color.parseColor("#33F44336")
            2 -> Color.parseColor("#33FF9800")
            3 -> Color.parseColor("#334CAF50")
            else -> Color.parseColor("#1E1E1E")
        }
        holder.card.setCardBackgroundColor(color)
        
        if (status > 0) {
            holder.card.strokeWidth = 4
            holder.card.strokeColor = when (status) {
                1 -> Color.parseColor("#F44336")
                2 -> Color.parseColor("#FF9800")
                3 -> Color.parseColor("#4CAF50")
                else -> Color.TRANSPARENT
            }
        } else {
            holder.card.strokeWidth = 0
        }
    }

    override fun getItemCount() = questions.size
}
