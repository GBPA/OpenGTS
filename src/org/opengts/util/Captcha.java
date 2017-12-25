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
//  2014/11/30  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;

/**
*** Captcha Image Generator
**/

public class Captcha
{

    // ------------------------------------------------------------------------

    public static final String  DEFAULT_ALPHA           = "abcdefghijkmnpqrstuvwxyz23456789";
    public static final boolean DEFAULT_IGNORE_CASE     = true;
    public static final int     DEFAULT_SYMBOL_COUNT    = 5;
    public static final int     DEFAULT_WIDTH           = 200;
    public static final int     DEFAULT_HEIGHT          = 80;
    
    public static final String  DEFAULT_FONT_NAME       = "Monospaced";
    public static final int     DEFAULT_FONT_STYLE      = Font.BOLD;
    public static final int     DEFAULT_FONT_SIZE       = 36;

    // ------------------------------------------------------------------------

    private Random  random      = null;

    private String  alphabet    = DEFAULT_ALPHA;
    private boolean ignoreCase  = DEFAULT_IGNORE_CASE;
    private int     symbCount   = DEFAULT_SYMBOL_COUNT;
    
    private String  fontName    = DEFAULT_FONT_NAME;
    private int     fontStyle   = DEFAULT_FONT_STYLE;
    private int     fontSize    = DEFAULT_FONT_SIZE;

    private String  value       = null;

    /**
    *** Constructor
    **/
    public Captcha()
    {
        this(DEFAULT_ALPHA, DEFAULT_IGNORE_CASE, DEFAULT_SYMBOL_COUNT);
    }

    /**
    *** Constructor
    **/
    public Captcha(int symCount)
    {
        this(DEFAULT_ALPHA, DEFAULT_IGNORE_CASE, symCount);
    }

    /**
    *** Constructor
    **/
    public Captcha(String alphabet, boolean ignoreCase, int symCount)
    {
        this.alphabet   = !StringTools.isBlank(alphabet)? alphabet : DEFAULT_ALPHA;
        this.ignoreCase = ignoreCase;
        this.symbCount  = (symCount >= 1)? symCount : DEFAULT_SYMBOL_COUNT;
        Random R = this.getRandom();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < this.symbCount; i++) {
            sb.append(this.alphabet.charAt(R.nextInt(this.alphabet.length())));
        }
        this.value = sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Captcha value randomizer
    **/
    protected Random getRandom()
    {
        if (this.random == null) {
            this.random = new Random();
        }
        return this.random;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the captch font
    **/
    protected Font getFont(int size)
    {
        int fsize = (size > 0)? size : DEFAULT_FONT_SIZE;
        return new Font(this.fontName, this.fontStyle, fsize);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the target value of this captcha
    **/
    public String getValue()
    {
        return this.value;
    }

    // ------------------------------------------------------------------------

    /* draw lines in Graphics context */
    private void _drawLines(int count, Graphics2D G, int width, int height, int maxLineWidth)
    {
        Random R = this.getRandom();
        for (int i = 0; i < count; i++) {
            int ca = 200, cb = 255 - ca;
            Color lineC = new Color(R.nextInt(ca)+cb, R.nextInt(ca)+cb, R.nextInt(ca)+cb); // ColorTools.randomColor(R);
            G.setColor(lineC);
            G.setStroke(new BasicStroke((float)R.nextInt(maxLineWidth) + 1.0F));
            int aX = R.nextInt((width / 2) - 4) + 2;
            int aY = R.nextInt(height - 4) + 2;
            int bX = width - R.nextInt((width / 2) - 4) + 2;
            int bY = R.nextInt(height - 4) + 2;
            if (R.nextBoolean()) {
                // -- line
                G.drawLine(aX, aY, bX, bY);
            } else {
                // -- oval
                int oW = bX - aX;
                int oH = bY - aY;
                G.drawOval(aX, aY, oW, oH);
            }
        }
    }

    /**
    *** Writes the captcha image to the specified OutputStream.
    *** If null is specified for the OutputStream, and new ByteArrayOutputStream will be created.
    *** @param bgColor  The background color
    *** @param width    The captcha image width
    *** @param height   The captcha image height
    *** @param output   The OutputStream to which the image is written
    *** @return The OutputStream to which the image was written
    **/
    public OutputStream writeImage(
        Color bgColor,
        int width, int height,
        OutputStream output)
    {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random R = this.getRandom();
        Color BG = (bgColor != null)? bgColor : Color.gray;

        /* output stream */
        OutputStream out = (output != null)? output : new ByteArrayOutputStream();

        /* create/write image */
        Graphics2D G = null;
        try {
    
            /* init graphics context */
            G = image.createGraphics();
            G.setColor(BG);
            G.fillRect(0, 0, width, height); // fill with background
    
            /* Font metrics */
            G.setFont(this.getFont(height - 14));
            FontMetrics FM = G.getFontMetrics();
            String val = this.getValue();
            int fmW = FM.stringWidth(val);
            int fmD = FM.getDescent();
            int fmA = FM.getMaxAscent();

            /* pre-draw random lines, in random colors */
            this._drawLines(10, G, width, height, 5);
    
            /* draw value */
            int overlap = 4;
            int X = (width  / 2) - (fmW / 2) + ((val.length() * overlap) / 2);
            int Y = (height / 2) + (fmA / 2);
            G.setColor(ColorTools.randomColor(R));
            int xp = X;
            for (int i = 0; i < val.length(); i++) {
                AffineTransform AT = new AffineTransform();
                AT.rotate(Math.toRadians((double)(((R.nextInt(40) - 20) + 360) % 360)));
                G.setFont(this.getFont(height - 14).deriveFont(AT));
                G.setColor(new Color(R.nextInt(80), R.nextInt(80), R.nextInt(80)));
                int yp = Y + (R.nextInt(8) - 4); // adjust up/down
                String v = val.substring(i, i+1);
                G.drawString(v, xp, yp);
                xp += FM.stringWidth(v) - overlap;
            }

            /* post-draw random lines, in random colors */
            this._drawLines(4, G, width, height, 2);

            /* draw border */
            G.setColor(Color.BLACK);
            G.setStroke(new BasicStroke(1.0F));
            G.drawLine(0      ,0       ,0      ,width   );
            G.drawLine(0      ,0       ,width  ,0       );
            G.drawLine(0      ,height-1,width  ,height-1);
            G.drawLine(width-1,height-1,width-1,0       );

            /* write to output stream */
            ImageIO.write(image, "png", out); // may throw IOException

        } catch (IOException ioe) {

            Print.logException("Unable to create Captcha", ioe);
            out = null;

        } catch (Throwable th) {

            Print.logException("Unable to create Captcha", th);
            out = null;

        } finally {

            /* free Graphics resources */
            if (G != null) {
                G.dispose();
            }

        }

        /* return OutputStream */
        return out;

    }

    // ------------------------------------------------------------------------

    /**
    *** Creates/returns the Captch image as a byte array
    *** @return The image byte array
    **/
    public byte[] getImageBytes()
    {
        return this.getImageBytes(120, 50);
    }

    /**
    *** Creates/returns the Captch image as a byte array
    *** @param width    The captcha image width
    *** @param height   The captcha image height
    *** @return The image byte array
    **/
    public byte[] getImageBytes(int width, int height)
    {
        return this.getImageBytes(Color.lightGray, width, height);
    }

    /**
    *** Creates/returns the Captch image as a byte array
    *** @param bgColor  The background color
    *** @param width    The captcha image width
    *** @param height   The captcha image height
    *** @return The image byte array
    **/
    public byte[] getImageBytes(
        Color bgColor,
        int width, int height)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream out = this.writeImage(bgColor, width, height, baos);
        return (out != null)? baos.toByteArray() : null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String ARG_OUTPUT[]      = { "output", "out" };
    private static String ARG_COUNT[]       = { "count"         };
    private static String ARG_WIDTH[]       = { "width" , "w"   };
    private static String ARG_HEIGHT[]      = { "height", "h"   };

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        File outFile  = RTConfig.getFile(ARG_OUTPUT,null);
        int  symCount = RTConfig.getInt( ARG_COUNT , DEFAULT_SYMBOL_COUNT);
        int  width    = RTConfig.getInt( ARG_WIDTH , DEFAULT_WIDTH);
        int  height   = RTConfig.getInt( ARG_HEIGHT, DEFAULT_HEIGHT);

        /* verify output file */
        if (outFile == null) {
            Print.sysPrintln("'output' not specified");
            System.exit(1);
        } else
        if (outFile.exists()) {
            Print.sysPrintln("Output file already exists: " + outFile);
            System.exit(1);
        }

        /* create captcha */
        Captcha captcha = new Captcha(symCount);

        /* get image */
        Color bgColor = Color.lightGray; // ColorTools.randomColor();
        byte imageB[] = captcha.getImageBytes(bgColor, width, height);
        if (imageB != null) {
            try {
                boolean ok = FileTools.writeFile(imageB, outFile);
                if (ok) {
                    Print.sysPrintln("Captcha written: " + outFile);
                    System.exit(0);
                } else {
                    Print.sysPrintln("Unable to write Captcha: " + outFile);
                    System.exit(3);
                }
            } catch (IOException ioe) {
                Print.logException("Error writing Captcha",ioe);
                System.exit(4);
            }
        } else {
            Print.sysPrintln("Unable to write Captcha image");
            System.exit(2);
        }

    }

}
