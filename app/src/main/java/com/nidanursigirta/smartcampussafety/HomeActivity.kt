package com.nidanursigirta.smartcampussafety

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nidanursigirta.smartcampussafety.databinding.ActivityHomeBinding
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var fullList: ArrayList<Report>
    private lateinit var adapterList: ArrayList<Report>

    private lateinit var reportAdapter: ReportAdapter

    // Seçili filtreyi hafızada tutmak için değişken
    private var currentFilterTitle = "Tümü"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.setHasFixedSize(true)

        fullList = arrayListOf()
        adapterList = arrayListOf()

        reportAdapter = ReportAdapter(adapterList)
        binding.recyclerView.adapter = reportAdapter

        // Verileri Çek
        getReports()

        // --- ARAMA İŞLEVİ ---
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterBySearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // --- PROFİL SAYFASI BUTONU ---
        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // --- HARİTA BUTONU (YENİ EKLENDİ) ---
        // ViewBinding kullandığın için findViewById'ye gerek yok, direkt binding ile erişiyoruz.
        binding.btnMapGecis.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        // --- FİLTRELEME BUTONU ---
        binding.ivFilter.setOnClickListener {
            showFilterMenu()
        }

        // Yeni Ekle Butonu
        binding.fabAddReport.setOnClickListener {
            val intent = Intent(this, AddReportActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getReports() {
        db.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(this, "Hata: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (value != null) {
                    fullList.clear()
                    for (document in value.documents) {
                        val report = document.toObject(Report::class.java)
                        if (report != null) {
                            fullList.add(report)
                        }
                    }
                    // Veri ilk geldiğinde mevcut filtreyi tekrar uygula
                    applyCurrentFilter()
                }
            }
    }

    // Seçili filtreyi listeye uygulama fonksiyonu
    private fun applyCurrentFilter() {
        when (currentFilterTitle) {
            "Tümü" -> reportAdapter.updateList(fullList)
            "Sadece Açık Olanlar" -> {
                val filtered = fullList.filter { it.status == "Açık" } as ArrayList<Report>
                reportAdapter.updateList(filtered)
            }
            "Tür: Sağlık" -> {
                val filtered = fullList.filter { it.type == "Sağlık" } as ArrayList<Report>
                reportAdapter.updateList(filtered)
            }
            "Tür: Güvenlik" -> {
                val filtered = fullList.filter { it.type == "Güvenlik" } as ArrayList<Report>
                reportAdapter.updateList(filtered)
            }
            "Tür: Teknik Arıza" -> {
                val filtered = fullList.filter { it.type == "Teknik Arıza" } as ArrayList<Report>
                reportAdapter.updateList(filtered)
            }
            else -> reportAdapter.updateList(fullList)
        }
    }

    private fun filterBySearch(text: String) {
        val filteredList = ArrayList<Report>()
        // Aramayı her zaman TÜM liste üzerinden yapıyoruz
        for (item in fullList) {
            if (item.title.lowercase(Locale.getDefault()).contains(text.lowercase(Locale.getDefault())) ||
                item.description.lowercase(Locale.getDefault()).contains(text.lowercase(Locale.getDefault()))) {
                filteredList.add(item)
            }
        }
        reportAdapter.updateList(filteredList)
    }

    // --- GÜNCELLENMİŞ FİLTRE MENÜSÜ ---
    private fun showFilterMenu() {
        val popup = PopupMenu(this, binding.ivFilter)

        // Yardımcı fonksiyon: Eğer bu başlık seçiliyse yanına "✓ " koy
        fun getTitle(title: String): String {
            return if (currentFilterTitle == title) "✓ $title" else title
        }

        // Menü seçeneklerini duruma göre işaretli ekle
        popup.menu.add(getTitle("Tümü"))
        popup.menu.add(getTitle("Sadece Açık Olanlar"))
        popup.menu.add(getTitle("Tür: Sağlık"))
        popup.menu.add(getTitle("Tür: Güvenlik"))
        popup.menu.add(getTitle("Tür: Teknik Arıza"))
        popup.menu.add("Çıkış Yap")

        popup.setOnMenuItemClickListener { item ->
            // Tıklanan öğenin başlığındaki "✓ " işaretini temizle ki orijinal metni bulalım
            val rawTitle = item.title.toString().replace("✓ ", "")

            if (rawTitle == "Çıkış Yap") {
                auth.signOut()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return@setOnMenuItemClickListener true
            }

            // Yeni filtreyi kaydet ve uygula
            currentFilterTitle = rawTitle
            applyCurrentFilter()

            true
        }
        popup.show()
    }
}