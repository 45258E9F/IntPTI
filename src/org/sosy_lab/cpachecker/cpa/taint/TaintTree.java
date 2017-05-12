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
package org.sosy_lab.cpachecker.cpa.taint;

import org.sosy_lab.cpachecker.cpa.taint.TaintState.Taint;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.ArrayAccessSegment;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.annotation.Nonnull;

public class TaintTree extends PathCopyingPersistentTree<String, Taint> {

  public TaintTree() {
    super();
  }

  public TaintTree(PathCopyingPersistentTree<String, Taint> pTree) {
    super(pTree.getRoot());
  }

  /**
   * Set the taint status of a given access path
   * For example, a.[3].f.[].f2 is an access path. The update of taint tree should be in the
   * from-bottom-to-top manner. When we update [3], we should update the corresponding []. When
   * we update [], all [*] subtree should be removed.
   *
   * @param path         an access path
   * @param taint        specified taint status
   * @param overrideMode whether the target subtree should be overridden
   * @return the updated taint tree
   */
  public TaintTree setTaint(AccessPath path, @Nonnull Taint taint, boolean overrideMode) {
    PersistentTreeNode<String, Taint> node = root;
    List<String> segments = AccessPath.toStrList(path);
    List<PathSegment> pathSegments = path.path();
    assert (segments.size() == pathSegments.size());
    Stack<PersistentTreeNode<String, Taint>> trace = new Stack<>();
    trace.push(root);
    for (String segment : segments) {
      if (node == null) {
        trace.push(null);
      } else {
        node = node.getChild(segment);
        trace.push(node);
      }
    }
    trace.pop();
    PersistentTreeNode<String, Taint> top;
    if (node == null) {
      top = new PersistentTreeNode<>(taint);
    } else {
      Taint newTaint = node.getElement();
      if (newTaint != Taint.TAINT || path.isDeterminedAccessPath()) {
        newTaint = taint;
      }
      if (overrideMode) {
        // subtree are pruned
        top = new PersistentTreeNode<>(newTaint);
      } else {
        top = node.setElementAndCopy(newTaint);
      }
    }
    int idx = segments.size() - 1;
    while (idx >= 0) {
      String currentSegment = segments.get(idx);
      PathSegment pathSegment = pathSegments.get(idx);
      node = trace.pop();
      if (node == null) {
        Map<String, PersistentTreeNode<String, Taint>> children = new HashMap<>();
        children.put(currentSegment, top);
        top = new PersistentTreeNode<>(null, children);
      } else {
        if (pathSegment instanceof ArrayConstIndexSegment) {
          PersistentTreeNode<String, Taint> newNode = node.setChildAndCopy(currentSegment, top);
          PersistentTreeNode<String, Taint> undetNode = node.getChild(ArrayAccessSegment.INSTANCE
              .getName());
          undetNode = PathCopyingPersistentTree.mergeNode(top, undetNode, treeMerger);
          newNode = newNode.setChildAndCopy(ArrayAccessSegment.INSTANCE.getName(), undetNode);
          top = newNode;
        } else if (pathSegment instanceof ArrayAccessSegment) {
          // other array subscripts are discarded, directly
          Taint element = node.getElement();
          Map<String, PersistentTreeNode<String, Taint>> children = new HashMap<>();
          children.put(ArrayAccessSegment.INSTANCE.getName(), top);
          top = new PersistentTreeNode<>(element, children);
        } else {
          // ordinary case
          top = node.setChildAndCopy(currentSegment, top);
        }
      }
      idx--;
    }
    return new TaintTree(new PathCopyingPersistentTree<>(top));
  }

  public TaintTree setSubTaintTree(
      AccessPath path, PersistentTreeNode<String, Taint> treeNode) {
    PersistentTreeNode<String, Taint> node = root;
    if (treeNode == null) {
      return this;
    }
    List<String> segments = AccessPath.toStrList(path);
    List<PathSegment> pathSegments = path.path();
    assert (segments.size() == pathSegments.size());
    Stack<PersistentTreeNode<String, Taint>> trace = new Stack<>();
    trace.push(root);
    for (String segment : segments) {
      if (node == null) {
        trace.push(null);
      } else {
        node = node.getChild(segment);
        trace.push(node);
      }
    }
    trace.pop();
    PersistentTreeNode<String, Taint> top = treeNode;
    int idx = segments.size() - 1;
    while (idx >= 0) {
      String currentSegment = segments.get(idx);
      PathSegment pathSegment = pathSegments.get(idx);
      node = trace.pop();
      if (node == null) {
        Map<String, PersistentTreeNode<String, Taint>> children = new HashMap<>();
        children.put(currentSegment, top);
        // we do not know how to specify a specific taint value here
        top = new PersistentTreeNode<>(null, children);
      } else {
        if (pathSegment instanceof ArrayConstIndexSegment) {
          PersistentTreeNode<String, Taint> newNode = node.setChildAndCopy(currentSegment, top);
          PersistentTreeNode<String, Taint> undetNode = node.getChild(ArrayAccessSegment.INSTANCE
              .getName());
          undetNode = PathCopyingPersistentTree.mergeNode(top, undetNode, treeMerger);
          newNode = newNode.setChildAndCopy(ArrayAccessSegment.INSTANCE.getName(), undetNode);
          top = newNode;
        } else if (pathSegment instanceof ArrayAccessSegment) {
          // other array subscripts are discarded, directly
          Taint element = node.getElement();
          Map<String, PersistentTreeNode<String, Taint>> children = new HashMap<>();
          children.put(ArrayAccessSegment.INSTANCE.getName(), top);
          top = new PersistentTreeNode<>(element, children);
        } else {
          top = node.setChildAndCopy(currentSegment, top);
        }
      }
      idx--;
    }
    return new TaintTree(new PathCopyingPersistentTree<>(top));
  }

  public TaintTree removeTaintTree(AccessPath path) {
    PersistentTreeNode<String, Taint> node = root;
    List<String> segments = AccessPath.toStrList(path);
    List<PathSegment> pathSegments = path.path();
    assert (segments.size() == pathSegments.size());
    Stack<PersistentTreeNode<String, Taint>> trace = new Stack<>();
    trace.push(root);
    for (String segment : segments) {
      if (node == null) {
        trace.push(null);
      } else {
        node = node.getChild(segment);
        trace.push(node);
      }
    }
    trace.pop();
    PersistentTreeNode<String, Taint> top = null;
    int idx = segments.size() - 1;
    while (idx >= 0) {
      String currentSegment = segments.get(idx);
      PathSegment pathSegment = pathSegments.get(idx);
      node = trace.pop();
      // if node is NULL, then top must be NULL (=>)
      if (node != null) {
        PersistentTreeNode<String, Taint> parent;
        if (top == null) {
          if (pathSegment instanceof ArrayAccessSegment) {
            parent = null;
          } else if (pathSegment instanceof ArrayConstIndexSegment) {
            parent = node.removeChildAndCopy(currentSegment);
            // merge other children to the undetermined index branch
            Collection<PersistentTreeNode<String, Taint>> values = node.values();
            PersistentTreeNode<String, Taint> initial = new PersistentTreeNode<>();
            for (PersistentTreeNode<String, Taint> value : values) {
              initial = PathCopyingPersistentTree.mergeNode(initial, value, treeMerger);
            }
            parent = parent.setChildAndCopy(ArrayAccessSegment.INSTANCE.getName(), initial);
          } else {
            parent = node.removeChildAndCopy(currentSegment);
          }
        } else {
          if (pathSegment instanceof ArrayAccessSegment) {
            Map<String, PersistentTreeNode<String, Taint>> children = new HashMap<>();
            children.put(ArrayAccessSegment.INSTANCE.getName(), top);
            Taint element = node.getElement();
            parent = new PersistentTreeNode<>(element, children);
          } else {
            // an approximation: we do not update the [] node, correspondingly
            // otherwise, we should carefully handle constant array subscript and field access cases
            parent = node.setChildAndCopy(currentSegment, top);
          }
        }
        top = parent;
      }
      idx--;
    }
    return new TaintTree(new PathCopyingPersistentTree<>(top));
  }

  /**
   * Query taint status of a given access path
   *
   * @param path an access path
   * @return the taint status, TAINT or CLEAN
   */
  public Taint queryTaintInfo(AccessPath path) {
    List<String> segments = AccessPath.toStrList(path);
    List<Taint> trace = trace(segments);
    for (int idx = trace.size() - 1; idx >= 0; idx--) {
      Taint currentTaint = trace.get(idx);
      if (currentTaint != null) {
        return currentTaint;
      }
    }
    // should this case occur?
    return Taint.CLEAN;
  }

  private static final TaintTreeMerger treeMerger = new TaintTreeMerger();

  private static class TaintTreeMerger implements MergeAdvisor<String, Taint> {
    @Override
    public Pair<Taint, Boolean> merge(
        @Nonnull Taint a, @Nonnull Taint b) {
      if (a == Taint.TAINT || b == Taint.TAINT) {
        return Pair.of(Taint.TAINT, true);
      }
      return Pair.of(Taint.CLEAN, true);
    }
  }

}
