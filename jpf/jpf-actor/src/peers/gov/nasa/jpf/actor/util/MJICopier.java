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

import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.MJIEnv;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * 
 * @author Bobak Hadidi (bhadidi2@illinois.edu)
 * @author Steven Lauterburg (steven.lauterburg@gmail.com)
 * 
 */
public class MJICopier {

  // for cloning optimization
  // these reference 'channels' are permitted because the objects are immutable
  public static HashSet<String> immutableClasses = null;
  static {
    immutableClasses = new HashSet<String>(20);
    immutableClasses.add("ClassInfo[name=java.lang.String]");
    immutableClasses.add("ClassInfo[name=java.lang.Integer]");
    immutableClasses.add("ClassInfo[name=java.lang.Double]");
    immutableClasses.add("ClassInfo[name=java.lang.Float]");
    immutableClasses.add("ClassInfo[name=java.lang.Long]");
    immutableClasses.add("ClassInfo[name=java.lang.Boolean]");
    immutableClasses.add("ClassInfo[name=java.lang.Byte]");
    immutableClasses.add("ClassInfo[name=java.lang.Short]");
    immutableClasses.add("ClassInfo[name=java.lang.Character]");
    immutableClasses.add("ClassInfo[name=osl.manager.ActorName]");
  }

  public static boolean isMutableClass(ClassInfo ci) {
    return immutableClasses.contains(ci.toString());
  }

  // this maps jpfIDs to jpfIDs
  // the id of the originals ref -> the id of the newly created copy's ref
  private Map<Integer, Integer> objectMap = new HashMap<Integer, Integer>();

  /***********************************************************/
  public int copy(MJIEnv env, int sourceObjRef) {
    // System.out.println("beginning copy");
    int newObjref = copyJPFObject(objectMap, env, sourceObjRef);
    return newObjref;
  }

  /***********************************************************/
  private int copyJPFObject(Map<Integer, Integer> objectMap, MJIEnv env,
                            int sourceObjRef) {

    // System.out.println("copying new obj");
    int newObjRef = MJIEnv.NULL;

    // if the pointer is null return immediately
    if (sourceObjRef == MJIEnv.NULL)
      return newObjRef;

    // if the object has been seen before return its ref
    if (objectMap.containsKey(sourceObjRef)) {
      // System.out.println("CYCLE");
      // System.out.println(sourceObjRef + " of type: " +
      // env.getClassInfo(sourceObjRef).getName());
      newObjRef = objectMap.get(sourceObjRef);
      return newObjRef;
    } else {
      // get the class info of the source object to be copied
      ClassInfo ci = env.getClassInfo(sourceObjRef);
      // if the obj is immutable, just return the src reference
      if (isMutableClass(ci))
        return sourceObjRef;

      // the src ref is either an array or obj
      if (ci.isArray()) {
        // System.out.println("beginning array copy");
        newObjRef = copyJPFArray(objectMap, env, sourceObjRef);
        // System.out.println("adding to map: " + sourceObjRef + " --> " +
        // newObjRef + " of type: " + env.getClassInfo(sourceObjRef));
        objectMap.put(sourceObjRef, newObjRef);
      } else {
        // create new object
        newObjRef = env.newObject(ci);
        // System.out.println("adding to map: " + sourceObjRef + " --> " +
        // newObjRef + " of type: " + env.getClassInfo(sourceObjRef));
        objectMap.put(sourceObjRef, newObjRef);

        copyJPFObjectFields(objectMap, env, sourceObjRef, newObjRef);
      }
    }

    return newObjRef;
  }

  /***********************************************************/
  private void copyJPFObjectFields(Map<Integer, Integer> objectMap, MJIEnv env,
                                   int sourceObjRef, int newObjRef) {

    ClassInfo ci = env.getClassInfo(sourceObjRef);

    // process all instance fields including those inherited
    // from super classes. (TODO: What about static fields??)
    // TODO: transient fields?
    while (ci != null) {
      FieldInfo[] fieldsInfo = ci.getDeclaredInstanceFields();

      // iterate, assign each field to a copy
      for (FieldInfo fi : fieldsInfo) {
        // System.out.println(fi + " " + fi.getName());
        copyJPFObjectField(objectMap, env, sourceObjRef, newObjRef, fi);
      }

      ci = ci.getSuperClass();
    }
  }

  /***********************************************************/
  private void copyJPFObjectField(Map<Integer, Integer> objectMap, MJIEnv env,
                                  int sourceObjRef, int newObjRef, FieldInfo fi) {

    ClassInfo ci = fi.getTypeClassInfo();
    // System.out.println(ci);

    // the field may be a primitive, array, or object
    if (ci.isPrimitive()) {
      // System.out.println("copying primitive field");
      copyJPFPrimitiveField(env, sourceObjRef, newObjRef, fi);
    } else { // it's a reference or an array
      // System.out.println("found reference field");
      int fieldRef = env.getReferenceField(sourceObjRef, fi.getName());
      int copyRef = copyJPFObject(objectMap, env, fieldRef); // recurse
      env.setReferenceField(newObjRef, fi.getName(), copyRef);
    }
  }

  /***********************************************************/
  private void copyJPFPrimitiveField(MJIEnv env, int sourceObjRef,
                                     int newObjRef, FieldInfo fi) {

    String ftype = fi.getType();

    if (ftype.equals("double")) {
      double d = env.getDoubleField(sourceObjRef, fi.getName());
      env.setDoubleField(newObjRef, fi.getName(), d);

    } else if (ftype.equals("float")) {
      float f = env.getFloatField(sourceObjRef, fi.getName());
      env.setFloatField(newObjRef, fi.getName(), f);

    } else if (ftype.equals("long")) {
      long l = env.getLongField(sourceObjRef, fi.getName());
      env.setLongField(newObjRef, fi.getName(), l);

    } else if (ftype.equals("int")) {
      int i = env.getIntField(sourceObjRef, fi.getName());
      env.setIntField(newObjRef, fi.getName(), i);

    } else if (ftype.equals("short")) {
      short i = env.getShortField(sourceObjRef, fi.getName());
      env.setShortField(newObjRef, fi.getName(), i);

    } else if (ftype.equals("byte")) {
      byte i = env.getByteField(sourceObjRef, fi.getName());
      env.setByteField(newObjRef, fi.getName(), i);

    } else if (ftype.equals("char")) {
      char i = env.getCharField(sourceObjRef, fi.getName());
      env.setCharField(newObjRef, fi.getName(), i);

    } else if (ftype.equals("boolean")) {
      boolean i = env.getBooleanField(sourceObjRef, fi.getName());
      env.setBooleanField(newObjRef, fi.getName(), i);

    } else {
      throw new UnsupportedOperationException("Copy for fields of type "
          + ftype + " has not been implemented yet");
    }
  }

  /***********************************************************/
  private int copyJPFArray(Map<Integer, Integer> objectMap, MJIEnv env,
                           int srcArrRef) {

    ClassInfo ci = env.getClassInfo(srcArrRef);
    assert ci.isArray();
    // System.out.println("copying array obj");

    // get the array size
    int arraySize = env.getArrayLength(srcArrRef);

    // make the new array
    int copyArrRef = env.NULL;

    if (ci.isReferenceArray() || ci.getName().startsWith("[[")) {
      // System.out.println("found ref array");
      // this is an array of object references or an array of arrays
      // get the array type in String
      String className = ci.getName();
      copyArrRef = env.newObjectArray(className, arraySize);
      // copy each array element
      for (int index = 0; index < arraySize; index++) {
        // copy each array element
        // System.out.println("doing copy for ref array elem..");
        int elementRef = env.getReferenceArrayElement(srcArrRef, index);
        int elemCopyRef = copyJPFObject(objectMap, env, elementRef);
        // assign copy to new array index
        env.setReferenceArrayElement(copyArrRef, index, elemCopyRef);
      }
    } else {
      // System.out.println("found primitive array");
      // this is an array of primitives
      // find the array type and set the copyArrRef
      String elemType = env.getArrayType(srcArrRef);
      switch (elemType.charAt(0)) {
      case 'D':
        copyArrRef = env.newDoubleArray(arraySize);
        break;
      case 'F':
        copyArrRef = env.newFloatArray(arraySize);
        break;
      case 'J':
        copyArrRef = env.newLongArray(arraySize);
        break;
      case 'I':
        copyArrRef = env.newIntArray(arraySize);
        break;
      case 'S':
        copyArrRef = env.newShortArray(arraySize);
        break;
      case 'B':
        copyArrRef = env.newByteArray(arraySize);
        break;
      case 'C':
        copyArrRef = env.newCharArray(arraySize);
        break;
      case 'Z':
        copyArrRef = env.newBooleanArray(arraySize);
        break;
      default:
        throw new UnsupportedOperationException("Copy for fields of type "
            + elemType + " has not been implemented yet");
      }
      for (int index = 0; index < arraySize; index++) {
        // System.out.println("size: " + arraySize + " on iter: " + index);
        copyJPFPrimitiveArrayElement(env, srcArrRef, copyArrRef, index);
      }
    }
    // return the ref to the newly created array
    return copyArrRef;
  }

  /***********************************************************/
  private void copyJPFPrimitiveArrayElement(MJIEnv env, int srcArrRef,
                                            int copyArrRef, int index) {

    String ftype = env.getArrayType(srcArrRef);

    // find the array type and set the copy reference to elem
    if (ftype.equals("D")) {
      double d = env.getDoubleArrayElement(srcArrRef, index);
      env.setDoubleArrayElement(copyArrRef, index, d);

    } else if (ftype.equals("F")) {
      float f = env.getFloatArrayElement(srcArrRef, index);
      env.setFloatArrayElement(copyArrRef, index, f);

    } else if (ftype.equals("J")) {
      long l = env.getLongArrayElement(srcArrRef, index);
      env.setLongArrayElement(copyArrRef, index, l);

    } else if (ftype.equals("I")) {
      int i = env.getIntArrayElement(srcArrRef, index);
      env.setIntArrayElement(copyArrRef, index, i);

    } else if (ftype.equals("S")) {
      short s = env.getShortArrayElement(srcArrRef, index);
      env.setShortArrayElement(copyArrRef, index, s);

    } else if (ftype.equals("B")) {
      byte b = env.getByteArrayElement(srcArrRef, index);
      env.setByteArrayElement(copyArrRef, index, b);

    } else if (ftype.equals("C")) {
      char c = env.getCharArrayElement(srcArrRef, index);
      env.setCharArrayElement(copyArrRef, index, c);

    } else if (ftype.equals("Z")) {
      boolean z = env.getBooleanArrayElement(srcArrRef, index);
      env.setBooleanArrayElement(copyArrRef, index, z);

    } else {
      throw new UnsupportedOperationException("Copy for fields of type "
          + ftype + " has not been implemented yet");
    }
  }

}
