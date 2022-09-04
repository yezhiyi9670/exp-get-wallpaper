package wmsdf.cl.exp4.getwallpaper.kotlin.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.io.FileDescriptor

object WallpaperUtils {

    private fun hasWallpaperAccess(context: Context): Boolean {
        return PermissionUtils.checkPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    @SuppressLint("MissingPermission")
    fun getWallpaperDrawable(context: Context): Drawable? {
        val wpm = WallpaperManager.getInstance(context)
        if (!hasWallpaperAccess(context)) {
            return null
        }
        return wpm.drawable
    }

    @RequiresApi(24)
    @SuppressLint("MissingPermission")
    fun getLockWallpaperDrawable(context: Context): Drawable? {
        val wpm = WallpaperManager.getInstance(context)
        if(!hasWallpaperAccess(context)) {
            return null
        }
        val parcelFileDescriptor: ParcelFileDescriptor?
            = wpm.getWallpaperFile(WallpaperManager.FLAG_LOCK)
        if(parcelFileDescriptor == null) {
            return getWallpaperDrawable(context)
        }
        val fileDescriptor = parcelFileDescriptor.fileDescriptor;
        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        try {
            parcelFileDescriptor.close()
        } catch(e: Exception) {
            Log.e("app/WallpaperUtils", "mParcelFileDescriptor.close() error")
        }
        return BitmapDrawable(context.resources, image)
    }

}