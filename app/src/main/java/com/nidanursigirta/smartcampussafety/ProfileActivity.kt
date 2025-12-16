package com.nidanursigirta.smartcampussafety

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nidanursigirta.smartcampussafety.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val notificationSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Toast.makeText(this, "Ayarlar Kaydedildi ✅", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnBackProfile.setOnClickListener { finish() }

        loadUserProfile()

        // YENİ: Kırmızı nokta kontrolü
        checkUnreadNotifications()

        binding.cardFollowed.setOnClickListener {
            val intent = Intent(this, FollowedReportsActivity_::class.java)
            startActivity(intent)
        }

        binding.cardNotificationSettings.setOnClickListener {
            val intent = Intent(this, NotificationSettingsActivity::class.java)
            notificationSettingsLauncher.launch(intent)
        }

        binding.btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    // Takip edilenler sayfasından dönünce nokta sönmüş mü diye tekrar kontrol et
    override fun onResume() {
        super.onResume()
        checkUnreadNotifications()
    }

    // --- KIRMIZI NOKTA MANTIĞI ---
    private fun checkUnreadNotifications() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).collection("followed_reports")
            .get()
            .addOnSuccessListener { documents ->
                // Eğer takip edilen yoksa noktayı gizle
                if (documents.isEmpty) {
                    binding.viewNotificationBadge.visibility = View.GONE
                    return@addOnSuccessListener
                }

                var hasUnread = false
                val totalDocs = documents.size()
                var checkedDocs = 0

                for (doc in documents) {
                    val reportId = doc.id
                    // Kullanıcının en son gördüğü zaman
                    val lastViewed = doc.getTimestamp("lastViewedTimestamp") ?: Timestamp(0, 0)

                    db.collection("reports").document(reportId).get()
                        .addOnSuccessListener { reportDoc ->
                            if (reportDoc.exists()) {
                                val lastUpdate = reportDoc.getTimestamp("lastUpdateTimestamp")

                                // Raporun Güncelleme Tarihi > Benim Görme Tarihim ise -> YENİ BİLDİRİM VAR!
                                if (lastUpdate != null && lastUpdate > lastViewed) {
                                    hasUnread = true
                                }
                            }

                            checkedDocs++
                            // Tüm kontrolller bittiğinde
                            if (checkedDocs == totalDocs) {
                                if (hasUnread) {
                                    binding.viewNotificationBadge.visibility = View.VISIBLE // Noktayı Yak
                                    // DÜZELTME: .text atamasını kaldırdık çünkü artık View kullanıyoruz
                                } else {
                                    binding.viewNotificationBadge.visibility = View.GONE // Noktayı Söndür
                                }
                            }
                        }
                        .addOnFailureListener {
                            checkedDocs++
                            if (checkedDocs == totalDocs && hasUnread) binding.viewNotificationBadge.visibility = View.VISIBLE
                        }
                }
            }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.tvProfileName.text = document.getString("nameSurname")
                    binding.tvProfileEmail.text = document.getString("email")
                    val role = document.getString("role") ?: "user"
                    binding.tvProfileRole.text = role.replaceFirstChar { it.uppercase() }
                }
            }
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Çıkış yapıldı", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}