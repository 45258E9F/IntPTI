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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import com.google.common.base.Predicate;

import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;

import java.io.Serializable;

import javax.annotation.Nonnull;

public class PointerTargetPattern implements Serializable, Predicate<PointerTarget> {

  protected PointerTargetPattern() {
    this.matchRange = false;
  }

  protected PointerTargetPattern(final String base) {
    this.base = base;
    this.containerOffset = 0;
    this.properOffset = 0;
    this.matchRange = false;
  }

  /**
   * Create PointerTargetPattern matching any possible target.
   */
  public static PointerTargetPattern any() {
    return new PointerTargetPattern();
  }

  /**
   * Create PointerTargetPattern matching targets in the memory block with the specified base name
   * and offset 0.
   *
   * @param base the base name specified
   */
  public static PointerTargetPattern forBase(String base) {
    return new PointerTargetPattern(base);
  }

  public static PointerTargetPattern forLeftHandSide(
      final CLeftHandSide lhs,
      final CtoFormulaTypeHandler pTypeHandler,
      final PointerTargetSetManager pPtsMgr,
      final CFAEdge pCfaEdge,
      final PointerTargetSetBuilder pPts) throws UnrecognizedCCodeException {
    LvalueToPointerTargetPatternVisitor v =
        new LvalueToPointerTargetPatternVisitor(pTypeHandler, pPtsMgr, pCfaEdge, pPts);
    return lhs.accept(v);
  }

  public void setBase(final String base) {
    this.base = base;
  }

  public void setRange(final int startOffset, final int size) {
    this.containerOffset = startOffset;
    this.properOffset = startOffset + size;
    this.matchRange = true;
    this.containerType = null;
  }

  public void setRange(final int size) {
    assert containerOffset != null && properOffset != null : "Starting address is inexact";
    this.containerOffset += properOffset;
    this.properOffset = containerOffset + size;
    this.matchRange = true;
    this.containerType = null;
  }

  public void setProperOffset(final int properOffset) {
    assert !matchRange : "Contradiction in target pattern: properOffset";
    this.properOffset = properOffset;
  }

  public Integer getProperOffset() {
    assert !matchRange : "Contradiction in target pattern: properOffset";
    return properOffset;
  }

  public Integer getRemainingOffset(PointerTargetSetManager ptsMgr) {
    assert !matchRange : "Contradiction in target pattern: remaining offset";
    if (containerType != null && containerOffset != null && properOffset != null) {
      return ptsMgr.getSize(containerType) - properOffset;
    } else {
      return null;
    }
  }

  /**
   * Increase containerOffset by properOffset, unset properOffset and set containerType.
   * Useful for array subscript visitors.
   */
  public void shift(final CType containerType) {
    assert !matchRange : "Contradiction in target pattern: shift";
    this.containerType = containerType;
    if (containerOffset != null) {
      if (properOffset != null) {
        containerOffset += properOffset;
      } else {
        containerOffset = null;
      }
    }
    properOffset = null;
  }

  /**
   * Increase containerOffset by properOffset, set properOffset and containerType.
   * Useful for field access visitors.
   */
  public void shift(final CType containerType, final int properOffset) {
    shift(containerType);
    this.properOffset = properOffset;
  }

  /**
   * Unset everything, except base
   */
  public void retainBase() {
    assert !matchRange : "Contradiction in target pattern: retainBase";
    containerType = null;
    properOffset = null;
    containerOffset = null;
  }

  /**
   * Unset all criteria
   */
  public void clear() {
    assert !matchRange : "Contradiction in target pattern: clear";
    base = null;
    containerType = null;
    properOffset = null;
    containerOffset = null;
  }

  public boolean matches(final @Nonnull PointerTarget target) {
    if (!matchRange) {
      if (properOffset != null && properOffset != target.properOffset) {
        return false;
      }
      if (containerOffset != null && containerOffset != target.containerOffset) {
        return false;
      }
      if (base != null && !base.equals(target.base)) {
        return false;
      }
      if (containerType != null && !containerType.equals(target.containerType)) {
        if (!(containerType instanceof CArrayType)
            || !(target.containerType instanceof CArrayType)) {
          return false;
        } else {
          return ((CArrayType) containerType).getType()
              .equals(((CArrayType) target.containerType).getType());
        }
      }
    } else {
      final int offset = target.containerOffset + target.properOffset;
      if (offset < containerOffset || offset >= properOffset) {
        return false;
      }
      if (base != null && !base.equals(target.base)) {
        return false;
      }
    }
    return true;
  }

  @Override
  @Deprecated // call matches(), it has a better name
  public boolean apply(PointerTarget pInput) {
    return matches(pInput);
  }

  public boolean isExact() {
    return base != null && containerOffset != null && properOffset != null;
  }

  public boolean isSemiExact() {
    return containerOffset != null && properOffset != null;
  }

  private String base = null;
  private CType containerType = null;
  private Integer properOffset = null;
  private Integer containerOffset = null;

  private boolean matchRange = false;

  private static final long serialVersionUID = -2918663736813010025L;
}
