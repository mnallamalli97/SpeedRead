package com.example.mnallamalli97.speedread

import android.content.Intent
import android.content.SharedPreferences
import android.media.Image
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.bumptech.glide.Glide
import com.example.mnallamalli97.speedread.R.layout

class CheckoutActivity : AppCompatActivity() {
  private var prePurchasebookTitle: TextView? = null
  private var prePurchaseBookAuthor: TextView? = null
  private var prePurchaseBookCover: ImageView? =null
  private var readSummaryButton: Button? = null
  private var isDarkModeOn = false
  private var pref: SharedPreferences? = null
  private var editor: SharedPreferences.Editor? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    pref = applicationContext.getSharedPreferences("MyPref", 0) // 0 - for private mode
    editor = pref!!.edit()
    isDarkModeOn = pref!!.getBoolean("isDarkModeOn", false)
    if (isDarkModeOn) {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    } else {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
    super.onCreate(savedInstanceState)
    setContentView(layout.pre_purchase_activity)

    prePurchaseBookAuthor = findViewById(R.id.prePurchaseBookAuthor)
    prePurchasebookTitle = findViewById(R.id.prePurchasebookTitle)
    val book_layout_file = findViewById<View>(R.id.prePurchaseBookCover)
    prePurchaseBookCover = book_layout_file.findViewById(R.id.bookCoverImageButton)
    readSummaryButton = findViewById(R.id.readSummaryButton)


    val extras = intent.extras!!
    val id = extras.getString("id")
    val bookPath = extras.getString("book_path")
    val author = extras.getString("author")
    val title = extras.getString("title")
    val bookCoverPath = extras.getString("cover")

    prePurchaseBookAuthor!!.text = author
    prePurchasebookTitle!!.text = title
    val url = bookCoverPath
    Glide.with(applicationContext)
        .load(url)
        .error(R.drawable.ic_library_books_24px)
        .into(prePurchaseBookCover!!)
    prePurchaseBookCover!!.isEnabled = false

    readSummaryButton!!.setOnClickListener {
      val intent = Intent(this@CheckoutActivity, SettingsActivity::class.java)
      intent.putExtra("title", title)
      intent.putExtra("book_path", bookPath)
      intent.putExtra("id", id)
      intent.putExtra("author", author)
      intent.putExtra("cover", bookCoverPath)

      startActivity(intent)
    }

  }
}


