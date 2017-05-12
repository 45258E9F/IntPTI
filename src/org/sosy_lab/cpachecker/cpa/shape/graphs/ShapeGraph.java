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
package org.sosy_lab.cpachecker.cpa.shape.graphs;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstraintRepresentation;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdgeFilter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGPointToEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.util.ConstraintsPool;
import org.sosy_lab.cpachecker.cpa.shape.util.EqualityRelation;
import org.sosy_lab.cpachecker.cpa.shape.util.EquivalenceRelation;
import org.sosy_lab.cpachecker.cpa.shape.util.InequalityRelation;
import org.sosy_lab.cpachecker.cpa.shape.util.ObjectSizeInfo;

import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ShapeGraph {

  private Set<SGObject> objects = new HashSet<>();
  protected Set<Long> values = new HashSet<>();
  Set<SGHasValueEdge> HVEdges = new HashSet<>();
  Map<Long, SGPointToEdge> PTEdges = new HashMap<>();

  private Map<SGObject, Boolean> validity = new HashMap<>();
  Map<SGObject, Long> refCounts = new HashMap<>();

  protected final MachineModel machineModel;

  private final static SGObject nullObject = SGObject.getNullObject();
  private final static long nullAddress = 0;

  protected EqualityRelation eq = new EqualityRelation();

  protected InequalityRelation neq = new InequalityRelation();
  protected ConstraintsPool constraints = new ConstraintsPool();

  ObjectSizeInfo sizeInfo = new ObjectSizeInfo();

  public ShapeGraph(final MachineModel pMachineModel) {
    SGPointToEdge nullPointer = new SGPointToEdge(nullAddress, nullObject, 0);
    addObject(nullObject);
    validity.put(nullObject, false);
    addValue(nullAddress);
    addPointToEdge(nullPointer);
    machineModel = pMachineModel;
  }

  public ShapeGraph(final ShapeGraph pMemory) {
    machineModel = pMemory.machineModel;
    HVEdges.addAll(pMemory.HVEdges);
    eq.putAll(pMemory.eq);
    neq.putAll(pMemory.neq);
    constraints.putAll(pMemory.constraints);
    validity.putAll(pMemory.validity);
    refCounts.putAll(pMemory.refCounts);
    objects.addAll(pMemory.objects);
    PTEdges.putAll(pMemory.PTEdges);
    values.addAll(pMemory.values);
    sizeInfo.putAll(pMemory.sizeInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(machineModel, HVEdges, eq, neq, constraints, validity, refCounts,
        objects, PTEdges, values, sizeInfo);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ShapeGraph other = (ShapeGraph) obj;
    return machineModel == other.machineModel &&
        Objects.equals(HVEdges, other.HVEdges) &&
        Objects.equals(eq, other.eq) &&
        Objects.equals(neq, other.neq) &&
        Objects.equals(constraints, other.constraints) &&
        Objects.equals(validity, other.validity) &&
        Objects.equals(refCounts, other.refCounts) &&
        Objects.equals(objects, other.objects) &&
        Objects.equals(PTEdges, other.PTEdges) &&
        Objects.equals(values, other.values) &&
        Objects.equals(sizeInfo, other.sizeInfo);
  }

  /* ***************** */
  /* modifying methods */
  /* ***************** */

  public final void addObject(final SGObject pObject, final boolean pValidity) {
    objects.add(pObject);
    validity.put(pObject, pValidity);
  }

  public final void addObject(final SGObject pObject) {
    addObject(pObject, true);
  }

  public final void removeObject(final SGObject pObject) {
    objects.remove(pObject);
    validity.remove(pObject);
    refCounts.remove(pObject);
    sizeInfo.removeObject(pObject);
  }

  public final void addValue(final Long pValue) {
    values.add(pValue);
  }

  public final void removeValue(final Long pValue) {
    values.remove(pValue);
  }

  public final void addPointToEdge(SGPointToEdge pEdge) {
    PTEdges.put(pEdge.getValue(), pEdge);
  }

  public void addHasValueEdge(SGHasValueEdge pEdge) {
    HVEdges.add(pEdge);
  }

  public void removeHasValueEdge(SGHasValueEdge pEdge) {
    HVEdges.remove(pEdge);
  }

  public void removeObjectAndEdges(final SGObject pObject) {
    removeObject(pObject);
    // then remove relevant edges
    Iterator<SGHasValueEdge> HViter = HVEdges.iterator();
    Iterator<SGPointToEdge> PTiter = PTEdges.values().iterator();
    while (HViter.hasNext()) {
      SGHasValueEdge hvEdge = HViter.next();
      if (hvEdge.getObject() == pObject) {
        Long value = hvEdge.getValue();
        SGObject target = getObjectPointedBy(value);
        if (target != null && target != nullObject) {
          decRef(target);
        }
        HViter.remove();
      }
    }
    while (PTiter.hasNext()) {
      if (PTiter.next().getObject() == pObject) {
        PTiter.remove();
      }
    }
  }

  /**
   * A mutation of {@link #removeObjectAndEdges(SGObject)}, which stores objects that may become
   * unreachable after object removal.
   */
  public void removeObjectAndEdges(final SGObject pObject, final Set<SGObject> pObserved) {
    removeObject(pObject);
    Iterator<SGHasValueEdge> hvIt = HVEdges.iterator();
    Iterator<SGPointToEdge> ptIt = PTEdges.values().iterator();
    while (hvIt.hasNext()) {
      SGHasValueEdge hvEdge = hvIt.next();
      if (hvEdge.getObject() == pObject) {
        Long value = hvEdge.getValue();
        SGObject target = getObjectPointedBy(value);
        if (target != null && target != nullObject) {
          decRef(target);
          // target object may become unreachable after this point
          pObserved.add(target);
        }
        hvIt.remove();
      }
    }
    while (ptIt.hasNext()) {
      if (ptIt.next().getObject() == pObject) {
        ptIt.remove();
      }
    }
  }

  /**
   * A mutation of {@link #removeObjectAndEdges(SGObject)}, which collects removed has-value
   * edges and point-to edges. This method is designed specifically for reallocation.
   */
  public void removeObjectAndEdges(
      final SGObject pObject, final Set<SGHasValueEdge> pHVEdges,
      final Set<SGPointToEdge> pPTEdges) {
    removeObject(pObject);
    Iterator<SGHasValueEdge> hvIt = HVEdges.iterator();
    Iterator<SGPointToEdge> ptIt = PTEdges.values().iterator();
    while (hvIt.hasNext()) {
      SGHasValueEdge hvEdge = hvIt.next();
      if (hvEdge.getObject() == pObject) {
        Long value = hvEdge.getValue();
        SGObject target = getObjectPointedBy(value);
        if (target != null && target != nullObject) {
          decRef(target);
        }
        hvIt.remove();
        pHVEdges.add(hvEdge);
      }
    }
    while (ptIt.hasNext()) {
      SGPointToEdge ptEdge = ptIt.next();
      if (ptEdge.getObject() == pObject) {
        ptIt.remove();
        pPTEdges.add(ptEdge);
      }
    }
  }

  public void setValidity(SGObject pObject, boolean pValidity) {
    if (!objects.contains(pObject)) {
      // WARNING: current object is not in the shape graph
      return;
    }
    validity.put(pObject, pValidity);
  }

  void incRef(SGObject pObject) {
    Long refCount = refCounts.get(pObject);
    if (refCount != null) {
      refCount++;
      refCounts.put(pObject, refCount);
    } else {
      refCounts.put(pObject, (long) 1);
    }
  }

  void decRef(SGObject pObject) {
    Long refCount = refCounts.get(pObject);
    if (refCount != null) {
      refCount--;
      if (refCount < 0) {
        refCount = 0L;
      }
      refCounts.put(pObject, refCount);
    }
  }

  /**
   * Note: It is generally not recommended to manually set the new reference counter for an
   * object. This method is designed for state merge only.
   */
  public void setRef(SGObject pObject, long newRef) {
    if (refCounts.containsKey(pObject)) {
      refCounts.put(pObject, (newRef < 0) ? 0 : newRef);
    }
  }

  public void resetRef(SGObject pObject) {
    if (refCounts.containsKey(pObject)) {
      refCounts.put(pObject, 0L);
    }
  }

  /**
   * Note: this function is only used in join operation
   */
  public void replaceHVSet(Set<SGHasValueEdge> pNewHV) {
    HVEdges.clear();
    HVEdges.addAll(pNewHV);
  }

  public void addNeqRelation(Long pV1, Long pV2) {
    neq.addRelation(pV1, pV2);
  }

  public void addConstraint(SymbolicExpression pSE) {
    constraints.push(pSE);
  }

  public void addConstraints(Collection<SymbolicExpression> pSes) {
    for (SymbolicExpression se : pSes) {
      constraints.push(se);
    }
  }

  public void addDisjunction(Collection<ConstraintRepresentation> pCrs) {
    constraints.addDisjunction(pCrs);
  }

  public void addEqualities(EquivalenceRelation<Long> pEqs) {
    eq.putAll(pEqs);
  }

  public void addSizeInfo(SGObject pObject, @Nonnull SymbolicExpression pSize) {
    sizeInfo.addObject(pObject, pSize);
  }

  /**
   * After-branch condition consists of branch-specific symbolic expressions and the disjunction
   * if available.
   */
  public Set<SymbolicExpression> getSimpleAfterBranchCondition() {
    return constraints.getAfterBranchCondition();
  }

  public Set<ConstraintRepresentation> getAfterBranchCondition() {
    ImmutableSet.Builder<ConstraintRepresentation> builder = ImmutableSet.builder();
    for (SymbolicExpression se : constraints.getAfterBranchCondition()) {
      builder.add(se);
    }
    ConstraintRepresentation disjunction = constraints.getDisjunction();
    if (disjunction != null) {
      builder.add(disjunction);
    }
    return builder.build();
  }

  public void setSimpleAfterBranchCondition(Set<SymbolicExpression> pConditions) {
    constraints.setAfterBranchCondition(pConditions);
  }

  /* *********************************************** */
  /* The following methods do not modify shape graph */
  /* *********************************************** */

  public static SGObject getNullObject() {
    return nullObject;
  }

  public static long getNullAddress() {
    return nullAddress;
  }

  final String valuesToString() {
    return "values = " + values.toString();
  }

  final String HVToString() {
    return "have-value = " + HVEdges.toString();
  }

  final String PTToString() {
    return "point-to = " + PTEdges.toString();
  }

  public final Set<Long> getValues() {
    return Collections.unmodifiableSet(values);
  }

  @Nullable
  public final CType getTypeForValue(long v) {
    for (SGHasValueEdge edge : HVEdges) {
      if (edge.getValue() == v) {
        return edge.getType();
      }
    }
    return null;
  }

  public final Set<SGObject> getObjects() {
    return Collections.unmodifiableSet(objects);
  }

  public final Set<SGHasValueEdge> getHVEdges() {
    return Collections.unmodifiableSet(HVEdges);
  }

  public final Set<SGHasValueEdge> getHVEdges(SGHasValueEdgeFilter pFilter) {
    return Collections.unmodifiableSet(pFilter.filterSet(HVEdges));
  }

  public final Map<Long, SGPointToEdge> getPTEdges() {
    return Collections.unmodifiableMap(PTEdges);
  }

  @Nullable
  public final SGObject getObjectPointedBy(Long pValue) {
    if (!values.contains(pValue)) {
      return null;
    }
    if (PTEdges.containsKey(pValue)) {
      return PTEdges.get(pValue).getObject();
    } else {
      return null;
    }
  }

  public final boolean isObjectValid(SGObject pObject) {
    if (!objects.contains(pObject)) {
      throw new IllegalArgumentException("Object [" + pObject + "] not in shape graph");
    }
    return validity.get(pObject);
  }

  final boolean isZeroRef(SGObject pObject) {
    Long refCount = refCounts.get(pObject);
    return (refCount != null && refCount == 0);
  }

  /**
   * Note: In general it is not recommended to call this method to get the concrete reference
   * counter. This method is designed for state merge only.
   */
  public final long getRef(SGObject pObject) {
    Long ref = refCounts.get(pObject);
    return (ref == null) ? 0 : ref;
  }

  public final MachineModel getMachineModel() {
    return machineModel;
  }

  public boolean isPointer(Long pV) {
    return PTEdges.containsKey(pV);
  }

  public SGPointToEdge getPointer(Long pV) {
    return PTEdges.get(pV);
  }

  /**
   * Merging two symbolic values and return the merged value which has the lowest identifier in
   * the certain equivalence class.
   */
  public void mergeValues(long pV1, long pV2) {
    boolean exists = eq.addRelation(pV1, pV2);
    if (!exists) {
      long v = eq.getRepresentative(pV1);
      if (v != pV1) {
        // pV1 should be replaced with v
        replaceValue(v, pV1);
      }
      if (v != pV2) {
        replaceValue(v, pV2);
      }
      sizeInfo.mergeValues(v, pV1, pV2);
    }
  }

  private void replaceValue(long newValue, long oldValue) {
    neq.mergeValues(newValue, oldValue);
    removeValue(oldValue);
    Set<SGHasValueEdge> newHVEdges = new HashSet<>();
    for (SGHasValueEdge hv : HVEdges) {
      if (hv.getValue() != oldValue) {
        newHVEdges.add(hv);
      } else {
        newHVEdges.add(new SGHasValueEdge(hv.getType(), hv.getOffset(), hv.getObject(), newValue));
      }
    }
    HVEdges.clear();
    HVEdges.addAll(newHVEdges);
  }

  public long getRepresentative(long pV) {
    return eq.getRepresentative(pV);
  }

  public EquivalenceRelation<Long> getEq() {
    return eq.getEq();
  }

  public boolean isNeq(long pV1, long pV2) {
    return neq.relationExists(pV1, pV2);
  }

  public Multimap<Long, Long> getNeq() {
    return neq.getNeq();
  }

  public List<SymbolicExpression> getConstraints() {
    return constraints.getConstraints();
  }

  public int getSizeOfConstraints() {
    return constraints.size();
  }

  public SymbolicExpression getConstraintOn(int index) {
    return constraints.get(index);
  }

  @Nullable
  public ConstraintRepresentation getDisjunction() {
    return constraints.getDisjunction();
  }

  public Multimap<Long, Integer> getNeedCheckingDefiniteAssignmentSet() {
    return constraints.getNeedCheckingDefiniteAssignmentSet();
  }

  public void setConstraintsPool(ConstraintsPool thatPool) {
    constraints.putAll(thatPool);
  }

  public ConstraintsPool getConstraintsPool() {
    return constraints;
  }

  @Nullable
  public SymbolicExpression getSizeForObject(SGObject pObject) {
    return sizeInfo.getLength(pObject);
  }

  public Map<SGObject, SymbolicExpression> getObjectLength() {
    return sizeInfo.getObjectLength();
  }

  /**
   * Count the valid reference from the given point-to edge set.
   */
  public long countValidRef(Set<SGPointToEdge> ptEdges) {
    Set<Long> pointers = FluentIterable.from(ptEdges).transform(
        new Function<SGPointToEdge, Long>() {
          @Override
          public Long apply(SGPointToEdge pSGPointToEdge) {
            return pSGPointToEdge.getValue();
          }
        }).toSet();
    Set<Long> hasValues = FluentIterable.from(HVEdges).transform(
        new Function<SGHasValueEdge, Long>() {
          @Override
          public Long apply(SGHasValueEdge pSGHasValueEdge) {
            return pSGHasValueEdge.getValue();
          }
        }).toSet();
    return Sets.intersection(pointers, hasValues).size();
  }

  /* ******************* */
  /* byte-level analysis */
  /* ******************* */

  /**
   * This method should only be called when the associated memory object of has-value edge is not
   * zero-initialized.
   */
  public boolean isCoveredByNullifiedBlocks(SGHasValueEdge pEdge) {
    return isCoveredByNullifiedBlocks(pEdge.getObject(), pEdge.getOffset(), pEdge.getSizeInBytes
        (machineModel));
  }

  private boolean isCoveredByNullifiedBlocks(SGObject pObject, int pOffset, int pSize) {
    BitSet nullBytes = getNullBytesFor(pObject);
    int expectedClear = pOffset + pSize;
    int nextClear = nullBytes.nextClearBit(pOffset);
    // FIX: if `nextClear` has value -1, then all remaining bits are set
    return nextClear >= expectedClear || nextClear < 0;
  }

  /**
   * Derive a bit-set signifying where the object bytes are nullified.
   */
  public BitSet getNullBytesFor(SGObject pObject) {
    // To support the memory object with uncertain size, we should first traverse the filtered
    // has-value edges and then compute the largest offset required.
    SGHasValueEdgeFilter filter;
    BitSet bits = new BitSet();
    // the object SHOULD NOT be zero-initialized
    assert (!pObject.isZeroInit());
    filter = SGHasValueEdgeFilter.objectFilter(pObject).filterHavingValue(nullAddress);
    Set<SGHasValueEdge> edges = getHVEdges(filter);
    bits.clear();
    for (SGHasValueEdge edge : edges) {
      bits.set(edge.getOffset(), edge.getOffset() + edge.getSizeInBytes(machineModel));
    }
    return bits;
  }

  /**
   * This method should only be called when the associated memory object of has-value edge is
   * zero-initialized.
   */
  public boolean isTaintedByNonNullBlocks(SGHasValueEdge pEdge) {
    return isTaintedByNonNullBlocks(pEdge.getObject(), pEdge.getOffset(), pEdge.getSizeInBytes
        (machineModel));
  }

  private boolean isTaintedByNonNullBlocks(SGObject pObject, int pOffset, int pSize) {
    BitSet nonnullBytes = getNonNullBytesFor(pObject);
    int shouldNotTaint = pOffset + pSize;
    int nextTaint = nonnullBytes.nextSetBit(pOffset);
    return nextTaint >= 0 && nextTaint < shouldNotTaint;
  }

  public BitSet getNonNullBytesFor(SGObject pObject) {
    SGHasValueEdgeFilter filter;
    BitSet bits = new BitSet();
    // the object SHOULD be zero-initialized
    assert (pObject.isZeroInit());
    filter = SGHasValueEdgeFilter.objectFilter(pObject).filterNotHavingValue(nullAddress);
    Set<SGHasValueEdge> edges = getHVEdges(filter);
    bits.clear();
    // pollute bit-vector with respect to non-null segments
    for (SGHasValueEdge edge : edges) {
      bits.set(edge.getOffset(), edge.getOffset() + edge.getSizeInBytes(machineModel));
    }
    // since has-value edges can be overlapped, we should add flip bits of null edges
    filter = filter.filterNotHavingValue(nullAddress);
    Set<SGHasValueEdge> zeroEdges = getHVEdges(filter);
    for (SGHasValueEdge zeroEdge : zeroEdges) {
      bits.clear(zeroEdge.getOffset(),
          zeroEdge.getOffset() + zeroEdge.getSizeInBytes(machineModel));
    }
    return bits;
  }

}
