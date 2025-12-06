package com.nidanursigirta.smartcampussafety

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

// 1. Veri Modeli
data class HaritaBildirim(
    val baslik: String,
    val tur: String,
    val zaman: String,
    val lat: Double,
    val lng: Double
)

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    // Arayüz elemanlarını tanımlayalım
    private lateinit var infoCard: CardView
    private lateinit var txtTur: TextView
    private lateinit var txtBaslik: TextView
    private lateinit var txtZaman: TextView
    private lateinit var btnDetay: Button

    // --- EKLENEN: Geri Butonu Değişkeni ---
    private lateinit var btnBackContainer: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // XML elemanlarını bağlayalım
        infoCard = findViewById(R.id.infoCard)
        txtTur = findViewById(R.id.txtTur)
        txtBaslik = findViewById(R.id.txtBaslik)
        txtZaman = findViewById(R.id.txtZaman)
        btnDetay = findViewById(R.id.btnDetay)

        // --- EKLENEN: Geri Butonu Tanımlama ve Tıklama Olayı ---
        btnBackContainer = findViewById(R.id.btnBackContainer)
        btnBackContainer.setOnClickListener {
            finish() // Sayfayı kapatıp geri döner
        }
        // -------------------------------------------------------

        // Haritayı yükleyelim
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // Harita hazır olduğunda bu fonksiyon çalışır
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // ÖRNEK VERİLER
        val bildirimListesi = listOf(
            HaritaBildirim("Kütüphane Girişi Kaygan", "Tehlike", "10 dk önce", 39.9208, 32.8541),
            HaritaBildirim("Kafeterya Çok Dolu", "Bilgi", "30 dk önce", 39.9215, 32.8530),
            HaritaBildirim("Asansör Arızalı", "Teknik", "2 saat önce", 39.9220, 32.8550)
        )

        // Verileri haritaya pin olarak ekle
        for (bildirim in bildirimListesi) {
            pinEkle(bildirim)
        }

        // Harita ilk açıldığında ilk bildirime odaklansın
        val ilkKonum = LatLng(bildirimListesi[0].lat, bildirimListesi[0].lng)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ilkKonum, 15f))

        // PİN TIKLAMA OLAYI
        mMap.setOnMarkerClickListener { marker ->
            val secilenBildirim = marker.tag as? HaritaBildirim
            if (secilenBildirim != null) {
                kartGoster(secilenBildirim)
            }
            false
        }

        // HARİTADA BOŞ YERE TIKLAMA OLAYI
        mMap.setOnMapClickListener {
            infoCard.visibility = View.GONE
        }
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
            Toast.makeText(this, "${bildirim.baslik} detayına gidiliyor...", Toast.LENGTH_SHORT).show()
        }

        infoCard.visibility = View.VISIBLE
    }
}