package wmsdf.cl.exp4.getwallpaper.kotlin.util

import android.content.res.ColorStateList
import androidx.core.graphics.ColorUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ColorDeriveUtils {

    private val GAMMA = 1.8

    /**
     * 从单个颜色创建空白的 ColorStateList
     */
    @JvmStatic
    fun stateListFromColor(color: Int): ColorStateList {
        return ColorStateList(arrayOf(intArrayOf()), intArrayOf(color))
    }

    /**
     * 颜色转换到 HSL(360, 1, 1)，返回结果
     */
    @JvmStatic
    private fun colorToHSL(color0: Int): DoubleArray {
        val hsl = floatArrayOf(0.0f, 0.0f, 0.0f)
        ColorUtils.colorToHSL(color0, hsl)
        return doubleArrayOf(hsl[0] * 1.0, hsl[1] * 1.0, hsl[2] * 1.0)
    }
    /**
     * 获取 HSL 下的色相(1)
     */
    @JvmStatic
    fun getHue(color0: Int): Double {
        return colorToHSL(color0)[0] / 360.0
    }
    /**
     * 获取 HSL 下的饱和度(1)
     */
    @JvmStatic
    fun getSaturation(color0: Int): Double {
        return colorToHSL(color0)[1]
    }
    /**
     * 获取 HSL 下色值(1)
     */
    @JvmStatic
    fun getHslLightness(color0: Int): Double {
        return colorToHSL(color0)[2]
    }

    /**
     * 颜色转换到 LAB(100, 1, 1)，返回结果
     */
    @JvmStatic
    fun colorToLAB(color0: Int): DoubleArray {
        val lab = DoubleArray(3) { 0.0 }
        ColorUtils.colorToLAB(color0, lab)
        return lab
    }
    /**
     * 获取 LAB 下的亮度(1)
     */
    @JvmStatic
    fun getLuminosity(color0: Int): Double {
        val lab = colorToLAB(color0)
        return (lab[0] / 100).pow(GAMMA)
    }
    /**
     * 为颜色重新设置 LAB 下的亮度(1)
     */
    @JvmStatic
    fun setLuminosity(color0: Int, luminosity: Double): Int {
        val lab = colorToLAB(color0)
        lab[0] = luminosity.pow(1 / GAMMA) * 100
        return ColorUtils.LABToColor(lab[0], lab[1], lab[2])
    }

    /**
     * 调节颜色的亮度，但不超过指定范围
     */
    @JvmStatic
    fun tweakLuminosity(color0: Int, delta: Double, clampMin: Double, clampMax: Double): Int {
        var lumin = getLuminosity(color0)
        lumin += delta
        if (lumin > clampMax) {
            lumin = clampMax
        }
        if (lumin < clampMin) {
            lumin = clampMin
        }
        return setLuminosity(color0, lumin)
    }

    /**
     * 在色板仅有单个颜色的情况下，计算强调色
     * @param color0 主题色
     * @param minDelta 亮度的最小差值
     * @return 强调色
     */
    @JvmStatic
    fun getSingleAccent(color0: Int, minDelta: Double): Int {
        return getPairAccent(color0, color0, minDelta)
    }

    /**
     * 在色板有两个颜色的情况下，计算强调色
     * @param color0 主题色
     * @param color1 色板上的第二颜色
     * @param minDelta 主题色与强调色亮度的最小差值
     * @return 强调色
     */
    @JvmStatic
    fun getPairAccent(color0: Int, color1:Int, minDelta: Double): Int {
        var shouldUseDarkerAccent: Boolean;
        val lumin0 = getLuminosity(color0)
        var lumin1 = getLuminosity(color1)
        shouldUseDarkerAccent = when {
            lumin0 > 1.0 - minDelta / 2 -> true
            lumin0 < 0.0 + minDelta / 2 -> false
            lumin0 > lumin1 -> true
            lumin0 < lumin1 -> false
            lumin0 >= 0.5 -> true
            else -> false
        }

        if(shouldUseDarkerAccent) {
            lumin1 = max(0.0, min(lumin1, lumin0 - minDelta))
        } else {
            lumin1 = min(1.0, max(lumin1, lumin0 + minDelta))
        }

        return setLuminosity(color1, lumin1)
    }

    /**
     * 计算强调色的变种（distancedAccent）
     * @param color0 主题色
     * @param color1 色板上的第二颜色，若无，使用主题色
     * @param color2 计算得到的强调色
     * @param delta 强调色变种与强调色的亮度差
     * @return 强调色变种
     */
    @JvmStatic
    fun getDistancedAccent(color0: Int, color1: Int, color2: Int, delta: Double): Int {
        val lumin0 = getLuminosity(color0)
        var lumin2 = getLuminosity(color2)

        if(lumin0 < lumin2) {
            lumin2 = max(0.0, lumin2 - delta)
        } else {
            lumin2 = min(1.0, lumin2 + delta)
        }

        return setLuminosity(color1, lumin2)
    }
}
