package com.example.taller3.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.taller3.data.Usuario
import com.example.taller3.databinding.ItemUsuarioBinding

class UsuarioAdapter(
    private val onVerUbicacionClick: (Usuario) -> Unit
) : ListAdapter<Usuario, UsuarioAdapter.UsuarioViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Usuario>() {
            override fun areItemsTheSame(oldItem: Usuario, newItem: Usuario) = oldItem.uid == newItem.uid
            override fun areContentsTheSame(oldItem: Usuario, newItem: Usuario) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val binding = ItemUsuarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UsuarioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UsuarioViewHolder(private val b: ItemUsuarioBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(usuario: Usuario) {
            b.tvNombre.text = usuario.nombre
            b.imgFoto.load(usuario.photoUrl) {
                placeholder(android.R.drawable.ic_menu_report_image)
            }
            b.btnVerUbicacion.setOnClickListener {
                onVerUbicacionClick(usuario)
            }
        }
    }
}
