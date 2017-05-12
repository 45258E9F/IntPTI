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
package org.sosy_lab.cpachecker.cpa.shape.merge.abs;

import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstraintRepresentation;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GuardedValue implements AbstractValue {

  private Long value;

  /* The guard consists of simple constraints and a disjunction. */

  private Set<SymbolicExpression> guard;
  private Set<ConstraintRepresentation> disjunction;

  private GuardedValue(Long pValue, Set<ConstraintRepresentation> pGuard) {
    value = pValue;
    guard = new HashSet<>();
    disjunction = new HashSet<>();
    // if the maximum guard depth is configured, we should check whether all the input guard
    // conditions should be added
    int maxDepth = CoreShapeAdapter.getInstance().getMaxGuardDepth();
    int counter = 0;
    for (ConstraintRepresentation cr : pGuard) {
      if (cr instanceof SymbolicExpression) {
        guard.add((SymbolicExpression) cr);
      } else {
        disjunction.add(cr);
      }
      if (maxDepth > 0) {
        counter++;
        if (counter >= maxDepth) {
          break;
        }
      }
    }
  }

  private GuardedValue(
      Long pValue, Set<SymbolicExpression> pGuard, Set<ConstraintRepresentation>
      pDisjunction) {
    value = pValue;
    guard = new HashSet<>();
    disjunction = new HashSet<>();
    // one heuristic: we add disjunctions on prior because they are more "informative"
    int maxDepth = CoreShapeAdapter.getInstance().getMaxGuardDepth();
    int counter = 0;
    for (ConstraintRepresentation clause : pDisjunction) {
      disjunction.add(clause);
      if (maxDepth > 0) {
        counter++;
        if (counter >= maxDepth) {
          return;
        }
      }
    }
    for (SymbolicExpression se : pGuard) {
      guard.add(se);
      if (maxDepth > 0) {
        counter++;
        if (counter >= maxDepth) {
          return;
        }
      }
    }
  }

  public static GuardedValue of(Long pValue, Set<ConstraintRepresentation> pGuard) {
    return new GuardedValue(pValue, pGuard);
  }

  public static GuardedValue of(Long pValue, GuardedValue pGV) {
    return new GuardedValue(pValue, pGV.guard, pGV.disjunction);
  }

  @Override
  public Long getValue() {
    return value;
  }

  public Set<SymbolicExpression> getGuard() {
    return Collections.unmodifiableSet(guard);
  }

  public Set<ConstraintRepresentation> getDisjunction() {
    return Collections.unmodifiableSet(disjunction);
  }

  /**
   * Inherit guard constraint of the given guarded value. This method is used for interpretation
   * purpose.
   */
  GuardedValue inherit(GuardedValue pValue) {
    Set<SymbolicExpression> newGuard = new HashSet<>(guard);
    Set<ConstraintRepresentation> newDisjunction = new HashSet<>(disjunction);
    newGuard.addAll(pValue.guard);
    newDisjunction.addAll(pValue.disjunction);
    return new GuardedValue(value, newGuard, newDisjunction);
  }

  /**
   * Append guard constraint to the current guarded value. This method is used for state merge.
   */
  void addMoreGuards(Set<ConstraintRepresentation> pGuards) {
    int maxDepth = CoreShapeAdapter.getInstance().getMaxGuardDepth();
    int counter = guard.size() + disjunction.size();
    for (ConstraintRepresentation cr : pGuards) {
      // we perform precondition check here because there has been some constraints
      if (maxDepth > 0) {
        if (counter >= maxDepth) {
          return;
        }
      }
      if (cr instanceof SymbolicExpression) {
        guard.add((SymbolicExpression) cr);
      } else {
        disjunction.add(cr);
      }
      counter++;
    }
  }

  GuardedValue replaceValue(Map<Long, Long> replaces) {
    Long replacement = replaces.get(value);
    if (replacement != null) {
      return new GuardedValue(replacement, guard, disjunction);
    } else {
      return this;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value, guard);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof GuardedValue)) {
      return false;
    }
    GuardedValue that = (GuardedValue) obj;
    return Objects.equal(value, that.value) && Objects.equal(guard, that.guard);
  }

  @Override
  public String toString() {
    return String.valueOf(value) + " | " + guard.toString() + " , " + disjunction.toString();
  }
}
