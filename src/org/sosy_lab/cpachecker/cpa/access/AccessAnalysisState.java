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
package org.sosy_lab.cpachecker.cpa.access;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.collections.preliminary.Presence;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree.PersistentTreeNode;
import org.sosy_lab.cpachecker.util.collections.tree.TreeVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * The state is a map: AccessPath -> AccessMode
 *
 * For all access paths in the state, no one could be prefix of another (because its subsumed).
 *
 * When used as summary, the arguments is named with $1, $2, $3, etc.
 */
public class AccessAnalysisState
    implements LatticeAbstractState<AccessAnalysisState>, FunctionSummary, Graphable {
  private static final AccessAnalysisState EMPTY = new AccessAnalysisState();

  public PathCopyingPersistentTree<String, Presence> readTree;
  public PathCopyingPersistentTree<String, Presence> writeTree;

  /*
   * tomgu
   * TODO for summary phase, record loop state
   * for each edge, we compute the function level
   * then if edge in loop, we compute again using loopState
   * Therefore, (1) when we join, we need join map (2) when we equal need map
   * (3) when we lessorEqual, we do not need. for each edge has also in function level

  public Map<Loop, AccessAnalysisState> loopStateMap = Maps.newHashMap();

  public AccessAnalysisState(ImmutableCollection<Loop> pImmutableCollection){
    this();
    if(pImmutableCollection.size() > 0){
      for(Loop l: pImmutableCollection){
        loopStateMap.put(l, new AccessAnalysisState());
      }
    }
  }

  public List<Loop> getLoopContainEdge(final CFAEdge edge){
    List<Loop> loops = Lists.newArrayList();
    for(Loop loop:loopStateMap.keySet()){
      if(loop.getInnerLoopEdges().contains(edge)) {
        loops.add(loop);
      }
    }
    return loops;
  }
 */

  /*
   * end of summary phase
   */

  public AccessAnalysisState(
      PathCopyingPersistentTree<String, Presence> readTree,
      PathCopyingPersistentTree<String, Presence> writeTree) {
    this.readTree = readTree;
    this.writeTree = writeTree;
  }

  public AccessAnalysisState() {
    readTree = new PathCopyingPersistentTree<>();
    writeTree = new PathCopyingPersistentTree<>();
  }

  public static AccessAnalysisState of() {
    return EMPTY;
  }

  public static AccessAnalysisState markRead(List<String> path, AccessAnalysisState pState) {
    return pState.read(path);
  }

  public static AccessAnalysisState markRead(AccessPath ap, AccessAnalysisState pState) {
    return (ap == null) ? pState : pState.read(ap);
  }

  public static AccessAnalysisState markWrite(List<String> path, AccessAnalysisState pState) {
    return pState.write(path);
  }

  public static AccessAnalysisState markWrite(AccessPath pAccessPath, AccessAnalysisState pState) {
    return pState.write(pAccessPath);
  }

  public AccessAnalysisState read(AccessPath ap) {
    Preconditions.checkNotNull(ap);

    return read(AccessPath.toStrList(ap));
  }

  public AccessAnalysisState read(List<String> path) {
    List<Presence> trace = readTree.trace(path);
    // check if any of the element on the trace is 'read'
    for (Presence p : trace) {
      if (p != null) {
        return this;
      }
    }
    // needs a copy
    return new AccessAnalysisState(readTree.setSubtreeAndCopy(path, Presence.INSTANCE), writeTree);
  }

  public AccessAnalysisState write(AccessPath ap) {
    return write(AccessPath.toStrList(ap));
  }

  public AccessAnalysisState write(List<String> path) {
    List<Presence> trace = writeTree.trace(path);
    // check if any of the element on the trace is 'read'
    for (Presence p : trace) {
      if (p != null) {
        return this;
      }
    }
    // needs a copy
    return new AccessAnalysisState(readTree, writeTree.setSubtreeAndCopy(path, Presence.INSTANCE));
  }

  /**
   * Handle declaration, erase all subtree of this particular variable
   */
  public AccessAnalysisState erase(String pQualifiedName) {
    List<String> path = new ArrayList<>();
    path.add(pQualifiedName);
    return new AccessAnalysisState(readTree.removeSubtreeAndCopy(path),
        writeTree.removeSubtreeAndCopy(path));
  }

  public static AccessAnalysisState join(AccessAnalysisState s1, AccessAnalysisState s2) {
    return s1.join(s2);
  }

  /**
   * Important notice:
   *
   * it should hold that
   *
   * that <= result <= top
   *
   * However, from {@link MergeOperator#merge(AbstractState, AbstractState, Precision)}
   * CPAChecker now has an issue #92, if result equals to that (result.equals(that) == true),
   * we need to return exactly the same instance of 'that'
   */
  @Override
  public AccessAnalysisState join(AccessAnalysisState that) {
    AccessAnalysisState merged = new AccessAnalysisState(
        mergeTree(this.readTree, that.readTree),
        mergeTree(this.writeTree, that.writeTree)
    );
/*    // TODO for loopMap, we need join. Basically, it is function level, all state has the same map-keys
    for(Loop loop:loopStateMap.keySet()){
      AccessAnalysisState thisState = loopStateMap.get(loop);
      AccessAnalysisState thatState = that.loopStateMap.get(loop);
      merged.loopStateMap.put(loop, new AccessAnalysisState(
        mergeTree(thisState.readTree, thatState.readTree),
        mergeTree(thisState.writeTree, thatState.writeTree)
        ));
    }
*/
    return merged.equals(that) ? that : merged;
  }

  private static PathCopyingPersistentTree<String, Presence> mergeTree(
      PathCopyingPersistentTree<String, Presence> a,
      PathCopyingPersistentTree<String, Presence> b) {
    if (a.isEmpty()) {
      return b;
    } else if (b.isEmpty()) {
      return a;
    } else {
      // both of them are non-empty
      return new PathCopyingPersistentTree<>(mergeTreeNode(a.getRoot(), b.getRoot()));
    }
  }

  /**
   * Precondition: a, b should not be empty at the same time
   */
  private static PersistentTreeNode<String, Presence> mergeTreeNode(
      PersistentTreeNode<String, Presence> a,
      PersistentTreeNode<String, Presence> b) {
    if (a == null) {
      return b;
    } else if (b == null) {
      return a;
    } else {
      // both a, b are not null
      if (a.getElement() != null) {
        return a;
      } else if (b.getElement() != null) {
        return b;
      } else {
        // otherwise, element == null
        Set<String> segs = ImmutableSet.<String>builder()
            .addAll(a.branches())
            .addAll(b.branches())
            .build();
        Map<String, PersistentTreeNode<String, Presence>> children = new HashMap<>();
        for (String s : segs) {
          children.put(s, mergeTreeNode(a.getChild(s), b.getChild(s)));
        }
        return new PersistentTreeNode<>(null, children);
      }
    }
  }

  // tomgu for loopMap we ignore isLessOrEqual
  @Override
  public boolean isLessOrEqual(AccessAnalysisState that) throws CPAException,
                                                                InterruptedException {
    return (lessOrEqualTree(this.readTree, that.readTree) && lessOrEqualTree(this.writeTree,
        that.writeTree));
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

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    final List<String> collector = new ArrayList<>();
    TreeVisitor<String, Presence> visitor = new TreeVisitor<String, Presence>() {
      @Override
      public TreeVisitStrategy visit(Stack<String> path, Presence element, boolean isLeaf) {
        if (isLeaf) {
          collector.add(Joiner.on('.').join(path));
        }
        return TreeVisitStrategy.CONTINUE;
      }

    };
    sb.append("READ: [");
    readTree.traverse(visitor);
    sb.append(Joiner.on(", ").join(collector));
    sb.append("];");
    sb.append("WRITE: [");
    collector.clear();
    writeTree.traverse(visitor);
    sb.append(Joiner.on(", ").join(collector));
    sb.append("];");
    return sb.toString();
  }

  public static AccessAnalysisState copyOf(AccessAnalysisState aas) {
    AccessAnalysisState result = new AccessAnalysisState(aas.readTree, aas.writeTree);
//    result.loopStateMap = aas.loopStateMap;
    return result;
  }

  @Override
  public String toDOTLabel() {
    return toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  public AccessAnalysisState move(List<String> oldPath, List<String> newPath) {
    return new AccessAnalysisState(readTree.move(oldPath, newPath),
        writeTree.move(oldPath, newPath));
  }

  public AccessAnalysisState eraseRead(List<String> path) {
    return new AccessAnalysisState(readTree.removeSubtreeAndCopy(path), writeTree);
  }

  /**
   * tomgu
   * remove write path
   */
  public AccessAnalysisState eraseWrite(List<String> path) {
    return new AccessAnalysisState(readTree, writeTree.removeSubtreeAndCopy(path));
  }

  public AccessAnalysisState getSubState(List<String> path) {
    return new AccessAnalysisState(
        new PathCopyingPersistentTree<>(readTree.getSubtreeRoot(path)),
        new PathCopyingPersistentTree<>(writeTree.getSubtreeRoot(path))
    );
  }

  /**
   * There should be at most 1 'true' along every path, and it is the leaf.
   *
   * Only merge when there is no 'true' along the path.
   */
  public AccessAnalysisState joinSubState(List<String> path, AccessAnalysisState state) {
    PathCopyingPersistentTree<String, Presence> empty = PathCopyingPersistentTree.of();
    AccessAnalysisState that = new AccessAnalysisState(
        empty.setSubtreeAndCopy(path, state.readTree),
        empty.setSubtreeAndCopy(path, state.writeTree)
    );
    // should use join
    return this.join(that);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof AccessAnalysisState)) {
      return false;
    }

    // for state in the loop, if any of them is not equal, return false;
    AccessAnalysisState that = (AccessAnalysisState) o;
/*
    for(Loop loop:loopStateMap.keySet()){
      AccessAnalysisState thisState = loopStateMap.get(loop);
      AccessAnalysisState thatState = that.loopStateMap.get(loop);
      if(Objects.equal(thisState.readTree, thatState.readTree) &&
        Objects.equal(thisState.writeTree, thatState.writeTree)){
        continue;
      }else{
        return false;
      }
    }
*/
    return Objects.equal(this.readTree, that.readTree) &&
        Objects.equal(this.writeTree, that.writeTree);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.readTree, this.writeTree);
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }
}
