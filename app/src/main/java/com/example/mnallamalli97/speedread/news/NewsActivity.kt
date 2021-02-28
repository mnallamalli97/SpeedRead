package com.example.mnallamalli97.speedread.news

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.Window
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.mnallamalli97.speedread.LibraryActivity
import com.example.mnallamalli97.speedread.R
import com.example.mnallamalli97.speedread.R.color
import com.example.mnallamalli97.speedread.R.drawable
import com.example.mnallamalli97.speedread.R.id
import com.example.mnallamalli97.speedread.R.layout
import com.example.mnallamalli97.speedread.SettingsActivity
import com.example.mnallamalli97.speedread.api.ApiClient
import com.example.mnallamalli97.speedread.api.ApiInterface
import com.example.mnallamalli97.speedread.models.Article
import com.example.mnallamalli97.speedread.models.News
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList

class NewsActivity : AppCompatActivity(), OnRefreshListener {
  private var recyclerView: RecyclerView? = null
  private var layoutManager: LayoutManager? = null
  private var articles: MutableList<Article> = ArrayList()
  private var adapter: Adapter? = null
  private val TAG = NewsActivity::class.java.simpleName
  private var topHeadline: TextView? = null
  private var swipeRefreshLayout: SwipeRefreshLayout? = null
  private var newsActivityRootView: CoordinatorLayout? = null
  private var errorLayout: RelativeLayout? = null
  private var errorImage: ImageView? = null
  private var errorTitle: TextView? = null
  private var errorMessage: TextView? = null
  private var btnRetry: Button? = null
  private var newsUpgradeButton: Button? = null
  private var isDarkModeOn = false
  private var pref: SharedPreferences? = null
  private var editor: SharedPreferences.Editor? = null
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
    swipeRefreshLayout = findViewById(id.swipe_refresh_layout)
    swipeRefreshLayout!!.setOnRefreshListener(this)
    swipeRefreshLayout!!.setColorSchemeResources(color.colorPrimaryDark)
    topHeadline = findViewById(id.topheadelines)
    recyclerView = findViewById(id.recyclerView)
    newsActivityRootView = findViewById(R.id.newsLayout)
    layoutManager = LinearLayoutManager(this@NewsActivity)
    recyclerView!!.setLayoutManager(layoutManager)
    recyclerView!!.setItemAnimator(DefaultItemAnimator())
    recyclerView!!.setNestedScrollingEnabled(false)
    onLoadingSwipeRefresh("")
    errorLayout = findViewById(id.errorLayout)
    errorImage = findViewById(id.errorImage)
    errorTitle = findViewById(id.errorTitle)
    errorMessage = findViewById(id.errorMessage)
    newsUpgradeButton = findViewById(id.newsUpgradeButton)
    btnRetry = findViewById(id.btnRetry)
    val libraryFAB = findViewById<FloatingActionButton>(id.libraryFAB)
    val uploadFAB = findViewById<FloatingActionButton>(id.uploadFAB)
    newsUpgradeButton!!.visibility = View.VISIBLE

    libraryFAB.setOnClickListener {
      val intent = Intent(this@NewsActivity, LibraryActivity::class.java)
      this@NewsActivity.startActivity(intent)
      this.overridePendingTransition(0,0)
    }

    uploadFAB.setOnClickListener {
      val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
      intent.addCategory(Intent.CATEGORY_OPENABLE)
      intent.type = "text/plain"
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      startActivityForResult(intent, 1214)
    }

    newsUpgradeButton!!.setOnTouchListener(object : OnTouchListener {
      override fun onTouch(
        v: View?,
        event: MotionEvent
      ): Boolean {
        val DRAWABLE_RIGHT = 2
        if (event.action === MotionEvent.ACTION_DOWN) {
          if (event.rawX >= newsUpgradeButton!!.right - newsUpgradeButton!!.compoundDrawables[DRAWABLE_RIGHT]
                  .bounds
                  .width()
          ) {
            newsUpgradeButton!!.visibility = View.GONE
            return true
          }
        }
        return false
      }
    })

    newsUpgradeButton!!.setOnClickListener {
      showDialog()
    }

  }

  private fun showDialog() {
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

  fun LoadJson(keyword: String) {
    errorLayout!!.visibility = View.GONE
    swipeRefreshLayout!!.isRefreshing = true
    val apiInterface = ApiClient.getApiClient()
        .create(
            ApiInterface::class.java
        )
    val country = Utils.getCountry()
    val language = Utils.getLanguage()
    val call: Call<News>
    call = if (keyword.length > 0) {
      apiInterface.getNewsSearch(keyword, language, "publishedAt", API_KEY)
    } else {
      apiInterface.getNews(country, API_KEY)
    }
    call.enqueue(object : Callback<News> {
      override fun onResponse(
        call: Call<News>,
        response: Response<News>
      ) {
        if (response.isSuccessful && response.body()
                !!.getArticle() != null
        ) {
          if (!articles.isEmpty()) {
            articles.clear()
          }
          articles = response.body()!!.article

          adapter = Adapter(articles, this@NewsActivity)
          recyclerView!!.adapter = adapter
          adapter!!.notifyDataSetChanged()
          initListener()
          topHeadline!!.visibility = View.VISIBLE
          swipeRefreshLayout!!.isRefreshing = false
        } else {
          topHeadline!!.visibility = View.INVISIBLE
          swipeRefreshLayout!!.isRefreshing = false
          val errorCode: String
          errorCode = when (response.code()) {
            404 -> "404 not found"
            500 -> "500 server broken"
            else -> "unknown error"
          }
          showErrorMessage(
              drawable.no_result,
              "No Result",
              """
                Please Try Again!
                $errorCode
                """.trimIndent()
          )
        }
      }

      override fun onFailure(
        call: Call<News>,
        t: Throwable
      ) {
        topHeadline!!.visibility = View.INVISIBLE
        swipeRefreshLayout!!.isRefreshing = false
        showErrorMessage(
            drawable.oops,
            "Oops..",
            """
              Network failure, Please Try Again
              $t
              """.trimIndent()
        )
      }
    })
  }

  private fun initListener() {
    adapter!!.setOnItemClickListener { view, position ->
      val imageView =
        view.findViewById<ImageView>(id.img)
      val intent = Intent(this@NewsActivity, SettingsActivity::class.java)
      val article = articles[position]
      intent.putExtra("url", article.url)
      intent.putExtra("title", article.title)
      intent.putExtra("img", article.urlToImage)
      intent.putExtra("date", article.publishedAt)
      intent.putExtra(
          "source", article.source
          .name
      )
      intent.putExtra("author", "Google News")
      intent.putExtra("content", article.content)
      val pair =
        Pair.create(
            imageView as View, ViewCompat.getTransitionName(imageView)
        )
      startActivityForResult(intent, 1214)
    }
  }
  override fun onRefresh() {
    LoadJson("")
  }

  private fun onLoadingSwipeRefresh(keyword: String) {
    swipeRefreshLayout!!.post { LoadJson(keyword) }
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
    btnRetry!!.setOnClickListener { onLoadingSwipeRefresh("") }
  }

  companion object {
    const val API_KEY = "6c5a6f37e8dc4f6194dce74bf821d4b7"
  }
}





/*
This code is used to fetch the urls of the top 100 news articles
Use the second request builder to to call each of those requests to get the content of those articles
 */


// https://rapidapi.com/newscatcher-api-newscatcher-api-default/api/newscatcher?endpoint=apiendpoint_afd4bf15-9861-4285-b122-95d6df0de330
//private var json: JSONObject? = null
//
//private var sample: TextView? = null
//
//
//    // call send message here
//
//    val client = OkHttpClient()
//
//    val getTop100Articles = Request.Builder()
//        .url("https://newscatcher.p.rapidapi.com/v1/latest_headlines?lang=en&media=True")
//        .get()
//        .addHeader("x-rapidapi-key", "f46e5c8c90msh9778f06fc8c5a49p16d051jsn87608541b961")
//        .addHeader("x-rapidapi-host", "newscatcher.p.rapidapi.com")
//        .build()
//
//    client.newCall(getTop100Articles).enqueue(object : Callback {
//      override fun onFailure(
//        call: Call,
//        e: IOException
//      ) {
//        TODO("Not yet implemented")
//      }
//
//      override fun onResponse(
//        call: Call,
//        response: Response
//      ) {
//        response.use {
//          if (!response.isSuccessful) throw IOException("Unexpected code $response")
//          else {
//            val resStr = response.body!!.string()
//            json = JSONObject(resStr)
//            val keys: Iterator<String> = json!!.keys()
//
//            val content = json!!.getString("link")
//
//            for (i in 0 until articles.length()) {
//              val item = persons.getJSONObject(i)
//
//              // Your code here
//            }
//
//          }
//        }
//      }
//
//    })
//

//
//    https://rapidapi.com/newscatcher-api-newscatcher-api-default/api/news-parser1?endpoint=apiendpoint_627103d7-92e1-4109-8d92-3a77b63302da
//
//    val request = Request.Builder()
//        .url("https://news-parser1.p.rapidapi.com/article_v1?url=https%3A%2F%2Fwww.phillyvoice.com%2Fsatisfaction-bariatric-surgery-often-decreases-over-time-new-study-finds")
//        .get()
//        .addHeader("x-rapidapi-key", "f46e5c8c90msh9778f06fc8c5a49p16d051jsn87608541b961")
//        .addHeader("x-rapidapi-host", "news-parser1.p.rapidapi.com")
//        .build()
//
//
//    client.newCall(request).enqueue(object : Callback {
//      override fun onFailure(
//        call: Call,
//        e: IOException
//      ) {
//        TODO("Not yet implemented")
//      }
//
//      override fun onResponse(
//        call: Call,
//        response: Response
//      ) {
//        response.use {
//          if (!response.isSuccessful) throw IOException("Unexpected code $response")
//          else {
//            val resStr = response.body!!.string()
//            json = JSONObject(resStr)
//
//            val content = json!!.getString("content_1")
//            println(content)
//
//          }
//        }
//      }
//
//    })

//
