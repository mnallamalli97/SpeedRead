package com.example.mnallamalli97.speedread

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.mnallamalli97.speedread.R.id
import com.example.mnallamalli97.speedread.R.layout
import com.example.mnallamalli97.speedread.news.NewsActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

class LibraryActivity : AppCompatActivity() {
  private val libraryList =
    ArrayList<Book>()
  private var listView: ListView? = null

  // SETUP FIREBASE
  var databaseReference: DatabaseReference? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(layout.library_activity)
    val adapter = PropertyArrayAdapter(this, 0, libraryList)
    retrieve(adapter)
    // Find list view and bind it with the custom adapter
    listView = findViewById(id.customListView)
    listView!!.adapter = adapter
    listView!!.onItemClickListener = OnItemClickListener { parent, view, position, id ->
      Toast.makeText(
          this@LibraryActivity, libraryList[position]
          .title, Toast.LENGTH_SHORT
      )
          .show()
    }
    val pullToRefresh = findViewById<SwipeRefreshLayout>(id.pullToRefresh)
    pullToRefresh.setOnRefreshListener {
      retrieve(adapter)
      pullToRefresh.isRefreshing = false
    }

    // add event listener so we can handle clicks
    val adapterViewListener =
      OnItemClickListener { parent, view, position, id ->

        // on click
        val book = libraryList[position]

        val intent = if (book.title == "Google News") {
          Intent(this@LibraryActivity, NewsActivity::class.java)
        } else {
          Intent(this@LibraryActivity, SettingsActivity::class.java)
        }

        intent.putExtra("title", book.title)
        intent.putExtra("book_path", book.bookPath)
        intent.putExtra("author", book.author)
        intent.putExtra("cover", book.bookCover)

        startActivity(intent)
      }
    // set the listener to the list view
    listView!!.onItemClickListener = adapterViewListener
  }

  private fun retrieve(adapter: PropertyArrayAdapter) {
    databaseReference = FirebaseDatabase.getInstance()
        .getReference("speedread")
        .child("books")
    libraryList.clear()
    databaseReference!!.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(dataSnapshot: DataSnapshot) {
        for (ds in dataSnapshot.children) {
          val book =
            ds.getValue(
                Book::class.java
            )!!
          val title = book.title
          val author = book.author
          val cover = ds.child("cover")
              .value
              .toString()
          val bookPath = book.bookPath
          libraryList.add(Book(title, author, cover, bookPath))
        }
        adapter!!.notifyDataSetChanged()
      }

      override fun onCancelled(databaseError: DatabaseError) {
        Log.d(ContentValues.TAG, databaseError.message) // Don't ignore errors!
      }
    })
  }

  // custom ArrayAdapter
  class PropertyArrayAdapter(
      context: Context,
      resource: Int,
      objects: ArrayList<Book>
  ) : ArrayAdapter<Book>(context, resource, objects as List<Book?>) {
    private var bookList: List<Book> = objects

    // called when rendering the list
    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {

      // get the property we are displaying
      val book = bookList[position]

      // get the inflater and inflate the XML layout for each item
      val inflater =
        context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
      val view = inflater.inflate(layout.book_layout, null)
      val title = view.findViewById<View>(id.bookTitle) as TextView
      val author = view.findViewById<View>(id.author) as TextView
      val cover =
        view.findViewById<View>(id.cover) as ImageView

      // set title and author attributes
      title.text = "Title: " + book.title
          .toString()
      author.text = "Author: " + book.author
          .toString()
      val url = book.bookCover
      Glide.with(context)
          .load(url)
          .into(cover)
      return view
    }
  }
}
