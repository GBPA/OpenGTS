// ----------------------------------------------------------------------------
// Copyright 2007-2017, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Change History:
//  2006/02/19  Martin D. Flynn
//      - Initial release
//  2006/04/17  Martin D. Flynn
//      - Add additional keywords to "parseColor"
//  2008/12/01  Martin D. Flynn
//      - Added 'RGB' class
//  2010/01/29  Martin D. Flynn
//      - Added 'isColor' method
//  2016/05/09  Martin D. Flynn
//      - Added alpha/transparency support
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.color.*;

/**
*** Color handling and conversion tools
**/

public class ColorTools
{

    // ------------------------------------------------------------------------

    private static final int           DEFAULT_RED        = 0x00;
    private static final int           DEFAULT_GREEN      = 0x00;
    private static final int           DEFAULT_BLUE       = 0x00;
    private static final int           DEFAULT_ALPHA      = 0xFF;

    // ------------------------------------------------------------------------
    // http://htmlhelp.com/cgi-bin/color.cgi?rgb=FFFFFF

    public static final ColorTools.RGB BLACK              = new ColorTools.RGB(0x000000,false);
    public static final ColorTools.RGB BROWN              = new ColorTools.RGB(0xA52A2A,false);
    public static final ColorTools.RGB RED                = new ColorTools.RGB(0xDD0000,false);
    public static final ColorTools.RGB ORANGE             = new ColorTools.RGB(0xFFA500,false);
    public static final ColorTools.RGB YELLOW             = new ColorTools.RGB(0xFFD700,false);
    public static final ColorTools.RGB DARK_YELLOW        = new ColorTools.RGB(0xAA9700,false);
    public static final ColorTools.RGB GREEN              = new ColorTools.RGB(0x00CC00,false);
    public static final ColorTools.RGB BLUE               = new ColorTools.RGB(0x0000EE,false);
    public static final ColorTools.RGB PURPLE             = new ColorTools.RGB(0x9400D3,false);
    public static final ColorTools.RGB GRAY               = new ColorTools.RGB(0x808080,false);
    public static final ColorTools.RGB DARK_GRAY          = new ColorTools.RGB(0x505050,false);
    public static final ColorTools.RGB LIGHT_GRAY         = new ColorTools.RGB(0xC0C0C0,false);
    public static final ColorTools.RGB WHITE              = new ColorTools.RGB(0xFFFFFF,false);
    public static final ColorTools.RGB CYAN               = new ColorTools.RGB(0x00FFFF,false);
    public static final ColorTools.RGB PINK               = new ColorTools.RGB(0xFF1493,false);
    public static final ColorTools.RGB MAGENTA            = new ColorTools.RGB(0x8B008B,false);

    public static final ColorTools.RGB COLOR_BLACK        = BLACK;
    public static final ColorTools.RGB COLOR_BROWN        = BROWN;
    public static final ColorTools.RGB COLOR_RED          = RED;
    public static final ColorTools.RGB COLOR_ORANGE       = ORANGE;
    public static final ColorTools.RGB COLOR_YELLOW       = YELLOW;
    public static final ColorTools.RGB COLOR_DARK_YELLOW  = DARK_YELLOW;
    public static final ColorTools.RGB COLOR_GREEN        = GREEN;
    public static final ColorTools.RGB COLOR_BLUE         = BLUE;
    public static final ColorTools.RGB COLOR_PURPLE       = PURPLE;
    public static final ColorTools.RGB COLOR_GRAY         = GRAY;
    public static final ColorTools.RGB COLOR_DARK_GRAY    = DARK_GRAY;
    public static final ColorTools.RGB COLOR_LIGHT_GRAY   = LIGHT_GRAY;
    public static final ColorTools.RGB COLOR_WHITE        = WHITE;
    public static final ColorTools.RGB COLOR_CYAN         = CYAN;
    public static final ColorTools.RGB COLOR_PINK         = PINK;
    public static final ColorTools.RGB COLOR_MAGENTA      = MAGENTA;

    public static final Color          black              = BLACK.toColor();
    public static final Color          brown              = BROWN.toColor();
    public static final Color          red                = RED.toColor();
    public static final Color          orange             = ORANGE.toColor();
    public static final Color          yellow             = YELLOW.toColor();
    public static final Color          darkYellow         = DARK_YELLOW.toColor();
    public static final Color          green              = GREEN.toColor();
    public static final Color          blue               = BLUE.toColor();
    public static final Color          purple             = PURPLE.toColor();
    public static final Color          gray               = GRAY.toColor();
    public static final Color          darkGray           = DARK_GRAY.toColor();
    public static final Color          lightGray          = LIGHT_GRAY.toColor();
    public static final Color          white              = WHITE.toColor();
    public static final Color          cyan               = CYAN.toColor();
    public static final Color          pink               = PINK.toColor();
    public static final Color          magenta            = MAGENTA.toColor();

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Masks the specified 0..255 color value to 0xFF
    *** @param colorVal The 0..255 color value
    *** @return The masked color value
    **/
    private static int _mask(int colorVal)
    {
        return colorVal & 0xFF;
    }

    /**
    *** Converts the specified 0..255 color value to a float
    *** @param colorVal The 0..255 color value
    *** @return The float value
    **/
    private static float _toColorFloat(int colorVal)
    {
        return _bound((float)_mask(colorVal) / 255.0F);
    }

    /**
    *** Converts the specified 0..1 color value to an integer
    *** @param colorVal The 0..1 color value
    *** @return The int value
    **/
    private static int _toColorInt(float colorVal)
    {
        return _bound(Math.round(colorVal * 255.0F));
    }

    /**
    *** Converts the specified 0..1 color value to an integer
    *** @param colorVal The 0..1 color value
    *** @return The int value
    **/
    private static int _toColorInt(double colorVal)
    {
        return ColorTools._toColorInt((float)colorVal);
    }

    /**
    *** Performs bounds checking on the specified 0..255 color value
    *** @param colorVal  The color value to bounds check
    *** @return A color value that is guaranteed to be within 0..255
    **/
    private static int _bound(int colorVal)
    {
        if (colorVal <   0) { colorVal =   0; }
        if (colorVal > 255) { colorVal = 255; }
        return colorVal;
    }

    /**
    *** Performs bounds checking on the specified 0..1 color value
    *** @param colorVal  The color value to bounds check
    *** @return A color value that is guaranteed to be within 0..1 (inclusive)
    **/
    private static float _bound(float colorVal)
    {
        if (colorVal < 0.0F) { colorVal = 0.0F; }
        if (colorVal > 1.0F) { colorVal = 1.0F; }
        return colorVal;
    }

    /**
    *** Performs bounds checking on the specified 0..1 color value
    *** @param colorVal  The color value to bounds check
    *** @return A color value that is guaranteed to be within 0..1 (inclusive)
    **/
    private static double _bound(double colorVal)
    {
        if (colorVal < 0.0) { colorVal = 0.0; }
        if (colorVal > 1.0) { colorVal = 1.0; }
        return colorVal;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** RGB class.
    **/
    public static class RGB
    {
        private int R = DEFAULT_RED;    // 0..256 red
        private int G = DEFAULT_GREEN;  // 0..256 green
        private int B = DEFAULT_BLUE;   // 0..256 blue
        private int A = DEFAULT_ALPHA;  // 0..256 alpha

        // ----------------------------

        /**
        *** Private Constructor
        **/
        private RGB()
        {
            this.R = DEFAULT_RED;
            this.G = DEFAULT_GREEN;
            this.B = DEFAULT_BLUE;
            this.A = DEFAULT_ALPHA;
        }

        /**
        *** Constructor<br>
        *** Extracts Alpha, Red, Green, Blue, values from hex integer.
        *** @param ARGB       The hex encoded Alpha, Red, Green, Blue values
        *** @param inclAlpha  True to extract Alpha as well, false to assume opaque
        **/
        public RGB(int ARGB, boolean inclAlpha) {
            this();
            this.R = _mask(ARGB >> 16);
            this.G = _mask(ARGB >>  8);
            this.B = _mask(ARGB >>  0);
            this.A = inclAlpha? _mask(ARGB >> 24) : DEFAULT_ALPHA;
        }

        // ----------------------------

        /**
        *** Constructor<br>
        *** Initializes with specified Red, Green, Blue component integer values (0..255)
        *** @param r  The 0..255 Red component integer value
        *** @param g  The 0..255 Green component integer value
        *** @param b  The 0..255 Blue component integer value
        **/
        public RGB(int r, int g, int b) {
            this();
            this.R = _mask(r);
            this.G = _mask(g);
            this.B = _mask(b);
            this.A = DEFAULT_ALPHA;
        }

        /**
        *** Constructor<br>
        *** Initializes with specified Red, Green, Blue, and Alpha component integer values (0..255)
        *** @param r  The 0..255 Red component integer value
        *** @param g  The 0..255 Green component integer value
        *** @param b  The 0..255 Blue component integer value
        *** @param a  The 0..255 Alpha component integer value (255 == full opaque)
        **/
        public RGB(int r, int g, int b, int a) {
            this(r,g,b);
            this.A = _mask(a);
        }

        /**
        *** Constructor<br>
        *** Initializes with specified Red, Green, Blue, and Alpha component integer values (0..255)
        *** @param RGBA  The array containing the RGB, and optionally Alpha components.
        ***     If the array length is >= 3, the RGB components will be initialized. 
        ***     If the array length is >= 4, the Alpha component will also be initialized.
        ***     If the array has less than 3 elements, the RGB instance will be initialized to black opaque.
        **/
        public RGB(int RGBA[]) {
            this();
            if (ListTools.size(RGBA) >= 3) {
                this.R = _mask(RGBA[0]);
                this.G = _mask(RGBA[1]);
                this.B = _mask(RGBA[2]);
                if ((RGBA.length >= 4) && (RGBA[3] >= 0)) {
                    this.A = _mask(RGBA[3]);
                }
            }
        }

        // ----------------------------

        /* hex constructor */
        //public RGB(int RGB) {
        //    this.R = (RGB >> 16) & 0xFF;
        //    this.G = (RGB >>  8) & 0xFF;
        //    this.B = (RGB >>  0) & 0xFF;
        //}

        // ----------------------------

        /**
        *** Constructor<br>
        *** Initializes with specified Red, Green, Blue component float values (0..1)
        *** @param r  The 0..1 Red component float value
        *** @param g  The 0..1 Green component float value
        *** @param b  The 0..1 Blue component float value
        **/
        public RGB(float r, float g, float b) {
            this();
            this.R = _toColorInt(r);
            this.G = _toColorInt(g);
            this.B = _toColorInt(b);
        }

        /**
        *** Constructor<br>
        *** Initializes with specified Red, Green, Blue component float values (0..1)
        *** @param r  The 0..1 Red component float value
        *** @param g  The 0..1 Green component float value
        *** @param b  The 0..1 Blue component float value
        *** @param a  The 0..1 Alpha component float value (1 == full opaque)
        **/
        public RGB(float r, float g, float b, float a) {
            this(r,g,b);
            this.A = _toColorInt(a);
        }

        /**
        *** Constructor<br>
        *** Initializes with specified Red, Green, Blue, and Alpha component float values (0..1)
        *** @param RGBA  The array containing the RGB, and optionally Alpha components.
        ***     If the array length is >= 3, the RGB components will be initialized. 
        ***     If the array length is >= 4, the Alpha component will also be initialized.
        ***     If the array has less than 3 elements, the RGB instance will be initialized to black opaque.
        **/
        public RGB(float RGBA[]) {
            if (ListTools.size(RGBA) >= 3) {
                this.R = _toColorInt(RGBA[0]);
                this.G = _toColorInt(RGBA[1]);
                this.B = _toColorInt(RGBA[2]);
                if ((RGBA.length >= 4) && (RGBA[3] >= 0.0F)) {
                    this.A = _toColorInt(RGBA[3]);
                }
            }
        }

        // ----------------------------

        /**
        *** Constructor<br>
        *** Initializes with specified Red, Green, Blue component double values (0..1)
        *** @param r  The 0..1 Red component double value
        *** @param g  The 0..1 Green component double value
        *** @param b  The 0..1 Blue component double value
        **/
        public RGB(double r, double g, double b) {
            this.R = _toColorInt(r);
            this.G = _toColorInt(g);
            this.B = _toColorInt(b);
        }

        /**
        *** Constructor<br>
        *** Initializes with specified Red, Green, Blue component double values (0..1)
        *** @param r  The 0..1 Red component double value
        *** @param g  The 0..1 Green component double value
        *** @param b  The 0..1 Blue component double value
        *** @param a  The 0..1 Alpha component double value (1 == full opaque)
        **/
        public RGB(double r, double g, double b, double a) {
            this(r,g,b);
            this.A = _toColorInt(a);
        }

        /**
        *** Constructor<br>
        *** Initializes with specified Red, Green, Blue, and Alpha component double values (0..1)
        *** @param RGBA  The array containing the RGB, and optionally Alpha components.
        ***     If the array length is >= 3, the RGB components will be initialized. 
        ***     If the array length is >= 4, the Alpha component will also be initialized.
        ***     If the array has less than 3 elements, the RGB instance will be initialized to black opaque.
        **/
        public RGB(double RGBA[]) {
            if (ListTools.size(RGBA) >= 3) {
                this.R = _toColorInt(RGBA[0]);
                this.G = _toColorInt(RGBA[1]);
                this.B = _toColorInt(RGBA[2]);
                if ((RGBA.length >= 4) && (RGBA[3] >= 0.0)) {
                    this.A = _toColorInt(RGBA[3]);
                }
            }
        }

        // ----------------------------

        /**
        *** Gets the hex encoded integer Alpha, Red, Green, Blue components (ARGB)
        *** @return The hex encoded integer Alpha, Red, Green, Blue components
        **/
        public int getRGB() {
            int a = this.getAlpha();
            int r = this.getRed();
            int g = this.getGreen();
            int b = this.getBlue();
            return (a << 24) | (r << 16) | (g << 8) | (b << 0);
        }

        // ----------------------------

        /**
        *** Gets the 0..255 Red component value
        *** Return The 0..255 Red component value
        **/
        public int getRed() {
            return (this.R >= 0)? _mask(this.R) : DEFAULT_RED;
        }

        /**
        *** Gets the 0..255 Green component value
        *** Return The 0..255 Green component value
        **/
        public int getGreen() {
            return (this.G >= 0)? _mask(this.G) : DEFAULT_GREEN;
        }

        /**
        *** Gets the 0..255 Blue component value
        *** Return The 0..255 Blue component value
        **/
        public int getBlue() {
            return (this.B >= 0)? _mask(this.B) : DEFAULT_BLUE;
        }

        /**
        *** Gets the 0..255 Alpha component value
        *** Return The 0..255 Alpha component value
        **/
        public int getAlpha() {
            return (this.A >= 0)? _mask(this.A) : DEFAULT_ALPHA;
        }

        /**
        *** Returns true if this instance has an Alpha component other tha 0xFF (opaque)
        *** @return True if this instance has an Alpha component other tha 0xFF (opaque)
        **/
        public boolean hasAlpha() {
            return (this.getAlpha() != DEFAULT_ALPHA)? true : false;
        }

        // ----------------------------

        /**
        *** Gets an array of the Color (RGB) components (no Alpha)
        *** @return An array of the Color components (no Alpha)
        **/
        public float[] getRGBColorComponents() {
            return new float[] { 
                _toColorFloat(this.getRed()), 
                _toColorFloat(this.getGreen()), 
                _toColorFloat(this.getBlue())
            };
        }

        /**
        *** Gets an array of the components (RGBA) (including Alpha)
        *** @return An array of the Color components (no Alpha)
        **/
        public float[] getRGBComponents() {
            return new float[] { 
                _toColorFloat(this.getRed()), 
                _toColorFloat(this.getGreen()), 
                _toColorFloat(this.getBlue()),
                _toColorFloat(this.getAlpha())
            };
        }

        // ----------------------------

        /**
        *** Returns a lightened version of this instance<br>
        *** (does not effect the current Alpha value)
        *** @param percent  The amount of lightening to apply to this instance.
        **/
        public RGB lighter(double percent) {
            float p = _bound((float)percent);
            float C[] = this.getRGBComponents(); // length == 4
            for (int i = 0; i < 3; i++) { // RGB only (alpha left as-is)
                C[i] = _bound(C[i] + ((1.0F - C[i]) * p)); // RGB modified in-place
            }
            return new RGB(C);
        }

        /**
        *** Returns a darkened version of this instance<br>
        *** (does not effect the current Alpha value)
        *** @param percent  The amount of darkening to apply to this instance.
        **/
        public RGB darker(double percent) {
            float p = _bound((float)percent);
            float C[] = this.getRGBComponents(); // length == 4
            for (int i = 0; i < 3; i++) { // RGB only (alpha left as-is)
                C[i] = _bound(C[i] - (C[i] * p)); // RGB modified in-place
            }
            return new RGB(C);
        }

        // ----------------------------

        /**
        *** Mixes the specified instance with this instance
        *** @param color2    The other RGB instance to mix with this instance
        *** @param weight    The amount of the specified instance to mix with this instance
        *** @param mixAlpha  True to include Alpha in mix, false to retain the 
        ***     Alpha value of this instance.
        **/
        public RGB mixWith(RGB color2, float weight, boolean mixAlpha) {
            RGB color1 = this;
            if (color2 == null) {
                return color1;
            } else {
                float rgb1[] = mixAlpha? color1.getRGBComponents() : color1.getRGBColorComponents();
                float rgb2[] = mixAlpha? color2.getRGBComponents() : color2.getRGBColorComponents();
                float rgba[] = new float[rgb1.length]; // RGBA (length == 3/4)
                for (int i = 0; i < rgba.length; i++) {
                    rgba[i] = _bound(rgb1[i] + ((rgb2[i] - rgb1[i]) * weight));
                }
                if (rgba.length == 4) { // RGBA
                    float a = rgba[3];
                    return new RGB(rgba[0], rgba[1], rgba[2], a);
                } else
                if (rgba.length == 3) { // RGB
                    float a = _toColorFloat(color1.getAlpha());
                    return new RGB(rgba[0], rgba[1], rgba[2], a);
                } else {
                    return color1; // unlikely
                }
            }
        }

        /**
        *** Mixes the specified instance with this instance.<br>
        *** (does not mix Alpha)
        *** @param color      The other RGB instance to mix with this instance
        *** @param weight     The amount of the specified instance to mix with this instance
        **/
        public RGB mixWith(RGB color, float weight) {
            return this.mixWith(color, weight, false);
        }

        /**
        *** Mixes the specified instance with this instance
        *** @param color     The other RGB instance to mix with this instance
        *** @param weight    The amount of the specified instance to mix with this instance
        *** @param mixAlpha  True to include Alpha in mix, false to retain the 
        ***     Alpha value of this instance.
        **/
        public RGB mixWith(RGB color, double weight, boolean mixAlpha) {
            return this.mixWith(color, (float)weight, mixAlpha);
        }

        /**
        *** Mixes the specified instance with this instance.<br>
        *** (does not mix Alpha)
        *** @param color    The other RGB instance to mix with this instance
        *** @param weight   The amount of the specified instance to mix with this instance
        **/
        public RGB mixWith(RGB color, double weight) {
            return this.mixWith(color, (float)weight, false);
        }

        // ----------------------------

        /**
        *** Returns a Color instance based on the RGBA values from this instance
        *** @return A Color instance based on this instance
        **/
        public Color toColor() {
            int r = this.getRed();
            int g = this.getGreen();
            int b = this.getBlue();
            int a = this.getAlpha();
            return new Color(r,g,b,a);
        }

        // ----------------------------

        /**
        *** Returns a Hex String representation of this instance.
        *** If this instance contains an Alpha component other than 0xFF (opaque), 
        *** then the returned String format will be "#AARRGGBB", otherwise the 
        *** returned String format will be just "#RRGGBB".
        *** @param pfxHash  True to prefix the Hex string with "#"
        *** @return A Hex String representation of this instance.
        **/
        public String toString(boolean pfxHash) {
            String h = Integer.toHexString(this.getRGB()).toUpperCase(); // AARRGGBB
            if (this.hasAlpha()) {
                return pfxHash? ("#" + h) : h;
            } else {
                String v = h.substring(2); // remove alpha
                return pfxHash? ("#" + v) : v;
            }
        }

        /**
        *** Returns a Hex String representation of this instance (without prefixing "#")
        *** @return A Hex String representation of this instance.
        **/
        public String toString() {
            return this.toString(true);
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Parses the Color String representation into an RGB instance.
    *** @param color  The color String representation to parse
    *** @param dft    The default Color returned if unable to parse a Color from the String
    *** @return The parsed Color instance.
    **/
    public static RGB parseColor(String color, RGB dft)
    {

        /* default */
        if (StringTools.isBlank(color)) {
            // -- return default as-is
            return dft;
        }

        /* check for +/- percent */
        double percent = 0.0;
        int p = color.indexOf("-");
        if (p >= 0) {
            // -- darken (ie. "red-30")
            int pct = StringTools.parseInt(color.substring(p+1),0);
            percent = -(double)pct / 100.0; // negative
            color   = color.substring(0,p);
        } else {
            p = color.indexOf("+");
            if (p >= 0) {
                // -- lighten (ie. "white+25")
                int pct = StringTools.parseInt(color.substring(p+1),0);
                percent = (double)pct / 100.0; // positive
                color   = color.substring(0,p);
            }
        }

        /* get color */
        RGB rtn;
        if (StringTools.isBlank(color)) {
            rtn = dft;
        } else
        if (color.equalsIgnoreCase("black")) {
            rtn = BLACK;
        } else
        if (color.equalsIgnoreCase("blue")) {
            rtn = BLUE;
        } else
        if (color.equalsIgnoreCase("cyan")) {
            rtn = CYAN;
        } else
        if (color.equalsIgnoreCase("darkGray")) {
            rtn = DARK_GRAY;
        } else
        if (color.equalsIgnoreCase("gray")) {
            rtn = GRAY;
        } else
        if (color.equalsIgnoreCase("green")) {
            rtn = GREEN;
        } else
        if (color.equalsIgnoreCase("lightGray")) {
            rtn = LIGHT_GRAY;
        } else
        if (color.equalsIgnoreCase("magenta")) {
            rtn = MAGENTA;
        } else
        if (color.equalsIgnoreCase("orange")) {
            rtn = ORANGE;
        } else
        if (color.equalsIgnoreCase("pink")) {
            rtn = PINK;
        } else
        if (color.equalsIgnoreCase("red")) {
            rtn = RED;
        } else
        if (color.equalsIgnoreCase("white")) {
            rtn = WHITE;
        } else
        if (color.equalsIgnoreCase("yellow")) {
            rtn = YELLOW;
        } else {
            // -- HTML/CSS format augmented to support alpha as well: #AARRGGBB
            String c = color.startsWith("#")? color.substring(1).trim() : color;
            if (StringTools.isHex(c,true)) {
                long argb = StringTools.parseHexLong(c, -1L);
                if (argb >= 0L) {
                    boolean inclAlpha = (c.length() > 6)? true : false;
                    rtn = new RGB((int)argb, inclAlpha);
                } else {
                    // -- unable to parse (unlikely, since we know all are hex chars)
                    rtn = dft; 
                    percent = 0.0;
                }
            } else {
                // -- not all hex, unable to parse 
                rtn = dft; 
                percent = 0.0;
            }
        }

        /* lighten/darken */
        if (rtn != null) {
            if (percent == 0.0) {
                // -- leave as-is
            } else
            if (percent >= 0.0) {
                // -- +percent: lighten
                rtn = rtn.lighter(Math.abs(percent));
            } else {
                // -- -percent: darken
                rtn = rtn.darker(Math.abs(percent));
            }
        }

        /* return resulting color */
        return rtn;

    }

    /**
    *** Parses the Color String representation into a Color instance.
    *** @param color  The color String representation to parse
    *** @param dft    The default Color returned if unable to parse a Color from the String
    *** @return The parsed Color instance.
    **/
    public static Color parseColor(String color, Color dft)
    {
        RGB rgb = ColorTools.parseColor(color, (RGB)null);
        return (rgb != null)? rgb.toColor() : dft;
    }

    /**
    *** Returns true if the specified string is a valid color
    *** @param color  The color String representation to test
    *** @return True if the specified string is a valid color
    **/
    public static boolean isColor(String color)
    {
        return (ColorTools.parseColor(color,(RGB)null) != null);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Creates a new Color that is lighter than the specified Color
    *** @param color   The Color to make lighter
    *** @param percent The percent to make lighter (does not effect alpha component)
    *** @return The new 'ligher' Color
    **/
    public static Color lighter(Color color, float percent)
    {
        if (color != null) {
            float rgb[];
            { // local scope
                float pct  = _bound(percent);
                float cc[] = color.getColorComponents(null); // Color only
                rgb = new float[cc.length];
                for (int i = 0; i < rgb.length; i++) {
                    rgb[i] = _bound(cc[i] + ((1.0F - cc[i]) * pct));
                }
            }
            ColorSpace cs = color.getColorSpace();
            return new Color(cs, rgb, _toColorFloat(color.getAlpha()));
        } else {
            return null;
        }
    }

    /**
    *** Creates a new Color that is lighter than the specified Color
    *** @param color   The Color to make lighter
    *** @param percent The percent to make lighter
    *** @return The new 'ligher' Color
    **/
    public static Color lighter(Color color, double percent)
    {
        return ColorTools.lighter(color, (float)percent);
    }

    // --------------------------------

    /**
    *** Creates a new Color that is darker than the specified Color
    *** @param color   The Color to make darker (does not effect alpha component)
    *** @param percent The percent to make darker
    *** @return The new 'darker' Color
    **/
    public static Color darker(Color color, float percent)
    {
        if (color != null) {
            float rgb[];
            { // local scope
                float pct  = _bound(percent);
                float cc[] = color.getColorComponents(null); // Color only
                rgb = new float[cc.length];
                for (int i = 0; i < rgb.length; i++) {
                    rgb[i] = _bound(cc[i] - (cc[i] * pct));
                }
            }
            ColorSpace cs = color.getColorSpace();
            return new Color(cs, rgb, _toColorFloat(color.getAlpha()));
        } else {
            return null;
        }
    }

    /**
    *** Creates a new Color that is darker than the specified Color
    *** @param color   The Color to make darker
    *** @param percent The percent to make darker
    *** @return The new 'darker' Color
    **/
    public static Color darker(Color color, double percent)
    {
        return ColorTools.darker(color, (float)percent);
    }

    // --------------------------------

    /**
    *** Creates a new Color with the specified transparency/alpha component
    *** @param color  The color to make transparent
    *** @param alpha  Tha percentage of transparency
    **/
    public static Color transparent(Color color, float alpha)
    {
        if (color != null) {
            float cc[] = color.getColorComponents(null); // Color only
            ColorSpace cs = color.getColorSpace();
            return new Color(cs, cc, _bound(alpha));
        } else {
            return null;
        }
    }

    /**
    *** Creates a new Color with the specified transparency/alpha component
    *** @param color  The color to make transparent
    *** @param alpha  Tha percentage of transparency
    **/
    public static Color transparent(Color color, double alpha)
    {
        return ColorTools.transparent(color, (float)alpha);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Mixes the second specified Color with the first specified Color, 
    *** based on the specified weight.
    *** (does not effect alpha components)
    *** @param color1  The starting Color 
    *** @param color2  The Color which will mixed with the starting Color
    *** @param weight  The amount of the second Color mixed with the first Color
    ***                A 'weight' of '0.0' will produce the first Color.  
    ***                A 'weight' of '1.0' will produce the second Color.
    *** @return The new mixed Color
    **/
    public static Color mix(Color color1, Color color2, float weight)
    {
        boolean mixAlpha = false;
        if (color1 == null) {
            return color2;
        } else
        if (color2 == null) {
            return color1;
        } else {
            float rgb1[] = mixAlpha? color1.getRGBComponents(null) : color1.getRGBColorComponents(null);
            float rgb2[] = mixAlpha? color2.getRGBComponents(null) : color2.getRGBColorComponents(null);
            float rgba[] = new float[rgb1.length]; // length == 3/4
            for (int i = 0; i < rgba.length; i++) {
                rgba[i] = _bound(rgb1[i] + ((rgb2[i] - rgb1[i]) * weight));
            }
            if (rgba.length == 4) {
                float A = rgba[3];
                return new Color(rgba[0], rgba[1], rgba[2], A);
            } else
            if (rgba.length == 3) {
                float A = _toColorFloat(color1.getAlpha());
                return new Color(rgba[0], rgba[1], rgba[2], A);
            } else {
                return color1; // unlikely
            }
        }
    }

    /**
    *** Mixes the second specified Color with the first specified Color, 
    *** based on the specified weight.
    *** (does not effect alpha components)
    *** @param color1  The starting Color 
    *** @param color2  The Color which will mixed with the starting Color
    *** @param weight  The amount of the second Color mixed with the first Color
    ***                A 'weight' of '0.0' will produce the first Color.  
    ***                A 'weight' of '1.0' will produce the second Color.
    *** @return The new mixed Color
    **/
    public static Color mix(Color color1, Color color2, double weight)
    {
        return ColorTools.mix(color1, color2, (float)weight);
    }

    // ------------------------------------------------------------------------

    /** 
    *** Converts an integer RGBA format to ARGB
    *** @param RGBA  The RGBA formated integer
    *** @return The ARGB formatted integer
    **/
    public static int RGBAtoARGB(int RGBA)
    {
        return ((RGBA >> 8) & 0x00FFFFFF) | ((RGBA << 24) & 0xFF000000);
    }

    /**
    *** Converts an integer ARGB format to RGBA
    *** @param ARGB  The ARGB formated integer
    *** @return The RGBA formatted integer
    **/
    public static int ARGBtoRGBA(int ARGB)
    {
        return ((ARGB << 8) & 0xFFFFFF00) | ((ARGB >> 24) & 0x000000FF);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a random color
    *** @param R  The radomizer (null to create a new Random instance)
    *** @return The generated random color
    **/
    public static Color randomColor(Random R)
    {
        Random r = (R != null)? R : new Random();
        return new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
    }

    /**
    *** Returns a random color
    *** @return The generated random color
    **/
    public static Color randomColor()
    {
        return ColorTools.randomColor(null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a hex String representation of the specified Color.
    *** @param color   The Color to convert to a String representation
    *** @param pfxHash Prefix with '#' if true
    *** @return The Color String representation
    **/
    public static String toHexString(Color color, boolean pfxHash)
    {
        if (color != null) {
            String v = Integer.toHexString(color.getRGB()).substring(2);
            return pfxHash? ("#" + v) : v;
        } else {
            return "";
        }
    }

    /**
    *** Returns a hex String representation of the specified Color.
    *** @param color  The Color to convert to a String representation
    *** @return The Color String representation
    **/
    public static String toHexString(Color color)
    {
        return ColorTools.toHexString(color, true);
    }

    // --------------------------------

    /**
    *** Returns a hex String representation of the specified RGB instance.
    *** @param color   The RGB instance to convert to a String representation
    *** @param pfxHash Prefix with '#' if true
    *** @return The Color String representation
    **/
    public static String toHexString(RGB color, boolean pfxHash)
    {
        if (color != null) {
            return color.toString(pfxHash);
        } else {
            return "";
        }
    }

    /**
    *** Returns a hex String representation of the specified RGB instance.
    *** @param color  The RGB instance to convert to a String representation
    *** @return The Color String representation
    **/
    public static String toHexString(RGB color)
    {
        return ColorTools.toHexString(color, true);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
