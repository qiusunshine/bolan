package com.hd.tvpro

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.PlaybackVideoFragment
import com.pngcui.skyworth.dlna.service.MediaRenderService
import com.smarx.notchlib.NotchScreenManager

/**
 * Loads [MainFragment].
 */
class MainActivity : FragmentActivity() {
    private val fragment = PlaybackVideoFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            NotchScreenManager.getInstance().setDisplayInNotch(this)
        } catch (e: Exception) {
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit()
        }

        val intent = Intent(
            this,
            MediaRenderService::class.java
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        //设置沉浸式状态栏，在MIUI系统中，状态栏背景透明。原生系统中，状态栏背景半透明。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
        super.onWindowFocusChanged(hasFocus)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && fragment.dispatchTouchEvent(ev)) {
            //fragment拦截了
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        if (fragment.onBackPressed()) {
            //fragment拦截了
            return
        }
        super.onBackPressed()
    }
}