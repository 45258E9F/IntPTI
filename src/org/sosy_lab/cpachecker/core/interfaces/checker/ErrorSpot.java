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
package org.sosy_lab.cpachecker.core.interfaces.checker;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

import javax.annotation.Nullable;

/**
 * For most cases, error spot should be an AST node (e.g. expression, statement).
 * However, in some cases, error spot should also be a blank edge.
 * This class is a universal solution of these two cases.
 */
public class ErrorSpot {

  @Nullable
  private final CAstNode astNode;
  @Nullable
  private final CFAEdge cfaEdge;

  private static final String UNDEFINED_FUNCTION = "#UNDEFINED#";

  public ErrorSpot(@Nullable CAstNode pNode, @Nullable CFAEdge pEdge) {
    astNode = pNode;
    cfaEdge = pEdge;
    Preconditions.checkArgument(astNode != null || cfaEdge != null);
  }

  public Optional<CAstNode> getASTNode() {
    return Optional.fromNullable(astNode);
  }

  public Optional<CFAEdge> getCFAEdge() {
    return Optional.fromNullable(cfaEdge);
  }

  public FileLocation getFileLocation() {
    if (astNode != null) {
      return astNode.getFileLocation();
    } else if (cfaEdge != null) {
      return cfaEdge.getFileLocation();
    } else {
      throw new AssertionError("Inconsistency error spot object.");
    }
  }

  public String getCode() {
    if (astNode != null) {
      return astNode.toASTString();
    } else if (cfaEdge != null) {
      return cfaEdge.getRawStatement();
    } else {
      throw new AssertionError("Inconsistency error spot object.");
    }
  }

  public String getFunctionName() {
    if (cfaEdge != null) {
      return cfaEdge.getPredecessor().getFunctionName();
    } else if (astNode instanceof CDeclaration) {
      String completeName = ((CDeclaration) astNode).getQualifiedName();
      int pos = completeName.indexOf("::");
      if (pos > 0) {
        return completeName.substring(0, pos);
      }
    }
    return UNDEFINED_FUNCTION;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(astNode, cfaEdge);
  }
}
