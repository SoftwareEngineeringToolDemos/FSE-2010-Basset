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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class DPORIntChoiceGeneratorTransistentSleep extends IntIntervalGenerator { //IntChoiceGenerator {

  private boolean persistentSetExpanded = false;
  private int min, max;
  private int next;

  private int[] enabledSet;
  private int[] receivers;

  private List<Integer> persistentSet; // contains ids of msgs into enabledSet
  private Set<Integer> sleepSet; // contains ids of msgs
  private int lastChosenMessage, lastChosenActor;
  private boolean initialized = false;

  public void reset() {
    next = -1;
  }

  public DPORIntChoiceGeneratorTransistentSleep(String id, int min, int max,
                                                int[] msgIds, int[] receivers,
                                                boolean expandOnlyOnce) {
    super(null,min,max);

    // assert msgIds.length == receivers.length;
    this.min = min;
    this.max = max;
    this.enabledSet = Arrays.copyOf(msgIds, msgIds.length);
    this.receivers = Arrays.copyOf(receivers, receivers.length);
    this.persistentSet = new ArrayList<Integer>();
    this.sleepSet = new HashSet<Integer>();
    reset();
  }

  public Set<Integer> getSleepSet() {
    return sleepSet;
  }

  public Integer getLastChosenMessage() {
    return lastChosenMessage;
  }

  public Integer getLastChosenActor() {
    return lastChosenActor;
  }

  private void removeFromSleepSet(int actorToAdd) {
    for (int i = 0; i < enabledSet.length; i++) {
      if (receivers[i] == actorToAdd) {
        sleepSet.remove(enabledSet[i]);
      }
    }
  }

  private void initialize() {
    if (this.getPreviousChoiceGeneratorOfType(this.getClass()) != null
    		&& (this.getPreviousChoiceGeneratorOfType(this.getClass()) instanceof DPORIntChoiceGeneratorTransistentSleep)) {
    	      this.sleepSet.addAll(((DPORIntChoiceGeneratorTransistentSleep) (this.
    	        getPreviousChoiceGeneratorOfType(this.getClass()))).getSleepSet());
    	      removeFromSleepSet(((DPORIntChoiceGeneratorTransistentSleep) (this.
    	        getPreviousChoiceGeneratorOfType(this.getClass()))).getLastChosenActor());
    	      // remaining: getLastchosenactor and removes all its messages
    	    }

    // nondeterministic choice: pick the first one which is not in the sleep
    // set
    int i = 0;
    while (sleepSet.contains(enabledSet[i]) && (++i) < receivers.length)
      ;
    if (i == receivers.length) {
      System.err.println("Nothing enabled. Aborted Path!!!!!!!!!!!!");
    } else
      persistentSet.add(enabledSet[i]);

    initialized = true;
  }

  public Integer getNextChoice() {
    if (!initialized) {
      this.initialize();
    }
    int choice = persistentSet.get(next);
    sleepSet.add(choice);
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
    if (persistentSet.contains(messageId) || sleepSet.contains(messageId))
      return;

    // Remove the last one if you had already expanded
    // to make place for a new actors
    if (persistentSetExpanded)
      persistentSet.remove(persistentSet.size() - 1);
    persistentSet.add(messageId);
    persistentSetExpanded = true;
  }

  public boolean hasMoreChoices() {
    if (!initialized) {
      this.initialize();
    }

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
