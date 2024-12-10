package com.example.mobilesoftware_proj

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mobilesoftware_proj.databinding.ActivityBookshelfBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex

data class Book(
    val title: String = "",
    val cover: String = "",
)

class BookAdapter(private val bookList: List<Book>):
    RecyclerView.Adapter<BookAdapter.BookViewHolder>() {
        class BookViewHolder(itemView: android.view.View):
            RecyclerView.ViewHolder(itemView) {
                val title = itemView.findViewById<android.widget.TextView>(R.id.book_title)
                val coverImg = itemView.findViewById<android.widget.ImageView>(R.id.bookshelf_image)
            }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): BookViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.bookshelf_recyclerview, parent, false)
        return BookViewHolder(view)

    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = bookList[position]
        holder.title.text = book.title
        Glide.with(holder.itemView.context)
            .load(book.cover)
            .into(holder.coverImg)
    }

    override fun getItemCount(): Int = bookList.size
    }

class BookshelfActivity : AppCompatActivity() {
    val binding by lazy { ActivityBookshelfBinding.inflate(layoutInflater) }
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val user = auth.currentUser
        if (user != null){
            val userId = user.uid
            loadBooks(userId)
        } else{
            Log.e("Auth", "User is not signed in")
        }
        val bookList = mutableListOf<Book>()


        val bottomNavigationView = binding.bottomNavigation
        bottomNavigationView.selectedItemId = R.id.menu_bookshelf
        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_bookshelf -> {
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
                    startActivity(Intent(this, MypageActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        binding.floatingActionButton.setOnClickListener {
            startActivity(Intent(this, SearchBookActivity::class.java))
            finish()
        }
    }

    private fun loadBooks(userId: String) {
        val bookList = mutableListOf<Book>()
        binding.recyclerview.layoutManager = LinearLayoutManager(this)

        db.collection("user")
            .document(userId)
            .collection("user_books")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val book = document.toObject(Book::class.java)
                    bookList.add(book)
                }
                binding.recyclerview.adapter = BookAdapter(bookList)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error getting documents: ", e)
            }
    }
}