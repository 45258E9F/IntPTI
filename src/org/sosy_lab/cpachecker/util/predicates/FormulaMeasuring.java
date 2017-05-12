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
package org.sosy_lab.cpachecker.util.predicates;

import com.google.common.collect.ImmutableSortedSet;

import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FunctionDeclaration;
import org.sosy_lab.solver.visitors.DefaultBooleanFormulaVisitor;
import org.sosy_lab.solver.visitors.TraversalProcess;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class FormulaMeasuring {

  public static class FormulaMeasures {
    private int conjunctions = 0;
    private int disjunctions = 0;
    private int negations = 0;
    private int atoms = 0;
    private final Set<String> variables = new HashSet<>();

    public int getAtoms() {
      return atoms;
    }

    public int getConjunctions() {
      return conjunctions;
    }

    public int getDisjunctions() {
      return disjunctions;
    }

    public int getNegations() {
      return negations;
    }

    public ImmutableSortedSet<String> getVariables() {
      return ImmutableSortedSet.copyOf(this.variables);
    }
  }

  private final FormulaManagerView managerView;

  public FormulaMeasuring(FormulaManagerView pManagerView) {
    this.managerView = pManagerView;
  }

  public FormulaMeasures measure(BooleanFormula formula) {
    FormulaMeasures result = new FormulaMeasures();
    managerView.getBooleanFormulaManager().visitRecursively(
        new FormulaMeasuringVisitor(managerView, result), formula
    );
    return result;
  }

  private static class FormulaMeasuringVisitor
      extends DefaultBooleanFormulaVisitor<TraversalProcess> {

    private final FormulaMeasures measures;
    private final FormulaManagerView fmgr;

    FormulaMeasuringVisitor(FormulaManagerView pFmgr, FormulaMeasures pMeasures) {
      measures = pMeasures;
      fmgr = pFmgr;
    }

    @Override
    protected TraversalProcess visitDefault() {
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitAtom(
        BooleanFormula pAtom,
        FunctionDeclaration<BooleanFormula> decl) {
      measures.atoms++;

      BooleanFormula atom = fmgr.uninstantiate(pAtom);
      measures.variables.addAll(fmgr.extractVariableNames(atom));
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitNot(BooleanFormula pOperand) {
      measures.negations++;
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitAnd(List<BooleanFormula> pOperands) {
      measures.conjunctions++;
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitOr(List<BooleanFormula> pOperands) {
      measures.disjunctions++;
      return TraversalProcess.CONTINUE;
    }
  }
}
