//
// Copyright (C) 2008 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.jvm;

import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.util.IntVector;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * element values for short[] objects
 */
public class ShortArrayFields extends ArrayFields {

  short[] values;

  public ShortArrayFields (int length) {
    values = new short[length];
  }

  public short[] asShortArray() {
    return values;
  }

  protected void printValue(PrintStream ps, int idx){
    ps.print(values[idx]);
  }
  
  public Object getValues(){
    return values;
  }

  public int arrayLength() {
    return values.length;
  }

  public int getHeapSize() {  // in bytes
    return values.length * 2;
  }

  public void appendTo (IntVector v) {
    v.appendPacked(values);
  }

  public ShortArrayFields clone(){
    ShortArrayFields f = (ShortArrayFields)cloneFields();
    f.values = values.clone();
    return f;
  }

  public boolean equals (Object o) {
    if (o instanceof ShortArrayFields) {
      ShortArrayFields other = (ShortArrayFields)o;
      short[] v = values;
      short[] vOther = other.values;
      if (v.length != vOther.length) {
        return false;
      }

      for (int i=0; i<v.length; i++) {
        if (v[i] != vOther[i]) {
          return false;
        }
      }

      return compareAttrs(other);

    } else {
      return false;
    }
  }


  public short getShortValue(int pos) {
    return values[pos];
  }

  public void setShortValue (int pos, short newValue) {
    values[pos] = newValue;
  }

  public void hash(HashData hd) {
    short[] v = values;
    for (int i=0; i < v.length; i++) {
      hd.add(v[i]);
    }
  }

}
