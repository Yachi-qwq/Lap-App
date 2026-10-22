package com.danielhuettel.lapapp

import android.content.Context
import android.graphics.Color
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class LapQuestionAdapter(
    private var questions: List<Question>,
    private var showAnswers: Boolean,
    private val showImagesInItem: Boolean = true,
    private val onStatusChanged: ((String, Int) -> Unit)? = null
) : RecyclerView.Adapter<LapQuestionAdapter.LapViewHolder>() {

    class LapViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.questionCard)
        val tvId: TextView = view.findViewById(R.id.tvQuestionId)
        val containerImages: LinearLayout = view.findViewById(R.id.containerImages)
        val tvQuestion: TextView = view.findViewById(R.id.tvQuestionText)
        val tvAnswer: TextView = view.findViewById(R.id.tvAnswerText)
        val divider: View = view.findViewById(R.id.divider)
        
        val layoutRatings: View? = view.findViewById(R.id.layoutRatingsInItem)
        val btnBad: MaterialButton? = view.findViewById(R.id.btnBadItem)
        val btnMedium: MaterialButton? = view.findViewById(R.id.btnMediumItem)
        val btnGood: MaterialButton? = view.findViewById(R.id.btnGoodItem)
    }

    fun updateList(newList: List<Question>) {
        this.questions = newList
        notifyDataSetChanged()
    }

    fun setShowAnswers(show: Boolean) {
        this.showAnswers = show
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LapViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question, parent, false)
        return LapViewHolder(view)
    }

    override fun onBindViewHolder(holder: LapViewHolder, position: Int) {
        val question = questions[position]
        val context = holder.itemView.context
        val prefs = context.getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        
        holder.tvId.text = question.id
        holder.tvQuestion.text = question.frage
        holder.tvAnswer.text = question.antwort
        
        // Bilder nur beim ersten Element (position 0) anzeigen
        if (showImagesInItem && position == 0) {
            AssetProvider.loadImagesForQuestion(context, question, holder.containerImages, horizontal = false)
            holder.containerImages.visibility = if (holder.containerImages.childCount > 0) View.VISIBLE else View.GONE
        } else {
            holder.containerImages.removeAllViews()
            holder.containerImages.visibility = View.GONE
        }
        
        // Antwort-Sichtbarkeit
        if (showAnswers) {
            holder.tvAnswer.visibility = View.VISIBLE
            holder.divider.visibility = View.VISIBLE
        } else {
            holder.tvAnswer.visibility = View.GONE
            holder.divider.visibility = View.GONE
        }

        val status = prefs.getInt("status_${question.id}", 0)
        updateItemHighlight(holder, status)

        holder.layoutRatings?.visibility = View.VISIBLE
        
        holder.btnBad?.setOnClickListener { 
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            saveStatus(context, question.id, 1)
        }
        holder.btnMedium?.setOnClickListener { 
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            saveStatus(context, question.id, 2)
        }
        holder.btnGood?.setOnClickListener { 
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            saveStatus(context, question.id, 3)
        }
    }

    private fun saveStatus(context: Context, id: String, status: Int) {
        context.getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE).edit()
            .putInt("status_$id", status).apply()
        
        // Firestore Sync
        FirebaseManager.saveQuestionStatus(id, status)

        onStatusChanged?.invoke(id, status)
        notifyDataSetChanged()
    }

    private fun updateItemHighlight(holder: LapViewHolder, status: Int) {
        when (status) {
            1 -> holder.card.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")))
            2 -> holder.card.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800")))
            3 -> holder.card.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")))
            else -> holder.card.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#1AFFFFFF")))
        }
        holder.card.strokeWidth = if (status > 0) 4 else 1
    }

    override fun getItemCount() = questions.size
}
