package com.example.androidlab

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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
                    // TODO: 실제 회원가입 로직 구현
                    showToast("회원가입 성공!")
                    
                    // HomeActivity로 이동
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish() // 현재 액티비티 종료
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