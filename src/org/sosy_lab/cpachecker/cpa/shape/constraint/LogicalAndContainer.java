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
package org.sosy_lab.cpachecker.cpa.shape.constraint;

import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.CRVisitor;

import java.util.ArrayList;
import java.util.List;

public class LogicalAndContainer implements ConstraintRepresentation {

  private final List<ConstraintRepresentation> conjuncts = new ArrayList<>();

  private LogicalAndContainer(List<? extends ConstraintRepresentation> pConjuncts) {
    conjuncts.addAll(pConjuncts);
  }

  public static ConstraintRepresentation of(List<? extends ConstraintRepresentation> pConjuncts) {
    int size = pConjuncts.size();
    if (size == 0) {
      return SEs.toConstant(KnownSymbolicValue.TRUE, CNumericTypes.INT);
    } else if (size == 1) {
      return pConjuncts.get(0);
    } else {
      List<ConstraintRepresentation> subConjuncts = new ArrayList<>();
      for (ConstraintRepresentation cr : pConjuncts) {
        // perform necessary reduction in creating AND container
        if (cr instanceof SymbolicExpression) {
          ShapeValue value = ((SymbolicExpression) cr).getValue();
          if (value.equals(KnownSymbolicValue.ZERO) || value.equals(KnownExplicitValue.ZERO)) {
            return SEs.toConstant(KnownSymbolicValue.FALSE, CNumericTypes.INT);
          }
          if (value.equals(KnownSymbolicValue.TRUE) || value.equals(KnownExplicitValue.ONE)) {
            continue;
          }
        }
        subConjuncts.add(cr);
      }
      return innerOf(subConjuncts);
    }
  }

  private static ConstraintRepresentation innerOf(
      List<? extends ConstraintRepresentation>
          pConjuncts) {
    int size = pConjuncts.size();
    if (size == 0) {
      return SEs.toConstant(KnownSymbolicValue.TRUE, CNumericTypes.INT);
    } else if (size == 1) {
      return pConjuncts.get(0);
    } else {
      return new LogicalAndContainer(pConjuncts);
    }
  }

  public int size() {
    return conjuncts.size();
  }

  public ConstraintRepresentation get(int index) {
    if (index < 0 || index >= conjuncts.size()) {
      throw new IllegalArgumentException("illegal index for clause");
    }
    return conjuncts.get(index);
  }

  @Override
  public <T> T accept(CRVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(conjuncts);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof LogicalAndContainer)) {
      return false;
    }
    LogicalAndContainer that = (LogicalAndContainer) obj;
    return Objects.equal(conjuncts, that.conjuncts);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("AND [\n");
    for (ConstraintRepresentation cr : conjuncts) {
      builder.append(cr.toString()).append("\n");
    }
    builder.append("] -> size = ").append(conjuncts.size());
    return builder.toString();
  }
}
