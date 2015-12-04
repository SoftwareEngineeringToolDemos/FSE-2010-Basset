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
import java.util.List;

import gov.nasa.jpf.actor.adapter.IItemsFactory;
//import gov.nasa.jpf.actor.adapter.scala.ScalaItemsFactory;
import gov.nasa.jpf.actor.env.Environment;
import gov.nasa.jpf.actor.env.TestDriver;
import gov.nasa.jpf.actor.env.TotalEnvironment;
import gov.nasa.jpf.actor.icore.IActor;
import gov.nasa.jpf.actor.icore.IActorName;
import gov.nasa.jpf.actor.icore.IActorStateListener;
import gov.nasa.jpf.actor.icore.IActorThread;
import gov.nasa.jpf.actor.icore.IMessage;
import gov.nasa.jpf.actor.icore.IPlatformListener;
import gov.nasa.jpf.actor.util.Logger;
import gov.nasa.jpf.actor.util.Stat;
import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.annotation.FilterField;

import static gov.nasa.jpf.actor.util.Constants.*;

/**
 * This is the primary controller class for Basset that manages the overall
 * exploration of actor programs
 * 
 * @author Steven Lauterburg (steven.lauterburg@gmail.com)
 * @author Mirco Dotta (mirco.dotta@gmail.com)
 * @author Rajesh K. Karmani (rkumar8@illinois.edu)
 * 
 */
public class Platform implements IActorStateListener {

  private static Platform platform;

  public static Platform getInstance() {
    assert (platform != null);
    return platform;
  }

  // private final LinkedList<String> path = new LinkedList<String>();

  @FilterField
  private String testDriver;
  private String language;

  @FilterField
  private final IItemsFactory factory;

  protected Cloud cloud;
  protected ActorRegistry actors;

  public Platform(IItemsFactory factory) {
    this.factory = factory;
    this.cloud = new Cloud();
    this.actors = new ActorRegistry();
    platform = this;
  }

  /**
   * Initial set up loop.
   * 
   * No exploration of message delivery schedules is performed during set up.
   * Messages are just processed in FIFO order without choice generators.
   */
  public void setUp() {

    while (true) {
      if (unstarted) { // this is used only by the Scala instantiation
        runReadyToStartActors();
        continue;
      }

      IMessage choice = cloud.getSimple();

      if (choice == null)
        break;

      deliver(choice);
    }
  }

  /**
   * Master test loop.
   * 
   * Choice generators are utilized to explore an actor program's different
   * message delivery schedules.
   */
  public void test() {
    Logger.debug(this, " Exploration Started");

    IMessage[] early_sleep = null;
    while (true) {
      if (unstarted) { // this is used only by the Scala instantiation
        runReadyToStartActors();
        continue;
      }

      IMessage choice = cloud.get();

      if (choice == null)
        break;

      Stat.incStatMessageDeliveryCount();
      deliver(choice);

      // exclusively for DPOR_TRANSISTENT**
      int dpor = Util.getIntegerProperty("basset.dpor");
      if (dpor == DPOR_TRANSISTENT_SLEEP || dpor == DPOR_TRANSISTENT) {
        traverseStack(choice);
      }

      if (Util.getTimeout()) {
        Verify.terminateSearch();
      }
    }

    checkForDeadlock();
    checkForUndeliveredMessages();

    // end of trace processing
    Stat.incStatJPFTraceCount();
    notifyTraceEnd();

    Logger.debug(this, " Trace " + Stat.getStatJPFTraceCount() + " Ended");

    cloud.printHistoryProcessedMessage();

    if (Util.getBooleanProperty("basset.tracestats"))
      printEndOfTraceStatistics();

    // for (String x : path) {
    // System.out.println(x);
    // }

    Verify.ignoreIf(true);
  }

  /**
   * This method processes sends and calls by adding them to the "cloud" of
   * messages that are in transit and incrementing the number of messages sent
   * statistic.
   * 
   * If dynamic partial order reduction is enabled, this method also invokes the
   * appropriate procedures to vector clock processing and any necessary updates
   * for the choice generators related to message handling
   * 
   */
  /**********************************************************/
  public void push(IMessage msg, boolean isSystemMessage) {

    // Vector clock and DPOR processing:
    // if isSystemMessage=true, the message origin is PlatformUtil (e.g.,
    // initialization sends in the driver) or something like that. The
    // message does not have a proper sender and should not be considered
    // for POR processing.
    int dpor = Util.getIntegerProperty("basset.dpor");
    if (dpor != DPOR_NONE && !isSystemMessage) {
      if (dpor == DPOR_DCUTE || dpor == DPOR_DCUTE_FAST) {
        pushDPORDcute(msg);
      } else if (dpor == DPOR_PERSISTENT || dpor == DPOR_PERSISTENT_FAST
          || dpor == DPOR_PERSISTENT_SLEEP) {
        pushDPORPersistent(msg);
      } else if (dpor == DPOR_TRANSISTENT || dpor == DPOR_TRANSISTENT_FAST
          || dpor == DPOR_TRANSISTENT_SLEEP) {
        pushDPORTransistent(msg);
      } else {
        throw new RuntimeException("Invalid DPOR specified: " + dpor);
      }
    }

    cloud.push(msg);
    Stat.incStatMessageSendCount();
  }

  /**********************************************************/
  private void pushDPORDcute(IMessage msg) {
    // get sender thread
    IActorThread sender = getActorThread(msg.getSender());

    // update sender thread clock to reflect the send event
    int senderId = sender.getActorName().getID();
    int receiverId = msg.getReceiver().getID();
    VectorClock senderClock = sender.getVectorClock();
    senderClock.setClock(senderId, senderClock.getClock(senderId) + 1);

    // set clock for the outgoing message
    VectorClock messageClock = new VectorClock(senderClock.getVector());
    msg.setVectorClock(messageClock);
    // System.out.println("== send " + msg.getID() + " from " + sender +
    // " to " + receiverId + " thread clock: " + senderClock);

    // if the current send event (a send to actor receiverId)
    // is independent (i.e. not causally related???) from a
    // previously executed receive event by that same actor (receiverId),
    // then set the needs_delay flag for the choice generator
    // corresponding to that receive event.
    for (int i = 0; i < receiveEventList.size(); i++) {
      ReceiveEvent event1 = receiveEventList.get(i);
      if (event1.receiverId == receiverId) {
        if (!event1.receiptClock.isLT(senderClock)
            && !senderClock.isLT(event1.receiptClock)) {

          Util.setNeedsDelayFlag(event1.currentCG);
          if (Util.getIntegerProperty("basset.dpor") == DPOR_DCUTE_FAST) {
            break; // The Rajesh Enhancement
          }
          // System.out.println("DELAY: " + event.currentCG);
        }
      }
    }
  }

  /**********************************************************/
  private void pushDPORPersistent(IMessage msg) {
    // get sender thread
    IActorThread sender = getActorThread(msg.getSender());

    // update sender thread clock to reflect the send event
    int senderId = sender.getActorName().getID();
    int receiverId = msg.getReceiver().getID();
    VectorClock senderClock = sender.getVectorClock();
    senderClock.setClock(senderId, senderClock.getClock(senderId) + 1);

    // set clock for the outgoing message
    VectorClock messageClock = new VectorClock(senderClock.getVector());
    msg.setVectorClock(messageClock);
    // System.out.println("== send " + msg.getID() + " from " + sender +
    // " to " + receiverId + " thread clock: " + senderClock);

    // if the current send event (a send to actor receiverId)
    // is independent (i.e. not causally related???) from a
    // previously executed receive event by that same actor (receiverId),
    // then update the persistent set for the choice generator
    // corresponding to that previously executed receive event.
    for (int i = 0; i < receiveEventList.size(); i++) {
      ReceiveEvent event1 = receiveEventList.get(i);
      if (event1.receiverId == receiverId) {
        if (!event1.receiptClock.isLT(senderClock)
            && !senderClock.isLT(event1.receiptClock)) {

          // continue searching through the previously executed
          // receive events
          // for the next receive event that is a causal predecessor
          // to the current send event.
          for (int j = i + 1; j < receiveEventList.size(); j++) {
            ReceiveEvent event2 = receiveEventList.get(j);
            if (event2.receiptClock.isLT(senderClock)) {
              // when this 2nd event is found, add the messageId
              // and receiverId of this event to the persistent
              // set of the 1st event's choice generator
              if (Util.getIntegerProperty("basset.dpor") == DPOR_PERSISTENT_SLEEP)
                Util.addToPersistentSet_Sleep(event1.currentCG,
                    event2.messageId, event2.receiverId);
              else
                Util.addToPersistentSet(event1.currentCG, event2.messageId,
                    event2.receiverId);
              break;
            }
          }

          if (Util.getIntegerProperty("basset.dpor") == DPOR_PERSISTENT_FAST
              || Util.getIntegerProperty("basset.dpor") == DPOR_TRANSISTENT_FAST) {
            break; // The Rajesh Enhancement
          }
          // System.out.println("ADD TO PERSISTENT: " +
          // event1.currentCG);
        }
      }
    }
  }

  /**********************************************************/
  private void pushDPORTransistent(IMessage msg) {
    // get sender thread
    IActorThread sender = getActorThread(msg.getSender());

    // update sender thread clock to reflect the send event
    int senderId = sender.getActorName().getID();
    int receiverId = msg.getReceiver().getID();
    VectorClock senderClock = sender.getVectorClock();
    senderClock.setClock(senderId, senderClock.getClock(senderId) + 1);

    // set clock for the outgoing message
    VectorClock messageClock = new VectorClock(senderClock.getVector());
    msg.setVectorClock(messageClock);
    // System.out.println("== send " + msg.getID() + " from " + sender +
    // " to " + receiverId + " thread clock: " + senderClock);

    // if the current send event (a send to actor receiverId)
    // is independent (i.e. not causally related???) from a
    // previously executed receive event by that same actor (receiverId),
    // then update the persistent set for the choice generator
    // corresponding to that previously executed receive event.
    for (int i = 0; i < receiveEventList.size(); i++) {
      ReceiveEvent event1 = receiveEventList.get(i);
      if (event1.receiverId == receiverId) {
        if (!event1.receiptClock.isLT(senderClock)
            && !senderClock.isLT(event1.receiptClock)) {

          // continue searching through the previously executed
          // receive events
          // for the next receive event that is a causal predecessor
          // to the current send event.
          for (int j = i + 1; j < receiveEventList.size(); j++) {
            ReceiveEvent event2 = receiveEventList.get(j);
            if (event2.receiptClock.isLT(senderClock)) {
              // when this 2nd event is found, add the messageId
              // and receiverId of this event to the persistent
              // set of the 1st event's choice generator
              if (Util.getIntegerProperty("basset.dpor") == DPOR_TRANSISTENT)
                Util.addToTransistentSet(event1.currentCG, event2.messageId,
                    event2.receiverId);
              else if (Util.getIntegerProperty("basset.dpor") == DPOR_TRANSISTENT_SLEEP)
                Util.addToTransistentSet_Sleep(event1.currentCG,
                    event2.messageId, event2.receiverId);
              break;
            }
          }

          if (Util.getIntegerProperty("basset.dpor") == DPOR_PERSISTENT_FAST
              || Util.getIntegerProperty("basset.dpor") == DPOR_TRANSISTENT_FAST) {
            break; // The Rajesh Enhancement
          }
          // System.out.println("ADD TO PERSISTENT: " +
          // event1.currentCG);
        }
      }
    }
  }

  /**********************************************************/
  private void traverseStack(IMessage msg) {
    // get sender thread
    IActorThread sender = getActorThread(msg.getSender());

    // update sender thread clock to reflect the send event
    int receiverId = msg.getReceiver().getID();

    // get clock for the outgoing message
    VectorClock messageClock = msg.getVectorClock();

    // if the current send event (a send to actor receiverId)
    // is independent (i.e. not causally related???) from a
    // previously executed receive event by that same actor (receiverId),
    // then update the persistent set for the choice generator
    // corresponding to that previously executed receive event.
    for (int i = 0; i < receiveEventList.size(); i++) {
      ReceiveEvent event1 = receiveEventList.get(i);
      if (event1.receiverId == receiverId) {
        if (!event1.receiptClock.isLT(messageClock)) {

          int dpor = Util.getIntegerProperty("basset.dpor");
          boolean predFound = false;
          // continue searching through the previously executed
          // receive events
          // for the next receive event that is a causal predecessor
          // to the current send event.
          for (int j = i + 1; j < receiveEventList.size(); j++) {
            ReceiveEvent event2 = receiveEventList.get(j);
            if (event2.receiptClock.isLT(messageClock)) {
              predFound = true;
              if (dpor == DPOR_TRANSISTENT_SLEEP)
                Util.addToTransistentSet_Sleep(event1.currentCG,
                    event2.messageId, receiverId);
              else if (dpor == DPOR_TRANSISTENT)
                Util.addToTransistentSet(event1.currentCG, event2.messageId,
                    receiverId);
              break;
            }
          }

          if (!predFound) {
            if (dpor == DPOR_TRANSISTENT_SLEEP)
              Util.addToTransistentSet_Sleep(event1.currentCG, msg.getID(),
                  receiverId);
            else if (dpor == DPOR_TRANSISTENT)
              Util.addToTransistentSet(event1.currentCG, msg.getID(),
                  receiverId);
          }
        }
      }
    }
  }

  /**
   * This method forwards any requests to create a message to the appropriate
   * item factory
   * 
   */
  /***********************************************************/
  public IMessage createMessage(Object[] args) {
    return factory.createMessage(args);
  }

  /**
   * returns the number of pending messages for the specified actor
   * 
   */
  /***********************************************************/
  public int getMailboxSize(IActorName actor) {
    return cloud.countPendingMessages(getActor(actor));
  }

  /***********************************************************/
  private void checkForDeadlock() {
    Environment env = null;
    TestDriver driverInstance = null;
    try {
      // XXX: This could be actually factored out and put in the
      // ItemsFactor since how classes are compiled is language-dependent 
      // and I cannot find a general solution that would fit in the Platform 
      // (which is where things are supposed to be language-INDEPENDENT...

      Class<?> klass = Class.forName(testDriver);
      Class<?> sklass = klass.getSuperclass();
 
      // if (factory instanceof ScalaItemsFactory && sklass.equals(Object.class)) {
      if (Util.getProperty("basset.language").equals("scala") && sklass.equals(Object.class)) {
        // if super class is object it could be because of how Scala
        // compiles "object", i.e., it always append a "$" at the
        // end of the class (object) name.
        klass = Class.forName(testDriver + "$");
        sklass = klass.getSuperclass();
      }

      if (sklass.equals(TestDriver.class)) {
        // XXX: This is bad, but how can I retrieve the driver object
        // instance? The Scala code uses an object, which is a
        // singleton. ActorFoundry creates an instance.
        // A general solution is needed.
        driverInstance = (TestDriver) sklass.newInstance();
        env = driverInstance.getEnvironment();
      }
    } catch (Exception e) {
    }
    // [FIXME]: Drivers that do not extend the TestDriver class are by
    // default considered to have a total environment. This should be
    // removed once and if we will force compilation failure if the test
    // driver class do not extend TestDriver.
    if (env == null)
      env = new TotalEnvironment();

    List<IActor> deadlocked = actors.getWaitingOnReplyActors();

    if (env.isTotal()) {
      deadlockDetected(deadlocked);
    } else {
      deadlockDetected(deadlocked);

      List<IActor> waiting = actors.getWaitingActors();
      List<?> msgs = env.finalMessages();
      if (msgs == null || msgs.size() == 0) {
        deadlockDetected(waiting);
      } else {
        List<IActor> aliveDeadlocked = new ArrayList<IActor>();
        boolean stucked = true;
        for (IActor actor : waiting) {
          for (Object msg : msgs) {
            if (actor.canBeDelivered(new CoreMessage(null, msg, actor
                .getActorName()))) {
              stucked = false;
              break;
            }
          }
          if (stucked)
            aliveDeadlocked.add(actor);
        }
        deadlockDetected(aliveDeadlocked);
      }
    }
  }

  /***********************************************************/
  private void deadlockDetected(List<IActor> deadlocked) {
    if (deadlocked.isEmpty())
      return;

    Stat.incStatDeadlocks();
    if (Util.getBooleanProperty("basset.tracestats")) {
      StringBuffer buf = new StringBuffer("DEADLOCK DETECTED!!\n");
      buf.append("Actors:\n");
      for (IActor actor : deadlocked)
        buf.append("    " + actor.getActorName() + "\n");
      buf.append("cannot make progress\n");
      buf
          .append("-------------------------------------------------------------");
      Logger.error(buf.toString());
    }
  }

  /***********************************************************/
  private void checkForUndeliveredMessages() {
    // warn user of undelivered messages, which may be the source of a
    // potential deadlock
    if (cloud.hasPendingMessages()) {
      Stat.incStatMessageDeliveryFailCount();
      if (Util.getBooleanProperty("basset.tracestats")) {
        List<IActor> deadlocked = actors.getWaitingOnReplyActors();
        List<IActor> idle = actors.getWaitingActors();

        List<IMessage> undeliv2deadlocked = new ArrayList<IMessage>();
        List<IMessage> undeliv2idle = new ArrayList<IMessage>();
        List<IMessage> undeliv2death = new ArrayList<IMessage>();

        List<IMessage> pendings = cloud.getPendingMessages();
        IActorName rcv = null;
        boolean added;

        for (IMessage msg : pendings) {
          added = false;
          rcv = msg.getReceiver();

          for (IActor actor : deadlocked) {
            if (actor.equalsActorName(rcv)) {
              undeliv2deadlocked.add(msg);
              added = true;
              break;
            }
          }

          if (added)
            continue;

          for (IActor actor : idle) {
            if (actor.equalsActorName(rcv)) {
              undeliv2idle.add(msg);
              added = true;
              break;
            }
          }

          if (added)
            continue;

          undeliv2death.add(msg);
        }

        // building cloud information report
        StringBuffer cloudInfo = new StringBuffer();

        if (!undeliv2deadlocked.isEmpty()) {
          cloudInfo.append("Messages:\n");
          for (IMessage msg : undeliv2deadlocked)
            cloudInfo.append("    " + msg + "\n");
          cloudInfo
              .append("cannot be delivered because the receiver actors are blocked because of a synchronous communication.\n");
        }
        if (!undeliv2idle.isEmpty()) {
          cloudInfo.append("Messages:\n");
          for (IMessage msg : undeliv2idle)
            cloudInfo.append("    " + msg + "\n");
          cloudInfo
              .append("cannot be delivered. The receiver actors are alive but their constraints don't allow the delivery of these messages.\n");
        }
        if (!undeliv2death.isEmpty()) {
          cloudInfo.append("Messages:\n");
          for (IMessage msg : undeliv2death)
            cloudInfo.append("    " + msg + "\n");
          cloudInfo
              .append("cannot be delivered because the receiver actors are no longer alive.\n");
        }

        cloudInfo
            .append("-------------------------------------------------------------\n");
        Logger.warning(this, cloudInfo.toString());
      }
    }
  }

  /***********************************************************/
  private void printEndOfTraceStatistics() {
    System.out.println("\n------------------------------------ end trace "
        + Stat.getStatJPFTraceCount() + "\n");
  }

  /***********************************************************/
  private void runReadyToStartActors() {
    if (unstarted) {
      unstarted = false;
      List<IActorThread> list = actors.getReadyActorThreads();
      // assert list.size() > 0;
      for (int i = 0; i < list.size(); i++) {
        // log.debug(this,"")
        list.get(i).yieldNow();
      }
    }
  }

  /***********************************************************/
  class ReceiveEvent {
    VectorClock receiptClock;
    int currentCG;
    int messageId;
    int receiverId;

    ReceiveEvent(VectorClock receiptClock, int currentCG, int messageId,
                 int receiverId) {
      this.receiptClock = receiptClock;
      this.currentCG = currentCG;
      this.messageId = messageId;
      this.receiverId = receiverId;
    }

    public String toString() {
      return "receiver: " + receiverId + " message: " + messageId + " cg: "
          + currentCG + " clock: " + receiptClock;
    }
  }

  /***********************************************************/
  ArrayList<ReceiveEvent> receiveEventList = new ArrayList<ReceiveEvent>();

  protected void deliver(IMessage msg) {
    // get receiver thread
    IActorThread receiver = getActorThread(msg.getReceiver());

    // update path
    // String record = "message " + msg.getID() + " delivered to: "
    // + msg.getReceiver();
    // System.out.println(record);
    // path.add(record);

    // if POR is active
    int dpor = Util.getIntegerProperty("basset.dpor");
    if (dpor != DPOR_NONE) {
      // if (dpor == DPOR_DCUTE || dpor == DPOR_DCUTE_FAST
      // || dpor == DPOR_PERSISTENT || dpor == DPOR_PERSISTENT_FAST
      // || dpor == DPOR_NEW_PERSISTENT || dpor == DPOR_NEW_PERSISTENT_FAST) {

      // process clock
      VectorClock currThreadClock = receiver.getVectorClock();
      VectorClock messageClock = msg.getVectorClock();
      VectorClock newThreadClock = currThreadClock.getMax(messageClock);
      int receiverId = receiver.getActorName().getID();
      newThreadClock.setClock(receiverId,
          newThreadClock.getClock(receiverId) + 1);
      receiver.setVectorClock(newThreadClock);

      // dpor -- save save the event clock and current choice generator
      VectorClock receiptClock = new VectorClock(newThreadClock.getVector());
      // System.out.println("== receive " + msg.getID() + " at " +
      // receiver + " thread clock: " + newThreadClock);
      int currentCG = Util.getCurrentCG();
      receiveEventList.add(new ReceiveEvent(receiptClock, currentCG, msg
          .getID(), receiverId));
    }

    // deliver the message and yield
    receiver.deliver(msg);
    receiver.yieldNow();
  }

  /***********************************************************/
  IActorThread getActorThread(IActorName name) {
    return actors.get(name);
  }

  /***********************************************************/
  public IActor getActor(IActorName name) {
    IActorThread athread = actors.get(name);
    return athread.getAttached();
  }

  /***********************************************************/
  public IActor createActor(Object ref) {
    Object[] actorObjects = factory.createActorObjects(ref);
    IActorName actorName = (IActorName) actorObjects[0];
    IActor actor = (IActor) actorObjects[1];
    IActorThread athread = (IActorThread) actorObjects[2];

    registerActor(actor, athread);

    return actor;
  }

  /***********************************************************/
  public void registerActor(IActor actor, IActorThread athread) {
    Stat.incStatActorCreateCount();
    actors.register(athread);
    addListener(actor);
    actor.addStateListener(this);
  }

  /***********************************************************/
  private List<IPlatformListener> observers = new ArrayList<IPlatformListener>();

  private void addListener(IPlatformListener o) {
    observers.add(o);
    Logger.debug(this, "now observing " + o);
  }

  /***********************************************************/
  private void removeListener(IPlatformListener o) {
    assert observers.contains(o);
    observers.remove(o);
    Logger.debug(this, "no longer observing " + o);
  }

  /***********************************************************/
  public void yieldNow() {
    Util.yieldToPlatform();
  }

  /***********************************************************/
  private void notifyTraceEnd() {
    for (int i = observers.size() - 1; i >= 0; i--) {
      observers.get(i).platformExplorationEnded();
    }
  }

  /***********************************************************/
  @Override
  public String toString() {
    return "Platform";
  }

  /***********************************************************/
  private boolean unstarted = false;

  public void notifyActorSuspended(IActor actor) {
    unstarted = true;
    // Fire actions for actor ready
  }

  /***********************************************************/
  public void notifyActorActive(IActor actor) {
    // Fire actions for actor active
  }

  /***********************************************************/
  public void notifyActorWaiting(IActor actor) {
    // Fire actions for actor waiting
  }

  /***********************************************************/
  public void notifyActorWaitingOnReply(IActor actor) {
    // Fire actions for actor waiting
  }

  /***********************************************************/
  public void notifyActorTerminated(IActor actor) {

  }

  /***********************************************************/
  public void notifyActorDestroyed(IActor actor) {
    removeListener(actor);
    // TODO: This is ugly
    actors.unregister(actor.getAttachedActorThread());
  }

  /***********************************************************/
  public void setTestDriverName(String userDriverMain) {
    assert testDriver == null;
    testDriver = userDriverMain;
  }

}
