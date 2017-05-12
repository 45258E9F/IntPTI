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
package org.sosy_lab.cpachecker.cpa.pointer2.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.pointer2.Pointer2State;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.FieldAccessSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;
import org.sosy_lab.cpachecker.util.access.PointerDereferenceSegment;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AccessSummaryApplicator {

  private final MachineModel machineModel;

  private static AccessSummaryApplicator instance;

  private AccessSummaryApplicator(MachineModel pMachineModel) {
    machineModel = pMachineModel;
  }

  public static void initialize(MachineModel pMachineModel) {
    instance = new AccessSummaryApplicator(pMachineModel);
  }

  public static AccessSummaryApplicator getInstance() {
    if (instance == null) {
      throw new IllegalArgumentException("Access summary applicator should be initialized first");
    }
    return instance;
  }

  public Pointer2State applySummary(Pointer2State pState, AccessPath pWritePath)
      throws UnrecognizedCCodeException {
    Pointer2State newState = pState;
    Collection<MemoryLocation> writtenLocations = getMemoryLocationsFor(pState, pWritePath);
    for (MemoryLocation writtenLoc : writtenLocations) {
      newState = newState.setPointsToInformation(writtenLoc, LocationSetTop.INSTANCE);
    }
    return newState;
  }

  public Pointer2State applySummary(Pointer2State pState, LocationSet pLocSet, CType pType) {
    Pointer2State newState = pState;
    if (pLocSet.isBot() || pLocSet.isTop()) {
      return newState;
    }
    Set<MemoryLocation> decomposed = decomposeLocations(Sets.newHashSet((ExplicitLocationSet)
        pLocSet), pType);
    for (MemoryLocation writtenLoc : decomposed) {
      newState = newState.setPointsToInformation(writtenLoc, LocationSetTop.INSTANCE);
    }
    return newState;
  }

  /**
   * Convert an access path into a set of memory locations.
   * @param pPath access path
   * @return set of memory locations covered by the given access path
   */
  private Collection<MemoryLocation> getMemoryLocationsFor(Pointer2State pState, AccessPath pPath) {
    String qualifiedName = pPath.getQualifiedName();
    MemoryLocation initLoc = MemoryLocation.valueOf(qualifiedName);
    return getCoveredMemoryLocations(pState, initLoc, pPath.getType(), pPath.afterFirstPath());
  }

  private Collection<MemoryLocation> getCoveredMemoryLocations(
      Pointer2State pState, MemoryLocation pInitLoc, CType pInitType,
      List<PathSegment> remSegments) {
    CType currentType = pInitType;
    Set<MemoryLocation> result = new HashSet<>();
    result.add(pInitLoc);
    for (PathSegment segment : remSegments) {
      if (segment instanceof PointerDereferenceSegment) {
        CPointerType pointerType = Types.extractPointerType(currentType);
        CArrayType arrayType = Types.extractArrayType(currentType);
        if ((pointerType == null) == (arrayType == null)) {
          return Collections.emptySet();
        }
        if (arrayType != null) {
          currentType = arrayType.getType();
          continue;
        }
        // otherwise, the current type is of pointer
        currentType = pointerType.getType();
        Set<MemoryLocation> newResult = new HashSet<>();
        for (MemoryLocation subLoc : result) {
          LocationSet subTarget = pState.getPointsToSet(subLoc);
          if (subTarget.isBot() || subTarget.isTop()) {
            // TODO: unsound analysis due to discarding top element
            continue;
          }
          Iterables.addAll(newResult, (ExplicitLocationSet) subTarget);
        }
        result = newResult;
      } else if (segment instanceof FieldAccessSegment) {
        String fieldName = segment.getName();
        Pair<Long, CType> fieldInfo = Types.getFieldInfo(currentType, fieldName, machineModel);
        Long delta = fieldInfo.getFirst();
        CType newType = fieldInfo.getSecond();
        if (newType == null || delta == null) {
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
          long delta = index * machineModel.getSizeof(currentType);
          for (MemoryLocation subLoc : result) {
            newResult.add(MemoryLocation.withOffset(subLoc, delta));
          }
        } else {
          currentType = pointerType.getType();
          long delta = index * machineModel.getSizeof(currentType);
          for (MemoryLocation subLoc : result) {
            LocationSet subTarget = pState.getPointsToSet(subLoc);
            if (subTarget.isBot() || subTarget.isTop()) {
              continue;
            }
            for (MemoryLocation target : (ExplicitLocationSet) subTarget) {
              newResult.add(MemoryLocation.withOffset(target, delta));
            }
          }
        }
        result = newResult;
      } else {
        // such as '&': we cannot handle them
        return Collections.emptySet();
      }
    }
    // if the current type is composite, we should further decompose the type to derive
    // fine-grained memory locations for points-to relaxation
    return decomposeLocations(result, currentType);
  }

  // Invariant: every memory location in the given set should have the specified type
  private Set<MemoryLocation> decomposeLocations(Set<MemoryLocation> pLocSet, CType pType) {
    Set<MemoryLocation> decomposed = new HashSet<>();
    try {
      decomposed = pType.accept(new DecomposeMemoryLocationVisitor(pLocSet));
    } catch (UnrecognizedCCodeException ignored) {}
    return decomposed;
  }

  private class DecomposeMemoryLocationVisitor
      implements CTypeVisitor<Set<MemoryLocation>, UnrecognizedCCodeException> {

    private Set<MemoryLocation> inputLocSet;

    DecomposeMemoryLocationVisitor(Set<MemoryLocation> pLocSet) {
      inputLocSet = pLocSet;
    }

    public Set<MemoryLocation> visitDefault() {
      return Collections.emptySet();
    }

    @Override
    public Set<MemoryLocation> visit(CArrayType pArrayType) throws UnrecognizedCCodeException {
      return visitDefault();
    }

    @Override
    public Set<MemoryLocation> visit(CCompositeType pCompositeType)
        throws UnrecognizedCCodeException {
      switch (pCompositeType.getKind()) {
        case STRUCT: {
          int offset = 0;
          Set<MemoryLocation> decomposed = new HashSet<>();
          for (CCompositeTypeMemberDeclaration member : pCompositeType.getMembers()) {
            CType memberType = member.getType();
            offset += machineModel.getPadding(offset, memberType);
            Set<MemoryLocation> newLocSet = new HashSet<>();
            for (MemoryLocation loc : inputLocSet) {
              newLocSet.add(MemoryLocation.withOffset(loc, offset));
            }
            newLocSet = decomposeLocations(newLocSet, memberType);
            decomposed.addAll(newLocSet);
            offset += machineModel.getSizeof(memberType);
          }
          return decomposed;
        }
        case UNION: {
          Set<MemoryLocation> decomposed = new HashSet<>();
          for (CCompositeTypeMemberDeclaration member : pCompositeType.getMembers()) {
            CType memberType = member.getType();
            Set<MemoryLocation> subLocSet = decomposeLocations(inputLocSet, memberType);
            decomposed.addAll(subLocSet);
          }
          return decomposed;
        }
        default:
          return Collections.emptySet();
      }
    }

    @Override
    public Set<MemoryLocation> visit(CElaboratedType pElaboratedType)
        throws UnrecognizedCCodeException {
      CComplexType realType = pElaboratedType.getRealType();
      if (realType == null) {
        return Collections.emptySet();
      }
      return realType.accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CEnumType pEnumType) throws UnrecognizedCCodeException {
      return visitDefault();
    }

    @Override
    public Set<MemoryLocation> visit(CFunctionType pFunctionType)
        throws UnrecognizedCCodeException {
      // function pointer
      return inputLocSet;
    }

    @Override
    public Set<MemoryLocation> visit(CPointerType pPointerType) throws UnrecognizedCCodeException {
      // no need to be decomposed further
      return inputLocSet;
    }

    @Override
    public Set<MemoryLocation> visit(CProblemType pProblemType) throws UnrecognizedCCodeException {
      return visitDefault();
    }

    @Override
    public Set<MemoryLocation> visit(CSimpleType pSimpleType) throws UnrecognizedCCodeException {
      return visitDefault();
    }

    @Override
    public Set<MemoryLocation> visit(CTypedefType pTypedefType) throws UnrecognizedCCodeException {
      CType realType = pTypedefType;
      while (realType instanceof CTypedefType) {
        realType = ((CTypedefType) realType).getRealType();
      }
      return realType.accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CVoidType pVoidType) throws UnrecognizedCCodeException {
      return visitDefault();
    }
  }

}
