package com.example.mobilesoftware_proj

import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser!!.uid

        // 해당 사용자의 user_books 컬렉션에서 같은 책이 있는지 먼저 확인
        db.collection("user").document(userId)
            .collection("user_books")
            .whereEqualTo("isbn13", book.isbn13) // isbn13을 기준으로 중복 체크
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // 책이 존재하지 않으면 새로 추가
                    val bookToSave = hashMapOf(
                        "title" to book.title,
                        "cover" to book.cover,
                        "isbn13" to book.isbn13, // isbn13 추가
                        "total_page" to book.totalPage,
                        "current_page" to 0,
                        "status" to "NOTREAD"
                    )

                    db.collection("user").document(userId)
                        .collection("user_books")
                        .add(bookToSave)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "${book.title}이(가) 서재에 추가되었습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                            startActivity(Intent(this, BookshelfActivity::class.java))
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this,
                                "서재 추가 실패: ${it.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    // 이미 책이 존재하는 경우
                    Toast.makeText(
                        this,
                        "이미 서재에 존재하는 책입니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "중복 확인 중 오류 발생: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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

    private fun searchBooks(query: String) = lifecycleScope.launch {
        val api = Retrofit.Builder()
            .baseUrl("https://www.aladin.co.kr/ttb/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AladinAPI::class.java)

        try {
            val searchResponse = withContext(Dispatchers.IO) {
                api.searchBooks(apiKey, query).execute()
            }

            if (searchResponse.isSuccessful) {
                val books = searchResponse.body()?.item ?: emptyList()

                // 병렬로 각 책의 상세 정보 가져오기
                val booksWithDetails = books.map { book ->
                    async(Dispatchers.IO) {
                        val detailResponse = api.getBookDetails(apiKey, book.isbn13).execute()
                        val totalPage = detailResponse.body()?.item?.firstOrNull()?.subInfo?.itemPage ?: 0
                        book.copy(totalPage = totalPage)
                    }
                }.awaitAll()

                adapter.updateBooks(booksWithDetails)
            } else {
                Toast.makeText(this@SearchBookActivity, "검색 결과를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this@SearchBookActivity, "검색 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


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

        @GET("ItemLookup.aspx")
        fun getBookDetails(
            @Query("ttbkey") apiKey: String,
            @Query("ItemId") itemId: String,
            @Query("ItemIdType") itemIdType: String = "ISBN13",
            @Query("output") output: String = "js",
            @Query("Version") version: Int = 20131101
        ): Call<BookDetailResponse>
    }

    //데이터 모델
    data class BookSearchResponse(
        val item: List<Book>
    )

    data class Book(
        val title: String,
        val author: String,
        val publisher: String,
        val cover: String, // 이미지 URL
        val isbn13: String, // ISBN-13 추가
        val totalPage: Int = 0 // 전체 페이지 수
    )
    data class BookDetailResponse(
        val item: List<BookDetail>
    )
    data class BookDetail(
        val subInfo: SubInfo
    )
    data class SubInfo(
        val itemPage: Int // 페이지 수
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