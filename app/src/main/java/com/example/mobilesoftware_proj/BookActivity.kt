package com.example.mobilesoftware_proj

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobilesoftware_proj.databinding.ActivityBookBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import androidx.core.util.Pair
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BookActivity : AppCompatActivity() {
    val binding by lazy { ActivityBookBinding.inflate(layoutInflater) }
    private val db = FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

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
                } else{
                    Log.e("BookActivity", "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("BookActivity", "get failed with ", exception)
            }
    }

}