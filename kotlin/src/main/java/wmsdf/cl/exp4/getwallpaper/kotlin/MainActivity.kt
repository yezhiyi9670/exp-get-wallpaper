package wmsdf.cl.exp4.getwallpaper.kotlin

import android.Manifest
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.JsonWriter
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import wmsdf.cl.exp4.getwallpaper.kotlin.hezhidao.palette.ImagePalette
import wmsdf.cl.exp4.getwallpaper.kotlin.intent.PhotoSelectContract
import wmsdf.cl.exp4.getwallpaper.kotlin.intent.WallpaperType
import wmsdf.cl.exp4.getwallpaper.kotlin.util.ColorDeriveUtils
import wmsdf.cl.exp4.getwallpaper.kotlin.util.FilePathUtils
import wmsdf.cl.exp4.getwallpaper.kotlin.util.PermissionUtils
import wmsdf.cl.exp4.getwallpaper.kotlin.util.WallpaperUtils
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import kotlin.math.floor

class MainActivity : AppCompatActivity() {

    private var wallpaperDrawable: BitmapDrawable? = null
    private var fileId: Int = 0

    private var palette1: ImagePalette? = null
    private var palette2: ImagePalette? = null

    private var img_wallpaper: ImageView? = null
    private var btn_get: Button? = null
    private var btn_save: Button? = null
    private var btn_extract: Button? = null
    private var btn_open: Button? = null

    private var color_display: ViewGroup? = null
    private var color_display_size: Int = 8

    private var imageOpenLauncher: ActivityResultLauncher<Unit>? = null

    private fun getColorCompat(resId: Int): Int {
        return ActivityCompat.getColor(this, resId)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        img_wallpaper = findViewById(R.id.img_wallpaper)
        btn_get = findViewById(R.id.btn_get)
        btn_extract = findViewById(R.id.btn_extract)
        btn_save = findViewById(R.id.btn_save)
        btn_open = findViewById(R.id.btn_open)
        btn_save!!.isEnabled = false
        btn_extract!!.isEnabled = false
        color_display = findViewById(R.id.color_display)
        color_display_size = color_display!!.childCount

        setActionGet()
        setActionOpen()
        setActionSave()
        setActionExtract()
        registerImageOpen()
    }

    /**
     * 设置动作“获取壁纸”
     */
    private fun setActionGet() {
        btn_get!!.setOnClickListener {
            getWallpaperDisplay(WallpaperType.SYSTEM)
        }
        if(Build.VERSION.SDK_INT >= 24) {
            btn_get!!.setOnLongClickListener {
                getWallpaperDisplay(WallpaperType.LOCK)
                true
            }
        }
    }
    /**
     * 设置动作“打开文件”
     */
    private fun setActionOpen() {
        btn_open!!.setOnClickListener {
            selectImage()
        }
    }
    /**
     * 设置动作“保存颜色”
     */
    private fun setActionExtract() {
        btn_extract!!.setOnClickListener {
            if (!checkPermissionRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return@setOnClickListener
            }
            val jsonFilePath = String.format("wallpaper%d_colors.json", fileId)
            val jsonTargetFile = FilePathUtils.getDownloadPath(jsonFilePath)
            var success = false
            try {
                jsonTargetFile.createNewFile()
                val writer = JsonWriter(OutputStreamWriter(FileOutputStream(jsonTargetFile), "UTF-8"))
                writer.setIndent("    ")
                writer.beginObject()
                writer.name("main").value((0x00FFFFFF).and(palette2!!.main))
                writer.name("accent").value((0x00FFFFFF).and(palette2!!.accent))
                writer.name("distancedAccent").value((0x00FFFFFF).and(palette2!!.distancedAccent))
                writer.name("colors")
                writer.beginArray()
                for(cl in palette2!!.colors) {
                    writer.value((0x00FFFFFF).and(cl.color))
                }
                writer.endArray()
                writer.endObject()
                writer.flush()
                success = true
            } catch (e: FileNotFoundException) {
                Log.e("app", "Cannot write, file not found.")
                e.printStackTrace()
            } catch (e: IOException) {
                Log.e("app", "Cannot write, IOException.")
                e.printStackTrace()
            }
            if(!success) {
                Toast.makeText(this, R.string.toast_save_fail, Toast.LENGTH_SHORT).show()
            } else {
                val message = String.format(getString(R.string.saved_body_l2), jsonTargetFile.getAbsolutePath())
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.saved_title_json))
                    .setMessage(message)
                    .setPositiveButton(R.string.btn_ok, null)
                    .create()
                    .show()
            }
        }
    }
    /**
     * 设置动作“保存图像”
     */
    private fun setActionSave() {
        btn_save!!.setOnClickListener {
            if (!checkPermissionRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return@setOnClickListener
            }
            val imageFilePath = String.format("wallpaper%d_image.png", fileId)
            val imageTargetFile = FilePathUtils.getDownloadPath(imageFilePath)
            var success = false
            try {
                imageTargetFile.createNewFile()
                val ofstream = FileOutputStream(imageTargetFile)
                wallpaperDrawable!!.bitmap.compress(Bitmap.CompressFormat.PNG, 0, ofstream)
                success = true
            } catch (e: FileNotFoundException) {
                Log.e("app", "Cannot write, file not found.")
                e.printStackTrace()
            } catch (e: IOException) {
                Log.e("app", "Cannot write, IOException.")
                e.printStackTrace()
            }
            if(!success) {
                Toast.makeText(this, R.string.toast_save_fail, Toast.LENGTH_SHORT).show()
            } else {
                FilePathUtils.updatePhotoAlbum(this, imageTargetFile)
                val message = String.format(getString(R.string.saved_body_l1), imageTargetFile.getAbsolutePath())
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.saved_title))
                    .setMessage(message)
                    .setPositiveButton(R.string.btn_ok, null)
                    .create()
                    .show()
            }
        }
    }

    /**
     * 重置按钮颜色
     */
    private fun resetButtonColors() {
        val buttons = arrayOf(btn_open, btn_get, btn_extract, btn_save)
        for (btn in buttons) {
            btn!!.backgroundTintList = ColorDeriveUtils.stateListFromColor(getColorCompat(R.color.theme_main))
            btn.setTextColor(getColorCompat(R.color.back_default))
        }
        window.statusBarColor = getColorCompat(R.color.theme_main_dense)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(getColorCompat(R.color.theme_main)))

        for(i in 0 until color_display_size) {
            color_display!!.getChildAt(i).setBackgroundColor(getColorCompat(R.color.void_default))
        }
    }

    /**
     * 从图像提取颜色并设置样式
     */
    private fun extractColorsUpdate() {
        if(wallpaperDrawable == null) {
            return
        }

        palette1 = ImagePalette(wallpaperDrawable!!.bitmap, 0.75)
        palette2 = ImagePalette(wallpaperDrawable!!.bitmap, 1.0)

        window.statusBarColor = ColorDeriveUtils.tweakLuminosity(palette1!!.main, -0.08, 0.0, 1.0)

        btn_open!!.backgroundTintList = ColorDeriveUtils.stateListFromColor(palette2!!.main)
        btn_open!!.setTextColor(getColorCompat(R.color.back_default))

        btn_get!!.backgroundTintList = ColorDeriveUtils.stateListFromColor(palette2!!.main)
        btn_get!!.setTextColor(getColorCompat(R.color.fore_default))

        btn_extract!!.backgroundTintList = ColorDeriveUtils.stateListFromColor(palette2!!.main)
        btn_extract!!.setTextColor(palette2!!.accent)

        btn_save!!.backgroundTintList = ColorDeriveUtils.stateListFromColor(palette2!!.accent)
        btn_save!!.setTextColor(palette2!!.distancedAccent)

        supportActionBar?.setBackgroundDrawable(ColorDrawable(palette1!!.main))

        for(i in 0 until color_display_size) {
            if(i >= palette2!!.colors.size) {
                break
            }
            color_display!!.getChildAt(i).setBackgroundColor(palette2!!.colors[i].color)
        }
    }

    /**
     * 显示请求权限的窗口
     */
    private fun showPermissionRequest() {
        val dialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.request_title))
                .setMessage(String.format(getString(R.string.request_body), getString(R.string.app_name)))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_ok) { _, _ ->
                    PermissionUtils.requestPermission(this, arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ))
                }
                .create()
        dialog.show()
    }

    /**
     * 检查权限。若不通过，打开请求窗口。
     * @param permission 权限
     * @return 是否已经授权
     */
    private fun checkPermissionRequest(permission: String): Boolean {
        if(!PermissionUtils.checkPermission(this, permission)) {
            showPermissionRequest()
            return false
        }
        return true
    }

    /**
     * 在视图上显示壁纸，并更新控件状态
     */
    private fun displayWallpaper() {
        img_wallpaper!!.setImageDrawable(wallpaperDrawable)
        btn_save!!.isEnabled = true
        btn_extract!!.isEnabled = true
        fileId = 100000 + floor(Math.random() * 900000).toInt()
        resetButtonColors()
        extractColorsUpdate()
    }

    /**
     * 手动选择图像
     */
    private fun selectImage() {
        imageOpenLauncher!!.launch(Unit)
    }
    /**
     * 注册打开图像动作
     */
    private fun registerImageOpen() {
        imageOpenLauncher = registerForActivityResult(PhotoSelectContract()) {
            if(it == null) {
                return@registerForActivityResult
            }
            try {
                val ifstream = contentResolver.openInputStream(it)
                val img = BitmapDrawable.createFromStream(ifstream, "image")
                if(img == null) {
                    Log.e("app", "Cannot get image.")
                    return@registerForActivityResult
                }
                wallpaperDrawable = img as BitmapDrawable
                displayWallpaper()
            } catch(e: FileNotFoundException) {
                Log.e("app", "Selected photo not found!")
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取壁纸并显示
     */
    private fun getWallpaperDisplay(type: WallpaperType) {
        if(!checkPermissionRequest(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            return
        }
        val result = if(type == WallpaperType.SYSTEM || Build.VERSION.SDK_INT < 24) {
            WallpaperUtils.getWallpaperDrawable(this)
        } else {
            WallpaperUtils.getLockWallpaperDrawable(this)
        }
        if(result == null) {
            Toast.makeText(this, R.string.toast_get_fail, Toast.LENGTH_SHORT).show()
            return
        }
        wallpaperDrawable = result as BitmapDrawable
        displayWallpaper()
    }
}