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
package org.sosy_lab.cpachecker.cpa.shape.util;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.constraint.BinarySE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.CastSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstantSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstraintRepresentation;
import org.sosy_lab.cpachecker.cpa.shape.constraint.LogicalAndContainer;
import org.sosy_lab.cpachecker.cpa.shape.constraint.LogicalOrContainer;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicKind;
import org.sosy_lab.cpachecker.cpa.shape.constraint.UnarySE;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.MergeTable;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

/**
 * The interface for managing constraints collected from assumptions.
 * Constraints are immutable. In other words, we do not replace the symbols in the existing
 * symbolic expressions. That means, symbols can be non-representatives.
 */
public final class ConstraintsPool {

  private final List<SymbolicExpression> constraints;
  private final Map<Long, CType> typeMap;

  /**
   * Each constraint representation is one clause of the total disjunction.
   * Invariant: merged state should have 0 or 1 clause. The number of clauses can be more than
   * one only when interpreting abstract values.
   */
  private List<ConstraintRepresentation> disjunction;

  private final Set<SymbolicExpression> branchConditions;


  public ConstraintsPool() {
    constraints = new ArrayList<>();
    typeMap = new TreeMap<>();
    disjunction = new ArrayList<>();
    branchConditions = Sets.newHashSet();
  }

  /* ************** */
  /* copying method */
  /* ************** */

  /**
   * Deep copy the constraints pool.
   *
   * @param iPool the source constraints pool
   */
  public void putAll(ConstraintsPool iPool) {
    constraints.addAll(iPool.constraints);
    typeMap.putAll(iPool.typeMap);

    disjunction.addAll(iPool.disjunction);

    branchConditions.addAll(iPool.branchConditions);
  }

  /* *********************** */
  /* constraint manipulation */
  /* *********************** */

  public void push(SymbolicExpression pSE) {
    // CHECK: if the symbolic expression is TRUE, nothing changes
    if (pSE.getValueKind() == SymbolicKind.SYMBOLIC && pSE.getValue().equals(
        KnownSymbolicValue.TRUE)) {
      return;
    }
    if (pSE.getValueKind() == SymbolicKind.EXPLICIT && pSE.getValue().equals(
        KnownExplicitValue.ONE)) {
      return;
    }
    constraints.add(pSE);
    Map<Long, CType> typedSymbols = CoreShapeAdapter.getInstance().extractTypedSymbols(pSE);
    typeMap.putAll(typedSymbols);
  }

  public void addDisjunction(Collection<ConstraintRepresentation> clauses) {
    disjunction.addAll(clauses);
    for (ConstraintRepresentation clause : clauses) {
      typeMap.putAll(CoreShapeAdapter.getInstance().extractTypedSymbols(clause));
    }
  }

  /**
   * When associating a symbolic value S with an explicit value E, we should generate a
   * constraint S=E and let SMT solver to perform constant propagation.
   *
   * @param pKey   a symbolic value
   * @param pValue an explicit value associated with the given symbolic value
   */
  public void putExplicitValue(KnownSymbolicValue pKey, KnownExplicitValue pValue) {
    CType type = typeMap.get(pKey.getAsLong());
    if (type == null) {
      // the given symbolic value is not in the constraint
      // future constraints containing this value will have this value replaced with explicit value.
      return;
    }
    ConstantSE lE = new ConstantSE(pKey, type, CIdExpression.DUMMY_ID(type));
    ConstantSE rE = new ConstantSE(pValue, type, CIdExpression.DUMMY_ID(type));
    BinarySE eqE = new BinarySE(lE, rE, BinaryOperator.EQUALS, CNumericTypes.INT, CIdExpression
        .DUMMY_ID(CNumericTypes.INT));
    constraints.add(eqE);
    // it is unnecessary to perform definite assignment check for this constraint, because we
    // generate this constraint under the premise that this assignment is definite
    // we add the relevance info for preventing checking unit constraint when constructing the
    // boolean formulae
  }

  /**
   * Get the size of accumulated constraints.
   */
  public int size() {
    return constraints.size();
  }

  /**
   * Get the symbolic expression on the specified index.
   */
  public SymbolicExpression get(int index) {
    if (index >= constraints.size()) {
      throw new IllegalArgumentException("specified index out of bound");
    }
    return constraints.get(index);
  }

  /**
   * Get the set of symbolic values necessary for definite assignment checking.
   * The return data is a mapping from symbolic value to the index of unit constraints (since it is
   * possible that one symbolic value has multiple unit constraints)
   */
  public Multimap<Long, Integer> getNeedCheckingDefiniteAssignmentSet() {
    Builder<Long, Integer> builder = ImmutableMultimap.builder();
    for (int i = 0; i < constraints.size(); i++) {
      SymbolicExpression constraint = constraints.get(i);
      Set<Long> symbols = CoreShapeAdapter.getInstance().extractSymbols(constraint);
      if (symbols.size() == 1) {
        // this is a unit constraint
        Long symbol = Iterables.getOnlyElement(symbols);
        builder.put(symbol, i);
      }
    }
    return builder.build();
  }

  /**
   * Export the full list of constraints.
   */
  public List<SymbolicExpression> getConstraints() {
    return ImmutableList.copyOf(constraints);
  }

  @Nullable
  public ConstraintRepresentation getDisjunction() {
    return LogicalAndContainer.of(disjunction);
  }

  /**
   * Prune constraints containing unused values in the specified value set.
   * ATTENTION 1: Constraint pruning leads to loss of information. For example, two constraints x =
   * y and y = z both contain a unused value `y` such that they are pruned. However, these two
   * constraints can be propagated to x = z. This does not corrupt the soundness of analysis though.
   * ATTENTION 2: The specified set of pruning values should contain not only representatives but
   * also other elements in the equivalence class. This is because values in constraints are not
   * regularized.
   *
   * @param pValues the set of unused values
   * @return whether any constraint is removed
   */
  public boolean prune(final Set<Long> pValues) {
    Set<Long> prunedValues = new TreeSet<>(pValues);
    boolean changed = false;
    Iterator<ConstraintRepresentation> iterator = disjunction.iterator();
    while (iterator.hasNext()) {
      ConstraintRepresentation clause = iterator.next();
      Set<Long> symbols = CoreShapeAdapter.getInstance().extractSymbols(clause);
      if (!Sets.intersection(symbols, prunedValues).isEmpty()) {
        // prune disjunction here
        iterator.remove();
        changed = true;
      }
    }
    Set<Integer> removalSet = new TreeSet<>();
    for (int i = 0; i < constraints.size(); i++) {
      SymbolicExpression se = constraints.get(i);
      Set<Long> symbols = CoreShapeAdapter.getInstance().extractSymbols(se);
      if (!Sets.intersection(symbols, prunedValues).isEmpty()) {
        removalSet.add(i);
      }
    }
    changed = changed || removeConstraints(removalSet);
    // re-construct type mapping
    typeMap.clear();
    branchConditions.clear();
    for (SymbolicExpression se : constraints) {
      typeMap.putAll(CoreShapeAdapter.getInstance().extractTypedSymbols(se));
    }
    for (ConstraintRepresentation clause : disjunction) {
      typeMap.putAll(CoreShapeAdapter.getInstance().extractTypedSymbols(clause));
    }
    return changed;
  }

  public Set<SymbolicExpression> getAfterBranchCondition() {
    return Collections.unmodifiableSet(branchConditions);
  }

  public void setAfterBranchCondition(Set<SymbolicExpression> pConditions) {
    branchConditions.clear();
    branchConditions.addAll(pConditions);
  }

  /**
   * Merge current constraints pool with another one. Equivalence table is required to query the
   * symbolic equivalence relations.
   */
  public ConstraintsPool merge(ConstraintsPool thatPool, MergeTable pTable) {
    // branch conditions are computed only when performing state merge
    branchConditions.clear();
    thatPool.branchConditions.clear();

    ConstraintsPool merged = new ConstraintsPool();
    // STEP 1: unify the prefix constraints
    int bound = Math.min(constraints.size(), thatPool.constraints.size());
    int index;
    for (index = 0; index < bound; index++) {
      SymbolicExpression se1 = constraints.get(index);
      SymbolicExpression se2 = thatPool.constraints.get(index);
      SymbolicExpression se = unifySymbolicExpressions(se1, se2, pTable);
      if (se == null) {
        break;
      }
      merged.push(se);
    }
    // STEP 2: traverse the remaining constraints.
    // (1) If C1 and C2 can be unified, we add them into the prefix constraints and removed them.
    // (2) If C1 and C2 are reversely-unified, we add them as branching conditions respectively.
    List<SymbolicExpression> remSes1 = new ArrayList<>();
    List<SymbolicExpression> remSes2 = new ArrayList<>();
    for (int i = index; i < constraints.size(); i++) {
      remSes1.add(constraints.get(i));
    }
    for (int i = index; i < thatPool.constraints.size(); i++) {
      remSes2.add(thatPool.constraints.get(i));
    }
    Iterator<SymbolicExpression> iterator1 = remSes1.iterator();
    while (iterator1.hasNext()) {
      SymbolicExpression se1 = iterator1.next();
      Iterator<SymbolicExpression> iterator2 = remSes2.iterator();
      while (iterator2.hasNext()) {
        SymbolicExpression se2 = iterator2.next();
        if (se1 instanceof BinarySE && se2 instanceof BinarySE) {
          BinarySE binSe1 = (BinarySE) se1;
          BinarySE binSe2 = (BinarySE) se2;
          BinaryOperator op1 = binSe1.getOperator();
          BinaryOperator op2 = binSe2.getOperator();
          SymbolicExpression operand11 = binSe1.getOperand1();
          SymbolicExpression operand12 = binSe1.getOperand2();
          SymbolicExpression operand21 = binSe2.getOperand1();
          SymbolicExpression operand22 = binSe2.getOperand2();
          SymbolicExpression mergedOp1, mergedOp2;
          if (op1 == op2) {
            // two symbolic expressions have the same operator
            mergedOp1 = unifySymbolicExpressions(operand11, operand21, pTable);
            if (mergedOp1 != null) {
              mergedOp2 = unifySymbolicExpressions(operand12, operand22, pTable);
              if (mergedOp2 != null) {
                // case (1)
                iterator1.remove();
                iterator2.remove();
                BinarySE binMerged = new BinarySE(mergedOp1, mergedOp2, op1, binSe1.getType(),
                    binSe1.getOriginalExpression());
                merged.push(binMerged);
                break;
              }
            }
          }
          // if we reach here, two symbolic expressions have not matched yet
          BinaryOperator newOp2 = op2;
          if (op2.isLogicalOperator()) {
            newOp2 = op2.getReversedLogicalOperator();
          }
          if (newOp2 != op2) {
            // we exchange the position of two operands and try the unification again
            if (op1 == newOp2) {
              mergedOp1 = unifySymbolicExpressions(operand11, operand22, pTable);
              if (mergedOp1 != null) {
                mergedOp2 = unifySymbolicExpressions(operand12, operand21, pTable);
                if (mergedOp2 != null) {
                  // case (1)
                  iterator1.remove();
                  iterator2.remove();
                  BinarySE binMerged = new BinarySE(mergedOp1, mergedOp2, op1, binSe1.getType(),
                      binSe1.getOriginalExpression());
                  merged.push(binMerged);
                  break;
                }
              }
            }
          }
        }
      }
    }
    // all the reminiscent constraints are treated as branch-specific conditions
    for (SymbolicExpression se : remSes1) {
      branchConditions.add(se);
    }
    for (SymbolicExpression se : remSes2) {
      thatPool.branchConditions.add(se);
    }

    // STEP 3: choose some constraints for constructing the disjunction
    // Invariant: the merged state should have only one clause in the disjunction
    List<ConstraintRepresentation> conjuncts1 = new ArrayList<>();
    List<ConstraintRepresentation> conjuncts2 = new ArrayList<>();
    conjuncts1.addAll(remSes1);
    conjuncts2.addAll(remSes2);
    conjuncts1.addAll(disjunction);
    conjuncts2.addAll(thatPool.disjunction);
    if (!conjuncts1.isEmpty() && !conjuncts2.isEmpty()) {
      List<ConstraintRepresentation> clauses = new ArrayList<>();
      clauses.add(LogicalAndContainer.of(conjuncts1));
      clauses.add(LogicalAndContainer.of(conjuncts2));
      ConstraintRepresentation newDisjunction = LogicalOrContainer.of(clauses);
      merged.disjunction.add(newDisjunction);
      // calculate the symbolic set for the new disjunction
      merged.typeMap.putAll(CoreShapeAdapter.getInstance().extractTypedSymbols(newDisjunction));
    }

    // that's all forks
    return merged;
  }

  /* ***************** */
  /* auxiliary methods */
  /* ***************** */

  /**
   * Remove the constraints of the specified indexes
   *
   * @return whether any constraint is removed
   */
  private boolean removeConstraints(Set<Integer> removedSet) {
    if (removedSet.isEmpty()) {
      return false;
    }
    // STEP 1: remove symbolic expressions
    boolean hasRemoved = false;
    TreeSet<Integer> removeSet = new TreeSet<>(removedSet);
    Iterator<Integer> removeIt = removeSet.iterator();
    int toRemoved = removeIt.next();
    int cursor = 0;
    Iterator<SymbolicExpression> seIt = constraints.iterator();
    seIt.next();
    while (seIt.hasNext()) {
      if (cursor == toRemoved) {
        hasRemoved = true;
        seIt.remove();
        if (!removeIt.hasNext()) {
          break;
        } else {
          toRemoved = removeIt.next();
        }
      }
      seIt.next();
      cursor++;
    }

    return hasRemoved;
  }

  @Nullable
  public static SymbolicExpression unifySymbolicExpressions(
      SymbolicExpression pSe1,
      SymbolicExpression pSe2,
      MergeTable pTable) {
    if (!pSe1.getClass().equals(pSe2.getClass())) {
      return null;
    }
    if (!pSe1.getType().getCanonicalType().equals(pSe2.getType().getCanonicalType())) {
      return null;
    }
    if (pSe1 instanceof ConstantSE) {
      assert (pSe2 instanceof ConstantSE);
      if (pSe1.getValueKind() != pSe2.getValueKind()) {
        return null;
      }
      if (pSe1.getValueKind() == SymbolicKind.EXPLICIT) {
        KnownExplicitValue v1 = (KnownExplicitValue) pSe1.getValue();
        KnownExplicitValue v2 = (KnownExplicitValue) pSe2.getValue();
        if (v1.equals(v2)) {
          return pSe1;
        } else {
          return null;
        }
      } else if (pSe1.getValueKind() == SymbolicKind.SYMBOLIC) {
        KnownSymbolicValue v1 = (KnownSymbolicValue) pSe1.getValue();
        KnownSymbolicValue v2 = (KnownSymbolicValue) pSe2.getValue();
        KnownSymbolicValue mv = null;
        if (v1.equals(v2)) {
          mv = v1;
        } else {
          Long r1 = pTable.getRepresentativeForRel1(v1.getAsLong());
          Long r2 = pTable.getRepresentativeForRel2(v2.getAsLong());
          Long v = pTable.merge(r1, r2);
          if (v != null) {
            mv = KnownSymbolicValue.valueOf(v);
          }
        }
        if (mv != null) {
          return new ConstantSE(mv, pSe1.getType(), pSe1.getOriginalExpression());
        } else {
          return null;
        }
      } else {
        // Constant symbolic expression contains UNKNOWN value. Is it possible?
        return pSe1;
      }
    } else if (pSe1 instanceof UnarySE) {
      assert (pSe2 instanceof UnarySE);
      if (((UnarySE) pSe1).getOperator() != ((UnarySE) pSe2).getOperator()) {
        return null;
      }
      SymbolicExpression mOperand = unifySymbolicExpressions(((UnarySE) pSe1).getOperand(),
          ((UnarySE) pSe2).getOperand(), pTable);
      if (mOperand == null) {
        return null;
      } else {
        return new UnarySE(mOperand, ((UnarySE) pSe1).getOperator(), pSe1.getType(),
            pSe1.getOriginalExpression());
      }
    } else if (pSe1 instanceof BinarySE) {
      assert (pSe2 instanceof BinarySE);
      if (((BinarySE) pSe1).getOperator() != ((BinarySE) pSe2).getOperator()) {
        return null;
      }
      SymbolicExpression mOp1 = unifySymbolicExpressions(((BinarySE) pSe1).getOperand1(), (
          (BinarySE) pSe2).getOperand1(), pTable);
      if (mOp1 == null) {
        return null;
      }
      SymbolicExpression mOp2 = unifySymbolicExpressions(((BinarySE) pSe1).getOperand2(),
          ((BinarySE) pSe2).getOperand2(), pTable);
      if (mOp2 == null) {
        return null;
      }
      return new BinarySE(mOp1, mOp2, ((BinarySE) pSe1).getOperator(), pSe1.getType(),
          pSe1.getOriginalExpression());
    } else if (pSe1 instanceof CastSE) {
      assert (pSe2 instanceof CastSE);
      SymbolicExpression mOperand = unifySymbolicExpressions(((CastSE) pSe1).getOperand(),
          ((CastSE) pSe2).getOperand(), pTable);
      if (mOperand == null) {
        return null;
      } else {
        return new CastSE(mOperand, pSe1.getType(), pSe1.getOriginalExpression());
      }
    } else {
      // unsupported symbolic expression type
      return null;
    }
  }

  /* **************** */
  /* override methods */
  /* **************** */

  @Override
  public int hashCode() {
    // the third mapping can be derived from the first two, thus it is excluded in computing hash
    return Objects.hashCode(constraints, typeMap, disjunction);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof ConstraintsPool)) {
      return false;
    }
    ConstraintsPool other = (ConstraintsPool) obj;
    return Objects.equal(constraints, other.constraints) && Objects.equal(typeMap, other.typeMap)
        && Objects.equal(disjunction, other.disjunction);
  }

}
