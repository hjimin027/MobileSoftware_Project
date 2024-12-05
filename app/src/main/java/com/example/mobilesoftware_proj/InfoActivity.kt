package com.example.mobilesoftware_proj

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobilesoftware_proj.databinding.ActivityInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class InfoActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // 닉네임
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("user").document(auth.currentUser!!.uid)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    binding.infoNameEdit.setText(document["nickname"].toString())
                } else {
                    Toast.makeText(this, "닉네임이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "닉네임을 불러오는데 실패했습니다: $exception", Toast.LENGTH_SHORT).show()
            }

        // 닉네임 변경
        binding.infoNameButton.setOnClickListener {
            val nickname = binding.infoNameEdit.text.toString()
            if (nickname.isEmpty()) {
                Toast.makeText(this, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            userRef.update("nickname", nickname)
                .addOnSuccessListener {
                    Toast.makeText(this, "닉네임 변경 성공!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "닉네임 변경 실패: ${exception}", Toast.LENGTH_SHORT).show()
                }
        }

        // 현재 로그인된 사용자 이메일 표시
        binding.infoEmailText.text = auth.currentUser?.email

        // 가입한 날짜
        //binding.infoDateText.text = auth.currentUser?.metadata?.creationTimestamp.toString()
        val creationTime = auth.currentUser?.metadata?.creationTimestamp
        if (creationTime != null) {
            val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            binding.infoDateText.text = dateFormat.format(creationTime)
        } else {
            binding.infoDateText.text = "가입일을 불러올 수 없습니다."
        }
    }
}