package com.yachihitoka.lapapp

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.github.chrisbanes.photoview.PhotoView

class FullScreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val photoView: PhotoView = findViewById(R.id.photoView)
        val btnClose: ImageButton = findViewById(R.id.btnClose)

        val resId = intent.getIntExtra("RES_ID", 0)
        if (resId != 0) {
            photoView.setImageResource(resId)
        } else {
            finish()
        }

        btnClose.setOnClickListener {
            finish()
        }
    }
}
