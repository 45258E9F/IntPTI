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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.base.Objects;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.counterexample.IDExpression;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.range.ArrayUncertainIndexSegment;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGAddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownAddVal;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMGConsistencyVerifier;
import org.sosy_lab.cpachecker.cpa.smg.graphs.TypeAndAlloca;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGIsLessOrEqual;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoin;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGInterpolant;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.AddressingSegment;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;
import org.sosy_lab.cpachecker.util.access.PointerDereferenceSegment;
import org.sosy_lab.cpachecker.util.refinement.ForgetfulState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javax.annotation.Nullable;

public class SMGState implements AbstractQueryableState, LatticeAbstractState<SMGState>,
                                 ForgetfulState<SMGStateInformation>, Graphable {

  // Properties:
  public static final String HAS_INVALID_FREES = "has-invalid-frees";
  public static final String HAS_INVALID_READS = "has-invalid-reads";
  public static final String HAS_INVALID_WRITES = "has-invalid-writes";
  public static final String HAS_LEAKS = "has-leaks";

  private final boolean memoryErrors;
  private final boolean unknownOnUndefined;

  private final AtomicInteger id_counter;

  private final BiMap<SMGKnownSymValue, SMGKnownExpValue> explicitValues = HashBiMap.create();
  private final CLangSMG heap;
  private final LogManager logger;
  private final int predecessorId;
  private final int id;

  private final SMGRuntimeCheck runtimeCheckLevel;
  private final String externalAllocationRecursiveName = "r_";
  private final int externalAllocationSize;

  //TODO These flags are not enough, they should contain more about the nature of the error.
  private final boolean invalidWrite;
  private final boolean invalidRead;
  private final boolean invalidFree;

  private Set<CExpression> invalidReadExpression;
  private Set<CExpression> invalidFreeExpression;

  private void issueMemoryLeakMessage() {
    issueMemoryError("Memory leak found", false);
  }

  private void issueInvalidReadMessage() {
    issueMemoryError("Invalid read found", true);
  }

  private void issueInvalidWriteMessage() {
    issueMemoryError("Invalid write found", true);
  }

  private void issueInvalidFreeMessage() {
    issueMemoryError("Invalid free found", true);
  }

  private void issueMemoryError(String pMessage, boolean pUndefinedBehavior) {
    if (memoryErrors) {
      logger.log(Level.FINE, pMessage);
    } else if (pUndefinedBehavior) {
      logger.log(Level.FINE, pMessage);
      logger.log(Level.WARNING,
          "Non-target undefined behavior detected. The verification result is unreliable.");
    }
  }

  /**
   * Constructor.
   *
   * Keeps consistency: yes
   *
   * @param pLogger             A logger to log any messages
   * @param pMachineModel       A machine model for the underlying SMGs
   * @param pTargetMemoryErrors targets property false valid memtrack
   * @param pUnknownOnUndefined assumes unknown value if undefined
   * @param pSMGRuntimeCheck    consistency check threshold
   */
  public SMGState(
      LogManager pLogger, MachineModel pMachineModel, boolean pTargetMemoryErrors,
      boolean pUnknownOnUndefined, SMGRuntimeCheck pSMGRuntimeCheck, int pExternalAllocationSize) {
    heap = new CLangSMG(pMachineModel);
    logger = pLogger;
    id_counter = new AtomicInteger(0);
    predecessorId = id_counter.getAndIncrement();
    id = id_counter.getAndIncrement();
    memoryErrors = pTargetMemoryErrors;
    unknownOnUndefined = pUnknownOnUndefined;
    this.runtimeCheckLevel = pSMGRuntimeCheck;
    invalidFree = false;
    invalidRead = false;
    invalidWrite = false;
    externalAllocationSize = pExternalAllocationSize;
  }

  public SMGState(
      LogManager pLogger, boolean pTargetMemoryErrors,
      boolean pUnknownOnUndefined, SMGRuntimeCheck pSMGRuntimeCheck, CLangSMG pHeap,
      AtomicInteger pId, int pPredId, Map<SMGKnownSymValue, SMGKnownExpValue> pMergedExplicitValues,
      int pExternalAllocationSize) {
    // merge
    heap = pHeap;
    logger = pLogger;
    id_counter = pId;
    predecessorId = pPredId;
    id = id_counter.getAndIncrement();
    memoryErrors = pTargetMemoryErrors;
    unknownOnUndefined = pUnknownOnUndefined;
    this.runtimeCheckLevel = pSMGRuntimeCheck;
    invalidFree = false;
    invalidRead = false;
    invalidWrite = false;
    explicitValues.putAll(pMergedExplicitValues);
    externalAllocationSize = pExternalAllocationSize;
  }

  SMGState(SMGState pOriginalState, SMGRuntimeCheck pSMGRuntimeCheck) {
    heap = new CLangSMG(pOriginalState.heap);
    logger = pOriginalState.logger;
    predecessorId = pOriginalState.getId();
    id_counter = pOriginalState.id_counter;
    id = id_counter.getAndIncrement();
    explicitValues.putAll(pOriginalState.explicitValues);
    memoryErrors = pOriginalState.memoryErrors;
    unknownOnUndefined = pOriginalState.unknownOnUndefined;
    runtimeCheckLevel = pSMGRuntimeCheck;
    invalidFree = pOriginalState.invalidFree;
    invalidRead = pOriginalState.invalidRead;
    invalidWrite = pOriginalState.invalidWrite;
    externalAllocationSize = pOriginalState.externalAllocationSize;
  }

  /**
   * Copy constructor.
   *
   * Keeps consistency: yes
   *
   * @param pOriginalState Original state. Will be the predecessor of the new state
   */
  public SMGState(SMGState pOriginalState) {
    heap = new CLangSMG(pOriginalState.heap);
    logger = pOriginalState.logger;
    predecessorId = pOriginalState.getId();
    id_counter = pOriginalState.id_counter;
    id = id_counter.getAndIncrement();
    explicitValues.putAll(pOriginalState.explicitValues);
    memoryErrors = pOriginalState.memoryErrors;
    unknownOnUndefined = pOriginalState.unknownOnUndefined;
    runtimeCheckLevel = pOriginalState.runtimeCheckLevel;
    invalidFree = pOriginalState.invalidFree;
    invalidRead = pOriginalState.invalidRead;
    invalidWrite = pOriginalState.invalidWrite;
    externalAllocationSize = pOriginalState.externalAllocationSize;
  }


  public SMGState(SMGState pOriginalState, CLangSMG newHeap) {
    heap = newHeap;
    logger = pOriginalState.logger;
    predecessorId = pOriginalState.getId();
    id_counter = pOriginalState.id_counter;
    id = id_counter.getAndIncrement();
    explicitValues.putAll(pOriginalState.explicitValues);
    memoryErrors = pOriginalState.memoryErrors;
    unknownOnUndefined = pOriginalState.unknownOnUndefined;
    runtimeCheckLevel = pOriginalState.runtimeCheckLevel;
    invalidFree = pOriginalState.invalidFree;
    invalidRead = pOriginalState.invalidRead;
    invalidWrite = pOriginalState.invalidWrite;
    externalAllocationSize = pOriginalState.externalAllocationSize;
  }

  private SMGState(SMGState pOriginalState, Property pProperty) {
    heap = new CLangSMG(pOriginalState.heap);
    logger = pOriginalState.logger;
    predecessorId = pOriginalState.getId();
    id_counter = pOriginalState.id_counter;
    id = id_counter.getAndIncrement();
    explicitValues.putAll(pOriginalState.explicitValues);
    memoryErrors = pOriginalState.memoryErrors;
    unknownOnUndefined = pOriginalState.unknownOnUndefined;
    runtimeCheckLevel = pOriginalState.runtimeCheckLevel;

    boolean pInvalidFree = pOriginalState.invalidFree;
    boolean pInvalidRead = pOriginalState.invalidRead;
    boolean pInvalidWrite = pOriginalState.invalidWrite;

    switch (pProperty) {
      case INVALID_FREE:
        pInvalidFree = true;
        break;
      case INVALID_READ:
        pInvalidRead = true;
        break;
      case INVALID_WRITE:
        pInvalidWrite = true;
        break;
      case INVALID_HEAP:
        break;
      default:
        throw new AssertionError();
    }

    invalidFree = pInvalidFree;
    invalidRead = pInvalidRead;
    invalidWrite = pInvalidWrite;
    externalAllocationSize = pOriginalState.externalAllocationSize;
  }

  public SMGState(
      Map<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues,
      CLangSMG pHeap,
      LogManager pLogger, int pExternalAllocationSize) {

    heap = pHeap;
    logger = pLogger;
    explicitValues.putAll(pExplicitValues);

    unknownOnUndefined = false;
    runtimeCheckLevel = SMGRuntimeCheck.NONE;
    predecessorId = -1;
    memoryErrors = false;
    invalidWrite = false;
    invalidRead = false;
    invalidFree = false;
    id_counter = new AtomicInteger(1);
    id = 0;

    externalAllocationSize = pExternalAllocationSize;
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof SMGState)) {
      return false;
    }
    SMGState that = (SMGState) other;
    return memoryErrors == that.memoryErrors &&
        unknownOnUndefined == that.unknownOnUndefined &&
        Objects.equal(explicitValues, that.explicitValues) &&
        Objects.equal(heap, that.heap) &&
        invalidFree == that.invalidFree &&
        invalidRead == that.invalidRead &&
        invalidWrite == that.invalidWrite;
  }

  public Set<AccessPath> parsePointToFromMemoryLocationToAccessPath(AccessPath path) {
    if (path == null) {
      return Collections.emptySet();
    }
    // if the last segment is uncertain index segment, we remove it and re-append it after
    // computing actual access path
    PathSegment lastSegment = path.getLastSegment();
    boolean uncertain = false;
    if (lastSegment instanceof ArrayUncertainIndexSegment) {
      path.removeLastSegment();
      uncertain = true;
    }

    // if we apply an actual access path here, we should directly return it instead of attempting
    // to compute its actual path
    if (path.isActualPath()) {
      return Collections.singleton(path);
    }

    if (!path.supportMemoryLocationRepresentation()) {
      return Collections.emptySet();
    }
    // restore the access path if possible
    if (uncertain) {
      path.appendSegment(lastSegment);
    }
    // produced access paths are all ACTUAL PATH (without additional check here)
    return heap.getPointToAccessPath(path, uncertain);
  }

  public Range computeAccessPathDifference(AccessPath path1, AccessPath path2) {
    if (path1 == null || path2 == null) {
      return Range.UNBOUND;
    }
    AccessPath newPath1 = AccessPath.copyOf(path1);
    AccessPath newPath2 = AccessPath.copyOf(path2);
    PathSegment lastSegment1 = newPath1.getLastSegment();
    PathSegment lastSegment2 = newPath2.getLastSegment();
    Range indexRange1 = Range.ZERO;
    Range indexRange2 = Range.ZERO;
    // Note: it is possible that the last segment is addressing segment
    if (lastSegment1 instanceof AddressingSegment) {
      newPath1.removeLastSegment();
    } else {
      if (lastSegment1 instanceof ArrayUncertainIndexSegment) {
        indexRange1 = ((ArrayUncertainIndexSegment) lastSegment1).getIndexRange();
        newPath1.removeLastSegment();
        newPath1.appendSegment(new ArrayConstIndexSegment(indexRange1.getLow().longValue()));
      }
      newPath1.appendSegment(new PointerDereferenceSegment());
    }
    if (lastSegment2 instanceof AddressingSegment) {
      newPath2.removeLastSegment();
    } else {
      if (lastSegment2 instanceof ArrayUncertainIndexSegment) {
        indexRange2 = ((ArrayUncertainIndexSegment) lastSegment2).getIndexRange();
        newPath2.removeLastSegment();
        newPath2.appendSegment(new ArrayConstIndexSegment(indexRange2.getLow().longValue()));
      }
      newPath2.appendSegment(new PointerDereferenceSegment());
    }
    if (!newPath1.supportMemoryLocationRepresentation() ||
        !newPath2.supportMemoryLocationRepresentation()) {
      return Range.UNBOUND;
    }
    Long diff = heap.getPointerDifference(newPath1, newPath2);
    if (diff == null) {
      return Range.UNBOUND;
    }
    return indexRange1.minus(indexRange2).plus(diff);
  }

  /**
   * Makes SMGState create a new object and put it into the global namespace
   *
   * Keeps consistency: yes
   *
   * @param pTypeSize Size of the type of the new global variable
   * @param pVarName  Name of the global variable
   * @return Newly created object
   * @throws SMGInconsistentException when resulting SMGState is inconsistent and the checks are
   *                                  enabled
   */
  public SMGObject addGlobalVariable(int pTypeSize, String pVarName)
      throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(pTypeSize, pVarName);

    heap.addGlobalObject(new_object);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return new_object;
  }

  /**
   * A refined version of addGlobalVariable to support type information
   */
  public SMGObject addGlobalVariable(int pTypeSize, String pVarName, CType type)
      throws SMGInconsistentException {
    heap.addTypeInfo(pVarName, type, TypeAndAlloca.STATIC);
    return addGlobalVariable(pTypeSize, pVarName);
  }

  /**
   * Makes SMGState create a new object and put it into the current stack
   * frame.
   *
   * Keeps consistency: yes
   *
   * @param pTypeSize Size of the type the new local variable
   * @param pVarName  Name of the local variable
   * @return Newly created object
   * @throws SMGInconsistentException when resulting SMGState is inconsistent and the checks are
   *                                  enabled
   */
  public SMGObject addLocalVariable(int pTypeSize, String pVarName, CType type)
      throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(pTypeSize, pVarName);

    heap.addStackObject(new_object);

    String functionName = heap.getStackFrames().peek().getFunctionDeclaration().getName();
    String qualifiedName = functionName + "::" + pVarName;
    heap.addTypeInfo(qualifiedName, type, TypeAndAlloca.STATIC);

    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return new_object;
  }

  /**
   * Makes SMGState create a new object, compares it with the given object, and puts the given
   * object into the current stack frame.
   *
   * Keeps consistency: yes
   *
   * @param pTypeSize Size of the type of the new variable
   * @param pVarName  Name of the local variable
   * @param smgObject object of local variable
   * @return given object
   * @throws SMGInconsistentException when resulting SMGState is inconsistent and the checks are
   *                                  enabled
   */
  public SMGObject addLocalVariable(int pTypeSize, String pVarName, SMGRegion smgObject)
      throws SMGInconsistentException {
    SMGRegion new_object2 = new SMGRegion(pTypeSize, pVarName);

    assert smgObject.getLabel().equals(new_object2.getLabel());

    // arrays are converted to pointers
    assert smgObject.getSize() == pTypeSize
        || smgObject.getSize() == heap.getMachineModel().getSizeofPtr();

    heap.addStackObject(smgObject);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return smgObject;
  }

  void addTypeInfo(String qualifiedName, CType type, boolean alloca) {
    heap.addTypeInfo(qualifiedName, type, alloca);
  }

  void addTemporaryVar2MemorySpace(String tempVar, String memoryName) {
    heap.addTempVarToName(tempVar, memoryName);
  }

  void reduceTempVar(String tempVar, CType newType) {
    heap.reduceTempVar(tempVar, newType);
  }

  /**
   * Adds a new frame for the function.
   *
   * Keeps consistency: yes
   *
   * @param pFunctionDefinition A function for which to create a new stack frame
   */
  public void addStackFrame(CFunctionDeclaration pFunctionDefinition)
      throws SMGInconsistentException {
    heap.addStackFrame(pFunctionDefinition);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
  }

  /* ********************************************* */
  /* Non-modifying functions: getters and the like */
  /* ********************************************* */

  /**
   * Constant.
   *
   * @return The ID of this SMGState
   */
  final public int getId() {
    return id;
  }

  /**
   * Constant.
   * .
   *
   * @return The predecessor state, i.e. one from which this one was copied
   */
  final public int getPredecessorId() {
    return predecessorId;
  }

  /**
   * Constant.
   *
   * @return A {@link SMGObject} for current function return value storage.
   */
  final public SMGObject getFunctionReturnObject() {
    return heap.getFunctionReturnObject();
  }

  /**
   * Get memory of variable with the given name.
   *
   * @param pVariableName A name of the desired variable
   * @return An object corresponding to the variable name
   */
  public SMGObject getObjectForVisibleVariable(String pVariableName) {
    return heap.getObjectForVisibleVariable(pVariableName);
  }

  /**
   * Based on the current setting of runtime check level, it either performs
   * a full consistency check or not. If the check is performed and the
   * state is deemed inconsistent, a {@link SMGInconsistentException} is thrown.
   *
   * Constant.
   *
   * @param pLevel A level of the check request. When e.g. HALF is passed, it means "perform the
   *               check if the setting is HALF or finer.
   */
  final public void performConsistencyCheck(SMGRuntimeCheck pLevel)
      throws SMGInconsistentException {
    if (runtimeCheckLevel.isFinerOrEqualThan(pLevel)) {
      if (!CLangSMGConsistencyVerifier.verifyCLangSMG(logger,
          heap)) {
        throw new SMGInconsistentException(
            "SMG was found inconsistent during a check");
      }
    }
  }

  /**
   * Returns a DOT representation of the SMGState.
   *
   * Constant.
   *
   * @param pName     A name of the graph.
   * @param pLocation A location in the program.
   * @return String containing a DOT graph corresponding to the SMGState.
   */
  public String toDot(String pName, String pLocation) {
    SMGPlotter plotter = new SMGPlotter();
    return plotter.smgAsDot(heap, pName, pLocation, explicitValues);
  }

  /**
   * Returns a DOT representation of the SMGState with explicit Values
   * inserted where possible.
   *
   * @param pName          A name of the graph.
   * @param pLocation      A location in the program.
   * @param pExplicitState the state that should be converted
   * @return String containing a DOT graph corresponding to the SMGState.
   */
  public String toDot(String pName, String pLocation, ValueAnalysisState pExplicitState) {
    SMGExplicitPlotter plotter = new SMGExplicitPlotter(pExplicitState, this);
    return plotter.smgAsDot(heap, "Explicit_" + pName, pLocation);
  }

  /**
   * @return A string representation of the SMGState.
   */
  @Override
  public String toString() {
    if (getPredecessorId() != 0) {
      return "SMGState [" + getId() + "] <-- parent [" + getPredecessorId() + "]\n"
          + heap.toString();
    } else {
      return "SMGState [" + getId() + "] <-- no parent, initial state\n" + heap.toString();
    }
  }

  /**
   * Returns a Points-To edge leading from a value. If the target is an abstract heap segment,
   * materialize heap segment.
   *
   * Constant.
   *
   * @param pValue A value for which to return the Points-To edge
   * @return the address represented by the passed value. The value needs to be a pointer, i.e. it
   * needs to have a points-to edge. If it does not have it, the method raises an exception.
   * @throws SMGInconsistentException When the value passed does not have a Points-To edge.
   */
  public SMGAddressValueAndStateList getPointerFromValue(Integer pValue)
      throws SMGInconsistentException {
    if (heap.isPointer(pValue)) {
      SMGEdgePointsTo addressValue = heap.getPointer(pValue);

      SMGAddressValue address = SMGKnownAddVal.valueOf(addressValue.getValue(),
          addressValue.getObject(), addressValue.getOffset());

      return SMGAddressValueAndStateList.of(SMGAddressValueAndState.of(this, address));
    }

    throw new SMGInconsistentException("Asked for a Points-To edge for a non-pointer value");
  }

  /**
   * Checks, if a symbolic value is an address.
   *
   * Constant.
   *
   * @param pValue A value for which to return the Points-To edge
   * @return True, if the smg contains a {@link SMGEdgePointsTo} edge with pValue as source, false
   * otherwise.
   */
  public boolean isPointer(Integer pValue) {

    return heap.isPointer(pValue);
  }

  /**
   * Read Value in field (object, type) of an Object. If a Value cannot be determined,
   * but the given object and field is a valid place to read a value, a new value will be
   * generated and returned. (Does not create a new State but modifies this state).
   *
   * @param pObject SMGObject representing the memory the field belongs to.
   * @param pOffset offset of field being read.
   * @param pType   type of field
   * @param exp     expression of the SMGObject
   * @return the value and the state (may be the given state)
   */
  public SMGValueAndState forceReadValue(
      SMGObject pObject, int pOffset, CType pType,
      CExpression exp)
      throws SMGInconsistentException {
    SMGValueAndState valueAndState = readValue(pObject, pOffset, pType, exp);

    // Do not create a value if the read is invalid.
    if (valueAndState.getObject().isUnknown() && valueAndState.getSmgState().invalidRead == false) {
      SMGStateEdgePair stateAndNewEdge;
      if (valueAndState.getSmgState().heap.isObjectExternallyAllocated(pObject)
          && pType.getCanonicalType() instanceof CPointerType) {
        SMGAddressValue new_address =
            valueAndState.getSmgState().addExternalAllocation(externalAllocationRecursiveName +
                pObject.getLabel());
        stateAndNewEdge = writeValue(pObject, pOffset, pType, new_address);
      } else {
        Integer newValue = SMGValueFactory.getNewValue();
        stateAndNewEdge = writeValue(pObject, pOffset, pType, newValue);
      }
      return SMGValueAndState.of(stateAndNewEdge.getState(),
          SMGKnownSymValue.valueOf(stateAndNewEdge.getNewEdge().getValue()));
    } else {
      return valueAndState;
    }
  }

  /**
   * Read Value in field (object, type) of an Object.
   *
   * @param pObject SMGObject representing the memory the field belongs to.
   * @param pOffset offset of field being read.
   * @param pType   type of field
   * @param exp     expression of the SMGObject
   * @return the value and the state (may be the given state)
   */
  public SMGValueAndState readValue(SMGObject pObject, int pOffset, CType pType, CExpression exp)
      throws SMGInconsistentException {
    if (!heap.isObjectValid(pObject) && !heap.isObjectExternallyAllocated(pObject)) {
      SMGState newState = setInvalidRead(exp);
      return SMGValueAndState.of(newState);
    }

    SMGEdgeHasValue edge = new SMGEdgeHasValue(pType, pOffset, pObject, 0);

    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();
    filter.filterByObject(pObject);
    filter.filterAtOffset(pOffset);
    Set<SMGEdgeHasValue> edges = heap.getHVEdges(filter);

    for (SMGEdgeHasValue object_edge : edges) {
      if (edge.isCompatibleFieldOnSameObject(object_edge, heap.getMachineModel())) {
        performConsistencyCheck(SMGRuntimeCheck.HALF);
        SMGSymbolicValue value = SMGKnownSymValue.valueOf(object_edge.getValue());
        return SMGValueAndState.of(this, value);
      }
    }

    if (heap.isCoveredByNullifiedBlocks(
        edge)) {
      return SMGValueAndState.of(this, SMGKnownSymValue.ZERO);
    }

    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return SMGValueAndState.of(this);
  }

  public SMGState setInvalidRead(CExpression exp) {
    SMGState newState = new SMGState(this, Property.INVALID_READ);
    newState.setInvalidReadExpression(exp);
    return newState;
  }

  /**
   * Write a value into a field (offset, type) of an Object.
   * Additionally, this method writes a points-to edge into the
   * SMG, if the given symbolic value points to an address, and
   *
   * @param pObject SMGObject representing the memory the field belongs to.
   * @param pOffset offset of field written into.
   * @param pType   type of field written into.
   * @param pValue  value to be written into field.
   * @return the edge and the new state (may be this state)
   */
  public SMGStateEdgePair writeValue(
      SMGObject pObject, int pOffset,
      CType pType, SMGSymbolicValue pValue) throws SMGInconsistentException {

    int value;

    // If the value is not yet known by the SMG
    // create a unconstrained new symbolic value
    if (pValue.isUnknown()) {
      value = SMGValueFactory.getNewValue();
    } else {
      value = pValue.getAsInt();
    }

    // If the value represents an address, and the address is known,
    // add the necessary points-To edge.
    if (pValue instanceof SMGAddressValue) {
      if (!containsValue(value)) {
        SMGAddress address = ((SMGAddressValue) pValue).getAddress();

        if (!address.isUnknown()) {
          addPointsToEdge(
              address.getObject(),
              address.getOffset().getAsInt(),
              value);
        }
      }
    }

    return writeValue(pObject, pOffset, pType, value);
  }

  public void addPointsToEdge(SMGObject pObject, int pOffset, int pValue) {

    // If the value is not known by the SMG, add it.
    if (!containsValue(pValue)) {
      heap.addValue(pValue);
    }

    SMGEdgePointsTo pointsToEdge = new SMGEdgePointsTo(pValue, pObject, pOffset);
    heap.addPointsToEdge(pointsToEdge);

  }

  /**
   * Write a value into a field (offset, type) of an Object.
   *
   * @param pObject SMGObject representing the memory the field belongs to.
   * @param pOffset offset of field written into.
   * @param pType   type of field written into.
   * @param pValue  value to be written into field.
   */
  public SMGStateEdgePair writeValue(SMGObject pObject, int pOffset, CType pType, Integer pValue)
      throws SMGInconsistentException {
    // vgl Algorithm 1 Byte-Precise Verification of Low-Level List Manipulation FIT-TR-2012-04

    if (!heap.isObjectValid(pObject) && !heap.isObjectExternallyAllocated(pObject)) {
      //Attempt to write to invalid object
      SMGState newState = setInvalidWrite();
      return new SMGStateEdgePair(newState);
    }

    SMGEdgeHasValue new_edge = new SMGEdgeHasValue(pType, pOffset, pObject, pValue);

    // Check if the edge is  not present already
    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(pObject);

    Set<SMGEdgeHasValue> edges = heap.getHVEdges(filter);
    if (edges.contains(new_edge)) {
      performConsistencyCheck(SMGRuntimeCheck.HALF);
      return new SMGStateEdgePair(this, new_edge);
    }

    // If the value is not in the SMG, we need to add it
    if (!heap.getValues().contains(pValue)) {
      heap.addValue(pValue);
    }

    HashSet<SMGEdgeHasValue> overlappingZeroEdges = new HashSet<>();

    /* We need to remove all non-zero overlapping edges
     * and remember all overlapping zero edges to shrink them later
     */
    for (SMGEdgeHasValue hv : edges) {

      boolean hvEdgeOverlaps = new_edge.overlapsWith(hv, heap.getMachineModel());
      boolean hvEdgeIsZero = hv.getValue() == heap.getNullValue();

      if (hvEdgeOverlaps) {
        if (hvEdgeIsZero) {
          overlappingZeroEdges.add(hv);
        } else {
          heap.removeHasValueEdge(hv);
        }
      }
    }

    shrinkOverlappingZeroEdges(new_edge, overlappingZeroEdges);

    heap.addHasValueEdge(new_edge);
    performConsistencyCheck(SMGRuntimeCheck.HALF);

    return new SMGStateEdgePair(this, new_edge);
  }

  public static class SMGStateEdgePair {

    private final SMGState smgState;
    private final SMGEdgeHasValue edge;

    private SMGStateEdgePair(SMGState pState, SMGEdgeHasValue pEdge) {
      smgState = pState;
      edge = pEdge;
    }

    private SMGStateEdgePair(SMGState pNewState) {
      smgState = pNewState;
      edge = null;
    }

    public boolean smgStateHasNewEdge() {
      return edge != null;
    }

    public SMGEdgeHasValue getNewEdge() {
      return edge;
    }

    public SMGState getState() {
      return smgState;
    }
  }

  private void shrinkOverlappingZeroEdges(
      SMGEdgeHasValue pNew_edge,
      Set<SMGEdgeHasValue> pOverlappingZeroEdges) {

    SMGObject object = pNew_edge.getObject();
    int offset = pNew_edge.getOffset();

    MachineModel maModel = heap.getMachineModel();
    int sizeOfType = pNew_edge.getSizeInBytes(maModel);

    // Shrink overlapping zero edges
    for (SMGEdgeHasValue zeroEdge : pOverlappingZeroEdges) {
      heap.removeHasValueEdge(zeroEdge);

      int zeroEdgeOffset = zeroEdge.getOffset();

      int offset2 = offset + sizeOfType;
      int zeroEdgeOffset2 = zeroEdgeOffset + zeroEdge.getSizeInBytes(maModel);

      if (zeroEdgeOffset < offset) {
        SMGEdgeHasValue newZeroEdge =
            new SMGEdgeHasValue(offset - zeroEdgeOffset, zeroEdgeOffset, object, 0);
        heap.addHasValueEdge(newZeroEdge);
      }

      if (offset2 < zeroEdgeOffset2) {
        SMGEdgeHasValue newZeroEdge =
            new SMGEdgeHasValue(zeroEdgeOffset2 - offset2, offset2, object, 0);
        heap.addHasValueEdge(newZeroEdge);
      }
    }
  }

  /**
   * Marks that an invalid write operation was performed on this smgState.
   */
  public SMGState setInvalidWrite() {
    return new SMGState(this, Property.INVALID_WRITE);
  }

  /**
   * Computes the join of this abstract State and the reached abstract State.
   *
   * @param reachedState the abstract state this state will be joined to.
   * @return the join of the two states.
   */
  @Override
  public SMGState join(SMGState reachedState) {
    // Not necessary if merge_SEP or SMGMerge and stop_SEP is used.
    return null;
  }

  /**
   * Computes the join of this abstract State and the reached abstract State,
   * or returns the reached state, if no join is defined.
   *
   * @param reachedState the abstract state this state will be joined to.
   * @return the join of the two states or reached state.
   * @throws SMGInconsistentException inconsistent smgs while
   */
  public SMGState joinSMG(SMGState reachedState) throws SMGInconsistentException {
    // Not necessary if merge_SEP and stop_SEP is used.

    SMGJoin join = new SMGJoin(this.heap, reachedState.heap);

    if (join.isDefined() && join.getStatus() != SMGJoinStatus.INCOMPARABLE
        && join.getStatus() != SMGJoinStatus.INCOMPLETE) {

      CLangSMG destHeap = join.getJointSMG();

      Map<SMGKnownSymValue, SMGKnownExpValue> mergedExplicitValues = new HashMap<>();

      for (Entry<SMGKnownSymValue, SMGKnownExpValue> entry : explicitValues.entrySet()) {
        if (destHeap.getValues().contains(entry.getKey().getAsInt())) {
          mergedExplicitValues.put(entry.getKey(), entry.getValue());
        }
      }

      for (Entry<SMGKnownSymValue, SMGKnownExpValue> entry : reachedState.explicitValues
          .entrySet()) {
        mergedExplicitValues.put(entry.getKey(), entry.getValue());
      }

      return new SMGState(logger, memoryErrors, unknownOnUndefined, runtimeCheckLevel, destHeap,
          id_counter, predecessorId, mergedExplicitValues, reachedState.externalAllocationSize);
    } else {
      return reachedState;
    }
  }

  /**
   * Computes whether this abstract state is covered by the given abstract state.
   * A state is covered by another state, if the set of concrete states
   * a state represents is a subset of the set of concrete states the other
   * state represents.
   *
   * @param reachedState already reached state, that may cover this state already.
   * @return True, if this state is covered by the given state, false otherwise.
   */
  @Override
  public boolean isLessOrEqual(SMGState reachedState) throws SMGInconsistentException {
    return SMGIsLessOrEqual.isLessOrEqual(reachedState.heap, heap);
  }

  @Override
  public String getCPAName() {
    return "SMGCPA";
  }

  public SMGState cleanProperty() {
    if (invalidWrite || invalidRead || invalidFree) {
      return new SMGState(logger, memoryErrors,
          unknownOnUndefined, runtimeCheckLevel,
          new CLangSMG(heap), id_counter, getId(),
          explicitValues, externalAllocationSize);
    } else {
      return this;
    }
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    // SMG Properties:
    // has-leaks:boolean

    switch (pProperty) {
      case HAS_LEAKS:
        if (heap.hasMemoryLeaks()) {
          //TODO: Give more information
          issueMemoryLeakMessage();
          return true;
        }
        return false;
      case HAS_INVALID_WRITES:
        if (invalidWrite) {
          //TODO: Give more information
          issueInvalidWriteMessage();
          return true;
        }
        return false;
      case HAS_INVALID_READS:
        if (invalidRead) {
          //TODO: Give more information
          issueInvalidReadMessage();
          return true;
        }
        return false;
      case HAS_INVALID_FREES:
        if (invalidFree) {
          //TODO: Give more information
          issueInvalidFreeMessage();
          return true;
        }
        return false;
      default:
        throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
    }
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    return checkProperty(pProperty);
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    // TODO Auto-generated method stub

  }

  public void addGlobalObject(SMGRegion newObject) {
    heap.addGlobalObject(newObject);
  }

  public boolean isGlobal(String variable) {
    return heap.getGlobalObjects().containsValue(heap.getObjectForVisibleVariable(variable));
  }

  public boolean isGlobal(SMGObject object) {
    return heap.getGlobalObjects().containsValue(object);
  }

  public boolean isHeapObject(SMGObject object) {
    return heap.getHeapObjects().contains(object);
  }

  /**
   * memory allocated in the heap has to be freed by the user,
   * otherwise this is a memory-leak.
   */
  public SMGAddressValue addNewHeapAllocation(int pSize, String pLabel)
      throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(pSize, pLabel);
    int new_value = SMGValueFactory.getNewValue();
    SMGEdgePointsTo points_to = new SMGEdgePointsTo(new_value, new_object, 0);
    heap.addHeapObject(new_object);
    heap.addValue(new_value);
    heap.addPointsToEdge(points_to);

    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return SMGKnownAddVal.valueOf(new_value, new_object, 0);
  }

  /**
   * memory externally allocated could be freed by the user
   */
  // TODO: refactore
  public SMGAddressValue addExternalAllocation(String pLabel) throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(externalAllocationSize, pLabel);
    int new_value = SMGValueFactory.getNewValue();
    SMGEdgePointsTo points_to = new SMGEdgePointsTo(new_value, new_object, 0);
    heap.addHeapObject(new_object);
    heap.addValue(new_value);
    heap.addPointsToEdge(points_to);

    heap.setExternallyAllocatedFlag(new_object, true);

    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return SMGKnownAddVal.valueOf(new_value, new_object, 0);
  }

  /**
   * memory allocated on the stack is automatically freed when leaving the current function scope
   */
  public SMGAddressValue addNewStackAllocation(int pSize, String pLabel)
      throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(pSize, pLabel);
    int new_value = SMGValueFactory.getNewValue();
    SMGEdgePointsTo points_to = new SMGEdgePointsTo(new_value, new_object, 0);
    heap.addStackObject(new_object);
    heap.addValue(new_value);
    heap.addPointsToEdge(points_to);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return SMGKnownAddVal.valueOf(new_value, new_object, 0);
  }

  public void setMemLeak() {
    heap.setMemoryLeak();
  }

  public boolean containsValue(int value) {
    return heap.getValues().contains(value);
  }

  /**
   * Get the symbolic value, that represents the address
   * pointing to the given memory with the given offset, if it exists.
   *
   * @param memory get address belonging to this memory.
   * @param offset get address with this offset relative to the beginning of the memory.
   * @return Address of the given field, or null, if such an address does not yet exist in the SMG.
   */
  @Nullable
  public Integer getAddress(SMGObject memory, int offset) {

    // TODO A better way of getting those edges, maybe with a filter
    // like the Has-Value-Edges

    Map<Integer, SMGEdgePointsTo> pointsToEdges = heap.getPTEdges();

    for (SMGEdgePointsTo edge : pointsToEdges.values()) {
      if (edge.getObject().equals(memory) && edge.getOffset() == offset) {
        return edge.getValue();
      }
    }

    return null;
  }

  /**
   * This method simulates a free invocation. It checks,
   * whether the call is valid, and invalidates the
   * Memory the given address points to.
   * The address (address, offset, smgObject) is the argument
   * of the free invocation. It does not need to be part of the SMG.
   *
   * @param address   The symbolic Value of the address.
   * @param offset    The offset of the address relative to the beginning of smgObject.
   * @param smgObject The memory the given Address belongs to.
   * @return returns a possible new State
   */
  public SMGState free(Integer address, Integer offset, SMGObject smgObject)
      throws SMGInconsistentException {

    if (!heap.isHeapObject(smgObject) && !heap.isObjectExternallyAllocated(smgObject)) {
      // You may not free any objects not on the heap.

      return setInvalidFree();
    }

    if (!(offset == 0)) {
      // you may not invoke free on any address that you
      // didn't get through a malloc invocation.
      // TODO: externally allocated memory could be freed partially

      return setInvalidFree();
    }

    if (!heap.isObjectValid(smgObject)) {
      // you may not invoke free multiple times on
      // the same object

      return setInvalidFree();
    }

    heap.setValidity(smgObject, false);
    heap.setExternallyAllocatedFlag(smgObject, false);
    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(smgObject);

    List<SMGEdgeHasValue> to_remove = new ArrayList<>();
    for (SMGEdgeHasValue edge : heap.getHVEdges(filter)) {
      to_remove.add(edge);
    }

    for (SMGEdgeHasValue edge : to_remove) {
      heap.removeHasValueEdge(edge);
    }

    // update type information in free procedure
    heap.removeTypeInfo(smgObject.getLabel());

    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return this;
  }

  /**
   * Drop the stack frame representing the stack of
   * the function with the given name
   */
  public void dropStackFrame() throws SMGInconsistentException {
    heap.dropStackFrame();
    performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  public void pruneUnreachable() throws SMGInconsistentException {
    heap.pruneUnreachable();
    //TODO: Explicit values pruning
    performConsistencyCheck(SMGRuntimeCheck.HALF);
  }

  /**
   * Signals an invalid free call.
   */
  public SMGState setInvalidFree() {
    return new SMGState(this, Property.INVALID_FREE);
  }

  public Set<SMGEdgeHasValue> getHVEdges(SMGEdgeHasValueFilter pFilter) {
    return heap.getHVEdges(pFilter);
  }

  Set<SMGEdgeHasValue> getHVEdges() {
    return heap.getHVEdges();
  }

  @Nullable
  public MemoryLocation resolveMemLoc(SMGAddress pValue, String pFunctionName) {
    return heap.resolveMemLoc(pValue, pFunctionName);
  }

  /**
   * Copys (shallow) the hv-edges of source in the given source range
   * to the target at the given target offset. Note that the source
   * range (pSourceRangeSize - pSourceRangeOffset) has to fit into
   * the target range ( size of pTarget - pTargetRangeOffset).
   * Also, pSourceRangeOffset has to be less or equal to the size
   * of the source Object.
   *
   * This method is mainly used to assign struct variables.
   *
   * @param pSource            the SMGObject providing the hv-edges
   * @param pTarget            the target of the copy process
   * @param pTargetRangeOffset begin the copy of source at this offset
   * @param pSourceRangeSize   the size of the copy of source (not the size of the copy, but the
   *                           size to the last bit of the source which should be copied).
   * @param pSourceRangeOffset insert the copy of source into target at this offset
   * @throws SMGInconsistentException thrown if the copying leads to an inconsistent SMG.
   */
  public SMGState copy(
      SMGObject pSource, SMGObject pTarget, int pSourceRangeOffset,
      int pSourceRangeSize, int pTargetRangeOffset) throws SMGInconsistentException {

    SMGState newSMGState = this;

    int copyRange = pSourceRangeSize - pSourceRangeOffset;

    // some sanity checks, if one of these safe conditions fails, we just return the original
    // state instead of letting the analyzer down
    if (!(pSource.getSize() >= pSourceRangeSize) ||
        !(pSourceRangeOffset >= 0) ||
        !(pTargetRangeOffset >= 0) ||
        !(copyRange >= 0) ||
        !(copyRange <= pTarget.getSize())) {
      return newSMGState;
    }

    // If copy range is 0, do nothing
    if (copyRange == 0) {
      return newSMGState;
    }

    int targetRangeSize = pTargetRangeOffset + copyRange;

    SMGEdgeHasValueFilter filterSource = new SMGEdgeHasValueFilter();
    filterSource.filterByObject(pSource);
    SMGEdgeHasValueFilter filterTarget = new SMGEdgeHasValueFilter();
    filterTarget.filterByObject(pTarget);

    //Remove all Target edges in range
    Set<SMGEdgeHasValue> targetEdges = getHVEdges(filterTarget);

    for (SMGEdgeHasValue edge : targetEdges) {
      if (edge.overlapsWith(pTargetRangeOffset, targetRangeSize, heap.getMachineModel())) {
        boolean hvEdgeIsZero = edge.getValue() == heap.getNullValue();
        heap.removeHasValueEdge(edge);
        if (hvEdgeIsZero) {
          SMGObject object = edge.getObject();

          MachineModel maModel = heap.getMachineModel();

          // Shrink overlapping zero edge
          int zeroEdgeOffset = edge.getOffset();

          int zeroEdgeOffset2 = zeroEdgeOffset + edge.getSizeInBytes(maModel);

          if (zeroEdgeOffset < pTargetRangeOffset) {
            SMGEdgeHasValue newZeroEdge =
                new SMGEdgeHasValue(pTargetRangeOffset - zeroEdgeOffset, zeroEdgeOffset, object, 0);
            heap.addHasValueEdge(newZeroEdge);
          }

          if (targetRangeSize < zeroEdgeOffset2) {
            SMGEdgeHasValue newZeroEdge =
                new SMGEdgeHasValue(zeroEdgeOffset2 - targetRangeSize, targetRangeSize, object, 0);
            heap.addHasValueEdge(newZeroEdge);
          }
        }
      }
    }

    // Copy all Source edges
    Set<SMGEdgeHasValue> sourceEdges = getHVEdges(filterSource);

    // Shift the source edge offset depending on the target range offset
    int copyShift = pTargetRangeOffset - pSourceRangeOffset;

    for (SMGEdgeHasValue edge : sourceEdges) {
      if (edge.overlapsWith(pSourceRangeOffset, pSourceRangeSize, heap.getMachineModel())) {
        int offset = edge.getOffset() + copyShift;
        newSMGState = writeValue(pTarget, offset, edge.getType(), edge.getValue()).getState();
      }
    }

    performConsistencyCheck(SMGRuntimeCheck.FULL);
    //TODO Why do I do this here?
    heap.pruneUnreachable();
    performConsistencyCheck(SMGRuntimeCheck.FULL);
    return newSMGState;
  }

  /**
   * Signals a dereference of a pointer or array
   * which could not be resolved.
   */
  public SMGState setUnknownDereference() {
    //TODO: This can actually be an invalid read too
    //      The flagging mechanism should be improved

    return new SMGState(this, Property.INVALID_WRITE);
  }

  public SMGObject getNullObject() {
    return heap.getNullObject();
  }

  public void identifyEqualValues(SMGKnownSymValue pKnownVal1, SMGKnownSymValue pKnownVal2) {

    assert !isInNeq(pKnownVal1, pKnownVal2);
    assert !(explicitValues.get(pKnownVal1) != null &&
        explicitValues.get(pKnownVal1).equals(explicitValues.get(pKnownVal2)));

    heap.mergeValues(pKnownVal1.getAsInt(), pKnownVal2.getAsInt());
    SMGKnownExpValue expVal = explicitValues.remove(pKnownVal2);
    if (expVal != null) {
      explicitValues.put(pKnownVal1, expVal);
    }
  }

  public void identifyNonEqualValues(SMGKnownSymValue pKnownVal1, SMGKnownSymValue pKnownVal2) {
    heap.addNeqRelation(pKnownVal1.getAsInt(), pKnownVal2.getAsInt());
  }

  public void addPredicateRelation(
      SMGSymbolicValue pV1, SMGSymbolicValue pV2, BinaryOperator pOp,
      CFAEdge pEdge) {
    heap.addPredicateRelation(pV1, pV2, pOp, pEdge);
  }

  public void addPredicateRelation(
      SMGSymbolicValue pV1, SMGExplicitValue pV2, BinaryOperator pOp,
      CFAEdge pEdge) {
    heap.addPredicateRelation(pV1, pV2, pOp, pEdge);
  }

  public void putExplicit(SMGKnownSymValue pKey, SMGKnownExpValue pValue) {

    if (explicitValues.inverse().containsKey(pValue)) {
      SMGKnownSymValue symValue = explicitValues.inverse().get(pValue);

      if (pKey.getAsInt() != symValue.getAsInt()) {
        explicitValues.remove(symValue);
        heap.mergeValues(pKey.getAsInt(), symValue.getAsInt());
        explicitValues.put(pKey, pValue);
      }

      return;
    }

    explicitValues.put(pKey, pValue);
  }

  public void clearExplicit(SMGKnownSymValue pKey) {
    explicitValues.remove(pKey);
  }

  boolean isExplicit(int value) {
    SMGKnownSymValue key = SMGKnownSymValue.valueOf(value);

    return explicitValues.containsKey(key);
  }

  SMGKnownExpValue getExplicit(int value) {
    SMGKnownSymValue key = SMGKnownSymValue.valueOf(value);

    assert explicitValues.containsKey(key);
    return explicitValues.get(key);
  }

  public SMGExplicitValue getExplicit(SMGKnownSymValue pKey) {
    if (explicitValues.containsKey(pKey)) {
      return explicitValues.get(pKey);
    }
    return SMGUnknownValue.getInstance();
  }

  private static enum Property {
    INVALID_READ,
    INVALID_WRITE,
    INVALID_FREE,
    INVALID_HEAP
  }

  public boolean isInNeq(SMGSymbolicValue pValue1, SMGSymbolicValue pValue2) {

    if (pValue1.isUnknown() || pValue2.isUnknown()) {
      return false;
    } else {
      return heap.haveNeqRelation(pValue1.getAsInt(), pValue2.getAsInt());
    }
  }

  IDExpression createIDExpression(SMGObject pObject) {
    return heap.createIDExpression(pObject);
  }

  @Override
  public SMGStateInformation forget(MemoryLocation pLocation) {
    return heap.forget(pLocation);
  }

  @Override
  public void remember(MemoryLocation pLocation, SMGStateInformation pForgottenInformation) {
    heap.remember(pLocation, pForgottenInformation);
  }

  @Override
  public Set<MemoryLocation> getTrackedMemoryLocations() {
    return heap.getTrackedMemoryLocations();
  }

  @Override
  public int getSize() {
    return heap.getHVEdges().size();
  }

  public SMGInterpolant createInterpolant() {
    // TODO Copy necessary?
    return new SMGInterpolant(new HashMap<>(explicitValues), new CLangSMG(heap), logger,
        externalAllocationSize);
  }

  public CType getTypeForMemoryLocation(MemoryLocation pMemoryLocation) {
    return heap.getTypeForMemoryLocation(pMemoryLocation);
  }

  public SMGObject getObjectForFunction(CFunctionDeclaration pDeclaration) {

    /* Treat functions as global objects with unnkown memory size.
     * Only write them into the smg when necessary*/
    String functionQualifiedSMGName = getUniqueFunctionName(pDeclaration);

    return heap.getObjectForVisibleVariable(functionQualifiedSMGName);
  }

  public SMGObject createObjectForFunction(CFunctionDeclaration pDeclaration)
      throws SMGInconsistentException {

    /* Treat functions as global variable with unknown memory size.
     * Only write them into the smg when necessary*/
    String functionQualifiedSMGName = getUniqueFunctionName(pDeclaration);

    assert heap.getObjectForVisibleVariable(functionQualifiedSMGName) == null;

    return addGlobalVariable(0, functionQualifiedSMGName);
  }

  private String getUniqueFunctionName(CFunctionDeclaration pDeclaration) {

    StringBuilder functionName = new StringBuilder(pDeclaration.getQualifiedName());

    // FIX: this possibly leads to problem when multiple files to be analyzed
    /*
    for (CParameterDeclaration parameterDcl : pDeclaration.getParameters()) {
      functionName.append("_");
      functionName.append(parameterDcl.toASTString().replace("*", "_").replace(" ", "_"));
    }
    */

    return "__" + functionName;
  }

  public PointerComparisonResult comparePointer(
      SMGSymbolicValue pV1, SMGSymbolicValue pV2,
      BinaryOperator pOp) {

    SMGEdgePointsTo pointer1 = heap.getPointer(pV1.getAsInt());
    SMGEdgePointsTo pointer2 = heap.getPointer(pV2.getAsInt());
    SMGObject object1 = pointer1.getObject();
    SMGObject object2 = pointer2.getObject();

    boolean isTrue = false;
    boolean isFalse = true;

    // there can be more precise comparsion when pointer point to the
    // same object.
    if (object1 == object2) {
      int offset1 = pointer1.getOffset();
      int offset2 = pointer2.getOffset();

      switch (pOp) {
        case GREATER_EQUAL:
          isTrue = offset1 >= offset2;
          isFalse = !isTrue;
          break;
        case GREATER_THAN:
          isTrue = offset1 > offset2;
          isFalse = !isTrue;
          break;
        case LESS_EQUAL:
          isTrue = offset1 <= offset2;
          isFalse = !isTrue;
          break;
        case LESS_THAN:
          isTrue = offset1 < offset2;
          isFalse = !isTrue;
          break;
        default:
          throw new AssertionError("Impossible case thrown");
      }

    }
    return PointerComparisonResult.valueOf(isTrue, isFalse);
  }

  public static class PointerComparisonResult {

    private final boolean isTrue;
    private final boolean isFalse;

    private PointerComparisonResult(boolean pIsTrue, boolean pIsFalse) {
      isTrue = pIsTrue;
      isFalse = pIsFalse;
    }

    public static PointerComparisonResult valueOf(boolean pIsTrue, boolean pIsFalse) {
      return new PointerComparisonResult(pIsTrue, pIsFalse);
    }

    public boolean isTrue() {
      return isTrue;
    }

    public boolean isFalse() {
      return isFalse;
    }
  }

  public SMGState abstraction() {
    SMGAbstractionManager manager = new SMGAbstractionManager(heap);
    return new SMGState(this, manager.execute());
  }

  public Set<CExpression> getInvalidReadExpression() {

    return SMGTransferRelation.errs;
    /*
    if(invalidReadExpression == null) {
      return Collections.emptySet();
    }
    return invalidReadExpression;*/
  }

  public void setInvalidReadExpression(CExpression exp) {
    SMGTransferRelation.errs.add(exp);
    /*
    if (invalidReadExpression == null) {
      invalidReadExpression = new HashSet<>();
    }
    invalidReadExpression.add(exp);*/
  }

  public Set<CExpression> getInvalidFreeExpression() {
    return SMGTransferRelation.errs;
    //return invalidFreeExpression;
  }

  public void setInvalidFreeExpression(CExpression exp) {
    if (invalidFreeExpression == null) {
      invalidFreeExpression = new HashSet<>();
    }
    invalidFreeExpression.add(exp);
  }

  @Override
  public String toDOTLabel() {
    return "";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}