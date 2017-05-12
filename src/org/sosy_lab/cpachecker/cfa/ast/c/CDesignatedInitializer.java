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
package org.sosy_lab.cpachecker.cfa.ast.c;


import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.ast.AbstractInitializer;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import java.util.List;
import java.util.Objects;

public class CDesignatedInitializer extends AbstractInitializer implements CInitializer {


  private final List<CDesignator> designators;
  private final CInitializer right;

  public CDesignatedInitializer(
      FileLocation pFileLocation,
      final List<CDesignator> pLeft,
      final CInitializer pRight) {
    super(pFileLocation);
    designators = ImmutableList.copyOf(pLeft);
    right = pRight;
  }

  @Override
  public String toASTString() {
    return from(designators).transform(CDesignator.TO_AST_STRING).join(Joiner.on(""))
        + " = " + right.toASTString();
  }

  public List<CDesignator> getDesignators() {
    return designators;
  }

  public CInitializer getRightHandSide() {
    return right;
  }

  @Override
  public <R, X extends Exception> R accept(CInitializerVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(designators);
    result = prime * result + Objects.hashCode(right);
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

    if (!(obj instanceof CDesignatedInitializer)
        || !super.equals(obj)) {
      return false;
    }

    CDesignatedInitializer other = (CDesignatedInitializer) obj;

    return Objects.equals(other.designators, designators) && Objects.equals(other.right, right);
  }

}