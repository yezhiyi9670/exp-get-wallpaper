package wmsdf.cl.exp4.getwallpaper.kotlin.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {

    fun requestPermission(context: Activity, permissions: Array<String>) {
        ActivityCompat.requestPermissions(context, permissions, 1)
    }

    fun checkPermission(context: Context, permission: String): Boolean {
        val permissionState = ContextCompat.checkSelfPermission(context, permission)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

}