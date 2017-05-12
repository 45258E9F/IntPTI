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
package org.sosy_lab.cpachecker.cpa.bam;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMCEXSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

public class ARGSubtreeRemover {

  private final BlockPartitioning partitioning;
  private final BAMDataManager data;
  private final Reducer wrappedReducer;
  private final BAMCache bamCache;
  private final LogManager logger;
  private final Timer removeCachedSubtreeTimer;
  private final boolean doPrecisionRefinementForAllStates;

  public ARGSubtreeRemover(BAMCPA bamCpa, Timer pRemoveCachedSubtreeTimer) {
    this.partitioning = bamCpa.getBlockPartitioning();
    this.data = bamCpa.getData();
    this.wrappedReducer = bamCpa.getReducer();
    this.bamCache = bamCpa.getData().bamCache;
    this.logger = bamCpa.getData().logger;
    this.removeCachedSubtreeTimer = pRemoveCachedSubtreeTimer;
    doPrecisionRefinementForAllStates = bamCpa.doPrecisionRefinementForAllStates();
  }

  void removeSubtree(
      ARGReachedSet mainReachedSet, ARGPath pPath,
      ARGState element, List<Precision> pNewPrecisions,
      List<Predicate<? super Precision>> pNewPrecisionTypes) {

    final ARGState firstState = (ARGState) mainReachedSet.asReachedSet().getFirstState();
    final ARGState lastState = (ARGState) mainReachedSet.asReachedSet().getLastState();

    assert pPath.asStatesList().get(0).getWrappedState() == firstState
        : "path should start with root state";
    assert Iterables.getLast(pPath.asStatesList()).getWrappedState() == lastState
        : "path should end with target state";
    assert lastState.isTarget();

    final List<ARGState> relevantCallStates = getRelevantCallStates(pPath.asStatesList(), element);
    assert relevantCallStates.get(0).getWrappedState() == firstState : "root should be relevant";
    assert relevantCallStates.size() >= 1
        : "at least the main-function should be open at the target-state";

    Multimap<ARGState, ARGState> neededRemoveCachedSubtreeCalls = LinkedHashMultimap.create();

    //iterate from root to element and remove all subtrees for subgraph calls
    for (int i = 0; i < relevantCallStates.size() - 1; i++) { // ignore root and the last element
      final ARGState pathElement = relevantCallStates.get(i);
      final ARGState nextElement = relevantCallStates.get(i + 1);
      neededRemoveCachedSubtreeCalls.put(
          getReachedState(pathElement),
          getReachedState(nextElement));
    }

    if (bamCache.doesAggressiveCaching()) {
      ensureExactCacheHitsOnPath(pPath, element, pNewPrecisions, neededRemoveCachedSubtreeCalls);
    }

    for (final Entry<ARGState, ARGState> removeCachedSubtreeArguments : neededRemoveCachedSubtreeCalls
        .entries()) {
      assert data.initialStateToReachedSet.get(removeCachedSubtreeArguments.getKey())
          .contains(removeCachedSubtreeArguments.getValue());
    }

    final ARGState lastRelevantNode = getReachedState(Iterables.getLast(relevantCallStates));
    final ARGState target = getReachedState(element);
    for (final Entry<ARGState, ARGState> removeCachedSubtreeArguments : neededRemoveCachedSubtreeCalls
        .entries()) {
      ReachedSet nextReachedSet =
          data.initialStateToReachedSet.get(removeCachedSubtreeArguments.getValue());
      final List<Precision> newPrecisions;
      final List<Predicate<? super Precision>> newPrecisionTypes;
      if (doPrecisionRefinementForAllStates
          // special option (mostly for testing)
          || (removeCachedSubtreeArguments.getValue() == lastRelevantNode)
          // last iteration, most inner block for refinement
          || (nextReachedSet != null && target.getParents()
          .contains(nextReachedSet.getFirstState()))) {
        // if second state of reached-set is removed, update parent-precision
        newPrecisions = pNewPrecisions;
        newPrecisionTypes = pNewPrecisionTypes;
      } else {
        newPrecisions = Collections.emptyList(); // no update of precision needed
        newPrecisionTypes = Collections.emptyList();
      }
      removeCachedSubtree(removeCachedSubtreeArguments.getKey(),
          removeCachedSubtreeArguments.getValue(), newPrecisions, newPrecisionTypes);
    }

    removeCachedSubtree(getReachedState(Iterables.getLast(relevantCallStates)),
        getReachedState(element), pNewPrecisions, pNewPrecisionTypes);

    // the main-reachedset contains only the root, exit-states and targets.
    // we assume, that the current refinement was caused by a target-state.
    mainReachedSet.removeSubtree(lastState);
  }

  private ARGState getReachedState(ARGState state) {
    return data.getMostInnerState(((BackwardARGState) state).getARGState());
  }

  /**
   * just remove a state and its subtree from the given reachedSet.
   */
  static void removeSubtree(ARGReachedSet reachedSet, ARGState argElement) {
    if (BAMTransferRelation.isHeadOfMainFunction(extractLocation(argElement))) {
      reachedSet.removeSubtree((ARGState) reachedSet.asReachedSet().getLastState());
    } else {
      reachedSet.removeSubtree(argElement);
    }
  }

  private void removeCachedSubtree(
      ARGState rootState, ARGState removeElement,
      List<Precision> pNewPrecisions,
      List<Predicate<? super Precision>> pPrecisionTypes) {
    assert pNewPrecisions.size() == pPrecisionTypes.size();
    removeCachedSubtreeTimer.start();

    try {

      logger.log(Level.FINER, "Remove cached subtree for", removeElement, " issued with precision",
          pNewPrecisions);

      if (removeElement.isDestroyed()) {
        logger.log(Level.FINER, "state was destroyed before");
        //apparently, removeElement was removed due to prior deletions
        return;
      }

      CFANode rootNode = extractLocation(rootState);
      Block rootSubtree = partitioning.getBlockForCallNode(rootNode);
      ReachedSet reachedSet = data.initialStateToReachedSet.get(rootState);
      assert reachedSet.contains(removeElement) : "removing state from wrong reachedSet: "
          + removeElement;
      assert !removeElement.getParents().isEmpty();

      Precision newPrecision = null;
      Predicate<? super Precision> newPrecisionType = null;
      if (!pNewPrecisions.isEmpty()) {
        final Pair<Precision, Predicate<? super Precision>> p = getUpdatedPrecision(
            reachedSet.getPrecision(removeElement), rootSubtree, pNewPrecisions, pPrecisionTypes);
        newPrecision = p.getFirst();
        newPrecisionType = p.getSecond();
      }

      AbstractState reducedRootState =
          wrappedReducer.getVariableReducedState(rootState, rootSubtree, rootNode);
      Precision reducedRootPrecision = reachedSet.getPrecision(reachedSet.getFirstState());
      bamCache.removeReturnEntry(reducedRootState, reducedRootPrecision, rootSubtree);
      bamCache.removeBlockEntry(reducedRootState, reducedRootPrecision, rootSubtree);

      logger.log(Level.FINEST,
          "Removing subtree, adding a new cached entry, and removing the former cached entries");

      boolean updateCacheNeeded = removeElement.getParents().contains(reachedSet.getFirstState());

      ARGReachedSet argReachedSet = new ARGReachedSet(reachedSet);
      if (newPrecision == null) {
        removeSubtree(argReachedSet, removeElement);
      } else {
        argReachedSet.removeSubtree(removeElement, newPrecision, newPrecisionType);
      }

      if (updateCacheNeeded && newPrecision != null) {
        logger.log(Level.FINER, "updating cache");
        bamCache.updatePrecisionForEntry(reducedRootState, reducedRootPrecision, rootSubtree,
            newPrecision);
      }

    } finally {
      removeCachedSubtreeTimer.stop();
    }
  }

  /**
   * This method builds the updated precision for the refinement. Normally, a list of sub-precisions
   * would be used and the reached-set updates it in its own. For BAM we build the correct
   * 'complete' precision, because we have to reduce it for the current block. Thus instead of a
   * list, we only have one top-level precision-object that wraps other updated precisions.
   */
  private Pair<Precision, Predicate<? super Precision>> getUpdatedPrecision(
      Precision removePrecision, final Block context,
      final List<Precision> precisions, final List<Predicate<? super Precision>> precisionTypes) {
    assert precisions.size() == precisionTypes.size() && !precisions.isEmpty();

    for (int i = 0; i < precisions.size(); i++) {
      removePrecision =
          Precisions.replaceByType(removePrecision, precisions.get(i), precisionTypes.get(i));
    }

    final Precision reducedPrecision =
        wrappedReducer.getVariableReducedPrecision(removePrecision, context);

    return Pair.<Precision, Predicate<? super Precision>>of(
        reducedPrecision, Predicates.instanceOf(reducedPrecision.getClass()));
  }

  /**
   * returns only those states, where a block starts that is 'open' at the cutState.
   */
  private List<ARGState> getRelevantCallStates(List<ARGState> path, ARGState bamCutState) {
    final Deque<ARGState> openCallStates = new ArrayDeque<>();
    for (final ARGState bamState : path) {

      final ARGState state = ((BackwardARGState) bamState).getARGState();

      // ASSUMPTION: there can be several block-exits at once per location, but only one block-entry per location.

      // we use a loop here, because a return-node can be the exit of several blocks at once.
      ARGState tmp = state;
      while (data.expandedStateToReducedState.containsKey(tmp) && bamCutState != bamState) {
        assert partitioning.isReturnNode(extractLocation(tmp))
            : "the mapping of expanded to reduced state should only exist for block-return-locations";
        // we are leaving a block, remove the start-state from the stack.
        tmp = (ARGState) data.expandedStateToReducedState.get(tmp);
        openCallStates.removeLast();
        // INFO:
        // if we leave several blocks at once, we leave the blocks in reverse order,
        // because the call-state of the most outer block is checked first.
        // We ignore this here, because we just need the 'number' of block-exits.
      }

      if (data.initialStateToReachedSet.containsKey(state)) {
        assert partitioning.isCallNode(extractLocation(state))
            : "the mapping of initial state to reached-set should only exist for block-start-locations";
        // we start a new sub-reached-set, add state as start-state of a (possibly) open block.
        // if we are at lastState, we do not want to enter the block
        openCallStates.addLast(bamState);
      }

      if (bamCutState == bamState) {
        // TODO:
        // current solution: when we found the cutState, we only enter new blocks, but never leave one.
        // maybe better solution: do not enter or leave a block, when we found the cutState.
        break;
      }
    }

    return new ArrayList<>(openCallStates);
  }

  /**
   * there might be some "imprecise" cache entries used along the path.
   * We remove all of them and create the "precise" entry for re-exploration.
   * We only update those blocks, where a nested block is imprecise.
   */
  private void ensureExactCacheHitsOnPath(
      ARGPath pPath, final ARGState pElement,
      List<Precision> pNewPrecisions, Multimap<ARGState, ARGState> neededRemoveCachedSubtreeCalls) {
    boolean cutStateFound = false;
    final Deque<Boolean> needsNewPrecisionEntries = new ArrayDeque<>();
    final Deque<Boolean> foundInnerUnpreciseEntries = new ArrayDeque<>();
    final Deque<ARGState> rootStates = new ArrayDeque<>();

    // add root from main-reached-set
    needsNewPrecisionEntries.add(false);
    foundInnerUnpreciseEntries.add(false);
    rootStates.add(pPath.getFirstState());

    for (ARGState bamState : pPath.asStatesList()) {
      assert needsNewPrecisionEntries.size() == foundInnerUnpreciseEntries.size();
      assert needsNewPrecisionEntries.size() == rootStates.size();

      if (bamState == pElement) {
        cutStateFound = true;
      }

      ARGState state = ((BackwardARGState) bamState).getARGState();

      for (AbstractState tmp : data.getExpandedStatesList(state)) {
        boolean isNewPrecisionEntry = needsNewPrecisionEntries.removeLast();
        boolean isNewPrecisionEntryForOuterBlock = needsNewPrecisionEntries.getLast();
        boolean removedUnpreciseInnerBlock = foundInnerUnpreciseEntries.removeLast();
        boolean foundInnerUnpreciseEntry = foundInnerUnpreciseEntries.getLast();

        ARGState rootState = rootStates.removeLast();
        if ((removedUnpreciseInnerBlock || isNewPrecisionEntry) && !isNewPrecisionEntryForOuterBlock
            && !foundInnerUnpreciseEntry) {

          if (cutStateFound) {
            // we indeed found an inner block that was imprecise,
            // if we are in a reached set that already uses the new precision and this is the first such entry
            // we have to remove the subtree starting from currentElement in the rootReachedSet
            neededRemoveCachedSubtreeCalls.put(getReachedState(rootState), (ARGState) tmp);
          }

          assert data.initialStateToReachedSet.get(getReachedState(rootState)).contains(tmp)
              : "reachedset for initial state " + getReachedState(rootState)
              + " does not contain state " + tmp;

          // replace last
          foundInnerUnpreciseEntries.removeLast();
          foundInnerUnpreciseEntries.addLast(true);
        }
      }

      if (data.initialStateToReachedSet.containsKey(state)) {
        // before reaching the cutstate, we assume that all cache-entries are sufficient
        ReachedSet openReachedSet =
            data.initialStateToReachedSet.get(rootStates.getLast().getWrappedState());
        boolean preciseEntry =
            !cutStateFound || createNewPreciseEntry(bamState, pNewPrecisions, openReachedSet);
        needsNewPrecisionEntries.addLast(preciseEntry);
        foundInnerUnpreciseEntries.addLast(false);
        rootStates.addLast(bamState);
      }
    }

    // now only the initial elements should be on the stacks
    assert needsNewPrecisionEntries.getLast() == false;
    assert rootStates.getLast() == pPath.getFirstState();
  }

  /**
   * This method creates a new precise entry if necessary, and returns whether the used entry needs
   * a new precision.
   */
  private boolean createNewPreciseEntry(
      ARGState rootState, List<Precision> pNewPrecisions,
      UnmodifiableReachedSet outerReachedSet) {

    // create updated precision
    ARGState initialState = (ARGState) rootState.getWrappedState();
    Precision rootPrecision = outerReachedSet.getPrecision(initialState);
    for (Precision pNewPrecision : pNewPrecisions) {
      rootPrecision = Precisions.replaceByType(rootPrecision, pNewPrecision,
          Predicates.instanceOf(pNewPrecision.getClass()));
    }

    // reduce the new precision and add a precise key for the new precision if needed
    CFANode node = extractLocation(rootState);
    Block context = partitioning.getBlockForCallNode(node);
    AbstractState reducedRootState =
        wrappedReducer.getVariableReducedState(getReachedState(rootState), context, node);
    Precision reducedNewPrecision =
        wrappedReducer.getVariableReducedPrecision(rootPrecision, context);
    if (!bamCache.containsPreciseKey(reducedRootState, reducedNewPrecision, context)) {
      ReachedSet reachedSet = data.createInitialReachedSet(reducedRootState, reducedNewPrecision);
      bamCache.put(reducedRootState, reducedNewPrecision, context, reachedSet);
    }

    // check if the used precision is equal to the new precision
    UnmodifiableReachedSet innerReachedSet = data.initialStateToReachedSet.get(initialState);
    Precision usedPrecision = innerReachedSet.getPrecision(innerReachedSet.getFirstState());
    boolean isNewPrecisionEntry = !usedPrecision.equals(reducedNewPrecision);
    return isNewPrecisionEntry;
  }
}
