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

//import gov.nasa.jpf.jvm.IntChoiceGenerator;
import gov.nasa.jpf.jvm.choice.IntIntervalGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Choice Generator that returns integer values that correspond to messages that
 * can be delivered to actors. The particular values that are returned (as well
 * as how many) is determined dynamically as the exploration of an actor system
 * proceeds.
 * 
 * This particular choice generator limits the values returned based on a...
 * 
 * TODO: Rajesh should complete this description
 * 
 * @author Steven Lauterburg (steven.lauterburg@gmail.com)
 * @author Rajesh K. Karmani (rkumar8@illinois.edu)
 * 
 */
public class DPORIntChoiceGeneratorTransistent extends IntIntervalGenerator { //IntChoiceGenerator {

  private boolean persistentSetExpanded = false;
  private int min, max;
  private int next;

  private int[] enabledSet;
  private int[] receivers;

  private List<Integer> persistentSet; // contains ids of msgs into enabledSet
  private int lastChosenMessage, lastChosenActor;

  public void reset() {
    next = -1;
  }

  public DPORIntChoiceGeneratorTransistent(String id, int min, int max,
                                           int[] msgIds, int[] receivers,
                                           boolean expandOnlyOnce) {
    super(id,min,max);

    // assert msgIds.length == receivers.length;
    this.min = min;
    this.max = max;
    this.enabledSet = Arrays.copyOf(msgIds, msgIds.length);
    this.receivers = Arrays.copyOf(receivers, receivers.length);
    this.persistentSet = new ArrayList<Integer>();
    persistentSet.add(enabledSet[0]);
    reset();
  }

  public Integer getLastChosenMessage() {
    return lastChosenMessage;
  }

  public Integer getLastChosenActor() {
    return lastChosenActor;
  }

  public Integer getNextChoice() {
    int choice = persistentSet.get(next);
    lastChosenMessage = choice;

    for (int i = 0; i < enabledSet.length; i++) {
      if (enabledSet[i] == choice) {
        // if (lastChosenActor != receivers[i]) persistentSetExpanded =
        // false;
        persistentSetExpanded = false;
        lastChosenActor = receivers[i];
        return i;
      }
    }

    return -1; // returns id of a message
  }

  public void addToTransistentSet(int messageId, int receiverId) {
    if (persistentSetExpanded && lastChosenActor == receiverId)
      return;

    // Check if new receiver id is already in the
    // persistent set. If it is a not, then add it.
    if (persistentSet.contains(messageId))
      return;

    // Remove the last one if you had already expanded
    // to make place for a new actors
    if (persistentSetExpanded)
      persistentSet.remove(persistentSet.size() - 1);
    persistentSet.add(messageId);
    persistentSetExpanded = true;
  }

  public boolean hasMoreChoices() {
    if (isDone) {
      return false;
    }
    return (next < persistentSet.size() - 1);
  }

  public void advance() {
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

  // public boolean isEnabled (IMsg sMsg) {
  // for (int i = 0; i < enabledSet.length; i++) {
  // if (enabledSet[i] == sMsg.getID()) {
  // return true;
  // }
  // }
  // return false;
  // }

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
