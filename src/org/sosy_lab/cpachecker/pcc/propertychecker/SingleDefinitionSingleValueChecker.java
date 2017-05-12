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
package org.sosy_lab.cpachecker.pcc.propertychecker;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;

import java.util.Collection;

/**
 * Checks if a certain variable is defined at most once by the program and checks if a certain
 * variable has a specific value at a specific location marked by a label in the program.
 */
public class SingleDefinitionSingleValueChecker implements PropertyChecker {

  private SingleDefinitionChecker defChecker;
  private SingleValueChecker valChecker;


  public SingleDefinitionSingleValueChecker(
      String varWithSingleDef, String varWithSingleValue, String varValue,
      String labelForLocationWithSingleValue) {
    defChecker = new SingleDefinitionChecker(varWithSingleDef);
    valChecker =
        new SingleValueChecker(varWithSingleValue, varValue, labelForLocationWithSingleValue);
  }

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean satisfiesProperty(Collection<AbstractState> pCertificate) {
    boolean result = defChecker.satisfiesProperty(pCertificate);
    if (result) {
      result = valChecker.satisfiesProperty(pCertificate);
    }
    return result;
  }
}
