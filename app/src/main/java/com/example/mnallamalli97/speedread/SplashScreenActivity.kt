package com.example.mnallamalli97.speedread

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import com.example.mnallamalli97.speedread.R.layout
import com.example.mnallamalli97.speedread.news.NewsActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

class SplashScreenActivity : Activity() {
  private val MILLISECOND_PER_MINUTE = 60000
  private val SPLASH_DISPLAY_LENGTH = 1500

  private val SPLASH_TEXT_1_TIME_IN_MILLISECONDS = 11000L
  private val SPLASH_TEXT_2_TIME_IN_MILLISECONDS = 6000L
  private val TOTAL_SPLASH_TEXT_TIME_IN_MILLISECONDS =
    SPLASH_TEXT_1_TIME_IN_MILLISECONDS + SPLASH_TEXT_2_TIME_IN_MILLISECONDS
  private var splashWordTextView: TextView? = null

  private var splashTimer1 = Timer()
  private var splashTimer2 = Timer()
  private var pref: SharedPreferences? = null
  private var editor: SharedPreferences.Editor? = null
  val applicationJob = Job()

  var helloWorldContent150 =
    "The average person can read at this speed: 150 words per minute. But, our brains can process information faster than our eyes can move."
  var helloWorldContent300 =
    "You are now reading at 300 words a minute. That was a quick improvement. You can read even faster with a little practice."

  /** Called when the activity is first created.  */
  public override fun onCreate(savedInstanceState: Bundle?) {
    pref = applicationContext.getSharedPreferences("MyPref", 0) // 0 - for private mode
    editor = pref!!.edit()
    super.onCreate(savedInstanceState)
    setContentView(layout.splash_screen_activity)

    splashWordTextView = findViewById(R.id.splashWordTextView)

    if (pref!!.getBoolean("showSplashScreenText", true)) {
      GlobalScope.launch {
        suspend {
          runSplashScreenIntro1(helloWorldContent150)
          delay(SPLASH_TEXT_1_TIME_IN_MILLISECONDS)

          runSplashScreenIntro2(helloWorldContent300)
          editor!!.putBoolean("showSplashScreenText", false)
          editor!!.apply()
          delay(SPLASH_TEXT_2_TIME_IN_MILLISECONDS)

        }.invoke()
      }
      Handler()
          .postDelayed({
            val mainIntent = Intent(this@SplashScreenActivity, NewsActivity::class.java)
            finish()
            startActivity(mainIntent)
            overridePendingTransition(0, 0);
          }, TOTAL_SPLASH_TEXT_TIME_IN_MILLISECONDS)
    } else {
      Handler()
          .postDelayed({
            val mainIntent = Intent(this@SplashScreenActivity, NewsActivity::class.java)
            finish()
            startActivity(mainIntent)
            overridePendingTransition(0, 0);
          }, SPLASH_DISPLAY_LENGTH.toLong())
    }

  }

  private fun runSplashScreenIntro1(
    helloWorldContent150: String?
  ) {

    val wc150 = helloWorldContent150!!.split(" ".toRegex())
        .toTypedArray()

    val totalWordCount = wc150.size
    var currentWordCount = 0
    val switchWordPeriod150 = (MILLISECOND_PER_MINUTE / 150).toLong()

    splashTimer1.scheduleAtFixedRate(object : TimerTask() {
      override fun run() {
        runOnUiThread {
          if (currentWordCount < totalWordCount) {
            splashWordTextView!!.text = wc150[currentWordCount]
            currentWordCount++
          } else {
            splashTimer1.cancel()
          }
        }
      }
    }, 0, switchWordPeriod150)

  }

  private fun runSplashScreenIntro2(
    helloWorldContent300: String?
  ) {
    val wc300 = helloWorldContent300!!.split(" ".toRegex())
        .toTypedArray()
    val totalWordCount = wc300.size
    var currentWordCount = 0
    val switchWordPeriod300 = (MILLISECOND_PER_MINUTE / 300).toLong()

    splashTimer2.scheduleAtFixedRate(object : TimerTask() {
      override fun run() {
        runOnUiThread {
          if (currentWordCount < totalWordCount) {
            splashWordTextView!!.text = wc300[currentWordCount]
            currentWordCount++
          } else {
            splashTimer2.cancel()
          }
        }
      }
    }, 0, switchWordPeriod300)

  }
}
