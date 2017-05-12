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
package org.sosy_lab.cpachecker.cfa.model;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;


public class ADeclarationEdge extends AbstractCFAEdge {

  protected final ADeclaration declaration;

  protected ADeclarationEdge(
      final String pRawSignature, final FileLocation pFileLocation,
      final CFANode pPredecessor, final CFANode pSuccessor, final ADeclaration pDeclaration) {

    super(pRawSignature, pFileLocation, pPredecessor, pSuccessor);
    declaration = pDeclaration;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.DeclarationEdge;
  }

  public ADeclaration getDeclaration() {
    return declaration;
  }

  @Override
  public Optional<? extends ADeclaration> getRawAST() {
    return Optional.of(declaration);
  }

  @Override
  public String getCode() {
    return declaration.toASTString();
  }

}
