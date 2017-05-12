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
package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import java.util.List;


public abstract class FunctionEntryNode extends CFANode {

  private final FileLocation location;
  private final AFunctionDeclaration functionDefinition;
  private final List<String> parameterNames;
  private final Optional<? extends AVariableDeclaration> returnVariable;

  // Check if call edges are added in the second pass
  private final FunctionExitNode exitNode;

  protected FunctionEntryNode(
      final FileLocation pFileLocation, String pFunctionName,
      FunctionExitNode pExitNode, final AFunctionDeclaration pFunctionDefinition,
      final List<String> pParameterNames,
      final Optional<? extends AVariableDeclaration> pReturnVariable) {

    super(pFunctionName);
    location = checkNotNull(pFileLocation);
    functionDefinition = pFunctionDefinition;
    parameterNames = ImmutableList.copyOf(pParameterNames);
    exitNode = pExitNode;
    returnVariable = checkNotNull(pReturnVariable);
  }

  public FileLocation getFileLocation() {
    return location;
  }

  public FunctionExitNode getExitNode() {
    return exitNode;
  }

  public AFunctionDeclaration getFunctionDefinition() {
    return functionDefinition;
  }

  public List<String> getFunctionParameterNames() {
    return parameterNames;
  }

  public abstract List<? extends AParameterDeclaration> getFunctionParameters();

  /**
   * Return a declaration for a pseudo variable that can be used to store
   * the return value of this function (if it has one).
   * This variable is the same as the one used by {@link AReturnStatement#asAssignment()}.
   */
  public Optional<? extends AVariableDeclaration> getReturnVariable() {
    return returnVariable;
  }
}