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
//  2015/05/03  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.math.*;

/**
*** Light weight Accumulator Boolean container.
*** Typically used in conditions where it is desireable to pass an Boolean
*** to an inner-class and have the value accessible from outside the inner-class.
*** (If the Boolean is to be used from different threads in a multi-threaded
*** environment, use "AtomicBoolean" instead).
**/

public class AccumulatorBoolean
{

    // ------------------------------------------------------------------------

    private volatile boolean accum = false;

    /**
    *** Constructor
    **/
    public AccumulatorBoolean()
    {
        this(false);
    }

    /**
    *** Constructor
    *** @param val  Initial value
    **/
    public AccumulatorBoolean(boolean val)
    {
        this.accum = val;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the value of the accumulator
    *** @param v  The new value
    **/
    public void set(boolean v)
    {
        this.accum = v;
    }

    /**
    *** Gets the value of the accumulator
    *** @return The current value
    **/
    public boolean get()
    {
        return this.accum;
    }

    // ------------------------------------------------------------------------

    /**
    *** AND's the specified value with the accumulator
    *** @param v  The value to AND
    **/
    public void and(boolean v)
    {
        this.accum = this.accum && v;
    }

    /**
    *** OR's the specified value with the accumulator
    *** @param v  The value to OR
    **/
    public void or(boolean v)
    {
        this.accum = this.accum || v;
    }

    /**
    *** XOR's the specified value with the accumulator
    *** @param v  The value to XOR
    **/
    public void xor(boolean v)
    {
        this.accum = this.accum ^ v;
    }

    /**
    *** NOT's the value of the accumulator
    **/
    public void not()
    {
        this.accum = !this.accum;
    }

    // ------------------------------------------------------------------------


    /**
    *** Returns this value as a byte
    *** @return This value converted to a byte
    **/
    public boolean booleanValue()
    {
        return this.get();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified object is equal to this instance
    *** @param other  The other object to test
    *** @return True if the specified object is equal to this instance
    **/
    public boolean equals(Object other)
    {
        if (other instanceof AccumulatorBoolean) {
            return (this.get() == ((AccumulatorBoolean)other).get())? true : false;
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

    /**
    *** Returns a Boolean representation of this instance
    *** @return A Boolean representation of this instance
    **/
    public Boolean toBoolean()
    {
        return new Boolean(this.get());
    }

    // ------------------------------------------------------------------------

}

