package com.hd.tvpro

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.PlaybackVideoFragment
import com.hd.tvpro.app.App
import com.hd.tvpro.util.QRCodeUtil.createQRCodeBitmap
import com.hd.tvpro.webserver.WebServer
import com.pngcui.skyworth.dlna.service.MediaRenderService
import com.pngcui.skyworth.dlna.util.CommonUtil
import com.smarx.notchlib.NotchScreenManager
import kotlinx.coroutines.*
import utils.IPUtil
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.min


/**
 * Loads [MainFragment].
 */
class MainActivity : FragmentActivity() {
    var isOnPause = false
    private val fragment = PlaybackVideoFragment()
    private var dialog: PopupWindow? = null

    fun getContext(): Context {
        return this
    }

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
        val scope = CoroutineScope(EmptyCoroutineContext)
        scope.launch(Dispatchers.IO) {
            delay(1000)
            withContext(Dispatchers.Main) {
                if (!isFinishing) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                }
            }
        }
        if (App.webServer == null) {
            try {
                App.webServer = WebServer()
                App.webServer?.start()
            } catch (e: Exception) {
            }
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

    override fun onKeyDown(keyCode: Int, ev: KeyEvent?): Boolean {
        if (ev != null && fragment.onKeyDown(keyCode, ev)) {
            //fragment拦截了
            return true
        }
        return super.onKeyDown(keyCode, ev)
    }

    override fun onBackPressed() {
        if (dialog != null && dialog!!.isShowing) {
            dialog?.dismiss()
            return
        }
        if (fragment.onBackPressed()) {
            //fragment拦截了
            return
        }
        showExitDialog()
//        super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        isOnPause = true
    }

    override fun onResume() {
        super.onResume()
        isOnPause = false
    }

//    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            if (fragment.onBackPressed()) {
//                //fragment拦截了
//                return true
//            }
//            showExitDialog()
//            return true
//        }
//        return super.onKeyDown(keyCode, event)
//    }


    //显示退出对话框
    fun showExitDialog() {
        hideHelpDialog()
        val fontSize = CommonUtil.getScreenWidth(this) / 42
        val inflater = layoutInflater
        val exitView: View = inflater.inflate(R.layout.layout_exit_dialog, null)
        val tv_isp = exitView.findViewById<View>(R.id.tv_isp) as TextView
        val btn_exit = exitView.findViewById<View>(R.id.btn_exit) as Button
        val btn_setting = exitView.findViewById<View>(R.id.btn_setting) as Button
        val qrcodeView = exitView.findViewById<View>(R.id.qrcodeView) as ImageView

        val qrParams: ViewGroup.LayoutParams = qrcodeView.layoutParams
        qrParams.height = min(CommonUtil.getScreenWidth(this), CommonUtil.getScreenHeight(this)) / 4
        qrParams.width = qrParams.height
        qrcodeView.layoutParams = qrParams

        val params: ViewGroup.LayoutParams = btn_exit.layoutParams
        params.height = CommonUtil.getScreenHeight(this) / 10
        btn_exit.layoutParams = params
        btn_setting.layoutParams = params
        btn_exit.setTextSize(TypedValue.COMPLEX_UNIT_PX, (fontSize * 12 / 10).toFloat())
        btn_setting.setTextSize(TypedValue.COMPLEX_UNIT_PX, (fontSize * 12 / 10).toFloat())
        tv_isp.text = "确认退出软件？"
        tv_isp.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())
        val tv_webadmin = exitView.findViewById<View>(R.id.tv_webadmin) as TextView
        val ip = IPUtil.getIP(getContext())
        val url = "http://$ip:12345"
        tv_webadmin.text = ("远程管理地址：\n$url")
        val mBitmap: Bitmap? = createQRCodeBitmap(url, 480, 480)
        qrcodeView.setImageBitmap(mBitmap)

        tv_webadmin.setTextSize(TypedValue.COMPLEX_UNIT_PX, (fontSize * 8 / 10).toFloat())
        btn_exit.text = "退出"
        btn_exit.setOnClickListener {
            finish()
        }
        btn_setting.text = "取消"
        btn_setting.setOnClickListener {
            if (dialog != null) {
                dialog?.dismiss()
            }
        }
        btn_exit.requestFocus()
        dialog =
            PopupWindow(
                exitView,
                CommonUtil.getScreenWidth(this) * 3 / 5,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        dialog?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.isFocusable = true
        dialog?.isOutsideTouchable = true
        dialog?.update()
        dialog?.showAtLocation(window?.decorView, Gravity.CENTER, 0, 0)
    }


    //显示退出对话框
    fun showHelpDialog() {
        hideHelpDialog()
        val fontSize = CommonUtil.getScreenWidth(this) / 42
        val inflater = layoutInflater
        val exitView: View = inflater.inflate(R.layout.layout_help_dialog, null)
        val tv_isp = exitView.findViewById<View>(R.id.tv_isp) as TextView
        val btn_exit = exitView.findViewById<View>(R.id.btn_exit) as Button
        val btn_setting = exitView.findViewById<View>(R.id.btn_setting) as Button
        val params: ViewGroup.LayoutParams = btn_exit.layoutParams
        params.height = CommonUtil.getScreenHeight(this) / 10
        btn_exit.layoutParams = params
        btn_setting.layoutParams = params
        btn_exit.setTextSize(TypedValue.COMPLEX_UNIT_PX, (fontSize * 12 / 10).toFloat())
        btn_setting.setTextSize(TypedValue.COMPLEX_UNIT_PX, (fontSize * 12 / 10).toFloat())
        tv_isp.text = "软件使用提示"
        tv_isp.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())
        val tv_webadmin = exitView.findViewById<View>(R.id.tv_webadmin) as TextView
        tv_webadmin.text = (
                "投屏成功视频开始播放后本提示会自动消失\n" +
                        "DLAN投屏：即海阔视界的播放器页面的传统投屏，也支持大多数其它软件直接投放\n" +
                        "网页投屏：需要海阔视界先在播放器页面点击网页投屏按钮，然后再打开本软件")
        tv_webadmin.setTextSize(TypedValue.COMPLEX_UNIT_PX, (fontSize * 8 / 10).toFloat())
        btn_exit.text = "确认"
        btn_exit.setOnClickListener {
            if (dialog != null) {
                dialog?.dismiss()
            }
        }
        btn_setting.text = "退出"
        btn_setting.setOnClickListener {
            finish()
        }
        btn_exit.requestFocus()
        dialog =
            PopupWindow(
                exitView,
                CommonUtil.getScreenWidth(this) * 3 / 5,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        dialog?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.isFocusable = true
        dialog?.isOutsideTouchable = true
        dialog?.update()
        dialog?.showAtLocation(window?.decorView, Gravity.CENTER, 0, 0)
    }

    fun hideHelpDialog() {
        if (dialog != null && dialog!!.isShowing) {
            dialog?.dismiss()
        }
    }
}