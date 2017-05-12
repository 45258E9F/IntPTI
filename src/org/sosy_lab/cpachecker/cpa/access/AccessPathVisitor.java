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
package org.sosy_lab.cpachecker.cpa.access;

import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.AccessPathBuilder;
import org.sosy_lab.cpachecker.util.access.AccessSummaryUtil;
import org.sosy_lab.cpachecker.util.access.AddressingSegment;
import org.sosy_lab.cpachecker.util.access.FieldAccessSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;
import org.sosy_lab.cpachecker.util.access.PointerDereferenceSegment;

/**
 * Recursively visit a CExpression:
 * (1) calculate the top level access path
 * (2) underlying access path is encapsulated in the changing of state.
 * for instance, a[(i++)+x] statement:
 * (a) mark reading of i and x
 * (b) mark writing of i
 * (c) return access path of 'a*'
 */
public class AccessPathVisitor
    implements CRightHandSideVisitor<AccessPath, UnrecognizedCCodeException> {
  // state which is used to store the access happened in this entire expression
  private AccessAnalysisState state = AccessAnalysisState.of();
  private AccessSummaryProvider provider;

  private CFunctionDeclaration pFunDelcaration = null;

  public AccessPathVisitor(AccessAnalysisState state, AccessSummaryProvider provider) {
    this.state = state;
    this.provider = provider;
  }

  @Override
  public AccessPath visit(CBinaryExpression pIastBinaryExpression)
      throws UnrecognizedCCodeException {
    // access path for binary operator is the first element
    // for instance, (x + y) returns 'x'
    // on: *(x + y), the path: x* is generated
    // on: z = (x + y), the path is marked read when visiting '='
    state = AccessAnalysisState.markRead(pIastBinaryExpression.getOperand2().accept(this), state);
    return pIastBinaryExpression.getOperand1().accept(this);
  }

  @Override
  public AccessPath visit(CCastExpression pIastCastExpression) throws UnrecognizedCCodeException {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public AccessPath visit(CCharLiteralExpression pIastCharLiteralExpression)
      throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public AccessPath visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public AccessPath visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public AccessPath visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public AccessPath visit(CTypeIdExpression pIastTypeIdExpression)
      throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public AccessPath visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCCodeException {
    // note:
    // (1) ++ and -- are not unary operator
    // (2) AMPER(&) is unary operator, we consider it in the following way:
    //    (a) if the last segment is '*', remove it
    //    (b) if not, simply drop this operator | tomgu FIXME bug Struct s *p = &s_s; we should not remove &
    AccessPath ap = pIastUnaryExpression.getOperand().accept(this);
    if (ap != null) {
      PathSegment seg = ap.getLastSegment();
      if (pIastUnaryExpression.getOperator().equals(UnaryOperator.AMPER)) {
        if (seg != null && seg instanceof PointerDereferenceSegment) {
          ap.removeLastSegment();
        } else {
          ap.appendSegment(AddressingSegment.INSTANCE);
        }
      }

    }
    return ap;
  }

  @Override
  public AccessPath visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public AccessPath visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public AccessPath visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws UnrecognizedCCodeException {
    // deal with the subscript
    AccessPath ap = pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
    state = AccessAnalysisState.markRead(ap, state);
    // deal with the array
    // Note: if arrayExpression is pointer type, a[i] = j; -> a should not be write; but char a[10], a may be write
    if (pIastArraySubscriptExpression.getArrayExpression()
        .getExpressionType() instanceof CPointerType) {
      return null;
    }
    if (pFunDelcaration != null) { // we only treat this in assign
      // Note: if array is parameters of function, it will be treated as pointer
      for (CParameterDeclaration p : pFunDelcaration.getParameters()) {
        String pName = p.getQualifiedName();
        if (!(pIastArraySubscriptExpression.getArrayExpression() instanceof CIdExpression)) {
          return pIastArraySubscriptExpression.getArrayExpression().accept(this);
        }
        String expName =
            ((CIdExpression) pIastArraySubscriptExpression.getArrayExpression()).getDeclaration()
                .getQualifiedName();
        if (pName.equals(expName)) {
          return null;
        }
      }
    }


    return pIastArraySubscriptExpression.getArrayExpression().accept(this);

  }

  @Override
  public AccessPath visit(CFieldReference pIastFieldReference) throws UnrecognizedCCodeException {
    // check whether it is a function pointer operation
    if (AccessSummaryUtil.isFunctionType(pIastFieldReference.getExpressionType())) {
      return null;
    }
    // tomgu should we put pointerDereference ahead of fieldAccess
    /**
     * struct s{ int i}
     * struct s *p; p->i = 1;  ==> p|*|i
     */
    AccessPath ap = pIastFieldReference.getFieldOwner().accept(this);
    if (ap != null) {
      if (pIastFieldReference.isPointerDereference()) {
        ap.appendSegment(new PointerDereferenceSegment());
      }
      ap.appendSegment(new FieldAccessSegment(pIastFieldReference.getFieldName()));
    }
    return ap;
  }

  @Override
  public AccessPath visit(CIdExpression pIastIdExpression) throws UnrecognizedCCodeException {
    // here we have to check whether it is a function declaration
    if (AccessSummaryUtil.isFunctionType(pIastIdExpression.getExpressionType())) {
      return null;
    }
    // create path segment
    AccessPathBuilder.getInstance()
        .addDeclaration(pIastIdExpression.getDeclaration().getQualifiedName(),
            pIastIdExpression.getDeclaration());
    return new AccessPath(pIastIdExpression.getDeclaration());
  }

  @Override
  public AccessPath visit(CPointerExpression pPointerExpression) throws UnrecognizedCCodeException {
    // in case of *(p + x), only p is dereferenced and x should be marked read
    AccessPath ap = pPointerExpression.getOperand().accept(this);
    if (ap != null) {
      ap.appendSegment(new PointerDereferenceSegment());
    }
    return ap;
  }

  @Override
  public AccessPath visit(CComplexCastExpression pComplexCastExpression)
      throws UnrecognizedCCodeException {
    return pComplexCastExpression.getOperand().accept(this);
  }

  @Override
  public AccessPath visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCCodeException {
    CExpression functionNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
    if (functionNameExpression instanceof CIdExpression) {
      // apply summary
      String functionName = ((CIdExpression) functionNameExpression).getName();
      state = AccessSummaryUtil.apply(
          functionName,
          provider,
          pIastFunctionCallExpression.getParameterExpressions(),
          state
      );
      // no access path for function call expression
      return null;
    } else {
      // do not handle function pointer, assume every actual parameter is read and every pointer is written
      //throw new UnrecognizedCCodeException("Function pointers are not handled!",
      //pIastFunctionCallExpression);
      return null;
    }
  }

  /**
   * State may be modified during visiting the expression
   */
  public AccessAnalysisState getState() {
    return state;
  }

  public void setDeclaration(CFunctionDeclaration pFunDeclaration) {
    // set for assign, where leftValue is arrayExpression
    this.pFunDelcaration = pFunDeclaration;
  }

}
