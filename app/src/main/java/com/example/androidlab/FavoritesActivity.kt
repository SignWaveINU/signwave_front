package com.example.androidlab
import android.widget.EditText // 이 줄을 추가하세요

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import android.content.SharedPreferences

class FavoritesActivity : AppCompatActivity() {
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var favoritesLayout: LinearLayout
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("FavoritesPrefs", MODE_PRIVATE)

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
            // 다이얼로그 생성
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_input, null)
            builder.setView(dialogView)

            val inputText = dialogView.findViewById<EditText>(R.id.inputText)
            val submitButton = dialogView.findViewById<Button>(R.id.submitButton)

            val dialog = builder.create()

            submitButton.setOnClickListener {
                val userInput = inputText.text.toString()
                // 사용자 입력 처리
                addNewItem(userInput)
                dialog.dismiss()
            }

            dialog.show()
        }

        // 저장된 텍스트 복원
        restoreFavorites()
    }

    private fun addNewItem(text: String) {
        // 새로운 항목 추가
        val newItemLayout = layoutInflater.inflate(R.layout.item_favorite, favoritesLayout, false)
        val textView: TextView = newItemLayout.findViewById(R.id.editText)
        val playButton: ImageButton = newItemLayout.findViewById(R.id.playButton)

        textView.text = text
        textView.isEnabled = false
        playButton.setOnClickListener {
            speakOut(text)
        }

        favoritesLayout.addView(newItemLayout)

        // SharedPreferences에 텍스트 저장
        saveFavorite(text)
    }

    private fun saveFavorite(text: String) {
        val editor = sharedPreferences.edit()
        val favorites = sharedPreferences.getStringSet("favorites", mutableSetOf()) ?: mutableSetOf()
        favorites.add(text)
        editor.putStringSet("favorites", favorites)
        editor.apply()
    }

    private fun restoreFavorites() {
        val favorites = sharedPreferences.getStringSet("favorites", mutableSetOf()) ?: mutableSetOf()
        for (text in favorites) {
            addNewItem(text)
        }
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