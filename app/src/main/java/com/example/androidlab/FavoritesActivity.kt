package com.example.androidlab

import android.widget.EditText
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.speech.tts.TextToSpeech
import java.util.Locale

class FavoritesActivity : AppCompatActivity() {
    private lateinit var favoritesLayout: LinearLayout
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        // favoritesLayout 초기화를 가장 먼저 수행
        favoritesLayout = findViewById(R.id.favoritesLayout)

        // TextToSpeech 초기화
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.KOREAN)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "언어 지원되지 않음")
                } else {
                    Log.d("TTS", "TextToSpeech 초기화 성공")
                }
            } else {
                Log.e("TTS", "초기화 실패")
            }
        }

        // SharedPreferences에서 저장된 텍스트 확인
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        sharedPreferences.getString("favorite_text", null)?.let { text ->
            addNewItem(text)
            // 사용 후 삭제
            sharedPreferences.edit().remove("favorite_text").apply()
        }

        // 전달받은 텍스트가 있다면 즐겨찾기에 추가
        intent.getStringExtra("favorite_text")?.let { text ->
            addNewItem(text)
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

        // 캘린더 버튼 클릭 리스너 추가
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
    }

    private fun addNewItem(text: String) {
        // 새로운 항목 추가
        val newItemLayout = layoutInflater.inflate(R.layout.item_favorite, favoritesLayout, false)
        val textView: TextView = newItemLayout.findViewById(R.id.editText)
        val playButton: ImageButton = newItemLayout.findViewById(R.id.playButton)
        val deleteButton: ImageButton = newItemLayout.findViewById(R.id.deleteButton)

        textView.text = text
        textView.isEnabled = false

        // 재생 버튼 클릭 리스너 추가
        playButton.setOnClickListener {
            speakOut(text) // 텍스트를 음성으로 읽기
        }

        // 삭제 버튼 클릭 시: 레이아웃에서 제거
        deleteButton.setOnClickListener {
            Log.d("FavoritesActivity", "삭제 버튼 클릭됨")
            favoritesLayout.removeView(newItemLayout) // UI에서 제거
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