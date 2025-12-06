package com.nidanursigirta.smartcampussafety

import android.content.Intent // 1. EKLENEN KÜTÜPHANE
import android.graphics.Color
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

        // İkon Rengi ve Türü
        when (report.type) {
            "Sağlık" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_health)
                holder.ivIcon.setColorFilter(Color.parseColor("#E91E63"))
            }
            "Güvenlik" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_security)
                holder.ivIcon.setColorFilter(Color.parseColor("#2196F3"))
            }
            "Teknik Arıza" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_repair)
                holder.ivIcon.setColorFilter(Color.parseColor("#FF9800"))
            }
            "Çevre" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_nature)
                holder.ivIcon.setColorFilter(Color.parseColor("#4CAF50"))
            }
            "Kayıp-Buluntu" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_lost)
                holder.ivIcon.setColorFilter(Color.parseColor("#9C27B0"))
            }
            else -> {
                holder.ivIcon.setImageResource(R.drawable.ic_default)
                holder.ivIcon.setColorFilter(Color.parseColor("#607D8B"))
            }
        }

        // --- 2. EKLENEN KISIM: TIKLAMA OLAYI ---
        holder.itemView.setOnClickListener {
            // Tıklanan raporun ID'sini alıp Detay sayfasına gönderiyoruz
            val context = holder.itemView.context
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("REPORT_ID", report.reportId) // ID'yi taşıyoruz
            context.startActivity(intent)
        }
        // ---------------------------------------
    }

    override fun getItemCount(): Int {
        return reportList.size
    }

    fun updateList(newList: ArrayList<Report>) {
        reportList = newList
        notifyDataSetChanged()
    }
}