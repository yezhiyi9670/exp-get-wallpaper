package wmsdf.cl.exp4.getwallpaper.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionUtils {

    public static void requestPermission(Activity context, String[] permissions) {
        ActivityCompat.requestPermissions(context, permissions, 1);
    }

    public static boolean checkPermission(Context context, String permission) {
        int permissionState = ContextCompat.checkSelfPermission(context, permission);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

}
