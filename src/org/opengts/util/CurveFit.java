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
//  Partial implementation of a ClientPacketHandler
// ----------------------------------------------------------------------------
// Change History:
//  2014/10/22  Martin D. Flynn
//     -Initial release
//  2015/08/16  Martin D. Flynn
//     -With a larger number of points, somewhere >= 10, accumulated rounding error
//      in a 64-bit double value can become sigificant.  Changes were made to also
//      support an arbitrary precision calculation using BigDecimal, however
//      performance will be impacted by using BigDecimal values in the calculation.
//     -Additional precision types added to support a partial CurveFit calculation
//      over sections of a large number of points.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
*** Curve-fit profile matrix
**/
public class CurveFit
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a MathContext for the specified number of digits 
    **/
    private static MathContext BigDecimalPrecision(int dig)
    {
        return new MathContext(dig, RoundingMode.HALF_EVEN);
    }

    /**
    *** Precision Enum
    **/
    public enum Precision {
        Double      (1,  0, "Double"  ),
        Digits16    (2, 16, "Digits16"), // 16-digits (produces a more much accurate result than Double)
        Digits18    (2, 18, "Digits18"), // 18-digits (same results as Digits34 upto ~150 points)
        Digits20    (2, 20, "Digits20"), // 20-digits (same results as Digits34 upto ~150 points)
        Digits22    (2, 22, "Digits22"), // 22-digits
        Digits24    (2, 24, "Digits24"), // 24-digits
        Digits34    (2, 34, "Digits34"), // 34-digits (tested to 233 XYPairs)
        Partial5    (3,  5, "Partial5"), // partial CurveFit over 5 points using Double precision
        Partial7    (3,  7, "Partial7"), // partial CurveFit over 7 points using Double precision
        Partial9    (3,  9, "Partial9"), // partial CurveFit over 9 points using Double precision
        Linear      (9,  0, "Linear"  ); // Linear interpolation between points
        private int         tt = 0;
        private int         dd = 0;
        private String      ss = null;
        private MathContext mc = null;
        Precision(int t, int d, String s)      { this.tt = t; this.dd = d; this.ss = s; }
        public String  toString()       { return this.ss; }
        public boolean isDouble()       { return (this.tt == 1); }
        public boolean isBigDecimal()   { return (this.tt == 2); }
        public boolean isPartial()      { return (this.tt == 3); }
        public boolean isLinear()       { return (this.tt == 9); }
        public boolean saveXY()         { return this.isPartial() || this.isLinear(); }
        public int     getPartialSize() { return this.isPartial()? this.dd : 0; }
        public MathContext getMathContext() {
            if ((this.mc == null) && this.isBigDecimal()) {
                int dig = this.dd;
                switch (this) {
                    case Digits34: this.mc = MathContext.DECIMAL128  ; break; // 34-digits
                    default      : this.mc = BigDecimalPrecision(dig); break;
                }
            }
            return this.mc; // will be null if not BigDecimal
        }
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String  PROP_CurveFit_defaultPrecision = "CurveFit.defaultPrecision";

    private static final Precision DefaultBigDecimalPrecision   = Precision.Digits20;

    /**
    *** Gets the default precision type
    **/
    public static Precision GetDefaultPrecision(int pointSize)
    {
        // -- get property defined default
        String Pn = RTConfig.getString(PROP_CurveFit_defaultPrecision,null);
        Precision P = CurveFit.GetPrecisionByName(Pn, null);
        if (P != null) {
            return P;
        }
        // -- calculate best default 
        if (pointSize < 10) {
            return Precision.Double;
        } else {
            return Precision.Partial7;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Parses the specified precision type string
    **/
    public static Precision GetPrecisionByName(String name, Precision dft)
    {
        // -- blank?
        if (StringTools.isBlank(name)) {
            // -- default precision
            return dft;
        } 
        // -- use EnumTools to find match on "toString()"
        Precision P = EnumTools.getValueOf(Precision.class, name, (Precision)null);
        if (P != null) {
            return P;
        }
        // -- check for alias names
        if (name.equalsIgnoreCase("Decimal64")) {
            // -- BigDecimal 64-bit (16 digits)
            return Precision.Digits16;
        } else
        if (name.equalsIgnoreCase("Decimal")) {
            // -- default BigDecimal precision
            return CurveFit.DefaultBigDecimalPrecision;
        } else
        if (name.equalsIgnoreCase("Decimal128")) {
            // -- BigDecimal 128-bit (34 digits)
            return Precision.Digits34;
        } else {
            Print.logWarn("Unrecognized CurveFit precision name: " + name);
            return dft;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Parse XY pair list.
    *** Leading/Trailing list characters (..) or [..], are optional.
    *** Does not reutrn null.
    **/
    public static XYPair[] ParseXYPair(String xyListStr)
    {
        return XYPair.ParseXYPair(xyListStr);
    }

    /**
    *** Parse XY pair list.
    *** Leading/Trailing list characters (..) or [..], are required.
    *** Does not reutrn null.
    **/
    public static XYPair[] ParseXYPair(String xyListStr, int fromNdx)
    {
        return XYPair.ParseXYPair(xyListStr, fromNdx);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Calculates the polynomial coefficients as "double" values
    **/
    protected static double[] _CalculateCoefficients(double M[][], double V[])
    {

        /* validate */
        if ((M == null) || (V == null) || (M.length < 2) || (M.length != V.length)) {
            return null;
        }

        /* Gaussian elimination */
        // -- algorithm obtained by comparing several web-based references
        int L = M.length; // length (>= 2)
        for (int pr = 0; pr < L; pr++) {
            // -- find/swap largest remaining row
            int mx = pr;
            for (int i = pr + 1; i < L; i++) {
                double Vi  = M[i][pr];
                double Vmx = M[mx][pr];
                if (Math.abs(Vi) > Math.abs(Vmx)) {
                    mx = i;
                }
            }
            ListTools.swap(M, pr, mx);
            ListTools.swap(V, pr, mx);
            // -- pivot
            double P = M[pr][pr]; // largest abs value
            if (P == 0.0) {
                // -- unable to complete (divide by zero)
                Print.logWarn("Unable to calculate curve-fit coefficients");
                return null;
            }
            for (int i = pr + 1; i < L; i++) {
                double c = M[i][pr] / P;
                for (int j = pr; j < L; j++) {
                    M[i][j] -= c * M[pr][j];
                }
                V[i] -= c * V[pr];
            }
        }

        /* calculate coefficients */
        double C[] = new double[L];
        for (int i = L - 1; i >= 0; i--) {
            double S = 0.0;
            for (int j = i + 1; j < L; j++) { // <-- skipped on first pass
                S += M[i][j] * C[j];
            }
            C[i] = (V[i] - S) / M[i][i];
        }

        /* return coefficients */
        return C;

    }

    /**
    *** Calculates the polynomial coefficients as "BigDecimal" values
    **/
    protected static BigDecimal[] _CalculateCoefficients(BigDecimal M[][], BigDecimal V[], MathContext mc)
    {

        /* validate */
        if ((M == null) || (V == null) || (M.length < 2) || (M.length != V.length)) {
            return null;
        }

        /* default MathContext */
        if (mc == null) {
            mc = MathContext.DECIMAL128; // Digits34
        }

        /* Gaussian elimination */
        // -- algorithm obtained by comparing several web-based references
        int L = M.length; // length (>= 2)
        for (int pr = 0; pr < L; pr++) {
            // -- find/swap largest remaining row
            int mx = pr;
            for (int i = pr + 1; i < L; i++) {
                BigDecimal Vi  = M[i][pr];
                BigDecimal Vmx = M[mx][pr];
                if (Vi.abs().compareTo(Vmx.abs()) > 0) {
                    mx = i;
                }
            }
            ListTools.swap(M, pr, mx);
            ListTools.swap(V, pr, mx);
            // -- pivot
            BigDecimal P = M[pr][pr]; // largest abs value
            if (P.equals(BigDecimal.ZERO)) {
                // -- unable to complete (divide by zero)
                Print.logWarn("Unable to calculate curve-fit coefficients");
                return null;
            }
            for (int i = pr + 1; i < L; i++) {
                BigDecimal c = M[i][pr].divide(P, mc);
                for (int j = pr; j < L; j++) {
                    M[i][j] = M[i][j].subtract(c.multiply(M[pr][j]));
                }
                V[i] = V[i].subtract(c.multiply(V[pr]));
            }
        }

        /* calculate coefficients */
        BigDecimal C[] = new BigDecimal[L];
        for (int i = L - 1; i >= 0; i--) {
            BigDecimal S = new BigDecimal(0.0, mc);
            for (int j = i + 1; j < L; j++) { // <-- skipped on first pass
                S = S.add(M[i][j].multiply(C[j]));
            }
            C[i] = V[i].subtract(S).divide(M[i][i], mc);
        }

        /* return coefficients */
        return C;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected XYPair        XY[]        = null;
    protected XYPair        minXY       = null;
    protected XYPair        maxXY       = null;

    protected Precision     precision   = null;

    protected double        dblCoeff[]  = null;
    protected BigDecimal    bigCoeff[]  = null;
    protected MathContext   bdMathCtx   = null;

    /**
    *** Protected constructor.
    *** Only allowed for subclasses
    **/
    protected CurveFit()
    {
        super();
    }

    /**
    *** Clone constructor (deep copy) 
    **/
    public CurveFit(CurveFit cf) 
    {
        this();
        if (cf != null) {
            this.precision = cf.precision;
            this.XY        = XYPair.CreateList(cf.XY); // deep copy (may be null)
            this.minXY     = XYPair.Copy(cf.minXY);    // may be null
            this.maxXY     = XYPair.Copy(cf.maxXY);    // may be null
            this.dblCoeff  = ListTools.toArray(cf.dblCoeff,0,-1); // may be null
            this.bigCoeff  = ListTools.toArray(cf.bigCoeff,0,-1); // may be null
        }
    }

    /**
    *** XYPair constructor 
    **/
    public CurveFit(XYPair xy[])
    {
        this(xy, null);
    }

    /**
    *** XYPair constructor 
    **/
    public CurveFit(XYPair xy[], Precision P)
    {
        this();
        // -- initial validation (handles xy == null)
        int xyLen = ListTools.size(xy);
        if (xyLen < 2) { // must have at least 2 points
            // -- invalid
            return;
        } 
        // -- in X-sorted order (no duplicates)?
        if (!XYPair.IsSortedByX(xy,false)) {
            // -- invalid
            return;
        }
        // -- precision
        this.precision = (P != null)? P : CurveFit.GetDefaultPrecision(xyLen);
        // -- save values
        if (this.precision.saveXY()) {
            this.XY    = XYPair.CreateList(xy);          // deep copy
            this.minXY = this.XY[0];                     // first element (local copy)
            this.maxXY = this.XY[this.XY.length - 1];    // last element (local copy)
        } else {
            // -- this.XY not save for non-Linear
            this.minXY = XYPair.Copy(xy[0]);             // first element (local copy)
            this.maxXY = XYPair.Copy(xy[xy.length - 1]); // last element (local copy)
        }
        // -- load "BigDecimal" matrix
        if (this.precision.isBigDecimal()) {
            BigDecimal M[][] = new BigDecimal[xyLen][xyLen];
            BigDecimal V[]   = new BigDecimal[xyLen];
            this.bdMathCtx   = this.precision.getMathContext(); // non-null
            for (int p = 0; p < xyLen; p++) {
                // -- extract X/Y
                double X = xy[p].getX();
                double Y = xy[p].getY();
                // -- load "BigDecimal" matrix row
                for (int i = 0; i < xyLen; i++) {
                    M[p][i] = (new BigDecimal(X,this.bdMathCtx)).pow(xyLen - i - 1);
                }
                V[p] = new BigDecimal(Y,this.bdMathCtx);
            }
            // -- calculate CurveFit coefficients
            this.bigCoeff = CurveFit._CalculateCoefficients(M, V, this.bdMathCtx); // may return null
        }
        // -- load "double" matrix
        if (this.precision.isDouble()) {
            double     M[][] = new double[xyLen][xyLen];
            double     V[]   = new double[xyLen];
            for (int p = 0; p < xyLen; p++) {
                // -- extract X/Y
                double X = xy[p].getX();
                double Y = xy[p].getY();
                // -- load "double" matrix row
                for (int i = 0; i < xyLen; i++) {
                    M[p][i] = Math.pow(X, (double)(xyLen - i - 1));
                }
                V[p] = Y;
            }
            // -- calculate CurveFit coefficients
            this.dblCoeff = CurveFit._CalculateCoefficients(M, V); // may return null
        }
    }

    /**
    *** String constructor 
    **/
    public CurveFit(String xyListStr)
    {
        this(CurveFit.ParseXYPair(xyListStr), null/*defaultPrecision*/);
    }

    /**
    *** String constructor 
    **/
    public CurveFit(String xyListStr, Precision P)
    {
        this(CurveFit.ParseXYPair(xyListStr), P);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this CurveFit is valid
    **/
    public boolean isValid() 
    {
        if (this.precision == null) {
            return false;
        } else
        if (this.precision.isDouble()) {
            return ((this.dblCoeff != null) && (this.dblCoeff.length > 0))? true : false;
        } else
        if (this.precision.isBigDecimal()) {
            return ((this.bigCoeff != null) && (this.bigCoeff.length > 0))? true : false;
        } else
        if (this.precision.isPartial()) {
            return ((this.XY != null) && (this.XY.length > 0))? true : false;
        } else
        if (this.precision.isLinear()) {
            return ((this.XY != null) && (this.XY.length > 0))? true : false;
        } else {
            return false;
        }
    }

    /**
    *** Returns true if the specified CurveFit is valid
    **/
    public static boolean isValid(CurveFit cf)
    {
        return ((cf != null) && cf.isValid())? true : false;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the minimum "X" value
    **/
    public double getMinimumX()
    {
        return (this.minXY != null)? this.minXY.getX() : Double.NaN;
    }

    /** 
    *** Gets the maximum "X" value
    **/
    public double getMaximumX()
    {
        return (this.maxXY != null)? this.maxXY.getX() : Double.NaN;
    }

    /**
    *** Gets the range of X
    **/
    public double getRangeX()
    {
        if ((this.minXY != null) && (this.maxXY != null)) {
            return this.maxXY.getX() - this.minXY.getX();
        } else {
            return Double.NaN;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the size of this CurveFit
    **/
    public int size() 
    {
        if (this.dblCoeff != null) {
            return this.dblCoeff.length;
        } else
        if (this.bigCoeff != null) {
            return this.bigCoeff.length;
        } else
        if (this.XY != null) {
            return this.XY.length;
        } else {
            return 0;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the value of Y based on the specified value for X.
    *** This function is valid only over the range specified by the originally 
    *** specified set of points.  Values of X less than the minimum range will
    *** return the minimum value for Y, ans X value greater than the maximum 
    *** range will return the maximum X value for Y.
    **/
    public double FTN(double X)
    {
        if (this.precision.isBigDecimal()) {
            // -- BigDecimal (may be time consuming to calculate)
            return this._FTN_bigDecimal(X);
        } else
        if (this.precision.isDouble()) {
            // -- 64-bit Java-Double, may not be accurate for more than 9 points
            return this._FTN_double(X);
        } else
        if (this.precision.isPartial()) {
            // -- 5/7-point CurveFit calculation range.  
            // -  Heavier than pre-calculated Double coefficients, but much more 
            // -  accurate than using Double with a larger number of points (typically
            // -  anything more than 10), ane much faster that using BigDecimal on
            // -  the entire range.
            return this._FTN_partial(X, this.precision.getPartialSize());
        } else
        if (this.precision.isLinear()) {
            // -- Linear interprolation between points.  Fast, but not as accurate.
            return this._FTN_linear(X);
        } else {
            // -- should not occur (no precision specified?)
            return 0.0;
        }
    }

    // --------------------------------

    /**
    *** Returns the value of Y based on the specified value for X.
    *** This function is valid only over the range specified by the originally 
    *** specified set of points.
    **/
    private double _FTN_double(double X)
    {

        /* get/check size */
        int L = (this.dblCoeff != null)? this.dblCoeff.length : 0;
        if (L <= 0) {
            return 0.0;
        }

        /* check X range */
        if (X <= this.minXY.getX()) {
            return this.minXY.getY();
        } else
        if (X >= this.maxXY.getX()) {
            return this.maxXY.getY();
        }

        /* calculate/return Y */
        double Yn = 0.0;
        double Xn = X;
        for (int i = 0; i < L; i++) {
            int P = L - i - 1; // L=14: 13,12,11,...
            int C = i;
            Yn += this.dblCoeff[C] * Math.pow(Xn,(double)P);
        }
        double Y = Yn; // denormalize
        return Y;

    }

    // --------------------------------

    /**
    *** Returns the value of Y based on the specified value for X.
    *** This function is valid only over the range specified by the originally 
    *** specified set of points.
    **/
    private double _FTN_bigDecimal(double X)
    {

        /* get/check size */
        int L = (this.bigCoeff != null)? this.bigCoeff.length : 0;
        if (L <= 0) {
            return 0.0;
        }

        /* check X range */
        if (X <= this.minXY.getX()) {
            return this.minXY.getY();
        } else
        if (X >= this.maxXY.getX()) {
            return this.maxXY.getY();
        }

        /* calculate/return Y */
        BigDecimal Yn = new BigDecimal(0.0, this.bdMathCtx);
        BigDecimal Xn = new BigDecimal(X, this.bdMathCtx);
        for (int i = 0; i < L; i++) {
            int P = L - i - 1; // L=14: 13,12,11,...
            int C = i;
            Yn = Yn.add(this.bigCoeff[C].multiply(Xn.pow(P)));
        }
        double Y = Yn.doubleValue(); // denormalize
        return Y;

    }

    // --------------------------------

    /**
    *** Returns the value of Y based on a partial CurveFit of the value for X.
    **/
    private double _FTN_partial(double X, int pointSize)
    {

        /* get/check size */
        int L = (this.XY != null)? this.XY.length : 0;
        if (L < 2) {
            return 0.0;
        }

        /* check X range */
        if (X <= this.minXY.getX()) {
            return this.minXY.getY();
        } else
        if (X >= this.maxXY.getX()) {
            return this.maxXY.getY();
        }

        /* start/end index */
        XYPair subXY[] = null;
        if (pointSize >= this.XY.length) {
            // -- full range
            subXY = this.XY;
        } else {
            // -- get index of point preceeding X
            int sNdx = -1;
            /* -- binary search (not necessarily faster than linear search for small N)
            int s = 0;
            int e = this.XY.length - 1;
            for (;(e - s) > 1;) {
                int p = (e + s) / 2;
                double Xi = this.XY[p].getX();
                if (X == Xi) {
                    s = p;
                    e = p;
                    break;
                } else
                if (X > Xi) {
                    s = p;
                } else
                if (X < Xi) {
                    e = p;
                }
            }
            if (s == e) {
                sNdx = s - (pointSize / 2); // 4:2 before, 5:2 before
            } else {
                sNdx = s - ((pointSize + 1) / 2); // 4:2 before, 5:3 before
            }
            */
            // -- Linear search
            for (int i = 0; i < this.XY.length; i++) {
                double Xi = this.XY[i].getX();
                if (X > Xi) {
                    sNdx = i - ((pointSize + 1) / 2); // 4:2 before, 5:3 before
                    continue;
                } else
                if (X == Xi) {
                    sNdx = i - (pointSize / 2); // 4:2 before, 5:2 before
                }
                break;
            }
            // -- adjust start index
            if (sNdx < 0) {
                sNdx = 0;
            } else
            if (sNdx > (this.XY.length - pointSize)) {
                sNdx = this.XY.length - pointSize;
            }
            // -- get sub-XYPair list
            subXY = new XYPair[pointSize];
            for (int i = 0; i < pointSize; i++) {
                subXY[i] = this.XY[sNdx + i];
            }
        }

        /* create CurveFirst over partial data */
        CurveFit cf = new CurveFit(subXY, Precision.Double);
        return cf.FTN(X);

    }

    // --------------------------------

    /**
    *** Returns the value of Y based on the specified value for X, using linear 
    *** interprolation between the nearest X points.
    *** RECOMMENDED FOR DEBUG CHECKING ONLY.
    **/
    private double _FTN_linear(double X)
    {

        /* get/check size */
        int L = (this.XY != null)? this.XY.length : 0;
        if (L <= 0) {
            return 0.0;
        }

        /* get high/low values */
        XYPair hiXY = null;
        XYPair loXY = null;
        for (int i = 0; i < this.XY.length; i++) {
            double Xi = this.XY[i].getX();
            if (Xi == X) {
                // -- exact match
                hiXY = this.XY[i];
                loXY = this.XY[i];
                break;
            } else
            if (Xi >= X) {
                // -- found range
                hiXY = this.XY[i];
                loXY = (i > 0)? this.XY[i - 1] : null;
                break;
            } else {
                // -- X < this.XY[i].getX()
                loXY = this.XY[i];
                // continue;
            }
        }

        /* calculate linear interpolation between points */
        final double Y;
        if ((hiXY != null) && (loXY != null)) {
            // -- interprolate
            double hiX = hiXY.getX();
            double loX = loXY.getX();
            if (hiX != loX) {
                // -- hi/lo differ, interprolate
                double dX  = (X - loX) / (hiX - loX);
                double hiY = hiXY.getY();
                double loY = loXY.getY();
                Y = loY + (dX * (hiY - loY));
            } else {
                // -- hi/lo are the same
                Y = loXY.getY();
            }
        } else
        if (loXY != null) {
            // -- hi is null
            Y = loXY.getY();
        } else 
        if (hiXY != null) {
            // -- lo is null
            Y = hiXY.getY();
        } else {
            // -- hi/lo are null
            Y = (X < 0.0)? 0.0 : (X > 1.0)? 1.0 : X;
        }
        return Y;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets a String representation of this instance
    **/
    public String toString()
    {
        return this.toString(new StringBuffer(),null).toString();
    }

    /**
    *** Gets a String representation of this instance
    **/
    public StringBuffer toString(StringBuffer sb, String fmt)
    {
        if (sb == null) { sb = new StringBuffer(); }
        if (this.isValid()) {
            sb.append("Precision=");
            sb.append(this.precision.toString());
            sb.append(" ");
            if (this.minXY != null) {
                sb.append("Min=");
                sb.append(this.minXY.toString());
                sb.append(" ");
            }
            if (this.maxXY != null) {
                sb.append("Max=");
                sb.append(this.maxXY.toString());
                sb.append(" ");
            }
            if (this.dblCoeff != null) {
                sb.append("Coeff=");
                for (int c = 0; c < this.dblCoeff.length; c++) {
                    if (c > 0) { sb.append(","); }
                    if (!StringTools.isBlank(fmt)) {
                        sb.append(StringTools.format(this.dblCoeff[c],fmt));
                    } else {
                        sb.append(this.dblCoeff[c]);
                    }
                }
            }
            if (this.bigCoeff != null) {
                sb.append("Coeff=");
                for (int c = 0; c < this.bigCoeff.length; c++) {
                    if (c > 0) { sb.append(","); }
                    if (!StringTools.isBlank(fmt)) {
                        sb.append(this.bigCoeff[c].toString());
                    } else {
                        sb.append(this.bigCoeff[c]);
                    }
                }
            }
        } else {
            sb.append("invalid");
        }
        return sb;
    }

    // ------------------------------------------------------------------------

    /**
    *** (Debug) Prints the matrix, values, and coefficients (for debug purposes only)
    **/
    public void print(String msg, double M[][], double V[])
    {
        Print.sysPrintln(msg + ":");
        if (M != null) {
            Print.sysPrintln("Matrix:");
            for (int m = 0; m < M.length; m++) {
                String fmt = "0.00000";
                StringBuffer sb = new StringBuffer();
                sb.append("| ");
                for (int i = 0; i < M[m].length; i++) {
                    sb.append(StringTools.format(M[m][i],fmt,fmt.length()+3));
                    sb.append(" ");
                }
                sb.append("|");
                // --
                sb.append("   ");
                // --
                sb.append("| ");
                sb.append(StringTools.format(V[m],fmt));
                sb.append(" |");
                Print.sysPrintln(sb.toString());
            }
        }
        // -- Coefficients
        if (this.dblCoeff != null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < ListTools.size(this.dblCoeff); i++) {
                sb.append(this.dblCoeff[i]);
                sb.append(",  ");
            }
            Print.sysPrintln("Coefficients: " + sb);
        }
        if (this.bigCoeff != null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < ListTools.size(this.bigCoeff); i++) {
                sb.append(this.bigCoeff[i].toString());
                sb.append(",  ");
            }
            Print.sysPrintln("Coefficients: " + sb);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String ARG_POINTS[]     = { "xyList"    , "pts" , "pairs", "xy" };
    public static final String ARG_VALUE[]      = { "value"     , "val" , "v"    , "x"  };
    public static final String ARG_PRECISION[]  = { "precision" , "pre" , "p"           };

    public static void main(String argv[])
    {
        // Cylinder:
        //  XY=[0.000,0.0000|0.125,0.0721|0.250,0.1955|0.375,0.3425|0.500,0.5000|0.625,0.6575|0.750,0.8045|0.875,0.9279|1.000,1.0000]
        //  C =-2.5928148517095906E-12,-3.9113549206247913,13.68974222220611,-20.388977777764367,16.74808888888266,-9.079324444442857,3.7157688888886873,0.22605714285715256,0.0
        //  bin/exeJava org.opengts.util.CurveFit '-xy=[0.000,0.0000|0.125,0.0721|0.250,0.1955|0.375,0.3425|0.500,0.5000|0.625,0.6575|0.750,0.8045|0.875,0.9279|1.000,1.0000]' -p=Double -value=0.7
        // --
        RTConfig.setCommandLineArgs(argv);

        /* get X/Y pairs */
        String xyListStr = RTConfig.getString(ARG_POINTS,null);
        if (StringTools.isBlank(xyListStr)) {
            Print.sysPrintln("Missing specified points: -xy=...");
            System.exit(99);
        }
        XYPair xy[] = ParseXYPair(xyListStr);
        if (RTConfig.isDebugMode()) {
            Print.sysPrintln("X/Y Pairs: ");
            Print.sysPrintln(XYPair.ToListString(xy));
        }

        /* precision */
        String precName = RTConfig.getString(ARG_PRECISION,null);
        Precision precision = GetPrecisionByName(precName, null);
        if (precision == null) {
            precision = CurveFit.GetDefaultPrecision(ListTools.size(xy));
        }
        Print.sysPrintln("Precision: " + precision);

        /* create CurveFit */
        long preCurveFitInit = System.currentTimeMillis();
        CurveFit cf = new CurveFit(xy, precision);
        long postCurveFitInit = System.currentTimeMillis();
        if (!cf.isValid()) {
            Print.sysPrintln("Invalid CurveFit points");
            System.exit(99);
        }
        if (RTConfig.isDebugMode()) {
            Print.sysPrintln("CurveFit: ");
            Print.sysPrintln(cf.toString());
            Print.sysPrintln("");
        }
        
        /* validate min/max X */
        double minX = cf.getMinimumX();
        double maxX = cf.getMaximumX();
        if (Double.isNaN(minX) || Double.isNaN(maxX)) {
            Print.sysPrintln("Invalid X min/max");
            System.exit(99);
        }

        /* value */
        String strVal = RTConfig.getString(ARG_VALUE,null);
        if (StringTools.isBlank(strVal)) {
            Print.sysPrintln("Missing '-value=' specification");
            System.exit(99);
        }

        /* simple conversion request */
        if (StringTools.isDouble(strVal,false)) {
            double X = StringTools.parseDouble(strVal,0.0);
            long preCurveFitCalc = System.currentTimeMillis();
            double Y = cf.FTN(X);
            long postCurveFitCalc = System.currentTimeMillis();
            //binarySearchTest(xy,0,X); Print.sysPrintln("");
            Print.sysPrintln(X + " ==> " + Y);
            if (RTConfig.isDebugMode()) {
                Print.sysPrintln("CurveFit init: " + (postCurveFitInit - preCurveFitInit) + " ms");
                Print.sysPrintln("CurveFit calc: " + (postCurveFitCalc - preCurveFitCalc) + " ms");
            }
            System.exit(0);
        }

        /* invalid "value" specified */
        Print.sysPrintln("ERROR: unrecognized value: " + strVal);

    }

}
