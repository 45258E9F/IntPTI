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
package org.sosy_lab.cpachecker.cpa.shape.values;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class is to represent a field.
 */
public final class ShapeField {

  /**
   * The offset of this field in the relevant memory object.
   */
  private final ShapeExplicitValue offset;

  /**
   * The type of the field.
   */
  private final CType type;

  /**
   * Constructor.
   *
   * @param pOffset the offset in the owner object
   * @param pType   the type of the field reference
   */
  public ShapeField(ShapeExplicitValue pOffset, CType pType) {
    offset = Preconditions.checkNotNull(pOffset);
    type = Preconditions.checkNotNull(pType);
  }

  private static final ShapeField UNKNOWN = new ShapeField(UnknownValue.getInstance(), new
      CProblemType("unknown"));

  public ShapeExplicitValue getOffset() {
    return offset;
  }

  public CType getType() {
    return type;
  }

  public boolean isUnknown() {
    return offset.isUnknown() || type instanceof CProblemType;
  }

  public static ShapeField getUnknownInstance() {
    return UNKNOWN;
  }

  @Override
  public String toString() {
    return "offset: " + offset + " type: " + type.toASTString("");
  }
}
