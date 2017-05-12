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
package org.sosy_lab.cpachecker.util.collections.set;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.util.collections.preliminary.Presence;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class PathCopyingPersistentSet<E extends Comparable<? super E>>
    implements PersistentSortedSet<E> {
  private static final PathCopyingPersistentSet<?> EMPTY_SET = new PathCopyingPersistentSet();

  private PersistentSortedMap<E, Presence> elements;

  public PathCopyingPersistentSet() {
    elements = PathCopyingPersistentTreeMap.of();
  }

  private PathCopyingPersistentSet(PersistentSortedMap<E, Presence> m) {
    elements = m;
  }

  @SuppressWarnings("unchecked")
  public static <E extends Comparable<? super E>> PersistentSortedSet<E> of() {
    return (PersistentSortedSet<E>) EMPTY_SET;
  }

  public static <E extends Comparable<? super E>> PersistentSortedSet<E> of(PersistentSortedMap<E, Presence> m) {
    return new PathCopyingPersistentSet<>(m);
  }

  public static <E extends Comparable<? super E>> PersistentSortedSet<E> of(Collection<E> cs) {
    PersistentSortedSet<E> s = of();
    for (E e : cs) {
      s = s.addAndCopy(e);
    }
    return s;
  }

  @Override
  public PersistentSortedSet<E> addAndCopy(E e) {
    return of(elements.putAndCopy(e, Presence.INSTANCE));
  }

  @Override
  public PersistentSortedSet<E> removeAndCopy(E e) {
    return of(elements.removeAndCopy(e));
  }

  @Override
  public PersistentSortedSet<E> empty() {
    return of(elements.empty());
  }

  @Override
  public Iterator<E> iterator() {
    return elements.keySet().iterator();
  }

  @Override
  public PersistentSortedSet<E> addAllAndCopy(Collection<E> cs) {
    PersistentSortedMap<E, Presence> newElements = elements;
    for (E e : cs) {
      newElements = newElements.putAndCopy(e, Presence.INSTANCE);
    }
    return of(newElements);
  }

  @Override
  public PersistentSortedSet<E> removeAllAndCopy(Collection<E> cs) {
    PersistentSortedMap<E, Presence> newElements = elements;
    for (E e : cs) {
      newElements = newElements.removeAndCopy(e);
    }
    return of(newElements);
  }

  @Override
  public boolean contains(E e) {
    return elements.containsKey(e);
  }

  @Override
  public boolean containsAll(Collection<E> cs) {
    for (E e : cs) {
      if (!contains(e)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isEmpty() {
    return elements.isEmpty();
  }

  @Override
  public String toString() {
    return elements.keySet().toString();
  }

  @Override
  public Set<E> asSet() {
    return elements.keySet();
  }
}
