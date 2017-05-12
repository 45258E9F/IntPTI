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
package org.sosy_lab.cpachecker.cpa.pointer2.summary.visitor;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Created by landq on 11/28/16.
 */
public class Pointer2ExpressionVisitor
    extends DefaultCExpressionVisitor<MemoryLocation, RuntimeException> {

  String functionName;

  public Pointer2ExpressionVisitor(String pFunctionName) {
    functionName = pFunctionName;
  }

  @Override
  protected MemoryLocation visitDefault(CExpression exp) throws RuntimeException {
    if (exp instanceof CUnaryExpression) {
      ((CUnaryExpression) exp).getOperand().accept(this);
    } else if (exp instanceof CIdExpression) {
      {
        if (exp.getExpressionType() instanceof CPointerType) {
          String identifier = ((CIdExpression) exp).getName();
          return MemoryLocation.valueOf(functionName, identifier);
        }
      }
    }
    return null;
  }
}
