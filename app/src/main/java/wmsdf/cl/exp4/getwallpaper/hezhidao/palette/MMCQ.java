package wmsdf.cl.exp4.getwallpaper.hezhidao.palette;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseLongArray;

import androidx.annotation.IntDef;
import androidx.core.graphics.ColorUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import wmsdf.cl.exp4.getwallpaper.util.ColorDeriveUtils;

/**
 * Created by hgm on 9/20/18.
 * Blog: https://blog.csdn.net/hegan2010/article/details/84308152
 * <p>
 * Modified Median Cut Quantization(MMCQ)
 * Leptonica: http://tpgit.github.io/UnOfficialLeptDocs/leptonica/color-quantization.html
 */
public class MMCQ {
    private static final String TAG = MMCQ.class.getSimpleName();

    private static final int MAX_ITERATIONS = 100;

    private static final int COLOR_ALPHA = 0;
    private static final int COLOR_RED = 1;
    private static final int COLOR_GREEN = 2;
    private static final int COLOR_BLUE = 3;

    @IntDef({COLOR_ALPHA, COLOR_RED, COLOR_GREEN, COLOR_BLUE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ColorPart {
    }

    private final int[] mPixelRGB;
    private final int mMaxColor;
    private double mFraction = 0.85d;
    private int mSigbits = 5;
    private int mRshift = 8 - mSigbits;
    private final int mWidth;
    private final int mHeight;
    private final SparseLongArray mPixHisto = new SparseLongArray();


    /**
     * @param bitmap   Image data [[A, R, G, B], ...]
     * @param maxColor Between [2, 256]
     */
    public MMCQ(Bitmap bitmap, int maxColor) {
        this(bitmap, maxColor, 0.85d, 5);
    }

    /**
     * @param bitmap   Image data [[A, R, G, B], ...]
     * @param maxColor Between [2, 256]
     * @param fraction Between [0.3, 0.9]
     * @param sigbits  5 or 6
     */
    public MMCQ(Bitmap bitmap, int maxColor, double fraction, int sigbits) {
        if (maxColor < 2 || maxColor > 256) {
            throw new IllegalArgumentException("maxColor should between [2, 256]!");
        }
        mMaxColor = maxColor;
        if (fraction < 0.3 || fraction > 0.9) {
            throw new IllegalArgumentException("fraction should between [0.3, 0.9]!");
        }
        mFraction = fraction;
        if (sigbits < 5 || sigbits > 6) {
            throw new IllegalArgumentException("sigbits should between [5, 6]!");
        }
        mSigbits = sigbits;
        mRshift = 8 - mSigbits;

        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        double hScale = 100d / (double) height;
        double wScale = 100d / (double) width;
        double scale = Math.min(hScale, wScale);
        if (scale < 0.8) {
            bitmap = Bitmap.createScaledBitmap(
                    bitmap, (int) (scale * width), (int) (scale * height), false);
        }
        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();
        mPixelRGB = new int[mWidth * mHeight];
        bitmap.getPixels(mPixelRGB, 0, mWidth, 0, 0, mWidth, mHeight);

        initPixHisto();
    }

    private void initPixHisto() {
        for (int color : mPixelRGB) {
            int alpha = Color.alpha(color);
            if (alpha < 128) {
                continue;
            }
            int red = Color.red(color) >> mRshift;
            int green = Color.green(color) >> mRshift;
            int blue = Color.blue(color) >> mRshift;
            int colorIndex = getColorIndexWithRgb(red, green, blue);
            long count = mPixHisto.get(colorIndex);
            mPixHisto.put(colorIndex, count + 1);
        }
    }

    public static int getColorIndexWithRgb(int red, int green, int blue) {
        return (red << 16) | (green << 8) | blue;
    }

    private VBox createVBox() {
        int rMax = getMax(COLOR_RED) >> mRshift;
        int rMin = getMin(COLOR_RED) >> mRshift;
        int gMax = getMax(COLOR_GREEN) >> mRshift;
        int gMin = getMin(COLOR_GREEN) >> mRshift;
        int bMax = getMax(COLOR_BLUE) >> mRshift;
        int bMin = getMin(COLOR_BLUE) >> mRshift;

        return new VBox(rMin, rMax, gMin, gMax, bMin, bMax, 1 << mRshift, mPixHisto);
    }

    private int getMax(@ColorPart int which) {
        int max = 0;
        for (int color : mPixelRGB) {
            int value = getColorPart(color, which);
            if (max < value) {
                max = value;
            }
        }
        return max;
    }

    private int getMin(@ColorPart int which) {
        int min = Integer.MAX_VALUE;
        for (int color : mPixelRGB) {
            int value = getColorPart(color, which);
            if (min > value) {
                min = value;
            }
        }
        return min;
    }

    private static VBox[] medianCutApply(VBox vBox) {
        long nPixs = 0;

        switch (vBox.mAxis) {
            case COLOR_RED: // Red axis is largest
                for (int r = vBox.r1; r <= vBox.r2; r++) {
                    for (int g = vBox.g1; g <= vBox.g2; g++) {
                        for (int b = vBox.b1; b <= vBox.b2; b++) {
                            long count = vBox.mHisto.get(getColorIndexWithRgb(r, g, b));
                            nPixs += count;
                        }
                    }
                    if (nPixs >= vBox.mNumPixs / 2) {
                        int left = r - vBox.r1;
                        int right = vBox.r2 - r;
                        int r2 = (left >= right) ?
                                Math.max(vBox.r1, r - 1 - left / 2) :
                                Math.min(vBox.r2 - 1, r + right / 2);
                        VBox vBox1 = new VBox(vBox.r1, r2, vBox.g1, vBox.g2, vBox.b1, vBox.b2,
                                vBox.mMultiple, vBox.mHisto);
                        VBox vBox2 = new VBox(r2 + 1, vBox.r2, vBox.g1, vBox.g2, vBox.b1, vBox.b2,
                                vBox.mMultiple, vBox.mHisto);
                        //Log.d(TAG, "VBOX " + vBox1.mNumPixs + " " + vBox2.mNumPixs);
                        if (isSimilarColor(vBox1.getAvgColor(), vBox2.getAvgColor())) {
                            break;
                        } else {
                            return new VBox[]{vBox1, vBox2};
                        }
                    }
                }

            case COLOR_GREEN: // Green axis is largest
                for (int g = vBox.g1; g <= vBox.g2; g++) {
                    for (int b = vBox.b1; b <= vBox.b2; b++) {
                        for (int r = vBox.r1; r <= vBox.r2; r++) {
                            long count = vBox.mHisto.get(getColorIndexWithRgb(r, g, b));
                            nPixs += count;
                        }
                    }
                    if (nPixs >= vBox.mNumPixs / 2) {
                        int left = g - vBox.g1;
                        int right = vBox.g2 - g;
                        int g2 = (left >= right) ?
                                Math.max(vBox.g1, g - 1 - left / 2) :
                                Math.min(vBox.g2 - 1, g + right / 2);
                        VBox vBox1 = new VBox(vBox.r1, vBox.r2, vBox.g1, g2, vBox.b1, vBox.b2,
                                vBox.mMultiple, vBox.mHisto);
                        VBox vBox2 = new VBox(vBox.r1, vBox.r2, g2 + 1, vBox.g2, vBox.b1, vBox.b2,
                                vBox.mMultiple, vBox.mHisto);
                        //Log.d(TAG, "VBOX " + vBox1.mNumPixs + " " + vBox2.mNumPixs);
                        if (isSimilarColor(vBox1.getAvgColor(), vBox2.getAvgColor())) {
                            break;
                        } else {
                            return new VBox[]{vBox1, vBox2};
                        }
                    }
                }

            case COLOR_BLUE: // Blue axis is largest
                for (int b = vBox.b1; b <= vBox.b2; b++) {
                    for (int r = vBox.r1; r <= vBox.r2; r++) {
                        for (int g = vBox.g1; g <= vBox.g2; g++) {
                            long count = vBox.mHisto.get(getColorIndexWithRgb(r, g, b));
                            nPixs += count;
                        }
                    }
                    if (nPixs >= vBox.mNumPixs / 2) {
                        int left = b - vBox.b1;
                        int right = vBox.b2 - b;
                        int b2 = (left >= right) ?
                                Math.max(vBox.b1, b - 1 - left / 2) :
                                Math.min(vBox.b2 - 1, b + right / 2);
                        VBox vBox1 = new VBox(vBox.r1, vBox.r2, vBox.g1, vBox.g2, vBox.b1, b2,
                                vBox.mMultiple, vBox.mHisto);
                        VBox vBox2 = new VBox(vBox.r1, vBox.r2, vBox.g1, vBox.g2, b2 + 1, vBox.b2,
                                vBox.mMultiple, vBox.mHisto);
                        //Log.d(TAG, "VBOX " + vBox1.mNumPixs + " " + vBox2.mNumPixs);
                        if (isSimilarColor(vBox1.getAvgColor(), vBox2.getAvgColor())) {
                            break;
                        } else {
                            return new VBox[]{vBox1, vBox2};
                        }
                    }
                }
        }
        return new VBox[]{vBox, null};
    }

    private static void iterCut(int maxColor, PriorityQueue<VBox> boxQueue) {
        int nColors = 1;
        int nIters = 0;
        List<VBox> store = new ArrayList<>();
        while (true) {
            if (nColors >= maxColor || boxQueue.isEmpty()) {
                break;
            }
            VBox vBox = boxQueue.poll();
            if (vBox.mNumPixs == 0) {
                Log.w(TAG, "Vbox has no pixels");
                //boxQueue.offer(vBox);
                continue;
            }
            VBox[] vBoxes = medianCutApply(vBox);
            if (vBoxes[0] == vBox || vBoxes[0].mNumPixs == vBox.mNumPixs) {
                store.add(vBoxes[0]);
                continue;
            }
            boxQueue.offer(vBoxes[0]);
            //if (vBoxes[1] != null) {
            nColors += 1;
            boxQueue.offer(vBoxes[1]);
            //}
            nIters += 1;
            if (nIters >= MAX_ITERATIONS) {
                Log.w(TAG, "Infinite loop; perhaps too few pixels!");
                break;
            }
        }
        boxQueue.addAll(store);
    }

    public ArrayList<ThemeColor> quantize() {
        if (mWidth * mHeight < mMaxColor) {
            throw new IllegalArgumentException(
                    "Image({" + mWidth + "}x{" + mHeight + "}) too small to be quantized");
        }

        VBox oriVBox = createVBox();
        PriorityQueue<VBox> pOneQueue = new PriorityQueue<>(mMaxColor);
        pOneQueue.offer(oriVBox);
        int popColors = (int) (mMaxColor * mFraction);
        iterCut(popColors, pOneQueue);

        PriorityQueue<VBox> boxQueue = new PriorityQueue<>(mMaxColor, new Comparator<VBox>() {
            @Override
            public int compare(VBox o1, VBox o2) {
                long priority1 = o1.getPriority() * o1.mVolume;
                long priority2 = o2.getPriority() * o2.mVolume;
                return Long.compare(priority1, priority2);
            }
        });

        boxQueue.addAll(pOneQueue);
        pOneQueue.clear();

        iterCut(mMaxColor - popColors + 1, boxQueue);

        pOneQueue.addAll(boxQueue);
        boxQueue.clear();

        PriorityQueue<ThemeColor> themeColors = new PriorityQueue<>(mMaxColor);

        while (!pOneQueue.isEmpty()) {
            VBox vBox = pOneQueue.poll();
            double proportion = (double) vBox.mNumPixs / oriVBox.mNumPixs;
            if (proportion < 0.05) {
                continue;
            }
            ThemeColor themeColor = new ThemeColor(vBox.getAvgColor(), proportion);
            themeColors.offer(themeColor);
        }

        ArrayList<ThemeColor> lst = new ArrayList<>(themeColors);
        ThemeColor[] arr = lst.toArray(new ThemeColor[0]);
        Arrays.sort(arr, (color1, color2) -> {
            if(color1.getProportion() > color2.getProportion()) return -1;
            if(color1.getProportion() < color2.getProportion()) return +1;
            return 0;
        });
        lst = new ArrayList<ThemeColor>();
        for(ThemeColor color : arr) {
            boolean hasSimilarOne = false;
            for(ThemeColor color2 : lst) {
                if(isSimilarColor(color.getColor(), color2.getColor())) {
                    hasSimilarOne = true;
                }
            }
            if(!hasSimilarOne) {
                lst.add(color);
            }
        }
        return lst;

    }

    public static int getColorPart(int color, @ColorPart int which) {
        switch (which) {
            case COLOR_ALPHA:
                return Color.alpha(color);
            case COLOR_RED:
                return Color.red(color);
            case COLOR_GREEN:
                return Color.green(color);
            case COLOR_BLUE:
                return Color.blue(color);
            default:
                throw new IllegalArgumentException(
                        "parameter which must be COLOR_ALPHA/COLOR_RED/COLOR_GREEN/COLOR_BLUE !");
        }
    }

    private static final double TOLERANCE_HUE = 1.0 / 10;
    private static final double TOLERANCE_DIST = 0.5;
    private static final double TOLERANCE_LIGHT = 0.3;
    private static final double TOLERANCE_SAT = 0.8;

    public static boolean isSimilarColor(int color1, int color2) {
        float hue1 = ColorDeriveUtils.getHue(color1);
        float hue2 = ColorDeriveUtils.getHue(color2);
        float light1 = ColorDeriveUtils.getLightness(color1);
        float light2 = ColorDeriveUtils.getLightness(color2);
        float sat1 = ColorDeriveUtils.getSaturation(color1);
        float sat2 = ColorDeriveUtils.getSaturation(color2);
        double dist = colorDistance(color1, color2);
        float meanLight = (ColorDeriveUtils.getHslLightness(color1) + ColorDeriveUtils.getHslLightness(color2)) / 2;

        float hueDifference = (float)Math.pow(((sat1 + sat2) / 2) * (meanLight <= 0.5 ? meanLight * 2 : (1 - meanLight) * 2), 0.5);

        if(1 == 1) {
            return Math.min(Math.abs(hue2 - hue1), 1.0 - Math.abs(hue2 - hue1)) * hueDifference < TOLERANCE_HUE
                    && Math.abs(light2 - light1) < TOLERANCE_LIGHT
                    && Math.abs(sat2 - sat1) < TOLERANCE_SAT;
        } else {
            return dist < TOLERANCE_DIST;
        }
    }

    public static double colorDistance(int color1, int color2) {
        int r1 = Color.red(color1);
        int g1 = Color.green(color1);
        int b1 = Color.blue(color1);
        int r2 = Color.red(color2);
        int g2 = Color.green(color2);
        int b2 = Color.blue(color2);
        double rd = (r1 - r2) / 255d;
        double gd = (g1 - g2) / 255d;
        double bd = (b1 - b2) / 255d;
        return Math.sqrt(rd * rd + gd * gd + bd * bd);
    }

    public static double distanceToBGW(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        double rg = (r - g) / 255d;
        double gb = (g - b) / 255d;
        double br = (b - r) / 255d;
        return Math.sqrt((rg * rg + gb * gb + br * br) / 3d);
    }

    /**
     * The color space is divided up into a set of 3D rectangular regions (called `vboxes`)
     */
    private static class VBox implements Comparable<VBox> {
        final int r1;
        final int r2;
        final int g1;
        final int g2;
        final int b1;
        final int b2;
        final SparseLongArray mHisto;
        final long mNumPixs;
        final long mVolume;
        final int mAxis;
        final int mMultiple;
        private int mAvgColor = -1;

        VBox(int r1, int r2, int g1, int g2, int b1, int b2, int multiple, SparseLongArray histo) {
            this.r1 = r1;
            this.r2 = r2;
            this.g1 = g1;
            this.g2 = g2;
            this.b1 = b1;
            this.b2 = b2;
            mMultiple = multiple;
            mHisto = histo;
            mNumPixs = population();
            final int rl = Math.abs(r2 - r1) + 1;
            final int gl = Math.abs(g2 - g1) + 1;
            final int bl = Math.abs(b2 - b1) + 1;
            mVolume = rl * gl * bl;
            final int max = Math.max(Math.max(rl, gl), bl);
            if (max == rl) {
                mAxis = COLOR_RED;
            } else if (max == gl) {
                mAxis = COLOR_GREEN;
            } else {
                mAxis = COLOR_BLUE;
            }
        }

        private long population() {
            long sum = 0;
            for (int r = r1; r <= r2; r++) {
                for (int g = g1; g <= g2; g++) {
                    for (int b = b1; b <= b2; b++) {
                        long count = mHisto.get(MMCQ.getColorIndexWithRgb(r, g, b));
                        sum += count;
                    }
                }
            }
            return sum;
        }

        public int getAvgColor() {
            if (mAvgColor == -1) {
                long total = 0;
                long rSum = 0;
                long gSum = 0;
                long bSum = 0;

                for (int r = r1; r <= r2; r++) {
                    for (int g = g1; g <= g2; g++) {
                        for (int b = b1; b <= b2; b++) {
                            long count = mHisto.get(MMCQ.getColorIndexWithRgb(r, g, b));
                            if (count != 0) {
                                total += count;
                                rSum += count * (r + 0.5) * mMultiple;
                                gSum += count * (g + 0.5) * mMultiple;
                                bSum += count * (b + 0.5) * mMultiple;
                            }
                        }
                    }
                }

                int r, g, b;
                if (total == 0) {
                    r = (r1 + r2 + 1) * mMultiple / 2;
                    g = (g1 + g2 + 1) * mMultiple / 2;
                    b = (b2 + b2 + 1) * mMultiple / 2;
                } else {
                    r = (int) (rSum / total);
                    g = (int) (gSum / total);
                    b = (int) (bSum / total);
                }
                mAvgColor = Color.rgb(r, g, b);
            }

            return mAvgColor;
        }

        public long getPriority() {
            return -mNumPixs;
        }

        @Override
        public int compareTo(VBox o) {
            long priority = getPriority();
            long oPriority = o.getPriority();
            return Long.compare(priority, oPriority);
        }
    }

    public static class ThemeColor implements Comparable<ThemeColor>, Parcelable {
        private static final float MIN_CONTRAST_TITLE_TEXT = 3.0f;
        private static final float MIN_CONTRAST_BODY_TEXT = 4.5f;

        private final int mColor;
        private final double mProportion;
        private final double mPriority;

        private boolean mGeneratedTextColors;
        private int mTitleTextColor;
        private int mBodyTextColor;

        private ThemeColor(int color, double proportion) {
            mColor = color;
            mProportion = proportion;
            Log.d(TAG, "proportion:" + mProportion + " RGB:"
                    + Color.red(mColor) + " " + Color.green(mColor) + " " + Color.blue(mColor));
            // (...) / 3d * (3 / 2d)
            double distance = colorDistance(mColor, Color.WHITE);
            mPriority = mProportion * distance;
        }

        private ThemeColor(Parcel in) {
            mColor = in.readInt();
            mProportion = in.readDouble();
            mPriority = in.readDouble();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mColor);
            dest.writeDouble(mProportion);
            dest.writeDouble(mPriority);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<ThemeColor> CREATOR = new Creator<ThemeColor>() {
            @Override
            public ThemeColor createFromParcel(Parcel in) {
                return new ThemeColor(in);
            }

            @Override
            public ThemeColor[] newArray(int size) {
                return new ThemeColor[size];
            }
        };

        @Override
        public int compareTo(ThemeColor themeColor) {
            double oPriority = themeColor.mPriority;
            return Double.compare(oPriority, mPriority);
        }

        public int getColor() {
            return mColor;
        }

        public double getProportion() {
            return mProportion;
        }

        public int getBodyTextColor() {
            ensureTextColorsGenerated();
            return mBodyTextColor;
        }

        public int getTitleTextColor() {
            ensureTextColorsGenerated();
            return mTitleTextColor;
        }

        private void ensureTextColorsGenerated() {
            if (!mGeneratedTextColors) {
                // First check white, as most colors will be dark
                final int lightBodyAlpha = ColorUtils.calculateMinimumAlpha(
                        Color.WHITE, mColor, MIN_CONTRAST_BODY_TEXT);
                final int lightTitleAlpha = ColorUtils.calculateMinimumAlpha(
                        Color.WHITE, mColor, MIN_CONTRAST_TITLE_TEXT);

                if (lightBodyAlpha != -1 && lightTitleAlpha != -1) {
                    // If we found valid light values, use them and return
                    mBodyTextColor = ColorUtils.setAlphaComponent(Color.WHITE, lightBodyAlpha);
                    mTitleTextColor = ColorUtils.setAlphaComponent(Color.WHITE, lightTitleAlpha);
                    mGeneratedTextColors = true;
                    return;
                }

                final int darkBodyAlpha = ColorUtils.calculateMinimumAlpha(
                        Color.BLACK, mColor, MIN_CONTRAST_BODY_TEXT);
                final int darkTitleAlpha = ColorUtils.calculateMinimumAlpha(
                        Color.BLACK, mColor, MIN_CONTRAST_TITLE_TEXT);

                if (darkBodyAlpha != -1 && darkTitleAlpha != -1) {
                    // If we found valid dark values, use them and return
                    mBodyTextColor = ColorUtils.setAlphaComponent(Color.BLACK, darkBodyAlpha);
                    mTitleTextColor = ColorUtils.setAlphaComponent(Color.BLACK, darkTitleAlpha);
                    mGeneratedTextColors = true;
                    return;
                }

                // If we reach here then we can not find title and body values which use the same
                // lightness, we need to use mismatched values
                mBodyTextColor = lightBodyAlpha != -1
                        ? ColorUtils.setAlphaComponent(Color.WHITE, lightBodyAlpha)
                        : ColorUtils.setAlphaComponent(Color.BLACK, darkBodyAlpha);
                mTitleTextColor = lightTitleAlpha != -1
                        ? ColorUtils.setAlphaComponent(Color.WHITE, lightTitleAlpha)
                        : ColorUtils.setAlphaComponent(Color.BLACK, darkTitleAlpha);
                mGeneratedTextColors = true;
            }
        }
    }
}