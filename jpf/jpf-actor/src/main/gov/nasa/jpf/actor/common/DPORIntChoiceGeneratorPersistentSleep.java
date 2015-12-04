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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//import gov.nasa.jpf.jvm.IntChoiceGenerator;
import gov.nasa.jpf.jvm.choice.IntIntervalGenerator;

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
public class DPORIntChoiceGeneratorPersistentSleep extends IntIntervalGenerator { //IntChoiceGenerator {

  boolean expandOnlyOnce = false;
  boolean persistentSetExpanded = false;
  int min, max;
  int next;

  int[] enabledSet;
  int[] receivers;

  List<Integer> persistentSet; // contains ids of msgs into enabledSet
  Set<Integer> sleepSet; // contains ids of msgs
  int lastChosenMessage, lastChosenActor;
  boolean initialized = false;

  public void reset() {
    next = -1;
  }

  public DPORIntChoiceGeneratorPersistentSleep(String id, int min, int max,
                                               int[] msgIds, int[] receivers,
                                               boolean expandOnlyOnce) {
    super(id,min,max);

    // assert msgIds.length == receivers.length;
    this.min = min;
    this.max = max;
    this.expandOnlyOnce = expandOnlyOnce;
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

  // for a given actor id, this method adds all the positions
  // of the actor into persistent set
  private boolean addToSet(int actorToAdd) {
    boolean result = false;
    for (int i = 0; i < enabledSet.length; i++) {
      if (receivers[i] == actorToAdd && !sleepSet.contains(enabledSet[i])) {
        persistentSet.add(enabledSet[i]);
        result = true;
      }
    }
    return result;
  }

  private void removeFromSleepSet(int actorToAdd) {
    for (int i = 0; i < enabledSet.length; i++) {
      if (receivers[i] == actorToAdd) {
        sleepSet.remove(enabledSet[i]);
      }
    }
  }

  // This code was modified by Abdullah to use getPreviousChoiceGeneratorOfType(this.getClass())
  // instead of getPreviousChoiceGenerator().  I'm not sure if this change is "correct" but
  // I am committing it for the present. Perhaps, Rajesh can shed some light on its correctness.
  //    -Steve
  private void initialize() {
    if (this.getPreviousChoiceGeneratorOfType(this.getClass()) != null
	&& (this.getPreviousChoiceGeneratorOfType(this.getClass()) instanceof DPORIntChoiceGeneratorPersistentSleep)) {
      this.sleepSet.addAll(((DPORIntChoiceGeneratorPersistentSleep) (this.
        getPreviousChoiceGeneratorOfType(this.getClass()))).getSleepSet());
      removeFromSleepSet(((DPORIntChoiceGeneratorPersistentSleep) (this.
        getPreviousChoiceGeneratorOfType(this.getClass()))).getLastChosenActor());
      // remaining: getLastchosenactor and removes all its messages
    }

    // nondeterministic choice: pick the first one which is not in the sleep set
    int i = 0;
    int actorToAdd = receivers[i];
    while (!addToSet(actorToAdd)) {
      if ((++i) == receivers.length) {
        System.err.println("Nothing enabled. Aborted Path!!!!!!!!!!!!");
        // Stat.incStatAbortedTraceCount();
        break;
      }
      actorToAdd = receivers[i];
    }
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
        if (lastChosenActor != receivers[i])
          persistentSetExpanded = false;
        lastChosenActor = receivers[i];
        return i;
      }
    }

    return -1; // returns id of a message
  }

  public void addToPersistentSet(int messageId, int receiverId) {
    if (expandOnlyOnce && persistentSetExpanded)
      return;

    // Check if new receiver id is already in the
    // persistent set. If it is a not, then add it.
    /*
     * for (int i = 0; i < persistentSet.size(); i++) { int index =
     * persistentSet.get(i); if (enabledSet[index] == receiverId) { return; } }
     */
    if (persistentSet.contains(messageId))
      return;

    boolean result = addToSet(receiverId);
    if (result && expandOnlyOnce)
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
