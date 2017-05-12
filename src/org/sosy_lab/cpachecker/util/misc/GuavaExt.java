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
package org.sosy_lab.cpachecker.util.misc;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;

import java.util.Comparator;

public class GuavaExt {
  public static <E, V extends Comparable<V>> E max(
      Iterable<E> elements,
      final Function<E, V> fitness) {
    Comparator<E> comparator = new Comparator<E>() {
      @Override
      public int compare(E a, E b) {
        return Ordering.natural().compare(fitness.apply(a), fitness.apply(b));
      }
    };
    return Ordering.from(comparator).max(elements);
  }

  public static <E, V extends Comparable<V>> E min(
      Iterable<E> elements,
      final Function<E, V> fitness) {
    Comparator<E> comparator = new Comparator<E>() {
      @Override
      public int compare(E a, E b) {
        return Ordering.natural().compare(fitness.apply(a), fitness.apply(b));
      }
    };
    return Ordering.from(comparator).min(elements);
  }

}
