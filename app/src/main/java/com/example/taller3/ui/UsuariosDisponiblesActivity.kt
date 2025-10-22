package com.example.taller3.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taller3.data.Usuario
import com.example.taller3.databinding.ActivityUsuariosDisponiblesBinding
import com.google.firebase.database.*

class UsuariosDisponiblesActivity : AppCompatActivity() {

    private lateinit var b: ActivityUsuariosDisponiblesBinding
    private lateinit var adapter: UsuarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityUsuariosDisponiblesBinding.inflate(layoutInflater)
        setContentView(b.root)

        adapter = UsuarioAdapter { usuario ->
            // Al hacer click en ver ubicación
            val intent = Intent(this, MapaSeguimientoActivity::class.java)
            intent.putExtra("uid_seguido", usuario.uid)
            startActivity(intent)
        }

        b.recyclerUsuarios.layoutManager = LinearLayoutManager(this)
        b.recyclerUsuarios.adapter = adapter

        loadUsuarios()
    }

    private fun loadUsuarios() {
        val ref = FirebaseDatabase.getInstance().reference.child("t3_users")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listaUsuarios = mutableListOf<Usuario>()
                for (child in snapshot.children) {
                    val usuario = child.getValue(Usuario::class.java)
                    if (usuario != null && usuario.disponible) {
                        listaUsuarios.add(usuario)
                    }
                }
                adapter.submitList(listaUsuarios)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
