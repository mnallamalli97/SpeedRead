package com.example.mnallamalli97.speedread

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.Switch
import android.widget.TextView
import com.example.mnallamalli97.speedread.R.id
import com.example.mnallamalli97.speedread.R.layout
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams

class SettingsActivity : AppCompatActivity() {
  private var seekBarValue: Int = 0
  private var seekBar: IndicatorSeekBar? = null
  private var saveButton: Button? = null
  private var libraryButton: Button? = null
  private var resultText: TextView? = null
  private var bookTitle: TextView? = null
  private var bookAuthor: TextView? = null
  private var bookPath: TextView? = null
  private var darkMode: Switch? = null
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
    libraryButton = findViewById(id.libraryButton)
    seekBar = findViewById(id.seekBar)
    resultText = findViewById(id.resultText)
    bookAuthor = findViewById(id.bookAuthor)
    bookTitle = findViewById(id.bookTitle)
    bookPath = findViewById(id.bookPath)
    darkMode = findViewById(id.darkMode)

    result = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
      darkMode!!.setChecked(true)
      true
    } else {
      darkMode!!.setChecked(false)
      false
    }
    darkMode!!.setOnCheckedChangeListener(OnCheckedChangeListener { buttonView, isChecked ->
      // do something, the isChecked will be
      // true if the switch is in the On position
      if (darkMode!!.isChecked()) {
        // darkMode.setBackgroundResource(R.drawable.switchbuttonfalse);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        editor!!.putBoolean("darkModeEnabled", true)
        editor!!.apply()
        val intent = intent
        intent.putExtra("darkModeEnabled", true)
        finish()
        startActivity(intent)
      } else {
        // darkMode.setBackgroundResource(R.drawable.switchbuttontrue);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        editor!!.putBoolean("darkModeEnabled", false)
        editor!!.apply()
        val intent = intent
        intent.putExtra("darkModeEnabled", true)
        finish()
        startActivity(intent)
      }
    })
    val author = extras.getString("author")
    val title = extras.getString("title")
    val path = extras.getString("book_path")
    val content = extras.getString("content")
    wordCount = extras.getInt("wordCount")
    seekBarValue = pref!!.getInt("speedreadSpeed", 250)
    userUploadUri = extras.getString("userUploadUri")

    bookTitle!!.setText(title)
    bookPath!!.setText(path)
    bookAuthor!!.setText(author)
    resultText!!.text = seekBarValue.toString()
    seekBar!!.setProgress(seekBarValue.toFloat())
    seekBar!!.onSeekChangeListener = object : OnSeekChangeListener {
      override fun onSeeking(seekParams: SeekParams) {
        seekBarValue = seekParams.progress
        resultText!!.text = seekBarValue.toString()
      }
      override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}
      override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
        resultText!!.text = seekBar.progress.toString()
        editor!!.putInt("speedReadSpeed", seekBarValue)
        editor!!.apply()
      }
    }

    saveButton!!.setOnClickListener(OnClickListener {
      val startIntent = Intent(this@SettingsActivity, MainActivity::class.java)
      val extras = Bundle()
      editor!!.putInt("speedreadSpeed", seekBarValue)
      editor!!.apply()
      extras.putString("title", title)
      extras.putString("author", author)
      extras.putString("book_path", path)
      extras.putBoolean("darkModeEnabled", result)
      extras.putString("content", content)
      extras.putInt("wordCount", wordCount)
      extras.putString("userUploadUri", userUploadUri)
      startIntent.putExtras(extras)
      startActivity(startIntent)
    })
    libraryButton!!.setOnClickListener(OnClickListener {
      val startIntent = Intent(this@SettingsActivity, LibraryActivity::class.java)
      startActivity(startIntent)
    })
  }
}
