//
// Copyright (C) 2010 United States Government as represented by the
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
package gov.nasa.jpf.actor.common;

import java.util.Arrays;
//import gov.nasa.jpf.jvm.IntChoiceGenerator;
import gov.nasa.jpf.jvm.choice.IntIntervalGenerator;

/**
 * Choice Generator that returns integer values that correspond to messages that
 * can be delivered to actors. The particular values that are returned (as well
 * as how many) is determined dynamically as the exploration of an actor system
 * proceeds.
 * 
 * This particular choice generator limits the values returned based on the
 * technique used in dCUTE (see Sen & Agha, 2006).
 * 
 * @author Steven Lauterburg (steven.lauterburg@gmail.com)
 * @author Rajesh K. Karmani (rkumar8@illinois.edu)
 * 
 */
public class DPORIntChoiceGeneratorDcute extends IntIntervalGenerator { //IntChoiceGenerator {

  boolean needs_delay = false;
  int min, max;
  int next;
  int[] receivers;

  public void reset() {
    next = min - 1;
  }

  public DPORIntChoiceGeneratorDcute(String id, int min, int max,
                                     int[] receivers) {
    super(id,min,max);

    this.min = min;
    this.max = max;
    this.receivers = Arrays.copyOf(receivers, receivers.length);
    reset();
  }

  public Integer getNextChoice() {
    return new Integer(next);
  }

  public void setNeedsDelayFlag(boolean flag) {
    // System.out.println("needs delay");
    needs_delay = flag;
  }

  int currProcess;
  int nextProcess;
  public boolean hasMoreChoices() {
    // System.out.println(Arrays.toString(receivers) + " nd? " + needs_delay);
    if (isDone) {
      return false;
    }
    if (next >= max) {
      return false;
    } else {
      currProcess = (next >= 0) ? receivers[next] : receivers[0];
      nextProcess = receivers[next + 1];
      // System.out.println("cp:" + currProcess + " np:" + nextProcess);

      if ((currProcess == nextProcess) || needs_delay) {
        return true;
      }
      return false;
    }
  }

  public void advance() {
    if (currProcess != nextProcess) {
      needs_delay = false;
    }
    next += 1;
  }

  public int getTotalNumberOfChoices() {
    return (Math.abs(max - min) + 1);
  }

  public int getProcessedNumberOfChoices() {
    if (next < min)
      return 0;
    return (Math.abs(next - min) + 1);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getName());
    if (id == null) {
      sb.append('[');
    } else {
      sb.append("[id=\"");
      sb.append(id);
      sb.append("\",");
    }
    sb.append(min);
    sb.append("..");
    sb.append(max);
    sb.append(",cur=");
    sb.append(getNextChoice());
    sb.append(']');
    return sb.toString();
  }

}
