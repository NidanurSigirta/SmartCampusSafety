package com.nidanursigirta.smartcampussafety

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nidanursigirta.smartcampussafety.databinding.ActivityFollowedReportsBinding

class FollowedReportsActivity_ : AppCompatActivity() {

    private lateinit var binding: ActivityFollowedReportsBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var reportList: ArrayList<Report>
    private lateinit var adapter: FollowedReportsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowedReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        binding.recyclerViewFollowed.layoutManager = LinearLayoutManager(this)
        reportList = arrayListOf()

        adapter = FollowedReportsAdapter(reportList) { report ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("REPORT_ID", report.reportId)
            startActivity(intent)
        }
        binding.recyclerViewFollowed.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        fetchFollowedReports()
    }

    private fun fetchFollowedReports() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).collection("followed_reports")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    reportList.clear()
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                val viewedTimestamps = HashMap<String, Timestamp>()
                for (doc in documents) {
                    val ts = doc.getTimestamp("lastViewedTimestamp")
                    if (ts != null) viewedTimestamps[doc.id] = ts
                    else viewedTimestamps[doc.id] = Timestamp(0, 0)
                }

                reportList.clear()
                adapter.notifyDataSetChanged()

                for (doc in documents) {
                    val reportId = doc.id

                    db.collection("reports").document(reportId).get().addOnSuccessListener { reportDoc ->
                        val report = reportDoc.toObject(Report::class.java)

                        if (report != null) {
                            val lastUpdate = report.lastUpdateTimestamp
                            val lastViewed = viewedTimestamps[report.reportId]

                            // --- TEMİZLEME MANTIĞI ---
                            // Eğer ben gördükten sonra yeni bir güncelleme olmadıysa
                            // mesajları yerel listede temizle (Veritabanından silmiyoruz, sadece göstermiyoruz)
                            if (lastUpdate == null || (lastViewed != null && lastViewed >= lastUpdate)) {
                                report.statusUpdateMessage = ""
                                report.descUpdateMessage = ""
                            }

                            reportList.add(report)
                            reportList.sortByDescending { it.timestamp }
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }
    }
}