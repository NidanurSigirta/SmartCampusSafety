package com.nidanursigirta.smartcampussafety

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class ReportAdapter(var reportList: ArrayList<Report>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvReportTitle)
        val tvDesc: TextView = itemView.findViewById(R.id.tvReportDesc)
        val tvStatus: TextView = itemView.findViewById(R.id.tvReportStatus)
        val tvDate: TextView = itemView.findViewById(R.id.tvReportDate)
        val ivIcon: ImageView = itemView.findViewById(R.id.ivReportIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reportList[position]

        holder.tvTitle.text = report.title
        holder.tvDesc.text = report.description
        holder.tvStatus.text = report.status

        // Tarih Formatlama
        if (report.timestamp != null) {
            val sdf = SimpleDateFormat("dd MMM HH:mm", Locale("tr", "TR"))
            holder.tvDate.text = sdf.format(report.timestamp!!.toDate())
        } else {
            holder.tvDate.text = "Tarih yok"
        }

        // Durum Rengi
        when (report.status) {
            "Açık" -> holder.tvStatus.setTextColor(Color.parseColor("#F44336")) // Kırmızı
            "İnceleniyor" -> holder.tvStatus.setTextColor(Color.parseColor("#FF9800")) // Turuncu
            "Çözüldü" -> holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")) // Yeşil
            else -> holder.tvStatus.setTextColor(Color.BLACK)
        }

        // --- İKON RENGİ VE TÜRÜ ---
        // İkonların soluk görünmemesi için onlara canlı renkler atıyoruz (Tint)
        when (report.type) {
            "Sağlık" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_health)
                holder.ivIcon.setColorFilter(Color.parseColor("#E91E63")) // Canlı Pembe/Kırmızı
            }
            "Güvenlik" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_security)
                holder.ivIcon.setColorFilter(Color.parseColor("#2196F3")) // Canlı Mavi
            }
            "Teknik Arıza" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_repair)
                holder.ivIcon.setColorFilter(Color.parseColor("#FF9800")) // Turuncu
            }
            "Çevre" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_nature)
                holder.ivIcon.setColorFilter(Color.parseColor("#4CAF50")) // Yeşil
            }
            "Kayıp-Buluntu" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_lost)
                holder.ivIcon.setColorFilter(Color.parseColor("#9C27B0")) // Mor
            }
            else -> {
                holder.ivIcon.setImageResource(R.drawable.ic_default)
                holder.ivIcon.setColorFilter(Color.parseColor("#607D8B")) // Gri
            }
        }
    }

    override fun getItemCount(): Int {
        return reportList.size
    }

    fun updateList(newList: ArrayList<Report>) {
        reportList = newList
        notifyDataSetChanged()
    }
}