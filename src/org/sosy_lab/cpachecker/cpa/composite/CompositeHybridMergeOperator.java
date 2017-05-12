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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.NonMergeableAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.merge.HybridMergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.merge.MergeMode;
import org.sosy_lab.cpachecker.core.interfaces.merge.MergeStrategyIdentifier;
import org.sosy_lab.cpachecker.core.interfaces.merge.MergeTactic;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


@Options(prefix = "cpa.composite")
public class CompositeHybridMergeOperator implements HybridMergeOperator {

  private static final Predicate<Object> NON_MERGABLE_STATE = Predicates.instanceOf
      (NonMergeableAbstractState.class);

  private final List<MergeOperator> mergeOperators;
  private final List<StopOperator> stopOperators;

  @Option(secure = true, name = "tactics", description = "merging strategies shared by component "
      + "states")
  private Set<MergeStrategyIdentifier> mergeTactics = Sets.newHashSet();

  private final List<MergeTactic> strategies = new ArrayList<>();

  private MergeMode mergeMode = MergeMode.SEP;

  public CompositeHybridMergeOperator(
      Configuration pConfig,
      ImmutableList<MergeOperator> pMergeOperators,
      ImmutableList<StopOperator> pStopOperators)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    mergeOperators = pMergeOperators;
    stopOperators = pStopOperators;
    createStrategies();
    if (!strategies.isEmpty()) {
      for (int i = 0; i < mergeOperators.size(); i++) {
        MergeOperator currentOp = mergeOperators.get(i);
        if (currentOp instanceof HybridMergeOperator) {
          ((HybridMergeOperator) currentOp).addTactics(strategies);
          mergeOperators.set(i, currentOp);
        }
      }
    }
  }

  /**
   * Create strategies according to specified tactics.
   */
  private void createStrategies() {
    for (MergeStrategyIdentifier mergeStrategy : mergeTactics) {
      String name = mergeStrategy.getClassName();
      Class<?> clazz = GlobalInfo.getInstance().retrieveClass(name);
      if (clazz == null) {
        // it is possible, if specified strategy has not been implemented yet
        continue;
      }
      try {
        Constructor<?> constructor = clazz.getConstructor();
        constructor.setAccessible(true);
        Object newObject = clazz.cast(constructor.newInstance());
        assert (newObject instanceof MergeTactic) : "merge strategy identifier should match a "
            + "valid MergeTactic class";
        strategies.add((MergeTactic) newObject);
      } catch (NoSuchMethodException ex) {
        continue;
      } catch (InstantiationException ex) {
        continue;
      } catch (IllegalAccessException ex) {
        continue;
      } catch (InvocationTargetException ex) {
        continue;
      }
    }
  }

  @Override
  public MergeMode getMergeMode() {
    return mergeMode;
  }

  @Override
  public AbstractState merge(
      AbstractState state1, AbstractState state2, Precision precision)
      throws CPAException, InterruptedException {

    resetMode();

    CompositeState successor = (CompositeState) state1;
    CompositeState reached = (CompositeState) state2;
    CompositePrecision compositePrec = (CompositePrecision) precision;

    assert (successor.getNumberOfStates() == reached.getNumberOfStates());

    if (FluentIterable.from(successor.getWrappedStates()).anyMatch(NON_MERGABLE_STATE) ||
        FluentIterable.from(reached.getWrappedStates()).anyMatch(NON_MERGABLE_STATE)) {
      return reached;
    }

    // try to merge each components
    ImmutableList.Builder<AbstractState> mergedStates = ImmutableList.builder();
    List<AbstractState> totalSuccessor = successor.getWrappedStates();
    List<AbstractState> totalReached = reached.getWrappedStates();
    List<Precision> totalPrec = compositePrec.getPrecisions();

    // if this flag is set, we just directly return the reached state
    boolean identicalStates = true;
    for (int i = 0; i < mergeOperators.size(); i++) {
      MergeOperator mergeOp = mergeOperators.get(i);
      AbstractState subSuccessor = totalSuccessor.get(i);
      AbstractState subReached = totalReached.get(i);
      Precision subPrec = totalPrec.get(i);
      StopOperator stopOp = stopOperators.get(i);

      AbstractState mergedState;
      if (mergeOp instanceof HybridMergeOperator) {
        mergedState = ((HybridMergeOperator) mergeOp).merge(subSuccessor, totalSuccessor,
            subReached, totalReached, subPrec, totalPrec);
      } else {
        mergedState = mergeOp.merge(subSuccessor, subReached, subPrec);
      }
      if (!stopOp.stop(subSuccessor, Collections.singleton(mergedState), subPrec)) {
        // we do not join-merge two states because the merged one does not cover successor state
        return reached;
      }
      if (mergedState != subReached) {
        // then the reached state should be updated
        identicalStates = false;
      }
      mergedStates.add(mergedState);
    }

    // if we reach here, then the merged state can also cover successor state, that means it is
    // unnecessary to create another path for successor state
    mergeMode = MergeMode.JOIN;
    if (identicalStates) {
      return reached;
    } else {
      return new CompositeState(mergedStates.build());
    }
  }

  @Override
  public AbstractState merge(
      AbstractState state1,
      List<AbstractState> otherStates1,
      AbstractState state2,
      List<AbstractState> otherStates2,
      Precision precision,
      List<Precision> otherPrecisions) throws CPAException, InterruptedException {
    throw new UnsupportedOperationException("compound merge is not supported in composite CPA");
  }

  @Override
  public void addTactics(List<MergeTactic> tactics) {
    throw new UnsupportedOperationException("tactic sharing is not supported in composite CPA");
  }

  private void resetMode() {
    mergeMode = MergeMode.SEP;
  }
}
