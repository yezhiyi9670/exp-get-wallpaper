package wmsdf.cl.exp4.getwallpaper.kotlin.hezhidao.palette

import android.graphics.Bitmap
import wmsdf.cl.exp4.getwallpaper.kotlin.util.ColorDeriveUtils

class ImagePalette(bmp: Bitmap, maxLumin: Double) {

    val main: Int
    val accent: Int
    val distancedAccent: Int
    val colors: Array<MMCQ.ThemeColor>

    private val minDelta0 = 0.35
    private val minDelta1 = 0.30
    private val minDelta2 = 0.30

    init {
        val mmcq = MMCQ(bmp, 256, 0.5, 5)
        colors = mmcq.quantize().toArray(emptyArray())

        var mainColor = colors[0].color
        if(ColorDeriveUtils.getLuminosity(mainColor) > maxLumin) {
            mainColor = ColorDeriveUtils.setLuminosity(mainColor, maxLumin)
        }
        main = mainColor

        if(colors.size <= 1) {
            accent = ColorDeriveUtils.getSingleAccent(main, minDelta0)
            distancedAccent = ColorDeriveUtils.getDistancedAccent(main, main, accent, minDelta2)
        } else {
            val color1 = colors[1].color
            accent = ColorDeriveUtils.getPairAccent(main, color1, minDelta1)
            distancedAccent = ColorDeriveUtils.getDistancedAccent(main, color1, accent, minDelta2)
        }
    }

}