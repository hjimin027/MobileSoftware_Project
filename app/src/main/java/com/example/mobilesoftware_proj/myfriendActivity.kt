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
import com.google.firebase.firestore.FirebaseFirestore

class myfriendActivity : AppCompatActivity() {
    val binding by lazy { ActivityMyfriendBinding.inflate(layoutInflater) }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val friendListAdapter by lazy { MyFriendAdapter(mutableListOf()) }

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
            finish()
        }

        binding.friendReq.setOnClickListener {
            startActivity(Intent(this, FriendReqActivity::class.java))
            finish()
        }
    }



    private fun setupRecyclerView() {
        binding.myfriendRecycle.layoutManager = LinearLayoutManager(this)
        binding.myfriendRecycle.adapter = friendListAdapter
    }

    private fun loadFriends() {
        val userUid = auth.currentUser?.uid
        if (userUid == null) {
            Toast.makeText(this, "로그인 상태를 확인하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("friends").document(userUid)
            .get()
            .addOnSuccessListener { document ->
                val friendUids = document.data?.keys ?: emptySet()
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

    private fun fetchFriendDetails(friendUids: Set<String>) {
        firestore.collection("users")
            .whereIn("uid", friendUids.toList())
            .get()
            .addOnSuccessListener { querySnapshot ->
                val friends = querySnapshot.documents.map { document ->
                    Friend(
                        name = document.getString("name") ?: "Unknown",
                        id = document.getString("uid") ?: "Unknown"
                    )
                }
                friendListAdapter.updateFriends(friends)
            }
            .addOnFailureListener {
                Toast.makeText(this, "친구 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    // RecyclerView Adapter
    inner class MyFriendAdapter(private var friendList: MutableList<Friend>) :
        RecyclerView.Adapter<MyFriendAdapter.MyFriendViewHolder>() {

        inner class MyFriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val friendImage: ImageView = itemView.findViewById(R.id.myfriend_image)
            val friendName: TextView = itemView.findViewById(R.id.friend_name)
            val friendId: TextView = itemView.findViewById(R.id.friend_id)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyFriendViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.myfriend_recyclerview, parent, false)
            return MyFriendViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyFriendViewHolder, position: Int) {
            val friend = friendList[position]
            holder.friendName.text = friend.name
            holder.friendId.text = friend.id
            // 예시로 친구 이미지는 기본 이미지로 설정
            holder.friendImage.setImageResource(R.drawable.baseline_emoji_people_24)
        }

        override fun getItemCount(): Int = friendList.size

        fun updateFriends(newFriends: List<Friend>) {
            friendList.clear()
            friendList.addAll(newFriends)
            notifyDataSetChanged()
        }
    }
}

data class Friend(
    val name: String,
    val id: String
)