package com.example.androidlab

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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


class HomeActivity : AppCompatActivity() {
    private lateinit var hands: Hands
    private lateinit var cameraInput: CameraInput
    private lateinit var glSurfaceView: SolutionGlSurfaceView<HandsResult>

    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.INTERNET,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_NETWORK_STATE
    ).toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

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

        glSurfaceView = SolutionGlSurfaceView(this@HomeActivity, hands.glContext,
            hands.glMajorVersion)
        glSurfaceView.setSolutionResultRenderer(HandsResultGlRenderer())
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

    override fun onDestroy() {
        super.onDestroy()
        hands.close()
    }
}