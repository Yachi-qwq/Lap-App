package com.yachihitoka.lapapp

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.charset.StandardCharsets

object AssetProvider {

    /**
     * Zentrale Hilfsfunktion zum Laden von Bildern aus Drawables in einen Container.
     * Unterstützt Click-to-Zoom und verschiedene Orientierungen.
     */
    fun loadImagesForQuestion(
        context: Context, 
        question: Question, 
        container: LinearLayout, 
        horizontal: Boolean = false,
        clickToZoom: Boolean = true
    ) {
        container.removeAllViews()
        // Für vertikale Scroll-Listen ist VERTICAL oft besser zur Zentrierung
        container.orientation = if (horizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        container.gravity = Gravity.CENTER_HORIZONTAL
        var anyImageFound = false

        val namesToTry = mutableListOf<String>()
        if (question.bilder.isNotEmpty()) {
            namesToTry.addAll(question.bilder)
        } else {
            // Standard-Schema: elektrik_h3_[id]
            val defaultId = question.id.lowercase().trim().replace(" ", "_")
            namesToTry.add("elektrik_h3_$defaultId")
        }

        namesToTry.forEach { name ->
            val cleanName = name.lowercase().trim()
                .removeSuffix(".png").removeSuffix(".jpg").removeSuffix(".jpeg")
            
            val resId = context.resources.getIdentifier(cleanName, "drawable", context.packageName)
            Log.d("IMAGE_DEBUG", "Versuche Bild zu laden: $cleanName")
            
            if (resId != 0) {
                anyImageFound = true
                val imageView = ImageView(context).apply {
                    // Breite auf wrap_content für korrekte Zentrierung
                    val width = LinearLayout.LayoutParams.WRAP_CONTENT
                    val height = if (horizontal) 200.dpToPx(context) else LinearLayout.LayoutParams.WRAP_CONTENT
                    
                    layoutParams = LinearLayout.LayoutParams(width, height).apply { 
                        gravity = Gravity.CENTER_HORIZONTAL
                        setMargins(0, 0, 0, 16.dpToPx(context))
                    }
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageResource(resId)
                    
                    if (clickToZoom) {
                        setOnClickListener {
                            val intent = Intent(context, FullScreenImageActivity::class.java)
                            intent.putExtra("RES_ID", resId)
                            context.startActivity(intent)
                        }
                    }
                }
                container.addView(imageView)
                Log.d("IMAGE_DEBUG", "Erfolgreich geladen: $cleanName")
            } else {
                Log.d("IMAGE_DEBUG", "Nicht gefunden: $cleanName")
            }
        }

        container.visibility = if (anyImageFound) View.VISIBLE else View.GONE
    }

    private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

    fun getQuestionsPath(): String = "berufe/anlagen_und_betriebstechnik_H3/fragen/fragen_elektrik_H3.json"

    fun loadQuestions(context: Context): List<Question> {
        return try {
            val jsonString = context.assets.open(getQuestionsPath()).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            Gson().fromJson(jsonString, object : TypeToken<List<Question>>() {}.type)
        } catch (e: Exception) {
            Log.e("JSON_LOAD", "Fehler: ${e.message}")
            emptyList()
        }
    }

    fun loadJSONFromAsset(context: Context, fileName: String): String? {
        return try {
            context.assets.open(fileName.removePrefix("assets/").removePrefix("/")).bufferedReader().use { it.readText() }
        } catch (e: Exception) { null }
    }

    fun listAssets(context: Context, path: String = "") {
        try {
            val assets = context.assets.list(path) ?: return
            for (asset in assets) {
                val fullPath = if (path.isEmpty()) asset else "$path/$asset"
                Log.d("ASSETS_CHECK", "Asset: $fullPath")
                listAssets(context, fullPath)
            }
        } catch (e: Exception) {
            Log.e("ASSETS_CHECK", "Fehler beim Auflisten von Assets ($path): ${e.message}")
        }
    }
}
