package com.example.androidlab

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val homeButton = findViewById<ImageButton>(R.id.homeButton)
        val starButton = findViewById<ImageButton>(R.id.starButton)
        val historyButton = findViewById<ImageButton>(R.id.historyButton)
        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)
        val micButton = findViewById<ImageButton>(R.id.micButton)

        homeButton.setOnClickListener {
            // 현재 화면이므로 아무 동작 하지 않음
        }

        starButton.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
            finish()
        }

        historyButton.setOnClickListener {
            // TODO: 기록 화면으로 이동
        }

        settingsButton.setOnClickListener {
            // TODO: 설정 화면으로 이동
        }

        micButton.setOnClickListener {
            // TODO: 음성 인식 시작
        }
    }
} 