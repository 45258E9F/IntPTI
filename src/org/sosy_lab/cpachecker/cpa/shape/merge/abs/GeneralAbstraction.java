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
package org.sosy_lab.cpachecker.cpa.shape.merge.abs;

import org.sosy_lab.cpachecker.cpa.shape.graphs.ShapeGraph;

import java.util.Collection;

/**
 * General interface for value abstraction.
 *
 * @param <T> the type of abstracted value pair
 */
public interface GeneralAbstraction<T extends AbstractValue> {

  void addAbstraction(Long abstractValue, ShapeGraph graph1, Long v1, ShapeGraph graph2, Long v2);

  void addNameAlias(String mainName, String hiddenName);

  /**
   * Interpret the concrete interpretation of a given abstracted value.
   */
  Collection<T> interpret(Long v);

  Collection<String> getAlias(String name);

}
