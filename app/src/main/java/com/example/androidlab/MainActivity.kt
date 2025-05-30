package com.example.androidlab

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
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
        setContentView(R.layout.activity_main)
        
        // 로그인 화면으로 이동
        findViewById<Button>(R.id.loginButton).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        findViewById<Button>(R.id.registerButton).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun initializeViews() {
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        togglePassword = findViewById(R.id.togglePassword)
        loginButton = findViewById(R.id.loginButton)
        forgotPassword = findViewById(R.id.forgotPassword)
        googleLoginButton = findViewById(R.id.googleLoginButton)
        signupButton = findViewById(R.id.signupButton)
    }

    private fun setupClickListeners() {
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

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: 실제 로그인 로직 구현
            Toast.makeText(this, "로그인 시도 중...", Toast.LENGTH_SHORT).show()
        }

        forgotPassword.setOnClickListener {
            // TODO: 비밀번호 재설정 화면으로 이동
            Toast.makeText(this, "비밀번호 재설정", Toast.LENGTH_SHORT).show()
        }

        googleLoginButton.setOnClickListener {
            // TODO: Google 로그인 구현
            Toast.makeText(this, "Google 로그인", Toast.LENGTH_SHORT).show()
        }

        signupButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}   