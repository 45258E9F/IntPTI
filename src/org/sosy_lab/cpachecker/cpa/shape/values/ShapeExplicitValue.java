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
package org.sosy_lab.cpachecker.cpa.shape.values;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public interface ShapeExplicitValue extends ShapeValue {

  ShapeExplicitValue add(ShapeExplicitValue pRValue);

  ShapeExplicitValue subtract(ShapeExplicitValue pRValue);

  ShapeExplicitValue multiply(ShapeExplicitValue pRValue);

  ShapeExplicitValue divide(ShapeExplicitValue pRValue);

  ShapeExplicitValue shiftLeft(ShapeExplicitValue pRValue);

  ShapeExplicitValue shiftRight(ShapeExplicitValue pRValue);

  ShapeExplicitValue castValue(CType pType, MachineModel pMachineModel);

}
