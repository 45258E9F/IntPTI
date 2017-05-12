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
package org.sosy_lab.cpachecker.util.collections.set;

import java.util.Collection;
import java.util.Set;

public interface PersistentSortedSet<E> extends Iterable<E> {

  boolean isEmpty();

  PersistentSortedSet<E> addAndCopy(E e);

  PersistentSortedSet<E> addAllAndCopy(Collection<E> cs);

  PersistentSortedSet<E> removeAndCopy(E e);

  PersistentSortedSet<E> removeAllAndCopy(Collection<E> cs);

  PersistentSortedSet<E> empty();

  boolean contains(E e);

  boolean containsAll(Collection<E> cs);

  Set<E> asSet();

}