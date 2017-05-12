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
package org.sosy_lab.cpachecker.util.collections.map;

import junit.framework.TestCase;

public class PathCopyingPersistentBiMapTest extends TestCase {
  public void testEmpty() {
    PersistentBiMap<Integer, String> map = PathCopyingPersistentBiMap.of();
    System.out.println(map.toString());
    assertTrue(map.isEmpty());
  }

  public void testPut() {
    PersistentBiMap<Integer, String> m0 = PathCopyingPersistentBiMap.of();
    assertTrue(m0.isEmpty());

    PersistentBiMap<Integer, String> m1 = m0.putAndCopy(1, "One");
    assertFalse(m1.isEmpty());
    assertTrue(m1.size() == 1);
    assertEquals("One", m1.get(1));

    PersistentBiMap<Integer, String> m2 = m0.putAndCopy(2, "Two");
    assertFalse(m2.isEmpty());
    assertTrue(m2.size() == 1);
    assertEquals("Two", m2.get(2));

    PersistentBiMap<Integer, String> m3 = m2.putAndCopy(3, "Three");
    assertFalse(m3.isEmpty());
    assertTrue(m3.size() == 2);
    assertEquals("Three", m3.get(3));

    // adding to the inverse
    PersistentBiMap<Integer, String> m4 = m3.inverse().putAndCopy("Four", 4).inverse();
    assertFalse(m4.isEmpty());
    assertTrue(m4.size() == 3);   // 2, 3, 4
    assertEquals("Four", m4.get(4));

    // putting duplicated elements cause exception
    try {
      m1.putAndCopy(1, "XXX");
      assertTrue(false);
    } catch (IllegalArgumentException e) {
    }

    try {
      m3.putAndCopy(2, "XXX");
      assertTrue(false);
    } catch (IllegalArgumentException e) {
    }

    try {
      m3.putAndCopy(3, "XXX");
      assertTrue(false);
    } catch (IllegalArgumentException e) {
    }

    try {
      m4.inverse().putAndCopy("Two", 0);
      assertTrue(false);
    } catch (IllegalArgumentException e) {
    }
  }

  public void testRemove() {
    PersistentBiMap<Integer, String> m0 = PathCopyingPersistentBiMap.of();
    assertTrue(m0.isEmpty());

    PersistentBiMap<Integer, String> m1 = m0.putAndCopy(1, "One");
    assertFalse(m1.isEmpty());
    assertTrue(m1.size() == 1);
    assertEquals("One", m1.get(1));

    PersistentBiMap<Integer, String> m2 = m0.putAndCopy(2, "Two").putAndCopy(3, "Three");
    assertFalse(m2.isEmpty());
    assertEquals(2, m2.size()); // 2, 3
    assertEquals("Two", m2.get(2));

    PersistentBiMap<Integer, String> m3 = m0.removeAndCopy(1);
    assertTrue(m3.isEmpty());

    PersistentBiMap<Integer, String> m4 = m2.removeAndCopy(2).removeAndCopy(-2);
    assertFalse(m4.isEmpty());
    assertFalse(m4.containsKey(1));
    assertFalse(m4.containsKey(2));
    assertTrue(m4.containsKey(3));
    assertFalse(m4.containsKey(-2));
  }

  public void testInverse() {
    PersistentBiMap<Integer, String> m0 = PathCopyingPersistentBiMap.of();
    assertTrue(m0.isEmpty());

    PersistentBiMap<Integer, String> m1 = m0.putAndCopy(1, "One");
    assertFalse(m1.isEmpty());
    assertTrue(m1.size() == 1);
    assertEquals("One", m1.get(1));

    PersistentBiMap<Integer, String> m2 = m0.putAndCopy(2, "Two").putAndCopy(3, "Three");
    assertFalse(m2.isEmpty());
    assertEquals(2, m2.size()); // 2, 3
    assertEquals("Two", m2.get(2));

    System.out.println(m2);
    assertEquals(2, m2.inverse().size());
    assertTrue(m2.inverse().containsKey("Two"));
    assertTrue(m2.inverse().containsKey("Three"));
    assertFalse(m2.inverse().containsKey("Zero"));
    assertFalse(m2.inverse().containsKey("Four"));
    assertEquals((Integer) 2, m2.inverse().get("Two"));
    assertEquals((Integer) 3, m2.inverse().get("Three"));
  }
}
