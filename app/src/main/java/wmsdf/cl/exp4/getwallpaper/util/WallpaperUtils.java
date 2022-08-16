package wmsdf.cl.exp4.getwallpaper.util;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.FileDescriptor;

public class WallpaperUtils {

    public static Drawable getWallpaperDrawable(Context context) {
        WallpaperManager wpm = WallpaperManager.getInstance(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        return wpm.getDrawable();
    }

    @RequiresApi(24)
    public static Drawable getLockWallpaperDrawable(Context context){
        WallpaperManager wpm = WallpaperManager.getInstance(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        ParcelFileDescriptor mParcelFileDescriptor = wpm.getWallpaperFile(WallpaperManager.FLAG_LOCK); // 获取锁屏壁纸
        if(mParcelFileDescriptor == null){
            return getWallpaperDrawable(context);
        }
        FileDescriptor fileDescriptor = mParcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);//获取Bitmap类型返回值
        try {
            mParcelFileDescriptor.close();
        } catch(Exception e) {
            android.util.Log.e("app/WallpaperUtils","mParcelFileDescriptor.close() error");
        }
        return new BitmapDrawable(context.getResources(), image);
    }

}
