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
package org.sosy_lab.cpachecker.core.phase.util;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.phase.CPAPhase;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;
import org.sosy_lab.cpachecker.util.misc.GuavaExt;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SequentialExecStrategy implements CPAPhaseExecStrategy {

  /**
   * A simple check-and-execute loop
   * When a phase is finished, its direct successors will likely to be executed then
   */
  public CPAPhaseStatus execSimple(List<CPAPhase> phases) throws Exception {
    // number of unfinished ancestors
    final Map<CPAPhase, Integer> blocking = Maps.newHashMap();
    for (CPAPhase p : phases) {
      blocking.put(p, 0); // init to 0
      for (CPAPhase q : p.getAncestors()) {
        if (!q.isProcessed()) {
          blocking.put(p, blocking.get(p) + 1);
        }
      }
    }
    CPAPhaseStatus status = CPAPhaseStatus.SUCCESS;
    Set<CPAPhase> remains = Sets.newHashSet(phases);
    Map<CPAPhase, Integer> order = Maps.newHashMap();     // the order when this phase is discovered
    while (status == CPAPhaseStatus.SUCCESS && !remains.isEmpty()) {
      // select unfinished phase with 0 unfinished ancestor
      FluentIterable<CPAPhase> candidates =
          FluentIterable.from(remains).filter(new Predicate<CPAPhase>() {
            @Override
            public boolean apply(CPAPhase p) {
              return blocking.get(p) == 0;
            }
          });
      if (candidates.size() == 0) {
        throw new InvalidConfigurationException("Invalid phase topology: no entry");
      }
      // find the one with minimal order (defaults to phase + 1)
      Function<CPAPhase, Integer> f = Functions.forMap(order, phases.size());
      CPAPhase current = GuavaExt.min(candidates, f);
      // run it
      if (!current.isProcessed()) {
        try {
          status = CPAPhaseStatus.mergeResult(status, current.run());
        } catch (Exception e) {
          status = CPAPhaseStatus.FAIL;
        }
      }
      // update state
      if (status == CPAPhaseStatus.SUCCESS) {
        remains.remove(current);
        for (CPAPhase q : current.getSuccessors()) {
          if (!order.containsKey(q)) {
            order.put(q, order.size());
          }
          blocking.put(q, blocking.get(q) - 1);
        }
      }
    }
    return status;
  }

  @Override
  public CPAPhaseStatus exec(List<CPAPhase> pPhase) throws Exception {
    // return execSimple(pPhase);
    return execRec(pPhase);
  }

  public CPAPhaseStatus execRec(List<CPAPhase> pPhase) throws Exception {
    /*
     * The sequential scheme works as DFS traverse of CPA phase graph.
     */
    CPAPhaseStatus status = CPAPhaseStatus.SUCCESS;
    if (pPhase.size() == 0) {
      return status;
    }
    CPAPhase entry = null;
    // try to find a start point
    for (CPAPhase phase : pPhase) {
      if (phase.checkReady()) {
        entry = phase;
        break;
      }
    }
    if (entry == null) {
      throw new InvalidConfigurationException("Invalid phase topology: no entry");
    }
    return handlePhase(entry, status);
  }

  private CPAPhaseStatus handlePhase(CPAPhase phase, CPAPhaseStatus prevStatus) throws Exception {
    CPAPhaseStatus status = prevStatus;
    status = CPAPhaseStatus.mergeResult(status, phase.run());
    if (status != CPAPhaseStatus.SUCCESS) {
      return status;
    }
    List<CPAPhase> successors = phase.getSuccessors();
    for (CPAPhase successor : successors) {
      if (successor.checkReady()) {
        status = CPAPhaseStatus.mergeResult(status, handlePhase(successor, status));
      } else {
        // then we need to find an entry of its ancestors
        List<CPAPhase> ancestors = successor.getAncestors();
        for (CPAPhase ancestor : ancestors) {
          status = CPAPhaseStatus.mergeResult(status, handleAncestor(ancestor, status));
        }
      }
    }
    return status;
  }

  private CPAPhaseStatus handleAncestor(CPAPhase phase, CPAPhaseStatus prevStatus)
      throws Exception {
    CPAPhaseStatus status = prevStatus;
    if (phase.isProcessed()) {
      return status;
    }
    if (phase.checkReady()) {
      return CPAPhaseStatus.mergeResult(status, handlePhase(phase, status));
    }
    // otherwise, this phase is still unready
    List<CPAPhase> ancestors = phase.getAncestors();
    for (CPAPhase ancestor : ancestors) {
      status = CPAPhaseStatus.mergeResult(status, handleAncestor(ancestor, status));
    }
    return status;
  }

  @Override
  public String getName() {
    return "Sequential Execution";
  }

}
