package com.nidanursigirta.smartcampussafety

import com.google.firebase.Timestamp
import java.io.Serializable

data class Report(
    var reportId: String = "",
    var title: String = "",
    var description: String = "",
    var type: String = "",
    var status: String = "Açık",
    var timestamp: Timestamp? = null,
    var creatorId: String = "",
    var imageUrl: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,

    // YENİ YAPI: Mesajları ayırdık
    var statusUpdateMessage: String = "", // Sadece durumla ilgili son mesaj
    var descUpdateMessage: String = "",   // Sadece açıklamayla ilgili son mesaj
    var lastUpdateTimestamp: Timestamp? = null
) : Serializable
{
    // Firestore'un veriyi okurken hata vermemesi için bu boş constructor(yapıcı) oluşturuldu
    constructor() : this("", "", "", "", "", null, "", "", 0.0, 0.0)
}