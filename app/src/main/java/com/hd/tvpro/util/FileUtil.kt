package utils

import android.util.Log
import com.hd.tvpro.app.App
import com.hd.tvpro.util.StringUtil
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

/**
 * @author fisher
 * @description 文件工具类
 */
object FileUtil {
    /**
     * 将String变成文本文件
     *
     * @param text     源String
     * @param fileName 目标文件路径
     */
    fun stringToFile(text: String, fileName: String) {
        try {
            val file = File(App.INSTANCE.cacheDir, fileName)
            val dir: File? = file.parentFile
            if (dir != null && !dir.exists()) {
                dir.mkdirs()
            }
            file.writeText(text)
        } catch (e: Exception) {
        }
    }

    fun fileToString(filePath: String): String {
        try {
            val file = File(App.INSTANCE.cacheDir, filePath)
            if (!file.exists()) {
                return ""
            }
            return file.readText()
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * 删除整个目录path,包括该目录下所有的子目录和文件
     *
     * @param path
     */
    fun deleteFile(path: String) {
        val rootFile = File(path)
        if (rootFile.exists()) {
            rootFile.delete()
        }
    }

    fun getFileName(url0: String): String {
        var url = url0
        if (StringUtil.isEmpty(url)) {
            return url
        }
        var s = url.split("#").toTypedArray()
        url = s[0]
        s = url.split("\\?").toTypedArray()
        url = s[0]
        val start = url.lastIndexOf("/")
        return if (start != -1 && start < url.length - 1) {
            decodeUrl(url.substring(start + 1), "UTF-8")
        } else {
            url0
        }
    }

    fun decodeUrl(str0: String, code: String?): String { //url解码
        var str = str0
        try {
            str = str.replace("%(?![0-9a-fA-F]{2})".toRegex(), "%25")
            str = str.replace("\\+".toRegex(), "%2B")
            str = URLDecoder.decode(str, code)
        } catch (e: UnsupportedEncodingException) {
            Log.e("FileUtil", "decodeUrl: ", e)
        }
        return str
    }
}