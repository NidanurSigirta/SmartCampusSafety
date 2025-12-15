package com.nidanursigirta.smartcampussafety

import com.google.firebase.Timestamp

// Firestore'dan veri çekerken kullanacağımız şablon sınıfı (Data Class)
// Bu değişken isimleri Firestore'daki alan isimleriyle AYNI olmalıdır.
data class Report(
    var reportId: String = "",
    var title: String = "",
    var description: String = "",
    var type: String = "", // Sağlık, Güvenlik, Teknik Arıza vb.
    var status: String = "Açık", // Varsayılan: Açık, İnceleniyor, Çözüldü
    var timestamp: Timestamp? = null, // Oluşturulma zamanı (Firebase Timestamp türünde)
    var creatorId: String = "",
    var imageUrl: String = "", // İsteğe bağlı fotoğraf URL'si
    val latitude: Double = 0.0,  // x ekseni korinatı
    val longitude: Double = 0.0  // y ekseni korinatı
){
    // Firestore'un veriyi okurken hata vermemesi için bu boş constructor(yapıcı) oluşturuldu
    constructor() : this("", "", "", "", "", null, "", "", 0.0, 0.0)
}