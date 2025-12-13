package com.nidanursigirta.smartcampussafety

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nidanursigirta.smartcampussafety.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // --- GÜVENLİK ADIMI ---
        // Test sırasında her seferinde giriş yapmak için oturumu kapatıyoruz
        if (auth.currentUser != null) {
            auth.signOut()
        }

        // KAYIT OL
        binding.signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // ŞİFREMİ UNUTTUM
        binding.forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        // GİRİŞ YAP
        binding.loginButton.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen e-posta ve şifre girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val userId = authResult.user!!.uid
                    checkUserRole(userId) // Rolü kontrol etmeye gidiyoruz
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Giriş Başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // --- DEĞİŞİKLİK YAPILAN KISIM BURASI ---
    private fun checkUserRole(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Veritabanındaki "role" alanını çekiyoruz
                    val role = document.getString("role") ?: "USER"
                    val name = document.getString("nameSurname") ?: "Kullanıcı"

                    Toast.makeText(this, "Hoş geldin $name ($role)", Toast.LENGTH_SHORT).show()

                    // ROL KONTROLÜ:
                    if (role == "Admin") {
                        // Eğer rol "Admin" ise Admin Paneline git
                        goToAdminPanel()
                    } else {
                        // Değilse normal ana sayfaya git
                        goToHome()
                    }

                } else {
                    Toast.makeText(this, "Kullanıcı verisi veritabanında bulunamadı!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Veri alma hatası! İnternet bağlantınızı kontrol edin.", Toast.LENGTH_SHORT).show()
            }
    }

    // Normal kullanıcılar için
    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Adminler için (YENİ EKLENDİ)
    private fun goToAdminPanel() {
        val intent = Intent(this, AdminPanelActivity::class.java)
        startActivity(intent)
        finish()
    }
}