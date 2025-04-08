package com.example.androidlab

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {
    private var isStarred1 = false // 첫 번째 별 아이콘 상태를 추적하는 변수
    private var isStarred2 = false // 두 번째 별 아이콘 상태를 추적하는 변수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // 하단 네비게이션 버튼 설정
        val homeButton = findViewById<ImageButton>(R.id.homeButton)
        val starButton = findViewById<ImageButton>(R.id.starButton)
        val historyButton = findViewById<ImageButton>(R.id.historyButton)
        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)

        // 첫 번째 별 아이콘 버튼 클릭 리스너 설정
        val imageButton1 = findViewById<ImageButton>(R.id.imageButton)
        imageButton1.setOnClickListener {
            isStarred1 = !isStarred1 // 상태 토글
            if (isStarred1) {
                imageButton1.setImageResource(R.drawable.ic_on_star_his) // 별이 채워진 아이콘으로 변경
            } else {
                imageButton1.setImageResource(R.drawable.ic_star_his) // 별이 비워진 아이콘으로 변경
            }
        }

        // 두 번째 별 아이콘 버튼 클릭 리스너 설정
        val imageButton2 = findViewById<ImageButton>(R.id.imageButton2)
        imageButton2.setOnClickListener {
            isStarred2 = !isStarred2 // 상태 토글
            if (isStarred2) {
                imageButton2.setImageResource(R.drawable.ic_on_star_his) // 별이 채워진 아이콘으로 변경
            } else {
                imageButton2.setImageResource(R.drawable.ic_star_his) // 별이 비워진 아이콘으로 변경
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
    }
}