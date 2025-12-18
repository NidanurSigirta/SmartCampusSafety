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

/*
Harita üzerindeki bildirimleri temsil eden veri modeli.
*/
data class HaritaBildirim(
    val reportId: String,
    val baslik: String,
    val tur: String,
    val zaman: String,
    val lat: Double,
    val lng: Double,
    val aciklama: String = "" // Bildirim detay ekranında gösterilecek açıklama.
)

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var db: FirebaseFirestore

    // Arayüz elemanları
    private lateinit var infoCard: CardView
    private lateinit var txtTur: TextView
    private lateinit var txtBaslik: TextView
    private lateinit var txtZaman: TextView
    private lateinit var btnDetay: Button

    private lateinit var searchViewMap: SearchView // Arama çubuğu değişkeni

    private lateinit var btnBackContainer: CardView


    // Konum seçimi için gerekli değişkenler
    private lateinit var btnKonumSec: Button
    private var secilenLatLng: LatLng? = null
    private var isSelectionMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        db = FirebaseFirestore.getInstance() // Firestore servisi başlatıldı.
        // XML elemanlarını bağlama işlemi.
        infoCard = findViewById(R.id.infoCard)
        txtTur = findViewById(R.id.txtTur)
        txtBaslik = findViewById(R.id.txtBaslik)
        txtZaman = findViewById(R.id.txtZaman)
        btnDetay = findViewById(R.id.btnDetay)
        btnKonumSec = findViewById(R.id.btnKonumSec)
        searchViewMap = findViewById(R.id.searchViewMap)

        // Geri butonu tanımlama ve tıklama olayı.
        btnBackContainer = findViewById(R.id.btnBackContainer)
        btnBackContainer.setOnClickListener {
            finish() // Sayfayı kapatıp geri döner
        }

        // --- Konum Seçme Mantığı ---

        // Diğer sayfadan "selection" modu ile gelinip gelinmediğinin kontrol etme.
        val mode = intent.getStringExtra("mode")
        if (mode == "selection") {
            isSelectionMode = true
            Toast.makeText(this, "Lütfen haritada bir yere tıklayın", Toast.LENGTH_LONG).show()
        }

        // Konum onay butonuna tıklandığında yapılacak işlemler.
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
                    searchLocation(query) // Girilen konumu arayan fonksiyonu çağırır.
                    searchViewMap.clearFocus() // Arama işlemi bitince  klavyeyi kapatır.
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        // Harita bileşenini yükleme.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Kampüs merkezi koordinatları (Varsayılan odak noktası).
        val campusCenter = LatLng(39.89953921087502, 41.244187083657714)

        // Harita tamamen yüklendiğinde kamerayı kampüs merkezine odakla.
        map.setOnMapLoadedCallback {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(campusCenter, 15f)) //Geçişi yumuşatmak için moveCamera() methodu yerine animateCamera() metodu kullanılmıştır.
        }

        // Eğer seçim modunda değilse, veritabanındaki kayıtlı pinleri getir.
        if (!isSelectionMode) {
            fetchReportsFromFirestore() // Sadece veritabanındaki gerçek harita verilerini getir
        }

        // Mevcut pinlere tıklama olayı.
        map.setOnMarkerClickListener { marker ->
            if (!isSelectionMode) {
                val secilenBildirim = marker.tag as? HaritaBildirim
                if (secilenBildirim != null) {
                    kartGoster(secilenBildirim)
                }
            }
            false
        }

        // Harita üzerinde boş bir yere tıklama olayı. (Elle seçim yapma kısmı)
        map.setOnMapClickListener { latLng ->
            if (isSelectionMode) {
                // Tıklanan konumu seçmek için konumSecimIslemi fonksiyonu kullanır.
                konumSecimIslemi(latLng, "Seçilen Konum")
            } else {
                // Normal modda haritada boş bir yere tıklanırsa bilgi kartını gizler.
                infoCard.visibility = View.GONE
            }
        }
    }

    /*
      Metin tabanlı konum araması yapar ve haritayı o konuma taşır.
     */
    private fun searchLocation(locationName: String) {
        val geocoder = Geocoder(this, Locale.getDefault()) //Arama çubuğu için gerekli servisi başlatır.
        try {
            val addressList: List<Address>? = geocoder.getFromLocationName(locationName, 1) //getFromLocationName eski yöntem daha sonrasında güncellenmesi lazım

            //Arama çubuğunda konum bulunduysa olacaklar:
            if (!addressList.isNullOrEmpty()) {
                val address = addressList[0] //Bulunan ilk adres
                val latLng = LatLng(address.latitude, address.longitude)

                // Kamerayı bulunan konuma taşır.
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

                // Seçim modundaysak bulunan konuma yeşil pin ekler.
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

    /*
      Harita üzerinde tıklanan veya aranan yeri işaretler.
     */
    private fun konumSecimIslemi(latLng: LatLng, title: String) {
        map.clear() // Eski pinleri siler.
        //Seçilen yeşil konum iconunu seçilen yerde gösterme:
        map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
        secilenLatLng = latLng // Seçilen koordinat değerlerini saklar.
        btnKonumSec.visibility = View.VISIBLE // Onay butonunu görünür yapar.
    }

    /*
      Firestore'dan gelen verileri haritaya pin olarak ekler.
     */
    private fun pinEkle(bildirim: HaritaBildirim) {
        val konum = LatLng(bildirim.lat, bildirim.lng)
        val renk = when (bildirim.tur) {
            "Tehlike" -> BitmapDescriptorFactory.HUE_RED
            "Bilgi" -> BitmapDescriptorFactory.HUE_BLUE
            "Teknik" -> BitmapDescriptorFactory.HUE_ORANGE
            else -> BitmapDescriptorFactory.HUE_VIOLET
        }

        val marker = map.addMarker(
            MarkerOptions()
                .position(konum)
                .title(bildirim.baslik)
                .icon(BitmapDescriptorFactory.defaultMarker(renk))
        )
        marker?.tag = bildirim
    }

    /*
      Haritadaki bir pine tıklandığında alt bilgi kartını doldurur ve gösterir.
     */
    private fun kartGoster(bildirim: HaritaBildirim) {
        txtBaslik.text = bildirim.baslik
        txtTur.text = bildirim.tur.uppercase()
        txtZaman.text = bildirim.zaman

        // Bildirim türüne göre metin rengini ayarlar.
        when(bildirim.tur) {
            "Tehlike" -> txtTur.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            "Bilgi" -> txtTur.setTextColor(resources.getColor(android.R.color.holo_blue_dark))
            else -> txtTur.setTextColor(resources.getColor(android.R.color.darker_gray))
        }

        // Detay butonuna tıklanınca detay sayfasına veri taşır.
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
            .whereEqualTo("status", "Açık") // Sadece aktif olan bildirimleri filtreler ve getirir.
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
                            pinEkle(bildirim) // Oluşturulan nesneyi haritaya ekler.
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

