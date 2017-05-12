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

import javax.annotation.Nullable;

public final class IntegerFix {

  public enum IntegerFixMode {
    SPECIFIER,
    // alter the declared type specifier
    SANITYCHECK,
    // insert sanity check on specified integer type
    CAST,           // cast the value to the specified type
  }

  private IntegerFixMode mode;
  // the target type can be null, when there does not exist an integer type capable to contain
  // all possible values of certain expression
  private CSimpleType targetType;

  public IntegerFix(IntegerFixMode pMode, @Nullable CSimpleType pTargetType) {
    mode = pMode;
    targetType = pTargetType;
  }

  public IntegerFixMode getFixMode() {
    return mode;
  }

  @Nullable
  public CSimpleType getTargetType() {
    return targetType;
  }

  public IntegerFix merge(IntegerFix other, MachineModel model) {
    if (mode == other.mode) {
      if (targetType == null || other.targetType == null) {
        return new IntegerFix(mode, null);
      } else {
        return new IntegerFix(mode, CTypes.mergeType(model, targetType, other.targetType));
      }
    }
    // we handle merge of fixes under different modes elsewhere
    return this;
  }

}
