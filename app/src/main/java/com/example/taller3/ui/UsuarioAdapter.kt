package com.example.taller3.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.taller3.R
import com.example.taller3.data.Usuario

class UsuarioAdapter(
    private val usuarios: MutableList<Usuario>,
    private val onVerMapaClick: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgUsuario: ImageView = itemView.findViewById(R.id.imgUsuario)
        val txtNombre: TextView = itemView.findViewById(R.id.txtNombre)
        val btnVerMapa: Button = itemView.findViewById(R.id.btnVerMapa)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]
        holder.txtNombre.text = "${usuario.nombre} ${usuario.apellido}".trim()

        // Carga imagen con Coil (placeholder si no hay)
        holder.imgUsuario.load(usuario.photoUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)
        }

        holder.btnVerMapa.setOnClickListener { onVerMapaClick(usuario) }
    }

    override fun getItemCount(): Int = usuarios.size

    fun updateList(newList: List<Usuario>) {
        usuarios.clear()
        usuarios.addAll(newList)
        notifyDataSetChanged()
    }
}
