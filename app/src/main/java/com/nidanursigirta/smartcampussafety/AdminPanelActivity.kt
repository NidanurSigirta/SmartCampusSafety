package com.nidanursigirta.smartcampussafety

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminPanelActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var reportList: ArrayList<Report>
    private lateinit var adapter: AdminReportAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        recyclerView = findViewById(R.id.recyclerViewAdmin)
        recyclerView.layoutManager = LinearLayoutManager(this)
        reportList = arrayListOf()

        // Adapteri bağla
        adapter = AdminReportAdapter(reportList) { report, view ->
            // Butona tıklanınca PopupMenu aç
            showStatusMenu(report, view)
        }
        recyclerView.adapter = adapter

        // Acil Durum Butonu
        findViewById<View>(R.id.btnEmergency).setOnClickListener {
            showEmergencyDialog()
        }

        // Verileri Firestore'dan çek
        fetchReports()
    }

    private fun fetchReports() {
        // Admin tüm bildirimleri görebilir, zamana göre sıralı
        // NOT: Firestore'daki koleksiyon adının "reports" olduğunu varsaydım.
        // Eğer farklıysa (örn: "bildirimler") burayı değiştir.
        db.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(this, "Hata oluştu: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (value != null) {
                    reportList.clear()
                    for (document in value) {
                        val report = document.toObject(Report::class.java)
                        // reportId boş gelirse döküman ID'sini ata (önlem amaçlı)
                        if (report.reportId.isEmpty()) {
                            report.reportId = document.id
                        }
                        reportList.add(report)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    // Durum Değiştirme Menüsü (Popup)
    private fun showStatusMenu(report: Report, view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add("Açık")
        popup.menu.add("İnceleniyor")
        popup.menu.add("Çözüldü")

        popup.setOnMenuItemClickListener { item ->
            val newStatus = item.title.toString()
            updateReportStatus(report.reportId, newStatus)
            true
        }
        popup.show()
    }

    // Firestore'da Durumu Güncelle
    private fun updateReportStatus(docId: String, newStatus: String) {
        db.collection("reports").document(docId)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Durum güncellendi: $newStatus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Güncelleme başarısız!", Toast.LENGTH_SHORT).show()
            }
    }

    // Acil Durum Duyurusu Yayınlama (Dialog Penceresi)
    private fun showEmergencyDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("⚠️ Acil Durum Duyurusu")
        builder.setMessage("Tüm kullanıcılara gönderilecek mesajı giriniz:")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Yayınla") { _, _ ->
            val message = input.text.toString()
            if (message.isNotEmpty()) {
                sendEmergencyBroadcast(message)
            }
        }
        builder.setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    // Acil Durumu Firestore'a Kaydet
    private fun sendEmergencyBroadcast(message: String) {
        val announcement = hashMapOf(
            "message" to message,
            "timestamp" to Timestamp.now(),
            "type" to "ACİL",
            "sender" to "Admin"
        )

        db.collection("announcements") // Yeni bir koleksiyon
            .add(announcement)
            .addOnSuccessListener {
                Toast.makeText(this, "Acil durum duyurusu yayınlandı!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Duyuru yayınlanamadı.", Toast.LENGTH_SHORT).show()
            }
    }
}