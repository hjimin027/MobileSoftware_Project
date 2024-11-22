package com.example.mobilesoftware_proj

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobilesoftware_proj.databinding.ActivityMyfriendBinding

class myfriendActivity : AppCompatActivity() {
    val binding by lazy { ActivityMyfriendBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val bottomNavigationView = binding.bottomNavigation

        bottomNavigationView.selectedItemId = R.id.menu_myfriend

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
                    true
                }
                R.id.menu_mypage -> {
                    startActivity(Intent(this, MypageActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}