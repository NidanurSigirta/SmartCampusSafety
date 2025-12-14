package com.nidanursigirta.smartcampussafety

import android.content.Intent
import android.location.Address
import android.location.Geocoder // Konum arama servisi
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView // Arama çubuğu
import androidx.cardview.widget.CardView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale // Dil ayarı
import com.google.firebase.firestore.FirebaseFirestore

// 1. Veri Modeli
data class HaritaBildirim(
    val reportId: String,
    val baslik: String,
    val tur: String,
    val zaman: String,
    val lat: Double,
    val lng: Double,
    val aciklama: String = "" //Bildiirm detay ekranı konum için
)

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var db: FirebaseFirestore

    // Arayüz elemanlarını tanımlayalım
    private lateinit var infoCard: CardView
    private lateinit var txtTur: TextView
    private lateinit var txtBaslik: TextView
    private lateinit var txtZaman: TextView
    private lateinit var btnDetay: Button

    private lateinit var searchViewMap: SearchView // Arama Çubuğu değişkeni

    // Geri Butonu Değişkeni
    private lateinit var btnBackContainer: CardView

    // Konum Seçimi İçin Gerekli Değişkenler
    private lateinit var btnKonumSec: Button
    private var secilenLatLng: LatLng? = null
    private var isSelectionMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        db = FirebaseFirestore.getInstance() //Firestore'u başlatıldı
        // XML elemanlarını bağlayalım
        infoCard = findViewById(R.id.infoCard)
        txtTur = findViewById(R.id.txtTur)
        txtBaslik = findViewById(R.id.txtBaslik)
        txtZaman = findViewById(R.id.txtZaman)
        btnDetay = findViewById(R.id.btnDetay)
        btnKonumSec = findViewById(R.id.btnKonumSec)
        searchViewMap = findViewById(R.id.searchViewMap)

        // Geri Butonu Tanımlama
        btnBackContainer = findViewById(R.id.btnBackContainer)
        btnBackContainer.setOnClickListener {
            finish() // Sayfayı kapatıp geri döner
        }

        // --- Konum Seçme Mantığı ---

        // Mod Kontrolü: Diğer sayfadan "selection" emri geldi mi?
        val mode = intent.getStringExtra("mode")
        if (mode == "selection") {
            isSelectionMode = true
            Toast.makeText(this, "Lütfen haritada bir yere tıklayın", Toast.LENGTH_LONG).show()
        }

        // Onay Butonuna Tıklayınca
        btnKonumSec.setOnClickListener {
            if (secilenLatLng != null) {
                // Seçilen verileri paketleyip geri gönderiyoruz
                val returnIntent = Intent()
                returnIntent.putExtra("latitude", secilenLatLng!!.latitude)
                returnIntent.putExtra("longitude", secilenLatLng!!.longitude)
                setResult(RESULT_OK, returnIntent)
                finish() // Sayfayı kapat ve AddReportActivity'ye dön
            } else {
                Toast.makeText(this, "Lütfen önce haritadan bir yer seçin!", Toast.LENGTH_SHORT).show()
            }
        }

        // Arama Çubuğu Ayarı
        searchViewMap.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchLocation(query) // EKSİK OLAN FONKSİYONU ARTIK AŞAĞIDA TANIMLADIK
                    searchViewMap.clearFocus() // Klavyeyi kapat
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        // Haritayı yükleme
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Kampüs Merkezi Koordinatları (Hem normal mod hem seçim modu için varsayılan odak)
        val campusCenter = LatLng(39.89953921087502, 41.244187083657714)

        if (!isSelectionMode) {
            // NORMAL MOD
            // Örnek liste kodları SİLİNDİ.

            fetchReportsFromFirestore() // Sadece veritabanındaki gerçek verileri getir

            // Harita açılınca varsayılan olarak kampüs merkezine odaklan
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusCenter, 15f))

        } else {
            // SEÇİM MODU (Kampüs merkezine odaklan)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusCenter, 15f))
        }

        // MEVCUT PİNLERE TIKLAMA (Sadece Normal Modda)
        mMap.setOnMarkerClickListener { marker ->
            if (!isSelectionMode) {
                val secilenBildirim = marker.tag as? HaritaBildirim
                if (secilenBildirim != null) {
                    kartGoster(secilenBildirim)
                }
            }
            false
        }

        // HARİTAYA TIKLAMA (Elle Seçim)
        mMap.setOnMapClickListener { latLng ->
            if (isSelectionMode) {
                // Tıklanan yeri seçmek için ortak fonksiyonu kullanıyoruz
                konumSecimIslemi(latLng, "Seçilen Konum")
            } else {
                // Normal modda haritaya boş bir yere tıklanırsa bilgi kartını gizle
                infoCard.visibility = View.GONE
            }
        }
    }

    //
    private fun searchLocation(locationName: String) {
        val geocoder = Geocoder(this, Locale.getDefault()) //Arama çubuğu için gerekli servisi başlatır.
        try {
            val addressList: List<Address>? = geocoder.getFromLocationName(locationName, 1) //getFromLocationName eski yöntem daha sonrasında güncellenmesi lazım

            //Arama çubuğunda konum bulunduysa olacaklar:
            if (!addressList.isNullOrEmpty()) {
                val address = addressList[0] //Bulunan ilk adres
                val latLng = LatLng(address.latitude, address.longitude)

                // Kamerayı oraya götür
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

                // Eğer seçim modundaysak oraya PİN de koy
                if (isSelectionMode) {
                    konumSecimIslemi(latLng, locationName) // Ortak fonksiyonu çağırdık
                    Toast.makeText(this, "Konum seçildi: $locationName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Konum bulundu: $locationName", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "Konum bulunamadı", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Arama hatası", Toast.LENGTH_SHORT).show()
        }
    }


    private fun konumSecimIslemi(latLng: LatLng, title: String) {
        mMap.clear() // Eski pinleri sil
        //Seçilen yeşil konum iconunu seçilen yerde gösterme
        mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
        secilenLatLng = latLng //Seçilen konumun kordinat değerlerini saklar
        btnKonumSec.visibility = View.VISIBLE // Onay butonunu aç
    }

    private fun pinEkle(bildirim: HaritaBildirim) {
        val konum = LatLng(bildirim.lat, bildirim.lng)
        val renk = when (bildirim.tur) {
            "Tehlike" -> BitmapDescriptorFactory.HUE_RED
            "Bilgi" -> BitmapDescriptorFactory.HUE_BLUE
            "Teknik" -> BitmapDescriptorFactory.HUE_ORANGE
            else -> BitmapDescriptorFactory.HUE_VIOLET
        }

        val marker = mMap.addMarker(
            MarkerOptions()
                .position(konum)
                .title(bildirim.baslik)
                .icon(BitmapDescriptorFactory.defaultMarker(renk))
        )
        marker?.tag = bildirim
    }

    private fun kartGoster(bildirim: HaritaBildirim) {
        txtBaslik.text = bildirim.baslik
        txtTur.text = bildirim.tur.uppercase()
        txtZaman.text = bildirim.zaman

        when(bildirim.tur) {
            "Tehlike" -> txtTur.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            "Bilgi" -> txtTur.setTextColor(resources.getColor(android.R.color.holo_blue_dark))
            else -> txtTur.setTextColor(resources.getColor(android.R.color.darker_gray))
        }


        btnDetay.setOnClickListener {
            val intent = Intent(this, DetailActivity::class.java) //İlgili bildirim sayfasına geçiş için intent oluşturuldu

            //Yeni sayfaya geçişte götürelecek veriler
            intent.putExtra("REPORT_ID", bildirim.reportId)
            intent.putExtra("baslik", bildirim.baslik)
            intent.putExtra("tur", bildirim.tur)
            intent.putExtra("zaman", bildirim.zaman)
            intent.putExtra("aciklama", bildirim.aciklama) // Detay açıklaması
            intent.putExtra("lat", bildirim.lat) // Harita konumu için lazım
            intent.putExtra("lng", bildirim.lng) // Harita konumu için lazım

            Toast.makeText(this, "${bildirim.baslik} detayına gidiliyor...", Toast.LENGTH_SHORT).show()

            startActivity(intent) //Bildirim detay sayfasına geçiş
        }
        infoCard.visibility = View.VISIBLE
    }

    //Yeni Bildirimin Haritada Gösterilmesi
    private fun fetchReportsFromFirestore(){
        db.collection("reports")
            .whereEqualTo("status", "Açık") // Sadece Açık olanları getir
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    try {
                        val id = document.id
                        val title = document.getString("title") ?: "Başlıksız"
                        val type = document.getString("type") ?: "Genel"
                        val desc = document.getString("description") ?: ""
                        val timestamp = document.getTimestamp("timestamp")?.toDate()?.toString() ?: "Tarih yok"

                        val lat = document.getDouble("latitude")
                        val lng = document.getDouble("longitude")

                        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                            val bildirim = HaritaBildirim(id, title, type, timestamp, lat, lng, desc)
                            pinEkle(bildirim) // Haritaya ekle
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Veriler alınamadı", Toast.LENGTH_SHORT).show()
            }
    }
}

