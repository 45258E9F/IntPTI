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
package org.sosy_lab.cpachecker.util.collections.tree;

import junit.framework.TestCase;

import java.util.List;

import scala.actors.threadpool.Arrays;


public class PathCopyingPersistentTreeTest extends TestCase {
  public void testEmpty() {
    PathCopyingPersistentTree<String, String> tree = PathCopyingPersistentTree.of();
    System.out.println(tree.toString());
    assertTrue(tree.isEmpty());
  }

  public void testAdd() {
    @SuppressWarnings("unchecked")
    List<String> path1 = Arrays.asList(new String[]{
        "a", "x", "->", "b"
    });
    @SuppressWarnings("unchecked")
    List<String> path2 = Arrays.asList(new String[]{
        "a", "y", "->", "b"
    });

    PathCopyingPersistentTree<String, String> t0 = PathCopyingPersistentTree.of();
    assertTrue(t0.isEmpty());
    System.out.println(t0.toString());
    // insert an element
    PathCopyingPersistentTree<String, String> t1 = t0.setElementAndCopy(path1, "v1");
    assertFalse(t1.isEmpty());
    System.out.println(t1.toString());
    // modify an element and make a copy
    PathCopyingPersistentTree<String, String> t2 = t1.setElementAndCopy(path1, "v2");
    assertFalse(t2.isEmpty());
    System.out.println(t2.toString());
    // insert another element and make a copy
    PathCopyingPersistentTree<String, String> t3 = t1.setElementAndCopy(path2, "v3");
    assertFalse(t3.isEmpty());
    System.out.println(t3.toString());
  }

  /**
   * Remove Leaf element
   */
  public void testRemoveSubtree() {
    @SuppressWarnings("unchecked")
    List<String> prefix1 = Arrays.asList(new String[]{
        "a"
    });
    @SuppressWarnings("unchecked")
    List<String> prefix2 = Arrays.asList(new String[]{
        "a", "y"
    });
    @SuppressWarnings("unchecked")
    List<String> path1 = Arrays.asList(new String[]{
        "a", "x", "->", "b"
    });
    @SuppressWarnings("unchecked")
    List<String> path2 = Arrays.asList(new String[]{
        "a", "y", "->", "b"
    });
    @SuppressWarnings("unchecked")
    List<String> path3 = Arrays.asList(new String[]{
        "a", "y", "->", "c"
    });

    PathCopyingPersistentTree<String, String> t0 = PathCopyingPersistentTree.of();
    assertTrue(t0.isEmpty());
    System.out.println(t0.toString());
    PathCopyingPersistentTree<String, String> t1 = t0
        .setElementAndCopy(path1, "v1")
        .setElementAndCopy(path2, "v2")
        .setElementAndCopy(path3, "v3");
    System.out.println(t1.toString());
    // check element
    assertTrue(t1.get(path1).equals("v1"));
    assertTrue(t1.get(path2).equals("v2"));
    assertTrue(t1.get(path3).equals("v3"));
    // remove v3 from t1
    PathCopyingPersistentTree<String, String> t2 = t1.removeSubtreeAndCopy(path3);
    System.out.println(t2.toString());
    assertTrue(t2.get(path1).equals("v1"));
    assertTrue(t2.get(path2).equals("v2"));
    assertNull(t2.get(path3));
    // further, remove v2 from t1
    PathCopyingPersistentTree<String, String> t3 = t2.removeSubtreeAndCopy(path2);
    System.out.println(t3.toString());
    assertTrue(t3.get(path1).equals("v1"));
    assertNull(t3.get(path2));
    assertNull(t3.get(path3));
    // remove v2 and v3 from t1
    PathCopyingPersistentTree<String, String> t4 = t1.removeSubtreeAndCopy(prefix2);
    System.out.println(t4.toString());
    assertTrue(t4.get(path1).equals("v1"));
    assertNull(t4.get(path2));
    assertNull(t4.get(path3));
    // remove v1, v2 and v3 from t1
    PathCopyingPersistentTree<String, String> t5 = t1.removeSubtreeAndCopy(prefix1);
    System.out.println(t5.toString());
    assertNull(t5.get(path1));
    assertNull(t5.get(path2));
    assertNull(t5.get(path3));
  }

  /**
   * Remove Subtree
   */
  public void testRemoveElement() {
    @SuppressWarnings({"unchecked", "unused"})
    List<String> prefix1 = Arrays.asList(new String[]{
        "a"
    });
    @SuppressWarnings({"unchecked", "unused"})
    List<String> prefix2 = Arrays.asList(new String[]{
        "a", "y"
    });
    @SuppressWarnings("unchecked")
    List<String> path1 = Arrays.asList(new String[]{
        "a", "x", "->", "b"
    });
    @SuppressWarnings("unchecked")
    List<String> path2 = Arrays.asList(new String[]{
        "a", "y", "->", "b"
    });
    @SuppressWarnings("unchecked")
    List<String> path3 = Arrays.asList(new String[]{
        "a", "y", "->", "c"
    });

    PathCopyingPersistentTree<String, String> t0 = PathCopyingPersistentTree.of();
    assertTrue(t0.isEmpty());
    System.out.println(t0.toString());
    PathCopyingPersistentTree<String, String> t1 = t0
        .setElementAndCopy(path1, "v1")
        .setElementAndCopy(path2, "v2")
        .setElementAndCopy(path3, "v3");
    System.out.println(t1.toString());
    // check element
    assertTrue(t1.get(path1).equals("v1"));
    assertTrue(t1.get(path2).equals("v2"));
    assertTrue(t1.get(path3).equals("v3"));
    // remove v1 from t1, unnecessary branch should be removed
    PathCopyingPersistentTree<String, String> tx = t1.removeElementAndCopy(path1);
    System.out.println(tx.toString());
    assertNull(tx.get(path1));
    assertTrue(tx.get(path2).equals("v2"));
    assertTrue(tx.get(path3).equals("v3"));
  }

  public void testEqualsAndHashcode() {
    @SuppressWarnings("unchecked")
    List<String> path1 = Arrays.asList(new String[]{
        "a", "x", "->", "b"
    });
    @SuppressWarnings("unchecked")
    List<String> path2 = Arrays.asList(new String[]{
        "a", "y", "->", "b"
    });
    @SuppressWarnings("unchecked")
    List<String> path3 = Arrays.asList(new String[]{
        "a", "y", "->", "c"
    });

    PathCopyingPersistentTree<String, String> t0 = PathCopyingPersistentTree.of();
    assertTrue(t0.isEmpty());
    System.out.println(t0.toString());
    PathCopyingPersistentTree<String, String> t1 = t0
        .setElementAndCopy(path1, "v1")
        .setElementAndCopy(path2, "v2")
        .setElementAndCopy(path3, "v3");
    System.out.println(t1.toString());
    PathCopyingPersistentTree<String, String> t2 = t0
        .setElementAndCopy(path1, "v1")
        .setElementAndCopy(path2, "v2")
        .setElementAndCopy(path3, "v3");
    System.out.println(t2.toString());
    PathCopyingPersistentTree<String, String> tv1 = t2.setElementAndCopy(path3, "vx");
    PathCopyingPersistentTree<String, String> tv2 = t2.removeElementAndCopy(path3);
    PathCopyingPersistentTree<String, String> tv3 = t2.setSubtreeAndCopy(path3, "vx");
    PathCopyingPersistentTree<String, String> tv4 = t2.removeSubtreeAndCopy(path3);
    // check equals
    assertEquals(t1, t2);
    assertFalse(t1.equals(tv1));
    assertFalse(t1.equals(tv2));
    assertFalse(t1.equals(tv3));
    assertFalse(t1.equals(tv4));
    // check hashcode
    assertEquals(t1.hashCode(), t2.hashCode());
    assertFalse(t1.hashCode() == tv1.hashCode());
    assertFalse(t1.hashCode() == tv2.hashCode());
    assertFalse(t1.hashCode() == tv3.hashCode());
    assertFalse(t1.hashCode() == tv4.hashCode());
  }
}
