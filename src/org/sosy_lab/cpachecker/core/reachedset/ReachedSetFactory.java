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
package org.sosy_lab.cpachecker.core.reachedset;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.waitlist.AutomatonFailedMatchesWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.AutomatonMatchesWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.CallstackSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.DominationSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.ExplicitSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.LoopstackSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.PostorderSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.ReversePostorderSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.ThreadingSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariableWaitlist;

@Options(prefix = "analysis")
public class ReachedSetFactory {

  protected enum ReachedSetType {
    NORMAL,
    LOCATIONMAPPED,
    PARTITIONED
  }

  @Option(secure = true, name = "traversal.order",
      description = "which strategy to adopt for visiting states?")
  private
  Waitlist.TraversalMethod traversalMethod = Waitlist.TraversalMethod.DFS;

  @Option(secure = true, name = "traversal.useCallstack",
      description = "handle states with a deeper callstack first"
          + "\nThis needs the CallstackCPA instance to have any effect.")
  private
  boolean useCallstack = false;

  @Option(secure = true, name = "traversal.useLoopstack",
      description = "handle states with a deeper loopstack first.")
  private
  boolean useLoopstack = false;

  @Option(secure = true, name = "traversal.useReverseLoopstack",
      description = "handle states with a more shallow loopstack first.")
  private
  boolean useReverseLoopstack = false;

  @Option(secure = true, name = "traversal.useReversePostorder",
      description = "Use an implementation of reverse postorder strategy that allows to select "
          + "a secondary strategy that is used if there are two states with the same reverse postorder id. "
          + "The secondary strategy is selected with 'analysis.traversal.order'.")
  private
  boolean useReversePostorder = false;

  @Option(secure = true, name = "traversal.usePostorder",
      description = "Use an implementation of postorder strategy that allows to select "
          + "a secondary strategy that is used if there are two states with the same postorder id. "
          + "The secondary strategy is selected with 'analysis.traversal.order'.")
  private
  boolean usePostorder = false;

  @Option(secure = true, name = "traversal.useExplicitInformation",
      description = "handle more abstract states (with less information) first? (only for ExplicitCPA)")
  private
  boolean useExplicitInformation = false;

  @Option(secure = true, name = "traversal.useAutomatonInformation",
      description = "handle abstract states with more automaton matches first? (only if AutomatonCPA enabled)")
  private
  boolean useAutomatonInformation = false;

  @Option(secure = true, name = "traversal.byAutomatonVariable",
      description = "traverse in the order defined by the values of an automaton variable")
  private
  String byAutomatonVariable = null;

  @Option(secure = true, name = "traversal.useNumberOfThreads",
      description = "handle abstract states with fewer running threads first? (needs ThreadingCPA)")
  private
  boolean useNumberOfThreads = false;

  @Option(secure = true, name = "traversal.useDominationOrder", description = "handle states to "
      + "be visited by the order of sorted domination tree")
  private
  boolean useDominationOrder = false;

  @Option(secure = true, name = "reachedSet",
      description = "which reached set implementation to use?"
          + "\nNORMAL: just a simple set"
          + "\nLOCATIONMAPPED: a different set per location "
          + "(faster, states with different locations cannot be merged)"
          + "\nPARTITIONED: partitioning depending on CPAs (e.g Location, Callstack etc.)")
  private
  ReachedSetType reachedSet = ReachedSetType.PARTITIONED;

  @Option(secure = true, name = "reachedSet.hierarchical", description = "handle reached abstract"
      + " states in a hierarchical manner, which is required for multi-entry analysis")
  private
  boolean useHierarchicalReachedSet = false;

  @Option(secure = true, name = "traversal.controlled.maxWaitingSize", description = "maximum "
      + "number of waiting size allowed in analysis, which is used to control the volume of path "
      + "space")
  private int maximumWaitingSize = 0;

  public ReachedSetFactory(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  public ReachedSet create() {
    WaitlistFactory waitlistFactory = traversalMethod;

    if (useAutomatonInformation) {
      waitlistFactory = AutomatonMatchesWaitlist.factory(waitlistFactory);
      waitlistFactory = AutomatonFailedMatchesWaitlist.factory(waitlistFactory);
    }
    if (useReversePostorder) {
      waitlistFactory = ReversePostorderSortedWaitlist.factory(waitlistFactory);
    }
    if (usePostorder) {
      waitlistFactory = PostorderSortedWaitlist.factory(waitlistFactory);
    }
    if (useLoopstack) {
      waitlistFactory = LoopstackSortedWaitlist.factory(waitlistFactory);
    }
    if (useReverseLoopstack) {
      waitlistFactory = LoopstackSortedWaitlist.reversedFactory(waitlistFactory);
    }
    if (useCallstack) {
      waitlistFactory = CallstackSortedWaitlist.factory(waitlistFactory);
    }
    if (useExplicitInformation) {
      waitlistFactory = ExplicitSortedWaitlist.factory(waitlistFactory);
    }
    if (byAutomatonVariable != null) {
      waitlistFactory = AutomatonVariableWaitlist.factory(waitlistFactory, byAutomatonVariable);
    }
    if (useNumberOfThreads) {
      waitlistFactory = ThreadingSortedWaitlist.factory(waitlistFactory);
    }
    if (useDominationOrder) {
      waitlistFactory = DominationSortedWaitlist.factory(waitlistFactory, maximumWaitingSize);
    }

    switch (reachedSet) {
      case PARTITIONED:
        return useHierarchicalReachedSet ? new PartitionedHierReachedSet(waitlistFactory) :
               new PartitionedReachedSet(waitlistFactory);

      case LOCATIONMAPPED:
        return useHierarchicalReachedSet ? new LocationMappedHierReachedSet(waitlistFactory) :
               new LocationMappedReachedSet(waitlistFactory);

      case NORMAL:
      default:
        return useHierarchicalReachedSet ? new HierarchicalReachedSet(waitlistFactory) :
               new DefaultReachedSet(waitlistFactory);
    }
  }
}
