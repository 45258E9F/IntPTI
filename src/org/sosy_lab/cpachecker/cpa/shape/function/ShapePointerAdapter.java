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
package org.sosy_lab.cpachecker.cpa.shape.function;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.function.DirectFunctionAdapter;
import org.sosy_lab.cpachecker.core.interfaces.function.MapParser;
import org.sosy_lab.cpachecker.cpa.boundary.BoundaryState;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SEs;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdgeFilter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.util.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.shape.util.UnknownTypes;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.ExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicExpressionAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndStateList;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

/**
 * This function handles functions with pointer return type.
 * The type of abstract domain element is {@link AddressValueAndStateList}.
 * Note: since we do not use expression checker to capture property violation in shape analysis,
 * we directly evaluate the actual argument value in handling each function call.
 */
public class ShapePointerAdapter implements DirectFunctionAdapter<ShapeState,
    AddressValueAndStateList> {

  private static ShapePointerAdapter INSTANCE = null;

  private MachineModel machineModel;
  private HashMap<String, String> handlerMap;

  private boolean allowAllocFail = false;

  private final boolean activeness;

  /**
   * This value is used as a dummy character denoting unknown character.
   */
  private static final char UNKNOWN_CHAR = Character.MAX_VALUE;

  private ShapePointerAdapter() {
    Optional<CFAInfo> cfaInfo = GlobalInfo.getInstance().getCFAInfo();
    if (cfaInfo.isPresent()) {
      machineModel = cfaInfo.get().getCFA().getMachineModel();
    } else {
      throw new AssertionError("Valid CFA is required for function analysis");
    }
    handlerMap = Maps.newHashMap();
    Path mapFile = GlobalInfo.getInstance().queryMapFilePath(getClass());
    if (mapFile != null) {
      MapParser.loadFromFile(mapFile, handlerMap);
    }
    activeness = GlobalInfo.getInstance().queryActiveness(getClass());
  }

  public static ShapePointerAdapter instance() {
    if (INSTANCE == null) {
      INSTANCE = new ShapePointerAdapter();
    }
    return INSTANCE;
  }

  /**
   * Specify whether allocation failure is allowed in analysis.
   * When this flag is OFF, all allocations are assumed to be successful, which helps to reduce
   * the size of state space.
   */
  public void setAllocationFail(boolean pAllocFlag) {
    allowAllocFail = pAllocFlag;
  }

  /* **************** */
  /* override methods */
  /* **************** */

  @Override
  public AddressValueAndStateList evaluateFunctionCallExpression(
      CFunctionCallExpression pFunctionCallExpression,
      ShapeState currentState,
      List<AbstractState> currentOtherStates,
      CFAEdge cfaEdge) {
    AddressValueAndStateList defaultResult = AddressValueAndStateList.of(currentState);
    if (!activeness) {
      return defaultResult;
    }
    CExpression name = pFunctionCallExpression.getFunctionNameExpression();
    if (name instanceof CIdExpression) {
      CSimpleDeclaration declaration = ((CIdExpression) name).getDeclaration();
      String funcName;
      if (declaration == null) {
        funcName = ((CIdExpression) name).getName();
      } else {
        // To get the name or the original name? That is a question.
        funcName = declaration.getName();
      }
      List<CExpression> parameters = pFunctionCallExpression.getParameterExpressions();
      AddressValueAndStateList results = handleFunction(funcName,
          parameters, currentState, currentOtherStates, cfaEdge);
      if (results != null) {
        return results;
      }
    }
    return defaultResult;
  }

  @Override
  public boolean isRegistered(CFunctionCallExpression pCFunctionCallExpression) {
    CExpression name = pCFunctionCallExpression.getFunctionNameExpression();
    if (name instanceof CIdExpression) {
      CSimpleDeclaration declaration = ((CIdExpression) name).getDeclaration();
      String funcName;
      if (declaration == null) {
        funcName = ((CIdExpression) name).getName();
      } else {
        funcName = declaration.getName();
      }
      String handlerName = handlerMap.get(funcName);
      if (handlerName != null) {
        return true;
      }
      // then, we check if we have supported this function by method name
      handlerName = funcName;
      boolean isReg = true;
      try {
        getClass().getDeclaredMethod(handlerName, List.class, ShapeState.class, List.class,
            CFAEdge.class);
      } catch (NoSuchMethodException e) {
        isReg = false;
      }
      return isReg;
    }
    return false;
  }

  /* ****************************** */
  /* methods for function semantics */
  /* ****************************** */

  @Nullable
  @SuppressWarnings("unchecked")
  private AddressValueAndStateList handleFunction(
      String name, List<CExpression> parameters, ShapeState currentState, List<AbstractState>
      currentOtherStates, CFAEdge cfaEdge) {
    String handlerName = handlerMap.get(name);
    if (handlerName == null) {
      handlerName = name;
    }
    try {
      Method targetMethod = getClass().getDeclaredMethod(handlerName, List.class, ShapeState.class,
          List.class, CFAEdge.class);
      return (AddressValueAndStateList) targetMethod.invoke(this,
          parameters, currentState, currentOtherStates, cfaEdge);
    } catch (NoSuchMethodException ex) {
      try {
        return handleUnknownFunction(parameters, currentState, currentOtherStates, cfaEdge);
      } catch (CPATransferException ex2) {
        return null;
      }
    } catch (IllegalAccessException | InvocationTargetException ex) {
      return null;
    }
  }

  /**
   * For unknown functions, we evaluate its arguments for detecting bugs such as invalid read.
   * This method definitely does not derive valid address values.
   */
  @SuppressWarnings("unused")
  private AddressValueAndStateList handleUnknownFunction(
      List<CExpression> parameters,
      ShapeState currentState,
      List<AbstractState> currentOtherStates,
      CFAEdge cfaEdge)
      throws CPATransferException {
    List<ShapeState> states = new ArrayList<>();
    states.add(currentState);
    for (CExpression parameter : parameters) {
      List<ShapeState> newStates = new ArrayList<>();
      for (ShapeState state : states) {
        SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance()
            .evaluateSymbolicValue(state, currentOtherStates, cfaEdge, parameter);
        newStates.addAll(FluentIterable.from(valueAndStates.asSymbolicValueAndStateList())
            .transform(new Function<SymbolicValueAndState, ShapeState>() {
              @Override
              public ShapeState apply(SymbolicValueAndState pSymbolicValueAndState) {
                return pSymbolicValueAndState.getShapeState();
              }
            }).toList());
      }
      states = newStates;
    }
    return AddressValueAndStateList.copyOfAddressList(FluentIterable.from(states).transform(
        new Function<ShapeState, AddressValueAndState>() {
          @Override
          public AddressValueAndState apply(ShapeState pShapeState) {
            return AddressValueAndState.of(pShapeState);
          }
        }).toList());
  }

  /**
   * The alloca() allocates `size` bytes of space in the stack frame of the caller. This
   * temporary space is automatically freed when the caller of alloca() returns to its caller.
   */
  @SuppressWarnings("unused")
  private AddressValueAndStateList __builtin_alloca(
      List<CExpression> parameters, ShapeState
      currentState, List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // void* alloca(size_t size);
    if (parameters.size() != 1) {
      return AddressValueAndStateList.of(currentState);
    }
    ShapeState cState = new ShapeState(currentState);
    CExpression size = parameters.get(0);
    List<AddressValueAndState> results = new ArrayList<>();
    List<SymbolicExpressionAndState> sizeResults = CoreShapeAdapter.getInstance()
        .evaluateSymbolicExpression(cState, currentOtherStates, cfaEdge, size);
    for (SymbolicExpressionAndState sizeResult : sizeResults) {
      SymbolicExpression sizeValue = sizeResult.getObject();
      sizeValue = SEs.convertTo(sizeValue, machineModel.getSizeTType(), machineModel);
      ShapeState newState = sizeResult.getShapeState();
      String allocLabel = "alloc_ID_" + getBoundaryIdentifier(currentOtherStates) + "_Line:" +
          cfaEdge.getFileLocation().getStartingLineNumber();
      ShapeAddressValue newAddress = newState.addStackAllocation(allocLabel,
          CPointerType.POINTER_TO_VOID, sizeValue);
      results.add(AddressValueAndState.of(newState, newAddress));
    }
    if (allowAllocFail) {
      results.add(AddressValueAndState.of(currentState, KnownAddressValue.NULL));
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  /**
   * Unlike function adapters for other analyses, in shape analysis please do not directly call
   * ExpressionCell.getState() to get the resultant state, because each result has its own
   * resultant state!
   */
  @SuppressWarnings("unused")
  private AddressValueAndStateList malloc(
      List<CExpression> parameters, ShapeState currentState, List<AbstractState>
      currentOtherStates, CFAEdge cfaEdge) throws CPATransferException {
    // void* malloc (size_t);
    if (parameters.size() != 1) {
      return AddressValueAndStateList.of(currentState);
    }
    ShapeState cState = new ShapeState(currentState);
    CExpression size = parameters.get(0);
    List<AddressValueAndState> results = new ArrayList<>();
    List<SymbolicExpressionAndState> sizeResults = CoreShapeAdapter.getInstance()
        .evaluateSymbolicExpression(cState, currentOtherStates, cfaEdge, size);
    for (SymbolicExpressionAndState sizeResult : sizeResults) {
      SymbolicExpression sizeValue = sizeResult.getObject();
      sizeValue = SEs.convertTo(sizeValue, machineModel.getSizeTType(), machineModel);
      ShapeState newState = sizeResult.getShapeState();
      String allocLabel = "malloc_ID_" + getBoundaryIdentifier(currentOtherStates) + "_Line:" +
          cfaEdge.getFileLocation().getStartingLineNumber();
      ShapeAddressValue newAddress = newState.addHeapAllocation(allocLabel, CPointerType
          .POINTER_TO_VOID, sizeValue, false, cfaEdge);
      results.add(AddressValueAndState.of(newState, newAddress));
    }
    if (allowAllocFail) {
      results.add(AddressValueAndState.of(currentState, KnownAddressValue.NULL));
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  @SuppressWarnings("unused")
  private AddressValueAndStateList realloc(
      List<CExpression> parameters, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // void * realloc (void* ptr, size_t size);
    // change the size of the memory block pointed to by ptr.
    // (1) this function MAY move the memory block to a new location
    // (2) if the new size is larger, the value of the newly allocated portion is indeterminate
    // (3) if ptr is NULL, then realloc() behaves the same as malloc()
    // (4) if size is 0, then it returns NULL or some other location that shall not be
    // de-referenced (for the convenience of our implementation, we return NULL instead)
    // (5) if allocation fails, NULL is returned
    if (parameters.size() != 2) {
      return AddressValueAndStateList.of(currentState);
    }
    CExpression pointer = parameters.get(0);
    CExpression size = parameters.get(1);
    List<AddressValueAndState> results = new ArrayList<>();
    AddressValueAndStateList pointerValueAndStates = CoreShapeAdapter.getInstance()
        .evaluateAddressValue(currentState, currentOtherStates, cfaEdge, pointer);
    for (AddressValueAndState pointerValueAndState : pointerValueAndStates
        .asAddressValueAndStateList()) {
      ShapeAddressValue pointerValue = pointerValueAndState.getObject();
      ShapeState newState = pointerValueAndState.getShapeState();
      List<SymbolicExpressionAndState> sizeValueAndStates = CoreShapeAdapter.getInstance()
          .evaluateSymbolicExpression(newState, currentOtherStates, cfaEdge, size);
      for (SymbolicExpressionAndState sizeValueAndState : sizeValueAndStates) {
        SymbolicExpression sizeValue = sizeValueAndState.getObject();
        newState = sizeValueAndState.getShapeState();
        results.addAll(evaluateRealloc(newState, currentOtherStates, cfaEdge, pointerValue,
            sizeValue));
      }
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  @SuppressWarnings("unused")
  private AddressValueAndStateList calloc(
      List<CExpression> parameters, ShapeState currentState, List<AbstractState>
      currentOtherStates, CFAEdge cfaEdge) throws CPATransferException {
    // void* calloc(size_t, size_t);
    // NOTE: calloc() allocates and zero-initializes the array
    if (parameters.size() != 2) {
      return AddressValueAndStateList.of(currentState);
    }
    CExpression num = parameters.get(0);
    CExpression size = parameters.get(1);
    List<AddressValueAndState> results = new ArrayList<>();
    List<SymbolicExpressionAndState> totalSizes = new ArrayList<>();
    ShapeState cState = new ShapeState(currentState);
    List<SymbolicExpressionAndState> numValueAndStates = CoreShapeAdapter.getInstance()
        .evaluateSymbolicExpression(cState, currentOtherStates, cfaEdge, num);
    for (SymbolicExpressionAndState numValueAndState : numValueAndStates) {
      SymbolicExpression numValue = numValueAndState.getObject();
      ShapeState newState = numValueAndState.getShapeState();
      List<SymbolicExpressionAndState> sizeValueAndStates = CoreShapeAdapter.getInstance()
          .evaluateSymbolicExpression(newState, currentOtherStates, cfaEdge, size);
      for (SymbolicExpressionAndState sizeValueAndState : sizeValueAndStates) {
        SymbolicExpression sizeValue = sizeValueAndState.getObject();
        newState = sizeValueAndState.getShapeState();
        SymbolicExpression totalSize = SEs.multiply(numValue, sizeValue,
            machineModel.getSizeTType(), machineModel);
        totalSizes.add(SymbolicExpressionAndState.of(newState, totalSize));
      }
    }
    // then we create heap allocations
    for (SymbolicExpressionAndState valueAndState : totalSizes) {
      SymbolicExpression sizeValue = valueAndState.getObject();
      sizeValue = SEs.convertTo(sizeValue, machineModel.getSizeTType(), machineModel);
      ShapeState newState = valueAndState.getShapeState();
      String allocLabel = "malloc_ID_" + getBoundaryIdentifier(currentOtherStates) + "_Line:" +
          cfaEdge.getFileLocation().getStartingLineNumber();
      ShapeAddressValue newAddress = newState.addHeapAllocation(allocLabel, CPointerType
          .POINTER_TO_VOID, sizeValue, true, cfaEdge);
      results.add(AddressValueAndState.of(newState, newAddress));
    }
    if (allowAllocFail) {
      results.add(AddressValueAndState.of(currentState, KnownAddressValue.NULL));
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  @SuppressWarnings("unused")
  private AddressValueAndStateList strdup(
      List<CExpression> parameters, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // char * strdup (const char * s1);
    // The strdup() function shall return a pointer to a new string, which is a duplicate of the
    // string pointed to by s1. The returned pointer can be passed to free(). A null pointer is
    // returned if the new string cannot be created.
    if (parameters.size() != 1) {
      return AddressValueAndStateList.of(currentState);
    }
    CExpression source = parameters.get(0);
    List<AddressValueAndState> results = new ArrayList<>();
    ShapeState cState = new ShapeState(currentState);
    AddressValueAndStateList sourceResults = CoreShapeAdapter.getInstance().evaluateAddressValue
        (cState, currentOtherStates, cfaEdge, source);
    for (AddressValueAndState sourceResult : sourceResults.asAddressValueAndStateList()) {
      ShapeAddressValue sourceAddress = sourceResult.getObject();
      ShapeState newState = sourceResult.getShapeState();
      String allocLabel = "strdup_ID_" + getBoundaryIdentifier(currentOtherStates) + "_Line:" +
          cfaEdge.getFileLocation().getStartingLineNumber();
      ShapeAddressValue newAddress;
      if (sourceAddress.isUnknown()) {
        // we know where to write, but the size and content are unknown
        newAddress = newState.addHeapAllocation(allocLabel, new CPointerType(false, false,
            CNumericTypes.CHAR), SEs.toUnknown(machineModel.getSizeTType()), false, cfaEdge);
        results.add(AddressValueAndState.of(newState, newAddress));
      } else {
        // STEP 1: read source string from the argument
        Pair<String, ShapeState> readResult = readStringFromState(newState, currentOtherStates,
            cfaEdge, sourceAddress, source);
        newState = Preconditions.checkNotNull(readResult.getSecond());
        if (newState.getInvalidReadStatus()) {
          results.add(AddressValueAndState.of(newState));
          continue;
        }
        String content = Preconditions.checkNotNull(readResult.getFirst());
        int allocSize = (content.length() + 1) * machineModel.getSizeofChar();
        newAddress = newState.addHeapAllocation(allocLabel, new CPointerType(false, false,
            CNumericTypes.CHAR), SEs.toConstant(KnownExplicitValue.valueOf(allocSize),
            machineModel.getSizeTType()), false, cfaEdge);
        newState = writeStringIntoState(newState, currentOtherStates, cfaEdge, newAddress, content);
        results.add(AddressValueAndState.of(newState, newAddress));
      }
    }
    if (allowAllocFail) {
      results.add(AddressValueAndState.of(currentState, KnownAddressValue.NULL));
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  @SuppressWarnings("unused")
  private AddressValueAndStateList memset(
      List<CExpression> parameters, ShapeState currentState, List<AbstractState>
      currentOtherStates, CFAEdge cfaEdge) throws CPATransferException {
    // void* memset(void* ptr, int value, size_t num);
    // memset() sets the first num bytes of blocks of memory pointed by ptr to the specified value
    if (parameters.size() != 3) {
      return AddressValueAndStateList.of(currentState);
    }
    CExpression buffer, ch, count;
    buffer = parameters.get(0);
    ch = parameters.get(1);
    count = parameters.get(2);
    List<AddressValueAndState> results = new ArrayList<>();
    AddressValueAndStateList bufferResults = CoreShapeAdapter.getInstance().evaluateAddressValue
        (currentState, currentOtherStates, cfaEdge, buffer);
    for (AddressValueAndState bufferResult : bufferResults.asAddressValueAndStateList()) {
      ShapeAddressValue bufferAddress = bufferResult.getObject();
      ShapeState newState = bufferResult.getShapeState();
      // The second one is the value to be written into the memory. Since all values in memory
      // graph are symbolic, we should obtain the symbolic value of ch. On the other hand, we
      // should also obtain the explicit value of ch for adding the association of
      // symbolic/explicit values.
      SymbolicValueAndStateList symCharResults = CoreShapeAdapter.getInstance()
          .evaluateSymbolicValue(newState, currentOtherStates, cfaEdge, ch);
      for (SymbolicValueAndState symCharResult : symCharResults.asSymbolicValueAndStateList()) {
        ShapeSymbolicValue symChar = symCharResult.getObject();
        newState = symCharResult.getShapeState();
        List<ExplicitValueAndState> expCharResults = CoreShapeAdapter.getInstance()
            .evaluateExplicitValue(newState, currentOtherStates, cfaEdge, ch);
        for (ExplicitValueAndState expCharResult : expCharResults) {
          ShapeExplicitValue expChar = expCharResult.getObject();
          newState = expCharResult.getShapeState();
          List<ExplicitValueAndState> countResults = CoreShapeAdapter.getInstance()
              .evaluateExplicitValue(newState, currentOtherStates, cfaEdge, count);
          for (ExplicitValueAndState countResult : countResults) {
            ShapeExplicitValue countValue = countResult.getObject();
            newState = countResult.getShapeState();
            AddressValueAndState memsetResult = evaluateMemset(newState, currentOtherStates,
                cfaEdge, bufferAddress, symChar, expChar, countValue);
            results.add(memsetResult);
          }
        }
      }
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  @SuppressWarnings("unused")
  private AddressValueAndStateList memcpy(
      List<CExpression> parameters, ShapeState currentState, List<AbstractState>
      currentOtherStates, CFAEdge cfaEdge) throws CPATransferException {
    // void* memcpy(void* destination, const void* source, size_t num);
    // memcpy() copies the values of num bytes from the location pointed by source to the memory
    // block pointed by destination
    if (parameters.size() != 3) {
      return AddressValueAndStateList.of(currentState);
    }
    CExpression target, source, size;
    target = parameters.get(0);
    source = parameters.get(1);
    size = parameters.get(2);
    List<AddressValueAndState> results = new ArrayList<>();
    AddressValueAndStateList targetValueAndStates = CoreShapeAdapter.getInstance()
        .evaluateAddressValue(currentState, currentOtherStates, cfaEdge, target);
    for (AddressValueAndState targetValueAndState : targetValueAndStates
        .asAddressValueAndStateList()) {
      ShapeAddressValue targetValue = targetValueAndState.getObject();
      ShapeState newState = targetValueAndState.getShapeState();
      AddressValueAndStateList sourceValueAndStates = CoreShapeAdapter.getInstance()
          .evaluateAddressValue(newState, currentOtherStates, cfaEdge, source);
      for (AddressValueAndState sourceValueAndState : sourceValueAndStates
          .asAddressValueAndStateList()) {
        ShapeAddressValue sourceValue = sourceValueAndState.getObject();
        newState = sourceValueAndState.getShapeState();
        List<ExplicitValueAndState> sizeValueAndStates = CoreShapeAdapter.getInstance()
            .evaluateExplicitValue(newState, currentOtherStates, cfaEdge, size);
        for (ExplicitValueAndState sizeValueAndState : sizeValueAndStates) {
          ShapeExplicitValue sizeValue = sizeValueAndState.getObject();
          newState = sizeValueAndState.getShapeState();
          AddressValueAndState memcpyResult = evaluateMemcpy(newState, targetValue,
              sourceValue, sizeValue, source);
          results.add(memcpyResult);
        }
      }
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  @SuppressWarnings("unused")
  private AddressValueAndStateList memmove(
      List<CExpression> parameters, ShapeState currentState, List<AbstractState>
      currentOtherStates, CFAEdge cfaEdge) throws CPATransferException {
    // void* memmove(void* destination, const void* source, size_t num);
    // Different from memcpy(), this function allows arrays pointed by destination and source
    // overlap.
    if (parameters.size() != 3) {
      return AddressValueAndStateList.of(currentState);
    }
    CExpression target, source, size;
    target = parameters.get(0);
    source = parameters.get(1);
    size = parameters.get(2);
    List<AddressValueAndState> results = new ArrayList<>();
    AddressValueAndStateList targetValueAndStates = CoreShapeAdapter.getInstance()
        .evaluateAddressValue(currentState, currentOtherStates, cfaEdge, target);
    for (AddressValueAndState targetValueAndState : targetValueAndStates
        .asAddressValueAndStateList()) {
      ShapeAddressValue targetValue = targetValueAndState.getObject();
      ShapeState newState = targetValueAndState.getShapeState();
      AddressValueAndStateList sourceValueAndStates = CoreShapeAdapter.getInstance()
          .evaluateAddressValue(newState, currentOtherStates, cfaEdge, source);
      for (AddressValueAndState sourceValueAndState : sourceValueAndStates
          .asAddressValueAndStateList()) {
        ShapeAddressValue sourceValue = sourceValueAndState.getObject();
        newState = sourceValueAndState.getShapeState();
        List<ExplicitValueAndState> sizeValueAndStates = CoreShapeAdapter.getInstance()
            .evaluateExplicitValue(newState, currentOtherStates, cfaEdge, size);
        for (ExplicitValueAndState sizeValueAndState : sizeValueAndStates) {
          ShapeExplicitValue sizeValue = sizeValueAndState.getObject();
          newState = sizeValueAndState.getShapeState();
          AddressValueAndState memmoveResult = evaluateMemmove(newState, targetValue,
              sourceValue, sizeValue, source);
          results.add(memmoveResult);
        }
      }
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  /**
   * Since the return type of free() is void, we return a unknown value as a fake return value.
   * In practice, we directly extract the resultant shape state instead of reading the resultant
   * symbolic value.
   */
  @SuppressWarnings("unused")
  private AddressValueAndStateList free(
      List<CExpression> parameters, ShapeState currentState, List<AbstractState>
      currentOtherStates, CFAEdge cfaEdge) throws CPATransferException {
    if (parameters.size() != 1) {
      return AddressValueAndStateList.of(currentState);
    }
    CExpression pointer = parameters.get(0);
    List<AddressValueAndState> results = new ArrayList<>();
    AddressValueAndStateList pointerValueAndStates = CoreShapeAdapter.getInstance()
        .evaluateAddressValue(currentState, currentOtherStates, cfaEdge, pointer);
    for (AddressValueAndState pointerValueAndState : pointerValueAndStates
        .asAddressValueAndStateList()) {
      ShapeAddressValue pointerValue = pointerValueAndState.getObject();
      ShapeState newState = pointerValueAndState.getShapeState();
      if (pointerValue.isUnknown()) {
        // we cannot determine the memory block to be freed, thus it is regarded as invalid free
        newState = newState.setInvalidFree();
        results.add(AddressValueAndState.of(newState));
        continue;
      }
      long addressValue = pointerValue.getAsLong();
      // (1) if a null pointer is to be freed, then free() does nothing;
      // (2) free() does not change the value of pointer, it just marks a pointer as "invalid"
      // for accessing and available for future allocation
      if (addressValue != CShapeGraph.getNullAddress()) {
        newState = newState.free(pointerValue.getObject(), pointerValue.getOffset().getAsInt());
      }
      results.add(AddressValueAndState.of(newState));
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  @SuppressWarnings("unused")
  private AddressValueAndStateList strncpy(
      List<CExpression> parameters, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // char * strncpy (char * destination, const char * source, size_t num);
    // copy the first `num` characters of `source` to `destination`. If the end of `source` is
    // found before `num` characters have been copied, `destination` is padded with zeros util a
    // total of `num` characters have been written to it.
    // Note: if `source` is longer than `num`, then `destination` would be non- null-terminated.
    // Read such `destination` could lead to buffer overflow.
    // Also, as memcpy(), destination and source strings should not overlap
    if (parameters.size() != 3) {
      return AddressValueAndStateList.of(currentState);
    }
    CExpression destination, source, size;
    destination = parameters.get(0);
    source = parameters.get(1);
    size = parameters.get(2);
    List<AddressValueAndState> results = new ArrayList<>();
    AddressValueAndStateList destValueAndStates = CoreShapeAdapter.getInstance()
        .evaluateAddressValue(currentState, currentOtherStates, cfaEdge, destination);
    for (AddressValueAndState destValueAndState : destValueAndStates.asAddressValueAndStateList()) {
      ShapeAddressValue destValue = destValueAndState.getObject();
      ShapeState newState = destValueAndState.getShapeState();
      AddressValueAndStateList sourceValueAndStates = CoreShapeAdapter.getInstance()
          .evaluateAddressValue(newState, currentOtherStates, cfaEdge, source);
      for (AddressValueAndState sourceValueAndState : sourceValueAndStates
          .asAddressValueAndStateList()) {
        ShapeAddressValue sourceValue = sourceValueAndState.getObject();
        newState = sourceValueAndState.getShapeState();
        List<ExplicitValueAndState> sizeValueAndStates = CoreShapeAdapter.getInstance()
            .evaluateExplicitValue(newState, currentOtherStates, cfaEdge, size);
        for (ExplicitValueAndState sizeValueAndState : sizeValueAndStates) {
          ShapeExplicitValue sizeValue = sizeValueAndState.getObject();
          newState = sizeValueAndState.getShapeState();
          // pass `source` for the purpose of marking invalid read
          AddressValueAndState strncpyResult = evaluateStrncpy(newState, currentOtherStates,
              cfaEdge, destValue, sourceValue, sizeValue, source);
          results.add(strncpyResult);
        }
      }
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  @SuppressWarnings("unused")
  private AddressValueAndStateList strcpy(
      List<CExpression> parameters, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // char * strcpy(char * destination, char * source);
    // we should check: (1) if `destination` has enough space for containing source along with
    // the terminating null character; (2) if `destination` and `source` overlap.
    if (parameters.size() != 2) {
      return AddressValueAndStateList.of(currentState);
    }
    CExpression destination, source;
    destination = parameters.get(0);
    source = parameters.get(1);
    List<AddressValueAndState> results = new ArrayList<>();
    AddressValueAndStateList destValueAndStates = CoreShapeAdapter.getInstance()
        .evaluateAddressValue(currentState, currentOtherStates, cfaEdge, destination);
    for (AddressValueAndState destValueAndState : destValueAndStates.asAddressValueAndStateList()) {
      ShapeAddressValue destValue = destValueAndState.getObject();
      ShapeState newState = destValueAndState.getShapeState();
      AddressValueAndStateList sourceValueAndStates = CoreShapeAdapter.getInstance()
          .evaluateAddressValue(newState, currentOtherStates, cfaEdge, source);
      for (AddressValueAndState sourceValueAndState : sourceValueAndStates
          .asAddressValueAndStateList()) {
        ShapeAddressValue sourceValue = sourceValueAndState.getObject();
        newState = sourceValueAndState.getShapeState();
        // `source` is required for marking invalid read
        AddressValueAndState strcpyResult = evaluateStrcpy(newState, currentOtherStates, cfaEdge,
            destValue, sourceValue, source);
        results.add(strcpyResult);
      }
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  @SuppressWarnings("unused")
  private AddressValueAndStateList strcat(
      List<CExpression> parameters, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // char * strcat(char *destination, const char * source);
    // The content of source is appended to the destination. We should check: (1) if buffer
    // overflow could occur in reading source string; (2) check if buffer overflow could occur in
    // appending content to the destination string; (3) check if destination and source overlap.
    // These checks are performed in sequel.
    if (parameters.size() != 2) {
      return AddressValueAndStateList.of(currentState);
    }
    CExpression destination, source;
    destination = parameters.get(0);
    source = parameters.get(1);
    List<AddressValueAndState> results = new ArrayList<>();
    AddressValueAndStateList destValueAndStates = CoreShapeAdapter.getInstance()
        .evaluateAddressValue(currentState, currentOtherStates, cfaEdge, destination);
    for (AddressValueAndState destValueAndState : destValueAndStates.asAddressValueAndStateList()) {
      ShapeAddressValue destValue = destValueAndState.getObject();
      ShapeState newState = destValueAndState.getShapeState();
      AddressValueAndStateList sourceValueAndStates = CoreShapeAdapter.getInstance()
          .evaluateAddressValue(newState, currentOtherStates, cfaEdge, source);
      for (AddressValueAndState sourceValueAndState : sourceValueAndStates
          .asAddressValueAndStateList()) {
        ShapeAddressValue sourceValue = sourceValueAndState.getObject();
        newState = sourceValueAndState.getShapeState();
        AddressValueAndState strcatResult = evaluateStrcat(newState, currentOtherStates, cfaEdge,
            destValue, sourceValue, source, destination);
        results.add(strcatResult);
      }
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  @SuppressWarnings("unused")
  private AddressValueAndStateList strncat(
      List<CExpression> parameters, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // char * strncat(char * destination, char * source, size_t num);
    // Copy the first `num` characters from source to destination, and plus a terminating null
    // character.
    // ** If the length of source is insufficient, only the content up to the terminating
    // null character is copied.
    // ** If the `source` contains '\0', then we only copy contents until '\0' is encountered.
    if (parameters.size() != 3) {
      return AddressValueAndStateList.of(currentState);
    }
    CExpression destination, source, size;
    destination = parameters.get(0);
    source = parameters.get(1);
    size = parameters.get(2);
    List<AddressValueAndState> results = new ArrayList<>();
    AddressValueAndStateList destValueAndStates = CoreShapeAdapter.getInstance()
        .evaluateAddressValue(currentState, currentOtherStates, cfaEdge, destination);
    for (AddressValueAndState destValueAndState : destValueAndStates.asAddressValueAndStateList()) {
      ShapeAddressValue destValue = destValueAndState.getObject();
      ShapeState newState = destValueAndState.getShapeState();
      AddressValueAndStateList sourceValueAndStates = CoreShapeAdapter.getInstance()
          .evaluateAddressValue(newState, currentOtherStates, cfaEdge, source);
      for (AddressValueAndState sourceValueAndState : sourceValueAndStates
          .asAddressValueAndStateList()) {
        ShapeAddressValue sourceValue = sourceValueAndState.getObject();
        newState = sourceValueAndState.getShapeState();
        List<ExplicitValueAndState> sizeValueAndStates = CoreShapeAdapter.getInstance()
            .evaluateExplicitValue(newState, currentOtherStates, cfaEdge, size);
        for (ExplicitValueAndState sizeValueAndState : sizeValueAndStates) {
          ShapeExplicitValue sizeValue = sizeValueAndState.getObject();
          newState = sizeValueAndState.getShapeState();
          AddressValueAndState strncatResult = evaluateStrncat(newState, currentOtherStates,
              cfaEdge, destValue, sourceValue, sizeValue, destination, source);
          results.add(strncatResult);
        }
      }
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  @SuppressWarnings("unused")
  private AddressValueAndStateList fopen(
      List<CExpression> parameters, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // FILE * fopen(const char *, const char *)
    if (parameters.size() != 2) {
      return AddressValueAndStateList.of(currentState);
    }
    CExpression fileName = parameters.get(0);
    CExpression mode = parameters.get(1);
    List<AddressValueAndState> results = new ArrayList<>();
    ShapeState cState = new ShapeState(currentState);
    AddressValueAndStateList nameValueAndStates = CoreShapeAdapter.getInstance()
        .evaluateAddressValue(cState, currentOtherStates, cfaEdge, fileName);
    for (AddressValueAndState nameValueAndState : nameValueAndStates.asAddressValueAndStateList()) {
      ShapeAddressValue nameValue = nameValueAndState.getObject();
      ShapeState newState = nameValueAndState.getShapeState();
      if (!nameValue.isUnknown() && nameValue.getObject() == SGObject.getNullObject()) {
        // no valid file can be created, skip this case and create NULL pointer as result in the end
        continue;
      }
      AddressValueAndStateList modeValueAndStates = CoreShapeAdapter.getInstance()
          .evaluateAddressValue(newState, currentOtherStates, cfaEdge, mode);
      for (AddressValueAndState modeValueAndState : modeValueAndStates
          .asAddressValueAndStateList()) {
        newState = modeValueAndState.getShapeState();
        ShapeAddressValue modeValue = modeValueAndState.getObject();
        if (!modeValue.isUnknown() && modeValue.getObject() == SGObject.getNullObject()) {
          continue;
        }
        // a new file handler is created here
        String resName = "resource_ID_" + getBoundaryIdentifier(currentOtherStates) + "_Line:" +
            cfaEdge.getFileLocation().getStartingLineNumber();
        ShapeAddressValue newAddress = newState.addResource(resName, "fopen", cfaEdge);
        results.add(AddressValueAndState.of(newState, newAddress));
      }
    }
    results.add(AddressValueAndState.of(currentState, KnownAddressValue.NULL));
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  @SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
  private AddressValueAndStateList open(
      List<CExpression> parameters, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // int open(const char *path, int oflag, ...);
    if (parameters.size() < 2) {
      return AddressValueAndStateList.of(currentState);
    }
    CExpression path = parameters.get(0);
    List<AddressValueAndState> results = new ArrayList<>();
    ShapeState cState = new ShapeState(currentState);
    AddressValueAndStateList pathValueAndStates = CoreShapeAdapter.getInstance()
        .evaluateAddressValue(cState, currentOtherStates, cfaEdge, path);
    for (AddressValueAndState pathValueAndState : pathValueAndStates.asAddressValueAndStateList()) {
      ShapeAddressValue pathValue = pathValueAndState.getObject();
      ShapeState newState = pathValueAndState.getShapeState();
      if (!pathValue.isUnknown() && pathValue.getObject() == SGObject.getNullObject()) {
        continue;
      }
      String resName = "resource_ID_" + getBoundaryIdentifier(currentOtherStates) + "_Line:" +
          cfaEdge.getFileLocation().getStartingLineNumber();
      newState.addResource(resName, "open", cfaEdge);
      // the symbolic return value should be unknown here
      results.add(AddressValueAndState.of(newState));
    }
    // file open fails
    results.add(AddressValueAndState.of(currentState));
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  /* ***************** */
  /* auxiliary methods */
  /* ***************** */

  private List<AddressValueAndState> evaluateRealloc(
      ShapeState newState,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge,
      ShapeAddressValue pointer,
      SymbolicExpression size)
      throws CPATransferException {
    List<AddressValueAndState> results = new ArrayList<>();
    if (pointer.isUnknown()) {
      // this pointer can be on anywhere of memory space
      results.add(AddressValueAndState.of(newState));
      return results;
    }
    SGObject object = pointer.getObject();
    if (!newState.isHeapObject(object)) {
      // undefined behavior
      results.add(AddressValueAndState.of(newState));
      return results;
    }
    int offset = pointer.getOffset().getAsInt();
    if (offset != 0) {
      // undefined behavior
      results.add(AddressValueAndState.of(newState));
      return results;
    }
    String label = "realloc_ID_" + getBoundaryIdentifier(otherStates) + "_Line:" + cfaEdge
        .getFileLocation().getStartingLineNumber();
    ShapeExplicitValue origSize = object.getSize();
    size = SEs.convertTo(size, machineModel.getSizeTType(), machineModel);
    if (!SEs.isExplicit(size) || origSize.isUnknown()) {
      if (allowAllocFail) {
        ShapeState nState = new ShapeState(newState);
        results.add(AddressValueAndState.of(nState, KnownAddressValue.NULL));
      }
      ShapeAddressValue newValue = newState.addHeapAllocation(label, CPointerType
          .POINTER_TO_VOID, size, false, cfaEdge);
      // then we completely remove the original object, excluding NULL object
      if (object != CShapeGraph.getNullObject()) {
        newState.dropHeapObject(object);
      }
      results.add(AddressValueAndState.of(newState, newValue));
      return results;
    }
    // the size of the original object and the new specified one are both determinant
    int newSize = size.getValue().getAsInt();
    int oldSize = origSize.getAsInt();
    if (newSize < oldSize) {
      // it is unnecessary to allocate a new memory block for new memory object
      // Reallocation operation replaces the specified object with a new one of the new size. We
      // should carefully remove out-of-bound edges.
      ShapeAddressValue newValue = newState.addHeapReallocation(cfaEdge, object, newSize);
      results.add(AddressValueAndState.of(newState, newValue));
    } else if (newSize > oldSize) {
      // we need too allocate a new, longer space
      // if the allocation fails, memory state keeps unchanged
      if (allowAllocFail) {
        ShapeState nState = new ShapeState(newState);
        results.add(AddressValueAndState.of(nState, KnownAddressValue.NULL));
      }
      ShapeAddressValue newValue = newState.addHeapAllocation(label, CPointerType
          .POINTER_TO_VOID, size, false, cfaEdge);
      SGObject newObject = newValue.getObject();
      newState = newState.copy(object, newObject, offset, oldSize, 0);
      if (object != CShapeGraph.getNullObject()) {
        newState.dropHeapObject(object);
      }
      results.add(AddressValueAndState.of(newState, newValue));
    } else {
      // the new/old sizes are equal
      results.add(AddressValueAndState.of(newState, pointer));
    }
    return results;
  }

  private AddressValueAndState evaluateMemset(
      ShapeState newState, List<AbstractState> otherStates, CFAEdge cfaEdge, ShapeAddressValue
      buffer, ShapeSymbolicValue symChar, ShapeExplicitValue expChar, ShapeExplicitValue count)
      throws CPATransferException {
    if (buffer.isUnknown()) {
      // ROB-2: a potential buffer overwrite issue
      return AddressValueAndState.of(newState);
    }
    if (count.isUnknown()) {
      // we know where to write the data, but we do not know how to write the memory
      return AddressValueAndState.of(newState.setInvalidWrite());
    }
    SGObject object = buffer.getObject();
    ShapeExplicitValue offset = buffer.getOffset();
    if (offset.isUnknown()) {
      return AddressValueAndState.of(newState);
    }
    long length = count.getAsLong();
    if (symChar.isUnknown()) {
      // create a new symbolic value
      symChar = KnownSymbolicValue.valueOf(SymbolicValueFactory.getNewValue());
    }
    // since here, the symbolic char value should be known
    if (symChar.equals(KnownSymbolicValue.ZERO)) {
      // that is to say, we swipe out all data in the specified memory region
      newState = CoreShapeAdapter.getInstance().writeValue(newState, otherStates, cfaEdge,
          object, offset, length, symChar);
    } else {
      KnownSymbolicValue symCh = (KnownSymbolicValue) symChar;
      if (!expChar.isUnknown()) {
        symCh = newState.updateExplicitValue(symCh, (KnownExplicitValue) expChar);
      }
      KnownExplicitValue kOffset = (KnownExplicitValue) offset;
      for (int i = 0; i < length; i++) {
        // we write the memory with new values by bytes
        newState = CoreShapeAdapter.getInstance().writeValue(newState, otherStates, cfaEdge,
            object, kOffset, CNumericTypes.UNSIGNED_CHAR, symCh);
        kOffset = (KnownExplicitValue) kOffset.add(KnownExplicitValue.ONE);
      }
    }
    return AddressValueAndState.of(newState, buffer);
  }

  private AddressValueAndState evaluateMemcpy(
      ShapeState newState, ShapeAddressValue target, ShapeAddressValue source,
      ShapeExplicitValue size, CExpression sourceExpression) throws CPATransferException {
    // Note: to avoid overflows, the size of array pointed by target and source pointers should
    // have at least "size" bytes and two arrays should not overlap.
    if (target.isUnknown()) {
      // ROB-2: possible buffer overwrite
      return AddressValueAndState.of(newState);
    }
    if (source.isUnknown()) {
      // ROB-1: possible buffer overrun
      return AddressValueAndState.of(newState);
    }
    if (size.isUnknown()) {
      // should we throw an invalid read or write error here?
      return AddressValueAndState.of(newState.setInvalidRead(sourceExpression).setInvalidWrite());
    }
    SGObject targetObject = target.getObject();
    SGObject sourceObject = source.getObject();
    int length = size.getAsInt();
    int targetStart = target.getOffset().getAsInt();
    int sourceStart = source.getOffset().getAsInt();

    // I. check if an buffer overrun would occur
    // Note: we do not interrupt the memory copy operation when overlap occurs, because the
    // operation is well-defined.
    ShapeExplicitValue targetObjectSize = targetObject.getSize();
    ShapeExplicitValue sourceObjectSize = sourceObject.getSize();
    if (targetObjectSize.isUnknown() || targetStart < 0 || targetStart + length > targetObjectSize
        .getAsInt()) {
      newState = newState.setInvalidWrite();
    }
    if (sourceObjectSize.isUnknown() || sourceStart < 0 || sourceStart + length > sourceObjectSize
        .getAsInt()) {
      newState = newState.setInvalidRead(sourceExpression);
    }

    // II. check if two arrays possibly overlap
    boolean isOverlap = false;
    if (targetObject == sourceObject) {
      // source and target are harbored in the same memory object
      if (sourceStart < targetStart) {
        if (sourceStart + length > targetStart) {
          isOverlap = true;
        }
      } else if (targetStart < sourceStart) {
        if (targetStart + length > sourceStart) {
          isOverlap = true;
        }
      }
    }
    if (isOverlap) {
      // TODO: maybe we should throw an undefined behavior error?
      return AddressValueAndState.of(newState);
    }

    // III. perform copying operation
    newState = newState.copy(sourceObject, targetObject, sourceStart, length, targetStart);
    return AddressValueAndState.of(newState, target);
  }

  private AddressValueAndState evaluateMemmove(
      ShapeState newState, ShapeAddressValue target, ShapeAddressValue source,
      ShapeExplicitValue size, CExpression sourceExpression) throws CPATransferException {
    // Note: this is a weakened version of evaluateMemcpy(), we do not care if the source and
    // target arrays could overlap.
    if (target.isUnknown()) {
      // ROB-2: a possible memory write issue
      return AddressValueAndState.of(newState);
    }
    if (source.isUnknown()) {
      // ROB-1: a possible memory read issue
      return AddressValueAndState.of(newState);
    }
    if (size.isUnknown()) {
      // should we throw an invalid read or write error here?
      return AddressValueAndState.of(newState.setInvalidRead(sourceExpression).setInvalidWrite());
    }
    SGObject targetObject = target.getObject();
    SGObject sourceObject = source.getObject();
    int length = size.getAsInt();
    int targetStart = target.getOffset().getAsInt();
    int sourceStart = source.getOffset().getAsInt();

    // check if buffer overflow would occur
    ShapeExplicitValue targetSize = targetObject.getSize();
    ShapeExplicitValue sourceSize = sourceObject.getSize();
    if (targetSize.isUnknown() || targetStart < 0 || targetStart + length > targetSize.getAsInt()) {
      newState = newState.setInvalidWrite();
    }
    if (sourceSize.isUnknown() || sourceStart < 0 || sourceStart + length > sourceSize.getAsInt()) {
      newState = newState.setInvalidRead(sourceExpression);
    }

    // perform copying operation
    // Note: ShapeState.copy() copies data between two memory regions without prohibition of
    // overlapping
    newState = newState.copy(sourceObject, targetObject, sourceStart, length, targetStart);
    return AddressValueAndState.of(newState, target);
  }

  private AddressValueAndState evaluateStrncpy(
      ShapeState newState,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge,
      ShapeAddressValue destValue,
      ShapeAddressValue sourceValue,
      ShapeExplicitValue size,
      CExpression sourceExpression)
      throws CPATransferException {

    if (destValue.isUnknown()) {
      // ROB-2: a possible memory write issue
      return AddressValueAndState.of(newState);
    }
    if (sourceValue.isUnknown()) {
      // ROB-1: a possible memory read issue
      return AddressValueAndState.of(newState);
    }
    if (size.isUnknown()) {
      return AddressValueAndState.of(newState.setInvalidRead(sourceExpression).setInvalidWrite());
    }
    SGObject destObject = destValue.getObject();
    SGObject sourceObject = sourceValue.getObject();
    int length = size.getAsInt();
    int destOffset = destValue.getOffset().getAsInt();
    int sourceOffset = sourceValue.getOffset().getAsInt();

    // I. check if buffer overflow would occur
    ShapeExplicitValue destSize = destObject.getSize();
    ShapeExplicitValue sourceSize = sourceObject.getSize();
    if (destSize.isUnknown() || destOffset < 0 || destOffset + length > destSize.getAsInt()) {
      newState = newState.setInvalidWrite();
    }
    if (sourceSize.isUnknown() || sourceOffset < 0 || sourceOffset + length > sourceSize.
        getAsInt()) {
      newState = newState.setInvalidRead(sourceExpression);
    }

    // II. check if two arrays possibly overlap
    boolean overlap = false;
    if (destObject == sourceObject) {
      if (sourceOffset < destOffset) {
        if (sourceOffset + length > destOffset) {
          overlap = true;
        }
      } else {
        if (destOffset + length > sourceOffset) {
          overlap = true;
        }
      }
    }
    if (overlap) {
      // TODO: mark undefined behavior error here
      return AddressValueAndState.of(newState);
    }

    // III. perform copy operation
    // Note:
    // (1) we cannot directly call copy() because we have to fill 0 when the source string has
    // insufficient length;
    // (2) in principle, strncpy() overwrites all contents in target memory region.
    newState = CoreShapeAdapter.getInstance().writeValue(newState, otherStates, cfaEdge, destObject,
        KnownExplicitValue.valueOf(destOffset), length, KnownSymbolicValue.ZERO);
    newState = newState.copy(sourceObject, destObject, sourceOffset, length, destOffset);
    return AddressValueAndState.of(newState, destValue);
  }

  private AddressValueAndState evaluateStrcpy(
      ShapeState newState,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge,
      ShapeAddressValue destValue,
      ShapeAddressValue sourceValue,
      CExpression source)
      throws CPATransferException {

    if (sourceValue.isUnknown()) {
      // ROB-1: a possible memory read issue
      return AddressValueAndState.of(newState);
    }

    SGObject sourceObject = sourceValue.getObject();
    int sourceOffset = sourceValue.getOffset().getAsInt();
    int sizeofChar = machineModel.getSizeofChar();
    boolean sourceSizeUnknown = sourceObject.getSize().isUnknown();

    // STEP 1: traverse source object to obtain the length of string. If no possible terminating
    // null character is encountered, buffer overflow would occur.
    int visitOffset = sourceOffset;
    boolean stopFlag = false;
    int length = 0;
    // we set all bits to UNKNOWN starting from the first unknown bit
    int firstUnknown = -1;
    while (!stopFlag) {
      SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance().readValue(newState,
          otherStates, cfaEdge, sourceObject, KnownExplicitValue.valueOf(visitOffset),
          CNumericTypes.CHAR, source);
      ShapeState chosenState = null;
      for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
        ShapeState state = valueAndState.getShapeState();
        if (state.getInvalidReadStatus()) {
          chosenState = state;
          break;
        }
      }
      if (chosenState != null) {
        return AddressValueAndState.of(chosenState);
      }
      ShapeSymbolicValue value;
      if (valueAndStates.size() > 1) {
        value = UnknownValue.getInstance();
      } else {
        SymbolicValueAndState valueAndState = Iterables.getOnlyElement(valueAndStates
            .asSymbolicValueAndStateList());
        value = valueAndState.getObject();
      }
      if (value.isUnknown()) {
        // update the first unknown index
        if (firstUnknown < 0) {
          firstUnknown = length;
        }
        if (sourceSizeUnknown) {
          for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
            ShapeState tState = valueAndState.getShapeState();
            SGHasValueEdgeFilter filter = SGHasValueEdgeFilter.objectFilter(sourceObject)
                .filterByRange(length, null).filterNotHavingValue(CShapeGraph
                    .getNullAddress());
            if (tState.getHasValueEdgesFor(filter).isEmpty()) {
              return AddressValueAndState.of(tState.setInvalidWrite());
            }
          }
        }
      } else if (value.equals(KnownSymbolicValue.ZERO)) {
        stopFlag = true;
        continue;
      }
      length++;
      visitOffset += sizeofChar;
    }
    // increment the length for the terminating null character
    assert (length > firstUnknown);
    length++;

    // STEP 2: check whether two memory objects possibly overlap
    if (destValue.isUnknown()) {
      // ROB-2: a possible memory write issue
      return AddressValueAndState.of(newState);
    }
    SGObject destObject = destValue.getObject();
    int destOffset = destValue.getOffset().getAsInt();

    boolean overlap = false;
    if (destObject == sourceObject) {
      if (sourceOffset < destOffset) {
        if (sourceOffset + length > destOffset) {
          overlap = true;
        }
      } else {
        if (destOffset + length > sourceOffset) {
          overlap = true;
        }
      }
    }
    if (overlap) {
      // TODO: mark undefined behavior error to this state
      return AddressValueAndState.of(newState);
    }

    // STEP 3: check if we have invalid write to the destination
    ShapeExplicitValue destSize = destObject.getSize();
    if (destSize.isUnknown() || destOffset < 0 || destOffset + length > destSize.getAsInt()) {
      newState = newState.setInvalidWrite();
      return AddressValueAndState.of(newState);
    }

    // STEP 4: perform copy operation
    if (firstUnknown < 0) {
      newState = newState.copy(sourceObject, destObject, sourceOffset, length, destOffset);
    } else {
      newState = newState.copy(sourceObject, destObject, sourceOffset, firstUnknown, destOffset);
      newState = newState.removeValue(destObject, destOffset + firstUnknown, UnknownTypes
          .createTypeWithLength(length - firstUnknown));
    }
    return AddressValueAndState.of(newState, destValue);
  }

  private AddressValueAndState evaluateStrcat(
      ShapeState newState,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge,
      ShapeAddressValue destValue,
      ShapeAddressValue sourceValue,
      CExpression source, CExpression destination)
      throws CPATransferException {

    if (sourceValue.isUnknown()) {
      return AddressValueAndState.of(newState.setInvalidRead(source));
    }
    SGObject sourceObject = sourceValue.getObject();
    int sourceOffset = sourceValue.getOffset().getAsInt();
    boolean sourceSizeUnknown = sourceObject.getSize().isUnknown();
    int sizeofChar = machineModel.getSizeofChar();

    // STEP 1: traverse source object and check if buffer overflow could occur
    int visitOffset = sourceOffset;
    boolean stopFlag = false;
    int sourceLength = 0;
    int sourceFirstUnknown = -1;
    while (!stopFlag) {
      SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance().readValue(newState,
          otherStates, cfaEdge, sourceObject, KnownExplicitValue.valueOf(visitOffset),
          CNumericTypes.CHAR, source);
      ShapeState chosenState = null;
      for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
        ShapeState state = valueAndState.getShapeState();
        if (state.getInvalidReadStatus()) {
          chosenState = state;
          break;
        }
      }
      if (chosenState != null) {
        return AddressValueAndState.of(chosenState);
      }
      ShapeSymbolicValue value;
      if (valueAndStates.size() > 1) {
        value = UnknownValue.getInstance();
      } else {
        SymbolicValueAndState valueAndState = Iterables.getOnlyElement(valueAndStates
            .asSymbolicValueAndStateList());
        value = valueAndState.getObject();
      }
      if (value.isUnknown()) {
        if (sourceFirstUnknown < 0) {
          sourceFirstUnknown = sourceLength;
        }
        if (sourceSizeUnknown) {
          stopFlag = true;
          for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
            ShapeState tState = valueAndState.getShapeState();
            SGHasValueEdgeFilter filter = SGHasValueEdgeFilter.objectFilter(sourceObject)
                .filterByRange(sourceLength, null).filterNotHavingValue(CShapeGraph
                    .getNullAddress());
            if (tState.getHasValueEdgesFor(filter).isEmpty()) {
              return AddressValueAndState.of(tState.setInvalidWrite());
            }
          }
          continue;
        }
      } else if (value.equals(KnownSymbolicValue.ZERO)) {
        stopFlag = true;
        continue;
      }
      sourceLength++;
      visitOffset += sizeofChar;
    }
    assert (sourceLength > sourceFirstUnknown);
    sourceLength++;

    // STEP 2: traverse destination object and check if buffer overflow could occur
    if (destValue.isUnknown()) {
      return AddressValueAndState.of(newState.setInvalidRead(destination));
    }
    SGObject destObject = destValue.getObject();
    int destOffset = destValue.getOffset().getAsInt();
    boolean destSizeUnknown = destObject.getSize().isUnknown();

    visitOffset = destOffset;
    stopFlag = false;
    int destLength = 0;
    int destFirstUnknown = -1;
    while (!stopFlag) {
      SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance().readValue(newState,
          otherStates, cfaEdge, destObject, KnownExplicitValue.valueOf(visitOffset),
          CNumericTypes.CHAR, destination);
      ShapeState chosenState = null;
      for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
        ShapeState state = valueAndState.getShapeState();
        if (state.getInvalidReadStatus()) {
          chosenState = state;
          break;
        }
      }
      if (chosenState != null) {
        return AddressValueAndState.of(chosenState);
      }
      ShapeSymbolicValue value;
      if (valueAndStates.size() > 1) {
        value = UnknownValue.getInstance();
      } else {
        SymbolicValueAndState valueAndState = Iterables.getOnlyElement(valueAndStates
            .asSymbolicValueAndStateList());
        value = valueAndState.getObject();
      }
      if (value.isUnknown()) {
        if (destFirstUnknown < 0) {
          destFirstUnknown = destLength;
        }
        if (destSizeUnknown) {
          stopFlag = true;
          continue;
        }
      } else if (value.equals(KnownSymbolicValue.ZERO)) {
        stopFlag = true;
        continue;
      }
      destLength++;
      visitOffset += sizeofChar;
    }
    assert (destLength > destFirstUnknown);

    // STEP 3: check if buffer overflow could occur when appending source string to the destination
    int maxWrittenLength = sourceLength + destLength;
    ShapeExplicitValue destSize = destObject.getSize();
    if (destSize.isUnknown() || destOffset < 0 || destOffset + maxWrittenLength > destSize
        .getAsInt()) {
      newState = newState.setInvalidWrite();
      return AddressValueAndState.of(newState);
    }

    // STEP 4: check whether source and destination possibly overlap
    boolean overlap = false;
    if (destObject == sourceObject) {
      if (sourceOffset < destOffset) {
        if (sourceOffset + sourceLength > destOffset) {
          overlap = true;
        }
      } else {
        if (destOffset + maxWrittenLength > sourceOffset) {
          overlap = true;
        }
      }
    }
    if (overlap) {
      // TODO: mark undefined behavior error to this state
      return AddressValueAndState.of(newState);
    }

    // STEP 5: perform concatenation
    if (destFirstUnknown < 0) {
      if (sourceFirstUnknown < 0) {
        newState = newState.copy(sourceObject, destObject, sourceOffset, sourceLength, destOffset +
            destLength);
      } else {
        newState = newState.copy(sourceObject, destObject, sourceOffset, sourceFirstUnknown,
            destOffset + destLength);
        newState = newState.removeValue(destObject, destOffset + destLength +
            sourceFirstUnknown, UnknownTypes.createTypeWithLength(sourceLength -
            sourceFirstUnknown));
      }
    } else {
      newState = newState.removeValue(destObject, destOffset + destFirstUnknown, UnknownTypes
          .createTypeWithLength(sourceLength + destLength - destFirstUnknown));
    }
    return AddressValueAndState.of(newState, destValue);
  }

  private AddressValueAndState evaluateStrncat(
      ShapeState newState,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge,
      ShapeAddressValue destValue,
      ShapeAddressValue sourceValue,
      ShapeExplicitValue size,
      CExpression destination, CExpression source)
      throws CPATransferException {

    if (size.isUnknown()) {
      return AddressValueAndState.of(newState.setInvalidWrite());
    }
    if (sourceValue.isUnknown()) {
      // ROB-1: a possible memory read issue
      return AddressValueAndState.of(newState);
    }
    SGObject sourceObject = sourceValue.getObject();
    int sourceOffset = sourceValue.getOffset().getAsInt();
    boolean srcSizeUnknown = sourceObject.getSize().isUnknown();
    int maxLength = size.getAsInt();
    int sizeofChar = machineModel.getSizeofChar();
    boolean needNullTail = true;

    // STEP 1: check if buffer overflow would occur at the source object
    int visitOffset = sourceOffset;
    boolean stopFlag = false;
    // the length of contents that are actually copied
    int sourceLength = 0;
    int sourceFirstUnknown = -1;
    while (!stopFlag) {
      SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance().readValue(newState,
          otherStates, cfaEdge, sourceObject, KnownExplicitValue.valueOf(visitOffset),
          CNumericTypes.CHAR, source);
      ShapeState chosenState = null;
      for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
        ShapeState state = valueAndState.getShapeState();
        if (state.getInvalidReadStatus()) {
          chosenState = state;
          break;
        }
      }
      if (chosenState != null) {
        return AddressValueAndState.of(chosenState);
      }
      ShapeSymbolicValue value;
      if (valueAndStates.size() > 1) {
        value = UnknownValue.getInstance();
      } else {
        SymbolicValueAndState valueAndState = Iterables.getOnlyElement(valueAndStates
            .asSymbolicValueAndStateList());
        value = valueAndState.getObject();
      }
      if (value.isUnknown()) {
        if (sourceFirstUnknown < 0) {
          sourceFirstUnknown = sourceLength;
        }
        if (srcSizeUnknown) {
          stopFlag = true;
          for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
            ShapeState tState = valueAndState.getShapeState();
            SGHasValueEdgeFilter filter = SGHasValueEdgeFilter.objectFilter(sourceObject)
                .filterByRange(sourceLength, null).filterNotHavingValue(CShapeGraph
                    .getNullAddress());
            if (tState.getHasValueEdgesFor(filter).isEmpty()) {
              return AddressValueAndState.of(tState.setInvalidWrite());
            }
          }
          continue;
        }
      } else if (value.equals(KnownSymbolicValue.ZERO)) {
        stopFlag = true;
        continue;
      }
      sourceLength++;
      if (sourceLength >= maxLength) {
        break;
      }
      visitOffset += sizeofChar;
    }
    if (stopFlag) {
      // if we reach here, then source string contains '\0'
      // it is unnecessary to append an additional terminating null character
      sourceLength++;
      needNullTail = false;
      assert (sourceLength <= maxLength);
    }

    // STEP 2: check if buffer overflow would occur at the destination object
    if (destValue.isUnknown()) {
      // ROB-2: a possible memory write issue
      return AddressValueAndState.of(newState);
    }
    SGObject destObject = destValue.getObject();
    int destOffset = destValue.getOffset().getAsInt();
    boolean destSizeUnknown = destObject.getSize().isUnknown();

    visitOffset = destOffset;
    stopFlag = false;
    int destLength = 0;
    int destFirstUnknown = -1;
    while (!stopFlag) {
      SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance().readValue(newState,
          otherStates, cfaEdge, destObject, KnownExplicitValue.valueOf(visitOffset),
          CNumericTypes.CHAR, destination);
      ShapeState chosenState = null;
      for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
        ShapeState state = valueAndState.getShapeState();
        if (state.getInvalidReadStatus()) {
          chosenState = state;
          break;
        }
      }
      if (chosenState != null) {
        return AddressValueAndState.of(chosenState);
      }
      ShapeSymbolicValue value;
      if (valueAndStates.size() > 1) {
        value = UnknownValue.getInstance();
      } else {
        SymbolicValueAndState valueAndState = Iterables.getOnlyElement(valueAndStates
            .asSymbolicValueAndStateList());
        value = valueAndState.getObject();
      }
      if (value.isUnknown()) {
        if (destFirstUnknown < 0) {
          destFirstUnknown = destLength;
        }
        if (destSizeUnknown) {
          stopFlag = true;
          continue;
        }
      } else if (value.equals(KnownSymbolicValue.ZERO)) {
        stopFlag = true;
        continue;
      }
      destLength++;
      visitOffset += sizeofChar;
    }
    assert (destLength > destFirstUnknown);

    // STEP 3: check if buffer overflow could occur when appending source string to the destination
    int maxWrittenLength = destLength + sourceLength;
    if (needNullTail) {
      maxWrittenLength++;
    }
    ShapeExplicitValue destSize = destObject.getSize();
    if (destSize.isUnknown() || destOffset < 0 || destOffset + maxWrittenLength > destSize
        .getAsInt()) {
      newState = newState.setInvalidWrite();
      return AddressValueAndState.of(newState);
    }

    // STEP 4: check whether source and destination possibly overlap
    boolean overlap = false;
    if (destObject == sourceObject) {
      if (sourceOffset < destOffset) {
        if (sourceOffset + sourceLength > destOffset) {
          overlap = true;
        }
      } else {
        if (destOffset + maxWrittenLength > sourceOffset) {
          overlap = true;
        }
      }
    }
    if (overlap) {
      // TODO: mark undefined behavior error in this state
      return AddressValueAndState.of(newState);
    }

    // STEP 5: perform concatenation
    if (destFirstUnknown < 0) {
      if (sourceFirstUnknown < 0) {
        newState = newState.copy(sourceObject, destObject, sourceOffset, sourceLength, destOffset +
            destLength);
        if (needNullTail) {
          newState = CoreShapeAdapter.getInstance().writeValue(newState, otherStates, cfaEdge,
              destObject, KnownExplicitValue.valueOf(destOffset + destLength + sourceLength),
              CNumericTypes.CHAR, KnownSymbolicValue.ZERO);
        }
      } else {
        newState = newState.copy(sourceObject, destObject, sourceOffset, sourceFirstUnknown,
            destOffset + destLength);
        newState = newState.removeValue(destObject, destOffset + destLength + sourceFirstUnknown,
            UnknownTypes.createTypeWithLength(sourceLength - sourceFirstUnknown + (needNullTail ?
                                                                                   1 : 0)));
      }
    } else {
      int coveredLength = sourceLength + (needNullTail ? 1 : 0);
      newState = newState.removeValue(destObject, destOffset + destFirstUnknown, UnknownTypes
          .createTypeWithLength(coveredLength + destLength - destFirstUnknown));
    }
    return AddressValueAndState.of(newState, destValue);
  }

  private Pair<String, ShapeState> readStringFromState(
      ShapeState newState,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge,
      ShapeAddressValue pAddressValue,
      CExpression stringExp)
      throws CPATransferException {
    assert (!pAddressValue.isUnknown());
    SGObject bufferObject = pAddressValue.getObject();
    boolean unknownSize = bufferObject.getSize().isUnknown();
    int bufferOffset = pAddressValue.getOffset().getAsInt();
    int sizeofChar = machineModel.getSizeofChar();
    StringBuilder builder = new StringBuilder();
    int visitOffset = bufferOffset;
    while (true) {
      // prevent infinite loop on processing memory object of uncertain size
      if (unknownSize && newState.getHasValueEdgesFor(SGHasValueEdgeFilter
          .objectFilter(bufferObject).filterByRange(visitOffset, null)).isEmpty()) {
        newState = newState.setInvalidRead(stringExp);
        return Pair.of("", newState);
      }
      SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance().readValue(newState,
          otherStates, cfaEdge, bufferObject, KnownExplicitValue.valueOf(visitOffset),
          CNumericTypes.CHAR, stringExp);
      ShapeState chosenState = null;
      for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
        ShapeState state = valueAndState.getShapeState();
        if (state.getInvalidReadStatus()) {
          chosenState = state;
          break;
        }
      }
      if (chosenState != null) {
        return Pair.of("", chosenState);
      }
      ShapeSymbolicValue value;
      if (valueAndStates.size() > 1) {
        value = UnknownValue.getInstance();
      } else {
        SymbolicValueAndState valueAndState = Iterables.getOnlyElement(valueAndStates
            .asSymbolicValueAndStateList());
        value = valueAndState.getObject();
        chosenState = valueAndState.getShapeState();
      }
      if (value.isUnknown()) {
        builder.append(UNKNOWN_CHAR);
      } else {
        assert (chosenState != null);
        if (value.equals(KnownSymbolicValue.ZERO)) {
          return Pair.of(builder.toString(), chosenState);
        }
        // otherwise, we try to derive the explicit value
        ShapeExplicitValue expValue = chosenState.getExplicit((KnownSymbolicValue) value);
        if (expValue.isUnknown()) {
          builder.append(UNKNOWN_CHAR);
        } else {
          builder.append((char) expValue.getAsInt());
        }
      }
      visitOffset += sizeofChar;
    }
  }

  private ShapeState writeStringIntoState(
      ShapeState newState, List<AbstractState> otherStates,
      CFAEdge cfaEdge, ShapeAddressValue addressValue,
      String content) throws CPATransferException {
    assert (!addressValue.isUnknown());
    SGObject bufferObject = addressValue.getObject();
    int bufferOffset = addressValue.getOffset().getAsInt();
    int sizeofChar = machineModel.getSizeofChar();
    ShapeState nextState = new ShapeState(newState);
    for (int i = 0; i < content.length(); i++) {
      char ch = content.charAt(i);
      if (ch == UNKNOWN_CHAR) {
        bufferOffset += sizeofChar;
        continue;
      }
      KnownExplicitValue chValue = KnownExplicitValue.valueOf(ch);
      SymbolicValueAndState symValueAndState = CoreShapeAdapter.getInstance().createSymbolic
          (nextState, chValue);
      ShapeSymbolicValue symValue = symValueAndState.getObject();
      nextState = symValueAndState.getShapeState();
      nextState = CoreShapeAdapter.getInstance().writeValue(nextState, otherStates, cfaEdge,
          bufferObject, KnownExplicitValue.valueOf(bufferOffset), CNumericTypes.CHAR, symValue);
      if (nextState.getInvalidWriteStatus()) {
        // it is unnecessary to continue writing values
        return nextState;
      }
      bufferOffset += sizeofChar;
    }
    // write the terminating null character
    return CoreShapeAdapter.getInstance().writeValue(nextState, otherStates, cfaEdge, bufferObject,
        KnownExplicitValue.valueOf(bufferOffset), CNumericTypes.CHAR, KnownSymbolicValue.ZERO);
  }

  /**
   * Get the loop stack identifier to distinguish resource allocations in different loop states.
   */
  private String getBoundaryIdentifier(List<AbstractState> otherStates) {
    if (otherStates == null) {
      return "";
    }
    BoundaryState boundaryState = AbstractStates.extractStateByType(otherStates, BoundaryState
        .class);
    if (boundaryState == null) {
      return "";
    }
    return boundaryState.getBoundaryIdentifier();
  }

}
