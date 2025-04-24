package com.example.androidlab

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // 하단 네비게이션 버튼 설정
        val homeButton = findViewById<ImageButton>(R.id.homeButton)
        val starButton = findViewById<ImageButton>(R.id.starButton)
        val historyButton = findViewById<ImageButton>(R.id.historyButton)
        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)


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
    }
}