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
package org.sosy_lab.cpachecker.cpa.shape.communicator;

import static org.sosy_lab.cpachecker.cpa.range.CompInteger.IntegerStatus.NORM;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeExternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeFunctionInstance;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeInternalLoopInstance;
import org.sosy_lab.cpachecker.cpa.range.CompInteger;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.cpa.range.util.Ranges;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SEs;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.values.Address;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressAndState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.PathSegment;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;
import org.sosy_lab.cpachecker.util.collections.tree.TreeVisitor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.annotation.Nullable;

public final class RangeSummaryApplicator {

  public static Collection<ShapeState> applyFunctionSummary(
      ShapeState pState,
      List<AbstractState> pOtherStates,
      CFunctionReturnEdge pEdge,
      RangeFunctionInstance pInstance)
      throws CPATransferException {
    ShapeState newState = new ShapeState(pState);
    PathCopyingPersistentTree<String, Range> globalSummary = pInstance.getGlobalSummary();
    // visit global summary tree to apply summary
    CollectConstraintVisitor visitor = new CollectConstraintVisitor(newState);
    globalSummary.traverse(visitor);
    List<SymbolicExpression> globalConstraints = visitor.getConstraints();
    if (globalConstraints == null) {
      return Collections.emptySet();
    }
    for (SymbolicExpression se : globalConstraints) {
      newState.addConstraint(se);
    }
    // apply return value summary to the LHS
    CFunctionCall call = pEdge.getSummaryEdge().getExpression();
    CType returnType = CoreShapeAdapter.getType(call.getFunctionCallExpression());
    if (call instanceof CFunctionCallAssignmentStatement) {
      CLeftHandSide lhs = ((CFunctionCallAssignmentStatement) call).getLeftHandSide();
      PathCopyingPersistentTree<String, Range> returnSummary = pInstance.getReturnSummary();
      // derive the base address of the LHS
      List<AddressAndState> addressAndStates = CoreShapeAdapter.getInstance().evaluateAddress
          (newState, pOtherStates, pEdge, lhs);
      Set<ShapeState> newStates = new HashSet<>();
      for (AddressAndState addressAndState : addressAndStates) {
        Address address = addressAndState.getObject();
        if (address.isUnknown()) {
          continue;
        }
        ShapeState nState = addressAndState.getShapeState();
        LHSConstraintVisitor visitor1 = new LHSConstraintVisitor(nState, address, returnType);
        returnSummary.traverse(visitor1);
        List<SymbolicExpression> returnConstraints = visitor1.getConstraints();
        if (returnConstraints != null) {
          for (SymbolicExpression se : returnConstraints) {
            nState.addConstraint(se);
          }
          newStates.add(nState);
        }
      }
      return Collections.unmodifiableSet(newStates);
    }
    return Collections.singleton(newState);
  }

  public static Multimap<CFAEdge, ShapeState> applyExternalLoopSummary(
      Multimap<CFAEdge, ShapeState> pStateMap,
      CFAEdge pInEdge,
      RangeExternalLoopInstance pInstance)
      throws CPATransferException {
    ImmutableMultimap.Builder<CFAEdge, ShapeState> builder = ImmutableMultimap.builder();
    for (CFAEdge edge : pStateMap.keySet()) {
      RangeState summaryState = pInstance.apply(pInEdge, edge);
      if (summaryState == null) {
        continue;
      }
      PathCopyingPersistentTree<String, Range> summaryTree = summaryState.getRanges();
      Collection<ShapeState> states = pStateMap.get(edge);
      for (ShapeState state : states) {
        CollectConstraintVisitor visitor = new CollectConstraintVisitor(state);
        summaryTree.traverse(visitor);
        List<SymbolicExpression> extraConstraints = visitor.getConstraints();
        if (extraConstraints != null) {
          ShapeState newState = new ShapeState(state);
          for (SymbolicExpression se : extraConstraints) {
            newState.addConstraint(se);
          }
          builder.put(edge, newState);
        }
      }
    }
    return builder.build();
  }

  public static Collection<ShapeState> applyInternalLoopSummary(
      ShapeState pState,
      CFAEdge pInEdge,
      RangeInternalLoopInstance pInstance)
      throws CPATransferException {
    RangeState summaryState = pInstance.apply(pInEdge);
    if (summaryState == null) {
      return Collections.emptySet();
    }
    PathCopyingPersistentTree<String, Range> summaryTree = summaryState.getRanges();
    CollectConstraintVisitor visitor = new CollectConstraintVisitor(pState);
    summaryTree.traverse(visitor);
    List<SymbolicExpression> extraConstraints = visitor.getConstraints();
    if (extraConstraints == null) {
      return Collections.emptySet();
    }
    ShapeState newState = new ShapeState(pState);
    for (SymbolicExpression se : extraConstraints) {
      newState.addConstraint(se);
    }
    return Collections.singleton(newState);
  }

  /**
   * Convert an access path along with its associated range into symbolic expression(s).
   * @param path path list composing the complete access path
   * @param r range in interval
   * @return a set of converted constraints, NULL for unreachable case (no successors)
   */
  private static List<SymbolicExpression> convert(ShapeState pState, List<String> path, Range r,
                                                  MachineModel pModel) {
    Pair<String, List<PathSegment>> nameAndSegments = AccessPath.reverseParse(path);
    String qualifiedName = nameAndSegments.getFirstNotNull();
    List<PathSegment> segments = nameAndSegments.getSecondNotNull();
    Pair<ShapeSymbolicValue, CType> valueAndType = CoreCommunicator.getInstance().getValueFor(
        pState, qualifiedName, segments);
    ShapeSymbolicValue value = valueAndType.getFirstNotNull();
    CType type = valueAndType.getSecond();
    return convert(value, type, r, pModel);
  }

  /**
   * Convert the tuple (base address, reminiscent path list) into symbolic expression(s).
   * @param pState current shape state
   * @param pBaseAddress base address
   * @param path access path segment list
   * @param r range in interval
   * @param pModel machine model
   * @return a set of converted constraints
   */
  private static List<SymbolicExpression> convert(ShapeState pState, Address pBaseAddress,
                                                  List<String> path, CType returnType,  Range r,
                                                  MachineModel pModel) {
    Pair<String, List<PathSegment>> nameAndSegments = AccessPath.reverseParse(path);
    List<PathSegment> segments = nameAndSegments.getSecondNotNull();
    Pair<ShapeSymbolicValue, CType> valueAndType = CoreCommunicator.getInstance().getValueFor
        (pState, pBaseAddress, returnType, segments);
    ShapeSymbolicValue value = valueAndType.getFirstNotNull();
    CType type = valueAndType.getSecond();
    return convert(value, type, r, pModel);
  }

  private static List<SymbolicExpression> convert(ShapeSymbolicValue value, @Nullable CType type,
                                                  Range r, MachineModel machineModel) {
    List<SymbolicExpression> constraints = new ArrayList<>();
    if (!value.isUnknown()) {
      if (type == null || !(type instanceof CSimpleType)) {
        return constraints;
      }
      if (r.isEmpty()) {
        return null;
      }
      KnownSymbolicValue actualValue = (KnownSymbolicValue) value;
      SymbolicExpression var = SEs.toConstant(actualValue, type);
      Range typeRange = Ranges.getTypeRange(type, machineModel);
      // check if the upper/lower bound is necessary
      CompInteger exLower = r.getLow();
      CompInteger exUpper = r.getHigh();
      CompInteger origLower = typeRange.getLow();
      CompInteger origUpper = typeRange.getHigh();
      if (exLower.compareTo(origLower) > 0) {
        // generate constraint for lower bound
        if (exLower.getStatus() == NORM) {
          BigInteger wrappedValue = exLower.getValue();
          SymbolicExpression constraint = SEs.makeBinary(var, SEs.toConstant(KnownExplicitValue
              .valueOf(wrappedValue), type), BinaryOperator.GREATER_EQUAL, CNumericTypes.INT);
          constraints.add(constraint);
        }
      }
      if (exUpper.compareTo(origUpper) < 0) {
        // generate constraint for upper bound
        if (exUpper.getStatus() == NORM) {
          BigInteger wrappedValue = exUpper.getValue();
          SymbolicExpression constraint = SEs.makeBinary(var, SEs.toConstant(KnownExplicitValue
              .valueOf(wrappedValue), type), BinaryOperator.LESS_EQUAL, CNumericTypes.INT);
          constraints.add(constraint);
        }
      }
    }
    return constraints;
  }

  private static class CollectConstraintVisitor implements TreeVisitor<String, Range> {

    private final ShapeState innerState;
    private final MachineModel machineModel;
    // constraint pool is not final because we set it to NULL if no successor should exist
    private List<SymbolicExpression> constraints;

    CollectConstraintVisitor(ShapeState pState) {
      innerState = pState;
      machineModel = innerState.getShapeGraph().getMachineModel();
      constraints = new ArrayList<>();
    }

    @Override
    public TreeVisitStrategy visit(
        Stack<String> path, Range element, boolean isLeaf) {
      if (isLeaf && element != null) {
        List<SymbolicExpression> subConstraints = convert(innerState, path, element, machineModel);
        if (subConstraints != null) {
          constraints.addAll(subConstraints);
          return TreeVisitStrategy.CONTINUE;
        } else {
          constraints = null;
          // no need to handle other ranges
          return TreeVisitStrategy.ABORT;
        }
      }
      return TreeVisitStrategy.CONTINUE;
    }


    @Nullable
    List<SymbolicExpression> getConstraints() {
      return constraints;
    }
  }

  private static class LHSConstraintVisitor implements TreeVisitor<String, Range> {

    private final ShapeState innerState;
    private final MachineModel machineModel;
    private final Address baseAddress;
    private final CType returnType;
    private List<SymbolicExpression> constraints;

    LHSConstraintVisitor(ShapeState pState, Address pBaseAddress, CType pReturnType) {
      innerState = pState;
      machineModel = innerState.getShapeGraph().getMachineModel();
      baseAddress = pBaseAddress;
      returnType = pReturnType;
      constraints = new ArrayList<>();
    }

    @Override
    public TreeVisitStrategy visit(Stack<String> path, Range element, boolean isLeaf) {
      if (isLeaf && element != null) {
        List<SymbolicExpression> subConstraints = convert(innerState, baseAddress, path,
            returnType, element, machineModel);
        if (subConstraints != null) {
          constraints.addAll(subConstraints);
          return TreeVisitStrategy.CONTINUE;
        } else {
          constraints = null;
          return TreeVisitStrategy.ABORT;
        }
      }
      return TreeVisitStrategy.CONTINUE;
    }

    @Nullable
    List<SymbolicExpression> getConstraints() {
      return constraints;
    }
  }


}
