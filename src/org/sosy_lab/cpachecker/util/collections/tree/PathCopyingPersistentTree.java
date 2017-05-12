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

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.collections.tree.TreeVisitor.TreeVisitStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.annotation.Nonnull;

/**
 * Persistent tree implementation
 * S: segment of access path
 * E: element, leaf element should not be null
 *
 * Invariant: every non-null node should contain at least one non-null element in its subtree
 */
public class PathCopyingPersistentTree<S extends Comparable<? super S>, E> {

  /**
   * Immutable tree node
   *
   * The content is never changed. Always produce a new instance if changing of content is required
   *
   * S: segment of access path
   * E: element
   */
  public static class PersistentTreeNode<S extends Comparable<? super S>, E> {
    private final PersistentMap<S, PersistentTreeNode<S, E>> children;
    private final E element;

    public PersistentTreeNode() {
      this.element = null;
      this.children = null;
    }

    public PersistentTreeNode(E element) {
      this.element = element;
      this.children = null;
    }

    public PersistentTreeNode(E element, Map<S, PersistentTreeNode<S, E>> children) {
      this.element = element;
      this.children = (children == null) ? null :
                      (children instanceof PersistentMap) ? (PersistentMap<S,
                          PersistentTreeNode<S, E>>) children : PathCopyingPersistentTreeMap
                          .copyOf(children);
    }

    public E getElement() {
      return element;
    }

    public Set<S> getKeys() {
      if (children != null) {
        return children.keySet();
      } else {
        return Sets.newHashSet();
      }
    }

    public PersistentTreeNode<S, E> getChild(S seg) {
      return (children == null) ? null : children.get(seg);
    }

    public boolean isLeaf() {
      return (children == null || children.size() == 0);
    }

    /**
     * May return null if this node is removed by this operation
     *
     * @param seg segment
     * @return new node
     */
    public PersistentTreeNode<S, E> removeChildAndCopy(S seg) {
      if (children != null && children.containsKey(seg)) {
        PersistentTreeNode<S, E> newNode =
            new PersistentTreeNode<>(element, children.removeAndCopy(seg));
        return (newNode.isEmpty()) ? null : newNode;
      } else {
        // nothing changed
        return this;
      }
    }

    public PersistentTreeNode<S, E> setChildAndCopy(S seg, PersistentTreeNode<S, E> child) {
      if (children == null) {
        Map<S, PersistentTreeNode<S, E>> m = Maps.newHashMap();
        m.put(seg, child);
        return new PersistentTreeNode<>(element, m);
      } else {
        return new PersistentTreeNode<>(element, children.putAndCopy(seg, child));
      }
    }

    public PersistentTreeNode<S, E> setElementAndCopy(E element) {
      return new PersistentTreeNode<>(element, children);
    }

    public PersistentTreeNode<S, E> setChildrenAndCopy(Map<S, PersistentTreeNode<S, E>> children) {
      return new PersistentTreeNode<>(element, children);
    }

    public boolean isEmpty() {
      return element == null && (children == null || children.size() == 0);
    }

    public Set<S> branches() {
      return (children == null) ? new HashSet<S>() : children.keySet();
    }

    public Collection<PersistentTreeNode<S, E>> values() {
      return (children == null) ? new ArrayList<PersistentTreeNode<S, E>>() : children.values();
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      strPlainRepr(sb, new Stack<String>());
      return sb.toString();
    }

    public void strPlainRepr(StringBuilder sb, Stack<String> prefix) {
      // [v0]
      // [v0.v1] = s1
      // [v0.v1.v3] = s3
      // [v0.v1.v4] = s4
      // [v0.v2] = s2
      if (element != null) {
        sb.append('[');
        sb.append(Joiner.on(" | ").join(prefix));
        sb.append(']');
        sb.append(" = ");
        sb.append(element);
        sb.append('\n');
      }
      if (children != null) {
        for (Map.Entry<S, PersistentTreeNode<S, E>> entry : children.entrySet()) {
          PersistentTreeNode<S, E> child = entry.getValue();
          prefix.push(entry.getKey().toString());
          child.strPlainRepr(sb, prefix);
          prefix.pop();
        }
      }
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || !(o instanceof PersistentTreeNode<?, ?>)) {
        return false;
      } else {
        PersistentTreeNode<?, ?> that = (PersistentTreeNode<?, ?>) o;
        return (Objects.equal(this.element, that.element) && Objects
            .equal(this.children, that.children));
      }
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(element, children);
    }
  }

  @SuppressWarnings("rawtypes")
  private static final PathCopyingPersistentTree EMPTY_TREE =
      new PathCopyingPersistentTree<>();

  public PathCopyingPersistentTree() {
  }

  public PathCopyingPersistentTree(PersistentTreeNode<S, E> root) {
    this.root = root;
  }

  @SuppressWarnings("unchecked")
  public static <S extends Comparable<? super S>, E> PathCopyingPersistentTree<S, E> of() {
    return (PathCopyingPersistentTree<S, E>) EMPTY_TREE;
  }

  protected PersistentTreeNode<S, E> root = null;

  /**
   * Look up for an element at a specific path
   *
   * @param path the path
   * @return element at the path, may be null
   */
  public PersistentTreeNode<S, E> lookup(List<S> path) {
    PersistentTreeNode<S, E> node = root;
    Iterator<S> iterator = path.iterator();
    while (node != null && iterator.hasNext()) {
      S seg = iterator.next();
      node = node.getChild(seg);
    }
    return node;
  }

  /**
   * nodes along the path, root is always the first
   *
   * @param path : the path
   * @return nonnull, the first element is always root (which may be null)
   */
  public List<E> trace(List<S> path) {
    List<E> ret = new ArrayList<>(path.size() + 1);
    ret.add(getElement(root));
    PersistentTreeNode<S, E> node = root;
    for (S seg : path) {
      node = getChild(node, seg);
      ret.add(getElement(node));
    }
    return ret;
  }

  private E getElement(PersistentTreeNode<S, E> node) {
    return (node == null) ? null : node.element;
  }

  private PersistentTreeNode<S, E> getChild(PersistentTreeNode<S, E> node, S seg) {
    return (node == null) ? null : node.getChild(seg);
  }

  public E get(List<S> path) {
    PersistentTreeNode<S, E> node = lookup(path);
    if (node == null) {
      return null;
    } else {
      return node.getElement();
    }
  }

  /**
   * Set a subtree
   *
   * @param path    : access path
   * @param subtree :
   * @return new tree
   */
  private PathCopyingPersistentTree<S, E> setSubtree(
      List<S> path,
      PersistentTreeNode<S, E> subtree) {
    Preconditions.checkNotNull(subtree, "sub tree should not be null.");

    PersistentTreeNode<S, E> node = root;
    Stack<PersistentTreeNode<S, E>> trace = new Stack<>();
    trace.push(root);
    for (S seg : path) {
      if (node == null) {
        trace.push(null);
      } else {
        node = node.getChild(seg);
        trace.push(node);
      }
    }
    PersistentTreeNode<S, E> top = subtree;
    trace.pop();  // pop out the last element (which is at the same position as subtree)
    int idx = path.size() - 1;
    while (!trace.isEmpty()) {
      S seg = path.get(idx);
      node = trace.pop();
      if (node == null) {
        // create new node
        Map<S, PersistentTreeNode<S, E>> children = new HashMap<>();
        children.put(seg, top);
        top = new PersistentTreeNode<>(null, children);
      } else {
        // merge with existing node
        top = node.setChildAndCopy(seg, top);
      }
      idx--;
    }
    return new PathCopyingPersistentTree<>(top);
  }

  private PathCopyingPersistentTree<S, E> setSubtreeAndNullifyPath(
      List<S> path, PersistentTreeNode<S, E> subtree) {
    Preconditions.checkNotNull(subtree, "sub tree should not be null.");

    PersistentTreeNode<S, E> node = root;
    Stack<PersistentTreeNode<S, E>> trace = new Stack<>();
    trace.push(root);
    for (S seg : path) {
      if (node == null) {
        trace.push(null);
      } else {
        node = node.getChild(seg);
        trace.push(node);
      }
    }
    PersistentTreeNode<S, E> top = subtree;
    trace.pop();  // pop out the last element (which is at the same position as subtree)
    int idx = path.size() - 1;
    while (!trace.isEmpty()) {
      S seg = path.get(idx);
      node = trace.pop();
      if (node == null) {
        // create new node
        Map<S, PersistentTreeNode<S, E>> children = new HashMap<>();
        children.put(seg, top);
        top = new PersistentTreeNode<>(null, children);
      } else {
        // merge with existing node
        top = node.setChildAndCopy(seg, top).setElementAndCopy(null);
      }
      idx--;
    }
    return new PathCopyingPersistentTree<>(top);
  }

  /**
   * Remove the subtree at a given path
   *
   * @param path : access path
   * @return new tree
   */
  public PathCopyingPersistentTree<S, E> removeSubtreeAndCopy(List<S> path) {
    PersistentTreeNode<S, E> node = root;
    Stack<PersistentTreeNode<S, E>> trace = new Stack<>();
    trace.push(root);
    for (S seg : path) {
      if (node == null) {
        // removing a branch that does not exists, which does not change anything.
        return this;
      } else {
        node = node.getChild(seg);
        trace.push(node);
      }
    }
    // all nodes are non-null in the trace
    PersistentTreeNode<S, E> top = null;
    trace.pop();  // pop out the last element (which is at the same position as subtree)
    int idx = path.size() - 1;
    while (!trace.isEmpty()) {
      S seg = path.get(idx);
      node = trace.pop();
      // FIXME: a possible null-dereference defect here?
      if (top == null && node.isEmpty()) {
        // does nothing
        // skip the empty node along the path
      } else {
        // merge with existing node
        PersistentTreeNode<S, E> parent;
        if (top == null) {
          parent = node.removeChildAndCopy(seg);
        } else {
          parent = node.setChildAndCopy(seg, top);
        }
        top = parent;
      }
      idx--;
    }
    return new PathCopyingPersistentTree<>(top);
  }

  /**
   * Modify the element at a given path and remove its subtree
   *
   * @param path : access path
   * @return : new tree
   */
  public PathCopyingPersistentTree<S, E> setSubtreeAndCopy(
      List<S> path,
      PathCopyingPersistentTree<S, E> subtree) {
    return subtree.isEmpty() ? this : setSubtree(path, subtree.root);
  }

  /**
   * Modify the element at a given path and remove its subtree
   *
   * @param path : access path
   * @return : new tree
   */
  public PathCopyingPersistentTree<S, E> setSubtreeAndCopy(
      List<S> path,
      PersistentTreeNode<S, E> subtree) {
    return subtree.isEmpty() ? this : setSubtree(path, subtree);
  }

  /**
   * Modify the element at a given path and remove its subtree
   *
   * @param path    : access path
   * @param element : new element, should not be null
   * @return : new tree
   */
  public PathCopyingPersistentTree<S, E> setSubtreeAndCopy(List<S> path, @Nonnull E element) {
    PersistentTreeNode<S, E> subtree = lookup(path);
    if (subtree == null) {
      return create(path, element);
    } else {
      return setSubtree(path, new PersistentTreeNode<S, E>(element));
    }
  }

  /**
   * By calling this method, only leaf node has non-null element.
   */
  public PathCopyingPersistentTree<S, E> setSubtreeAndNullifyPath(
      List<S> path, @Nonnull E element) {
    PersistentTreeNode<S, E> subtree = lookup(path);
    if (subtree == null) {
      return create(path, element);
    } else {
      return setSubtreeAndNullifyPath(path, new PersistentTreeNode<S, E>(element));
    }
  }

  /**
   * Remove the element at a given path
   * While the removed element is leaf, remove unnecessary branches that leads to it.
   *
   * @param path : access path
   * @return : new tree
   */
  public PathCopyingPersistentTree<S, E> removeElementAndCopy(List<S> path) {
    PersistentTreeNode<S, E> subtree = lookup(path);
    if (subtree == null) {
      // the path does not exist
      return this;
    } else if (subtree.isLeaf()) {
      return removeSubtreeAndCopy(path);
    } else {
      // shrink is not needed because it has non-null descents
      return setSubtree(path, subtree.setElementAndCopy(null));
    }
  }

  /**
   * Modify the element at a given path while preserving subtrees
   *
   * @param path    : access path
   * @param element : should not be null
   * @return : new tree
   */
  public PathCopyingPersistentTree<S, E> setElementAndCopy(List<S> path, @Nonnull E element) {
    PersistentTreeNode<S, E> subtree = lookup(path);
    if (subtree == null) {
      return create(path, element);
    } else {
      return setSubtree(path, subtree.setElementAndCopy(element));
    }
  }

  /**
   * Precondition: no element at the given path
   *
   * @param path    : access path
   * @param element : element
   * @return : new tree
   */
  private PathCopyingPersistentTree<S, E> create(List<S> path, E element) {
    return setSubtree(path, new PersistentTreeNode<S, E>(element));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (root == null) {
      sb.append("[]");
    } else {
      root.strPlainRepr(sb, new Stack<String>());
    }
    return sb.toString();
  }

  @SuppressWarnings("unused")
  private void strRepr(StringBuilder sb, int indent, PersistentTreeNode<S, E> node, S inedge) {
    // [v0]
    //   [v1] <- s1
    //     [v3] <- s3
    //     [v4] <- s4
    //   [v2] <- s2
    for (int i = 0; i < indent; i++) {
      sb.append(' ');
    }
    sb.append('[');
    if (node.element != null) {
      sb.append(node.element);
    }
    sb.append(']');
    if (inedge != null) {
      sb.append(" <- ");
      sb.append('(');
      sb.append(inedge);
      sb.append(')');
    }
    sb.append('\n');
    if (node.children != null) {
      for (Map.Entry<S, PersistentTreeNode<S, E>> entry : node.children.entrySet()) {
        PersistentTreeNode<S, E> child = entry.getValue();
        strRepr(sb, indent + 2, child, entry.getKey());
      }
    }
  }

  public boolean isEmpty() {
    return root == null || root.isEmpty();
  }

  public PersistentTreeNode<S, E> getSubtreeRoot(List<S> path) {
    return lookup(path);
  }

  public PathCopyingPersistentTree<S, E> getSubtree(List<S> path) {
    return new PathCopyingPersistentTree<>(lookup(path));
  }

  public static <S extends Comparable<? super S>, E> PathCopyingPersistentTree<S, E> wrap(
      List<S> path, PathCopyingPersistentTree<S, E> subtree) {
    PathCopyingPersistentTree<S, E> tree = new PathCopyingPersistentTree<>();
    if (subtree.isEmpty()) {
      return tree;
    } else {
      return tree.setSubtreeAndCopy(path, subtree);
    }
  }

  public void traverse(TreeVisitor<S, E> visitor) {
    Stack<S> path = new Stack<>();
    if (root != null) {
      traverse(root, path, visitor);
    }
  }

  private TreeVisitStrategy traverse(
      PersistentTreeNode<S, E> node,
      Stack<S> path,
      TreeVisitor<S, E> visitor) {
    TreeVisitStrategy tvs = visitor.visit(path, node.getElement(), node.isLeaf());
    if (tvs == TreeVisitStrategy.ABORT) {
      return TreeVisitStrategy.ABORT;
    } else if (tvs == TreeVisitStrategy.SKIP) {
      // skip visiting the descents
      // but continue on other branches
      return TreeVisitStrategy.CONTINUE;
    } else {
      // CONTINUE
      if (node.children != null) {
        for (Map.Entry<S, PersistentTreeNode<S, E>> entry : node.children.entrySet()) {
          path.push(entry.getKey());
          TreeVisitStrategy tvsp = traverse(entry.getValue(), path, visitor);
          path.pop();
          if (tvsp == TreeVisitStrategy.ABORT) {
            return TreeVisitStrategy.ABORT;
          }
        }
      }
      return TreeVisitStrategy.CONTINUE;
    }
  }

  public PathCopyingPersistentTree<S, E> move(List<S> oldPath, List<S> newPath) {
    PersistentTreeNode<S, E> subtree = getSubtreeRoot(oldPath);
    if (subtree == null) {
      // does not exist, no modification
      return this;
    } else {
      return this.removeSubtreeAndCopy(oldPath).setSubtreeAndCopy(newPath, subtree);
    }
  }

  /**
   * @return May be null
   */
  public PersistentTreeNode<S, E> getRoot() {
    return root;
  }

  @Override
  public boolean equals(Object o) {
    return !(o == null || !(o instanceof PathCopyingPersistentTree<?, ?>)) &&
        Objects.equal(this.root, ((PathCopyingPersistentTree<?, ?>) o).root);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.root);
  }

  public interface MergeAdvisor<S, E> {
    /*
     * Pair<E, Boolean>
     * E: the merged value
     * Boolean: whether sub tree should also be merged
     *
     * Ensures: both a and b are nonnull
     */
    Pair<E, Boolean> merge(@Nonnull E a, @Nonnull E b);
  }

  /**
   * Merge a tree
   * (1) if one of the tree is empty, the other tree is used directly
   * (2) we use an advisor to guide the merging process
   */
  public static <S extends Comparable<? super S>, E>
  PathCopyingPersistentTree<S, E> merge(
      PathCopyingPersistentTree<S, E> a,
      PathCopyingPersistentTree<S, E> b,
      MergeAdvisor<S, E> advisor
  ) {
    if (a.isEmpty()) {
      return b;
    } else if (b.isEmpty()) {
      return a;
    } else {
      // both of them are non-empty
      return new PathCopyingPersistentTree<>(mergeNode(a.root, b.root, advisor));
    }
  }

  /**
   * Precondition: a, b should not be empty at the same time
   */
  protected static <S extends Comparable<? super S>, E>
  PersistentTreeNode<S, E> mergeNode(
      PersistentTreeNode<S, E> a,
      PersistentTreeNode<S, E> b,
      MergeAdvisor<S, E> advisor
  ) {
    // empty node is merged directly without asking the advisor
    if (a == null) {
      return b;
    } else if (b == null) {
      return a;
    } else {
      // both a, b are not null
      E valueA = a.getElement();
      E valueB = b.getElement();
      E value;
      boolean shouldMerge;
      if (valueA == null || valueB == null) {
        value = null;
        shouldMerge = true;
      } else {
        Pair<E, Boolean> advice = advisor.merge(valueA, valueB);
        value = advice.getFirst();
        shouldMerge = advice.getSecondNotNull();
      }
      if (shouldMerge) {
        // continue to merge children
        Set<S> segs = ImmutableSet.<S>builder()
            .addAll(a.branches())
            .addAll(b.branches())
            .build();
        Map<S, PersistentTreeNode<S, E>> children = new HashMap<>();
        for (S s : segs) {
          children.put(s, mergeNode(a.getChild(s), b.getChild(s), advisor));
        }
        return new PersistentTreeNode<>(value, children);
      } else {
        // Ensure: if we reach here, value should not be NULL
        return new PersistentTreeNode<>(value);
      }
    }
  }

  public static class OverrideMerger<S, E> implements MergeAdvisor<S,
      E> {
    @Override
    public Pair<E, Boolean> merge(@Nonnull E a, @Nonnull E b) {
      return Pair.of(b, true);
    }
  }

  /**
   * Before comparing less equal, make sure the isEqual method of E is well-defined
   */
  public static abstract class TreeLessEqualComparator<S extends Comparable<? super S>, E> {
    /**
     * Default order is null value less than nonnull
     */
    public boolean nullValueFirst() {
      return true;
    }

    /**
     * Sort the children by the path segment
     * Then the comparison is performed accordingly.
     *
     * @return If the return value is null, the comparison does not rely on the order of children
     * I.e., the order in which children are compared is arbitrary.
     */
    @SuppressWarnings("unused")
    protected List<S> sortPathSegment(Collection<S> pathSegments) {
      return null;
    }

    /**
     * Compares 2 elements, requires partial order
     * use isEqual to determine whether two elements are equal
     */
    abstract protected boolean isLessEqual(@Nonnull E a, @Nonnull E b);

    public boolean safeIsLessEqual(E a, E b) {
      if (a == null && b == null) {
        return true;
      } else if (a == null) {
        return nullValueFirst();
      } else if (b == null) {
        return !nullValueFirst();
      } else {
        return isLessEqual(a, b);
      }
    }
  }

  public static abstract class TreeComparator<S extends Comparable<? super S>, E>
      extends TreeLessEqualComparator<S, E> {
    /**
     * Compares 2 elements, requires total order
     */
    abstract protected int compare(@Nonnull E a, @Nonnull E b);

    @Override
    public boolean isLessEqual(E a, E b) {
      return compare(a, b) <= 0;
    }

    public int safeCompare(E a, E b) {
      if (a == null && b == null) {
        return 0;
      } else if (a == null) {
        return nullValueFirst() ? -1 : 1;
      } else if (b == null) {
        return nullValueFirst() ? 1 : -1;
      } else {
        return compare(a, b);
      }
    }
  }

  public static <S extends Comparable<? super S>, E> int compare(
      PathCopyingPersistentTree<S, E> a,
      PathCopyingPersistentTree<S, E> b,
      TreeComparator<S, E> comparator
  ) {
    return compareNode(a.getRoot(), b.getRoot(), comparator);
  }

  private static <S extends Comparable<? super S>, E> int compareNode(
      PersistentTreeNode<S, E> a, PersistentTreeNode<S, E> b, TreeComparator<S, E> comparator) {
    if (a == null && b == null) {
      return 0;
    } else if (a == null) {
      return comparator.nullValueFirst() ? -1 : 1;  // subtree 'b' is non-empty
    } else if (b == null) {
      return comparator.nullValueFirst() ? 1 : -1;  // subtree 'a' is non-empty
    } else {
      // a != null && b != null
      E va = a.getElement();
      E vb = b.getElement();
      int elementOrder = comparator.safeCompare(va, vb);
      if (elementOrder == 0) {
        // compare children
        Set<S> segs = Sets.newHashSet();
        segs.addAll(a.branches());
        segs.addAll(b.branches());
        List<S> orderedSegments = comparator.sortPathSegment(segs);
        if (orderedSegments == null) {
          orderedSegments = new ArrayList<>(segs);  // use arbitrary order
        }
        for (S s : orderedSegments) {
          int childOrder = compareNode(a.getChild(s), b.getChild(s), comparator);
          if (childOrder != 0) {
            return childOrder;
          }
        }
        return 0;
      } else {
        return elementOrder;
      }
    }
  }

  public static <S extends Comparable<? super S>, E> boolean isLessEqual(
      PathCopyingPersistentTree<S, E> a,
      PathCopyingPersistentTree<S, E> b,
      TreeLessEqualComparator<S, E> comparator
  ) {
    return isLessEqualNode(a.getRoot(), b.getRoot(), comparator);
  }

  private static <S extends Comparable<? super S>, E> boolean isLessEqualNode(
      PersistentTreeNode<S, E> a,
      PersistentTreeNode<S, E> b,
      TreeLessEqualComparator<S, E> comparator) {
    if (a == null && b == null) {
      return true;
    } else if (a == null) {
      return comparator.nullValueFirst();   // subtree 'b' is non-empty
    } else if (b == null) {
      return !comparator.nullValueFirst();  // subtree 'a' is non-empty
    } else {
      // a != null && b != null
      E va = a.getElement();
      E vb = b.getElement();
      boolean elementLessEqual = comparator.safeIsLessEqual(va, vb);
      if (elementLessEqual) {
        if (Objects.equal(va, vb)) {
          return true;
        } else {
          // compare children
          Set<S> segs = Sets.newHashSet();
          segs.addAll(a.branches());
          segs.addAll(b.branches());
          List<S> orderedSegments = comparator.sortPathSegment(segs);
          if (orderedSegments == null) {
            orderedSegments = new ArrayList<>(segs);  // use arbitrary order
          }
          for (S s : orderedSegments) {
            boolean childLessEqual = isLessEqualNode(a.getChild(s), b.getChild(s), comparator);
            if (!childLessEqual) {
              return false;
            }
          }
          return true;
        }
      } else {
        return false;
      }
    }
  }
}