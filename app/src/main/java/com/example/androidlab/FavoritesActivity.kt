package com.example.androidlab

import android.widget.EditText
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.speech.tts.TextToSpeech
import java.util.Locale
import api.RetrofitClient
import api.TranslationHistory
import api.FavoriteRequest
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

data class FavoriteResponse(
    val success: Boolean,
    val message: String
)

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

        // 즐겨찾기 기록 조회
        fetchFavorites()

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
        addButton.setOnClickListener {
            Toast.makeText(this, "번역 기록에서 즐겨찾기를 추가해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchFavorites() {
        lifecycleScope.launch {
            try {
                val token = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getString("token", "") ?: ""
                val favorites = RetrofitClient.favoriteApi.getFavorites("Bearer $token")
                Log.d("FavoritesActivity", "즐겨찾기 조회 성공: ${favorites.size}개의 기록")
                
                favoritesLayout.removeAllViews()
                
                for (favorite in favorites) {
                    addNewItem(favorite.translatedText, favorite.translationHistoryId)
                }
            } catch (e: Exception) {
                Log.e("FavoritesActivity", "즐겨찾기 조회 실패: ${e.message}")
            }
        }
    }

    private fun addNewItem(text: String, historyId: Int) {
        val newItemLayout = layoutInflater.inflate(R.layout.item_favorite, favoritesLayout, false)
        val textView: TextView = newItemLayout.findViewById(R.id.editText)
        val playButton: ImageButton = newItemLayout.findViewById(R.id.playButton)
        val deleteButton: ImageButton = newItemLayout.findViewById(R.id.deleteButton)

        textView.text = text
        textView.isEnabled = false

        playButton.setOnClickListener {
            speakOut(text)
        }

        deleteButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val token = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getString("token", "") ?: ""
                    RetrofitClient.favoriteApi.removeFavorite("Bearer $token", historyId)
                    favoritesLayout.removeView(newItemLayout)
                    Log.d("FavoritesActivity", "즐겨찾기 삭제 성공")
                } catch (e: Exception) {
                    Log.e("FavoritesActivity", "즐겨찾기 삭제 중 오류 발생: ${e.message}")
                }
            }
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