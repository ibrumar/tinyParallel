/**
 * Copyright (c) 2011, Jordi Cortadella
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the name of the <organization> nor the
 *      names of its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package interp;

/**
 * Class to represent data in the interpreter.
 * Each data item has a type and a value. The type can be integer
 * or Boolean. Each operation asserts that the operands have the
 * appropriate types.
 * All the arithmetic and Boolean operations are calculated in-place,
 * i.e., the result is stored in the same data.
 * The type VOID is used to represent void values on function returns.
 */

import parser.*;

public class Data {
    /** Types of data */
    //public enum Type {VOID, BOOLEAN, INTEGER;}
    public String type;
    /** Type of data*/
    //private Type type;

    private boolean isVector;

    private boolean isShared;

    Data(String s) { type = s; isVector = false; isShared = true; }

    /** Copy constructor */
    Data(Data d) { type = d.type; isVector = d.isVector; isShared = d.isShared; } //this must be checked

    /** Returns the type of data */
    public String getType() { return type; }

    /** Indicates whether the data is Boolean */
    public boolean isBoolean() { return type.equals("bool"); }

    /** Indicates whether the data is integer */
    public boolean isInteger() { return type.equals("int"); }

    /** Indicates whether the data is void */
    public boolean isVoid() { return type.equals("void"); }

    /** Indicates whether the data is a vector */
    public boolean isVector() { return isVector; }
 
    public boolean isShared() { return isShared; }
    
    public void setShared(boolean value) { isShared = value; }

    /** Indicates whether the data is a vector */
    public void setVector() { isVector = true; }

    /** Returns a string representing the data in textual form. */
    public String toString() {
        return "The type is " + type + " isVector boolean has value " + isVector;
    }
    

}
