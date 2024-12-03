package com.example.mobilesoftware_proj

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilesoftware_proj.databinding.ActivitySearchFriendBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchFriendActivity : AppCompatActivity() {
    val binding by lazy { ActivitySearchFriendBinding.inflate(layoutInflater) }
    val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        binding.recyclerview.adapter = adapter()

        binding.searchButton.setOnClickListener {
            val emailQuery = binding.searchFriend.text.toString()
            if (emailQuery.isNotEmpty()) {
                searchUserByEmail(emailQuery)
            } else {
                Toast.makeText(this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchUserByEmail(email: String) {
        db.collection("user")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                val userList = mutableListOf<User>()
                for (document in documents) {
                    val user = document.toObject(User::class.java)
                    userList.add(user)
                }
                updateRecyclerView(userList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "검색 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    data class User(
        val nickname: String = "",
        val email: String = ""
    )

    private fun updateRecyclerView(users: List<User>) {
        val adapter = UserAdapter(users) { user ->
            addFriend(user)
        }
        binding.recyclerview.adapter = adapter
    }

    private fun addFriend(user: User) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            db.collection("user").document(currentUserId)
                .collection("friends").document(user.email)
                .set(user)
                .addOnSuccessListener {
                    Toast.makeText(this, "${user.nickname}님을 친구로 추가했습니다!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "친구 추가 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    class UserAdapter(
        private val users: List<User>,
        private val onAddFriendClick: (User) -> Unit
    ) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
        class UserViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return UserViewHolder(binding)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            val user = users[position]
            holder.binding.userNickname.text = user.nickname
            holder.binding.userEmail.text = user.email
            holder.binding.addFriendButton.setOnClickListener {
                onAddFriendClick(user)
            }
        }

        override fun getItemCount() = users.size
    }
}