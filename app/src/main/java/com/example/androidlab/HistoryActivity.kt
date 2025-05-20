package com.example.androidlab

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import api.FavoriteRequest
import api.RetrofitClient
import api.TranslationHistory
import kotlinx.coroutines.launch
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var textToSpeech: TextToSpeech
    private var translationHistoryList: List<TranslationHistory> = emptyList() // 클래스 레벨로 이동

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
    
        // SharedPreferences에서 아이콘 상태 복원
        val isIconPressed = sharedPreferences.getBoolean("isIconPressed", false)
        imageButton.setImageResource(if (isIconPressed) R.drawable.ic_on_record else R.drawable.ic_record)

        // 첫 번째 버튼 클릭 시 아이콘을 변경하고 즐겨찾기에 추가하는 리스너 추가
        imageButton.setOnClickListener {
            val currentIcon = imageButton.drawable
            val isCurrentlyPressed = currentIcon.constantState == resources.getDrawable(R.drawable.ic_record).constantState
            val newIcon = if (isCurrentlyPressed) {
                R.drawable.ic_on_record
            } else {
                R.drawable.ic_record
            }
            imageButton.setImageResource(newIcon)
    
            // 아이콘 상태를 SharedPreferences에 저장
            lifecycleScope.launch {
                try {
                    if (isCurrentlyPressed) {
                        if (translationHistoryList.isNotEmpty()) {
                            // API 호출로 즐겨찾기 추가
                            val response = RetrofitClient.favoriteApi.addFavorite(
                                "Bearer ${sharedPreferences.getString("token", "")}",
                                FavoriteRequest(translationHistoryList[0].translationHistoryId)
                            )
                            
                            // 성공적으로 즐겨찾기 추가됨
                            with(sharedPreferences.edit()) {
                                putBoolean("isIconPressed", isCurrentlyPressed)
                                putString("favorite_text", translatedText)
                                apply()
                            }
                            
                            runOnUiThread {
                                Toast.makeText(this@HistoryActivity, "즐겨찾기에 추가되었습니다", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@HistoryActivity, "기록을 먼저 불러와주세요", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HistoryActivity", "즐겨찾기 추가 실패: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(this@HistoryActivity, "즐겨찾기 추가에 실패했습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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
        // 현재 시간 표시 제거
        // updateCurrentTime() // 이 줄 제거
    }

    private fun updateCurrentTime() {
        val timeFormat = java.text.SimpleDateFormat("yyyy년 M월 d일 H:mm", Locale.KOREA)
        val currentTime = timeFormat.format(java.util.Date())

        val timeTextView1 = findViewById<TextView>(R.id.timeTextView1)

        timeTextView1.text = currentTime
    }

    private fun updateHistoryItems(histories: List<TranslationHistory>) {
        try {
            val favoritesLayout = findViewById<LinearLayout>(R.id.favoritesLayout)
            favoritesLayout.removeAllViews() // 기존 뷰 모두 제거
        
            for (history in histories) {
                // 새로운 레이아웃 인플레이트
                val itemView = layoutInflater.inflate(R.layout.history_item, favoritesLayout, false)
    
                // 텍스트 설정
                val textView = itemView.findViewById<TextView>(R.id.translatedTextView)
                textView.text = history.translatedText
    
                // 시간 설정 - 날짜 형식 변경
                val timeTextView = itemView.findViewById<TextView>(R.id.timeTextView1)
                val createdTime = history.createdTime.substring(0, 19) // "2025-05-19T09:01:30" 형식으로 자르기
                    .replace('T', ' ') // T를 공백으로 변경
                timeTextView.text = createdTime
    
                // 즐겨찾기 버튼 상태 설정
                val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton1)
                imageButton.setImageResource(if (history.favorite) R.drawable.ic_on_record else R.drawable.ic_record)
    
                // 재생 버튼 클릭 리스너 설정
                val playButton = itemView.findViewById<ImageButton>(R.id.playButton)
                playButton.setOnClickListener {
                    textToSpeech.speak(history.translatedText, TextToSpeech.QUEUE_FLUSH, null, null)
                }

                // 삭제 버튼 클릭 리스너 설정
                val deleteButton = itemView.findViewById<ImageButton>(R.id.deleteButton)
                deleteButton.setOnClickListener {
                    lifecycleScope.launch {
                        try {
                            val token = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getString("token", "") ?: ""
                            RetrofitClient.historyApi.deleteHistory("Bearer $token", history.translationHistoryId)
                            favoritesLayout.removeView(itemView)
                            Log.d("HistoryActivity", "번역 기록 삭제 성공")
                        } catch (e: Exception) {
                            Log.e("HistoryActivity", "번역 기록 삭제 중 오류 발생: ${e.message}")
                        }
                    }
                }
    
                // 즐겨찾기 버튼 클릭 리스너 설정
                imageButton.setOnClickListener {
                    lifecycleScope.launch {
                        try {
                            val token = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getString("token", "") ?: ""
                            val currentIcon = imageButton.drawable
                            val isCurrentlyPressed = currentIcon.constantState == resources.getDrawable(R.drawable.ic_on_record).constantState
                            
                            if (isCurrentlyPressed) {
                                // 즐겨찾기 해제
                                RetrofitClient.favoriteApi.removeFavorite(
                                    "Bearer $token",
                                    history.translationHistoryId
                                )
                                runOnUiThread {
                                    imageButton.setImageResource(R.drawable.ic_record)
                                    Toast.makeText(this@HistoryActivity, "즐겨찾기가 해제되었습니다", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // 즐겨찾기 추가
                                RetrofitClient.favoriteApi.addFavorite(
                                    "Bearer $token",
                                    FavoriteRequest(history.translationHistoryId)
                                )
                                runOnUiThread {
                                    imageButton.setImageResource(R.drawable.ic_on_record)
                                    Toast.makeText(this@HistoryActivity, "즐겨찾기에 추가되었습니다", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HistoryActivity", "즐겨찾기 상태 변경 실패: ${e.message}")
                            runOnUiThread {
                                Toast.makeText(this@HistoryActivity, "즐겨찾기 상태 변경에 실패했습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
        
                favoritesLayout.addView(itemView)
            }
        } catch (e: Exception) {
            Log.e("HistoryActivity", "UI 업데이트 중 오류 발생: ${e.message}")
            e.printStackTrace()
        }
    }

    // fetchTranslationHistory 함수 수정
    private fun fetchTranslationHistory() {
        lifecycleScope.launch {
            try {
                Log.d("HistoryActivity", "API 호출 시작")
                
                val token = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getString("token", "") ?: ""
                translationHistoryList = RetrofitClient.historyApi.getTranslationHistory("Bearer $token")
                Log.d("HistoryActivity", "받은 데이터: $translationHistoryList")

                if (translationHistoryList.isNotEmpty()) {
                    runOnUiThread {
                        updateHistoryItems(translationHistoryList) // 새로운 함수 호출
                    }
                } else {
                    Log.d("HistoryActivity", "API 호출 성공했지만 데이터가 비어있음")
                    runOnUiThread {
                        Toast.makeText(this@HistoryActivity, "기록이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("HistoryActivity", "API 호출 실패: ${e.message}")
                Log.e("HistoryActivity", "상세 에러:", e)
                
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