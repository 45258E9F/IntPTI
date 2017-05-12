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
package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.any;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationWithCheck;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationWithErrorStore;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationWithNarrowingSupport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageTransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public final class CompositeTransferRelation implements TransferRelationWithErrorStore,
                                                        TransferRelationWithNarrowingSupport {

  @Options(prefix = "cpa.composite")
  private static class CompositeTransferRelationOptions {
    @Option(secure = true,
        description = "Split MultiEdges and pass each inner edge to the component CPAs"
            + " to allow strengthen calls after each single edge."
            + " Does not work with backwards analysis!")
    private boolean splitMultiEdges = false;

    @Option(secure = true, description = "Instead of introducing MultiEdges in the CFA"
        + " the Composite CPA can handle all paths in the CFA where MultiEdges could"
        + " be if they were there. This has the big advantage, that we can have"
        + " error locations in the middle of multi edges, which is not possible with"
        + "static MultiEdges.\n Note that while this option is set to true,"
        + " cfa.useMultiEdges has to be set to false.")
    private boolean useDynamicMultiEdges = false;
  }

  private final ImmutableList<TransferRelation> transferRelations;
  private final CFA cfa;
  private final int size;
  private int assumptionIndex = -1;
  private int predicatesIndex = -1;

  private final CompositeTransferRelationOptions transferOptions;

  // for collecting error reports from wrapped CPAs
  private final List<ErrorReport> totalErrorReports;

  public CompositeTransferRelation(
      ImmutableList<TransferRelation> pTransferRelations,
      Configuration pConfig, CFA pCFA) throws InvalidConfigurationException {
    transferOptions = new CompositeTransferRelationOptions();
    pConfig.inject(transferOptions);
    transferRelations = pTransferRelations;
    cfa = pCFA;
    size = pTransferRelations.size();

    // prepare special case handling if both predicates and assumptions are used
    for (int i = 0; i < size; i++) {
      TransferRelation t = pTransferRelations.get(i);
      if (t instanceof PredicateTransferRelation) {
        predicatesIndex = i;
      }
      if (t instanceof AssumptionStorageTransferRelation) {
        assumptionIndex = i;
      }
    }

    // initialize total collection of error reports
    totalErrorReports = Lists.newArrayList();
  }

  @Override
  public Collection<ErrorReport> getStoredErrorReports() {
    return Collections.unmodifiableList(totalErrorReports);
  }

  /**
   * Reset total error reports instantly after finishing one transfer.
   * This method is reserved for ARG CPA only.
   */
  @Override
  public void resetErrorReports() {
    totalErrorReports.clear();
  }

  @Override
  public Collection<CompositeState> getAbstractSuccessors(
      AbstractState element, List<AbstractState> otherStates, Precision precision)
      throws CPATransferException, InterruptedException {
    CompositeState compositeState = (CompositeState) element;
    CompositePrecision compositePrecision = (CompositePrecision) precision;
    Collection<CompositeState> results;

    AbstractStateWithLocations locState =
        extractStateByType(compositeState, AbstractStateWithLocations.class);
    if (locState == null) {
      throw new CPATransferException(
          "Analysis without any CPA tracking locations is not supported, please add one to the configuration (e.g., LocationCPA).");
    }

    results = new ArrayList<>(2);

    for (CFAEdge edge : locState.getOutgoingEdges()) {
      getAbstractSuccessorForEdge(compositeState, compositePrecision, edge, results, false);
    }

    return results;
  }

  @Override
  public Collection<CompositeState> getAbstractSuccessorsForEdge(
      AbstractState element, List<AbstractState> otherStates, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    CompositeState compositeState = (CompositeState) element;
    CompositePrecision compositePrecision = (CompositePrecision) precision;

    Collection<CompositeState> results = new ArrayList<>(1);
    getAbstractSuccessorForEdge(compositeState, compositePrecision, cfaEdge, results, false);

    return results;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsUnderNarrowing(
      AbstractState state, List<AbstractState> otherStates, Precision precision)
      throws CPATransferException, InterruptedException {
    // under narrowing mode, we invoke narrowing version of transfer relation as possible for
    // each CPA component
    CompositeState compositeState = (CompositeState) state;
    CompositePrecision compositePrecision = (CompositePrecision) precision;
    Collection<CompositeState> results;

    AbstractStateWithLocations locState =
        extractStateByType(compositeState, AbstractStateWithLocations.class);
    if (locState == null) {
      throw new CPATransferException(
          "Analysis without any CPA tracking locations is not supported, please add one to the configuration (e.g., LocationCPA).");
    }

    results = new ArrayList<>(2);

    for (CFAEdge edge : locState.getOutgoingEdges()) {
      getAbstractSuccessorForEdge(compositeState, compositePrecision, edge, results, true);
    }

    return results;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdgeUnderNarrowing(
      AbstractState state, List<AbstractState> otherStates, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    CompositeState compositeState = (CompositeState) state;
    CompositePrecision compositePrecision = (CompositePrecision) precision;

    Collection<CompositeState> results = new ArrayList<>(1);
    getAbstractSuccessorForEdge(compositeState, compositePrecision, cfaEdge, results, true);

    return results;
  }

  private void getAbstractSuccessorForEdge(
      CompositeState compositeState, CompositePrecision compositePrecision, CFAEdge cfaEdge,
      Collection<CompositeState> compositeSuccessors, boolean narrowFlag)
      throws CPATransferException, InterruptedException {

    if (transferOptions.useDynamicMultiEdges) {

      assert !(cfaEdge instanceof MultiEdge) : "Static and dynamic MultiEdges may not be mixed.";

      CFANode startNode = cfaEdge.getPredecessor();

      // dynamic multiEdges may be used if the following conditions apply
      if (isValidMultiEdgeStart(startNode)
          && isValidMultiEdgeComponent(cfaEdge)) {

        Collection<CompositeState> currentStates = new ArrayList<>(1);
        currentStates.add(compositeState);
        CFAEdge nextEdge = cfaEdge;

        while (isValidMultiEdgeComponent(nextEdge)) {
          Collection<CompositeState> successorStates = new ArrayList<>(currentStates.size());

          for (CompositeState currentState : currentStates) {
            getAbstractSuccessorForSimpleEdge(currentState, compositePrecision, cfaEdge,
                successorStates, narrowFlag);
          }

          // if we found a target state in the current successors immediately return
          if (from(successorStates).anyMatch(AbstractStates.IS_TARGET_STATE)) {
            compositeSuccessors.addAll(successorStates);
            return;
          }

          // make successor states the new to-be-handled states for the next edge
          currentStates = Collections.unmodifiableCollection(successorStates);

          startNode = cfaEdge.getSuccessor();
          if (startNode.getNumLeavingEdges() == 1) {
            cfaEdge = startNode.getLeavingEdge(0);
          } else {
            break;
          }
        }

        compositeSuccessors.addAll(currentStates);

        // no use for dynamic multi edges right now, just compute the successor
        // for the given edge
      } else {
        getAbstractSuccessorForSimpleEdge(compositeState, compositePrecision, cfaEdge,
            compositeSuccessors, narrowFlag);
      }

    } else if (transferOptions.splitMultiEdges && cfaEdge instanceof MultiEdge) {
      // We want to resolve MultiEdges here such that for every edge along
      // the MultiEdge there is a separate call to TransferRelation.getAbstractSuccessorsForEdge
      // and especially to TransferRelation.strengthen.
      // As there can be multiple successors at each step,
      // we keep a list of "frontier states", handle one edge for each of them,
      // and use the successors of all these states as the new frontier.
      Collection<CompositeState> currentStates = new ArrayList<>(1);
      currentStates.add(compositeState);

      for (CFAEdge simpleEdge : ((MultiEdge) cfaEdge).getEdges()) {
        Collection<CompositeState> successorStates = new ArrayList<>(currentStates.size());

        for (CompositeState currentState : currentStates) {
          getAbstractSuccessorForSimpleEdge(currentState, compositePrecision, simpleEdge,
              successorStates, narrowFlag);
        }

        assert !from(successorStates).anyMatch(AbstractStates.IS_TARGET_STATE);
        // make successor states the new to-be-handled states for the next edge
        currentStates = Collections.unmodifiableCollection(successorStates);
      }

      compositeSuccessors.addAll(currentStates);

    } else {
      getAbstractSuccessorForSimpleEdge(compositeState, compositePrecision, cfaEdge,
          compositeSuccessors, narrowFlag);

      // collect error reports from each component of composite transfer relation
      for (int i = 0; i < size; i++) {
        TransferRelation currentTransfer = transferRelations.get(i);
        if (!(currentTransfer instanceof TransferRelationWithCheck)) {
          continue;
        }
        totalErrorReports.addAll(((TransferRelationWithCheck) currentTransfer).getErrorReports());
        // error reports are temporarily stored in transfer relation
        ((TransferRelationWithCheck) currentTransfer).resetErrorReports();
      }
      if (!totalErrorReports.isEmpty()) {
        // update the error state of successors (states after the transfer)
        // FIX: if the successor is empty, we reset the error reports in order to prevent side
        // effect on other paths
        if (compositeSuccessors.isEmpty()) {
          totalErrorReports.clear();
        } else {
          for (CompositeState successor : compositeSuccessors) {
            successor.updateErrorReports(totalErrorReports);
          }
        }
      }

    }
  }

  private boolean isValidMultiEdgeStart(CFANode node) {
    return node.getNumLeavingEdges() == 1         // linear chain of edges
        && node.getLeavingSummaryEdge() == null   // without a functioncall
        && node.getNumEnteringEdges() > 0;        // without a functionstart
  }

  /**
   * This method checks if the given edge and its successor node are a valid
   * component for a continuing dynamic MultiEdge.
   */
  private boolean isValidMultiEdgeComponent(CFAEdge edge) {
    boolean result = edge.getEdgeType() == CFAEdgeType.BlankEdge
        || edge.getEdgeType() == CFAEdgeType.DeclarationEdge
        || edge.getEdgeType() == CFAEdgeType.StatementEdge
        || edge.getEdgeType() == CFAEdgeType.ReturnStatementEdge;

    CFANode nodeAfterEdge = edge.getSuccessor();

    result = result && nodeAfterEdge.getNumLeavingEdges() == 1
        && nodeAfterEdge.getNumEnteringEdges() == 1
        && nodeAfterEdge.getLeavingSummaryEdge() == null
        && !nodeAfterEdge.isLoopStart()
        && nodeAfterEdge.getClass() == CFANode.class;

    return result && !containsFunctionCall(edge);
  }

  /**
   * This method checks, if the given (statement) edge contains a function call
   * directly or via a function pointer.
   *
   * @param edge the edge to inspect
   * @return whether or not this edge contains a function call or not.
   */
  private boolean containsFunctionCall(CFAEdge edge) {
    if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {
      CStatementEdge statementEdge = (CStatementEdge) edge;

      if ((statementEdge.getStatement() instanceof CFunctionCall)) {
        CFunctionCall call = ((CFunctionCall) statementEdge.getStatement());
        CSimpleDeclaration declaration = call.getFunctionCallExpression().getDeclaration();

        // declaration == null -> functionPointer
        // functionName exists in CFA -> functioncall with CFA for called function
        // otherwise: call of non-existent function, example: nondet_int() -> ignore this case
        return declaration == null || cfa.getAllFunctionNames()
            .contains(declaration.getQualifiedName());
      }
      return (statementEdge.getStatement() instanceof CFunctionCall);
    }
    return false;
  }

  private void getAbstractSuccessorForSimpleEdge(
      CompositeState compositeState, CompositePrecision compositePrecision, CFAEdge cfaEdge,
      Collection<CompositeState> compositeSuccessors, boolean narrowFlag)
      throws CPATransferException, InterruptedException {
    assert cfaEdge != null;

    // FIRST OF ALL, call check-and-refine for expressions
    Collection<List<AbstractState>> allRefinedElements = callCheckAndRefineExpression
        (compositeState, compositePrecision, cfaEdge);

    for (List<AbstractState> lRefinedState : allRefinedElements) {
      // call all the post operators
      Collection<List<AbstractState>> allResultingElements =
          callTransferRelation(lRefinedState, compositePrecision, cfaEdge, narrowFlag);

      // call strengthen for each result
      for (List<AbstractState> lReachedState : allResultingElements) {

        Collection<List<AbstractState>> lResultingElements =
            callStrengthen(lReachedState, compositePrecision, cfaEdge);

        // FINALLY, call check-and-refine for transfer relations with checking
        for (List<AbstractState> lList : lResultingElements) {
          Collection<List<AbstractState>> llResultingElements =
              callCheckAndRefineState(lList, compositePrecision, cfaEdge);

          // create a composite state for each result of strengthen
          for (List<AbstractState> llList : llResultingElements) {
            compositeSuccessors.add(new CompositeState(llList));
          }
        }
      }
    }
  }

  /**
   * check and refine for each component of composite state
   *
   * @param initialState the initial composite state
   * @param pPrecision   the initial composite precision
   * @param pCFAEdge     the CFA edge between initialState and reachedState
   * @return collection of resultant composite states after check-and-refine
   */
  private Collection<List<AbstractState>> callCheckAndRefineExpression(
      CompositeState initialState,
      CompositePrecision pPrecision,
      final CFAEdge pCFAEdge)
      throws CPATransferException, InterruptedException {

    int resultCount = 1;
    List<AbstractState> initialStates = initialState.getWrappedStates();
    checkArgument(initialStates.size() == size, "State with wrong number of components");
    List<Collection<? extends AbstractState>> allComponentSuccessors = new ArrayList<>(size);

    for (int i = 0; i < size; i++) {
      TransferRelation currentTransfer = transferRelations.get(i);
      AbstractState currentInitialState = initialStates.get(i);
      Precision currentPrecision = pPrecision.get(i);
      Collection<? extends AbstractState> refinedComponents;
      if (!(currentTransfer instanceof TransferRelationWithCheck)) {
        refinedComponents = Collections.singletonList(currentInitialState);
      } else {
        refinedComponents = ((TransferRelationWithCheck) currentTransfer).checkAndRefineExpression
            (currentInitialState, initialStates, currentPrecision, pCFAEdge);
        resultCount *= refinedComponents.size();
      }
      allComponentSuccessors.add(refinedComponents);
    }
    return createCartesianProduct(allComponentSuccessors, resultCount);
  }

  private Collection<List<AbstractState>> callCheckAndRefineState(
      final List<AbstractState> componentElements,
      final CompositePrecision compositePrecision,
      final CFAEdge cfaEdge) throws CPATransferException, InterruptedException {

    int resultCount = 1;
    checkArgument(componentElements.size() == size, "State with wrong number of component states "
        + "given");
    List<Collection<? extends AbstractState>> allComponentsSuccessors = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      TransferRelation currentTransfer = transferRelations.get(i);
      AbstractState currentInitialState = componentElements.get(i);
      Precision currentPrecision = compositePrecision.get(i);
      Collection<? extends AbstractState> refinedComponents;
      if (!(currentTransfer instanceof TransferRelationWithCheck)) {
        refinedComponents = Collections.singletonList(currentInitialState);
      } else {
        refinedComponents = ((TransferRelationWithCheck) currentTransfer).checkAndRefineState
            (currentInitialState, componentElements, currentPrecision, cfaEdge);
        resultCount *= refinedComponents.size();
      }
      allComponentsSuccessors.add(refinedComponents);
    }
    return createCartesianProduct(allComponentsSuccessors, resultCount);
  }


  private Collection<List<AbstractState>> callTransferRelation(
      final List<AbstractState> componentElements,
      final CompositePrecision compositePrecision, final CFAEdge cfaEdge, boolean narrowFlag)
      throws CPATransferException, InterruptedException {
    int resultCount = 1;
    checkArgument(componentElements.size() == size,
        "State with wrong number of component states given");
    List<Collection<? extends AbstractState>> allComponentsSuccessors = new ArrayList<>(size);

    for (int i = 0; i < size; i++) {
      TransferRelation lCurrentTransfer = transferRelations.get(i);
      AbstractState lCurrentElement = componentElements.get(i);
      Precision lCurrentPrecision = compositePrecision.get(i);

      Collection<? extends AbstractState> componentSuccessors;
      if (narrowFlag && lCurrentTransfer instanceof TransferRelationWithNarrowingSupport) {
        componentSuccessors = ((TransferRelationWithNarrowingSupport) lCurrentTransfer)
            .getAbstractSuccessorsForEdgeUnderNarrowing(lCurrentElement, componentElements,
                lCurrentPrecision, cfaEdge);
      } else {
        componentSuccessors = lCurrentTransfer.getAbstractSuccessorsForEdge(
            lCurrentElement, componentElements, lCurrentPrecision, cfaEdge);
      }
      resultCount *= componentSuccessors.size();

      if (resultCount == 0) {
        // shortcut
        break;
      }

      allComponentsSuccessors.add(componentSuccessors);
    }

    // create cartesian product of all elements we got
    return createCartesianProduct(allComponentsSuccessors, resultCount);
  }

  private Collection<List<AbstractState>> callStrengthen(
      final List<AbstractState> reachedState,
      final CompositePrecision compositePrecision, final CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    List<Collection<? extends AbstractState>> lStrengthenResults = new ArrayList<>(size);
    int resultCount = 1;

    for (int i = 0; i < size; i++) {

      TransferRelation lCurrentTransfer = transferRelations.get(i);
      AbstractState lCurrentElement = reachedState.get(i);
      Precision lCurrentPrecision = compositePrecision.get(i);

      Collection<? extends AbstractState> lResultsList =
          lCurrentTransfer.strengthen(lCurrentElement, reachedState, cfaEdge, lCurrentPrecision);

      if (lResultsList == null) {
        lStrengthenResults.add(Collections.singleton(lCurrentElement));
      } else {
        resultCount *= lResultsList.size();

        if (resultCount == 0) {
          // shortcut
          break;
        }

        lStrengthenResults.add(lResultsList);
      }
    }

    // special case handling if we have predicate and assumption cpas
    // TODO remove as soon as we call strengthen in a fixpoint loop
    if (predicatesIndex >= 0 && assumptionIndex >= 0 && resultCount > 0) {
      AbstractState predElement = Iterables.getOnlyElement(lStrengthenResults.get(predicatesIndex));
      AbstractState assumptionElement =
          Iterables.getOnlyElement(lStrengthenResults.get(assumptionIndex));
      Precision predPrecision = compositePrecision.get(predicatesIndex);
      TransferRelation predTransfer = transferRelations.get(predicatesIndex);

      Collection<? extends AbstractState> predResult = predTransfer
          .strengthen(predElement, Collections.singletonList(assumptionElement), cfaEdge,
              predPrecision);
      resultCount *= predResult.size();

      lStrengthenResults.set(predicatesIndex, predResult);
    }

    // create cartesian product
    Collection<List<AbstractState>> strengthenedStates =
        createCartesianProduct(lStrengthenResults, resultCount);

    // If state was not a target state before but a target state was found during strengthening,
    // we call strengthen again such that the other CPAs can act on this information.
    // Note that this terminates because in the inner call the input state
    // is already a target state and this branch won't be taken.
    // TODO Generalize this into a full fixpoint algorithm.
    if (!any(reachedState, IS_TARGET_STATE)) {
      Collection<List<AbstractState>> newStrengthenedStates = new ArrayList<>(resultCount);

      for (List<AbstractState> strengthenedState : strengthenedStates) {
        if (any(strengthenedState, IS_TARGET_STATE)) {
          newStrengthenedStates
              .addAll(callStrengthen(strengthenedState, compositePrecision, cfaEdge));
        } else {
          newStrengthenedStates.add(strengthenedState);
        }
      }
      return newStrengthenedStates;

    } else {
      return strengthenedStates;
    }
  }

  static Collection<List<AbstractState>> createCartesianProduct(
      List<Collection<? extends AbstractState>> allComponentsSuccessors, int resultCount) {
    Collection<List<AbstractState>> allResultingElements;
    switch (resultCount) {
      case 0:
        // at least one CPA decided that there is no successor
        allResultingElements = Collections.emptySet();
        break;

      case 1:
        List<AbstractState> resultingElements = new ArrayList<>(allComponentsSuccessors.size());
        for (Collection<? extends AbstractState> componentSuccessors : allComponentsSuccessors) {
          resultingElements.add(Iterables.getOnlyElement(componentSuccessors));
        }
        allResultingElements = Collections.singleton(resultingElements);
        break;

      default:
        // create cartesian product of all componentSuccessors and store the result in allResultingElements
        List<AbstractState> initialPrefix = Collections.emptyList();
        allResultingElements = new ArrayList<>(resultCount);
        createCartesianProduct0(allComponentsSuccessors, initialPrefix, allResultingElements);
    }

    assert resultCount == allResultingElements.size();
    return allResultingElements;
  }

  private static void createCartesianProduct0(
      List<Collection<? extends AbstractState>> allComponentsSuccessors,
      List<AbstractState> prefix, Collection<List<AbstractState>> allResultingElements) {

    if (prefix.size() == allComponentsSuccessors.size()) {
      allResultingElements.add(prefix);

    } else {
      int depth = prefix.size();
      Collection<? extends AbstractState> myComponentsSuccessors =
          allComponentsSuccessors.get(depth);

      for (AbstractState currentComponent : myComponentsSuccessors) {
        List<AbstractState> newPrefix = new ArrayList<>(prefix);
        newPrefix.add(currentComponent);

        createCartesianProduct0(allComponentsSuccessors, newPrefix, allResultingElements);
      }
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState element,
      List<AbstractState> otherElements, CFAEdge cfaEdge,
      Precision precision) {
    // strengthen is only called by the composite CPA on its component CPAs
    return null;
  }

  boolean areAbstractSuccessors(
      AbstractState pElement, List<AbstractState> otherStates, CFAEdge
      pCfaEdge, Collection<? extends AbstractState> pSuccessors, List<ConfigurableProgramAnalysis>
          cpas) throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(pCfaEdge);

    CompositeState compositeState = (CompositeState) pElement;

    int resultCount = 1;
    boolean result = true;
    for (int i = 0; i < size; ++i) {
      Set<AbstractState> componentSuccessors = new HashSet<>();
      for (AbstractState successor : pSuccessors) {
        CompositeState compositeSuccessor = (CompositeState) successor;
        if (compositeSuccessor.getNumberOfStates() != size) {
          return false;
        }
        componentSuccessors.add(compositeSuccessor.get(i));
      }
      resultCount *= componentSuccessors.size();
      ProofChecker componentProofChecker = (ProofChecker) cpas.get(i);
      if (!componentProofChecker.areAbstractSuccessors(compositeState.get(i), otherStates, pCfaEdge,
          componentSuccessors)) {
        result =
            false; //if there are no successors it might be still ok if one of the other components is fine with the empty set
      } else {
        if (componentSuccessors.isEmpty()) {
          assert pSuccessors.isEmpty();
          return true; //another component is indeed fine with the empty set as set of successors; transition is ok
        }
      }
    }

    if (resultCount > pSuccessors.size()) {
      return false;
    }

    HashSet<List<AbstractState>> states = new HashSet<>();
    for (AbstractState successor : pSuccessors) {
      states.add(((CompositeState) successor).getWrappedStates());
    }
    return resultCount == states.size() && result;

  }
}
