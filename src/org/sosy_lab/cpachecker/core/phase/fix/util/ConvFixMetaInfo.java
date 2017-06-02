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
package org.sosy_lab.cpachecker.core.phase.fix.util;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.bugfix.MutableASTForFix;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFix.IntegerFixMode;

public class ConvFixMetaInfo implements IntegerFixMetaInfo {

  private final String expInStr;
  private final CType originType;
  private final CType demandType;

  private ConvFixMetaInfo(MutableASTForFix exp, CType tOrigin, CType tDemand) {
    expInStr = exp.synthesize();
    originType = tOrigin;
    demandType = tDemand;
  }

  public static ConvFixMetaInfo of(MutableASTForFix exp, CType tOrigin, CType tDemand) {
    return new ConvFixMetaInfo(exp, tOrigin, tDemand);
  }

  @Override
  public IntegerFixMode getMode() {
    return IntegerFixMode.CHECK_CONV;
  }

  @Override
  public String toString() {
    return "\"_exp\":" + "\"" + SourceStringInliner.inline(expInStr) + "\"" +
        "," +
        "\"_origin\":" + "\"" + originType.toString() + "\"" + "," +
        "\"_demand\":" + "\"" + demandType.toString() + "\"";
  }
}
