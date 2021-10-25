package com.hd.tvpro

import android.os.Bundle
import android.view.MotionEvent
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.PlaybackVideoFragment

/**
 * Loads [MainFragment].
 */
class MainActivity : FragmentActivity() {
    private val fragment = PlaybackVideoFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && fragment.dispatchTouchEvent(ev)) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}