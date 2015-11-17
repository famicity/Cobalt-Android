/**
 *
 * CobaltFontManager
 * Cobalt
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Cobaltians
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package fr.cobaltians.cobalt.font;

import android.content.Context;

import java.util.HashMap;

/**
 * Singleton allowing to
 */
public class CobaltFontManager {

    /**
     * the fonts key in cobalt.conf
     */
    private static final String kFonts = "fonts";

    /**
     * Initializes and returns a font drawable with a font icon identifier, color, text size and padding
     * @param context the activity context
     * @param identifier the font icon identifier as "font-key font-icon" (i.e.: fa fa-mobile)
     * @param color the text color as a color-int
     * @param textSize the text size in sp
     * @param padding the padding in dp
     * @return a Drawable or null
     */
    // TODO: hint, use getFonts method below and see FrontFamicity.getIdResourceFont() method
    public static CobaltAbstractFontDrawable getCobaltFontDrawable(Context context, String identifier, int color, float textSize, float padding) {
        return null;
    }

    /**
     * Returns font key:class tuples as HashMap
     * @return font key:class tuples as HashMap
     */
    // TODO: hint, see Cobalt.getPlugins() method
    private static HashMap<String, Class<? extends CobaltAbstractFontDrawable>> getFonts() {
        return null;
    }
}
