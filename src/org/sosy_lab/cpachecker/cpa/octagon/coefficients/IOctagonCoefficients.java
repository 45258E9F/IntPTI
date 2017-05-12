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

/**
 * Class for representing Coeffecients which show the value of a variable
 * dependant on all other variables and a constant value.
 */
@SuppressWarnings("rawtypes")
public interface IOctagonCoefficients {

  /**
   * Creates a NumArray out of the coefficient array.
   */
  NumArray getNumArray(OctagonManager manager);

  /**
   * Returns the size of the coefficient list.
   */
  int size();

  IOctagonCoefficients expandToSize(int size, OctagonState oct);

  /**
   * Adds two OctCoefficients.
   *
   * @return The new added Coefficient.
   */
  IOctagonCoefficients add(IOctagonCoefficients summand);

  /**
   * Substracts two OctCoefficients.
   *
   * @return The new substracted Coefficient.
   */
  IOctagonCoefficients sub(IOctagonCoefficients subtrahend);

  IOctagonCoefficients mul(IOctagonCoefficients factor);

  IOctagonCoefficients mul(OctagonNumericValue factor);

  IOctagonCoefficients mul(OctagonInterval interval);

  IOctagonCoefficients div(IOctagonCoefficients divisor);

  IOctagonCoefficients div(OctagonNumericValue divisor);

  IOctagonCoefficients div(OctagonInterval interval);

  /**
   * Indicates whether the Coefficient List only consists of a constant value.
   */
  boolean hasOnlyConstantValue();

  boolean hasOnlyOneValue();

  int getVariableIndex();

  @Override
  boolean equals(Object obj);

  @Override
  int hashCode();

  @Override
  String toString();

}
