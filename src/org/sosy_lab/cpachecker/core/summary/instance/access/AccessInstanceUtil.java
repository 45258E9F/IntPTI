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
package org.sosy_lab.cpachecker.core.summary.instance.access;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.AccessPathBuilder;
import org.sosy_lab.cpachecker.util.collections.preliminary.Presence;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree.PersistentTreeNode;
import org.sosy_lab.cpachecker.util.collections.tree.TreeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class AccessInstanceUtil {

  private static AccessInstanceUtil instance;

  private AccessInstanceUtil() {
  }

  public static AccessInstanceUtil getInstance() {
    if (instance == null) {
      instance = new AccessInstanceUtil();
    }
    return instance;
  }

  /**
   * build accessResult from read/write tree;
   *
   * @param pReadTree  access paths to be read
   * @param pWriteTree access paths to be written
   * @return access summary result
   */
  public AccessResult buildResult(
      PathCopyingPersistentTree<String, Presence> pReadTree,
      PathCopyingPersistentTree<String, Presence> pWriteTree) {
    List<AccessPath> readPaths = getAccessPath(pReadTree);
    List<AccessPath> writePaths = getAccessPath(pWriteTree);
    return AccessResult.of(readPaths, writePaths);
  }

  private List<AccessPath> getAccessPath(PathCopyingPersistentTree<String, Presence> tree) {
    final List<List<String>> paths = Lists.newArrayList();
    tree.traverse(new TreeVisitor<String, Presence>() {
      @Override
      public TreeVisitStrategy visit(Stack<String> path, Presence element, boolean isLeaf) {
        if (!path.isEmpty() && path.peek().equals("*")) {
          List<String> p = new ArrayList<>(path);
          if (isLeaf) {
            paths.add(p);
            return TreeVisitStrategy.SKIP;
          } else {
            return TreeVisitStrategy.CONTINUE;
          }
        } else {
          if (isLeaf) {
            List<String> p = new ArrayList<>(path);
            paths.add(p);
          }
          return TreeVisitStrategy.CONTINUE;
        }
      }
    });

    List<AccessPath> result = Lists.newArrayList();
    for (List<String> p : paths) {
      AccessPath ap = AccessPathBuilder.getInstance().buildAccessPath(p);
      if (ap != null) {
        result.add(ap);
      }
    }
    return result;
  }

  public boolean isLessOrEqual(
      AccessFunctionInstance pThis,
      AccessFunctionInstance pThat) {
    return (lessOrEqualTree(pThis.readTree, pThat.readTree)
        && lessOrEqualTree(pThis.writeTree, pThat.writeTree));
  }

  public boolean isLessOrEqual(AccessLoopInstance pThis, AccessLoopInstance pThat) {
    return (lessOrEqualTree(pThis.readTree, pThat.readTree)
        && lessOrEqualTree(pThis.writeTree, pThat.writeTree));
  }

  private boolean lessOrEqualTree(
      PathCopyingPersistentTree<String, Presence> a,
      PathCopyingPersistentTree<String, Presence> b) {
    return lessOrEqualTreeNode(a.getRoot(), b.getRoot());
  }

  private boolean lessOrEqualTreeNode(
      PersistentTreeNode<String, Presence> a,
      PersistentTreeNode<String, Presence> b) {
    if (a == null) {
      return true;
    } else if (b == null) {
      return false;
    } else {
      // a != null && b != null
      if (b.getElement() != null) {
        // in this case, b is a leaf node with
        return true;
      } else if (a.getElement() != null) {
        return false;
      } else {
        // compare children
        Set<String> as = a.branches();
        Set<String> bs = a.branches();
        if (bs.containsAll(as)) {
          for (String s : as) {
            if (!lessOrEqualTreeNode(a.getChild(s), b.getChild(s))) {
              return false;
            }
          }
          return true;
        } else {
          return false;
        }
      }
    }
  }

  public boolean isEqual(
      AccessFunctionInstance pThis,
      AccessFunctionInstance pThat) {
    return isTreeEqual(pThis.readTree, pThat.readTree) && isTreeEqual(pThis.writeTree,
        pThat.writeTree);
  }

  public boolean isEqual(AccessLoopInstance pThis, AccessLoopInstance pThat) {
    return isTreeEqual(pThis.readTree, pThat.readTree) && isTreeEqual(pThis.writeTree,
        pThat.writeTree);
  }

  private boolean isTreeEqual(
      PathCopyingPersistentTree<String, Presence> t1,
      PathCopyingPersistentTree<String, Presence> t2) {
    return Objects.equal(t1, t2);
  }


}
