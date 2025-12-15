package com.nidanursigirta.smartcampussafety

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// onStatusClick: Bir butona basıldığında Activity'deki fonksiyonu tetikler
class AdminReportAdapter(
    private val reportList: ArrayList<Report>,
    private val onStatusClick: (Report, View) -> Unit
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

        // Duruma göre renk değişimi (Görsellik puanı kazandırır)
        when (currentItem.status) {
            "Açık" -> holder.tvStatus.setTextColor(Color.RED)
            "İnceleniyor" -> holder.tvStatus.setTextColor(Color.parseColor("#FFA500")) // Turuncu
            "Çözüldü" -> holder.tvStatus.setTextColor(Color.parseColor("#008000")) // Yeşil
        }

        // Butona tıklanınca bu raporu ve butonu(popup menü açmak için) gönder
        holder.btnChangeStatus.setOnClickListener {
            onStatusClick(currentItem, holder.btnChangeStatus)
        }
    }

    override fun getItemCount(): Int {
        return reportList.size
    }
}