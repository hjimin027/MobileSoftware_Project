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
import com.example.mobilesoftware_proj.databinding.SearchfriendRecyclerviewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchFriendActivity : AppCompatActivity() {
    val binding by lazy { ActivitySearchFriendBinding.inflate(layoutInflater) }
    val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.recyclerview.layoutManager = LinearLayoutManager(this)

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
            .whereGreaterThanOrEqualTo("email", email)
            .whereLessThanOrEqualTo("email", email + "\uf8ff")
            .get()
            .addOnSuccessListener { documents ->
                val userList = documents.map { it.toObject(User::class.java) }
                binding.recyclerview.adapter = UserAdapter(userList) { user ->
                    sendFriendRequest(user)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "검색 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendFriendRequest(user: User) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        if (currentUserEmail != null) {
            // 현재 사용자의 고유 아이디 조회
            db.collection("user")
                .whereEqualTo("email", currentUserEmail)
                .get()
                .addOnSuccessListener { currentUserDocs ->
                    val currentUserId = currentUserDocs.documents.firstOrNull()?.id

                    if (currentUserId != null) {
                        // 요청받는 사용자의 고유 아이디 조회
                        db.collection("user")
                            .whereEqualTo("email", user.email)
                            .get()
                            .addOnSuccessListener { userDocs ->
                                val toUserId = userDocs.documents.firstOrNull()?.id

                                if (toUserId != null) {
                                    // friend_req 컬렉션에 요청 데이터 추가
                                    val request = mapOf(
                                        "from" to currentUserId,
                                        "to" to toUserId,
                                        "status" to "REQUEST"
                                    )
                                    db.collection("friend_req")
                                        .add(request)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                this,
                                                "${user.nickname}님에게 친구 요청을 보냈습니다!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                this,
                                                "친구 요청 실패: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                } else {
                                    Toast.makeText(this, "요청받는 사용자를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "사용자 조회 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "로그인한 사용자를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "현재 사용자 조회 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "로그인 정보가 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    data class User(
        val nickname: String = "",
        val email: String = ""
    )


    inner class UserAdapter(
        private val userList: List<User>,
        private val onFriendRequestClick: (User) -> Unit
    ) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

        inner class UserViewHolder(val binding: SearchfriendRecyclerviewBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val binding = SearchfriendRecyclerviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return UserViewHolder(binding)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            val user = userList[position]
            holder.binding.friendName.text = user.nickname
            holder.binding.friendId.text = user.email
            holder.itemView.setOnClickListener {
                onFriendRequestClick(user)
            }
        }

        override fun getItemCount() = userList.size
    }
}