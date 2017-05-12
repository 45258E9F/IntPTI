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
package org.sosy_lab.cpachecker.cpa.shape.graphs;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGPointToEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.model.CStackFrame;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGRegion;
import org.sosy_lab.cpachecker.cpa.shape.merge.abs.GuardedAbstraction;
import org.sosy_lab.cpachecker.cpa.shape.merge.abs.GuardedValue;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.BasicValuePair;
import org.sosy_lab.cpachecker.cpa.shape.values.Address;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter.AddressDirection;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.FieldAccessSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;
import org.sosy_lab.cpachecker.util.access.PointerDereferenceSegment;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An instantiation of shape graph for C programming language.
 */
public class CShapeGraph extends ShapeGraph {

  private final Deque<CStackFrame> stackObjects = new ArrayDeque<>();

  private final Map<String, SGObject> heapObjects = new HashMap<>();
  private final Map<SGObject, CFAEdge> heapLocations = new HashMap<>();

  private final Map<String, SGRegion> globalObjects = new HashMap<>();

  /**
   * All the VLAs allocated in the stack space are not removed instantly. They are removed only
   * when a stack allocation of a VLA or stack frame is encountered.
   */
  private final Set<SGObject> danglingSet = new HashSet<>();
  private boolean hasLeak = false;
  private Set<CFAEdge> leakEdges = new HashSet<>();

  /**
   * mapping memory location to its abstract value.
   * NOTE: this should be consistent with have-value edges.
   */
  private final Map<MemoryLocation, Long> memoryValue = new HashMap<>();

  /**
   * Abstraction information, including:
   * (1) how a value is derived from, for example, abstracted from two values in two states,
   * (2) how the refinement is specified.
   * If no abstraction is employed in analysis, this information stays empty.
   */
  private GuardedAbstraction abstraction = GuardedAbstraction.of();

  public CShapeGraph(MachineModel pMachineModel) {
    super(pMachineModel);
    SGObject nullObject = getNullObject();
    heapObjects.put(nullObject.getLabel(), nullObject);
  }

  public CShapeGraph(CShapeGraph pMemory) {
    super(pMemory);
    for (CStackFrame frame : pMemory.stackObjects) {
      CStackFrame newFrame = new CStackFrame(frame);
      stackObjects.add(newFrame);
    }
    heapObjects.putAll(pMemory.heapObjects);
    heapLocations.putAll(pMemory.heapLocations);
    globalObjects.putAll(pMemory.globalObjects);
    memoryValue.putAll(pMemory.memoryValue);
    danglingSet.addAll(pMemory.danglingSet);

    hasLeak = pMemory.hasLeak;
    leakEdges.addAll(pMemory.leakEdges);

    abstraction.copy(pMemory.abstraction);
  }

  /* ***************** */
  /* override methods  */
  /* *******************/

  @Override
  public void addHasValueEdge(SGHasValueEdge pEdge) {
    super.addHasValueEdge(pEdge);
    SGObject object = pEdge.getObject();
    int offset = pEdge.getOffset();
    Long value = pEdge.getValue();
    SGObject assignTarget = getObjectPointedBy(value);
    if (assignTarget != null && assignTarget != getNullObject()) {
      incRef(assignTarget);
    }
    MemoryLocation location = getMemoryLocationFromObject(object, offset);
    if (location != null) {
      memoryValue.put(location, value);
    }
  }

  @Override
  public void removeHasValueEdge(SGHasValueEdge pEdge) {
    super.removeHasValueEdge(pEdge);
    SGObject removedTarget = getObjectPointedBy(pEdge.getValue());
    if (removedTarget != null && removedTarget != getNullObject()) {
      decRef(removedTarget);
    }
    MemoryLocation location = getMemoryLocationFromObject(pEdge.getObject(), pEdge.getOffset());
    if (location != null) {
      memoryValue.remove(location);
    }
  }

  @Override
  public void removeObjectAndEdges(SGObject pObject) {
    super.removeObjectAndEdges(pObject);
    String label = pObject.getLabel();
    Iterator<MemoryLocation> locIter = memoryValue.keySet().iterator();
    while (locIter.hasNext()) {
      if (locIter.next().getIdentifier().equals(label)) {
        locIter.remove();
      }
    }
  }

  @Override
  public void removeObjectAndEdges(SGObject pObject, Set<SGObject> pObserved) {
    super.removeObjectAndEdges(pObject, pObserved);
    String label = pObject.getLabel();
    Iterator<MemoryLocation> locIter = memoryValue.keySet().iterator();
    while (locIter.hasNext()) {
      if (locIter.next().getIdentifier().equals(label)) {
        locIter.remove();
      }
    }
  }

  @Override
  public void removeObjectAndEdges(
      final SGObject pObject, final Set<SGHasValueEdge> pHVEdges,
      final Set<SGPointToEdge> pPTEdges) {
    super.removeObjectAndEdges(pObject, pHVEdges, pPTEdges);
    String label = pObject.getLabel();
    Iterator<MemoryLocation> locIter = memoryValue.keySet().iterator();
    while (locIter.hasNext()) {
      if (locIter.next().getIdentifier().equals(label)) {
        locIter.remove();
      }
    }
  }

  @Override
  public void replaceHVSet(Set<SGHasValueEdge> pNewHV) {
    super.replaceHVSet(pNewHV);
    memoryValue.clear();
    // re-construct memory value mapping
    for (SGHasValueEdge edge : pNewHV) {
      SGObject object = edge.getObject();
      int offset = edge.getOffset();
      Long value = edge.getValue();
      MemoryLocation location = getMemoryLocationFromObject(object, offset);
      if (location != null) {
        memoryValue.put(location, value);
      }
    }
  }

  /**
   * Precondition: Let N1 and N2 are associated explicit values of pV1 and pV2 respectively. N1
   * \= N2.
   */
  public void mergeValues(long pV1, long pV2) {
    Deque<BasicValuePair> pendingMerges = new ArrayDeque<>();
    pendingMerges.add(BasicValuePair.of(pV1, pV2));
    while (!pendingMerges.isEmpty()) {
      BasicValuePair pair = pendingMerges.poll();
      Long v1 = pair.getLeft();
      Long v2 = pair.getRight();
      boolean exists = eq.addRelation(v1, v2);
      if (!exists) {
        long v = eq.getRepresentative(v1);
        Set<BasicValuePair> newMerges = new HashSet<>();
        if (v != v1) {
          replaceValue(v, v1, newMerges);
        }
        if (v != v2) {
          replaceValue(v, v2, newMerges);
        }
        sizeInfo.mergeValues(v, v1, v2);
        // add new derived value merges to the work-list
        for (BasicValuePair newMerge : newMerges) {
          pendingMerges.offer(newMerge);
        }
      }
    }
  }

  private void replaceValue(long newValue, long oldValue, Set<BasicValuePair> newMerges) {
    neq.mergeValues(newValue, oldValue);
    abstraction.mergeValues(newValue, oldValue, newMerges);
    removeValue(oldValue);
    Set<SGHasValueEdge> newHVEdges = new HashSet<>();
    Map<MemoryLocation, Long> newMemoryValues = new HashMap<>();
    for (SGHasValueEdge edge : HVEdges) {
      MemoryLocation loc = getMemoryLocationFromObject(edge.getObject(), edge.getOffset());
      if (edge.getValue() != oldValue) {
        newHVEdges.add(edge);
        if (loc != null) {
          newMemoryValues.put(loc, edge.getValue());
        }
      } else {
        newHVEdges.add(new SGHasValueEdge(edge.getType(), edge.getOffset(), edge.getObject(),
            newValue));
        if (loc != null) {
          newMemoryValues.put(loc, newValue);
        }
      }
    }
    HVEdges.clear();
    HVEdges.addAll(newHVEdges);
    memoryValue.clear();
    memoryValue.putAll(newMemoryValues);
  }

  /**
   * Associate a symbolic value with an explicit value.
   */
  public void putExplicitValue(KnownSymbolicValue pKey, KnownExplicitValue pValue) {
    constraints.putExplicitValue(pKey, pValue);
  }

  /* ******************************************* */
  /* methods that manipulate local/global values */
  /* ******************************************* */

  public void addHeapObject(SGObject pObject, CFAEdge pCFAEdge) {
    // though it is possible that the requested object has already existed, this is not erroneous
    // because it cannot cause any problem
    heapObjects.put(pObject.getLabel(), pObject);
    heapLocations.put(pObject, pCFAEdge);
    refCounts.put(pObject, 0L);
    super.addObject(pObject);
  }

  public void addGlobalObject(SGRegion pObject) {
    globalObjects.put(pObject.getLabel(), pObject);
    super.addObject(pObject);
  }

  public void addStackObject(SGRegion pObject, boolean isVLA) {
    super.addObject(pObject);
    ShapeExplicitValue size = pObject.getSize();
    @Nullable
    Integer length;
    if (isVLA || size.isUnknown()) {
      length = null;
      removeDanglingSet();
    } else {
      length = size.getAsInt();
    }
    stackObjects.peek().addStackVariable(pObject.getLabel(), pObject, length);
  }

  public void addStackFrame(CFunctionDeclaration pDeclaration) {
    CStackFrame newFrame = new CStackFrame(pDeclaration, getMachineModel());
    SGObject returnObject = newFrame.getReturnObject();
    if (returnObject != null) {
      super.addObject(returnObject);
    }
    stackObjects.push(newFrame);
    removeDanglingSet();
  }

  public void dropStackFrame() {
    CStackFrame frame = stackObjects.pop();
    Pair<Set<SGObject>, Set<SGObject>> objects = frame.getTriagedObjects();
    Set<SGObject> forRemoval = Preconditions.checkNotNull(objects.getFirst());
    Set<SGObject> forDangle = Preconditions.checkNotNull(objects.getSecond());
    for (SGObject object : forRemoval) {
      removeObjectAndEdges(object);
    }
    danglingSet.addAll(forDangle);
  }

  public void removeStackObject(SGObject pObject) {
    if (pObject instanceof SGRegion) {
      removeObjectAndEdges(pObject);
      stackObjects.peek().removeStackVariable((SGRegion) pObject);
    }
  }

  private void removeDanglingSet() {
    for (SGObject object : danglingSet) {
      removeObjectAndEdges(object);
    }
    danglingSet.clear();
  }

  /**
   * Reset the current dangling set with the given one.
   * Precondition: the current dangling set is empty.
   * Note: this method should only be called by state merge algorithm.
   */
  public void setDanglingSet(Set<SGObject> pDanglingSet) {
    if (danglingSet.isEmpty()) {
      danglingSet.addAll(pDanglingSet);
    }
  }

  /**
   * Note: this method is applicable only for state merge.
   */
  public void setMemoryLeak(boolean pStatus) {
    hasLeak = pStatus;
  }

  public void dropGlobals() {
    for (SGObject object : globalObjects.values()) {
      removeObjectAndEdges(object);
    }
    globalObjects.clear();
  }

  public void dropHeapObject(SGObject pObject) {
    String name = pObject.getLabel();
    if (heapObjects.containsKey(name)) {
      heapObjects.remove(name);
      heapLocations.remove(pObject);
    }
  }

  /**
   * Remove unreachable memory objects, which reveals memory leak issue.
   *
   * @param pValuesHasExplicit the set of symbolic values associated with explicit values
   * @return whether any constraint is removed
   */
  public boolean pruneUnreachable(Set<Long> pValuesHasExplicit) {
    // STEP 1: remove leaked objects and restore leak edges for the purpose of error reporting
    Iterator<SGObject> heapIt = heapObjects.values().iterator();
    Set<SGObject> observed = new HashSet<>();
    while (heapIt.hasNext()) {
      SGObject heapObject = heapIt.next();
      if (isObjectValid(heapObject)) {
        if (isZeroRef(heapObject)) {
          // this object is not referenced by any other objects
          hasLeak = true;
          leakEdges.add(heapLocations.remove(heapObject));
          // remove unreachable object to avoid duplicated error reporting
          heapIt.remove();
          removeObjectAndEdges(heapObject, observed);
          abstraction.pruneObject(heapObject);
        }
      }
    }
    boolean hasRemoved;
    do {
      Set<SGObject> newObserved = new HashSet<>();
      for (SGObject obj : observed) {
        if (!heapObjects.containsKey(obj.getLabel())) {
          continue;
        }
        if (isZeroRef(obj)) {
          Set<SGObject> newSubObserved = new HashSet<>();
          removeObjectAndEdges(obj, newSubObserved);
          abstraction.pruneObject(obj);
          newObserved.addAll(newSubObserved);
        }
      }
      observed.clear();
      observed.addAll(newObserved);
    } while (!observed.isEmpty());
    // STEP 2: collect unused value. An unused value is not used in any has-value edge or point-to
    // edge.
    Set<Long> removedValues = extractUnusedValues(pValuesHasExplicit);
    if (removedValues.isEmpty()) {
      // no unused value is found, then it is unnecessary to prune the state
      return false;
    }
    // otherwise, we prune the state according to the set of unused values
    for (Long unusedVal : removedValues) {
      removeValue(unusedVal);
    }
    // STEP 3: prune abstraction info
    Set<BasicValuePair> newValueMerges = new HashSet<>();
    // Pruning abstraction info may introduce new unused values. Consider (v1,v2) -> v where v1
    // and v2 are pruned, then v should also be pruned because v is replaced with v1 or v2 in
    // refinement.
    abstraction.pruneValues(removedValues, newValueMerges);
    // STEP 4: prune constraints
    hasRemoved = constraints.prune(eq.getClosures(removedValues));
    neq.prune(removedValues);
    // STEP 5: process equality by pruning abstraction info
    for (BasicValuePair mergePair : newValueMerges) {
      // we should check if value merge changes the constraint pool
      mergeValues(mergePair.getLeft(), mergePair.getRight());
    }

    return hasRemoved;
  }

  private Set<Long> extractUnusedValues(Set<Long> pValuesHasExplicit) {
    Set<Long> allValues = new TreeSet<>(getValues());
    Set<Long> usedValues = new TreeSet<>();
    Set<SGHasValueEdge> hvEdges = getHVEdges();
    for (SGHasValueEdge hvEdge : hvEdges) {
      usedValues.add(hvEdge.getValue());
    }
    Collection<SGPointToEdge> ptEdges = getPTEdges().values();
    for (SGPointToEdge ptEdge : ptEdges) {
      usedValues.add(ptEdge.getValue());
    }
    // UPDATE: Consider the case where the value of a local variable x has two values (Y: 4 and
    // Z: 5), then in the merged state the value of x is X such that (Y,Z) -> X. Y and Z will not
    // appear in the shape graph but they should be kept for refinement.
    // Thus, if an abstract value is used, then its refinement images are also used.
    usedValues.addAll(pValuesHasExplicit);
    return Collections.unmodifiableSet(Sets.difference(allValues, usedValues));
  }

  /**
   * Reset the leak edge set with the specified one.
   * Precondition: current leak edge set is empty.
   * Note: this method is applicable only for state merge algorithm.
   */
  public void setLeakEdges(Set<CFAEdge> edges) {
    if (leakEdges.isEmpty()) {
      leakEdges.addAll(edges);
    }
  }

  /**
   * Reset the abstraction with the specified one.
   * Note: this method is applicable only for state merge algorithm.
   */
  public void setAbstraction(GuardedAbstraction pAbstraction) {
    abstraction.copy(pAbstraction);
  }

  public void dropAbstraction(KnownSymbolicValue pV) {
    abstraction.pruneValue(pV.getAsLong());
  }

  public Collection<GuardedValue> interpret(Long pValue) {
    return abstraction.interpret(pValue);
  }

  public boolean isAbstractionEmpty() {
    return abstraction.isEmpty();
  }

  /* ****************************************** */
  /* The followings are non-modifying functions */
  /* ****************************************** */

  @Override
  public String toString() {
    return "CShapeGraph [\n stack_objects=" + stackObjects + "\n heap_objects" + heapObjects +
        "\n global_objects = " + globalObjects + "\n " + valuesToString() + "\n" + PTToString() +
        "\n" + HVToString() + "\n" + memoryValue.toString() + "\n";
  }

  public Map<String, SGRegion> getGlobalObjects() {
    return Collections.unmodifiableMap(globalObjects);
  }

  public Deque<CStackFrame> getStackFrames() {
    return stackObjects;
  }

  public SGObject getFunctionReturnObject() {
    return stackObjects.peek().getReturnObject();
  }

  @Nullable
  private CStackFrame getStackFrameForObject(SGObject pObject) {
    String label = pObject.getLabel();
    for (CStackFrame frame : stackObjects) {
      if ((frame.containsVariable(label) && frame.getVariable(label) == pObject) ||
          pObject == frame.getReturnObject()) {
        return frame;
      }
    }
    // if there is no such stack object, we return NULL instead
    return null;
  }

  @Nullable
  private CStackFrame getStackFrame(String pName) {
    for (CStackFrame frame : stackObjects) {
      String frameName = frame.getFunctionDeclaration().getName();
      if (frameName.equals(pName)) {
        return frame;
      }
    }
    return null;
  }

  @Nullable
  public String getFunctionName(SGObject pObject) {
    for (CStackFrame frame : stackObjects) {
      if (frame.getAllObjects().contains(pObject)) {
        return frame.getFunctionDeclaration().getName();
      }
    }
    return null;
  }

  public boolean isStackObject(SGObject pObject) {
    return stackObjects.peek().containsObject(pObject);
  }

  public SGObject getVisibleStackObject(String pName) {
    if (stackObjects.size() != 0) {
      return stackObjects.peek().getVariable(pName);
    }
    return null;
  }

  public boolean isGlobalObject(SGObject pObject) {
    return globalObjects.containsKey(pObject.getLabel());
  }

  public boolean isHeapObject(SGObject pObject) {
    return heapObjects.containsKey(pObject.getLabel());
  }

  public Map<String, SGObject> getHeapObjects() {
    return Collections.unmodifiableMap(heapObjects);
  }

  private boolean hasHeapObject(String pLocationID) {
    return heapObjects.containsKey(pLocationID);
  }

  @Nullable
  public SGObject getHeapObject(String pLocationID) {
    return heapObjects.get(pLocationID);
  }

  @Nullable
  public CFAEdge getEdgeForHeapObject(SGObject pObject) {
    return heapLocations.get(pObject);
  }

  public boolean hasMemoryLeak() {
    return hasLeak;
  }

  public Set<CFAEdge> getLeakEdges() {
    return Collections.unmodifiableSet(leakEdges);
  }

  public void resetMemoryLeak() {
    hasLeak = false;
    leakEdges.clear();
  }

  public Set<SGObject> getDanglingSet() {
    return Collections.unmodifiableSet(danglingSet);
  }

  @Nullable
  public SGObject getObjectForVisibleVariable(String pName) {
    if (stackObjects.size() != 0) {
      if (stackObjects.peek().containsVariable(pName)) {
        return stackObjects.peek().getVariable(pName);
      }
    }
    if (globalObjects.containsKey(pName)) {
      return globalObjects.get(pName);
    }
    // heap objects are generated by allocation methods
    return null;
  }

  @Nullable
  public StackObjectInfo getStackObjectInfo(SGObject pObject) {
    String label = pObject.getLabel();
    long level = 0;
    Iterator<CStackFrame> stackIterator = stackObjects.descendingIterator();
    while (stackIterator.hasNext()) {
      CStackFrame frame = stackIterator.next();
      if (frame.containsVariable(label)) {
        SGRegion region = frame.getVariable(label);
        if (region == pObject) {
          return new StackObjectInfo(level, frame.isVLA(region), false);
        }
      }
      if (pObject == frame.getReturnObject()) {
        return new StackObjectInfo(level, false, true);
      }
      level++;
    }
    // the specified object does not exist on the stack
    return null;
  }

  public static class StackObjectInfo {

    /**
     * The level of stack frame where the target stack object is in.
     * The top-most frame has level 0.
     */
    private final long frameLevel;

    private final boolean isVLA;

    private final boolean isReturn;

    StackObjectInfo(long pLevel, boolean pIsVLA, boolean pIsReturn) {
      frameLevel = pLevel;
      isVLA = pIsVLA;
      isReturn = pIsReturn;

      if (isReturn) {
        Preconditions.checkArgument(!isVLA);
      }
    }

    @Nullable
    public Integer compare(
        @Nonnull StackObjectInfo pInfo,
        AddressDirection functionDirection,
        AddressDirection variableDirection) {
      Integer comparison = 0;
      if (frameLevel < pInfo.frameLevel) {
        comparison = -1;
      } else if (frameLevel > pInfo.frameLevel) {
        comparison = 1;
      }
      if (functionDirection == AddressDirection.UPWARDS) {
        comparison *= -1;
      }
      if (comparison != 0) {
        return comparison;
      }
      // they are in the same frame level
      boolean otherIsReturn = pInfo.isReturn;
      if (isReturn && otherIsReturn) {
        return 0;
      }
      if (isReturn) {
        comparison = -1;
      } else if (otherIsReturn) {
        comparison = 1;
      } else {
        if (isVLA && !pInfo.isVLA) {
          comparison = -1;
        } else if (!isVLA && pInfo.isVLA) {
          comparison = 1;
        } else {
          comparison = null;
        }
      }
      if (comparison != null && variableDirection == AddressDirection.UPWARDS) {
        comparison *= -1;
      }
      return comparison;
    }
  }

  public GuardedAbstraction getAbstraction() {
    return abstraction;
  }

  /* **************************** */
  /* Memory location manipulation */
  /* **************************** */

  @Nullable
  private MemoryLocation getMemoryLocationFromObject(SGObject pObject, int pOffset) {
    if (isHeapObject(pObject) || (pObject instanceof SGRegion && globalObjects
        .containsValue(pObject))) {
      // heap or global object
      return MemoryLocation.valueOf(pObject.getLabel(), pOffset);
    } else {
      // stack object
      CStackFrame frame = getStackFrameForObject(pObject);
      if (frame == null) {
        return null;
      }
      String functionName = frame.getFunctionDeclaration().getName();
      return MemoryLocation.valueOf(functionName, pObject.getLabel(), pOffset);
    }
  }

  @Nullable
  public SGObject getObjectFromMemoryLocation(MemoryLocation pLocation) {
    String locLabel = pLocation.getIdentifier();
    if (pLocation.isOnFunctionStack()) {
      CStackFrame frame = getStackFrame(pLocation.getFunctionName());
      if (frame == null) {
        return null;
      }
      if (locLabel.contains(CStackFrame.RETVAL_LABEL)) {
        return frame.getReturnObject();
      }
      return frame.getVariable(locLabel);
    } else if (globalObjects.containsKey(locLabel)) {
      return globalObjects.get(locLabel);
    } else if (hasHeapObject(locLabel)) {
      return getHeapObject(locLabel);
    } else {
      return null;
    }
  }

  /* ************************* */
  /* shape analysis strengthen */
  /* ************************* */

  /**
   * Compute a set of actual paths of the given access path
   * Definition: an ACTUAL ACCESS PATH only contains: (1) declaration, (2) field access, (3)
   * constant array index.
   * NOTE: for the sake of generality, the input access path should be canonical, i.e. containing
   * no analysis-specific segments such as uncertain array index. Pointer dereference segment is
   * allowed.
   *
   * @param path an access path
   * @return Target access paths which are point-to targets of given access path. This set can be
   * empty or singleton set.
   */
  public Set<AccessPath> getPointToAccessPath(AccessPath path) {
    MemoryPoint memoryPoint = fromAccessPathToMemoryPoint(path);
    if (memoryPoint == null) {
      return Collections.emptySet();
    }
    return fromMemoryPointToAccessPaths(memoryPoint);
  }

  @Nullable
  public MemoryPoint fromAccessPathToMemoryPoint(AccessPath path) {
    String name = path.getQualifiedName();
    MemoryLocation currentLoc = getMemoryLocationFor(name);
    if (currentLoc == null) {
      return null;
    }
    return getMemoryPointForSegments(currentLoc, path.getType(), path.afterFirstPath());
  }

  @Nullable
  public MemoryPoint fromAccessPathToMemoryPoint(String declaredName, List<PathSegment> segments) {
    MemoryLocation currentLoc = getMemoryLocationFor(declaredName);
    if (currentLoc == null) {
      return null;
    }
    CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().orNull();
    if (cfaInfo == null) {
      return null;
    }
    CType declaredType = cfaInfo.getType(declaredName);
    if (declaredType == null) {
      return null;
    }
    return getMemoryPointForSegments(currentLoc, declaredType, segments);
  }

  @Nullable
  public MemoryPoint fromAddressToMemoryPoint(
      Address pAddress, CType pType, List<PathSegment>
      pSegments) {
    assert (!pAddress.isUnknown());
    MemoryLocation location = getMemoryLocationFromObject(pAddress.getObject(), pAddress
        .getOffset().getAsInt());
    if (location == null) {
      return null;
    }
    return getMemoryPointForSegments(location, pType, pSegments);
  }

  @Nullable
  private MemoryPoint getMemoryPointForSegments(
      MemoryLocation pLocation, CType pType,
      List<PathSegment> segments) {
    MemoryLocation currentLoc = pLocation;
    CType currentType = pType;
    long currentOffset = pLocation.getOffset();
    for (PathSegment segment : segments) {
      if (segment instanceof PointerDereferenceSegment) {
        // then, current type should be either pointer or array
        CPointerType pointerType = Types.extractPointerType(currentType);
        CArrayType arrayType = Types.extractArrayType(currentType);
        if ((pointerType == null) == (arrayType == null)) {
          throw new UnsupportedOperationException("pointer dereference only supports either "
              + "pointer or array");
        }
        if (arrayType != null) {
          // *arr is equivalent to arr[0]
          currentType = arrayType.getType();
          continue;
        }
        // if we reach here, then pointerType is not null
        Long address = memoryValue.get(currentLoc);
        if (address == null) {
          return null;
        }
        currentLoc = getMemoryLocationPointedBy(address);
        if (currentLoc == null) {
          return null;
        }
        currentOffset = currentLoc.getOffset();
        currentType = pointerType.getType();
      } else if (segment instanceof FieldAccessSegment) {
        // calculate the new offset in current memory object
        // if the offset exceeds the size of current memory object, buffer overflow error occurs
        // and we should report an error (but not here)
        String fieldName = segment.getName();
        Pair<Long, CType> fieldInfo = Types.getFieldInfo(currentType, fieldName, machineModel);
        Long delta = fieldInfo.getFirst();
        CType newType = fieldInfo.getSecond();
        if (delta == null || newType == null) {
          return null;
        }
        currentOffset += delta;
        currentType = newType;
      } else if (segment instanceof ArrayConstIndexSegment) {
        // calculate the new offset
        // two cases: (1) dynamic allocated array, in which case array expression has pointer
        // type; (2) statically allocated array, in which case array expression has array type
        long index = ((ArrayConstIndexSegment) segment).getIndex();
        CPointerType pointerType = Types.extractPointerType(currentType);
        CArrayType arrayType = Types.extractArrayType(currentType);
        if ((pointerType == null) == (arrayType == null)) {
          throw new UnsupportedOperationException("array subscript only supports either pointer "
              + "or array");
        }
        if (arrayType != null) {
          currentType = arrayType.getType();
          currentOffset += (index * machineModel.getSizeof(currentType));
        } else {
          MemoryLocation newLocation = MemoryLocation.valueOf(currentLoc, currentOffset);
          Long address = memoryValue.get(newLocation);
          if (address == null) {
            return null;
          }
          currentLoc = getMemoryLocationPointedBy(address);
          if (currentLoc == null) {
            return null;
          }
          currentOffset = currentLoc.getOffset();
          currentType = pointerType.getType();
          currentOffset += (index * machineModel.getSizeof(currentType));
        }
      } else {
        throw new UnsupportedOperationException("unsupported segment: " + segment);
      }
    }
    return new MemoryPoint(currentLoc, currentOffset, currentType);
  }

  private Set<AccessPath> fromMemoryPointToAccessPaths(MemoryPoint pMemoryPoint) {
    MemoryLocation location = pMemoryPoint.getLocation();
    long offset = pMemoryPoint.getOffset();
    CType targetType = pMemoryPoint.getType();
    String memoryName = location.getDeclaredName();
    Pair<CType, Boolean> typeAndAlloca = getTypeAndAllocationInfo(memoryName);
    if (typeAndAlloca == null) {
      // that means, we cannot find a memory region with specified name
      return Collections.emptySet();
    }
    // since the resultant access path should be an actual path, we can only append 3 kinds of
    // segments: (1) declaration, which is always the first one, (2) constant array index, (3)
    // field reference. Obviously, only the last two cases should be considered.
    // [1] memory location derives from malloc
    //     1.1 extract array element type
    //     1.2 compute the index of array element
    // [2] other cases
    //     2.1 if offset + size of current type reaches the end of given type, terminate.
    //     2.2 otherwise, compute the array index or field where the current type is in
    //         and then continue the above procedure until termination
    CType dataType = typeAndAlloca.getFirst();
    Boolean isDynamic = typeAndAlloca.getSecond();
    assert (dataType != null && isDynamic != null);
    boolean isGlobal = !location.isOnFunctionStack();
    CVariableDeclaration var = new CVariableDeclaration(
        FileLocation.DUMMY,
        isGlobal,
        CStorageClass.AUTO,
        dataType,
        location.getIdentifier(),
        location.getIdentifier(),
        location.getDeclaredName(),
        null
    );
    AccessPath newPath = new AccessPath(var);
    if (isDynamic) {
      // dynamically allocated memory: we should append an array index segment first
      dataType = Types.dereferenceType(dataType);
      int unitLength = machineModel.getSizeof(dataType);
      long index = offset / unitLength;
      offset = offset % unitLength;
      newPath.appendSegment(new ArrayConstIndexSegment(index));
    }
    return constructAccessPath(newPath, dataType, offset, targetType);
  }

  private Set<AccessPath> constructAccessPath(
      AccessPath path, CType currentType, long offset,
      CType targetType) {
    if (currentType instanceof CTypedefType) {
      return constructAccessPath(path, ((CTypedefType) currentType).getRealType(), offset,
          targetType);
    }
    // check if the construction could terminate
    if (offset == 0 && Types.isEquivalent(currentType, targetType)) {
      return Collections.singleton(path);
    } else if (offset != 0 &&
        (currentType instanceof CEnumType || currentType instanceof CPointerType ||
            currentType instanceof CSimpleType || currentType instanceof CVoidType)) {
      // if current type is elementary type and the offset is non-zero, then there is no matching
      // access path
      return Collections.emptySet();
    }
    // exclude some impossible cases rapidly
    if (currentType instanceof CProblemType) {
      return Collections.emptySet();
    }
    // if we reach here:
    // (1) offset equals ZERO but current type does not match target type
    // (2) offset is non-zero and current type is not an elementary type

    // non-terminate case: array
    if (currentType instanceof CArrayType) {
      CType elementType = ((CArrayType) currentType).getType();
      int elementSize = machineModel.getSizeof(elementType);
      long index = offset / elementSize;
      long newOffset = offset % elementSize;
      // here we do not check possible buffer overflow
      AccessPath newPath = AccessPath.copyOf(path);
      newPath.appendSegment(new ArrayConstIndexSegment(index));
      return constructAccessPath(newPath, elementType, newOffset, targetType);
    }
    // non-terminate case: struct / union
    if (currentType instanceof CCompositeType) {
      CCompositeType compositeType = (CCompositeType) currentType;
      switch (compositeType.getKind()) {
        case STRUCT:
          return handleStruct(path, compositeType, offset, targetType);
        case UNION:
          return handleUnion(path, compositeType, offset, targetType);
        case ENUM:
        default:
          throw new UnsupportedOperationException("unsupported composite type: " + compositeType);
      }
    }
    if (currentType instanceof CElaboratedType) {
      CType defType = ((CElaboratedType) currentType).getRealType();
      if (defType != null) {
        return constructAccessPath(path, defType, offset, targetType);
      }
      return Collections.emptySet();
    }
    // if we reach here, then the offset equals zero but the type does not matching the target
    return Collections.emptySet();
  }

  private Set<AccessPath> handleStruct(
      AccessPath path, CCompositeType currentType, long offset,
      CType targetType) {
    int position = 0;
    for (CCompositeTypeMemberDeclaration member : currentType.getMembers()) {
      CType memberType = member.getType();
      position = machineModel.getPadding(position, memberType);
      int sizeOfMember = machineModel.getSizeof(memberType);
      long delta = offset - position;
      if (delta >= 0 && delta < sizeOfMember) {
        // then, we enter this field
        if (delta + machineModel.getSizeof(targetType) > sizeOfMember) {
          return Collections.emptySet();
        }
        String memberName = member.getName();
        AccessPath newPath = AccessPath.copyOf(path);
        newPath.appendSegment(new FieldAccessSegment(memberName));
        return constructAccessPath(newPath, memberType, delta, targetType);
      }
      position += sizeOfMember;
    }
    // if we reach here, that means the offset corresponds to non-data region, which is possibly
    // a buffer overflow defect
    return Collections.emptySet();
  }

  private Set<AccessPath> handleUnion(
      AccessPath path, CCompositeType currentType, long offset,
      CType targetType) {
    // Note: since we can visit a memory block by multiple manners, it is possible to derive more
    // than one matching access paths
    Set<AccessPath> results = new HashSet<>();
    for (CCompositeTypeMemberDeclaration member : currentType.getMembers()) {
      CType memberType = member.getType();
      int sizeOfMember = machineModel.getSizeof(memberType);
      if (offset >= 0 && offset < sizeOfMember) {
        if (offset + machineModel.getSizeof(targetType) > sizeOfMember) {
          // then this member is impossible, try the next one instead
          continue;
        }
        AccessPath newPath = AccessPath.copyOf(path);
        String memberName = member.getName();
        newPath.appendSegment(new FieldAccessSegment(memberName));
        results.addAll(constructAccessPath(newPath, memberType, offset, targetType));
      }
    }
    return results;
  }

  private final static String SPLITTER = "::";

  @Nullable
  private MemoryLocation getMemoryLocationFor(String qualifiedName) {
    String[] nameSplit = qualifiedName.split(SPLITTER);
    if (nameSplit.length == 1) {
      // then, this name corresponds to global object
      SGRegion region = globalObjects.get(qualifiedName);
      if (region == null) {
        return null;
      } else {
        return MemoryLocation.valueOf(region.getLabel(), 0);
      }
    } else {
      String functionName = nameSplit[0];
      String identifier = nameSplit[1];
      for (CStackFrame frame : stackObjects) {
        if (frame.getFunctionDeclaration().getName().equals(functionName)) {
          SGObject region = frame.getVariable(identifier);
          if (region == null) {
            return null;
          } else {
            return MemoryLocation.valueOf(functionName, identifier, 0);
          }
        }
      }
      return null;
    }
  }

  @Nullable
  private MemoryLocation getMemoryLocationPointedBy(Long address) {
    if (!values.contains(address)) {
      return null;
    }
    if (PTEdges.containsKey(address)) {
      SGPointToEdge ptEdge = PTEdges.get(address);
      SGObject object = ptEdge.getObject();
      int offset = ptEdge.getOffset();
      return getMemoryLocationFromObject(object, offset);
    } else {
      return null;
    }
  }

  @Nullable
  private Pair<CType, Boolean> getTypeAndAllocationInfo(String name) {
    String[] nameSplit = name.split(SPLITTER);
    if (nameSplit.length == 1) {
      // this name could correspond to global object or heap object
      SGRegion region = globalObjects.get(name);
      if (region != null) {
        return Pair.of(region.getType(), region.isDynamic());
      }
      SGObject object = heapObjects.get(name);
      if (object != null) {
        if (object instanceof SGRegion) {
          return Pair.of(((SGRegion) object).getType(), ((SGRegion) object).isDynamic());
        } else {
          // for example, null object
          CType defaultType = CPointerType.POINTER_TO_VOID;
          return Pair.of(defaultType, SGRegion.STATIC);
        }
      }
      // if we reach here, this name corresponds to nothing
      return null;
    } else {
      String functionName = nameSplit[0];
      String identifier = nameSplit[1];
      for (CStackFrame frame : stackObjects) {
        if (frame.getFunctionDeclaration().getName().equals(functionName)) {
          SGObject object = frame.getVariable(identifier);
          if (object == null) {
            // if the selected memory object is an abstracted one, then we cannot derive its type
            // and allocation type
            return null;
          } else {
            SGRegion region = (SGRegion) object;
            return Pair.of(region.getType(), region.isDynamic());
          }
        }
      }
      return null;
    }
  }

  /**
   * The intermediate structure of memory location representation. The memory location denotes
   * the memory block in stack or heap space, while offset denotes the distance from the starting
   * point of memory block. By type information we can extract appropriate length of data from
   * memory.
   */
  public class MemoryPoint {

    private final MemoryLocation location;
    private final long offset;
    private final CType type;

    MemoryPoint(MemoryLocation pLocation, Long pOffset, CType pType) {
      location = pLocation;
      offset = pOffset;
      type = pType;
    }

    public MemoryLocation getLocation() {
      return location;
    }

    public long getOffset() {
      return offset;
    }

    public CType getType() {
      return type;
    }
  }

}
