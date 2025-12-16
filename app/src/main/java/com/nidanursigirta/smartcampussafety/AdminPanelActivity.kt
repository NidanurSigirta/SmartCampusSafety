package com.nidanursigirta.smartcampussafety

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminPanelActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var reportList: ArrayList<Report>
    private lateinit var adapter: AdminReportAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Duyuru Kartı
    private lateinit var cardAnnouncement: CardView
    private lateinit var tvAnnouncementText: TextView
    private var currentAnnouncementId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        recyclerView = findViewById(R.id.recyclerViewAdmin)
        recyclerView.layoutManager = LinearLayoutManager(this)
        reportList = arrayListOf()

        cardAnnouncement = findViewById(R.id.cardActiveAnnouncement)
        tvAnnouncementText = findViewById(R.id.tvAdminAnnouncementText)

        findViewById<ImageView>(R.id.btnAdminLogout).setOnClickListener { logoutAdmin() }
        findViewById<FloatingActionButton>(R.id.btnEmergency).setOnClickListener { showEmergencyDialog() }

        // Duyuru Kartına Tıklayınca SİLME İşlemi
        cardAnnouncement.setOnClickListener {
            deleteAnnouncement()
        }

        adapter = AdminReportAdapter(reportList) { report ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("REPORT_ID", report.reportId)
            intent.putExtra("is_admin", true)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        fetchReports()
        // Aktif duyuruyu dinle
        listenForActiveAnnouncement()
    }

    private fun listenForActiveAnnouncement() {
        db.collection("announcements")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                if (value != null && !value.isEmpty) {
                    val doc = value.documents[0]
                    val message = doc.getString("message") ?: ""
                    currentAnnouncementId = doc.id

                    if (message.isNotEmpty()) {
                        cardAnnouncement.visibility = View.VISIBLE
                        tvAnnouncementText.text = message
                    } else {
                        cardAnnouncement.visibility = View.GONE
                    }
                } else {
                    cardAnnouncement.visibility = View.GONE
                }
            }
    }

    private fun deleteAnnouncement() {
        if (currentAnnouncementId == null) return

        AlertDialog.Builder(this)
            .setTitle("Duyuruyu Kaldır")
            .setMessage("Bu acil durum duyurusunu yayından kaldırmak istiyor musunuz?")
            .setPositiveButton("Evet, Kaldır") { _, _ ->
                db.collection("announcements").document(currentAnnouncementId!!)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Duyuru kaldırıldı.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun logoutAdmin() {
        AlertDialog.Builder(this)
            .setTitle("Çıkış Yap")
            .setMessage("Admin oturumunu kapatmak istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun fetchReports() {
        db.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                if (value != null) {
                    reportList.clear()
                    for (document in value) {
                        val report = document.toObject(Report::class.java)
                        if (report.reportId.isEmpty()) report.reportId = document.id
                        reportList.add(report)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun showEmergencyDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("⚠️ Acil Durum Duyurusu")
        builder.setMessage("Tüm kullanıcılara gönderilecek acil mesajı giriniz:")
        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Yayınla") { _, _ ->
            val message = input.text.toString()
            if (message.isNotEmpty()) {
                val announcement = hashMapOf(
                    "message" to message,
                    "timestamp" to Timestamp.now(),
                    "type" to "ACİL",
                    "sender" to "Admin"
                )
                db.collection("announcements").add(announcement)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Duyuru yayınlandı!", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        builder.setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }
        builder.show()
    }
}