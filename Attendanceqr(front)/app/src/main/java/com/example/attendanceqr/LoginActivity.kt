package com.example.attendanceqr

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST

class LoginActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize SharedPreferences and Retrofit
        sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
        apiService = RetrofitClient.instance

        // Toolbar setup
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // UI elements
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Field validation
            if (email.isEmpty()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Password is required"
                return@setOnClickListener
            }

            // Disable button during login attempt
            btnLogin.isEnabled = false
            btnLogin.text = "Logging in..."

            // Call API for authentication
            authenticateUser(email, password)
        }
    }

    private fun authenticateUser(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.login(LoginRequest(email, password))
                handleLoginResponse(response)
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetLoginButton()
                }
            }
        }
    }

    private fun handleLoginResponse(response: Response<LoginResponse>) {
        runOnUiThread {
            if (response.isSuccessful) {
                response.body()?.let { loginResponse ->
                    // Save token and user data
                    sharedPreferences.edit().apply {
                        putString("ACCESS_TOKEN", loginResponse.token)
                        putString("USER_EMAIL", loginResponse.user.email)
                        putString("USER_ROLE", loginResponse.user.role)
                        apply()
                    }

                    // Navigate based on role

                    val intent = Intent(this@LoginActivity, ScanActivity::class.java)

                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                }
            } else {
                Toast.makeText(
                    this@LoginActivity,
                    "Login failed: ${response.code()}",
                    Toast.LENGTH_SHORT
                ).show()
                resetLoginButton()
            }
        }
    }

    private fun resetLoginButton() {
        findViewById<MaterialButton>(R.id.btnLogin).apply {
            isEnabled = true
            text = "Login"
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val user: User)
interface ApiService {
    @POST("login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
