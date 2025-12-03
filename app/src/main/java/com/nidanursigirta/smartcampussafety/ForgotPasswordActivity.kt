package com.nidanursigirta.smartcampussafety

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.nidanursigirta.smartcampussafety.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Girişe Dön
        binding.tvBackToLogin.setOnClickListener {
            finish()
        }

        // ŞİFREMİ YENİLE BUTONU
        binding.btnSendResetLink.setOnClickListener {
            // Formdaki 3 bilgiyi de alalım
            val email = binding.etResetEmail.text.toString().trim()
            val pass1 = binding.etNewPassword.text.toString().trim()
            val pass2 = binding.etConfirmPassword.text.toString().trim()

            // 1. E-posta Kontrolü
            if (email.isEmpty()) {
                binding.etResetEmail.error = "Lütfen e-posta giriniz!"
                return@setOnClickListener
            }

            // 2. Şifre Kutuları Boş mu?
            if (pass1.isEmpty() || pass2.isEmpty()) {
                Toast.makeText(this, "Lütfen yeni şifrenizi giriniz.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Şifreler Aynı mı?
            if (pass1 != pass2) {
                binding.etConfirmPassword.error = "Şifreler uyuşmuyor!"
                return@setOnClickListener
            }

            // 4. Her şey tamam, Firebase'e Mail Gönderme İsteği
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    // Kullanıcıya şifrelerini onayladığımızı ama son adımın mailde olduğunu söylüyoruz
                    Toast.makeText(this, "Bilgiler alındı! Onay için e-postanıza gönderilen linke tıklayınız.", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }
}