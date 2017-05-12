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
package org.sosy_lab.cpachecker.cpa.coverage;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.coverage.CoverageData.CoverageMode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CoverageTransferRelation extends SingleEdgeTransferRelation {

  private final CoverageData cov;

  public CoverageTransferRelation(CFA pCFA, CoverageData pCov) {
    cov = Preconditions.checkNotNull(pCov);
    Preconditions.checkArgument(cov.getCoverageMode() == CoverageMode.TRANSFER);

    // ------------ Existing lines ----------------
    for (CFANode node : pCFA.getAllNodes()) {
      // This part adds lines, which are only on edges, such as "return" or "goto"
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        if (edge instanceof MultiEdge) {
          for (CFAEdge innerEdge : ((MultiEdge) edge).getEdges()) {
            cov.handleEdgeCoverage(innerEdge, false);
          }

        } else {
          cov.handleEdgeCoverage(edge, false);
        }
      }
    }

    // ------------ Existing functions -------------
    for (FunctionEntryNode entryNode : pCFA.getAllFunctionHeads()) {
      cov.putExistingFunction(entryNode);
    }

  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement,
      List<AbstractState> otherStates,
      Precision pPrecision,
      CFAEdge pCfaEdge)
      throws CPATransferException {

    if (pCfaEdge instanceof MultiEdge) {
      for (CFAEdge innerEdge : ((MultiEdge) pCfaEdge).getEdges()) {
        handleNonMultiEdge(innerEdge);
      }

    } else {
      handleNonMultiEdge(pCfaEdge);
    }

    return Collections.singleton(pElement);
  }

  private void handleNonMultiEdge(CFAEdge pEdge) {

    cov.handleEdgeCoverage(pEdge, true);

    if (pEdge.getPredecessor() instanceof FunctionEntryNode) {
      cov.addVisitedFunction((FunctionEntryNode) pEdge.getPredecessor());
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {

    return null;
  }

}
