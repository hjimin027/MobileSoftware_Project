package com.example.mobilesoftware_proj

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobilesoftware_proj.databinding.ActivityBookBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import androidx.core.util.Pair
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Locale

class BookActivity : AppCompatActivity() {
    val binding by lazy { ActivityBookBinding.inflate(layoutInflater) }
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.currentProgressText.addTextChangedListener {
            if (it.toString().toIntOrNull() != null){
                updateCurrentPageInFirestore(it.toString().toInt())
            }
        }

        binding.calendar.setOnClickListener{
            val dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                    .setTitleText("검색 기간을 골라주세요")
                    .build()

            dateRangePicker.show(supportFragmentManager, "date_picker")
            dateRangePicker.addOnPositiveButtonClickListener(object :
                MaterialPickerOnPositiveButtonClickListener<Pair<Long, Long>> {
                override fun onPositiveButtonClick(selection: Pair<Long, Long>?) {
                    val calendar = Calendar.getInstance()
                    // 시작 날짜 설정 및 출력
                    calendar.timeInMillis = selection?.first ?: 0
                    val startDate = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(calendar.time)
                    binding.startDate.text = startDate

                    // 종료 날짜 설정 및 출력
                    calendar.timeInMillis = selection?.second ?: 0
                    val endDate = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(calendar.time)
                    binding.endDate.text = endDate

                    // 선택된 날짜 저장 로직 추가
                    val bookId = intent.getStringExtra("bookId")
                    if (bookId != null) {
                        saveDateToDatabase(bookId, startDate, endDate)
                    } else {
                        Log.e("BookActivity", "bookId is null")
                        Toast.makeText(this@BookActivity, "책 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        val bookId = intent.getStringExtra("bookId")
        if (bookId != null){
            loadBookDetails(bookId)
        } else{
            Log.e("BookActivity", "bookId is null")
        }
    }

    private fun saveDateToDatabase(bookId: String, startDate: String, endDate: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // 시작 및 종료 날짜를 Date 객체로 변환
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val start = dateFormat.parse(startDate)
        val end = dateFormat.parse(endDate)

        if (start != null && end != null) {
            // 날짜 차이를 계산
            val diff = ((end.time - start.time) / (1000 * 60 * 60 * 24)).toInt() + 1

            db.collection("user")
                .document(userId)
                .collection("user_books")
                .document(bookId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val totalPage = document.getLong("total_page")?.toInt() ?: 0
                        val title = document.getString("title") ?: ""
                        if (totalPage > 0) {
                            // 목표 분량 계산
                            val goalDay = totalPage / diff
                            val goalLast = goalDay + (totalPage % diff)

                            // Firestore에 저장할 데이터
                            val dateData = mapOf(
                                "start_date" to startDate,
                                "end_date" to endDate,
                                "goal_day" to goalDay,
                                "goal_last" to goalLast
                            )

                            // Firestore 업데이트
                            db.collection("user")
                                .document(userId)
                                .collection("user_books")
                                .document(bookId)
                                .update(dateData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "날짜와 목표 분량이 저장되었습니다.", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("BookActivity", "목표 분량 저장 실패", exception)
                                    Toast.makeText(this, "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT)
                                        .show()
                                }

                            val calendar = Calendar.getInstance()
                            calendar.time = start
                            var accumulatedPages = 0
                            for (i in 0 until diff) {
                                val date = dateFormat.format(calendar.time)
                                accumulatedPages += if (i == diff - 1) goalLast else goalDay
                                val bookData = mapOf(
                                    "title" to title,
                                    "pages" to accumulatedPages
                                )
                                db.collection("user")
                                    .document(userId)
                                    .collection("reading_schedule")
                                    .document(date)
                                    .collection("books")
                                    .document(bookId)
                                    .set(bookData, SetOptions.merge())
                                calendar.add(Calendar.DATE, 1)
                            }
                        } else {
                            Toast.makeText(this, "총 페이지 수를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("BookActivity", "Firestore 문서 조회 실패", exception)
                }
        } else {
            Toast.makeText(this, "유효하지 않은 날짜 형식입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCurrentPageInFirestore(newPage: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?:return
        val bookId = intent.getStringExtra("bookId") ?: return

        db.collection("user")
            .document(userId)
            .collection("user_books")
            .document(bookId)
            .update("current_page", newPage)
            .addOnSuccessListener {
                Log.d("BookActivity", "DocumentSnapshot successfully updated!")
            }
            .addOnFailureListener { e ->
                Log.e("BookActivity", "Error updating document", e)
            }
    }

    private fun loadBookDetails(bookId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?:return

        db.collection("user")
            .document(userId)
            .collection("user_books")
            .document(bookId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    val title = document.getString("title")
                    val cover = document.getString("cover")

                    binding.bookTitle.text = title

                    Glide.with(this)
                        .load(cover)
                        .into(binding.bookImage)

                    val startDate = document.getString("start_date")
                    val endDate = document.getString("end_date")

                    if (startDate != null && endDate != null) {
                        binding.startDate.text = startDate
                        binding.endDate.text = endDate
                    }

                    val totalPage = document.getLong("total_page")?.toInt() ?: 0
                    val currentPage = document.getLong("current_page")?.toInt() ?: 0

                    binding.currentProgressText.text = "${currentPage} / ${totalPage} 쪽"
                    binding.currentProgressBar.progress =
                        if (totalPage > 0) (currentPage.toFloat() / totalPage * 100).toInt() else 0

                    // 목표 진행도 계산을 위해 현재 날짜 데이터 가져오기
                    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                    val today = dateFormat.format(Calendar.getInstance().time)

                    db.collection("user")
                        .document(userId)
                        .collection("reading_schedule")
                        .document(today)
                        .collection("books")
                        .document(bookId)
                        .get()
                        .addOnSuccessListener { bookDoc ->
                            if (bookDoc.exists()) {
                                val goalProgress = bookDoc.getLong("pages")?.toInt() ?: 0

                                // 목표 진행도 업데이트
                                binding.goalProgressText.text = "${goalProgress} / ${totalPage} 쪽"
                                binding.goalProgressBar.progress =
                                    if (totalPage > 0) (goalProgress.toFloat() / totalPage * 100).toInt() else 0
                            } else {
                                Log.e("BookActivity", "No such document in reading_schedule")
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("BookActivity", "Failed to fetch reading schedule data", exception)
                        }

                    /*val goalPage = document.getLong("current_page")
                    // TODO: goalPage의 document.getLong("goal_page")로 바꾸기!!goal page 필요
                    val totalPage = document.getLong("total_page")
                    binding.goalProgressText.text = "${goalPage}/${totalPage} 쪽"
                    binding.currentProgressText.text = "${currentPage}/${totalPage} 쪽"
                    // TODO: 아래 goalProgressBar의 current_page -> goal_page
                    binding.goalProgressBar.progress =
                        ((document.getLong("current_page")?.toFloat() ?: 0f) / (document.getLong("total_page")?.toFloat() ?: 1f) * 100).toInt()
                    binding.currentProgressBar.progress =
                        ((document.getLong("current_page")?.toFloat() ?: 0f) / (document.getLong("total_page")?.toFloat() ?: 1f) * 100).toInt()*/
                } else{
                    Log.e("BookActivity", "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("BookActivity", "get failed with ", exception)
            }
    }


}