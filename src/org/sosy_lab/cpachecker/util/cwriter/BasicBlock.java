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
package org.sosy_lab.cpachecker.util.cwriter;

import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class BasicBlock {

  private static final String SINGLE_INDENT = "  ";

  /**
   * element id of the ARG element that has the conditional statement
   */
  private final int stateId;

  /**
   * true for if, false for else
   */
  private boolean condition;

  /**
   * flag to determine whether this condition was closed by another merge node before
   */
  private boolean isClosedBefore = false;

  /**
   * the set of declarations already contained in this block
   */
  private Set<ADeclarationEdge> declarations = new HashSet<>();

  // this is the code of this element
  private final String firstCodeLine;
  private final List<Object> codeList;

  public BasicBlock(int pElementId, String pFunctionName) {
    stateId = pElementId;
    codeList = new ArrayList<>();
    firstCodeLine = pFunctionName;
  }

  public BasicBlock(int pElementId, CAssumeEdge pEdge, String pConditionString) {
    stateId = pElementId;
    codeList = new ArrayList<>();
    condition = pEdge.getTruthAssumption();
    firstCodeLine = pConditionString;
  }

  public int getStateId() {
    return stateId;
  }

  public boolean isCondition() {
    return condition;
  }

  public boolean isClosedBefore() {
    return isClosedBefore;
  }

  public void setClosedBefore(boolean pIsClosedBefore) {
    isClosedBefore = pIsClosedBefore;
  }

  public void write(Object pStatement) {
    if (!(pStatement instanceof String)
        || !((String) pStatement).isEmpty()) {
      codeList.add(pStatement);
    }
  }

  void addDeclaration(ADeclarationEdge declarationEdge) {
    declarations.add(declarationEdge);
  }

  /**
   * This method checks whether or nor the given declaration is already part of this block.
   *
   * This is needed, as some tools (e.g. llbmc, i.e. clang) do not allow re-declaration of a
   * previously declared variable.
   *
   * @param declarationEdge the edge to check
   * @return true, if the given declaration is already part of this block, else false
   */
  boolean hasDeclaration(ADeclarationEdge declarationEdge) {
    return declarations.contains(declarationEdge);
  }

  public String getCode() {
    return getCode("").toString();
  }

  private StringBuilder getCode(String pIndent) {
    StringBuilder ret = new StringBuilder();

    ret.append(pIndent);
    ret.append(firstCodeLine);
    ret.append(" {\n");

    String indent = pIndent + SINGLE_INDENT;

    for (Object obj : codeList) {
      // check whether we have a simple statement
      // or a conditional statement
      if (obj instanceof String) {
        ret.append(indent);
        ret.append((String) obj);
      } else if (obj instanceof BasicBlock) {
        ret.append(((BasicBlock) obj).getCode(indent));
      } else {
        assert false;
      }
      ret.append("\n");
    }

    ret.append(pIndent);
    ret.append("}\n");
    return ret;
  }

  @Override
  public String toString() {
    return "Element id: " + stateId + " Condition: " + condition + " .. is closed "
        + isClosedBefore;
  }
}