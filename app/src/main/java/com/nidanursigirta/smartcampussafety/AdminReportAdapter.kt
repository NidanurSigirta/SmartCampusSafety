package com.nidanursigirta.smartcampussafety

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// onViewDetailClick: "Bildirimi Görüntüle" butonuna basıldığında çalışır
class AdminReportAdapter(
    private val reportList: ArrayList<Report>,
    private val onViewDetailClick: (Report) -> Unit
) : RecyclerView.Adapter<AdminReportAdapter.AdminViewHolder>() {

    class AdminViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvAdminReportTitle)
        val tvType: TextView = itemView.findViewById(R.id.tvAdminReportType)
        val tvStatus: TextView = itemView.findViewById(R.id.tvAdminReportStatus)
        val btnChangeStatus: Button = itemView.findViewById(R.id.btnChangeStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_report, parent, false)
        return AdminViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        val currentItem = reportList[position]

        holder.tvTitle.text = currentItem.title
        holder.tvType.text = "Tür: ${currentItem.type}"
        holder.tvStatus.text = "Durum: ${currentItem.status}"

        // --- İSTEK: Butonun Adını Değiştir ---
        holder.btnChangeStatus.text = "Bildirimi Görüntüle"

        // Renklendirme
        when (currentItem.status) {
            "Açık" -> holder.tvStatus.setTextColor(Color.RED)
            "İnceleniyor" -> holder.tvStatus.setTextColor(Color.parseColor("#FFA500"))
            "Çözüldü" -> holder.tvStatus.setTextColor(Color.parseColor("#008000"))
        }

        // --- İSTEK: Mavi Butona Basınca Detay Aç ---
        holder.btnChangeStatus.setOnClickListener {
            onViewDetailClick(currentItem)
        }

        // Satırın kendisine tıklama özelliği iptal edildi (Sadece butona basılsın)
        holder.itemView.setOnClickListener(null)
    }

    override fun getItemCount(): Int {
        return reportList.size
    }
}