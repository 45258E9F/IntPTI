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
package org.sosy_lab.cpachecker.cpa.shape.visitors.constraint;

import org.sosy_lab.cpachecker.cpa.shape.constraint.BinarySE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.CastSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstantSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.LogicalAndContainer;
import org.sosy_lab.cpachecker.cpa.shape.constraint.LogicalOrContainer;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicKind;
import org.sosy_lab.cpachecker.cpa.shape.constraint.UnarySE;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;

import java.util.Set;
import java.util.TreeSet;

/**
 * Collect symbolic identifiers from a symbolic expression.
 */
public class SymbolicCollector implements CRVisitor<Set<Long>> {

  @Override
  public Set<Long> visit(ConstantSE pValue) {
    Set<Long> result = new TreeSet<>();
    if (pValue.getValueKind() == SymbolicKind.SYMBOLIC) {
      KnownSymbolicValue value = (KnownSymbolicValue) pValue.getValue();
      result.add(value.getAsLong());
    }
    return result;
  }

  @Override
  public Set<Long> visit(BinarySE pValue) {
    SymbolicExpression operand1 = pValue.getOperand1();
    SymbolicExpression operand2 = pValue.getOperand2();
    Set<Long> values = new TreeSet<>();
    values.addAll(operand1.accept(this));
    values.addAll(operand2.accept(this));
    return values;
  }

  @Override
  public Set<Long> visit(UnarySE pValue) {
    SymbolicExpression operand = pValue.getOperand();
    return operand.accept(this);
  }

  @Override
  public Set<Long> visit(CastSE pValue) {
    SymbolicExpression operand = pValue.getOperand();
    return operand.accept(this);
  }

  @Override
  public Set<Long> visit(LogicalOrContainer pContainer) {
    Set<Long> values = new TreeSet<>();
    for (int i = 0; i < pContainer.size(); i++) {
      values.addAll(pContainer.get(i).accept(this));
    }
    return values;
  }

  @Override
  public Set<Long> visit(LogicalAndContainer pContainer) {
    Set<Long> values = new TreeSet<>();
    for (int i = 0; i < pContainer.size(); i++) {
      values.addAll(pContainer.get(i).accept(this));
    }
    return values;
  }
}
