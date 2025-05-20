package com.example.androidlab

import ReservationRequest
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import api.RetrofitClient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarActivity : AppCompatActivity() {
    private lateinit var calendarView: CalendarView
    private lateinit var reservationText: TextView
    private lateinit var deleteButton: ImageButton
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // 뷰 초기화
        calendarView = findViewById(R.id.calendarView)
        reservationText = findViewById(R.id.reservationText)
        deleteButton = findViewById(R.id.deleteButton)

        // 저장된 예약 정보 불러오기
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val savedReservation = sharedPreferences.getString("reservation", "")
        reservationText.text = savedReservation
        deleteButton.visibility = if (savedReservation.isNullOrEmpty()) View.GONE else View.VISIBLE

        // 삭제 버튼 설정
        deleteButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            val reservationId = sharedPreferences.getInt("reservationId", -1)
            
            if (reservationId != -1) {
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val token = sharedPreferences.getString("token", "") ?: ""
                            val response = RetrofitClient.reservationApi.deleteReservation("Bearer $token", reservationId)
                            
                            if (response.isSuccessful) {
                                withContext(Dispatchers.Main) {
                                    reservationText.text = ""
                                    deleteButton.visibility = View.GONE
                                    // 저장된 예약 정보와 ID 삭제
                                    sharedPreferences.edit()
                                        .remove("reservation")
                                        .remove("reservationId")
                                        .apply()
                                }
                                android.util.Log.d("CalendarActivity", "예약이 성공적으로 삭제되었습니다")
                            } else {
                                val errorMessage = response.errorBody()?.string() ?: "예약 삭제에 실패했습니다"
                                android.util.Log.e("CalendarActivity", "예약 삭제 실패: $errorMessage")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CalendarActivity", "예약 삭제 중 오류 발생: ${e.message}")
                    }
                }
            }
        }

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

            // 선택된 날짜 사용
            val calendar = Calendar.getInstance()
            val selectedDateParts = selectedDate.split("월 ", "일")
            if (selectedDateParts.size >= 2) {
                val month = selectedDateParts[0].toInt() - 1  // 월은 0부터 시작
                val day = selectedDateParts[1].toInt()
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
            }
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

            val reservationDate = dateFormat.format(calendar.time)
            val reservationTime = timeFormat.format(calendar.time)

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val token = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getString("token", "") ?: ""
                        val request = ReservationRequest(
                            hospitalName = hospital,
                            reservationDate = reservationDate,
                            reservationTime = reservationTime
                        )
                        val response = RetrofitClient.reservationApi.createReservation("Bearer $token", request)

                        if (response.isSuccessful) {
                            val reservationResponse = response.body()
                            withContext(Dispatchers.Main) {
                                // API 응답으로 텍스트뷰 업데이트
                                val timeString = String.format("%02d:%02d", hour, minute)
                                val finalText = "${reservationResponse?.reservationDate} $timeString ${reservationResponse?.hospitalName} 예약"
                                reservationText.text = finalText
                                deleteButton.visibility = View.VISIBLE
                                
                                // 예약 정보와 ID 저장
                                getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("reservation", finalText)
                                    .putInt("reservationId", reservationResponse?.reservationId ?: -1)
                                    .apply()
                            }
                            android.util.Log.d("CalendarActivity", "예약이 성공적으로 생성되었습니다")
                        } else {
                            val errorMessage = response.errorBody()?.string() ?: "예약 생성에 실패했습니다"
                            android.util.Log.e("CalendarActivity", "예약 생성 실패: $errorMessage")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CalendarActivity", "예약 생성 중 오류 발생: ${e.message}")
                }
            }

            dialog.dismiss()
        }


        dialog.show()
    }
}