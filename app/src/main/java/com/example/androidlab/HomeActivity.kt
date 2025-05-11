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
import api.LandmarkData
import api.RetrofitClient
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
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class HomeActivity : AppCompatActivity(){
    private lateinit var hands: Hands
    private lateinit var cameraInput: CameraInput
    private lateinit var glSurfaceView: SolutionGlSurfaceView<HandsResult>
    private lateinit var handsResultGlRenderer: HandsResultGlRenderer
    private lateinit var token: String
    private lateinit var translationText: TextView
    private lateinit var textToSpeech: TextToSpeech

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
                .setMaxNumHands(1)
                .setRunOnGpu(true)
                .build()
        )
        hands.setErrorListener { message, e -> Log.e("TAG", "MediaPipe Hands error: $message") }

        cameraInput = CameraInput(this@HomeActivity)
        cameraInput.setNewFrameListener { hands.send(it) }

        // handsResultGlRenderer 초기화
        handsResultGlRenderer = HandsResultGlRenderer { landmarkData ->
            // API 호출
            sendLandmarkDataToServer(landmarkData)
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

    private fun sendLandmarkDataToServer(landmarkData: LandmarkData) {
        // CoroutineScope를 사용하여 비동기 API 호출
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.submitLandmarks("Bearer $token", landmarkData)
                if (response.isSuccessful) {
                    val submitResponse = response.body() // SubmitResponse 객체 가져오기
                    val translatedText = submitResponse?.sentence ?: "번역된 내용이 없습니다." // 번역된 내용 가져오기

                    // UI 업데이트는 메인 스레드에서 수행해야 하므로 withContext 사용
                    withContext(Dispatchers.Main) {
                        translationText.text = translatedText // TextView에 텍스트 설정
                        Log.d("TAG", "API 호출 성공: $translatedText")
                    }
                } else {
                    Log.e("TAG", "API 호출 실패: ${response.errorBody()?.string()}")
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
}