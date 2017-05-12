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
package org.sosy_lab.cpachecker.cfa.model.java;


import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class JDeclarationEdge extends ADeclarationEdge {


  public JDeclarationEdge(
      final String pRawSignature, final FileLocation pFileLocation,
      final CFANode pPredecessor, final CFANode pSuccessor, final JDeclaration pDeclaration) {

    super(pRawSignature, pFileLocation, pPredecessor, pSuccessor, pDeclaration);

  }

  @Override
  public JDeclaration getDeclaration() {
    return (JDeclaration) declaration;
  }

  @Override
  public Optional<JDeclaration> getRawAST() {
    return Optional.of((JDeclaration) declaration);
  }
}
