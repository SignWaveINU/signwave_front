package com.example.androidlab

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import api.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var textToSpeech: TextToSpeech

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // SharedPreferences에서 데이터 받기
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val translatedText = sharedPreferences.getString("translatedText", "번역된 내용이 없습니다.")

        // 텍스트 뷰 업데이트
        val textView: TextView = findViewById(R.id.translatedTextView)
        textView.text = translatedText

        // TextToSpeech 초기화
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.KOREAN)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // 언어 지원되지 않음
                }
            }
        }

        val imageButton = findViewById<ImageButton>(R.id.imageButton1)

        // 첫 번째 버튼 클릭 시 아이콘을 변경하고 즐겨찾기에 추가하는 리스너 추가
        imageButton.setOnClickListener {
            val currentIcon = imageButton.drawable
            val newIcon = if (currentIcon.constantState == resources.getDrawable(R.drawable.ic_record).constantState) {
                R.drawable.ic_on_record // 아이콘을 변경
            } else {
                R.drawable.ic_record // 아이콘을 다시 변경
            }
            imageButton.setImageResource(newIcon) // 아이콘 변경

            // translatedTextView의 텍스트를 가져와서 SharedPreferences에 저장
            val translatedText = findViewById<TextView>(R.id.translatedTextView).text.toString()
            val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString("favorite_text", translatedText)
                apply()
            }
            Toast.makeText(this, "즐겨찾기에 추가되었습니다", Toast.LENGTH_SHORT).show()
        }


        // 하단 네비게이션 버튼 설정
        val homeButton = findViewById<ImageButton>(R.id.homeButton)
        val starButton = findViewById<ImageButton>(R.id.starButton)
        val historyButton = findViewById<ImageButton>(R.id.historyButton)
        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)
        val playButton = findViewById<ImageButton>(R.id.playButton)
        val translatedTextView = findViewById<TextView>(R.id.translatedTextView)

        // 첫 번째 재생 버튼 클릭 리스너 설정
        playButton.setOnClickListener {
            val textToRead = translatedTextView.text.toString()
            if (textToRead.isNotEmpty()) {
                textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }


        // 하단 네비게이션 버튼 클릭 리스너 설정
        homeButton.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        starButton.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }

        historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // 캘린더 버튼 클릭 리스너 설정
        settingsButton.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        // API 호출
        fetchTranslationHistory()
        // 현재 시간 표시
        updateCurrentTime()
    }

    private fun updateCurrentTime() {
        val timeFormat = java.text.SimpleDateFormat("yyyy년 M월 d일 H:mm", Locale.KOREA)
        val currentTime = timeFormat.format(java.util.Date())

        val timeTextView1 = findViewById<TextView>(R.id.timeTextView1)

        timeTextView1.text = currentTime
    }

    // API 호출 함수
    private fun fetchTranslationHistory() {
        lifecycleScope.launch {
            try {
                // API 호출 시도 로그
                Log.d("HistoryActivity", "API 호출 시작")
                
                val translationHistoryList = RetrofitClient.historyApi.getTranslationHistory()
                Log.d("HistoryActivity", "받은 데이터: $translationHistoryList")

                if (translationHistoryList.isNotEmpty()) {
                    val translatedText = translationHistoryList[0].translatedText
                    Log.d("HistoryActivity", "설정할 텍스트: $translatedText")
                    
                    runOnUiThread {
                        val textView: TextView = findViewById(R.id.translatedTextView)
                        textView.text = translatedText
                        Log.d("HistoryActivity", "텍스트 설정 완료")
                    }
                } else {
                    Log.d("HistoryActivity", "API 호출 성공했지만 데이터가 비어있음")
                    runOnUiThread {
                        Toast.makeText(this@HistoryActivity, "기록이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("HistoryActivity", "API 호출 실패: ${e.message}")
                Log.e("HistoryActivity", "상세 에러:", e)  // 스택 트레이스 출력
                
                runOnUiThread {
                    Toast.makeText(this@HistoryActivity, "데이터를 불러오지 못했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.shutdown() // TextToSpeech 종료
    }
}
