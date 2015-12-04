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
package gov.nasa.jpf.actor.util;

import java.io.PrintStream;

import gov.nasa.jpf.actor.JPF_gov_nasa_jpf_actor_core_Util;
import gov.nasa.jpf.jvm.MJIEnv;

/**
 * Logging utility to be used by code at the native or JVM level.
 * 
 * @author Mirco Dotta (<mirco.dotta@gmail.com>)
 * 
 */
public class NativeLogger {

  private static enum LogLevel {
    DEBUG, VERBOSE, WARNING, SEVERE, INFO, ERROR
  }

  private static PrintStream out = System.out;
  private static PrintStream err = System.err;

  public static void debug(String msg, MJIEnv env) {
    if (isDebugMode(env))
      log(out, LogLevel.DEBUG, msg);
  }

  public static void debug(Object obj, String msg, MJIEnv env) {
    if (isDebugMode(env))
      log(out, LogLevel.DEBUG, obj, msg);
  }

  public static void warning(String msg, MJIEnv env) {
    log(out, LogLevel.WARNING, msg);
  }

  public static void warning(Object obj, String msg, MJIEnv env) {
    log(out, LogLevel.WARNING, obj, msg);
  }

  public static void severe(String msg, MJIEnv env) {
    log(out, LogLevel.SEVERE, msg);
  }

  public static void severe(Object obj, String msg, MJIEnv env) {
    log(out, LogLevel.SEVERE, obj, msg);
  }

  public static void info(String msg, MJIEnv env) {
    if (isInfoMode(env))
      log(out, LogLevel.INFO, msg);
  }

  public static void verbose(String msg, MJIEnv env) {
    if (isVerboseMode(env))
      log(out, LogLevel.VERBOSE, msg);
  }

  public static void verbose(Object obj, String msg, MJIEnv env) {
    if (isVerboseMode(env))
      log(out, LogLevel.VERBOSE, obj, msg);
  }

  public static void error(Object obj, String msg, MJIEnv env) {
    if (isErrorMode(env))
      log(err, LogLevel.ERROR, obj, msg);
  }

  public static void error(String msg, MJIEnv env) {
    if (isErrorMode(env))
      log(err, LogLevel.ERROR, msg);
  }

  public static void log(PrintStream stream, LogLevel level, String msg) {
    stream.println(level + ": " + msg);
  }

  public static void log(PrintStream stream, LogLevel level, Object obj,
                         String msg) {
    log(stream, level, obj + " - " + msg);
  }

  public static void log(PrintStream stream, LogLevel level, Object obj) {
    stream.println(level + "(object): " + obj);
  }

  public static boolean isVerboseMode(MJIEnv env) {
    return getBooleanProperty("basset.debug", env)
        || getBooleanProperty("basset.verbose", env);
  }

  public static boolean isDebugMode(MJIEnv env) {
    return getBooleanProperty("basset.debug", env);
  }

  public static boolean isInfoMode(MJIEnv env) {
    return getBooleanProperty("basset.info", env);
  }

  public static boolean isErrorMode(MJIEnv env) {
    return getBooleanProperty("basset.debug", env)
        || getBooleanProperty("basset.error", env);
  }

  private static boolean getBooleanProperty(String property, MJIEnv env) {
    return JPF_gov_nasa_jpf_actor_core_Util
        .getBooleanProperty__Ljava_lang_String_2__Z(env, 0, env
            .newString(property));
  }

}
