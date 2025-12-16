package com.nidanursigirta.smartcampussafety

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
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

    private var currentFilterTitle = "Tümü"
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        checkUserRole()

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.setHasFixedSize(true)

        fullList = arrayListOf()
        adapterList = arrayListOf()

        reportAdapter = ReportAdapter(adapterList)
        binding.recyclerView.adapter = reportAdapter

        getReports()
        // YENİ: Acil Duyuruları Dinle
        listenForEmergencyAnnouncements()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterBySearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnMapGecis.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        binding.ivFilter.setOnClickListener {
            showFilterMenu()
        }

        binding.fabAddReport.setOnClickListener {
            val intent = Intent(this, AddReportActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        checkForNotifications()
    }

    // --- ACİL DUYURU DİNLEME ---
    private fun listenForEmergencyAnnouncements() {
        // En son atılan duyuruyu al
        db.collection("announcements")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                if (value != null && !value.isEmpty) {
                    val doc = value.documents[0]
                    val message = doc.getString("message") ?: ""

                    if (message.isNotEmpty()) {
                        binding.cardEmergency.visibility = View.VISIBLE
                        binding.tvEmergencyMessage.text = message

                        // Tıklayınca detaylı oku (Popup)
                        binding.cardEmergency.setOnClickListener {
                            AlertDialog.Builder(this)
                                .setTitle("🚨 ACİL DUYURU")
                                .setMessage(message)
                                .setPositiveButton("Tamam", null)
                                .show()
                        }
                    } else {
                        binding.cardEmergency.visibility = View.GONE
                    }
                } else {
                    binding.cardEmergency.visibility = View.GONE
                }
            }
    }

    private fun checkForNotifications() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("followed_reports")
            .get().addOnSuccessListener { documents ->
                if (documents.size() == 0) {
                    binding.viewHomeNotificationBadge.visibility = View.GONE
                    return@addOnSuccessListener
                }
                var hasUnread = false
                val totalDocs = documents.size()
                var checkedDocs = 0
                for (doc in documents) {
                    val reportId = doc.id
                    val lastViewed = doc.getTimestamp("lastViewedTimestamp") ?: Timestamp(0, 0)
                    db.collection("reports").document(reportId).get().addOnSuccessListener { reportDoc ->
                        if (reportDoc.exists()) {
                            val lastUpdate = reportDoc.getTimestamp("lastUpdateTimestamp")
                            if (lastUpdate != null && lastUpdate > lastViewed) {
                                hasUnread = true
                            }
                        }
                        checkedDocs++
                        if (checkedDocs == totalDocs) {
                            if (hasUnread) binding.viewHomeNotificationBadge.visibility = View.VISIBLE
                            else binding.viewHomeNotificationBadge.visibility = View.GONE
                        }
                    }
                }
            }
    }

    private fun checkUserRole() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val role = document.getString("role")
                if (role == "ADMIN") {
                    isAdmin = true
                }
            }
        }
    }

    private fun getReports() {
        db.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                if (value != null) {
                    fullList.clear()
                    for (document in value.documents) {
                        val report = document.toObject(Report::class.java)
                        if (report != null) {
                            fullList.add(report)
                        }
                    }
                    applyCurrentFilter()
                }
            }
    }

    private fun applyCurrentFilter() {
        when (currentFilterTitle) {
            "Tümü" -> reportAdapter.updateList(fullList)
            "Açık Olanlar" -> {
                val filtered = fullList.filter { it.status == "Açık" } as ArrayList<Report>
                reportAdapter.updateList(filtered)
            }
            "Tür: Sağlık" -> reportAdapter.updateList(fullList.filter { it.type == "Sağlık" } as ArrayList<Report>)
            "Tür: Güvenlik" -> reportAdapter.updateList(fullList.filter { it.type == "Güvenlik" } as ArrayList<Report>)
            "Tür: Teknik Arıza" -> reportAdapter.updateList(fullList.filter { it.type == "Teknik Arıza" } as ArrayList<Report>)
            "Tür: Kayıp-Buluntu" -> reportAdapter.updateList(fullList.filter { it.type == "Kayıp-Buluntu" } as ArrayList<Report>)
            "Tür: Çevre" -> reportAdapter.updateList(fullList.filter { it.type == "Çevre" } as ArrayList<Report>)
            else -> reportAdapter.updateList(fullList)
        }
    }

    private fun filterBySearch(text: String) {
        val filteredList = ArrayList<Report>()
        for (item in fullList) {
            if (item.title.lowercase(Locale.getDefault()).contains(text.lowercase(Locale.getDefault())) ||
                item.description.lowercase(Locale.getDefault()).contains(text.lowercase(Locale.getDefault()))) {
                filteredList.add(item)
            }
        }
        reportAdapter.updateList(filteredList)
    }

    private fun showFilterMenu() {
        val popup = PopupMenu(this, binding.ivFilter)
        fun getTitle(title: String): String = if (currentFilterTitle == title) "✓ $title" else title

        if (isAdmin) popup.menu.add("★ Admin Paneli")
        popup.menu.add(getTitle("Tümü"))
        popup.menu.add(getTitle("Açık Olanlar"))
        popup.menu.add(getTitle("Tür: Sağlık"))
        popup.menu.add(getTitle("Tür: Güvenlik"))
        popup.menu.add(getTitle("Tür: Teknik Arıza"))
        popup.menu.add(getTitle("Tür: Kayıp-Buluntu"))
        popup.menu.add(getTitle("Tür: Çevre"))

        popup.setOnMenuItemClickListener { item ->
            val rawTitle = item.title.toString().replace("✓ ", "")
            if (rawTitle == "★ Admin Paneli") {
                val intent = Intent(this, AdminPanelActivity::class.java)
                startActivity(intent)
                return@setOnMenuItemClickListener true
            }
            currentFilterTitle = rawTitle
            applyCurrentFilter()
            true
        }
        popup.show()
    }
}