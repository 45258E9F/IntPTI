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
package org.sosy_lab.cpachecker.cpa.shape.graphs.model;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGRegion;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

public class CStackFrame {

  public static final String RETVAL_LABEL = "___cpa_return_var_";

  private final CFunctionDeclaration function;

  private final Map<String, SGRegion> stack = new HashMap<>();
  private final Set<SGRegion> VLASet = new HashSet<>();

  private final SGRegion returnObject;

  public CStackFrame(CFunctionDeclaration pDeclaration, MachineModel pModel) {
    function = pDeclaration;
    CType returnType = pDeclaration.getType().getReturnType().getCanonicalType();
    if (returnType instanceof CVoidType) {
      returnObject = null;
    } else {
      int size = pModel.getSizeof(returnType);
      returnObject = new SGRegion(CStackFrame.RETVAL_LABEL, returnType, size, SGRegion.STATIC);
    }
  }

  public CStackFrame(CStackFrame pFrame) {
    function = pFrame.function;
    stack.putAll(pFrame.stack);
    VLASet.addAll(pFrame.VLASet);
    returnObject = pFrame.returnObject;
  }

  public void addStackVariable(String pVariable, SGRegion pRegion, @Nullable Integer size) {
    // no need to use qualified name, since stack objects are classified by different function
    // stacks
    if (stack.containsKey(pVariable)) {
      throw new IllegalArgumentException("Stack frame for function '" +
          function.toASTString() + "' already contains a variable '" + pVariable + "'");
    }
    if (size == null) {
      // VLA
      VLASet.add(pRegion);
    }
    stack.put(pVariable, pRegion);
  }

  public void removeStackVariable(SGRegion pRegion) {
    stack.remove(pRegion.getLabel());
    VLASet.remove(pRegion);
  }

  /* *********************** */
  /* Non-modifying functions */
  /* *********************** */

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof CStackFrame)) {
      return false;
    }
    CStackFrame other = (CStackFrame) obj;
    // We do not compare two counters here. If stacks are equal, counters are always equal.
    return this.function.equals(other.function) && this.stack.equals(other.stack) && this
        .returnObject.equals(other.returnObject);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(function, stack, returnObject);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("<");
    for (SGObject region : stack.values()) {
      builder.append(" ").append(region);
    }
    builder.append(" >");
    return builder.toString();
  }

  @Nullable
  public SGRegion getVariable(String pName) {
    return stack.get(pName);
  }

  public boolean containsVariable(String pName) {
    return stack.containsKey(pName);
  }

  public boolean containsObject(SGObject pObject) {
    SGObject hit = stack.get(pObject.getLabel());
    return hit != null && hit == pObject;
  }

  public boolean isVLA(SGRegion pRegion) {
    return VLASet.contains(pRegion);
  }

  public CFunctionDeclaration getFunctionDeclaration() {
    return function;
  }

  public Map<String, SGRegion> getVariables() {
    return Collections.unmodifiableMap(stack);
  }

  public Set<SGObject> getAllObjects() {
    HashSet<SGObject> objects = new HashSet<>();
    objects.addAll(stack.values());
    return Collections.unmodifiableSet(objects);
  }

  /**
   * All the objects in current stack frame are classified for the convenience of removal.
   */
  public Pair<Set<SGObject>, Set<SGObject>> getTriagedObjects() {
    Set<SGObject> removed = new HashSet<>();
    Set<SGObject> VLAs = new HashSet<>();
    removed.addAll(stack.values());
    if (returnObject != null) {
      removed.add(returnObject);
    }
    VLAs.addAll(VLASet);
    removed = Sets.difference(removed, VLAs);
    return Pair.of(removed, VLAs);
  }

  @Nullable
  public SGRegion getReturnObject() {
    return returnObject;
  }

}
