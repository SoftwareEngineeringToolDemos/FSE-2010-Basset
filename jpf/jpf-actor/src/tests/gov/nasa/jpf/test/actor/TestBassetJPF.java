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
package gov.nasa.jpf.test.actor;

import org.junit.Assert;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.actor.Basset;
import gov.nasa.jpf.actor.Statistics;
import gov.nasa.jpf.util.test.TestJPF;

/**
 * 
 * @author Steven Lauterburg (steven.lauterburg@gmail.com)
 * 
 */
public abstract class TestBassetJPF extends TestJPF {

  /***********************************************************/
  protected void runBasset(String[] jpfArgs) {
    Config conf = JPF.createConfig(jpfArgs);
    JPF jpf = new JPF(conf);
    jpf.run();
  }

  /***********************************************************/
  protected void runBasset(String language, String[] options, String subject,
                           String[] subjectArgs) {
    try {
      runBasset(language, options, Class.forName(subject), subjectArgs);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  /***********************************************************/
  protected void runBasset(String language, String[] options, Class<?> subject,
                           String[] subjectArgs) {

    Statistics.resetExplorationStatistics();

    int optionsLen = options == null ? 0 : options.length;
    int argsLen = subjectArgs == null ? 0 : subjectArgs.length;

    String[] jpfArgs = new String[optionsLen + argsLen + 3];
    jpfArgs[0] = "+basset.language=" + language;
    if (optionsLen > 0)
      System.arraycopy(options, 0, jpfArgs, 1, optionsLen);
    jpfArgs[optionsLen + 1] = Basset.class.getName();
    jpfArgs[optionsLen + 2] = subject.getName();
    if (argsLen > 0)
      System.arraycopy(subjectArgs, 0, jpfArgs, (optionsLen + 3), argsLen);

    runBasset(jpfArgs);
  }

  /***********************************************************/
  protected void checkResults(int traceCount, int messageDeliveryFailCount,
                              int abortedTraceCount, int messageDeliveryCount,
                              int actorCreateCount, int actorDestroyedCount,
                              int actorTerminatedCount, int deadlockCount) {

    Assert.assertEquals(traceCount, Statistics.statJPFTraceCount);
    Assert.assertEquals(messageDeliveryFailCount,
        Statistics.statMessageDeliveryFailCount);
    Assert.assertEquals(abortedTraceCount, Statistics.statAbortedTraceCount);
    Assert.assertEquals(messageDeliveryCount,
        Statistics.statMessageDeliveryCount);
    Assert.assertEquals(actorCreateCount, Statistics.statActorCreateCount);
    Assert
        .assertEquals(actorDestroyedCount, Statistics.statActorDestroyedCount);
    Assert.assertEquals(actorTerminatedCount,
        Statistics.statActorTerminatedCount);
    Assert.assertEquals(deadlockCount, Statistics.statDeadlocks);
  }

}
