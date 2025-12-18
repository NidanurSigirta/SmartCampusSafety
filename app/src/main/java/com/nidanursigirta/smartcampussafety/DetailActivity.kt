package com.nidanursigirta.smartcampussafety

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nidanursigirta.smartcampussafety.databinding.ActivityDetailBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class DetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var reportId: String? = null
    private var currentReport: Report? = null
    private lateinit var map: GoogleMap
    private var isFollowing = false
    private var isAdminMode = false

    // Durum seçenekleri
    private val statusOptions = arrayOf("Açık", "İnceleniyor", "Çözüldü")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Veritabanı ve kimlik doğrulama servislerini başlatma.
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Önceki sayfadan gönderilen verileri (ID ve Mod) alma.
        reportId = intent.getStringExtra("REPORT_ID")
        isAdminMode = intent.getBooleanExtra("is_admin", false)

        binding.btnBack.setOnClickListener { finish() }

        if (reportId != null) {
            getReportDetails(reportId!!)
            checkUserRole()
            checkIfFollowing()
            markAsViewed()
        } else {
            Toast.makeText(this, "Hata: Rapor ID yok", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Harita bileşenini (Fragment) arayüzden tanımlayıp yükleme işlemini başlatıyoruz.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.detailMapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Kullanıcı arayüzü buton dinleyicileri tanımlandı.
        binding.btnFollow.setOnClickListener { toggleFollowStatus() }
        binding.btnUpdateStatus.setOnClickListener { updateReportStatus() }
        binding.btnUpdateDesc.setOnClickListener { updateDescription() }
        binding.btnDeleteReport.setOnClickListener { deleteReport() }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statusOptions)
        binding.spinnerStatus.adapter = adapter
    }

    /** * Google Haritası hazır olduğunda ilk konum odaklamasını yapan
     * ve arayüz zoom ayarlarını içeren fonksiyon.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Detay ekranı olduğu için kaydırmayı (scroll) kapatıp, yakınlaştırma (zoom) özelliğini açık bıraktık.
        map.uiSettings.isScrollGesturesEnabled = false
        map.uiSettings.isZoomGesturesEnabled = true

        // Veritabanından gelen rapor verisi harita hazır olmadan önce yüklendiyse, konumu hemen güncelliyoruz.
        if (currentReport != null) updateMapLocation(currentReport!!)
    }

    // Rapor detaylarını anlık olarak takip eden fonksiyon.
    private fun getReportDetails(id: String) {
        db.collection("reports").document(id)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Toast.makeText(this, "Veri güncellenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val report = document.toObject(Report::class.java)
                    currentReport = report
                    updateUI(report)

                    // Bildirimi oluşturan kişinin bilgilerini çekme işlemi.
                    if (report != null && report.creatorId.isNotEmpty()) {
                        fetchCreatorInfo(report.creatorId)
                    }
                }
            }
    }

    // Kullanıcı bilgisini çeken fonksiyon.
    private fun fetchCreatorInfo(creatorId: String) {
        db.collection("users").document(creatorId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("nameSurname") ?: "Bilinmeyen Kullanıcı"
                    val department = document.getString("department") ?: ""

                    // Admin için hem isim hem bölüm bilgisi yararlı olur.
                    if (department.isNotEmpty()) {
                        binding.txtCreatorName.text = "Oluşturan: $name ($department)"
                    } else {
                        binding.txtCreatorName.text = "Oluşturan: $name"
                    }
                } else {
                    binding.txtCreatorName.text = "Oluşturan: Bilinmiyor"
                }
            }
            .addOnFailureListener {
                binding.txtCreatorName.text = "Oluşturan bilgisi alınamadı"
            }
    }

    private fun updateUI(report: Report?) {
        if (report == null) return
        binding.txtTitle.text = report.title

        if (binding.etDescription.text.toString() != report.description) {
            binding.etDescription.setText(report.description)
        }

        binding.txtType.text = report.type
        binding.txtDate.text = report.timestamp?.toDate().toString()
        binding.txtStatus.text = report.status.uppercase()

        val statusIndex = statusOptions.indexOfFirst { it.equals(report.status, ignoreCase = true) }
        if (statusIndex >= 0) {
            binding.spinnerStatus.setSelection(statusIndex)
        }

        when (report.status) {
            "Açık" -> binding.txtStatus.setTextColor(Color.RED)
            "İnceleniyor" -> binding.txtStatus.setTextColor(Color.parseColor("#FF9800"))
            "Çözüldü" -> binding.txtStatus.setTextColor(Color.parseColor("#4CAF50"))
        }

        if (::map.isInitialized) updateMapLocation(report)
    }

    private fun updateMapLocation(report: Report) {
        // Atatürk Üniversitesi Kampüs Merkezi koordinatları.
        val campusCenter = LatLng(39.89953921087502, 41.244187083657714)

        // Harita görsel olarak tamamen hazır olduğunda komutu çalıştır.
        // (Bu sayede varsayılan konum olarak Afrika değil de Atatürk Üniversitesi Kampüs Merkezi'ni gösterecek.)
        map.setOnMapLoadedCallback {
            // Bildirimde gerçek bir konum eklenmişse:
            if (report.latitude != 0.0 && report.longitude != 0.0) {
                val loc = LatLng(report.latitude, report.longitude)
                map.clear()
                map.addMarker(MarkerOptions().position(loc).title(report.title))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 16f))
            }
            // Konum bilgisi eklenmediyse veya eklenirken bir sorun çıktıysa konum olarak üniversite kampüsünü göster.
            else {
                // Harita üzerinde yumuşak bir geçiş için animateCamera() metodu kullanıldı.
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(campusCenter, 15f))
            }
        }
    }

    private fun checkUserRole() {
        if (isAdminMode) {
            setupAdminUI()
            return
        }
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { document ->
            val role = document.getString("role")
            if (role == "admin" || role == "ADMIN") {
                setupAdminUI()
            } else {
                setupUserUI()
            }
        }
    }

    private fun setupAdminUI() {
        binding.adminPanel.visibility = View.VISIBLE
        binding.btnFollow.visibility = View.GONE
        binding.etDescription.isEnabled = true
        binding.etDescription.setBackgroundColor(Color.parseColor("#E3F2FD"))
    }

    private fun setupUserUI() {
        binding.adminPanel.visibility = View.GONE
        binding.btnFollow.visibility = View.VISIBLE
        binding.etDescription.isEnabled = false
        binding.etDescription.setBackgroundColor(Color.TRANSPARENT)
    }

    // --- İŞLEMLER ---

    private fun updateReportStatus() {
        val newStatus = binding.spinnerStatus.selectedItem.toString()

        // Sadece Durum Mesajını güncelliyoruz (Eskisinin üzerine yazar, geçmiş birikmez).
        val msg = "Durum '$newStatus' olarak güncellendi."

        db.collection("reports").document(reportId!!)
            .update(
                mapOf(
                    "status" to newStatus,
                    "statusUpdateMessage" to msg, // Ayrı alan
                    "lastUpdateTimestamp" to com.google.firebase.Timestamp.now()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Durum güncellendi", Toast.LENGTH_SHORT).show()
                binding.txtStatus.text = newStatus.uppercase()
            }
    }

    private fun updateDescription() {
        val newDesc = binding.etDescription.text.toString()
        if (reportId == null) return

        // Sadece Açıklama Mesajını güncelliyoruz.
        val msg = "Bildirim açıklaması admin tarafından düzenlendi."

        db.collection("reports").document(reportId!!)
            .update(
                mapOf(
                    "description" to newDesc,
                    "descUpdateMessage" to msg, // Ayrı alan
                    "lastUpdateTimestamp" to com.google.firebase.Timestamp.now()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Açıklama kaydedildi", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteReport() {
        AlertDialog.Builder(this)
            .setTitle("Silme Onayı")
            .setMessage("Bu bildirim kalıcı olarak silinsin mi?")
            .setPositiveButton("Evet") { _, _ ->
                db.collection("reports").document(reportId!!)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Silindi.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun checkIfFollowing() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("followed_reports").document(reportId!!)
            .get().addOnSuccessListener {
                isFollowing = it.exists()
                updateFollowButtonUI()
            }
    }

    private fun markAsViewed() {
        val uid = auth.currentUser?.uid ?: return
        if (reportId == null) return

        // Kullanıcının takip listesindeki bu rapora "Şu an gördüm" bilgisini yaz.
        db.collection("users").document(uid)
            .collection("followed_reports").document(reportId!!)
            .update("lastViewedTimestamp", com.google.firebase.Timestamp.now())
            .addOnFailureListener {
                // Eğer doküman yoksa (takip etmiyorsa) hata verir, önemli değil.
            }
    }

    private fun toggleFollowStatus() {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(uid)
            .collection("followed_reports").document(reportId!!)

        if (isFollowing) {
            ref.delete().addOnSuccessListener { isFollowing = false; updateFollowButtonUI() }
        } else {
            ref.set(hashMapOf("timestamp" to System.currentTimeMillis()))
                .addOnSuccessListener { isFollowing = true; updateFollowButtonUI() }
        }
    }

    private fun updateFollowButtonUI() {
        binding.btnFollow.text = if (isFollowing) "Takipten Çık" else "Takip Et"
        binding.btnFollow.setBackgroundColor(if (isFollowing) Color.GRAY else Color.parseColor("#2196F3"))
    }
}