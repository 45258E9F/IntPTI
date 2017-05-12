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
package org.sosy_lab.cpachecker.cpa.shape.visitors.results;

import org.sosy_lab.cpachecker.cpa.shape.ShapeState;

/**
 * A generic class that binds an object with shape state.
 * This is the super class of many shape visitor products.
 */
public abstract class ShapeObjectAndState<T> {

  private final ShapeState shapeState;
  private final T object;

  protected ShapeObjectAndState(ShapeState pState, T pValue) {
    shapeState = pState;
    object = pValue;
  }

  public T getObject() {
    return object;
  }

  public ShapeState getShapeState() {
    return shapeState;
  }

  @Override
  public String toString() {
    return object.toString() + " | state id: " + shapeState.getId();
  }
}
