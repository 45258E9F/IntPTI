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
package org.sosy_lab.cpachecker.util.assumptions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Representation of an assumption of the form \land_i. pc = l_i ==> \phi_i
 */
public class AssumptionWithLocation implements Appender {

  private final FormulaManagerView manager;

  // map from location to (conjunctive) list of invariants
  private final Map<CFANode, BooleanFormula> map = new HashMap<>();

  public AssumptionWithLocation(FormulaManagerView pManager) {
    manager = pManager;
  }

  public static AssumptionWithLocation copyOf(AssumptionWithLocation a) {
    AssumptionWithLocation result = new AssumptionWithLocation(a.manager);
    result.map.putAll(a.map);
    return result;
  }

  /**
   * Return the number of locations for which we have an assumption.
   */
  public int getNumberOfLocations() {
    return map.size();
  }

  @Override
  public void appendTo(Appendable out) throws IOException {
    Joiner.on('\n').appendTo(out, Collections2.transform(map.entrySet(), assumptionFormatter));
  }

  @Override
  public String toString() {
    return Appenders.toString(this);
  }

  private static final Function<Entry<CFANode, BooleanFormula>, String> assumptionFormatter
      = new Function<Entry<CFANode, BooleanFormula>, String>() {

    @Override
    public String apply(Map.Entry<CFANode, BooleanFormula> entry) {
      int nodeId = entry.getKey().getNodeNumber();
      BooleanFormula assumption = entry.getValue();
      return "pc = " + nodeId + "\t =====>  " + assumption.toString();
    }
  };

  public void add(CFANode node, BooleanFormula assumption) {
    checkNotNull(node);
    checkNotNull(assumption);
    BooleanFormulaManager bfmgr = manager.getBooleanFormulaManager();
    if (!bfmgr.isTrue(assumption)) {
      BooleanFormula oldInvariant = map.get(node);
      if (oldInvariant == null) {
        map.put(node, assumption);
      } else {
        map.put(node, bfmgr.and(oldInvariant, assumption));
      }
    }
  }
}
