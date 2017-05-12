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
 * Default implementation of property checker. Accepts every abstract state and every set of
 * abstract states. Does not check any property.
 */
public class DefaultPropertyChecker implements PropertyChecker {

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    return true;
  }

  @Override
  public boolean satisfiesProperty(Collection<AbstractState> pCertificate) {
    return true;
  }

}
