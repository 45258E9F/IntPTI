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
package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.NumeralFormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FunctionDeclaration;
import org.sosy_lab.solver.api.FunctionDeclarationKind;
import org.sosy_lab.solver.api.Model;
import org.sosy_lab.solver.api.Model.ValueAssignment;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.solver.basicimpl.tactics.Tactic;
import org.sosy_lab.solver.visitors.DefaultFormulaVisitor;
import org.sosy_lab.solver.visitors.TraversalProcess;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormulaLinearizationManager {
  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerView fmgr;
  private final NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifmgr;
  private final PolicyIterationStatistics statistics;

  // Opt environment cached to perform evaluation queries on the model.
  private Model model;

  public static final String CHOICE_VAR_NAME = "__POLICY_CHOICE_";
  private final UniqueIdGenerator choiceVarCounter = new UniqueIdGenerator();

  public FormulaLinearizationManager(
      BooleanFormulaManager pBfmgr, FormulaManagerView pFmgr,
      NumeralFormulaManagerView<IntegerFormula, IntegerFormula> pIfmgr,
      PolicyIterationStatistics pStatistics) {
    bfmgr = pBfmgr;
    fmgr = pFmgr;
    ifmgr = pIfmgr;
    statistics = pStatistics;
  }

  /**
   * Convert non-concave statements into disjunctions.
   *
   * At the moment handles:
   *
   * x NOT(EQ(A, B)) => (A > B) \/ (A < B)
   */
  public BooleanFormula linearize(BooleanFormula input) {
    return bfmgr.transformRecursively(new BooleanFormulaTransformationVisitor(fmgr) {
      @Override
      public BooleanFormula visitNot(BooleanFormula pOperand) {
        List<BooleanFormula> split = fmgr.splitNumeralEqualityIfPossible(pOperand);

        // Pattern matching on (NOT (= A B)).
        if (split.size() == 2) {
          return bfmgr.or(
              bfmgr.not(split.get(0)), bfmgr.not(split.get(1))
          );
        }
        return super.visitNot(pOperand);
      }
    }, input);
  }

  /**
   * Annotate disjunctions with choice variables.
   */
  public BooleanFormula annotateDisjunctions(BooleanFormula input) {
    return bfmgr.transformRecursively(new BooleanFormulaTransformationVisitor(fmgr) {
      @Override
      public BooleanFormula visitOr(List<BooleanFormula> processed) {
        IntegerFormula choiceVar = getFreshVar();
        List<BooleanFormula> newArgs = new ArrayList<>();
        for (int i = 0; i < processed.size(); i++) {
          newArgs.add(
              bfmgr.and(
                  processed.get(i), fmgr.makeEqual(choiceVar, ifmgr.makeNumber(i))));
        }
        return bfmgr.or(newArgs);
      }
    }, input);
  }

  private IntegerFormula getFreshVar() {
    String freshVarName = CHOICE_VAR_NAME + choiceVarCounter.getFreshId();
    return ifmgr.makeVariable(freshVarName);
  }

  /**
   * Removes disjunctions from the {@code input} formula, by replacing them
   * with arguments which were used to generate the {@code model}.
   */
  public BooleanFormula enforceChoice(
      final BooleanFormula input,
      final Model model
  ) {
    Map<Formula, Formula> mapping = new HashMap<>();
    for (ValueAssignment entry : model) {
      String termName = entry.getName();
      if (termName.contains(CHOICE_VAR_NAME)) {
        BigInteger value = (BigInteger) entry.getValue();
        mapping.put(ifmgr.makeVariable(termName), ifmgr.makeNumber(value));
      }
    }

    BooleanFormula pathSelected = fmgr.substitute(input, mapping);
    pathSelected = fmgr.simplify(pathSelected);
    return pathSelected;
  }

  /**
   * Removes UFs and ITEs from the formula, effectively making it's semantics
   * "concave".
   */
  public BooleanFormula convertToPolicy(
      BooleanFormula f,
      Model pModel) throws InterruptedException {

    model = pModel;

    statistics.ackermannizationTimer.start();
    f = fmgr.applyTactic(f, Tactic.NNF);

    // Get rid of UFs.
    BooleanFormula noUFs = processUFs(f);

    // Get rid of ite-expressions.
    BooleanFormula out = bfmgr.visit(new ReplaceITEVisitor(), noUFs);
    statistics.ackermannizationTimer.stop();

    return out;
  }

  /**
   * TODO: does not correctly replace if-then-else's
   * which occur INSIDE the formula.
   */
  private class ReplaceITEVisitor
      extends BooleanFormulaManagerView.BooleanFormulaTransformationVisitor {

    private ReplaceITEVisitor() {
      super(fmgr);
    }

    @Override
    public BooleanFormula visitIfThenElse(
        BooleanFormula pCondition, BooleanFormula pThenFormula, BooleanFormula pElseFormula) {

      Boolean cond = model.evaluate(pCondition);
      if (cond != null && cond) {
        return pThenFormula;
      } else {
        return pElseFormula;
      }
    }
  }

  /**
   * Ackermannization:
   * Requires a fixpoint computation as UFs can take other UFs as arguments.
   * First removes UFs with no arguments, etc.
   */
  private BooleanFormula processUFs(BooleanFormula f) {
    Multimap<String, Pair<Formula, List<Formula>>> UFs = findUFs(f);

    Map<Formula, Formula> substitution = new HashMap<>();
    List<BooleanFormula> extraConstraints = new ArrayList<>();

    for (String funcName : UFs.keySet()) {
      List<Pair<Formula, List<Formula>>> ufList = new ArrayList<>(UFs.get(funcName));
      for (int idx1 = 0; idx1 < ufList.size(); idx1++) {
        Pair<Formula, List<Formula>> p = ufList.get(idx1);

        Formula uf = p.getFirst();
        List<Formula> args = p.getSecondNotNull();

        Formula freshVar = fmgr.makeVariable(fmgr.getFormulaType(uf),
            freshUFName(idx1));
        substitution.put(uf, freshVar);

        for (int idx2 = idx1 + 1; idx2 < ufList.size(); idx2++) {
          Pair<Formula, List<Formula>> p2 = ufList.get(idx2);
          List<Formula> otherArgs = p2.getSecondNotNull();

          Formula otherUF = p2.getFirst();

          /**
           * If UFs are equal under the given model, force them to be equal in
           * the resulting policy bound.
           */
          Preconditions.checkState(args.size() == otherArgs.size());
          boolean argsEqual = true;
          for (int i = 0; i < args.size(); i++) {
            Object evalA = model.evaluate(args.get(i));
            Object evalB = model.evaluate(otherArgs.get(i));
            if (evalA != null && evalB != null && !evalA.equals(evalB)) {
              argsEqual = false;
            }
          }
          if (argsEqual) {
            Formula otherFreshVar = fmgr.makeVariable(
                fmgr.getFormulaType(otherUF),
                freshUFName(idx2)
            );
            extraConstraints.add(fmgr.makeEqual(freshVar, otherFreshVar));
          }
        }
      }
    }

    // Get rid of UFs.
    BooleanFormula formulaNoUFs = fmgr.substitute(f, substitution);
    return bfmgr.and(
        formulaNoUFs, bfmgr.and(extraConstraints)
    );
  }

  private Multimap<String, Pair<Formula, List<Formula>>> findUFs(Formula f) {
    final Multimap<String, Pair<Formula, List<Formula>>> UFs = HashMultimap.create();

    fmgr.visitRecursively(new DefaultFormulaVisitor<TraversalProcess>() {
      @Override
      protected TraversalProcess visitDefault(Formula f) {
        return TraversalProcess.CONTINUE;
      }

      @Override
      public TraversalProcess visitFunction(
          Formula f,
          List<Formula> args,
          FunctionDeclaration<?> decl) {
        if (decl.getKind() == FunctionDeclarationKind.UF) {
          UFs.put(decl.getName(), Pair.of(f, args));

        }
        return TraversalProcess.CONTINUE;
      }
    }, f);

    return UFs;
  }

  private String freshUFName(int idx) {
    return "__UF_fresh_" + idx;
  }
}
