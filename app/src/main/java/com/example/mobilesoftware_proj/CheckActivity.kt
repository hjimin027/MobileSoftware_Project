package com.example.mobilesoftware_proj

import android.annotation.SuppressLint
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import java.util.Calendar
import java.util.Locale

class CheckActivity : AppCompatActivity() {
    private val binding by lazy { ActivityCheckBinding.inflate(layoutInflater) }
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        finishAffinity() // 스택의 모든 액티비티 종료
    }

    @SuppressLint("DefaultLocale")
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

        val today = Calendar.getInstance()
        val currentDate = String.format("%04d.%02d.%02d", today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH))
        loadBooksForDate(currentDate)
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
                    val checked = document.getBoolean("checked") ?: false
                    val book = Book(title, pages, checked = checked)
                    books.add(book)
                }
                binding.recyclerview.adapter = BookAdapter(books)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "책을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                Log.e("CheckActivity", exception.toString())
            }
    }

    data class Book(
        val title: String = "",
        val goalPages: Int = 0,
        var previousPage: Int = 0,
        var checked: Boolean = false
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
            holder.checkBox.isChecked = book.checked

            if (userId != null) {
                db.collection("user")
                    .document(userId)
                    .collection("user_books")
                    .whereEqualTo("title", book.title)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            val currentPage = document.getLong("current_page")?.toInt() ?: 0
                            book.previousPage = currentPage
                            holder.todayPage.setText(currentPage.toString()) // 불러온 current_page 값을 EditText에 설정
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("CheckActivity", "current_page 불러오기에 실패했습니다: $exception")
                    }
            }

            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                try {
                    book.checked = isChecked
                    if (isChecked) {
                        holder.todayPage.setText(book.goalPages.toString())
                        saveCurrentPage(book.title, book.goalPages, book.checked)
                    } else {
                        holder.todayPage.setText(book.previousPage.toString())
                        saveCurrentPage(book.title, book.previousPage, book.checked)
                    }
                } catch (e: Exception) {
                    Log.e("CheckActivity", e.toString())
                }
            }

            holder.todayPage.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    try {
                        val currentPage = s.toString().toIntOrNull() ?: 0
                        saveCurrentPage(book.title, currentPage, holder.checkBox.isChecked)
                    } catch (e: Exception) {
                        Log.e("CheckActivity", e.toString())
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        private fun saveCurrentPage(bookTitle: String, currentPage: Int, isChecked: Boolean) {
            if (userId == null) return

            val userBooksRef = db.collection("user")
                .document(userId)
                .collection("user_books")

            // Update current_page and check status
            userBooksRef
                .whereEqualTo("title", bookTitle)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val bookId = document.id

                        // Update current_page
                        userBooksRef
                            .document(bookId)
                            .update("current_page", currentPage)
                            .addOnSuccessListener {
                                Log.i("CheckActivity", "current_page 저장되었습니다.")

                                // Check if current_page == total_page
                                userBooksRef
                                    .document(bookId)
                                    .get()
                                    .addOnSuccessListener { doc ->
                                        val totalPage = doc.getLong("total_page")?.toInt() ?: 0
                                        if (currentPage == totalPage) {
                                            // Update status to "READ"
                                            userBooksRef
                                                .document(bookId)
                                                .update("status", "READ")
                                                .addOnSuccessListener {
                                                    Log.i(
                                                        "CheckActivity",
                                                        "status가 'READ'로 변경되었습니다."
                                                    )
                                                }
                                                .addOnFailureListener {
                                                    Log.e("CheckActivity", "status 업데이트에 실패했습니다.")
                                                }
                                        } else{
                                            userBooksRef
                                                .document(bookId)
                                                .update("status", "NOTREAD")
                                                .addOnSuccessListener {
                                                    Log.i(
                                                        "CheckActivity",
                                                        "status가 'NOTREAD'로 변경되었습니다."
                                                    )
                                                }
                                                .addOnFailureListener {
                                                    Log.e("CheckActivity", "status 업데이트에 실패했습니다.")
                                                }
                                        }
                                    }
                                    .addOnFailureListener {
                                        Log.e("CheckActivity", "total_page 확인에 실패했습니다.")
                                    }
                            }
                            .addOnFailureListener {
                                Log.e("CheckActivity", "current_page 저장에 실패했습니다.")
                            }
                    }
                }

            // Update checked in reading_schedule
            val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            val currentDate = dateFormat.format(binding.calendarView.date)

            db.collection("user")
                .document(userId)
                .collection("reading_schedule")
                .document(currentDate)
                .collection("books")
                .whereEqualTo("title", bookTitle)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val bookId = document.id
                        db.collection("user")
                            .document(userId)
                            .collection("reading_schedule")
                            .document(currentDate)
                            .collection("books")
                            .document(bookId)
                            .update("checked", isChecked)
                            .addOnSuccessListener {
                                Log.i("CheckActivity", "checked 저장되었습니다.")
                            }
                            .addOnFailureListener {
                                Log.e("CheckActivity", "checked 저장에 실패했습니다.")
                            }
                    }
                }
        }
    }

}