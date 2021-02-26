package com.example.mnallamalli97.speedread

import android.content.Context
import android.content.SharedPreferences

class Book {

  var title: String? = null
    private set
  var author: String? = null
    private set
  var bookCover: String? = null
    private set
  var bookPath: String? = null
    private set
  var id: Int? = null
    private set
  var bookChaptersNames: ArrayList<String>? = null
    private set
  var bookChaptersPaths: ArrayList<String>? = null
    private set
  private var storage: SharedPreferences? = null

  constructor() { }

  constructor(context: Context) {
    context.getSharedPreferences("MyPref", 0)
  }

  constructor(
    id: Int?,
    title: String?,
    author: String?,
    bookChaptersNames: ArrayList<String>?,
    bookChaptersPaths: ArrayList<String>?,
    bookCover: String?,
    bookPath: String?
  ) {
    this.id = id
    this.title = title
    this.author = author
    this.bookChaptersNames = bookChaptersNames
    this.bookChaptersPaths = bookChaptersPaths
    this.bookCover = bookCover
    this.bookPath = bookPath
  }


  fun isRated(itemId: Int): Boolean {
    return storage!!.getBoolean(itemId.toString(), false)
  }

  fun setRated(
    itemId: Int,
    isRated: Boolean
  ) {
    storage!!.edit()
        .putBoolean(itemId.toString(), isRated)
        .apply()
  }

  companion object {
    fun get(context: Context): Book {
      return Book(context)
    }

    const val FEATURED_LIST = "featured_list"
    const val TOP_CHARTS_LIST = "top_charts_list"
  }
}