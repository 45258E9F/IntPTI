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
package org.sosy_lab.cpachecker.cpa.smg.graphs;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.counterexample.IDExpression;
import org.sosy_lab.cpachecker.cpa.range.ArrayUncertainIndexSegment;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGStateInformation;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.FieldAccessSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;
import org.sosy_lab.cpachecker.util.access.PointerDereferenceSegment;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Extending SMG with notions specific for programs in C language:
 * - separation of global, heap and stack objects
 * - null object and value
 */
public class CLangSMG extends SMG {
  /**
   * A container for object found on the stack:
   * - local variables
   * - parameters
   *
   * TODO: [STACK-FRAME-STRUCTURE] Perhaps it could be wrapped in a class?
   */
  final private Deque<CLangStackFrame> stack_objects = new ArrayDeque<>();

  /**
   * A container for objects allocated on heap
   */
  final private Set<SMGObject> heap_objects = new HashSet<>();

  /**
   * A container for global objects
   */
  final private Map<String, SMGRegion> global_objects = new HashMap<>();

  /**
   * A mapping from qualified name to type
   */
  final private Map<String, TypeAndAlloca> typeMapping = new HashMap<>();

  /**
   * A mapping from temporary variable to memory object name
   */
  final private Map<String, String> tempVar2Name = new HashMap<>();

  /**
   * A flag signifying the edge leading to this state caused memory to be leaked
   * TODO: Seems pretty arbitrary: perhaps we should have a more general solution,
   * like a container with (type, message) error witness kind of thing?
   */
  private boolean has_leaks = false;

  static private LogManager logger = null;

  /**
   * A flag setting if the class should perform additional consistency checks.
   * It should be useful only during debugging, when is should find bad
   * external calls closer to their origin. We probably do not want t
   * run the checks in the production build.
   */
  static private boolean perform_checks = false;

  static public void setPerformChecks(boolean pSetting, LogManager logger) {
    CLangSMG.perform_checks = pSetting;
    CLangSMG.logger = logger;
  }

  static public boolean performChecks() {
    return CLangSMG.perform_checks;
  }

  /**
   * Constructor.
   *
   * Keeps consistency: yes
   *
   * Newly constructed CLangSMG contains a single nullObject with an address
   * pointing to it, and is empty otherwise.
   */
  public CLangSMG(MachineModel pMachineModel) {
    super(pMachineModel);
    heap_objects.add(getNullObject());
  }

  /**
   * Copy constructor.
   *
   * Keeps consistency: yes
   *
   * @param pHeap The original CLangSMG
   */
  public CLangSMG(CLangSMG pHeap) {
    super(pHeap);

    for (CLangStackFrame stack_frame : pHeap.stack_objects) {
      CLangStackFrame new_frame = new CLangStackFrame(stack_frame);
      stack_objects.add(new_frame);
    }

    heap_objects.addAll(pHeap.heap_objects);
    global_objects.putAll(pHeap.global_objects);
    has_leaks = pHeap.has_leaks;
    typeMapping.putAll(pHeap.typeMapping);
    tempVar2Name.putAll(pHeap.tempVar2Name);
  }

  /**
   * Add a object to the heap.
   *
   * Keeps consistency: no
   *
   * With checks: throws {@link IllegalArgumentException} when asked to add
   * an object already present.
   *
   * @param pObject Object to add.
   */
  public void addHeapObject(SMGObject pObject) {
    if (CLangSMG.performChecks() && heap_objects.contains(pObject)) {
      throw new IllegalArgumentException("Heap object already in the SMG: [" + pObject + "]");
    }
    heap_objects.add(pObject);
    addObject(pObject);
  }

  /**
   * Add a global object to the SMG
   *
   * Keeps consistency: no
   *
   * With checks: throws {@link IllegalArgumentException} when asked to add
   * an object already present, or an global object with a label identifying
   * different object
   *
   * @param pObject Object to add
   */
  public void addGlobalObject(SMGRegion pObject) {
    if (CLangSMG.performChecks() && global_objects.values().contains(pObject)) {
      throw new IllegalArgumentException("Global object already in the SMG: [" + pObject + "]");
    }

    if (CLangSMG.performChecks() && global_objects.containsKey(pObject.getLabel())) {
      throw new IllegalArgumentException(
          "Global object with label [" + pObject.getLabel() + "] already in the SMG");
    }

    global_objects.put(pObject.getLabel(), pObject);
    super.addObject(pObject);
  }

  /**
   * Adds an object to the current stack frame
   *
   * Keeps consistency: no
   *
   * @param pObject Object to add
   *
   *                TODO: [SCOPES] Scope visibility vs. stack frame issues: handle cases where a
   *                variable is visible but is is allowed to override (inner blocks) TODO:
   *                Consistency check (allow): different objects with same label inside a frame, but
   *                in different block TODO: Test for this consistency check
   *
   *                TODO: Shall we need an extension for putting objects to upper frames?
   */
  public void addStackObject(SMGRegion pObject) {
    super.addObject(pObject);
    stack_objects.peek().addStackVariable(pObject.getLabel(), pObject);
  }

  /**
   * Add a new stack frame for the passed function.
   *
   * Keeps consistency: yes
   *
   * @param pFunctionDeclaration A function for which to create a new stack frame
   */
  public void addStackFrame(CFunctionDeclaration pFunctionDeclaration) {
    CLangStackFrame newFrame = new CLangStackFrame(pFunctionDeclaration, getMachineModel());

    // Return object is NULL for void functions
    SMGObject returnObject = newFrame.getReturnObject();
    if (returnObject != null) {
      super.addObject(newFrame.getReturnObject());
    }
    stack_objects.push(newFrame);
  }

  public void addTypeInfo(String qualifiedName, CType type, boolean alloca) {
    typeMapping.put(qualifiedName, new TypeAndAlloca(type, alloca));
  }

  public void removeTypeInfo(String qualifiedName) {
    typeMapping.remove(qualifiedName);
  }

  public void addTempVarToName(String tempVarName, String memoryName) {
    tempVar2Name.put(tempVarName, memoryName);
  }

  public void reduceTempVar(String tempVarName, CType newType) {
    String memName = tempVar2Name.get(tempVarName);
    if (memName != null) {
      TypeAndAlloca target = typeMapping.get(memName);
      assert target != null;
      target.updateType(newType);
      typeMapping.put(memName, target);
      // clear temporary variable from type mapping if possible
      tempVar2Name.remove(tempVarName);
      typeMapping.remove(tempVarName);
    }
  }


  /**
   * Sets a flag indicating this SMG is a successor over the edge causing a
   * memory leak.
   *
   * Keeps consistency: yes
   */
  public void setMemoryLeak() {
    has_leaks = true;
  }

  /**
   * Remove a top stack frame from the SMG, along with all objects in it, and
   * any edges leading from/to it.
   *
   * TODO: A testcase with (invalid) passing of an address of a dropped frame object
   * outside, and working with them. For that, we should probably keep those as invalid, so
   * we can spot such bug.
   *
   * Keeps consistency: yes
   */
  public void dropStackFrame() {
    CLangStackFrame frame = stack_objects.pop();
    for (SMGObject object : frame.getAllObjects()) {
      removeObjectAndEdges(object);
    }

    // remove local variables from type mapping
    String functionName = frame.getFunctionDeclaration().getName();
    Iterator<Entry<String, TypeAndAlloca>> typeMapIterator = typeMapping.entrySet().iterator();
    while (typeMapIterator.hasNext()) {
      String name = typeMapIterator.next().getKey();
      if (name.startsWith(functionName + "::")) {
        // safely remove elements by manipulating iterator
        typeMapIterator.remove();
      }
    }

    if (CLangSMG.performChecks()) {
      CLangSMGConsistencyVerifier.verifyCLangSMG(CLangSMG.logger, this);
    }
  }

  /**
   * Prune the SMG: remove all unreachable objects (heap ones: global and stack
   * are always reachable) and values.
   *
   * TODO: Too large. Refactor into fewer pieces
   *
   * Keeps consistency: yes
   */
  public void pruneUnreachable() {
    Set<SMGObject> seen = new HashSet<>();
    Set<Integer> seen_values = new HashSet<>();
    Queue<SMGObject> workqueue = new ArrayDeque<>();

    // TODO: wrap to getStackObjects(), perhaps just internally?
    for (CLangStackFrame frame : getStackFrames()) {
      for (SMGObject stack_object : frame.getAllObjects()) {
        workqueue.add(stack_object);
      }
    }

    workqueue.addAll(getGlobalObjects().values());

    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();

    /*
     * TODO: Refactor into generic methods for obtaining reachable/unreachable
     * subSMGs
     *
     * TODO: Perhaps introduce a SubSMG class which would be a SMG tied
     * to a certain (Clang)SMG and guaranteed to be a subset of it?
     */

    while (!workqueue.isEmpty()) {
      SMGObject processed = workqueue.remove();
      if (!seen.contains(processed)) {
        seen.add(processed);
        filter.filterByObject(processed);
        for (SMGEdgeHasValue outbound : getHVEdges(filter)) {
          SMGObject pointedObject = getObjectPointedBy(outbound.getValue());
          if (pointedObject != null && !seen.contains(pointedObject)) {
            workqueue.add(pointedObject);
          }
          if (!seen_values.contains(Integer.valueOf(outbound.getValue()))) {
            seen_values.add(Integer.valueOf(outbound.getValue()));
          }
        }
      }
    }

    /*
     * TODO: Refactor into generic methods for substracting SubSMGs (see above)
     */
    Set<SMGObject> stray_objects = new HashSet<>(Sets.difference(getObjects(), seen));

    // Mark all reachable from ExternallyAllocated objects as safe for remove
    workqueue.addAll(stray_objects);
    while (!workqueue.isEmpty()) {
      SMGObject processed = workqueue.remove();
      if (isObjectExternallyAllocated(processed)) {
        filter.filterByObject(processed);
        for (SMGEdgeHasValue outbound : getHVEdges(filter)) {
          SMGObject pointedObject = getObjectPointedBy(outbound.getValue());
          if (stray_objects.contains(pointedObject) && !isObjectExternallyAllocated(
              pointedObject)) {
            setExternallyAllocatedFlag(pointedObject, true);
            workqueue.add(pointedObject);
          }
        }
      }
    }

    for (SMGObject stray_object : stray_objects) {
      if (stray_object.notNull()) {
        if (isObjectValid(stray_object) && !isObjectExternallyAllocated(stray_object)) {
          setMemoryLeak();
        }
        removeObjectAndEdges(stray_object);
        heap_objects.remove(stray_object);

      }
    }

    Set<Integer> stray_values = new HashSet<>(Sets.difference(getValues(), seen_values));
    for (Integer stray_value : stray_values) {
      if (stray_value != getNullValue()) {
        // Here, we can't just remove stray value, we also have to remove the points-to edge
        if (isPointer(stray_value)) {
          removePointsToEdge(stray_value);
        }

        removeValue(stray_value);
      }
    }

    if (CLangSMG.performChecks()) {
      CLangSMGConsistencyVerifier.verifyCLangSMG(CLangSMG.logger, this);
    }
  }

  /* ********************************************* */
  /* Non-modifying functions: getters and the like */
  /* ********************************************* */

  /**
   * Getter for obtaining a string representation of the CLangSMG. Constant.
   *
   * @return String representation of the CLangSMG
   */
  @Override
  public String toString() {
    return "CLangSMG [\n stack_objects=" + stack_objects + "\n heap_objects=" + heap_objects
        + "\n global_objects="
        + global_objects + "\n " + valuesToString() + "\n " + ptToString() + "\n " + hvToString()
        + "\n" + getMapOfMemoryLocationsWithValue().toString() + "\n]";
  }

  private Map<MemoryLocation, Integer> getMapOfMemoryLocationsWithValue() {

    Set<MemoryLocation> memlocs = getTrackedMemoryLocations();
    Map<MemoryLocation, Integer> result = new HashMap<>();

    for (MemoryLocation memloc : memlocs) {
      Set<SMGEdgeHasValue> edge = getHVEdgeFromMemoryLocation(memloc);
      result.put(memloc, edge.iterator().next().getValue());
    }

    return result;
  }

  private Map<Integer, MemoryLocation> getMapOfPointToWithLocation() {

    Map<Integer, MemoryLocation> result = new HashMap<>();

    for (Integer value : values) {
      SMGEdgePointsTo ptEdge = pt_edges.get(value);
      if (ptEdge != null) {
        MemoryLocation loc = resolveMemLoc(ptEdge);
        result.put(value, loc);
      }
    }

    return result;
  }

  private Map<String, MemoryLocation> getMapOfQualifiedNameToLocation() {

    Map<String, MemoryLocation> result = new HashMap<>();

    // we traverse (1) global objects, (2) stack objects and (3) heap objects for constructing
    // mapping
    for (Map.Entry<String, SMGRegion> object : global_objects.entrySet()) {
      String id = object.getKey();
      result.put(id, MemoryLocation.valueOf(id, 0));
    }
    for (SMGObject heap_object : heap_objects) {
      String label = heap_object.getLabel();
      result.put(label, MemoryLocation.valueOf(label, 0));
    }
    for (CLangStackFrame frame : stack_objects) {
      String functionName = frame.getFunctionDeclaration().getName();
      for (Map.Entry<String, SMGRegion> stackVar : frame.getVariables().entrySet()) {
        String id = stackVar.getKey();
        MemoryLocation loc = MemoryLocation.valueOf(functionName, id, 0);
        result.put(loc.getDeclaredName(), loc);
      }
    }

    return result;
  }

  /**
   * The intermediate structure of memory location representation. The memory location denotes
   * the memory block in stack or heap space, while offset denotes the distance from the starting
   * point of memory block. By type information we can extract appropriate length of data from
   * memory.
   */
  private class MemoryPoint {

    private final MemoryLocation location;
    private final Long offset;
    private final CType type;

    MemoryPoint(MemoryLocation pLoc, Long pOffset, CType pType) {
      location = pLoc;
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

  @Nullable
  private MemoryPoint fromAccessPathToMemoryPoint
      (
          Map<MemoryLocation, Integer> memLoc2Value,
          Map<Integer, MemoryLocation> value2PointTo,
          Map<String, MemoryLocation> name2MemLoc,
          AccessPath path) {
    String qName = path.getQualifiedName();
    MemoryLocation currentLoc = name2MemLoc.get(qName);
    if (currentLoc == null) {
      return null;
    }
    List<PathSegment> segments = path.path();
    long currentOffset = 0;
    CType currentType = path.getType();
    for (int i = 1; i < segments.size(); i++) {
      PathSegment segment = segments.get(i);
      if (segment instanceof PointerDereferenceSegment) {
        // get the target address and then update the target memory location
        if (currentType instanceof CArrayType) {
          // *arr is equivalent to arr[0]
          // we only need to change the current type
          currentType = ((CArrayType) currentType).getType();
          continue;
        }
        Integer address = memLoc2Value.get(currentLoc);
        if (address == null) {
          return null;
        }
        currentLoc = value2PointTo.get(address);
        if (currentLoc == null) {
          return null;
        }
        currentOffset = currentLoc.getOffset();
        currentType = Types.dereferenceType(currentType);
      } else if (segment instanceof FieldAccessSegment) {
        // calculate the new offset in current SMG object
        // if the offset exceeds the size of SMG object, it is the SMG CPA's responsibility to
        // report an error (Buffer overflow issue)
        String fieldName = segment.getName();
        Pair<Long, CType> fieldInfo = Types.getFieldInfo(currentType, fieldName, machine_model);
        Long delta = fieldInfo.getFirst();
        CType newType = fieldInfo.getSecond();
        if (delta == null || newType == null) {
          return null;
        }
        currentOffset += delta;
        currentType = newType;
      } else if (segment instanceof ArrayConstIndexSegment) {
        // calculate the new offset, similar with handling field reference
        // two cases: (1) dynamic allocated array, in which case the type is a pointer type; (2)
        // statically allocated array, in which case the type is an array type.
        long index = ((ArrayConstIndexSegment) segment).getIndex();
        if (currentType instanceof CArrayType) {
          // get the element type
          currentType = ((CArrayType) currentType).getType();
          currentOffset += (index * machine_model.getSizeof(currentType));
        } else {
          // the type of array must be pointer
          Preconditions.checkArgument(currentType instanceof CPointerType);
          MemoryLocation newLoc = MemoryLocation.valueOf(currentLoc, currentOffset);
          Integer address = memLoc2Value.get(newLoc);
          if (address == null) {
            return null;
          }
          currentLoc = value2PointTo.get(address);
          if (currentLoc == null) {
            return null;
          }
          currentOffset = currentLoc.getOffset();
          currentType = ((CPointerType) currentType).getType();
          currentOffset += (index * machine_model.getSizeof(currentType));
        }
      } else {
        throw new AssertionError("Unsupported segment: " + segment);
      }
    }
    return new MemoryPoint(currentLoc, currentOffset, currentType);
  }

  /**
   * Compute a set of actual access paths of the given access path.
   * Definition: an ACTUAL ACCESS PATH only contains: (1) declaration, (2) field access, (3)
   * constant array index.
   * Note that the given path should be determined.
   * The resultant access path can be empty or a singleton set (i.e. containing only one access
   * path).
   *
   * @param path        An access path corresponds to a memory location whose value is memory
   *                    address
   * @param isUncertain Whether the last segment of access path is uncertain array index segment
   * @return Target access paths which are point-to target of given access path
   */
  public Set<AccessPath> getPointToAccessPath(AccessPath path, boolean isUncertain) {
    // STEP 1: some preparation works
    Map<MemoryLocation, Integer> memLoc2Value = getMapOfMemoryLocationsWithValue();
    Map<Integer, MemoryLocation> value2PointTo = getMapOfPointToWithLocation();
    Map<String, MemoryLocation> qName2memLoc = getMapOfQualifiedNameToLocation();

    // STEP 2: if the last segment is uncertain index segment, we temporarily replace it with
    // constant array index segment whose index is the lowest one
    ArrayUncertainIndexSegment uncertainSeg = null;
    if (isUncertain) {
      uncertainSeg = (ArrayUncertainIndexSegment) path.getLastSegment();
      path.removeLastSegment();
      Long lowIndex = uncertainSeg.getIndexRange().getLow().longValue();
      if (lowIndex == null) {
        return Collections.emptySet();
      }
      path.appendSegment(new ArrayConstIndexSegment(lowIndex));
    }

    MemoryPoint memoryPoint = fromAccessPathToMemoryPoint(memLoc2Value, value2PointTo,
        qName2memLoc, path);
    if (memoryPoint == null) {
      return Collections.emptySet();
    }
    // if we reach here, we have found a memory location that is consistent with given access path

    // STEP 3: try to find the target of this pointer access path, and restore its access path
    // with respect to its type
    Set<AccessPath> pathSet = fromMemoryLocationToAccessPaths(
        memoryPoint.getLocation(), memoryPoint.getOffset(), memoryPoint.getType());
    if (!isUncertain) {
      return pathSet;
    } else {
      // if the last segment is array index, we apply index range to create a new access path;
      // otherwise, we discard this access path
      Set<AccessPath> newPathSet = new HashSet<>();
      Range indexRange = uncertainSeg.getIndexRange();
      for (AccessPath singlePath : pathSet) {
        PathSegment lastSegment = singlePath.getLastSegment();
        if (lastSegment instanceof ArrayConstIndexSegment) {
          long lastIndex = ((ArrayConstIndexSegment) lastSegment).getIndex();
          Range newIndexRange = indexRange.plus(lastIndex);
          singlePath.removeLastSegment();
          singlePath.appendSegment(new ArrayUncertainIndexSegment(newIndexRange));
          newPathSet.add(singlePath);
        }
      }
      return newPathSet;
    }
  }

  @Nullable
  public Long getPointerDifference(AccessPath path1, AccessPath path2) {
    // STEP 1: some preparation works
    Map<MemoryLocation, Integer> memLoc2Value = getMapOfMemoryLocationsWithValue();
    Map<Integer, MemoryLocation> value2PointTo = getMapOfPointToWithLocation();
    Map<String, MemoryLocation> qName2memLoc = getMapOfQualifiedNameToLocation();

    // STEP 2: compute memory points for these paths
    MemoryPoint point1 = fromAccessPathToMemoryPoint(memLoc2Value, value2PointTo, qName2memLoc,
        path1);
    MemoryPoint point2 = fromAccessPathToMemoryPoint(memLoc2Value, value2PointTo, qName2memLoc,
        path2);
    if (point1 == null || point2 == null) {
      return null;
    }
    // if two memory points have different types, then we cannot handle this case
    // ** when we try to compute the difference of two pointers of incompatible types, the
    // program will not pass the compilation
    if (!Types.isEquivalent(point1.getType(), point2.getType())) {
      return null;
    }
    if (MemoryLocation.sameMemoryBlock(point1.getLocation(), point2.getLocation())) {
      long offset1 = point1.getOffset();
      long offset2 = point2.getOffset();
      // Note: if two pointers p1 and p2 have irregular distance, we use up-rounding for counting
      // their difference. For example, if they are of (long*) and their distance is 10, then the
      // result of pointer difference is 2.
      int elementSize = machine_model.getSizeof(point1.getType());
      long basicDelta = offset1 - offset2;
      if (basicDelta >= 0) {
        return (basicDelta + elementSize - 1) / elementSize;
      } else {
        return (basicDelta - elementSize + 1) / elementSize;
      }
    }
    // if we reach here, then two points belong to different memory blocks
    return null;
  }

  private Set<AccessPath> fromMemoryLocationToAccessPaths(
      MemoryLocation location, Long offset,
      CType memberType) {
    String qualifiedName = location.getDeclaredName();
    TypeAndAlloca memLocInfo = typeMapping.get(qualifiedName);
    if (memLocInfo == null) {
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
    CType dataType = memLocInfo.getType();

    // STEP 1: the very first segment of access path --- variable declaration
    boolean isGlobal = !location.isOnFunctionStack();
    CVariableDeclaration varDecl = new CVariableDeclaration(
        FileLocation.DUMMY,
        isGlobal,
        CStorageClass.AUTO,
        dataType,
        location.getIdentifier(),
        location.getIdentifier(),
        location.getDeclaredName(),
        null);
    AccessPath finalPath = new AccessPath(varDecl);
    long newOffset = offset;
    if (memLocInfo.getAlloca()) {
      // dynamically allocated memory, we should append an array index segment first
      dataType = Types.dereferenceType(dataType);
      int unitLength = machine_model.getSizeof(dataType);
      long index = offset / unitLength;
      newOffset = offset % unitLength;
      finalPath.appendSegment(new ArrayConstIndexSegment(index));
    }

    // STEP 2: construct access path by approaching target offset and type
    // Note: the status indicates that if we have found a valid access path. If `false` is
    // returned, then no access path is found.
    return constructAccessPath(finalPath, dataType, newOffset, memberType);
  }

  private Set<AccessPath> constructAccessPath(
      AccessPath path, CType type, Long offset, CType
      targetType) {
    if (type instanceof CTypedefType) {
      return constructAccessPath(path, ((CTypedefType) type).getRealType(), offset, targetType);
    }
    // check if the construction could terminate
    if (offset == 0 && machine_model.getSizeof(type) == machine_model.getSizeof(targetType)) {
      return Collections.singleton(path);
    } else if (offset != 0 &&
        (type instanceof CEnumType || type instanceof CPointerType || type instanceof
            CSimpleType || type instanceof CVoidType)) {
      return Collections.emptySet();
    }
    // exclude some obviously impossible cases
    if (type instanceof CFunctionType ||
        type instanceof CProblemType) {
      return Collections.emptySet();
    }
    // non-terminate case: append new segments
    if (type instanceof CArrayType) {
      // we can prove that array element does not need additional padding computation (by
      // structural induction)
      CType elementType = ((CArrayType) type).getType();
      int elementSize = machine_model.getSizeof(elementType);
      long index = offset / elementSize;
      long newOffset = offset % elementSize;
      // check if possible buffer overflow would occur
      CExpression arrayLength = ((CArrayType) type).getLength();
      if (arrayLength instanceof CIntegerLiteralExpression) {
        int length = ((CIntegerLiteralExpression) arrayLength).getValue().intValue();
        if (index >= length) {
          return Collections.emptySet();
        }
      }
      path.appendSegment(new ArrayConstIndexSegment(index));
      return constructAccessPath(path, elementType, newOffset, targetType);
    }
    if (type instanceof CCompositeType) {
      CCompositeType compositeType = (CCompositeType) type;
      switch (compositeType.getKind()) {
        case STRUCT:
          return handleStruct(path, compositeType, offset, targetType);
        case UNION:
          return handleUnion(path, compositeType, offset, targetType);
        case ENUM:
        default:
          throw new AssertionError();
      }
    }
    if (type instanceof CElaboratedType) {
      CType defType = ((CElaboratedType) type).getRealType();
      if (defType != null) {
        return constructAccessPath(path, type, offset, targetType);
      }
      // otherwise, we fail to find a valid access path
      return Collections.emptySet();
    }
    // if we reach here, then the offset equals zero but the type does not matching the target
    return Collections.emptySet();
  }

  private Set<AccessPath> handleStruct(
      AccessPath path, CCompositeType type, Long offset, CType
      targetType) {
    int position = 0;
    int sizeOfMember;
    for (CCompositeTypeMemberDeclaration member : type.getMembers()) {
      // for each structure member, we should consider padding
      CType memberType = member.getType();
      position += machine_model.getPadding(position, memberType);
      sizeOfMember = machine_model.getSizeof(memberType);
      long delta = offset - position;
      if (delta >= 0 && delta < sizeOfMember) {
        // enter this field

        // check if buffer overflow would occur
        if (delta + machine_model.getSizeof(targetType) >= sizeOfMember) {
          return Collections.emptySet();
        }

        String memberName = member.getName();
        path.appendSegment(new FieldAccessSegment(memberName));
        return constructAccessPath(path, memberType, delta, targetType);
      }
      position += sizeOfMember;
    }
    // if we reach here, that means the offset corresponds to non-data region, which is possibly
    // a buffer overflow defect
    return Collections.emptySet();
  }

  private Set<AccessPath> handleUnion(
      AccessPath path, CCompositeType type, Long offset, CType
      targetType) {
    // Note: since we can visit a memory block by multiple manners, it is possible to derive more
    // than one matching access paths
    Set<AccessPath> results = new HashSet<>();
    for (CCompositeTypeMemberDeclaration member : type.getMembers()) {
      CType memberType = member.getType();
      int sizeOfMember = machine_model.getSizeof(memberType);
      if (offset >= 0 && offset < sizeOfMember) {

        // check if buffer overflow would occur
        if (offset + machine_model.getSizeof(targetType) >= sizeOfMember) {
          return Collections.emptySet();
        }

        AccessPath newPath = AccessPath.copyOf(path);
        String memberName = member.getName();
        newPath.appendSegment(new FieldAccessSegment(memberName));
        results.addAll(constructAccessPath(newPath, memberType, offset, targetType));
      }
    }
    return results;
  }

  /**
   * Returns an SMGObject tied to the variable name. The name must be visible in
   * the current scope: it needs to be visible either in the current frame, or it
   * is a global variable. Constant.
   *
   * @param pVariableName A name of the variable
   * @return An object tied to the name, if such exists in the visible scope. Null otherwise.
   *
   * TODO: [SCOPES] Test for getting visible local object hiding other local object
   */
  public SMGRegion getObjectForVisibleVariable(String pVariableName) {
    // Look in the local frame
    if (stack_objects.size() != 0) {
      if (stack_objects.peek().containsVariable(pVariableName)) {
        return stack_objects.peek().getVariable(pVariableName);
      }
    }

    // Look in the global scope
    if (global_objects.containsKey(pVariableName)) {
      return global_objects.get(pVariableName);
    }
    return null;
  }

  /**
   * Returns the (modifiable) stack of frames containing objects. Constant.
   *
   * @return Stack of frames
   */
  public Deque<CLangStackFrame> getStackFrames() {
    //TODO: [FRAMES-STACK-STRUCTURE] This still allows modification, as queues
    // do not have the appropriate unmodifiable method. There is probably some good
    // way how to provide a read-only view for iteration, but I do not know it
    return stack_objects;
  }

  /**
   * Constant.
   *
   * @return Unmodifiable view of the set of the heap objects
   */
  public Set<SMGObject> getHeapObjects() {
    return Collections.unmodifiableSet(heap_objects);
  }

  /**
   * Constant.
   *
   * Checks whether given object is on the heap.
   *
   * @param object SMGObject to be checked.
   * @return True, if the given object is referenced in the set of heap objects, false otherwise.
   */
  public boolean isHeapObject(SMGObject object) {
    return heap_objects.contains(object);
  }

  /**
   * Constant.
   *
   * @return Unmodifiable map from variable names to global objects.
   */
  public Map<String, SMGRegion> getGlobalObjects() {
    return Collections.unmodifiableMap(global_objects);
  }

  /**
   * Constant.
   *
   * @return True if the SMG is a successor over the edge causing some memory to be leaked. Returns
   * false otherwise.
   */
  public boolean hasMemoryLeaks() {
    // TODO: [MEMLEAK DETECTION] There needs to be a proper graph algorithm
    //       in the future. Right now, we can discover memory leaks only
    //       after unassigned malloc call result, so we know that immediately.
    return has_leaks;
  }

  /**
   * Constant.
   *
   * @return a {@link SMGObject} for current function return value
   */
  public SMGObject getFunctionReturnObject() {
    return stack_objects.peek().getReturnObject();
  }

  @Nullable
  public String getFunctionName(SMGObject pObject) {
    for (CLangStackFrame cLangStack : stack_objects) {
      if (cLangStack.getAllObjects().contains(pObject)) {
        return cLangStack.getFunctionDeclaration().getName();
      }
    }

    return null;
  }

  @Override
  public void mergeValues(int v1, int v2) {

    super.mergeValues(v1, v2);

    if (CLangSMG.performChecks()) {
      CLangSMGConsistencyVerifier.verifyCLangSMG(CLangSMG.logger, this);
    }
  }

  final public void removeHeapObjectAndEdges(SMGObject pObject) {
    heap_objects.remove(pObject);
    removeObjectAndEdges(pObject);
  }

  public IDExpression createIDExpression(SMGObject pObject) {

    if (global_objects.containsValue(pObject)) {
      // TODO Breaks if label is changed
      return new IDExpression(pObject.getLabel());
    }

    for (CLangStackFrame frame : stack_objects) {
      if (frame.getVariables().containsValue(pObject)) {
        // TODO Breaks if label is changed

        return new IDExpression(pObject.getLabel(), frame.getFunctionDeclaration().getName());
      }
    }

    return null;
  }

  private Set<SMGEdgeHasValue> getHVEdgeFromMemoryLocation(MemoryLocation pLocation) {

    SMGObject objectAtLocation = getObjectFromMemoryLocation(pLocation);

    if (objectAtLocation == null) {
      return Collections.emptySet();
    }

    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(objectAtLocation);

    if (pLocation.isReference()) {
      filter.filterAtOffset((int) pLocation.getOffset());
    }

    // Remember, edges may overlap with different types
    Set<SMGEdgeHasValue> edgesToForget = getHVEdges(filter);

    return edgesToForget;
  }

  @Nullable
  private SMGObject getObjectFromMemoryLocation(MemoryLocation pLocation) {

    String locId = pLocation.getIdentifier();

    if (pLocation.isOnFunctionStack()) {

      if (!hasStackFrame(pLocation.getFunctionName())) {
        return null;
      }

      CLangStackFrame frame = getStackFrame(pLocation.getFunctionName());

      if (locId.equals("___cpa_temp_result_var_")) {
        return frame.getReturnObject();
      }

      if (!frame.hasVariable(locId)) {
        return null;
      }

      return frame.getVariable(locId);
    } else if (global_objects.containsKey(locId)) {

      return global_objects.get(locId);
    } else if (hasHeapObjectWithId(locId)) {

      return getHeapObjectWithId(locId);
    } else {
      return null;
    }
  }

  public SMGStateInformation forget(MemoryLocation pLocation) {

    Set<SMGEdgeHasValue> edgesToForget = getHVEdgeFromMemoryLocation(pLocation);

    if (edgesToForget.isEmpty()) {
      return SMGStateInformation.of();
    }

    for (SMGEdgeHasValue edgeToForget : edgesToForget) {
      removeHasValueEdge(edgeToForget);
    }

    return SMGStateInformation.of(edgesToForget, getPTEdges());
  }

  private SMGObject getHeapObjectWithId(String pLocId) {

    for (SMGObject object : heap_objects) {
      if (object.getLabel().equals(pLocId)) {
        return object;
      }
    }

    throw new AssertionError("Heap has no such object");
  }

  private boolean hasHeapObjectWithId(String pLocId) {

    for (SMGObject object : heap_objects) {
      if (object.getLabel().equals(pLocId)) {
        return true;
      }
    }

    return false;
  }

  /*
   * Returns stack frame of given function name
   */
  private boolean hasStackFrame(String pFunctionName) {

    for (CLangStackFrame frame : stack_objects) {
      String frameName = frame.getFunctionDeclaration().getName();
      if (frameName.equals(pFunctionName)) {
        return true;
      }
    }

    return false;
  }

  /*
   * Returns stack frame of given function name
   */
  private CLangStackFrame getStackFrame(String pFunctionName) {

    for (CLangStackFrame frame : stack_objects) {
      String frameName = frame.getFunctionDeclaration().getName();
      if (frameName.equals(pFunctionName)) {
        return frame;
      }
    }

    throw new AssertionError("No stack frame " + pFunctionName + " exists.");
  }

  public void remember(MemoryLocation pLocation, SMGStateInformation pForgottenInformation) {
    for (SMGEdgeHasValue edge : pForgottenInformation.getHvEdges()) {
      if (!getHVEdges().contains(edge)) {
        rememberHasValueEdge(edge, pForgottenInformation.getPtEdges(), pLocation);
      }
    }
  }

  private void rememberHasValueEdge(
      SMGEdgeHasValue pEdge, Map<Integer, SMGEdgePointsTo> pPtEdges,
      MemoryLocation pLocation) {
    SMGObject edgeObject = pEdge.getObject();
    Integer value = pEdge.getValue();

    assert getObjectFromMemoryLocation(pLocation) == edgeObject : "SMG object at location "
        + pLocation.getAsSimpleString() + " is unexpected object";

    if (!getValues().contains(value)) {
      addValue(value);
    }

    addHasValueEdge(pEdge);

    if (pPtEdges.containsKey(value)) {
      SMGEdgePointsTo ptEdge = pPtEdges.get(value);

      // FIXME does not keep consistency
      if (!getPTEdges().containsKey(value)) {
        addPointsToEdge(ptEdge);
      }
    }
  }

  public Set<MemoryLocation> getTrackedMemoryLocations() {

    Set<MemoryLocation> result = new HashSet<>();

    for (SMGEdgeHasValue hvedge : getHVEdges()) {
      result.add(resolveMemLoc(hvedge));
    }

    return result;
  }

  private MemoryLocation resolveMemLoc(SMGEdgeHasValue hvEdge) {

    SMGObject object = hvEdge.getObject();
    long offset = hvEdge.getOffset();

    if (isHeapObject(object) || (object instanceof SMGRegion &&
        global_objects.containsValue(object))) {
      return MemoryLocation.valueOf(object.getLabel(), offset);
    } else {

      CLangStackFrame frame = getStackFrameOfObject(object);

      String functionName = frame.getFunctionDeclaration().getName();

      return MemoryLocation.valueOf(functionName, object.getLabel(), offset);
    }
  }

  /**
   * This function is used to obtain memory location from point-to SMG edge.
   *
   * @param ptEdge point-to edge
   * @return memory location of corresponding SMG object
   */
  private MemoryLocation resolveMemLoc(SMGEdgePointsTo ptEdge) {
    SMGObject object = ptEdge.getObject();
    int offset = ptEdge.getOffset();
    if (isHeapObject(object) || (object instanceof SMGRegion &&
        global_objects.containsValue(object))) {
      return MemoryLocation.valueOf(object.getLabel(), offset);
    } else {
      CLangStackFrame frame = getStackFrameOfObject(object);
      String functionName = frame.getFunctionDeclaration().getName();
      return MemoryLocation.valueOf(functionName, object.getLabel(), offset);
    }
  }

  private CLangStackFrame getStackFrameOfObject(SMGObject pObject) {

    String regionLabel = pObject.getLabel();

    for (CLangStackFrame frame : stack_objects) {
      if ((frame.containsVariable(regionLabel)
          && frame.getVariable(regionLabel) == pObject)
          || pObject == frame.getReturnObject()) {

        return frame;
      }
    }

    throw new AssertionError("object " + pObject.getLabel() + " is not on a function stack");
  }

  public MemoryLocation resolveMemLoc(SMGAddress pValue, String pFunctionName) {
    SMGObject object = pValue.getObject();
    long offset = pValue.getOffset().getAsLong();

    if (global_objects.containsValue(object) || isHeapObject(object)) {
      return MemoryLocation.valueOf(object.getLabel(), offset);
    } else {
      return MemoryLocation.valueOf(pFunctionName, object.getLabel(), offset);
    }
  }

  @Nullable
  public CType getTypeForMemoryLocation(MemoryLocation pMemoryLocation) {
    Set<SMGEdgeHasValue> edgesToForget = getHVEdgeFromMemoryLocation(pMemoryLocation);

    if (edgesToForget.isEmpty()) {
      return null;
    }

    return edgesToForget.iterator().next().getType();
  }
}
