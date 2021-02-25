package com.example.mnallamalli97.speedread

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.preference.PreferenceManager
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.example.mnallamalli97.speedread.R.string
import com.yarolegovich.discretescrollview.DiscreteScrollView
import com.yarolegovich.discretescrollview.InfiniteScrollAdapter
import java.lang.ref.WeakReference

/**
 * Created by yarolegovich on 08.03.2017.
 */
class SelectChapter private constructor(context: Context) {
  private val KEY_TRANSITION_TIME: String

  private class TransitionTimeChangeListener(scrollView: DiscreteScrollView) :
      OnSharedPreferenceChangeListener {
    private val scrollView: WeakReference<DiscreteScrollView>
    override fun onSharedPreferenceChanged(
      sharedPreferences: SharedPreferences,
      key: String
    ) {
      if (key == instance!!.KEY_TRANSITION_TIME) {
        val scrollView = scrollView.get()
        if (scrollView != null) {
          scrollView.setItemTransitionTimeMillis(sharedPreferences.getInt(key, 150))
        } else {
          sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }
      }
    }

    init {
      this.scrollView = WeakReference(scrollView)
    }
  }

  companion object {
    private var instance: SelectChapter? = null
    @JvmStatic fun init(context: Context) {
      instance = SelectChapter(context)
    }

    fun smoothScrollToUserSelectedPosition(
      scrollView: DiscreteScrollView,
      anchor: View?
    ) {
      val popupMenu =
        PopupMenu(scrollView.context, anchor!!)
      val menu = popupMenu.menu
      val adapter = scrollView.adapter
      val itemCount =
        if (adapter is InfiniteScrollAdapter<*>) adapter.realItemCount else adapter?.itemCount ?: 0
      for (i in 0 until itemCount) {
        menu.add((i + 1).toString())
      }
      popupMenu.setOnMenuItemClickListener { item ->
        var destination = item.title
            .toString()
            .toInt() - 1
        if (adapter is InfiniteScrollAdapter<*>) {
          destination = adapter.getClosestPosition(destination)
        }
        scrollView.smoothScrollToPosition(destination)
        true
      }
      popupMenu.show()
    }

    val transitionTime: Int
      get() = defaultPrefs()
          .getInt(instance!!.KEY_TRANSITION_TIME, 150)

    private fun defaultPrefs(): SharedPreferences {
      return PreferenceManager.getDefaultSharedPreferences(App.getInstance())
    }
  }

  init {
    KEY_TRANSITION_TIME = context.getString(string.pref_key_transition_time)
  }
}