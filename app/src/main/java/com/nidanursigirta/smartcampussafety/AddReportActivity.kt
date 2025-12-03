package com.nidanursigirta.smartcampussafety

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nidanursigirta.smartcampussafety.databinding.ActivityAddReportBinding
import java.util.Date

class AddReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddReportBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // 1. Geri Tuşu
        binding.btnBack.setOnClickListener { finish() }

        // 2. Spinner (Tür Seçimi)
        // İlk eleman "Hint" metnidir.
        val types = arrayOf("Bildirim Türü Seçiniz", "Sağlık", "Güvenlik", "Teknik Arıza", "Kayıp-Buluntu", "Çevre")

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, types) {

            // getDropDownView: Listeyi açtığında görünecekler (DÜZELTİLDİ)
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                // Her zaman super'den gelen view'ı alıyoruz, böylece layout özellikleri bozulmaz
                val view = super.getDropDownView(position, convertView, parent) as TextView

                if (position == 0) {
                    // Eğer 0. eleman ise (Hint), boyutunu sıfırla ve gizle
                    view.visibility = View.GONE
                    view.layoutParams = ViewGroup.LayoutParams(0, 0)
                } else {
                    // Diğer elemanları normal göster
                    view.visibility = View.VISIBLE
                    // Genişlik ve yükseklik ayarını standart yap
                    view.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    view.setTextColor(Color.BLACK)
                    view.setPadding(30, 30, 30, 30) // Listede elemanlar arası boşluk
                }
                return view
            }

            // getView: Liste kapalıyken kutunun içinde görünen
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView

                // Padding'i sıfırlıyoruz ki XML'deki padding geçerli olsun
                view.setPadding(0, 0, 0, 0)

                if (position == 0) {
                    // "Seçiniz" yazısı -> Standart EditText Hint Rengi (Gri) (#808080)
                    view.setTextColor(Color.parseColor("#808080"))
                    view.textSize = 14f
                } else {
                    // Seçim yapıldıysa -> SİYAH (Normal metin gibi)
                    view.setTextColor(Color.BLACK)
                    view.textSize = 14f
                }
                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter

        // 3. Butonlar (Toast Mesajı)
        binding.btnAddLocation.setOnClickListener {
            Toast.makeText(this, "Harita sonra eklenecek", Toast.LENGTH_SHORT).show()
        }
        binding.btnAddPhoto.setOnClickListener {
            Toast.makeText(this, "Kamera sonra eklenecek", Toast.LENGTH_SHORT).show()
        }

        // 4. Gönder Butonu
        binding.btnSubmitReport.setOnClickListener {
            val title = binding.etReportTitle.text.toString().trim()
            val desc = binding.etReportDesc.text.toString().trim()
            val type = binding.spinnerType.selectedItem.toString()

            // Eğer hala 0. eleman ("Bildirim Türü Seçiniz") seçiliyse hata ver
            if (binding.spinnerType.selectedItemPosition == 0) {
                Toast.makeText(this, "Lütfen bir bildirim türü seçiniz!", Toast.LENGTH_SHORT).show()
                binding.spinnerType.performClick() // Listeyi otomatik aç
                return@setOnClickListener
            }

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Başlık ve açıklama zorunludur", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveReportToFirestore(title, desc, type)
        }
    }

    private fun saveReportToFirestore(title: String, desc: String, type: String) {
        val user = auth.currentUser ?: return
        val newReportRef = db.collection("reports").document()

        val report = hashMapOf(
            "reportId" to newReportRef.id,
            "title" to title,
            "description" to desc,
            "type" to type,
            "status" to "Açık",
            "timestamp" to Timestamp(Date()),
            "creatorId" to user.uid,
            "imageUrl" to ""
        )

        newReportRef.set(report)
            .addOnSuccessListener {
                Toast.makeText(this, "Bildirim gönderildi!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Hata: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}