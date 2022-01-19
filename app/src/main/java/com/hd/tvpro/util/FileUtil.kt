package utils

import com.hd.tvpro.app.App
import java.io.File

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
}