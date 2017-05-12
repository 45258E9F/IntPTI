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
package org.sosy_lab.cpachecker.cpa.octagon.coefficients;

import org.sosy_lab.cpachecker.cpa.octagon.OctagonState;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonInterval;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonNumericValue;
import org.sosy_lab.cpachecker.util.octagon.NumArray;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

@SuppressWarnings("rawtypes")
public final class OctagonUniversalCoefficients extends AOctagonCoefficients {

  public static final OctagonUniversalCoefficients INSTANCE = new OctagonUniversalCoefficients();

  private OctagonUniversalCoefficients() {
    super(0, null);
  }

  @Override
  public OctagonUniversalCoefficients expandToSize(int pSize, OctagonState oct) {
    return this;
  }

  @Override
  public int getVariableIndex() {
    throw new UnsupportedOperationException(
        "Do only call this method on coefficients with exactly one value");
  }

  @Override
  public OctagonUniversalCoefficients add(IOctagonCoefficients pOther) {
    return INSTANCE;
  }

  @Override
  public OctagonUniversalCoefficients sub(IOctagonCoefficients pOther) {
    return INSTANCE;
  }

  @Override
  public IOctagonCoefficients mul(OctagonNumericValue pFactor) {
    return INSTANCE;
  }

  @Override
  public IOctagonCoefficients mul(OctagonInterval interval) {
    return INSTANCE;
  }

  @Override
  protected IOctagonCoefficients mulInner(IOctagonCoefficients pOct) {
    return INSTANCE;
  }

  @Override
  protected IOctagonCoefficients divInner(IOctagonCoefficients pOct) {
    return INSTANCE;
  }

  @Override
  public IOctagonCoefficients div(OctagonNumericValue pDivisor) {
    return INSTANCE;
  }

  @Override
  public IOctagonCoefficients div(OctagonInterval interval) {
    return INSTANCE;
  }

  @Override
  public NumArray getNumArray(OctagonManager manager) {
    return null;
  }

  @Override
  public boolean hasOnlyConstantValue() {
    return false;
  }

  @Override
  public boolean hasOnlyOneValue() {
    return false;
  }

  @Override
  public int hashCode() {
    // this is a singleton we just use the system hashcode
    return System.identityHashCode(this);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof OctagonUniversalCoefficients;
  }

}
