//
// Copyright (C) 2006 United States Government as represented by the
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

package gov.nasa.jpf.jvm;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;

// NOTE - this only works because DecimalFormat is a Format subclass, i.e.
// the java.text.Format native peer will be initialized first
// (otherwise we shouldn't depend on static data of other native peers)

public class JPF_java_text_DecimalFormat {

  static final int INTEGER_STYLE=0;
  static final int NUMBER_STYLE=1;
       
  static NumberFormat getInstance (MJIEnv env, int objref) {
    Format fmt = JPF_java_text_Format.getInstance(env,objref);
    assert fmt instanceof NumberFormat;
    
    return (NumberFormat)fmt;
  }
  
  /*
   * NOTE: if we would directly intercept the ctors, we would have to
   * explicitly call the superclass ctors here (the 'id' handle gets
   * initialized in the java.text.Format ctor) 
   */
  
  public static void init0____V (MJIEnv env, int objref) {
    DecimalFormat fmt = new DecimalFormat();
    JPF_java_text_Format.putInstance(env,objref,fmt);    
  }
  
  public static void init0__Ljava_lang_String_2__V (MJIEnv env, int objref, int patternref) {
    String pattern = env.getStringObject(patternref);
    
    DecimalFormat fmt = new DecimalFormat(pattern);
    JPF_java_text_Format.putInstance(env,objref,fmt);    
  }
  
  public static void init0__I__V (MJIEnv env, int objref, int style) {
    NumberFormat fmt = null;
    if (style == INTEGER_STYLE) {
      fmt = NumberFormat.getIntegerInstance();
    } else if (style == NUMBER_STYLE) {
      fmt = NumberFormat.getNumberInstance();
    } else {
      // unknown style
      fmt = new DecimalFormat();
    }
    
    JPF_java_text_Format.putInstance(env,objref,fmt);    
  }
  
  public static void setMaximumFractionDigits__I__V (MJIEnv env, int objref, int newValue){
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      fmt.setMaximumFractionDigits(newValue);
    }
  }
  public static void setMaximumIntegerDigits__I__V (MJIEnv env, int objref, int newValue){
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      fmt.setMaximumIntegerDigits(newValue);
    }
  }
  public static void setMinimumFractionDigits__I__V (MJIEnv env, int objref, int newValue){
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      fmt.setMinimumFractionDigits(newValue);
    }
  }
  public static void setMinimumIntegerDigits__I__V (MJIEnv env, int objref, int newValue){
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      fmt.setMinimumIntegerDigits(newValue);
    }
  }
  
  public static int format__J__Ljava_lang_String_2 (MJIEnv env, int objref, long number) {
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      String s = fmt.format(number);
      int sref = env.newString(s);
      return sref;
    }
    
    return MJIEnv.NULL;
  }
  
  public static int format__D__Ljava_lang_String_2 (MJIEnv env, int objref, double number) {
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      String s = fmt.format(number);
      int sref = env.newString(s);
      return sref;
    }
    
    return MJIEnv.NULL;
  }

  public static void setParseIntegerOnly__Z__V(MJIEnv env, int objref, boolean value) {
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      fmt.setParseIntegerOnly(value);
    }
  }

  public static boolean isParseIntegerOnly____Z(MJIEnv env, int objref) {
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      return fmt.isParseIntegerOnly();
    }
    return false;
  }

  public static void setGroupingUsed__Z__V(MJIEnv env, int objref, boolean newValue) {
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      fmt.setGroupingUsed(newValue);
    }
  }

  public static boolean isGroupingUsed____Z(MJIEnv env, int objref) {
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      return fmt.isGroupingUsed();
    }
    return false;
  }

  public static int parse__Ljava_lang_String_2Ljava_text_ParsePosition_2__Ljava_lang_Number_2(MJIEnv env, int objref,int sourceRef,int parsePositionRef) {
    String source = env.getStringObject(sourceRef);
    ParsePosition parsePosition = createParsePositionFromRef(env,parsePositionRef);
    NumberFormat fmt = getInstance(env,objref);
    Number number = null;
    if (fmt != null) {
      number = fmt.parse(source,parsePosition);
    }
    updateParsePositionRef(env,parsePositionRef, parsePosition);
    return createNumberRefFromNumber(env,number);
  }

  private static ParsePosition createParsePositionFromRef(MJIEnv env,int parsePositionRef) {
    int index = env.getIntField(parsePositionRef, "index");
    int errorIndex = env.getIntField(parsePositionRef, "errorIndex");
    ParsePosition ps = new ParsePosition(index);
    ps.setErrorIndex(errorIndex);
    return ps;
  }

  private static void updateParsePositionRef(MJIEnv env,int parsePositionRef, ParsePosition parsePosition) {
    env.setIntField(parsePositionRef, "index", parsePosition.getIndex());
    env.setIntField(parsePositionRef, "errorIndex", parsePosition.getErrorIndex());
  }

  private static int createNumberRefFromNumber(MJIEnv env,Number number) {
    int numberRef = MJIEnv.NULL;
    if(number instanceof Double) {
      numberRef = env.newObject("java.lang.Double");
      env.setDoubleField(numberRef, "value", number.doubleValue());
    } else if(number instanceof Long) {
      numberRef = env.newObject("java.lang.Long");
      env.setLongField(numberRef, "value", number.longValue());
    }
    return numberRef;
  }

}
