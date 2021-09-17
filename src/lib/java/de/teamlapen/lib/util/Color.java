package de.teamlapen.lib.util;

import java.util.Arrays;

public class Color {

    public static final Color WHITE = new Color(255, 255, 255);
    public static final Color BLUE = new Color(0, 0, 255);
    public static final Color MAGENTA = new Color(255, 0, 255);
    public static final Color MAGENTA_DARK = new Color(124, 0, 124);
    public static final Color GRAY = new Color(128, 128, 128);
    public static final Color GREEN = new Color(0, 255, 0);
    public static final Color RED = new Color(255, 0, 0);
    public static final Color YELLOW    = new Color(255, 255, 0);


    private final int value;
    private final float[] frgbvalue;

    public Color(int red, int green, int blue, int alpha) {
        this(getRgb(red, green, blue, alpha), true);
    }

    public Color(int rgb) {
        this(rgb, false);
    }

    public Color(int rgb, boolean hasAlpha) {
        if (!hasAlpha) {
            rgb = 0xff000000 | rgb;
        }
        this.value = rgb;
        this.frgbvalue = new float[4];
        this.frgbvalue[0] = ((float) getRed()) / 255f;
        this.frgbvalue[1] = ((float) getGreen()) / 255f;
        this.frgbvalue[2] = ((float) getBlue()) / 255f;
        this.frgbvalue[3] = ((float) getAlpha()) / 255f;
    }

    public Color(int red, int green, int blue) {
        this(red, green, blue, 255);
    }

    public Color(float red, float green, float blue, float alpha) {
        this((int) red * 255, (int) green * 255, (int) blue * 255, (int) alpha * 255);
    }

    public Color(float red, float green, float blue) {
        this(red, green, blue, 1);
    }

    public static int getRgb(int red, int green, int blue) {
        return getRgb(red, green, blue, 255);
    }

    public int getRed() {
        return (value >> 16) & 0xFF;
    }

    public int getGreen() {
        return (value >> 8) & 0xFF;
    }

    public int getBlue() {
        return (value) & 0xFF;
    }

    public int getAlpha() {
        return (value >> 24) & 0xff;
    }

    public int getRGB() {
        return value;
    }

    public float[] getRGBComponents() {
        return Arrays.copyOf(this.frgbvalue,4);
    }

    public float[] getRGBColorComponents() {
        return Arrays.copyOf(this.frgbvalue,3);
    }

    public static int getRgb(int red, int green, int blue, int alpha) {
        return ((alpha & 0xFF) << 24) |
                ((red & 0xFF) << 16) |
                ((green & 0xFF) << 8) |
                ((blue & 0xFF));
    }

}
