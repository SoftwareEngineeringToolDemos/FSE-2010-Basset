//
// Copyright (C) 2006 United States Government as represented by the
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
package gov.nasa.jpf.jvm.bytecode;

import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;


/**
 * Push long or double from runtime constant pool (wide index)
 * ... => ..., value
 */
public class LDC2_W extends Instruction {

  public enum Type {LONG, DOUBLE};

  protected Type type;
  protected long value;

  public LDC2_W(long l){
    value = l;
    type = Type.LONG;
  }

  public LDC2_W(double d){
    value = Double.doubleToLongBits(d);
    type = Type.DOUBLE;
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo th) {
    th.longPush(value);

    return getNext(th);
  }

  public int getLength() {
    return 3; // opcode, index1, index2
  }
  
  public int getByteCode () {
    return 0x14;
  }
  
  public Type getType() {
    return type;
  }
  
  public double getDoubleValue(){
	  if(type!=Type.DOUBLE){
		  throw new IllegalStateException();
	  }
    
	  return Double.longBitsToDouble(value);
  }
  
  public long getValue() {
    return value;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
