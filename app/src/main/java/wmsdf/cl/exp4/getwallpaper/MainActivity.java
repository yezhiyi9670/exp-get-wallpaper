package wmsdf.cl.exp4.getwallpaper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.JsonWriter;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import wmsdf.cl.exp4.getwallpaper.hezhidao.palette.ImagePalette;
import wmsdf.cl.exp4.getwallpaper.hezhidao.palette.MMCQ;
import wmsdf.cl.exp4.getwallpaper.intent.PhotoSelectContract;
import wmsdf.cl.exp4.getwallpaper.intent.WallpaperType;
import wmsdf.cl.exp4.getwallpaper.util.ColorDeriveUtils;
import wmsdf.cl.exp4.getwallpaper.util.FilePathUtils;
import wmsdf.cl.exp4.getwallpaper.util.PermissionUtils;
import wmsdf.cl.exp4.getwallpaper.util.WallpaperUtils;

public class MainActivity extends AppCompatActivity {

    private BitmapDrawable wallpaperDrawable = null;
    int fileId = 0;

    ImagePalette palette1 = null;
    ImagePalette palette2 = null;

    private ImageView img_wallpaper;
    private Button btn_get;
    private Button btn_save;
    private Button btn_extract;
    private Button btn_open;

    private ViewGroup color_display;
    private int color_display_size = 0;

    private ActivityResultLauncher<Integer> imageOpenLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        img_wallpaper = findViewById(R.id.img_wallpaper);
        btn_get = findViewById(R.id.btn_get);
        btn_extract = findViewById(R.id.btn_extract);
        btn_save = findViewById(R.id.btn_save);
        btn_open = findViewById(R.id.btn_open);
        btn_save.setEnabled(false);
        btn_extract.setEnabled(false);
        color_display = findViewById(R.id.color_display);
        color_display_size = color_display.getChildCount();

        setActionGet();
        setActionOpen();
        setActionSave();
        setActionExtract();
        registerImageOpen();
    }

    /**
     * 设置动作“获取壁纸”
     */
    private void setActionGet() {
        btn_get.setOnClickListener(targetView -> {
            getWallpaperDisplay(WallpaperType.SYSTEM);
        });
        if(Build.VERSION.SDK_INT >= 24) {
            btn_get.setOnLongClickListener(targetView -> {
                getWallpaperDisplay(WallpaperType.LOCK);
                return true;
            });
        }
    }
    /**
     * 设置动作“打开文件”
     */
    private void setActionOpen() {
        btn_open.setOnClickListener(targetView -> {
            selectImage();
        });
    }
    /**
     * 设置动作“提取颜色”
     */
    private void setActionExtract() {
        btn_extract.setOnClickListener(targetView -> {
            if(!checkPermissionRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return;
            }
            String jsonFilePath = String.format("wallpaper%d_colors.json", fileId);
            File jsonTargetFile = FilePathUtils.getDownloadPath(jsonFilePath);
            boolean success = false;
            try {
                jsonTargetFile.createNewFile();
                JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(jsonTargetFile), "UTF-8"));
                writer.setIndent("    ");
                writer.beginObject();
                writer.name("main").value(0x00FFFFFF & palette2.mainColor);
                writer.name("accent").value(0x00FFFFFF & palette2.accentColor);
                writer.name("distancedAccent").value(0x00FFFFFF & palette2.distancedAccentColor);
                writer.name("colors");
                writer.beginArray();
                for(MMCQ.ThemeColor cl : palette2.colors) {
                    writer.value(0x00FFFFFF & cl.getColor());
                }
                writer.endArray();
                writer.endObject();
                writer.flush();
                success = true;
            } catch (FileNotFoundException e) {
                Log.e("app", "Cannot write, file not found.");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e("app", "Cannot write, IOException.");
                e.printStackTrace();
            }
            if(!success) {
                Toast.makeText(this, R.string.toast_save_fail, Toast.LENGTH_SHORT).show();
            } else {
                String message = String.format(getString(R.string.saved_body_l2), jsonTargetFile.getAbsolutePath());
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.saved_title_json))
                        .setMessage(message)
                        .setPositiveButton(R.string.btn_ok, null)
                        .create()
                        .show();
            }
        });
    }
    /**
     * 设置动作“保存图像”
     */
    private void setActionSave() {
        btn_save.setOnClickListener(targetView -> {
            if(!checkPermissionRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return;
            }
            String imageFilePath = String.format("wallpaper%d_image.png", fileId);
            File imageTargetFile = FilePathUtils.getDownloadPath(imageFilePath);
            boolean success = false;
            try {
                imageTargetFile.createNewFile();
                FileOutputStream ofstream = new FileOutputStream(imageTargetFile);
                wallpaperDrawable.getBitmap().compress(Bitmap.CompressFormat.PNG, 0, ofstream);
                success = true;
            } catch (FileNotFoundException e) {
                Log.e("app", "Cannot write, file not found.");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e("app", "Cannot write, IOException.");
                e.printStackTrace();
            }
            if(!success) {
                Toast.makeText(this, R.string.toast_save_fail, Toast.LENGTH_SHORT).show();
            } else {
                FilePathUtils.updatePhotoAlbum(this, imageTargetFile);
                String message = String.format(getString(R.string.saved_body_l1), imageTargetFile.getAbsolutePath());
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.saved_title))
                        .setMessage(message)
                        .setPositiveButton(R.string.btn_ok, null)
                        .create()
                        .show();
            }
        });
    }

    /**
     * 重置按钮颜色
     */
    private void resetButtonColors() {
        Button[] buttons = new Button[]{btn_open, btn_get, btn_extract, btn_save};
        for (Button btn : buttons) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                btn.setBackgroundTintList(ColorDeriveUtils.stateListFromColor(ActivityCompat.getColor(this, R.color.theme_main)));
                btn.setTextColor(ActivityCompat.getColor(this, R.color.back_default));
            }
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ActivityCompat.getColor(this, R.color.theme_main_dense));
        }
        ((AppCompatActivity)this).getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ActivityCompat.getColor(this, R.color.theme_main)));

        for(int i = 0; i < color_display_size; i++) {
            color_display.getChildAt(i).setBackgroundColor(ActivityCompat.getColor(this, R.color.void_default));
        }
    }

    /**
     * 提取颜色，设置样式
     */
    private void extractColorsUpdate() {
        palette1 = new ImagePalette(wallpaperDrawable.getBitmap(), 256, 0.75f);
        palette2 = new ImagePalette(wallpaperDrawable.getBitmap(), 256, 1.0f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ColorDeriveUtils.tweakLightness(palette1.mainColor, -0.08f, 0.0f, 1.0f));

            btn_open.setBackgroundTintList(ColorDeriveUtils.stateListFromColor(palette2.mainColor));
            btn_open.setTextColor(ActivityCompat.getColor(this, R.color.back_default));

            btn_get.setBackgroundTintList(ColorDeriveUtils.stateListFromColor(palette2.mainColor));
            btn_get.setTextColor(ActivityCompat.getColor(this, R.color.fore_default));

            btn_extract.setBackgroundTintList(ColorDeriveUtils.stateListFromColor(palette2.mainColor));
            btn_extract.setTextColor(palette2.accentColor);

            btn_save.setBackgroundTintList(ColorDeriveUtils.stateListFromColor(palette2.accentColor));
            btn_save.setTextColor(palette2.distancedAccentColor);
        }
        ((AppCompatActivity)this).getSupportActionBar().setBackgroundDrawable(new ColorDrawable(palette1.mainColor));

        for(int i = 0; i < color_display_size; i++) {
            if(i >= palette2.colors.length) {
                break;
            }
            color_display.getChildAt(i).setBackgroundColor(palette2.colors[i].getColor());
        }
    }

    /**
     * 显示请求权限的窗口
     */
    private void showPermissionRequest() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.request_title))
                .setMessage(String.format(getString(R.string.request_body), getString(R.string.app_name)))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_ok, (dialogInterface, i) -> {
                    PermissionUtils.requestPermission(this, new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    });
                })
                .create();

        dialog.show();
    }

    /**
     * 检查权限。若不通过，打开请求窗口。
     * @param permission 权限
     * @return 是否已经授权
     */
    private boolean checkPermissionRequest(String permission) {
        if(!PermissionUtils.checkPermission(this, permission)) {
            showPermissionRequest();
            return false;
        }
        return true;
    }

    /**
     * 在视图上显示壁纸，并更新控件状态
     */
    private void displayWallpaper() {
        img_wallpaper.setImageDrawable(wallpaperDrawable);
        btn_save.setEnabled(true);
        btn_extract.setEnabled(true);
        fileId = 10000 + (int)(Math.random() * 90000);
        resetButtonColors();
        extractColorsUpdate();
    }

    /**
     * 手动选择图像
     */
    private void selectImage() {
        imageOpenLauncher.launch(0);
    }

    /**
     * 注册打开图像
     */
    private void registerImageOpen() {
         imageOpenLauncher = registerForActivityResult(new PhotoSelectContract(), result -> {
             if(result == null) {
                 return;
             }
             try {
                 InputStream ifstream = getContentResolver().openInputStream(result);
                 BitmapDrawable img = (BitmapDrawable)BitmapDrawable.createFromStream(ifstream, "image");
                 if (img == null) {
                     Log.e("app", "Cannot get image.");
                     return;
                 }
                 wallpaperDrawable = img;
                 displayWallpaper();
             } catch(FileNotFoundException e) {
                 Log.e("app", "File not found!");
                 e.printStackTrace();
             }
         });
    }

    /**
     * 获取壁纸并显示
     */
    private void getWallpaperDisplay(WallpaperType type) {
        if(!checkPermissionRequest(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            return;
        }
        wallpaperDrawable = null;
        if(type == WallpaperType.SYSTEM || Build.VERSION.SDK_INT < 24) {
            wallpaperDrawable = (BitmapDrawable)WallpaperUtils.getWallpaperDrawable(this);
        } else {
            wallpaperDrawable = (BitmapDrawable)WallpaperUtils.getLockWallpaperDrawable(this);
        }
        if(wallpaperDrawable == null) {
            Toast.makeText(this, R.string.toast_get_fail, Toast.LENGTH_SHORT).show();
            return;
        }
        displayWallpaper();
    }
}