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
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
  private var topBooksListView: NonScrollListView? = null
  private var currentBookPosition: Int = 0

  // SETUP FIREBASE
  var databaseReference: DatabaseReference? = null
  var t: GenericTypeIndicator<ArrayList<String>> = object : GenericTypeIndicator<ArrayList<String>>() {}

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(layout.library_activity)
    val layoutManager = CenterZoomLayout( this)
    booksRecyclerView = findViewById<RecyclerView>(R.id.tourRV)
    topBooksListView = findViewById(R.id.listView)



    layoutManager.orientation = LinearLayoutManager.HORIZONTAL

    booksRecyclerView!!.layoutManager = layoutManager

    // To Auto Centre View
    val snapHelper = LinearSnapHelper()
    snapHelper.attachToRecyclerView(booksRecyclerView)
    booksRecyclerView!!.isNestedScrollingEnabled = false

    book = Book?.get(applicationContext)

    //Arraylist to recyclerview
    val featuredBooksAdapter = CustomBookAdapter(libraryList)
    val topChartsListViewAdapter = TopChartsListViewAdapter(this, libraryList)
    booksRecyclerView!!.adapter = featuredBooksAdapter
    topBooksListView!!.adapter = topChartsListViewAdapter

    retrieve(featuredBooksAdapter, topChartsListViewAdapter)

    findViewById<View>(id.discoveryPageNewsButton).setOnClickListener(this)
    findViewById<View>(id.discoveryPageUploadButton).setOnClickListener(this)

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
      Intent(this@LibraryActivity, CheckoutActivity::class.java)
    }

    intent.putExtra("title", book.title)
    intent.putExtra("book_path", book.bookPath)
    intent.putExtra("book_summary_path", book.bookSummaryPath)
    intent.putExtra("id", book.id)
    intent.putExtra("author", book.author)
    intent.putExtra("cover", book.bookCover)

    startActivity(intent)
  }


  override fun onClick(v: View) {
    when (v.id) {
      id.discoveryPageNewsButton -> {
        val startNewsIntent = Intent(this@LibraryActivity, NewsActivity::class.java)
        finish()
        startActivity(startNewsIntent)
      }
      id.discoveryPageUploadButton -> {
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

  private fun retrieve(adapter: CustomBookAdapter, topChartsListViewAdapter: TopChartsListViewAdapter) {
    databaseReference = FirebaseDatabase.getInstance()
        .getReference("speedread")
        .child("books")
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
          val bookPrice = book?.bookPrice
          val bookSummaryPath = book?.bookSummaryPath
          val isPurchased = book!!.purchased
          libraryList.add(Book(id, title, author, bookChaptersNames, bookChaptersPaths, cover, bookSummaryPath, bookPath, bookPrice, isPurchased))
        }
        adapter!!.notifyDataSetChanged()
        topChartsListViewAdapter!!.notifyDataSetChanged()
      }

      override fun onCancelled(databaseError: DatabaseError) {
        Log.d(ContentValues.TAG, databaseError.message) // Don't ignore errors!
      }
    })
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

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
      // Create a new view, which defines the UI of the list item
      val view = LayoutInflater.from(viewGroup.context)
          .inflate(R.layout.book_layout, viewGroup, false)

      return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

//      // set title and author attributes
//      viewHolder.title.text = "Title: " + dataSet[position].title.toString()
//      viewHolder.author.text = "Author: " + dataSet[position].author.toString()

      val url = dataSet[position].bookCover
      Glide.with(viewHolder.itemView.context)
          .load(url)
          .error(R.drawable.ic_library_books_24px)
          .into(viewHolder.cover)
    }

    // Return the size of your dataset (invoked by the layout manager)
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

  //Class MyAdapter
  open class TopChartsListViewAdapter(private val context: Context, private val arrayList: ArrayList<Book>) : BaseAdapter() {
    private lateinit var bookCover: ImageView
    private lateinit var bookTitle: TextView
    private lateinit var bookAuthor: TextView
    override fun getCount(): Int {
      return arrayList.size
    }
    override fun getItem(position: Int): Any {
      return position
    }
    override fun getItemId(position: Int): Long {
      return position.toLong()
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
      var convertView = convertView
      convertView = LayoutInflater.from(context).inflate(R.layout.top_charts_row_item, parent, false)
      bookCover = convertView.findViewById(R.id.bookCover)
      bookTitle = convertView.findViewById(R.id.bookTitle)
      bookAuthor = convertView.findViewById(R.id.bookAuthor)


      val url = arrayList[position].bookCover
      Glide.with(convertView.context)
          .load(url)
          .error(R.drawable.ic_library_books_24px)
          .into(bookCover)

      bookTitle.text = " " + arrayList[position].title
      bookAuthor.text = arrayList[position].author
      return convertView
    }
  }
}
