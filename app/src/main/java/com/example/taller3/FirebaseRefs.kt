package com.example.taller3.data

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

object FirebaseRefs {
    fun users(): DatabaseReference = Firebase.database.getReference("t3_users")
}
