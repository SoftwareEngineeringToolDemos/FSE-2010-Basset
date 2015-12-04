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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//import gov.nasa.jpf.jvm.IntChoiceGenerator;
import gov.nasa.jpf.jvm.choice.IntIntervalGenerator;

/**
 * Choice Generator that returns integer values that correspond to messages that
 * can be delivered to actors. The particular values that are returned (as well
 * as how many) is determined dynamically as the exploration of an actor system
 * proceeds.
 * 
 * This particular choice generator limits the values returned based on a
 * technique which uses dynamically computing persistent sets (see Flanagan &
 * Godefroid, 2005). The technique was adapted for testing actor programs.
 * 
 * @author Steven Lauterburg (steven.lauterburg@gmail.com)
 * @author Rajesh K. Karmani (rkumar8@illinois.edu)
 * 
 */
public class DPORIntChoiceGeneratorPersistent extends IntIntervalGenerator { //IntChoiceGenerator {

  boolean expandOnlyOnce = false;
  boolean persistentSetExpanded = false;
  int min, max;
  int next;

  int[] enabledSet;

  List<Integer> persistentSet; // contains indexes into enabledSet

  public void reset() {
    next = min - 1;
  }

  public DPORIntChoiceGeneratorPersistent(String id, int min, int max,
                                          int[] receivers,
                                          boolean expandOnlyOnce) {
    super(id,min,max);

    this.min = min;
    this.max = max;
    this.expandOnlyOnce = expandOnlyOnce;
    this.enabledSet = Arrays.copyOf(receivers, receivers.length);
    this.persistentSet = new ArrayList<Integer>();
    int actorToAdd = enabledSet[0];
    addToSet(actorToAdd);
    reset();
  }

  // for a given actor id, this method adds all the positions
  // of the actor into persistent set
  private void addToSet(int actorToAdd) {
    for (int i = 0; i < enabledSet.length; i++) {
      if (enabledSet[i] == actorToAdd) {
        persistentSet.add(i);
      }
    }
  }

  public Integer getNextChoice() {
    return persistentSet.get(next);
  }

  public void addToPersistentSet(int messageId, int receiverId) {
    if (expandOnlyOnce && persistentSetExpanded)
      return;

    // Check if new receiver id is already in the
    // persistent set. If it is a not, then add it.
    for (int i = 0; i < persistentSet.size(); i++) {
      int index = persistentSet.get(i);
      if (enabledSet[index] == receiverId) {
        return;
      }
    }

    if (expandOnlyOnce)
      persistentSetExpanded = true;
    addToSet(receiverId);
  }

  public boolean hasMoreChoices() {
    if (isDone) {
      return false;
    }
    return (next < persistentSet.size() - 1);
  }

  public void advance() {

    // checking for UNLOCK the persistent set
    int currIndex = next >= 0 ? next : 0;
    int nextIndex = currIndex + 1;
    if (nextIndex < persistentSet.size()) {
      int currId = enabledSet[persistentSet.get(currIndex)];
      int nextId = enabledSet[persistentSet.get(nextIndex)];
      // Unlock the persistent set
      if (currId != nextId)
        persistentSetExpanded = false;
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
