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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.actor.core.Util;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.Types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Bobak Hadidi (bhadidi2@illinois.edu)
 * 
 */
public class MJILinearizer {

  /** <b>null</b> references are mapped to the integer <code>NULL_REF_LIN</code> */
  public static final int NULL_REF_LIN = -999;

  private String[] fieldsFilter;

  public MJILinearizer() {
    // Linearization is applied to all fields in the object graph
  }

  public MJILinearizer(Config conf) {
    fieldsFilter = conf.getStringArray("state.linearizer.exclude_fields",
        new String[] {});
  }

  private int virtualObjectId = 5000;
  private Map<Integer, Integer> objectMap = new HashMap<Integer, Integer>();

  private int virtualClassId = 6000;
  private Map<Integer, Integer> classMap = new HashMap<Integer, Integer>();

  public Map<Integer, Integer> getObjectMap() {
    return objectMap;
  }

  /***********************************************************/
  public List<Integer> linearize(MJIEnv env, int objref) {
    List<Integer> linearization = linearizeJPFObject(objectMap, env, objref);
    return linearization;
  }

  /***********************************************************/
  private List<Integer> linearizeJPFObject(Map<Integer, Integer> objectMap,
                                           MJIEnv env, int objref) {

    // objref is a reference to either an object or an array
    // if the pointer is null return immediately
    if (objref == MJIEnv.NULL)
      return Util.asList(NULL_REF_LIN);

    List<Integer> result = new ArrayList<Integer>();

    // Add the object's mapped instance id
    if (objectMap.containsKey(objref)) {
      int objId = objectMap.get(objref);
      result.add(-objId); // if previously encountered then return its
      // negative value
      return result;
    } else {
      objectMap.put(objref, ++virtualObjectId);
      result.add(virtualObjectId);
    }

    // Add the object's mapped class id. Class ids are included
    // in the linearization to distinguish between classes that
    // have the same set of fields and to support reconstruction
    // of object graphs from linearizations.
    ClassInfo ci = env.getClassInfo(objref);
    // System.out.println("ci: " + ci);
    int classref = ci.getUniqueId();

    // //////// current code
    // result.add(classref);

    // //////// proposed code
    if (classMap.containsKey(classref)) {
      result.add(classMap.get(classref));
    } else {
      classMap.put(classref, ++virtualClassId);
      result.add(virtualClassId);
    }

    if (ci.isArray()) {
      result.addAll(linearizeJPFArray(objectMap, env, objref));
    } else {
      result.addAll(linearizeJPFObjectFields(objectMap, env, objref));
    }

    return result;
  }

  /***********************************************************/
  private List<Integer> linearizeJPFObjectFields(
                                                 Map<Integer, Integer> objectMap,
                                                 MJIEnv env, int instancePointer) {

    ClassInfo ci = env.getClassInfo(instancePointer);

    List<Integer> result = new ArrayList<Integer>();

    // process all instance fields including those inherited
    // from super classes. (TODO: What about static fields??)
    // TODO: transient fields?
    while (ci != null) {
      FieldInfo[] fieldsInfo = ci.getDeclaredInstanceFields();

      for (FieldInfo fi : fieldsInfo) {
        // if(!isFieldIgnored(fi))
        result.addAll(linearizeJPFObjectField(objectMap, env, instancePointer,
            fi));
      }

      ci = ci.getSuperClass();
    }

    return result;
  }

  /***********************************************************/
  private List<Integer> linearizeJPFObjectField(
                                                Map<Integer, Integer> objectMap,
                                                MJIEnv env,
                                                int instancePointer,
                                                FieldInfo fi) {

    ClassInfo ci = fi.getTypeClassInfo();
    if (ci.isPrimitive()) {
      return linearizeJPFPrimitiveField(env, instancePointer, fi);
    } else { // it's a reference or an array
      int fieldPointer = env.getReferenceField(instancePointer, fi);
      return linearizeJPFObject(objectMap, env, fieldPointer);
    }
  }

  /***********************************************************/
  private List<Integer> linearizeJPFArray(Map<Integer, Integer> objectMap,
                                          MJIEnv env, int instancePointer) {

    ClassInfo instanceInfo = env.getClassInfo(instancePointer);
    assert instanceInfo.isArray();

    List<Integer> result = new ArrayList<Integer>(1);

    // add the size of the array to the linearization
    int arraySize = env.getArrayLength(instancePointer);
    result.add(arraySize);

    // process each array element
    for (int index = 0; index < arraySize; index++) {
      if (instanceInfo.isReferenceArray()) {
        // this is an array of object references
        int elementRef = env.getReferenceArrayElement(instancePointer, index);
        result.addAll(linearizeJPFObject(objectMap, env, elementRef));

      } else if (instanceInfo.getName().startsWith("[[")) {
        // this is an array of arrays
        int elementRef = env.getReferenceArrayElement(instancePointer, index);
        result.addAll(linearizeJPFObject(objectMap, env, elementRef));

      } else {
        // this is an array of primitives
        result.addAll(linearizeJPFPrimitiveArrayElement(env, instancePointer,
            index));
      }
    }
    return result;
  }

  /***********************************************************/
  private List<Integer> linearizeJPFPrimitiveArrayElement(MJIEnv env,
                                                          int instancePointer,
                                                          int index) {

    String ftype = env.getArrayType(instancePointer);

    if (ftype.equals("D")) {
      double d = env.getDoubleArrayElement(instancePointer, index);
      int hiDouble = Types.hiDouble(d);
      int loDouble = Types.loDouble(d);
      return Util.asList(hiDouble, loDouble);

    } else if (ftype.equals("F")) {
      float f = env.getFloatArrayElement(instancePointer, index);
      return Util.asList(Types.floatToInt(f));

    } else if (ftype.equals("J")) {
      long l = env.getLongArrayElement(instancePointer, index);
      int hiLong = Types.hiLong(l);
      int loLong = Types.loLong(l);
      return Util.asList(hiLong, loLong);

    } else if (ftype.equals("I") || ftype.equals("S") || ftype.equals("B")
        || ftype.equals("C") || ftype.equals("Z")) {
      return Util.asList(env.getIntArrayElement(instancePointer, index));

    } else {
      throw new UnsupportedOperationException(
          "Linearization for members of type " + ftype
              + " has not been implemented yet");
    }
  }

  /***********************************************************/
  private List<Integer> linearizeJPFPrimitiveField(MJIEnv env,
                                                   int instancePointer,
                                                   FieldInfo fi) {

    String ftype = fi.getType();

    if (ftype.equals("double")) {
      double d = env.getDoubleField(instancePointer, fi.getName());
      int hiDouble = Types.hiDouble(d);
      int loDouble = Types.loDouble(d);
      return Util.asList(hiDouble, loDouble);

    } else if (ftype.equals("float")) {
      float f = env.getFloatField(instancePointer, fi.getName());
      return Util.asList(Types.floatToInt(f));

    } else if (ftype.equals("long")) {
      long l = env.getLongField(instancePointer, fi.getName());
      int hiLong = Types.hiLong(l);
      int loLong = Types.loLong(l);
      return Util.asList(hiLong, loLong);

    } else if (ftype.equals("int") || ftype.equals("short")
        || ftype.equals("byte") || ftype.equals("char")
        || ftype.equals("boolean")) {
      return Util.asList(env.getIntField(instancePointer, fi.getName()));

    } else {
      throw new UnsupportedOperationException(
          "Linearization for members of type " + ftype
              + " has not been implemented yet");
    }
  }

}
