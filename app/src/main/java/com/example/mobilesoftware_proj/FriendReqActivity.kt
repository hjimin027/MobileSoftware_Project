package com.example.mobilesoftware_proj

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilesoftware_proj.databinding.ActivityFriendReqBinding
import com.example.mobilesoftware_proj.databinding.FriendreqRecyclerviewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FriendReqActivity : AppCompatActivity() {
    private val binding by lazy { ActivityFriendReqBinding.inflate(layoutInflater) }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.requests.layoutManager = LinearLayoutManager(this)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("FriendReqActivity", "Current UID: $currentUserId")
        if (currentUserId == null) {
            Toast.makeText(this, "로그인 정보를 확인해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 실시간 요청 데이터 구독
        db.collection("friend_req")
            .whereEqualTo("to", currentUserId)
            .whereEqualTo("status", "REQUEST")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FriendRequest", "데이터 로드 실패", e)
                    Toast.makeText(this, "데이터 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    Log.d("FriendRequest", "Received ${snapshot.documents.size} documents")

                    val requestList = snapshot.documents.mapNotNull { document ->
                        val request = document.toObject(FriendRequest::class.java)
                        request?.let { it.copy(documentId = document.id) }
                    }.toMutableList()

                    if (requestList.isNotEmpty()) {
                        binding.requests.adapter = FriendReqAdapter(requestList)
                    } else {
                        Toast.makeText(this, "친구 요청이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    data class FriendRequest(
        val from: String = "",
        val to: String = "",
        val status: String = "",
        val documentId: String = ""
    )

    inner class FriendReqAdapter(private val requestList: MutableList<FriendRequest>) :
        RecyclerView.Adapter<FriendReqAdapter.FriendReqViewHolder>() {

        inner class FriendReqViewHolder(val binding: FriendreqRecyclerviewBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendReqViewHolder {
            val binding = FriendreqRecyclerviewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return FriendReqViewHolder(binding)
        }

        override fun onBindViewHolder(holder: FriendReqViewHolder, position: Int) {
            val request = requestList[position]

            // Firestore에서 요청 보낸 사용자의 정보 조회
            db.collection("user")
                .document(request.from)
                .get()
                .addOnSuccessListener { document ->
                    val nickname = document.getString("nickname") ?: "알 수 없음"
                    val email = document.getString("email") ?: "알 수 없음" // 이메일 필드 조회
                    holder.binding.reqName.text = nickname
                    holder.binding.reqId.text = email

                    // 요청 수락 버튼 클릭 이벤트
                    holder.binding.accept.setOnClickListener {
                        acceptFriendRequest(request, position)
                    }

                    // 요청 거절 버튼 클릭 이벤트
                    holder.binding.reject.setOnClickListener {
                        rejectFriendRequest(request, position)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FriendRequest", "사용자 정보 조회 실패", e)
                    Toast.makeText(this@FriendReqActivity, "사용자 정보 조회 실패", Toast.LENGTH_SHORT).show()
                }
        }

        override fun getItemCount() = requestList.size

        // 특정 아이템 제거 함수
        fun removeItem(position: Int) {
            requestList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    // 친구 요청 수락 메서드
    private fun acceptFriendRequest(request: FriendRequest, position: Int) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val batch = db.batch()

        // 요청 상태 ACCEPT로 업데이트
        val requestDoc = db.collection("friend_req").document(request.documentId)
        batch.update(requestDoc, "status", "ACCEPT")

        // `friends` 컬렉션에 상대방 UID 추가 (각 사용자 문서에 필드로 추가)
        val currentUserFriendDoc = db.collection("friends").document(currentUserId)
        batch.set(
            currentUserFriendDoc,
            mapOf(request.from to true),
            SetOptions.merge()
        )

        val fromUserFriendDoc = db.collection("friends").document(request.from)
        batch.set(
            fromUserFriendDoc,
            mapOf(currentUserId to true),
            SetOptions.merge()
        )

        // 요청 데이터 삭제
        batch.delete(requestDoc)

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this@FriendReqActivity, "친구 요청을 수락했습니다.", Toast.LENGTH_SHORT).show()
                (binding.requests.adapter as FriendReqAdapter).removeItem(position)
            }
            .addOnFailureListener { e ->
                Log.e("FriendRequest", "친구 요청 처리 실패", e)
                Toast.makeText(this@FriendReqActivity, "친구 요청 처리 실패", Toast.LENGTH_SHORT).show()
            }
    }

    // 친구 요청 거절 메서드
    private fun rejectFriendRequest(request: FriendRequest, position: Int) {
        db.collection("friend_req").document(request.documentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this@FriendReqActivity, "친구 요청을 거절했습니다.", Toast.LENGTH_SHORT).show()
                (binding.requests.adapter as FriendReqAdapter).removeItem(position)
            }
            .addOnFailureListener { e ->
                Log.e("FriendRequest", "친구 요청 거절 실패", e)
                Toast.makeText(this@FriendReqActivity, "친구 요청 거절 실패", Toast.LENGTH_SHORT).show()
            }
    }
}
