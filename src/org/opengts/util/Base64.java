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
// Description:
//  Base64 encoding/decoding
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2007/09/16  Martin D. Flynn
//     -Changed to allow overriding the alphabet used to encode bytes.
//  2009/01/28  Martin D. Flynn
//     -Added command-line encode/decode options.
//  2012/04/20  Martin D. Flynn
//     -Added "Base64HttpAlpha" Http-URL save alphabet.
//     -Added Alphabet option to shuffle.
//  2017/10/09  Martin D. Flynn
//     -Renamed "Base64HttpAlpha" to "Base64UrlAlpha"
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.math.BigInteger;

/**
*** Base64 encoding/decoding tools
**/

public class Base64
{

    // ------------------------------------------------------------------------

    /**
    *** The Base64 padding character
    **/
    public static final char Base64Pad = '=';

    /**
    *** The Standard Base64 alphabet
    **/
    public static final char Base64Alphabet[] = {
        'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
        'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
        '0','1','2','3','4','5','6','7','8','9','+','/',
    };

    /**
    *** An HTTP/URL-Safe Base64 alphabet
    **/
    public static final char Base64UrlAlpha[] = {
        'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
        'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
        '0','1','2','3','4','5','6','7','8','9','-','_',
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Base64 Decode Parse Exception
    **/
    public static class Base64DecodeException
        extends Exception
    {
        public Base64DecodeException(String msg) {
            super(msg);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** return a shuffled Base64 alphabet
    *** @param seed  A large number used as a randomizer seed to shuffle the alphabet
    *** @return The shuffled alphabet
    **/
    public static char[] shuffleAlphabet(long seed)
    {
        return Base64.shuffleAlphabet(seed, Base64Alphabet);
    }

    /**
    *** return a shuffled Base64 alphabet
    *** @param seed  A large number used as a randomizer seed to shuffle the alphabet
    *** @param alpha Alphabet to shuffle
    *** @return The shuffled alphabet
    **/
    public static char[] shuffleAlphabet(long seed, char alpha[])
    {
        if (alpha == null) {
            return null;
        } else {
            char newAlpha[] = new char[alpha.length]; // Base64Alphabet.length
            System.arraycopy(alpha,0,newAlpha,0,newAlpha.length);
            return ListTools.shuffle(newAlpha, seed);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** return a shuffled Base64 alphabet
    *** @param seed  A large number used as a randomizer seed to shuffle the alphabet
    *** @return The shuffled alphabet
    **/
    public static char[] shuffleAlphabet(BigInteger seed)
    {
        return Base64.shuffleAlphabet(seed, Base64Alphabet);
    }

    /**
    *** return a shuffled Base64 alphabet
    *** @param seed  A large number used as a randomizer seed to shuffle the alphabet
    *** @param alpha Alphabet to shuffle
    *** @return The shuffled alphabet
    **/
    public static char[] shuffleAlphabet(BigInteger seed, char alpha[])
    {
        char newAlpha[] = new char[Base64Alphabet.length];
        System.arraycopy(alpha,0,newAlpha,0,newAlpha.length);
        return ListTools.shuffle(newAlpha, seed);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a character from the specified alphabet based on the specified index
    *** @param ndx    The character array index
    *** @param alpha  The character array alphabet
    *** @return The character at the specified index
    **/
    protected static char _encodeChar(int ndx, char alpha[])
    {
        return alpha[ndx];
    }

    // ----------------------------------------

    /**
    *** Encodes the specified String based on the Base64 alphabet 
    *** @param str  The String to encode
    *** @return  The Base64 encoded String
    **/
    public static String encode(String str)
    {
        return (str != null)? Base64.encode(str.getBytes()) : "";
    }

    /**
    *** Encodes the specified String based on the Base64 alphabet 
    *** @param str  The String to encode
    *** @param seed The randomizer seed used to shuffle the alphabet
    *** @return  The Base64 encoded String
    **/
    public static String encode(String str, long seed)
    {
        return (str != null)? Base64.encode(str.getBytes(),seed) : "";
    }

    /**
    *** Encodes the specified String based on the Base64 alphabet 
    *** @param str  The String to encode
    *** @param seed The randomizer seed used to shuffle the alphabet
    *** @param pad  The padding character
    *** @return  The Base64 encoded String
    **/
    public static String encode(String str, long seed, char pad)
    {
        return (str != null)? Base64.encode(str.getBytes(),seed,pad) : "";
    }

    /**
    *** Encodes the specified String based on the Base64 alphabet 
    *** @param str  The String to encode
    *** @param seed The randomizer seed used to shuffle the alphabet
    *** @return  The Base64 encoded String
    **/
    public static String encode(String str, BigInteger seed)
    {
        return (str != null)? Base64.encode(str.getBytes(),seed) : "";
    }

    /**
    *** Encodes the specified String based on the Base64 alphabet 
    *** @param str  The String to encode
    *** @param seed The randomizer seed used to shuffle the alphabet
    *** @param pad  The padding character
    *** @return  The Base64 encoded String
    **/
    public static String encode(String str, BigInteger seed, char pad)
    {
        return (str != null)? Base64.encode(str.getBytes(),seed,pad) : "";
    }

    // ----------------------------------------

    /**
    *** Encodes the specified String based on the Base64URL alphabet 
    *** @param str  The String to encode
    *** @return  The Base64URL encoded String
    **/
    public static String encodeURL(String str)
    {
        return (str != null)? Base64.encode(str.getBytes(),Base64UrlAlpha,'\0') : "";
    }

    // ----------------------------------------

    /**
    *** Encodes the specified byte array based on the Base64 alphabet
    *** @param buff  The byte array to encode
    *** @return  The Base64 encoded byte array
    **/
    public static String encode(byte buff[])
    {
        return Base64.encode(buff, Base64Alphabet, Base64Pad);
    }

    /**
    *** Encodes the specified byte array based on the specified alphabet
    *** @param buff  The byte array to encode
    *** @param alpha The alphabet used to encode the byte array
    *** @return The encoded byte array
    **/
    public static String encode(byte buff[], char alpha[])
    {
        return Base64.encode(buff, alpha, Base64Pad);
    }

    /**
    *** Encodes the specified byte array based on the specified alphabet
    *** @param buff  The byte array to encode
    *** @param alpha The alphabet used to encode the byte array
    *** @param pad   The padding character
    *** @return The encoded byte array
    **/
    public static String encode(byte buff[], char alpha[], char pad)
    {

        /* no buff specified? */
        if ((buff == null) || (buff.length == 0)) {
            return "";
        }

        /* default alphabet */
        if (alpha == null) { alpha = Base64Alphabet; }

        /* encoded string buffer */
        StringBuffer sb = new StringBuffer();
        int len = buff.length;

        /* encode byte array */
        for (int i = 0; i < len; i += 3) {
            // 00000000 00000000 00000000
            // 10000010 00001000 00100000

            /* place next 3 bytes into register */
            int              reg24  = ((int)buff[i  ] << 16) & 0xFF0000;
            if ((i+1)<len) { reg24 |= ((int)buff[i+1] <<  8) & 0x00FF00; }
            if ((i+2)<len) { reg24 |= ((int)buff[i+2]      ) & 0x0000FF; }

            /* encode data 6 bits at a time */
            // -- char #1
            sb.append(    alpha[(reg24 >>> 18) & 0x3F]);
            // -- char #2
            sb.append(    alpha[(reg24 >>> 12) & 0x3F]);
            // -- char #3
            if ((i + 1) < len) {
                sb.append(alpha[(reg24 >>>  6) & 0x3F]);
            } else 
            if (pad != '\0') {
                sb.append(pad);
            }
            // -- char #4
            if ((i + 2) < len) {
                sb.append(alpha[(reg24       ) & 0x3F]);
            } else 
            if (pad != '\0') {
                sb.append(pad);
            }

        }

        /* return encoded string */
        return sb.toString();

    }

    // ----------------------------------------

    /**
    *** Encodes the specified byte array based on a shuffled Base64 alphabet
    *** @param buff  The byte array to encode
    *** @param seed  The randomizer seed used to shuffle the alphabet
    *** @return The encoded byte array
    **/
    public static String encode(byte buff[], long seed)
    {
        char alpha[] = Base64.shuffleAlphabet(seed);
        return Base64.encode(buff, alpha, Base64Pad);
    }

    /**
    *** Encodes the specified byte array based on a shuffled Base64 alphabet
    *** @param buff  The byte array to encode
    *** @param seed  The randomizer seed used to shuffle the alphabet
    *** @param pad   The padding character
    *** @return The encoded byte array
    **/
    public static String encode(byte buff[], long seed, char pad)
    {
        char alpha[] = Base64.shuffleAlphabet(seed);
        return Base64.encode(buff, alpha, pad);
    }

    /**
    *** Encodes the specified byte array based on a shuffled Base64 alphabet
    *** @param buff  The byte array to encode
    *** @param seed  The randomizer seed used to shuffle the alphabet
    *** @return The encoded byte array
    **/
    public static String encode(byte buff[], BigInteger seed)
    {
        char alpha[] = Base64.shuffleAlphabet(seed);
        return Base64.encode(buff, alpha, Base64Pad);
    }

    /**
    *** Encodes the specified byte array based on a shuffled Base64 alphabet
    *** @param buff  The byte array to encode
    *** @param seed  The randomizer seed used to shuffle the alphabet
    *** @param pad   The padding character
    *** @return The encoded byte array
    **/
    public static String encode(byte buff[], BigInteger seed, char pad)
    {
        char alpha[] = Base64.shuffleAlphabet(seed);
        return Base64.encode(buff, alpha, pad);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the index of the specified character in the specified array
    *** @param ch    The target character for which the index is returned
    *** @param alpha The character alphabet
    *** @return The index of the target character in the specified alphabet
    **/
    protected static int _decodeChar(char ch, char alpha[]) 
        throws Base64DecodeException
    {
        for (int i = 0; i < alpha.length; i++) {
            if (ch == alpha[i]) {
                return i;
            }
        }
        // -- invalid character found
        throw new Base64DecodeException("Invalid char: " + ch);
    }

    /**
    *** Decodes the specified String based on the Base64 alphabet
    *** @param b64Str  The String to decode
    *** @return The decoded byte array
    **/
    public static byte[] decode(String b64Str)
        throws Base64DecodeException
    {
        return Base64.decode(b64Str, Base64Alphabet, Base64Pad);
    }

    /**
    *** Decodes the specified String based on the specified alphabet
    *** @param b64Str  The String to decode
    *** @param alpha   The character alphabet used to decode the String
    *** @return The decoded byte array
    **/
    public static byte[] decode(String b64Str, char alpha[])
        throws Base64DecodeException
    {
        return Base64.decode(b64Str, alpha, Base64Pad);
    }

    /**
    *** Decodes the specified String based on the specified alphabet
    *** @param b64Str  The String to decode
    *** @param alpha   The character alphabet used to decode the String
    *** @param pad   The padding character
    *** @return The decoded byte array
    **/
    public static byte[] decode(String b64Str, char alpha[], char pad)
        throws Base64DecodeException
    {

        /* default alphabet */
        if (alpha == null) { alpha = Base64Alphabet; }

        /* validate Base64 String */
        if ((b64Str == null) || b64Str.equals("")) {
            return new byte[0];
        }

        /* encoded string buffer - stip pad */
        int len = b64Str.length();
        while ((len > 0) && (b64Str.charAt(len - 1) == pad)) { len--; }
        
        /* output buffer length */
        // XX==, XXX=, XXXX, XXXXXX==
        int b = 0, blen = (((len - 1) / 4) * 3) + ((len - 1) % 4);
        if (((len - 1) % 4) == 0) {
            // the encoded Base64 String has an invalid length
            blen++; // allow for an extra byte
        }
        byte buff[] = new byte[blen]; 
        // 1=?0, 2=1, 3=2, 4=3, 5=?3, 6=4, 7=5, 8=6, 9=?6, 10=7, ...
        
        for (int i = 0; i < len; i += 4) {
            
            /* place next 4 characters into a 24-bit register */
            int              reg24  = (_decodeChar(b64Str.charAt(i  ),alpha) << 18) & 0xFC0000;
            if ((i+1)<len) { reg24 |= (_decodeChar(b64Str.charAt(i+1),alpha) << 12) & 0x03F000; }
            if ((i+2)<len) { reg24 |= (_decodeChar(b64Str.charAt(i+2),alpha) <<  6) & 0x000FC0; }
            if ((i+3)<len) { reg24 |= (_decodeChar(b64Str.charAt(i+3),alpha)      ) & 0x00003F; }

            /* decode register into 3 bytes */
                             buff[b++] = (byte)((reg24 >>> 16) & 0xFF);
            if ((i+2)<len) { buff[b++] = (byte)((reg24 >>>  8) & 0xFF); }
            if ((i+3)<len) { buff[b++] = (byte)((reg24       ) & 0xFF); }

        }
        
        return buff;
        
    }

    /**
    *** Decodes the specified String based on a scambled Base64 alphabet
    *** @param b64Str  The String to decode
    *** @param seed    The randomizer seed used to shuffle the alphabet
    *** @return The decoded byte array
    **/
    public static byte[] decode(String b64Str, long seed)
        throws Base64DecodeException
    {
        char alpha[] = Base64.shuffleAlphabet(seed);
        return Base64.decode(b64Str, alpha, Base64Pad);
    }

    /**
    *** Decodes the specified String based on a scambled Base64 alphabet
    *** @param b64Str  The String to decode
    *** @param seed    The randomizer seed used to shuffle the alphabet
    *** @return The decoded byte array
    **/
    public static byte[] decode(String b64Str, BigInteger seed)
        throws Base64DecodeException
    {
        char alpha[] = Base64.shuffleAlphabet(seed);
        return Base64.decode(b64Str, alpha, Base64Pad);
    }

    // --------------------------------

    /**
    *** Decodes the specified String based on the Base64Url alphabet
    *** @param b64UrlStr  The String to decode
    *** @return The decoded byte array
    **/
    public static byte[] decodeURL(String b64UrlStr)
        throws Base64DecodeException
    {
        if (StringTools.isBlank(b64UrlStr)) {
            // -- nothing to decode
            return new byte[0];
        } else {
            char pad = '\0'; // padding should be omitted, but COULD be '=' or '.'
            char lch = b64UrlStr.charAt(b64UrlStr.length() - 1);
            if ("=.".indexOf(lch) >= 0) {
                pad = lch; // last char is padding
            }
            return Base64.decode(b64UrlStr, Base64UrlAlpha, pad);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_DECODE[]    = new String[] { "decode" , "d"  };
    private static final String ARG_ENCODE[]    = new String[] { "encode" , "e"  };
    private static final String ARG_SEED[]      = new String[] { "seed"   , "s"  };
    private static final String ARG_OUTPUT[]    = new String[] { "output" , "toFile", "out" };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + Base64.class.getName() + " {options}");
        Print.logInfo("Options:");
        Print.logInfo("  -decode=<Base64>   Decode Base64 string to ASCII");
        Print.logInfo("  -encode=<ASCII>    Encode ASCII string to Base64");
        System.exit(1);
    }

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        long seed = RTConfig.getLong(ARG_SEED,0L);

        /* decode Base64 strings */
        if (RTConfig.hasProperty(ARG_DECODE)) {
            String b64 = RTConfig.getString(ARG_DECODE,"");
            // -- read from file?
            if (b64.startsWith("file:")) {
                java.io.File file = new java.io.File(b64.substring("file:".length()));
                byte data[] = FileTools.readFile(file);
                b64 = StringTools.toStringValue(data).trim(); // convert to string
                b64 = StringTools.stripChars(b64,"\n\r\t "); // remove CR,NL,TAB/SPACE chars
                Print.sysPrintln("Decoding:\n" + b64);
            }
            // -- decode
            try {
                byte b[] = (seed != 0L)? Base64.decode(b64,seed) : Base64.decode(b64);
                Print.sysPrintln("Hex  : 0x" + StringTools.toHexString(b));
                Print.sysPrintln("ASCII: " + StringTools.toStringValue(b,'.'));
                // -- output to file?
                String fileStr = RTConfig.getString(ARG_OUTPUT,null);
                if (!StringTools.isBlank(fileStr)) {
                    java.io.File file = new java.io.File(fileStr);
                    if (file.exists()) {
                        Print.sysPrintln("ERROR: output file already exists - " + file);
                        System.exit(1);
                    }
                    try {
                        FileTools.writeFile(b, file, false);
                    } catch (Throwable ioe) {
                        Print.sysPrintln("ERROR: Unable to write to file - " + file);
                        System.exit(1);
                    }
                }
                System.exit(0);
            } catch (Base64DecodeException bde)  {
                Print.logException("Invalid data", bde);
                System.exit(1);
            }
        }

        /* encode Base64 strings */
        if (RTConfig.hasProperty(ARG_ENCODE)) {
            String ascii = RTConfig.getString(ARG_ENCODE,"");
            byte b[] = ascii.startsWith("0x")? StringTools.parseHex(ascii,new byte[0]) : ascii.getBytes();
            String b64 = (seed > 0L)? Base64.encode(b,seed) : Base64.encode(b);
            Print.sysPrintln("Base64: " + b64);
            System.exit(0);
        }

        /* no options */
        usage();
        
    }
    
}
