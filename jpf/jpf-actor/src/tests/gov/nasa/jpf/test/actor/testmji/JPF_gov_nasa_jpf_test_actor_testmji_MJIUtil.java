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
package gov.nasa.jpf.test.actor.testmji;

import java.util.List;

import gov.nasa.jpf.actor.util.MJICopier;
import gov.nasa.jpf.actor.util.MJILinearizer;
import gov.nasa.jpf.jvm.MJIEnv;

/**
 * 
 * @author Bobak Hadidi (bhadidi2@illinois.edu)
 * 
 */
@SuppressWarnings("unchecked")
public class JPF_gov_nasa_jpf_test_actor_testmji_MJIUtil {

  public static List<Integer> linearization;
  public static List<Integer>[] linArray = new List[10];

  /*******************************************************/
  public static int copy(MJIEnv env, int classRef, int objRef) {
    int copyRef = new MJICopier().copy(env, objRef);
    return copyRef;
  }

  /*******************************************************/
  public static void linearize(MJIEnv env, int classRef, int objref, int slot) {
    linearization = new MJILinearizer().linearize(env, objref);
    System.out.println("Linearization: " + linearization.toString());
    if (slot < 0 || slot > 9)
      throw new RuntimeException(
          "Invalid linearization slot.  Must be in range 0-9.");
    linArray[slot] = linearization;
  }

}
