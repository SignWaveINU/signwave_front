package com.example.androidlab

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavoritesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var homeButton: ImageButton
    private lateinit var starButton: ImageButton
    private lateinit var historyButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var micButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        initializeViews()
        setupRecyclerView()
        setupBottomNavigation()
        
        // 임시로 빈 화면 표시
        showEmptyView(true)
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.favoritesRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        homeButton = findViewById(R.id.homeButton)
        starButton = findViewById(R.id.starButton)
        historyButton = findViewById(R.id.historyButton)
        settingsButton = findViewById(R.id.settingsButton)
        micButton = findViewById(R.id.micButton)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        // TODO: Adapter 구현 및 설정
    }

    private fun setupBottomNavigation() {
        homeButton.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        starButton.setOnClickListener {
            // 현재 화면이므로 아무 동작 하지 않음
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

    private fun showEmptyView(show: Boolean) {
        emptyView.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
} 