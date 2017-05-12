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
package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.collect.Iterables.transform;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.ast.AbstractInitializer;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import java.util.List;
import java.util.Objects;


public class CInitializerList extends AbstractInitializer implements CInitializer, CAstNode {

  private final List<CInitializer> initializerList;

  public CInitializerList(
      final FileLocation pFileLocation,
      final List<CInitializer> pInitializerList) {
    super(pFileLocation);
    initializerList = ImmutableList.copyOf(pInitializerList);
  }

  public List<CInitializer> getInitializers() {
    return initializerList;
  }

  @Override
  public String toASTString() {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append("{ ");
    Joiner.on(", ").appendTo(lASTString, transform(initializerList, CInitializer.TO_AST_STRING));
    lASTString.append(" }");

    return lASTString.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(initializerList);
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

    if (!(obj instanceof CInitializerList)
        || !super.equals(obj)) {
      return false;
    }

    CInitializerList other = (CInitializerList) obj;

    return Objects.equals(other.initializerList, initializerList);
  }

  @Override
  public <R, X extends Exception> R accept(CInitializerVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }
}
