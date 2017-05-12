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
package org.sosy_lab.cpachecker.util.error;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.util.Set;


public class DummyErrorState extends ARGState {

  private static final Property dummyProperty = new Property() {
    @Override
    public String toString() {
      return "DummyProperty";
    }
  };

  private static final long serialVersionUID = 1338393013733003150L;

  public DummyErrorState(final AbstractState pWrapped) {
    super(pWrapped, null);
  }

  @Override
  public boolean isTarget() {
    return true;
  }

  @Override
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    return ImmutableSet.of(dummyProperty);
  }

  @Override
  public Object getPartitionKey() {
    return null;
  }
}
