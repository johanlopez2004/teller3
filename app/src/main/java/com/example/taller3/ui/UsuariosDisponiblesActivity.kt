package com.example.taller3.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taller3.R
import com.example.taller3.data.Usuario
import com.example.taller3.data.FirebaseRefs
import com.example.taller3.ui.adapters.UsuarioAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class UsuariosDisponiblesActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: UsuarioAdapter
    private val listaUsuarios = mutableListOf<Usuario>()

    private val usersRef = FirebaseRefs.users()

    private val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val nuevos = mutableListOf<Usuario>()
            for (child in snapshot.children) {
                val u = child.getValue(Usuario::class.java)
                if (u != null && u.disponible) {
                    nuevos.add(u)
                }
            }
            listaUsuarios.clear()
            listaUsuarios.addAll(nuevos)
            adapter.updateList(listaUsuarios)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("UsuariosDisp", "DB cancelled: ${error.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usuarios_disponibles)

        recycler = findViewById(R.id.recyclerUsuarios)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = UsuarioAdapter(listaUsuarios) { usuario ->
            // Al pulsar "Ver posición" → abrir actividad de seguimiento
            val intent = Intent(this, MapaSeguimientoActivity::class.java)
            intent.putExtra("uidUsuario", usuario.uid)
            intent.putExtra("nombreUsuario", "${usuario.nombre} ${usuario.apellido}".trim())
            startActivity(intent)
        }
        recycler.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        // Escuchar cambios en usuarios (se filtrará por disponible)
        usersRef.addValueEventListener(listener)
    }

    override fun onStop() {
        super.onStop()
        usersRef.removeEventListener(listener)
    }
}
