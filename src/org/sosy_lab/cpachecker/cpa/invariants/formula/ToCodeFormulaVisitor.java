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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.math.BigInteger;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Instances of this class are compound state invariants visitors used to
 * convert the visited invariants formulae into bit vector formulae.
 */
public class ToCodeFormulaVisitor
    implements ParameterizedNumeralFormulaVisitor<
    CompoundInterval,
    Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>>, String>,
               ParameterizedBooleanFormulaVisitor<
                   CompoundInterval,
                   Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>>,
                   ExpressionTree<String>> {

  private static final CSimpleType[] TYPES = new CSimpleType[]{
      CNumericTypes.SIGNED_CHAR,
      CNumericTypes.UNSIGNED_CHAR,
      CNumericTypes.SHORT_INT,
      CNumericTypes.UNSIGNED_SHORT_INT,
      CNumericTypes.INT,
      CNumericTypes.SIGNED_INT,
      CNumericTypes.UNSIGNED_INT,
      CNumericTypes.LONG_INT,
      CNumericTypes.UNSIGNED_LONG_INT,
      CNumericTypes.LONG_LONG_INT,
      CNumericTypes.UNSIGNED_LONG_LONG_INT};

  /**
   * The formula evaluation visitor used to evaluate compound state invariants
   * formulae to compound states.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  private final MachineModel machineModel;

  /**
   * Creates a new visitor for converting compound state invariants formulae to
   * bit vector formulae by using the given formula manager, and evaluation visitor.
   *
   * @param pEvaluationVisitor the formula evaluation visitor used to evaluate compound state
   *                           invariants formulae to compound states.
   * @param pMachineModel      the machine model used to find the cast types.
   */
  public ToCodeFormulaVisitor(
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor,
      MachineModel pMachineModel) {
    this.evaluationVisitor = pEvaluationVisitor;
    this.machineModel = pMachineModel;
  }

  private CSimpleType determineType(BitVectorInfo pBitVectorInfo) {
    int sizeOfChar = machineModel.getSizeofCharInBits();
    int size = pBitVectorInfo.getSize();
    boolean isSigned = pBitVectorInfo.isSigned();
    for (CSimpleType type : TYPES) {
      if (machineModel.isSigned(type) == isSigned
          && machineModel.getSizeof(type) * sizeOfChar >= size) {
        return type;
      }
    }
    return CNumericTypes.INT;
  }

  /**
   * Evaluates the given compound state invariants formula and tries to convert
   * the resulting value into a bit vector formula.
   *
   * @param pFormula     the formula to evaluate.
   * @param pEnvironment the environment to evaluate the formula in.
   * @return a bit vector formula representing the evaluation of the given formula or
   * <code>null</code> if the evaluation of the given formula could not be represented as a bit
   * vector formula.
   */
  private
  @Nullable
  String evaluate(
      NumeralFormula<CompoundInterval> pFormula,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval intervals = pFormula.accept(this.evaluationVisitor, pEnvironment);
    if (intervals.isSingleton()) {
      return asBitVectorFormula(pFormula.getBitVectorInfo(), intervals.getValue());
    }
    return null;
  }

  /**
   * Encodes the given value as a bit vector formula using the given bit vector
   * information.
   *
   * @param pBitVectorInfo the bit vector information.
   * @param pValue         the value.
   * @return a bit vector formula representing the given value as a bit vector with the given size.
   */
  private String asBitVectorFormula(BitVectorInfo pBitVectorInfo, BigInteger pValue) {
    int size = pBitVectorInfo.getSize();
    BigInteger value = pValue;
    // Get only the [size] least significant bits
    BigInteger upperExclusive = BigInteger.valueOf(2).pow(size);
    boolean negative = value.signum() < 0;
    if (negative && !value.equals(upperExclusive.shiftRight(1).negate())) {
      value = value.negate();
      value =
          value.and(BigInteger.valueOf(2).pow(size - 1).subtract(BigInteger.valueOf(1))).negate();
    } else if (!negative) {
      value = value.and(BigInteger.valueOf(2).pow(size).subtract(BigInteger.valueOf(1)));
    }
    String result = value.toString();
    if (!pBitVectorInfo.isSigned()) {
      result += "U";
    }
    if (pBitVectorInfo.getSize() > 32) {
      result += "LL";
    }
    return result;
  }

  @Override
  public String visit(
      Add<CompoundInterval> pAdd,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    NumeralFormula<CompoundInterval> summand1 = pAdd.getSummand1();
    NumeralFormula<CompoundInterval> summand2 = pAdd.getSummand2();
    String summand1Str = summand1.accept(this, pEnvironment);
    String summand2Str = summand2.accept(this, pEnvironment);
    if (summand1Str == null || summand2Str == null) {
      return evaluate(pAdd, pEnvironment);
    }
    NumeralFormula<CompoundInterval> negated = getNegated(summand1);
    String negatedStr = negated == null ? null : negated.accept(this, pEnvironment);
    if (negatedStr != null) {
      return "(" + summand2Str + " - " + negatedStr + ")";
    }
    negated = getNegated(summand2);
    negatedStr = negated == null ? null : negated.accept(this, pEnvironment);
    if (negated != null) {
      return "(" + summand1Str + " - " + negatedStr + ")";
    }
    return "(" + summand1Str + " + " + summand2Str + ")";
  }

  private NumeralFormula<CompoundInterval> getNegated(NumeralFormula<CompoundInterval> pFormula) {
    if (!(pFormula instanceof Multiply)) {
      return null;
    }
    Multiply<CompoundInterval> multiply = (Multiply<CompoundInterval>) pFormula;
    if (multiply.getFactor1() instanceof Constant) {
      Constant<CompoundInterval> constant = (Constant<CompoundInterval>) multiply.getFactor1();
      CompoundInterval value = constant.getValue();
      if (value.isSingleton() && value.contains(BigInteger.valueOf(-1))) {
        return multiply.getFactor2();
      }
    }
    if (multiply.getFactor2() instanceof Constant) {
      Constant<CompoundInterval> constant = (Constant<CompoundInterval>) multiply.getFactor2();
      CompoundInterval value = constant.getValue();
      if (value.isSingleton() && value.contains(BigInteger.valueOf(-1))) {
        return multiply.getFactor1();
      }
    }
    return null;
  }

  @Override
  public String visit(
      BinaryAnd<CompoundInterval> pAnd,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pAnd, pEnvironment);
  }

  @Override
  public String visit(
      BinaryNot<CompoundInterval> pNot,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pNot, pEnvironment);
  }

  @Override
  public String visit(
      BinaryOr<CompoundInterval> pOr,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pOr, pEnvironment);
  }

  @Override
  public String visit(
      BinaryXor<CompoundInterval> pXor,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pXor, pEnvironment);
  }

  @Override
  public String visit(
      Constant<CompoundInterval> pConstant,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pConstant, pEnvironment);
  }

  @Override
  public String visit(
      Divide<CompoundInterval> pDivide,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    String numerator = pDivide.getNumerator().accept(this, pEnvironment);
    String denominator = pDivide.getDenominator().accept(this, pEnvironment);
    if (numerator == null || denominator == null) {
      return evaluate(pDivide, pEnvironment);
    }
    return "(" + numerator + " / " + denominator + ")";
  }

  @Override
  public String visit(
      Exclusion<CompoundInterval> pExclusion,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pExclusion, pEnvironment);
  }

  @Override
  public String visit(
      Modulo<CompoundInterval> pModulo,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    String numerator = pModulo.getNumerator().accept(this, pEnvironment);
    String denominator = pModulo.getDenominator().accept(this, pEnvironment);
    if (numerator == null || denominator == null) {
      return evaluate(pModulo, pEnvironment);
    }
    return "(" + numerator + " % " + denominator + ")";
  }

  @Override
  public String visit(
      Multiply<CompoundInterval> pMultiply,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    String factor1 = pMultiply.getFactor1().accept(this, pEnvironment);
    String factor2 = pMultiply.getFactor2().accept(this, pEnvironment);
    if (factor1 == null || factor2 == null) {
      return evaluate(pMultiply, pEnvironment);
    }
    NumeralFormula<CompoundInterval> negated = getNegated(pMultiply);
    String negatedStr = negated == null ? null : negated.accept(this, pEnvironment);
    if (negatedStr != null) {
      return "(-" + negatedStr + ")";
    }
    return "(" + factor1 + " * " + factor2 + ")";
  }

  @Override
  public String visit(
      ShiftLeft<CompoundInterval> pShiftLeft,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pShiftLeft, pEnvironment);
  }

  @Override
  public String visit(
      ShiftRight<CompoundInterval> pShiftRight,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pShiftRight, pEnvironment);
  }

  @Override
  public String visit(
      Union<CompoundInterval> pUnion,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pUnion, pEnvironment);
  }

  @Override
  public String visit(
      Variable<CompoundInterval> pVariable,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return pVariable.getMemoryLocation().getIdentifier();
  }

  @Override
  public String visit(
      Cast<CompoundInterval> pCast,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    BitVectorInfo sourceInfo = pCast.getCasted().getBitVectorInfo();
    BitVectorInfo targetInfo = pCast.getBitVectorInfo();
    int sourceSize = sourceInfo.getSize();
    int targetSize = targetInfo.getSize();
    String sourceFormula = pCast.getCasted().accept(this, pEnvironment);
    if (sourceSize == targetSize && sourceInfo.isSigned() == targetInfo.isSigned()
        || sourceFormula == null) {
      return sourceFormula;
    }
    CSimpleType castType = determineType(targetInfo);
    return String.format("(%s) %s", castType, sourceFormula);
  }

  @Override
  public String visit(
      IfThenElse<CompoundInterval> pIfThenElse,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    BooleanConstant<CompoundInterval> conditionEval =
        pIfThenElse.getCondition().accept(evaluationVisitor, pEnvironment);
    if (BooleanConstant.isTrue(conditionEval)) {
      return pIfThenElse.getPositiveCase().accept(this, pEnvironment);
    }
    if (BooleanConstant.isFalse(conditionEval)) {
      return pIfThenElse.getNegativeCase().accept(this, pEnvironment);
    }

    ExpressionTree<String> conditionFormula = pIfThenElse.getCondition().accept(this, pEnvironment);
    if (conditionFormula == null) {
      return InvariantsFormulaManager.INSTANCE
          .union(pIfThenElse.getPositiveCase(), pIfThenElse.getNegativeCase())
          .accept(this, pEnvironment);
    }

    String positiveCaseFormula = pIfThenElse.getPositiveCase().accept(this, pEnvironment);
    if (positiveCaseFormula == null) {
      return null;
    }
    String negativeCaseFormula = pIfThenElse.getNegativeCase().accept(this, pEnvironment);
    if (negativeCaseFormula == null) {
      return null;
    }
    return "("
        + conditionFormula
        + " ? "
        + positiveCaseFormula
        + " : "
        + negativeCaseFormula + ")";
  }

  public static Integer getSize(NumeralFormula<CompoundInterval> pFormula) {
    return pFormula.getBitVectorInfo().getSize();
  }

  @Override
  public ExpressionTree<String> visit(
      Equal<CompoundInterval> pEqual,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    BitVectorInfo bitVectorInfo = pEqual.getOperand1().getBitVectorInfo();

    // Check not equals
    ExpressionTree<String> inversion = ExpressionTrees.getTrue();
    CompoundInterval op1EvalInvert =
        pEqual.getOperand1().accept(evaluationVisitor, pEnvironment).invert();
    if (op1EvalInvert.isSingleton() && pEqual.getOperand2() instanceof Variable) {
      inversion = And.of(inversion,
          not(Equal.of(Constant.of(bitVectorInfo, op1EvalInvert), pEqual.getOperand2())
              .accept(this, pEnvironment)));
    }
    CompoundInterval op2EvalInvert =
        pEqual.getOperand2().accept(evaluationVisitor, pEnvironment).invert();
    if (op2EvalInvert.isSingleton() && pEqual.getOperand1() instanceof Variable) {
      inversion = And.of(inversion,
          not(Equal.of(pEqual.getOperand1(), Constant.of(bitVectorInfo, op2EvalInvert))
              .accept(this, pEnvironment)));
    }

    // General case
    String operand1 = pEqual.getOperand1().accept(this, pEnvironment);
    String operand2 = pEqual.getOperand2().accept(this, pEnvironment);
    if (operand1 == null && operand2 == null) {
      return null;
    }
    if (operand1 == null || operand2 == null) {
      final String left;
      final NumeralFormula<CompoundInterval> right;
      if (operand1 != null) {
        left = operand1;
        right = pEqual.getOperand2();
      } else {
        left = operand2;
        right = pEqual.getOperand1();
      }
      CompoundInterval rightValue = right.accept(evaluationVisitor, pEnvironment);
      ExpressionTree<String> bf = ExpressionTrees.getFalse();
      for (SimpleInterval interval : rightValue.getIntervals()) {
        ExpressionTree<String> intervalFormula = ExpressionTrees.getTrue();
        if (interval.isSingleton()) {
          String value = asBitVectorFormula(bitVectorInfo, interval.getLowerBound());
          intervalFormula = And.of(intervalFormula, equal(left, value));
        } else {
          if (interval.hasLowerBound()) {
            String lb = asBitVectorFormula(bitVectorInfo, interval.getLowerBound());
            intervalFormula = And.of(intervalFormula, greaterEqual(left, lb));
          }
          if (interval.hasUpperBound()) {
            String ub = asBitVectorFormula(bitVectorInfo, interval.getUpperBound());
            intervalFormula = And.of(intervalFormula, lessEqual(left, ub));
          }
        }
        bf = Or.of(bf, intervalFormula);
      }
      return And.of(bf, inversion);
    }
    return And.of(equal(operand1, operand2), inversion);
  }

  @Override
  public ExpressionTree<String> visit(
      LessThan<CompoundInterval> pLessThan,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    BitVectorInfo bitVectorInfo = pLessThan.getOperand1().getBitVectorInfo();
    String operand1 = pLessThan.getOperand1().accept(this, pEnvironment);
    String operand2 = pLessThan.getOperand2().accept(this, pEnvironment);
    if (operand1 == null && operand2 == null) {
      return null;
    }
    if (operand1 == null || operand2 == null) {
      final String left;
      final NumeralFormula<CompoundInterval> right;
      final boolean lessThan;
      if (operand1 != null) {
        left = operand1;
        right = pLessThan.getOperand2();
        lessThan = true;
      } else {
        left = operand2;
        right = pLessThan.getOperand1();
        lessThan = false;
      }
      CompoundInterval rightValue = right.accept(evaluationVisitor, pEnvironment);
      if (rightValue.isBottom()) {
        return ExpressionTrees.getFalse();
      }
      if (lessThan) {
        if (rightValue.hasUpperBound()) {
          return lessThan(left, asBitVectorFormula(bitVectorInfo, rightValue.getUpperBound()));
        }
      } else if (rightValue.hasLowerBound()) {
        return greaterThan(left, asBitVectorFormula(bitVectorInfo, rightValue.getLowerBound()));
      }
      return null;
    }
    return lessThan(operand1, operand2);
  }

  private static final ExpressionTree<String> lessThan(String pLess, String pMore) {
    return LeafExpression.of(String.format("(%s < %s)", pLess, pMore));
  }

  private static final ExpressionTree<String> lessEqual(String pLess, String pMore) {
    return LeafExpression.of(String.format("(%s <= %s)", pLess, pMore));
  }

  private static final ExpressionTree<String> greaterThan(String pMore, String pLess) {
    return LeafExpression.of(String.format("(%s > %s)", pMore, pLess));
  }

  private static final ExpressionTree<String> greaterEqual(String pMore, String pLess) {
    return LeafExpression.of(String.format("(%s >= %s)", pMore, pLess));
  }

  private static final ExpressionTree<String> equal(String pLess, String pMore) {
    return LeafExpression.of(String.format("(%s == %s)", pLess, pMore));
  }

  private static final ExpressionTree<String> not(ExpressionTree<String> pOp) {
    if (pOp.equals(ExpressionTrees.getFalse())) {
      return ExpressionTrees.getTrue();
    }
    if (pOp.equals(ExpressionTrees.getTrue())) {
      return ExpressionTrees.getFalse();
    }
    if (pOp instanceof LeafExpression) {
      return ((LeafExpression<String>) pOp).negate();
    }
    return LeafExpression.<String>of(String.format("(!(%s))", pOp));
  }

  @Override
  public ExpressionTree<String> visit(
      LogicalAnd<CompoundInterval> pAnd,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return And.of(
        pAnd.getOperand1().accept(this, pEnvironment),
        pAnd.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public ExpressionTree<String> visit(
      LogicalNot<CompoundInterval> pNot,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return not(pNot.getNegated().accept(this, pEnvironment));
  }

  @Override
  public ExpressionTree<String> visitFalse(
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return ExpressionTrees.getFalse();
  }

  @Override
  public ExpressionTree<String> visitTrue(
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return ExpressionTrees.getTrue();
  }

}
