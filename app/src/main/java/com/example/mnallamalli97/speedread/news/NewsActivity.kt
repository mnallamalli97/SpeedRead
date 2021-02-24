package com.example.mnallamalli97.speedread.news

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.mnallamalli97.speedread.LibraryActivity
import com.example.mnallamalli97.speedread.MainActivity
import com.example.mnallamalli97.speedread.MainActivity.Companion
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
  private var errorLayout: RelativeLayout? = null
  private var errorImage: ImageView? = null
  private var errorTitle: TextView? = null
  private var errorMessage: TextView? = null
  private var btnRetry: Button? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(layout.news_activity)
    swipeRefreshLayout = findViewById(id.swipe_refresh_layout)
    swipeRefreshLayout!!.setOnRefreshListener(this)
    swipeRefreshLayout!!.setColorSchemeResources(color.colorPrimaryDark)
    topHeadline = findViewById(id.topheadelines)
    recyclerView = findViewById(id.recyclerView)
    layoutManager = LinearLayoutManager(this@NewsActivity)
    recyclerView!!.setLayoutManager(layoutManager)
    recyclerView!!.setItemAnimator(DefaultItemAnimator())
    recyclerView!!.setNestedScrollingEnabled(false)
    onLoadingSwipeRefresh("")
    errorLayout = findViewById(id.errorLayout)
    errorImage = findViewById(id.errorImage)
    errorTitle = findViewById(id.errorTitle)
    errorMessage = findViewById(id.errorMessage)
    btnRetry = findViewById(id.btnRetry)
    val libraryFAB = findViewById<FloatingActionButton>(id.libraryFAB)
    val uploadFAB = findViewById<FloatingActionButton>(id.uploadFAB)



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
      val cut = result.lastIndexOf('/')
      if (cut != -1) {
        result = result.substring(cut + 1)
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
      val optionsCompat =
        ActivityOptionsCompat.makeSceneTransitionAnimation(
            this@NewsActivity,
            pair
        )
      startActivityForResult(intent, 1214)
    }
  }

  //    @Override
  //    public boolean onCreateOptionsMenu(Menu menu) {
  //
  //        MenuInflater inflater = getMenuInflater();
  //        inflater.inflate(R.menu.menu_main, menu);
  //        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
  //        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
  //        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
  //
  //        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
  //        searchView.setQueryHint("Search Latest News...");
  //        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
  //            @Override
  //            public boolean onQueryTextSubmit(String query) {
  //                if (query.length() > 2){
  //                    onLoadingSwipeRefresh(query);
  //                }
  //                else {
  //                    Toast.makeText(NewsActivity.this, "Type more than two letters!", Toast.LENGTH_SHORT).show();
  //                }
  //                return false;
  //            }
  //
  //            @Override
  //            public boolean onQueryTextChange(String newText) {
  //                return false;
  //            }
  //        });
  //
  //        searchMenuItem.getIcon().setVisible(false, false);
  //
  //        return true;
  //    }
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