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
package org.sosy_lab.cpachecker.cpa.shape.merge.util;

import static org.sosy_lab.cpachecker.cpa.shape.util.ConstraintsPool.unifySymbolicExpressions;

import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.constraint.BinarySE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdgeFilter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.model.CStackFrame;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGRegion;
import org.sosy_lab.cpachecker.cpa.shape.merge.abs.GuardedAbstraction;
import org.sosy_lab.cpachecker.cpa.shape.util.EquivalenceRelation;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A utility class for evaluating less-or-equal relation.
 */
@Options(prefix = "cpa.shape")
public final class ShapeComplexLessOrEqual implements ShapeLessOrEqual {

  /**
   * How to compare constraints in shape state? We mainly have two methods:
   * (1) SUBSET: less state should have more constraints and more derived explicit values;
   * (2) IMPLICATION: less state should entail larger state.
   */
  private enum ConstraintComparisonType {
    SUBSET,
    IMPLICATION
  }

  @Option(secure = true, description = "type of less-or-equal operator", toUppercase = true)
  private ConstraintComparisonType comparisonType = ConstraintComparisonType.SUBSET;

  private final Solver solver;

  ShapeComplexLessOrEqual(Configuration pConfig, Solver pSolver)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    solver = pSolver;
  }

  /* ************************ */
  /* less-or-equal algorithms */
  /* ************************ */

  /**
   * Check if successor <= reached. (In other words, concrete states corresponding to larger
   * state should completely contain the concrete states of less state.)
   * Invariant: (S1, S2) = S, then we have S1 <= S and S2 <= S.
   *
   * @return true if the above statement holds, and false otherwise
   */
  @Override
  public boolean isLessOrEqual(ShapeState successor, ShapeState reached) {

    if (successor.equals(reached)) {
      return true;
    }

    // STEP 1: compare error flags
    if (!isErrorFlagsLessOrEqual(successor, reached)) {
      return false;
    }

    CShapeGraph pG1 = successor.getShapeGraph();
    CShapeGraph pG2 = reached.getShapeGraph();

    EquivalenceRelation<Long> eq1 = pG1.getEq();
    EquivalenceRelation<Long> eq2 = pG2.getEq();
    MergeTable table = new MergeTable(eq1, eq2);
    GuardedAbstraction superAbsInfo = pG2.getAbstraction();

    // STEP 2: check the equality of symbolic values
    if (!isEquivalenceLessOrEqual(pG1, pG2)) {
      return false;
    }

    // STEP 3: compare explicit value mappings
    if (!isExplicitsLessOrEqual(successor, reached, table)) {
      return false;
    }

    // STEP 4: compare the constraints (including inequality)
    if (!isConstraintsLessOrEqual(successor, reached, table)) {
      return false;
    }

    // STEP 5: compare the shape graphs
    if (pG1 == pG2) {
      return true;
    }
    // If we could have pG1 <= pG2, then two shape graphs must be comparable.
    // ** If two graphs have different sets of global objects, it could be reasonable because
    // different branches may create new string literals.
    if (pG1.getStackFrames().size() != pG2.getStackFrames().size()) {
      return false;
    }
    Deque<CStackFrame> stackG1 = pG1.getStackFrames(); // successor, expected to be smaller
    Deque<CStackFrame> stackG2 = pG2.getStackFrames(); // reached, expected to be larger
    Iterator<CStackFrame> stackIt1 = stackG1.descendingIterator();
    Iterator<CStackFrame> stackIt2 = stackG2.descendingIterator();
    while (stackIt1.hasNext() && stackIt2.hasNext()) {
      CStackFrame frameG1 = stackIt1.next();
      CStackFrame frameG2 = stackIt2.next();
      if (!frameG1.getFunctionDeclaration().equals(frameG2.getFunctionDeclaration())) {
        return false;
      }
      // The joined state (larger state) should have fewer variables, for unshared ones are simply
      // discarded.
      if (frameG1.getAllObjects().size() < frameG2.getAllObjects().size()) {
        return false;
      }
      // compare return objects
      if (!(frameG1.getFunctionDeclaration().getType().getReturnType().getCanonicalType()
          instanceof CVoidType) && !isFieldLessOrEqual(pG1, frameG1.getReturnObject(), pG2,
          frameG2.getReturnObject(), table, superAbsInfo)) {
        return false;
      }
      // compare each object in two stack frames
      // we only need to compare objects in share here
      // Since we only keep common local variables, G2 should contains fewer variables. In the
      // other words, variables in G2 should be contained in G1.
      for (String varName : frameG2.getVariables().keySet()) {
        if (!frameG1.containsVariable(varName)) {
          return false;
        }
        SGObject localVar1 = frameG1.getVariable(varName);
        SGObject localVar2 = frameG2.getVariable(varName);
        if (!isFieldLessOrEqual(pG1, localVar1, pG2, localVar2, table, superAbsInfo)) {
          return false;
        }
      }
    }

    // compare each global object
    // If (S1, S2) = S, then S contains all the global objects from S1 and S2.
    // That says, the larger state should have more global variables.
    Map<String, SGRegion> globalG1 = pG1.getGlobalObjects();
    Map<String, SGRegion> globalG2 = pG2.getGlobalObjects();
    if (globalG1.size() > globalG2.size()) {
      return false;
    }
    for (Entry<String, SGRegion> varEntry : globalG1.entrySet()) {
      String name = varEntry.getKey();
      SGRegion region1 = varEntry.getValue();
      if (!globalG2.containsKey(name)) {
        return false;
      }
      SGRegion region2 = globalG2.get(name);
      if (!isFieldLessOrEqual(pG1, region1, pG2, region2, table, superAbsInfo)) {
        return false;
      }
    }

    // compare each heap object
    // Note: it is possible that some heap objects are unreachable due to memory leak issue.
    // However, for S <= S', S' should contain all the objects in S in the sense of type and size
    // (but name does not matter, for example two branches create two heap objects with the same
    // type and size, then they are merged into one).
    Map<String, SGObject> heapG1 = pG1.getHeapObjects();
    Map<String, SGObject> heapG2 = pG2.getHeapObjects();
    // extend the second key set with all possible alias
    // alias mapping: N's hidden names and N -> N
    Map<String, String> alias = new HashMap<>();
    for (String key : heapG2.keySet()) {
      Collection<String> subKeys = superAbsInfo.getAlias(key);
      for (String subKey : subKeys) {
        alias.put(subKey, key);
      }
    }
    for (Entry<String, SGObject> heap1 : heapG1.entrySet()) {
      String key1 = heap1.getKey();
      SGObject object1 = heap1.getValue();
      String key2 = alias.get(key1);
      if (key2 == null) {
        return false;
      }
      SGObject object2 = heapG2.get(key2);
      if (!isFieldLessOrEqual(pG1, object1, pG2, object2, table, superAbsInfo)) {
        return false;
      }
    }

    // that's all forks
    return true;

  }

  /**
   * Examine the less-or-equal relation between two shape objects from two shape graphs.
   *
   * @return true if pObjLess <= pObjLarger and false otherwise.
   */
  private boolean isFieldLessOrEqual(
      CShapeGraph pGLess, SGObject pObjLess,
      CShapeGraph pGLarger, SGObject pObjLarger,
      MergeTable pTable, GuardedAbstraction pAbsLarger) {
    // If two objects are of different types, they are not comparable.
    if (!pObjLess.getClass().equals(pObjLarger.getClass())) {
      return false;
    }
    // If two objects have different sizes, they are not comparable.
    if (!pObjLess.getSize().equals(pObjLarger.getSize())) {
      return false;
    }
    // If two objects have different zero-initialization properties, they are not comparable.
    if (pObjLess.isZeroInit() != pObjLarger.isZeroInit()) {
      return false;
    }
    // Here we only examine the has-value edges, otherwise heap objects may be covered for
    // multiple times, which arises performance issue.
    // Consider two matching objects O and O' where O is in the sub-state and O' is in the merged
    // state (O <= O'). Then, for each has-value edge in O, O' should have a matching has-value edge.
    // Furthermore, if their values are v and v' respectively, then we have (1) v = v', (2) v is
    // abstracted into v'.
    SGHasValueEdgeFilter filter1 = SGHasValueEdgeFilter.objectFilter(pObjLess);
    SGHasValueEdgeFilter filter2 = SGHasValueEdgeFilter.objectFilter(pObjLarger);
    Set<SGHasValueEdge> hvEdges1 = pGLess.getHVEdges(filter1);
    for (SGHasValueEdge hvEdge : hvEdges1) {
      int offset = hvEdge.getOffset();
      CType type = hvEdge.getType();
      long value = hvEdge.getValue();
      filter2 = filter2.filterAtOffset(offset).filterByType(type);
      Set<SGHasValueEdge> matchingEdges = pGLarger.getHVEdges(filter2);
      if (matchingEdges.size() != 1) {
        return false;
      }
      // check the value here
      SGHasValueEdge mEdge = Iterables.getOnlyElement(matchingEdges);
      long mValue = mEdge.getValue();
      // the correctness of `isAbstractedTo` ? Since stop operator is typically used on two
      // states with join-merge relation, we can rely on abstraction info to derive exact
      // abstraction relation.

      if (!pTable.isEq(value, mValue) && !pAbsLarger.abstractFromTo(value, mValue)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Examine the less-or-equal relation between constraints in two shape states.
   * pGS <= pGR, thus pGR should contain fewer constraints
   */
  private boolean isConstraintsLessOrEqual(
      ShapeState successor, ShapeState reached,
      final MergeTable table) {

    CShapeGraph pGS = successor.getShapeGraph();
    CShapeGraph pGR = reached.getShapeGraph();

    switch (comparisonType) {
      case SUBSET: {
        // STEP 1: check less-or-equal relation for inequalities
        Multimap<Long, Long> neqReached = getHalf(pGR.getNeq());
        Multimap<Long, Long> neqSuccessor = pGS.getNeq();
        for (Entry<Long, Long> neqL : neqReached.entries()) {
          Long l = neqL.getKey();
          final Long r = neqL.getValue();
          boolean matched = false;
          Set<Long> els = table.getEquivalentValuesFromRel2ToRel1(l);
          for (Long el : els) {
            Collection<Long> sr = neqSuccessor.get(el);
            matched = FluentIterable.from(sr).filter(new Predicate<Long>() {
              @Override
              public boolean apply(Long pLong) {
                return table.isEq(pLong, r);
              }
            }).isEmpty();
            if (matched) {
              break;
            }
          }
          if (!matched) {
            return false;
          }
        }
        // STEP 2: check less-or-equal relation for constraints (symbolic expressions)
        List<SymbolicExpression> seS = pGS.getConstraints();
        List<SymbolicExpression> seR = pGR.getConstraints();
        if (seR.size() > seS.size()) {
          return false;
        }
        int index;
        for (index = 0; index < seR.size(); index++) {
          SymbolicExpression seFromR = seR.get(index);
          SymbolicExpression seFromS = seS.get(index);
          if (!seFromR.equals(seFromS)) {
            break;
          }
        }
        boolean isMatched = false;
        for (int i = index; i < seR.size(); i++) {
          SymbolicExpression seFromR = seR.get(i);
          if (!(seFromR instanceof BinarySE)) {
            return false;
          }
          BinarySE binR = (BinarySE) seFromR;
          BinaryOperator opR = binR.getOperator();
          SymbolicExpression operand1R = binR.getOperand1();
          SymbolicExpression operand2R = binR.getOperand2();
          for (int j = index; j < seS.size(); j++) {
            SymbolicExpression seFromS = seS.get(j);
            if (!(seFromS instanceof BinarySE)) {
              return false;
            }
            BinarySE binS = (BinarySE) seFromS;
            BinaryOperator opS = binS.getOperator();
            SymbolicExpression operand1S = binS.getOperand1();
            SymbolicExpression operand2S = binS.getOperand2();
            if (opR == opS) {
              if (unifySymbolicExpressions(operand1S, operand1R, table) != null &&
                  unifySymbolicExpressions(operand2S, operand2R, table) != null) {
                isMatched = true;
                break;
              }
            } else if (opR.getReversedLogicalOperator() == opS) {
              if (unifySymbolicExpressions(operand1S, operand2R, table) != null &&
                  unifySymbolicExpressions(operand2S, operand1R, table) != null) {
                isMatched = true;
                break;
              }
            }
          }
          if (isMatched) {
            isMatched = false;
          } else {
            return false;
          }
        }
        return true;
      }
      case IMPLICATION: {
        // we first generate boolean formulae for constraint pool, then we generate equality and
        // inequality relations as additional formulae
        try {
          BooleanFormula implying = successor.getTotalFormula();
          BooleanFormula implied = reached.getTotalFormula();
          return solver.implies(implying, implied);
        } catch (UnrecognizedCCodeException | InterruptedException | SolverException ex) {
          // if the exception is encountered, we simply return TRUE for continuing the check
          return true;
        }
      }
      default:
        return true;
    }
  }

  /**
   * Examine successor <= reached by comparing their error flags.
   */
  private boolean isErrorFlagsLessOrEqual(ShapeState successor, ShapeState reached) {
    // examine error flags
    if (successor.getInvalidReadStatus() && !reached.getInvalidReadStatus()) {
      return false;
    }
    if (successor.getInvalidWriteStatus() && !reached.getInvalidWriteStatus()) {
      return false;
    }
    if (successor.getInvalidFreeStatus() && !reached.getInvalidFreeStatus()) {
      return false;
    }
    if (successor.getMemoryLeakStatus() && !reached.getMemoryLeakStatus()) {
      return false;
    }
    if (successor.getStackAddressReturn() && !reached.getStackAddressReturn()) {
      return false;
    }
    // examine witnesses of errors
    if (!reached.getInvalidReadExpression().containsAll(successor.getInvalidReadExpression())) {
      return false;
    }
    if (!reached.getMemoryLeakCFAEdges().containsAll(successor.getMemoryLeakCFAEdges())) {
      return false;
    }
    return true;
  }

  /**
   * Examine the equivalence relations from successor and reached state. If pGS <= pGR, then for
   * each (v1, v2) \in pGR, we should have (v1, v2) \in pGS.
   */
  private boolean isEquivalenceLessOrEqual(CShapeGraph pGS, CShapeGraph pGR) {
    return pGS.getEq().isLessOrEqual(pGR.getEq());
  }

  /**
   * Examine the less_or_equal relation for symbolic-explicit mappings.
   * We expect pS1 <= pS2, such that each mapping v -> e in pS2 should have an equivalent one in
   * pS1.
   */
  private boolean isExplicitsLessOrEqual(ShapeState pS1, ShapeState pS2, MergeTable pTable) {
    BiMap<KnownExplicitValue, KnownSymbolicValue> exp2Sym1 = pS1.getExplicitValues().inverse();
    BiMap<KnownExplicitValue, KnownSymbolicValue> exp2Sym2 = pS2.getExplicitValues().inverse();
    for (Entry<KnownExplicitValue, KnownSymbolicValue> entry2 : exp2Sym2.entrySet()) {
      KnownExplicitValue expValue = entry2.getKey();
      KnownSymbolicValue symValue1 = exp2Sym1.get(expValue);
      if (symValue1 == null) {
        return false;
      } else {
        KnownSymbolicValue symValue2 = entry2.getValue();
        if (!symValue1.equals(symValue2)) {
          pTable.putExplicitEquality(symValue1.getAsLong(), symValue2.getAsLong(), expValue);
        }
      }
    }
    return true;
  }

  /* ************** */
  /* utility method */
  /* ************** */

  /**
   * Extract the half of inequality multimap to prevent duplicated processing.
   * Invariant: If (x,y) is in the inequality, then so is (y,x).
   */
  public static Multimap<Long, Long> getHalf(Multimap<Long, Long> pNeq) {
    Builder<Long, Long> builder = ImmutableMultimap.builder();
    for (Entry<Long, Long> entry : pNeq.entries()) {
      Long l = entry.getKey();
      Long r = entry.getValue();
      if (l < r) {
        builder.put(l, r);
      }
    }
    return builder.build();
  }

}
