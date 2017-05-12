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
package org.sosy_lab.cpachecker.cpa.range;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.SwitchableGraphable;
import org.sosy_lab.cpachecker.core.interfaces.function.ADCombinator;
import org.sosy_lab.cpachecker.core.interfaces.function.ADUnit;
import org.sosy_lab.cpachecker.core.interfaces.summary.SummaryAcceptableState;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessExternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessFunctionInstance;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessInternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeExternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeFunctionInstance;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeInternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.cpa.range.util.AccessSummaryApplicator;
import org.sosy_lab.cpachecker.cpa.range.util.Ranges;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree.OverrideMerger;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree.PersistentTreeNode;
import org.sosy_lab.cpachecker.util.collections.tree.TreeVisitor;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public class RangeState implements LatticeAbstractState<RangeState>, SwitchableGraphable,
                                   SummaryAcceptableState {

  private PathCopyingPersistentTree<String, Range> ranges;

  private static final Pattern arrayPattern = Pattern.compile("\\[([\\d]+)]");

  public RangeState() {
    ranges = PathCopyingPersistentTree.of();
  }

  public RangeState(PathCopyingPersistentTree<String, Range> pRanges) {
    ranges = pRanges;
  }

  public Range getRange(AccessPath accessPath, MachineModel machineModel) {
    // The input access path may contain undetermined array index
    // For an array subscript A[x] where x \in [a,b], then A[x] \in A[a] \cup ... \cup A[b]
    if (accessPath == null) {
      return Range.UNBOUND;
    }
    // examine if current access path is declared, if not we return empty range as a result
    // use of undefined value error?
    PersistentTreeNode<String, Range> root = ranges.getRoot();
    if (root == null || root.getChild(accessPath.getQualifiedName()) == null) {
      if (accessPath.isGlobal()) {
        List<CType> typeList = accessPath.parseTypeList();
        if (!typeList.isEmpty()) {
          CType lastType = typeList.get(typeList.size() - 1);
          return Ranges.getTypeRange(lastType, machineModel);
        }
      }
      return Range.EMPTY;
    }
    if (accessPath.isCanonicalAccessPath()) {
      Range result = ranges.get(AccessPath.toStrList(accessPath));
      return (result != null) ? result : Range.UNBOUND;
    } else {
      // handle access path with undetermined index
      // NOTE: we use BFS on range tree since we can terminate the search early when there are
      // missing access paths
      List<PersistentTreeNode<String, Range>> workingQueue = new ArrayList<>();
      workingQueue.add(ranges.getRoot());
      List<PathSegment> segments = accessPath.path();
      for (PathSegment segment : segments) {
        if (segment instanceof ArrayUncertainIndexSegment) {
          Range indexRange = ((ArrayUncertainIndexSegment) segment).getIndexRange();
          CompInteger currentIndex = indexRange.getLow();
          CompInteger limitIndex = indexRange.getHigh();
          List<PersistentTreeNode<String, Range>> tmpQueue = new ArrayList<>();
          while (currentIndex.compareTo(limitIndex) <= 0) {
            Long longIndex = currentIndex.longValue();
            if (longIndex == null) {
              // this case is very rare
              return Range.UNBOUND;
            }
            ArrayConstIndexSegment constSegment = new ArrayConstIndexSegment(longIndex);
            String followSegStr = constSegment.getName();
            for (PersistentTreeNode<String, Range> node : workingQueue) {
              PersistentTreeNode<String, Range> child = node.getChild(followSegStr);
              if (child == null) {
                return Range.UNBOUND;
              }
              tmpQueue.add(child);
            }
            // self-increment
            currentIndex = currentIndex.add(CompInteger.ONE);
          }
          // if we reach here, the working queue is updated
          workingQueue.clear();
          workingQueue.addAll(tmpQueue);
        } else {
          // ordinary case
          String segStr = segment.getName();
          List<PersistentTreeNode<String, Range>> tmpQueue = new ArrayList<>();
          for (PersistentTreeNode<String, Range> node : workingQueue) {
            PersistentTreeNode<String, Range> child = node.getChild(segStr);
            if (child == null) {
              return Range.UNBOUND;
            }
            tmpQueue.add(child);
          }
          // if we reach here, then each node has specified child
          workingQueue.clear();
          workingQueue.addAll(tmpQueue);
        }
      }
      // after traversing all segments, we try to union all elements of reminiscent tree nodes
      Range result = Range.EMPTY;
      for (PersistentTreeNode<String, Range> node : workingQueue) {
        Range result2 = node.getElement();
        if (result2 == null) {
          // this access path has no corresponding record in current state
          return Range.UNBOUND;
        }
        result = result.union(result2);
      }
      return result;
    }
  }

  // for internal use only
  public PathCopyingPersistentTree<String, Range> getRanges() {
    return ranges;
  }

  @Nullable
  public PersistentTreeNode<String, Range> getSubTree(String name) {
    return ranges.getSubtreeRoot(Lists.newArrayList(name));
  }

  public boolean contains(AccessPath accessPath) {
    return accessPath != null && ranges.get(AccessPath.toStrList(accessPath)) != null;
  }

  /**
   * If the forSummary flag is on, we do not write any range for global access path.
   */
  public void addRange(AccessPath path, Range range, boolean forSummary) {
    if (path == null) {
      return;
    }
    if (forSummary && path.isGlobal()) {
      return;
    }
    // The input access path may contain undetermined array index, we should carefully handle
    // them in order to guarantee soundness of analysis
    if (range.isUnbound()) {
      removeRange(path);
      return;
    }
    // No access path containing pointer dereference is allowed in range mapping
    if (path.containsPointerOperation()) {
      return;
    }

    // consider an access path P: A[2-4].field and current state contains A[3].field,
    // we update A[3].field by union operation on original value and specified updated range.
    // This is because we are not sure which element should be updated (over-approximation)
    if (path.isCanonicalAccessPath()) {
      // this is the simple case
      List<String> strPath = AccessPath.toStrList(path);
      Range existRange = ranges.get(strPath);
      if (existRange == null || !existRange.equals(range)) {
        ranges = ranges.setElementAndCopy(strPath, range);
      }
    } else {
      // in this case, access path contains undetermined array index
      // we employ a method that lazily prunes paths that do not correspond to given fuzzy path
      Deque<PathSegment> segments = new ArrayDeque<>(path.path());
      PathCopyingPersistentTree<String, PersistentTreeNode<String, Range>> pathTree =
          projectAccessPath(ranges.getRoot(), segments);
      // update range(s) for corresponding access path(s)
      RangeUpdateVisitor rangeVisitor = new RangeUpdateVisitor(range);
      pathTree.traverse(rangeVisitor);
    }
  }

  /**
   * Update range tree at the low level.
   * Only for special use.
   */
  public void addRange(List<String> pathList, Range range, boolean forSummary) {
    if (pathList.isEmpty()) {
      return;
    }
    if (forSummary) {
      String firstPath = pathList.get(0);
      if (!firstPath.contains("::")) {
        // the qualified name does not contain function name --- global access path
        return;
      }
    }
    if (range.isUnbound()) {
      String declared = pathList.get(0);
      if (ranges.getSubtreeRoot(Lists.newArrayList(declared)) == null) {
        ranges = ranges.setElementAndCopy(Lists.newArrayList(declared), Range.UNBOUND);
      }
    } else {
      ranges = ranges.setElementAndCopy(pathList, range);
    }
  }

  /**
   * Update ranges for a set of access paths.
   */
  private class RangeUpdateVisitor implements TreeVisitor<String, PersistentTreeNode<String,
      Range>> {

    private final Range newRange;

    RangeUpdateVisitor(Range pRange) {
      newRange = pRange;
    }

    @Override
    public TreeVisitStrategy visit(
        Stack<String> path, PersistentTreeNode<String, Range> element,
        boolean isLeaf) {
      if (isLeaf && element != null) {
        // this access path MUST correspond to a valid range element
        Range oldRange = element.getElement();
        if (oldRange == null) {
          if (path.size() == 1) {
            ranges = ranges.setElementAndCopy(path, Range.UNBOUND);
          } else {
            ranges = ranges.removeElementAndCopy(path);
          }
        } else {
          oldRange = oldRange.union(newRange);
          if (oldRange.isUnbound()) {
            if (path.size() == 1) {
              ranges = ranges.setElementAndCopy(path, Range.UNBOUND);
            } else {
              ranges = ranges.removeElementAndCopy(path);
            }
          } else {
            // we do not add unbound range into range tree
            ranges = ranges.setElementAndCopy(path, oldRange);
          }
        }
      }
      return TreeVisitStrategy.CONTINUE;
    }
  }

  /**
   * Retrieve access paths from current state given an access path possibly contains undetermined
   * array index segment
   *
   * @param node     current traversed tree node
   * @param segments path segments to be handled
   * @return hit access paths organized by a persistent tree
   */
  private PathCopyingPersistentTree<String, PersistentTreeNode<String, Range>> projectAccessPath(
      PersistentTreeNode<String, Range> node, Deque<PathSegment> segments) {
    if (node == null) {
      // if current node is null, we simply return an empty tree (because no access paths are hit)
      return PathCopyingPersistentTree.of();
    }
    // NOTE: we use DFS here since there is no need to terminate search when a missing access
    // path is encountered
    PathCopyingPersistentTree<String, PersistentTreeNode<String, Range>> pathTree =
        PathCopyingPersistentTree.of();
    if (segments.isEmpty()) {
      pathTree = new PathCopyingPersistentTree<>(new PersistentTreeNode<String,
          PersistentTreeNode<String, Range>>(node));
      return pathTree;
    }
    // prevent direct modification on path segments queue
    Deque<PathSegment> currentSegments = new ArrayDeque<>(segments);
    PathSegment segment = currentSegments.poll();
    if (segment instanceof ArrayUncertainIndexSegment) {
      Range indexRange = ((ArrayUncertainIndexSegment) segment).getIndexRange();
      // TODO: traverse state or range? is there some optimal solutions?
      Set<String> keys = node.getKeys();
      for (String key : keys) {
        Matcher matcher = arrayPattern.matcher(key);
        if (matcher.find()) {
          String strIndex = matcher.group(1);
          Long longIndex = Long.parseLong(strIndex);
          if (indexRange.in(longIndex)) {
            // this key hits the current segment
            PersistentTreeNode<String, Range> nextNode = node.getChild(key);
            PathCopyingPersistentTree<String, PersistentTreeNode<String, Range>> subtree =
                projectAccessPath(nextNode, currentSegments);
            pathTree = pathTree.setSubtreeAndCopy(Lists.newArrayList(key), subtree);
          }
        }
      }
    } else {
      String strSeg = segment.getName();
      PersistentTreeNode<String, Range> nextNode = node.getChild(strSeg);
      if (nextNode == null) {
        return pathTree;
      }
      PathCopyingPersistentTree<String, PersistentTreeNode<String, Range>> subtree =
          projectAccessPath(nextNode, currentSegments);
      pathTree = pathTree.setSubtreeAndCopy(Lists.newArrayList(strSeg), subtree);
    }
    return pathTree;
  }

  public void addAllRanges(PathCopyingPersistentTree<String, Range> rangeTree) {
    // NOTE: for now this method is called only when handling declaration, all ranges should be
    // overwritten
    OverrideMerger<String, Range> overrideMerger = new OverrideMerger<>();
    ranges = PathCopyingPersistentTree.merge(ranges, rangeTree, overrideMerger);
  }

  /**
   * Search for all access paths that begin with {@param replaced} and add new access paths by
   * replacing {@param replaced} with {@param replacement}
   *
   * @param pReplaced    the prefixes of access paths to be replaced (left-hand-side)
   * @param pReplacement the specified replacing prefix (right-hand-side)
   */
  public void replaceAndCopy(AccessPath pReplacement, AccessPath pReplaced, boolean forSummary) {
    if (pReplaced == null || pReplacement == null) {
      return;
    }
    if (forSummary && pReplaced.isGlobal()) {
      return;
    }
    Deque<PathSegment> rightSegments = new ArrayDeque<>(pReplacement.path());
    PathCopyingPersistentTree<String, PersistentTreeNode<String, Range>> rightTree =
        projectAccessPath(ranges.getRoot(), rightSegments);
    // merge the right tree
    TreeNodeMergeVisitor mVisitor = new TreeNodeMergeVisitor();
    rightTree.traverse(mVisitor);
    // this merged tree node is to be inserted to the left-hand node
    PersistentTreeNode<String, Range> rightNode = mVisitor.getTotalNode();
    // if the returned node is null, then we directly return from this function
    if (rightNode == null) {
      return;
    }
    // FIX: we should not project left path to range tree since it possibly does not exist there
    if (!pReplaced.isCanonicalAccessPath()) {
      // now we merge access paths of left-hand-side
      Deque<PathSegment> leftSegments = new ArrayDeque<>(pReplaced.path());
      PathCopyingPersistentTree<String, PersistentTreeNode<String, Range>> leftTree =
          projectAccessPath(ranges.getRoot(), leftSegments);
      SubtreeUpdateVisitor uVisitor = new SubtreeUpdateVisitor(rightNode, true);
      leftTree.traverse(uVisitor);
    } else {
      // we directly update this access path
      ranges = ranges.setSubtreeAndCopy(AccessPath.toStrList(pReplaced), rightNode);
    }
  }

  private class SubtreeUpdateVisitor
      implements TreeVisitor<String, PersistentTreeNode<String, Range>> {

    /**
     * mergeMode = true, then we merge the existing subtree with given tree
     * mergeMode = false, then we directly set the subtree
     */
    private final boolean mergeMode;

    private final PersistentTreeNode<String, Range> givenNode;

    SubtreeUpdateVisitor(PersistentTreeNode<String, Range> pNode, boolean pMergeMode) {
      givenNode = pNode;
      mergeMode = pMergeMode;
    }

    @Override
    public TreeVisitStrategy visit(
        Stack<String> path, PersistentTreeNode<String, Range> element, boolean isLeaf) {
      if (isLeaf && element != null) {
        if (mergeMode) {
          PersistentTreeNode<String, Range> resultNode = mergeTreeNode(element, givenNode);
          ranges = ranges.setSubtreeAndCopy(path, resultNode);
        } else {
          ranges = ranges.setSubtreeAndCopy(path, givenNode);
        }
      }
      return TreeVisitStrategy.CONTINUE;
    }
  }

  private class TreeNodeMergeVisitor
      implements TreeVisitor<String, PersistentTreeNode<String, Range>> {

    private PersistentTreeNode<String, Range> total = null;

    PersistentTreeNode<String, Range> getTotalNode() {
      return total;
    }

    @Override
    public TreeVisitStrategy visit(
        Stack<String> path, PersistentTreeNode<String, Range> element, boolean isLeaf) {
      if (isLeaf && element != null) {
        if (total == null) {
          total = element;
        } else {
          // corresponding to the case where access path contains undetermined array index
          total = mergeTreeNode(total, element);
        }
      }
      return TreeVisitStrategy.CONTINUE;
    }
  }

  public static PathCopyingPersistentTree<String, Range> mergeTree(
      PathCopyingPersistentTree<String, Range> a,
      PathCopyingPersistentTree<String, Range> b) {
    PersistentTreeNode<String, Range> aRoot = a.getRoot();
    PersistentTreeNode<String, Range> bRoot = b.getRoot();
    if (aRoot == null && bRoot == null) {
      return PathCopyingPersistentTree.of();
    }
    if (aRoot == null) {
      return new PathCopyingPersistentTree<>(bRoot);
    }
    if (bRoot == null) {
      return new PathCopyingPersistentTree<>(aRoot);
    }
    Set<String> aKeys = aRoot.getKeys();
    Set<String> bKeys = bRoot.getKeys();
    Set<String> totalKeys = Sets.union(aKeys, bKeys);
    PersistentMap<String, PersistentTreeNode<String, Range>> newChildren =
        PathCopyingPersistentTreeMap.of();
    for (String totalKey : totalKeys) {
      PersistentTreeNode<String, Range> aNode = aRoot.getChild(totalKey);
      PersistentTreeNode<String, Range> bNode = bRoot.getChild(totalKey);
      PersistentTreeNode<String, Range> merged = mergeTreeNode(aNode, bNode);
      if (merged != null) {
        newChildren = newChildren.putAndCopy(totalKey, merged);
      }
    }
    PersistentTreeNode<String, Range> newRoot = new PersistentTreeNode<>(null, newChildren);
    return new PathCopyingPersistentTree<>(newRoot);
  }

  /**
   * Invariant: a and b should not be null simultaneously
   */
  private static PersistentTreeNode<String, Range> mergeTreeNode(
      PersistentTreeNode<String, Range> a, PersistentTreeNode<String, Range> b) {
    assert (a != null || b != null);
    if (a == null) {
      return b;
    } else if (b == null) {
      return a;
    }
    // merge element
    Range aElement = a.getElement();
    Range bElement = b.getElement();
    Range tElement;
    if (aElement == null || bElement == null) {
      tElement = null;
    } else {
      tElement = aElement.union(bElement);
    }
    // merge children maps
    Set<String> aKeys = a.getKeys();
    Set<String> bKeys = b.getKeys();
    Set<String> commonKeys = Sets.intersection(aKeys, bKeys);
    PersistentMap<String, PersistentTreeNode<String, Range>> newChildren =
        PathCopyingPersistentTreeMap.of();
    for (String commonKey : commonKeys) {
      PersistentTreeNode<String, Range> aNode = a.getChild(commonKey);
      PersistentTreeNode<String, Range> bNode = b.getChild(commonKey);
      PersistentTreeNode<String, Range> cNode = mergeTreeNode(aNode, bNode);
      if (cNode != null) {
        newChildren = newChildren.putAndCopy(commonKey, cNode);
      }
    }
    if (newChildren.isEmpty()) {
      newChildren = null;
    }
    return new PersistentTreeNode<>(tElement, newChildren);
  }

  /* ********************* */
  /* summary manipulations */
  /* ********************* */

  /**
   * Forcibly update current range state by the given range state.
   * This method is designed for summary application.
   *
   * @param pState the target state contains new information
   */
  public void forcedUpdate(RangeState pState) {
    addAllRanges(pState.ranges);
  }

  public void addRangesWithPrefix(AccessPath path, PersistentTreeNode<String, Range> subTree) {
    if (subTree != null) {
      List<String> pathStr = AccessPath.toStrList(path);
      ranges = ranges.setSubtreeAndCopy(pathStr, subTree);
    }
  }

  public void addRangesWithPrefix(String name, PersistentTreeNode<String, Range> subtree) {
    if (subtree != null) {
      ranges = ranges.setSubtreeAndCopy(Lists.newArrayList(name), subtree);
    }
  }

  public PersistentTreeNode<String, Range> removeRangesWithPrefix(String name) {
    PersistentTreeNode<String, Range> subtree = ranges.getSubtreeRoot(Lists.newArrayList(name));
    if(subtree != null) {
      ranges = ranges.removeSubtreeAndCopy(Lists.newArrayList(name));
    }
    return subtree;
  }

  /**
   * Create an abstract domain combinator according to given access path prefix.
   * Such prefix path can be the path of struct or array.
   *
   * @param path the prefix access path
   * @return an abstract domain combinator in {@link Range} domain
   */
  public ADCombinator<Range> createADCombinatorFromPrefix(final AccessPath path) {
    Deque<PathSegment> segments = new ArrayDeque<>(path.path());
    PathCopyingPersistentTree<String, PersistentTreeNode<String, Range>> pathTree =
        projectAccessPath(ranges.getRoot(), segments);
    TreeNodeMergeVisitor mVisitor = new TreeNodeMergeVisitor();
    pathTree.traverse(mVisitor);
    PersistentTreeNode<String, Range> node = mVisitor.getTotalNode();
    // traverse this node and get the path list
    final ADCombinator<Range> combinator = new ADCombinator<>(path);
    TreeVisitor<String, Range> combVisitor = new TreeVisitor<String, Range>() {
      @Override
      public TreeVisitStrategy visit(Stack<String> pPath, Range element, boolean isLeaf) {
        if (isLeaf && element != null) {
          combinator.insertValue(new ADUnit<>(element, pPath, path));
        }
        return TreeVisitStrategy.CONTINUE;
      }
    };
    if (node == null) {
      // nothing is inserted into combinator
      return combinator;
    }
    PathCopyingPersistentTree<String, Range> nodeTree = new PathCopyingPersistentTree<>(node);
    nodeTree.traverse(combVisitor);
    return combinator;
  }

  /**
   * While removing the specified access path, we should keep the declaration (i.e. the first
   * declaration segment) in the range tree. Otherwise, the variable becomes undeclared (but
   * certain variable is removed only after the stack is removed).
   */
  private void removeRange(AccessPath path) {
    Deque<PathSegment> pathQueue = new ArrayDeque<>(path.path());
    PathCopyingPersistentTree<String, PersistentTreeNode<String, Range>> pathTree =
        projectAccessPath(ranges.getRoot(), pathQueue);
    RemovePathVisitor rVisitor = new RemovePathVisitor(ranges);
    pathTree.traverse(rVisitor);
    ranges = rVisitor.getTree();
    // add the declaration segment if it does not exist
    String declName = path.getQualifiedName();
    PersistentTreeNode<String, Range> root = ranges.getRoot();
    if (root == null || root.getChild(declName) == null) {
      ranges = ranges.setElementAndCopy(Lists.newArrayList(declName), Range.UNBOUND);
    }
  }

  private class RemovePathVisitor
      implements TreeVisitor<String, PersistentTreeNode<String, Range>> {

    private PathCopyingPersistentTree<String, Range> rangeTree;

    RemovePathVisitor(PathCopyingPersistentTree<String, Range> pTree) {
      rangeTree = pTree;
    }

    PathCopyingPersistentTree<String, Range> getTree() {
      return rangeTree;
    }

    @Override
    public TreeVisitStrategy visit(
        Stack<String> path, PersistentTreeNode<String, Range> element, boolean isLeaf) {
      if (isLeaf && element != null) {
        rangeTree = rangeTree.removeSubtreeAndCopy(path);
      }
      return TreeVisitStrategy.CONTINUE;
    }
  }

  void dropFrame(String pCalledFunctionName) {
    PersistentTreeNode<String, Range> root = ranges.getRoot();
    if (root != null) {
      Set<String> keys = root.getKeys();
      for (String key : keys) {
        if (key.startsWith(pCalledFunctionName + "::")) {
          ranges = ranges.removeSubtreeAndCopy(Lists.newArrayList(key));
        }
      }
    }
  }

  public static RangeState copyOf(RangeState old) {
    return new RangeState(old.ranges);
  }

  @Override
  public RangeState join(RangeState other) {
    PathCopyingPersistentTree<String, Range> thisRanges = this.ranges;
    PathCopyingPersistentTree<String, Range> thatRanges = other.ranges;
    PathCopyingPersistentTree<String, Range> merged = mergeTree(thisRanges, thatRanges);
    return new RangeState(merged);
  }

  @Override
  public boolean isLessOrEqual(RangeState other) {
    TreeUndefCompareVisitor undefVisitor = new TreeUndefCompareVisitor(other.ranges);
    ranges.traverse(undefVisitor);
    if (!undefVisitor.getIsLessOrEqual()) {
      return false;
    }
    TreeCompareVisitor compVisitor = new TreeCompareVisitor(ranges);
    other.ranges.traverse(compVisitor);
    return compVisitor.getIsLessOrEqual();

  }

  /**
   * Compare the definedness of declarations.
   */
  private class TreeUndefCompareVisitor implements TreeVisitor<String, Range> {

    private final PathCopyingPersistentTree<String, Range> rightSide;
    private boolean isLessOrEqual = true;

    TreeUndefCompareVisitor(PathCopyingPersistentTree<String, Range> pRight) {
      rightSide = pRight;
    }

    public boolean getIsLessOrEqual() {
      return isLessOrEqual;
    }

    @Override
    public TreeVisitStrategy visit(Stack<String> path, Range element, boolean isLeaf) {
      if (path.size() == 1) {
        if (rightSide.getSubtreeRoot(path) == null) {
          // the right state has an undeclared variable
          isLessOrEqual = false;
          return TreeVisitStrategy.ABORT;
        }
        return TreeVisitStrategy.SKIP;
      }
      // this should NOT be reached
      return TreeVisitStrategy.CONTINUE;
    }
  }

  /**
   * Compare ranges of access paths.
   */
  private class TreeCompareVisitor implements TreeVisitor<String, Range> {

    private final PathCopyingPersistentTree<String, Range> leftSide;
    private boolean isLessOrEqual = true;

    TreeCompareVisitor(PathCopyingPersistentTree<String, Range> pLeft) {
      leftSide = pLeft;
    }

    public boolean getIsLessOrEqual() {
      return isLessOrEqual;
    }

    @Override
    public TreeVisitStrategy visit(Stack<String> path, Range element, boolean isLeaf) {
      if (isLeaf && element != null) {
        Range leftRange = leftSide.get(path);
        if (leftRange == null) {
          if (path.size() == 1) {
            leftRange = Range.EMPTY;
          } else {
            leftRange = Range.UNBOUND;
          }
        }
        if (!element.contains(leftRange)) {
          // 1. the left range is unbounded but the right one is not
          // 2. the right range does not contain the corresponding left range
          isLessOrEqual = false;
          return TreeVisitStrategy.ABORT;
        }
      }
      return TreeVisitStrategy.CONTINUE;
    }
  }

  private class TreeEqualsVisitor implements TreeVisitor<String, Range> {

    private final PathCopyingPersistentTree<String, Range> other;
    private boolean isEquals = true;

    TreeEqualsVisitor(PathCopyingPersistentTree<String, Range> pOther) {
      other = pOther;
    }

    public boolean isEquals() {
      return isEquals;
    }

    @Override
    public TreeVisitStrategy visit(Stack<String> path, Range element, boolean isLeaf) {
      if (isLeaf && element != null) {
        Range otherRange = other.get(path);
        if (otherRange == null || !otherRange.equals(element)) {
          isEquals = false;
          return TreeVisitStrategy.ABORT;
        }
      }
      return TreeVisitStrategy.CONTINUE;
    }
  }

  /**
   * Perform widening on two given states.
   *
   * @param r a newer state
   * @return widening result
   */
  public RangeState widening(RangeState r) {
    RangeTreeWideningVisitor wVisitor = new RangeTreeWideningVisitor(ranges);
    r.ranges.traverse(wVisitor);
    return wVisitor.getWidenedState();
  }

  private class RangeTreeWideningVisitor implements TreeVisitor<String, Range> {

    private final PathCopyingPersistentTree<String, Range> older;
    private RangeState newState = new RangeState();

    RangeTreeWideningVisitor(PathCopyingPersistentTree<String, Range> pOlder) {
      older = pOlder;
    }

    @Override
    public TreeVisitStrategy visit(Stack<String> path, Range element, boolean isLeaf) {
      if (isLeaf && element != null) {
        Range oldRange;
        if (path.size() == 1 && older.getSubtreeRoot(path) == null) {
          oldRange = Range.EMPTY;
        } else {
          oldRange = older.get(path);
          if (oldRange == null) {
            oldRange = Range.UNBOUND;
          }
        }
        Range result;
        if (oldRange.isEmpty()) {
          result = Range.UNBOUND;
        } else if (element.isEmpty()) {
          result = Range.EMPTY;
        } else {
          CompInteger min, max;
          CompInteger oldMin = oldRange.getLow();
          CompInteger newMin = element.getLow();
          min = oldMin.compareTo(newMin) > 0 ? CompInteger.NEGATIVE_INF : oldMin;
          CompInteger oldMax = oldRange.getHigh();
          CompInteger newMax = element.getHigh();
          max = oldMax.compareTo(newMax) < 0 ? CompInteger.POSITIVE_INF : oldMax;
          result = new Range(min, max);
        }
        // widening fixes the values for global variables
        newState.addRange(path, result, true);
      }
      return TreeVisitStrategy.CONTINUE;
    }

    RangeState getWidenedState() {
      return newState;
    }
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || !getClass().equals(other.getClass())) {
      return false;
    }
    RangeState otherState = (RangeState) other;
    TreeEqualsVisitor eVisitor = new TreeEqualsVisitor(otherState.ranges);
    this.ranges.traverse(eVisitor);
    return eVisitor.isEquals();
  }

  @Override
  public int hashCode() {
    return ranges.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[\n");
    PrintTreeVisitor pVisitor = new PrintTreeVisitor(true);
    ranges.traverse(pVisitor);
    sb.append(pVisitor.getString());
    return sb.append("] size-> ").append(pVisitor.getSize()).toString();
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    PrintTreeVisitor pVisitor = new PrintTreeVisitor(false);
    ranges.traverse(pVisitor);
    sb.append(pVisitor.getString());
    sb.append("}");
    return sb.toString();
  }

  private class PrintTreeVisitor implements TreeVisitor<String, Range> {

    private StringBuilder builder = new StringBuilder();
    private final boolean needNewLine;
    private long size = 0;

    PrintTreeVisitor(boolean pNewLine) {
      needNewLine = pNewLine;
    }

    public String getString() {
      return builder.toString();
    }

    public long getSize() {
      return size;
    }

    @Override
    public TreeVisitStrategy visit(Stack<String> path, Range element, boolean isLeaf) {
      if (isLeaf && element != null) {
        builder.append(Joiner.on('.').join(path));
        builder.append(" = ");
        builder.append(element);
        builder.append(", ");
        if (needNewLine) {
          builder.append("\n");
        }
        size++;
      }
      return TreeVisitStrategy.CONTINUE;
    }
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  public static Range evaluateRange(
      RangeState readableState, List<AbstractState> pOtherStates,
      CRightHandSide rightHand, MachineModel model)
      throws UnrecognizedCCodeException {
    return rightHand.accept(new ExpressionRangeVisitor(readableState, pOtherStates, model, false));
  }

  /**
   * This function use information in other CPA to derive the actual path if exists.
   */
  @Nullable
  public static AccessPath getAccessPath(
      RangeState readableState,
      List<AbstractState> otherStates,
      CLeftHandSide leftHand,
      MachineModel model) {
    LeftHandAccessPathVisitor visitor = new LeftHandAccessPathVisitor(new ExpressionRangeVisitor
        (readableState, otherStates, model, false));
    AccessPath path;
    try {
      path = leftHand.accept(visitor).orNull();
    } catch (UnrecognizedCCodeException ex) {
      path = null;
    }
    return path;
  }

  @Override
  public boolean getActiveStatus() {
    return false;
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }

  /* ******************* */
  /* summary application */
  /* ******************* */

  @Override
  public Collection<? extends AbstractState> applyFunctionSummary(
      List<SummaryInstance> pSummaryList,
      CFAEdge inEdge,
      CFAEdge outEdge,
      List<AbstractState> pOtherStates) throws CPATransferException {
    RangeState newState = RangeState.copyOf(this);
    CFunctionCall call = (CFunctionCall) ((FunctionReturnEdge) outEdge).getSummaryEdge()
        .getExpression();
    for (SummaryInstance summary : pSummaryList) {
      if (summary instanceof RangeFunctionInstance) {
        // first, we apply summary on global variables
        RangeState summaryState = ((RangeFunctionInstance) summary).apply();
        newState = applySummary(newState, summaryState, true);
        // second, we apply summary on LHS of function call expression
        if (call instanceof CFunctionCallAssignmentStatement) {
          CLeftHandSide lhs = ((CFunctionCallAssignmentStatement) call).getLeftHandSide();
          MachineModel model = Preconditions.checkNotNull(GlobalInfo.getInstance().getCFAInfo()
              .orNull()).getCFA().getMachineModel();
          AccessPath leftPath = getAccessPath(newState, pOtherStates, lhs, model);
          if (leftPath != null) {
            PathCopyingPersistentTree<String, Range> returnSummary = ((RangeFunctionInstance)
                summary).getReturnSummary();
            PersistentTreeNode<String, Range> returnRoot = returnSummary.getRoot();
            PathCopyingPersistentTree<String, Range> returnTree = PathCopyingPersistentTree.of();
            if (returnRoot != null) {
              String returnVar = Iterables.getOnlyElement(returnRoot.getKeys());
              PersistentTreeNode<String, Range> valueNode = returnRoot.getChild(returnVar);
              if (valueNode != null) {
                returnTree = returnTree.setSubtreeAndCopy(AccessPath.toStrList(leftPath),
                    valueNode);
              }
            }
            RangeState returnState = new RangeState(returnTree);
            newState = applySummary(newState, returnState, true);
          }
        }
      } else if (summary instanceof AccessFunctionInstance) {
        // first, we apply summary on global variables
        List<AccessPath> writtenPaths = ((AccessFunctionInstance) summary).apply().writes;
        for (AccessPath writtenPath : writtenPaths) {
          // check whether the certain path is declared
          if (newState.ranges.getSubtreeRoot(Lists.newArrayList(writtenPath.getQualifiedName()))
              == null) {
            continue;
          }
          // check whether the written access path contains dereference segment
          if (!writtenPath.isActualPath()) {
            continue;
          }
          newState = AccessSummaryApplicator.getInstance().applySummary(newState, writtenPath);
        }
        // second, we apply summary on LHS
        if (call instanceof CFunctionCallAssignmentStatement) {
          CLeftHandSide lhs = ((CFunctionCallAssignmentStatement) call).getLeftHandSide();
          MachineModel model = Preconditions.checkNotNull(GlobalInfo.getInstance().getCFAInfo()
              .orNull()).getCFA().getMachineModel();
          AccessPath leftPath = getAccessPath(newState, pOtherStates, lhs, model);
          if (leftPath != null) {
            newState = AccessSummaryApplicator.getInstance().applySummary(newState, leftPath);
          }
        }
      } else {
        // unsupported summaries
        continue;
      }
    }
    return Collections.singleton(newState);
  }

  @Override
  public Multimap<CFAEdge, AbstractState> applyExternalLoopSummary(
      List<SummaryInstance> pSummaryList,
      CFAEdge inEdge,
      Collection<CFAEdge> outEdges,
      List<AbstractState> pOtherStates) throws CPATransferException {
    Multimap<CFAEdge, RangeState> resultMap = HashMultimap.create();
    for (CFAEdge outEdge : outEdges) {
      resultMap.put(outEdge, this);
    }
    for (SummaryInstance summary : pSummaryList) {
      Multimap<CFAEdge, RangeState> newResultMap = HashMultimap.create();
      if (summary instanceof RangeExternalLoopInstance) {
        for (CFAEdge outEdge : outEdges) {
          RangeState summaryState = ((RangeExternalLoopInstance) summary).apply(inEdge, outEdge);
          for (RangeState state : resultMap.get(outEdge)) {
            RangeState newState = RangeState.copyOf(state);
            newState = applySummary(newState, summaryState, true);
            newResultMap.put(outEdge, newState);
          }
        }
      } else if (summary instanceof AccessExternalLoopInstance) {
        for (CFAEdge outEdge : outEdges) {
          List<AccessPath> writtenPaths = ((AccessExternalLoopInstance) summary).apply(inEdge,
              outEdge).writes;
          for (RangeState state : resultMap.get(outEdge)) {
            RangeState newState = RangeState.copyOf(state);
            for (AccessPath writtenPath : writtenPaths) {
              if (newState.ranges.getSubtreeRoot(Lists.newArrayList(writtenPath.getQualifiedName
                  ())) == null) {
                continue;
              }
              if (!writtenPath.isActualPath()) {
                continue;
              }
              newState = AccessSummaryApplicator.getInstance().applySummary(newState, writtenPath);
            }
            newResultMap.put(outEdge, newState);
          }
        }
      } else {
        // unsupported summaries
        continue;
      }
      resultMap = newResultMap;
    }
    ImmutableMultimap.Builder<CFAEdge, AbstractState> builder = ImmutableMultimap.builder();
    builder.putAll(resultMap);
    return builder.build();
  }

  @Override
  public Collection<? extends AbstractState> applyInternalLoopSummary(
      List<SummaryInstance> pSummaryList, CFAEdge inEdge, List<AbstractState> pOtherStates)
      throws CPATransferException {
    RangeState newState = RangeState.copyOf(this);
    for (SummaryInstance summary : pSummaryList) {
      if (summary instanceof RangeInternalLoopInstance) {
        RangeState summaryState = ((RangeInternalLoopInstance) summary).apply(inEdge);
        newState = applySummary(newState, summaryState, false);
      } else if (summary instanceof AccessInternalLoopInstance) {
        List<AccessPath> writtenPaths = ((AccessInternalLoopInstance) summary).apply(inEdge).writes;
        for(AccessPath writtenPath : writtenPaths) {
          if (newState.ranges.getSubtreeRoot(Lists.newArrayList(writtenPath.getQualifiedName
              ())) == null) {
            continue;
          }
          if (!writtenPath.isActualPath()) {
            continue;
          }
          newState = AccessSummaryApplicator.getInstance().applySummary(newState, writtenPath);
        }
      } else {
        // unsupported summaries
        continue;
      }
    }
    return Collections.singleton(newState);
  }

  private static RangeState applySummary(RangeState state, RangeState summary, boolean pExternal) {
    if (summary == null) {
      // when no summary is available
      return state;
    }
    PathCopyingPersistentTree<String, Range> thisRanges = state.ranges;
    PathCopyingPersistentTree<String, Range> summaryRanges = summary.ranges;
    RangeSummaryUpdateVisitor visitor = new RangeSummaryUpdateVisitor(thisRanges, pExternal);
    summaryRanges.traverse(visitor);
    PathCopyingPersistentTree<String, Range> updatedRanges = visitor.getUpdatedRangeTree();
    return new RangeState(updatedRanges);
  }

  /**
   * Traverse summary information to update range state tree.
   */
  private static class RangeSummaryUpdateVisitor implements TreeVisitor<String, Range> {

    private PathCopyingPersistentTree<String, Range> old;
    private final boolean isExternal;

    RangeSummaryUpdateVisitor(PathCopyingPersistentTree<String, Range> pOld, boolean pIsExternal) {
      old = pOld;
      isExternal = pIsExternal;
    }

    PathCopyingPersistentTree<String, Range> getUpdatedRangeTree() {
      return old;
    }

    @Override
    public TreeVisitStrategy visit(Stack<String> path, Range element, boolean isLeaf) {
      // check if current access path is declared in the old range tree
      if (path.size() == 1 && old.getSubtreeRoot(path) == null) {
        if (isExternal) {
          return TreeVisitStrategy.SKIP;
        }
      }
      if (isLeaf && element != null) {
        Range existRange = old.get(path);
        Range newRange = element;
        if (existRange != null) {
          newRange = newRange.intersect(existRange);
        }
        old = old.setElementAndCopy(path, newRange);
      }
      return TreeVisitStrategy.CONTINUE;
    }
  }

}
