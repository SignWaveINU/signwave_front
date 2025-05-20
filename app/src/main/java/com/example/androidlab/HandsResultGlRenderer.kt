package com.example.androidlab

import android.Manifest
import android.content.Context
import android.opengl.GLES20
import android.os.Environment
import android.util.Log
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.solutioncore.ResultGlRenderer
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsResult
import java.io.File

import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel

class HandsResultGlRenderer(
    private val context: Context,
    private val onLandmarkDataReady: (Any, String?) -> Unit
) : ResultGlRenderer<HandsResult?> {
    private var program = 0
    private var positionHandle = 0
    private var projectionMatrixHandle = 0
    private var colorHandle = 0

    // 랜드마크 시퀀스를 클래스의 멤버 변수로 선언
    private val landmarkSequences = mutableListOf<List<Float>>()
    
    // 손 감지 상태를 추적하는 변수
    private var wasHandDetected = false
    
    // API 호출 제어를 위한 변수들
    private var isProcessing = false
    private var lastApiCallTime = 0L
    private val apiCallInterval = 1000L // 1초 간격으로 API 호출 제한

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    override fun setupRendering() {
        program = GLES20.glCreateProgram()
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        projectionMatrixHandle = GLES20.glGetUniformLocation(program, "uProjectionMatrix")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private fun saveLandmarkDataToCsv(landmarkSequences: List<List<Float>>): String? {
        try {
            if (landmarkSequences.isEmpty()) {
                Log.d(TAG, "저장할 랜드마크 데이터가 없습니다.")
                return null
            }

            // CSV 파일 저장
            val csvFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "landmark_data.csv")
            val csvContent = StringBuilder()
            
            // CSV 헤더 작성
            val header = mutableListOf<String>()
            for (hand in listOf("hand1", "hand2")) {
                for (i in 0..20) {
                    header.add("${hand}_landmark${i}_x")
                    header.add("${hand}_landmark${i}_y")
                    header.add("${hand}_landmark${i}_z")
                }
            }
            csvContent.append(header.joinToString(",")).append("\n")
            
            // 프레임별 손 랜드마크 데이터 작성
            landmarkSequences.forEach { frameData ->
                try {
                    val row = when {
                        frameData.isEmpty() -> List(126) { 0f }  // 손이 하나도 없는 경우
                        frameData.size == 63 -> frameData + List(63) { 0f }  // 한 손만 감지된 경우
                        else -> frameData  // 두 손 다 감지된 경우
                    }
                    
                    csvContent.append(row.joinToString(",")).append("\n")
                } catch (e: Exception) {
                    Log.e(TAG, "프레임 데이터 처리 중 오류 발생", e)
                }
            }
            
            csvFile.writeText(csvContent.toString())
            Log.d(TAG, "CSV 저장 완료: ${csvFile.absolutePath}")
            
            return csvFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "파일 저장 중 오류 발생", e)
            return null
        }
    }

    fun release() {
        coroutineScope.cancel()
        GLES20.glDeleteProgram(program)
    }

    // 손의 존재 여부를 추적하기 위한 변수 추가
    private var wasHandPresent = false
    private var currentHandPresent = false
    
    private fun interpolateLandmarks(landmarkList: List<LandmarkProto.NormalizedLandmark>): List<LandmarkProto.NormalizedLandmark> {
        val interpolatedList = mutableListOf<LandmarkProto.NormalizedLandmark>()
        
        // 손바닥 중심점 추가
        interpolatedList.add(landmarkList[0])
        
        // 각 손가락에 대해 5개의 랜드마크 생성
        for (finger in 0..4) {  // 5개의 손가락
            val baseIndex = 1 + finger * 4  // 각 손가락의 시작 인덱스
            
            // 기존 4개의 랜드마크
            val p1 = landmarkList[baseIndex]
            val p2 = landmarkList[baseIndex + 1]
            val p3 = landmarkList[baseIndex + 2]
            val p4 = landmarkList[baseIndex + 3]
            
            // 첫 번째 랜드마크 추가
            interpolatedList.add(p1)
            
            // 두 번째와 세 번째 사이에 새로운 랜드마크 추가
            val newPoint1 = LandmarkProto.NormalizedLandmark.newBuilder()
                .setX((p2.x + p3.x) / 2)
                .setY((p2.y + p3.y) / 2)
                .setZ((p2.z + p3.z) / 2)
                .build()
            interpolatedList.add(p2)
            interpolatedList.add(newPoint1)
            interpolatedList.add(p3)
            interpolatedList.add(p4)
        }
        
        return interpolatedList
    }

    override fun renderResult(result: HandsResult?, projectionMatrix: FloatArray) {
        try {
            if (result == null) {
                if (wasHandPresent) {
                    sendAccumulatedData()
                }
                wasHandPresent = false
                currentHandPresent = false
                return
            }

            GLES20.glUseProgram(program)
            GLES20.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0)
            GLES20.glLineWidth(CONNECTION_THICKNESS)
            
            val numHands = result.multiHandLandmarks().size
            currentHandPresent = numHands > 0
            
            if (currentHandPresent) {
                try {
                    // 손 데이터를 저장할 배열 (126개 값: 21 landmarks × 3 coords × 2 hands)
                    val handData = FloatArray(126) { 0f }
                    
                    for (i in 0 until numHands) {
                        if (i >= 2) break // 최대 2개의 손만 처리
                        
                        val originalLandmarkList = result.multiHandLandmarks()[i].landmarkList
                        if (originalLandmarkList.size < 21) continue // 랜드마크가 부족한 경우 건너뛰기
                        
                        val interpolatedLandmarkList = interpolateLandmarks(originalLandmarkList)
                        
                        // 보간된 랜드마크 데이터 저장
                        val handLandmarks = interpolatedLandmarkList.flatMap { landmark ->
                            listOf(landmark.x, landmark.y, landmark.z)
                        }

                        // 손 데이터를 handData 배열에 저장 (hand1 또는 hand2)
                        val startIndex = i * 63 // 0 또는 63
                        for (j in handLandmarks.indices) {
                            if (startIndex + j < handData.size) {
                                handData[startIndex + j] = handLandmarks[j]
                            }
                        }
                        
                        // 렌더링은 원본 랜드마크로 수행
                        drawConnections(
                            originalLandmarkList,
                            if (result.multiHandedness()[i].label == "Left") LEFT_HAND_CONNECTION_COLOR
                            else RIGHT_HAND_CONNECTION_COLOR
                        )
                        
                        for (landmark in originalLandmarkList) {
                            drawCircle(
                                landmark.x,
                                landmark.y,
                                if (result.multiHandedness()[i].label == "Left") LEFT_HAND_LANDMARK_COLOR
                                else RIGHT_HAND_LANDMARK_COLOR
                            )
                            
                            drawHollowCircle(
                                landmark.x,
                                landmark.y,
                                if (result.multiHandedness()[i].label == "Left") LEFT_HAND_HOLLOW_CIRCLE_COLOR
                                else RIGHT_HAND_HOLLOW_CIRCLE_COLOR
                            )
                        }
                    }
                    
                    landmarkSequences.add(handData.toList())
                } catch (e: Exception) {
                    Log.e(TAG, "손 데이터 처리 중 오류 발생", e)
                }
            } else if (wasHandPresent) {
                sendAccumulatedData()
            }
            
            wasHandPresent = currentHandPresent
        } catch (e: Exception) {
            Log.e(TAG, "renderResult 처리 중 오류 발생", e)
        }
    }
    
    private fun sendAccumulatedData() {
        if (landmarkSequences.isNotEmpty()) {
            val csvFilePath = saveLandmarkDataToCsv(landmarkSequences.toList())
            onLandmarkDataReady(landmarkSequences.toList(), csvFilePath)
            landmarkSequences.clear()
        }
    }
    
    private fun drawConnections(
        handLandmarkList: List<LandmarkProto.NormalizedLandmark>,
        colorArray: FloatArray
    ) {
        GLES20.glUniform4fv(colorHandle, 1, colorArray, 0)
        for (c in Hands.HAND_CONNECTIONS) {
            val start = handLandmarkList[c.start()]
            val end = handLandmarkList[c.end()]
            val vertex = floatArrayOf(start.x, start.y, end.x, end.y)
            val vertexBuffer = ByteBuffer.allocateDirect(vertex.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertex)
            vertexBuffer.position(0)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT,
                false, 0, vertexBuffer)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)
        }
    }

    private fun drawCircle(x: Float, y: Float, colorArray: FloatArray) {
        GLES20.glUniform4fv(colorHandle, 1, colorArray, 0)
        val vertexCount = NUM_SEGMENTS + 2
        val vertices = FloatArray(vertexCount * 3)
        vertices[0] = x
        vertices[1] = y
        vertices[2] = 0f
        for (i in 1 until vertexCount) {
            val angle = 2.0f * i * Math.PI.toFloat() / NUM_SEGMENTS
            val currentIndex = 3 * i
            vertices[currentIndex] = x + (LANDMARK_RADIUS * Math.cos(angle.toDouble())).toFloat()
            vertices[currentIndex + 1] =
                y + (LANDMARK_RADIUS * Math.sin(angle.toDouble())).toFloat()
            vertices[currentIndex + 2] = 0f
        }
        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT,
            false, 0, vertexBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)
    }

    private fun drawHollowCircle(x: Float, y: Float, colorArray: FloatArray) {
        GLES20.glUniform4fv(colorHandle, 1, colorArray, 0)
        val vertexCount = NUM_SEGMENTS + 1
        val vertices = FloatArray(vertexCount * 3)
        for (i in 0 until vertexCount) {
            val angle = 2.0f * i * Math.PI.toFloat() / NUM_SEGMENTS
            val currentIndex = 3 * i
            vertices[currentIndex] =
                x + (HOLLOW_CIRCLE_RADIUS * Math.cos(angle.toDouble())).toFloat()
            vertices[currentIndex + 1] =
                y + (HOLLOW_CIRCLE_RADIUS * Math.sin(angle.toDouble())).toFloat()
            vertices[currentIndex + 2] = 0f
        }
        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT,
            false, 0, vertexBuffer)
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount)
    }

    private fun requestPermissions() {
        Dexter.withContext(context)
            .withPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        // 권한이 허용되었을 때의 처리
                    } else {
                        // 권한이 거부되었을 때의 처리
                        Log.e(TAG, "권한이 거부되었습니다.")
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    companion object {
        private const val TAG = "HandsResultGlRenderer"
        private val LEFT_HAND_CONNECTION_COLOR = floatArrayOf(0.2f, 1f, 0.2f, 1f)
        private val RIGHT_HAND_CONNECTION_COLOR = floatArrayOf(1f, 0.2f, 0.2f, 1f)
        private const val CONNECTION_THICKNESS = 25.0f
        private val LEFT_HAND_HOLLOW_CIRCLE_COLOR = floatArrayOf(0.2f, 1f, 0.2f, 1f)
        private val RIGHT_HAND_HOLLOW_CIRCLE_COLOR = floatArrayOf(1f, 0.2f, 0.2f, 1f)
        private const val HOLLOW_CIRCLE_RADIUS = 0.01f
        private val LEFT_HAND_LANDMARK_COLOR = floatArrayOf(1f, 0.2f, 0.2f, 1f)
        private val RIGHT_HAND_LANDMARK_COLOR = floatArrayOf(0.2f, 1f, 0.2f, 1f)
        private const val LANDMARK_RADIUS = 0.008f
        private const val NUM_SEGMENTS = 120
        private const val VERTEX_SHADER = ("uniform mat4 uProjectionMatrix;\n"
                + "attribute vec4 vPosition;\n"
                + "void main() {\n"
                + "  gl_Position = uProjectionMatrix * vPosition;\n"
                + "}")
        private const val FRAGMENT_SHADER = ("precision mediump float;\n"
                + "uniform vec4 uColor;\n"
                + "void main() {\n"
                + "  gl_FragColor = uColor;\n"
                + "}")
    }
}