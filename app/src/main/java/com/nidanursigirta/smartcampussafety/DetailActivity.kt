package com.nidanursigirta.smartcampussafety

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nidanursigirta.smartcampussafety.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var reportId: String? = null
    private var currentReport: Report? = null

    // Takip durumu kontrolü için
    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Intent'ten ID'yi al
        reportId = intent.getStringExtra("REPORT_ID")

        binding.btnBack.setOnClickListener { finish() }

        if (reportId != null) {
            getReportDetails(reportId!!)
            checkUserRole()
            checkIfFollowing()
        } else {
            Toast.makeText(this, "Hata: Rapor ID bulunamadı", Toast.LENGTH_SHORT).show()
            finish()
        }

        // --- BUTON İŞLEMLERİ ---

        // Kullanıcı Takip Butonu
        binding.btnFollow.setOnClickListener {
            toggleFollowStatus()
        }

        // Admin Güncelleme Butonu
        binding.btnUpdateStatus.setOnClickListener {
            updateReportStatus()
        }

        // Admin Spinner Hazırlığı
        val statuses = arrayOf("Açık", "İnceleniyor", "Çözüldü")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)
        binding.spinnerStatus.adapter = adapter
    }

    private fun getReportDetails(id: String) {
        db.collection("reports").document(id).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val report = document.toObject(Report::class.java)
                    currentReport = report
                    updateUI(report)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Veri çekilemedi", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI(report: Report?) {
        if (report == null) return

        binding.txtTitle.text = report.title
        binding.txtDescription.text = report.description
        binding.txtType.text = report.type
        binding.txtDate.text = report.timestamp?.toDate().toString()

        // Durum Rengi Ayarlama
        binding.txtStatus.text = report.status.uppercase()
        when (report.status) {
            "Açık" -> binding.txtStatus.setTextColor(Color.RED)
            "İnceleniyor" -> binding.txtStatus.setTextColor(Color.parseColor("#FF9800")) // Turuncu
            "Çözüldü" -> binding.txtStatus.setTextColor(Color.parseColor("#4CAF50")) // Yeşil
        }

        // Harita kodları buradan tamamen kaldırıldı.
    }

    // --- ROL KONTROLÜ (Admin mi User mı?) ---
    private fun checkUserRole() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get().addOnSuccessListener { document ->
            val role = document.getString("role")

            if (role == "admin") {
                // Admin ise güncelleme panelini göster
                binding.adminPanel.visibility = View.VISIBLE
                binding.btnFollow.visibility = View.GONE
            } else {
                // User ise takip butonunu göster
                binding.adminPanel.visibility = View.GONE
                binding.btnFollow.visibility = View.VISIBLE
            }
        }
    }

    // --- ADMIN İŞLEVİ: DURUM GÜNCELLEME ---
    private fun updateReportStatus() {
        val newStatus = binding.spinnerStatus.selectedItem.toString()
        if (reportId == null) return

        db.collection("reports").document(reportId!!)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Durum güncellendi: $newStatus", Toast.LENGTH_SHORT).show()
                // UI'ı anlık güncelle
                binding.txtStatus.text = newStatus.uppercase()
                // Rengi de güncelle
                updateUI(currentReport?.apply { status = newStatus })
            }
            .addOnFailureListener {
                Toast.makeText(this, "Güncelleme başarısız", Toast.LENGTH_SHORT).show()
            }
    }

    // --- USER İŞLEVİ: TAKİP ET / BIRAK ---
    private fun checkIfFollowing() {
        val uid = auth.currentUser?.uid ?: return
        if (reportId == null) return

        db.collection("users").document(uid)
            .collection("followed_reports").document(reportId!!)
            .get()
            .addOnSuccessListener { document ->
                isFollowing = document.exists()
                updateFollowButtonUI()
            }
    }

    private fun toggleFollowStatus() {
        val uid = auth.currentUser?.uid ?: return
        if (reportId == null) return

        val followRef = db.collection("users").document(uid)
            .collection("followed_reports").document(reportId!!)

        if (isFollowing) {
            // Takipten Çık
            followRef.delete().addOnSuccessListener {
                isFollowing = false
                updateFollowButtonUI()
                Toast.makeText(this, "Takipten çıkıldı", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Takip Et
            val data = hashMapOf("timestamp" to System.currentTimeMillis())
            followRef.set(data).addOnSuccessListener {
                isFollowing = true
                updateFollowButtonUI()
                Toast.makeText(this, "Takip listesine eklendi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFollowButtonUI() {
        if (isFollowing) {
            binding.btnFollow.text = "Takipten Çık"
            binding.btnFollow.setBackgroundColor(Color.GRAY)
        } else {
            binding.btnFollow.text = "Bu Bildirimi Takip Et"
            binding.btnFollow.setBackgroundColor(Color.parseColor("#2196F3")) // Mavi
        }
    }
}