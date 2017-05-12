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
package org.sosy_lab.cpachecker.core.defaults;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

import java.io.Serializable;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Base class for AbstractStates which wrap the abstract state of exactly
 * one CPA.
 */
public abstract class AbstractSingleWrapperState
    implements AbstractWrapperState, Targetable, Partitionable, Serializable {

  private static final long serialVersionUID = -332757795984736107L;
  private static final Function<AbstractState, AbstractState> unwrapFunction
      = new Function<AbstractState, AbstractState>() {

    @Override
    public AbstractState apply(AbstractState pArg0) {
      Preconditions.checkArgument(pArg0 instanceof AbstractSingleWrapperState);

      return ((AbstractSingleWrapperState) pArg0).getWrappedState();
    }
  };

  public static Function<AbstractState, AbstractState> getUnwrapFunction() {
    return unwrapFunction;
  }

  private final AbstractState wrappedState;

  public AbstractSingleWrapperState(@Nullable AbstractState pWrappedState) {
    // TODO this collides with some CPAs' way of handling dummy states, but it should really be not null here
    // Preconditions.checkNotNull(pWrappedState);
    wrappedState = pWrappedState;
  }

  public AbstractState getWrappedState() {
    return wrappedState;
  }

  @Override
  public boolean isTarget() {
    if (wrappedState instanceof Targetable) {
      return ((Targetable) wrappedState).isTarget();
    } else {
      return false;
    }
  }

  @Override
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    checkState(isTarget());
    return ((Targetable) wrappedState).getViolatedProperties();
  }

  @Override
  public Object getPartitionKey() {
    if (wrappedState instanceof Partitionable) {
      return ((Partitionable) wrappedState).getPartitionKey();
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    return wrappedState.toString();
  }

  @Override
  public ImmutableList<AbstractState> getWrappedStates() {
    return ImmutableList.of(wrappedState);
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AbstractSingleWrapperState)) {
      return false;
    }
    AbstractSingleWrapperState that = (AbstractSingleWrapperState) other;
    return ((wrappedState == null) == (that.wrappedState == null)) &&
        (wrappedState == null || wrappedState.isEqualTo(that.wrappedState));
  }
}