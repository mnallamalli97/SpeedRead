package com.example.mnallamalli97.speedread.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Article {

  @SerializedName("author") @Expose var author: String? = null

  @SerializedName("title") @Expose var title: String? = null

  @SerializedName("url") @Expose var url: String? = null

  @SerializedName("description") @Expose var description: String? = null

  @SerializedName("urlToImage") @Expose var image: String? = null

  @SerializedName("publishedAt") @Expose var publishedDate: String? = null

  @SerializedName("source") @Expose var source: String? = null

  constructor(
    title: String?,
    author: String?,
    url: String?,
    description: String?,
    image: String?,
    publishedDate: String?,
    source: String?
  ) {
    this.title = title
    this.author = author
    this.url = url
    this.description = description
    this.image = image
    this.publishedDate = publishedDate
    this.source = source
  }
}