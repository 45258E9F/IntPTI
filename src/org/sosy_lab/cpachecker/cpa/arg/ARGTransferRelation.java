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
package org.sosy_lab.cpachecker.cpa.arg;

import static org.sosy_lab.cpachecker.util.AbstractStates.getOutgoingEdges;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationWithErrorStore;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationWithNarrowingSupport;
import org.sosy_lab.cpachecker.core.interfaces.checker.CheckerWithInstantErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReportWithTrace;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Options
public class ARGTransferRelation implements TransferRelationWithNarrowingSupport {

  private final TransferRelation transferRelation;

  private static TraceOptions traceOptions;

  public ARGTransferRelation(TransferRelation tr, Configuration config)
      throws InvalidConfigurationException {
    transferRelation = tr;
    traceOptions = new TraceOptions();
    config.inject(traceOptions);
    // reset the path counter once we create a new instance of ARG transfer relation
    ARGPathCounter.reset();
  }

  public static long getMaxNumOfPath() {
    return traceOptions.getMaxNumOfPath();
  }

  @Override
  public Collection<ARGState> getAbstractSuccessors(
      AbstractState pElement, List<AbstractState> otherStates, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    ARGState element = (ARGState) pElement;
    return getAbstractSuccessors0(element, otherStates, pPrecision, false);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState,
      List<AbstractState> otherStates,
      Precision pPrecision,
      CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException(
        "ARGCPA needs to be used as the outer-most CPA,"
            + " thus it does not support returning successors for a single edge.");
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsUnderNarrowing(
      AbstractState state, List<AbstractState> otherStates, Precision precision)
      throws CPATransferException, InterruptedException {
    ARGState element = (ARGState) state;
    return getAbstractSuccessors0(element, otherStates, precision, true);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdgeUnderNarrowing(
      AbstractState state, List<AbstractState> otherStates, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException("ARGCPA works as the outermost CPA, thus transfer for"
        + " edge is not supported");
  }

  private Collection<ARGState> getAbstractSuccessors0(
      ARGState state, List<AbstractState>
      otherStates, Precision precision, boolean narrowFlag)
      throws CPATransferException, InterruptedException {
    // covered elements may be in the reached set, but should always be ignored
    if (state.isCovered()) {
      return Collections.emptySet();
    }

    state.markExpanded();

    AbstractState wrappedState = state.getWrappedState();
    Collection<? extends AbstractState> successors;
    if (narrowFlag && transferRelation instanceof TransferRelationWithNarrowingSupport) {
      successors = ((TransferRelationWithNarrowingSupport) transferRelation)
          .getAbstractSuccessorsUnderNarrowing(wrappedState, otherStates, precision);
    } else {
      successors = transferRelation.getAbstractSuccessors(wrappedState, otherStates, precision);
    }
    if (successors.isEmpty()) {
      return Collections.emptySet();
    }

    List<ARGState> wrappedSuccessors = new ArrayList<>();
    for (AbstractState absElement : successors) {
      ARGState successorElem = new ARGState(absElement, state);
      wrappedSuccessors.add(successorElem);
    }

    handleErrorReports(wrappedSuccessors);

    // Check if the number of paths has been exceeded the upper bound.
    int numOfSuccessors = wrappedSuccessors.size();
    List<Integer> indexList = ARGPathCounter.getTrimmedIndex(numOfSuccessors,
        traceOptions.getMaxNumOfPath());
    if (indexList != null) {
      Collection<ARGState> trimmedSuccessors = new ArrayList<>();
      for (Integer index : indexList) {
        trimmedSuccessors.add(wrappedSuccessors.get(index));
      }
      return trimmedSuccessors;
    }

    return wrappedSuccessors;
  }

  private void handleErrorReports(Collection<ARGState> argStates) {
    if (!(transferRelation instanceof TransferRelationWithErrorStore)) {
      // nothing to do
      return;
    }
    Collection<ErrorReport> errors = ((TransferRelationWithErrorStore) transferRelation)
        .getStoredErrorReports();
    for (ARGState argState : argStates) {
      for (ErrorReport singleError : errors) {
        if (!(singleError instanceof ErrorReportWithTrace)) {
          GlobalInfo.getInstance().updateErrorCollector(singleError);
          continue;
        }
        // otherwise, we compute a trace for this error.
        // Trace info. helps developer to diagnosis the cause of error
        ErrorReportWithTrace singleTracedError = (ErrorReportWithTrace) singleError;
        CheckerWithInstantErrorReport checker = singleTracedError.getChecker();
        ARGPath errorPath = ARGUtils.getOnePathTo(argState, traceOptions.getMaximumPathDepth());
        List<ARGState> criticalStates = checker.getInverseCriticalStates(errorPath,
            singleTracedError.getErrorSpot());
        singleTracedError.updateCriticalStates(criticalStates);
        singleTracedError.updateErrorTrace(errorPath);
        GlobalInfo.getInstance().updateErrorCollector(singleTracedError);
      }
    }

    // don't forget to clear stored error reports
    ((TransferRelationWithErrorStore) transferRelation).resetErrorReports();
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState element,
      List<AbstractState> otherElements, CFAEdge cfaEdge,
      Precision precision) {
    return null;
  }

  boolean areAbstractSuccessors(
      AbstractState pElement, List<AbstractState> otherStates, CFAEdge
      pCfaEdge, Collection<? extends AbstractState> pSuccessors, ProofChecker wrappedProofChecker)
      throws CPATransferException, InterruptedException {
    ARGState element = (ARGState) pElement;

    assert Iterables.elementsEqual(element.getChildren(), pSuccessors);

    AbstractState wrappedState = element.getWrappedState();
    Multimap<CFAEdge, AbstractState> wrappedSuccessors = HashMultimap.create();
    for (AbstractState absElement : pSuccessors) {
      ARGState successorElem = (ARGState) absElement;
      wrappedSuccessors.put(element.getEdgeToChild(successorElem), successorElem.getWrappedState());
    }

    if (pCfaEdge != null) {
      return wrappedProofChecker.areAbstractSuccessors(wrappedState, otherStates, pCfaEdge,
          wrappedSuccessors.get(pCfaEdge));
    }

    for (CFAEdge edge : getOutgoingEdges(element)) {
      if (!wrappedProofChecker.areAbstractSuccessors(wrappedState, otherStates, edge,
          wrappedSuccessors.get(edge))) {
        return false;
      }
    }
    return true;
  }
}
