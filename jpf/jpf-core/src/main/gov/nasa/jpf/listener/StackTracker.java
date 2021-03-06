//
// Copyright (C) 2007 United States Government as represented by the
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
package gov.nasa.jpf.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.INVOKESPECIAL;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.VirtualInvocation;
import gov.nasa.jpf.search.Search;

import java.io.PrintWriter;

/**
 * simple tool to log stack invocations
 *
 * at this point, it doesn't do fancy things yet, but gives a more high
 * level idea of what got executed by JPF than the ExecTracker
 */
public class StackTracker extends ListenerAdapter {

  static final String INDENT = "  ";

  MethodInfo lastMi;
  PrintWriter out;
  long nextLog;
  int logPeriod;

  public StackTracker (Config conf, JPF jpf) {
    out = new PrintWriter(System.out, true);
    logPeriod = conf.getInt("jpf.stack_tracker.log_period", 5000);
  }

  void logStack(ThreadInfo ti) {
    long time = System.currentTimeMillis();

    if (time < nextLog) {
      return;
    }

    nextLog = time + logPeriod;

    out.println();
    out.print("Thread: ");
    out.print(ti.getId());
    out.println(":");

    out.println(ti.getStackTrace());
    out.println();
  }

  public void executeInstruction (JVM vm) {
    Instruction insn = vm.getLastInstruction();
    MethodInfo mi = insn.getMethodInfo();
    ThreadInfo ti = vm.getLastThreadInfo();

    if (mi != lastMi) {
      logStack(ti);
      lastMi = mi;

    } else if (insn instanceof InvokeInstruction) {
      MethodInfo callee;

      // that's the only little gist of it - if this is a VirtualInvocation,
      // we have to dig the callee out by ourselves (it's not known
      // before execution)

      if (insn instanceof VirtualInvocation) {
        VirtualInvocation callInsn = (VirtualInvocation)insn;
        int objref = callInsn.getCalleeThis(ti);
        callee = callInsn.getInvokedMethod(ti, objref);

      } else if (insn instanceof INVOKESPECIAL) {
        INVOKESPECIAL callInsn = (INVOKESPECIAL)insn;
        callee = callInsn.getInvokedMethod(ti);

      } else {
        InvokeInstruction callInsn = (InvokeInstruction)insn;
        callee = callInsn.getInvokedMethod(ti);
      }

      if (callee != null) {
        if (callee.isMJI()) {
          logStack(ti);
        }
      } else {
        out.println("ERROR: unknown callee of: " + insn);
      }
    }
  }

  public void stateAdvanced(Search search) {
    lastMi = null;
  }

  public void stateBacktracked(Search search) {
    lastMi = null;
  }
}
