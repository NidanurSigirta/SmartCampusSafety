package com.nidanursigirta.smartcampussafety

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.content.Intent //Sayfalar arası geçiş isteğinde bulunmak için
import com.google.firebase.auth.FirebaseAuth
import com.nidanursigirta.smartcampussafety.databinding.ActivityMainBinding // xml ile kotlin arasındaki viewBinding kullanımı için

class MainActivity : AppCompatActivity() {
    //lateinit daha sonra değer atanacak anlamında
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) //xml dosyasındakileri koda çevirip hafızada tutar
        setContentView(binding.root) //setContentView(R.layout.activity_main) yerine kullanacağız

        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Giriş butonuna tıklandığında olacaklar
        binding.loginButton.setOnClickListener{}

        //Kayıt ol butonuna tıklandığında kayıt sayfasına geçiş
        binding.signUpButton.setOnClickListener{
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }


        //Şifremi Unuttum'a tıklandığında olacaklar:
        binding.forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java) //Bu sayfadan ForgotPasswordActivity sayfasına geçiş isteği
            startActivity(intent) //ForgotPasswordActivity sayfasına geçiş yapıldı
        }

    }
}