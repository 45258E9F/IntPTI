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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.LazyFutureTask;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class AdjustableInvariantGenerator<T extends InvariantGenerator>
    extends AbstractInvariantGenerator implements StatisticsProvider {

  private final ShutdownNotifier shutdownNotifier;

  private final Function<? super T, ? extends T> adjust;

  private final AtomicBoolean isProgramSafe = new AtomicBoolean();

  private final AtomicReference<T> invariantGenerator;

  private final AtomicReference<Future<FormulaAndTreeSupplier>> currentInvariantSupplier =
      new AtomicReference<>();

  public AdjustableInvariantGenerator(
      ShutdownNotifier pShutdownNotifier,
      T pInitialGenerator,
      Function<? super T, ? extends T> pAdjust) {
    shutdownNotifier = pShutdownNotifier;
    invariantGenerator = new AtomicReference<>(pInitialGenerator);
    adjust = pAdjust;
  }

  @Override
  public void start(CFANode pInitialLocation) {
    invariantGenerator.get().start(pInitialLocation);
    setSupplier(invariantGenerator.get());
  }

  public boolean adjustAndContinue(CFANode pInitialLocation)
      throws CPAException, InterruptedException {
    final T current = invariantGenerator.get();
    try {
      setSupplier(new FormulaAndTreeSupplier(current.get(), current.getAsExpressionTree()));
    } finally {
      if (current.isProgramSafe()) {
        isProgramSafe.set(true);
      }
    }
    final T next = adjust.apply(current);
    boolean adjustable = next != current && next != null;
    if (adjustable) {
      next.start(pInitialLocation);
      invariantGenerator.set(next);
    }
    return adjustable;
  }

  @Override
  public void cancel() {
    invariantGenerator.get().cancel();
  }

  private FormulaAndTreeSupplier getInternal() throws CPAException, InterruptedException {
    Future<FormulaAndTreeSupplier> supplier = currentInvariantSupplier.get();
    Preconditions.checkState(supplier != null);
    try {
      return supplier.get();
    } catch (ExecutionException e) {
      if (e.getCause() instanceof InterruptedException) {
        return new FormulaAndTreeSupplier(
            invariantGenerator.get().get(), invariantGenerator.get().getAsExpressionTree());
      }
      Throwables.propagateIfPossible(e.getCause(), CPAException.class);
      throw new UnexpectedCheckedException("invariant generation", e.getCause());
    } catch (CancellationException e) {
      shutdownNotifier.shutdownIfNecessary();
      throw e;
    }
  }

  @Override
  public InvariantSupplier get() throws CPAException, InterruptedException {
    return getInternal();
  }

  @Override
  public ExpressionTreeSupplier getAsExpressionTree() throws CPAException, InterruptedException {
    return getInternal();
  }

  @Override
  public boolean isProgramSafe() {
    return isProgramSafe.get() || invariantGenerator.get().isProgramSafe();
  }

  @Override
  public void injectInvariant(CFANode pLocation, AssumeEdge pAssumption)
      throws UnrecognizedCodeException {
    invariantGenerator.get().injectInvariant(pLocation, pAssumption);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    InvariantGenerator invariantGenerator = this.invariantGenerator.get();
    if (invariantGenerator instanceof StatisticsProvider) {
      ((StatisticsProvider) invariantGenerator).collectStatistics(pStatsCollection);
    }
  }

  private void setSupplier(final InvariantGenerator pInvariantGenerator) {
    currentInvariantSupplier.set(new LazyFutureTask<>(new Callable<FormulaAndTreeSupplier>() {

      @Override
      public FormulaAndTreeSupplier call() throws Exception {
        return new FormulaAndTreeSupplier(pInvariantGenerator.get(),
            pInvariantGenerator.getAsExpressionTree());
      }

    }));
  }

  private void setSupplier(final FormulaAndTreeSupplier pSupplier) {
    currentInvariantSupplier.set(new LazyFutureTask<>(new Callable<FormulaAndTreeSupplier>() {

      @Override
      public FormulaAndTreeSupplier call() throws Exception {
        return pSupplier;
      }

    }));
  }

}
