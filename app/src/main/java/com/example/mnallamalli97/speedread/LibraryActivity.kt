package com.example.mnallamalli97.speedread

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mnallamalli97.speedread.LibraryActivity.CustomBookAdapter
import com.example.mnallamalli97.speedread.LibraryActivity.CustomBookAdapter.ViewHolder
import com.example.mnallamalli97.speedread.R.color
import com.example.mnallamalli97.speedread.R.drawable
import com.example.mnallamalli97.speedread.R.id
import com.example.mnallamalli97.speedread.R.layout
import com.example.mnallamalli97.speedread.news.NewsActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.yarolegovich.discretescrollview.DiscreteScrollView
import com.yarolegovich.discretescrollview.InfiniteScrollAdapter
import java.util.ArrayList

class LibraryActivity : AppCompatActivity(),
    DiscreteScrollView.OnItemChangedListener<CustomBookAdapter.ViewHolder>,
    OnClickListener {
  private val libraryList = ArrayList<Book>()
  private var book: Book? = null
  private var listView: ListView? = null
  private var horizontalScrollView: DiscreteScrollView? = null
  private var bookCoverImageButton: ImageButton? = null
  private var rateItemButton: ImageView? = null
  private var bookChaptersMap: Map<String, String>? = null

  // SETUP FIREBASE
  var databaseReference: DatabaseReference? = null
  var booksDataReference: DatabaseReference? = null
  var t: GenericTypeIndicator<ArrayList<String>> = object : GenericTypeIndicator<ArrayList<String>>() {}

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(layout.library_activity)
    book = Book?.get(applicationContext)
    val adapter = CustomBookAdapter(libraryList)
    horizontalScrollView = findViewById(id.item_picker)
    bookCoverImageButton = findViewById(R.id.bookCoverImageButton)
    rateItemButton = findViewById<ImageView>(id.item_btn_rate)
    horizontalScrollView!!.adapter = adapter


    retrieve(adapter)


    findViewById<View>(id.item_btn_rate).setOnClickListener(this)
    findViewById<View>(id.item_btn_buy).setOnClickListener(this)
    findViewById<View>(id.item_btn_comment).setOnClickListener(this)




//    val pullToRefresh = findViewById<SwipeRefreshLayout>(id.pullToRefresh)
//    pullToRefresh.setOnRefreshListener {
//      retrieve(adapter)
//      pullToRefresh.isRefreshing = false
//    }

  }

  fun bookCoverClick(v: View) {
    // on click
    val book = libraryList[horizontalScrollView!!.currentItem]

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

  private fun onItemChanged(item: Book) {
    changeRateButtonState(item)
  }

  private fun changeRateButtonState(item: Book) {
    if (item.isRated(item.id!!)) {
      rateItemButton!!.setImageResource(drawable.ic_star_black_24dp)
      rateItemButton!!.setColorFilter(ContextCompat.getColor(this, color.shopRatedStar))
    } else {
      rateItemButton!!.setImageResource(drawable.ic_star_border_black_24dp)
      rateItemButton!!.setColorFilter(ContextCompat.getColor(this, color.shopSecondary))
    }
  }

  override fun onCurrentItemChanged(
    viewHolder: ViewHolder?,
    adapterPosition: Int
  ) {
    val positionInDataSet: Int = horizontalScrollView!!.currentItem
    onItemChanged(libraryList.get(positionInDataSet))
  }

  override fun onClick(v: View) {
    when (v.id) {
      id.item_btn_comment -> selectAndLoadChapter(horizontalScrollView!!, v)
      id.item_btn_buy -> bookCoverClick(v)

    }
  }

  private fun retrieve(adapter: CustomBookAdapter) {
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
        adapter!!.notifyDataSetChanged()
      }

      override fun onCancelled(databaseError: DatabaseError) {
        Log.d(ContentValues.TAG, databaseError.message) // Don't ignore errors!
      }
    })
  }

  private fun selectAndLoadChapter(
    scrollView: DiscreteScrollView,
    anchor: View?
  ) {
    val popupMenu = PopupMenu(scrollView.context, anchor!!)
    val menu = popupMenu.menu
    val numberOfChapters = libraryList[scrollView.currentItem].bookChaptersNames?.size ?: 0

    for (i in 0 until numberOfChapters) {
      menu.add(i, i, i, libraryList[scrollView.currentItem].bookChaptersNames?.get(i))
    }

    popupMenu.setOnMenuItemClickListener { item ->



      val chapterPath = libraryList[scrollView.currentItem].bookChaptersPaths?.get(item.itemId)


      val intent = Intent(this@LibraryActivity, SettingsActivity::class.java)


      intent.putExtra("title",libraryList[scrollView.currentItem].title)
      intent.putExtra("book_path", chapterPath)
      intent.putExtra("id", libraryList[scrollView.currentItem].id)
      intent.putExtra("author", libraryList[scrollView.currentItem].author)
      intent.putExtra("cover", libraryList[scrollView.currentItem].bookCover)

      startActivity(intent)
      true
    }
    popupMenu.show()
  }

  class CustomBookAdapter(private val dataSet: List<Book>) :
      RecyclerView.Adapter<CustomBookAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
      val title: TextView
      val author: TextView
      val cover: ImageView

      init {
        // Define click listener for the ViewHolder's View.
        title = view.findViewById<View>(id.bookTitle) as TextView
        author = view.findViewById<View>(id.author) as TextView
        cover = view.findViewById<View>(id.bookCoverImageButton) as ImageButton
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

      // set title and author attributes
      viewHolder.title.text = "Title: " + dataSet[position].title.toString()
      viewHolder.author.text = "Author: " + dataSet[position].author.toString()

      val url = dataSet[position].bookCover
      Glide.with(viewHolder.itemView.context)
          .load(url)
          .error(R.drawable.ic_library_books_24px)
          .into(viewHolder.cover)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

  }
}
