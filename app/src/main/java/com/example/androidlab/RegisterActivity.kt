package com.example.androidlab

import RegisterApi
import RegisterRequest
import RegisterResponse
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import api.RetrofitClient
import retrofit2.Call
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {
    private lateinit var backButton: ImageButton
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var passwordConfirmInput: EditText
    private lateinit var registerButton: TextView
    private lateinit var googleLoginButton: ImageButton
    private lateinit var loginLink: TextView
    private var isPasswordVisible = false
    private var isPasswordConfirmVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        passwordConfirmInput = findViewById(R.id.passwordConfirmInput)
        registerButton = findViewById(R.id.registerButton)
        googleLoginButton = findViewById(R.id.googleLoginButton)
        loginLink = findViewById(R.id.loginLink)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        registerButton.setOnClickListener {
            val name = nameInput.text.toString()
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val passwordConfirm = passwordConfirmInput.text.toString()

            when {
                name.isEmpty() -> showToast("이름을 입력해주세요")
                email.isEmpty() -> showToast("이메일을 입력해주세요")
                password.isEmpty() -> showToast("비밀번호를 입력해주세요")
                passwordConfirm.isEmpty() -> showToast("비밀번호 확인을 입력해주세요")
                password != passwordConfirm -> showToast("비밀번호가 일치하지 않습니다")
                else -> {
                    val api = RetrofitClient.instance.create(RegisterApi::class.java)
                    val request = RegisterRequest(email, name, password)

                    api.register(request).enqueue(object : retrofit2.Callback<RegisterResponse> {
                        override fun onResponse(
                            call: Call<RegisterResponse>,
                            response: Response<RegisterResponse>
                        ) {
                            if (response.isSuccessful) {
                                val user = response.body()
                                showToast("회원가입 성공! ${user?.nickname}님 환영합니다")
                                val intent = Intent(this@RegisterActivity, HomeActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                showToast("회원가입 실패: ${response.code()}")
                            }
                        }

                        override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                            showToast("서버 연결 실패: ${t.message}")
                        }
                    })
                }
            }
        }

        googleLoginButton.setOnClickListener {
            // TODO: Google 로그인 구현
            showToast("Google 계정으로 회원가입")
        }

        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
} 