package com.example.mobilesoftware_proj

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mobilesoftware_proj.databinding.ActivityBookshelfBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

data class Book(
    val id: String = "",
    val title: String = "",
    val cover: String = "",
    val current_page: Int = 0,
    val total_page: Int = 10,
    val status: String = "NOTREAD"
)

class BookAdapter(private val bookList: List<Book>) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {
    class BookViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val title: android.widget.TextView = itemView.findViewById(R.id.book_title)
        val coverImg: android.widget.ImageView = itemView.findViewById(R.id.bookshelf_image)
        val bookshelfProgress: android.widget.ProgressBar = itemView.findViewById(R.id.bookshelf_progress)
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
        holder.bookshelfProgress.progress = (book.current_page.toFloat() / book.total_page.toFloat() * 100).toInt()
        if (book.status == "READ") {
            holder.itemView.isEnabled = false
            holder.itemView.alpha = 0.5f
        } else {
            holder.itemView.isEnabled = true
            holder.itemView.alpha = 1.0f
        }
        holder.itemView.setOnClickListener{
            val intent = android.content.Intent(holder.itemView.context, BookActivity::class.java)
            intent.putExtra("bookId", book.id)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = bookList.size
}

class BookshelfActivity : AppCompatActivity() {
    private val binding by lazy { ActivityBookshelfBinding.inflate(layoutInflater) }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth.currentUser?.let { user ->
            loadBooks(user.uid)
        } ?: Log.e("Auth", "User is not signed in")

        setupBottomNavigation()
        setupFloatingActionButton()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.menu_bookshelf
        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_bookshelf -> true
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
    }

    private fun setupFloatingActionButton() {
        binding.floatingActionButton.setOnClickListener {
            startActivity(Intent(this, SearchBookActivity::class.java))
            //finish()
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
                    document.toObject<Book>().copy(id = document.id).let { book ->
                        bookList.add(book)
                    }
                }
                binding.recyclerview.adapter = BookAdapter(bookList)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error getting documents: ", e)
            }
    }
}