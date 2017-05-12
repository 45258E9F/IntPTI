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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessFunctionInstance;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessResult;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.util.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.shape.values.Address;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressAndState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.PathSegment;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

public final class AccessSummaryApplicator {

  public static Collection<ShapeState> applyFunctionSummary(
      ShapeState pState,
      List<AbstractState> pOtherStates,
      CFunctionReturnEdge pEdge,
      AccessFunctionInstance pInstance)
      throws CPATransferException {
    List<String> paramNames = FluentIterable.from(pEdge.getFunctionEntry().getFunctionParameters()).
        transform(new Function<CParameterDeclaration, String>() {
          @Override
          public String apply(CParameterDeclaration pCParameterDeclaration) {
            return pCParameterDeclaration.getQualifiedName();
          }
        }).toList();
    CFunctionCall call = pEdge.getSummaryEdge().getExpression();
    CFunctionCallExpression callExpression = call.getFunctionCallExpression();
    List<CExpression> arguments = callExpression.getParameterExpressions();
    // Sometimes, parameter list and argument list have inconsistent length, which is usually the
    // case for call of a function with variable number of arguments.
    assert (paramNames.size() <= arguments.size());
    Map<ShapeState, Parameter2Address> addressMap = new HashMap<>();
    addressMap.put(pState, Parameter2Address.of());
    for (int i = 0; i < paramNames.size(); i++) {
      String paramName = paramNames.get(i);
      CExpression argument = arguments.get(i);
      if (!(argument instanceof CLeftHandSide)) {
        continue;
      }
      CLeftHandSide leftArg = (CLeftHandSide) argument;
      Map<ShapeState, Parameter2Address> newAddressMap = new HashMap<>();
      for (Entry<ShapeState, Parameter2Address> entry : addressMap.entrySet()) {
        ShapeState newState = entry.getKey();
        Parameter2Address p2a = entry.getValue();
        List<AddressAndState> addressAndStates = CoreShapeAdapter.getInstance().evaluateAddress
            (newState, pOtherStates, pEdge, leftArg);
        for (AddressAndState addressAndState : addressAndStates) {
          Address address = addressAndState.getObject();
          if (address.isUnknown()) {
            // only known address is added
            continue;
          }
          newState = addressAndState.getShapeState();
          Parameter2Address newP2A = Parameter2Address.copy(p2a);
          newP2A.put(paramName, address);
          newAddressMap.put(newState, newP2A);
        }
      }
      addressMap.clear();
      addressMap.putAll(newAddressMap);
    }
    Set<ShapeState> results = new HashSet<>();
    List<AccessPath> writePaths = pInstance.apply().writes;
    for (Entry<ShapeState, Parameter2Address> entry : addressMap.entrySet()) {
      ShapeState newState = entry.getKey();
      Parameter2Address p2a = entry.getValue();
      for (AccessPath writePath : writePaths) {
        String qualifiedName = writePath.getQualifiedName();
        Address baseAddress = p2a.get(qualifiedName);
        if (baseAddress == null) {
          // CASE 1: the access path does not have the prefix of any function parameter.
          results.add(applySummaryResult(newState, pOtherStates, pEdge, writePath));
        } else {
          // CASE 2: the access path has the prefix of one function parameter.
          List<PathSegment> remSegments = writePath.afterFirstPath();
          CType type = writePath.getType();
          results.add(applySummaryResult(newState, pOtherStates, pEdge, baseAddress, type,
              remSegments));
        }
      }
    }
    // relax the LHS if exists
    if (call instanceof CFunctionCallAssignmentStatement) {
      CLeftHandSide lhs = ((CFunctionCallAssignmentStatement) call).getLeftHandSide();
      CType returnType = CoreShapeAdapter.getType(callExpression);
      Set<ShapeState> newResults = new HashSet<>();
      for (ShapeState resultState : results) {
        List<AddressAndState> addressAndStates = CoreShapeAdapter.getInstance().evaluateAddress
            (resultState, pOtherStates, pEdge, lhs);
        for (AddressAndState addressAndState : addressAndStates) {
          ShapeState nState = addressAndState.getShapeState();
          Address nAddress = addressAndState.getObject();
          if (nAddress.isUnknown()) {
            continue;
          }
          SGObject object = nAddress.getObject();
          ShapeExplicitValue offset = nAddress.getOffset();
          Long newValue = SymbolicValueFactory.getNewValue();
          nState = CoreShapeAdapter.getInstance().writeValue(nState, pOtherStates, pEdge, object,
              offset, returnType, KnownSymbolicValue.valueOf(newValue));
          newResults.add(nState);
        }
      }
      results = newResults;
    }
    // post-process
    if (results.isEmpty()) {
      // if there is no summary to be applied, we should at least return the original state
      results.add(pState);
    }
    return Collections.unmodifiableCollection(results);
  }

  public static Multimap<CFAEdge, ShapeState> applyExternalLoopSummary(
      Multimap<CFAEdge, ShapeState> pEdge2States, List<AbstractState> pOtherStates, CFAEdge
      pInEdge, AccessLoopInstance pInstance) throws CPATransferException {
    ImmutableMultimap.Builder<CFAEdge, ShapeState> builder = ImmutableMultimap.builder();
    for (CFAEdge edge : pEdge2States.keySet()) {
      AccessResult accessResult = pInstance.apply(pInEdge, edge);
      Collection<ShapeState> states = pEdge2States.get(edge);
      Set<ShapeState> successorsForEdge = new HashSet<>();
      for (ShapeState state : states) {
        Collection<ShapeState> newStates = applySummaryResult(state, pOtherStates, edge,
            accessResult.writes);
        successorsForEdge.addAll(newStates);
      }
      builder.putAll(edge, successorsForEdge);
    }
    return builder.build();
  }

  public static Collection<ShapeState> applyInternalLoopSummary(
      ShapeState pState,
      List<AbstractState> pOtherStates,
      CFAEdge pInEdge,
      AccessLoopInstance pInstance)
      throws CPATransferException {
    AccessResult accessResult = pInstance.apply(pInEdge);
    return applySummaryResult(pState, pOtherStates, pInEdge, accessResult.writes);
  }

  private static Collection<ShapeState> applySummaryResult(
      ShapeState pState, List<AbstractState>
      pOtherStates, CFAEdge pEdge, List<AccessPath> writePaths)
      throws CPATransferException {
    ShapeState newState = pState;
    for (AccessPath writePath : writePaths) {
      newState = applySummaryResult(newState, pOtherStates, pEdge, writePath);
    }
    return Collections.singleton(newState);
  }

  private static ShapeState applySummaryResult(
      ShapeState pState, List<AbstractState> pOtherStates,
      CFAEdge pEdge, AccessPath pWritePath)
      throws CPATransferException {
    ShapeState newState = new ShapeState(pState);
    SGHasValueEdge hvEdge = CoreCommunicator.getInstance().getHasValueEdgeFor(pState, pWritePath);
    if (hvEdge != null) {
      SGObject object = hvEdge.getObject();
      int offset = hvEdge.getOffset();
      CType type = hvEdge.getType();
      // STEP 1: remove value(s) on the specified memory interval
      newState = newState.removeValue(object, offset, type);
      // STEP 2: write a fresh value
      Long newValue = SymbolicValueFactory.getNewValue();
      newState = CoreShapeAdapter.getInstance().writeValue(newState, pOtherStates, pEdge, object,
          KnownExplicitValue.valueOf(offset), type, KnownSymbolicValue.valueOf(newValue));
    }
    return newState;
  }

  private static ShapeState applySummaryResult(
      ShapeState pState, List<AbstractState> pOtherStates,
      CFAEdge pEdge, Address pAddress, CType pType,
      List<PathSegment> pRemSegments)
      throws CPATransferException {
    ShapeState newState = new ShapeState(pState);
    SGHasValueEdge hvEdge = CoreCommunicator.getInstance().getHasValueEdgeFor(pState, pAddress,
        pType, pRemSegments);
    if (hvEdge != null) {
      SGObject object = hvEdge.getObject();
      int offset = hvEdge.getOffset();
      CType type = hvEdge.getType();
      newState = newState.removeValue(object, offset, type);
      Long newValue = SymbolicValueFactory.getNewValue();
      newState = CoreShapeAdapter.getInstance().writeValue(newState, pOtherStates, pEdge, object,
          KnownExplicitValue.valueOf(offset), type, KnownSymbolicValue.valueOf(newValue));
    }
    return newState;
  }

  private static class Parameter2Address {

    Map<String, Address> name2Address = new HashMap<>();

    private Parameter2Address() {
    }

    private Parameter2Address(Parameter2Address pP2A) {
      name2Address.putAll(pP2A.name2Address);
    }

    static Parameter2Address of() {
      return new Parameter2Address();
    }

    static Parameter2Address copy(Parameter2Address pP2A) {
      return new Parameter2Address(pP2A);
    }

    void put(String pName, Address pAddress) {
      name2Address.put(pName, pAddress);
    }

    @Nullable
    Address get(String pName) {
      return name2Address.get(pName);
    }

  }

}
