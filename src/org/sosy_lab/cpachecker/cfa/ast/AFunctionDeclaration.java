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
package org.sosy_lab.cpachecker.cfa.ast;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.types.IAFunctionType;

import java.util.List;
import java.util.Objects;


public abstract class AFunctionDeclaration extends AbstractDeclaration {

  private final List<AParameterDeclaration> parameters;

  public AFunctionDeclaration(
      FileLocation pFileLocation, IAFunctionType pType, String pName,
      List<? extends AParameterDeclaration> pParameters) {
    super(pFileLocation, true, pType, pName, pName);

    parameters = ImmutableList.copyOf(pParameters);
  }

  @Override
  public IAFunctionType getType() {
    return (IAFunctionType) super.getType();
  }

  public List<? extends AParameterDeclaration> getParameters() {
    return parameters;
  }

  @Override
  public String getQualifiedName() {
    return getName();
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(parameters);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AFunctionDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    AFunctionDeclaration other = (AFunctionDeclaration) obj;

    return Objects.equals(other.parameters, parameters);
  }
}
