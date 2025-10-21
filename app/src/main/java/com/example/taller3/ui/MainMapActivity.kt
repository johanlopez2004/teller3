package com.example.taller3.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.databinding.ActivityMainMapBinding

class MainMapActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainMapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainMapBinding.inflate(layoutInflater)
        setContentView(b.root)
    }
}
