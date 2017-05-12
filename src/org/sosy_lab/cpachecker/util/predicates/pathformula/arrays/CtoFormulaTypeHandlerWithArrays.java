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
package org.sosy_lab.cpachecker.util.predicates.pathformula.arrays;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.solver.api.FormulaType;


public class CtoFormulaTypeHandlerWithArrays extends CtoFormulaTypeHandler {

  public CtoFormulaTypeHandlerWithArrays(LogManager pLogger, MachineModel pMachineModel) {
    super(pLogger, pMachineModel);
  }

  @Override
  public FormulaType<?> getFormulaTypeFromCType(CType pType) {
    if (pType instanceof CArrayType) {
      final CArrayType at = (CArrayType) pType;
      FormulaType<?> arrayIndexType = getFormulaTypeFromCType(
          machineModel.getPointerEquivalentSimpleType()); // TODO: Is this correct?
      FormulaType<?> arrayElementType = getFormulaTypeFromCType(at.getType());
      return FormulaType.getArrayType(arrayIndexType, arrayElementType);
    }

    return super.getFormulaTypeFromCType(pType);
  }
}
