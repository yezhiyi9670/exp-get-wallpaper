package wmsdf.cl.exp4.getwallpaper.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;

public class FilePathUtils {

    public static File getDownloadPath(String filename) {
        File externalRoot = Environment.getExternalStorageDirectory();

        File destDir = new File(externalRoot, "Download");
        destDir.mkdir();

        return new File(destDir, filename);
    }

    public static String getMimeType(File file) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String type = fileNameMap.getContentTypeFor(file.getName());
        return type;
    }

    public static void updatePhotoAlbum(Context mContext, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
            values.put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(file));
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM);
            ContentResolver contentResolver = mContext.getContentResolver();
            Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                return;
            }
            try {
                OutputStream out = contentResolver.openOutputStream(uri);
                FileInputStream fis = new FileInputStream(file);
                android.os.FileUtils.copy(fis, out);
                fis.close();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            MediaScannerConnection.scanFile(mContext.getApplicationContext(), new String[]{file.getAbsolutePath()}, new String[]{"image/jpeg"}, null);
        }
    }


}
