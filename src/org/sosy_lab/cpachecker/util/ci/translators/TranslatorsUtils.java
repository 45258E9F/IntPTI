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
package org.sosy_lab.cpachecker.util.ci.translators;


public class TranslatorsUtils {

  private TranslatorsUtils() {
  }

  public static String getVarLessOrEqualValRequirement(final String pVar, final Number pVal) {
    StringBuilder sb = new StringBuilder();
    sb.append("(<= ");
    sb.append(pVar);
    sb.append(" ");
    sb.append(pVal.longValue());
    sb.append(")");
    return sb.toString();
  }

  public static String getVarGreaterOrEqualValRequirement(final String pVar, final Number pVal) {
    StringBuilder sb = new StringBuilder();
    sb.append("(>= ");
    sb.append(pVar);
    sb.append(" ");
    sb.append(pVal.longValue());
    sb.append(")");
    return sb.toString();
  }

  public static String getVarInBoundsRequirement(
      final String pVar,
      final Number pLow,
      final Number pHigh) {
    StringBuilder sb = new StringBuilder();
    sb.append("(and ");
    sb.append(getVarGreaterOrEqualValRequirement(pVar, pLow));
    sb.append(" ");
    sb.append(getVarLessOrEqualValRequirement(pVar, pHigh));
    sb.append(")");
    return sb.toString();
  }
}
