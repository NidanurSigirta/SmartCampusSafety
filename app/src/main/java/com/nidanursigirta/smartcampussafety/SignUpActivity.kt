package com.nidanursigirta.smartcampussafety

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nidanursigirta.smartcampussafety.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Firebase Araçlarını Başlatıyoruz
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // --- Geri Dönüş Linki ---
        binding.tvBackToLogin.setOnClickListener {
            finish() // Sayfayı kapatır, arkadaki giriş ekranına döner
        }

        // --- KAYIT OL BUTONU ---
        binding.btnRegisterFinish.setOnClickListener {
            // Formdaki verileri alalım
            val nameSurname = binding.etNameSurname.text.toString().trim()
            val department = binding.etDepartment.text.toString().trim()
            val email = binding.etRegisterEmail.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString().trim()

            // A) Boş Alan Kontrolü
            if (nameSurname.isEmpty() || department.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // B) Şifre Uzunluk Kontrolü
            if (password.length < 6) {
                binding.etRegisterPassword.error = "Şifre en az 6 karakter olmalı"
                return@setOnClickListener
            }

            // C) Firebase'de Kullanıcı Oluşturma Başlıyor
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    // 1. Aşama: Authentication kaydı başarılı!
                    val userId = authResult.user!!.uid

                    // 2. Aşama: Şimdi detayları veritabanına (Firestore) yazalım
                    // Proje dokümanına göre varsayılan rol "USER" olmalı.
                    val userInfo = hashMapOf(
                        "uid" to userId,
                        "nameSurname" to nameSurname,
                        "department" to department,
                        "email" to email,
                        "role" to "USER"
                    )

                    db.collection("users").document(userId)
                        .set(userInfo)
                        .addOnSuccessListener {
                            // Hem giriş hem veritabanı kaydı başarılı!
                            Toast.makeText(this, "Kayıt Başarılı! Giriş yapabilirsiniz.", Toast.LENGTH_LONG).show()

                            // Kullanıcıyı oluşturur oluşturmaz otomatik giriş yapmış sayar Firebase.
                            // Ama biz "Giriş yapabilirsiniz" dediğimiz için çıkış yaptıralım, eliyle girsin.
                            auth.signOut()

                            finish() // Kayıt ekranını kapat
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Veritabanı Hatası: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    // E-posta zaten varsa veya format hatalıysa buraya düşer
                    Toast.makeText(this, "Kayıt Başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}