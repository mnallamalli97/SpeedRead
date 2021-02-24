package com.example.mnallamalli97.speedread

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.example.mnallamalli97.speedread.R.layout

class SplashScreen : Activity() {
  /** Duration of wait  */
  private val SPLASH_DISPLAY_LENGTH = 1500

  /** Called when the activity is first created.  */
  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(layout.splash_screen_activity)

    Handler()
        .postDelayed({
          val mainIntent = Intent(this@SplashScreen, LibraryActivity::class.java)
          this@SplashScreen.startActivity(mainIntent)
          this.overridePendingTransition(0, 0);
          finish()
        }, SPLASH_DISPLAY_LENGTH.toLong())
  }
}
