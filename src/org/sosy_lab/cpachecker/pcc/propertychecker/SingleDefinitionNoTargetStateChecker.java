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


public class SingleDefinitionNoTargetStateChecker implements PropertyChecker {

  private SingleDefinitionChecker defChecker;
  private NoTargetStateChecker targetChecker;


  public SingleDefinitionNoTargetStateChecker(final String varWithSingleDef) {
    defChecker = new SingleDefinitionChecker(varWithSingleDef);
    targetChecker = new NoTargetStateChecker();
  }

  @Override
  public boolean satisfiesProperty(final AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean satisfiesProperty(final Collection<AbstractState> pCertificate) {
    return defChecker.satisfiesProperty(pCertificate)
        && targetChecker.satisfiesProperty(pCertificate);
  }

}
