package org.cobaltians.cobalt.activities;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.util.TypedValue;

import org.cobaltians.cobalt.Cobalt;

/**
 * Manage Themes colors
 * @author rsp
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CobaltThemes {

    protected static final String TAG = CobaltActivity.class.getSimpleName();
    private CobaltActivity activity;
    private static CobaltThemes mInstance;

    public final int COLOR_PRIMARY = android.R.attr.colorPrimary;
    public final int COLOR_PRIMARY_DARK = android.R.attr.colorPrimaryDark;
    public final int COLOR_ACCENT = android.R.attr.colorAccent;
    public final int COLOR_NAVIGATION_BAR = android.R.attr.navigationBarColor;
    public final int COLOR_STATUS_BAR = android.R.attr.statusBarColor;
    public final int COLOR_TEXT_PRIMARY = android.R.attr.textColorPrimary;
    public final int COLOR_TEXT_SECONDARY = android.R.attr.textColorSecondary;
    public final int COLOR_BACKGROUND = android.R.attr.colorBackground;
    public final int COLOR_FOREGROUND = android.R.attr.colorForeground;

    public CobaltThemes(CobaltActivity activity) {
        this.activity = activity;
    }

    /**
     * Returns singleton class instance
     */
    public static CobaltThemes getInstance(CobaltActivity activity) {
        if (mInstance == null) {
            synchronized (CobaltThemes.class) {
                if (mInstance == null) {
                    mInstance = new CobaltThemes(activity);
                }
            }
        }
        return mInstance;
    }

    /**
     * Methods used to get colors of theme used by the app (theme defined in the manifest's app,
     * generally 'theme.cobalt.light')
     */

    public String getBackgroundColor() {
        return getAppThemeValue(COLOR_BACKGROUND);
    }

    public String getTextColor() {
        return getAppThemeValue(COLOR_TEXT_PRIMARY);
    }

    public String getForegroundColor() {
        return getAppThemeValue(COLOR_FOREGROUND);
    }

    public String getTextColorSecondary() {
        return getAppThemeValue(COLOR_TEXT_SECONDARY);
    }

    /*
     * API < 21: new theme color usage are replaced by their lower-api equivalent
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public String getDarkColor() {
        return getAppThemeValue(COLOR_PRIMARY_DARK);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public String getStatusBarColor() {
        return getAppThemeValue(COLOR_STATUS_BAR);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public String getAccentColor() {
        return getAppThemeValue(COLOR_ACCENT);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public String getPrimaryColor() {
        return getAppThemeValue(COLOR_PRIMARY);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public String getBottomBarColor() {
        return getAppThemeValue(COLOR_NAVIGATION_BAR);
    }

    /**
     * getAppThemeValue (int themeAttr)
     * Return a theme color to proper HexValue
     *
     * @param themeAttr int corresponding to an attribute of the theme
     *                  Possibles parameters are:
     *                  android.R.attr.colorPrimary // require api > 21
     *                  android.R.attr.colorPrimaryDark // require api > 21
     *                  android.R.attr.colorAccent // require api > 21
     *                  android.R.attr.navigationBarColor // require api > 21
     *                  android.R.attr.statusBarColor // require api > 21
     *                  android.R.attr.textColorPrimary
     *                  android.R.attr.textColorSecondary
     *                  android.R.attr.colorBackground
     *                  android.R.attr.colorForeground
     * @see String TypedValueToHexColor(TypedValue typedValue)
     */
    private String getAppThemeValue(int themeAttr) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar == null) {
            return null;
        }
        TypedValue typedValue = new TypedValue();
        switch (themeAttr) {
            case android.R.attr.colorPrimary: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    actionBar.getThemedContext().getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
                else
                    actionBar.getThemedContext().getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
                break;
            }
            case android.R.attr.colorPrimaryDark: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    actionBar.getThemedContext().getTheme().resolveAttribute(android.R.attr.colorPrimaryDark, typedValue, true);
                else
                    actionBar.getThemedContext().getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
                break;
            }
            case android.R.attr.colorAccent: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    actionBar.getThemedContext().getTheme().resolveAttribute(android.R.attr.colorAccent, typedValue, true);
                else
                    actionBar.getThemedContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
                break;
            }
            case android.R.attr.navigationBarColor: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    actionBar.getThemedContext().getTheme().resolveAttribute(android.R.attr.navigationBarColor, typedValue, true);
                else
                    actionBar.getThemedContext().getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
                break;
            }
            case android.R.attr.statusBarColor: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    actionBar.getThemedContext().getTheme().resolveAttribute(android.R.attr.statusBarColor, typedValue, true);
                else
                    actionBar.getThemedContext().getTheme().resolveAttribute(android.R.attr.colorForeground, typedValue, true);
                break;
            }
            case android.R.attr.textColorPrimary: {
                actionBar.getThemedContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
                break;
            }
            case android.R.attr.textColorSecondary: {
                actionBar.getThemedContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
                break;
            }
            case android.R.attr.colorBackground: {
                actionBar.getThemedContext().getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
                break;
            }
            case android.R.attr.colorForeground: {
                actionBar.getThemedContext().getTheme().resolveAttribute(android.R.attr.colorForeground, typedValue, true);
                break;
            }
            default: {
                if (Cobalt.DEBUG)
                    Log.e(TAG, "getAppThemeValue() return null value: int " + themeAttr + " not recognized.");
                return null;
            }
        }
        return TypedValueToHexColor(typedValue);
    }

    /**
     * TypedValueToHexColor (TypedValue typedValue)
     * Return a ARGB8 color
     *
     * @param typedValue Array holding a color that was originally specified as
     *                   #argb (TYPE_INT_COLOR_ARGB4),
     *                   #aarrggbb (TYPE_INT_COLOR_ARGB8),
     *                   #rgb (TYPE_INT_COLOR_RGB4),
     *                   #rrggbb (TYPE_INT_COLOR_RGB8).
     * @return string containing the associated format of color:
     */
    private String TypedValueToHexColor(TypedValue typedValue) {
        String color = null;
        switch (typedValue.type) {
            case TypedValue.TYPE_STRING: {
                // TypedValue is string type (old themes values)
                String hexColor = String.format("%08x", typedValue.data);
                color = hexColor.substring(2, 8) + hexColor.substring(0, 2);
                break;
            }
            case TypedValue.TYPE_INT_COLOR_RGB8:
            case TypedValue.TYPE_INT_COLOR_ARGB8: { // #ff669900 to #rrggbbaa
                // Coerce TypedValue to get a string code color like '#ff669900'
                String coercedColor = typedValue.coerceToString().toString();
                // Remove '#' and put transparency at the end for Cobalt.parseColor()
                String transparency = coercedColor.substring(1, 3);
                String red = coercedColor.substring(3, 5);
                String green = coercedColor.substring(5, 7);
                String blue = coercedColor.substring(7, 9);
                color = red + green + blue + transparency; // #rrggbbaa
                break;
            }
            case TypedValue.TYPE_INT_COLOR_ARGB4: { // #argb to #aarrggbb
                String coercedColor = typedValue.coerceToString().toString();
                // Remove '#' and put transparency at the end for Cobalt.parseColor()
                String transparency = coercedColor.substring(0, 1);
                String red = coercedColor.substring(1, 2);
                String green = coercedColor.substring(2, 3);
                String blue = coercedColor.substring(3, 4);
                transparency += transparency;
                red += red;
                green += green;
                blue += blue;
                color = red + green + blue + transparency; // #rrggbbaa
                break;
            }
            case TypedValue.TYPE_INT_COLOR_RGB4: { // #rgb to #rrggbbaa
                String coercedColor = typedValue.coerceToString().toString();
                // Remove '#' and put transparency at the end for Cobalt.parseColor()
                String red = coercedColor.substring(0, 1);
                String green = coercedColor.substring(1, 2);
                String blue = coercedColor.substring(2, 3);
                red += red;
                green += green;
                blue += blue;
                color = red + green + blue + "FF"; // #rrggbbaa
                break;
            }
            default:
                // if TypedValue type color is not known, method will return null
                if (Cobalt.DEBUG)
                    Log.d(TAG, "TypedValue type color not known: " + typedValue.type + ".");
        }
        return color;
    }
}
