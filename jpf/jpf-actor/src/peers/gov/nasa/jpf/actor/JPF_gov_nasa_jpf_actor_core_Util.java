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
package gov.nasa.jpf.actor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.actor.common.DPORIntChoiceGeneratorDcute;
import gov.nasa.jpf.actor.common.DPORIntChoiceGeneratorPersistent;
import gov.nasa.jpf.actor.common.DPORIntChoiceGeneratorPersistentSleep;
import gov.nasa.jpf.actor.common.DPORIntChoiceGeneratorTransistent;
import gov.nasa.jpf.actor.common.DPORIntChoiceGeneratorTransistentSleep;
import gov.nasa.jpf.actor.core.CoreActorName;
import gov.nasa.jpf.actor.util.MJICopier;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.IntChoiceGenerator;
import gov.nasa.jpf.jvm.JPF_java_lang_Thread;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.ThreadList;
import gov.nasa.jpf.jvm.choice.IntIntervalGenerator;
import gov.nasa.jpf.jvm.choice.ThreadChoiceFromSet;

//import gov.nasa.jpf.jvm.abstraction.filter.ActorFilteringSerializer;

/**
 * 
 * @author Steven Lauterburg (steven.lauterburg@gmail.com)
 * @author Rajesh K. Karmani (rkumar8@illinois.edu)
 * 
 */
public class JPF_gov_nasa_jpf_actor_core_Util {

  /*******************************************************/
  static boolean searchTerminated = false;

  public static void terminateSearch(MJIEnv env, int clsref) {
    searchTerminated = true;
  }

  public static boolean isSearchTerminated(MJIEnv env, int clsref) {
    return searchTerminated;
  }

  /*******************************************************/
  public static int copy(MJIEnv env, int classRef, int objRef) {
    // return the JPF reference for the new object
    int copyRef = new MJICopier().copy(env, objRef);
    return copyRef;
  }

  /*******************************************************
   * Support for "actor send average" ordering heuristic
   *******************************************************/
  static private Map<Integer, AveragePair> sendAverage = new HashMap<Integer, AveragePair>();

  static class AveragePair {
    private int sumOfCounts = 0;
    private int numberOfCounts = 0;

    void addCount(int count) {
      sumOfCounts += count;
      numberOfCounts++;
    }

    double getAverage() {
      return (double) sumOfCounts / (double) numberOfCounts;
    }
  }

  public static void addSendCount(MJIEnv env, int clsObjRef, int actorId,
                                  int count) {
    if (sendAverage.containsKey(actorId)) {
      sendAverage.get(actorId).addCount(count);
    } else {
      AveragePair ap = new AveragePair();
      ap.addCount(count);
      sendAverage.put(actorId, ap);
    }
  }

  public static double getSendAverage(MJIEnv env, int clsObjRef, int actorId) {
    if (sendAverage.containsKey(actorId)) {
      return sendAverage.get(actorId).getAverage();
    } else {
      return 1.0;
    }
  }

  /*******************************************************
   * Support for ordering policy
   *******************************************************/
  static private List<Integer> order = new ArrayList<Integer>();

  public static void setOrder(MJIEnv env, int clsObjRef, List<Integer> ord) {
    order = ord;
  }

  public static List<Integer> getOrder(MJIEnv env, int clsObjRef) {
    return order;
  }

  public static void changeToHighandLowOrder(MJIEnv env, int clsObjRef,
                                             int high, int low) {

    for (Integer id : order) {
      if (id.intValue() == high || id.intValue() == low)
        order.remove(id);
    }
    order.add(0, high);
    order.add(low);
  }

  /*******************************************************
   * Support for "graph reachability" ordering heuristic
   *******************************************************/
  static private Map<Integer, Node> graph = new HashMap<Integer, Node>();

  static class Node {
    int actorId;
    Set<Integer> sentTo;

    Node(int id) {
      this.actorId = id;
      sentTo = new HashSet<Integer>();
    }

    void addEdgeTo(int receiver) {
      if (!sentTo.contains(receiver))
        sentTo.add(receiver);
    }

    boolean hasSentTo(int receiver) {
      if (sentTo.contains(receiver))
        return true;
      return false;
    }
  }

  public static void addEdge(MJIEnv env, int clsObjRef, int sender, int receiver) {
    if (!graph.containsKey(sender))
      graph.put(sender, new Node(sender));
    if (!graph.containsKey(receiver))
      graph.put(receiver, new Node(receiver));
    Node senderNode = graph.get(sender);
    senderNode.addEdgeTo(receiver);
  }

  public static boolean hasPathto(MJIEnv env, int clsObjRef, int sender,
                                  int receiver) {
    if (!graph.containsKey(sender))
      return false;
    if (!graph.containsKey(receiver))
      return false;
    Node senderNode = graph.get(sender);
    Set<Integer> reachableSet = new TreeSet<Integer>();
    TreeSet<Integer> workList = new TreeSet<Integer>();
    workList.add(senderNode.actorId);
    while (!workList.isEmpty()) {
      reachableSet.addAll(workList);
      if (reachableSet.contains(receiver))
        return true;
      int id = workList.first();
      workList.remove(id);
      Node n = graph.get(id);
      for (Integer receiverId : n.sentTo) {
        if (reachableSet.add(receiverId)) {
          workList.add(receiverId);
        }
      }
    }
    return false;
  }

  /*******************************************************
   * Support for thread switching
   *******************************************************/
  public static void startThreadInInterruptedStatus(MJIEnv env, int rcls,
                                                    int threadRef) {
    JPF_java_lang_Thread.start____V(env, threadRef);

    ThreadInfo newThread = env.getVM().getThreadList().locate(threadRef);
    newThread.setState(ThreadInfo.State.INTERRUPTED);

    // System.out.println("Start thread interrupted<"
    // + env.getStringObject(env.getReferenceField(threadRef, "name"))
    // + "> set to " + newThread.getStateName());
  }

  /***********************************************************/
  public static void yieldToPlatform____V(MJIEnv env, int rcls) {
    ThreadInfo mainThread = ThreadInfo.getMainThread();
    yieldTo(env, mainThread);
  }

  /***********************************************************/
  public static void yieldTo__Ljava_lang_Object_2__V(MJIEnv env, int rcls,
                                                     int threadRef) {
    int name = env.getReferenceField(threadRef, "name");
    String actorThreadName = env.getStringObject(name);

    yieldTo(env, actorThreadName);
  }

  /***********************************************************/
  private static void yieldTo(MJIEnv env, String threadName) {
    ThreadInfo next = getThreadInfoForName(env, threadName);

    if (next == null) {
      throw new RuntimeException("Thread<" + threadName + "> does not exist!!");
    }

    yieldTo(env, next);
  }

  /***********************************************************/
  private static void yieldTo(MJIEnv env, ThreadInfo next) {
    yield(env, env.getVM().getCurrentThread(), next);
  }

  /***********************************************************/
  private static void yield(MJIEnv env, ThreadInfo current, ThreadInfo next) {
    if (!current.isFirstStepInsn()) {
      next.setState(ThreadInfo.State.RUNNING);
      current.setState(ThreadInfo.State.INTERRUPTED);
      ChoiceGenerator<?> cg = new ThreadChoiceFromSet("basset-yield",
          new ThreadInfo[] { next }, true);

      if (cg != null) {
        SystemState ss = env.getVM().getSystemState();
        ss.setNextChoiceGenerator(cg);
      }
    }

    // System.out.println("Yielded execution: Thread<" + current.getName() + ","
    // + current.getStateName() + "> --> Thread<" + next.getName() + ","
    // + next.getStateName() + ">");
  }

  /***********************************************************/
  private static ThreadInfo getThreadInfoForName(MJIEnv env, String threadName) {

    ThreadList threads = env.getVM().getThreadList();
    for (ThreadInfo t : threads) {
      if (t.getName().equals(threadName)) {
        return t;
      }
    }

    // if a thread with the specified name is not found, return null
    return null;
  }

  /***********************************************************/
  public static boolean getTimeout(MJIEnv env, int clsref) {
    return BassetListener.timeout;
  }

  /***********************************************************/
  public static int getProperty__Ljava_lang_String_2__Ljava_lang_String_2(
                                                                          MJIEnv env,
                                                                          int clsObjRef,
                                                                          int keyRef) {
    if (keyRef != MJIEnv.NULL) {
      Config conf = env.getConfig();

      String key = env.getStringObject(keyRef);
      String val = conf.getString(key);

      if (val != null) {
        return env.newString(val);
      } else {
        return MJIEnv.NULL;
      }

    } else {
      return MJIEnv.NULL;
    }
  }

  /***********************************************************/
  public static int getIntegerProperty__Ljava_lang_String_2__I(MJIEnv env,
                                                               int clsObjRef,
                                                               int keyRef) {
    if (keyRef != MJIEnv.NULL) {
      Config conf = env.getConfig();

      String key = env.getStringObject(keyRef);
      int val = conf.getInt(key);

      return val;

    } else {
      return 0;
    }
  }

  /***********************************************************/
  public static boolean getBooleanProperty__Ljava_lang_String_2__Z(
                                                                   MJIEnv env,
                                                                   int clsObjRef,
                                                                   int keyRef) {
    Config conf = env.getConfig();

    String key = env.getStringObject(keyRef);
    boolean val = conf.getBoolean(key);

    return val;
  }

  /***********************************************************/
  // This is the same as Verify.getInt except that it always creates a
  // choice generator even when there is only one value (i.e., min == max)
  public static int getIntAndForceChoiceGenerator__II__I(MJIEnv env, int rcls,
                                                         int min, int max) {
    ThreadInfo ti = ThreadInfo.getCurrentThread(); // env.getThreadInfo();
    SystemState ss = env.getVM().getSystemState();
    ChoiceGenerator<?> cg;

    if (!ti.isFirstStepInsn()) { // first time around
      // if (min == max) return min;

      cg = new IntIntervalGenerator("basset-getIntAndForce", min, max);
      ss.setNextChoiceGenerator(cg);
      // ti.skipInstructionLogging();
      env.repeatInvocation();
      return 0; // not used anyways

    } else {
      cg = ss.getChoiceGenerator();

      assert (cg != null) && (cg instanceof IntChoiceGenerator) : "expected IntChoiceGenerator, got: "
          + cg;
      return ((IntChoiceGenerator) cg).getNextChoice().intValue();
    }
  }

  /***********************************************************/
  public static int getIntDPORDcute(MJIEnv env, int clsObjRef, int min,
                                    int max, int receiverArrayRef) {
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    SystemState ss = env.getVM().getSystemState();
    ChoiceGenerator<?> cg;
    int[] receivers = env.getIntArrayObject(receiverArrayRef);

    if (!ti.isFirstStepInsn()) { // first time around
      if (getBooleanProperty__Ljava_lang_String_2__Z(env, clsObjRef, env
          .newString("basset.bigstep"))) {
        if (min == max)
          return min;
      }
      cg = new DPORIntChoiceGeneratorDcute("basset-DPORDcute", min, max,
          receivers);
      ss.setNextChoiceGenerator(cg);
      // ti.skipInstructionLogging();
      env.repeatInvocation();
      return 0; // not used anyways

    } else {
      cg = ss.getChoiceGenerator();

      assert (cg != null) && (cg instanceof DPORIntChoiceGeneratorDcute) : "expected DPORIntChoiceGeneratorDcute, got: "
          + cg;
      return ((DPORIntChoiceGeneratorDcute) cg).getNextChoice().intValue();
    }
  }

  /***********************************************************/
  public static int getIntDPORPersistent(MJIEnv env, int clsObjRef, int min,
                                         int max, int msgIdArrayRef,
                                         int receiverArrayRef,
                                         boolean expandOnlyOnce) {
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    SystemState ss = env.getVM().getSystemState();
    ChoiceGenerator<?> cg;
    int[] msgIds = env.getIntArrayObject(msgIdArrayRef);
    int[] receivers = env.getIntArrayObject(receiverArrayRef);

    if (!ti.isFirstStepInsn()) { // first time around
      if (getBooleanProperty__Ljava_lang_String_2__Z(env, clsObjRef, env
          .newString("basset.bigstep"))) {
        if (min == max)
          return min;
      }
      if (expandOnlyOnce)
        cg = new DPORIntChoiceGeneratorTransistent("basset-DPORTransistent",
            min, max, msgIds, receivers, expandOnlyOnce);
      else
        cg = new DPORIntChoiceGeneratorPersistent("basset-DPORPersistent", min,
            max, receivers, expandOnlyOnce);
      ss.setNextChoiceGenerator(cg);
      // ti.skipInstructionLogging();
      env.repeatInvocation();
      return 0; // not used anyways

    } else {
      cg = ss.getChoiceGenerator();
      if (expandOnlyOnce) {
        assert (cg != null)
            && (cg instanceof DPORIntChoiceGeneratorTransistent) : "expected DPORIntChoiceGeneratorTransistent, got: "
            + cg;
        return ((DPORIntChoiceGeneratorTransistent) cg).getNextChoice()
            .intValue();
      } else {

        assert (cg != null) && (cg instanceof DPORIntChoiceGeneratorPersistent) : "expected DPORIntChoiceGeneratorPersistent, got: "
            + cg;
        return ((DPORIntChoiceGeneratorPersistent) cg).getNextChoice()
            .intValue();
      }
    }
  }

  /***********************************************************/
  public static int getIntDPORPersistentSleep(MJIEnv env, int clsObjRef,
                                              int min, int max,
                                              int msgIdArrayRef,
                                              int receiverArrayRef,
                                              boolean expandOnlyOnce) {
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    SystemState ss = env.getVM().getSystemState();
    ChoiceGenerator<?> cg = null;
    int[] msgIds = env.getIntArrayObject(msgIdArrayRef);
    int[] receivers = env.getIntArrayObject(receiverArrayRef);

    if (!ti.isFirstStepInsn()) { // first time around
      if (getBooleanProperty__Ljava_lang_String_2__Z(env, clsObjRef, env
          .newString("basset.bigstep"))) {
        if (min == max)
          return min;
      }
      if (expandOnlyOnce)
        cg = new DPORIntChoiceGeneratorTransistentSleep(
            "basset-DPORTransistentSleep", min, max, msgIds, receivers,
            expandOnlyOnce);
      else
        cg = new DPORIntChoiceGeneratorPersistentSleep(
            "basset-DPORPersistentSleep", min, max, msgIds, receivers,
            expandOnlyOnce);
      ss.setNextChoiceGenerator(cg);
      // ti.skipInstructionLogging();
      env.repeatInvocation();
      return 0; // not used anyways
    } else {
      cg = ss.getChoiceGenerator();

      if (expandOnlyOnce) {
        assert (cg != null)
            && (cg instanceof DPORIntChoiceGeneratorTransistentSleep) : "expected DPORIntChoiceGeneratorTransistentSleep, got: "
            + cg;
        return ((DPORIntChoiceGeneratorTransistentSleep) cg).getNextChoice()
            .intValue();
      } else {
        assert (cg != null)
            && (cg instanceof DPORIntChoiceGeneratorPersistentSleep) : "expected DPORIntChoiceGeneratorPersistentSleep, got: "
            + cg;
        return ((DPORIntChoiceGeneratorPersistentSleep) cg).getNextChoice()
            .intValue();
      }
    }
  }

  /***********************************************************/
  static ArrayList<ChoiceGenerator<?>> cglist = new ArrayList<ChoiceGenerator<?>>();

  public static int getCurrentCG(MJIEnv env, int clsObjRef) {
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    SystemState ss = env.getVM().getSystemState();
    ChoiceGenerator<?> cg = (ChoiceGenerator<?>) ss.getChoiceGenerator();

    if (!cglist.contains(cg))
      cglist.add(cg);
    return cglist.indexOf(cg);
  }

  /***********************************************************/
  public static void setNeedsDelayFlag(MJIEnv env, int clsObjRef, int index) {
    DPORIntChoiceGeneratorDcute cg = (DPORIntChoiceGeneratorDcute) cglist
        .get(index);
    cg.setNeedsDelayFlag(true);
  }

  /***********************************************************/
  public static void addToPersistentSet(MJIEnv env, int clsObjRef, int index,
                                        int messageId, int receiverId) {
    DPORIntChoiceGeneratorPersistent cg = (DPORIntChoiceGeneratorPersistent) cglist
        .get(index);
    cg.addToPersistentSet(messageId, receiverId);
  }

  /***********************************************************/
  public static void addToPersistentSet_Sleep(MJIEnv env, int clsObjRef,
                                              int index, int messageId,
                                              int receiverId) {
    DPORIntChoiceGeneratorPersistentSleep cg = (DPORIntChoiceGeneratorPersistentSleep) cglist
        .get(index);
    cg.addToPersistentSet(messageId, receiverId);
  }

  /***********************************************************/
  public static void addToTransistentSet_Sleep(MJIEnv env, int clsObjRef,
                                               int index, int messageId,
                                               int receiverId) {
    DPORIntChoiceGeneratorTransistentSleep cg = (DPORIntChoiceGeneratorTransistentSleep) cglist
        .get(index);
    cg.addToTransistentSet(messageId, receiverId);
  }

  /***********************************************************/
  public static void addToTransistentSet(MJIEnv env, int clsObjRef, int index,
                                         int messageId, int receiverId) {
    DPORIntChoiceGeneratorTransistent cg = (DPORIntChoiceGeneratorTransistent) cglist
        .get(index);
    cg.addToTransistentSet(messageId, receiverId);
  }

  /***********************************************************/
  public static void unsuspendPlatformThread____V(MJIEnv env, int rcls) {
    ThreadInfo platformThread = ThreadInfo.getMainThread();

    ThreadInfo current = ThreadInfo.getCurrentThread();

    if (current.getName().startsWith(CoreActorName.JPF_ACTOR_NAME)) {
      if (!current.isFirstStepInsn()) {
        platformThread.setState(ThreadInfo.State.RUNNING);
      }
    }
  }

  /***********************************************************/
  public static boolean isUsingActorStateSerializer(MJIEnv env, int rcls) {
    // TODO: need to restore this once the ActorSerializer is functioning
    // properly

    return false;
    // try {
    // gov.nasa.jpf.jvm.StateSerializer ss = JVM.getVM().getSerializer();
    // return (ss != null) && (ss instanceof ActorFilteringSerializer);
    // } catch (Config.Exception _) {
    // System.exit(1);
    // return false;
    // }
  }

}
