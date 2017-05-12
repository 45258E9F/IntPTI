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
package org.sosy_lab.cpachecker.cpa.value.symbolic;

import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Class for transforming {@link AExpression}s to {@link SymbolicExpression}s.
 *
 * <p>For each transformation, a new object has to be created. Otherwise, the resulting expressions
 * might not reflect the programs correct concrete states.</p>
 */
public class ExpressionTransformer {

  protected final String functionName;

  protected ValueAnalysisState valueState;

  public ExpressionTransformer(String pFunctionName, ValueAnalysisState pValueState) {
    functionName = pFunctionName;
    valueState = pValueState;
  }

  protected Value createNumericValue(long pValue) {
    return new NumericValue(pValue);
  }

  protected Value createNumericValue(BigDecimal pValue) {
    return new NumericValue(pValue);
  }

  protected Value createNumericValue(BigInteger pValue) {
    return new NumericValue(pValue);
  }

  public SymbolicExpression visit(AIdExpression pIastIdExpression) {
    final MemoryLocation memLoc = getMemoryLocation(pIastIdExpression.getDeclaration());

    final Type idType = pIastIdExpression.getExpressionType();

    if (valueState.contains(memLoc)) {
      final Value idValue = valueState.getValueFor(memLoc);

      return SymbolicValueFactory.getInstance().asConstant(idValue, idType).copyForLocation(memLoc);

    } else {
      return null;
    }
  }

  private MemoryLocation getMemoryLocation(ASimpleDeclaration pDeclaration) {
    if (pDeclaration instanceof ADeclaration && ((ADeclaration) pDeclaration).isGlobal()) {
      return MemoryLocation.valueOf(pDeclaration.getName());

    } else {
      return MemoryLocation.valueOf(functionName, pDeclaration.getName());
    }
  }

  public SymbolicExpression visit(AIntegerLiteralExpression pIastIntegerLiteralExpression) {
    final BigInteger value = pIastIntegerLiteralExpression.getValue();
    final Type intType = pIastIntegerLiteralExpression.getExpressionType();

    return SymbolicValueFactory.getInstance().asConstant(createNumericValue(value), intType);
  }

  public SymbolicExpression visit(ACharLiteralExpression pIastCharLiteralExpression) {
    final long castValue = pIastCharLiteralExpression.getCharacter();
    final Type charType = pIastCharLiteralExpression.getExpressionType();

    return SymbolicValueFactory.getInstance().asConstant(createNumericValue(castValue), charType);
  }

  public SymbolicExpression visit(AFloatLiteralExpression pIastFloatLiteralExpression) {
    final BigDecimal value = pIastFloatLiteralExpression.getValue();
    final Type floatType = pIastFloatLiteralExpression.getExpressionType();

    return SymbolicValueFactory.getInstance().asConstant(createNumericValue(value), floatType);
  }

}
