package com.example.taller3.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller3.R
import com.example.taller3.databinding.ActivityMainMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

class MainMapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var b: ActivityMainMapBinding
    private var disponible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        b = ActivityMainMapBinding.inflate(layoutInflater)
        setContentView(b.root)

        setSupportActionBar(b.toolbar)
        map = b.map
        map.setMultiTouchControls(true)
        map.controller.setZoom(14.0)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )
        } else {
            loadUserLocation()
        }

        loadJsonMarkers()
    }

    // ======== MENÚ SUPERIOR ========
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                Firebase.auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }

            R.id.action_status -> {
                toggleStatus(item)
                true
            }

            R.id.action_usuarios -> {
                startActivity(Intent(this, UsuariosDisponiblesActivity::class.java))
                true
            }


            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleStatus(item: MenuItem) {
        disponible = !disponible

        if (disponible) {
            item.title = "Estado: Disponible"
            item.icon = ContextCompat.getDrawable(this, android.R.drawable.presence_online)
            Toast.makeText(this, "Estás disponible", Toast.LENGTH_SHORT).show()
        } else {
            item.title = "Estado: No disponible"
            item.icon = ContextCompat.getDrawable(this, android.R.drawable.presence_busy)
            Toast.makeText(this, "Estás desconectado", Toast.LENGTH_SHORT).show()
        }

        // Guardar estado en Firebase (opcional)
        val user = Firebase.auth.currentUser
        user?.let {
            val ref = Firebase.database.reference.child("t3_users").child(it.uid)
            ref.child("disponible").setValue(if (disponible) true else false)
        }
    }

    // ======== UBICACIÓN ========
    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED && coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun loadUserLocation() {
        try {
            if (hasLocationPermission()) {
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()
                fusedClient.requestLocationUpdates(request, object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let { location ->
                            val point = GeoPoint(location.latitude, location.longitude)
                            map.controller.setCenter(point)
                            addMarker(point, "Tu ubicación", true)
                            fusedClient.removeLocationUpdates(this)
                        }
                    }
                }, Looper.getMainLooper())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // ======== MARCADORES JSON ========
    private fun loadJsonMarkers() {
        try {
            val json = assets.open("locations.json").bufferedReader().use { it.readText() }
            val jsonObj = JSONObject(json)
            val arr = jsonObj.getJSONArray("locationsArray")

            for (i in 0 until arr.length()) {
                val loc = arr.getJSONObject(i)
                val point = GeoPoint(loc.getDouble("latitude"), loc.getDouble("longitude"))
                addMarker(point, loc.getString("name"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addMarker(point: GeoPoint, title: String, isUser: Boolean = false) {
        val marker = Marker(map)
        marker.position = point
        marker.title = title
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        if (isUser) {
            // Usa un ícono genérico si no tienes ic_my_location
            marker.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation)
        }
        map.overlays.add(marker)
    }

    // ======== CICLO DE VIDA ========
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == 1001 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            loadUserLocation()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
