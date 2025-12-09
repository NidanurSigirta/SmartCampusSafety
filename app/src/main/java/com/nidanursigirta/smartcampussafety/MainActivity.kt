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
        // Tasarım dosyasını (XML) koda bağlıyoruz
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase araçlarını başlatıyoruz
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // --- GÜVENLİK ADIMI: OTOMATİK GİRİŞİ İPTAL ETME ---
        // Uygulama her açıldığında önceki oturumu kapatıyoruz ki
        // kullanıcı tekrar şifre girmek zorunda kalsın (Test aşaması için).
        if (auth.currentUser != null) {
            auth.signOut()
        }

        // ----------------------------------------------------------------
        // ADIM 1: KAYIT OL SAYFASINA GEÇİŞ (Duygu'nun koduyla aynı mantık)
        // ----------------------------------------------------------------
        binding.signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // ----------------------------------------------------------------
        // ADIM 2: ŞİFREMİ UNUTTUM SAYFASINA GEÇİŞ
        // ----------------------------------------------------------------
        binding.forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        // ----------------------------------------------------------------
        // ADIM 3: GİRİŞ YAP BUTONU
        // ----------------------------------------------------------------
        binding.loginButton.setOnClickListener {
            // Kullanıcının girdiği verileri alalım
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // 1. Boş alan kontrolü
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen e-posta ve şifre girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Firebase Authentication ile giriş yapmayı dene
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    // Giriş başarılı! Şimdi veritabanından rolünü öğrenelim.
                    val userId = authResult.user!!.uid
                    checkUserRole(userId)
                }
                .addOnFailureListener { e ->
                    // Giriş başarısız (Şifre yanlış, kullanıcı yok vb.)
                    Toast.makeText(this, "Giriş Başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // Kullanıcının rolünü (Admin/User) Firestore'dan öğrenen fonksiyon
    private fun checkUserRole(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Rol verisini al, eğer yoksa varsayılan "USER" olsun
                    val role = document.getString("role") ?: "USER"
                    val name = document.getString("nameSurname") ?: "Kullanıcı"

                    Toast.makeText(this, "Hoş geldin $name ($role)", Toast.LENGTH_SHORT).show()

                    // Ana sayfaya git
                    goToHome()
                } else {
                    Toast.makeText(this, "Kullanıcı verisi veritabanında bulunamadı!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Veri alma hatası! İnternet bağlantınızı kontrol edin.", Toast.LENGTH_SHORT).show()
            }
    }

    // Ana sayfaya (HomeActivity) geçiş yapan yardımcı fonksiyon
    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        // Geri tuşuna basınca tekrar giriş ekranına dönmesin diye finish() ekledik
        finish()
    }
}