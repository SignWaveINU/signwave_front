package com.example.androidlab

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class FavoritesActivity : AppCompatActivity() {
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var favoritesLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        // TextToSpeech 초기화
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.KOREAN)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "언어 지원되지 않음")
                }
            } else {
                Log.e("TTS", "초기화 실패")
            }
        }

        // 홈 버튼 클릭 리스너 추가
        val homeButton: ImageButton = findViewById(R.id.homeButton)
        homeButton.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        // 기록 버튼 클릭 리스너 추가
        val historyButton: ImageButton = findViewById(R.id.historyButton)
        historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            finish()
        }

        // 캘린터 버튼 클릭 리스너 추가
        val calendarButton: ImageButton = findViewById(R.id.settingsButton)
        calendarButton.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
            finish()
        }

        // 추가 버튼 클릭 리스너 추가
        val addButton: ImageButton = findViewById(R.id.addButton)
        favoritesLayout = findViewById(R.id.favoritesLayout)

        addButton.setOnClickListener {
            val newItemText = "새로운 항목 텍스트" // 사용자가 입력한 텍스트로 변경 가능
            addNewItem(newItemText)
        }
    }

    private fun addNewItem(text: String) {
        // 새로운 항목 추가
        val newItemLayout = layoutInflater.inflate(R.layout.item_favorite, favoritesLayout, false)
        val textView: TextView = newItemLayout.findViewById(R.id.textView)
        val playButton: ImageButton = newItemLayout.findViewById(R.id.playButton)

        textView.text = text
        playButton.setOnClickListener {
            speakOut(text)
        }

        favoritesLayout.addView(newItemLayout)
    }

    private fun speakOut(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}