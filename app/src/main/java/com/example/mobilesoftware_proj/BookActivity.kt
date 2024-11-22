package com.example.mobilesoftware_proj

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobilesoftware_proj.databinding.ActivityBookBinding
import com.google.android.material.datepicker.MaterialDatePicker

class BookActivity : AppCompatActivity() {
    val binding by lazy { ActivityBookBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }


}