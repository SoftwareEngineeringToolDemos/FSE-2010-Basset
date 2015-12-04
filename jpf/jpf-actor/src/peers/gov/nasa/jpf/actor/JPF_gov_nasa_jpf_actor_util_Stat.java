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

import gov.nasa.jpf.jvm.MJIEnv;

/**
 * 
 * @author Steven Lauterburg (steven.lauterburg@gmail.com)
 * 
 */
public class JPF_gov_nasa_jpf_actor_util_Stat {

  /***********************************************************/
  public static void incStatActorCreateCount(MJIEnv env, int clsref) {
    Statistics.statActorCreateCount++;
  }

  public static void incStatActorDestroyedCount(MJIEnv env, int clsref) {
    Statistics.statActorDestroyedCount++;
  }

  public static void incStatActorTerminatedCount(MJIEnv env, int clsref) {
    Statistics.statActorTerminatedCount++;
  }

  public static void incStatJPFTraceCount(MJIEnv env, int clsref) {
    Statistics.statJPFTraceCount++;
  }

  public static void incStatMessageDeliveryCount(MJIEnv env, int clsref) {
    Statistics.statMessageDeliveryCount++;
  }

  public static void incStatMessageSendCount(MJIEnv env, int clsref) {
    Statistics.statMessageSendCount++;
  }

  public static void incStatMessageDeliveryFailCount(MJIEnv env, int clsref) {
    Statistics.statMessageDeliveryFailCount++;
  }

  public static void incStatDeadlocks(MJIEnv env, int clsref) {
    Statistics.statDeadlocks++;
  }

  public static void incStatAbortedTraceCount(MJIEnv env, int clsref) {
    Statistics.statAbortedTraceCount++;
  }

  /***********************************************************/
  public static int getStatJPFTraceCount(MJIEnv env, int clsref) {
    return Statistics.statJPFTraceCount;
  }

}
