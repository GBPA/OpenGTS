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
//  This class provides many File based utilities
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/05/14  Martin D. Flynn
//     -Added method 'writeEscapedUnicode'
//  2009/04/02  Martin D. Flynn
//     -Added command-line "-strings=<len>" option for display string content 
//      from an input file specified with "-file=<file>"
//  2009/05/24  Martin D. Flynn
//     -Added "copyFile" method
//     -Changed command-line arguments.
//  2009/06/01  Martin D. Flynn
//     -Added 'toFile(URL)' method
//  2010/01/29  Martin D. Flynn
//     -Added additional 'toFile' methods
//  2011/01/28  Martin D. Flynn
//     -Added '-tofile' option to command-line '-wget' command.
//  2011/04/01  Martin D. Flynn
//     -Added "canExecute", "canRead", and "canWrite"
//     -Added "traverseAllFiles"
//  2011/05/13  Martin D. Flynn
//     -Added "findPatternInFile"
//  2011/06/16  Martin D. Flynn
//     -Added "getFiles" with regex matcher, and simulated file glob
//  2011/08/21  Martin D. Flynn
//     -Fixed "copyStreams" pattern matching issue (thanks to Jan Wedel, YanXu)
//     -Added command-line tool option "-filterLog"
//  2015/05/03  Martin D. Flynn
//     -Added redirect support to "copyFile(URL..." to redirect where the
//      method "setInstanceFollowRedirects" fails (ie. protocol changes, etc).
//  2015/08/16  Martin D. Flynn
//     -Removed reflection checks for "canRead", "canWrite", "canExecute"
//     -Added support for JRE 1.7+ "getAttribute" (using reflection)
//     -Added support for JRE 1.7+ "getFilePath" (calls "toPath" via reflection)
//     -Added support for JRE 1.7+ "isSymbolicLink" (calls "getAttribute")
//     -Added support for JRE 1.7+ "getUserOwner" (calls "getAttribute")
//     -Added support for JRE 1.7+ "getGroupOwner" (calls "getAttribute")
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import java.lang.reflect.Array;

import java.math.*;
import java.security.*;

/**
*** File handling tools
**/

public class FileTools
{

    // ------------------------------------------------------------------------

    private static String ProgressPrefix = "";

    // ------------------------------------------------------------------------

    /**
    *** Convert a File specification to a URL
    *** @param file  The file specification
    *** @return A URL representing the specified file
    *** @throws MalformedURLException
    ***     If a protocol handler for the URL could not be found,
    ***     or if some other error occurred while constructing the URL
    **/
    public static URL toURL(File file)
        throws MalformedURLException
    {
        if (file == null) {
            return null;
        } else {
            return file.toURI().toURL();
        }
    }

    /**
    *** Convert a URL specification to a File
    *** @param url  The URL specification
    *** @return A file representing the specified URL
    *** @throws URIsyntaxException if the protocol was not 'file' or URL could 
    ***     otherwise not be converted to a file
    **/
    public static File toFile(URL url)
        throws URISyntaxException
    {
        if (url == null) {
            return null;
        } else 
        if (!url.getProtocol().equalsIgnoreCase("file")) {
            throw new URISyntaxException(url.toString(), "Invalid protocol (expecting 'file')");
        } else {
            try {
                return new File(url.toURI());
            } catch (IllegalArgumentException iae) {
                return new File(HTMLTools.decodeParameter(url.getPath()));
            }
        }
    }

    /**
    *** Convert a URL specification to a File
    *** @param base  The base file specification
    *** @param path  The file path directories (last element may be a file name)
    *** @return A file representing the specified path
    **/
    public static File toFile(File base, String path[])
    {
        if (base == null) {
            return null;
        } else
        if (ListTools.isEmpty(path)) {
            return base;
        } else {
            File b = base;
            for (int i = 0; i < path.length; i++) {
                b = new File(b, path[i]);
            }
            return b;
        }
    }

    /**
    *** Convert a URL specification to a File
    *** @param path  The file path directories (last element may be a file name)
    *** @return A file representing the specified path
    **/
    public static File toFile(String path[])
    {
        if (ListTools.isEmpty(path)) {
            return null;
        } else {
            File b = new File(path[0]);
            for (int i = 1; i < path.length; i++) {
                b = new File(b, path[i]);
            }
            return b;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the real (canonical) file for the specified file/path
    *** @param file  The file for which the real file will be returned
    *** @param rtnFileOnErr  True to return the specified file if an error occurs when attempting 
    ***         to call 'getCanonicalFile'.  Null may still be returned if the specified file does
    ***         not exist.
    *** @return The real (canonical) file, or null if the file does not exist,
    ***         or if an error occurs
    **/
    public static File getRealFile(File file, boolean rtnFileOnErr)
    {
        if ((file != null) && file.exists()) {
            try {
                // -- NOTE: may not return the actual/real file for Windows "Junction" linked directories.
                return file.getCanonicalFile();
            } catch (IOException ioe) {
                // -- ignore error
                //Print.logException("Unable to obtain RealFile: " + file);
                return rtnFileOnErr? file : null;
            }
        }
        return null;
    }

    /**
    *** Gets the real (canonical) file for the specified file/path
    *** @param dir   The directory containing the specified file name
    *** @param file  The file for which the real file will be returned
    *** @param rtnFileOnErr  True to return the specified file if an error occurs when attempting 
    ***         to call 'getCanonicalFile'.  Null may still be returned if the specified file does
    ***         not exist.
    *** @return The real (canonical) file, or null if the file does not exist,
    ***         or if an error occurs
    **/
    public static File getRealFile(File dir, String file, boolean rtnFileOnErr)
    {
        if ((dir != null) && dir.isDirectory()) {
            if (file == null) {
                return FileTools.getRealFile(dir,rtnFileOnErr);
            } else {
                return FileTools.getRealFile(new File(dir,file),rtnFileOnErr);
            }
        }
        return null;
    }

    // --------------------------------

    /**
    *** Gets the real (canonical) file for the specified file/path
    *** @param file  The file for which the real file will be returned
    *** @return The real (canonical) file, or null if the file does not exist,
    ***         or if an error occurs
    **/
    public static File getRealFile(File file)
    {
        return FileTools.getRealFile(file,false);
    }

    /**
    *** Gets the real (canonical) file for the specified file/path
    *** @param dir   The directory containing the specified file name
    *** @param file  The file for which the real file will be returned
    *** @return The real (canonical) file, or null if the file does not exist,
    ***         or if an error occurs
    **/
    public static File getRealFile(File dir, String file)
    {
        return FileTools.getRealFile(dir,file,false);
    }

    // ------------------------------------------------------------------------
    
    public static final int DEFAULT_URL_COPYFILE_TIMEOUT_MS = 7000;

    /**
    *** Copies an input URL to an output file
    *** @param inpURL  The URL from which data will be read
    *** @param outFile The File to whish data will be written
    *** @return The number of bytes copied
    **/
    public static int copyFile(URL inpURL, File outFile)
        throws IOException
    {
        return FileTools.copyFile(inpURL, outFile, false, DEFAULT_URL_COPYFILE_TIMEOUT_MS);
    }

    /**
    *** Copies an input URL to an output file
    *** @param inpURL    The URL from which data will be read
    *** @param outFile   The File to whish data will be written
    *** @param progress  Show progress
    *** @return The number of bytes copied
    **/
    public static int copyFile(URL inpURL, File outFile, boolean progress)
        throws IOException
    {
        return FileTools.copyFile(inpURL, outFile, progress, DEFAULT_URL_COPYFILE_TIMEOUT_MS);
    }

    /**
    *** Copies an input URL to an output file
    *** @param inpURL    The URL from which data will be read
    *** @param outFile   The File to whish data will be written
    *** @param progress  Show progress
    *** @param timeoutMS The timeout in milliseconds (0 for infinite timeout)
    *** @return The number of bytes copied
    **/
    public static int copyFile(URL inpURL, File outFile, boolean progress, int timeoutMS)
        throws IOException
    {

        /* nothing to copy? */
        if (inpURL == null) {
            // -- nothing to copy "from"
            return 0;
        } else
        if (outFile == null) {
            // -- nothing to copy "to"
            return 0;
        }

        /* copy */
        int      byteCnt = 0;
        OutputStream out = null;
        boolean closeOut = true;
        try {

            /* get output stream (check for stdout/stderr) */
            String outName = outFile.toString();
            if (outName.equalsIgnoreCase("stdout")) {
                //Print.sysPrintln("Output to: stdout");
                out = System.out;
                closeOut = false;
            } else
            if (outName.equalsIgnoreCase("stderr")) {
                //Print.sysPrintln("Output to: stderr");
                out = System.err;
                closeOut = false;
            } else {
                //Print.sysPrintln("Output to: " + outFile);
                out = new FileOutputStream(outFile, false); // IOException
                closeOut = true;
            }

            /* copy (may throw IOException, HTMLTools.HttpIOException) */
            byteCnt = FileTools.copyFile(inpURL, out, progress, timeoutMS);

        } finally {

            /* close */
            if (closeOut && (out != null)) {
                FileTools.closeStream(out); // output
            }

        }

        /* return number of bytes copied */
        return byteCnt;

    }

    // ------------------------------------------------------------------------

    /**
    *** Copies an input URL to an output file
    *** @param inpURL  The URL from which data will be read
    *** @param out     The OutputStream to whish data will be written
    *** @return The number of bytes copied
    **/
    public static int copyFile(URL inpURL, OutputStream out)
        throws IOException
    {
        return FileTools.copyFile(inpURL, out, false/*progress*/, DEFAULT_URL_COPYFILE_TIMEOUT_MS);
    }

    /**
    *** Copies an input URL to an output file
    *** @param inpURL    The URL from which data will be read
    *** @param out       The OutputStream to whish data will be written
    *** @param progress  Show progress
    *** @return The number of bytes copied
    **/
    public static int copyFile(URL inpURL, OutputStream out, boolean progress)
        throws IOException
    {
        return FileTools.copyFile(inpURL, out, progress, DEFAULT_URL_COPYFILE_TIMEOUT_MS);
    }

    /**
    *** Copies an input URL to an output file
    *** @param inpURL    The URL from which data will be read
    *** @param out       The OutputStream to whish data will be written
    *** @param progress  Show progress
    *** @param timeoutMS The timeout in milliseconds (0 for infinite timeout)
    *** @return The number of bytes copied
    **/
    public static int copyFile(URL inpURL, OutputStream out, boolean progress, int timeoutMS)
        throws IOException
    {
        String reqMeth = "get"; // HTMLTools.REQUEST_GET;

        /* nothing to copy? */
        if (inpURL == null) {
            // -- nothing to copy "from"
            return 0;
        } else
        if (out == null) {
            // -- nothing to copy "to"
            return 0;
        }

        /* copy (with retry) */
        for (int R = 0; R < 2; R++) { // max 3 retry attempts (2 redirects)

            int byteCnt = 0;
            URLConnection urlConn = null;
            InputStream uis = null;
            try {

                /* init connection */
                urlConn = inpURL.openConnection();
                urlConn.setAllowUserInteraction(false);
                urlConn.setRequestProperty(HTMLTools.PROP_User_Agent, HTMLTools.getHttpUserAgent());
                if (timeoutMS > 0) {
                    urlConn.setConnectTimeout(timeoutMS); 
                    urlConn.setReadTimeout(timeoutMS); 
                }
                if (urlConn instanceof HttpURLConnection) {
                    HttpURLConnection httpConn = (HttpURLConnection)urlConn;
                    if (reqMeth.equalsIgnoreCase("post")) {
                        httpConn.setRequestMethod(HTMLTools.REQUEST_POST);
                        httpConn.setDoInput(true);
                        httpConn.setDoOutput(true);
                        httpConn.setUseCaches(false);
                    } else {
                        httpConn.setRequestMethod(HTMLTools.REQUEST_GET);
                    }
                    httpConn.setInstanceFollowRedirects(true); // fails on http <==> https
                }

                /* connect */
                urlConn.connect(); // possible NoRouteToHostException, etc.
                uis = urlConn.getInputStream();
                //Print.logInfo("URL InputStream: " + StringTools.className(uis));
                // -- Class: sun.net.www.protocol.http.HttpURLConnection$HttpInputStream

                /* response code */
                int rc = (urlConn instanceof HttpURLConnection)? 
                    ((HttpURLConnection)urlConn).getResponseCode() : 
                    0;
                if ((rc != 0) && (rc/100 != 2/*2xx*/)) {

                    /* 3xx redirect? */
                    // -- may occur even though "setInstanceFollowRedirects" is set true.
                    // -  "setInstanceFollowRedirects" will not redirect if the protocol changes
                    // -  (ie. "http" to "https", "https" to "http", etc).
                    // -  Since we are just trying to download a file here, there should be no
                    // -  security implementations of manually following a redirect.
                    if ((rc == 301) ||    // Moved Permanently
                        (rc == 302) ||    // Found (Moved Temporarily)
                        (rc == 303) ||    // See Other
                        (rc == 307) ||    // Temporary Redirect
                        (rc == 308)   ) { // Permanent Redirect
                        // -- host indicated redirect (and protocol has changed)
                        String newURL = urlConn.getHeaderField("Location"); // redirect URL
                        Print.sysPrintln("REDIRECT : " + newURL);
                        if (!StringTools.isBlank(newURL)) {
                            // -- close current connection, set new URL, and try again
                            FileTools.closeStream(uis); // input
                            if (urlConn instanceof HttpURLConnection) {
                                ((HttpURLConnection)urlConn).disconnect();
                            }
                            try {
                                // -- set new URL and try again
                                inpURL = new URL(newURL);
                                continue;
                            } catch (MalformedURLException mue) {
                                // -- invalid URL specified on redirect
                                return 0;
                            }
                        } else {
                            // -- redirect URL is blank
                            return 0;
                        }
                    }

                    /* check other possible response codes? */
                    Print.sysPrintln("ERROR: code=" + rc);
                    return 0;

                }

                /* copy data */
                int len = urlConn.getContentLength();
                byteCnt = FileTools.copyStreams(uis, out, null/*pattern*/, len, progress);
    
            } catch (IOException ioe) {
                // -- this error may be due to either the input(http) or output(file) streams

                /* connection is not HttpURLConnection (unlikely) */
                if (!(urlConn instanceof HttpURLConnection)) {
                    // -- unlikely, since "inpURL" is likely http/https
                    throw ioe;
                }

                /* wrap IOException in HTMLTools.HttpIOException */
                // -- TODO: should only be wrapped if IOException is due to input
                throw new HTMLTools.HttpIOException(ioe, (HttpURLConnection)urlConn);

            } finally {

                /* close */
                if (uis != null) {
                    FileTools.closeStream(uis); // input
                }
                // -- output stream to be closed by caller

                /* disconnect */
                if (urlConn instanceof HttpURLConnection) {
                    ((HttpURLConnection)urlConn).disconnect();
                }

            }

            /* return number of bytes copied */
            return byteCnt;

        } // retry loop

        /* control does not read here */
        return 0;

    }

    // ------------------------------------------------------------------------
    
    private static  int COPY_STREAMS_BLOCK_SIZE = 200 * 1024;

    /**
    *** Copies bytes from one stream to another
    *** @param input  The InputStream
    *** @param output The OutputStream
    *** @return The number of bytes copied
    *** @throws IOException if an I/O error occurs
    **/
    public static int copyStreams(InputStream input, OutputStream output)
        throws IOException
    {
        return FileTools.copyStreams(input, output, null/*pattern*/, -1/*maxLen*/, false/*progress*/);
    }

    /**
    *** Copies bytes from one stream to another
    *** @param input  The InputStream
    *** @param output The OutputStream
    *** @param maxLen The maximum number of bytes to copy
    *** @return The number of bytes copied
    *** @throws IOException if an I/O error occurs
    **/
    public static int copyStreams(InputStream input, OutputStream output, 
        int maxLen)
        throws IOException
    {
        return FileTools.copyStreams(input, output, null/*pattern*/, maxLen, false/*progress*/);
    }
   
    /**
    *** Copies bytes from one stream to another
    *** @param input    The InputStream
    *** @param output   The OutputStream
    *** @param pattern  The pattern to match, to terminate the copy 
    *** @param maxLen   The maximum number of bytes to copy
    *** @return The number of bytes copied
    *** @throws IOException if an I/O error occurs
    **/
    public static int copyStreams(InputStream input, OutputStream output, 
        byte pattern[], int maxLen)
        throws IOException
    {
        return FileTools.copyStreams(input, output, pattern, maxLen, false/*progress*/);
    }
    
    /**
    *** Copies bytes from one stream to another.<br>
    *** If 'maxLen' is >= '0', then at most 'maxLen' bytes will be written to the output stream.
    *** If 'pattern' is specified, the stream will be scanned for the first occurance of the
    *** matching pattern, however, only 'maxLen' bytes will be written.
    *** to the output stream.
    *** @param input   The InputStream
    *** @param output  The OutputStream
    *** @param pattern The pattern to match, to terminate the copy 
    *** @param maxLen  The maximum number of bytes to copy
    *** @param progress Show progress (to stdout)
    *** @return The number of bytes copied
    *** @throws EOFException if the end of stream is reached before the pattern is found
    *** @throws IOException if an I/O error occurs (on either input or output)
    **/
    public static int copyStreams(InputStream input, OutputStream output, 
        byte pattern[], int maxLen, 
        boolean progress)
        throws EOFException, IOException
    {

        /* copy nothing? */
        if ((input == null) || (output == null) || (maxLen == 0)) {
            return 0;
        }

        /* pattern? */
        boolean hasPattern = false;
        int pndx = 0, plen = 0;;
        if ((pattern != null) && (pattern.length > 0)) {
            hasPattern = true;
            plen = pattern.length;
            //Print.logInfo("Pattern: ["+plen+"] " + StringTools.toStringValue(pattern,'.'));
        }

        /* copy bytes */
        int  stopType  = 0; // 1=eof, 2=maxlen, 3=pattern, 4=inputError, 5=outputError
        long startMS   = DateTime.getCurrentTimeMillis();
        long lastMS    = 0L;
        int  length    = 0; // count of bytes copied
        byte tmpBuff[] = new byte[COPY_STREAMS_BLOCK_SIZE + plen]; // copy block size
        byte ch[]      = new byte[1];
        while (true) {

            /* read length */
            int readLen;
            if (!hasPattern && (maxLen > 0)) {
                readLen = maxLen - length;
                if (readLen <= 0) {
                    break; // done reading
                } else
                if (readLen > COPY_STREAMS_BLOCK_SIZE) {
                    readLen = COPY_STREAMS_BLOCK_SIZE; // max block size
                }
            } else {
                readLen = COPY_STREAMS_BLOCK_SIZE; // not '0'
            }
            // -- 'readLen' is > 0 here

            /* read input stream */
            int cnt;
            try {
                if (hasPattern) {
                    // -- read until pattern (could probably be optimized)
                    cnt = 0;
                    patternMatch:
                    for (;cnt < readLen;) {
                        int c = input.read(ch, 0, 1); // input IOException
                        if (c < 0) {
                            // -- EOF
                            if (cnt <= 0) {
                                // -- if we've read nothing, set 'cnt' to EOF as well
                                cnt = c;
                            }
                            break;
                        } else 
                        if (c == 0) {
                            // -- 'cnt' contains the count we've read so far
                            break;
                        }
                        // -- normal length (c == 1)
                        while (true) {
                            if (ch[0] == pattern[pndx]) {
                                // -- pattern character matched
                                pndx++;
                                if (pndx >= plen) {
                                    break patternMatch; // full pattern matched
                                }
                            } else {
                                // -- start over with pattern matching
                                if (pndx > 0) {
                                    // -- copy current matched portion into buffer
                                    System.arraycopy(pattern, 0, tmpBuff, cnt, pndx); 
                                    cnt += pndx;
                                    pndx = 0;
                                    continue; // recheck current character
                                }
                                tmpBuff[cnt++] = ch[0];
                            }
                            break;
                        }
                    } // for (cnt < readLen)
                } else {
                    // -- read entire block
                    cnt = input.read(tmpBuff, 0, readLen); // input IOException
                }
            } catch (IOException inpIOE) {
                // -- input IOException
                // -  TODO: indicate to caller that this is due to input
                stopType = 6; // outputError
                throw inpIOE;
            }

            /* copy to output stream */
            if (cnt < 0) {
                // -- EOF
                stopType = 1; // EOF
                if (hasPattern) {
                    throw new EOFException("Pattern not found"); // input IOException
                } else
                if (progress && (maxLen > 0) && (length != maxLen)) {
                    Print.logError(ProgressPrefix+"Copy size mismatch: " + maxLen + " --> " + length);
                }
                break;
            } else
            if (cnt > 0) {
                try {
                    if (maxLen < 0) {
                        output.write(tmpBuff, 0, cnt); // output IOException
                        length += cnt;
                    } else
                    if (length < maxLen) {
                        int wrtLen = ((length + cnt) > maxLen)? (maxLen - length) : cnt;
                        output.write(tmpBuff, 0, wrtLen); // output IOException
                        length += wrtLen;
                        if (!hasPattern && (length >= maxLen)) {
                            stopType = 2; // maxlen
                            break; // per 'maxLen', done copying
                        }
                    }
                } catch (IOException outIOE) {
                    // -- output IOException
                    // -  TODO: indicate to caller that this is due to output
                    stopType = 5; // outputError
                    throw outIOE;
                }
            } else {
                //Print.logDebug("Read 0 bytes ... continuing");
            }

            /* pattern matched? */
            if (hasPattern && (pndx >= plen)) {
                stopType = 3; // pattern
                break; // per 'pattern', done copying
            }

            /* show progress */
            if (progress) {
                // -- Copying XXXXX of XXXXX bytes
                long nowMS = DateTime.getCurrentTimeMillis();
                if ((nowMS - lastMS) >= 1000L) {
                    lastMS = nowMS;
                    double elapseMS = (double)(nowMS - startMS);
                    if (maxLen > 0) {
                        double p = (double)length / (double)maxLen;
                        double totalMS = elapseMS / p;
                        //double remainMS = totalMS - elapseMS;
                        Print.sysPrint(ProgressPrefix+"Copying - %7d/%d bytes (%2.0f/%.0f sec, %2.0f%%)\r", 
                            length, maxLen, (elapseMS/1000.0), (totalMS/1000.0), (p*100.0));
                    } else {
                        Print.sysPrint(ProgressPrefix+"Copying - %7d bytes (%2.0f sec)\r", 
                            length, (elapseMS/1000.0));
                    }
                }
            }

        } // while (true)

        /* flush output */
        output.flush(); // output IOException

        /* show final progress */
        if (progress) {
            // -- Copied XXXXX of XXXXX bytes
            double elapseMS = (double)(DateTime.getCurrentTimeMillis() - startMS);
            Print.sysPrintln(ProgressPrefix+"Copied - %7d bytes (%.0f sec)                                     ", 
                length, (elapseMS/1000.0));
        }

        /* return number of bytes copied */
        return length;
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Opens the specified file for reading
    *** @param file  The path of the file to open
    *** @return The opened InputStream
    **/
    public static InputStream openInputFile(String file)
    {
        if ((file != null) && !file.equals("")) {
            return FileTools.openInputFile(new File(file));
        } else {
            return null;
        }
    }

    /**
    *** Opens the specified file for reading
    *** @param file  The file to open
    *** @return The opened InputStream
    **/
    public static InputStream openInputFile(File file)
    {
        try {
            return new FileInputStream(file);
        } catch (IOException ioe) {
            Print.logError("Unable to open file: " + file + " [" + ioe + "]");
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Closes the specified InputStream.
    *** @param in  The InputStream to close
    **/
    public static void closeStream(InputStream in)
    {
        if (in == null) {
            // -- InputStream not specified
        } else
        if (in == System.in) {
            // -- Will not close System.in
        } else {
            try {
                in.close();
            } catch (IOException ioe) {
                //Print.logError("Unable to close stream: " + ioe);
            }
        }
    }

    /**
    *** Closes the specified OutputStream
    *** @param out  The OutputStream to close
    **/
    public static void closeStream(OutputStream out)
    {
        if (out == null) {
            // -- OutputStream not specified
        } else
        if (out == System.out) {
            // -- will not close System.out
            //Print.logWarn("Ignoring close on 'stdout'");
        } else
        if (out == System.err) {
            // -- will not close System.err
            //Print.logWarn("Ignoring close on 'stderr'");
        } else {
            try {
                out.close();
            } catch (IOException ioe) {
                //Print.logError("Unable to close stream: " + ioe);
            }
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns an array of bytes read from the specified InputStream
    *** @param input  The InputStream
    *** @return The array of bytes read from the InputStream
    *** @throws IOException if an I/O error occurs
    **/
    public static byte[] readStream(InputStream input)
        throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FileTools.copyStreams(input, output, null/*pattern*/, -1/*maxLen*/, false/*progress*/);
        return output.toByteArray();
    }

    /** 
    *** Returns an array of bytes read from the specified InputStream
    *** @param input  The InputStream
    *** @param maxLen The maximum number of bytes to read from the stream
    *** @return The array of bytes read from the InputStream
    *** @throws IOException if an I/O error occurs
    **/
    public static byte[] readStream(InputStream input, int maxLen)
        throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FileTools.copyStreams(input, output, null/*pattern*/, maxLen, false/*progress*/);
        return output.toByteArray();
    }

    // ------------------------------------------------------------------------

    /**
    *** Writes a String to the specified OutputStream
    *** @param output  The OutputStream 
    *** @param dataStr The String to write to the OutputStream
    *** @throws IOException if an I/O error occurs
    **/
    public static void writeStream(OutputStream output, String dataStr)
        throws IOException
    {
        byte data[] = dataStr.getBytes();
        output.write(data, 0, data.length);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns an array of bytes read from the specified file
    *** @param file  The file path from which the byte array is read
    *** @return The byte array read from the specified file
    **/
    public static byte[] readFile(String file)
    {
        if ((file != null) && !file.equals("")) {
            return FileTools.readFile(new File(file));
        } else {
            return null;
        }
    }

    /**
    *** Returns an array of bytes read from the specified file
    *** @param file  The file from which the byte array is read
    *** @return The byte array read from the specified file
    **/
    public static byte[] readFile(File file)
    {
        return FileTools.readFile(file, -1/*maxLen*/);
    }

    /**
    *** Returns an array of bytes read from the specified file
    *** @param file   The file from which the byte array is read
    *** @param maxLen The maximum number of bytes to read from the file
    *** @return The byte array read from the specified file
    **/
    public static byte[] readFile(File file, int maxLen)
    {
        if (file == null) {
            return null;
        } else
        if (!file.exists()) {
            Print.logError("File does not exist: " + file);
            return null;
        } else {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                return FileTools.readStream(fis, maxLen);
            } catch (IOException ioe) {
                Print.logError("Unable to read file: " + file + " [" + ioe + "]");
            } finally {
                if (fis != null) { try { fis.close(); } catch (IOException ioe) {/*ignore*/} }
            }
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Reads a single line of characters from the specified InputStream, terminated by
    *** either a newline (\n) or carriage-return (\r)
    *** @param input  The InputStream
    *** @param eol    The specific end-of-line character to search for
    *** @return The line read from the InputStream
    *** @throws EOFException if the end of the input stream is encountered
    *** @throws IOException if an I/O error occurs
    **/
    public static String readLine(InputStream input, char eol)
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        while (true) {
            int ch = input.read();
            if (ch < 0) { // eof
                throw new EOFException("End of InputStream");
            } else
            if (ch == eol) {
                return sb.toString();
            }
            sb.append((char)ch);
        }
    }

    /**
    *** Reads a single line of characters from the specified InputStream, terminated by
    *** either a newline (\n) or carriage-return (\r)
    *** @param input  The InputStream
    *** @return The line read from the InputStream
    *** @throws EOFException if the end of the input stream is encountered
    *** @throws IOException if an I/O error occurs
    **/
    public static String readLine(InputStream input)
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        while (true) {
            int ch = input.read();
            if (ch < 0) { // eof
                throw new EOFException("End of InputStream");
            } else
            if ((ch == '\r') || (ch == '\n')) {
                return sb.toString();
            }
            sb.append((char)ch);
        }
    }

    /**
    *** Reads a single line of characters from the specified InputStream, terminated by
    *** a newline only (\n).  Carriage-returns (\r) are ignored.
    *** @param input  The InputStream
    *** @return The line read from the InputStream
    *** @throws EOFException if the end of the input stream is encountered
    *** @throws IOException if an I/O error occurs
    **/
    public static String readLineNL(InputStream input)
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        while (true) {
            int ch = input.read();
            if (ch < 0) { // eof
                throw new EOFException("End of InputStream");
            } else
            if (ch == '\r') {
                continue;
            } else
            if (ch == '\n') {
                return sb.toString();
            }
            sb.append((char)ch);
        }
    }

    /**
    *** Reads a single line of characters from stdin, terminated by
    *** either a newline (\n) or carriage-return (\r)
    *** @return The line read from stdin
    *** @throws IOException if an I/O error occurs
    **/
    public static String readLine_stdin()
        throws IOException
    {
        // clear characters already in stdin buffer
        while (System.in.available() > 0) { System.in.read(); } 
        // read from stdin
        return FileTools.readLine(System.in);
    }

    /**
    *** Prints a message, and reads a line of text from stdin
    *** @param msg  The message to print
    *** @param dft  The default String returned, if no text was entered
    *** @return The line of text read from stdin
    *** @throws IOException if an I/O error occurs
    **/
    public static String readString_stdin(String msg, String dft)
        throws IOException
    {
        if (msg == null) { msg = ""; }
        Print.sysPrintln(msg + "    [String: default='" + dft + "'] ");
        for (;;) {
            Print.sysPrint("?");
            String line = FileTools.readLine_stdin();
            if (line.equals("")) {
                if (dft != null) {
                    return dft;
                } else {
                    // if there is no default, a non-empty String is required
                    Print.sysPrint("String required, please re-enter] ");
                    continue;
                }
            }
            return line;
        }
    }

    /**
    *** Prints a message, and reads a boolean value from stdin
    *** @param msg  The message to print
    *** @param dft  The default boolean value returned, if no value was entered
    *** @return The boolean value read from stdin
    *** @throws IOException if an I/O error occurs
    **/
    public static boolean readBoolean_stdin(String msg, boolean dft)
        throws IOException
    {
        if (msg == null) { msg = ""; }
        Print.sysPrintln(msg + "    [Boolean: default='" + dft + "'] ");
        for (;;) {
            Print.sysPrint("?");
            String line = FileTools.readLine_stdin().trim();
            if (line.equals("")) {
                return dft;
            } else
            if (!StringTools.isBoolean(line,true)) {
                Print.sysPrint("Boolean required, please re-enter] ");
                continue;
            }
            return StringTools.parseBoolean(line, dft);
        }
    }

    /**
    *** Prints a message, and reads a long value from stdin
    *** @param msg  The message to print
    *** @param dft  The default long value returned, if no value was entered
    *** @return The long value read from stdin
    *** @throws IOException if an I/O error occurs
    **/
    public static long readLong_stdin(String msg, long dft)
        throws IOException
    {
        if (msg == null) { msg = ""; }
        Print.sysPrintln(msg + "    [Long: default='" + dft + "'] ");
        for (;;) {
            Print.sysPrint("?");
            String line = FileTools.readLine_stdin().trim();
            if (line.equals("")) {
                return dft;
            } else
            if (!Character.isDigit(line.charAt(0)) && (line.charAt(0) != '-')) {
                Print.sysPrint("Long required, please re-enter] ");
                continue;
            }
            return StringTools.parseLong(line, dft);
        }
    }

    /**
    *** Prints a message, and reads a double value from stdin
    *** @param msg  The message to print
    *** @param dft  The default double value returned, if no value was entered
    *** @return The double value read from stdin
    *** @throws IOException if an I/O error occurs
    **/
    public static double readDouble_stdin(String msg, double dft)
        throws IOException
    {
        if (msg == null) { msg = ""; }
        Print.sysPrintln(msg + "    [Double: default='" + dft + "'] ");
        for (;;) {
            Print.sysPrint("?");
            String line = FileTools.readLine_stdin().trim();
            if (line.equals("")) {
                return dft;
            } else
            if (!Character.isDigit(line.charAt(0)) && (line.charAt(0) != '-') && (line.charAt(0) != '.')) {
                Print.sysPrint("Double required, please re-enter] ");
                continue;
            }
            return StringTools.parseDouble(line, dft);
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Writes a byte array to the specified file
    *** @param data  The byte array to write to the file
    *** @param file  The file to which the byte array is written
    *** @return True if the bytes were successfully written to the file
    *** @throws IOException if an I/O error occurs
    **/
    public static boolean writeFile(byte data[], File file)
        throws IOException
    {
        return FileTools.writeFile(data, file, false);
    }

    /**
    *** Writes a byte array to the specified file
    *** @param data  The byte array to write to the file
    *** @param file  The file to which the byte array is written
    *** @param append True to append the bytes to the file, false to overwrite.
    *** @return True if the bytes were successfully written to the file
    *** @throws IOException if an error occurred.
    **/
    public static boolean writeFile(byte data[], File file, boolean append)
        throws IOException
    {
        if ((data != null) && (file != null)) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file, append);
                fos.write(data, 0, data.length);
                return true;
            } finally {
                try { fos.close(); } catch (Throwable t) {/* ignore */}
            }
        } 
        return false;
    }

    /**
    *** Writes a String to the specified file in "ISO-8859-1" character encoding.<br>
    *** Unicode characters are escaped using the '\u0000' format.
    *** @param dataStr  The String to write to the file
    *** @param file     The file to which the byte array is written
    *** @return True if the String was successfully written to the file
    *** @throws IOException if an error occurred.
    **/
    public static boolean writeEscapedUnicode(String dataStr, File file)
        throws IOException
    {
        boolean append = false;
        if ((dataStr != null) && (file != null)) {
            FileOutputStream fos = new FileOutputStream(file, append);
            BufferedWriter fbw = null;
            try {
                fbw = new BufferedWriter(new OutputStreamWriter(fos, "8859_1"));
                int len = dataStr.length();
                for (int i = 0; i < len; i++) {
                    char ch = dataStr.charAt(i);
                    if ((ch == '\n') || (ch == '\r')) {
                        fbw.write(ch);
                    } else
                    if ((ch == '\t') || (ch == '\f')) {
                        fbw.write(ch);
                    } else
                    if ((ch < 0x0020) || (ch > 0x007e)) {
                        fbw.write('\\');
                        fbw.write('u');
                        fbw.write(StringTools.hexNybble((ch >> 12) & 0xF));
                        fbw.write(StringTools.hexNybble((ch >>  8) & 0xF));
                        fbw.write(StringTools.hexNybble((ch >>  4) & 0xF));
                        fbw.write(StringTools.hexNybble( ch        & 0xF));
                    } else {
                        fbw.write(ch);
                    }
                }
                return true;
            } finally {
                try { fbw.close(); } catch (Throwable t) {/* ignore */}
            }
        } 
        return false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Search (grep) for the specified pattern in the specified file
    *** @param file         The target file
    *** @param pattern      The pattern String
    *** @param ignoreCase   True to ignore case
    *** @return A list of matches, or null if no match was found
    **/
    public static java.util.List<String> findPatternInFile(File file, String pattern, boolean ignoreCase)
    {

        /* invalid file */
        if ((file == null) || !file.isFile()) {
            return null;
        }

        /* invalid pattern */
        if ((pattern == null) || pattern.equals("")) { // spaces are significant (do not use "StringTools.isBlank")
            return null;
        }

        /* open/read file */
        String p = ignoreCase? pattern.toLowerCase() : pattern;
        Vector<String> matchList = new Vector<String>();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            for (;;) {
                String line = FileTools.readLine(fis);
                if (line == null) { break; }
                if (ignoreCase) {
                    if (line.toLowerCase().indexOf(p) >= 0) {
                        matchList.add(line);
                    }
                } else {
                    if (line.indexOf(p) >= 0) {
                        matchList.add(line);
                    }
                }
            }
        } catch (FileNotFoundException fnfe) {
            return null;
        } catch (EOFException eofe) {
            // ignore
        } catch (IOException ioe) {
            Print.logError("Error reading file: " + file + " [" + ioe + "]");
            return null;
        } finally {
            if (fis != null) { try {fis.close();} catch (Throwable th) {/*ignore*/} }
        }

        /* return match list */
        return (matchList.size() > 0)? matchList : null;

    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the extension characters from the specified file name
    *** @param filePath  The file name
    *** @return The extension characters (does not return null)
    **/
    public static String getExtension(String filePath) 
    {
        if (filePath != null) {
            return getExtension(new File(filePath));
        }
        return "";
    }

    /** 
    *** Gets the shortest possible extension characters from the specified file.
    *** IE. "file.aa.bb.cc" would return extension "cc".
    *** @param file  The file
    *** @return The extension characters (does not return null)
    **/
    public static String getExtension(File file) 
    {
        if (file != null) {
            String fileName = file.getName();
            int p = fileName.lastIndexOf(".");
            if ((p >= 0) && (p < (fileName.length() - 1))) {
                return fileName.substring(p + 1);
            }
        }
        return "";
    }

    /**
    *** Returns true if the specified file path has an extension which matches one of the
    *** extensions listed in the specified String array
    *** @param filePath  The file path/name
    *** @param extn      An array of file extensions
    *** @return True if the specified file path has a matching exention
    **/
    public static boolean hasExtension(String filePath, String... extn)
    {
        if (filePath != null) {
            return hasExtension(new File(filePath), extn);
        }
        return false;
    }

    /**
    *** Returns true if the specified file has an extension which matches one of the
    *** extensions listed in the specified String array
    *** @param file      The file
    *** @param extn      An array of file extensions
    *** @return True if the specified file has a matching exention
    **/
    public static boolean hasExtension(File file, String... extn)
    {
        if ((file != null) && (extn != null)) {
            String e = getExtension(file);
            for (int i = 0; i < extn.length; i++) {
                if (e.equalsIgnoreCase(extn[i])) { return true; }
            }
        }
        return false;
    }

    /**
    *** Removes the extension from the specified file path
    *** @param filePath  The file path from which the extension will be removed
    *** @return The file path with the extension removed
    **/
    public static String removeExtension(String filePath)
    {
        if (filePath != null) {
            return removeExtension(new File(filePath));
        }
        return filePath;
    }

    /**
    *** Removes the extension from the specified file
    *** @param file  The file from which the extension will be removed
    *** @return The file path with the extension removed
    **/
    public static String removeExtension(File file)
    {
        if (file != null) {
            String fileName = file.getName();
            int p = fileName.indexOf(".");
            if (p > 0) { // '.' in column 0 not allowed
                file = new File(file.getParentFile(), fileName.substring(0, p));
            }
            return file.getPath();
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified character is a file separator
    *** @param ch  The character to test
    *** @return True if the specified character is a file separator
    **/
    public static boolean isFileSeparatorChar(char ch)
    {
        if (ch == File.separatorChar) {
            // simple test, matches Java's understanding of a file path separator
            return true;
        } else 
        if (OSTools.isWindows() && (ch == '/')) {
            // '/' can be used as a file path separator on Windows
            return true;
        } else {
            // not a file path separator character
            return false;
        }
    }

    /**
    *** Returns true if the specified String contains a file separator
    *** @param fn  The String file path
    *** @return True if the file String contains a file separator
    **/
    public static boolean hasFileSeparator(String fn)
    {
        if (fn == null) {
            // no string, no file separator
            return false;
        } else
        if (fn.indexOf(File.separator) >= 0) {
            // simple test, matches Java's understanding of a file path separator
            return true;
        } else
        if (OSTools.isWindows() && (fn.indexOf('/') >= 0)) {
            // '/' can be used as a file path separator on Windows
            return true;
        } else {
            // no file path separator found
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Resolves specified command relative to the environment "PATH" variable
    *** @param cmd  The command name.  If the specified command is an absolute path,
    ***             then the specified command path will me returned as-is.
    *** @return The resolved command path
    **/
    public static File resolveCommand(String cmd)
    {
        if (StringTools.isBlank(cmd)) {
            return null;
        } else {
            File cmdFile = new File(cmd);
            if (cmdFile.isAbsolute()) {
                return cmdFile;
            } else {
                String envPath = System.getenv("PATH");
                String path[] = StringTools.split(envPath, File.pathSeparatorChar);
                for (int i = 0; i < path.length; i++) {
                    File cmdf = new File(path[i], cmd);
                    if (cmdf.isFile()) {
                        Print.logInfo("Found: " + cmdf);
                        return cmdf;
                    }
                }
                return null;
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified path reference is absolute
    *** @param f  The file to test for absolute
    *** @return True if the specified file is absolute
    **/
    public static boolean isAbsolute(File f)
    {

        /* file not specified */
        if (f == null) {
            return false;
        }

        /* OS absolute */
        if (f.isAbsolute()) {
            return true;
        }

        /* special case for Windows files on non-Windows OS */
        String path = f.toString();
        if ((path.length() > 1) && (path.charAt(1) == ':')) {
            // IE: "C:\directory\file.xyz"
            return true;
        }

        /* assume not absolute */
        return false;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified item is a file
    *** @param f  The item to check
    *** @return True if the specified item is a file
    **/
    public static boolean isFile(String f)
    {
        return !StringTools.isBlank(f) && FileTools.isFile(new File(f));
    }

    /**
    *** Returns true if the specified item is a file
    *** @param f  The item to check
    *** @return True if the specified item is a file
    **/
    public static boolean isFile(File f)
    {
        return (f != null) && f.isFile();
    }

    /**
    *** Returns true if the specified item is a file with the specified extension
    *** @param f    The item to check
    *** @param ext  The required file extension
    *** @return True if the specified item is a file with the specified extension
    **/
    public static boolean isFile(File f, String ext)
    {
        if ((f == null) || !f.isFile()) {
            return false;
        } else
        if (ext != null) {
            String fe = FileTools.getExtension(f);
            return ext.equalsIgnoreCase(fe);
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified item is a directory
    *** @param d  The item to check
    *** @return True if the specified item is a directory
    **/
    public static boolean isDirectory(String d)
    {
        return !StringTools.isBlank(d) && FileTools.isDirectory(new File(d));
    }

    /**
    *** Returns true if the specified item is a directory
    *** @param d  The item to check
    *** @return True if the specified item is a directory
    **/
    public static boolean isDirectory(File d)
    {
        return (d != null) && d.isDirectory();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified file is a Windows shortcut file<br>
    *** (EXPERIMENTAL: May not work with all Windows shortcut files)
    *** @param link  A file possibly representing a Windows shortcut file
    *** @return True if the specified file is a Windows shortcut file
    **/
    public static boolean isWindowsShortcut(File link)
    {
        // Little-Endian bytes:
        // Header: 4C 00 00 00
        // GUID  : 01 14 02 00 00 00 00 00 C0 00 00 00 00 00 00 46
        if (FileTools.isFile(link) && FileTools.getExtension(link).equalsIgnoreCase("lnk")) {
            byte b[] = FileTools.readFile(link,4); // 0x0000004C (LittleEndian)
            if (ListTools.size(b) >= 4) {
                long val = Payload.decodeLong(b, 0, 4, false/*bigEndian?*/, false/*signed*/, 0L);
                return (val == 0x0000004CL)? true : false;
            }
        }
        return false;
    }

    /**
    *** Gets the String path target of the Windows shortcut.<br>
    *** (EXPERIMENTAL: May not work with all Windows shortcut files)
    *** @param link  The Windows shortcut file.
    *** @param makeAbsolute If true and the shortcut is not absolute, the returned file will
    ***     be made relative to the parent directory of the specified link.  If false, the
    ***     returned file will left as specified in the shortcut link.
    *** @return The shortcut target file/directory.
    **/
    public static File getWindowsShortcutFile(File link, boolean makeAbsolute)
    {
        // References:
        //  - http://ithreats.files.wordpress.com/2009/05/lnk_the_windows_shortcut_file_format.pdf
        /* must be a file (not a directory) */
        if (!FileTools.isFile(link)) {
            return null;
        }
        File parentDir = link.getParentFile();

        /* create Payload containing contents */
        Payload p = new Payload(FileTools.readFile(link,1000),false); // LittleEndian

        /* header/GUID */
        long header  = p.readULong(4,0L,"header");      // 00:04    0x0000004C
        byte GUID[]  = p.readBytes(16,"GUID");          // 04:16    0x0114020000000000C0000000000046

        /* flags/attr */
        long flags   = p.readULong(4,0L,"HeaderFlags"); // 20:04    0x0000000D [00001101]
        //  0 = Shell Item-ID present
        //  1 = File/Directory
        //  2 = Description present
        //  3 = Relative Path present
        //  4 = Working Directory present
        //  5 = Command-Line args present
        //  6 = Custom Icon

        /* attr */
        long attr    = p.readULong(4,0L,"Attributes");  // 24:04    0x00000000
        //  0 = Read Only
        //  1 = Hidden
        //  2 = System file
        //  3 = Volume label (?)
        //  4 = Directory
        //  5 = Modified since backup/archive
        //  6 = Encrypted
        //  7 = Normal?
        //  8 = Temporary
        //  9 = Sparse
        // 10 = Has reparse point data
        // 11 = Compressed
        // 12 = Offline

        /* Time 1/2/3 */
        long time_1  = p.readLong(8,0L,"CreateTime");   // 28:08 Creation Time
        long time_2  = p.readLong(8,0L,"ModTime");      // 36:08 Modification Time
        long time_3  = p.readLong(8,0L,"AccessTime");   // 44:08 Last Access Time

        /* file length */
        long fileLen = p.readULong(4,0L,"FileLength");  // 52:04

        /* icon number */
        long iconNdx = p.readULong(4,0L,"IconNum");     // 56:04

        /* ShowWnd */
        long showWnd = p.readULong(4,0L,"ShowWnd");     // 60:04

        /* hotkey */
        long hotKey  = p.readULong(4,0L,"HotKey");      // 64:04

        /* unknown/zero */
        long zero_1  = p.readULong(4,0L,"Zero-1");      // 68:04
        long zero_2  = p.readULong(4,0L,"Zero-2");      // 72:04

        /* Shell Item ID List (see ITEMIDLIST) */
        if ((flags & 0x0001L) != 0L) {
            int siLen = p.readUInt(2,0,"ShellItemID");
            p.readSkip(siLen);
        }

        /* File Location Info */
        String path = null;
        if ((flags & 0x0002L) != 0L) {
            p.saveIndex();
            int fileLocLen = p.readInt(4,0,"FileLocLen");   // 00:04 Will be '0' if flags bit 1 is '0' ??
            if (fileLocLen > 0) {
                p.restoreIndex();
                byte L[] = p.readBytes(fileLocLen, "FileLocTable");
                Payload f = new Payload(L,false);
                f.readSkip(4); //already have the length in "fileLocLen"
                int ofsStart     = f.readInt(4,0,"OfsStart");       // 04:04 0x1C
                int flFlags      = f.readInt(4,0,"Volume");         // 08:04 Bit0=LocalVol, Bit1=NetShare
                int ofsLocVolTbl = f.readInt(4,0,"OfsLocVolTbl");   // 12:04 When "LocalVol"
                int ofsBasePath  = f.readInt(4,0,"OfsBasePath");    // 16:04 When "LocalVol"
                int ofsNetVol    = f.readInt(4,0,"OfsNetVolTbl");   // 20:04 When "LocalVol"
                int ofsFinalPath = f.readInt(4,0,"OfsFinalPath");   // 24:04 
                if ((flFlags & 0x01) != 0) {
                    // LocalFile: Base + Final
                    String bPath = (ofsBasePath  >= 0x1C)? StringTools.parseString(L,ofsBasePath ,"") : "";
                    String fPath = (ofsFinalPath >= 0x1C)? StringTools.parseString(L,ofsFinalPath,"") : "";
                    path = (bPath + fPath);
                }
            }
        }

        /* Description */
        if ((flags & 0x0004L) != 0L) {
            int dLen = p.readInt(2,0,"DescLen");
            String desc = p.readString(dLen,false/*varLen*/,"Desc");
            // not used
        }

        /* Relative Path */
        if ((flags & 0x0008L) != 0L) {
            int dLen = p.readInt(2,0,"RelPathLen");
            String relPath = p.readString(dLen,false/*varLen*/,"RelPath");
            if (StringTools.isBlank(path)) {
                path = relPath;
            }
        }

        /* Working Dir */
        if ((flags & 0x0010L) != 0L) {
            int dLen = p.readInt(2,0,"WorkDirLen");
            String workDir = p.readString(dLen,false/*varLen*/,"WorkDir");
            // not used
        }

        /* command-line args, etc */
        // ... not used

        /* file */
        File file = null;
        if (!StringTools.isBlank(path)) {
            file = new File(path);
            if (!makeAbsolute) {
                // leave as-is
            } else
            if (FileTools.isAbsolute(file)) {
                // already absolute, leave as-is
            } else
            if (path.startsWith("\\") || path.startsWith("/")) {
                // assume absolute, leave as-is
            } else {
                // make relative to parent dir 
                file = new File(parentDir, file.toString());
            }
        }

        /* return target */
        return file; // may be null

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified file is a Cygwin symbolic link<br>
    *** (EXPERIMENTAL: May not work with all Cygwin symbolic link files)
    *** @param link  A file possibly representing a Cygwin symbolic link
    *** @return True if the specified file is a Cygwin symbolic link
    **/
    public static boolean isCygwinSymlink(File link)
    {
        if (FileTools.isFile(link)) {
            byte b[] = FileTools.readFile(link,10); // "!<symlink>
            if (ListTools.size(b) >= 10) {
                String s = StringTools.toStringValue(b);
                return s.equalsIgnoreCase("!<symlink>");
            }
        }
        return false;
    }

    /**
    *** Gets the String path target of the Cygwin symbolic link<br>
    *** (EXPERIMENTAL: May not work with all Cygwin symbolic link files)
    *** @param link  The Cygwin symbolic link file.
    *** @param makeAbsolute If true and the link is not absolute, the returned file will
    ***     be made relative to the parent directory of the specified link.  If false, the
    ***     returned file will left as specified in the Cygwin symbolic link.
    *** @return The Cygwin symbolic link target file/directory.
    **/
    public static File getCygwinSymlinkFile(File link, boolean makeAbsolute)
    {
        /* must be a file (not a directory) */
        if (!FileTools.isFile(link)) {
            return null;
        }
        File parentDir = link.getParentFile();

        /* create Payload containing contents */
        Payload p = new Payload(FileTools.readFile(link,300));

        /* check for Cygwin symbolic link */
        String s = p.readString(10,false); // "!<symlink>
        if (!s.equalsIgnoreCase("!<symlink>")) {
            // not a Cygwin link file
            return null;
        }

        /* UTF-16: Byte-Order-Mark (BOM) */
        byte BOM[] = p.peekBytes(2);
        int utf16 = 0; // default to non-UTF16
        if (BOM.length == 2) {
            //Print.logInfo("BOM: 0x" + StringTools.toHexString(BOM));
            if ((BOM[0] == (byte)0xFF) && (BOM[1] == (byte)0xFE)) {
                utf16 = 1; // MSB
            } else
            if ((BOM[0] == (byte)0xFE) && (BOM[1] == (byte)0xFF)) {
                utf16 = 2; // LSB
            }
        }

        /* get link path */
        String path = null;
        if (utf16 > 0) {
            // UTF-16 encoding
            p.readSkip(2); // skip 0xFFFE / 0xFEFF
            StringBuffer sb = new StringBuffer();
            while (p.hasAvailableRead()) {
                int ich = p.readUInt(2,0); 
                char ch = (utf16 == 1)? (char)((ich >> 8) & 0xFF) : (char)(ich & 0xFF);
                if (ch == 0) { break; } // done
                sb.append(ch);
            }
            path = sb.toString();
        } else {
            // TODO: assume UTF-8 encoding?
            StringBuffer sb = new StringBuffer();
            while (p.hasAvailableRead()) {
                int ich = p.readUInt(2,0); 
                char ch = (char)((ich >> 8) & 0xFF); // assume MSB
                if (ch == 0) { break; } // done
                sb.append(ch);
            }
            path = sb.toString();
        }

        /* file */
        File file = null;
        if (!StringTools.isBlank(path)) {
            file = new File(path);
            if (!makeAbsolute) {
                // leave as-is
            } else
            if (FileTools.isAbsolute(file)) {
                // already absolute, leave as-is
            } else
            if (path.startsWith("\\") || path.startsWith("/")) {
                // assume absolute, leave as-is
            } else {
                // make relative to parent dir 
                file = new File(parentDir, file.toString());
            }
        }

        /* return target */
        return file; // may be null

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the file can be executed
    *** @param f  The file to test
    *** @return True if the file can be executed
    **/
    public static boolean canExecute(File f)
        //throws UnsupportedOperationException
        //*** @throws UnsupportedOperationException if the JRE does not support the "canExecute" method.
    {
        if (f == null) {
            return false; // can't execute a file that is null
        }
        return f.canExecute();
        /* the following is not needed for JRE 1.6+
        try {
            MethodAction canExecMeth = new MethodAction(f, "canExecute");
            return ((Boolean)canExecMeth.invoke()).booleanValue();
        } catch (NoSuchMethodException nsme) {
            UnsupportedOperationException usoe = new UnsupportedOperationException("'canExecute' not supported");
            usoe.initCause(nsme);
            throw usoe;
        } catch (Throwable th) { // MethodInvocationException
            UnsupportedOperationException usoe = new UnsupportedOperationException("Exception thrown during 'canExecute'");
            usoe.initCause(th);
            throw usoe;
        }
        */
    }

    /**
    *** Returns true if the file can be read
    *** @param f  The file to test
    *** @return True if the file can be read
    **/
    public static boolean canRead(File f)
        //throws UnsupportedOperationException
        //*** @throws UnsupportedOperationException if the JRE does not support the "canRead" method.
    {
        if (f == null) {
            return false; // can't read a file that is null
        }
        return f.canRead();
        /* the following is not needed for JRE 1.6+
        try {
            MethodAction canReadMeth = new MethodAction(f, "canRead");
            return ((Boolean)canReadMeth.invoke()).booleanValue();
        } catch (NoSuchMethodException nsme) {
            UnsupportedOperationException usoe = new UnsupportedOperationException("'canRead' not supported");
            usoe.initCause(nsme);
            throw usoe;
        } catch (Throwable th) { // MethodInvocationException
            UnsupportedOperationException usoe = new UnsupportedOperationException("Exception thrown during 'canRead'");
            usoe.initCause(th);
            throw usoe;
        }
        */
    }

    /**
    *** Returns true if the file can be written
    *** @param f  The file to test
    *** @return True if the file can be written
    **/
    public static boolean canWrite(File f)
        //throws UnsupportedOperationException
        //*** @throws UnsupportedOperationException if the JRE does not support the "canWrite" method.
    {
        if (f == null) {
            return false; // can't write to a file that is null
        }
        return f.canWrite();
        /* the following is not needed for JRE 1.6+
        try {
            MethodAction canWriteMeth = new MethodAction(f, "canWrite");
            return ((Boolean)canWriteMeth.invoke()).booleanValue();
        } catch (NoSuchMethodException nsme) {
            UnsupportedOperationException usoe = new UnsupportedOperationException("'canWrite' not supported");
            usoe.initCause(nsme);
            throw usoe;
        } catch (Throwable th) { // MethodInvocationException
            UnsupportedOperationException usoe = new UnsupportedOperationException("Exception thrown during 'canWrite'");
            usoe.initCause(th);
            throw usoe;
        }
        */
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the file path for the first file found in the specified list
    *** @param parentDir  The parent directory
    *** @param fileNames  The list of files to search for
    *** @return The path to the file, or null if not found
    **/
    public static File findFile(File parentDir, String fileNames[])
    {

        /* invalid parent directory? */
        if (!FileTools.isDirectory(parentDir)) {
            return null;
        }

        /* invalid file list */
        if ((fileNames == null) || (fileNames.length <= 0)) {
            return null;
        }

        /* search list */
        for (String fn : fileNames) {
            File file = new File(parentDir, fn);
            if (file.isFile()) {
                return file;
            }
        }

        /* not found */
        return null;

    }
    
    // ------------------------------------------------------------------------

    /**
    *** Return an array of all filesystem 'root' directories
    *** @return An array of all filesystem 'root' directories
    **/
    public static File[] getFilesystemRoots()
    {
        return File.listRoots();
    }

    /**
    *** Returns an array of sub-directories within the specified parent directory.
    *** Files are not included in the list.
    *** @param dir  The parent directory
    *** @return An array of sub-directories within the specified parent directory, or null
    ***         if the specified file is not a directory.
    **/
    public static File[] getDirectories(File dir)
    {

        /* no directory */
        if ((dir == null) || !dir.isDirectory()) {
            return null;
        }

        /* get all directories */
        return dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });

    }

    /**
    *** Returns an array of files within the specified parent directory.
    *** Directories are not included in the list. (non-recursive)
    *** @param dir  The parent directory
    *** @return An array of files within the specified parent directory, or null
    ***         if the specified directory is not a directory.
    **/
    public static File[] getFiles(File dir)
    {

        /* no directory */
        if ((dir == null) || !dir.isDirectory()) {
            return null;
        }

        /* get all files */
        return dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile();
            }
        });

    }

    /**
    *** Returns an array of files within the specified parent directory which match 
    *** the specified extensions. (recursive-decent)
    *** Directories are not included in the list.
    *** @param fileList List where matching files will be placed
    *** @param dir      The parent directory
    *** @param extnSet  The set of extensions to look for
    *** @param filter   The filter used to select files to include in the list
    *** @return An array of files within the specified parent directory, or null
    ***         if the specified file is not a directory.
    **/
    private static boolean _getAllFiles(Collection<File> fileList, File dir, 
        final Set<String> extnSet, final FileFilter filter)
    {

        /* no directory */
        if ((dir == null) || !dir.isDirectory()) {
            return false;
        }

        /* get files in current directory */
        File fileDirs[] = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true; // accept to allow decending into this directory
                } else {
                    if ((extnSet != null) && !extnSet.contains(FileTools.getExtension(file))) {
                        return false; // rejected: non-matching extension
                    } else
                    if ((filter != null) && !filter.accept(file)) {
                        return false; // rejected: non-matching filter
                    } else {
                        return true;
                    }
                }
            }
        });

        /* add all files */
        for (File fd : fileDirs) {
            if (fd.isFile()) {
                fileList.add(fd);
            }
        }

        /* recursive-decent into all directories */
        for (File fd : fileDirs) {
            if (fd.isDirectory()) {
                FileTools._getAllFiles(fileList, fd, extnSet, filter);
            }
        }

        /* return */
        return true;

    }

    /**
    *** Returns an array of files within the specified parent directory which match 
    *** the specified extensions.
    *** Directories are not included in the list.
    *** @param dir      The parent directory
    *** @param extnSet  The set of extensions to look for
    *** @param recursiveDecent  True to descend all directories looking for matching files
    *** @return An array of files within the specified parent directory, or null
    ***         if the specified file is not a directory.
    **/
    public static File[] getFiles(File dir, final Set<String> extnSet, boolean recursiveDecent)
    {

        /* no directory */
        if ((dir == null) || !dir.isDirectory()) {
            return null;
        }

        /* this directory only */
        if (!recursiveDecent) {
            return dir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    if (!file.isFile()) {
                        return false;
                    } else
                    if (extnSet == null) {
                        return true;
                    } else {
                        String extn = FileTools.getExtension(file);
                        return extnSet.contains(extn);
                    }
                }
            });
        }

        /* recursive decent */
        Vector<File> fileList = new Vector<File>();
        FileTools._getAllFiles(fileList, dir, extnSet, null);
        return fileList.toArray(new File[fileList.size()]);

    }
    
    /**
    *** Returns an array of files within the specified parent directory which match 
    *** the specified extensions.
    *** Directories are not included in the list.
    *** @param dir       The parent directory
    *** @param extnList  The list of extensions to look for
    *** @param recursiveDecent  True to descend all directories looking for matching files
    *** @return An array of files within the specified parent directory, or null
    ***         if the specified directory is not a directory.
    **/
    public static File[] getFiles(File dir, String extnList[], boolean recursiveDecent)
    {

        /* no directory */
        if ((dir == null) || !dir.isDirectory()) {
            return null;
        }

        /* get files */
        if (extnList == null) {
            return FileTools.getFiles(dir, (Set<String>)null, recursiveDecent);
        } else {
            Set<String> extnSet = ListTools.toSet(extnList, new HashSet<String>());
            return FileTools.getFiles(dir, extnSet, recursiveDecent);
        }

    }

    /**
    *** Returns an array of files within the specified parent directory.
    *** Directories are not included in the list.
    *** @param dir    The parent directory
    *** @param regex  A regular expression (Pattern) that the file names must match
    *** @param recursiveDecent  True to descend all directories looking for matching files
    *** @return An array of files within the specified parent directory, or null
    ***         if the specified directory is not a directory.
    **/
    public static File[] getFiles(File dir, final Pattern regex, boolean recursiveDecent)
    {

        /* no directory */
        if ((dir == null) || !dir.isDirectory()) {
            return null;
        }

        /* regular expression filter */
        FileFilter filter = new FileFilter() {
            public boolean accept(File file) {
                if (!file.isFile()) {
                    return false;
                } else
                if (regex == null) {
                    return true;
                } else {
                    return regex.matcher(file.getName()).matches();
                }
            }
        };

        /* this directory only */
        if (!recursiveDecent) {
            return dir.listFiles(filter);
        }

        /* regular expression match */
        Vector<File> fileList = new Vector<File>();
        FileTools._getAllFiles(fileList, dir, null, filter);
        return fileList.toArray(new File[fileList.size()]);

    }

    /**
    *** Returns an array of files within the specified parent directory.
    *** Directories are not included in the list.
    *** @param dir    The parent directory
    *** @param fileGlob  A file glob expression that the file names must match
    *** @param recursiveDecent  True to descend all directories looking for matching files
    *** @return An array of files within the specified parent directory, or null
    ***         if the specified directory is not a directory.
    **/
    public static File[] getFiles(File dir, String fileGlob, boolean recursiveDecent)
    {

        /* no directory */
        if ((dir == null) || !dir.isDirectory()) {
            return null;
        }

        /* file glob pattern */
        Pattern pattern = null;
        if (fileGlob != null) {
            // create regex from glob
            // Note: this does only a partial job of converting glob to regex.
            // Should be replaced with Java 7 "PathMatcher":
            //    "FileSystems.getDefault().getPathMatcher("glob:" + pattern);"
            StringBuffer fileRegex = new StringBuffer();
            fileRegex.append("^");
            for (int c = 0; c < fileGlob.length(); c++) {
                char ch = fileGlob.charAt(c);
                switch (ch) {
                    case '^' : fileRegex.append("\\^" ) ; break;
                    case '$' : fileRegex.append("\\$" ) ; break;
                    case '.' : fileRegex.append("\\." ) ; break;
                    case '?' : fileRegex.append("."   ) ; break;
                    case '*' : fileRegex.append(".*"  ) ; break;
                    case '\\': fileRegex.append("\\\\") ; break;
                    default  : fileRegex.append(ch    ) ; break;
                }
            }
            fileRegex.append("$");
            //Print.logInfo("File name regex: " + fileRegex);
            // create pattern
            try {
                pattern = Pattern.compile(fileRegex.toString());
            } catch (PatternSyntaxException pse) {
                Print.logError("Invalid fileName regular expression: " + fileRegex);
                return null;
            }
        }

        /* regular expression filter */
        return FileTools.getFiles(dir, pattern, recursiveDecent);

    }

    // ------------------------------------------------------------------------

    /**
    *** Executed the filter "accept" callback on all files within the specified directory 
    *** @param dir      The parent directory
    *** @param filter   The callback filter
    *** @return True if the specified file is a directory, false otherwise.
    **/
    public static boolean traverseAllFiles(File dir, FileFilter filter)
    {
        if ((dir == null) || !dir.isDirectory()) {
            return false;
        } else {
            // -- "dir" is a directory
            File fileDirs[] = dir.listFiles(); // all files/directories
            if (fileDirs != null) {
                // -- traverse all files/directories
                for (File fd : fileDirs) {
                    filter.accept(fd);  // callback
                    if (fd.isDirectory()) {
                        // -- decend into directory
                        FileTools.traverseAllFiles(fd, filter);
                    }
                }
                return true;
            } else {
                // -- an IOException occurred on the parent directory
                return false;
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the MD5 hash for the specified file
    *** @param file  The file
    *** @return The MD5 hash
    **/
    public static String getHash_MD5(File file)
    {

        /* not file */
        if ((file == null) || !file.isFile()) {
            return null;
        }

        /* read file */
        byte data[] = FileTools.readFile(file);
        if (data == null) {
            return null;
        }
        
        /* return MD5 hash */
        return FileTools.getHash_MD5(data);

    }

    /**
    *** Returns the MD5 hash for the specified bytes
    *** @param data  The data bytes
    *** @return The MD5 hash
    **/
    public static String getHash_MD5(byte data[])
    {
        if (!ListTools.isEmpty(data)) {
            try {
                MessageDigest msgDigest = MessageDigest.getInstance("MD5");
                msgDigest.reset();
                msgDigest.update(data);
                return StringTools.toHexString(msgDigest.digest()).toLowerCase();
            } catch (NoSuchAlgorithmException nsae) {
                //Print.logException("MD5 Algorithm not found", nsae);
                return null;
            }
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the SHA-1 hash for the specified bytes
    *** @param file  The file
    *** @return The SHA-1 hash
    **/
    public static String getHash_SHA1(File file)
    {

        /* not file */
        if ((file == null) || !file.isFile()) {
            return null;
        }

        /* read file */
        byte data[] = FileTools.readFile(file);
        if (data == null) {
            return null;
        }
        
        /* return MD5 hash */
        return FileTools.getHash_SHA1(data);

    }

    /**
    *** Returns the SHA-1 hash for the specified bytes
    *** @param data  The data bytes
    *** @return The SHA-1 hash
    **/
    public static String getHash_SHA1(byte data[])
    {
        if (!ListTools.isEmpty(data)) {
            try {
                MessageDigest msgDigest = MessageDigest.getInstance("SHA-1");
                msgDigest.reset();
                msgDigest.update(data);
                return StringTools.toHexString(msgDigest.digest()).toLowerCase();
            } catch (NoSuchAlgorithmException nsae) {
                //Print.logException("MD5 Algorithm not found", nsae);
                return null;
            }
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the SHA-256 hash for the specified bytes
    *** @param file  The file
    *** @return The SHA-256 hash
    **/
    public static String getHash_SHA256(File file)
    {

        /* not file */
        if ((file == null) || !file.isFile()) {
            return null;
        }

        /* read file */
        byte data[] = FileTools.readFile(file);
        if (data == null) {
            return null;
        }
        
        /* return MD5 hash */
        return FileTools.getHash_SHA256(data);

    }

    /**
    *** Returns the SHA-256 hash for the specified bytes
    *** @param data  The data bytes
    *** @return The SHA-256 hash
    **/
    public static String getHash_SHA256(byte data[])
    {
        if (!ListTools.isEmpty(data)) {
            try {
                MessageDigest msgDigest = MessageDigest.getInstance("SHA-256");
                msgDigest.reset();
                msgDigest.update(data);
                return StringTools.toHexString(msgDigest.digest()).toLowerCase();
            } catch (NoSuchAlgorithmException nsae) {
                //Print.logException("MD5 Algorithm not found", nsae);
                return null;
            }
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- Java 7

    private static volatile boolean     FilesGetAttribute_didInit                = false;
    private static          Object      FilesGetAttribute_initLock               = new Object();
    private static          Class<?>    Java_nio_file_Path_class                 = null;
    private static          Class<?>    Java_nio_file_LinkOption_class           = null;
    private static          Object      Java_nio_file_LinkOption_NOFOLLOW_LINKS  = null;

    private enum _LinkOptionDummy { A, B, C };

    /**
    *** Initialize Files objects needed for calls to "getAttribute".
    *** Requires Java 7 to obtain useful results
    **/
    @SuppressWarnings("unchecked")
    private static boolean _initFilesGetAttribute()
    {

        /* already initialized */
        if (FilesGetAttribute_didInit) {
            return (Java_nio_file_Path_class != null)? true : false;
        }

        /* init */
        synchronized (FilesGetAttribute_initLock) {
            if (!FilesGetAttribute_didInit) { // check again after lock

                /*volatile java.nio.file.* classes */
                Class<?>    pathClass = null; // java.nio.file.Path
                Class<_LinkOptionDummy> linkOptClass = null; // java.nio.file.LinkOption
                try {
                    pathClass    = Class.forName("java.nio.file.Path");
                    linkOptClass = (Class<_LinkOptionDummy>)Class.forName("java.nio.file.LinkOption");
                    //Print.logDebug("Path       class: " + StringTools.className(pathClass));
                    //Print.logDebug("LinkOption class: " + StringTools.className(linkOptClass));
                } catch (Throwable th) {
                    Print.logDebug("Unable to get class 'java.nio.file.*' classes: " + th);
                    return false; // exit synchronized
                }

                /* java.nio.file.LinkOption.NOFOLLOW_LINKS enum */
                Enum<?> linkOpt = null; // java.nio.file.LinkOption.NOFOLLOW_LINKS
                try {
                    linkOpt = Enum.valueOf(linkOptClass, "NOFOLLOW_LINKS");
                    //Print.logDebug("LinkOption: " + linkOpt);
                } catch (Throwable th) {
                    Print.logDebug("Unable to get class 'LinkOption.NOFOLLOW_LINKS': " + th);
                    return false; // exit synchronized
                }

                /* all ok at this point */
                Java_nio_file_Path_class                = pathClass;
                Java_nio_file_LinkOption_class          = linkOptClass;
                Java_nio_file_LinkOption_NOFOLLOW_LINKS = linkOpt;

                /* set initialized */
                FilesGetAttribute_didInit = true;

            }
        }

        /* return successful init */
        return (Java_nio_file_Path_class != null)? true : false;

    }

    /**
    *** Returns true if "Files.getAttribute(...)" is supported.
    **/
    public static boolean supportsFilesGetAttribute()
    {
        return FileTools._initFilesGetAttribute();
    }

    /** 
    *** Returns a java.nio.file.Path instance for this file
    **/
    public static Object getFilePath(File file)
        throws UnsupportedOperationException, IllegalArgumentException
    {
        if (file != null) {
            // -- use reflection for JRE 1.6 and below (not required for JRE 1.7+)
            try {
                return (new MethodAction(file,"toPath")).invoke();
            } catch (NoSuchMethodException nsme) {
                throw new UnsupportedOperationException("'<File>.toPath()' not supported by this JRE");
            } catch (Throwable th) {
                // -- java.nio.file.InvalidPathException
                Print.logException("Invocation error", th);
                throw new IllegalArgumentException("Invalid file/path specification");
            }
            // return file.toPath(); // JRE 1.7+ only
        } else {
            return null;
        }
    }

    /**
    *** Gets the specified attribute for this file.
    *** Will compile on Java-6, however requires Java-7 for valid results.
    *** Will return null, when running on Java-6
    **/
    @SuppressWarnings("unchecked")
    public static Object getAttribute(File file, String attr)
        throws UnsupportedOperationException, 
               IllegalArgumentException, 
               IOException, 
               SecurityException
    {
        // -- Basic Attributes:
        // -    "basic:lastModifiedTime"    (FileTime)
        // -    "basic:lastAccessTime"      (FileTime)
        // -    "basic:creationTime"        (FileTime)
        // -    "basic:size"                (Long)
        // -    "basic:isRegularFile"       (Boolean)
        // -    "basic:isDirectory"         (Boolean)
        // -    "basic:isSymbolicLink"      (Boolean)
        // -    "basic:isOther"             (Boolean)
        // -    "basic:fileKey"             (Object)
        // -- DOS Attributes:
        // -    "dos:readonly"              (Boolean)
        // -    "dos:hidden"                (Boolean)
        // -    "dos:system"                (Boolean)
        // -    "dos:archive"               (Boolean)
        // -- Posix Attributes:
        // -    "posix:permissions"         (Set<PosixFilePermission>)
        // -    "posix:group"               (java.nio.file.attribute.GroupPrincipal)
        // -- ACL Attributes:
        // -    "acl:acl"                   (java.util.List<AclEntry>)
        // -    "acl:owner"                 (java.nio.file.attribute.UserPrincipal)
        // -- Unix Attributes:
        // -    "unix:uid"                  (Integer)
        // -    "unix:gid"                  (Integer)
        // -    "unix:owner"                (java.nio.file.attribute.UserPrincipal)
        // -    "unix:group"                (java.nio.file.attribute.GroupPrincipal)
        // -    "unix:fileKey"              (jsun.nio.fs.UnixFileKey)

        /* return null if no file defined */
        if (file == null) {
            throw new IllegalArgumentException("Missing File specification");
        }

        /* initialize requires classes/objects */
        if (!FileTools._initFilesGetAttribute()) {
            throw new UnsupportedOperationException("Not using Java 7+");
        }
        // -- successfully initialized

        /* get file path */
        //Path filePath = file.toPath();
        Object filePath = FileTools.getFilePath(file); // java.nio.file.Path
        if (filePath == null) {
            throw new IllegalArgumentException("Invalid File specification");
        }

        /* get attribute */
        try {
            //Object obj = Files.getAttribute(filePath, attr, LinkOption.NOFOLLOW_LINKS);
            Object linkOptArray[] = (Object[])Array.newInstance(Java_nio_file_LinkOption_class, 1);  // "unchecked cast"
            linkOptArray[0] = Java_nio_file_LinkOption_NOFOLLOW_LINKS;
            MethodAction filesAttr = new MethodAction("java.nio.file.Files", "getAttribute", 
                Java_nio_file_Path_class, String.class, linkOptArray.getClass());
            Object obj = filesAttr.invoke(filePath, attr, linkOptArray);
            // -- return attribute result
            Print.logDebug("Attribute: " + obj + " [" + StringTools.className(obj) + "]");
            if (obj == null) {
                return "";
            } else
            if (obj instanceof Set) {
                return StringTools.join((Set)obj, ",");
            } else
            if (obj instanceof java.util.List) {
                return StringTools.join((java.util.List)obj, ",");
            } else {
                return obj.toString();
            }
        } catch (UnsupportedOperationException uoe) {
            throw uoe;
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (IOException ioe) {
            throw ioe;
        } catch (SecurityException se) {
            throw se;
        } catch (Throwable th) {
            // -- UnsupportedOperationException
            // -- IllegalArgumentException
            // -- IOException
            // -- SecurityException
            Print.logDebug("Unable to get attribute: " + th);
            return new UnsupportedOperationException("Unknown error: " + th);
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this file is a symbolic link
    **/
    public static boolean isSymbolicLink(File file)
        throws UnsupportedOperationException 
    {
        // -- requires Java-7
        if (file != null) {
            try {
                Object symLink = FileTools.getAttribute(file, "basic:isSymbolicLink");
                return StringTools.parseBoolean(symLink,false);
                //return Files.isSymbolicLink(file.toPath());
            } catch (UnsupportedOperationException uoe) {
                throw uoe;
            } catch (Throwable th) {
                // -- UnsupportedOperationException
                // -- IllegalArgumentException
                // -- IOException
                return false;
            }
        } else {
            return false;
        }
    }

    /**
    *** Gets the target of this symbolic link file.
    *** Returns null if this file is not a symbolic link, or if an error occurs.
    **/
    /* incomplete
    public static File getSymbolicLinkTarget(File file)
    {
        // -- requires Java-7
        if (FileTools.isSymbolicLink(file)) {
            try {
                java.nio.file.Path linkPath = file.toPath();
                java.nio.file.Path filePath = Files.readSymbolicLink(linkPath);
                return (filePath != null)? filePath.toFile() : null;
            } catch (Throwable th) {
                // -- UnsupportedOperationException
                // -- java.nio.file.NotLinkException
                // -- IOException
                // -- SecurityException
                return null;
            }
        } else {
            return null;
        }
    }
    */

    /**
    *** Gets the user owner of this file
    **/
    public static String getUserOwner(File file)
    {
        if (file != null) {
            try {
                Object U = FileTools.getAttribute(file, "unix:owner");
                if (U == null) { U = FileTools.getAttribute(file, "acl:owner"); }
                // -- Java 7
                //java.nio.file.attribute.UserPrincipal U = Files.getOwner(file.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS);
                return (U != null)? U.toString() : null;
            } catch (UnsupportedOperationException uoe) {
                //Print.logError("Java version does not support 'getUserOwner'");
                return null;
            } catch (Throwable th) {
                // -- UnsupportedOperationException
                // -- java.nio.file.NoSuchFileException
                // -- java.nio.file.NotLinkException
                // -- IOException
                // -- SecurityException
                if (StringTools.className(th).indexOf("NoSuchFileException") >= 0) {
                    return null;
                } else {
                    //Print.logError("Unable to get file owner: " + th);
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    /**
    *** Gets the group owner of this file
    **/
    public static String getGroupOwner(File file)
    {
        if (file != null) {
            try {
                Object G = FileTools.getAttribute(file, "unix:group");
                if (G == null) { G = FileTools.getAttribute(file, "posix:group"); }
                return (G != null)? G.toString() : null;
            } catch (UnsupportedOperationException uoe) {
                //Print.logError("Java version does not support 'getGroupOwner'");
                return null;
            } catch (Throwable th) {
                // -- UnsupportedOperationException
                // -- java.nio.file.NoSuchFileException
                // -- java.nio.file.NotLinkException
                // -- IOException
                // -- SecurityException
                if (StringTools.className(th).indexOf("NoSuchFileException") >= 0) {
                    return null;
                } else {
                    Print.logError("Unable to get file group: " + th);
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* remove null bytes from data */
    private static byte[] deNullify(byte data[])
    {
        byte nonull[] = new byte[data.length];
        int n = 0;
        for (int d = 0; d < data.length; d++) {
            if (data[d] != 0) {
                nonull[n++] = data[d];
            }
        }
        if (n < data.length) {
            byte x[] = new byte[n];
            System.arraycopy(nonull,0, x,0, n);
            nonull = x;
        }
        return nonull;
    }

    // ------------------------------------------------------------------------

    private static final String ARG_FILE[]          = { "file"       , "f"        };    // File
    private static final String ARG_ATTR[]          = { "attr"       , "a"        };    // File
    private static final String ARG_WGET[]          = { "wget"       , "url"      };    // URL
    private static final String ARG_HEX_EXPORT[]    = { "toHex"      , "hexExport"};    // File
    private static final String ARG_HEX_IMPORT[]    = { "fromHex"    , "hexImport"};    // File
    private static final String ARG_JAVA_STRING[]   = { "javaString"              };    // File
    private static final String ARG_COPYDIR[]       = { "copyDir"    , "copy"     };    // Directory
    private static final String ARG_PROGRESS[]      = { "progress"                };    // boolean
    private static final String ARG_TODIR[]         = { "dir"        , "todir"    };    // Directory
    private static final String ARG_TOFILE[]        = { "to"         , "tofile"   };    // File
    private static final String ARG_OVERWRITE[]     = { "overwrite"               };    // boolean
    private static final String ARG_TIMEOUT[]       = { "timeoutMS"  , "timeout"  };    // int
    private static final String ARG_WHICH[]         = { "which"                   };    // command name
    private static final String ARG_WIDTH[]         = { "width"      , "w"        };    // int
    private static final String ARG_DUMP[]          = { "dump"                    };    // boolean
    private static final String ARG_STRINGS[]       = { "strings"                 };    // boolean
    private static final String ARG_UNI_ENCODE[]    = { "uniencode"  , "ue"       };    // boolean
    private static final String ARG_UNI_DECODE[]    = { "unidecode"  , "ud"       };    // boolean
    private static final String ARG_FIND[]          = { "find"                    };    // String
    private static final String ARG_COUNTUNIQUE[]   = { "countUnique"             };    // File
    private static final String ARG_FILTERLOG[]     = { "filterLog"  , "flog"     };    // File
    private static final String ARG_NO_HEADER[]     = { "noHeader"   , "nh"       };    // Boolean
    private static final String ARG_SEARCH[]        = { "search"     , "grep"     };    // File
    private static final String ARG_MATCH[]         = { "match"                   };    // String\String
    private static final String ARG_COPY_CSV[]      = { "copyCSV"                 };    // File
    private static final String ARG_MD5[]           = { "md5"                     };    // File
    private static final String ARG_SHA1[]          = { "sha1"                    };    // File
    private static final String ARG_SHA256[]        = { "sha256"                  };    // File
    private static final String ARG_IS_LINK[]       = { "isLink"                  };    // File
    private static final String ARG_QUIET[]         = { "quiet"                   };    // boolean

    private static final String ARG_PATTERN[]       = { "pattern"                 };    // int

    /**
    *** Display usage
    **/
    private static void usage()
    {
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + FileTools.class.getName() + " {options}");
        Print.sysPrintln("Options:");
        Print.sysPrintln("  -dump=<file>                  Print hex dump");
        Print.sysPrintln("  -strings=<file>               Display all contained strings");
        Print.sysPrintln("  -where=<cmd>                  Find location of file in path");
        Print.sysPrintln("  -wget=<url> [-todir=<dir>]    Read file from URL and copy to current dir");
        Print.sysPrintln("  -wget=<url> [-tofile=<file>]  Read file from URL and copy to specified file");
        Print.sysPrintln("  -flog=<logFile>               Filter log file");
        Print.sysPrintln("  -grep=<file> -match=pattern   Search for A\\B\\C patterns in file");
        System.exit(1);
    }

    /**
    *** Debug/Testing entry point
    *** @param argv  The Command-line args
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        /* wget */
        // -- provides similar functionality the Linux "wget"
        if (RTConfig.hasProperty(ARG_WGET)) {
            //Print.sysPrintln("");
            String urlStr = RTConfig.getString(ARG_WGET,"");
            URIArg url = new URIArg(urlStr);
            if (RTConfig.isDebugMode()) { Print.sysPrintln("URL: " + url); }

            /* output file name */
            File    outFile     = null;
            File    toDir       = RTConfig.getFile(ARG_TODIR,new File(".")); // not null
            File    toFile      = RTConfig.getFile(ARG_TOFILE,null);
            boolean overwrite   = RTConfig.getBoolean(ARG_OVERWRITE,false);
            boolean quiet       = RTConfig.getBoolean(ARG_QUIET,false);
            if (toFile == null) {
                String urlFile = url.getFile();
                int p = (urlFile != null)? urlFile.lastIndexOf('/') : -1;
                if (p > 0) {
                    String name = urlFile.substring(p+1);
                    outFile = new File(toDir,name);
                    if (!overwrite && outFile.exists()) {
                        Print.sysPrintln("Output file already exists: " + outFile);
                        System.exit(1);
                    }
                } else {
                    Print.sysPrintln("Output filename required for this URL: " + urlStr);
                    System.exit(1);
                }
            } else
            if (toFile.toString().equalsIgnoreCase("stdout")) {
                outFile = new File("stdout");
                quiet = true;
            } else
            if (toFile.toString().equalsIgnoreCase("stderr")) {
                outFile = new File("stderr");
                quiet = true;
            } else {
                outFile = toFile.isAbsolute()? toFile : new File(toDir, toFile.toString());
                // -- parent dir does not exist?
                File outParentDir = outFile.getParentFile();
                if ((outParentDir != null) && !outParentDir.isDirectory()) {
                    Print.sysPrintln("Output file parent directory does not exist: " + outFile.getParent());
                    System.exit(1);
                }
                // -- specifies a directory?
                if (outFile.isDirectory()) {
                    Print.sysPrintln("Output file specifies a directory: " + outFile);
                    System.exit(1);
                }
                // -- exists and not overwrite?
                if (!overwrite && outFile.exists()) {
                    Print.sysPrintln("Output file already exists: " + outFile);
                    System.exit(1);
                }
            }

            /* input URL */
            URL inpURL = null;
            try {
                inpURL = new URL(urlStr);
            } catch (MalformedURLException mue) {
                Print.sysPrintln("ERROR: Invalid URL: " + urlStr);
            }

            /* text header */
            if (!quiet) {
                Print.sysPrintln("Copy from: " + inpURL);
                Print.sysPrintln("Copy to  : " + outFile);
            }

            /* copy */
            try {
                int     timeoutMS = RTConfig.getInt(ARG_TIMEOUT, 7000);
                boolean progress  = RTConfig.getBoolean(ARG_PROGRESS,!quiet);
                FileTools.copyFile(inpURL, outFile, progress, timeoutMS);
                Print.sysPrintln("Done.");
            } catch (HTMLTools.HttpIOException hioe) {
                // -- http connection errors
                Throwable th = hioe.getCause(); // should not be null
                if (th instanceof IOException) {
                    // -- this is the expected control path
                    int rc = hioe.getResponseCode();
                    String rcs = (rc > 0)? (String.valueOf(rc)+" ") : "";
                    if (th instanceof UnknownHostException) {
                        Print.sysPrintln("ERROR("+rcs+"Host not found): " + th.getMessage());
                    } else
                    if (th instanceof FileNotFoundException) {
                        Print.sysPrintln("ERROR("+rcs+"File not found): " + th.getMessage());
                    } else
                    if (th instanceof SocketTimeoutException) {
                        Print.sysPrintln("ERROR("+rcs+"Connection timeout): " + th.getMessage());
                    } else {
                        Print.logException("ERROR("+rcs+"Exception)", hioe);
                    }
                } else {
                    // -- unlikely
                    Print.logException("ERROR(Exception)", hioe);
                }
                System.exit(99);
            } catch (UnknownHostException uhe) {
                // -- unlikely, since any connection errors would have been 
                // -  wrapped in HTMLTools.HttpIOException
                Print.sysPrintln("ERROR(Host not found): " + uhe.getMessage());
                System.exit(99);
            } catch (FileNotFoundException fnfe) {
                // -- since connection errors are wrapped in HTMLTools.HttpIOException, 
                // -  this likely is an issue with the "to" file.
                Print.sysPrintln("ERROR(File not found): " + fnfe.getMessage());
                System.exit(99);
            } catch (SocketTimeoutException ste) {
                // -- unlikely, since any connection errors would have been  
                // -  wrapped in HTMLTools.HttpIOException
                Print.sysPrintln("ERROR(Socket Timeout): " + ste.getMessage());
                System.exit(99);
            } catch (Throwable th) {
                Print.logException("ERROR(Exception)", th);
                System.exit(99);
            }
            //Print.sysPrintln("");
            System.exit(0);

        }

        /* file attributes */
        // -- displays file attributes
        if (RTConfig.hasProperty(ARG_ATTR)) {
            File   file = RTConfig.getFile(ARG_FILE,null);
            String attr = RTConfig.getString(ARG_ATTR, null);
            if (file == null) {
                Print.sysPrintln("ERROR: missing '-file' specification");
                System.exit(1);
            }  
            try {
                Print.sysPrintln("User Owner    : " + FileTools.getUserOwner(file));
                Print.sysPrintln("Group Owner   : " + FileTools.getGroupOwner(file));
                Print.sysPrintln("SymbolicLink  : " + FileTools.isSymbolicLink(file));
                Print.sysPrintln("File Attribute: " + FileTools.getAttribute(file, attr));
                System.exit(0);
            } catch (Throwable th) {
                Print.sysPrintln("ERROR: " + th);
                System.exit(1);
            }
        }

        /* which command */
        // -- finds specified command in the current PATH
        if (RTConfig.hasProperty(ARG_WHICH)) {
            File file = RTConfig.getFile(ARG_WHICH,null);
            Print.sysPrintln("Where: " + FileTools.resolveCommand(file.toString()));
            System.exit(0);
        }

        /* unicode decode */
        // -- decode specified unicode character
        if (RTConfig.hasProperty(ARG_UNI_DECODE)) {
            File file = RTConfig.getFile(ARG_UNI_DECODE,null);
            byte data[] = FileTools.readFile(file);
            String dataStr = StringTools.unescapeUnicode(StringTools.toStringValue(data));
            Print.sysPrintln(dataStr);
            System.exit(0);
        }

        /* import ascii file to Java String */
        // -- creates a Java "String text=" wrapper around the contents of the specified file
        if (RTConfig.hasProperty(ARG_JAVA_STRING)) {
            File impFile = RTConfig.getFile(ARG_JAVA_STRING,null);
            if (!FileTools.isFile(impFile)) {
                Print.sysPrintln("Not a file: " + impFile);
                System.exit(1);
            }
            // -- read file
            String text = StringTools.toStringValue(FileTools.readFile(impFile));
            // -- split lines
            String line[] = StringTools.split(text,'\n');
            // -- assemble 
            StringBuffer sb = new StringBuffer();
            sb.append("String text = \n");
            for (String L : line) {
                L = StringTools.replace(L, "\"", "\\\"");
                sb.append("    \"");
                sb.append(L);
                sb.append("\\n\" +\n");
            }
            sb.append("    \"\";\n");
            Print.sysPrintln(sb.toString());
            // -- exit
            System.exit(0);
        }

        /* import ascii-hex file to binary */
        // -- parses ascii-hex from one file and writes binary output to second file
        if (RTConfig.hasProperty(ARG_HEX_IMPORT)) {
            // -- get file to import
            File impFile = RTConfig.getFile(ARG_HEX_IMPORT,null);
            if (!FileTools.isFile(impFile)) {
                Print.sysPrintln("Not a file: " + impFile);
                System.exit(1);
            }
            // -- read and trim ascii hex
            String asciiHex = StringTools.toStringValue(FileTools.readFile(impFile));
            asciiHex = StringTools.stripChars(asciiHex," \t\r\n");
            // -- parse to binary
            byte bin[] = StringTools.parseHex(asciiHex,null);
            if (bin == null) {
                Print.sysPrintln("Unable to parse hex from file (invalid hex?): " + impFile);
                System.exit(1);
            }
            // -- get output file
            File toFile = RTConfig.getFile(ARG_TOFILE,null);
            if (toFile == null) {
                Print.sysPrintln("ERROR: 'toFile' not specified");
                System.exit(1);
            } else
            if (toFile.exists()) {
                Print.sysPrintln("'toFile' already exists: " + toFile);
                System.exit(1);
            }
            // -- write to file
            try {
                Print.sysPrintln("Writing to file: " + toFile);
                FileTools.writeFile(bin, toFile, false/*append?*/);
                Print.sysPrintln("Data written to: " + toFile + " [" + FileTools.getRealFile(toFile) + "]");
            } catch (IOException ioe) {
                Print.logException("Error writing to: " + toFile, ioe);
                System.exit(99);
            }
            // -- done
            System.exit(0);
        }

        /* export binary file to ascii-hex */
        // -- reads binary from one file and writes ascii-hex output to second file
        if (RTConfig.hasProperty(ARG_HEX_EXPORT)) {
            // bin/exeJava -quit -hexExport=FROM_FILE -width=64
            // -- get file to export
            File file = RTConfig.getFile(ARG_HEX_EXPORT,null);
            if (!FileTools.isFile(file)) {
                Print.sysPrintln("Not a file: " + file);
                System.exit(1);
            }
            // -- read binary data
            byte data[] = FileTools.readFile(file);
            if (ListTools.size(data) <= 0) {
                Print.sysPrintln("File is empty: " + file);
                System.exit(1);
            }
            // -- export to ascii-hex
            int width = RTConfig.getInt(ARG_WIDTH,32);
            if (width <= 0) {
                Print.sysPrintln(StringTools.toHexString(data));
            } else {
                for (int ofs = 0; (ofs < data.length); ofs += width) {
                    Print.sysPrintln(StringTools.toHexString(data,ofs,width));
                }
            }
            // -- done
            System.exit(0);
        }

        /* hex dump */
        // -- displays a hex dump of the specified binary file
        if (RTConfig.hasProperty(ARG_DUMP)) {
            // -- get file to dump
            File file = RTConfig.getFile(ARG_DUMP,null);
            // -- read binary data
            byte data[] = FileTools.readFile(file);
            System.out.println("Size " + ((data!=null)?data.length:-1));
            // -- format hex output
            int width = RTConfig.getInt(ARG_WIDTH,16);
            System.out.println(StringTools.formatHexString(data,width));
            // -- done
            System.exit(0);
        }

        /* display strings > 4 chars */
        // -- similar functionality to Linux "strings" command
        if (RTConfig.hasProperty(ARG_STRINGS)) {
            File file = RTConfig.getFile(ARG_STRINGS,null);
            byte data[] = FileTools.readFile(file);
            int len = RTConfig.getInt(ARG_WIDTH,4);
            if (len < 0) {
                len = -len;
                data = deNullify(data);
            }
            int s = -1, n = 0;
            for (int b = 0; b < data.length; b++) {
                if ((data[b] >= (byte)32) && (data[b] <= (byte)127)) {
                    if (s < 0) { s = b; }
                    n++;
                } else 
                if (s >= 0) {
                    if ((b - s) >= len) {
                        String x = StringTools.toStringValue(data, s, b - s);
                        Print.sysPrintln(x);
                    }
                    s = -1;
                }
            }
            if (s >= 0) {
                if ((data.length - s) >= len) {
                    String x = StringTools.toStringValue(data, s, data.length - s);
                    Print.sysPrintln(x);
                }
                s = -1;
            }
            System.exit(0);
        }

        /* find files */
        // -- find a file realtive to the current directory
        if (RTConfig.hasProperty(ARG_FIND)) {
            String glob = RTConfig.getString(ARG_FIND,"");
            File files[] = FileTools.getFiles(new File("."), glob, true);
            if (ListTools.size(files) > 0) {
                for (File f : files) {
                    Print.sysPrintln(f.toString());
                }
            }
            System.exit(0);
        }

        /* count unique line prefixes */
        // -- counts the number of mathing line references in the specified file
        // -  [INFO_|04/30 09:20:11]
        if (RTConfig.hasProperty(ARG_COUNTUNIQUE)) {
            File logFile = RTConfig.getFile(ARG_COUNTUNIQUE,null);
            if (!FileTools.isFile(logFile)) {
                Print.sysPrintln("ERROR: not a file: " + logFile);
                System.exit(99);
            }
            int width = RTConfig.getInt(ARG_WIDTH,22);
            int exit = 0;
            int count = 0;
            String lastUniq = "";
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(logFile);
                while (true) {
                    String s = FileTools.readLine(fis);
                    if (s == null) { break; }
                    String uniq = (s.length() <= width)? s : s.substring(0,width);
                    if (lastUniq == null) {
                        lastUniq = uniq;
                        count = 1;
                    } else
                    if (uniq.equals(lastUniq)) {
                        count++;
                    } else {
                        Print.sysPrintln("\"" + lastUniq + "\" count " + count);
                        lastUniq = uniq;
                        count = 1;
                    }
                }
                exit = 0;
            } catch (EOFException eof) {
                exit = 0;
            } catch (IOException ioe) {
                Print.logException("File error",ioe);
                exit = 1;
            } finally {
                try { fis.close(); } catch (Throwable th) {/*ignore*/}
            }
            System.exit(exit);
        }

        /* filter logs */
        // -- filters a log file by removing the line header information 
        // -  "-filterLog=dcs.log -noHeader"
        if (RTConfig.hasProperty(ARG_FILTERLOG)) {
            File logFile = RTConfig.getFile(ARG_FILTERLOG,null);
            if (!FileTools.isFile(logFile)) {
                Print.sysPrintln("ERROR: not a file: " + logFile);
                System.exit(99);
            }
            boolean noHeader = RTConfig.getBoolean(ARG_NO_HEADER,false);
            int exit = 0;
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(logFile);
                while (true) {
                    String s = FileTools.readLine(fis);
                    if (s == null) { break; }
                    // xxxx[INFO_|08/05 16:34:11|OSTools.printMemoryUsage:219] 
                    int fc = s.indexOf("[");
                    if (fc >= 0) {
                        int ec = s.indexOf("]",fc);
                        if (noHeader) {
                            s = s.substring(ec+1);
                            if (s.startsWith(" ")) {
                                s = s.substring(1);
                            }
                        } else {
                            int vb = s.indexOf("|",fc+10);
                            if ((ec >= 0) && (vb >= 0) && (fc < ec) && (vb < ec)) {
                                s = s.substring(fc,vb) + s.substring(ec);
                            }
                        }
                    }
                    Print.sysPrintln(s);
                }
                exit = 0;
            } catch (EOFException eof) {
                exit = 0;
            } catch (IOException ioe) {
                Print.logException("File error",ioe);
                exit = 1;
            } finally {
                try { fis.close(); } catch (Throwable th) {/*ignore*/}
            }
            System.exit(exit);
        }

        /* search logs */
        // -- search log files for a matching target value
        if (RTConfig.hasProperty(ARG_SEARCH)) {
            File grepDir    = RTConfig.getFile(ARG_SEARCH,null);
            File grepFile[] = null;
            if (grepDir.isFile()) {
                grepFile = new File[] { grepDir };
            } else
            if (grepDir.isDirectory()) {
                grepFile = FileTools.getFiles(grepDir);
            } else {
                Print.sysPrintln("ERROR: invalid file specification: " + grepDir);
                System.exit(99);
            }
            String matchStr = RTConfig.getString(ARG_MATCH,null);
            if (StringTools.isBlank(matchStr)) {
                Print.sysPrintln("ERROR: Search string not specified");
                System.exit(99);
            }
            String match[] = StringTools.split(matchStr,'\\');
            for (File f : grepFile) {
                FileInputStream fis = null;
                try {
                    Print.sysPrintln("-----------------------------------------------");
                    Print.sysPrintln("File: " + f);
                    fis = new FileInputStream(f);
                    int m = 0;
                    String matchLines[] = new String[match.length];
                    while (true) {
                        String s = FileTools.readLine(fis);
                        if (s == null) { break; }
                        for (int i = 0; i < m; i++) {
                            if (StringTools.indexOfIgnoreCase(s,match[i]) >= 0) {
                                matchLines[i] = s;
                                m = i + 1;
                                continue;
                            }
                        }
                        if (StringTools.indexOfIgnoreCase(s,match[m]) >= 0) {
                            matchLines[m] = s;
                            m++;
                            if (m >= match.length) {
                                Print.sysPrintln("---------");
                                for (int i = 0; i < matchLines.length; i++) {
                                    Print.sysPrintln(matchLines[i]);
                                }
                                m = 0;
                            }
                        }
                    }
                } catch (EOFException eof) {
                    // -- next file
                } catch (IOException ioe) {
                    Print.logException("File error",ioe);
                    System.exit(1);
                } finally {
                    try { fis.close(); } catch (Throwable th) {/*ignore*/}
                }
            }
            System.exit(0);
        }

        /* copy CSV */
        // -- remove any concatenation lines in first file while copying to second
        if (RTConfig.hasProperty(ARG_COPY_CSV)) {
            File csvFile = RTConfig.getFile(ARG_COPY_CSV,null);
            if (!FileTools.isFile(csvFile)) {
                Print.sysPrintln("ERROR: not a file: " + csvFile);
                System.exit(99);
            }
            File toFile  = RTConfig.getFile(ARG_TOFILE,null);
            if ((toFile != null) && toFile.exists()) {
                Print.sysPrintln("ERROR: 'toFile' already exists: " + toFile);
                System.exit(99);
            }
            int exit = 0;
            FileInputStream  fis = null;
            FileOutputStream fos = null;
            char STRIP_CR[] = new char[] { '\r' };
            try {
                fis = new FileInputStream(csvFile);
                fos = (toFile != null)? new FileOutputStream(toFile) : null;
                String p = null;
                int actualRow = 0, virtualRow = 0;
                while (true) {
                    // -- readline
                    String s = FileTools.readLine(fis,'\n');
                    if (s == null) { break; } // done
                    actualRow++;
                    // -- cancatenate previous continuation row
                    if (p != null) {
                        s = p + s;
                        p = null;
                    }
                    // -- check for continuation
                    if (s.endsWith("\\")) {
                        Print.sysPrintln("Found continuation line at " + actualRow);
                        //Print.sysPrintln("Line: " + s);
                        p = s.substring(0,s.length()-1); // remove trailing \\
                        continue;
                    }
                    // -- remove any \r
                    String r = s.replace('\r',' ');
                    if (!s.equals(r)) {
                        Print.sysPrintln("** Stripped CR at " + actualRow);
                    }
                    // -- check for non-printable chars
                    virtualRow++;
                    if (fos != null) {
                        byte b[] = r.getBytes();
                        fos.write(b, 0, b.length);
                        fos.write('\n'); // replace stripped NL above
                    } else {
                        Print.sysPrintln(r);
                    }
                }
                exit = 0;
            } catch (EOFException eof) {
                exit = 0;
            } catch (IOException ioe) {
                Print.logException("File error",ioe);
                exit = 1;
            } finally {
                try { if (fis != null) fis.close(); } catch (Throwable th) {/*ignore*/}
                try { if (fos != null) fos.close(); } catch (Throwable th) {/*ignore*/}
            }
            System.exit(exit);
        }

        /* MD5 hash */
        // -- displays the MD5 hash of the specified file
        if (RTConfig.hasProperty(ARG_MD5)) {
            File file = RTConfig.getFile(ARG_MD5,null);
            if (!FileTools.isFile(file)) {
                Print.sysPrintln("ERROR: not a file: " + file);
                System.exit(99);
            }
            Print.sysPrintln("MD5("+file+")= "+FileTools.getHash_MD5(file));
            System.exit(0);
        }

        /* SHA1 hash */
        // -- displays the SHA1 hash of the specified file
        if (RTConfig.hasProperty(ARG_SHA1)) {
            File file = RTConfig.getFile(ARG_SHA1,null);
            if (!FileTools.isFile(file)) {
                Print.sysPrintln("ERROR: not a file: " + file);
                System.exit(99);
            }
            Print.sysPrintln("SHA1("+file+")= "+FileTools.getHash_SHA1(file));
            System.exit(0);
        }

        /* SHA256 hash */
        // -- displays the SHA256 hash of the specified file
        if (RTConfig.hasProperty(ARG_SHA256)) {
            File file = RTConfig.getFile(ARG_SHA256,null);
            if (!FileTools.isFile(file)) {
                Print.sysPrintln("ERROR: not a file: " + file);
                System.exit(99);
            }
            Print.sysPrintln("SHA256("+file+")= "+FileTools.getHash_SHA256(file));
            System.exit(0);
        }

        /* is Cygwin/Windows link? */
        // -- displays whether the specified file is a Cygwin/Windows link
        if (RTConfig.hasProperty(ARG_IS_LINK)) {
            File file = RTConfig.getFile(ARG_IS_LINK,null);
            if (!FileTools.isFile(file)) {
                Print.sysPrintln("ERROR: not a file: " + file);
                System.exit(99);
            }
            //Payload.SetDebugLogging(true);
            if (FileTools.isCygwinSymlink(file)) {
                Print.sysPrintln("Cygwin symlink: " + FileTools.getCygwinSymlinkFile(file,true));
            } else
            if (FileTools.isWindowsShortcut(file)) {
                Print.sysPrintln("Windows shortcut: " + FileTools.getWindowsShortcutFile(file,true));
            } else {
                Print.sysPrintln("Unable to determine if file is a symlink/shortcut");
            }
            System.exit(0);
        }
        
        /* copy directory "contents" to destination directory */
        // -- copies the contents of the first directory to the second directory
        if (RTConfig.hasProperty(ARG_COPYDIR)) {
            // -- get canonical paths
            final File fromPath;
            final File toPath;
            { // local scope
                File fromDir = RTConfig.getFile(ARG_COPYDIR,null);
                File toDir   = RTConfig.getFile(ARG_TODIR  ,null);
                fromPath = FileTools.getRealFile(fromDir,true);
                toPath   = FileTools.getRealFile(toDir  ,true);
                if (!FileTools.isDirectory(fromPath)) {
                    Print.sysPrintln("'From' file is not a directory: " + fromDir);
                    System.exit(1);
                } else
                if (!FileTools.isDirectory(toPath)) {
                    Print.sysPrintln("'To' file is not a directory: " + toDir);
                    System.exit(2);
                }
            }
            // -- copy
            Print.sysPrintln("Copy from: " + fromPath);
            Print.sysPrintln("Copy to  : " + toPath);
            Print.sysPrintln("----------");
            final String  fromPathStr = fromPath.toString();
            final boolean progress    = RTConfig.getBoolean(ARG_PROGRESS,false);
            ProgressPrefix = " ==> ";
            FileTools.traverseAllFiles(fromPath, new FileFilter() {
                public boolean accept(File file) {
                    //Print.sysPrintln("Traversed file: " + file);
                    // -- get file path relative to 'fromPath'
                    { // local scope
                        String fileStr = file.toString();
                        if (!fileStr.startsWith(fromPathStr)) {
                            Print.sysPrintln(ProgressPrefix+"File does not appear to be relative to 'From' directory: " + file);
                            return false;
                        }
                        String relFileStr = fileStr.substring(fromPathStr.length());
                        if (relFileStr.startsWith("/")) {
                            relFileStr = relFileStr.substring(1);
                        }
                        file = new File(relFileStr);
                    }
                    //Print.sysPrintln("Relative file: " + file);
                    // -- 
                    File fromFile = new File(fromPath, file.toString());
                    File toFile   = new File(toPath  , file.toString());
                    if (fromFile.isDirectory()) {
                        Print.sysPrintln("Make dir : " + toFile);
                        if (toFile.exists()) {
                            Print.sysPrintln(ProgressPrefix+"Already exists: " + toFile);
                        } else
                        if (!toFile.mkdirs()) {
                            Print.sysPrintln(ProgressPrefix+"Unable to create directory: " + toFile);
                        }
                    } else 
                    if (fromFile.isFile()) {
                        try {
                            URL fromURL = FileTools.toURL(fromFile); // MalformedURLException
                            String fromURLPad = StringTools.padRight(fromURL.toString(), ' ', 70);
                            //Print.sysPrintln("Copy file: " + fromURLPad + " -to-  " + toFile);
                            Print.sysPrintln("Copy file: " + file);
                            if (toFile.exists()) {
                                Print.sysPrintln(ProgressPrefix+"Already exists: " + toFile);
                            } else {
                                FileTools.copyFile(fromURL, toFile, progress, 10000); // IOException
                            }
                        } catch (MalformedURLException mue) {
                            // -- unlikely
                            Print.sysPrintln(ProgressPrefix+"File could not be converted to URL: " + mue);
                        } catch (Throwable th) {
                            // -- copy error
                            Print.sysPrintln(ProgressPrefix+"Error during copy: " + th);
                        }
                    } else {
                        // -- unlikely
                        Print.sysPrintln("Unrecognized file: " + fromFile);
                    }
                    return true;
                }
            });
            // -- done
            System.exit(0);
        }

        /* done */
        usage();

    }

}
