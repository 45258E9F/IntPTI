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
package org.sosy_lab.cpachecker.core.interfaces;

/**
 * Interface which specifies that the state can be dumped to the
 * [part of] the graphviz label.
 */
public interface Graphable {

  /**
   * Return a string representation of this object
   * that is suitable to be printed inside a label of a node in a DOT graph.
   *
   * @return A non-null but possibly empty string.
   */
  public String toDOTLabel();

  /**
   * Return whether this object is somehow special as opposed
   * to other objects of the same type,
   * and should be highlighted in the output.
   */
  public boolean shouldBeHighlighted();
}
