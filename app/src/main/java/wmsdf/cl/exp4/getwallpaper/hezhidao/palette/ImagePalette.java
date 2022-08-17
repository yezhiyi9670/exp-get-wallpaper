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
    public MMCQ.ThemeColor[] colors;

    private float minDelta0 = 0.35f;
    private float minDelta1 = 0.30f;
    private float minDelta2 = 0.30f;

    public ImagePalette(Bitmap bmp, int maxColor, float maxLightness) {
        MMCQ mmcq = new MMCQ(bmp, maxColor, 0.5, 5);
        colors = mmcq.quantize().toArray(new MMCQ.ThemeColor[0]);

        // Log.d("app", String.format("Color scheme length: %d", colors.length));

        // for(int i = 0; i < colors.length; i++) {
        //     Log.d("app/ImagePalette", String.format("%d %f", i, colors[i].getProportion()));
        // }

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
