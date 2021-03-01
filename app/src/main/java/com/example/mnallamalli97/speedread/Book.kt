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
  var bookChaptersName: ArrayList<String>? = null
    private set
  var bookChaptersPath: ArrayList<String>? = null
    private set
  var bookPrice: Float? = null
    private set
  var bookSummaryPath: String? = null
    private set
  var purchased: Boolean? = null
  private var storage: SharedPreferences? = null

  constructor() {}

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
    bookSummaryPath: String?,
    bookPath: String?,
    bookPrice: Float?,
    isPurchased: Boolean?
  ) {
    this.id = id
    this.title = title
    this.author = author
    this.bookChaptersName = bookChaptersNames
    this.bookChaptersPath = bookChaptersPaths
    this.bookCover = bookCover
    this.bookSummaryPath = bookSummaryPath
    this.bookPath = bookPath
    this.bookPrice = bookPrice
    this.purchased = isPurchased
  }

  companion object {
    fun get(context: Context): Book {
      return Book(context)
    }

    const val FEATURED_LIST = "featured_list"
    const val TOP_CHARTS_LIST = "top_charts_list"
  }
}