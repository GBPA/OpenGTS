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
//  2008/07/27  Martin D. Flynn
//     -Initial release
//  2009/10/02  Martin D. Flynn
//     -Added "TextIcon" to allow combining a pushpin with embedded text.
//  2009/11/10  Martin D. Flynn
//     -Added PushpinChooser support
//  2009/12/16  Martin D. Flynn
//     -Added separate error message for TextIcon text/font errors
//  2011/01/28  Martin D. Flynn
//     -Added additional arrow formatting
//  2011/05/13  Martin D. Flynn
//     -Fixed "evHeadingMarkerURL_eu" conversion of kilometers to miles
//  2014/01/01  Martin D. Flynn
//     -Added "LoadImageIcon(...)"
//     -Perform additional error checking for images failing to load.
//  2015/05/11  Martin D. Flynn
//     -Added multi-line support to TextIcon
//     -Added meta-tags for changing font size/color in TextIcon
//  2016/12/21  Martin D. Flynn
//     -Moved "writePushpinChooserJS" to "PushpinChooser.java"
//     -Repackaged to "org.opengts.db"
//     -Added support for "LoadFromDirectory" [2.6.4-B77]
//     -Changed "LoadFromDirectory" to specify hotspot in filename [2.6.4-B82]
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;
import java.io.*;
import java.net.URL;

import java.awt.Font;
import java.awt.Color;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Ellipse2D;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import javax.swing.ImageIcon;
import javax.imageio.ImageIO;

//import javax.servlet.*;
//import javax.servlet.http.HttpServletRequest;

import org.opengts.util.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class PushpinIcon
{

    // ------------------------------------------------------------------------
    // References:
    // Google marker/pushpins:
    //   http://gmapicons.googlepages.com/home
    //      - http://www.google.com/mapfiles/marker.png
    //      - http://www.google.com/mapfiles/dd-start.png
    //      - http://www.google.com/mapfiles/dd-end.png
    //      - http://www.google.com/mapfiles/marker[A..Z].png
    //      - http://www.google.com/mapfiles/shadow50.png
    //      - http://maps.google.com/mapfiles/arrow.png
    //      - http://maps.google.com/mapfiles/arrowshadow.png
    // Other possible images:
    //   http://www.greendmedia.com/img_project/googleMapIcons/
    //   http://www1.arcwebservices.com/v2006/help/index_Left.htm#StartTopic=support/arcwebicons.htm#|SkinName=ArcWeb
    //   http://www.gpsdrive.de/development/map-icons/overview.en.shtml
    //   http://mapki.com/index.php?title=Icon_Image_Sets
    // Create your own map marker/pushpins:
    //   http://www.gmaplive.com/marker_maker.php
    //   http://www.cartosoft.com/mapicons
    //   http://gmaps-utility-library.googlecode.com/svn/trunk/mapiconmaker/1.0/examples/markericonoptions-wizard.html
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Borrowed from Google Maps.  Change these to your own desired push-pins
    // (The dimension of these images is 12x20)

    private static final String G_PUSHPIN_URL           = GoogleKML.GooglePushpinBase();

    private static final String G_PUSHPIN_BLACK         = G_PUSHPIN_URL + "mm_20_black.png";
    private static final String G_PUSHPIN_BROWN         = G_PUSHPIN_URL + "mm_20_brown.png";
    private static final String G_PUSHPIN_RED           = G_PUSHPIN_URL + "mm_20_red.png";
    private static final String G_PUSHPIN_ORANGE        = G_PUSHPIN_URL + "mm_20_orange.png";
    private static final String G_PUSHPIN_YELLOW        = G_PUSHPIN_URL + "mm_20_yellow.png";
    private static final String G_PUSHPIN_GREEN         = G_PUSHPIN_URL + "mm_20_green.png";
    private static final String G_PUSHPIN_BLUE          = G_PUSHPIN_URL + "mm_20_blue.png";
    private static final String G_PUSHPIN_PURPLE        = G_PUSHPIN_URL + "mm_20_purple.png";
    private static final String G_PUSHPIN_GRAY          = G_PUSHPIN_URL + "mm_20_gray.png";
    private static final String G_PUSHPIN_WHITE         = G_PUSHPIN_URL + "mm_20_white.png";

    private static final int    G_ICON_SIZE[]           = new int[] { 12, 20 };
    private static final int    G_ICON_OFFSET[]         = new int[] {  6, 20 };

    private static final String G_PUSHPIN_SHADOW        = G_PUSHPIN_URL + "mm_20_shadow.png";

    private static final int    G_SHADOW_SIZE[]         = new int[] { 22, 20 };

    // ------------------------------------------------------------------------

    public  static       String DEFAULT_TEXT_FONT       = "SanSerif";
    public  static       double DEFAULT_FONT_SIZE       = 10.0;

    // ------------------------------------------------------------------------
    // -- Default icon images included with OpenGTS
    // -  (created using "Marker Maker" once located at "http://www.gmaplive.com/marker_maker.php")

    private static final String PUSHPIN_URL             = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/";

    private static final String PUSHPIN_BLACK           = PUSHPIN_URL + "pin30_black.png";
    private static final String PUSHPIN_BROWN           = PUSHPIN_URL + "pin30_brown.png";
    private static final String PUSHPIN_RED             = PUSHPIN_URL + "pin30_red.png";
    private static final String PUSHPIN_ORANGE          = PUSHPIN_URL + "pin30_orange.png";
    private static final String PUSHPIN_YELLOW          = PUSHPIN_URL + "pin30_yellow.png";
    private static final String PUSHPIN_GREEN           = PUSHPIN_URL + "pin30_green.png";
    private static final String PUSHPIN_BLUE            = PUSHPIN_URL + "pin30_blue.png";
    private static final String PUSHPIN_PURPLE          = PUSHPIN_URL + "pin30_purple.png";
    private static final String PUSHPIN_GRAY            = PUSHPIN_URL + "pin30_gray.png";
    private static final String PUSHPIN_WHITE           = PUSHPIN_URL + "pin30_white.png";

    private static final int    ICON_SIZE[]             = new int[] { 18, 30 };
    private static final int    ICON_OFFSET[]           = new int[] {  9, 30 };

    private static final String PUSHPIN_SHADOW          = PUSHPIN_URL + "pin30_shadow.png";

    private static final int    SHADOW_SIZE[]           = new int[] { 30, 30 };

    // ------------------------------------------------------------------------
    // -- Color pushpin indices */
    // -- This represents the standard color "position" for the first 10 pushpin icons
    // -  The ordering is established in the method "PushpinIcon._DefaultPushpinIconMap()"

    public  static final int    ICON_PUSHPIN_BLACK      = 0;
    public  static final int    ICON_PUSHPIN_BROWN      = 1;
    public  static final int    ICON_PUSHPIN_RED        = 2;
    public  static final int    ICON_PUSHPIN_ORANGE     = 3;
    public  static final int    ICON_PUSHPIN_YELLOW     = 4;
    public  static final int    ICON_PUSHPIN_GREEN      = 5;
    public  static final int    ICON_PUSHPIN_BLUE       = 6;
    public  static final int    ICON_PUSHPIN_PURPLE     = 7;
    public  static final int    ICON_PUSHPIN_GRAY       = 8;
    public  static final int    ICON_PUSHPIN_WHITE      = 9;
    public  static final int    ICON_PUSHPIN_MAXVALUE   = ICON_PUSHPIN_WHITE;

    // ------------------------------------------------------------------------

    private static          Object                         DefaultPushpinIconMap_lock = new Object();
    private static volatile OrderedMap<String,PushpinIcon> DefaultPushpinIconMap = null;

    /**
    *** Returns the default PushpinIcon map
    *** @return The default PushpinIcon map
    **/
    private static OrderedMap<String,PushpinIcon> _DefaultPushpinIconMap()
    {
        if (DefaultPushpinIconMap == null) {
            synchronized (DefaultPushpinIconMap_lock) {
                if (DefaultPushpinIconMap == null) {
                    OrderedMap<String,PushpinIcon> map = new OrderedMap<String,PushpinIcon>();
                    map.put("black" ,new PushpinIcon("black" ,null,PUSHPIN_BLACK ,ICON_SIZE,ICON_OFFSET,PUSHPIN_SHADOW,SHADOW_SIZE));
                    map.put("brown" ,new PushpinIcon("brown" ,null,PUSHPIN_BROWN ,ICON_SIZE,ICON_OFFSET,PUSHPIN_SHADOW,SHADOW_SIZE));
                    map.put("red"   ,new PushpinIcon("red"   ,null,PUSHPIN_RED   ,ICON_SIZE,ICON_OFFSET,PUSHPIN_SHADOW,SHADOW_SIZE));
                    map.put("orange",new PushpinIcon("orange",null,PUSHPIN_ORANGE,ICON_SIZE,ICON_OFFSET,PUSHPIN_SHADOW,SHADOW_SIZE));
                    map.put("yellow",new PushpinIcon("yellow",null,PUSHPIN_YELLOW,ICON_SIZE,ICON_OFFSET,PUSHPIN_SHADOW,SHADOW_SIZE));
                    map.put("green" ,new PushpinIcon("green" ,null,PUSHPIN_GREEN ,ICON_SIZE,ICON_OFFSET,PUSHPIN_SHADOW,SHADOW_SIZE));
                    map.put("blue"  ,new PushpinIcon("blue"  ,null,PUSHPIN_BLUE  ,ICON_SIZE,ICON_OFFSET,PUSHPIN_SHADOW,SHADOW_SIZE));
                    map.put("purple",new PushpinIcon("purple",null,PUSHPIN_PURPLE,ICON_SIZE,ICON_OFFSET,PUSHPIN_SHADOW,SHADOW_SIZE));
                    map.put("gray"  ,new PushpinIcon("gray"  ,null,PUSHPIN_GRAY  ,ICON_SIZE,ICON_OFFSET,PUSHPIN_SHADOW,SHADOW_SIZE));
                    map.put("white" ,new PushpinIcon("white" ,null,PUSHPIN_WHITE ,ICON_SIZE,ICON_OFFSET,PUSHPIN_SHADOW,SHADOW_SIZE));
                    DefaultPushpinIconMap = map;
                }
            }
        }
        return DefaultPushpinIconMap;
    }

    /**
    *** Returns a shalow copy of the PushpinIcon map
    *** @return The PushpinIcon map
    **/
    public static OrderedMap<String,PushpinIcon> newDefaultPushpinIconMap()
    {
        return new OrderedMap<String,PushpinIcon>(_DefaultPushpinIconMap()); // shallow copy
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final boolean IMAGEICON_DIRECT_LOAD = false;

    /**
    *** Load ImageIcon from File
    **/
    public static ImageIcon LoadImageIcon(File file)
    {
        // -- ImageIcon methods:
        // -    getIconWidth()
        // -    getIconHeight()
        try {
            if (file == null) {
                Print.logWarn("Specified File is null");
                return null;
            } else
            if (!file.isFile()) {
                Print.logWarn("File does not exist: " + file);
                return null;
            } else {
                ImageIcon icon = new ImageIcon(file.toString());
                if (icon.getImageLoadStatus() == MediaTracker.ERRORED) {
                    Print.logWarn("Error loading ImageIcon");
                }
                return icon;
            }
        } catch (Throwable th) { // X11 Window error
            // ... "java.lang.NoClassDefFoundError: Could not initialize class sun.awt.X11.XToolkit"
            Print.logWarn("Unable to create ImageIcon: " + th);
            return null;
        }
    }

    /**
    *** Load ImageIcon from URL
    **/
    public static ImageIcon LoadImageIcon(URL url)
    {
        try {
            if (url == null) {
                Print.logWarn("Specified URL is null");
                return null;
            } else
            if (IMAGEICON_DIRECT_LOAD) {
                //Print.logInfo("Loading ImageIcon from URL");
                ImageIcon icon = new ImageIcon(url);
                if (icon.getImageLoadStatus() == MediaTracker.ERRORED) {
                    Print.logWarn("Error loading ImageIcon");
                }
                return icon;
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream input = null;
                ImageIcon icon = null;
                try {
                    input = url.openStream();
                    FileTools.copyStreams(input, baos);
                    baos.close(); // no-op
                    byte b[] = baos.toByteArray();
                    //Print.logInfo("Loading ImageIcon from bytes (len="+ListTools.size(b)+")");
                    if (!ListTools.isEmpty(b)) {
                        icon = new ImageIcon(baos.toByteArray());
                        if (icon.getImageLoadStatus() == MediaTracker.ERRORED) {
                            Print.logWarn("Error loading ImageIcon");
                        }
                    } else {
                        Print.logWarn("ImageIcon file is empty/null");
                    }
                } catch (IOException ioe) {
                    Print.logException("Error reading URL: " + url, ioe);
                } finally {
                    if (input != null) { try{input.close();}catch(Throwable err){/*ignore*/} }
                }
                return icon;
            }
        } catch (Throwable th) { // X11 Window error
            // ... "java.lang.NoClassDefFoundError: Could not initialize class sun.awt.X11.XToolkit"
            Print.logException("Unable to create ImageIcon", th);
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String MARKER_ARG_ICON[]         = { "icon"   , "ic", "file" };
    public static final String MARKER_ARG_FRAME[]        = { "frame"  , "fr"         };
    public static final String MARKER_ARG_FILL_COLOR[]   = { "fill"   , "bg", "fc"   };
    public static final String MARKER_ARG_BORDER_COLOR[] = { "border" , "bc"         };
    public static final String MARKER_ARG_COLOR[]        = { "color"  , "fg"         };
    public static final String MARKER_ARG_TEXT[]         = { "text"   , "tx"         };
    public static final String MARKER_ARG_ARROW[]        = { "arrow"  , "ar"         };
  //public static final String MARKER_ARG_PUSHPIN[]      = { "pushpin", "pp", "pin"  }; // might not be used

    private static File ResolveFile(String filePath)
    {

        /* no file? */
        if (StringTools.isBlank(filePath)) {
            return new File("");
        }

        /* already absolute file */
        if (filePath.startsWith("/")) {
            return new File(filePath);
        }

        /* relative to local context path */
        String ctxPathS = RTConfig.getContextPath();
        if (StringTools.isBlank(ctxPathS)) {
            // -- no context path, return relative file as-is
            return new File(filePath);
        }
        File ctxPath = new File(ctxPathS);
        File ctxPathFile = new File(ctxPath, filePath);

        /* WebApp/Servlet? */
        if (RTConfig.isTrueServlet()) {
            // -- return filePath relative to context-path
            return ctxPathFile;
        }

        /* Command-Line? */
        if (RTConfig.isCommandLine()) {
            // -- context-path is likely the same as $GTS_HOME
            File trackDir = new File(ctxPath, "war/track");
            File fileDirPath = new File(trackDir, filePath);
            if (fileDirPath.isFile()) {
                // -- found image at "$GTS_HOME/war/track/..."
                return fileDirPath;
            }
        }

        /* check filePath relative to context path */
        if (ctxPathFile.isFile()) {
            // -- file happens to be present relative to $GTS_HOME
            return ctxPathFile;
        }

        /* no idea where file is located, return as-is */
        return new File(filePath);

    }

    public static final TextIcon TextIconList[] = {
        new TextIcon("White_47x1"   ,
            "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/label47_fill.png",
            3, 2, 42, 13,   // Color frame
            3, 2, 42, 13,   // Text frame
            11, null),      // Font size, Font name
        new TextIcon("White_67x1"   ,
            "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/label67_fill.png"  ,
            3, 2, 62, 13,   // Color frame
            3, 2, 62, 13,   // Text frame
            11, null),      // Font size, Font name
        new TextIcon("White_67x2"   ,
            "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/label67x2_fill.png",
            2, 2, 62, 26,   // Color frame
            2, 2, 62, 26,   // Text frame
            11, null),      // Font size, Font name
        new TextIcon("White_67x2_pp",
            "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/label67x2_fill.png",
            2, 2, 62, 26,   // Color frame
            5, 2, 59, 26,   // Text frame (room for PushPin)
            11, null),      // Font size, Font name
        new TextIcon("White_67x3"   ,
            "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/label67x3_fill.png",
            2, 2, 62, 38,   // Color frame
            2, 2, 62, 38,   // Text frame
            11, null),      // Font size, Font name
        new TextIcon("White_67x3_pp",
            "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/label67x3_fill.png",
            2, 2, 62, 38,   // Color frame
            5, 2, 59, 38,   // Text frame (room for PushPin)
            11, null),      // Font size, Font name
    };

    public static TextIcon GetTextIcon(String name)
    {
        if (!StringTools.isBlank(name)) {
            for (TextIcon ti : TextIconList) {
                if (name.equalsIgnoreCase(ti.getName())) {
                    // -- load the ImageIcon now
                    ti.getImageIcon();
                    // -- return TextIcon
                    return ti;
                }
            }
        }
        Print.logWarn("TextIcon not found: " + name);
        return null;
    }

    public static URL ToURL(File file)
    {
        try {
            return FileTools.toURL(file);
        } catch (java.net.MalformedURLException mue) {
            Print.logError("Invalid file specification: " + file);
            return null;
        }
    }

    private static final String META_START = "%{"; // %{+1,red}
    private static final String META_END   = "}";
    public static class TextIcon
    {
        // -- name
        private String      name            = "";
        // -- color frame
        private int         frameOffs_x     = 0;
        private int         frameOffs_y     = 0;
        private int         frameSize_w     = 0;
        private int         frameSize_h     = 0;
        // -- text frame
        private int         frameOffs_x2    = 0;
        private int         frameOffs_y2    = 0;
        private int         frameSize_w2    = 0;
        private int         frameSize_h2    = 0;
        // -- font
        private String      fontName        = null;
        private double      fontSize        = 0.0;
        // -- colors
        private Color       fillColor       = null;
        private Color       bordColor       = null;
        private Color       foreColor       = null;
        // -- image
        private volatile URL       imageURL  = null;
        private volatile ImageIcon imageIcon = null;
        // ---
        public TextIcon(String name,
            String path,
            int xOfs,  int yOfs,  int xSiz,  int ySiz,
            int xOfs2, int yOfs2, int xSiz2, int ySiz2,
            double fontPt, String fontName) {
            File iconFile = ResolveFile(path);
            URL  iconURL  = ToURL(iconFile);
            this._init(
                name, iconURL, // ImageIcon lazy loading
                xOfs, yOfs, xSiz, ySiz,
                fontPt, fontName);
            this._setTextFrame(xOfs2, yOfs2, xSiz2, ySiz2);
        }
        // ---
        public TextIcon(ImageIcon icon,
            int xOfs, int yOfs, int xSiz, int ySiz,
            int fontPt, String fontName) {
            this._init(
                icon, // ImageIcon already loaded
                xOfs, yOfs, xSiz, ySiz,
                fontPt, fontName);
        }
        // ---
        public TextIcon(File file,
            int xOfs, int yOfs, int xSiz, int ySiz,
            double fontPt, String fontName) {
            this(ToURL(file),
                xOfs, yOfs, xSiz, ySiz,
                fontPt, fontName);
        }
        // ---
        public TextIcon(URL url,
            int xOfs, int yOfs, int xSiz, int ySiz,
            double fontPt, String fontName) {
            this._init(LoadImageIcon(url),  // ImageIcon loaded now
                xOfs, yOfs, xSiz, ySiz,
                fontPt, fontName);
        }
        // ---
        public TextIcon(TextIcon ti) {
            if (ti != null) {
                this._setName(ti.name);
                this._setColorFrame(ti.frameOffs_x,ti.frameOffs_y,ti.frameSize_w,ti.frameSize_h);
                this._setTextFrame(ti.frameOffs_x2,ti.frameOffs_y2,ti.frameSize_w2,ti.frameSize_h2);
                this._setFont(ti.fontName,ti.fontSize);
                this.fillColor = ti.fillColor;
                this.bordColor = ti.bordColor;
                this.foreColor = ti.foreColor;
                synchronized (ti) {
                    this.imageURL  = ti.imageURL;
                    this.imageIcon = ti.imageIcon;
                }
            } else {
                Print.logStackTrace("Specified TextIcon is null");
            }
        }
        // ---
        private void _init(
            String name, URL iconURL,
            int xOfs, int yOfs, int xSiz, int ySiz,
            double fontPt, String fontName) {
            this._setName(name);
            this._setColorFrame(xOfs, yOfs, xSiz, ySiz);
            this._setFont(fontName, fontPt);
            this.imageURL  = iconURL; // may be null
            this.imageIcon = null;
            if (!StringTools.isBlank(name) && (this.imageURL == null)) {
                Print.logError("Unable to init TextIcon: " + name);
            }
        }
        private void _init(
            ImageIcon icon,
            int xOfs, int yOfs, int xSiz, int ySiz,
            double fontPt, String fontName) {
            this._init(null,(URL)null,xOfs,yOfs,xSiz,ySiz,fontPt,fontName);
            this.imageIcon = icon;
            if (this.imageIcon == null) {
                Print.logWarn("Specified ImageIcon is null");
            } else {
                int loadStat = this.imageIcon.getImageLoadStatus();
                if (loadStat == MediaTracker.ABORTED) {
                    Print.logWarn("ImageIcon loading aborted");
                } else
                if (loadStat == MediaTracker.ERRORED) {
                    Print.logWarn("ImageIcon load error");
                }
            }
        }
        // ---
        private void _setName(String n) {
            this.name = StringTools.trim(n);
        }
        public String getName() {
            return this.name;
        }
        // ---
        private void _setColorFrame(int x, int y, int w, int h) {
            this.frameOffs_x = x;
            this.frameOffs_y = y;
            this.frameSize_w = w;
            this.frameSize_h = h;
            this._setTextFrame(x, y, w, h);
        }
        public int getColorOffset_X() {
            return this.frameOffs_x;
        }
        public int getColorOffset_Y() {
            return this.frameOffs_y;
        }
        public int getColorSize_W() {
            return this.frameSize_w;
        }
        public int getColorSize_H() {
            return this.frameSize_h;
        }
        // ---
        private void _setTextFrame(int x, int y, int w, int h) {
            this.frameOffs_x2 = x;
            this.frameOffs_y2 = y;
            if ((w > 0) && (h > 0)) {
                this.frameSize_w2 = w;
                this.frameSize_h2 = h;
            } else {
                this.frameSize_w2 = this.frameSize_w;
                this.frameSize_h2 = this.frameSize_h;
            }
        }
        public int getTextOffset_X() {
            return this.frameOffs_x2;
        }
        public int getTextOffset_Y() {
            return this.frameOffs_y2;
        }
        public int getTextSize_W() {
            return (this.frameSize_w2 > 0)? this.frameSize_w2 : this.getColorSize_W();
        }
        public int getTextSize_H() {
            return (this.frameSize_h2 > 0)? this.frameSize_h2 : this.getColorSize_H();
        }
        // ---
        public void setFillColor(Color color) {
            this.fillColor = color;
        }
        public void setBorderColor(Color color) {
            this.bordColor = color;
        }
        public void setForegroundColor(Color color) {
            this.foreColor = color;
        }
        // ---
        private void _setFont(String name, double pt) {
            this.fontName = StringTools.blankDefault(name, DEFAULT_TEXT_FONT);
            this.fontSize = (pt > 0.0)? pt : DEFAULT_FONT_SIZE;
        }
        public String getFontName() {
            return !StringTools.isBlank(this.fontName)? this.fontName : DEFAULT_TEXT_FONT;
        }
        public double getFontSize() {
            return (this.fontSize > 0.0)? this.fontSize : DEFAULT_FONT_SIZE;
        }
        // ---
        public ImageIcon getImageIcon() {
            // -- load ImageIcon, if not already loaded
            if ((this.imageIcon == null) && (this.imageURL != null)) {
                synchronized (this) {
                    // -- check again after lock
                    if ((this.imageIcon == null) && (this.imageURL != null)) {
                        this.imageIcon = LoadImageIcon(this.imageURL);
                        this.imageURL  = null; // clear so we don't try this again
                    }
                }
            }
            // -- return ImageIcon (if successfully loaded)
            if (this.imageIcon == null) {
                // -- not loaded (invalid file?)
                return null;
            } else
            if (this.imageIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                // -- unable to load?
                return null;
            } else {
                return this.imageIcon;
            }
        }
        public RenderedImage createImage(String text, double arrow) {
            // -- check ImageIcon
            ImageIcon icon = this.getImageIcon();
            if (icon == null) {
                Print.logWarn("ImageIcon is null");
                return null;
            }
            // -- check load status
            int loadStat = icon.getImageLoadStatus();
            if (loadStat == MediaTracker.ABORTED) {
                Print.logWarn("ImageIcon loading aborted");
                return null;
            } else
            if (loadStat == MediaTracker.ERRORED) {
                Print.logWarn("ImageIcon load error");
                return null;
            }
            // -- warn if still loading, continue ...
            if (loadStat == MediaTracker.LOADING) {
                Print.logWarn("ImageIcon is still loading");
            }
            // -- get BufferedImage
            BufferedImage image = null;
            Graphics2D    g2D   = null;
            try {
                int xOfs = this.getColorOffset_X();
                int yOfs = this.getColorOffset_Y();
                int xSiz = this.getColorSize_W();
                int ySiz = this.getColorSize_H();
                int W = icon.getIconWidth();
                int H = icon.getIconHeight();
                image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
                g2D = image.createGraphics();
                /* draw template icon */
                g2D.drawImage(icon.getImage(),0,0,W,H,new ImageObserver() {
                    public boolean imageUpdate(Image img, int infoflags, int x, int y, int W, int H) {
                        return false;
                    }
                });
                /* fill rectangular region with color */
                if (this.fillColor != null) {
                    int alpha = this.fillColor.getAlpha();
                    if (alpha != 0xFF) {
                        // -- alpha component
                        float alphaF = (float)alpha / 255.0F;
                        Composite origComposite = g2D.getComposite();
                        g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN,alphaF));
                        g2D.setColor(this.fillColor);
                        g2D.fillRect(xOfs, yOfs, xSiz, ySiz);
                        g2D.setComposite(origComposite);
                    } else {
                        // -- opaque
                        g2D.setColor(this.fillColor);
                        g2D.fillRect(xOfs, yOfs, xSiz, ySiz);
                    }
                }
                if (this.bordColor != null) {
                    // -- always opaque (ignore alpha)
                    g2D.setColor(this.bordColor);
                    g2D.drawRect(xOfs, yOfs, xSiz-1, ySiz-1);
                }
            } catch (Throwable th) {
                Print.logError("Unable to create TextIcon image: " + th);
                return null; // fatal error
            }
            // -- draw text
            try {
                int    xOfs    = this.getTextOffset_X();
                int    yOfs    = this.getTextOffset_Y();
                int    xSiz    = this.getTextSize_W();
                int    ySiz    = this.getTextSize_H();
                String txtL[]  = StringTools.split(StringTools.trim(text),'\n');
                int    lineCnt = ListTools.size(txtL);
                double fontPT  = this.getFontSize();
                Font   font    = new Font(this.getFontName(), Font.PLAIN, (int)Math.round(fontPT));
                Color  fColor  = (this.foreColor != null)? this.foreColor : Color.black;
                double textH   = (fontPT * (double)lineCnt) + (double)(lineCnt - 1);
                double dY      = (((double)ySiz - textH) / 2.0);
                for (String txt : txtL) {
                    double pt = fontPT;
                    dY += pt - 1.0; // reasonable guess at height
                    if (txt.length() > 0) {
                        String T = txt;
                        Font   F = font;
                        Color  C = fColor;
                        if (T.startsWith(META_START)) {
                            // -- meta data "%{FontSize,Color}Text" (ie. "%{-1,red}Hello"
                            int p = T.indexOf(META_END);
                            if (p > 0) {
                                String meta = T.substring(META_START.length(),p);
                                T = T.substring(p+1);
                                //Print.logInfo("Text = " + T);
                                String sc[] = StringTools.split(meta,',');
                                if ((sc.length > 0) && !StringTools.isBlank(sc[0])) {
                                    // -- font size
                                    if (sc[0].startsWith("+") || sc[0].startsWith("-")) {
                                        pt = (double)F.getSize2D() + StringTools.parseDouble(sc[0],0.0F);
                                    } else {
                                        pt = StringTools.parseDouble(sc[0],pt);
                                    }
                                    F = new Font(this.getFontName(), Font.PLAIN, (int)Math.round(pt));
                                }
                                if ((sc.length > 1) && !StringTools.isBlank(sc[1])) {
                                    // -- font color
                                    C = ColorTools.parseColor(sc[1],C);
                                }
                            }
                        }
                        g2D.setFont(F);
                        g2D.setColor(C);
                        double txPx = g2D.getFontMetrics().stringWidth(T); // (pt * 0.59375) * (double)txLen;
                        double dX = ((double)xSiz - txPx) / 2.0;
                        if (dX < 0) { dX = 0; }
                        g2D.drawString(T, (float)((double)xOfs + dX), (float)((double)yOfs + dY));
                    }
                    dY += 1.0; // space between lines
                }
            } catch (Throwable th) {
                Print.logError("Unable to draw text into Icon: " + th);
                // continuing without drawn text
            }
            // -- draw arrow
            try {
                int xOfs = this.getColorOffset_X();
                int yOfs = this.getColorOffset_Y();
                int xSiz = this.getColorSize_W();
                int ySiz = this.getColorSize_H();
                if (arrow == 360.0) {
                    double D        = (double)Math.min(xSiz,ySiz) - 5.0;
                    double dx       = xOfs + ((xSiz - D) / 2.0);
                    double dy       = yOfs + ((ySiz - D) / 2.0);
                    Ellipse2D.Double circle = new Ellipse2D.Double(dx,dy,D,D);
                    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // -- border (drawn first)
                    if (this.bordColor != null) {
                        g2D.setColor(this.bordColor);
                        g2D.draw(circle); // outline
                    } else {
                        g2D.setColor(Color.black);
                        g2D.draw(circle); // outline
                    }
                    // -- fill (drawn on top of border)
                    g2D.setColor((this.foreColor != null)? this.foreColor : Color.black);
                    g2D.fill(circle);
                } else
                if (arrow >= 0.0) {
                    // -- draw arrow
                  //double L        = this.getFontSize() + 2.0;
                    double L        = (double)Math.min(xSiz,ySiz) - 2.0;
                    Polygon poly    = new Polygon();
                    double thetaR   = (360.0 - ((arrow - 20.0) % 360.0)) * GeoPoint.RADIANS;
                    double thetaC   = (360.0 - ((arrow       ) % 360.0)) * GeoPoint.RADIANS;
                    double thetaL   = (360.0 - ((arrow + 20.0) % 360.0)) * GeoPoint.RADIANS;
                    double dx       = Math.sin(thetaC) * L;
                    double dy       = Math.cos(thetaC) * L;
                    int    axOfs    = xOfs + (xSiz/2) - (int)Math.round(dx/2); // + 1;
                    if (axOfs < 0) { axOfs = 0; }
                    int    ayOfs    = yOfs + (ySiz/2) - (int)Math.round(dy/2) - 1; // + 1;
                    if (ayOfs < 0) { ayOfs = 0; }
                    poly.addPoint(0, 0);
                    poly.addPoint((int)(Math.sin(thetaR)*L)      , (int)(Math.cos(thetaR)*L      ));
                    poly.addPoint((int)(Math.sin(thetaC)*(L-2.0)), (int)(Math.cos(thetaC)*(L-2.0)));
                    poly.addPoint((int)(Math.sin(thetaL)*L)      , (int)(Math.cos(thetaL)*L      ));
                    poly.addPoint(0, 0);
                    poly.translate(axOfs, ayOfs);
                    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // border (drawn first)
                    if (this.bordColor != null) {
                        g2D.setColor(this.bordColor);
                        g2D.draw(poly); // outline
                    } else {
                        g2D.setColor(Color.black);
                        g2D.draw(poly); // outline
                    }
                    // fill (drawn on top of border)
                    g2D.setColor((this.foreColor != null)? this.foreColor : Color.black);
                    g2D.fill(poly);
                }
            } catch (Throwable th) {
                Print.logError("Unable to draw arrow into Icon: " + th);
                // -- continuing without drawn text
            }
            // -- return image
            try {
                g2D.dispose(); // release graphics
            } catch (Throwable th) {
                Print.logError("Error disposing image graphics: " + th);
            }
            return image;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Loads icon images found at the specified directory
    *** @param baseDir  The directory containing the icon images to load.
    ***                 Must be relative to the servlet context path.
    **/
    public static OrderedMap<String,PushpinIcon> LoadFromDirectory(String baseDir,
        OrderedMap<String,PushpinIcon> ppiMap)
    {
        //Print.logInfo("Relative Image directory: " + baseDir);

        /* adjust baseDir */
        if (StringTools.isBlank(baseDir)) {
            baseDir = "./";
        } else
        if (!baseDir.endsWith("/")) {
            baseDir += "/";
        }

        /* get context path */
        String ctxPath = RTConfig.getContextPath();
        if (!RTConfig.isWebApp()) {
            // -- likely running from command-line
            ctxPath += "/war/track"; // assume images are relative to "war/track/"
        }

        /* full image directory */
        File ppDir = new File(ctxPath, baseDir);
        if (!ppDir.exists()) {
            // -- quietly ignore
            return ppiMap;
        } else
        if (!ppDir.isDirectory()) {
            // -- exists, but is not a directory!
            Print.logError("ERROR: not a directory: " + ppDir);
            return ppiMap;
        }

        /* extract all images */
        String ppExtn[] = { "png", "jpg", "jpeg", "gif" };
        File ppFiles[] = FileTools.getFiles(ppDir, ppExtn, false/*no-recursive*/);
        if (ListTools.isEmpty(ppFiles)) {
            // -- directory exists, but does not contain any images
            Print.logWarn("ERROR: no pushpin images found: " + ppDir);
            return ppiMap;
        }

        /* sort by name */
        ListTools.sort(ppFiles, new ListTools.StringComparator<File>(true,true) {
            public int compare(File f1, File f2) {
                return super._compare(f1.getName(),f2.getName());
            }
        });

        /* create "PushpinIcon" entries */
        Set<String> savedPPName = new HashSet<String>();
        if (ppiMap == null) { ppiMap = new OrderedMap<String,PushpinIcon>(); }
        for (File ppFile : ppFiles) {

            /* load pushpin image */
            ImageIcon ppIcon = LoadImageIcon(ppFile);
            if (ppIcon == null) {
                Print.logWarn("Warning: unable to load image: " + ppFile);
                continue;
            }

            /* pushpin key */
            String ppName = FileTools.removeExtension(ppFile.getName());

            /* pushpin name */
            String ppIconURL = baseDir + ppFile.getName();

            /* shadow? (not currently supported) */
            String shadowURL    = null; // no shadow
            int    shadowSize[] = null;
            if (StringTools.containsIgnoreCase(ppIconURL,"shadow")) {
                continue; // skip shadow icons
            }

            /* pushpin dimensions */
            int ppW = ppIcon.getIconWidth();
            int ppH = ppIcon.getIconHeight();
            int ppIconSize[] = new int[] { ppW, ppH };

            /* hotspot */
            int ppIconHotspot[] = new int[] { ppW / 2, ppH / 2 }; // default center
            if (ppName.indexOf(":") >= 0) { // [2.6.4-B82]
                // -- "PushpinName:20,10", "PushpinName:m,c", "PushpinName:m"
                int hsp = ppName.indexOf(":");
                String dim[] = StringTools.split(ppName.substring(hsp+1),',');
                // -- X
                if (dim.length >= 1) {
                    int n = 0; // X
                    if (dim[n].equalsIgnoreCase("C")) { // center
                        ppIconHotspot[n] = ppIconSize[n] / 2;
                    } else
                    if (dim[n].equalsIgnoreCase("M")) { // maximum
                        ppIconHotspot[n] = ppIconSize[n];
                    } else {
                        int v = StringTools.parseInt(dim[n],-1);
                        if (v >= 0) {
                            ppIconHotspot[n] = Math.min(v,ppIconSize[n]);
                        }
                    }
                }
                // -- Y
                if (dim.length >= 2) {
                    int n = 1; // Y
                    if (dim[n].equalsIgnoreCase("C")) { // center
                        ppIconHotspot[n] = ppIconSize[n] / 2;
                    } else
                    if (dim[n].equalsIgnoreCase("M")) { // maximum
                        ppIconHotspot[n] = ppIconSize[n];
                    } else {
                        int v = StringTools.parseInt(dim[n],-1);
                        if (v >= 0) {
                            ppIconHotspot[n] = Math.min(v,ppIconSize[n]);
                        }
                    }
                }
                // -- trim ":X,Y" from name
                ppName = ppName.substring(0,hsp);
            }

            /* create PushpinIcon */
            // -- with support for allowing the hotspot to be included in the
            // -  filename, it's possible that there could be different file
            // -  names, but identical pushpin names.  Only the first pushpin
            // -  name encountered in the directory will be saved.
            if (!savedPPName.contains(ppName)) {
                // -- ppName not yet added
                PushpinIcon ppi = new PushpinIcon(
                    ppName   , null,
                    ppIconURL, ppIconSize, ppIconHotspot,
                    shadowURL, shadowSize
                    );
                ppiMap.put(ppName, ppi);
                // -- save ppName
                savedPPName.add(ppName);
            }

        }

        /* return map */
        return ppiMap;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the PushpinIcon instance of the iconName from the specified pushpin/icon map
    *** @param iconName  The name of the icon/pushpin
    *** @param iconMap   The icon/pushpin map
    *** @return The PushpinIcon instance of the icon/pushpin, or null if not found
    **/
    public static PushpinIcon getPushpinIcon(String iconName, OrderedMap<String,PushpinIcon> iconMap)
    {
        OrderedMap<String,PushpinIcon> map = (iconMap != null)? iconMap : _DefaultPushpinIconMap();
        return map.get(iconName); // may return null
    }

    /**
    *** Gets the index of the iconName in the specified pushpin/icon map
    *** @param iconName  The name of the icon/pushpin
    *** @param iconMap   The icon/pushpin map
    *** @param dft       The default to return if the icon name is not foud in the map
    *** @return The index of the icon/pushpin
    **/
    public static int getPushpinIconIndex(String iconName, OrderedMap<String,PushpinIcon> iconMap, int dft)
    {

        /* no iconName specified? */
        if (StringTools.isBlank(iconName)) {
            return dft;
        }

        /* no iconMap specified? */
        int iconMapSize = ListTools.size(iconMap);
        if (iconMapSize <= 0) {
            // -- iconMap null/empty (should not occur)
            if (iconName.equalsIgnoreCase("black" )) { return ICON_PUSHPIN_BLACK ; }
            if (iconName.equalsIgnoreCase("brown" )) { return ICON_PUSHPIN_BROWN ; }
            if (iconName.equalsIgnoreCase("red"   )) { return ICON_PUSHPIN_RED   ; }
            if (iconName.equalsIgnoreCase("orange")) { return ICON_PUSHPIN_ORANGE; }
            if (iconName.equalsIgnoreCase("yellow")) { return ICON_PUSHPIN_YELLOW; }
            if (iconName.equalsIgnoreCase("green" )) { return ICON_PUSHPIN_GREEN ; }
            if (iconName.equalsIgnoreCase("blue"  )) { return ICON_PUSHPIN_BLUE  ; }
            if (iconName.equalsIgnoreCase("purple")) { return ICON_PUSHPIN_PURPLE; }
            if (iconName.equalsIgnoreCase("gray"  )) { return ICON_PUSHPIN_GRAY  ; }
            if (iconName.equalsIgnoreCase("white" )) { return ICON_PUSHPIN_WHITE ; }
            return dft;
        }

        /* iconName specifies index (ie. "#12") */
        if (iconName.startsWith("#")) {
            int ndx = StringTools.parseInt(iconName.substring(1),-1);
            return ((ndx >= 0) && (ndx < iconMapSize))? ndx : dft;
        }

        /* return index */
        int ndx = iconMap.indexOfKey(iconName); // index of PushpinIcon
        return (ndx >= 0)? ndx : dft;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String          name         = "";

    private String          imageURL     = null;

    private String          iconURL      = null;
    private boolean         iconEval     = false;
    private int             iconWidth    = 12;
    private int             iconHeight   = 20;
    private int             iconHotspotX = 6;
    private int             iconHotspotY = 20;

    private String          shadowURL    = null;
    private int             shadowWidth  = 22;
    private int             shadowHeight = 20;

    private String          backURL      = null;
    private int             backWidth    = 0;
    private int             backHeight   = 0;
    private int             backOffsetX  = 0;
    private int             backOffsetY  = 0;

    // ------------------------------------------------------------------------

    /**
    *** Constructor
    *** @param name         This PushPin name
    *** @param imageURL     The image used to represent the icon for user selection
    *** @param icon         The icon image URL, for Javascript command which returns the icon URL
    *** @param iconEval     "false" indicates that 'icon' specifies a fixed URL, "true" indicates that 'icon'
    ***                     is a Javascript line that when evaluated, will return the icon URL.
    *** @param iconSize     2 element array containing the icon image width/height (in pixels)
    *** @param iconHotspot  2 element array specifying the icon image X/Y 'hotspot' (in pixels)
    *** @param shadow       The icon shadow image url
    *** @param shadowSize   2 element array containing the icon shadow width/height (in pixels)
    *** @param back         The icon background url
    *** @param backSize     2 element array containing the icon background image width/height (in pixels)
    *** @param backOffset   2 element array specifying the icon background image X/Y 'hotspot' (in pixels)
    **/
    public PushpinIcon(
        String name  , String imageURL,
        String icon  , boolean iconEval, int iconSize[], int iconHotspot[],
        String shadow, int shadowSize[],
        String back  , int backSize[]  , int backOffset[]
        )
    {

        /* name */
        this.name         = StringTools.trim(name);

        /* image representation */
        this.imageURL     = StringTools.trim(imageURL);

        /* icon image */
        this.iconURL      = StringTools.trim(icon);
        this.iconEval     = iconEval;
        this.iconWidth    = (ListTools.size(iconSize)    >= 2)? iconSize[0]   : 0;
        this.iconHeight   = (ListTools.size(iconSize)    >= 2)? iconSize[1]   : 0;
        this.iconHotspotX = (ListTools.size(iconHotspot) >= 2)? iconHotspot[0] : 0;
        this.iconHotspotY = (ListTools.size(iconHotspot) >= 2)? iconHotspot[1] : 0;

        /* icon shadow */
        this.shadowURL    = shadow;
        this.shadowWidth  = (ListTools.size(shadowSize)  >= 2)? shadowSize[0] : 0;
        this.shadowHeight = (ListTools.size(shadowSize)  >= 2)? shadowSize[1] : 0;

        /* icon background image */
        this.backURL      = StringTools.trim(back);
        this.backWidth    = (ListTools.size(backSize)    >= 2)? backSize[0]   : 0;
        this.backHeight   = (ListTools.size(backSize)    >= 2)? backSize[1]   : 0;
        this.backOffsetX  = (ListTools.size(backOffset)  >= 2)? backOffset[0] : 0;
        this.backOffsetY  = (ListTools.size(backOffset)  >= 2)? backOffset[1] : 0;

    }

    /**
    *** Constructor
    *** @param name          This PushPin name
    *** @param imageURL      The image used to represent the icon for user selection
    *** @param icon          The icon image URL
    *** @param iconSize      2 element array containing the icon image width/height (in pixels)
    *** @param iconHotspot   2 element array specifying the icon image X/Y 'hotspot' (in pixels)
    *** @param shadow        The icon shadow image url
    *** @param shadowSize    2 element array containing the icon shadow width/height (in pixels)
    **/
    public PushpinIcon(
        String name  , String imageURL,
        String icon  , int iconSize[], int iconHotspot[],
        String shadow, int shadowSize[]
        )
    {
        this(name , imageURL,
            icon  , false, iconSize, iconHotspot,
            shadow, shadowSize,
            null  , null, null
            );
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the name of this PushPin
    *** @return The name of this PushPin
    **/
    public String getName()
    {
        return this.name;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the representation image URL for this Pushpin Icon for user selection
    *** @return The representation image url
    **/
    public String getImageURL()
    {
        if (!StringTools.isBlank(this.imageURL)) {
            return this.imageURL;
        } else
        if (!this.getIconEval()) {
            return this.getIconURL();
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the Pushpin Icon image URL
    *** @return The icon image url
    **/
    public String getIconURL()
    {
        return this.iconURL;
    }

    /**
    *** Gets the Pushpin Icon image URL
    *** @param keyValMap  The StringTools.KeyValueMap interface for replacing any 'keys' found in the URL
    *** @return The icon image url
    **/
    /* not currently used
    public String getIconURL(StringTools.KeyValueMap keyValMap)
    {
        // This method allows handling pushpin URLs of the form:
        //  - http://pp.example.com/image/pushpin.png?heading=${heading}
        //  - http://pp.example.com/image/pushpin.png?R=${R}&G=${G}&B=${B}
        //  - http://pp.example.com/image/pushpin.png?color=${color}
        //  - http://pp.example.com/image/pushpin.png?index=${index}
        //  - etc.
        return StringTools.replaceKeys(this.iconURL, keyValMap);
    }
    */

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the icon URL should be run through "eval(...)"
    *** @return True if the icon URL should be run through "eval(...)"
    **/
    public boolean getIconEval()
    {
        return this.iconEval;
    }

    /**
    *** This method attempts to duplicate the JavaScript evaluation for generated
    *** pushpin URLs which will be used on the maps/reports, etc.<br>
    *** This impelmentation should match the JavaScript implementations in the "jsmaps.js"
    *** module.  The following JavaScript functions are duplicated here:
    *** <ul>
    ***   <li>evHeadingYellowURL(e)</li>
    ***   <li>evHeadingGreenURL(e)</li>
    ***   <li>evHeadingMarkerURL(e)</li>
    ***   <li>evHeadingMarkerURL_eu(e)</li>
    ***   <li>evHeadingMarkerURL_ca(e)</li>
    ***   <li>evDeviceNameIconURL(e)</li>
    ***   <li>evArrowIconURL(e,...)</li>
    ***   <li>evIndexedIconURL(e)</li>
    *** </ul>
    *** @param e     The EventData instance
    *** @param index The event index (starting with '1')
    *** @return  The pushpin URL
    **/
    public String getIconEvalURL(EventData e, int index)
    {
        // Possible evaluated urls:
        //  evHeadingYellowURL(e)
        //  evHeadingGreenURL(e)
        //  evHeadingMarkerURL(e)
        //  evHeadingMarkerURL_eu(e)
        //  evHeadingMarkerURL_ca(e)
        //  evDeviceNameIconURL(e)             - white
        //  evDeviceNameIconURL(e,'FF8888')    - red
        //  evDeviceNameIconURL(e,'88FF88')    - green
        //  evDeviceNameIconURL(e,'8888FF')    - blue
        //  evDeviceNameIconURL(e,'FFFF88')    - yellow
        //  evArrowIconURL(e,'/images/pp/pin30_cyan.png','3,4,11,7,10')
        //  evArrowIconURL(e,'/images/pp/pin30_yellow.png','3,4,11,7,10')
        //  evIndexedIconURL(e)

        /* return default icon, if not an evaluated pushpin */
        if (!this.getIconEval()) {
            return this.getImageURL();
        }

        /* test eval */
        double e_speedKPH = (e != null)? e.getSpeedKPH() : 0.0;
        double e_heading  = (e != null)? e.getHeading()  : 0.0;
        String eval = this.getIconURL();
        if (eval.startsWith("evHeadingYellowURL(")) {
            if (e_speedKPH < 1.0) {
                // probably not moving
                return "images/pp/pin30_yellow.png";
            } else {
                int x = (int)Math.round(e_heading / 45.0) % 8;
                return "images/pp/pin30_yellow_h"+x+".png";
            }
        } else
        if (eval.startsWith("evHeadingGreenURL(")) {
            if (e_speedKPH < 1.0) {
                // probably not moving
                return "images/pp/pin30_green.png";
            } else {
                int x = (int)Math.round(e_heading / 45.0) % 8;
                return "images/pp/pin30_green_h"+x+".png";
            }
        } else
        if (eval.startsWith("evHeadingMarkerURL(")) {
            if (e_speedKPH < 5.0) {
                // probably not moving
                return "images/pp/pin30_red_dot.png";
            } else
            if (e_speedKPH < 32.0) {
                // 5 <= X < 32
                int x = (int)Math.round(e_heading / 45.0) % 8;
                return "images/pp/pin30_yellow_h"+x+".png";
            } else {
                // 32 <= X
                int x = (int)Math.round(e_heading / 45.0) % 8;
                return "images/pp/pin30_green_h"+x+".png";
            }
        } else
        if (eval.startsWith("evHeadingMarkerURL_eu(")) {
            double speedMPH = e_speedKPH * GeoPoint.MILES_PER_KILOMETER;
            if (speedMPH < 5.0) {
                // probably not moving
                return "images/pp/pin30_red_dot.png";
            } else
            if (speedMPH < 50.0) {
                int x = (int)Math.round(e_heading / 45.0) % 8;
                return "images/pp/pin30_yellow_h"+x+".png";
            } else
            if (speedMPH < 90.0) {
                int x = (int)Math.round(e_heading / 45.0) % 8;
                return "images/pp/pin30_green_h"+x+".png";
            } else
            if (speedMPH < 110.0) {
                return "images/pp/pin30_gray.png";
            } else {
                return "images/pp/pin30_black.png";
            }
        } else
        if (eval.startsWith("evHeadingMarkerURL_ca(")) {
            if (e_speedKPH < 1.0) {
                // probably not moving
                return "images/pp/pin30_red.png";
            } else
            if (e_speedKPH < 70.0) {
                int x = (int)Math.round(e_heading / 45.0) % 8;
                return "images/pp/pin30_yellow_h"+x+".png";
            } else
            if (e_speedKPH < 100.0) {
                int x = (int)Math.round(e_heading / 45.0) % 8;
                return "images/pp/pin30_green_h"+x+".png";
            } else
            if (e_speedKPH < 130.0) {
                int x = (int)Math.round(e_heading / 45.0) % 8;
                return "images/pp/pin30_blue_h"+x+".png";
            } else {
                int x = (int)Math.round(e_heading / 45.0) % 8;
                return "images/pp/pin30_gray_h"+x+".png";
            }
        } else
        if (eval.startsWith("evDeviceNameIconURL(")) {
            //  evDeviceNameIconURL(e)             - white
            //  evDeviceNameIconURL(e,'FF8888')    - red
            //  evDeviceNameIconURL(e,'88FF88')    - green
            //  evDeviceNameIconURL(e,'8888FF')    - blue
            //  evDeviceNameIconURL(e,'FFFF88')    - yellow
            // http://DOMAIN/track/Marker?icon=/images/pp/label47_fill.png&fr=3,2,42,13,9&text=Demo2&border=red&fill=yellow
            Device dev    = (e != null)? e.getDevice() : null;
            String icon   = "/images/pp/label47_fill.png";
            String fr     = "3,2,42,13,9";
            String text   = (dev != null)? dev.getDisplayName() : (e != null)? e.getDeviceID() : "";
            String fill   = ""; // fill color (TODO: parse frame color value, if present)
            String border = ""; // border color
            String color  = ""; // text color
            String url =
                "Marker?"  +
                "icon="    + icon +
                "&fr="     + fr +
                "&fill="   + fill +
                "&border=" + border +
                "&color="  + color +
                "&text="   + StringTools.htmlFilterValue(text);
            return url;
        } else
        if (eval.startsWith("evArrowIconURL(")) {
            //  evArrowIconURL(e,'/images/pp/pin30_cyan.png','3,4,11,7,10')
            //  evArrowIconURL(e,'/images/pp/pin30_yellow.png','3,4,11,7,10')
            double arrow  = e_heading;
            String icon   = "/images/pp/pin30_cyan.png"; // TODO: parse image url
            String fr     = "3,4,11,7,10";
            String text   = "";
            String fill   = "";
            String border = ""; // border color
            String color  = "000000"; // text color
            String url =
                "Marker?"  +
                "icon="    + icon +
                "&fr="     + fr +
                "&fill="   + fill +
                "&border=" + border +
                "&color="  + color +
                "&arrow="  + arrow;
            return url;
        } else
        if (eval.startsWith("evIndexedIconURL(")) {
            // http://DOMAIN/track/Marker?icon=/images/pp/pin30_blue_fill.png&fr=3,4,11,7,9,Serif&color=880000&text=99
            String icon   = "/images/pp/pin30_blue_fill.png";
            String fr     = "3,4,11,7,9,Serif";
            String text   = String.valueOf(1); // TODO: set index ("e.index")
            String fill   = ""; // fill color (TODO: parse frame color value, if present)
            String border = ""; // border color
            String color  = "880000"; // text color
            String url =
                "Marker?"  +
                "icon="    + icon +
                "&fr="     + fr +
                "&fill="   + fill +
                "&border=" + border +
                "&color="  + color +
                "&text="   + StringTools.htmlFilterValue(text);
            return url;
        }

        /* not found, return default */
        return this.getImageURL();

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Pushpin Icon image width (in pixels)
    *** @return The icon image width
    **/
    public int getIconWidth()
    {
        return this.iconWidth;
    }

    /**
    *** Gets the Pushpin Icon image height (in pixels)
    *** @return The icon image height
    **/
    public int getIconHeight()
    {
        return this.iconHeight;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Pushpin Icon image 'hotspot' X-offset (in pixels)
    *** @return The icon image 'hotspot' X-offset
    **/
    public int getIconHotspotX()
    {
        return this.iconHotspotX;
    }

    /**
    *** Gets the Pushpin Icon image 'hotspot' Y-offset (in pixels)
    *** @return The icon image 'hotspot' Y-offset
    **/
    public int getIconHotspotY()
    {
        return this.iconHotspotY;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the shadow has been defined
    **/
    public boolean hasShadow()
    {
        if (StringTools.isBlank(this.getShadowURL())) {
            return false;
        } else
        if ((this.getShadowWidth() <= 0) || (this.getShadowHeight() <= 0)) {
            return false;
        } else {
            return true;
        }
    }

    /**
    *** Gets the Pushpin Icon shadow image URL
    *** @return The icon shadow image url
    **/
    public String getShadowURL()
    {
        return this.shadowURL;
    }

    // --------------------------------

    /**
    *** Gets the Pushpin Icon shadow image width (in pixels)
    *** @return The icon shadow image width
    **/
    public int getShadowWidth()
    {
        return this.shadowWidth;
    }

    /**
    *** Gets the Pushpin Icon shadow image height (in pixels)
    *** @return The icon shadow image height
    **/
    public int getShadowHeight()
    {
        return this.shadowHeight;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the Pushpin Icon background image URL
    *** @return The icon background image url
    **/
    public String getBackgroundURL()
    {
        return this.backURL;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Pushpin Icon background image width (in pixels)
    *** @return The icon background image width
    **/
    public int getBackgroundWidth()
    {
        return this.backWidth;
    }

    /**
    *** Gets the Pushpin Icon background image height (in pixels)
    *** @return The icon background image height
    **/
    public int getBackgroundHeight()
    {
        return this.backHeight;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Pushpin Icon background image 'hotspot' X-offset (in pixels)
    *** @return The icon image 'hotspot' X-offset
    **/
    public int getBackgroundOffsetX()
    {
        return this.backOffsetX;
    }

    /**
    *** Gets the Pushpin Icon background image 'hotspot' Y-offset (in pixels)
    *** @return The icon image 'hotspot' Y-offset
    **/
    public int getBackgroundOffsetY()
    {
        return this.backOffsetY;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the String representation of this Pushpin object
    *** @return The String representation of this Pushpin object
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Icon:'"  ).append(this.getIconURL()).append("', ");
        sb.append("IconSize:[").append(this.getIconWidth()).append(",").append(this.getIconHeight()).append("], ");
        sb.append("IconOffset:[").append(this.getIconHotspotX()).append(",").append(this.getIconHotspotY()).append("], ");
        sb.append("Shadow:'").append(this.getShadowURL()).append("', ");
        sb.append("ShadowSize:[").append(this.getShadowWidth()).append(",").append(this.getShadowHeight()).append("]");
        if (!StringTools.isBlank(this.getBackgroundURL())) {
            sb.append("Back:'"  ).append(this.getBackgroundURL()).append("', ");
            sb.append("BackSize:[").append(this.getBackgroundWidth()).append(",").append(this.getBackgroundHeight()).append("], ");
            sb.append("BackOffset:[").append(this.getBackgroundOffsetX()).append(",").append(this.getBackgroundOffsetY()).append("], ");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_IMAGE_DIR[] = { "pushpinDir", "ppDir", "dir" };

    /**
    *** Outputs (to stdout) a custom "Pushpins" tag section from the images found in a specified directory.<br>
    *** Notes:<br>
    ***     - Shadows are not currently supported.<br>
    ***     - Any image name containing "shadow" is ignored.<br>
    ***     - Supported image file extensions include: "png", "jpg", "jpeg", "gif"<br>
    ***     - The Pushpin "key" will be the name of the file, without the extension.<br>
    ***     - The "iconOffset" will be set to half the image width and height.<br>
    ***     - The directory subpath following "/war/track/" will be used as the "baseURL".<br>
    *** Example:<br>
    ***     bin/exeJava org.opengts.war.tools.PushpinIcon -dir=Path/To/War/Track/Pushpin/Directory<br>
    *** Output (stdout):<br>
    ***     &lt;!-- Custom pushpins --&gt;<br>
    ***     &lt;Pushpins baseURL="Pushpin/Directory/"&gt;<br>
    ***         ...<br>
    ***         &lt;Pushpin key="Custom_Pushpin" icon="Custom_Pushpin.png" iconSize="14,37" iconOffset="7,18" /&gt;<br>
    ***         ...<br>
    ***     &lt;/Pushpins&gt;<br>
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        /* image directory */
        File ppDir = FileTools.getRealFile(RTConfig.getFile(ARG_IMAGE_DIR,null),true);
        if (!FileTools.isDirectory(ppDir)) {
            Print.sysPrintln("ERROR: not a directory: " + ppDir);
            System.exit(1);
        }

        /* remove context path */
        String ppDirStr = ppDir.toString();
        String ctxPath  = RTConfig.getContextPath();
        if (ppDirStr.startsWith(ctxPath)) {
            ppDirStr = ppDirStr.substring(ctxPath.length());
            if (ppDirStr.startsWith("/")) {
                ppDirStr = ppDirStr.substring(1);
            }
            if (ppDirStr.startsWith("war/track/")) {
                ppDirStr = ppDirStr.substring("war/track/".length());
            }
        }

        /* load all PushpinIcon images */
        OrderedMap<String,PushpinIcon> ppiMap = PushpinIcon.LoadFromDirectory(ppDirStr,null);
        if (ListTools.isEmpty(ppiMap)) {
            Print.sysPrintln("No PushpinIcon's found in directory: " + ppDir);
            System.exit(1);
        }

        /* get longest filename */
        int ppMaxLen = 0;
        for (String ppKey : ppiMap.keySet()) {
            PushpinIcon ppi = ppiMap.get(ppKey);
            String ppiName = ppi.getIconURL();
            ppMaxLen = Math.max(ppMaxLen, ppiName.length());
        }

        /* Pushpins baseURL/ */
        String ppDirS    = ppDir.toString();
        String warTrackS = "war/track/";
        int    warTrackP = StringTools.indexOfIgnoreCase(ppDirS,warTrackS);
        String baseURL   = (warTrackP >= 0)? ppDirS.substring(warTrackP + warTrackS.length()) : ppDirS;
        if (!baseURL.endsWith("/")) { baseURL += "/"; }
        Print.sysPrintln("");
        Print.sysPrintln("<!-- Custom pushpins -->");
        Print.sysPrintln("<Pushpins baseURL=\""+baseURL+"\">");

        /* create "Pushpin" tag entries */
        int xtraSp = 1; // extra space between entries
        for (String ppKey : ppiMap.keySet()) {
            // <Pushpin key="Custom_Blue" icon="Custom_Blue.png" iconSize="14,37" iconOffset="7,18"/>
            PushpinIcon ppi = ppiMap.get(ppKey);

            /* begin "Pushpin" tag */
            StringBuffer sb = new StringBuffer();
            sb.append("    ");
            sb.append("<Pushpin");

            /* "key=" */
            String ppName = ppi.getName();
            sb.append(" ");
            String key = "key=\"" + ppName + "\"";
            sb.append(StringTools.padRight(key,' ',ppMaxLen-4+6+xtraSp));

            /* "icon=" */
            String ppIconURL = ppi.getIconURL();
            sb.append(" ");
            String icon = "icon=\"" + ppIconURL + "\"";
            sb.append(StringTools.padRight(icon,' ',ppMaxLen+7+xtraSp));

            /* "iconSize=" */
            int ppWidth  = ppi.getIconWidth();
            int ppHeight = ppi.getIconHeight();
            sb.append(" ");
            String iconSize = "iconSize=\"" + ppWidth + "," + ppHeight + "\"";
            sb.append(StringTools.padRight(iconSize,' ',16+xtraSp));

            /* "iconOffset=" */
            int hotspotX = ppi.getIconHotspotX();
            int hotspotY = ppi.getIconHotspotY();
            sb.append(" ");
            String iconOffset = "iconOffset=\"" + hotspotX + "," + hotspotY + "\"";
            sb.append(StringTools.padRight(iconOffset,' ',18));

            /* shadow? */
            if (ppi.hasShadow()) {
                String sp = StringTools.replicateString(" ",xtraSp);

                /* "shadow=" */
                String shadowURL = ppi.getShadowURL();
                sb.append(sp);
                sb.append(" ");
                String shadow = "shadow=\"" + shadowURL + "\"";
                sb.append(shadow);

                /* "shadowSize=" */
                int shadowWidth  = ppi.getShadowWidth();
                int shadowHeight = ppi.getShadowHeight();
                sb.append(sp);
                sb.append(" ");
                String shadowSize = "shadowSize=\"" + shadowWidth + "," + shadowHeight + "\"";
                sb.append(shadowSize);

            }

            /* end "Pushpin" tag */
            sb.append("/>");
            Print.sysPrintln(sb.toString());

        }

        /* end */
        Print.sysPrintln("</Pushpins>");
        Print.sysPrintln("");
        System.exit(0);

    }

}
