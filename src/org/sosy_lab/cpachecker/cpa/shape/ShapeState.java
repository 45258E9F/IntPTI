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
package org.sosy_lab.cpachecker.cpa.shape;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.SwitchableGraphable;
import org.sosy_lab.cpachecker.core.interfaces.summary.SummaryAcceptableState;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessFunctionInstance;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ArithFunctionSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ArithLoopSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeExternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeFunctionInstance;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeInternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.cpa.range.ArrayUncertainIndexSegment;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.shape.communicator.AccessSummaryApplicator;
import org.sosy_lab.cpachecker.cpa.shape.communicator.ArithSummaryApplicator;
import org.sosy_lab.cpachecker.cpa.shape.communicator.RangeSummaryApplicator;
import org.sosy_lab.cpachecker.cpa.shape.constraint.BinarySE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstantSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstraintRepresentation;
import org.sosy_lab.cpachecker.cpa.shape.constraint.FormulaCreator;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph.MemoryPoint;
import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph.StackObjectInfo;
import org.sosy_lab.cpachecker.cpa.shape.graphs.ShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdgeFilter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGPointToEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGRegion;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGResource;
import org.sosy_lab.cpachecker.cpa.shape.merge.CShapeGraphJoin;
import org.sosy_lab.cpachecker.cpa.shape.merge.abs.GuardedValue;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.MergeTable;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.ShapeComplexLessOrEqual;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.ShapeGeneralLessOrEqual;
import org.sosy_lab.cpachecker.cpa.shape.util.EquivalenceRelation;
import org.sosy_lab.cpachecker.cpa.shape.util.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.shape.util.UnknownTypes;
import org.sosy_lab.cpachecker.cpa.shape.values.Address;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.StateEdgePair;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndStateList;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;
import org.sosy_lab.cpachecker.util.access.PointerDereferenceSegment;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.Model;
import org.sosy_lab.solver.api.Model.ValueAssignment;
import org.sosy_lab.solver.api.ProverEnvironment;
import org.sosy_lab.solver.api.SolverContext.ProverOptions;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import javax.annotation.Nullable;

public class ShapeState implements LatticeAbstractState<ShapeState>, SummaryAcceptableState,
                                   SwitchableGraphable {

  private final AtomicLong counter;

  private final BiMap<KnownSymbolicValue, KnownExplicitValue> explicitValues = HashBiMap.create();
  private final CShapeGraph memory;
  private final LogManager logger;
  private final long preId;
  private final long id;

  /* *********** */
  /* error flags */
  /* *********** */

  private boolean invalidRead = false;
  private boolean invalidWrite = false;
  private boolean invalidFree = false;
  private boolean memoryLeak = false;
  private boolean stackAddressReturn = false;

  private Set<CExpression> invalidReadExpression = new HashSet<>();
  private Set<CFAEdge> leakCFAEdges = new HashSet<>();

  /* ********************* */
  /* constraint processing */
  /* ********************* */

  private Solver solver;
  private ProverEnvironment prover;
  private FormulaCreator formulaCreator;
  private FormulaManagerView formulaManager;
  private List<BooleanFormula> formulae = new ArrayList<>();
  private int numOfProcessedConstraints = 0;
  private boolean disjunctionProcessed = false;

  /* ************ */
  /* constructors */
  /* ************ */

  public ShapeState(LogManager pLogger, MachineModel pMachineModel) {
    memory = new CShapeGraph(pMachineModel);
    logger = pLogger;
    counter = new AtomicLong(0);
    preId = counter.getAndIncrement();
    id = counter.getAndIncrement();
  }

  public ShapeState(ShapeState pState) {
    memory = new CShapeGraph(pState.memory);
    logger = pState.logger;
    preId = pState.id;
    counter = pState.counter;
    id = counter.getAndIncrement();
    explicitValues.putAll(pState.explicitValues);
    invalidFree = pState.invalidFree;
    invalidRead = pState.invalidRead;
    invalidWrite = pState.invalidWrite;
    memoryLeak = pState.memoryLeak;
    stackAddressReturn = pState.stackAddressReturn;
    invalidReadExpression.addAll(pState.invalidReadExpression);
    leakCFAEdges.addAll(pState.leakCFAEdges);

    solver = pState.solver;
    prover = pState.prover;
    formulaManager = pState.formulaManager;
    formulaCreator = pState.formulaCreator;
    formulae.addAll(pState.formulae);
    numOfProcessedConstraints = pState.numOfProcessedConstraints;
    disjunctionProcessed = pState.disjunctionProcessed;
  }

  public ShapeState(ShapeState pState, MemoryErrorProperty property) {
    memory = new CShapeGraph(pState.memory);
    logger = pState.logger;
    preId = pState.id;
    counter = pState.counter;
    id = counter.getAndIncrement();
    explicitValues.putAll(pState.explicitValues);

    // carefully set the specified error property
    boolean pInvalidFree = pState.invalidFree;
    boolean pInvalidRead = pState.invalidRead;
    boolean pInvalidWrite = pState.invalidWrite;
    boolean pMemoryLeak = pState.memoryLeak;
    boolean pStackReturn = pState.stackAddressReturn;
    invalidReadExpression.addAll(pState.invalidReadExpression);
    leakCFAEdges.addAll(pState.leakCFAEdges);
    switch (property) {
      case INVALID_READ:
        pInvalidRead = true;
        break;
      case INVALID_WRITE:
        pInvalidWrite = true;
        break;
      case INVALID_FREE:
        pInvalidFree = true;
        break;
      default:
        throw new IllegalArgumentException("unsupported memory safety violation");
    }
    invalidFree = pInvalidFree;
    invalidRead = pInvalidRead;
    invalidWrite = pInvalidWrite;
    memoryLeak = pMemoryLeak;
    stackAddressReturn = pStackReturn;

    solver = pState.solver;
    prover = pState.prover;
    formulaManager = pState.formulaManager;
    formulaCreator = pState.formulaCreator;
    formulae.addAll(pState.formulae);
    numOfProcessedConstraints = pState.numOfProcessedConstraints;
    disjunctionProcessed = pState.disjunctionProcessed;
  }

  /**
   * A constructor for state merge.
   */
  private ShapeState(
      LogManager pLogger, CShapeGraph pMemory,
      BiMap<KnownSymbolicValue, KnownExplicitValue> pExplicit, AtomicLong pCounter,
      long pPreId, boolean pInvalidRead, boolean pInvalidWrite,
      boolean pInvalidFree, boolean pStackReturn, Set<CExpression> pInvalidExpSet,
      Solver pSolver, FormulaManagerView pFormulaManager,
      FormulaCreator pFormulaCreator) {
    // merged state is constructed here
    logger = pLogger;
    memory = pMemory;
    explicitValues.putAll(pExplicit);
    counter = pCounter;
    preId = pPreId;
    id = counter.getAndIncrement();
    invalidRead = pInvalidRead;
    invalidWrite = pInvalidWrite;
    invalidFree = pInvalidFree;
    memoryLeak = memory.hasMemoryLeak();
    invalidReadExpression.addAll(pInvalidExpSet);
    leakCFAEdges.addAll(memory.getLeakEdges());
    stackAddressReturn = pStackReturn;

    solver = pSolver;
    formulaManager = pFormulaManager;
    formulaCreator = pFormulaCreator;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(explicitValues, memory, invalidRead, invalidWrite,
        invalidFree, memoryLeak, stackAddressReturn, invalidReadExpression, leakCFAEdges);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof ShapeState)) {
      return false;
    }
    ShapeState that = (ShapeState) obj;
    return
        Objects.equal(explicitValues, that.explicitValues) &&
            Objects.equal(memory, that.memory) &&
            Objects.equal(invalidRead, that.invalidRead) &&
            Objects.equal(invalidWrite, that.invalidWrite) &&
            Objects.equal(invalidFree, that.invalidFree) &&
            Objects.equal(memoryLeak, that.memoryLeak) &&
            Objects.equal(stackAddressReturn, that.stackAddressReturn) &&
            Objects.equal(invalidReadExpression, that.invalidReadExpression) &&
            Objects.equal(leakCFAEdges, that.leakCFAEdges);
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }

  /* ******************* */
  /* modifying functions */
  /* ******************* */

  private SGObject addGlobalVariable(CType pType, String pName) {
    SGRegion object = new SGRegion(pName, pType, memory.getMachineModel().getSizeof(pType),
        SGRegion.STATIC);
    memory.addGlobalObject(object);
    return object;
  }

  /**
   * Global variable without initializer is nullified (i.e. zero-initialized).
   */
  SGObject addGlobalVariable(CType pType, ShapeExplicitValue pSize, String pName) {
    SGRegion object = new SGRegion(pName, pType, pSize, SGRegion.STATIC, true);
    memory.addGlobalObject(object);
    return object;
  }

  /**
   * Add local variable with awareness of VLA. All the VLAs (including ones with explicit size
   * derived statically) should be allocated in non-zero segments.
   */
  SGObject addLocalVariable(CType pType, ShapeExplicitValue pSize, String pName) {
    // check if there exists a stack object with the same name
    SGObject oldObject = memory.getVisibleStackObject(pName);
    if (oldObject != null) {
      memory.removeStackObject(oldObject);
    }
    SGRegion object = new SGRegion(pName, pType, pSize, SGRegion.STATIC);
    memory.addStackObject(object, false);
    return object;
  }

  SGObject addLocalVariableForVLA(
      CType pType, ShapeExplicitValue pSize, SymbolicExpression
      sizeExp, String pName) {
    // check if there exists a stack object with the same name
    SGObject oldObject = memory.getVisibleStackObject(pName);
    if (oldObject != null) {
      memory.removeObjectAndEdges(oldObject);
    }
    SGRegion object = new SGRegion(pName, pType, pSize, SGRegion.DYNAMIC);
    memory.addSizeInfo(object, sizeExp);
    memory.addStackObject(object, true);
    return object;
  }

  void addLocalVariable(SGRegion pRegion) {
    memory.addStackObject(pRegion, false);
  }

  public void addStackFrame(CFunctionDeclaration pDeclaration) {
    memory.addStackFrame(pDeclaration);
  }

  public void addGlobalObject(SGRegion region) {
    memory.addGlobalObject(region);
  }

  /**
   * Write a symbolic value into the specified field.
   */
  public StateEdgePair writeValue(
      SGObject pObject, int pOffset,
      CType pType, ShapeSymbolicValue pValue) {
    long newValue;

    if (pValue.isUnknown()) {
      newValue = SymbolicValueFactory.getNewValue();
    } else {
      newValue = pValue.getAsLong();
    }

    // if the new value represents a known address, then we should add necessary point-to edge
    if (pValue instanceof ShapeAddressValue) {
      // if this address value has been already included, then there is no need to add a new
      // point-to edge into memory graph
      if (!containsValue(newValue)) {
        Address address = ((ShapeAddressValue) pValue).getAddress();
        if (!address.isUnknown()) {
          memory.addValue(newValue);
          SGPointToEdge newPTEdge = new SGPointToEdge(newValue, address.getObject(), address
              .getOffset().getAsInt());
          memory.addPointToEdge(newPTEdge);
        }
      }
    }
    // finally, we add has-value edge
    return writeValue(pObject, pOffset, pType, newValue);
  }

  /**
   * Write a symbolic value into an object (via a has-value edge)
   *
   * @param pObject memory object that the target value belongs to
   * @param pOffset offset of memory to be written on
   * @param pType   type of field written to
   * @param pValue  (symbolic) value to be written
   */
  private StateEdgePair writeValue(
      SGObject pObject,
      int pOffset,
      CType pType,
      Long pValue) {
    if (!memory.isObjectValid(pObject)) {
      // then we have invalid write error
      ShapeState newState = setInvalidWrite();
      return new StateEdgePair(newState);
    }
    SGHasValueEdge newEdge = new SGHasValueEdge(pType, pOffset, pObject, pValue);

    // check if such edge has been presented already
    SGHasValueEdgeFilter filter = SGHasValueEdgeFilter.objectFilter(pObject);
    Set<SGHasValueEdge> edges = memory.getHVEdges(filter);
    if (edges.contains(newEdge)) {
      // there is no need to change the state
      return new StateEdgePair(this, newEdge);
    }

    // check if the value to be added is new
    if (!memory.getValues().contains(pValue)) {
      memory.addValue(pValue);
    }

    // if there are has-value edges with value zero overlap the current has-value edge, then we
    // keep the reminiscent part of existing edges
    Set<SGHasValueEdge> overlappingZeroEdges = new HashSet<>();
    Set<SGHasValueEdge> overlappingNonZeroEdges = new HashSet<>();
    boolean isZeroObject = pObject.isZeroInit();
    for (SGHasValueEdge hvEdge : edges) {
      boolean overlaps = newEdge.overlapsWith(hvEdge, memory.getMachineModel());
      boolean isZeroEdge = hvEdge.getValue() == CShapeGraph.getNullAddress();
      if (overlaps) {
        if (isZeroEdge) {
          overlappingZeroEdges.add(hvEdge);
        } else {
          overlappingNonZeroEdges.add(hvEdge);
        }
      }
    }
    if (isZeroObject) {
      // new edge to be written should also be updated
      shrinkOverlappingEdgesForZI(newEdge, overlappingZeroEdges, overlappingNonZeroEdges);
    } else {
      shrinkOverlappingEdgesForNonZI(newEdge, overlappingZeroEdges, overlappingNonZeroEdges);
    }
    // if the object is zero-initialized and the value to be written is 0, we do not add new edge
    if (!isZeroObject || pValue != 0) {
      memory.addHasValueEdge(newEdge);
    }
    return new StateEdgePair(this, newEdge);
  }


  /**
   * Precondition: the object of the input new has-value edge is not zero-initialized.
   */
  private void shrinkOverlappingEdgesForNonZI(
      SGHasValueEdge edge,
      Set<SGHasValueEdge> pOverlappingZeroEdges,
      Set<SGHasValueEdge> pOverlappingNonZeroEdges) {
    SGObject object = edge.getObject();
    assert (!object.isZeroInit());
    int offset = edge.getOffset();
    MachineModel model = memory.getMachineModel();
    int ends = offset + edge.getSizeInBytes(model);
    for (SGHasValueEdge hvEdge : pOverlappingZeroEdges) {
      memory.removeHasValueEdge(hvEdge);
      int zeroOffset = hvEdge.getOffset();
      int zeroEnds = zeroOffset + hvEdge.getSizeInBytes(model);
      if (zeroOffset < offset) {
        SGHasValueEdge newZeroEdge = new SGHasValueEdge(offset - zeroOffset, zeroOffset, object,
            ShapeGraph.getNullAddress());
        memory.addHasValueEdge(newZeroEdge);
        offset = zeroOffset;
      }
      if (ends < zeroEnds) {
        SGHasValueEdge newZeroEdge = new SGHasValueEdge(zeroEnds - ends, ends, object, ShapeGraph
            .getNullAddress());
        memory.addHasValueEdge(newZeroEdge);
        ends = zeroEnds;
      }
    }
    for (SGHasValueEdge hvEdge : pOverlappingNonZeroEdges) {
      memory.removeHasValueEdge(hvEdge);
      int hvOffset = hvEdge.getOffset();
      int hvEnds = hvOffset + hvEdge.getSizeInBytes(model);
      if (hvOffset < offset) {
        Long newValue = SymbolicValueFactory.getNewValue();
        SGHasValueEdge newEdge = new SGHasValueEdge(offset - hvOffset, hvOffset, object, newValue);
        memory.addValue(newValue);
        memory.addHasValueEdge(newEdge);
      }
      if (ends < hvEnds) {
        Long newValue = SymbolicValueFactory.getNewValue();
        SGHasValueEdge newEdge = new SGHasValueEdge(hvEnds - ends, ends, object, newValue);
        memory.addValue(newValue);
        memory.addHasValueEdge(newEdge);
      }
    }
  }

  /**
   * Precondition: the object of the input new has-value edge is zero-initialized.
   */
  private void shrinkOverlappingEdgesForZI(
      SGHasValueEdge edge,
      Set<SGHasValueEdge> pOverlappingZeroEdges,
      Set<SGHasValueEdge> pOverlappingNonZeroEdges) {
    SGObject object = edge.getObject();
    assert (object.isZeroInit());
    int offset = edge.getOffset();
    MachineModel model = memory.getMachineModel();
    int ends = offset + edge.getSizeInBytes(model);
    // STEP 1: split zero edges and change the border of the inserted edge
    for (SGHasValueEdge zeroEdge : pOverlappingZeroEdges) {
      memory.removeHasValueEdge(zeroEdge);
      int zeroOffset = zeroEdge.getOffset();
      int zeroEnds = zeroOffset + zeroEdge.getSizeInBytes(model);
      if (zeroOffset < offset) {
        offset = zeroOffset;
      }
      if (ends < zeroEnds) {
        ends = zeroEnds;
      }
    }
    // STEP 3: shrink non-zero edges
    for (SGHasValueEdge nonZeroEdge : pOverlappingNonZeroEdges) {
      memory.removeHasValueEdge(nonZeroEdge);
      int hvOffset = nonZeroEdge.getOffset();
      int hvEnds = hvOffset + nonZeroEdge.getSizeInBytes(model);
      if (hvOffset < offset) {
        Long newValue = SymbolicValueFactory.getNewValue();
        memory.addValue(newValue);
        SGHasValueEdge newHVEdge = new SGHasValueEdge(offset - hvOffset, hvOffset, object,
            newValue);
        memory.addHasValueEdge(newHVEdge);
      }
      if (ends < hvEnds) {
        Long newValue = SymbolicValueFactory.getNewValue();
        memory.addValue(newValue);
        SGHasValueEdge newHVEdge = new SGHasValueEdge(hvEnds - ends, ends, object, newValue);
        memory.addHasValueEdge(newHVEdge);
      }
    }
  }

  /**
   * Create a new symbolic value as the address of the specified memory object.
   * (Note: this method does not check if the specified memory object has already had address
   * value.)
   *
   * @param pObject memory object
   * @param pOffset offset in the memory object
   * @param pValue  specified (symbolic) value
   * @return the new state with new address linkage
   */
  public ShapeState writeAddress(SGObject pObject, int pOffset, long pValue) {
    SGPointToEdge newPTEdge = new SGPointToEdge(pValue, pObject, pOffset);
    if (!memory.getValues().contains(pValue)) {
      memory.addValue(pValue);
    }
    memory.addPointToEdge(newPTEdge);
    return this;
  }

  /**
   * Swipe out has-value edges in the specified memory region. If a has-value edge is removed,
   * then the corresponding memory region should have some values.
   *
   * @param pObject a memory object
   * @param pOffset the offset in the memory object
   * @param pType   the type to be swiped out
   * @return updated shape state (after removal)
   */
  public ShapeState removeValue(SGObject pObject, int pOffset, CType pType) {
    if (!memory.isObjectValid(pObject)) {
      // we don't know how to change the memory state
      return this;
    }
    // If the specified type is const char *, we should swipe all the remaining has-value edges out.
    // Note: when checking the compatibility of two types, const is discarded. Thus, we should
    // carefully check if the given type is const.
    if (CoreShapeAdapter.isCompatible(pType, CPointerType.POINTER_TO_CONST_CHAR) ||
        pType.isConst()) {
      return removeString(pObject, pOffset);
    }
    // check if current object is zero-initialized
    boolean isZeroInit = pObject.isZeroInit();
    // generate new symbolic value
    Long newValue = SymbolicValueFactory.getNewValue();
    memory.addValue(newValue);
    // create a new edge to be inserted
    SGHasValueEdge vEdge = new SGHasValueEdge(pType, pOffset, pObject, newValue);
    SGHasValueEdgeFilter filter = SGHasValueEdgeFilter.objectFilter(pObject);
    Set<SGHasValueEdge> edges = memory.getHVEdges(filter);
    Set<SGHasValueEdge> overlappingZeroEdges = new HashSet<>();
    Set<SGHasValueEdge> overlappingNonZeroEdges = new HashSet<>();
    for (SGHasValueEdge hvEdge : edges) {
      boolean overlaps = vEdge.overlapsWith(hvEdge, memory.getMachineModel());
      if (overlaps) {
        if (hvEdge.getValue() == ShapeGraph.getNullAddress()) {
          overlappingZeroEdges.add(hvEdge);
        } else {
          overlappingNonZeroEdges.add(hvEdge);
        }
      }
    }
    if (!isZeroInit) {
      shrinkOverlappingEdgesForNonZI(vEdge, overlappingZeroEdges, overlappingNonZeroEdges);
    } else {
      shrinkOverlappingEdgesForZI(vEdge, overlappingNonZeroEdges, overlappingNonZeroEdges);
    }
    memory.addHasValueEdge(vEdge);
    return this;
  }

  /**
   * Swipe out all the has-value edges in the pObject since the given offset.
   * Precondition: pObject is valid.
   */
  private ShapeState removeString(SGObject pObject, int pOffset) {
    boolean isZeroInit = pObject.isZeroInit();
    ShapeExplicitValue oSize = pObject.getSize();
    int bound = pOffset;
    SGHasValueEdgeFilter possibleFilter = SGHasValueEdgeFilter.objectFilter(pObject);
    Set<SGHasValueEdge> possibleEdges = memory.getHVEdges(possibleFilter);
    MachineModel model = memory.getMachineModel();
    if (oSize.isUnknown()) {
      for (SGHasValueEdge edge : possibleEdges) {
        int upper = edge.getOffset() + edge.getSizeInBytes(model);
        if (bound < upper) {
          bound = upper;
        }
      }
    } else {
      bound = oSize.getAsInt();
    }
    if (bound == pOffset) {
      // the length of modifying region is 0
      return this;
    }
    // we create a virtual edge for the purpose of shrinking overlapped edges
    SGHasValueEdge editEdge = new SGHasValueEdge(UnknownTypes.createTypeWithLength(bound -
        pOffset), pOffset, pObject, 0);
    Set<SGHasValueEdge> overlappingZeroEdges = new HashSet<>();
    Set<SGHasValueEdge> overlappingNonZeroEdges = new HashSet<>();
    for (SGHasValueEdge hvEdge : possibleEdges) {
      boolean overlaps = editEdge.overlapsWith(hvEdge, model);
      if (overlaps) {
        if (hvEdge.getValue() == ShapeGraph.getNullAddress()) {
          overlappingZeroEdges.add(hvEdge);
        } else {
          overlappingNonZeroEdges.add(hvEdge);
        }
      }
    }
    if (!isZeroInit) {
      shrinkOverlappingEdgesForNonZI(editEdge, overlappingZeroEdges, overlappingNonZeroEdges);
    } else {
      shrinkOverlappingEdgesForZI(editEdge, overlappingNonZeroEdges, overlappingZeroEdges);
    }
    // insert fresh symbolic value to each byte in the modifying region
    for (int i = pOffset; i < bound; i++) {
      Long newValue = SymbolicValueFactory.getNewValue();
      memory.addValue(newValue);
      SGHasValueEdge newEdge = new SGHasValueEdge(CNumericTypes.CHAR, i, pObject, newValue);
      memory.addHasValueEdge(newEdge);
    }
    return this;
  }

  /**
   * Create a new heap memory space.
   *
   * @param pLabel The label of memory object.
   * @param pType  The memory object should be interpreted as which type?
   * @param pSize  The size of memory object, can be uncertain.
   * @param pZero  Whether the memory object is zero-initialized
   * @param pEdge  CFAEdge of heap allocation
   * @return the address value of allocated heap object
   */
  public ShapeAddressValue addHeapAllocation(
      String pLabel, CType pType, SymbolicExpression
      pSize, boolean pZero, CFAEdge pEdge) {
    ShapeExplicitValue size = UnknownValue.getInstance();
    boolean addSizeInfo = true;
    if (pSize instanceof ConstantSE) {
      switch (pSize.getValueKind()) {
        case EXPLICIT:
          size = (KnownExplicitValue) pSize.getValue();
          addSizeInfo = false;
          break;
        case UNKNOWN:
          addSizeInfo = false;
          break;
        default:
          break;
      }
    }
    SGRegion region = new SGRegion(pLabel, pType, size, SGRegion.DYNAMIC, pZero);
    if (addSizeInfo) {
      memory.addSizeInfo(region, pSize);
    }
    long newValue = SymbolicValueFactory.getNewValue();
    SGPointToEdge ptEdge = new SGPointToEdge(newValue, region, 0);
    memory.addHeapObject(region, pEdge);
    memory.addValue(newValue);
    memory.addPointToEdge(ptEdge);
    return KnownAddressValue.valueOf(Address.valueOf(region, 0), newValue);
  }

  /**
   * Trim a memory object by changing its size and pruning has-value edges.
   * Precondition: only shape region in heap space can be reallocated.
   *
   * @param pEdge   the CFA edge of reallocation operation
   * @param pObject a memory object to be trimmed
   * @param newSize new size, which should be smaller than the original size
   * @return the re-allocated heap object
   */
  public ShapeAddressValue addHeapReallocation(CFAEdge pEdge, SGObject pObject, int newSize) {
    assert (pObject instanceof SGRegion);
    SGRegion newObject = new SGRegion(pObject.getLabel().concat("_r"), ((SGRegion) pObject)
        .getType(), KnownExplicitValue.valueOf(newSize), SGRegion.DYNAMIC, pObject.isZeroInit());
    // STEP 1: remove specified object and associated edges
    Set<SGHasValueEdge> removedHVEdges = new HashSet<>();
    Set<SGPointToEdge> removedPTEdges = new HashSet<>();
    memory.dropHeapObject(pObject);
    memory.removeObjectAndEdges(pObject, removedHVEdges, removedPTEdges);

    // STEP 2: choose edges associated with memory block of offset ranging in [0, newSize).
    Set<SGHasValueEdge> newHVEdges = new HashSet<>();
    Set<SGPointToEdge> newPTEdges = new HashSet<>();
    for (SGPointToEdge edge : removedPTEdges) {
      int offset = edge.getOffset();
      if (offset < newSize) {
        newPTEdges.add(new SGPointToEdge(edge.getValue(), newObject, offset));
      }
    }
    for (SGHasValueEdge edge : removedHVEdges) {
      int offset = edge.getOffset();
      if (offset < newSize) {
        int ends = offset + edge.getSizeInBytes(memory.getMachineModel());
        SGHasValueEdge newEdge;
        if (ends > newSize) {
          long value = edge.getValue();
          if (value == CShapeGraph.getNullAddress()) {
            newEdge = new SGHasValueEdge(newSize - offset, offset, newObject, CShapeGraph
                .getNullAddress());
          } else {
            Long fresh = SymbolicValueFactory.getNewValue();
            memory.addValue(fresh);
            newEdge = new SGHasValueEdge(newSize - offset, offset, newObject, fresh);
          }
        } else {
          newEdge = new SGHasValueEdge(edge.getType(), offset, newObject, edge.getValue());
        }
        newHVEdges.add(newEdge);
      }
    }
    // STEP 3: add new has-value edges and point-to edges
    memory.addHeapObject(newObject, pEdge);
    Long addressValue = null;
    for (SGHasValueEdge edge : newHVEdges) {
      memory.addHasValueEdge(edge);
    }
    for (SGPointToEdge edge : newPTEdges) {
      memory.addPointToEdge(edge);
      if (edge.getOffset() == 0) {
        addressValue = edge.getValue();
      }
    }
    if (addressValue == null) {
      // this case should not happen
      addressValue = SymbolicValueFactory.getNewValue();
      memory.addValue(addressValue);
      SGPointToEdge ptEdge = new SGPointToEdge(addressValue, newObject, 0);
      memory.addPointToEdge(ptEdge);
    }
    // STEP 4: set a correct reference count for the new object
    // Reference count and the number of associated point-to edges are not always consistent. A
    // reference should be x -> a -> y where x->a is has-value association and a->y is point-to
    // association.
    long ref = memory.countValidRef(newPTEdges);
    memory.setRef(newObject, ref);
    return KnownAddressValue.valueOf(Address.valueOf(newObject, 0), addressValue);
  }

  /**
   * A resource object is a special kind of heap object.
   * A resource can be a file handler or a network socket.
   * A resource should be created and released carefully by specific calls.
   *
   * @param pLabel   The label of the resource.
   * @param pCreator The creator of the resource.
   * @param pEdge    CFAEdge of resource creation.
   * @return the address value of allocated resource
   */
  public ShapeAddressValue addResource(String pLabel, String pCreator, CFAEdge pEdge) {
    SGResource resource = new SGResource(pLabel, pCreator);
    long newValue = SymbolicValueFactory.getNewValue();
    SGPointToEdge ptEdge = new SGPointToEdge(newValue, resource, 0);
    memory.addHeapObject(resource, pEdge);
    memory.addValue(newValue);
    memory.addPointToEdge(ptEdge);
    return KnownAddressValue.valueOf(Address.valueOf(resource, 0), newValue);
  }

  /**
   * Not all stack allocations are static. Methods such as _alloca() supports allocating a
   * variable length of stack memory block.
   *
   * @param pLabel label of allocated memory
   * @param pType  type of allocated memory, usually a pointer or array type
   * @param pSize  size of allocated memory, can be uncertain (denoted by Unknown value)
   */
  public ShapeAddressValue addStackAllocation(
      String pLabel, CType pType,
      SymbolicExpression pSize) {
    ShapeExplicitValue size = UnknownValue.getInstance();
    boolean addSizeInfo = true;
    if (pSize instanceof ConstantSE) {
      switch (pSize.getValueKind()) {
        case EXPLICIT:
          size = (KnownExplicitValue) pSize.getValue();
          addSizeInfo = false;
          break;
        case UNKNOWN:
          addSizeInfo = false;
          break;
        default:
          break;
      }
    }
    SGRegion region = new SGRegion(pLabel, pType, size, true);
    if (addSizeInfo) {
      memory.addSizeInfo(region, pSize);
    }
    long newValue = SymbolicValueFactory.getNewValue();
    SGPointToEdge ptEdge = new SGPointToEdge(newValue, region, 0);
    memory.addStackObject(region, true);
    memory.addValue(newValue);
    memory.addPointToEdge(ptEdge);
    return KnownAddressValue.valueOf(Address.valueOf(region, 0), newValue);
  }

  /**
   * A simulation of free procedure.
   */
  public ShapeState free(SGObject pObject, int pOffset) {
    if (!memory.isHeapObject(pObject)) {
      // free on-heap space
      return setInvalidFree();
    }
    if (pOffset != 0) {
      // partial free
      return setInvalidFree();
    }
    if (!memory.isObjectValid(pObject)) {
      // double-free
      return setInvalidFree();
    }
    // freed memory should have 0 reference count
    memory.resetRef(pObject);
    memory.setValidity(pObject, false);
    // remove relevant has-value edges
    SGHasValueEdgeFilter filter = SGHasValueEdgeFilter.objectFilter(pObject);
    List<SGHasValueEdge> forRemove = new ArrayList<>(memory.getHVEdges(filter));
    for (SGHasValueEdge edge : forRemove) {
      memory.removeHasValueEdge(edge);
    }
    return this;
  }

  /**
   * A simulation of resource shut-down.
   */
  public ShapeState freeResource(SGResource pResource, String pCreator) {
    if (!memory.isObjectValid(pResource)) {
      // double free, or double shutdown
      return setInvalidFree();
    }
    if (!pResource.getCreator().equals(pCreator)) {
      // invalid free, since we use wrong shutdown method
      return setInvalidFree();
    }
    memory.resetRef(pResource);
    memory.setValidity(pResource, false);
    // resource should not have has-value edge
    return this;
  }

  public boolean isStackObject(SGObject pObject) {
    return memory.isStackObject(pObject);
  }

  public boolean isHeapObject(SGObject pObject) {
    return memory.isHeapObject(pObject);
  }

  public void dropStackFrame() {
    memory.dropStackFrame();
  }

  /**
   * This method is served for checking memory leak for main function only.
   */
  public void dropGlobals() {
    memory.dropGlobals();
  }

  /**
   * This method is served for realloc() implementation only.
   */
  public void dropHeapObject(SGObject pObject) {
    memory.dropHeapObject(pObject);
    memory.removeObjectAndEdges(pObject);
  }

  /**
   * Copy the source data into the target memory region.
   *
   * @param pSource       source memory object
   * @param pTarget       target memory object
   * @param pSourceOffset offset of source memory object
   * @param pSourceSize   size of source memory object
   * @param pTargetOffset offset of target memory object
   * @return new state after copying
   */
  public ShapeState copy(
      SGObject pSource, SGObject pTarget, int pSourceOffset, int pSourceSize,
      int pTargetOffset) {
    ShapeState newState = this;
    boolean isTargetZero = pTarget.isZeroInit();
    boolean isSourceZero = pSource.isZeroInit();

    if (pSource.getSize().isUnknown() || pSource.getSize().isUnknown() || pSourceSize <= 0) {
      return newState;
    }
    int sourceEnds = pSourceOffset + pSourceSize;
    int targetEnds = pTargetOffset + pSourceSize;

    // Note: we do not check the sanity of buffer operation. The check should be done prior to
    // invocation of this method.

    SGHasValueEdgeFilter sourceFilter = SGHasValueEdgeFilter.objectFilter(pSource);
    SGHasValueEdgeFilter targetFilter = SGHasValueEdgeFilter.objectFilter(pTarget);
    Set<SGHasValueEdge> targetEdges = memory.getHVEdges(targetFilter);
    Set<SGHasValueEdge> sourceEdges = memory.getHVEdges(sourceFilter);
    MachineModel model = memory.getMachineModel();

    // remove all has-value edges WITHIN the target region
    // overlapped edges are handled in copying new has-value edges
    for (SGHasValueEdge edge : targetEdges) {
      if (edge.within(pTargetOffset, targetEnds, model)) {
        memory.removeHasValueEdge(edge);
      }
    }

    /*
     * when copying has-value edges to the target region, there are 4 cases:
     *            +--------+----------------+----------------+
     *            |   T\S  |        Z       |      Non-Z     |
     *            +--------+----------------+----------------+
     *            |    Z   |     Direct     |    Non-Z Fill  |
     *            +--------+----------------+----------------+
     *            |  Non-Z |     Z-Fill     |      Direct    |
     *            +--------+----------------+----------------+
     */
    int delta = pTargetOffset - pSourceOffset;
    if (isSourceZero != isTargetZero) {
      // each bit represents a byte
      // a bit is marked when it is tainted by has-value edge in the source object
      BitSet bits = new BitSet();
      for (SGHasValueEdge edge : sourceEdges) {
        bits.set(edge.getOffset(), edge.getOffset() + edge.getSizeInBytes(model));
      }
      int workingPos = bits.nextClearBit(pSourceOffset);
      while (workingPos >= 0 && workingPos < sourceEnds) {
        int clearEnd = bits.nextSetBit(workingPos);
        if (clearEnd < 0 || clearEnd > sourceEnds) {
          // the reminiscent memory region is empty
          if (workingPos < sourceEnds) {
            Long newValue;
            if (isSourceZero) {
              // zero-fill the hole
              newValue = ShapeGraph.getNullAddress();
            } else {
              // fill the hole using a new generated symbolic value
              // TODO: attempt to copy uninitialized data to the target
              newValue = SymbolicValueFactory.getNewValue();
            }
            newState = newState.writeValue(pTarget, workingPos + delta, UnknownTypes
                .createTypeWithLength(sourceEnds - workingPos), newValue).getState();
          }
          break;
        } else {
          // sanity check, which should always hold
          if (workingPos < clearEnd) {
            Long newValue;
            if (isSourceZero) {
              newValue = ShapeGraph.getNullAddress();
            } else {
              // TODO: attempt to copy uninitialized data to the target
              newValue = SymbolicValueFactory.getNewValue();
            }
            newState = newState.writeValue(pTarget, workingPos + delta, UnknownTypes
                .createTypeWithLength(clearEnd - workingPos), newValue).getState();
          }
          workingPos = bits.nextClearBit(clearEnd);
        }
      }
    }
    // copy existing edges
    BitSet nonNullEdgeBytes = null;
    if (!isSourceZero) {
      // source object is not zero-initialized
      nonNullEdgeBytes = new BitSet();
      sourceFilter = sourceFilter.filterNotHavingValue(CShapeGraph.getNullAddress());
      for (SGHasValueEdge edge : memory.getHVEdges(sourceFilter)) {
        int offset = edge.getOffset();
        nonNullEdgeBytes.set(offset, offset + edge.getSizeInBytes(model));
      }
    }
    for (SGHasValueEdge edge : sourceEdges) {
      int myOffset = edge.getOffset();
      int myEnd = myOffset + model.getSizeof(edge.getType());
      boolean isChanged = false;
      if (myOffset < pSourceOffset) {
        myOffset = pSourceOffset;
        isChanged = true;
      }
      if (myEnd > sourceEnds) {
        myEnd = sourceEnds;
        isChanged = true;
      }
      if (myOffset < myEnd) {
        // we can insert a has-value edge here
        int newOffset = myOffset + delta;
        long edgeValue = edge.getValue();
        if (edgeValue == ShapeGraph.getNullAddress() && isTargetZero) {
          // we need to add zero edges for those segments covered by existing non-zero edges
          if (nonNullEdgeBytes != null) {
            // the source is not zero-initialized
            int nextTaint = nonNullEdgeBytes.nextSetBit(myOffset);
            if (!nonNullEdgeBytes.get(myOffset) && (nextTaint >= myEnd || nextTaint < 0)) {
              // current zero edge is not tainted, which means it is unnecessary to add this edge
              // to a zero-initialized object
              continue;
            }
          }
        }
        if (!isChanged) {
          newState = writeValue(pTarget, newOffset, edge.getType(), edgeValue).getState();
        } else {
          // partial of this has-value edge could be inserted
          if (edge.getValue() == ShapeGraph.getNullAddress()) {
            // current object is not zero-initialized
            newState = writeValue(pTarget, newOffset, UnknownTypes.createTypeWithLength(myEnd -
                myOffset), ShapeGraph.getNullAddress()).getState();
          } else {
            Long newValue = SymbolicValueFactory.getNewValue();
            newState = writeValue(pTarget, newOffset, UnknownTypes.createTypeWithLength(myEnd -
                myOffset), newValue).getState();
          }
        }
      }
    }
    return newState;
  }

  /**
   * Prune the unreachable components in the memory graph, which could reveal memory leak bug.
   */
  public void pruneUnreachable() {
    // collect symbolic values used in explicit mapping
    Set<Long> valuesHasExplicit = FluentIterable.from(explicitValues.keySet()).transform(
        new Function<KnownSymbolicValue, Long>() {
          @Override
          public Long apply(KnownSymbolicValue pKnownSymbolicValue) {
            return pKnownSymbolicValue.getAsLong();
          }
        }).toSet();
    if (memory.pruneUnreachable(valuesHasExplicit)) {
      refreshConstraint();
    }
    // After value merge, the explicit mapping could contain x->n where x is not the minimum
    // element in [v]. Therefore we perform regularization on explicit mapping here.
    Map<Long, Long> changeMap = new HashMap<>();
    for (Entry<KnownExplicitValue, KnownSymbolicValue> entry : explicitValues.inverse()
        .entrySet()) {
      Long symbolic = entry.getValue().getAsLong();
      Long rep = memory.getRepresentative(symbolic);
      if (!rep.equals(symbolic)) {
        changeMap.put(symbolic, rep);
      }
    }
    for (Entry<Long, Long> changeEntry : changeMap.entrySet()) {
      KnownSymbolicValue key = KnownSymbolicValue.valueOf(changeEntry.getKey());
      KnownExplicitValue value = explicitValues.get(key);
      KnownSymbolicValue newKey = KnownSymbolicValue.valueOf(changeEntry.getValue());
      // If the explicit value is previously associated with another key, then this operation
      // will silently remove the existing association.
      explicitValues.forcePut(newKey, value);
    }
    if (memory.hasMemoryLeak()) {
      memoryLeak = true;
      leakCFAEdges.addAll(memory.getLeakEdges());
    }
  }

  /**
   * Update explicit value mapping while returning the merged symbolic value and adding binding
   * relation in the constraint pool.
   *
   * @param pKey   symbolic value
   * @param pValue explicit value (with numerical semantics)
   * @return the new symbolic value after merging
   */
  public KnownSymbolicValue updateExplicitValue(
      KnownSymbolicValue pKey,
      KnownExplicitValue pValue) {
    // add key symbolic value to the memory graph first, for this value will be finally added
    // into the shape state
    long keyAsLong = pKey.getAsLong();
    if (!memory.getValues().contains(keyAsLong)) {
      memory.addValue(keyAsLong);
    }
    if (explicitValues.containsValue(pValue)) {
      KnownSymbolicValue symbolicValue = explicitValues.inverse().get(pValue);
      long oldKey = symbolicValue.getAsLong();
      memory.mergeValues(oldKey, keyAsLong);
      // we should maintain that the key symbolic value should have the lowest identifier in its
      // equivalence class
      // Based on the fact that `symbolicValue` is the lowest identifier of its equivalence class
      // before merging, the explicit value mapping is touched only if new representative is
      // lower than `symbolicValue`.
      if (oldKey != keyAsLong) {
        // create a new binding between the new value and the explicit
        memory.putExplicitValue(pKey, pValue);
      }
      if (oldKey > keyAsLong) {
        explicitValues.remove(symbolicValue);
        putExplicitMapping(pKey, pValue);
        return pKey;
      } else {
        return symbolicValue;
      }
    } else {
      // this explicit value is associated with a symbolic value for the first time
      long v = memory.getRepresentative(keyAsLong);
      if (v == keyAsLong) {
        putExplicitMapping(pKey, pValue);
        memory.putExplicitValue(pKey, pValue);
        return pKey;
      } else {
        KnownSymbolicValue represent = KnownSymbolicValue.valueOf(v);
        putExplicitMapping(represent, pValue);
        memory.putExplicitValue(represent, pValue);
        return represent;
      }
    }
  }

  /**
   * Update explicit value mapping without returning the updated symbolic value.
   */
  @Nullable
  void putExplicitValue(KnownSymbolicValue pKey, KnownExplicitValue pValue) {
    long keyAsLong = pKey.getAsLong();
    if (!memory.getValues().contains(keyAsLong)) {
      memory.addValue(keyAsLong);
    }
    if (explicitValues.containsValue(pValue)) {
      KnownSymbolicValue symbolicValue = explicitValues.inverse().get(pValue);
      long oldKey = symbolicValue.getAsLong();
      memory.mergeValues(oldKey, keyAsLong);
      if (oldKey > keyAsLong) {
        explicitValues.remove(symbolicValue);
        putExplicitMapping(pKey, pValue);
      }
    } else {
      long v = memory.getRepresentative(keyAsLong);
      if (v == keyAsLong) {
        putExplicitMapping(pKey, pValue);
      } else {
        KnownSymbolicValue represent = KnownSymbolicValue.valueOf(v);
        putExplicitMapping(represent, pValue);
      }
    }
  }

  void identifyEqualValues(KnownSymbolicValue pValue1, KnownSymbolicValue pValue2) {
    long val1 = pValue1.getAsLong();
    long val2 = pValue2.getAsLong();
    // first, we attempt to merge two values
    // it is unnecessary to generate an equality constraint because this method is used in
    // addressing assumption
    memory.mergeValues(val1, val2);
    long v = memory.getRepresentative(val1);
    KnownExplicitValue removedValue = null;
    if (v != val1) {
      removedValue = explicitValues.remove(pValue1);
      if (removedValue == null && v != val2) {
        removedValue = explicitValues.remove(pValue2);
      }
    }
    if (removedValue != null) {
      putExplicitMapping(KnownSymbolicValue.valueOf(v), removedValue);
    }
  }

  void identifyInequalValues(KnownSymbolicValue pValue1, KnownSymbolicValue pValue2) {
    memory.addNeqRelation(pValue1.getAsLong(), pValue2.getAsLong());
  }

  /**
   * Precondition: the specified function does not have its function pointer yet.
   */
  public SGObject createFunctionObject(CFunctionDeclaration pDeclaration) {
    String functionName = pDeclaration.getQualifiedName();
    // however, space for function object cannot be statically determined in general
    return addGlobalVariable(pDeclaration.getType(), functionName);
  }

  private void putExplicitMapping(KnownSymbolicValue ksv, KnownExplicitValue kev) {
    explicitValues.put(ksv, kev);
    memory.dropAbstraction(ksv);
  }

  /* *********************** */
  /* non-modifying functions */
  /* *********************** */

  public final long getId() {
    return id;
  }

  public final SGObject getFunctionReturnObject() {
    return memory.getFunctionReturnObject();
  }

  @Nullable
  public final SGObject getObjectForVisibleVariable(String pName) {
    return memory.getObjectForVisibleVariable(pName);
  }

  public boolean containsValue(long pValue) {
    return memory.getValues().contains(pValue);
  }

  @Override
  public String toString() {
    if (preId != 0) {
      return "ShapeState [" + id + "] <-- parent [" + preId + "]\n" + memory.toString();
    } else {
      return "ShapeState [" + id + "] <-- initial state\n" + memory.toString();
    }
  }

  public BiMap<KnownSymbolicValue, KnownExplicitValue> getExplicitValues() {
    // Note: we should return an immutable data structure
    Builder<KnownSymbolicValue, KnownExplicitValue> builder = ImmutableBiMap.builder();
    return builder.putAll(explicitValues).build();

  }

  public CShapeGraph getShapeGraph() {
    return memory;
  }

  /**
   * Read (symbolic) value from an object.
   *
   * @param pObject memory object the target value belongs to
   * @param pOffset offset of memory being read
   * @param pType   how to interpret the memory region
   * @param exp     expression of certain memory region (for error reporting purpose)
   * @return the symbolic value along with state (however state keeps unchanged in reading value)
   */
  public SymbolicValueAndStateList readValue(
      SGObject pObject,
      int pOffset,
      CType pType,
      CExpression exp) {
    if (!memory.isObjectValid(pObject)) {
      // then we have invalid read error
      ShapeState newState = setInvalidRead(exp);
      return SymbolicValueAndStateList.of(newState);
    }

    SGHasValueEdgeFilter filter =
        SGHasValueEdgeFilter.objectFilter(pObject).filterAtOffset(pOffset);
    Set<SGHasValueEdge> hvEdges = memory.getHVEdges(filter);

    SGHasValueEdge edge0 = new SGHasValueEdge(pType, pOffset, pObject, 0);
    for (SGHasValueEdge hvEdge : hvEdges) {
      // if there are an existing field precisely matches the desired memory region, then we
      // directly return the value of this field.
      if (edge0.isCompatibleFieldOnTheSameObject(hvEdge, memory.getMachineModel())) {
        ShapeSymbolicValue value = KnownSymbolicValue.valueOf(hvEdge.getValue());
        if (value.isUnknown()) {
          return SymbolicValueAndStateList.of(this);
        }
        // If the abstraction is empty, we do not perform value interpretation and feasibility
        // check in order to improve performance, especially for separated merge.
        if (memory.isAbstractionEmpty()) {
          return SymbolicValueAndStateList.of(this, value);
        }
        Long symValue = value.getAsLong();
        Collection<GuardedValue> interpretation = memory.interpret(symValue);
        if (interpretation.isEmpty()) {
          // How could this case occur?
          return SymbolicValueAndStateList.of(this);
        }
        List<SymbolicValueAndState> results = new ArrayList<>(interpretation.size());
        for (GuardedValue gv : interpretation) {
          ShapeState newState = new ShapeState(this);
          newState.addConstraints(gv.getGuard());
          newState.addDisjunction(gv.getDisjunction());
          try {
            boolean sat = checkSatForRefinement();
            if (sat) {
              KnownSymbolicValue newValue = KnownSymbolicValue.valueOf(gv.getValue());
              results.add(SymbolicValueAndState.of(newState, newValue));
            }
          } catch (InterruptedException | UnrecognizedCCodeException | SolverException pE) {
            // by default, we do not add the derived concrete value for efficiency reason
            logger.log(Level.SEVERE, "Failed to perform SMT solving on branch conditions");
          }
        }
        // if the number of interpreted values exceeds the specified threshold, we randomly
        // choose some of them as the result
        int maxValues = CoreShapeAdapter.getInstance().getMaxInterpretations();
        if (maxValues > 0 && results.size() > maxValues) {
          List<SymbolicValueAndState> chosenResults = new ArrayList<>();
          Random random = new Random();
          for (int i = 0; i < maxValues; i++) {
            int index = random.nextInt(results.size());
            chosenResults.add(results.get(index));
            results.remove(index);
          }
          return SymbolicValueAndStateList.copyOfValueList(chosenResults);
        } else {
          return SymbolicValueAndStateList.copyOfValueList(results);
        }
      }
    }
    // check whether the specified memory region has all-zero bits
    if ((!pObject.isZeroInit() && memory.isCoveredByNullifiedBlocks(edge0)) ||
        (pObject.isZeroInit() && !memory.isTaintedByNonNullBlocks(edge0))) {
      return SymbolicValueAndStateList.of(this, KnownSymbolicValue.ZERO);
    }
    // otherwise, maybe we arbitrarily point to the memory region which contains incomplete field
    // value
    return SymbolicValueAndStateList.of(this);
  }

  /**
   * Extract all has-value edges starting within the specified range and sort them by their offset.
   *
   * @param pObject memory object
   * @param start   lower bound of specified range
   * @param end     upper bound of specified range
   * @return a map from start offset to has-value edge
   */
  public Map<Integer, SGHasValueEdge> getHasValueEdgesInRange(
      SGObject pObject, int start, int
      end) {
    if (!memory.isObjectValid(pObject)) {
      return Maps.newHashMap();
    }
    Preconditions.checkArgument(start <= end);
    SGHasValueEdgeFilter filter = SGHasValueEdgeFilter.objectFilter(pObject).filterByRange(start,
        end);
    Set<SGHasValueEdge> hvEdges = memory.getHVEdges(filter);
    Map<Integer, SGHasValueEdge> edgeMap = new HashMap<>();
    for (SGHasValueEdge hvEdge : hvEdges) {
      int offset = hvEdge.getOffset();
      edgeMap.put(offset, hvEdge);
    }
    return edgeMap;
  }

  @Nullable
  public SGObject getHeapObject(String objectName) {
    return memory.getHeapObject(objectName);
  }

  /**
   * Get the address of specified memory with offset. The address is a known symbolic value.
   */
  @Nullable
  public Long getAddress(final SGObject pObject, final int offset) {
    Map<Long, SGPointToEdge> ptEdges = memory.getPTEdges();
    // I believe it is more efficient to use fluent iterable structure
    Set<SGPointToEdge> matches = FluentIterable.from(ptEdges.values()).filter(
        new Predicate<SGPointToEdge>() {
          @Override
          public boolean apply(SGPointToEdge pSGPointToEdge) {
            return (pSGPointToEdge.getObject() == pObject) && (pSGPointToEdge.getOffset() ==
                offset);
          }
        }).toSet();
    if (matches.isEmpty()) {
      return null;
    }
    return matches.iterator().next().getValue();
  }

  /**
   * Obtain the point-to information with respect to the given value
   *
   * @param pValue a value, should be a pointer value
   * @return address value
   */
  public AddressValueAndState getPointToForAddress(Long pValue) {
    if (memory.isPointer(pValue)) {
      SGPointToEdge pointTo = memory.getPointer(pValue);
      ShapeAddressValue address = KnownAddressValue.valueOf(pointTo.getObject(), pointTo
          .getOffset(), pointTo.getValue());
      return AddressValueAndState.of(this, address);
    }
    // otherwise, we ask for a non-pointer value
    return AddressValueAndState.of(this);
  }

  /**
   * Obtain the point-to information according to the given value without returning the state.
   */
  public ShapeAddressValue getPointToForAddressValue(Long pValue) {
    if (memory.isPointer(pValue)) {
      SGPointToEdge pointTo = memory.getPointer(pValue);
      return KnownAddressValue.valueOf(pointTo.getObject(), pointTo.getOffset(), pointTo.getValue
          ());
    }
    return UnknownValue.getInstance();
  }

  /**
   * Obtain the point-to edge given the symbolic value.
   *
   * @param pValue a possibly address value
   * @return the point-to edge
   */
  public SGPointToEdge getPointer(Long pValue) {
    return memory.getPointer(pValue);
  }

  @Nullable
  public StackObjectInfo getStackObjectInfo(SGObject pObject) {
    return memory.getStackObjectInfo(pObject);
  }

  public SGObject getNullObject() {
    return CShapeGraph.getNullObject();
  }

  public ShapeExplicitValue getExplicit(KnownSymbolicValue pKey) {
    if (explicitValues.containsKey(pKey)) {
      return explicitValues.get(pKey);
    }
    return UnknownValue.getInstance();
  }

  public ShapeSymbolicValue getSymbolic(KnownExplicitValue pValue) {
    if (explicitValues.containsValue(pValue)) {
      return explicitValues.inverse().get(pValue);
    }
    return UnknownValue.getInstance();
  }

  public boolean isNeq(ShapeSymbolicValue pValue1, ShapeSymbolicValue pValue2) {
    if (pValue1.isUnknown() || pValue2.isUnknown()) {
      return false;
    } else {
      return memory.isNeq(pValue1.getAsLong(), pValue2.getAsLong());
    }
  }

  /**
   * If the given symbolic value is an address value.
   *
   * @param pValue a symbolic value as integer
   * @return whether it is an address value
   */
  public boolean isAddress(Long pValue) {
    return memory.isPointer(pValue);
  }

  public SGObject getFunctionObject(CFunctionDeclaration pDeclaration) {
    String functionName = pDeclaration.getQualifiedName();
    return memory.getObjectForVisibleVariable(functionName);
  }

  public Set<SGHasValueEdge> getHasValueEdgesFor(SGHasValueEdgeFilter pFilter) {
    return memory.getHVEdges(pFilter);
  }

  /**
   * Null bytes are set in bit-vector.
   * Precondition: the input object should NOT be zero-initialized.
   */
  public BitSet getNullBytesFor(SGObject pObject) {
    assert (!pObject.isZeroInit());
    return memory.getNullBytesFor(pObject);
  }

  /**
   * Non-null bytes are set in bit-vector.
   * Precondition: the input object should be zero-initialized.
   */
  public BitSet getNonNullBytesFor(SGObject pObject) {
    assert (pObject.isZeroInit());
    return memory.getNonNullBytesFor(pObject);
  }

  @Nullable
  public SymbolicExpression getSizeForObject(SGObject pObject) {
    return memory.getSizeForObject(pObject);
  }

  /* ************************ */
  /* error handling functions */
  /* ************************ */

  private enum MemoryErrorProperty {
    INVALID_READ,
    INVALID_WRITE,
    INVALID_FREE
  }

  public ShapeState setInvalidRead(CExpression exp) {
    ShapeState newState = new ShapeState(this, MemoryErrorProperty.INVALID_READ);
    newState.setInvalidReadExpression(exp);
    return newState;
  }

  private void setInvalidReadExpression(CExpression exp) {
    invalidReadExpression.add(exp);
  }

  public ShapeState setInvalidWrite() {
    return new ShapeState(this, MemoryErrorProperty.INVALID_WRITE);
  }

  public ShapeState setInvalidFree() {
    return new ShapeState(this, MemoryErrorProperty.INVALID_FREE);
  }

  public boolean getInvalidReadStatus() {
    return invalidRead;
  }

  public Set<CExpression> getInvalidReadExpression() {
    return invalidReadExpression;
  }

  public boolean getInvalidWriteStatus() {
    return invalidWrite;
  }

  public boolean getInvalidFreeStatus() {
    return invalidFree;
  }

  public boolean getMemoryLeakStatus() {
    return memoryLeak;
  }

  public Set<CFAEdge> getMemoryLeakCFAEdges() {
    return leakCFAEdges;
  }

  void resetMemoryLeakStatus() {
    memoryLeak = false;
    memory.resetMemoryLeak();
  }

  void setStackAddressReturn() {
    stackAddressReturn = true;
  }

  public boolean getStackAddressReturn() {
    return stackAddressReturn;
  }

  void resetStackAddressReturn() {
    stackAddressReturn = false;
  }

  /* ******************** */
  /* strengthen functions */
  /* ******************** */

  /**
   * Other CPAs can query actual access path by invoking this method.
   *
   * @param path an access path possibly contains dereference operation
   * @return an actual access path
   */
  public Set<AccessPath> getPointsToTargetForAccessPath(AccessPath path) {
    if (path == null) {
      return Collections.emptySet();
    }
    // prune access path to reduce segments corresponding to actual memory access
    AccessPath newPath = AccessPath.copyOf(path);
    List<PathSegment> segments = path.path();
    List<CType> types = path.parseTypeList();
    assert (segments.size() == types.size());
    int cutPoint = segments.size() - 1;
    while (cutPoint >= 1) {
      PathSegment segment = segments.get(cutPoint);
      CType type = types.get(cutPoint - 1);
      if (segment instanceof PointerDereferenceSegment) {
        // we have to stop, since it could only be parsed by pointer analysis
        break;
      } else if (segment instanceof ArrayConstIndexSegment || segment instanceof
          ArrayUncertainIndexSegment) {
        // an array subscript can be a pointer dereference, depending on the type of array
        // expression
        if (Types.extractPointerType(type) != null) {
          break;
        }
      }
      // remove the last segment of new access path
      newPath.removeLastSegment();
      cutPoint--;
    }
    boolean lastTruncatedIsArray = false;
    if (cutPoint + 1 < segments.size()) {
      PathSegment lastTruncated = segments.get(cutPoint + 1);
      if (lastTruncated instanceof ArrayConstIndexSegment || lastTruncated instanceof
          ArrayUncertainIndexSegment) {
        newPath.appendSegment(new ArrayConstIndexSegment(0));
        lastTruncatedIsArray = true;
      }
    }
    // sanity checks
    Set<AccessPath> resultPaths;
    if (newPath.isActualPath()) {
      resultPaths = Collections.singleton(newPath);
    } else if (!newPath.supportMemoryLocationRepresentation()) {
      resultPaths = Collections.emptySet();
    } else {
      resultPaths = memory.getPointToAccessPath(newPath);
    }
    // post-processing
    if (resultPaths.isEmpty()) {
      return Collections.emptySet();
    } else {
      Set<AccessPath> results = new HashSet<>();
      for (AccessPath singlePath : resultPaths) {
        if (cutPoint + 1 >= segments.size()) {
          // no previous truncation
          results.add(singlePath);
          continue;
        }
        PathSegment lastSegment = singlePath.getLastSegment();
        PathSegment lastTruncated = segments.get(cutPoint + 1);
        if (lastSegment instanceof ArrayConstIndexSegment && lastTruncatedIsArray) {
          PathSegment merged = mergeArraySegment(lastSegment, lastTruncated);
          singlePath.removeLastSegment();
          singlePath.appendSegment(merged);
        } else {
          singlePath.appendSegment(lastTruncated);
        }
        for (int i = cutPoint + 2; i < segments.size(); i++) {
          singlePath.appendSegment(segments.get(i));
        }
        results.add(singlePath);
      }
      return results;
    }
  }

  /**
   * merge array segments
   */
  private PathSegment mergeArraySegment(PathSegment pSeg1, PathSegment pSeg2) {
    assert (pSeg1 instanceof ArrayConstIndexSegment);
    if (pSeg2 instanceof ArrayConstIndexSegment) {
      long index = ((ArrayConstIndexSegment) pSeg1).getIndex() + ((ArrayConstIndexSegment) pSeg2)
          .getIndex();
      return new ArrayConstIndexSegment(index);
    } else if (pSeg2 instanceof ArrayUncertainIndexSegment) {
      Range indexRange = ((ArrayUncertainIndexSegment) pSeg2).getIndexRange();
      indexRange = indexRange.plus(((ArrayConstIndexSegment) pSeg1).getIndex());
      return new ArrayUncertainIndexSegment(indexRange);
    } else {
      throw new UnsupportedOperationException("undefined path segment merge operation");
    }
  }

  /* ****************** */
  /* override functions */
  /* ****************** */

  @Override
  public ShapeState join(ShapeState other) {
    return null;
  }

  @Override
  public boolean isLessOrEqual(ShapeState reached) throws CPAException, InterruptedException {
    return ShapeGeneralLessOrEqual.getInstance().getLessOrEqual().isLessOrEqual(this, reached);
  }

  /* ********************* */
  /* visualization methods */
  /* ********************* */

  @Override
  public boolean getActiveStatus() {
    return true;
  }

  @Override
  public String toDOTLabel() {
    return "Shape ID: " + String.valueOf(id);
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  /* ********************* */
  /* constraint processing */
  /* ********************* */

  public void initialize(
      Solver pSolver, FormulaManagerView pFormulaManager, FormulaCreator
      pFormulaCreator) {
    // prover should be created and closed on demand
    solver = pSolver;
    formulaManager = pFormulaManager;
    formulaCreator = pFormulaCreator;
  }

  boolean hasInitialized() {
    return (solver != null && formulaManager != null && formulaCreator != null);
  }

  /**
   * Add an constraint on the constraint container.
   */
  public void addConstraint(SymbolicExpression constraint) {
    memory.addConstraint(constraint);
  }

  private void addConstraints(Collection<SymbolicExpression> constraints) {
    memory.addConstraints(constraints);
  }

  private void addDisjunction(Set<ConstraintRepresentation> constraints) {
    memory.addDisjunction(constraints);
  }

  /**
   * Check the satisfiability of current state.
   * If constraints are unsatisfiable, current path should be pruned instantly.
   *
   * @return TRUE for satisfiable and FALSE otherwise.
   */
  boolean checkSat() throws SolverException, InterruptedException,
                            UnrecognizedCCodeException {
    // by default, the result is UNSAT, which is aggressive to cover some hard cases
    boolean sat = true;
    // There is no need to generate constraint for derived inequalities, because they are derived
    // directly from binary comparisons. Only constraints in the pool have generated formulae.

    // ** IMPLEMENTATION ISSUE: as the path grows, the prefix of path can be cached to save the
    // computation resource
    try {
      if (memory.getSizeOfConstraints() > 0) {
        prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
        BooleanFormula totalFormula = getFullFormula();
        prover.push(totalFormula);
        sat = !prover.isUnsat();
        if (sat) {
          resolveDefiniteAssignment();
        }
      }
    } catch (Exception e) {
      // TODO: investigate whether this case can be reached
      sat = false;
    } finally {
      closeProver();
    }
    return sat;
  }

  /**
   * Check the satisfiability of assertions inside the specified solver along with the additional
   * assumptions.
   *
   * @param assumptions additional assumptions (constraints)
   * @return TRUE for satisfiable and FALSE for unsatisfiable.
   */
  public boolean checkSatWithAssumptions(List<ConstraintRepresentation> assumptions)
      throws SolverException, InterruptedException, UnrecognizedCCodeException {
    boolean sat;
    try {
      prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
      if (memory.getSizeOfConstraints() > 0) {
        BooleanFormula totalFormula = getFullFormula();
        prover.push(totalFormula);
      }
      // generate additional assumptions, which are safe conditions for specific defects
      for (ConstraintRepresentation assumption : assumptions) {
        if (assumption instanceof ConstantSE) {
          ShapeValue value = ((ConstantSE) assumption).getValue();
          if (value.equals(KnownSymbolicValue.ZERO) || value.equals(KnownExplicitValue.ZERO)) {
            return false;
          }
          // If the assumption is a constant, including TRUE or some other constant values, we
          // should not feed it to solver.
          continue;
        }
        BooleanFormula formula = formulaCreator.createFormula(assumption);
        if (formula != null) {
          prover.push(formula);
        }
      }
      // if the additional constraints are empty, we simply return SAT
      sat = !prover.isUnsat();
    } catch (Exception e) {
      // TODO: investigate whether this case can be reached
      sat = false;
    } finally {
      closeProver();
    }
    return sat;
  }

  private boolean checkSatForRefinement() throws SolverException, InterruptedException,
                                                 UnrecognizedCCodeException {
    boolean sat;
    try {
      prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
      BooleanFormula branchFormula = getFullFormula();
      prover.push(branchFormula);
      sat = !prover.isUnsat();
    } catch (Exception e) {
      // TODO: investigate whether this case can be reached
      sat = false;
    } finally {
      closeProver();
    }
    return sat;
  }

  /**
   * Prover environment is a kind of computation resource, which should be released on time for
   * preventing memory issues.
   */
  private void closeProver() {
    if (prover != null) {
      prover.close();
      prover = null;
    }
  }

  /**
   * Refresh the constraint pool.
   */
  private void refreshConstraint() {
    numOfProcessedConstraints = 0;
    disjunctionProcessed = false;
    formulae.clear();
  }

  /**
   * Derive the formula for constraint solving from symbolic expressions.
   * Note: since the prefix of path is frequently reused, they are carefully cached.
   */
  private BooleanFormula getFullFormula() throws UnrecognizedCCodeException, InterruptedException {
    createMissingFormulae();
    return formulaManager.getBooleanFormulaManager().and(formulae);
  }

  /**
   * Derive the total boolean formula including path conditions and equalities/inequalities.
   * This method is called only in less_or_equal operator working under IMPLICATION mode.
   */
  public BooleanFormula getTotalFormula() throws UnrecognizedCCodeException, InterruptedException {
    BooleanFormula path = getFullFormula();
    BooleanFormula eq = getEqFormula();
    BooleanFormula neq = getNeqFormula();
    return formulaManager.getBooleanFormulaManager().and(Lists.newArrayList(path, eq, neq));
  }

  /**
   * Generate boolean formula for equality relations.
   */
  private BooleanFormula getEqFormula() throws UnrecognizedCCodeException, InterruptedException {
    EquivalenceRelation<Long> eq = memory.getEq();
    Collection<Long> reps = eq.getRepresentatives();
    List<BooleanFormula> totalEqs = new ArrayList<>();
    for (Long rep : reps) {
      CType type = memory.getTypeForValue(rep);
      if (type == null) {
        // in general, this should not happen
        continue;
      }
      Set<Long> values = eq.getEquivalentValues(rep);
      List<ConstantSE> constants = new ArrayList<>(values.size());
      for (Long value : values) {
        constants.add(new ConstantSE(KnownSymbolicValue.valueOf(value), type, CIdExpression
            .DUMMY_ID(type)));
      }
      List<BooleanFormula> eqs = new ArrayList<>(constants.size() - 1);
      for (int i = 0; i < constants.size() - 1; i++) {
        BinarySE eqSe = new BinarySE(constants.get(i), constants.get(i + 1), BinaryOperator.EQUALS,
            CNumericTypes.INT, CIdExpression.DUMMY_ID(CNumericTypes.INT));
        eqs.add(formulaCreator.createFormula(eqSe));
      }
      totalEqs.addAll(eqs);
    }
    return formulaManager.getBooleanFormulaManager().and(totalEqs);
  }

  /**
   * Generate boolean formula for inequality relations.
   * For each inequality pair (x,y), if x and y have incompatible types, we do not generate
   * boolean formula for it.
   */
  private BooleanFormula getNeqFormula() throws UnrecognizedCCodeException, InterruptedException {
    Multimap<Long, Long> neq = ShapeComplexLessOrEqual.getHalf(memory.getNeq());
    List<BooleanFormula> totalNeqs = new ArrayList<>();
    Map<Long, ConstantSE> seMap = new TreeMap<>();
    for (Entry<Long, Long> entry : neq.entries()) {
      Long v1 = entry.getKey();
      Long v2 = entry.getValue();
      ConstantSE se1, se2;
      if (seMap.containsKey(v1)) {
        se1 = seMap.get(v1);
        if (se1 == null) {
          // that means, we cannot derive the type of symbolic value v1
          continue;
        }
      } else {
        CType t = memory.getTypeForValue(v1);
        if (t == null) {
          seMap.put(v1, null);
          continue;
        }
        se1 = new ConstantSE(KnownSymbolicValue.valueOf(v1), t, CIdExpression.DUMMY_ID(t));
        seMap.put(v1, se1);
      }
      if (seMap.containsKey(v2)) {
        se2 = seMap.get(v2);
        if (se2 == null) {
          continue;
        }
      } else {
        CType t = memory.getTypeForValue(v2);
        if (t == null) {
          seMap.put(v2, null);
          continue;
        }
        se2 = new ConstantSE(KnownSymbolicValue.valueOf(v2), t, CIdExpression.DUMMY_ID(t));
        seMap.put(v2, se2);
      }
      // if we reach here, then v1 and v2 correspond to their own symbolic expressions
      if (CoreShapeAdapter.isCompatible(se1.getType(), se2.getType())) {
        totalNeqs.add(formulaCreator.createFormula(new BinarySE(se1, se2, BinaryOperator
            .NOT_EQUALS, CNumericTypes.INT, CIdExpression.DUMMY_ID(CNumericTypes.INT))));
      }
    }
    return formulaManager.getBooleanFormulaManager().and(totalNeqs);
  }

  /**
   * Construct formula cache.
   */
  private void createMissingFormulae() throws UnrecognizedCCodeException, InterruptedException {
    int moreSize = memory.getSizeOfConstraints();
    int lessSize = numOfProcessedConstraints;
    int delta = moreSize - lessSize;
    assert delta >= 0;
    for (int i = moreSize - delta; i < moreSize; i++) {
      numOfProcessedConstraints++;
      SymbolicExpression expression = memory.getConstraintOn(i);
      BooleanFormula newFormula = formulaCreator.createFormula(expression);
      // derived formula could be null, when the condition expression contains unknown values
      if (newFormula != null) {
        formulae.add(newFormula);
      }
    }
    assert moreSize == numOfProcessedConstraints;
    // check whether disjunction has been processed
    if (!disjunctionProcessed) {
      disjunctionProcessed = true;
      ConstraintRepresentation disjunction = memory.getDisjunction();
      if (disjunction != null) {
        BooleanFormula newFormula = formulaCreator.createFormula(disjunction);
        if (newFormula != null) {
          formulae.add(newFormula);
        }
      }
    }
  }

  /**
   * Resolve if some formula variables have determined value. If so, we specify them with
   * explicit value and further simplify the constraint state.
   */
  private void resolveDefiniteAssignment() throws SolverException, InterruptedException,
                                                  UnrecognizedCCodeException {
    // only symbolic values in unit constraints are checked
    Map<KnownSymbolicValue, KnownExplicitValue> newExplicits = new HashMap<>();
    Multimap<Long, Integer> unitSymbols = memory.getNeedCheckingDefiniteAssignmentSet();
    Model model = prover.getModel();
    for (ValueAssignment assign : model) {
      Formula key = assign.getKey();
      // check if the key of assignment corresponds to a unit constraint
      if (ConstantSE.isSymbolicTerm(key.toString())) {
        KnownSymbolicValue symbol = ConstantSE.toSymbolicValue(assign.getName());
        if (unitSymbols.containsKey(symbol.getAsLong())) {
          Object value = assign.getValue();
          KnownExplicitValue expValue = convertToExplicit(value);
          if (expValue != null) {
            if (isDefiniteAssignment(assign)) {
              newExplicits.put(symbol, expValue);
            }
          }
        }
      }
    }
    // reduce constraints by replacing symbolic values with derived explicit ones
    for (Entry<KnownSymbolicValue, KnownExplicitValue> entry : newExplicits.entrySet()) {
      putExplicitValue(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Convert value assignment in the model to explicit value.
   */
  @Nullable
  private KnownExplicitValue convertToExplicit(Object pObject) {
    if (pObject instanceof Number) {
      return KnownExplicitValue.of((Number) pObject);
    } else if (pObject instanceof Boolean) {
      // C does not natively support boolean value, thus we use 0 and 1 to denote FALSE and TRUE,
      // respectively
      boolean bValue = (boolean) pObject;
      if (bValue) {
        return KnownExplicitValue.ONE;
      } else {
        return KnownExplicitValue.ZERO;
      }
    } else {
      // other cases are not supported
      return null;
    }
  }

  /**
   * Check if the value assignment in the model is definite.
   *
   * @param pAssignment value assignment
   * @return TRUE if the assignment is definite, FALSE otherwise.
   */
  private boolean isDefiniteAssignment(ValueAssignment pAssignment) throws SolverException,
                                                                           InterruptedException {
    BooleanFormula oppositeFormula = formulaManager.makeNot(formulaCreator
        .createFormulaFromAssignment(pAssignment));
    prover.push(oppositeFormula);
    boolean unsat = prover.isUnsat();
    prover.pop();
    // if the prover returns UNSAT, that means the corresponding key term should have only one
    // explicit value, which implies an explicit value association
    return unsat;
  }

  /* *********** */
  /* state merge */
  /* *********** */

  /* we put the join-merger here because it is unnecessary to write so many getters */

  public static class ShapeStateJoinOperator implements MergeOperator {

    private static final ShapeStateJoinOperator joiner = new ShapeStateJoinOperator();

    public static ShapeStateJoinOperator getInstance() {
      return joiner;
    }

    @Override
    public AbstractState merge(
        AbstractState state1, AbstractState state2, Precision precision)
        throws CPAException, InterruptedException {
      ShapeState s1 = (ShapeState) state1;
      ShapeState s2 = (ShapeState) state2;
      return join(s1, s2);
    }

    /**
     * Join-merge two shape states.
     *
     * @param pS1 the successor state
     * @param pS2 the reached state
     * @return the joined state
     */
    private ShapeState join(ShapeState pS1, ShapeState pS2) {

      if (pS1.equals(pS2)) {
        return pS2;
      }

      // STEP 1: merging flags of blocking errors. If they are inconsistent, we should not merge
      // two states
      boolean newInvalidRead, newInvalidWrite, newInvalidFree;
      if (pS1.invalidRead != pS2.invalidRead) {
        return pS2;
      }
      newInvalidRead = pS2.invalidRead;
      if (pS1.invalidWrite != pS2.invalidWrite) {
        return pS2;
      }
      newInvalidWrite = pS2.invalidWrite;
      if (pS1.invalidFree != pS2.invalidFree) {
        return pS2;
      }
      newInvalidFree = pS2.invalidFree;

      // STEP 2: merge associations between symbolic value and explicit value
      CShapeGraph graph1 = pS1.memory;
      CShapeGraph graph2 = pS2.memory;
      MergeTable table = new MergeTable(graph1.getEq(), graph2.getEq());

      BiMap<KnownExplicitValue, KnownSymbolicValue> exp2Sym1 = pS1.getExplicitValues().inverse();
      BiMap<KnownExplicitValue, KnownSymbolicValue> exp2Sym2 = pS2.getExplicitValues().inverse();
      BiMap<KnownSymbolicValue, KnownExplicitValue> newExplicits = HashBiMap.create();
      for (Entry<KnownExplicitValue, KnownSymbolicValue> entry1 : exp2Sym1.entrySet()) {
        KnownExplicitValue expValue = entry1.getKey();
        KnownSymbolicValue symValue2 = exp2Sym2.get(expValue);
        if (symValue2 == null) {
          continue;
        }
        KnownSymbolicValue symValue1 = entry1.getValue();
        Long newValue = table.merge(symValue1.getAsLong(), symValue2.getAsLong());
        if (newValue == null) {
          table.putExplicitEquality(symValue1.getAsLong(), symValue2.getAsLong(), expValue);
          continue;
        }
        newExplicits.forcePut(KnownSymbolicValue.valueOf(newValue), expValue);
      }

      // STEP 3: merge memory graphs
      // Isolation mechanism is employed, thus pS1 and pS2 keep unchanged.

      CShapeGraphJoin graphJoin = new CShapeGraphJoin(graph1, graph2, table);
      if (!graphJoin.isDefined()) {
        return pS2;
      }
      CShapeGraph mergedGraph = graphJoin.getMerged();
      BiMap<Long, KnownExplicitValue> derivedExplicits = table.getNewExplicitRelation();
      for (Entry<Long, KnownExplicitValue> entry : derivedExplicits.entrySet()) {
        Long key = entry.getKey();
        KnownExplicitValue value = entry.getValue();
        newExplicits.forcePut(KnownSymbolicValue.valueOf(key), value);
      }

      // STEP 3: merge error flags
      boolean newStackReturn = pS1.stackAddressReturn || pS2.stackAddressReturn;
      Set<CExpression> invalidReads = Sets.union(pS1.invalidReadExpression,
          pS2.invalidReadExpression);

      // constraint processing is handled in transfer relation on-demand
      // Note: for a merged state, its number of processed constraints is 0, which means transfer
      // relation should completely rebuild the boolean formulae.
      // create the new shape state
      return new ShapeState(pS2.logger, mergedGraph, newExplicits, pS1.counter,
          pS2.preId, newInvalidRead, newInvalidWrite, newInvalidFree, newStackReturn, invalidReads,
          pS2.solver, pS2.formulaManager, pS2.formulaCreator);

      // that's all forks
    }

  }

  /* ************** */
  /* communications */
  /* ************** */

  public SGObject getObjectForMemoryLocation(MemoryLocation pLocation) {
    return memory.getObjectFromMemoryLocation(pLocation);
  }

  @Nullable
  public MemoryPoint getMemoryPointForAccessPath(AccessPath pPath) {
    if (pPath == null) {
      return null;
    }
    if (!pPath.isCanonicalAccessPath()) {
      // undetermined segment is not allowed
      return null;
    }
    return memory.fromAccessPathToMemoryPoint(pPath);
  }

  @Nullable
  public MemoryPoint getMemoryPointForAccessPath(String pDeclaredName, List<PathSegment>
      pSegments) {
    return memory.fromAccessPathToMemoryPoint(pDeclaredName, pSegments);
  }

  @Nullable
  public MemoryPoint getMemoryPointForAddress(
      Address pAddress, CType pType, List<PathSegment>
      pSegments) {
    return memory.fromAddressToMemoryPoint(pAddress, pType, pSegments);
  }

  /* ******************* */
  /* summary application */
  /* ******************* */

  @Override
  public Collection<? extends AbstractState> applyFunctionSummary(
      List<SummaryInstance> pSummaryList, CFAEdge inEdge, CFAEdge outEdge,
      List<AbstractState> pOtherStates) throws CPATransferException {
    Set<ShapeState> results = Collections.singleton(this);
    for (SummaryInstance summary : pSummaryList) {
      Set<ShapeState> subResults = Sets.newHashSet();
      if (summary instanceof AccessFunctionInstance) {
        for (ShapeState state : results) {
          subResults.addAll(AccessSummaryApplicator.applyFunctionSummary(state, pOtherStates,
              (CFunctionReturnEdge) outEdge, (AccessFunctionInstance) summary));
        }
      } else if (summary instanceof ArithFunctionSummaryInstance) {
        for (ShapeState state : results) {
          subResults.addAll(ArithSummaryApplicator.applyFunctionSummary(state, pOtherStates,
              (CFunctionReturnEdge) outEdge, (ArithFunctionSummaryInstance) summary));
        }
      } else if (summary instanceof RangeFunctionInstance) {
        for (ShapeState state : results) {
          subResults.addAll(
              RangeSummaryApplicator.applyFunctionSummary(state, pOtherStates,
              (CFunctionReturnEdge) outEdge, (RangeFunctionInstance) summary));
        }
      } else {
        continue;
      }
      results = subResults;
    }
    return Collections.unmodifiableCollection(results);
  }

  @Override
  public Multimap<CFAEdge, AbstractState> applyExternalLoopSummary(
      List<SummaryInstance> pSummaryList, CFAEdge inEdge, Collection<CFAEdge> outEdges,
      List<AbstractState> pOtherStates) throws CPATransferException {
    Multimap<CFAEdge, ShapeState> resultMap = HashMultimap.create();
    for (CFAEdge outEdge : outEdges) {
      resultMap.put(outEdge, this);
    }
    for (SummaryInstance summary : pSummaryList) {
      if (summary instanceof AccessLoopInstance) {
        resultMap = AccessSummaryApplicator.applyExternalLoopSummary(resultMap, pOtherStates,
            inEdge, (AccessLoopInstance) summary);
      } else if (summary instanceof ArithLoopSummaryInstance) {
        resultMap = ArithSummaryApplicator.applyExternalLoopSummary(resultMap, pOtherStates,
            inEdge, (ArithLoopSummaryInstance) summary);
      } else if (summary instanceof RangeExternalLoopInstance) {
        resultMap = RangeSummaryApplicator.applyExternalLoopSummary(resultMap, inEdge,
            (RangeExternalLoopInstance) summary);
      } else {
        continue;
      }
    }
    ImmutableMultimap.Builder<CFAEdge, AbstractState> builder = ImmutableMultimap.builder();
    builder.putAll(resultMap);
    return builder.build();
  }

  @Override
  public Collection<? extends AbstractState> applyInternalLoopSummary(
      List<SummaryInstance> pSummaryList, CFAEdge inEdge, List<AbstractState> pOtherStates)
      throws CPATransferException {
    Set<ShapeState> results = Collections.singleton(this);
    for (SummaryInstance summary : pSummaryList) {
      Set<ShapeState> subResults = new HashSet<>();
      if (summary instanceof AccessLoopInstance) {
        for (ShapeState state : results) {
          subResults.addAll(AccessSummaryApplicator.applyInternalLoopSummary(state,
              pOtherStates, inEdge, (AccessLoopInstance) summary));
        }
      } else if (summary instanceof ArithLoopSummaryInstance) {
        for (ShapeState state : results) {
          subResults.addAll(ArithSummaryApplicator.applyInternalLoopSummary(state,
              pOtherStates, inEdge, (ArithLoopSummaryInstance) summary));
        }
      } else if (summary instanceof RangeInternalLoopInstance) {
        for (ShapeState state : results) {
          subResults.addAll(RangeSummaryApplicator.applyInternalLoopSummary(state, inEdge,
              (RangeInternalLoopInstance) summary));
        }
      } else {
        continue;
      }
      results = subResults;
    }
    return Collections.unmodifiableCollection(results);
  }
}
