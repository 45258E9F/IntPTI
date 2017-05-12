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
package org.sosy_lab.cpachecker.cpa.validvars;

import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

import java.io.Serializable;


public class ValidVarsState
    implements AbstractState, AbstractQueryableState, Graphable, Serializable {

  private static final long serialVersionUID = 9159663474411886276L;
  private final ValidVars validVariables;

  public ValidVarsState(ValidVars pValidVars) {
    validVariables = pValidVars;
  }

  public ValidVars getValidVariables() {
    return validVariables;
  }

  @Override
  public String getCPAName() {
    return "ValidVars";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    return validVariables.containsVar(pProperty);
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    return checkProperty(pProperty);
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    throw new InvalidQueryException(
        "Cannot modify values of valid vars state (" + this.getClass().getCanonicalName()
            + ").");
  }

  @Override
  public String toDOTLabel() {
    return validVariables.toStringInDOTFormat();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }
}
