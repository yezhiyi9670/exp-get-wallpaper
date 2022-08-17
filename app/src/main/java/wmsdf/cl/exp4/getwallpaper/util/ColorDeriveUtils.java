package wmsdf.cl.exp4.getwallpaper.util;

import android.content.res.ColorStateList;
import android.util.Log;

import androidx.core.graphics.ColorUtils;

public class ColorDeriveUtils {

    public static ColorStateList stateListFromColor(int color) {
        return new ColorStateList(new int[][]{new int[0]}, new int[]{color});
    }

    public static float getHue(int color0) {
        float[] hsl = new float[]{0.0f, 0.0f, 0.0f};
        ColorUtils.colorToHSL(color0, hsl);
        // Log.d("app/ColorDeriveUtils", String.format("Hue value: %f", hsl[0]));
        return hsl[0] / 360.0f;
    }

    public static float getSaturation(int color0) {
        float[] hsl = new float[]{0.0f, 0.0f, 0.0f};
        ColorUtils.colorToHSL(color0, hsl);
        return hsl[1];
    }

    public static int setLightness(int color0, float val) {
        double[] hsl = new double[]{0.0f, 0.0f, 0.0f};
        androidx.core.graphics.ColorUtils.colorToLAB(color0, hsl);
        hsl[0] = Math.pow(val, 1 / 1.8f) * 100;
        return androidx.core.graphics.ColorUtils.LABToColor(hsl[0], hsl[1], hsl[2]);
    }

    public static float getLightness(int color0) {
        double[] hsl = new double[]{0.0f, 0.0f, 0.0f};
        androidx.core.graphics.ColorUtils.colorToLAB(color0, hsl);
        return (float)Math.pow((float)hsl[0] / 100, 1.8f);
    }

    public static float getHslLightness(int color0) {
        float[] hsl = new float[]{0.0f, 0.0f, 0.0f};
        ColorUtils.colorToHSL(color0, hsl);
        return hsl[2];
    }

    public static int tweakLightness(int color0, float delta, float clampMin, float clampMax) {
        float lightness = getLightness(color0);
        lightness += delta;
        if(lightness > clampMax) {
            lightness = clampMax;
        }
        if(lightness < clampMin) {
            lightness = clampMin;
        }
        return setLightness(color0, lightness);
    }

    public static int getSingleAccent(int color0, float minDelta) {
        float lightness = getLightness(color0);
        if(lightness < 0.5) {
            lightness = Math.min(1.0f, Math.max(lightness + minDelta, (lightness + 1) / 2));
        } else {
            lightness = Math.max(0.0f, Math.min(lightness - minDelta, (lightness + 0) / 2));
        }
        return setLightness(color0, lightness);
    }

    public static int getPairAccent(int color0, int color1, float minDelta) {
        float lightness0 = getLightness(color0);
        float lightness1 = getLightness(color1);
        if((lightness0 < lightness1 && lightness0 <= 1.0 - minDelta / 2) || lightness0 < 0.0 + minDelta / 2) {
            lightness1 = Math.min(1.0f, Math.max(lightness1, lightness0 + minDelta));
        } else {
            lightness1 = Math.max(0.0f, Math.min(lightness1, lightness0 - minDelta));
        }
        return setLightness(color1, lightness1);
    }

    public static int getDistancedAccent(int color0, int color1, int color2, float delta2) {
        float lightness0 = getLightness(color0);
        float lightness1 = getLightness(color1);
        float lightness2 = getLightness(color2);
        if(lightness0 < lightness2) {
            lightness2 = lightness2 - delta2;
        } else {
            lightness2 = lightness2 + delta2;
        }
        if(lightness2 > 1.0f) {
            lightness2 = 1.0f;
        }
        if(lightness2 < 0.0f) {
            lightness2 = 0.0f;
        }
        return setLightness(color1, lightness2);
    }
}
