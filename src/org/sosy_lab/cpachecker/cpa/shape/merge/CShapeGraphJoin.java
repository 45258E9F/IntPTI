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
package org.sosy_lab.cpachecker.cpa.shape.merge;

import static org.sosy_lab.cpachecker.cpa.shape.util.ConstraintsPool.unifySymbolicExpressions;

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdgeFilter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGPointToEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.model.CStackFrame;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGRegion;
import org.sosy_lab.cpachecker.cpa.shape.merge.abs.GuardedAbstraction;
import org.sosy_lab.cpachecker.cpa.shape.merge.joiner.SGObjectJoin;
import org.sosy_lab.cpachecker.cpa.shape.merge.joiner.SGPendingJoin;
import org.sosy_lab.cpachecker.cpa.shape.merge.joiner.SGValueJoin;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.BasicValuePair;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.MergeTable;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.PointerInfo;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.SGNodeMapping;
import org.sosy_lab.cpachecker.cpa.shape.util.ConstraintsPool;
import org.sosy_lab.cpachecker.cpa.shape.visitors.value.PointerVisitor;

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

public final class CShapeGraphJoin {

  /**
   * The flag indicating that whether the given two C shape graphs could be merged.
   */
  private boolean defined = false;

  private CShapeGraph opG1;
  private CShapeGraph opG2;
  private CShapeGraph merged;

  private SGNodeMapping mapping1 = new SGNodeMapping();
  private SGNodeMapping mapping2 = new SGNodeMapping();

  private MergeTable table;

  /**
   * Actually, two states to be merged contain their own abstraction info. However, for the
   * sake of efficiency, we store new abstraction info from the scratch and merge it with the
   * original abstraction info in a simple way: discard all old info inconsistent with the new one.
   */
  private GuardedAbstraction abstraction = GuardedAbstraction.of();

  private static final Predicate<String> IS_STRING_LITERAL = new Predicate<String>() {
    @Override
    public boolean apply(String pS) {
      return pS.startsWith(PointerVisitor.LITERAL_PREFIX);
    }
  };

  public CShapeGraphJoin(CShapeGraph pG1, CShapeGraph pG2, MergeTable pTable) {
    // back-up shape graphs, since they could be modified in state merge
    // that's to say, pG1 and pG2 are immutable
    opG1 = new CShapeGraph(pG1);
    opG2 = new CShapeGraph(pG2);

    // create a table for quick value merge
    table = pTable;

    // we incrementally construct the merged shape graph from the scratch
    merged = new CShapeGraph(opG1.getMachineModel());
    Map<String, SGRegion> globalVars1 = opG1.getGlobalObjects();
    Map<String, SGRegion> globalVars2 = opG2.getGlobalObjects();
    Deque<CStackFrame> stackFrames1 = opG1.getStackFrames();
    Deque<CStackFrame> stackFrames2 = opG2.getStackFrames();

    // STEP 0: merge constraints
    merged.addEqualities(table.getMergedEq());
    mergeNeq();
    mergeConstraints();
    pG1.setSimpleAfterBranchCondition(opG1.getSimpleAfterBranchCondition());
    pG2.setSimpleAfterBranchCondition(opG2.getSimpleAfterBranchCondition());

    // merge size info structure
    mergeSizeInfo();

    // STEP 1: merge global variables
    Set<String> globalNames = new HashSet<>();
    globalNames.addAll(globalVars1.keySet());
    globalNames.addAll(globalVars2.keySet());
    for (String globalName : globalNames) {
      SGRegion globalVar1 = globalVars1.get(globalName);
      SGRegion globalVar2 = globalVars2.get(globalName);
      if (globalVar1 == null || globalVar2 == null) {
        // this case occurs when a string literal is created in a specific branch
        continue;
      }
      SGObject joinedVar = globalVar1.join(globalVar2);
      assert (joinedVar instanceof SGRegion);
      SGRegion globalVar = (SGRegion) joinedVar;
      merged.addGlobalObject(globalVar);
      mapping1.put(globalVar1, globalVar);
      mapping2.put(globalVar2, globalVar);
    }

    // STEP 2: merge stack objects
    if (stackFrames1.size() != stackFrames2.size()) {
      return;
    }
    Iterator<CStackFrame> stackIterator1 = stackFrames1.descendingIterator();
    Iterator<CStackFrame> stackIterator2 = stackFrames2.descendingIterator();
    while (stackIterator1.hasNext() && stackIterator2.hasNext()) {
      CStackFrame frame1 = stackIterator1.next();
      CStackFrame frame2 = stackIterator2.next();
      CFunctionDeclaration func1 = frame1.getFunctionDeclaration();
      CFunctionDeclaration func2 = frame2.getFunctionDeclaration();
      if (!func1.equals(func2)) {
        // two stack frame sequences should match
        return;
      }
      merged.addStackFrame(func1);
      Set<String> localNames = new HashSet<>();
      localNames.addAll(frame1.getVariables().keySet());
      localNames.addAll(frame2.getVariables().keySet());
      for (String localName : localNames) {
        SGRegion localVar1 = frame1.getVariable(localName);
        SGRegion localVar2 = frame2.getVariable(localName);
        if (localVar1 == null || localVar2 == null) {
          // This situation occurs when the certain local variable is declared in a code block.
          // If one path goes through the block and another one does not, one frame contains such
          // variable but another frame does not. Such variable is unnecessary to be kept.
          continue;
        }
        SGObject joinedVar = localVar1.join(localVar2);
        if (!(joinedVar instanceof SGRegion)) {
          // two objects may have incompatible types, and we cannot merge them here
          return;
        }
        SGRegion localVar = (SGRegion) joinedVar;
        boolean localVarIsVLA = frame1.isVLA(localVar1);
        if (localVarIsVLA != frame2.isVLA(localVar2)) {
          // one is allocated by __builtin_alloc() but the other is not
          // we can say, two objects have incompatible types here
          return;
        }
        merged.addStackObject(localVar, localVarIsVLA);
        mapping1.put(localVar1, localVar);
        mapping2.put(localVar2, localVar);
      }
    }

    // STEP 3: MERGE WORKS. Merging starts from global and local objects (they are ROOTS) and
    // propagates to heap objects.
    for (Entry<String, SGRegion> globalEntry : merged.getGlobalObjects().entrySet()) {
      String key = globalEntry.getKey();
      SGRegion globalVar1 = globalVars1.get(key);
      SGRegion globalVar2 = globalVars2.get(key);
      SGRegion destination = globalEntry.getValue();
      SGObjectJoin objectJoin = new SGObjectJoin(opG1, opG2, merged,
          mapping1, mapping2,
          globalVar1, globalVar2, destination,
          abstraction,
          table);
      if (!objectJoin.isDefined()) {
        return;
      }
      objectJoinUpdater(objectJoin);
    }
    // We join stack objects from the newest stack frame to the older ones. This is because newer
    // stack frames could have undefined join issue with larger possibility.
    stackIterator1 = stackFrames1.iterator();
    stackIterator2 = stackFrames2.iterator();
    Iterator<CStackFrame> mergedIterator = merged.getStackFrames().iterator();
    while (stackIterator1.hasNext() && stackIterator2.hasNext() && mergedIterator.hasNext()) {
      CStackFrame frame1 = stackIterator1.next();
      CStackFrame frame2 = stackIterator2.next();
      CStackFrame mergedFrame = mergedIterator.next();
      for (Entry<String, SGRegion> localEntry : mergedFrame.getVariables().entrySet()) {
        String localName = localEntry.getKey();
        SGRegion localVar1 = frame1.getVariable(localName);
        SGRegion localVar2 = frame2.getVariable(localName);
        SGRegion destination = localEntry.getValue();
        SGObjectJoin objectJoin = new SGObjectJoin(opG1, opG2, merged,
            mapping1, mapping2,
            localVar1, localVar2, destination,
            abstraction,
            table);
        if (!objectJoin.isDefined()) {
          return;
        }
        objectJoinUpdater(objectJoin);
      }
      // merge return object which is not included in the set of local variables
      SGRegion return1 = frame1.getReturnObject();
      SGRegion return2 = frame2.getReturnObject();
      SGRegion destReturn = mergedFrame.getReturnObject();
      if (destReturn != null) {
        // destination return object could be NULL when the return type is `void`
        SGObjectJoin retJoin = new SGObjectJoin(opG1, opG2, merged,
            mapping1, mapping2,
            return1, return2, destReturn,
            abstraction,
            table);
        if (!retJoin.isDefined()) {
          return;
        }
        objectJoinUpdater(retJoin);
      }
    }
    // three stacks should have the same size
    if (stackIterator1.hasNext() != stackIterator2.hasNext() ||
        stackIterator2.hasNext() != mergedIterator.hasNext()) {
      return;
    }

    // inherit the original abstraction info
    // For any two states to be merged, they share the same path prefix util the branching point.
    // Furthermore, no more abstraction info is generated since the branching point. That means,
    // two states should have consistent abstraction info. However, due to symbolic value merge,
    // two abstractions are not "all the same". We should use equality information to correctly
    // derive their merge results.
    replaceConcreteValues();
    Set<BasicValuePair> merges = new HashSet<>();
    GuardedAbstraction inherited = opG1.getAbstraction().merge(opG1, opG2.getAbstraction(), opG2,
        merges, abstraction);
    abstraction.putAll(inherited);
    for (BasicValuePair pair : merges) {
      Long l = pair.getLeft();
      Long r = pair.getRight();
      merged.mergeValues(l, r);
    }

    // STEP 4: merge dangling objects due to abstraction
    mergeDanglingObjects();

    // STEP 5: add pending objects
    SGPendingJoin pendJoin = new SGPendingJoin(opG1, opG2, merged,
        mapping1, mapping2);
    pendingUpdater(pendJoin);

    // STEP 6: merge point-to edges that are not associated with any other has-value edges
    // Since some address values are forcibly generated, they are not reachable from stack/global
    // objects. We should add those new point-to edges, especially for string literals.
    // O -> (D -> V) where O is object, D is offset and V is value
    mergePointTos();

    // STEP 7: merge other meta-info
    // merge dangling sets
    mergeDanglingSet();
    // memory leak status
    merged.setMemoryLeak(opG1.hasMemoryLeak() || opG2.hasMemoryLeak());
    // merge leak edges
    Set<CFAEdge> edges = Sets.union(opG1.getLeakEdges(), opG2.getLeakEdges());
    merged.setLeakEdges(edges);

    // STEP 8: update new abstraction info into the merged shape graph
    merged.setAbstraction(abstraction);

    // that's all forks
    defined = true;
  }

  /* ***************** */
  /* auxiliary methods */
  /* ***************** */

  private void replaceConcreteValues() {
    abstraction.replaceConcreteValues(mapping1.getReplacement(), mapping2.getReplacement());
  }

  private void mergeDanglingObjects() {
    // Dangling object refers to unshared global objects including string literals and function
    // pointers.
    Map<String, SGRegion> globalObjects1 = opG1.getGlobalObjects();
    Map<String, SGRegion> globalObjects2 = opG2.getGlobalObjects();
    Set<String> names1 = new HashSet<>(globalObjects1.keySet());
    Set<String> names2 = new HashSet<>(globalObjects2.keySet());
    Set<String> commonNames = Sets.intersection(names1, names2);
    for (Entry<String, SGRegion> globalObj1 : globalObjects1.entrySet()) {
      String name = globalObj1.getKey();
      SGRegion r = globalObj1.getValue();
      if (commonNames.contains(name)) {
        continue;
      }
      mapping1.put(r, r);
      // be specific: the merged object is a global object
      merged.addGlobalObject(r);
      merged.setValidity(r, opG1.isObjectValid(r));

      // for a string literal, its has-value edges are required to be inserted
      if (IS_STRING_LITERAL.apply(name)) {
        SGHasValueEdgeFilter filter = SGHasValueEdgeFilter.objectFilter(r);
        for (SGHasValueEdge hvEdge : opG1.getHVEdges(filter)) {
          Long v = hvEdge.getValue();
          merged.addValue(v);
          merged.addHasValueEdge(hvEdge);

        }
      }
    }
    for (Entry<String, SGRegion> globalObj2 : globalObjects2.entrySet()) {
      String name = globalObj2.getKey();
      SGRegion r = globalObj2.getValue();
      if (commonNames.contains(name)) {
        continue;
      }
      mapping2.put(r, r);
      merged.addGlobalObject(r);
      if (IS_STRING_LITERAL.apply(name)) {
        SGHasValueEdgeFilter filter = SGHasValueEdgeFilter.objectFilter(r);
        for (SGHasValueEdge hvEdge : opG2.getHVEdges(filter)) {
          Long v = hvEdge.getValue();
          merged.addValue(v);
          merged.addHasValueEdge(hvEdge);
        }
      }
    }
  }

  private void mergePointTos() {
    Multimap<SGObject, PointerInfo> pendPointerMap1 = HashMultimap.create();
    Multimap<SGObject, PointerInfo> pendPointerMap2 = HashMultimap.create();
    Multimap<SGObject, PointerInfo> absPointerMap1 = HashMultimap.create();
    Multimap<SGObject, PointerInfo> absPointerMap2 = HashMultimap.create();
    for (SGPointToEdge ptEdge1 : opG1.getPTEdges().values()) {
      SGObject object = ptEdge1.getObject();
      if (object == CShapeGraph.getNullObject()) {
        continue;
      }
      SGObject mergedObject = mapping1.get(object);
      PointerInfo ptInfo = PointerInfo.of(ptEdge1.getOffset(), ptEdge1.getValue());
      if (mergedObject != null) {
        // this point-to edge is associated with an existing merged object
        pendPointerMap1.put(mergedObject, ptInfo);
      } else {
        // this point-to edge is possibly associated with abstracted object or leaked object
        absPointerMap1.put(object, ptInfo);
      }
    }
    for (SGPointToEdge ptEdge2 : opG2.getPTEdges().values()) {
      SGObject object = ptEdge2.getObject();
      if (object == CShapeGraph.getNullObject()) {
        continue;
      }
      SGObject mergedObject = mapping2.get(object);
      PointerInfo ptInfo = PointerInfo.of(ptEdge2.getOffset(), ptEdge2.getValue());
      if (mergedObject != null) {
        pendPointerMap2.put(mergedObject, ptInfo);
      } else {
        absPointerMap2.put(object, ptInfo);
      }
    }
    // STEP 1: add pending point-to edges
    Set<SGObject> keys1 = pendPointerMap1.keySet();
    Set<SGObject> keys2 = pendPointerMap2.keySet();
    Set<SGObject> commonKeys = Sets.intersection(keys1, keys2);
    for (SGObject key : commonKeys) {
      Map<Integer, Long> valueMap1 = PointerInfo.toMap(pendPointerMap1.get(key));
      Map<Integer, Long> valueMap2 = PointerInfo.toMap(pendPointerMap2.get(key));
      mergePointToEdges(key, valueMap1, valueMap2);
    }

    Set<SGObject> keysFor1 = Sets.difference(keys1, commonKeys);
    Set<SGObject> keysFor2 = Sets.difference(keys2, commonKeys);
    for (SGObject key1 : keysFor1) {
      Map<Integer, Long> pendMap = PointerInfo.toMap(pendPointerMap1.get(key1));
      for (Entry<Integer, Long> entry : pendMap.entrySet()) {
        Long v = entry.getValue();
        SGPointToEdge ptEdge = new SGPointToEdge(v, key1, entry.getKey());
        merged.addValue(v);
        merged.addPointToEdge(ptEdge);
      }
    }
    for (SGObject key2 : keysFor2) {
      Map<Integer, Long> pendMap = PointerInfo.toMap(pendPointerMap2.get(key2));
      for (Entry<Integer, Long> entry : pendMap.entrySet()) {
        Long v = entry.getValue();
        SGPointToEdge ptEdge = new SGPointToEdge(v, key2, entry.getKey());
        merged.addValue(v);
        merged.addPointToEdge(ptEdge);
      }
    }

    // STEP 2: add dangling objects by the previous abstraction
    Set<SGObject> danglingObjects = Sets.intersection(absPointerMap1.keySet(),
        absPointerMap2.keySet());
    for (SGObject dangling : danglingObjects) {
      // such object should be heap object
      if (opG1.isHeapObject(dangling) && opG2.isHeapObject(dangling)) {
        CFAEdge edge = opG1.getEdgeForHeapObject(dangling);
        assert (edge != null) : "Every heap object should be associated with a CFA edge";
        merged.addHeapObject(dangling, edge);
        SGObjectJoin objectJoin = new SGObjectJoin(opG1, opG2, merged, mapping1, mapping2,
            dangling, dangling, dangling, abstraction, table);
        if (!objectJoin.isDefined()) {
          return;
        }
        objectJoinUpdater(objectJoin);

        // merge point-to edges associated with this object
        Map<Integer, Long> pointerMap1 = PointerInfo.toMap(absPointerMap1.get(dangling));
        Map<Integer, Long> pointerMap2 = PointerInfo.toMap(absPointerMap2.get(dangling));
        mergePointToEdges(dangling, pointerMap1, pointerMap2);

        // update merged validity status and reference count
        boolean validity1 = opG1.isObjectValid(dangling);
        boolean validity2 = opG2.isObjectValid(dangling);
        merged.setValidity(dangling, (validity1 == validity2) && validity1);
        long ref1 = opG1.getRef(dangling);
        long ref2 = opG2.getRef(dangling);
        merged.setRef(dangling, (ref1 > ref2) ? ref2 : ref1);
      }
    }

    // STEP 3: add leaked objects. Such objects can be the witnesses of memory leak. Different
    // from the dangling/pending objects, we do not add any has-value edge or point-to edge for
    // them.
    for (SGObject object : absPointerMap1.keySet()) {
      if (!danglingObjects.contains(object) && opG1.isHeapObject(object)) {
        long ref = opG1.getRef(object);
        if (ref == 0) {
          merged.addHeapObject(object, opG1.getEdgeForHeapObject(object));
          merged.setRef(object, ref);
          merged.setValidity(object, opG1.isObjectValid(object));
        }
      }
    }
    for (SGObject object : absPointerMap2.keySet()) {
      if (!danglingObjects.contains(object) && opG2.isHeapObject(object)) {
        long ref = opG2.getRef(object);
        if (ref == 0) {
          merged.addHeapObject(object, opG2.getEdgeForHeapObject(object));
          merged.setRef(object, ref);
          merged.setValidity(object, opG2.isObjectValid(object));
        }
      }
    }

  }

  /**
   * Merge point-to edges associated with an existing object in the merged shape graph.
   *
   * @param pObject the specified merged object
   * @param ptInfo1 pointer information from S1
   * @param ptInfo2 pointer information from S2
   */
  private void mergePointToEdges(
      SGObject pObject,
      Map<Integer, Long> ptInfo1,
      Map<Integer, Long> ptInfo2) {
    Iterator<Entry<Integer, Long>> iterator1 = ptInfo1.entrySet().iterator();
    Iterator<Entry<Integer, Long>> iterator2 = ptInfo2.entrySet().iterator();
    // Invariant: two iterators should cover at least one element respectively
    Entry<Integer, Long> entry1 = iterator1.next();
    Entry<Integer, Long> entry2 = iterator2.next();
    do {
      // (1) Two entries are NULL, then it should be captured by while-condition.
      // (2) The first entry is NULL, then we directly add point-to edge for the second entry.
      // The symmetry case also applies.
      // (3) Two entries are both non-null, then we first check if two values have been already
      // merged. If not, we perform symbolic merge on them.
      //
      // Consider there is a point-to edge (v, l, O1) from G1 but no (* ,l, O1) from G2, then v
      // must not be an existing value in merged shape graph. Otherwise, v = (v, v), and there
      // exists v in G2 merging with v in G1. Since v is pointer, then the target object O' in
      // G2 is O or O' other than O. For the latter case, we have O -> O1 and O' -> O1 where O1
      // is the merged object in merged shape graph and v has the same offset in G1 and G2 (e.g
      // . l). Therefore, we have (v, l, O1) from G1 and (v, l, O1) from G2. That was a
      // contradiction.
      if (entry1 == null) {
        // Is it possible that v is not the least element in its equivalence class of the
        // merged equivalence relation? No.
        // Proof:
        // Assume that the least element is v' and the equivalence class is [v']. Obviously, we
        // have v \in [v']. Also, [v'] is the intersection of [v']_1 and [v']_2 from G1 and G2
        // respectively. Then, we have v \in [v']_1. However, since v appears in the G1, v must
        // be the least element of its equivalence class (i.e. [v']_1), thus v < v'. That was a
        // contradiction.
        Long v = entry2.getValue();
        SGPointToEdge ptEdge = new SGPointToEdge(v, pObject, entry2.getKey());
        merged.addValue(v);
        merged.addPointToEdge(ptEdge);
        entry2 = iterator2.hasNext() ? iterator2.next() : null;
      } else if (entry2 == null) {
        Long v = entry1.getValue();
        SGPointToEdge ptEdge = new SGPointToEdge(v, pObject, entry1.getKey());
        merged.addValue(v);
        merged.addPointToEdge(ptEdge);
        entry1 = iterator1.hasNext() ? iterator1.next() : null;
      } else {
        int offset1 = entry1.getKey();
        int offset2 = entry2.getKey();
        Long v1 = entry1.getValue();
        Long v2 = entry2.getValue();
        if (offset1 == offset2) {
          Long v = SGValueJoin.mergeSymbolicValues(v1, v2, opG1, opG2, merged, abstraction,
              table);
          SGPointToEdge ptEdge = new SGPointToEdge(v, pObject, offset1);
          merged.addPointToEdge(ptEdge);
          entry1 = iterator1.hasNext() ? iterator1.next() : null;
          entry2 = iterator2.hasNext() ? iterator2.next() : null;
        } else if (offset1 < offset2) {
          // increase the offset1
          SGPointToEdge ptEdge = new SGPointToEdge(v1, pObject, offset1);
          merged.addValue(v1);
          merged.addPointToEdge(ptEdge);
          entry1 = iterator1.hasNext() ? iterator1.next() : null;
        } else {
          // increase the offset2
          SGPointToEdge ptEdge = new SGPointToEdge(v2, pObject, offset2);
          merged.addValue(v2);
          merged.addPointToEdge(ptEdge);
          entry2 = iterator2.hasNext() ? iterator2.next() : null;
        }
      }
    } while (entry1 != null || entry2 != null);
  }

  /**
   * Add inequality information plus new info to the merged state.
   * Invariant: for each inequality (p,q) inserted in the merged state, we have: both p and q are
   * in the merged state. (In other words, we do not introduce new values in this phase)
   */
  private void mergeNeq() {
    // we create new copies to prevent corruption of the original inequalities
    Multimap<Long, Long> neq1 = TreeMultimap.create();
    for (Entry<Long, Long> entry1 : opG1.getNeq().entries()) {
      Long key = entry1.getKey();
      Long value = entry1.getValue();
      if (key < value) {
        neq1.put(key, value);
      }
    }
    Multimap<Long, Long> neq2 = opG2.getNeq();
    for (Entry<Long, Long> entry1 : neq1.entries()) {
      Long l1 = entry1.getKey();
      Long r1 = entry1.getValue();
      boolean matched = false;
      Set<Long> l2Candidates = table.getEquivalentValuesFromRel1ToRel2(l1);
      for (Long l2Candidate : l2Candidates) {
        Collection<Long> r2Candidates = neq2.get(l2Candidate);
        for (Long r2Candidate : r2Candidates) {
          Long mV = unifyValues(r1, r2Candidate);
          if (mV != null) {
            merged.addNeqRelation(table.merge(l1, l2Candidate), mV);
            matched = true;
            break;
          }
        }
        if (matched) {
          break;
        }
      }
    }
  }

  /**
   * Unifying two symbolic values.
   *
   * @param pV1 value from G1
   * @param pV2 value from G2
   * @return unified value or NULL
   */
  @Nullable
  private Long unifyValues(Long pV1, Long pV2) {
    // STEP 1: derive common inequalities by explicitly extending equivalence relation
    Long v = table.merge(pV1, pV2);
    if (v != null) {
      return v;
    }
    return null;
  }

  /**
   * Add consistent path constraints the merged state.
   * Invariant: constraints from G1 contain representatives and explicit literals only.
   * Important observation: two states should share a set of path conditions since they share the
   * path prefix. Therefore, we attempt to unify constraints one by one and when a failed
   * unification is encountered, we discard the remaining constraints.
   */
  private void mergeConstraints() {
    ConstraintsPool pool1 = opG1.getConstraintsPool();
    ConstraintsPool pool2 = opG2.getConstraintsPool();
    merged.setConstraintsPool(pool1.merge(pool2, table));
  }

  /**
   * Merge size info for objects.
   * Shared objects should share the same size (so their size expression can be unified). This is
   * because they are created before the branching point.
   * For unshared objects, their size info are directly inherited and it does not corrupt the
   * consistency.
   */
  private void mergeSizeInfo() {
    Map<SGObject, SymbolicExpression> sizeInfo1 = opG1.getObjectLength();
    Map<SGObject, SymbolicExpression> sizeInfo2 = opG2.getObjectLength();
    Set<SGObject> commonObjects = Sets.intersection(sizeInfo1.keySet(), sizeInfo2.keySet());
    Set<SGObject> exclusives1 = Sets.difference(sizeInfo1.keySet(), commonObjects);
    Set<SGObject> exclusives2 = Sets.difference(sizeInfo2.keySet(), commonObjects);
    // STEP 1: unify shared objects
    for (SGObject commonObject : commonObjects) {
      SymbolicExpression se1 = sizeInfo1.get(commonObject);
      SymbolicExpression se2 = sizeInfo2.get(commonObject);
      SymbolicExpression se = unifySymbolicExpressions(se1, se2, table);
      if (se == null) {
        // in general, this case should not happen
        se = se1;
      }
      merged.addSizeInfo(commonObject, se);
    }
    // STEP 2: add exclusive objects
    for (SGObject obj1 : exclusives1) {
      merged.addSizeInfo(obj1, sizeInfo1.get(obj1));
    }
    for (SGObject obj2 : exclusives2) {
      merged.addSizeInfo(obj2, sizeInfo2.get(obj2));
    }
  }

  private void mergeDanglingSet() {
    // Dangling set is used to preserve dynamically allocated data in the stack frame. Thus, it
    // is OK even when the dangling set is always empty. However, in order to pass tests in
    // Juliet test suite benchmark, dangling set is necessary.
    Set<SGObject> dangling1 = opG1.getDanglingSet();
    Set<SGObject> dangling2 = opG2.getDanglingSet();
    // The resultant dangling set should be, in spirit, the union of two dangling sets. Consider
    // O1 -> O1', O2 -> O2'. If O1' = O2', then O1 = O1' = O2' = O2. By the premise of merging,
    // O1 and O2 should be both VLA or not. If O1' \= O2', then O1' = O1 is a pending object and
    // so is O2. Thus, if O1 is a dangling object, O1' should also be a dangling object (it is
    // also a VLA).
    Set<SGObject> newDangling = new HashSet<>();
    for (SGObject object : dangling1) {
      SGObject t = mapping1.get(object);
      if (t != null) {
        newDangling.add(t);
      }
    }
    for (SGObject object : dangling2) {
      SGObject t = mapping2.get(object);
      if (t != null) {
        newDangling.add(t);
      }
    }
    merged.setDanglingSet(newDangling);
  }

  /* ******** */
  /* updaters */
  /* ******** */

  private void objectJoinUpdater(SGObjectJoin joiner) {
    opG1 = joiner.getShapeGraph1();
    opG2 = joiner.getShapeGraph2();
    merged = joiner.getDestGraph();
    mapping1 = joiner.getMapping1();
    mapping2 = joiner.getMapping2();
    abstraction = joiner.getAbstraction();
  }

  private void pendingUpdater(SGPendingJoin joiner) {
    merged = joiner.getDestGraph();
    mapping1 = joiner.getMapping1();
    mapping2 = joiner.getMapping2();
  }

  /* ******* */
  /* getters */
  /* ******* */

  public boolean isDefined() {
    return defined;
  }

  public CShapeGraph getMerged() {
    return merged;
  }

  public SGNodeMapping getMapping1() {
    return mapping1;
  }

  public SGNodeMapping getMapping2() {
    return mapping2;
  }

  public GuardedAbstraction getAbstraction() {
    return abstraction;
  }

}
