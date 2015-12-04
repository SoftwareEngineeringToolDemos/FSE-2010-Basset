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

import java.util.LinkedList;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.jvm.MJIEnv;
import static gov.nasa.jpf.test.actor.testmji.JPF_gov_nasa_jpf_test_actor_testmji_MJIUtil.linArray;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * This class contains a series of JUnit tests for the MJICopier class.
 * 
 * @author Bobak Hadidi (bhadidi2@illinois.edu)
 * 
 */
public class MJICopierTest {

  /*******************************************************/
  protected void run(String driverClass) {
    LinkedList<String> jpfArgs = new LinkedList<String>();
    jpfArgs.add(driverClass);
    // let example objects know to do the copy test rather than linearize
    jpfArgs.add("test copy");
    String[] args = jpfArgs.toArray(new String[0]);
    JPF jpf = new JPF(args);
    jpf.run();
  }

  /************************ Tests ************************/

  // 1. Object with int and null reference
  @Test
  public void TestCopySimple() {
    run(gov.nasa.jpf.test.actor.testmji.ObjSimple.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 2. Object with only primitive fields
  @Test
  public void TestCopyPrimitives() {
    run(gov.nasa.jpf.test.actor.testmji.ObjPrimitives.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 3. Object with other object references
  @Test
  public void TestCopyObjs() {
    run(gov.nasa.jpf.test.actor.testmji.ObjObjs.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 4. Object with circular references (cyclic graph)
  @Test
  public void TestCopyCycle() {
    run(gov.nasa.jpf.test.actor.testmji.ObjCycle.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 5. Object extending another class with new fields
  @Test
  public void TestCopySubClass() {
    run(gov.nasa.jpf.test.actor.testmji.ObjSubclass.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 6. Object with simple enum fields
  @Test
  public void TestCopyEnumSimple() {
    run(gov.nasa.jpf.test.actor.testmji.ObjEnumSimple.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 7. Object with enum fields
  @Test
  public void TestCopyEnum() {
    run(gov.nasa.jpf.test.actor.testmji.ObjEnum.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 8. Object with primitive arrays
  @Test
  public void TestCopyPrimArray() {
    run(gov.nasa.jpf.test.actor.testmji.ObjPrimArray.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 8. Object with multidimensional primitive arrays
  @Test
  public void TestCopyMultiPrimArray() {
    run(gov.nasa.jpf.test.actor.testmji.ObjMultiPrimArray.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 10. Object with object arrays
  @Test
  public void TestCopyObjArray() {
    run(gov.nasa.jpf.test.actor.testmji.ObjObjArray.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 11. Object with multidimensional object arrays
  @Test
  public void TestCopyMultiObjArray() {
    run(gov.nasa.jpf.test.actor.testmji.ObjMultiObjArray.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 12. Object with reference array of size 0
  @Test
  public void TestCopyRefArrSize0() {
    run(gov.nasa.jpf.test.actor.testmji.ObjRefArrSize0.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 13. Object implementing an interface with (static) fields
  @Test
  public void TestCopyStatic() {
    run(gov.nasa.jpf.test.actor.testmji.ObjStatic.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 14. Object with some common data structure references
  @Test
  public void TestCopyDataStructs() {
    run(gov.nasa.jpf.test.actor.testmji.ObjDataStructs.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 15. Object with some other common data structure references
  @Test
  public void TestCopyDataStructs2() {
    run(gov.nasa.jpf.test.actor.testmji.ObjDataStructs2.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 16. Object with some other common data structure references
  @Test
  public void TestCopyDataStructs3() {
    run(gov.nasa.jpf.test.actor.testmji.ObjDataStructs3.class.getName());
    assertEquals(linArray[0].toString(), linArray[1].toString());
  }

  // 17. Object with immutable ref - should be shared
  @Test
  public void TestImmutableRef() {
    run(gov.nasa.jpf.test.actor.testmji.ObjImmutes.class.getName());
    // see ObjImmutes.main

    // passes if the copy and original both reference the same immutable string
    assertEquals(linArray[0].indexOf(0), MJIEnv.NULL);
    // passes if the copy AN did not change along with the orig AN
    assertEquals(linArray[1].indexOf(0), MJIEnv.NULL);
    // passes if the objSimple references, which are not immutable, differ
    assertEquals(linArray[2].indexOf(0), MJIEnv.NULL);
  }

}
