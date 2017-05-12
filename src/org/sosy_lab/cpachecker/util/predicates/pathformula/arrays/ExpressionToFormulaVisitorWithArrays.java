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
package org.sosy_lab.cpachecker.util.predicates.pathformula.arrays;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.Formula;

public class ExpressionToFormulaVisitorWithArrays extends ExpressionToFormulaVisitor {

  private final ArrayFormulaManagerView amgr;
  private final CToFormulaConverterWithArrays ctfa;
  private final MachineModel machine;

  public ExpressionToFormulaVisitorWithArrays(
      CToFormulaConverterWithArrays pCtoFormulaConverter,
      FormulaManagerView pMgr, MachineModel pMachineModel, CFAEdge pEdge,
      String pFunction, SSAMapBuilder pSsa, Constraints pConstraints) {
    super(pCtoFormulaConverter, pMgr, pEdge, pFunction, pSsa, pConstraints);

    amgr = mgr.getArrayFormulaManager();
    ctfa = pCtoFormulaConverter;
    machine = pMachineModel;
  }

  @Override
  public Formula visit(CArraySubscriptExpression pE) throws UnrecognizedCCodeException {

    // Examples for a CArraySubscriptExpression:
    //
    //    a[2]
    //      .arrayExpression: a
    //      .subscriptExpression: 2
    //      .type: (int)[]
    //
    //    --> (select a 2)
    //
    //    a[3][7]
    //      .type: int
    //      .subscriptExpression: 7
    //      .arrayExpression: CArraySubscriptExpression
    //          .type: (int)[]
    //          .subscriptExpression: 3
    //          .arrayExpression: CIdExpression a
    //
    //    --> (select (select a 7) 3)

    final ArrayFormula<?, ?> selectFrom;

    // Handling of the array expression --------------------------------------
    if (pE.getArrayExpression() instanceof CIdExpression) {
      final CIdExpression idExpr = (CIdExpression) pE.getArrayExpression();
      final String arrayVarName = idExpr.getDeclaration().getQualifiedName();
      final CType arrayType = pE.getArrayExpression().getExpressionType();

      selectFrom = (ArrayFormula<?, ?>) ctfa.makeVariable(arrayVarName, arrayType, ssa);

    } else if (pE.getArrayExpression() instanceof CArraySubscriptExpression) {
      final CArraySubscriptExpression subExpr = (CArraySubscriptExpression) pE.getArrayExpression();

      selectFrom = (ArrayFormula<?, ?>) subExpr.accept(this);

    } else {
      throw new UnrecognizedCCodeException(
          "CArraySubscriptExpression: Unknown type of array-expression!", pE);
    }

    // Handling of the index expression --------------------------------------
    // Make a cast of the subscript expression to the type of the array index
    final Formula indexExprFormula = pE.getSubscriptExpression().accept(this);
    final Formula castedIndexExprFormula = ctfa.makeCast(
        pE.getSubscriptExpression().getExpressionType(),
        machine.getPointerEquivalentSimpleType(), // TODO: Is this correct?
        indexExprFormula, null, null);

    // SELECT! ---------------------------------------------------------------
    return amgr.select(selectFrom, castedIndexExprFormula);
  }

  @Override
  public Formula visit(CUnaryExpression pExp) throws UnrecognizedCCodeException {
    final CExpression operand = pExp.getOperand();
    final UnaryOperator op = pExp.getOperator();

    if (op == UnaryOperator.AMPER && operand instanceof CArraySubscriptExpression) {
      // C99 standard (draft), 6.5.3.2 Address and indirection operators:
      //    "Similarly,if the operand is the result of a [] operator,
      //     neither the & operator nor the unary * that is implied
      //     by the [] is evaluated and the result is as if the & operator were removed
      //     and the [] operator were changed to a + operator."

      // Example:
      //  The C expression
      //    &(a[2]) == &(b[i])
      //  is semantically equivalent to
      //       a[2] == b[i]

      return operand.accept(this);

    } else {
      return super.visit(pExp);
    }
  }
}
