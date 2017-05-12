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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.MachineModel.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.solver.api.FormulaType;

import java.util.logging.Level;


public class CtoFormulaTypeHandler {

  protected final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;

  private final BaseSizeofVisitor sizeofVisitor;

  private final FormulaType<?> pointerType;

  public CtoFormulaTypeHandler(LogManager pLogger, MachineModel pMachineModel) {
    logger = new LogManagerWithoutDuplicates(pLogger);
    machineModel = pMachineModel;

    sizeofVisitor = new BaseSizeofVisitor(pMachineModel);

    final int pointerSize = machineModel.getSizeofPtr();
    final int bitsPerByte = machineModel.getSizeofCharInBits();
    pointerType = FormulaType.getBitvectorTypeWithSize(pointerSize * bitsPerByte);
  }

  /**
   * Returns the size in bytes of the given type.
   * Always use this method instead of machineModel.getSizeOf,
   * because this method can handle dereference-types.
   *
   * @param pType the type to calculate the size of.
   * @return the size in bytes of the given type.
   */
  public int getSizeof(CType pType) {
    int size = pType.accept(sizeofVisitor);
    if (size == 0) {
      CType type = pType.getCanonicalType();
      if (type instanceof CArrayType) {
        // C11 §6.7.6.2 (1)
        logger.logOnce(Level.WARNING, "Type", pType, "is a zero-length array, this is undefined.");
      } else if (type instanceof CCompositeType) {
        // UNDEFINED: http://stackoverflow.com/questions/1626446/what-is-the-size-of-an-empty-struct-in-c
        logger.logOnce(Level.WARNING, "Type", pType, "has no fields, this is undefined.");
      } else {
        logger.logOnce(Level.WARNING, "Type", pType, "has size 0 bytes.");
      }
    }
    return size;
  }

  public FormulaType<?> getFormulaTypeFromCType(CType type) {
    if (type instanceof CSimpleType) {
      CSimpleType simpleType = (CSimpleType) type;
      switch (simpleType.getType()) {
        case FLOAT:
          return FormulaType.getSinglePrecisionFloatingPointType();
        case DOUBLE:
          return FormulaType.getDoublePrecisionFloatingPointType();
        default:
          break;
      }
    }

    int byteSize = getSizeof(type);

    int bitsPerByte = machineModel.getSizeofCharInBits();
    // byte to bits
    return FormulaType.getBitvectorTypeWithSize(byteSize * bitsPerByte);
  }

  public FormulaType<?> getPointerType() {
    return pointerType;
  }
}
