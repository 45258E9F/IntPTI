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
package org.sosy_lab.cpachecker.util.predicates.smt;

import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.ArrayFormulaManager;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.FormulaType.ArrayFormulaType;

import javax.annotation.Nonnull;

/**
 * Implements some methods for easier interaction with the formula manager for
 * array formulas.
 */
public class ArrayFormulaManagerView extends BaseManagerView implements ArrayFormulaManager {

  private ArrayFormulaManager manager;

  /**
   * Creates the new formula manager view for arrays.
   *
   * @param pWrappingHandler A handler for wrapping and unwrapping of formulae.
   * @param pManager         The formula manager capable of the SMT theory of arrays.
   */
  ArrayFormulaManagerView(
      final @Nonnull FormulaWrappingHandler pWrappingHandler,
      final @Nonnull ArrayFormulaManager pManager) {
    super(pWrappingHandler);
    this.manager = pManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <TI extends Formula, TE extends Formula> TE select(
      final @Nonnull ArrayFormula<TI, TE> pArray, final @Nonnull Formula pIndex) {

    @SuppressWarnings("unchecked")
    final ArrayFormula<TI, TE> declaredArray = (ArrayFormula<TI, TE>) unwrap(pArray);
    final TE selectResult = manager.select(declaredArray, unwrap(pIndex));
    final FormulaType<TE> resultType = getElementType(pArray);

    // the result of a select can also be a reference to an array! (multi-dimensional arrays)
    // example: returns an array
    return wrap(resultType, selectResult);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <TI extends Formula, TE extends Formula> ArrayFormula<TI, TE> store(
      final @Nonnull ArrayFormula<TI, TE> pArray,
      final @Nonnull Formula pIndex,
      final @Nonnull Formula pValue) {

    @SuppressWarnings("unchecked")
    final ArrayFormula<TI, TE> declaredArray = (ArrayFormula<TI, TE>) unwrap(pArray);
    final ArrayFormulaType<TI, TE> inputArrayType =
        new ArrayFormulaType<>(getIndexType(pArray), getElementType(pArray));

    final ArrayFormula<TI, TE> resultFormula =
        manager.store(declaredArray, unwrap(pIndex), unwrap(pValue));
    if (resultFormula instanceof WrappingFormula<?, ?>) {
      return resultFormula;
    } else {
      return wrap(inputArrayType, resultFormula);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <TI extends Formula, TE extends Formula, FTI extends FormulaType<TI>,
      FTE extends FormulaType<TE>> ArrayFormula<TI, TE> makeArray(
      final @Nonnull String pName,
      final @Nonnull FTI pIndexType,
      final @Nonnull FTE pElementType) {

    final ArrayFormulaType<TI, TE> inputArrayType =
        new ArrayFormulaType<>(pIndexType, pElementType);

    @SuppressWarnings("unchecked")
    final FTI unwrappedIndexType = (FTI) unwrapType(pIndexType);
    @SuppressWarnings("unchecked")
    final FTE unwrappedElementType = (FTE) unwrapType(pElementType);

    final ArrayFormula<TI, TE> result =
        manager.makeArray(pName, unwrappedIndexType, unwrappedElementType);

    return wrap(inputArrayType, result);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public <TI extends Formula, TE extends Formula> ArrayFormula<TI, TE> makeArray(
      final @Nonnull String pName, final @Nonnull ArrayFormulaType<TI, TE> type) {
    return wrap(
        type, manager.makeArray(pName, (ArrayFormulaType<Formula, Formula>) unwrapType(type)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <TI extends Formula> FormulaType<TI> getIndexType(
      final @Nonnull ArrayFormula<TI, ?> pArray) {
    if (pArray instanceof WrappingFormula<?, ?>) {
      @SuppressWarnings("unchecked")
      ArrayFormulaType<TI, ?> t =
          (ArrayFormulaType<TI, ?>) ((WrappingFormula<?, ?>) pArray).getType();
      return t.getIndexType();
    }
    return manager.getIndexType(pArray);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <TE extends Formula> FormulaType<TE> getElementType(
      final @Nonnull ArrayFormula<?, TE> pArray) {
    if (pArray instanceof WrappingFormula<?, ?>) {
      @SuppressWarnings("unchecked")
      ArrayFormulaType<?, TE> t =
          (ArrayFormulaType<?, TE>) ((WrappingFormula<?, ?>) pArray).getType();
      return t.getElementType();
    }
    return manager.getElementType(pArray);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <TI extends Formula, TE extends Formula> BooleanFormula equivalence(
      final @Nonnull ArrayFormula<TI, TE> pArray1, final @Nonnull ArrayFormula<TI, TE> pArray2) {

    @SuppressWarnings("unchecked")
    final ArrayFormula<TI, TE> declaredArray1 = (ArrayFormula<TI, TE>) unwrap(pArray1);
    @SuppressWarnings("unchecked")
    final ArrayFormula<TI, TE> declaredArray2 = (ArrayFormula<TI, TE>) unwrap(pArray2);

    BooleanFormula result = manager.equivalence(declaredArray1, declaredArray2);
    return wrap(FormulaType.BooleanType, result);
  }

}
