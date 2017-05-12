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

public class LogicalOrContainer implements ConstraintRepresentation {

  private final List<ConstraintRepresentation> disjuncts = new ArrayList<>();

  private LogicalOrContainer(List<? extends ConstraintRepresentation> pDisjuncts) {
    disjuncts.addAll(pDisjuncts);
  }

  public static ConstraintRepresentation of(List<? extends ConstraintRepresentation> pDisjuncts) {
    int size = pDisjuncts.size();
    if (size == 0) {
      return SEs.toConstant(KnownSymbolicValue.FALSE, CNumericTypes.INT);
    } else if (size == 1) {
      return pDisjuncts.get(0);
    } else {
      List<ConstraintRepresentation> subDisjuncts = new ArrayList<>();
      for (ConstraintRepresentation cr : pDisjuncts) {
        if (cr instanceof SymbolicExpression) {
          ShapeValue value = ((SymbolicExpression) cr).getValue();
          if (value.equals(KnownSymbolicValue.ZERO) || value.equals(KnownExplicitValue.ZERO)) {
            continue;
          }
          if (value.equals(KnownSymbolicValue.TRUE) || value.equals(KnownExplicitValue.ONE)) {
            return SEs.toConstant(KnownSymbolicValue.TRUE, CNumericTypes.INT);
          }
        }
        subDisjuncts.add(cr);
      }
      return innerOf(subDisjuncts);
    }
  }

  private static ConstraintRepresentation innerOf(
      List<? extends ConstraintRepresentation>
          pDisjuncts) {
    int size = pDisjuncts.size();
    if (size == 0) {
      return SEs.toConstant(KnownSymbolicValue.FALSE, CNumericTypes.INT);
    } else if (size == 1) {
      return pDisjuncts.get(0);
    } else {
      return new LogicalOrContainer(pDisjuncts);
    }
  }

  public int size() {
    return disjuncts.size();
  }

  public ConstraintRepresentation get(int index) {
    if (index < 0 || index >= disjuncts.size()) {
      throw new IllegalArgumentException("illegal index for clause");
    }
    return disjuncts.get(index);
  }

  @Override
  public <T> T accept(CRVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(disjuncts);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof LogicalOrContainer)) {
      return false;
    }
    LogicalOrContainer that = (LogicalOrContainer) obj;
    return Objects.equal(disjuncts, that.disjuncts);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("OR [\n");
    for (ConstraintRepresentation cr : disjuncts) {
      builder.append(cr.toString()).append("\n");
    }
    builder.append("] -> size = ").append(disjuncts.size());
    return builder.toString();
  }
}
