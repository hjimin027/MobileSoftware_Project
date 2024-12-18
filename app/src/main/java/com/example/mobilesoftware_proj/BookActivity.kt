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
        // Firestore에 저장할 데이터
        val dateData = hashMapOf(
            "bookId" to bookId,
            "start_date" to startDate,
            "end_date" to endDate
        )
        // Firestore에서 기존 문서를 찾고 업데이트
        db.collection("user")
            .document(userId)
            .collection("calendar")
            .whereEqualTo("bookId", bookId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // 문서가 없다면 새로 추가
                    db.collection("user")
                        .document(userId)
                        .collection("calendar")
                        .add(dateData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "날짜가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Log.e("BookActivity", "날짜 저장 실패", exception)
                            Toast.makeText(this, "날짜 저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // 문서가 존재하면 첫 번째 문서를 업데이트
                    val existingDocument = querySnapshot.documents[0]
                    val documentId = existingDocument.id
                    db.collection("user")
                        .document(userId)
                        .collection("calendar")
                        .document(documentId)
                        .set(dateData) // 기존 문서를 덮어씀
                        .addOnSuccessListener {
                            Toast.makeText(this, "날짜가 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Log.e("BookActivity", "날짜 업데이트 실패", exception)
                            Toast.makeText(this, "날짜 업데이트 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("BookActivity", "날짜 저장 실패", exception)
                Toast.makeText(this, "날짜 저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
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

                    loadDatesFromDatabase(userId, bookId)

                    val goalPage = document.getLong("current_page")
                    // TODO: goalPage의 document.getLong("goal_page")로 바꾸기!!goal page 필요
                    val totalPage = document.getLong("total_page")
                    binding.goalProgressText.text = "${goalPage}/${totalPage} 쪽"
                    binding.currentProgressText.hint = document.getLong("current_page").toString()
                    binding.currentProgressTextTotal.text = "/${totalPage} 쪽"
                    binding.goalProgressBar.progress =
                        ((document.getLong("current_page")?.toFloat() ?: 0f) / (document.getLong("total_page")?.toFloat() ?: 1f) * 100).toInt()

                    binding.currentProgressBar.progress =
                        ((document.getLong("current_page")?.toFloat() ?: 0f) / (document.getLong("total_page")?.toFloat() ?: 1f) * 100).toInt()
                } else{
                    Log.e("BookActivity", "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("BookActivity", "get failed with ", exception)
            }
    }
    private fun loadDatesFromDatabase(userId: String, bookId: String) {
        db.collection("user")
            .document(userId)
            .collection("calendar")
            .whereEqualTo("bookId", bookId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val startDate = document.getString("start_date")
                    val endDate = document.getString("end_date")
                    // 날짜 정보가 있으면 화면에 표시
                    if (startDate != null && endDate != null) {
                        binding.startDate.text = startDate
                        binding.endDate.text = endDate
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("BookActivity", "Error loading dates", exception)
            }
    }

}