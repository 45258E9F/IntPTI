/*
 * IntPTI: integer error fixing by proper-type inference
 * Copyright (c) 2017.
 *
 * Open-source component:
 *
 * CPAchecker
 * Copyright (C) 2007-2014  Dirk Beyer
 *
 * Guava: Google Core Libraries for Java
 * Copyright (C) 2010-2006  Google
 *
 *
 */
package org.sosy_lab.cpachecker.util;

import org.sosy_lab.cpachecker.cpa.sign.SIGN;

import java.util.regex.Pattern;


public class CheckTypesOfStringsUtil {

  private CheckTypesOfStringsUtil() {
  }

  public static boolean isLong(String s) {
    return Pattern.matches("-?\\d+", s);
  }

  public static boolean isSIGN(String s) {
    try {
      SIGN.valueOf(s);
    } catch (IllegalArgumentException ex) {
      return false;
    }
    return true;
  }
}
