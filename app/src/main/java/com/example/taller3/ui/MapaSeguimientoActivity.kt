package com.example.taller3.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.R
import com.example.taller3.data.FirebaseRefs
import com.google.android.gms.location.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.google.firebase.database.DatabaseReference


class MapaSeguimientoActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private var markerSeguido: Marker? = null
    private var markerYo: Marker? = null
    private var miPosicion: GeoPoint? = null
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var seguimientoRefListener: ValueEventListener
    private var seguimientoRef: DatabaseReference? = null
    private var seguimientoRefPath: String? = null

    private lateinit var tvDistancia: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_mapa_seguimiento)

        map = findViewById(R.id.map)
        tvDistancia = findViewById(R.id.tvDistancia)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        // Permisos
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                3001
            )
        } else {
            startLocationUpdates()
        }

        val uidUsuario = intent.getStringExtra("uidUsuario") ?: run {
            Toast.makeText(this, "UID no recibido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val nombre = intent.getStringExtra("nombreUsuario") ?: "Usuario"

        title = "Siguiendo: $nombre"

        // Escuchar cambios en el perfil del usuario seguido dentro de t3_users (lat,lng)
        val ref = FirebaseRefs.users().child(uidUsuario)
        seguimientoRef = ref
        ref.addValueEventListener(seguimientoRefListener)

        seguimientoRefListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("lat").getValue(Double::class.java)
                val lng = snapshot.child("lng").getValue(Double::class.java)
                if (lat != null && lng != null) {
                    val punto = GeoPoint(lat, lng)
                    runOnUiThread {
                        if (markerSeguido == null) {
                            markerSeguido = Marker(map)
                            markerSeguido!!.title = nombre
                            markerSeguido!!.position = punto
                            map.overlays.add(markerSeguido)
                        } else {
                            markerSeguido!!.position = punto
                        }
                        map.controller.setCenter(punto)
                        actualizarDistancia(punto)
                        map.invalidate()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // no-op
            }
        }
        ref.addValueEventListener(seguimientoRefListener)
        FirebaseRefs.users().root.child(seguimientoRefPath!!).removeEventListener(seguimientoRefListener)
    }

    private fun actualizarDistancia(posSeguido: GeoPoint) {
        miPosicion?.let { mi ->
            val metros = mi.distanceToAsDouble(posSeguido)
            tvDistancia.text = "Distancia: %.1f m".format(metros)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED
    }

    // Pedimos actualizaciones precisas para obtener mi ubicación y calcular distancia en tiempo real
    private lateinit var locationCallback: LocationCallback
    private fun startLocationUpdates() {
        try {
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).build()
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val l = result.lastLocation ?: return
                    miPosicion = GeoPoint(l.latitude, l.longitude)
                    runOnUiThread {
                        if (markerYo == null) {
                            markerYo = Marker(map)
                            markerYo!!.title = "Tú"
                            markerYo!!.position = miPosicion
                            map.overlays.add(markerYo)
                        } else {
                            markerYo!!.position = miPosicion
                        }
                        actualizarDistancia(markerSeguido?.position ?: return@runOnUiThread)
                        map.invalidate()
                    }
                }
            }
            fusedClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 3001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::fusedClient.isInitialized && ::locationCallback.isInitialized) {
                fusedClient.removeLocationUpdates(locationCallback)
            }
        } catch (e: Exception) {}

        // Eliminar listener Firebase correctamente
        try {
            seguimientoRef?.removeEventListener(seguimientoRefListener)
        } catch (e: Exception) {}
    }

}
