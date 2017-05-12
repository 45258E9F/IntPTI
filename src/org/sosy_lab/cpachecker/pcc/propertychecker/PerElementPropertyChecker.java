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
 * Checks if an abstract state or a set of abstract states adheres to the property which should be
 * checked by the specific implementation of PerElementPropertyChecker. Property is always checked
 * individually for every element.
 */
public abstract class PerElementPropertyChecker implements PropertyChecker {

  @Override
  public boolean satisfiesProperty(Collection<AbstractState> pCertificate) {
    for (AbstractState elem : pCertificate) {
      if (!satisfiesProperty(elem)) {
        return false;
      }
    }
    return true;
  }

}
