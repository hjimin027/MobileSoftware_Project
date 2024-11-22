package com.example.mobilesoftware_proj

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobilesoftware_proj.databinding.ActivityMypageBinding

class MypageActivity : AppCompatActivity() {
    val binding by lazy { ActivityMypageBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val bottomNavigationView = binding.bottomNavigation

        bottomNavigationView.selectedItemId = R.id.menu_mypage

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_bookshelf -> {
                    startActivity(Intent(this, BookshelfActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_goal -> {
                    startActivity(Intent(this, CheckActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_myfriend -> {
                    startActivity(Intent(this, myfriendActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_mypage -> {
                    true
                }
                else -> false
            }
        }
    }
}