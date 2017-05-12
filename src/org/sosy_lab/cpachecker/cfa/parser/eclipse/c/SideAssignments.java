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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;


class SideAssignments {

  private final Deque<List<CAstNode>> preSideAssignments;
  private final Deque<List<CAstNode>> postSideAssignments;
  private final Deque<List<Pair<IASTExpression, CIdExpression>>> conditionalExpressions;

  public SideAssignments() {
    preSideAssignments = new ArrayDeque<>();
    postSideAssignments = new ArrayDeque<>();
    conditionalExpressions = new ArrayDeque<>();
  }

  public SideAssignments(
      Deque<List<CAstNode>> preSideAssignments,
      Deque<List<CAstNode>> postSideAssignments,
      Deque<List<Pair<IASTExpression, CIdExpression>>> conditionalExpressions) {
    this.preSideAssignments = preSideAssignments;
    this.postSideAssignments = postSideAssignments;
    this.conditionalExpressions = conditionalExpressions;
  }

  public void enterBlock() {
    preSideAssignments.push(new ArrayList<CAstNode>());
    postSideAssignments.push(new ArrayList<CAstNode>());
    conditionalExpressions.push(new ArrayList<Pair<IASTExpression, CIdExpression>>());
  }

  public void leaveBlock() {
    Preconditions.checkArgument(!preSideAssignments.isEmpty(),
        "leaving sideassignment block before handling all of them");
    Preconditions.checkArgument(!postSideAssignments.isEmpty(),
        "leaving sideassignment block before handling all of them");
    Preconditions.checkArgument(!conditionalExpressions.isEmpty(),
        "leaving sideassignment block before handling all of them");
    preSideAssignments.pop();
    postSideAssignments.pop();
    conditionalExpressions.pop();
  }

  public List<CAstNode> getAndResetPreSideAssignments() {
    List<CAstNode> result = new ArrayList<>(preSideAssignments.peek());
    preSideAssignments.peek().clear();
    return result;
  }

  public List<CAstNode> getAndResetPostSideAssignments() {
    List<CAstNode> result = new ArrayList<>(postSideAssignments.peek());
    postSideAssignments.peek().clear();
    return result;
  }

  public List<Pair<IASTExpression, CIdExpression>> getAndResetConditionalExpressions() {
    List<Pair<IASTExpression, CIdExpression>> result =
        new ArrayList<>(conditionalExpressions.peek());
    conditionalExpressions.peek().clear();
    return result;
  }

  public boolean hasConditionalExpression() {
    if (conditionalExpressions.isEmpty()) {
      return false;
    }
    return !conditionalExpressions.peek().isEmpty();
  }

  public boolean hasPreSideAssignments() {
    if (preSideAssignments.isEmpty()) {
      return false;
    }
    return !preSideAssignments.peek().isEmpty();
  }

  public boolean hasPostSideAssignments() {
    if (postSideAssignments.isEmpty()) {
      return false;
    }
    return !postSideAssignments.peek().isEmpty();
  }

  public List<Pair<IASTExpression, CIdExpression>> getConditionalExpressions() {
    return Collections.unmodifiableList(conditionalExpressions.peek());
  }

  public void addConditionalExpression(IASTExpression e, CIdExpression tempVar) {
    conditionalExpressions.peek().add(Pair.of(checkNotNull(e), tempVar));
  }

  public void addPreSideAssignment(CAstNode node) {
    preSideAssignments.peek().add(node);
  }

  public void addPostSideAssignment(CAstNode node) {
    postSideAssignments.peek().add(node);
  }
}
