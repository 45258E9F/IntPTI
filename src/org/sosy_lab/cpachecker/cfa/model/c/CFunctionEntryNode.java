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
package org.sosy_lab.cpachecker.cfa.model.c;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

import java.util.List;

public class CFunctionEntryNode extends FunctionEntryNode {

  public CFunctionEntryNode(
      final FileLocation pFileLocation,
      final CFunctionDeclaration pFunctionDefinition,
      final FunctionExitNode pExitNode,
      final List<String> pParameterNames,
      final Optional<CVariableDeclaration> pReturnVariable) {

    super(pFileLocation, pFunctionDefinition.getName(), pExitNode,
        pFunctionDefinition, pParameterNames, pReturnVariable);
  }

  @Override
  public CFunctionDeclaration getFunctionDefinition() {
    return (CFunctionDeclaration) super.getFunctionDefinition();
  }

  @Override
  public List<CParameterDeclaration> getFunctionParameters() {
    return getFunctionDefinition().getParameters();
  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<CVariableDeclaration> getReturnVariable() {
    return (Optional<CVariableDeclaration>) super.getReturnVariable();
  }
}
