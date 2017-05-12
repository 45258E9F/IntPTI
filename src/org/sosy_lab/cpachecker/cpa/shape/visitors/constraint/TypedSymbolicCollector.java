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
package org.sosy_lab.cpachecker.cpa.shape.visitors.constraint;

import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.constraint.BinarySE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.CastSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstantSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.LogicalAndContainer;
import org.sosy_lab.cpachecker.cpa.shape.constraint.LogicalOrContainer;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicKind;
import org.sosy_lab.cpachecker.cpa.shape.constraint.UnarySE;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;

import java.util.Map;
import java.util.TreeMap;

public class TypedSymbolicCollector implements CRVisitor<Map<Long, CType>> {

  @Override
  public Map<Long, CType> visit(LogicalOrContainer pContainer) {
    Map<Long, CType> result = new TreeMap<>();
    for (int i = 0; i < pContainer.size(); i++) {
      result.putAll(pContainer.get(i).accept(this));
    }
    return result;
  }

  @Override
  public Map<Long, CType> visit(LogicalAndContainer pContainer) {
    Map<Long, CType> result = new TreeMap<>();
    for (int i = 0; i < pContainer.size(); i++) {
      result.putAll(pContainer.get(i).accept(this));
    }
    return result;
  }

  @Override
  public Map<Long, CType> visit(ConstantSE pValue) {
    Map<Long, CType> result = Maps.newTreeMap();
    if (pValue.getValueKind() == SymbolicKind.SYMBOLIC) {
      KnownSymbolicValue value = (KnownSymbolicValue) pValue.getValue();
      result.put(value.getAsLong(), pValue.getType());
    }
    return result;
  }

  @Override
  public Map<Long, CType> visit(BinarySE pValue) {
    Map<Long, CType> map1 = pValue.getOperand1().accept(this);
    Map<Long, CType> map2 = pValue.getOperand2().accept(this);
    map1.putAll(map2);
    return map1;
  }

  @Override
  public Map<Long, CType> visit(UnarySE pValue) {
    return pValue.getOperand().accept(this);
  }

  @Override
  public Map<Long, CType> visit(CastSE pValue) {
    return pValue.getOperand().accept(this);
  }
}
