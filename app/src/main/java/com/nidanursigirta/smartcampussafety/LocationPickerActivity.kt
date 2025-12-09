package com.nidanursigirta.smartcampussafety

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        // 1. XML'deki elemanları koda bağlama:
        searchView = findViewById(R.id.searchView)
        val btnConfirm = findViewById<Button>(R.id.btnConfirmLocation)

        // Haritayı başlatma:
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 2. Arama Kutusu Ayarları
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val location = searchView.query.toString()
                searchLocation(location)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        // 3. "Bu konumu seç" Butonu
        btnConfirm.setOnClickListener {
            if (selectedLocation != null) {
                val intent = Intent()
                intent.putExtra("lat", selectedLocation!!.latitude)
                intent.putExtra("lng", selectedLocation!!.longitude)
                setResult(RESULT_OK, intent)
                finish() // Sayfayı kapat
            } else {
                Toast.makeText(this, "Lütfen haritadan bir yer seçin!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // İzin Kontrolü
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        }

        // Başlangıç Konumu
        val baslangic = LatLng(39.9208, 32.8541)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(baslangic, 15f))

        // Haritaya Sorun Pinini Ekleme
        mMap.setOnMapClickListener { latLng ->
            addMarker(latLng, "Seçilen Konum")
        }
    }

    //İsim İle Konum Arama
    private fun searchLocation(locationName: String) {
        if (locationName.isEmpty()) return

        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addressList: List<Address>? = geocoder.getFromLocationName(locationName, 1)

            if (!addressList.isNullOrEmpty()) {
                val address = addressList[0]
                val latLng = LatLng(address.latitude, address.longitude)
                addMarker(latLng, locationName)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Toast.makeText(this, "Konum bulunamadı", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //Pin Ekleme Yardımıcısı
    private fun addMarker(latLng: LatLng, title: String) {
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(latLng).title(title))
        selectedLocation = latLng
    }
}