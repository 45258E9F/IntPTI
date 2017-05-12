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
package org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions;

import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.Pair;

import java.math.BigInteger;
import java.util.List;

/**
 * Class to generate assumptions related to over/underflow
 * of integer arithmetic operations
 */
public class ArithmeticOverflowAssumptionBuilder
    implements GenericAssumptionBuilder {

  /* type bounds, assuming 32-bit machine */
  // TODO use MachineModel
  public static final CIntegerLiteralExpression INT_MAX =
      new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT,
          BigInteger.valueOf(2147483647L));
  public static final CIntegerLiteralExpression INT_MIN =
      new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT,
          BigInteger.valueOf(-2147483648L));

  private static Pair<CIntegerLiteralExpression, CIntegerLiteralExpression> boundsForType(CType typ) {
    if (typ instanceof CSimpleType) {
      CSimpleType btyp = (CSimpleType) typ;

      switch (btyp.getType()) {
        case INT:
          // TODO not handled yet by mathsat so we assume all vars are signed integers for now
          // will enable later
          return Pair.of
              (INT_MIN, INT_MAX);
        //          if (btyp.isLong())
        //            if (btyp.isUnsigned())
        //              return new Pair<>
        //          (DummyASTNumericalLiteralExpression.ULONG_MIN, DummyASTNumericalLiteralExpression.ULONG_MAX);
        //            else
        //              return new Pair<>
        //          (DummyASTNumericalLiteralExpression.LONG_MIN, DummyASTNumericalLiteralExpression.LONG_MAX);
        //          else if (btyp.isShort())
        //            if (btyp.isUnsigned())
        //              return new Pair<>
        //          (DummyASTNumericalLiteralExpression.USHRT_MIN, DummyASTNumericalLiteralExpression.USHRT_MAX);
        //            else
        //              return new Pair<>
        //          (DummyASTNumericalLiteralExpression.SHRT_MIN, DummyASTNumericalLiteralExpression.SHRT_MAX);
        //          else
        //            if (btyp.isUnsigned())
        //              return new Pair<>
        //          (DummyASTNumericalLiteralExpression.UINT_MIN, DummyASTNumericalLiteralExpression.UINT_MAX);
        //            else
        //              return new Pair<>
        //          (DummyASTNumericalLiteralExpression.INT_MIN, DummyASTNumericalLiteralExpression.INT_MAX);
        //        case IBasicType.t_char:
        //          if (btyp.isUnsigned())
        //            return new Pair<>
        //          (DummyASTNumericalLiteralExpression.UCHAR_MIN, DummyASTNumericalLiteralExpression.UCHAR_MAX);
        //          else
        //            return new Pair<>
        //          (DummyASTNumericalLiteralExpression.CHAR_MIN, DummyASTNumericalLiteralExpression.CHAR_MAX);
        default:
          // TODO add other bounds
          break;
      }
    }
    return Pair.of(null, null);
  }

  /**
   * Compute and conjunct the assumption for the given arithmetic
   * expression, ignoring bounds if applicable. The method does
   * not check that the expression is indeed an arithmetic expression.
   */
  private static void conjunctPredicateForArithmeticExpression(
      CExpression exp, List<CExpression> result) {
    conjunctPredicateForArithmeticExpression(exp.getExpressionType(), exp, result);
  }

  /**
   * Compute and conjunct the assumption for the given arithmetic
   * expression, given as its type and its expression.
   * The two last, boolean arguments allow to avoid generating
   * lower and/or upper bounds predicates.
   */
  private static void conjunctPredicateForArithmeticExpression(
      CType typ,
      CExpression exp, List<CExpression> result) {

    Pair<CIntegerLiteralExpression, CIntegerLiteralExpression> bounds =
        boundsForType(typ);

    if (bounds.getFirst() != null) {

      result.add(new CBinaryExpression(FileLocation.DUMMY, null, null, exp,
          bounds.getFirst(), BinaryOperator.GREATER_EQUAL));
    }

    if (bounds.getSecond() != null) {

      result.add(new CBinaryExpression(FileLocation.DUMMY, null, null, exp,
          bounds.getSecond(), BinaryOperator.LESS_EQUAL));
    }
  }

  private static void visit(CExpression pExpression, List<CExpression> result) {
    if (pExpression instanceof CIdExpression) {
      conjunctPredicateForArithmeticExpression(pExpression, result);
    } else if (pExpression instanceof CBinaryExpression) {
      CBinaryExpression binexp = (CBinaryExpression) pExpression;
      CExpression op1 = binexp.getOperand1();
      // Only variables for now, ignoring * & operators
      if (op1 instanceof CIdExpression) {
        conjunctPredicateForArithmeticExpression(op1, result);
      }
    } else if (pExpression instanceof CUnaryExpression) {
      CUnaryExpression unexp = (CUnaryExpression) pExpression;
      CExpression op1 = unexp.getOperand();
      // Only variables. Ignoring * & operators for now
      if (op1 instanceof CIdExpression) {
        conjunctPredicateForArithmeticExpression(op1, result);
      }
    } else if (pExpression instanceof CCastExpression) {
      CCastExpression castexp = (CCastExpression) pExpression;
      CType toType = castexp.getExpressionType();
      conjunctPredicateForArithmeticExpression(toType, castexp.getOperand(), result);
    }
  }

  @Override
  public List<CExpression> assumptionsForEdge(CFAEdge pEdge) {
    List<CExpression> result = Lists.newArrayList();

    switch (pEdge.getEdgeType()) {
      case AssumeEdge:
        CAssumeEdge assumeEdge = (CAssumeEdge) pEdge;
        visit(assumeEdge.getExpression(), result);
        break;
      case FunctionCallEdge:
        CFunctionCallEdge fcallEdge = (CFunctionCallEdge) pEdge;
        if (!fcallEdge.getArguments().isEmpty()) {
          CFunctionEntryNode fdefnode = fcallEdge.getSuccessor();
          List<CParameterDeclaration> formalParams = fdefnode.getFunctionParameters();
          for (CParameterDeclaration paramdecl : formalParams) {
            CExpression exp = new CIdExpression(paramdecl.getFileLocation(), paramdecl);
            visit(exp, result);
          }
        }
        break;
      case StatementEdge:
        CStatementEdge stmtEdge = (CStatementEdge) pEdge;

        CStatement stmt = stmtEdge.getStatement();
        if (stmt instanceof CAssignment) {
          visit(((CAssignment) stmt).getLeftHandSide(), result);
        }
        break;
      case ReturnStatementEdge:
        CReturnStatementEdge returnEdge = (CReturnStatementEdge) pEdge;

        if (returnEdge.getExpression().isPresent()) {
          visit(returnEdge.getExpression().get(), result);
        }
        break;
      default:
        // TODO assumptions or other edge types, e.g. declarations?
        break;
    }
    return result;
  }
}
