package com.nidanursigirta.smartcampussafety

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nidanursigirta.smartcampussafety.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Geri Dön Tuşu
        binding.btnBackProfile.setOnClickListener { finish() }

        // Kullanıcı Bilgilerini Yükle
        loadUserProfile()

        // --- BUTONLAR ---

        // 1. Takip Edilenler
        binding.cardFollowed.setOnClickListener {
            Toast.makeText(this, "Takip edilenler sayfasına gidiliyor...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, FollowedReportsActivity_::class.java)
            startActivity(intent) //Takip edilenler sayfasına geçiş yapıldı
        }

        // 2. Bildirim Ayarları
        binding.cardNotificationSettings.setOnClickListener {
            Toast.makeText(this, "Bildirim ayarları açılıyor...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, NotificationSettingsActivity::class.java)
            startActivity(intent) //Bildirim ayarları sayfasına geçiş yapıldı
        }

        // 3. Çıkış Yap
        binding.btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    //Kullanıcı Bilgilerini Yüklemek İçin Gerekli Fonksiyon
    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId == null) return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("nameSurname") ?: "İsimsiz Kullanıcı"
                    val email = document.getString("email") ?: auth.currentUser?.email

                    // 1. Rolü veritabanından çek (Eğer boşsa varsayılan 'user' olsun)
                    val role = document.getString("role") ?: "user"

                    // 2. Verileri Ekrana Yaz
                    binding.tvProfileName.text = name
                    binding.tvProfileEmail.text = email

                    // 3. Rolü Yazdır (Sadece admin veya user)
                    // .capitalize() kullanarak "admin" -> "Admin" görünmesini sağlıyoruz, daha şık durur.
                    binding.tvProfileRole.text = role.replaceFirstChar { it.uppercase() }

                    // Profil fotoğrafı yükleme kısmı (Şimdilik varsayılan ikon kalıyor)
                    // İleride buraya Glide kodları gelecek.
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Profil bilgileri yüklenemedi", Toast.LENGTH_SHORT).show()
            }
    }

    //Uygulamadan Çıkış Fonksiyonu
    private fun logoutUser() {
        auth.signOut() // Firebase oturumunu kapat
        Toast.makeText(this, "Çıkış yapıldı", Toast.LENGTH_SHORT).show()

        // Giriş ekranına yönlendir ve geri dönüşü engelle (Geri tuşuna basınca profile dönmesin)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}