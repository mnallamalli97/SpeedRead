package com.example.mnallamalli97.speedread

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.mnallamalli97.speedread.R.id
import com.example.mnallamalli97.speedread.R.layout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest.Builder
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams

class SettingsActivity : AppCompatActivity() {
  private var seekBarValue: Int = 0
  private var seekBar: IndicatorSeekBar? = null
  private var saveButton: Button? = null
  private var showAdButton: Button? = null
  private var darkModeButton: Button? = null
  private var contactNewsButton: Button? = null
  private var resultText: TextView? = null
  private var bookTitle: TextView? = null
  private var settingsProgressBar: ProgressBar? = null
  private var bookAuthor: TextView? = null
  private var pref: SharedPreferences? = null
  private var editor: SharedPreferences.Editor? = null
  private var wordCount = 0
  private var userUploadUri: String? = null
  private var mInterstitialAd: InterstitialAd? = null


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(layout.settings_activity)
    MobileAds.initialize(this)

    val extras = intent.extras!!
    showAdButton = findViewById(id.showAdButton)
    saveButton = findViewById(id.saveButton)
    seekBar = findViewById(id.seekBar)
    resultText = findViewById(id.resultText)
    bookAuthor = findViewById(id.bookAuthor)
    settingsProgressBar = findViewById(id.settingsProgressBar)
    bookTitle = findViewById(id.bookTitle)
    darkModeButton = findViewById(id.darkMode)
    contactNewsButton = findViewById(id.contactNewsButton)

    loadInterstitial()

    pref =
      applicationContext.getSharedPreferences("MyPref", 0) // 0 - for private mode
    editor = pref!!.edit()
    val isDarkModeOn = pref!!.getBoolean("isDarkModeOn", false)


    val author = extras.getString("author")
    val title = extras.getString("title")
    val path = extras.getString("book_path")
    val bookSummaryPath = extras.getString("book_summary_path")
    val bookChaptersNames = extras.getStringArrayList("bookChaptersName")
    val bookChaptersPaths = extras.getStringArrayList("bookChaptersPath")
    val content = extras.getString("content")
    wordCount = extras.getInt("wordCount")
    seekBarValue = pref!!.getInt("speedreadSpeed", 300)
    userUploadUri = extras.getString("userUploadUri")

    bookTitle!!.setText(title)
    bookAuthor!!.setText(author)
    resultText!!.text = String.format(resources.getString(R.string.words_per_min_settings_page), seekBarValue.toString());
    seekBar!!.setProgress(seekBarValue.toFloat())
    seekBar!!.onSeekChangeListener = object : OnSeekChangeListener {
      override fun onSeeking(seekParams: SeekParams) {
        seekBarValue = seekParams.progress
        resultText!!.text = String.format(resources.getString(R.string.words_per_min_settings_page), seekBarValue.toString());
      }
      override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}
      override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
        resultText!!.text = String.format(resources.getString(R.string.words_per_min_settings_page), seekBarValue.toString());
        editor!!.putInt("speedReadSpeed", seekBarValue)
        editor!!.apply()
      }
    }

    seekBar!!.customSectionTrackColor {
      it[0] = resources.getColor(R.color.color_green, null)
      it[1] = resources.getColor(R.color.color_green, null)
      it[2] = resources.getColor(R.color.color_yellow, null)
      it[3] = resources.getColor(R.color.color_red, null)
      true //true if apply color , otherwise no change
    }

    if (isDarkModeOn) {
      darkModeButton!!.text = "DISABLE DARK MODE"
    } else {
      darkModeButton!!.text = "ENABLE DARK MODE"
    }

    if (author == "Google News") {
      contactNewsButton!!.visibility = View.VISIBLE
    } else {
      contactNewsButton!!.visibility = View.GONE
    }


    contactNewsButton!!.setOnClickListener {
      val startIntent = Intent(this@SettingsActivity, WebviewActivity::class.java)
      startActivity(startIntent)
    }

    darkModeButton!!.setOnClickListener {

      if (isDarkModeOn) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        editor!!.putBoolean("isDarkModeOn", false)
        editor!!.apply()
        val intent = intent
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("isDarkModeOn", false)
        finish()
        startActivity(intent)
      } else {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        editor!!.putBoolean("isDarkModeOn", true)
        editor!!.apply()
        val intent = intent
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("isDarkModeOn", true)
        finish()
        startActivity(intent)
      }
    }

    showAdButton!!.setOnClickListener(OnClickListener {
      showInterstitialAd()
    })

    saveButton!!.setOnClickListener(OnClickListener {
      val startIntent = Intent(this@SettingsActivity, MainActivity::class.java)
      val extras = Bundle()
      editor!!.putInt("speedreadSpeed", seekBarValue)
      editor!!.apply()
      extras.putString("title", title)
      extras.putString("author", author)
      extras.putString("book_path", path)
      extras.putString("book_summary_path", bookSummaryPath)
      extras.putStringArrayList("bookChaptersNames", bookChaptersNames)
      extras.putStringArrayList("bookChaptersPaths", bookChaptersPaths)
      extras.putString("content", content)
      extras.putInt("wordCount", wordCount)
      extras.putString("userUploadUri", userUploadUri)
      startIntent.putExtras(extras)
      finish()
      startActivity(startIntent)
    })
  }

  private fun loadInterstitial() {
    var adRequest = Builder().build()

    InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
      override fun onAdFailedToLoad(adError: LoadAdError) {
        Log.d(TAG, adError?.message)
        mInterstitialAd = null
      }

      override fun onAdLoaded(interstitialAd: InterstitialAd) {
        Log.d(TAG, "Ad was loaded.")
        showAdButton!!.isEnabled = true
        settingsProgressBar!!.visibility = View.GONE
        mInterstitialAd = interstitialAd
      }
    })

    mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
      override fun onAdDismissedFullScreenContent() {
        Log.d(TAG, "Ad was dismissed.")
        loadInterstitial()
      }

      override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
        Log.d(TAG, "'Ad failed to show.")
      }

      override fun onAdShowedFullScreenContent() {
        Log.d(TAG, "Ad showed fullscreen content.")
      }
    }
  }

  private fun showInterstitialAd() {
    if (mInterstitialAd != null) {
      mInterstitialAd?.show(this)
      var adRequest = Builder().build()
      InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
        override fun onAdFailedToLoad(adError: LoadAdError) {
          Log.d(TAG, adError?.message)
          mInterstitialAd = null
        }

        override fun onAdLoaded(interstitialAd: InterstitialAd) {
          Log.d(TAG, "Ad was loaded.")
          showAdButton!!.isEnabled = true
          settingsProgressBar!!.visibility = View.GONE
          mInterstitialAd = interstitialAd
          loadInterstitial()
        }
      })
    } else {
      Log.d("TAG", "The interstitial ad wasn't ready yet.")
    }
  }

  companion object {
    const val TAG = "SettingsActivity"
  }
}
