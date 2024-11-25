package com.example.mobilesoftware_proj

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobilesoftware_proj.databinding.ActivityInfoBinding
import com.google.firebase.auth.FirebaseAuth

class InfoActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // 현재 로그인된 사용자 이메일 표시
        binding.infoEmailText.text = auth.currentUser?.email

        // 로그인한 날짜
        binding.infoDateText.text = auth.currentUser?.metadata?.creationTimestamp.toString()
    }
}