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
package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.SwitchableGraphable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.summary.SummaryAcceptableState;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.weakness.WeaknessProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CompositeState implements AbstractWrapperState,
                                       Targetable, Partitionable, Serializable, Graphable,
                                       SummaryAcceptableState {
  private static final long serialVersionUID = -5143296331663510680L;
  private final ImmutableList<AbstractState> states;
  private transient Object partitionKey; // lazily initialized

  private final List<ErrorReport> errorReportsForState;

  public CompositeState(List<AbstractState> elements) {
    this.states = ImmutableList.copyOf(elements);
    errorReportsForState = Lists.newArrayList();
  }

  public static CompositeState copyOf(CompositeState old) {
    return new CompositeState(old.states);
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof CompositeState)) {
      return false;
    }
    CompositeState that = (CompositeState) other;
    if (states.size() != that.states.size()) {
      return false;
    }
    for (int i = 0; i < states.size(); i++) {
      AbstractState c1 = states.get(i);
      AbstractState c2 = that.states.get(i);
      if (!c1.isEqualTo(c2)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isTarget() {
    for (AbstractState element : states) {
      if ((element instanceof Targetable) && ((Targetable) element).isTarget()) {
        return true;
      }
    }
    if (!errorReportsForState.isEmpty()) {
      return true;
    }
    return false;
  }

  int getNumberOfStates() {
    return states.size();
  }

  /**
   * Update error reports from composite transfer relation
   *
   * @param incomingErrors error reports extracted in transfer relation
   */
  void updateErrorReports(Collection<ErrorReport> incomingErrors) {
    errorReportsForState.addAll(incomingErrors);
  }

  @Override
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    checkState(isTarget());
    Set<Property> properties = Sets.newHashSetWithExpectedSize(states.size());
    for (AbstractState element : states) {
      if ((element instanceof Targetable) && ((Targetable) element).isTarget()) {
        properties.addAll(((Targetable) element).getViolatedProperties());
      }
    }
    for (ErrorReport error : errorReportsForState) {
      WeaknessProperty weaknessProperty = new WeaknessProperty(error.getWeakness());
      properties.add(weaknessProperty);
    }
    return properties;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append('(');
    for (AbstractState element : states) {
      builder.append(element.getClass().getSimpleName());
      builder.append(": ");
      builder.append(element.toString());
      builder.append("\n ");
    }
    builder.replace(builder.length() - 1, builder.length(), ")");

    return builder.toString();
  }

  @Override
  public String toDOTLabel() {
    StringBuilder builder = new StringBuilder();
    for (AbstractState element : states) {
      if (element instanceof Graphable) {
        if (element instanceof SwitchableGraphable) {
          if (!((SwitchableGraphable) element).getActiveStatus()) {
            // don't output DOT label
            continue;
          }
        }
        String label = ((Graphable) element).toDOTLabel();
        if (!label.isEmpty()) {
          builder.append(element.getClass().getSimpleName());
          builder.append(": ");
          builder.append(label);
          builder.append("\\n ");
        }
      }
    }

    return builder.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    for (AbstractState element : states) {
      if (element instanceof Graphable) {
        if (((Graphable) element).shouldBeHighlighted()) {
          return true;
        }
      }
    }
    return false;
  }

  public AbstractState get(int idx) {
    return states.get(idx);
  }

  @Override
  public List<AbstractState> getWrappedStates() {
    return states;
  }


  @Override
  public Object getPartitionKey() {
    if (partitionKey == null) {
      Object[] keys = new Object[states.size()];

      int i = 0;
      for (AbstractState element : states) {
        if (element instanceof Partitionable) {
          keys[i] = ((Partitionable) element).getPartitionKey();
        }
        i++;
      }

      // wrap array of keys in object to enable overriding of equals and hashCode
      partitionKey = new CompositePartitionKey(keys);
    }

    return partitionKey;
  }

  private static final class CompositePartitionKey {

    private final Object[] keys;

    private CompositePartitionKey(Object[] pElements) {
      keys = pElements;
    }

    @Override
    public boolean equals(Object pObj) {
      return this == pObj || pObj instanceof CompositePartitionKey && Arrays
          .equals(this.keys, ((CompositePartitionKey) pObj).keys);

    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(keys);
    }

    @Override
    public String toString() {
      return "[" + Joiner.on(", ").skipNulls().join(keys) + "]";
    }
  }

  /* ******************* */
  /* summary application */
  /* ******************* */

  @Override
  public Collection<? extends AbstractState> applyFunctionSummary(
      List<SummaryInstance> pSummaryList, CFAEdge inEdge, CFAEdge outEdge, List<AbstractState>
      pOtherStates) throws CPATransferException {
    List<AbstractState> wrappedStates = this.getWrappedStates();
    int size = wrappedStates.size();
    List<Collection<? extends AbstractState>> componentSuccessors = new ArrayList<>(size);
    int resultSize = 1;
    for (AbstractState currentState : wrappedStates) {
      Collection<? extends AbstractState> currentSuccessors;
      if (currentState instanceof SummaryAcceptableState) {
        currentSuccessors = ((SummaryAcceptableState) currentState).applyFunctionSummary
            (pSummaryList, inEdge, outEdge, wrappedStates);
        resultSize *= currentSuccessors.size();
      } else {
        currentSuccessors = Collections.singleton(currentState);
      }
      componentSuccessors.add(currentSuccessors);
    }
    Collection<List<AbstractState>> product = CompositeTransferRelation.createCartesianProduct
        (componentSuccessors, resultSize);
    return FluentIterable.from(product).transform(new Function<List<AbstractState>, CompositeState>
        () {
      @Override
      public CompositeState apply(List<AbstractState> pAbstractStates) {
        return new CompositeState(pAbstractStates);
      }
    }).toSet();
  }

  @Override
  public Multimap<CFAEdge, AbstractState> applyExternalLoopSummary(
      List<SummaryInstance> pSummaryList, CFAEdge inEdge, Collection<CFAEdge> outEdges,
      List<AbstractState> pOtherStates) throws CPATransferException {
    List<AbstractState> wrappedStates = this.getWrappedStates();
    int size = wrappedStates.size();
    List<Multimap<CFAEdge, AbstractState>> componentSuccessors = new ArrayList<>(size);
    for (AbstractState wrappedState : wrappedStates) {
      Multimap<CFAEdge, AbstractState> currentSuccessors;
      if (wrappedState instanceof SummaryAcceptableState) {
        currentSuccessors = ((SummaryAcceptableState) wrappedState).applyExternalLoopSummary
            (pSummaryList, inEdge, outEdges, wrappedStates);
      } else {
        currentSuccessors = HashMultimap.create();
        for (CFAEdge outEdge : outEdges) {
          currentSuccessors.put(outEdge, wrappedState);
        }
      }
      componentSuccessors.add(currentSuccessors);
    }
    ImmutableMultimap.Builder<CFAEdge, AbstractState> builder = ImmutableMultimap.builder();
    for (CFAEdge outEdge : outEdges) {
      List<Collection<? extends AbstractState>> components = new ArrayList<>(size);
      int resultCount = 1;
      for (Multimap<CFAEdge, AbstractState> componentSuccessor : componentSuccessors) {
        Collection<? extends AbstractState> successorsForEdge = componentSuccessor.get(outEdge);
        components.add(successorsForEdge);
        resultCount *= successorsForEdge.size();
      }
      Collection<List<AbstractState>> productForEdge = CompositeTransferRelation
          .createCartesianProduct(components, resultCount);
      for (List<AbstractState> product : productForEdge) {
        CompositeState newState = new CompositeState(product);
        builder.put(outEdge, newState);
      }
    }
    return builder.build();
  }

  @Override
  public Collection<? extends AbstractState> applyInternalLoopSummary(
      List<SummaryInstance> pSummaryList, CFAEdge inEdge, List<AbstractState> pOtherStates)
      throws CPATransferException {
    List<AbstractState> wrappedStates = this.getWrappedStates();
    int size = wrappedStates.size();
    List<Collection<? extends AbstractState>> componentSuccessors = new ArrayList<>(size);
    int resultCount = 1;
    for (AbstractState currentState : wrappedStates) {
      Collection<? extends AbstractState> currentSuccessors;
      if (currentState instanceof SummaryAcceptableState) {
        currentSuccessors = ((SummaryAcceptableState) currentState).applyInternalLoopSummary
            (pSummaryList, inEdge, wrappedStates);
        resultCount *= currentSuccessors.size();
      } else {
        currentSuccessors = Collections.singleton(currentState);
      }
      componentSuccessors.add(currentSuccessors);
    }
    Collection<List<AbstractState>> product = CompositeTransferRelation.createCartesianProduct(
        componentSuccessors, resultCount);
    return FluentIterable.from(product).transform(new Function<List<AbstractState>,
        CompositeState>() {
      @Override
      public CompositeState apply(List<AbstractState> pAbstractStates) {
        return new CompositeState(pAbstractStates);
      }
    }).toSet();
  }
}
