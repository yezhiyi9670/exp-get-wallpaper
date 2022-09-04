package wmsdf.cl.exp4.getwallpaper.kotlin.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection
import kotlin.Exception

object FilePathUtils {

    fun getDownloadPath(filename: String): File {
        val externalRoot = Environment.getExternalStorageDirectory()

        val destDir = File(externalRoot, "Download")
        destDir.mkdir()

        return File(destDir, filename)
    }

    fun getMimeType(file: File): String {
        val fileNameMap = URLConnection.getFileNameMap()
        val type = fileNameMap.getContentTypeFor(file.name)
        return type
    }

    fun updatePhotoAlbum(context: Context, file: File) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            values.put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(file))
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
            val contentResolver = context.contentResolver
            val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                return
            }
            try {
                val outStream = contentResolver.openOutputStream(uri)
                val fileInputStream = FileInputStream(file)
                if (outStream == null) {
                    return
                }
                android.os.FileUtils.copy(fileInputStream, outStream)
                fileInputStream.close()
                outStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
                null
            )
        }
    }

}