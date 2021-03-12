package com.example.mnallamalli97.speedread.news

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.example.mnallamalli97.speedread.LibraryActivity
import com.example.mnallamalli97.speedread.R
import com.example.mnallamalli97.speedread.R.drawable
import com.example.mnallamalli97.speedread.R.id
import com.example.mnallamalli97.speedread.R.layout
import com.example.mnallamalli97.speedread.SettingsActivity
import com.example.mnallamalli97.speedread.models.Article
import com.google.android.material.floatingactionbutton.FloatingActionButton
import nl.bryanderidder.themedtogglebuttongroup.ThemedToggleButtonGroup
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.ArrayList

class NewsActivity : AppCompatActivity() {
  private var recyclerView: RecyclerView? = null
  private var layoutManager: LayoutManager? = null
  private var articles: MutableList<Article> = ArrayList()
  private var adapter: Adapter? = null
  private var newsActivityRootView: ConstraintLayout? = null
  private var loadingArticleProgressBar: ProgressBar? = null
  private var errorLayout: RelativeLayout? = null
  private var errorImage: ImageView? = null
  private var errorTitle: TextView? = null
  private var test: TextView? = null
  private var errorMessage: TextView? = null
  private var btnRetry: Button? = null
  // private var newsUpgradeButton: Button? = null
  private var isDarkModeOn = false
  private var pref: SharedPreferences? = null
  private var editor: SharedPreferences.Editor? = null
  private var selectedCategory: String? = null
  private var selectedArticle: Article? = null
  private var fullArticleContent = ""
  private var articleId = ""
  private var articleMapping: MutableMap<String, String> = mutableMapOf()

  private var th1: Thread? = null
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
    setContentView(layout.news_activity)
    recyclerView = findViewById(id.recyclerView)
    newsActivityRootView = findViewById(R.id.newsLayout)
    layoutManager = LinearLayoutManager(this@NewsActivity)
    recyclerView!!.setLayoutManager(layoutManager)
    recyclerView!!.setItemAnimator(DefaultItemAnimator())
    recyclerView!!.setNestedScrollingEnabled(false)
    errorLayout = findViewById(id.errorLayout)
    loadingArticleProgressBar = findViewById(id.loadingArticleProgressBar)
    errorImage = findViewById(id.errorImage)
    errorTitle = findViewById(id.errorTitle)
    test = findViewById(id.test)

    errorMessage = findViewById(id.errorMessage)
    // newsUpgradeButton = findViewById(id.newsUpgradeButton)
    btnRetry = findViewById(id.btnRetry)
    val libraryFAB = findViewById<FloatingActionButton>(id.libraryFAB)
    val uploadFAB = findViewById<FloatingActionButton>(id.uploadFAB)

    // newsUpgradeButton!!.visibility = View.VISIBLE

    libraryFAB.setOnClickListener {
      val intent = Intent(this@NewsActivity, LibraryActivity::class.java)
      this@NewsActivity.startActivity(intent)
      this.overridePendingTransition(0, 0)
    }

    uploadFAB.setOnClickListener {
      val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
      intent.addCategory(Intent.CATEGORY_OPENABLE)
      intent.type = "text/plain"
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      startActivityForResult(intent, 1214)
    }




    th1 = Thread( object : Thread() {
      override fun run() {
        getUrlsFromGoogleNews("general")
      }
    })
    th1!!.start()

//    newsUpgradeButton!!.setOnTouchListener(object : OnTouchListener {
//      override fun onTouch(
//        v: View?,
//        event: MotionEvent
//      ): Boolean {
//        val DRAWABLE_RIGHT = 2
//        if (event.action === MotionEvent.ACTION_DOWN) {
//          if (event.rawX >= newsUpgradeButton!!.right - newsUpgradeButton!!.compoundDrawables[DRAWABLE_RIGHT]
//                  .bounds
//                  .width()
//          ) {
//            newsUpgradeButton!!.visibility = View.GONE
//            return true
//          }
//        }
//        return false
//      }
//    })
//
//    newsUpgradeButton!!.setOnClickListener {
//      showDialog()
//    }

  }

  fun getUrlsFromGoogleNews(newsCategory: String) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(
            "https://newsapi.org/v2/top-headlines?category=$newsCategory&country=us&apiKey=6c5a6f37e8dc4f6194dce74bf821d4b7"
        )
        .build()

    client.newCall(request)
        .enqueue(object : okhttp3.Callback {
          override fun onFailure(
            call: okhttp3.Call,
            e: IOException
          ) {
            showErrorMessage(
                drawable.oops,
                "Oops..",
                """
              Network failure, Please Try Again
              $e
              """.trimIndent()
            )
          }

          override fun onResponse(
            call: okhttp3.Call,
            response: okhttp3.Response
          ) {
            val jsonData = response.body!!.string()

            val obj = JSONObject(jsonData)
            val resultArray: org.json.JSONArray = obj.getJSONArray("articles")
            for (i in 0 until resultArray.length()) {
              val obj: JSONObject = resultArray.getJSONObject(i)
              //store your variable
              articleId = i.toString()
              val title = obj.getString("title")
              val author = obj.getString("author")
              val url = obj.getString("url")
              val description = obj.getString("description")
              val urlToImage = obj.getString("urlToImage")
              val publishedDate = obj.getString("publishedAt")
              val sourceArray: org.json.JSONObject = obj.getJSONObject("source")
              val source = sourceArray.getString("name")

              articleMapping[articleId] = url
              articles.add(Article(title, author, url, description, urlToImage, publishedDate, source))
            }



            runOnUiThread {
              adapter = Adapter(articles, this@NewsActivity)
              adapter!!.notifyDataSetChanged()
              recyclerView!!.adapter = adapter
              initListener()
            }


          }

        })
  }

  fun getFullContent(selectedArticle: Article?) {
    val client = OkHttpClient()
    val mediaType = "application/json".toMediaTypeOrNull()
    val body = RequestBody.create(mediaType, "{\"url\":\"${selectedArticle?.url}\"}")
    val request = Request.Builder()
        .url("https://news-article-extraction.p.rapidapi.com/")
        .post(body)
        .addHeader("content-type", "application/json")
        .addHeader("x-rapidapi-key", "f46e5c8c90msh9778f06fc8c5a49p16d051jsn87608541b961")
        .addHeader("x-rapidapi-host", "news-article-extraction.p.rapidapi.com")
        .build()

    client.newCall(request)
        .enqueue(object : okhttp3.Callback {
          override fun onFailure(
            call: okhttp3.Call,
            e: IOException
          ) {
            showErrorMessage(
                drawable.oops,
                "Oops..",
                """
              Network failure, Please Try Again
              $e
              """.trimIndent()
            )
          }

          override fun onResponse(
            call: okhttp3.Call,
            response: okhttp3.Response
          ) {
            val jsonData = response.body!!.string()
            Log.d("testArticle", jsonData)

            val obj = JSONObject(jsonData)
            val resultArray: org.json.JSONObject = obj.getJSONObject("result")
            for (i in 0 until resultArray.length()) {

              val author = resultArray.getString("author")
              fullArticleContent = resultArray.getString("content")
            }

            startActivityForResult(intent, 1215)





          }
        })
  }

  private fun showUpgradeDialog() {
    val dialog = Dialog(this@NewsActivity)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setCancelable(false)
    dialog.setContentView(R.layout.upgrade_to_full)
    val closeButton = dialog.findViewById(R.id.close_button) as ImageButton
    val upgradeButton = dialog.findViewById(R.id.upgrade_button) as Button
    closeButton.setOnClickListener {
      dialog.dismiss()
    }
    upgradeButton.setOnClickListener { }
    dialog.show()

    val layoutParams: WindowManager.LayoutParams = LayoutParams()
    layoutParams.copyFrom(
        dialog.window!!.attributes
    )
    layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
    layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
    dialog.window!!.attributes = layoutParams
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == 1214 && resultCode == Activity.RESULT_OK) {
      data?.data?.also { uri ->
        val intent = Intent(this@NewsActivity, SettingsActivity::class.java)
        intent.putExtra("title", getFileName(uri))
        intent.putExtra("author", "User")
        intent.putExtra("userUploadUri", uri.toString())
        startActivity(intent)
      }
    }

    if (requestCode == 1215) {

        val intent = Intent(this@NewsActivity, SettingsActivity::class.java)

      val article = selectedArticle!!

      intent.putExtra("url", article.url)
      intent.putExtra("title", article.title)
      intent.putExtra("img", article.image)
      intent.putExtra("date", article.publishedDate)
      intent.putExtra("source", article.source)
      intent.putExtra("author", "Google News")
      intent.putExtra("content", fullArticleContent)

      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
      loadingArticleProgressBar!!.visibility = View.GONE
      startActivity(intent)

    }

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


  private fun initListener() {
    adapter!!.setOnItemClickListener { view, position ->
      loadingArticleProgressBar!!.visibility = View.VISIBLE
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
          WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
      object : Thread() {
        override fun run() {
          selectedArticle = articles.get(index = position)
          getFullContent(selectedArticle)
        }
      }.start()
    }



  }

  private fun showErrorMessage(
    imageView: Int,
    title: String,
    message: String
  ) {
    if (errorLayout!!.visibility == View.GONE) {
      errorLayout!!.visibility = View.VISIBLE
    }
    errorImage!!.setImageResource(imageView)
    errorTitle!!.text = title
    errorMessage!!.text = message
    // btnRetry!!.setOnClickListener { LoadJson("") }
  }

  companion object {
    const val API_KEY = "6c5a6f37e8dc4f6194dce74bf821d4b7"
  }
}


