package com.example.androidlab

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import api.LoginRequest
import api.LoginResponse
import api.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private lateinit var backButton: ImageButton
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var togglePassword: ImageButton
    private lateinit var loginButton: TextView
    private lateinit var forgotPassword: TextView
    private lateinit var googleLoginButton: ImageButton
    private lateinit var signupButton: TextView
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        togglePassword = findViewById(R.id.togglePassword)
        loginButton = findViewById(R.id.loginButton)
        forgotPassword = findViewById(R.id.forgotPassword)
        googleLoginButton = findViewById(R.id.googleLoginButton)
        signupButton = findViewById(R.id.signupButton)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        togglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordInput.transformationMethod = null
            } else {
                passwordInput.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            passwordInput.setSelection(passwordInput.text.length)
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isEmpty()) {
                showToast("이메일을 입력해주세요")
            } else if (password.isEmpty()) {
                showToast("비밀번호를 입력해주세요")
            } else {
                loginUser(email, password)
            }
        }

        forgotPassword.setOnClickListener {
            // TODO: 비밀번호 재설정 화면으로 이동
            Toast.makeText(this, "비밀번호 재설정", Toast.LENGTH_SHORT).show()
        }

        googleLoginButton.setOnClickListener {
            showToast("Google 계정으로 로그인")
        }

        signupButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun loginUser(email: String, password: String) {
        val loginRequest = LoginRequest(email, password)

        RetrofitClient.loginApi.login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val token = response.body()?.token
                    Log.d("LoginActivity", "Token: $token")
                    
                    // 토큰을 SharedPreferences에 저장
                    val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                    sharedPreferences.edit().putString("token", token).apply()

                    // 번역 기록 조회
                    lifecycleScope.launch {
                        try {
                            val histories = RetrofitClient.historyApi.getTranslationHistory("Bearer $token")
                            Log.d("LoginActivity", "번역 기록 조회 성공: ${histories.size}개의 기록")
                            Log.d("LoginActivity", "번역 기록: $histories")
                        } catch (e: Exception) {
                            Log.e("LoginActivity", "번역 기록 조회 실패: ${e.message}")
                        }
                    }

                    showToast("로그인 성공!")

                    // HomeActivity로 이동
                    val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    showToast("로그인 실패: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                showToast("로그인 오류: ${t.message}")
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}