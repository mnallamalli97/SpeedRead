package com.example.mnallamalli97.speedread

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings.System.getString
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.mnallamalli97.speedread.R.color
import com.example.mnallamalli97.speedread.R.id
import com.example.mnallamalli97.speedread.R.layout
import com.warkiz.widget.ColorCollector
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams

class SettingsActivity : AppCompatActivity() {
  private var seekBarValue: Int = 0
  private var seekBar: IndicatorSeekBar? = null
  private var saveButton: Button? = null
  private var resultText: TextView? = null
  private var bookTitle: TextView? = null
  private var bookAuthor: TextView? = null
  private var pref: SharedPreferences? = null
  private var editor: SharedPreferences.Editor? = null
  private var result = false
  private var wordCount = 0
  private var userUploadUri: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(layout.settings_activity)
    pref =
      applicationContext.getSharedPreferences("MyPref", 0) // 0 - for private mode
    editor = pref!!.edit()
    val extras = intent.extras
    saveButton = findViewById(id.saveButton)
    seekBar = findViewById(id.seekBar)
    resultText = findViewById(id.resultText)
    bookAuthor = findViewById(id.bookAuthor)
    bookTitle = findViewById(id.bookTitle)


    val author = extras.getString("author")
    val title = extras.getString("title")
    val path = extras.getString("book_path")
    val bookChaptersNames = extras.getString("bookChaptersNames")
    val bookChaptersPaths = extras.getString("bookChaptersPaths")
    val content = extras.getString("content")
    wordCount = extras.getInt("wordCount")
    seekBarValue = pref!!.getInt("speedreadSpeed", 250)
    userUploadUri = extras.getString("userUploadUri")

    bookTitle!!.setText(title)
    bookAuthor!!.setText(author)
    resultText!!.text = String.format(resources.getString(R.string.words_per_min_settings_page), seekBarValue.toString());
    seekBar!!.setProgress(seekBarValue.toFloat())
    seekBar!!.onSeekChangeListener = object : OnSeekChangeListener {
      override fun onSeeking(seekParams: SeekParams) {
        seekBarValue = seekParams.progress
        resultText!!.text = String.format(resources.getString(R.string.words_per_min_settings_page), seekBarValue.toString());
      }
      override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}
      override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
        resultText!!.text = String.format(resources.getString(R.string.words_per_min_settings_page), seekBarValue.toString());
        editor!!.putInt("speedReadSpeed", seekBarValue)
        editor!!.apply()
      }
    }

    seekBar!!.customSectionTrackColor {
      it[0] = resources.getColor(R.color.color_green, null)
      it[1] = resources.getColor(R.color.color_green, null)
      it[2] = resources.getColor(R.color.color_yellow, null)
      it[3] = resources.getColor(R.color.color_red, null)
      true //true if apply color , otherwise no change

    };

    saveButton!!.setOnClickListener(OnClickListener {
      val startIntent = Intent(this@SettingsActivity, MainActivity::class.java)
      val extras = Bundle()
      editor!!.putInt("speedreadSpeed", seekBarValue)
      editor!!.apply()
      extras.putString("title", title)
      extras.putString("author", author)
      extras.putString("book_path", path)
      extras.putString("bookChaptersNames", bookChaptersNames)
      extras.putString("bookChaptersPaths", bookChaptersPaths)
      extras.putBoolean("darkModeEnabled", result)
      extras.putString("content", content)
      extras.putInt("wordCount", wordCount)
      extras.putString("userUploadUri", userUploadUri)
      startIntent.putExtras(extras)
      finish()
      startActivity(startIntent)
    })
//    libraryButton!!.setOnClickListener(OnClickListener {
//      val startIntent = Intent(this@SettingsActivity, LibraryActivity::class.java)
//      startActivity(startIntent)
//    })
  }
}
