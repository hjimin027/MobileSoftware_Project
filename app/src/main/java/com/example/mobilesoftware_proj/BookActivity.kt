package com.example.mobilesoftware_proj

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
//                    .setSelection(
//                        Pair(
//                            MaterialDatePicker.thisMonthInUtcMilliseconds(),
//                            MaterialDatePicker.todayInUtcMilliseconds()
//                        )
//                    )
                    .build()

            dateRangePicker.show(supportFragmentManager, "date_picker")
            dateRangePicker.addOnPositiveButtonClickListener(object :
                MaterialPickerOnPositiveButtonClickListener<Pair<Long, Long>> {
                    override fun onPositiveButtonClick(selection: Pair<Long, Long>?) {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = selection?.first ?: 0
                        binding.startDate.text = SimpleDateFormat("yyyy.MM.dd").format(calendar.time).toString()

                        calendar.timeInMillis = selection?.second ?: 0
                         binding.endDate.text = SimpleDateFormat("yyyy.MM.dd").format(calendar.time).toString()
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

}