package com.example.mnallamalli97.speedread.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class User {

  @SerializedName("id") @Expose var id: String? = null

  @SerializedName("isPurchasedMap") @Expose var isPurchasedMap: MutableMap<String, Boolean> = mutableMapOf()


  // Register new user
  constructor(
    id: String?
  ) {
    this.id = id
  }

  constructor(
    id: String?,
    isPurchasedMap: MutableMap<String, Boolean>,
  ) {
    this.id = id
    this.isPurchasedMap = isPurchasedMap
  }
}