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
package org.sosy_lab.cpachecker.cpa.shape.util;

import com.google.common.base.Objects;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstantSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.util.collections.map.PersistentMaps;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Some objects, such as heap memory allocated by malloc(), usually have symbolic size. Since the
 * size of object is represented
 */
public class ObjectSizeInfo {

  private PersistentMap<SGObject, SymbolicExpression> objectLength;

  public ObjectSizeInfo() {
    objectLength = PathCopyingPersistentTreeMap.of();
  }

  public void putAll(ObjectSizeInfo info) {
    objectLength = PersistentMaps.copy(info.objectLength);
  }

  public void addObject(SGObject pObject, @Nonnull SymbolicExpression se) {
    if (se instanceof ConstantSE) {
      switch (se.getValueKind()) {
        // If the size is explicit, we should directly update the size of corresponding shape
        // graph object.
        // If the size is unknown, then we don't have any idea on the object size.
        case EXPLICIT:
        case UNKNOWN:
          return;
      }
    }
    objectLength = objectLength.putAndCopy(pObject, se);
  }

  public void removeObject(SGObject pObject) {
    // DISCUSSION: it is unnecessary to prune this structure, because object length table keeps
    // consistent with the objects in the shape graph. When we are about to remove an object, we
    // consistently remove the entry in the table if exists.
    objectLength = objectLength.removeAndCopy(pObject);
  }

  @Nullable
  public SymbolicExpression getLength(SGObject pObject) {
    return objectLength.get(pObject);
  }

  public Map<SGObject, SymbolicExpression> getObjectLength() {
    return Collections.unmodifiableMap(objectLength);
  }

  /* ************* */
  /* miscellaneous */
  /* ************* */

  /**
   * Merge two symbolic values if they are proved to be identical.
   *
   * @param pV  the replacing value
   * @param pV1 the value (possibly) to be replaced
   * @param pV2 the value (possibly) to be replaced
   */
  public void mergeValues(Long pV, Long pV1, Long pV2) {
    KnownSymbolicValue newV = KnownSymbolicValue.valueOf(pV);
    Set<KnownSymbolicValue> oldVs = new HashSet<>();
    if (!pV.equals(pV1)) {
      oldVs.add(KnownSymbolicValue.valueOf(pV1));
    }
    if (!pV.equals(pV2)) {
      oldVs.add(KnownSymbolicValue.valueOf(pV2));
    }
    assert (!oldVs.isEmpty());
    for (Entry<SGObject, SymbolicExpression> entry : objectLength.entrySet()) {
      SGObject key = entry.getKey();
      SymbolicExpression se = entry.getValue();
      ReducerResult result = CoreShapeAdapter.getInstance().mergeValues(se, newV, oldVs);
      if (result.getChangeFlag()) {
        se = (SymbolicExpression) result.getExpression();
        objectLength = objectLength.putAndCopy(key, se);
      }
    }
  }

  /* ********* */
  /* overrides */
  /* ********* */

  @Override
  public int hashCode() {
    return Objects.hashCode(objectLength);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof ObjectSizeInfo)) {
      return false;
    }
    ObjectSizeInfo that = (ObjectSizeInfo) obj;
    return objectLength.equals(that.objectLength);
  }
}
