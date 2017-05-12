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
package org.sosy_lab.cpachecker.cfa.ast.java;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;

import java.util.Objects;

/**
 * This class makes the return of an object reference to the caller of
 * an constructor explicit. Semantically, it is the equivalent of return this;
 * It may however only occur at the end of an constructor in the cfa.
 *
 * The returnClassType only provides the compile time type, i. e. the class,
 * which declared the constructor. This may not always be the case,
 * i.e. super constructor invocation.
 */
public class JObjectReferenceReturn extends JReturnStatement {

  private final JClassType classReference;

  public JObjectReferenceReturn(FileLocation pFileLocation, JClassType pClassReference) {
    super(pFileLocation,
        Optional.<JExpression>of(new JThisExpression(pFileLocation, pClassReference)));
    classReference = pClassReference;
  }

  public JClassType getReturnClassType() {
    return classReference;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(classReference);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JObjectReferenceReturn)
        || !super.equals(obj)) {
      return false;
    }

    JObjectReferenceReturn other = (JObjectReferenceReturn) obj;

    return Objects.equals(other.classReference, classReference);
  }

}