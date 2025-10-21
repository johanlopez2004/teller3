package com.example.taller3.data

import android.net.Uri
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth = Firebase.auth
    private val storage = Firebase.storage

    suspend fun registerEmailPassword(email: String, password: String) =
        auth.createUserWithEmailAndPassword(email, password).await()

    suspend fun loginEmailPassword(email: String, password: String) =
        auth.signInWithEmailAndPassword(email, password).await()

    fun currentUid(): String? = auth.currentUser?.uid

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = profile.uid.ifEmpty { currentUid() ?: error("No UID") }
        FirebaseRefs.users().child(uid).setValue(profile.copy(uid = uid)).await()
    }

    // Sube la imagen original (alta resolución) desde el Uri del Photo Picker
    suspend fun uploadProfilePhotoHighRes(uid: String, localImage: Uri): String {
        val ref = storage.getReference("t3_profile_photos/$uid.jpg")
        ref.putFile(localImage).await()
        return ref.downloadUrl.await().toString()
    }

    fun signOut() = auth.signOut()
}
