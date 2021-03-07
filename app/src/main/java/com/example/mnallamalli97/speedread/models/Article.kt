package com.example.mnallamalli97.speedread.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Article {

  @SerializedName("id") @Expose var id: String? = null

  @SerializedName("title") @Expose var title: String? = null

  @SerializedName("url") @Expose var url: String? = null

  @SerializedName("description") @Expose var description: String? = null

  @SerializedName("image") @Expose var image: String? = null

  @SerializedName("publishedDate") @Expose var publishedDate: String? = null

  @SerializedName("source") @Expose var source: String? = null

  constructor(
    id: String?,
    title: String?,
    url: String?,
    description: String?,
    image: String?,
    publishedDate: String?,
    source: String?
  ) {
    this.id = id
    this.title = title
    this.url = url
    this.description = description
    this.image = image
    this.publishedDate = publishedDate
    this.source = source
  }
}