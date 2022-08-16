package wmsdf.cl.exp4.getwallpaper.hezhidao.palette;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.Nullable;

import wmsdf.cl.exp4.getwallpaper.util.ColorDeriveUtils;

public class ImagePalette {

    public int mainColor;
    public int accentColor;
    public int distancedAccentColor;

    private float minDelta0 = 0.35f;
    private float minDelta1 = 0.28f;
    private float minDelta2 = 0.30f;

    public ImagePalette(Bitmap bmp, int maxColor, float maxLightness) {
        MMCQ mmcq = new MMCQ(bmp, maxColor);
        MMCQ.ThemeColor[] colors = mmcq.quantize().toArray(new MMCQ.ThemeColor[0]);

        mainColor = colors[0].getColor();
        if(ColorDeriveUtils.getLightness(mainColor) > maxLightness) {
            mainColor = ColorDeriveUtils.setLightness(mainColor, maxLightness);
        }
        if(colors.length <= 1) {
            accentColor = ColorDeriveUtils.getSingleAccent(mainColor, minDelta0);
            distancedAccentColor = ColorDeriveUtils.getDistancedAccent(mainColor, mainColor, accentColor, minDelta2);
        } else {
            int color1 = colors[1].getColor();
            accentColor = ColorDeriveUtils.getPairAccent(mainColor, color1, minDelta1);
            distancedAccentColor = ColorDeriveUtils.getDistancedAccent(mainColor, color1, accentColor, minDelta2);
        }
    }

}
