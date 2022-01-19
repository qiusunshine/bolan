package com.hd.tvpro.util

import android.graphics.Bitmap
import android.graphics.Color
import android.text.TextUtils
import androidx.annotation.ColorInt
import androidx.annotation.Nullable
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.CharacterSetECI
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.*


/**
 * 作者：By 15968
 * 日期：On 2022/1/19
 * 时间：At 11:58
 */
object QRCodeUtil {
    /**
     * 创建二维码位图
     *
     * @param content 字符串内容(支持中文)
     * @param width 位图宽度(单位:px)
     * @param height 位图高度(单位:px)
     * @return
     */
    @Nullable
    fun createQRCodeBitmap(content: String?, width: Int, height: Int): Bitmap? {
        return createQRCodeBitmap(
            content,
            width,
            height,
            "UTF-8",
            "H",
            "2",
            Color.BLACK,
            Color.WHITE
        )
    }

    /**
     * 创建二维码位图 (支持自定义配置和自定义样式)
     *
     * @param content 字符串内容
     * @param width 位图宽度,要求>=0(单位:px)
     * @param height 位图高度,要求>=0(单位:px)
     * @param character_set 字符集/字符转码格式 (支持格式:[CharacterSetECI])。传null时,zxing源码默认使用 "ISO-8859-1"
     * @param error_correction 容错级别 (支持级别:[ErrorCorrectionLevel])。传null时,zxing源码默认使用 "L"
     * @param margin 空白边距 (可修改,要求:整型且>=0), 传null时,zxing源码默认使用"4"。
     * @param color_black 黑色色块的自定义颜色值
     * @param color_white 白色色块的自定义颜色值
     * @return
     */
    @Nullable
    fun createQRCodeBitmap(
        content: String?,
        width: Int,
        height: Int,
        @Nullable character_set: String?,
        @Nullable error_correction: String?,
        @Nullable margin: String?,
        @ColorInt color_black: Int,
        @ColorInt color_white: Int
    ): Bitmap? {
        /** 1.参数合法性判断  */
        if (TextUtils.isEmpty(content)) { // 字符串内容判空
            return null
        }
        if (width < 0 || height < 0) { // 宽和高都需要>=0
            return null
        }
        try {
            /** 2.设置二维码相关配置,生成BitMatrix(位矩阵)对象  */
            val hints: Hashtable<EncodeHintType, String?> = Hashtable()
            if (!TextUtils.isEmpty(character_set)) {
                hints.put(EncodeHintType.CHARACTER_SET, character_set) // 字符转码格式设置
            }
            if (!TextUtils.isEmpty(error_correction)) {
                hints.put(EncodeHintType.ERROR_CORRECTION, error_correction) // 容错级别设置
            }
            if (!TextUtils.isEmpty(margin)) {
                hints.put(EncodeHintType.MARGIN, margin) // 空白边距设置
            }
            val bitMatrix =
                QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)

            /** 3.创建像素数组,并根据BitMatrix(位矩阵)对象为数组元素赋颜色值  */
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (bitMatrix[x, y]) {
                        pixels[y * width + x] = color_black // 黑色色块像素设置
                    } else {
                        pixels[y * width + x] = color_white // 白色色块像素设置
                    }
                }
            }
            /** 4.创建Bitmap对象,根据像素数组设置Bitmap每个像素点的颜色值,之后返回Bitmap对象  */
            val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
        }
        return null
    }
}