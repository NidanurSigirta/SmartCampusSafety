package com.nidanursigirta.smartcampussafety

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.nidanursigirta.smartcampussafety.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tasarım dosyasını (XML) koda bağlıyoruz
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase'i başlatıyoruz
        auth = FirebaseAuth.getInstance()

        // ----------------------------------------------------------------
        // ADIM 3: KAYIT OL BUTONUNA TIKLAYINCA YENİ SAYFAYA GİTME İŞLEMİ
        // ----------------------------------------------------------------
        binding.signUpButton.setOnClickListener {
            // Intent: Bir sayfadan diğerine geçiş niyetidir.
            // MainActivity'den -> SignUpActivity'ye git diyoruz.
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // --- GİRİŞ YAP BUTONU (Şimdilik basit bir mesaj versin) ---
        binding.loginButton.setOnClickListener {
            // Kullanıcı adı şifre kontrolünü daha sonra ekleyeceğiz
            Toast.makeText(this, "Giriş butonu çalışıyor", Toast.LENGTH_SHORT).show()
        }

        // --- ŞİFREMİ UNUTTUM ---
        // DİKKAT: Henüz "ForgotPasswordActivity" dosyasını oluşturmadığımız için
        // bu kod açık olursa uygulama hata verir (kırmızı yanar).
        // O yüzden şimdilik yorum satırı (//) içine aldım. O sayfayı yapınca açarız.
        /*
        binding.forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
        */
    }
}