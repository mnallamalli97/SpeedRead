package com.example.mnallamalli97.speedread

import android.app.Activity
import android.app.DownloadManager
import android.app.DownloadManager.Request
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.mnallamalli97.speedread.R.drawable
import com.example.mnallamalli97.speedread.R.id
import com.example.mnallamalli97.speedread.R.layout
import com.example.mnallamalli97.speedread.news.NewsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
  private var pauseButton: Button? = null
  private var importButton: Button? = null
  private var newsButton: Button? = null
  private var rewindButton: Button? = null
  private var forwardButton: Button? = null
  private var wordTextView: TextView? = null
  private var wordSpeedTextView: TextView? = null
  private var bookTitleTextView: TextView? = null
  private var bookProgress: ProgressBar? = null
  private var newSpeed: Int = 0
  private var bookTitle: String? = null
  private var bookAuthor: String? = null
  private var bookPath: String? = null
  private var bookSummaryPath: String? = null
  private var bookContent: String? = null
  private var currentWordCount = 0
  private var totalWordCount = 0
  private var isDarkModeOn = false
  private var pref: SharedPreferences? = null
  private var editor: SharedPreferences.Editor? = null
  private var userUploadUri: String? = null
  private var newsTimer = Timer()
  private var booksTimer = Timer()
  private var userUploadsTimer = Timer()

  private var pauseTimer = false
  var firebaseStorage =
    FirebaseStorage.getInstance("gs://speedread1214.appspot.com/")
  var storageReference: StorageReference? = null
  var ref: StorageReference? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    pref = applicationContext.getSharedPreferences("MyPref", 0) // 0 - for private mode
    editor = pref!!.edit()
    isDarkModeOn = pref!!.getBoolean("isDarkModeOn", false)
    if (isDarkModeOn) {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
    else {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
    super.onCreate(savedInstanceState)
    setContentView(layout.activity_main)

    wordTextView = findViewById(id.mainWord)
    wordSpeedTextView = findViewById(id.wordSpeed)
    bookTitleTextView = findViewById(id.bookTitle)
    pauseButton = findViewById(id.pauseButton)
    importButton = findViewById(id.importButton)
    newsButton = findViewById(id.newsButton)
    forwardButton = findViewById(id.forwardButton)
    rewindButton = findViewById(id.rewindButton)
    bookProgress = findViewById(id.bookProgress)

    val extras = intent.extras!!
    newSpeed = pref!!.getInt("speedreadSpeed", 250)
    bookTitle = extras.getString("title", "BOOKTITLE")
    bookAuthor = extras.getString("author", "BOOKAUTHOR")
    bookPath = extras.getString("book_path")
    bookSummaryPath = extras.getString("book_summary_path")
    bookContent = extras.getString("content", "BOOKCONTENT")
    currentWordCount = extras.getInt("wordCount", 0)
    userUploadUri = extras.getString("userUploadUri", "USERUPLOADURI")
    wordSpeedTextView!!.text = String.format(resources.getString(R.string.words_per_min_settings_page),  newSpeed.toString())
    bookTitleTextView!!.text = bookTitle.toString()

    val bottomNavigationView =
      findViewById<View>(id.bottomNavigation) as BottomNavigationView
    bottomNavigationView.setOnNavigationItemSelectedListener { item ->
      when (item.itemId) {
        id.action_settings -> {
          val startSettingsIntent =
            Intent(this@MainActivity, SettingsActivity::class.java)
          val extras = Bundle()
          editor!!.putInt("speedreadSpeed", newSpeed)
          editor!!.apply()
          extras.putString("title", bookTitle)
          extras.putString("author", bookAuthor)
          extras.putString("book_path", bookPath)
          extras.putString("book_summary_path", bookSummaryPath)
          extras.putInt("wordCount", currentWordCount)
          extras.putString("content", bookContent)
          extras.putString("userUploadUri", userUploadUri)
          startSettingsIntent.putExtras(extras)
          startActivity(startSettingsIntent)
        }
        id.action_library -> {
          val startLibraryIntent = Intent(this@MainActivity, LibraryActivity::class.java)
          startActivity(startLibraryIntent)
        }
      }
      true
    }

    if (bookAuthor == "Google News") {
      runNewsWords(bookContent, newSpeed)
    } else if (bookAuthor == "User") {
      readTextFromUri(Uri.parse(userUploadUri))
    } else {
      if (bookTitle!!.contains("Summary")) {
        runLibraryWords(bookSummaryPath = bookSummaryPath, speed = newSpeed)
      } else {
        runLibraryWords(bookPath = bookPath, speed = newSpeed)
      }
    }

    pauseButton!!.setOnClickListener(OnClickListener {
      if (pauseTimer) {
        pauseButton!!.setBackgroundResource(drawable.ic_pause_circle_outline_black_72dp)
      } else {
        pauseButton!!.setBackgroundResource(drawable.ic_play_circle_outline_black_72dp)
      }
      pauseTimer = !pauseTimer
    })
    rewindButton!!.setOnClickListener(OnClickListener { // decrease i by 10
      currentWordCount = if (currentWordCount - 10 > 0) currentWordCount - 10 else 0
    })
    forwardButton!!.setOnClickListener(OnClickListener { // increase i by 10
      currentWordCount = if (currentWordCount + 10 < totalWordCount) currentWordCount + 10 else 0
    })
    importButton!!.setOnClickListener(
        OnClickListener { // open the pick to select and import the file
          pauseTimer = true
          readUserUploadWords()
        })
    newsButton!!.setOnClickListener(
        OnClickListener { // open up a news feed and be able to select an article to play
          val startNewsIntent = Intent(this@MainActivity, NewsActivity::class.java)
          startActivity(startNewsIntent)
        })
  }

  fun runLibraryWords(
    bookPath: String? = null,
    bookSummaryPath: String? = null,
    speed: Int
  ) {
    if (bookPath != null) {

      // DOWNLOADING BOOK ONTO DEVICE
      // Instantiates a client
      storageReference = firebaseStorage.reference
      ref = storageReference!!.child("books/$bookPath")
      ref!!.downloadUrl
          .addOnSuccessListener { uri ->
            val url = uri.toString()
            downloadFileAndShowWords(
                this@MainActivity, bookPath, ".txt", Environment.DIRECTORY_DOWNLOADS,
                url, speed
            )
          }
          .addOnFailureListener { e -> e.printStackTrace() }
    } else if (bookSummaryPath != null) {
      storageReference = firebaseStorage.reference
      ref = storageReference!!.child("books/$bookSummaryPath")
      ref!!.downloadUrl
          .addOnSuccessListener { uri ->
            val url = uri.toString()
            downloadFileAndShowWords(
                this@MainActivity, bookSummaryPath, ".txt", Environment.DIRECTORY_DOWNLOADS,
                url, speed
            )
          }
          .addOnFailureListener { e -> e.printStackTrace() }
    } else {

      runNewsWords(bookContent, speed)
    }
  }

  private fun runNewsWords(
      bookContent: String?,
      speed: Int
  ) {
    val wc = bookContent!!.split(" ".toRegex())
        .toTypedArray()
    totalWordCount = wc.size
    bookProgress!!.max = totalWordCount
    val switchWordPeriod = (MILLISECOND_PER_MINUTE / speed).toLong()

    booksTimer.cancel()
    userUploadsTimer.cancel()

    newsTimer.scheduleAtFixedRate(object : TimerTask() {
      override fun run() {
        runOnUiThread {
          if (totalWordCount != 1 && currentWordCount < totalWordCount) {
            wordTextView!!.text = wc[currentWordCount]
            if (!pauseTimer) currentWordCount++
            bookProgress!!.progress = currentWordCount
          } else {
            newsTimer.cancel()
          }
        }
      }
    }, 500, switchWordPeriod)
  }

  fun downloadFileAndShowWords(
      context: Context,
      fileName: String?,
      fileExtension: String,
      destinationDirectory: String?,
      url: String?,
      speed: Int
  ) {
    val downloadManager =
      context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val uri = Uri.parse(url)
    val request = Request(uri)
    request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    request.setDestinationInExternalFilesDir(
        context, destinationDirectory, fileName + fileExtension
    )

    // Local temp file has been created
    // load file into input stream and split each word to display in textview
    var line: String
    val downloadPath = if (bookPath != null) arrayOf(
        "/storage/emulated/0/Android/data/com.mnallamalli97.speedread/files/Download/$bookPath.txt"
    ) else arrayOf(
        "/storage/emulated/0/Android/data/com.mnallamalli97.speedread/files/Download/$bookSummaryPath.txt"
    )

    val tmpDir = File(downloadPath[0])
    val exists = tmpDir.exists()
    if (!exists) {
      downloadManager.enqueue(request)
    }
    try {
      FileInputStream(downloadPath[0])
          .use { `is` ->
            InputStreamReader(`is`, Charset.forName("UTF-8"))
                .use { isr ->
                  BufferedReader(isr)
                      .use { br ->
                        // readLine here will be an entire chapter
                        line = br.readLine()
                        val wc = line.split(" ".toRegex())
                            .toTypedArray()

                        totalWordCount = wc.size
                        bookProgress!!.max = totalWordCount
                        val switchWordPeriod = (MILLISECOND_PER_MINUTE / speed).toLong()

                        newsTimer.cancel()
                        userUploadsTimer.cancel()

                        booksTimer.scheduleAtFixedRate(object : TimerTask() {
                          override fun run() {
                            runOnUiThread {
                              if (totalWordCount != 1 && currentWordCount < totalWordCount) {
                                wordTextView!!.text = wc[currentWordCount]
                                if (!pauseTimer) currentWordCount++
                                bookProgress!!.progress = currentWordCount
                              } else {
                                booksTimer.cancel()
                              }
                            }
                          }
                        }, 500, switchWordPeriod)
                      }
                }
          }
    } catch (e: FileNotFoundException) {
      e.printStackTrace()
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }

  private fun readUserUploadWords() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = "text/plain"
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    startActivityForResult(intent, PICK_PDF_FILE)
  }

  override fun onActivityResult(
      requestCode: Int,
      resultCode: Int,
      data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == PICK_PDF_FILE && resultCode == Activity.RESULT_OK) {
      data?.data?.also { uri ->
        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
        intent.putExtra("title", getFileName(uri))
        intent.putExtra("author", "User")
        intent.putExtra("userUploadUri", uri.toString())
        startActivity(intent)
      }
    }
  }

  @Throws(IOException::class)
  private fun readTextFromUri(uri: Uri?) {

    try {
      var line: String
      InputStreamReader(contentResolver.openInputStream(uri!!))
          .use { isr ->
            BufferedReader(isr)
                .use { br ->
                  // readLine here will be an entire chapter
                  line = br.readLine()
                  val wc = line.split(" ".toRegex())
                      .toTypedArray()

                  totalWordCount = wc.size
                  bookProgress!!.max = totalWordCount

                  val switchWordPeriod = (MILLISECOND_PER_MINUTE / newSpeed).toLong()
                  booksTimer.cancel()
                  newsTimer.cancel()

                  userUploadsTimer.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                      runOnUiThread {
                        if (totalWordCount != 1 && currentWordCount < totalWordCount) {
                          wordTextView!!.text = wc[currentWordCount]
                          if (!pauseTimer) currentWordCount++
                          bookProgress!!.progress = currentWordCount
                        } else {
                          userUploadsTimer.cancel()
                        }
                      }
                    }
                  }, 500, switchWordPeriod)
                }
          }
    } catch (e: NullPointerException) { }
  }

  fun getFileName(uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
      val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
      try {
        if (cursor != null && cursor.moveToFirst()) {
          result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
      } finally {
        cursor!!.close()
      }
    }
    if (result == null) {
      result = uri.path
      val cut = result!!.lastIndexOf('/')
      if (cut != -1) {
        result = result!!.substring(cut + 1)
      }
    }
    return result
  }

  companion object {
    // Request code for selecting a PDF document.
    private const val PICK_PDF_FILE = 1214
    private const val MILLISECOND_PER_MINUTE = 60000
  }
}
