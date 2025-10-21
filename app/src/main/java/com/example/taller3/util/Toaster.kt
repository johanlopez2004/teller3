package com.example.taller3.util

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.taller3.R

enum class ToastType { INFO, SUCCESS, ERROR }

object Toaster {

    fun show(context: Context, msg: String, type: ToastType = ToastType.INFO) {
        val inflater = LayoutInflater.from(context)
        val view: View = inflater.inflate(R.layout.view_toast, null)
        val tv = view.findViewById<TextView>(R.id.message)
        val icon = view.findViewById<ImageView>(R.id.icon)

        tv.text = msg
        when (type) {
            ToastType.INFO -> icon.setImageResource(android.R.drawable.ic_dialog_info)
            ToastType.SUCCESS -> icon.setImageResource(android.R.drawable.checkbox_on_background)
            ToastType.ERROR -> icon.setImageResource(android.R.drawable.ic_delete)
        }

        Toast(context).apply {
            duration = Toast.LENGTH_SHORT
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 120)
            this.view = view
        }.show()
    }

    fun info(ctx: Context, msg: String) = show(ctx, msg, ToastType.INFO)
    fun ok(ctx: Context, msg: String) = show(ctx, msg, ToastType.SUCCESS)
    fun error(ctx: Context, msg: String) = show(ctx, msg, ToastType.ERROR)
}
