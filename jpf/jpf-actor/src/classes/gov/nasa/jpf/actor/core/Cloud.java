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
package gov.nasa.jpf.actor.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gov.nasa.jpf.actor.core.Platform.ReceiveEvent;
import gov.nasa.jpf.actor.icore.IActor;
import gov.nasa.jpf.actor.icore.IActorName;
import gov.nasa.jpf.actor.icore.IActorThread;
import gov.nasa.jpf.actor.icore.IMessage;
import gov.nasa.jpf.actor.util.Logger;
import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.annotation.FilterField;

import static gov.nasa.jpf.actor.util.Constants.*;

/**
 * 
 * @author Steven Lauterburg (steven.lauterburg@gmail.com)
 * @author Mirco Dotta (mirco.dotta@gmail.com)
 * @author Rajesh K. Karmani (rkumar8@illinois.edu)
 * 
 */
public class Cloud {

  private List<IMessage> pendingMessages;
  // For ST2
  private ArrayList<ReceiveEvent> receiveEventList;

  @FilterField
  private StringBuffer buf;
  @FilterField
  private static int count = 0;

  @FilterField
  private static int[] priority = null;

  @FilterField
  private int currentActorSendCount = 0;
  @FilterField
  private int currentActorId = 0;

  /***********************************************************/
  public Cloud() {
    this.pendingMessages = new ArrayList<IMessage>();
    this.buf = new StringBuffer();
  }

  /***********************************************************/
  public int countPendingMessages(IActor actor) {
    int result = 0;
    for (IMessage msg : pendingMessages) {
      if (msg.getReceiver().equals(actor))
        result++;
    }
    return result;
  }

  /***********************************************************/
  public void push(IMessage msg) {
    pendingMessages.add(msg);

    // for high send average heuristic
    currentActorSendCount++;

    // for send graph reachability heuristic
    Util.addEdge(msg.getSender().getID(), msg.getReceiver().getID());

    // System.out.println(" pushed. [pendings=" + totalPendingMessages() +
    // "] " + msg);
    Logger.verbose(this, msg + " pushed. [pendings=" + totalPendingMessages()
        + "]");
  }

  /***********************************************************/
  public IMessage get() {

    count++;

    // for high send average heuristic
    Util.addSendCount(currentActorId, currentActorSendCount);

    IMessage result = null;
    int dporType = Util.getIntegerProperty("basset.dpor");
    if (dporType == DPOR_NONE) {
      result = getNormal();
    } else if (dporType == DPOR_DCUTE || dporType == DPOR_DCUTE_FAST) {
      result = getDPOR_dcute();
    } else if (dporType == DPOR_PERSISTENT || dporType == DPOR_PERSISTENT_FAST
        || dporType == DPOR_TRANSISTENT || dporType == DPOR_TRANSISTENT_FAST) {
      result = getDPOR_persistent(dporType);
    } else if (dporType == DPOR_PERSISTENT_SLEEP
        || dporType == DPOR_TRANSISTENT_SLEEP) {
      result = getDPOR_persistent_sleep(dporType);
    } else {
      throw new RuntimeException("Unknown DPOR type " + dporType);
    }

    // for high send average heuristic
    if (result != null) {
      currentActorSendCount = 0;
      currentActorId = result.getReceiver().getID();
    }

    return result;
  };

  /***********************************************************/
  static Comparator<IMessage> compNormal = new Comparator<IMessage>() {
    @Override
    public int compare(IMessage m1, IMessage m2) {
      String n1 = m1.getReceiver().getName();
      String n2 = m2.getReceiver().getName();
      int c = n1.compareTo(n2);
      return c;
    }
  };

  /***********************************************************/
  public IMessage getSimple() {
    List<Integer> enabledMessages = new ArrayList<Integer>();

//    // sort the messages so that state comparison can
//    // potentially identify more equivalent states
//    if (Util.isUsingActorStateSerializer())
//      Collections.sort(pendingMessages, compNormal);

    // create list of enabled/deliverable messages
    for (int i = 0; i < pendingMessages.size(); i++) {
      IMessage msg = pendingMessages.get(i);
      IActorThread athread = Platform.getInstance().getActorThread(
          msg.getReceiver());
      if (athread != null && athread.getAttached().canBeDelivered(msg)) {
        enabledMessages.add(i);
      }
    }

    // return null if there are no enabled/deliverable messages
    int choiceSize = enabledMessages.size();
    if (choiceSize == 0)
      return null;

    // choose next message to deliver using appropriate choice generator
    int choice= 0;
    
    // return chosen message from the pending message list
    return pendingMessages.remove(enabledMessages.get(choice).intValue());
  }

  /***********************************************************/
  public IMessage getNormal() {
    List<Integer> enabledMessages = new ArrayList<Integer>();

    // sort the messages so that state comparison can
    // potentially identify more equivalent states
    if (Util.isUsingActorStateSerializer())
      Collections.sort(pendingMessages, compNormal);

    // create list of enabled/deliverable messages
    for (int i = 0; i < pendingMessages.size(); i++) {
      IMessage msg = pendingMessages.get(i);
      IActorThread athread = Platform.getInstance().getActorThread(
          msg.getReceiver());
      if (athread != null && athread.getAttached().canBeDelivered(msg)) {
        enabledMessages.add(i);
      }
    }

    // return null if there are no enabled/deliverable messages
    int choiceSize = enabledMessages.size();
    if (choiceSize == 0)
      return null;

    // choose next message to deliver using appropriate choice generator
    int choice;
    if (Util.getBooleanProperty("basset.bigstep")
        && Util.getBooleanProperty("basset.threadyield")) {
      throw new RuntimeException("bigstep and threadyield cannot both be true");
    } else if (Util.getBooleanProperty("basset.bigstep")
        && !Util.getBooleanProperty("basset.threadyield")) {
      choice = Verify.getInt(0, choiceSize - 1);
    } else if (!Util.getBooleanProperty("basset.bigstep")
        && Util.getBooleanProperty("basset.threadyield")) {
      choice = Verify.getInt(0, choiceSize - 1);
    } else { // if (!Util.getBooleanProperty("basset.bigstep") &&
      // !Util.getBooleanProperty("basset.threadyield")) {
      choice = Util.getIntAndForceChoiceGenerator(0, choiceSize - 1);
    }

    int[] enabledReceivers = new int[enabledMessages.size()];
    for (int i = 0; i < enabledMessages.size(); i++)
      enabledReceivers[i] = pendingMessages.get(enabledMessages.get(i))
          .getReceiver().getID();
    // System.out.println(count + ":: choices: " +
    // java.util.Arrays.toString(enabledReceivers)
    // + " choice: " + choice + " receiver: "
    // +
    // pendingMessages.get(enabledMessages.get(choice)).getReceiver().getID());

    // return chosen message from the pending message list
    return pendingMessages.remove(enabledMessages.get(choice).intValue());
  }

  /***********************************************************/
  // this is the original dpor method used for the ase paper
  private IMessage getDPOR_dcute() {
    List<IMessage> enabledMessages = new ArrayList<IMessage>();

    // create a list of enabled/deliverable messages
    for (int i = 0; i < pendingMessages.size(); i++) {
      IMessage msg = pendingMessages.get(i);
      IActorName receiver = msg.getReceiver();
      IActorThread athread = Platform.getInstance().getActorThread(receiver);
      if (athread != null && athread.getAttached().canBeDelivered(msg)) {
        enabledMessages.add(msg);
      }
    }

    // sort enabled messages based on specified heuristic
    enabledMessages = MessageOrderer.orderByHeuristic(enabledMessages);

    // return null if there are no enabled/deliverable messages
    int numEnabledMessages = enabledMessages.size();
    if (numEnabledMessages == 0)
      return null;

    // create an array of the receiver ids for the enabled messages
    int[] receiverIdArray = new int[enabledMessages.size()];
    for (int i = 0; i < enabledMessages.size(); i++) {
      receiverIdArray[i] = enabledMessages.get(i).getReceiver().getID();
    }
    // System.out.println(java.util.Arrays.toString(receiverIdArray));

    // choose next message to deliver using appropriate choice generator
    // currently we still need a cg even if these is only one choice
    // the dpor saves the corresponding cg that chose each received message
    int choice;
    if (Util.getBooleanProperty("basset.bigstep") && numEnabledMessages == 1)
      choice = 0;
    else
      choice = Util.getIntDPORDcute(0, numEnabledMessages - 1, receiverIdArray);

    // System.out.println(count + ":: choices: " +
    // java.util.Arrays.toString(receiverIdArray)
    // + " choice: " + choice + " receiver: "
    // + receiverIdArray[choice]);

    // return chosen message from the enabled message list
    // also remove chosen message from pending message list
    IMessage chosenMessage = enabledMessages.get(choice);
    boolean found = pendingMessages.remove(chosenMessage);
    assert (found);

    return chosenMessage;

    // bufferForHistory(chosenMessage);

  }

  /***********************************************************/
  // this dpor get method extends the original dcute version by
  // adding support for persistent sets
  public IMessage getDPOR_persistent(int dporType) {
    List<IMessage> enabledMessages = new ArrayList<IMessage>();

    // create a list of enabled/deliverable messages
    for (int i = 0; i < pendingMessages.size(); i++) {
      IMessage msg = pendingMessages.get(i);
      IActorName receiver = msg.getReceiver();
      IActorThread athread = Platform.getInstance().getActorThread(receiver);
      if (athread != null && athread.getAttached().canBeDelivered(msg)) {
        enabledMessages.add(msg);
      }
    }

    // sort the list of enabled messages using the specified heuristic
    enabledMessages = MessageOrderer.orderByHeuristic(enabledMessages);

    // return null if there are no enabled/deliverable messages
    int numEnabledMessages = enabledMessages.size();
    if (numEnabledMessages == 0)
      return null;

    // create an array of the receiver ids for the enabled messages
    int[] enabledMsgIdArray = new int[enabledMessages.size()];
    int[] receiverIdArray = new int[enabledMessages.size()];
    for (int i = 0; i < enabledMessages.size(); i++) {
      receiverIdArray[i] = enabledMessages.get(i).getReceiver().getID();
      enabledMsgIdArray[i] = enabledMessages.get(i).getID();
    }

    // choose next message to deliver using appropriate choice generator
    // currently we still need a cg even if these is only one choice
    // the dpor saves the corresponding cg that chose each received message
    int choice;
    if (Util.getBooleanProperty("basset.bigstep") && numEnabledMessages == 1)
      choice = 0;
    else if (dporType == DPOR_TRANSISTENT || dporType == DPOR_TRANSISTENT_FAST) {
      choice = Util.getIntDPORPersistent(0, numEnabledMessages - 1,
          enabledMsgIdArray, receiverIdArray, true);
    } else {
      choice = Util.getIntDPORPersistent(0, numEnabledMessages - 1,
          enabledMsgIdArray, receiverIdArray, false);
    }

    // System.out.println(count + ":: choices: " +
    // java.util.Arrays.toString(receiverIdArray)
    // + " choice: " + choice + " receiver: "
    // + receiverIdArray[choice]);

    // return chosen message from the enabled message list
    // also remove chosen message from pending message list
    IMessage chosenMessage = enabledMessages.get(choice);
    boolean found = pendingMessages.remove(chosenMessage);
    assert (found);

    return chosenMessage;

    // bufferForHistory(chosenMessage);

  }

  /***********************************************************/
  // this dpor get method extends the original dcute version by
  // adding support for persistent sets
  public IMessage getDPOR_persistent_sleep(int dporType) {
    List<IMessage> enabledMessages = new ArrayList<IMessage>();

    // create a list of enabled/deliverable messages
    for (int i = 0; i < pendingMessages.size(); i++) {
      IMessage msg = pendingMessages.get(i);
      IActorName receiver = msg.getReceiver();
      IActorThread athread = Platform.getInstance().getActorThread(receiver);
      if (athread != null && athread.getAttached().canBeDelivered(msg)) {
        enabledMessages.add(msg);
      }
    }

    // sort the list of enabled messages using the specified heuristic
    enabledMessages = MessageOrderer.orderByHeuristic(enabledMessages);

    // return null if there are no enabled/deliverable messages
    int numEnabledMessages = enabledMessages.size();
    if (numEnabledMessages == 0)
      return null;

    // create an array of the receiver ids for the enabled messages
    int[] enabledMsgIdArray = new int[enabledMessages.size()];
    int[] receiversArray = new int[enabledMessages.size()];
    for (int i = 0; i < enabledMessages.size(); i++) {
      enabledMsgIdArray[i] = enabledMessages.get(i).getID();
      receiversArray[i] = enabledMessages.get(i).getReceiver().getID();
    }
    // System.out.println(java.util.Arrays.toString(receiverIdArray));

    // choose next message to deliver using appropriate choice generator
    // currently we still need a cg even if these is only one choice
    // the dpor saves the corresponding cg that chose each received message
    int choice = 0;
    if (Util.getBooleanProperty("basset.bigstep") && numEnabledMessages == 1)
      choice = 0;
    else if (dporType == DPOR_TRANSISTENT_SLEEP) {
      choice = Util.getIntDPORPersistentSleep(0, numEnabledMessages - 1,
          enabledMsgIdArray, receiversArray, true);
    } else if (dporType == DPOR_PERSISTENT_SLEEP) {
      choice = Util.getIntDPORPersistentSleep(0, numEnabledMessages - 1,
          enabledMsgIdArray, receiversArray, false);
    }

    // System.out.println(count + ":: choices: " +
    // java.util.Arrays.toString(enabledMsgIdArray) +
    // java.util.Arrays.toString(receiversArray)
    // + " choice: " + choice + " receiver: "
    // + "");

    // return chosen message from the enabled message list
    // also remove chosen message from pending message list
    IMessage chosenMessage = null;
    // for (int i = 0; i < enabledMsgIdArray.length; i++) {
    // if (enabledMsgIdArray[i] == choice)
    // chosenMessage = enabledMessages.get(i);
    // }

    chosenMessage = enabledMessages.get(choice);

    boolean found = pendingMessages.remove(chosenMessage);
    assert (found);

    return chosenMessage;

    // bufferForHistory(chosenMessage);

  }

  private VectorClock getMsgRcceiverClock(IMessage sMsg) {
    for (ReceiveEvent event : receiveEventList) {
      if (event.messageId == sMsg.getID())
        return event.receiptClock;
    }
    return null;
  }

  /***********************************************************/
  private void bufferForHistory(IMessage msg) {
    /* if(Debug.isDebugMode()) buf.append("> "+msg+"\n"); */
  }

  /***********************************************************/
  public boolean hasPendingMessages() {
    return totalPendingMessages() > 0;
  }

  /***********************************************************/
  public boolean hasPendingMessages(IActor actor) {
    return countPendingMessages(actor) > 0;
  }

  /***********************************************************/
  public void printPendingMessagesInfo() {
    for (IMessage msg : pendingMessages)
      Logger.warning(this, "Pending: " + msg);
  }

  /***********************************************************/
  public int totalPendingMessages() {
    return pendingMessages.size();
  }

  /***********************************************************/
  public String toString() {
    return "Cloud";
  }

  /***********************************************************/
  public void printHistoryProcessedMessage() {
    // log.info(buf.toString());
  }

  /***********************************************************/
  // XXX: Be careful in using this. Side-effects can have very bad
  // consequences ...
  List<IMessage> getPendingMessages() {
    return pendingMessages;
  }

  public void setEventList(ArrayList<ReceiveEvent> receiveEvents) {
    // TODO Auto-generated method stub
    this.receiveEventList = receiveEvents;

  }

}
