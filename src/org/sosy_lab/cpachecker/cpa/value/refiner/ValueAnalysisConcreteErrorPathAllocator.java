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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.counterexample.Address;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcreteStatePathNode;
import org.sosy_lab.cpachecker.core.counterexample.IDExpression;
import org.sosy_lab.cpachecker.core.counterexample.LeftHandSide;
import org.sosy_lab.cpachecker.core.counterexample.Memory;
import org.sosy_lab.cpachecker.core.counterexample.MemoryName;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class ValueAnalysisConcreteErrorPathAllocator {

  private static final MemoryName MEMORY_NAME = new MemoryName() {

    @Override
    public String getMemoryName(CRightHandSide pExp, Address pAddress) {
      return "Value_Analysis_Heap";
    }
  };

  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;

  public ValueAnalysisConcreteErrorPathAllocator(
      Configuration pConfig,
      LogManager pLogger,
      MachineModel pMachineModel) throws InvalidConfigurationException {
    this.assumptionToEdgeAllocator = new AssumptionToEdgeAllocator(pConfig, pLogger, pMachineModel);
  }

  public ConcreteStatePath allocateAssignmentsToPath(ARGPath pPath) {

    List<Pair<ValueAnalysisState, CFAEdge>> path = new ArrayList<>(pPath.size());

    PathIterator it = pPath.pathIterator();

    while (it.hasNext()) {
      it.advance();
      ValueAnalysisState state =
          AbstractStates.extractStateByType(it.getAbstractState(), ValueAnalysisState.class);
      CFAEdge edge = it.getIncomingEdge();

      if (state == null) {
        return null;
      }

      path.add(Pair.of(state, edge));
    }

    return createConcreteStatePath(path);
  }

  public CFAPathWithAssumptions allocateAssignmentsToPath(List<Pair<ValueAnalysisState, CFAEdge>> pPath) {
    ConcreteStatePath concreteStatePath = createConcreteStatePath(pPath);
    return CFAPathWithAssumptions.of(concreteStatePath, assumptionToEdgeAllocator);
  }

  private ConcreteStatePath createConcreteStatePath(List<Pair<ValueAnalysisState, CFAEdge>> pPath) {

    List<ConcreteStatePathNode> result = new ArrayList<>(pPath.size());

    /*"We generate addresses for our memory locations.
     * This avoids needing to get the CDeclaration
     * representing each memory location, which would be necessary if we
     * wanted to exactly map each memory location to a LeftHandSide.*/
    Map<LeftHandSide, Address> variableAddresses =
        generateVariableAddresses(
            FluentIterable.from(pPath).transform(Pair.<ValueAnalysisState>getProjectionToFirst()));

    for (Pair<ValueAnalysisState, CFAEdge> edgeStatePair : pPath) {

      ValueAnalysisState valueState = edgeStatePair.getFirst();
      CFAEdge edge = edgeStatePair.getSecond();

      ConcreteStatePathNode node;

      if (edge.getEdgeType() == CFAEdgeType.MultiEdge) {

        node = createMultiEdge(valueState, (MultiEdge) edge, variableAddresses);
      } else {
        ConcreteState concreteState = createConcreteState(valueState, variableAddresses);
        node = ConcreteStatePath.valueOfPathNode(concreteState, edge);
      }

      result.add(node);
    }


    return new ConcreteStatePath(result);
  }

  public static ConcreteState createConcreteState(ValueAnalysisState pValueState) {
    Map<LeftHandSide, Address> variableAddresses =
        generateVariableAddresses(Collections.singleton(pValueState));
    return createConcreteState(pValueState, variableAddresses);
  }

  private ConcreteStatePathNode createMultiEdge(
      ValueAnalysisState pValueState, MultiEdge multiEdge,
      Map<LeftHandSide, Address> pVariableAddresses) {

    int size = multiEdge.getEdges().size();

    ConcreteState[] singleConcreteStates = new ConcreteState[size];

    ListIterator<CFAEdge> iterator = multiEdge.getEdges().listIterator(size);

    Set<CLeftHandSide> alreadyAssigned = new HashSet<>();

    // we have the state for the last edge
    iterator.previous();
    singleConcreteStates[size - 1] = createConcreteState(pValueState, pVariableAddresses);

    int index = size - 2;

    while (iterator.hasPrevious()) {
      CFAEdge cfaEdge = iterator.previous();

      ConcreteState state;

      // We know only values for LeftHandSides that have not yet been assigned.
      if (allValuesForLeftHandSideKnown(cfaEdge, alreadyAssigned)) {
        state = createConcreteState(pValueState, pVariableAddresses);
      } else {
        state = ConcreteState.empty();
      }
      singleConcreteStates[index] = state;

      addLeftHandSide(cfaEdge, alreadyAssigned);
      index--;
    }

    return ConcreteStatePath.valueOfPathNode(Arrays.asList(singleConcreteStates), multiEdge);
  }

  private boolean allValuesForLeftHandSideKnown(
      CFAEdge pCfaEdge,
      Set<CLeftHandSide> pAlreadyAssigned) {

    if (pCfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      return isDeclarationValueKnown((CDeclarationEdge) pCfaEdge, pAlreadyAssigned);
    } else if (pCfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      return isStatementValueKnown((CStatementEdge) pCfaEdge, pAlreadyAssigned);
    }

    return false;
  }

  private void addLeftHandSide(CFAEdge pCfaEdge, Set<CLeftHandSide> pAlreadyAssigned) {

    if (pCfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      CStatement stmt = ((CStatementEdge) pCfaEdge).getStatement();

      if (stmt instanceof CAssignment) {
        CLeftHandSide lhs = ((CAssignment) stmt).getLeftHandSide();
        pAlreadyAssigned.add(lhs);
      }
    }
  }

  private boolean isStatementValueKnown(
      CStatementEdge pCfaEdge,
      Set<CLeftHandSide> pAlreadyAssigned) {

    CStatement stmt = pCfaEdge.getStatement();

    if (stmt instanceof CAssignment) {
      CLeftHandSide leftHandSide = ((CAssignment) stmt).getLeftHandSide();

      return isLeftHandSideValueKnown(leftHandSide, pAlreadyAssigned);
    }

    // If the statement is not an assignment, the lvalue does not exist
    return true;
  }

  private boolean isLeftHandSideValueKnown(
      CLeftHandSide pLHS,
      Set<CLeftHandSide> pAlreadyAssigned) {

    ValueKnownVisitor v = new ValueKnownVisitor(pAlreadyAssigned);
    return pLHS.accept(v);
  }

  /**
   * Checks, if we know a value. This is the case, if the value will not be assigned in the future.
   * Since we traverse the multi edge from bottom to top, this means if a left hand side, that was
   * already assigned, may not be part of the Left Hand Side we want to know the value of.
   */
  private static class ValueKnownVisitor
      extends DefaultCExpressionVisitor<Boolean, RuntimeException> {

    private final Set<CLeftHandSide> alreadyAssigned;

    public ValueKnownVisitor(Set<CLeftHandSide> pAlreadyAssigned) {
      alreadyAssigned = pAlreadyAssigned;
    }

    @Override
    protected Boolean visitDefault(CExpression pExp) {
      return true;
    }

    @Override
    public Boolean visit(CArraySubscriptExpression pE) {
      return !alreadyAssigned.contains(pE);
    }

    @Override
    public Boolean visit(CBinaryExpression pE) {
      return pE.getOperand1().accept(this)
          && pE.getOperand2().accept(this);
    }

    @Override
    public Boolean visit(CCastExpression pE) {
      return pE.getOperand().accept(this);
    }

    //TODO Complex Cast
    @Override
    public Boolean visit(CFieldReference pE) {
      return !alreadyAssigned.contains(pE);
    }

    @Override
    public Boolean visit(CIdExpression pE) {
      return !alreadyAssigned.contains(pE);
    }

    @Override
    public Boolean visit(CPointerExpression pE) {
      return !alreadyAssigned.contains(pE);
    }

    @Override
    public Boolean visit(CUnaryExpression pE) {
      return pE.getOperand().accept(this);
    }
  }


  private boolean isDeclarationValueKnown(
      CDeclarationEdge pCfaEdge,
      Set<CLeftHandSide> pAlreadyAssigned) {

    CDeclaration dcl = pCfaEdge.getDeclaration();

    if (dcl instanceof CVariableDeclaration) {
      CIdExpression idExp = new CIdExpression(dcl.getFileLocation(), dcl);

      return isLeftHandSideValueKnown(idExp, pAlreadyAssigned);
    }

    // only variable declaration matter for value analysis
    return true;
  }

  private static Map<LeftHandSide, Address> generateVariableAddresses(Iterable<ValueAnalysisState> pPath) {

    // Get all base IdExpressions for memory locations, ignoring the offset
    Multimap<IDExpression, MemoryLocation> memoryLocationsInPath =
        getAllMemoryLocationsInPath(pPath);

    // Generate consistent Addresses, with non overlapping fields.
    return generateVariableAddresses(memoryLocationsInPath);
  }

  private static Map<LeftHandSide, Address> generateVariableAddresses(Multimap<IDExpression, MemoryLocation> pMemoryLocationsInPath) {

    Map<LeftHandSide, Address> result =
        Maps.newHashMapWithExpectedSize(pMemoryLocationsInPath.size());

    // Start with Address 0
    Address nextAddressToBeAssigned = Address.valueOf(BigInteger.ZERO);

    for (IDExpression variable : pMemoryLocationsInPath.keySet()) {
      result.put(variable, nextAddressToBeAssigned);

      // leave enough space for values between addresses
      nextAddressToBeAssigned =
          generateNextAddresses(pMemoryLocationsInPath.get(variable), nextAddressToBeAssigned);

    }

    return result;
  }

  private static Address generateNextAddresses(
      Collection<MemoryLocation> pCollection,
      Address pNextAddressToBeAssigned) {

    long biggestStoredOffsetInPath = 0;

    for (MemoryLocation loc : pCollection) {
      if (loc.isReference() && loc.getOffset() > biggestStoredOffsetInPath) {
        biggestStoredOffsetInPath = loc.getOffset();
      }
    }

    // Leave enough space for a long Value
    // TODO find good value
    long spaceForLastValue = 64;
    BigInteger offset = BigInteger.valueOf(biggestStoredOffsetInPath + spaceForLastValue);

    return pNextAddressToBeAssigned.addOffset(offset);
  }

  private static Multimap<IDExpression, MemoryLocation> getAllMemoryLocationsInPath(
      Iterable<ValueAnalysisState> pPath) {

    Multimap<IDExpression, MemoryLocation> result = HashMultimap.create();

    for (ValueAnalysisState valueState : pPath) {
      putIfNotExists(valueState, result);
    }
    return result;
  }

  private static void putIfNotExists(
      ValueAnalysisState pState, Multimap<IDExpression, MemoryLocation> memoryLocationMap) {
    ValueAnalysisState valueState = pState;

    for (MemoryLocation loc : valueState.getConstantsMapView().keySet()) {
      IDExpression idExp = createBaseIdExpresssion(loc);

      if (!memoryLocationMap.containsEntry(idExp, loc)) {
        memoryLocationMap.put(idExp, loc);
      }
    }
  }

  private static IDExpression createBaseIdExpresssion(MemoryLocation pLoc) {

    if (!pLoc.isOnFunctionStack()) {
      return new IDExpression(pLoc.getIdentifier());
    } else {
      return new IDExpression(pLoc.getIdentifier(), pLoc.getFunctionName());
    }
  }

  //TODO move to util? (without param generated addresses)
  private static ConcreteState createConcreteState(
      ValueAnalysisState pValueState,
      Map<LeftHandSide, Address> pVariableAddressMap) {


    Map<LeftHandSide, Object> variables = ImmutableMap.of();
    Map<String, Memory> allocatedMemory = allocateAddresses(pValueState, pVariableAddressMap);
    // We assign every variable to the heap, thats why the variable map is empty.
    return new ConcreteState(variables, allocatedMemory, pVariableAddressMap, MEMORY_NAME);
  }

  private static Map<String, Memory> allocateAddresses(
      ValueAnalysisState pValueState,
      Map<LeftHandSide, Address> pVariableAddressMap) {

    Map<Address, Object> values = createHeapValues(pValueState, pVariableAddressMap);

    // memory name of value analysis does not need to know expression or address
    Memory heap = new Memory(MEMORY_NAME.getMemoryName(null, null), values);

    Map<String, Memory> result = new HashMap<>();

    result.put(heap.getName(), heap);

    return result;
  }

  private static Map<Address, Object> createHeapValues(
      ValueAnalysisState pValueState,
      Map<LeftHandSide, Address> pVariableAddressMap) {

    Map<MemoryLocation, Value> valueView = pValueState.getConstantsMapView();

    Map<Address, Object> result = new HashMap<>();

    for (Entry<MemoryLocation, Value> entry : valueView.entrySet()) {
      MemoryLocation heapLoc = entry.getKey();
      Value valueAsValue = entry.getValue();

      if (!valueAsValue.isNumericValue()) {
        // Skip non numerical values for now
        // TODO Should they also be integrated?
        continue;
      }

      Number value = valueAsValue.asNumericValue().getNumber();
      LeftHandSide lhs = createBaseIdExpresssion(heapLoc);
      assert pVariableAddressMap.containsKey(lhs);
      Address baseAddress = pVariableAddressMap.get(lhs);
      Address address = baseAddress;
      if (heapLoc.isReference()) {
        address = baseAddress.addOffset(BigInteger.valueOf(heapLoc.getOffset()));
      }
      result.put(address, value);
    }

    return result;
  }
}