package com.example.androidlab

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity

class CalendarActivity : AppCompatActivity() {
    private lateinit var calendarView: CalendarView
    private lateinit var reservationText: TextView
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // 뷰 초기화
        calendarView = findViewById(R.id.calendarView)
        reservationText = findViewById(R.id.reservationText)

        // 예약 정보 불러오기
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val savedReservation = sharedPref.getString("reservation", "")
        reservationText.text = savedReservation // 저장된 예약 텍스트를 설정

        // 네비게이션 버튼 설정
        val homeButton = findViewById<ImageButton>(R.id.homeButton)
        val starButton = findViewById<ImageButton>(R.id.starButton)
        val historyButton = findViewById<ImageButton>(R.id.historyButton)

        homeButton.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        starButton.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }

        historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // 캘린더 날짜 선택 리스너 설정
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "${month + 1}월 ${dayOfMonth}일"
            showTimePickerDialog()
        }
    }

    private fun showTimePickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_time_pick, null)
        val hospitalName = dialogView.findViewById<EditText>(R.id.hospitalName)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnConfirm.setOnClickListener {
            val hospital = hospitalName.text.toString()
            val hour = timePicker.hour
            val minute = timePicker.minute
            val timeString = String.format("%02d:%02d", hour, minute)

            val finalText = if (hospital.isNotEmpty()) {
                "$selectedDate $timeString $hospital 예약"
            } else {
                "$selectedDate $timeString 예약"
            }

            reservationText.text = finalText

            // ✅ 텍스트를 설정한 다음에 저장!
            val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            with (sharedPref.edit()) {
                putString("reservation", finalText)
                apply()
            }

            dialog.dismiss()
        }

        dialog.show()
    }
}