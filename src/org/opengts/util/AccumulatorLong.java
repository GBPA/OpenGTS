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
//  2009/08/07  Martin D. Flynn
//     -Initial release
//  2017/01/08  Martin D. Flynn
//     -Subclass from Number
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.math.*;

/**
*** Light weight Accumulator Long container.
*** Typically used in conditions where it is desireable to pass an accumulator
*** to an inner-class and have the value accessible from outside the inner-class.
*** (If the accumulator is to be used from different threads in a multi-threaded
*** environment, use "AtomicLong" instead).
**/

public class AccumulatorLong
    extends Number
{

    // ------------------------------------------------------------------------

    private volatile long accum = 0L;

    /**
    *** Constructor
    **/
    public AccumulatorLong()
    {
        this(0L);
    }

    /**
    *** Constructor
    *** @param val  Initial value
    **/
    public AccumulatorLong(long val)
    {
        this.accum = val;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the value of the accumulator
    *** @param v  The new value
    **/
    public void set(long v)
    {
        this.accum = v;
    }

    /**
    *** Gets the value of the accumulator
    *** @return The current value
    **/
    public long get()
    {
        return this.accum;
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the specified value to the accumulator
    *** @param v  The value to add
    **/
    public void add(long v)
    {
        this.accum += v;
    }

    /**
    *** Subtracts the specified value from the accumulator
    *** @param v  The value to subtract
    **/
    public void subtract(long v)
    {
        this.accum -= v;
    }

    // ------------------------------------------------------------------------

    /**
    *** Increment the value of the accumulator by 1
    **/
    public void increment()
    {
        ++this.accum;
    }

    /**
    *** Decrement the value of the accumulator by 1
    **/
    public void decrement()
    {
        --this.accum;
    }

    // --------------------------------

    /**
    *** Increment the value of the accumulator by 1, and return new value.
    **/
    public long preIncrement() // incrementAndGet()
    {
        return ++this.accum;
    }

    /**
    *** Decrement the value of the accumulator by 1, and return new value.
    **/
    public long preDecrement() // decrementAndGet()
    {
        return --this.accum;
    }

    // --------------------------------

    /**
    *** Increment the value of the accumulator by 1, and return value prior to increment.
    **/
    public long postIncrement() // getAndIncrement()
    {
        return this.accum++;
    }

    /**
    *** Decrement the value of the accumulator by 1, and return value prior to decrement
    **/
    public long postDecrement() // getAndDecrement()
    {
        return this.accum--;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Increment, and return the next value
    *** @return The next value
    **/
    public long next() // addAndGet(1L)
    {
        this.increment();
        return this.get();
    }

    /** 
    *** Add the specified value, and return the result
    *** @param v The value to add
    *** @return The next value
    **/
    public long next(long v) // addAndGet(v)
    {
        this.add(v);
        return this.get();
    }

    // ------------------------------------------------------------------------
    // Number methods

    /**
    *** Returns this value as a byte
    *** @return This value converted to a byte
    **/
    @SuppressWarnings("cast")
    public byte byteValue()
    {
        return (byte)this.get();
    }

    /**
    *** Returns this value as a short
    *** @return This value converted to a short
    **/
    @SuppressWarnings("cast")
    public short shortValue()
    {
        return (short)this.get();
    }

    /**
    *** Returns this value as a int
    *** @return This value converted to a int
    **/
    @SuppressWarnings("cast")
    public int intValue()
    {
        return (int)this.get();
    }

    /**
    *** Returns this value as a long
    *** @return This value converted to a long
    **/
    @SuppressWarnings("cast")
    public long longValue()
    {
        return (long)this.get();
    }

    /**
    *** Returns this value as a float
    *** @return This value converted to a float
    **/
    @SuppressWarnings("cast")
    public float floatValue()
    {
        return (float)this.get();
    }

    /**
    *** Returns this value as a double
    *** @return This value converted to a double
    **/
    @SuppressWarnings("cast")
    public double doubleValue()
    {
        return (double)this.get();
    }

    // --------------------------------

    /**
    *** Returns this value as a byte
    *** @return This value converted to a byte
    **/
    public boolean booleanValue()
    {
        return (this.longValue() != 0L)? true : false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified object is equal to this instance
    *** @param other  The other object to test
    *** @return True if the specified object is equal to this instance
    **/
    public boolean equals(Object other)
    {
        if (other instanceof AccumulatorLong) {
            return (this.get() == ((AccumulatorLong)other).get())? true : false;
        } else {
            return false;
        }
    }

    /**
    *** Returns a String representation of this instance
    *** @return A String representation of this instance
    **/
    public String toString()
    {
        return String.valueOf(this.get());
    }

    // ------------------------------------------------------------------------

}

