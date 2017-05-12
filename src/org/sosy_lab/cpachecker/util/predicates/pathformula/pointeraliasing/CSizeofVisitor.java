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

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.MachineModel.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;

public class CSizeofVisitor extends BaseSizeofVisitor
    implements CTypeVisitor<Integer, IllegalArgumentException> {

  public CSizeofVisitor(
      final MachineModel machineModel,
      final FormulaEncodingWithPointerAliasingOptions options) {
    super(machineModel);
    this.options = options;
  }

  @Override
  public Integer visit(final CArrayType t) throws IllegalArgumentException {
    Integer length = CTypeUtils.getArrayLength(t);

    if (length == null) {
      length = options.defaultArrayLength();
    }

    final int sizeOfType = t.getType().accept(this);
    return length * sizeOfType;
  }

  private final FormulaEncodingWithPointerAliasingOptions options;
}
