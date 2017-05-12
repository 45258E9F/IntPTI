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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * This class represents initializer expressions in variable and field declarations.
 */
public class JInitializerExpression extends AInitializerExpression implements JInitializer {

  public JInitializerExpression(FileLocation pFileLocation, JExpression pExpression) {
    super(pFileLocation, pExpression);
  }

  @Override
  public JExpression getExpression() {
    return (JExpression) super.getExpression();
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JInitializerExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
