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
package org.sosy_lab.cpachecker.core.bugfix.instance.integer;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.core.phase.fix.util.CastFixMetaInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class IntegerFix {

  public enum IntegerFixMode {
    // alter the declared type specifier
    SPECIFIER("SPECIFIER", 3),
    // insert sanity check for conversion
    CHECK_CONV("CHECK_CONV", 2),
    // insert sanity check for arithmetic operation
    CHECK_ARITH("CHECK_ARITH", 1),
    // cast the value to the specified type
    CAST("CAST", 0);

    private final String name;
    // fixes with higher priority should be applied in priority
    private final int priority;

    IntegerFixMode(String pName, int pPriority) {
      name = pName;
      priority = pPriority;
    }

    public String getName() {
      return name;
    }

    public int getPriority() {
      return priority;
    }

  }

  private IntegerFixMode mode;
  // the target type can be null, when there does not exist an integer type capable to contain
  // all possible values of certain expression
  private CSimpleType targetType;

  // only applicable for cast fix
  private final CastFixMetaInfo meta;

  public IntegerFix(IntegerFixMode pMode, @Nullable CSimpleType pTargetType) {
    mode = pMode;
    targetType = pTargetType;
    meta = null;
  }

  IntegerFix(@Nonnull CSimpleType pTargetType, @Nonnull CastFixMetaInfo pMeta) {
    mode = IntegerFixMode.CAST;
    targetType = pTargetType;
    meta = pMeta;
  }

  private IntegerFix(IntegerFixMode pMode, @Nullable CSimpleType pTargetType, @Nullable
                     CastFixMetaInfo pMeta) {
    mode = pMode;
    targetType = pTargetType;
    meta = pMeta;
  }

  public IntegerFixMode getFixMode() {
    return mode;
  }

  @Nullable
  public CSimpleType getTargetType() {
    return targetType;
  }

  @Nullable
  public CastFixMetaInfo getMeta() {
    return meta;
  }

  public IntegerFix merge(IntegerFix other, MachineModel model) {
    if (mode == other.mode) {
      CSimpleType mergedType;
      if (targetType == null || other.targetType == null) {
        mergedType = null;
      } else {
        mergedType = CTypes.mergeType(model, targetType, other.targetType);
      }
      CastFixMetaInfo mergedMeta = (meta != null) ? meta : (other.meta);
      return new IntegerFix(mode, mergedType, mergedMeta);
    }
    // we handle merge of fixes under different modes elsewhere
    return this;
  }

}
