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
package org.sosy_lab.cpachecker.core.algorithm.summary;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Table;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationWithNarrowingSupport;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

@Options(prefix = "summary.narrow")
public class NarrowingRefinementAlgorithm implements Algorithm {

  @Option(secure = true, description = "If a location has been narrowed for more than the given "
      + "threshold, the narrowing operation saturates.")
  private int maxNarrows = 1;

  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;

  private static CFA cfa;

  public NarrowingRefinementAlgorithm(Configuration pConfig, ConfigurableProgramAnalysis pCPA)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    if (pCPA == null) {
      throw new InvalidConfigurationException("Valid CPA required for narrowing operation");
    }
    // Since the narrowing refinement algorithm use reached set view instead of legacy reached
    // set, we discard ARG information (which may cause some weird problems) here.
    CompositeCPA compCPA = CPAs.retrieveCPA(pCPA, CompositeCPA.class);
    if (compCPA == null) {
      throw new InvalidConfigurationException("Composite CPA required for narrowing operation");
    }
    transferRelation = compCPA.getTransferRelation();
    // we assume that we do not use precision adjustment to change precision
    mergeOperator = compCPA.getMergeOperator();

    if (cfa == null) {
      CFAInfo info = GlobalInfo.getInstance().getCFAInfo().orNull();
      if (info == null) {
        throw new InvalidConfigurationException("CFA required for narrowing algorithm");
      }
      cfa = info.getCFA();
    }
  }

  public Multimap<CFANode, AbstractState> run0(ReachedSet reachedSet, final FunctionEntryNode entry)
      throws CPAException, InterruptedException {
    // re-construct location-mapped reached set and add states inside the specified function only
    LocationReachedSetView reachedView = new LocationReachedSetView();
    LocationWaitlist waitlist = new LocationWaitlist();
    waitlist.push(entry);
    for (AbstractState reachedState : reachedSet) {
      CFANode node = AbstractStates.extractLocation(reachedState);
      if (node == null) {
        continue;
      }
      if (!reachedView.contains(node)) {
        Collection<AbstractState> states = reachedSet.getReached(reachedState);
        for (AbstractState state : states) {
          Precision precision = reachedSet.getPrecision(state);
          CompositeState compState = AbstractStates.extractStateByType(state, CompositeState.class);
          assert (compState != null) : "CompositeState missing, but CompositeCPA exists";
          reachedView.add(compState, precision);
        }
      }
    }
    waitlist.addFixedLoc(entry);
    // run the algorithm on the reached view with location waitlist
    run1(reachedView, waitlist);
    return reachedView.getLocatiton2StateMap();
  }

  private void run1(final LocationReachedSetView reachedView, final LocationWaitlist waitlist)
      throws CPAException, InterruptedException {
    PartialStateManager manager = new PartialStateManager();
    while (!waitlist.isEmpty()) {
      CFANode loc = waitlist.pop();
      // to improve the performance of narrowing, we skip global declarations at the main function
      if (loc.getNumLeavingEdges() == 1) {
        CFAEdge leavingEdge = loc.getLeavingEdge(0);
        if (leavingEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
          CDeclarationEdge declarationEdge = (CDeclarationEdge) leavingEdge;
          if (declarationEdge.getDeclaration().isGlobal()) {
            // we neither compute transfer relation for this edge nor update the reached view
            CFANode successor = leavingEdge.getSuccessor();
            waitlist.push(successor);
            waitlist.addFixedLoc(successor);
            continue;
          }
        }
      }

      Collection<AbstractState> currentStates = reachedView.getReached(loc);
      if (currentStates.isEmpty()) {
        continue;
      }
      for (AbstractState currentState : currentStates) {
        Precision currentPrec = reachedView.getPrecision(currentState);
        Collection<? extends AbstractState> successors;
        if (transferRelation instanceof TransferRelationWithNarrowingSupport) {
          successors = ((TransferRelationWithNarrowingSupport) transferRelation)
              .getAbstractSuccessorsUnderNarrowing(currentState,
                  Lists.<AbstractState>newArrayList(), currentPrec);
        } else {
          successors = transferRelation.getAbstractSuccessors(currentState, Lists
              .<AbstractState>newArrayList(), currentPrec);
        }
        Map<CFANode, AbstractState> successorsByLoc = new HashMap<>();
        for (AbstractState successor : successors) {
          // In general, precision adjustment is performed here to accelerate the analysis. Since
          // reached set is no longer available, we skip precision adjustment step. This does no
          // harm on correctness of algorithm, though efficiency could be degraded in some cases.
          // Hence, `precision` does not matter.
          CFANode succLoc = AbstractStates.extractLocation(successor);
          if (succLoc != null) {
            if (successorsByLoc.containsKey(succLoc)) {
              AbstractState old = successorsByLoc.get(succLoc);
              successorsByLoc.put(succLoc, mergeOperator.merge(successor, old, currentPrec));
            } else {
              successorsByLoc.put(succLoc, successor);
            }
          }
        }
        // try to update the successor locations, partial or full?
        for (Entry<CFANode, AbstractState> entry : successorsByLoc.entrySet()) {
          CFANode successorLoc = entry.getKey();
          AbstractState successorState = entry.getValue();
          Collection<CFAEdge> enteringEdges = ENTERING_EDGE_OF.apply(successorLoc);
          assert (enteringEdges != null);
          // check if all predecessors are fixed
          boolean allPredecessorsFixed = true;
          for (CFAEdge enteringEdge : enteringEdges) {
            CFANode predecessor = enteringEdge.getPredecessor();
            if (!waitlist.isFixed(predecessor)) {
              allPredecessorsFixed = false;
              break;
            }
          }
          if (allPredecessorsFixed) {
            if (enteringEdges.size() == 1) {
              // `successorState` is the resultant state for the successor location
              // Note: we do not need to narrow the location here, because the location is about
              // to be fixed finally
              reachedView.addByForce(successorState, currentPrec);
            } else {
              manager.addPartialState(successorLoc, loc, successorState);
              reachedView.addByForce(manager.getTotalSuccessor(successorLoc, currentPrec),
                  currentPrec);
            }
            waitlist.push(successorLoc);
            waitlist.addFixedLoc(successorLoc);
          } else {
            if (enteringEdges.size() == 1) {
              // `successorState` can be used directly to update the successor location, however
              // whether this location is marked as fixed depends on if the abstract state is
              // *ACTUALLY* refined
              boolean changed = reachedView.addByForce(successorState, currentPrec);
              waitlist.push(successorLoc);
              if (!changed) {
                waitlist.addFixedLoc(successorLoc);
              } else {
                waitlist.narrow(successorLoc);
              }
            } else {
              manager.addPartialState(successorLoc, loc, successorState);
              // find predecessors that have not had the successor partial state yet
              boolean partialReady = true;
              for (CFAEdge enteringEdge : enteringEdges) {
                CFANode predecessor = enteringEdge.getPredecessor();
                if (!manager.hasPartialState(successorLoc, predecessor)) {
                  waitlist.push(predecessor);
                  partialReady = false;
                }
              }
              if (partialReady) {
                boolean changed = reachedView.addByForce(manager.getTotalSuccessor(successorLoc,
                    currentPrec), currentPrec);
                waitlist.push(successorLoc);
                if (changed) {
                  waitlist.narrow(successorLoc);
                }
                // else: in the original algorithm, we will mark the successor location as fixed
              }
              // otherwise, we do not put successor locations into waitlist
            }
          }
        }
      }
    }
  }

  private static final Function<CFANode, Collection<CFAEdge>> ENTERING_EDGE_OF = new
      Function<CFANode, Collection<CFAEdge>>() {
        @Override
        public Collection<CFAEdge> apply(final CFANode pCFANode) {
          return CFAUtils.allEnteringEdges(pCFANode).filter(new Predicate<CFAEdge>() {
            @Override
            public boolean apply(CFAEdge pEdge) {
              return pCFANode.getFunctionName().equals(pEdge.getPredecessor().getFunctionName());
            }
          }).toSet();
        }
      };

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    throw new UnsupportedOperationException("Narrowing algorithm should not be invoked by run()");
  }

  class LocationWaitlist implements Comparator<CFANode> {

    private final Queue<CFANode> elements;

    private final Map<CFANode, Integer> narrows;

    private final Set<CFANode> fixedSet;

    LocationWaitlist() {
      elements = new ArrayDeque<>();
      narrows = new HashMap<>();
      fixedSet = new HashSet<>();
    }

    public void push(CFANode loc) {
      // priority queue allows duplicated elements
      // if the specified location has been saturated, there is no need to push this location
      // into waitlist any more
      if (!elements.contains(loc) && !fixedSet.contains(loc)) {
        elements.add(loc);
      }
    }

    public CFANode pop() {
      return elements.poll();
    }

    public boolean isEmpty() {
      return elements.isEmpty();
    }

    @Override
    public int compare(CFANode pL1, CFANode pL2) {
      return pL1.getPostDominatorId() - pL2.getPostDominatorId();
    }

    /**
     * track the status of narrowing for states
     */
    void narrow(CFANode loc) {
      int narrowCount = 1;
      if (narrows.containsKey(loc)) {
        narrowCount = narrows.get(loc) + 1;
        narrows.put(loc, narrowCount);
      } else {
        narrows.put(loc, narrowCount);
      }
      if (maxNarrows > 0 && narrowCount >= maxNarrows) {
        fixedSet.add(loc);
      }
    }

    void addFixedLoc(CFANode loc) {
      fixedSet.add(loc);
    }

    boolean isFixed(CFANode loc) {
      return fixedSet.contains(loc);
    }

  }

  class LocationReachedSetView {

    private final Multimap<CFANode, AbstractState> statesByLoc = HashMultimap.create();
    private final Map<AbstractState, Precision> states = new HashMap<>();

    Collection<AbstractState> getReached(CFANode loc) {
      return statesByLoc.get(loc);
    }

    boolean contains(CFANode loc) {
      return statesByLoc.containsKey(loc);
    }

    void add(AbstractState state, Precision precision)
        throws CPAException, InterruptedException {
      CFANode loc = AbstractStates.extractLocation(state);
      if (loc == null) {
        throw new IllegalArgumentException("Wrapped state should support location mapping");
      }
      // we copy old states to prevent concurrent modification exception when accessing
      // location-state mapping
      Collection<AbstractState> olds = new HashSet<>(statesByLoc.get(loc));
      if (olds.isEmpty()) {
        statesByLoc.put(loc, state);
        states.put(state, precision);
      } else {
        for (AbstractState old : olds) {
          AbstractState merged = mergeOperator.merge(state, old, precision);
          if (!merged.isEqualTo(old)) {
            // update reached set view with merged new state
            statesByLoc.remove(loc, old);
            statesByLoc.put(loc, merged);
            states.remove(old);
            states.put(merged, precision);
          }
        }
      }
    }

    /**
     * Forcibly update reached state.
     * This is the main method for refining the state.
     *
     * @return whether something *ACTUALLY* changes
     */
    boolean addByForce(AbstractState state, Precision precision) {
      CFANode loc = AbstractStates.extractLocation(state);
      if (loc == null) {
        throw new IllegalArgumentException("Wrapped state should support location mapping");
      }
      Collection<AbstractState> olds = statesByLoc.get(loc);
      boolean changed = true;
      if (olds.size() == 1) {
        AbstractState onlyOld = Iterables.getOnlyElement(olds);
        if (state.isEqualTo(onlyOld)) {
          changed = false;
        }
      }
      if (changed) {
        statesByLoc.removeAll(loc);
        for (AbstractState old : olds) {
          states.remove(old);
        }
        statesByLoc.put(loc, state);
        states.put(state, precision);
      }
      return changed;
    }

    Precision getPrecision(AbstractState state) {
      Precision precision = states.get(state);
      Preconditions.checkArgument(precision != null, "Specified state not exist in the reached "
          + "set view");
      return precision;
    }

    Multimap<CFANode, AbstractState> getLocatiton2StateMap() {
      return Multimaps.unmodifiableMultimap(statesByLoc);
    }
  }

  class PartialStateManager {

    // <R, C, V> where R is the certain location, C is its predecessor and R is the partial state
    // by computing transfer from C to R
    private final Table<CFANode, CFANode, AbstractState> partialStateTable = HashBasedTable
        .create();

    void addPartialState(
        CFANode sink, CFANode source, AbstractState newState)
        throws CPAException, InterruptedException {
      partialStateTable.put(sink, source, newState);
    }

    AbstractState getTotalSuccessor(CFANode sink, Precision precision)
        throws CPAException, InterruptedException {
      Map<CFANode, AbstractState> partialStates = partialStateTable.row(sink);
      assert (partialStates.size() >= 1);
      AbstractState result = null;
      for (AbstractState partialState : partialStates.values()) {
        if (result == null) {
          result = partialState;
        } else {
          result = mergeOperator.merge(partialState, result, precision);
        }
      }
      assert result != null : "There should be at least one predecessor state";
      return result;
    }

    boolean hasPartialState(CFANode sink, CFANode source) {
      return partialStateTable.contains(sink, source);
    }
  }

}
