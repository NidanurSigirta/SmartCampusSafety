package com.nidanursigirta.smartcampussafety

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nidanursigirta.smartcampussafety.databinding.ActivityFollowedReportsBinding

class FollowedReportsActivity_ : AppCompatActivity() {

    private lateinit var binding: ActivityFollowedReportsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var adapter: ReportAdapter
    private var followedList = ArrayList<Report>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowedReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Geri butonu
        binding.btnBackFollowed.setOnClickListener { finish() }

        // RecyclerView
        adapter = ReportAdapter(followedList)
        binding.recyclerViewFollowed.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewFollowed.adapter = adapter

        // Takip edilen raporları yükle
        loadFollowedReports()
    }

    private fun loadFollowedReports() {
        val uid = auth.currentUser?.uid ?: return

        // 1) Kullanıcının takip ettiği rapor ID'lerini çek
        db.collection("users").document(uid)
            .collection("followed_reports")
            .get()
            .addOnSuccessListener { ids ->
                if (ids.isEmpty) {
                    Toast.makeText(this, "Takip edilen bildirimin yok.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val reportIds = ids.documents.map { it.id }

                // 2) Her ID için raporu getir
                followedList.clear()

                for (id in reportIds) {
                    db.collection("reports").document(id).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                val report = doc.toObject(Report::class.java)
                                if (report != null) {
                                    followedList.add(report)
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Takip edilenler yüklenemedi.", Toast.LENGTH_SHORT).show()
            }
    }
}
