package com.nidanursigirta.smartcampussafety

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FollowedReportsAdapter(
    private val reportList: ArrayList<Report>,
    private val onItemClick: (Report) -> Unit
) : RecyclerView.Adapter<FollowedReportsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvFollowedTitle)
        val tvType: TextView = itemView.findViewById(R.id.tvFollowedType)
        val tvStatus: TextView = itemView.findViewById(R.id.tvFollowedStatus)
        val ivIcon: ImageView = itemView.findViewById(R.id.ivFollowedIcon)
        val viewDot: View = itemView.findViewById(R.id.viewUnreadDot)

        // İki ayrı kutu
        val layoutStatus: LinearLayout = itemView.findViewById(R.id.layoutStatusUpdate)
        val tvStatusMsg: TextView = itemView.findViewById(R.id.tvStatusMsg)

        val layoutDesc: LinearLayout = itemView.findViewById(R.id.layoutDescUpdate)
        val tvDescMsg: TextView = itemView.findViewById(R.id.tvDescMsg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_followed_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = reportList[position]

        holder.tvTitle.text = report.title
        holder.tvType.text = report.type
        holder.tvStatus.text = report.status.uppercase()

        // --- GÖRÜNÜRLÜK MANTIĞI ---
        var hasUnread = false

        // 1. Durum Mesajı Var mı?
        if (report.statusUpdateMessage.isNotEmpty()) {
            holder.layoutStatus.visibility = View.VISIBLE
            holder.tvStatusMsg.text = report.statusUpdateMessage
            hasUnread = true
        } else {
            holder.layoutStatus.visibility = View.GONE
        }

        // 2. Açıklama Mesajı Var mı?
        if (report.descUpdateMessage.isNotEmpty()) {
            holder.layoutDesc.visibility = View.VISIBLE
            holder.tvDescMsg.text = report.descUpdateMessage
            hasUnread = true
        } else {
            holder.layoutDesc.visibility = View.GONE
        }

        // Eğer herhangi biri varsa kırmızı noktayı yak
        if (hasUnread) {
            holder.viewDot.visibility = View.VISIBLE
        } else {
            holder.viewDot.visibility = View.GONE
        }

        // Renkler
        when (report.status) {
            "Açık" -> {
                holder.tvStatus.setTextColor(Color.parseColor("#D32F2F"))
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"))
            }
            "Çözüldü" -> {
                holder.tvStatus.setTextColor(Color.parseColor("#388E3C"))
                holder.tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"))
            }
            else -> {
                holder.tvStatus.setTextColor(Color.parseColor("#F57C00"))
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FFF3E0"))
            }
        }

        when (report.type) {
            "Sağlık" -> holder.ivIcon.setImageResource(R.drawable.ic_health)
            "Güvenlik" -> holder.ivIcon.setImageResource(R.drawable.ic_security)
            "Teknik Arıza" -> holder.ivIcon.setImageResource(R.drawable.ic_repair)
            "Çevre" -> holder.ivIcon.setImageResource(R.drawable.ic_nature)
            "Kayıp-Buluntu" -> holder.ivIcon.setImageResource(R.drawable.ic_lost)
            else -> holder.ivIcon.setImageResource(R.drawable.ic_default)
        }

        holder.itemView.setOnClickListener {
            onItemClick(report)
        }
    }

    override fun getItemCount(): Int = reportList.size
}