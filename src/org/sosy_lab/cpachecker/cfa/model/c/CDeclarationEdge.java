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
package org.sosy_lab.cpachecker.cfa.model.c;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class CDeclarationEdge extends ADeclarationEdge {


  public CDeclarationEdge(
      final String pRawSignature, final FileLocation pFileLocation,
      final CFANode pPredecessor, final CFANode pSuccessor, final CDeclaration pDeclaration) {

    super(pRawSignature, pFileLocation, pPredecessor, pSuccessor, pDeclaration);

  }

  @Override
  public CDeclaration getDeclaration() {
    return (CDeclaration) declaration;
  }

  @Override
  public Optional<CDeclaration> getRawAST() {
    return Optional.of((CDeclaration) declaration);
  }
}
