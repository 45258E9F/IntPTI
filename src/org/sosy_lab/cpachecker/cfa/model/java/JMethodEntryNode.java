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
package org.sosy_lab.cpachecker.cfa.model.java;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

import java.util.List;

public class JMethodEntryNode extends FunctionEntryNode {

  public JMethodEntryNode(
      final FileLocation pFileLocation,
      final JMethodDeclaration pMethodDefinition,
      final FunctionExitNode pExitNode,
      final List<String> pParameterNames,
      final Optional<? extends JVariableDeclaration> pReturnVariable) {

    super(pFileLocation, pMethodDefinition.getName(), pExitNode, pMethodDefinition,
        pParameterNames, pReturnVariable);
  }

  @Override
  public JMethodDeclaration getFunctionDefinition() {
    return (JMethodDeclaration) super.getFunctionDefinition();
  }

  @Override
  public List<JParameterDeclaration> getFunctionParameters() {
    return getFunctionDefinition().getParameters();
  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<? extends JVariableDeclaration> getReturnVariable() {
    return (Optional<? extends JVariableDeclaration>) super.getReturnVariable();
  }
}
