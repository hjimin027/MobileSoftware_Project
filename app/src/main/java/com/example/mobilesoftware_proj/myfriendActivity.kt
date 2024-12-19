package com.example.mobilesoftware_proj

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilesoftware_proj.databinding.ActivityMyfriendBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class myfriendActivity : AppCompatActivity() {
    val binding by lazy { ActivityMyfriendBinding.inflate(layoutInflater) }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val friendListAdapter by lazy { FriendListAdapter(mutableListOf()) }

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        finishAffinity() // 스택의 모든 액티비티 종료
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupRecyclerView()
        loadFriends()

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

        binding.floatingActionButton.setOnClickListener {
            startActivity(Intent(this, SearchFriendActivity::class.java))
            //finish()
        }

        binding.friendReq.setOnClickListener {
            startActivity(Intent(this, FriendReqActivity::class.java))
            //finish()
        }
    }



    private fun setupRecyclerView() {
        binding.myfriendRecycle.layoutManager = LinearLayoutManager(this)
        binding.myfriendRecycle.adapter = friendListAdapter
    }

    private fun loadFriends() {
        val userUid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "로그인 상태를 확인하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("friends").document(userUid)
            .get()
            .addOnSuccessListener { document ->
                val friendUids = document.data?.keys?.toList() ?: emptyList()
                if (friendUids.isEmpty()) {
                    Toast.makeText(this, "친구가 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    fetchFriendDetails(friendUids)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "친구 목록을 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchFriendDetails(friendUids: List<String>) {
        firestore.collection("user")
            .whereIn(FieldPath.documentId(), friendUids.take(10)) // Firestore 제한 처리
            .get()
            .addOnSuccessListener { querySnapshot ->
                val friends = querySnapshot.documents.map { document ->
                    Friend(
                        name = document.getString("nickname") ?: "Unknown",
                        email = document.getString("email") ?: "No Email",
                        uid = document.id
                    )
                }
                friendListAdapter.updateFriends(friends)
            }
            .addOnFailureListener {
                Toast.makeText(this, "친구 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    class FriendListAdapter(private var friends: List<Friend>) : RecyclerView.Adapter<FriendListAdapter.FriendViewHolder>() {
        class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameTextView: TextView = view.findViewById(R.id.friend_name)
            val emailTextView: TextView = view.findViewById(R.id.friend_id)

            fun bind(friend: Friend, onClick: (String) -> Unit) {
                nameTextView.text = friend.name
                emailTextView.text = friend.email
                itemView.setOnClickListener { onClick(friend.uid) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.myfriend_recyclerview, parent, false)
            return FriendViewHolder(view)
        }

        override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
            val friend = friends[position]
            holder.bind(friend) { friendUid ->
                val intent = Intent(holder.itemView.context, FriendBookshelfActivity::class.java)
                intent.putExtra("FRIEND_USER_ID", friendUid)
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = friends.size

        fun updateFriends(newFriends: List<Friend>) {
            friends = newFriends
            notifyDataSetChanged()
        }
    }
}

data class Friend(
    val name: String,
    val email: String,
    val uid: String
)