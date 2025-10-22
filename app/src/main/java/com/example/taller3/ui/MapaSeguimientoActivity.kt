package com.example.taller3.ui

import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.taller3.R
import com.example.taller3.data.Usuario
import com.example.taller3.databinding.ActivityMapaSeguimientoBinding
import com.google.android.gms.location.*
import com.google.firebase.database.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class MapaSeguimientoActivity : AppCompatActivity() {

    private lateinit var b: ActivityMapaSeguimientoBinding
    private lateinit var map: org.osmdroid.views.MapView
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var seguidoUid: String
    private lateinit var markerSeguido: Marker
    private val listaUpdates = mutableListOf<ValueEventListener>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMapaSeguimientoBinding.inflate(layoutInflater)
        setContentView(b.root)

        seguidoUid = intent.getStringExtra("uid_seguido") ?: return finish()

        map = b.map
        map.setMultiTouchControls(true)
        map.controller.setZoom(14.0)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        startUserLocationUpdates()
        startSeguimiento()
    }

    private fun startUserLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    val point = GeoPoint(it.latitude, it.longitude)
                    map.controller.setCenter(point)
                    // marker de mi propia ubicación si quieres
                }
            }
        }

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun startSeguimiento() {
        val ref = FirebaseDatabase.getInstance().reference.child("t3_users").child(seguidoUid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usuario = snapshot.getValue(Usuario::class.java) ?: return
                val point = GeoPoint(usuario.lat, usuario.lng)

                if (!::markerSeguido.isInitialized) {
                    markerSeguido = Marker(map).apply {
                        position = point
                        title = usuario.nombre
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = ContextCompat.getDrawable(this@MapaSeguimientoActivity, R.drawable.ic_user_placeholder)
                    }
                    map.overlays.add(markerSeguido)
                } else {
                    markerSeguido.position = point
                }

                // distancia a mi ubicación
                fusedClient.lastLocation.addOnSuccessListener { loc ->
                    val distancia = FloatArray(1)
                    Location.distanceBetween(
                        loc.latitude, loc.longitude,
                        usuario.lat, usuario.lng,
                        distancia
                    )
                    b.tvDistancia.text = "Distancia: ${distancia[0].toInt()} m"
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        listaUpdates.add(listener)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::fusedClient.isInitialized && ::locationCallback.isInitialized) {
            fusedClient.removeLocationUpdates(locationCallback)
        }
        listaUpdates.forEach { listener ->
            FirebaseDatabase.getInstance().reference.child("t3_users")
                .child(seguidoUid).removeEventListener(listener)
        }
    }
}
