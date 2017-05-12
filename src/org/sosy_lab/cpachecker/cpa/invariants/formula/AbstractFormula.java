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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;


abstract class AbstractFormula<ConstantType> implements NumeralFormula<ConstantType> {

  private final BitVectorInfo info;

  public AbstractFormula(BitVectorInfo pInfo) {
    this.info = pInfo;
  }

  @Override
  public BitVectorInfo getBitVectorInfo() {
    return this.info;
  }

}
