package com.example.androidlab

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import api.RetrofitClient
import api.TTSRequest
import com.google.mediapipe.solutioncore.CameraInput
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsOptions
import com.google.mediapipe.solutions.hands.HandsResult
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody


class HomeActivity : AppCompatActivity(){
    private lateinit var hands: Hands
    private lateinit var cameraInput: CameraInput
    private lateinit var glSurfaceView: SolutionGlSurfaceView<HandsResult>
    private lateinit var handsResultGlRenderer: HandsResultGlRenderer
    private lateinit var token: String
    private lateinit var translationText: TextView
    private lateinit var textToSpeech: TextToSpeech

    // API 호출 제한을 위한 변수들
    private var lastApiCallTime = 0L
    private val apiCallInterval = 2000L // 2초 간격으로 API 호출 제한
    private var isProcessing = false

    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.INTERNET,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_NETWORK_STATE
    ).toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // SharedPreferences에서 토큰 가져오기
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        token = sharedPreferences.getString("token", "") ?: ""

        // TextView 초기화
        translationText = findViewById(R.id.translationText)

        // TextToSpeech 초기화
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.KOREAN)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "언어 지원되지 않음")
                }
            } else {
                Log.e("TTS", "TextToSpeech 초기화 실패")
            }
        }

        // 음성 버튼 클릭 리스너 추가
        val voiceButton: ImageButton = findViewById(R.id.voiceButton)
        voiceButton.setOnClickListener {
            val textToRead = translationText.text.toString()
            if (textToRead.isNotEmpty()) {
                textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                Toast.makeText(this, "읽을 텍스트가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 권한 요청 및 초기화
        requestPermissions()

        // 즐겨찾기 버튼 클릭 리스너 추가
        val starButton: ImageButton = findViewById(R.id.starButton)
        starButton.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }

        // 기록 버튼 클릭 리스너 추가
        val historyButton: ImageButton = findViewById(R.id.historyButton)
        historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // 캘린터 버튼 클릭 리스너 추가
        val calendarButton: ImageButton = findViewById(R.id.settingsButton)
        calendarButton.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
    }

    private fun requestPermissions() {
        val permissionListener = object : PermissionListener {
            override fun onPermissionGranted(response: PermissionGrantedResponse) {
                setupStreamingModePipeline()
                glSurfaceView.post { startCamera() }
                glSurfaceView.visibility = View.VISIBLE
                initView()
            }

            override fun onPermissionDenied(response: PermissionDeniedResponse) {
                Toast.makeText(this@HomeActivity, "권한을 다시 설정해주세요!", Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionRationaleShouldBeShown(
                permission: PermissionRequest,
                token: PermissionToken
            ) {
                token.continuePermissionRequest()
            }
        }

        Dexter.withContext(this)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(permissionListener)
            .check()
    }

    private fun setupStreamingModePipeline() {
        hands = Hands(
            this@HomeActivity,
            HandsOptions.builder()
                .setStaticImageMode(false)
                .setMaxNumHands(2)
                .setRunOnGpu(true)
                .build()
        )
        hands.setErrorListener { message, e -> Log.e("TAG", "MediaPipe Hands error: $message") }

        cameraInput = CameraInput(this@HomeActivity)
        cameraInput.setNewFrameListener { hands.send(it) }

        // handsResultGlRenderer 초기화
        handsResultGlRenderer = HandsResultGlRenderer(this) { landmarkData, jsonFilePath ->
            // API 호출
            sendLandmarkDataToServer(landmarkData, jsonFilePath)
        }

        glSurfaceView = SolutionGlSurfaceView(this@HomeActivity, hands.glContext, hands.glMajorVersion)
        glSurfaceView.setSolutionResultRenderer(handsResultGlRenderer)
        glSurfaceView.setRenderInputImage(true)

        hands.setResultListener {
            glSurfaceView.setRenderData(it)
            glSurfaceView.requestRender()
        }

        glSurfaceView.post(this::startCamera)

        // activity_main.xml에 선언한 FrameLayout에 화면을 띄우는 코드
        findViewById<FrameLayout>(R.id.preview_display_layout).apply {
            removeAllViewsInLayout()
            addView(glSurfaceView)
            glSurfaceView.visibility = View.VISIBLE
            requestLayout()
        }
    }

    private fun startCamera() {
        cameraInput.start(
            this@HomeActivity,
            hands.glContext,
            CameraInput.CameraFacing.FRONT,
            glSurfaceView.width,
            glSurfaceView.height
        )
    }

    private fun initView() {
        // UI 초기화 코드 (필요한 경우 추가)
    }

    private fun sendLandmarkDataToServer(landmarkData: Any, csvFilePath: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 토큰 검증
                if (token.isEmpty()) {
                    Log.e("TAG", "토큰이 비어있습니다")
                    return@launch
                }

                val authHeader = "Bearer $token"
                Log.d("TAG", "인증 헤더: $authHeader")

                // CSV 파일을 사용하여 API 호출
                val csvFile = File(csvFilePath)
                val requestBody = csvFile.asRequestBody("text/csv".toMediaTypeOrNull())
                val csvPart = MultipartBody.Part.createFormData("file", csvFile.name, requestBody)

                val response = RetrofitClient.instance.submitLandmarks(authHeader, csvPart)
                if (response.isSuccessful) {
                    val submitResponse = response.body()
                    val translatedText = submitResponse?.sentence ?: "번역된 내용이 없습니다."

                    // UI 업데이트는 메인 스레드에서 수행
                    runOnUiThread {
                        val translationTextView: TextView = findViewById(R.id.translationText)
                        translationTextView.text = translatedText
                        Toast.makeText(this@HomeActivity, "손 동작이 번역되었습니다.", Toast.LENGTH_SHORT).show()
                        
                        // 바로 TTS 실행
                        textToSpeech.speak(translatedText, TextToSpeech.QUEUE_FLUSH, null, null)
                    }

                    // TTS API 호출
                    try {
                        val ttsRequest = TTSRequest(sentence = translatedText)
                        RetrofitClient.ttsApi.generateTTS(authHeader, ttsRequest)
                    } catch (e: Exception) {
                        Log.e("TAG", "TTS API 호출 중 오류 발생: ${e.message}")
                    }

                    // 2. 번역이 성공한 후에 번역 기록 조회 (3초 지연 후)
                    kotlinx.coroutines.delay(3000) // 3초 지연
                    try {
                        val historyResponse = RetrofitClient.historyApi.getTranslationHistory(authHeader)
                        if (historyResponse.isNotEmpty()) {
                            val latestHistory = historyResponse[0]
                            if (latestHistory.translatedText == translatedText) {
                                // 번역된 텍스트가 기록에 정상적으로 저장됨
                            }
                        }
                    } catch (e: Exception) {
                        // 예외 처리
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("TAG", "API 호출 실패: $errorBody")
                    Log.e("TAG", "응답 코드: ${response.code()}")
                    Log.e("TAG", "응답 메시지: ${response.message()}")

                    // 403 에러인 경우 토큰 재확인
                    if (response.code() == 403) {
                        Log.e("TAG", "403 Forbidden - 토큰이 만료되었거나 잘못되었을 수 있습니다")
                        // SharedPreferences에서 토큰 재확인
                        val currentToken = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                            .getString("token", "") ?: ""
                        Log.e("TAG", "현재 저장된 토큰: $currentToken")
                    }

                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "API 호출 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("TAG", "API 호출 중 오류 발생: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hands.close()
        textToSpeech.shutdown()
    }

    override fun onPause() {
        super.onPause()
        // TTS 중지
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
    }
}