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
import androidx.lifecycle.lifecycleScope
import api.FavoriteRequest
import api.RetrofitClient
import kotlinx.coroutines.launch

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
        val deleteButton: ImageButton = newItemLayout.findViewById(R.id.deleteButton)

        // 고유한 ID 생성
        val translationHistoryId = System.currentTimeMillis().toInt() // 현재 시간을 ID로 사용
        val memberId = 0 // 기본값으로 0을 사용 (필요에 따라 수정 가능)
        
        textView.text = text
        textView.isEnabled = false
        playButton.setOnClickListener {
            speakOut(text)
        }

        // 삭제 버튼 클릭 시: 레이아웃에서 제거 + SharedPreferences에서 제거
        deleteButton.setOnClickListener {
            favoritesLayout.removeView(newItemLayout) // UI에서 제거
            removeFavorite(translationHistoryId.toLong()) // SharedPreferences에서 제거
        }

        favoritesLayout.addView(newItemLayout)

        // SharedPreferences에 텍스트와 ID 저장
        saveFavorite(text, translationHistoryId.toLong(), memberId)

        // API 호출
        sendApiRequest(translationHistoryId)
    }

    private fun sendApiRequest(translationHistoryId: Int) {
        val apiRequest = FavoriteRequest(translationHistoryId)
        val token = "Bearer YOUR_ACCESS_TOKEN" // 실제 토큰으로 변경하세요.

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.favoriteApi.addFavorite(token, apiRequest) // token과 request를 전달
                // 응답 처리
                val memberId = response.memberId
                // memberId를 사용하여 추가 작업 수행
            } catch (e: Exception) {
                Log.e("API Error", "Failed to add favorite: ${e.message}")
            }
        }
    }

    private fun saveFavorite(text: String, translationHistoryId: Long, memberId: Int) {
        val editor = sharedPreferences.edit()
        val favorites = sharedPreferences.getStringSet("favorites", mutableSetOf()) ?: mutableSetOf()
        favorites.add("$translationHistoryId:$memberId:$text") // ID, memberId, 텍스트를 함께 저장
        editor.putStringSet("favorites", favorites)
        editor.apply()
    }

    private fun restoreFavorites() {
        val favorites = sharedPreferences.getStringSet("favorites", mutableSetOf()) ?: mutableSetOf()
        for (favorite in favorites) {
            val (id, memberId, text) = favorite.split(":", limit = 3)
            addNewItem(text) // 텍스트만 추가
        }
    }

    private fun speakOut(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun removeFavorite(translationHistoryId: Long) {
        val favorites = sharedPreferences.getStringSet("favorites", mutableSetOf())?.toMutableSet() ?: return
        favorites.removeIf { it.startsWith("$translationHistoryId:") }
        sharedPreferences.edit().putStringSet("favorites", favorites).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}