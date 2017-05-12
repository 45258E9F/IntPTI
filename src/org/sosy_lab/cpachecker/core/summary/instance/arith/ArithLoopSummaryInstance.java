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
package org.sosy_lab.cpachecker.core.summary.instance.arith;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.summary.apply.AbstractLoopSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.apply.ApplicableExternalLoopSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.apply.ApplicableInternalLoopSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ast.LinearConstraint;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ast.LinearVariable;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.cpa.apron.ApronState;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.List;
import java.util.Map;

import apron.Abstract0;
import apron.Lincons0;

public class ArithLoopSummaryInstance extends AbstractLoopSummaryInstance
    implements
    ApplicableExternalLoopSummaryInstance<List<LinearConstraint<Integer, LinearVariable>>>,
    ApplicableInternalLoopSummaryInstance<List<LinearConstraint<Integer, LinearVariable>>> {

  private Map<CFAEdge, List<LinearConstraint<Integer, LinearVariable>>> exitEdgeToConst =
      Maps.newHashMap();
  private Map<CFAEdge, List<LinearConstraint<Integer, LinearVariable>>> entryEdgeToConst =
      Maps.newHashMap();

  private Map<CFAEdge, ApronState> exitEdgeToApron = Maps.newHashMap();
  private Map<CFAEdge, ApronState> entryEdgeToApron = Maps.newHashMap();

  public ArithLoopSummaryInstance(Loop loop) {
    super(loop);
  }

  public void addConstraintsForExitEdge(CFAEdge edge, ApronState state) {
    List<LinearConstraint<Integer, LinearVariable>> linearConstraints = Lists.newArrayList();
    storeLinearConstraintsFromApronState(state, linearConstraints);
    exitEdgeToConst.put(edge, linearConstraints);
    exitEdgeToApron.put(edge, state);
  }

  public void addConstraintsForEntryEdge(CFAEdge edge, ApronState state) {
    List<LinearConstraint<Integer, LinearVariable>> linearConstraints = Lists.newArrayList();
    storeLinearConstraintsFromApronState(state, linearConstraints);
    entryEdgeToConst.put(edge, linearConstraints);
    entryEdgeToApron.put(edge, state);
  }

  public static ArithLoopSummaryInstance of(Loop loop) {
    return new ArithLoopSummaryInstance(loop);
  }

  private void storeLinearConstraintsFromApronState(
      ApronState apronState,
      List<LinearConstraint<Integer, LinearVariable>> linearConstraints) {
    Abstract0 abstract0 = apronState.getApronNativeState();
    Lincons0[] linearCons = abstract0.toLincons(abstract0.getCreationManager());
    for (Lincons0 lincons : linearCons) {
      linearConstraints.add(LinearConstraint.fromApronConstraint(apronState, lincons));
    }
  }

  @Override
  public boolean isEqualTo(SummaryInstance pThat) {
    if (pThat instanceof ArithLoopSummaryInstance) {
      return false;
    } else {
      ArithLoopSummaryInstance that = (ArithLoopSummaryInstance) pThat;

      if (!compareMap(this.exitEdgeToApron, that.getExitEdgeToApronState())) {
        return false;
      }
      if (!compareMap(this.entryEdgeToApron, that.getEntryEdgeToApronState())) {
        return false;
      }
      return true;
    }
  }

  boolean compareMap(Map<CFAEdge, ApronState> map1, Map<CFAEdge, ApronState> map2) {
    if (map1.size() != map2.size()) {
      return false;
    }
    for (CFAEdge edge : map1.keySet()) {
      ApronState apronState1 = map1.get(edge);
      ApronState apronState2 = map2.get(edge);
      if (!apronStateEquals(apronState1, apronState2)) {
        return false;
      }
    }
    return true;
  }

  boolean apronStateEquals(ApronState apronState1, ApronState apronState2) {
    if (apronState1 == apronState2) {
      return true;
    } else if (apronState1 == null || apronState2 == null) {
      return false;
    } else {
      return apronState1.getLessOrEquals(apronState2) &&
          apronState2.getLessOrEquals(apronState1);
    }
  }

  @Override
  public List<LinearConstraint<Integer, LinearVariable>> apply(
      CFAEdge pEntering,
      CFAEdge pLeaving) {
    // ignore the entering edge
    return exitEdgeToConst.get(pLeaving);
  }

  @Override
  public List<LinearConstraint<Integer, LinearVariable>> apply(CFAEdge pEntering) {
    return entryEdgeToConst.get(pEntering);
  }

  public Map<CFAEdge, ApronState> getExitEdgeToApronState() {
    return this.exitEdgeToApron;
  }

  public Map<CFAEdge, ApronState> getEntryEdgeToApronState() {
    return this.entryEdgeToApron;
  }
}
