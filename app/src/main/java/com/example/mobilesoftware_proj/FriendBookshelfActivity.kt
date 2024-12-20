package com.example.mobilesoftware_proj

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mobilesoftware_proj.databinding.ActivityFriendBookshelfBinding
import com.google.firebase.firestore.FirebaseFirestore

data class FriendBook(
    val title: String = "",
    val cover: String = "",
    val current_page: Int = 0,
    val total_page: Int = 10
)

class FriendBookAdapter(private val bookList: List<FriendBook>) : RecyclerView.Adapter<FriendBookAdapter.BookViewHolder>() {

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.book_title)
        val coverImg: ImageView = itemView.findViewById(R.id.bookshelf_image)
        val progress: ProgressBar = itemView.findViewById(R.id.bookshelf_progress)
        val progressText: TextView = itemView.findViewById(R.id.progress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.friendbookshelf_recyclerview, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = bookList[position]
        holder.title.text = book.title
        Glide.with(holder.itemView.context)
            .load(book.cover)
            .into(holder.coverImg)

        // 진행도 계산
        val progressPercentage = if (book.total_page > 0) {
            (book.current_page.toFloat() / book.total_page * 100).toInt()
        } else {
            0
        }

        holder.progress.progress = progressPercentage
    }

    override fun getItemCount(): Int = bookList.size
}

class FriendBookshelfActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val binding by lazy { ActivityFriendBookshelfBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Intent로 전달된 friendUserId 읽기
        val friendUserId = intent.getStringExtra("FRIEND_USER_ID")
        if (friendUserId != null) {
            // 친구의 책 데이터를 불러오기
            loadFriendBooks(friendUserId)
        } else {
            Log.e("Intent", "Friend userId is missing")
        }
    }

    private fun loadFriendBooks(friendUserId: String) {
        val bookList = mutableListOf<FriendBook>()
        binding.recyclerview.layoutManager = LinearLayoutManager(this)

        db.collection("user")
            .document(friendUserId)
            .collection("user_books")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val book = document.toObject(FriendBook::class.java)
                    bookList.add(book)
                }
                binding.recyclerview.adapter = FriendBookAdapter(bookList)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error getting friend's books: ", e)
            }
    }
}