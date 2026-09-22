package com.mediaxplayer.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#101010"))
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        val title = TextView(this).apply {
            text = "Media X Player"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
        }

        val hint = TextView(this).apply {
            text = "वीडियो का लिंक नीचे डालो — MP4, MKV, WebM, HLS (.m3u8), DASH (.mpd)"
            setTextColor(Color.parseColor("#B0B0B0"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(20))
        }

        val input = EditText(this).apply {
            hint = "https://..."
            setHintTextColor(Color.parseColor("#808080"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(Color.parseColor("#222222"))
        }

        val playBtn = Button(this).apply {
            text = "▶  प्ले करो"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#E8B04A"))
            setTextColor(Color.parseColor("#1A1A1A"))
        }

        playBtn.setOnClickListener {
            val url = input.text.toString().trim()
            if (url.isEmpty() || !url.startsWith("http")) {
                Toast.makeText(this, "सही लिंक डालो (http से शुरू)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val i = Intent(this, PlayerActivity::class.java)
            i.putExtra("url", url)
            startActivity(i)
        }

        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(14)

        root.addView(title)
        root.addView(hint)
        root.addView(input, lp)
        root.addView(playBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(18) })

        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val i = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(dp(24) + i.left, dp(24) + i.top, dp(24) + i.right, dp(24) + i.bottom)
            insets
        }
    }
}
