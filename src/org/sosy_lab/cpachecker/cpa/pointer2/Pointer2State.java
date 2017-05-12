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
package org.sosy_lab.cpachecker.cpa.pointer2;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.SwitchableGraphable;
import org.sosy_lab.cpachecker.core.interfaces.summary.SummaryAcceptableState;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessExternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessFunctionInstance;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessInternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.cpa.pointer2.util.AccessSummaryApplicator;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetBot;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;
import org.sosy_lab.cpachecker.cpa.shape.visitors.value.PointerVisitor;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Pointer2State is a basic state class for pointer analysis. No summary relevant information
 * included here.
 */
public class Pointer2State implements AbstractState, SwitchableGraphable, SummaryAcceptableState {

  static final Pointer2State INITIAL_STATE = new Pointer2State();

  private final PersistentSortedMap<MemoryLocation, LocationSet> pointsToMap;

  private Pointer2State() {
    pointsToMap = PathCopyingPersistentTreeMap.of();
  }

  private Pointer2State(PersistentSortedMap<MemoryLocation, LocationSet> pPointsToMap) {
    pointsToMap = pPointsToMap;
  }

  Pointer2State(Pointer2State pState) {
    pointsToMap = pState.pointsToMap;
  }

  Pointer2State addPointsToInformation(MemoryLocation pSource, MemoryLocation pTarget) {
    LocationSet prevPTSet = getPointsToSet(pSource);
    LocationSet newPTSet = prevPTSet.addElement(pTarget);
    return new Pointer2State(pointsToMap.putAndCopy(pSource, newPTSet));
  }

  Pointer2State addPointsToInformation(
      MemoryLocation pSource, Iterable<MemoryLocation>
      pTargets) {
    LocationSet prevPTSet = getPointsToSet(pSource);
    LocationSet newPTSet = prevPTSet.addElements(pTargets);
    return new Pointer2State(pointsToMap.putAndCopy(pSource, newPTSet));
  }

  Pointer2State addPointsToInformation(MemoryLocation pSource, LocationSet pTargets) {
    if (pTargets.isBot()) {
      return this;
    }
    if (pTargets.isTop()) {
      return new Pointer2State(pointsToMap.putAndCopy(pSource, LocationSetTop.INSTANCE));
    }
    LocationSet prevPTSet = getPointsToSet(pSource);
    return new Pointer2State(pointsToMap.putAndCopy(pSource, prevPTSet.addElements(pTargets)));
  }

  // this method is designed for state merge only
  Pointer2State mergePointsToInformation(MemoryLocation pSource, LocationSet pTargets) {
    if (pTargets.isBot()) {
      return this;
    }
    if (pTargets.isTop()) {
      return new Pointer2State(pointsToMap.putAndCopy(pSource, LocationSetTop.INSTANCE));
    }
    assert (pTargets instanceof ExplicitLocationSet);
    LocationSet prevPTSet = getPointsToSet(pSource);
    boolean tainted = !pTargets.equals(prevPTSet);
    LocationSet newPTSet = ((ExplicitLocationSet) pTargets).addElements(prevPTSet, tainted);
    return new Pointer2State(pointsToMap.putAndCopy(pSource, newPTSet));
  }

  public Pointer2State setPointsToInformation(MemoryLocation pSource, LocationSet pTargets) {
    if (pTargets.isBot()) {
      return new Pointer2State(pointsToMap.removeAndCopy(pSource));
    } else {
      return new Pointer2State(pointsToMap.putAndCopy(pSource, pTargets));
    }
  }

  public LocationSet getPointsToSet(MemoryLocation pKey) {
    LocationSet result = pointsToMap.get(pKey);
    if (result == null) {
      return LocationSetBot.INSTANCE;
    }
    return result;
  }

  @Nullable
  Boolean pointsTo(MemoryLocation pSource, MemoryLocation pTarget) {
    LocationSet pointsToSet = getPointsToSet(pSource);
    if (pointsToSet.equals(LocationSetBot.INSTANCE)) {
      return false;
    }
    if (pointsToSet instanceof ExplicitLocationSet) {
      ExplicitLocationSet explicitLocationSet = (ExplicitLocationSet) pointsToSet;
      if (explicitLocationSet.mayPointTo(pTarget)) {
        return explicitLocationSet.getSize() == 1 ? true : null;
      } else {
        return false;
      }
    }
    return null;
  }

  boolean definitelyPointsTo(MemoryLocation pSource, MemoryLocation pTarget) {
    return Boolean.TRUE.equals(pointsTo(pSource, pTarget));
  }

  boolean definitelyNotPointsTo(MemoryLocation pSource, MemoryLocation pTarget) {
    return Boolean.FALSE.equals(pointsTo(pSource, pTarget));
  }

  boolean mayPointTo(MemoryLocation pSource, MemoryLocation pTarget) {
    return !Boolean.FALSE.equals(pointsTo(pSource, pTarget));
  }

  Set<MemoryLocation> getKnownLocations() {
    return FluentIterable.from(Iterables
        .concat(pointsToMap.keySet(), FluentIterable.from(pointsToMap.values())
            .transformAndConcat(new Function<LocationSet, Iterable<? extends MemoryLocation>>() {

              @Override
              public Iterable<? extends MemoryLocation> apply(LocationSet pArg0) {
                if (pArg0 instanceof ExplicitLocationSet) {
                  return (ExplicitLocationSet) pArg0;
                }
                return Collections.emptySet();
              }

            }))).toSet();
  }

  Map<MemoryLocation, LocationSet> getPointsToMap() {
    return Collections.unmodifiableMap(this.pointsToMap);
  }

  @Override
  public int hashCode() {
    return pointsToMap.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || !(obj == null || !(obj instanceof Pointer2State)) && pointsToMap
        .equals(((Pointer2State) obj).pointsToMap);
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }

  @Override
  public String toString() {
    return pointsToMap.toString();
  }

  @Override
  public String toDOTLabel() {
    return toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  Pointer2State dropFrame(String pFunctionName) {
    Pointer2State state = this;
    for (MemoryLocation keyLoc : pointsToMap.keySet()) {
      if (keyLoc.getFunctionName().equals(pFunctionName)) {
        state = new Pointer2State(state.pointsToMap.removeAndCopy(keyLoc));
      } else {
        LocationSet value = state.pointsToMap.get(keyLoc);
        assert (value != null);
        LocationSet newValue = dropLocation(value, pFunctionName);
        if (!value.equals(newValue)) {
          state = new Pointer2State(state.pointsToMap.putAndCopy(keyLoc, newValue));
        }
      }
    }
    return state;
  }

  @Override
  public boolean getActiveStatus() {
    return false;
  }

  private LocationSet dropLocation(LocationSet pLocationSet, String pFunctionName) {
    if (pLocationSet.isTop() || pLocationSet.isBot()) {
      return pLocationSet;
    }
    if (pLocationSet instanceof ExplicitLocationSet) {
      ExplicitLocationSet expLocSet = (ExplicitLocationSet) pLocationSet;
      LocationSet newLocSet = expLocSet;
      for (MemoryLocation loc : expLocSet) {
        if (loc.getFunctionName().equals(pFunctionName)) {
          newLocSet = newLocSet.removeElement(loc);
        }
      }
      return newLocSet;
    }
    // Could we reach here?
    return pLocationSet;
  }

  /* ************************* */
  /* location-type association */
  /* ************************* */

  @Nullable
  private static CType getType(String name) {
    if (name.startsWith(PointerVisitor.LITERAL_PREFIX)) {
      return CPointerType.POINTER_TO_CONST_CHAR;
    } else if (name.equals(MemoryLocation.NULL_OBJECT.getDeclaredName())) {
      return CPointerType.POINTER_TO_VOID;
    }
    CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().orNull();
    if (cfaInfo == null) {
      return null;
    }
    return cfaInfo.getType(name);
  }

  private static boolean isDynamicAllocName(String name) {
    return name.startsWith(Pointer2FunctionAdapter.MALLOC_PREFIX);
  }

  /* ****************** */
  /* strengthen methods */
  /* ****************** */

  public Set<AccessPath> getPointsToTargetForAccessPath(AccessPath path, MachineModel model) {
    // the input access path should;
    // (1) no uncertain array index segment,
    // (2) only one dereference operation at the end (dereference operations include * and [])
    if (path == null) {
      return Collections.emptySet();
    }
    Set<MemoryLocation> locations = asLocationSet(path, model);
    if (locations.isEmpty()) {
      return Collections.emptySet();
    }
    List<CType> typeList = path.parseTypeList();
    CType targetType = typeList.get(typeList.size() - 1);
    Set<AccessPath> result = new HashSet<>();
    for (MemoryLocation location : locations) {
      result.addAll(fromMemoryLocationToAccessPath(location, targetType, model));
    }
    return result;
  }

  private Set<MemoryLocation> asLocationSet(AccessPath pPath, MachineModel pMachineModel) {
    String name = pPath.getQualifiedName();
    MemoryLocation currentLoc = MemoryLocation.valueOf(name);
    return getMemoryLocationFromSegments(currentLoc, pPath.getType(), pPath.afterFirstPath(),
        pMachineModel);
  }

  private Set<MemoryLocation> getMemoryLocationFromSegments(
      MemoryLocation pLocation, CType
      pType, List<PathSegment> pSegments, MachineModel pModel) {
    CType currentType = pType;
    Set<MemoryLocation> result = new HashSet<>();
    result.add(pLocation);
    for (PathSegment segment : pSegments) {
      if (segment instanceof PointerDereferenceSegment) {
        CPointerType pointerType = Types.extractPointerType(currentType);
        CArrayType arrayType = Types.extractArrayType(currentType);
        if ((pointerType == null) == (arrayType == null)) {
          return Collections.emptySet();
        }
        if (arrayType != null) {
          // *arr is equivalent to arr[0], thus memory locations should keep unchanged
          currentType = arrayType.getType();
          continue;
        }
        // if we reach here, pointerType should be non-null
        currentType = pointerType.getType();
        Set<MemoryLocation> newResult = new HashSet<>();
        for (MemoryLocation subLoc : result) {
          LocationSet subTargets = getPointsToSet(subLoc);
          if (subTargets.isBot() || subTargets.isTop()) {
            // if the current memory location points to everything, we do not know what the
            // dereference of certain location would be, and return an empty set of memory
            // locations as result
            continue;
          }
          Iterables.addAll(newResult, (ExplicitLocationSet) subTargets);
        }
        result = newResult;
      } else if (segment instanceof FieldAccessSegment) {
        String fieldName = segment.getName();
        Pair<Long, CType> fieldInfo = Types.getFieldInfo(currentType, fieldName, pModel);
        Long delta = fieldInfo.getFirst();
        CType newType = fieldInfo.getSecond();
        if (delta == null || newType == null) {
          return Collections.emptySet();
        }
        currentType = newType;
        Set<MemoryLocation> newResult = new HashSet<>();
        for (MemoryLocation subLoc : result) {
          newResult.add(MemoryLocation.withOffset(subLoc, delta));
        }
        result = newResult;
      } else if (segment instanceof ArrayConstIndexSegment) {
        long index = ((ArrayConstIndexSegment) segment).getIndex();
        CPointerType pointerType = Types.extractPointerType(currentType);
        CArrayType arrayType = Types.extractArrayType(currentType);
        if ((pointerType == null) == (arrayType == null)) {
          return Collections.emptySet();
        }
        Set<MemoryLocation> newResult = new HashSet<>();
        if (arrayType != null) {
          currentType = arrayType.getType();
          long delta = index * pModel.getSizeof(currentType);
          for (MemoryLocation subLoc : result) {
            newResult.add(MemoryLocation.withOffset(subLoc, delta));
          }
        } else {
          currentType = pointerType.getType();
          long delta = index * pModel.getSizeof(currentType);
          for (MemoryLocation subLoc : result) {
            LocationSet subTargets = getPointsToSet(subLoc);
            if (subTargets.isBot() || subTargets.isTop()) {
              continue;
            }
            for (MemoryLocation target : (ExplicitLocationSet) subTargets) {
              newResult.add(MemoryLocation.withOffset(target, delta));
            }
          }
        }
        result = newResult;
      } else {
        throw new UnsupportedOperationException("unsupported segment: " + segment);
      }
    }
    return result;

  }

  private Set<AccessPath> fromMemoryLocationToAccessPath(
      MemoryLocation pLocation, CType targetType,
      MachineModel pModel) {
    String locName = pLocation.getDeclaredName();
    CType baseType = getType(locName);
    long offset = pLocation.getOffset();
    if (baseType == null) {
      // missing type information, which is unexpected generally
      return Collections.emptySet();
    }
    CVariableDeclaration var = new CVariableDeclaration(
        FileLocation.DUMMY,
        !pLocation.isOnFunctionStack(),
        CStorageClass.AUTO,
        baseType,
        pLocation.getIdentifier(),
        pLocation.getIdentifier(),
        pLocation.getDeclaredName(),
        null
    );
    AccessPath newPath = new AccessPath(var);
    boolean isDynamic = isDynamicAllocName(locName);
    if (isDynamic) {
      // we should append the array index segment after declaration segment
      baseType = Types.dereferenceType(baseType);
      int unitLength = pModel.getSizeof(baseType);
      long index = offset / unitLength;
      offset = offset % unitLength;
      newPath.appendSegment(new ArrayConstIndexSegment(index));
    }
    return constructAccessPath(newPath, baseType, offset, targetType, pModel);
  }

  private Set<AccessPath> constructAccessPath(
      AccessPath path, CType currentType, long offset,
      CType targetType, MachineModel machineModel) {
    if (currentType instanceof CTypedefType) {
      return constructAccessPath(path, ((CTypedefType) currentType).getRealType(), offset,
          targetType, machineModel);
    }
    if (offset == 0 && Types.isEquivalent(currentType, targetType)) {
      return Collections.singleton(path);
    } else if (offset != 0 &&
        (currentType instanceof CEnumType || currentType instanceof CPointerType ||
            currentType instanceof CSimpleType || currentType instanceof CVoidType)) {
      // current data type cannot be composed any further
      return Collections.emptySet();
    }
    if (currentType instanceof CProblemType) {
      return Collections.emptySet();
    }
    if (currentType instanceof CArrayType) {
      CType elementType = ((CArrayType) currentType).getType();
      int elementSize = machineModel.getSizeof(elementType);
      long index = offset / elementSize;
      long newOffset = offset % elementSize;
      AccessPath newPath = AccessPath.copyOf(path);
      newPath.appendSegment(new ArrayConstIndexSegment(index));
      return constructAccessPath(newPath, elementType, newOffset, targetType, machineModel);
    }
    if (currentType instanceof CCompositeType) {
      CCompositeType compositeType = (CCompositeType) currentType;
      switch (compositeType.getKind()) {
        case STRUCT:
          return handleStruct(path, compositeType, offset, targetType, machineModel);
        case UNION:
          return handleUnion(path, compositeType, offset, targetType, machineModel);
        case ENUM:
        default:
          throw new UnsupportedOperationException("unsupported composite type: " + compositeType);
      }
    }
    if (currentType instanceof CElaboratedType) {
      CType defType = ((CElaboratedType) currentType).getRealType();
      if (defType != null) {
        return constructAccessPath(path, defType, offset, targetType, machineModel);
      }
      return Collections.emptySet();
    }
    return Collections.emptySet();
  }

  private Set<AccessPath> handleStruct(
      AccessPath path, CCompositeType compositeType, long
      offset, CType targetType, MachineModel machineModel) {
    int position = 0;
    for (CCompositeTypeMemberDeclaration member : compositeType.getMembers()) {
      CType memberType = member.getType();
      position = machineModel.getPadding(position, memberType);
      int sizeofMember = machineModel.getSizeof(memberType);
      long delta = offset - position;
      if (delta >= 0 && delta < sizeofMember) {
        // enter this field
        if (delta + machineModel.getSizeof(targetType) > sizeofMember) {
          return Collections.emptySet();
        }
        String memberName = member.getName();
        AccessPath newPath = AccessPath.copyOf(path);
        newPath.appendSegment(new FieldAccessSegment(memberName));
        return constructAccessPath(newPath, memberType, delta, targetType, machineModel);
      }
      position += sizeofMember;
    }
    return Collections.emptySet();
  }

  private Set<AccessPath> handleUnion(
      AccessPath path, CCompositeType compositeType, long offset,
      CType targetType, MachineModel machineModel) {
    Set<AccessPath> results = new HashSet<>();
    for (CCompositeTypeMemberDeclaration member : compositeType.getMembers()) {
      CType memberType = member.getType();
      int sizeofMember = machineModel.getSizeof(memberType);
      if (offset >= 0 && offset < sizeofMember) {
        if (offset + machineModel.getSizeof(targetType) > sizeofMember) {
          continue;
        }
        AccessPath newPath = AccessPath.copyOf(path);
        String memberName = member.getName();
        newPath.appendSegment(new FieldAccessSegment(memberName));
        results.addAll(constructAccessPath(newPath, memberType, offset, targetType, machineModel));
      }
    }
    return results;
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
    Pointer2State newState = this;
    CFunctionCall call = (CFunctionCall) ((FunctionReturnEdge) outEdge).getSummaryEdge()
        .getExpression();
    for (SummaryInstance summary : pSummaryList) {
      if (summary instanceof AccessFunctionInstance) {
        List<AccessPath> writtenPaths = ((AccessFunctionInstance) summary).apply().writes;
        for (AccessPath writtenPath : writtenPaths) {
          newState = AccessSummaryApplicator.getInstance().applySummary(newState, writtenPath);
        }
        // LHS of function assignment
        if (call instanceof CFunctionCallAssignmentStatement) {
          CLeftHandSide lhs = ((CFunctionCallAssignmentStatement) call).getLeftHandSide();
          CType leftType = lhs.getExpressionType().getCanonicalType();
          MachineModel model = Preconditions.checkNotNull(GlobalInfo.getInstance().getCFAInfo()
              .orNull()).getCFA().getMachineModel();
          LocationSet locSet = Pointer2TransferRelation.asLocations(newState, pOtherStates, lhs, 0,
              model);
          newState = AccessSummaryApplicator.getInstance().applySummary(newState, locSet, leftType);
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
    Multimap<CFAEdge, Pointer2State> resultMap = HashMultimap.create();
    for (CFAEdge outEdge : outEdges) {
      resultMap.put(outEdge, this);
    }
    for (SummaryInstance summary : pSummaryList) {
      Multimap<CFAEdge, Pointer2State> newResultMap = HashMultimap.create();
      if (summary instanceof AccessExternalLoopInstance) {
        for (CFAEdge outEdge : outEdges) {
          List<AccessPath> writtenPaths = ((AccessExternalLoopInstance) summary).apply(inEdge,
              outEdge).writes;
          for (Pointer2State state : resultMap.get(outEdge)) {
            Pointer2State newState = state;
            for (AccessPath writtenPath : writtenPaths) {
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
    Pointer2State newState = this;
    for (SummaryInstance summary : pSummaryList) {
      if (summary instanceof AccessInternalLoopInstance) {
        List<AccessPath> writtenPaths = ((AccessInternalLoopInstance) summary).apply(inEdge).writes;
        for (AccessPath writtenPath : writtenPaths) {
          newState = AccessSummaryApplicator.getInstance().applySummary(newState, writtenPath);
        }
      } else {
        // unsupported summaries
        continue;
      }
    }
    return Collections.singleton(newState);
  }
}
