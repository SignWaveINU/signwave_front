package com.example.androidlab

import ReservationRequest
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import api.RetrofitClient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        reservationText.text = ""  // TextView 초기화

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
            
            // API 요청을 위한 날짜 형식 변환
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            
            val reservationDate = dateFormat.format(calendar.time)
            val reservationTime = timeFormat.format(calendar.time)

            // API 호출
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = ReservationRequest(
                        hospitalName = hospital,
                        reservationDate = reservationDate,
                        reservationTime = reservationTime
                    )

                    val response = RetrofitClient.reservationApi.createReservation("", request)
                    
                    if (response.isSuccessful) {
                        val reservationResponse = response.body()
                        // UI 업데이트
                        runOnUiThread {
                            val timeString = String.format("%02d:%02d", hour, minute)
                            val finalText = "$selectedDate $timeString ${reservationResponse?.hospitalName} 예약"
                            reservationText.text = finalText
                            Toast.makeText(this@CalendarActivity, "예약이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@CalendarActivity, "예약 생성에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@CalendarActivity, "오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            dialog.dismiss()
        }

        dialog.show()
    }
}