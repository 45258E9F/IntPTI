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
package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.FunctionDeclaration;
import org.sosy_lab.solver.api.NumeralFormula;
import org.sosy_lab.solver.api.NumeralFormulaManager;
import org.sosy_lab.solver.api.UFManager;

/**
 * As not all solvers support non-linear arithmetics,
 * we use uninterpreted functions whenever a direct operation fails.
 *
 * <p>
 * This behaviour depends on the solver and differs for different solvers,
 * if they support more or less operations.
 * </p>
 */
public class NonLinearUFNumeralFormulaManager
    <ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
    extends NumeralFormulaManagerView<ParamFormulaType, ResultFormulaType> {

  private static final String UF_MULTIPLY_NAME = "_*_";
  private static final String UF_DIVIDE_NAME = "_/_";
  private static final String UF_MODULO_NAME = "_%_";

  private final FunctionDeclaration<ResultFormulaType> multUfDecl;
  private final FunctionDeclaration<ResultFormulaType> divUfDecl;
  private final FunctionDeclaration<ResultFormulaType> modUfDecl;

  private final UFManager functionManager;

  NonLinearUFNumeralFormulaManager(
      FormulaWrappingHandler pWrappingHandler,
      NumeralFormulaManager<ParamFormulaType, ResultFormulaType> numeralFormulaManager,
      UFManager pFunctionManager) {
    super(pWrappingHandler, numeralFormulaManager);
    functionManager = checkNotNull(pFunctionManager);
    FormulaType<ResultFormulaType> formulaType = manager.getFormulaType();

    multUfDecl = createBinaryFunction(UF_MULTIPLY_NAME, formulaType);
    divUfDecl = createBinaryFunction(UF_DIVIDE_NAME, formulaType);
    modUfDecl = createBinaryFunction(UF_MODULO_NAME, formulaType);
  }

  private FunctionDeclaration<ResultFormulaType> createBinaryFunction(
      String name, FormulaType<ResultFormulaType> formulaType) {
    return functionManager.declareUF(
        formulaType + "_" + name, formulaType, formulaType, formulaType);
  }

  private ResultFormulaType makeUf(
      FunctionDeclaration<ResultFormulaType> decl, ParamFormulaType t1, ParamFormulaType t2) {
    return functionManager.callUF(decl, ImmutableList.of(t1, t2));
  }

  @Override
  public ResultFormulaType divide(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    ResultFormulaType result;
    try {
      result = manager.divide(pNumber1, pNumber2);
    } catch (UnsupportedOperationException e) {
      result = makeUf(divUfDecl, pNumber1, pNumber2);
    }
    return result;
  }

  @Override
  public ResultFormulaType modulo(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    ResultFormulaType result;
    try {
      result = manager.modulo(pNumber1, pNumber2);
    } catch (UnsupportedOperationException e) {
      result = makeUf(modUfDecl, pNumber1, pNumber2);
    }
    return result;
  }

  @Override
  public ResultFormulaType multiply(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    ResultFormulaType result;
    try {
      result = manager.multiply(pNumber1, pNumber2);
    } catch (UnsupportedOperationException e) {
      result = makeUf(multUfDecl, pNumber1, pNumber2);
    }
    return result;
  }
}
