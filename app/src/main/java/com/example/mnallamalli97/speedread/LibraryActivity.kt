package com.example.mnallamalli97.speedread

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.bumptech.glide.Glide
import com.example.mnallamalli97.speedread.R.id
import com.example.mnallamalli97.speedread.R.layout
import com.example.mnallamalli97.speedread.news.NewsActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import java.lang.Math.abs
import java.util.ArrayList

class LibraryActivity : AppCompatActivity(),
    OnClickListener {
  private val libraryList = ArrayList<Book>()
  private var book: Book? = null
  private var booksRecyclerView: RecyclerView? = null
  private var topChartsAdapter: TopChartsAdapter = TopChartsAdapter(libraryList)
  private var featuredBooksAdapter: CustomBookAdapter = CustomBookAdapter(libraryList)
  private var topChartsRecyclerView: RecyclerView? = null
  private var currentBookPosition: Int = 0

  // SETUP FIREBASE
  var databaseReference: DatabaseReference? = null
  var booksDataReference: DatabaseReference? = null
  var t: GenericTypeIndicator<ArrayList<String>> = object : GenericTypeIndicator<ArrayList<String>>() {}

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(layout.library_activity)
    val layoutManager = CenterZoomLayout( this)
    booksRecyclerView = findViewById<RecyclerView>(R.id.tourRV)
    topChartsRecyclerView = findViewById(R.id.topSellersRecyclerView)


    layoutManager.orientation = LinearLayoutManager.HORIZONTAL

    booksRecyclerView!!.layoutManager = layoutManager

    // To Auto Centre View
    val snapHelper = LinearSnapHelper()
    snapHelper.attachToRecyclerView(booksRecyclerView)

    book = Book?.get(applicationContext)


    booksRecyclerView!!.adapter = featuredBooksAdapter
    topChartsRecyclerView!!.adapter = topChartsAdapter


    retrieve(featuredBooksAdapter, topChartsAdapter)

    findViewById<View>(id.item_btn_news).setOnClickListener(this)
    findViewById<View>(id.item_btn_buy).setOnClickListener(this)
    findViewById<View>(id.item_btn_upload).setOnClickListener(this)
    findViewById<View>(id.item_btn_comment).setOnClickListener(this)

    booksRecyclerView!!.addOnScrollListener(object : OnScrollListener() {
      override fun onScrollStateChanged(
        recyclerView: RecyclerView,
        newState: Int
      ) {
        super.onScrollStateChanged(recyclerView, newState)
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
          //Dragging
        } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
          currentBookPosition = getCurrentItem()
        }
      }
    })



//    val pullToRefresh = findViewById<SwipeRefreshLayout>(id.pullToRefresh)
//    pullToRefresh.setOnRefreshListener {
//      retrieve(adapter)
//      pullToRefresh.isRefreshing = false
//    }

  }

  private fun getCurrentItem(): Int {
    return (booksRecyclerView!!.layoutManager as LinearLayoutManager)
        .findFirstVisibleItemPosition()
  }

  fun bookCoverClick(v: View) {
    // on click
    val book = libraryList[currentBookPosition]

    val intent = if (book.title == "Google News") {
      Intent(this@LibraryActivity, NewsActivity::class.java)
    } else {
      Intent(this@LibraryActivity, SettingsActivity::class.java)
    }

    intent.putExtra("title", book.title)
    intent.putExtra("book_path", book.bookPath)
    intent.putExtra("id", book.id)
    intent.putExtra("author", book.author)
    intent.putExtra("cover", book.bookCover)

    startActivity(intent)
  }


  override fun onClick(v: View) {
    when (v.id) {
      id.item_btn_comment -> selectAndLoadChapter(v)
      id.item_btn_buy -> bookCoverClick(v)
      id.item_btn_news -> {
        val startNewsIntent = Intent(this@LibraryActivity, NewsActivity::class.java)
        startActivity(startNewsIntent)
      }
      id.item_btn_upload -> {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/plain"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivityForResult(intent, 1214)
      }
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
        val intent = Intent(this@LibraryActivity, SettingsActivity::class.java)
        intent.putExtra("title", getFileName(uri))
        intent.putExtra("author", "User")
        intent.putExtra("userUploadUri", uri.toString())
        startActivity(intent)
      }
    }
  }

  private fun retrieve(adapter: CustomBookAdapter, topChartsAdapter: TopChartsAdapter) {
    databaseReference = FirebaseDatabase.getInstance()
        .getReference("speedread")
        .child("books")
    booksDataReference = databaseReference!!.child("bookChaptersName")
    libraryList.clear()
    databaseReference!!.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(dataSnapshot: DataSnapshot) {
        for (ds in dataSnapshot.children) {
          val book = ds.getValue(Book::class.java)
          val id = book?.id
          val title = book?.title
          val bookChaptersNames = ds.child("bookChaptersName").getValue(t)
          val bookChaptersPaths = ds.child("bookChaptersPath").getValue(t)
          val author = book?.author
          val cover = ds.child("cover")
              .value
              .toString()
          val bookPath = book?.bookPath
          libraryList.add(Book(id, title, author, bookChaptersNames, bookChaptersPaths, cover, bookPath))
        }
        topChartsAdapter!!.notifyDataSetChanged()
        adapter!!.notifyDataSetChanged()
      }

      override fun onCancelled(databaseError: DatabaseError) {
        Log.d(ContentValues.TAG, databaseError.message) // Don't ignore errors!
      }
    })
  }

  private fun selectAndLoadChapter(
    anchor: View?
  ) {
    val popupMenu = PopupMenu(this@LibraryActivity, anchor!!)
    val menu = popupMenu.menu
    val numberOfChapters = libraryList[currentBookPosition].bookChaptersNames?.size ?: 0

    for (i in 0 until numberOfChapters) {
      menu.add(i, i, i, libraryList[currentBookPosition].bookChaptersNames?.get(i))
    }

    popupMenu.setOnMenuItemClickListener { item ->
      val chapterPath = libraryList[currentBookPosition].bookChaptersPaths?.get(item.itemId)
      val intent = Intent(this@LibraryActivity, SettingsActivity::class.java)


      intent.putExtra("title",libraryList[currentBookPosition].title)
      intent.putExtra("book_path", chapterPath)
      intent.putExtra("id", libraryList[currentBookPosition].id)
      intent.putExtra("author", libraryList[currentBookPosition].author)
      intent.putExtra("cover", libraryList[currentBookPosition].bookCover)

      startActivity(intent)
      true
    }
    popupMenu.show()
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

  class CustomBookAdapter(private val dataSet: List<Book>) :
      RecyclerView.Adapter<CustomBookAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
      val cover: ImageView

      init {
        cover = view.findViewById<View>(id.bookCoverImageButton) as ImageView
      }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
      val view = LayoutInflater.from(viewGroup.context)
          .inflate(R.layout.book_layout, viewGroup, false)

      return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
      val url = dataSet[position].bookCover
      Glide.with(viewHolder.itemView.context)
          .load(url)
          .error(R.drawable.ic_library_books_24px)
          .into(viewHolder.cover)
    }

    override fun getItemCount() = dataSet.size
  }

  class CenterZoomLayout : LinearLayoutManager {
    private val mShrinkAmount = 0.0f;
    private val mShrinkDistance = 0.9f;

    constructor(context: Context) : super(context)

    constructor(context: Context, orientation: Int, reverseLayout: Boolean):
        super(
            context, orientation, reverseLayout
        )

    override fun scrollHorizontallyBy(
      dx: Int,
      recycler: RecyclerView.Recycler?,
      state: RecyclerView.State?
    ): Int {
      val orientation = orientation
      if (orientation == HORIZONTAL) {
        val scrolled = super.scrollHorizontallyBy(dx, recycler, state)
        val midpoint = width/2f
        val d0 = 0f
        val d1 = mShrinkDistance + midpoint
        val s0 = 1f
        val s1 = 1f - mShrinkAmount

        for (i in 0 until childCount) {
          val child = getChildAt(i)
          val childMidPoint = (getDecoratedRight(child!!)
              +getDecoratedLeft(child)/2f)
          val d = d1.coerceAtMost(abs(midpoint - childMidPoint))
          val scale = s0+(s1-s0) *(d-d0)/(d1-d0)
          child.scaleX = scale
          child.scaleY = scale
        }
        return scrolled
      }
      else{
        return 0
      }
    }
  }


  class TopChartsAdapter(private val list: List<Book>) :
      RecyclerView.Adapter<TopChartsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
      val bookCover: ImageView
      val bookTitle: TextView
      val bookAuthor: TextView


      init {
        bookCover = itemView.findViewById(R.id.bookCover)
        bookTitle = itemView.findViewById(R.id.bookTitle)
        bookAuthor = itemView.findViewById(R.id.bookAuthor)
      }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
      val view = LayoutInflater.from(viewGroup.context)
          .inflate(R.layout.top_charts_row_item, viewGroup, false)

      return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
      viewHolder.bookTitle.text = list[position].title
      viewHolder.bookAuthor.text = list[position].author

      val url = list[position].bookCover
      Glide.with(viewHolder.itemView.context)
          .load(url)
          .error(R.drawable.ic_library_books_24px)
          .into(viewHolder.bookCover)

    }
    override fun getItemCount() = list.size
  }


}
