/*
 * Tsmart-BD: The static analysis component of Tsmart platform
 *
 * Copyright (C) 2013-2017  Tsinghua University
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
package org.sosy_lab.cpachecker.cfa.types.java;

import org.sosy_lab.cpachecker.cfa.types.AFunctionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Description of a Java method through its return type and list of (possibly variable) parameters.
 */
public class JMethodType extends AFunctionType implements JType {

  private static final long serialVersionUID = 1324108617808888102L;

  private static final JMethodType UNRESOLVABLE_TYPE = new JMethodType(
      JSimpleType.getUnspecified(), new ArrayList<JType>(), false);

  /**
   * Creates a new <code>JMethodType</code> object that stores the given information.
   *
   * @param pReturnType   the return type of the method this object describes
   * @param pParameters   the list of parameters the described method takes
   * @param pTakesVarArgs if <code>true</code>, the described method takes a variable amount of
   *                      arguments, otherwise not
   */
  public JMethodType(JType pReturnType, List<JType> pParameters, boolean pTakesVarArgs) {
    super(pReturnType, pParameters, pTakesVarArgs);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<JType> getParameters() {
    return (List<JType>) super.getParameters();
  }

  /**
   * Returns a {@link JMethodType} object that describes an unresolvable method.
   *
   * @return a {@link JMethodType} object that describes an unresolvable method
   */
  public static JMethodType createUnresolvableType() {
    return UNRESOLVABLE_TYPE;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }
}
