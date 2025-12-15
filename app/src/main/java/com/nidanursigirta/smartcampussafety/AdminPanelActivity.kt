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

    // Arayüz elemanları ve Veri yönetimi için değişkenler
    private lateinit var recyclerView: RecyclerView
    private lateinit var reportList: ArrayList<Report>
    private lateinit var adapter: AdminReportAdapter
    private val db = FirebaseFirestore.getInstance() // Firestore veritabanı bağlantısı

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        // RecyclerView (Liste) Ayarları
        recyclerView = findViewById(R.id.recyclerViewAdmin)
        recyclerView.layoutManager = LinearLayoutManager(this) // Listeyi alt alta sıralar
        reportList = arrayListOf() // Verilerin tutulacağı boş listeyi başlat

        // Adapter Bağlantısı:
        // Adapter, verileri (reportList) alıp arayüze (RecyclerView) yerleştirir.
        // Süslü parantez içindeki lambda fonksiyonu, "Durum Değiştir" butonuna tıklandığında çalışır.
        adapter = AdminReportAdapter(reportList) { report, view ->
            showStatusMenu(report, view) // Tıklanan raporun durumunu değiştirmek için menüyü aç
        }
        recyclerView.adapter = adapter

        // Acil Durum Butonu Tanımlaması
        // Admin, tüm okula acil bir duyuru geçmek isterse bu butonu kullanır.
        findViewById<View>(R.id.btnEmergency).setOnClickListener {
            showEmergencyDialog() // Mesaj giriş penceresini aç
        }

        // Sayfa açıldığında verileri veritabanından çekmeye başla
        fetchReports()
    }

    /**
     * Firestore'dan raporları çeker ve "Canlı" (Real-time) olarak dinler.
     * Veritabanına yeni bir veri eklendiğinde veya bir durum değiştiğinde sayfa otomatik yenilenir.
     */
    private fun fetchReports() {
        db.collection("reports") // "reports" koleksiyonuna bağlan
            .orderBy("timestamp", Query.Direction.DESCENDING) // Yeniden eskiye doğru sırala
            .addSnapshotListener { value, error -> // Anlık veri dinleyicisi (Snapshot Listener)

                // Hata kontrolü: Veritabanına erişim yoksa veya internet kesikse
                if (error != null) {
                    Toast.makeText(this, "Veri çekme hatası: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                // Veri geldiyse işlemleri yap
                if (value != null) {
                    reportList.clear() // Eski listeyi temizle (Duplicate olmasın diye)

                    for (document in value) {
                        // Gelen dökümanı 'Report' sınıfına dönüştür
                        val report = document.toObject(Report::class.java)

                        // ÖNEMLİ: Eğer veritabanı içindeki 'reportId' alanı boşsa,
                        // dökümanın kendi ID'sini (Document ID) modele atıyoruz.
                        // Bu ID, güncelleme işlemleri için zorunludur.
                        if (report.reportId.isEmpty()) {
                            report.reportId = document.id
                        }

                        reportList.add(report) // Listeye ekle
                    }

                    // Adapter'a "Veriler değişti, listeyi yenile" komutu ver
                    adapter.notifyDataSetChanged()
                }
            }
    }

    /**
     * Her bir rapor kartındaki "Seçenekler" butonuna basınca açılan menü.
     * Admin buradan raporun durumunu (Açık/Çözüldü vb.) değiştirir.
     */
    private fun showStatusMenu(report: Report, view: View) {
        val popup = PopupMenu(this, view)
        // Menü seçeneklerini ekle
        popup.menu.add("Açık")
        popup.menu.add("İnceleniyor")
        popup.menu.add("Çözüldü")

        // Seçeneklerden birine tıklanınca ne olacak?
        popup.setOnMenuItemClickListener { item ->
            val newStatus = item.title.toString() // Seçilen yeni durumu al (Örn: "Çözüldü")
            updateReportStatus(report.reportId, newStatus) // Firestore güncelleme fonksiyonunu çağır
            true
        }
        popup.show() // Menüyü ekranda göster
    }

    /**
     * Firestore üzerindeki belirli bir raporun sadece "status" alanını günceller.
     * @param docId: Güncellenecek dökümanın kimliği
     * @param newStatus: Yeni durum metni
     */
    private fun updateReportStatus(docId: String, newStatus: String) {
        db.collection("reports").document(docId)
            .update("status", newStatus) // Sadece status alanını değiştirir, diğer verileri korur
            .addOnSuccessListener {
                Toast.makeText(this, "Durum başarıyla güncellendi: $newStatus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Güncelleme sırasında hata oluştu!", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Acil durum mesajı girmek için ekrana bir Dialog (Pencere) açar.
     */
    private fun showEmergencyDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("⚠️ Acil Durum Duyurusu")
        builder.setMessage("Tüm kullanıcılara gönderilecek acil mesajı giriniz:")

        // Dialog içine yazı yazılabilecek bir alan (EditText) ekle
        val input = EditText(this)
        builder.setView(input)

        // "Yayınla" butonuna basınca
        builder.setPositiveButton("Yayınla") { _, _ ->
            val message = input.text.toString()
            if (message.isNotEmpty()) {
                sendEmergencyBroadcast(message) // Veritabanına kaydet
            } else {
                Toast.makeText(this, "Boş mesaj gönderilemez!", Toast.LENGTH_SHORT).show()
            }
        }
        // "İptal" butonuna basınca pencereyi kapat
        builder.setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    /**
     * Girilen acil durum mesajını "announcements" adlı ayrı bir koleksiyona kaydeder.
     * Kullanıcı tarafında bu koleksiyon dinlenerek bildirim gösterilebilir.
     */
    private fun sendEmergencyBroadcast(message: String) {
        // Gönderilecek veri paketi (Hashmap)
        val announcement = hashMapOf(
            "message" to message,
            "timestamp" to Timestamp.now(), // Şu anki zaman
            "type" to "ACİL",
            "sender" to "Admin"
        )

        db.collection("announcements") // Duyurular için ayrı koleksiyon
            .add(announcement)
            .addOnSuccessListener {
                Toast.makeText(this, "Acil durum duyurusu tüm sisteme yayınlandı!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Duyuru yayınlanırken hata oluştu.", Toast.LENGTH_SHORT).show()
            }
    }
}