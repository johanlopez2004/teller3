package com.example.taller3.data

data class Usuario(
    val uid: String = "",
    val nombre: String = "",
    val photoUrl: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val estado: String = "no disponible",
    val disponible: Boolean = false
)
