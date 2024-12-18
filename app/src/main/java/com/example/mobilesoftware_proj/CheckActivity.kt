package com.example.mobilesoftware_proj

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilesoftware_proj.databinding.ActivityCheckBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CheckActivity : AppCompatActivity() {
    private val binding by lazy { ActivityCheckBinding.inflate(layoutInflater) }
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val bottomNavigationView = binding.bottomNavigation

        bottomNavigationView.selectedItemId = R.id.menu_goal

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_bookshelf -> {
                    startActivity(Intent(this, BookshelfActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_goal -> {
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

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = String.format("%04d.%02d.%02d", year, month + 1, dayOfMonth)
            loadBooksForDate(selectedDate)
        }
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
    }

    private fun loadBooksForDate(date: String) {
        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("user")
            .document(userId)
            .collection("reading_schedule")
            .document(date)
            .collection("books")
            .get()
            .addOnSuccessListener { documents ->
                val books = mutableListOf<Book>()
                for (document in documents) {
                    val title = document.getString("title") ?: ""
                    val pages = document.getLong("pages")?.toInt() ?: 0
                    val book = Book(title, pages)
                    books.add(book)
                }
                binding.recyclerview.adapter = BookAdapter(books)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "책을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    data class Book(
        val title: String = "",
        val goalPages: Int = 0,
        var previousPage: Int = 0
    )

    inner class BookAdapter(private val books: List<Book>) : RecyclerView.Adapter<BookAdapter.BookViewHolder>(){
        inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.bookTitle)
            val todayPage: EditText = itemView.findViewById(R.id.pageInput)
            val goalPages: TextView = itemView.findViewById(R.id.pageGoal)
            val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.check_recyclerview, parent, false)
            return BookViewHolder(view)
        }

        override fun getItemCount() = books.size

        override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
            val book = books[position]
            holder.title.text = book.title
            holder.goalPages.text = "/${book.goalPages}"
            holder.todayPage.hint = book.goalPages.toString()

            if (userId != null){
                db.collection("user")
                    .document(userId)
                    .collection("user_books")
                    .whereEqualTo("title", book.title)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            val currentPage = document.get("currentPage") as Long
                            book.previousPage = currentPage.toInt()
                        }
                    }
            }

            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked){
                    holder.todayPage.setText(book.goalPages.toString())
                    saveCurrentPage(book.title, book.goalPages)
                } else {
                    holder.todayPage.setText("")
                    saveCurrentPage(book.title, book.previousPage)
                }
            }


            holder.todayPage.addTextChangedListener(object : TextWatcher{
                override fun afterTextChanged(s: Editable?) {
                    val currentPage = s.toString().toIntOrNull() ?: 0
                    saveCurrentPage(book.title, currentPage)
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
            })
        }

        private fun saveCurrentPage(bookTitle: String, currentPage: Int) {
            if (userId == null) return

            db.collection("user")
                .document(userId)
                .collection("user_books")
                .whereEqualTo("title", bookTitle)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val bookId = document.id
                        db.collection("user")
                            .document(userId)
                            .collection("user_books")
                            .document(bookId)
                            .update("currentPage", currentPage)
                            .addOnSuccessListener {
                                Toast.makeText(this@CheckActivity, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@CheckActivity, "저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
        }
    }

}