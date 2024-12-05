package com.example.mobilesoftware_proj

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Toast
import com.example.mobilesoftware_proj.databinding.ActivitySearchBookBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.http.GET
import retrofit2.http.Query

class SearchBookActivity : AppCompatActivity() {
    val binding by lazy { ActivitySearchBookBinding.inflate(layoutInflater) }
    val books: List<Book> = emptyList() //초기화
    val adapter = BookAdapter(books){ selectedBook -> addToLibrary(selectedBook) }
    private val apiKey = "ttbnunuhelios2112001"
    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    fun addToLibrary(book: Book) {
        // 임시 구현
        //Toast.makeText(this, "${book.title}이(가) 서재에 추가되었습니다.", Toast.LENGTH_SHORT).show()
        auth = FirebaseAuth.getInstance()

        val book = hashMapOf(
            "title" to book.title,
            "cover" to book.cover,
            // "total_page" to
            "current_page" to 0,
            "status" to "NOTREAD"
        )
        db.collection("user").document(auth.currentUser!!.uid)
            .collection("user_books")
            .add(book)
            .addOnSuccessListener {
                Toast.makeText(this, "${title}이(가) 서재에 추가되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "서재 추가 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //리사이클러뷰 설정
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        binding.recyclerview.adapter = adapter

        //검색 버튼 기능 구현
        binding.searchButton.setOnClickListener {
            val query = binding.searchBook.text.toString()
            if (query.isNotEmpty()) {
                searchBooks(query)
            } else {
                Toast.makeText(this, "검색어를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun searchBooks(query: String) {
        val api = Retrofit.Builder()
            .baseUrl("https://www.aladin.co.kr/ttb/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AladinAPI::class.java)

        api.searchBooks(apiKey, query).enqueue(object : Callback<BookSearchResponse> {
            override fun onResponse(call: Call<BookSearchResponse>, response: Response<BookSearchResponse>) {
                if (response.isSuccessful) {
                    val books = response.body()?.item ?: emptyList()
                    adapter.updateBooks(books)
                } else {
                    Toast.makeText(this@SearchBookActivity, "검색 결과를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BookSearchResponse>, t: Throwable) {
                t.printStackTrace()
                Toast.makeText(this@SearchBookActivity, "검색에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Retrofit API 인터페이스
    interface AladinAPI {
        @GET("ItemSearch.aspx")
        fun searchBooks(
            @Query("ttbkey") apiKey: String,
            @Query("Query") query: String,
            @Query("QueryType") queryType: String = "Title",
            @Query("MaxResults") maxResults: Int = 10,
            @Query("start") start: Int = 1,
            @Query("SearchTarget") searchTarget: String = "Book",
            @Query("output") output: String = "js",
            @Query("Version") version: Int = 20131101
        ): Call<BookSearchResponse>
    }

    //데이터 모델
    data class BookSearchResponse(
        val item: List<Book>
    )
    data class Book(
        val title: String,
        val author: String,
        val publisher: String,
        val cover: String //이미지 URL
    )

    //리사이클러뷰 어댑터
    class BookAdapter(var books: List<Book>, val onItemClick: (Book) -> Unit) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.searchbook_recyclerview, parent, false)
            return BookViewHolder(view, onItemClick)
        }

        override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
            holder.bind(books[position])
        }

        override fun getItemCount() = books.size

        fun updateBooks(newBooks: List<Book>) {
            books = newBooks
            notifyDataSetChanged()
        }

        class BookViewHolder(itemView: View, val onItemClick: (Book) -> Unit) : RecyclerView.ViewHolder(itemView) {
            private val title: TextView = itemView.findViewById(R.id.book_title)
            private val author: TextView = itemView.findViewById(R.id.book_author)
            private val publisher: TextView = itemView.findViewById(R.id.book_publisher)
            private val cover: ImageView = itemView.findViewById(R.id.searchbook_image)

            fun bind(book: Book) {
                title.text = book.title
                author.text = book.author
                publisher.text = book.publisher
                Glide.with(itemView).load(book.cover).into(cover)

                itemView.setOnClickListener {
                    onItemClick(book)
                }
            }
        }
    }
}