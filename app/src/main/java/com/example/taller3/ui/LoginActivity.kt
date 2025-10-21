package com.example.taller3.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.data.UserRepository
import com.example.taller3.databinding.ActivityLoginBinding
import com.example.taller3.util.Toaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding
    private val repo = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnLogin.setOnClickListener {
            val email = b.etEmail.text.toString().trim()
            val pass = b.etPassword.text.toString().trim()

            if (email.isEmpty()) { Toaster.error(this, "Ingresa tu correo"); return@setOnClickListener }
            if (pass.isEmpty()) { Toaster.error(this, "Ingresa tu contraseña"); return@setOnClickListener }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    repo.loginEmailPassword(email, pass)
                    Toaster.ok(this@LoginActivity, "¡Bienvenido!")
                    goHome()
                } catch (e: Exception) {
                    val msg = e.message ?: "No se pudo iniciar sesión"
                    Toaster.error(this@LoginActivity, when {
                        "There is no user record" in msg -> "Usuario no encontrado"
                        "INVALID_EMAIL" in msg || "badly formatted" in msg -> "Correo inválido"
                        "INVALID_PASSWORD" in msg || "password is invalid" in msg -> "Contraseña incorrecta"
                        else -> msg
                    })
                }
            }
        }

        b.btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun goHome() {
        startActivity(Intent(this, MainMapActivity::class.java))
        finish()
    }
}
