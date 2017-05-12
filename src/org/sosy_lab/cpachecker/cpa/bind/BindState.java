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
package org.sosy_lab.cpachecker.cpa.bind;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.SwitchableGraphable;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree.TreeLessEqualComparator;
import org.sosy_lab.cpachecker.util.collections.tree.TreeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

public class BindState implements LatticeAbstractState<BindState>, SwitchableGraphable {

  public static final BindState topElement = new BindState();

  private BindState stateOnLastFunctionCall;

  private PathCopyingPersistentTree<String, BindingPoint> localDefs;
  private PathCopyingPersistentTree<String, BindingPoint> globalDefs;

  public BindState() {
    this.stateOnLastFunctionCall = null;
    this.localDefs = new PathCopyingPersistentTree<>();
    this.globalDefs = new PathCopyingPersistentTree<>();
  }

  public BindState(
      BindState pStateOnLastFunctionCall,
      PathCopyingPersistentTree<String, BindingPoint> pLocalDefs,
      PathCopyingPersistentTree<String, BindingPoint> pGlobalDefs) {
    this.stateOnLastFunctionCall = pStateOnLastFunctionCall;
    this.localDefs = pLocalDefs;
    this.globalDefs = pGlobalDefs;
  }

  /**
   * Create binding state with global variable names (at the very beginning of the CFA)
   */
  public BindState(Set<AccessPath> globalVariableNames) {
    this.stateOnLastFunctionCall = null;
    this.globalDefs =
        addVariables(PathCopyingPersistentTree.<String, BindingPoint>of(), globalVariableNames);
    this.localDefs = new PathCopyingPersistentTree<>();
  }

  private static PathCopyingPersistentTree<String, BindingPoint> addVariables(
      PathCopyingPersistentTree<String, BindingPoint> tree, Set<AccessPath> variableNames) {
    for (AccessPath name : variableNames) {
      tree = tree.setElementAndCopy(
          AccessPath.toStrList(name),
          UndefinedBindingPoint.getInstance()
      );
    }
    return tree;
  }

  // public BindingAnalysisState
  public BindingPoint getDefinition(AccessPath ap) {
    List<String> path = AccessPath.toStrList(ap);
    // local variables overrides global ones
    BindingPoint bp = localDefs.get(path);
    if (bp == null) {
      bp = globalDefs.get(path);
    }
    return bp;
  }

  public Set<AccessPath> getDepencencies(AccessPath ap) {
    // TODO: this is used to retrieve data dependencies of variables, which can be
    // used to obtain a concise counter example path.
    // We leave it as future work.
    return null;
  }

  /**
   * Deep copy a state
   */
  public static BindState copyOf(BindState pState) {
    return new BindState(pState.stateOnLastFunctionCall, pState.localDefs, pState.globalDefs);
  }

  @Override
  public int hashCode() {
    // tail-recursive style
    return Objects.hash(globalDefs, localDefs, stateOnLastFunctionCall);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof BindState)) {
      return false;
    }
    BindState that = (BindState) obj;
    return Objects.equals(localDefs, that.localDefs) && Objects.equals(globalDefs, that
        .globalDefs) && Objects.equals(stateOnLastFunctionCall, that.stateOnLastFunctionCall);
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }

  @Override
  public boolean getActiveStatus() {
    return false;
  }

  public static abstract class BindingPoint {

    public abstract boolean haveSuchWrite(AccessPath path);

    public static boolean isLessEqual(BindingPoint a, BindingPoint b) {
      Preconditions.checkNotNull(a);
      Preconditions.checkNotNull(b);
      if (a instanceof UndefinedBindingPoint || b instanceof UnknownBindingPoint) {
        return true;
      }
      if (a instanceof UnknownBindingPoint || b instanceof UndefinedBindingPoint) {
        return false;
      }
      assert (a instanceof ProgramBinding && b instanceof ProgramBinding);
      return a.equals(b);
    }
  }

  /**
   * Bottom: the access path is not yet assigned a value
   *
   * Singleton
   */
  public static class UndefinedBindingPoint extends BindingPoint {
    private static final UndefinedBindingPoint INSTANCE = new UndefinedBindingPoint();

    private UndefinedBindingPoint() {
    }

    public static UndefinedBindingPoint getInstance() {
      return INSTANCE;
    }

    @Override
    public boolean haveSuchWrite(AccessPath path) {
      return false;
    }

    @Override
    public String toString() {
      return "[Undefined]";
    }
  }

  /**
   * Top: not sure where is it bound, i.e., multiple binding points or the binding point is to
   * complex that we determine not to record it (e.g., bound in function call).
   *
   * Singleton
   */
  public static class UnknownBindingPoint extends BindingPoint {
    private static final UnknownBindingPoint INSTANCE = new UnknownBindingPoint();

    private UnknownBindingPoint() {
    }

    public static UnknownBindingPoint getInstance() {
      return INSTANCE;
    }

    @Override
    public boolean haveSuchWrite(AccessPath path) {
      return false;
    }

    @Override
    public String toString() {
      return "[*]";
    }
  }

  /**
   * The binding is defined a CFAEdge
   *
   * It is not a CExpression because the declaration with initializer is not a CExpression but a
   * CInitializer
   */
  public static class ProgramBinding extends BindingPoint {

    private final CFAEdge definition;

    private PathCopyingPersistentTree<String, Integer> writes;

    public ProgramBinding(CFAEdge definition, List<AbstractState> otherStates) {
      this.definition = definition;
      try {
        writes = BindUtils.getWritesFromCFAEdge(definition, otherStates);
      } catch (UnrecognizedCCodeException e) {
        // if there is problem in parsing CFA edge, we clear the write set
        writes = PathCopyingPersistentTree.of();
      }
    }

    @Override
    public boolean haveSuchWrite(AccessPath path) {
      List<Integer> values = writes.trace(AccessPath.toStrList(path));
      for (Integer value : values) {
        if (value != null) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || !(o instanceof ProgramBinding)) {
        return false;
      }
      ProgramBinding that = (ProgramBinding) o;
      return this.definition.equals(that.definition);
    }

    @Override
    public int hashCode() {
      return Objects.hash(definition);
    }

    @Override
    public String toString() {
      return definition.toString();
    }
  }

  @Override
  public BindState join(BindState that) {
    throw new RuntimeException(
        "Binding Analysis is not supposed to perform 'join', please always use MergeSepOperator.");
  }

  @Override
  public boolean isLessOrEqual(BindState that) throws CPAException, InterruptedException {
    // does not compare the 'stateOnLastFunctionCall', let other CPA handle the call stack
    // compare only the bindings

    TreeLessEqualComparator<String, BindingPoint> comparator =
        new TreeLessEqualComparator<String, BindingPoint>() {
          @Override
          public boolean isLessEqual(BindingPoint pA, BindingPoint pB) {
            return BindingPoint.isLessEqual(pA, pB);
          }
        };

    return PathCopyingPersistentTree.isLessEqual(this.localDefs, that.localDefs, comparator) &&
        PathCopyingPersistentTree.isLessEqual(this.globalDefs, that.globalDefs, comparator);
  }

  /**
   * Create a BindState by pushing a call stack to the current state
   *
   * @param parameters formal parameters
   * @param localVars  the set of local variables, initialize them as undefined.
   * @return the new state
   */
  public BindState pushCallStack(
      List<CParameterDeclaration> parameters, Set<AccessPath>
      localVars, CFAEdge edge, List<AbstractState> otherStates) {
    // Note: we do not retain access paths out of current range
    PathCopyingPersistentTree<String, BindingPoint> localDefs = PathCopyingPersistentTree.of();
    // set local variables to undefined
    for (AccessPath local : localVars) {
      localDefs = localDefs.setElementAndCopy(
          AccessPath.toStrList(local),
          UndefinedBindingPoint.getInstance()
      );
    }
    // set parameters as defined by the actual arguments
    for (CParameterDeclaration cpd : parameters) {
      localDefs = localDefs.setElementAndCopy(
          AccessPath.toStrList(new AccessPath(cpd)),
          new ProgramBinding(edge, otherStates)
      );
    }
    return new BindState(this, localDefs, this.globalDefs);
  }

  public BindState addLocalBinding(AccessPath ap, CFAEdge edge, List<AbstractState> otherStates) {
    return new BindState(
        this.stateOnLastFunctionCall,
        this.localDefs.setSubtreeAndNullifyPath(AccessPath.toStrList(ap), new ProgramBinding
            (edge, otherStates)),
        this.globalDefs
    );
  }

  public BindState addGlobalBinding(AccessPath ap, CFAEdge edge, List<AbstractState> otherStates) {
    return new BindState(
        this.stateOnLastFunctionCall,
        this.localDefs,
        this.globalDefs.setSubtreeAndNullifyPath(AccessPath.toStrList(ap), new ProgramBinding
            (edge, otherStates))
    );
  }

  private BindState removeLocalBinding(AccessPath ap) {
    return new BindState(
        this.stateOnLastFunctionCall,
        this.localDefs.setSubtreeAndNullifyPath(AccessPath.toStrList(ap), UndefinedBindingPoint
            .getInstance()),
        this.globalDefs
    );
  }

  private BindState removeGlobalBinding(AccessPath ap) {
    return new BindState(
        this.stateOnLastFunctionCall,
        this.localDefs,
        this.globalDefs.setSubtreeAndNullifyPath(AccessPath.toStrList(ap), UndefinedBindingPoint
            .getInstance())
    );
  }


  public BindState popCallStack() {
    Preconditions.checkNotNull(stateOnLastFunctionCall);
    return new BindState(
        stateOnLastFunctionCall.stateOnLastFunctionCall,
        stateOnLastFunctionCall.localDefs,
        globalDefs
    );
  }

  /**
   * Check if the 'ap' is targeted at 'global' or 'local' variable.
   * Note that this is not fully accurate because is does not perform alias analysis
   * For instance. global_var->field1 may points to a memory location in heap,
   * but it is recorded in globalDefs
   */
  public BindState updateBinding(AccessPath ap, CFAEdge edge, List<AbstractState> otherStates) {
    if (ap.startFromGlobal()) {
      return addGlobalBinding(ap, edge, otherStates);
    } else {
      return addLocalBinding(ap, edge, otherStates);
    }
  }

  public BindState removeBinding(AccessPath ap) {
    if (ap.startFromGlobal()) {
      return removeGlobalBinding(ap);
    } else {
      return removeLocalBinding(ap);
    }
  }

  private class SpuriousPathVisitor implements TreeVisitor<String, BindingPoint> {

    private PathCopyingPersistentTree<String, Integer> spuriousPaths = new
        PathCopyingPersistentTree<>();
    private final AccessPath ap;

    SpuriousPathVisitor(AccessPath pPath) {
      ap = pPath;
    }

    PathCopyingPersistentTree<String, Integer> getSpuriousPaths() {
      return spuriousPaths;
    }

    @Override
    public TreeVisitStrategy visit(
        Stack<String> path, BindingPoint element, boolean isLeaf) {
      if (element != null && element.haveSuchWrite(ap)) {
        // record the path for removal
        List<String> path0 = Lists.newArrayList(path);
        spuriousPaths = spuriousPaths.setElementAndCopy(path0, 0);
      }
      return TreeVisitStrategy.CONTINUE;
    }
  }

  private class PathPruneVisitor implements TreeVisitor<String, Integer> {

    private PathCopyingPersistentTree<String, BindingPoint> defs;

    PathPruneVisitor(PathCopyingPersistentTree<String, BindingPoint> pDefs) {
      defs = pDefs;
    }

    PathCopyingPersistentTree<String, BindingPoint> getDefs() {
      return defs;
    }

    @Override
    public TreeVisitStrategy visit(Stack<String> path, Integer element, boolean isLeaf) {
      if (element != null) {
        defs = defs.setSubtreeAndCopy(path, UndefinedBindingPoint.getInstance());
      }
      return TreeVisitStrategy.CONTINUE;
    }
  }

  private PathCopyingPersistentTree<String, BindingPoint> removeBindingWithSuchWrite(
      final AccessPath ap, PathCopyingPersistentTree<String, BindingPoint> defs) {
    SpuriousPathVisitor visitor = new SpuriousPathVisitor(ap);
    defs.traverse(visitor);
    PathCopyingPersistentTree<String, Integer> spuriousPaths = visitor.getSpuriousPaths();
    PathPruneVisitor pruneVisitor = new PathPruneVisitor(defs);
    spuriousPaths.traverse(pruneVisitor);
    return pruneVisitor.getDefs();
  }

  BindState removeBindingWithSuchWrite(final AccessPath ap) {
    localDefs = removeBindingWithSuchWrite(ap, localDefs);
    globalDefs = removeBindingWithSuchWrite(ap, globalDefs);
    return new BindState(this.stateOnLastFunctionCall, localDefs, globalDefs);
  }

  public PathCopyingPersistentTree<String, BindingPoint> getLocalBindings() {
    return localDefs;
  }

  public PathCopyingPersistentTree<String, BindingPoint> getGlobalBindings() {
    return globalDefs;
  }

  /**
   * Given an access path, enumerate all binding expressions
   *
   * @param path        an access path
   * @param otherStates list of other components of abstract state, for computing access path
   * @param level       level of bind unwinding to prevent recursion issue
   * @return a list of binding expressions
   */
  public List<CRightHandSide> getBindedExpression(
      AccessPath path,
      List<AbstractState> otherStates,
      int level)
      throws UnrecognizedCCodeException {
    if (level <= 0) {
      return Lists.newArrayList();
    }
    BindingPoint bindPoint;
    List<CRightHandSide> candidates = new ArrayList<>();
    if (path.startFromGlobal()) {
      bindPoint = globalDefs.get(AccessPath.toStrList(path));
    } else {
      bindPoint = localDefs.get(AccessPath.toStrList(path));
    }
    if (bindPoint != null && bindPoint instanceof ProgramBinding) {
      CFAEdge thisEdge = ((ProgramBinding) bindPoint).definition;
      CRightHandSide rightHand = BindUtils.extractRightHandSide(thisEdge, path);
      if (rightHand != null) {
        candidates.add(rightHand);
      }
      if (rightHand instanceof CLeftHandSide) {
        AccessPath newPath = ((CLeftHandSide) rightHand).accept(new AccessPathExtractorForLHS
            (otherStates));
        if (newPath != null) {
          candidates.addAll(getBindedExpression(newPath, otherStates, level - 1));
        }
      }
    }
    return candidates;
  }

  @Override
  public String toDOTLabel() {
    final StringBuilder sb = new StringBuilder();
    final List<String> collector = new ArrayList<>();
    TreeVisitor<String, BindingPoint> visitor = new TreeVisitor<String, BindingPoint>() {
      @Override
      public TreeVisitStrategy visit(Stack<String> path, BindingPoint bp, boolean isLeaf) {
        if (bp != null) {
          collector.add(Joiner.on('.').join(path) + " <-- " + bp.toString());
        }
        return TreeVisitStrategy.CONTINUE;
      }

    };
    sb.append("\n<<< Local >>>\n");
    localDefs.traverse(visitor);
    sb.append(Joiner.on("\n").join(collector));
    collector.clear();
    sb.append("\n<<< Global >>>\n");
    globalDefs.traverse(visitor);
    sb.append(Joiner.on("\n").join(collector));
    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}