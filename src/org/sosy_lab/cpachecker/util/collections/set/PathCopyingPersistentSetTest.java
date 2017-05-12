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

import com.google.common.collect.Lists;

import junit.framework.TestCase;

import java.util.List;


public class PathCopyingPersistentSetTest extends TestCase {
  public void testEmpty() {
    PersistentSortedSet<Integer> set = PathCopyingPersistentSet.of();
    System.out.println(set.toString());
    assertTrue(set.isEmpty());
  }

  public void testAdd() {
    PersistentSortedSet<Integer> t0 = PathCopyingPersistentSet.of();
    assertTrue(t0.isEmpty());
    System.out.println(t0.toString());
    // insert an element
    PersistentSortedSet<Integer> t1 = t0.addAndCopy(1);
    assertFalse(t1.isEmpty());
    System.out.println(t1.toString());
    // insert another element and make a copy
    PersistentSortedSet<Integer> t2 = t1.addAndCopy(2);
    assertFalse(t2.isEmpty());
    assertTrue(t2.contains(2));
    System.out.println(t2.toString());
    // insert another element on t1
    PersistentSortedSet<Integer> t3 = t1.addAndCopy(3);
    assertFalse(t3.isEmpty());
    assertFalse(t3.contains(2));
    assertTrue(t3.contains(3));
    System.out.println(t3.toString());
  }

  public void testAddAll() {
    PersistentSortedSet<Integer> t0 = PathCopyingPersistentSet.of();
    assertTrue(t0.isEmpty());
    System.out.println(t0.toString());
    // insert an element
    List<Integer> elems = Lists.newArrayList();
    elems.add(1);
    elems.add(2);
    elems.add(3);
    PersistentSortedSet<Integer> t1 = t0.addAllAndCopy(elems);
    assertFalse(t1.isEmpty());
    assertTrue(t1.contains(1));
    assertTrue(t1.contains(2));
    assertTrue(t1.contains(3));
    assertFalse(t1.contains(0));
    assertFalse(t1.contains(4));
    System.out.println(t1.toString());
  }
}