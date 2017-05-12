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
package org.sosy_lab.cpachecker.cpa.shape.constraint;

import com.google.common.base.Optional;

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeValue;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.NumeralFormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FloatingPointFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.FormulaType.FloatingPointType;
import org.sosy_lab.solver.api.Model.ValueAssignment;
import org.sosy_lab.solver.api.NumeralFormula;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Convert a {@link SymbolicExpression} into {@link BooleanFormula}.
 */
public class FormulaCreator {

  private final FormulaManagerView formulaManager;
  private final CtoFormulaConverter formulaTransformer;

  private final String functionName;

  public FormulaCreator(
      final FormulaManagerView pFormulaManager,
      final CtoFormulaConverter pTransformer,
      final String pFunctionName) {
    formulaManager = pFormulaManager;
    formulaTransformer = pTransformer;
    functionName = pFunctionName;
  }


  /**
   * Create a boolean according to a {@link ConstraintRepresentation}.
   * Note that this interface includes symbolic expression, logical AND/OR structures.
   *
   * @param pConstraint the given constraint representation
   * @return converted boolean formula
   */
  @Nullable
  public BooleanFormula createFormula(ConstraintRepresentation pConstraint)
      throws UnrecognizedCCodeException, InterruptedException {
    if (pConstraint instanceof SymbolicExpression) {
      return createFormula((SymbolicExpression) pConstraint);
    } else if (pConstraint instanceof LogicalAndContainer) {
      LogicalAndContainer andContainer = (LogicalAndContainer) pConstraint;
      int clauseSize = andContainer.size();
      List<BooleanFormula> clauses = new ArrayList<>(clauseSize);
      for (int i = 0; i < clauseSize; i++) {
        ConstraintRepresentation clause = andContainer.get(i);
        BooleanFormula clauseFormula = createFormula(clause);
        if (clauseFormula != null) {
          clauses.add(clauseFormula);
        }
      }
      if (clauses.size() == 0) {
        return formulaManager.getBooleanFormulaManager().makeBoolean(true);
      }
      return formulaManager.getBooleanFormulaManager().and(clauses);
    } else if (pConstraint instanceof LogicalOrContainer) {
      LogicalOrContainer orContainer = (LogicalOrContainer) pConstraint;
      int clauseSize = orContainer.size();
      List<BooleanFormula> clauses = new ArrayList<>(clauseSize);
      for (int i = 0; i < clauseSize; i++) {
        ConstraintRepresentation clause = orContainer.get(i);
        BooleanFormula clauseFormula = createFormula(clause);
        if (clauseFormula != null) {
          clauses.add(clauseFormula);
        }
      }
      if (clauses.size() == 0) {
        // if the number of valid clauses is zero, boolean literal of TRUE is returned
        return formulaManager.getBooleanFormulaManager().makeBoolean(false);
      }
      return formulaManager.getBooleanFormulaManager().or(clauses);
    } else {
      throw new IllegalStateException("unsupported constraint representation");
    }
  }

  /**
   * Transform assumption into boolean formula.
   * Generally, an assumption expression is a binary logical expression.
   */
  @Nullable
  private BooleanFormula createFormula(SymbolicExpression constraint)
      throws UnrecognizedCCodeException, InterruptedException {
    if (constraint instanceof ConstantSE) {
      ShapeValue value = constraint.getValue();
      if (value.equals(KnownSymbolicValue.ZERO) || value.equals(KnownExplicitValue.ZERO)) {
        return formulaManager.getBooleanFormulaManager().makeBoolean(false);
      } else if (value.equals(KnownSymbolicValue.TRUE) || value.equals(KnownExplicitValue.ONE)) {
        return formulaManager.getBooleanFormulaManager().makeBoolean(true);
      }
      // otherwise, it is not a boolean formula!
      return null;
    }
    CExpression constraintExp = constraint.getSymbolicExpression();
    if (constraintExp == null) {
      return null;
    }
    try {
      return formulaTransformer.makePredicate(constraintExp, getDummyEdge(), functionName,
          getSSAMapBuilder());
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * Transform model assignment into boolean formula.
   *
   * @param modelAssign An assignment in the satisfiability model
   * @return transformed boolean formula
   */
  public BooleanFormula createFormulaFromAssignment(ValueAssignment modelAssign) {
    final Formula key = modelAssign.getKey();
    final Object value = modelAssign.getValue();
    FormulaType<?> keyType = formulaManager.getFormulaType(key);
    Formula rightFormula = null;

    // only numeric assignment is handled here
    final NumeralFormulaManagerView<NumeralFormula, NumeralFormula.RationalFormula>
        rationalFormulaManager = formulaManager.getRationalFormulaManager();
    if (value instanceof Number) {
      BigInteger intValue = null;
      BigDecimal decValue = null;
      if (value instanceof Long) {
        intValue = BigInteger.valueOf((long) value);
      } else if (value instanceof BigInteger) {
        intValue = (BigInteger) value;
      } else if (value instanceof BigDecimal) {
        decValue = (BigDecimal) value;
      } else if (value instanceof Float || value instanceof Double) {
        assert keyType.isFloatingPointType();
        final FloatingPointFormula floatVar = (FloatingPointFormula) key;
        final Double dblValue;
        if (value instanceof Float) {
          dblValue = ((Float) value).doubleValue();
        } else {
          dblValue = (Double) value;
        }

        if (dblValue.isNaN()) {
          return getNaNFormula(floatVar);
        } else if (dblValue.equals(Double.POSITIVE_INFINITY)) {
          return getPositiveInfFormula(floatVar);
        } else if (dblValue.equals(Double.NEGATIVE_INFINITY)) {
          return getNegativeInfFormula(floatVar);
        } else {
          decValue = BigDecimal.valueOf(dblValue);
        }
      } else if (value instanceof Rational) {
        rightFormula = rationalFormulaManager.makeNumber((Rational) value);
      }
      // other cases should be impossible
      if (intValue != null) {
        rightFormula = formulaManager.makeNumber(keyType, intValue);
      } else if (decValue != null) {
        if (keyType.isRationalType()) {
          rightFormula = rationalFormulaManager.makeNumber(decValue);
        } else {
          assert keyType.isFloatingPointType();
          FloatingPointType floatType = (FloatingPointType) keyType;
          rightFormula = formulaManager.getFloatingPointFormulaManager().makeNumber(decValue,
              floatType);
        }
      }
    }

    if (rightFormula != null) {
      return formulaManager.makeEqual(key, rightFormula);
    } else {
      throw new AssertionError("unsupported model assignment");
    }
  }

  /* ***************** */
  /* auxiliary methods */
  /* ***************** */

  private CFAEdge getDummyEdge() {
    return DummyEdge.getInstance(functionName);
  }

  private SSAMapBuilder getSSAMapBuilder() {
    return SSAMap.emptySSAMap().builder();
  }

  private BooleanFormula getNaNFormula(FloatingPointFormula pFormula) {
    return formulaManager.getFloatingPointFormulaManager().isNaN(pFormula);
  }

  private BooleanFormula getPositiveInfFormula(FloatingPointFormula pFormula) {
    FloatingPointType formulaType = (FloatingPointType) formulaManager.getFormulaType(pFormula);
    Formula infFormula = formulaManager.getFloatingPointFormulaManager().makePlusInfinity
        (formulaType);
    return formulaManager.makeEqual(pFormula, infFormula);
  }

  private BooleanFormula getNegativeInfFormula(FloatingPointFormula pFormula) {
    FloatingPointType formulaType = (FloatingPointType) formulaManager.getFormulaType(pFormula);
    Formula infFormula = formulaManager.getFloatingPointFormulaManager().makeMinusInfinity
        (formulaType);
    return formulaManager.makeEqual(pFormula, infFormula);
  }

  /* ************************ */
  /* auxiliary data structure */
  /* ************************ */

  /**
   * A dummy edge, for the purpose of calling the existing making predicate method.
   */
  private static class DummyEdge implements CFAEdge {

    private static final String UNKNOWN = "<unknown>";
    private static final FileLocation DUMMY_LOCATION = new FileLocation(0, UNKNOWN, 0, 0, 0);

    private static Map<String, DummyEdge> cache = new HashMap<>();

    private final CFANode dummyNode;

    private DummyEdge(String pFunctionName) {
      dummyNode = new CFANode(pFunctionName);
    }

    public static DummyEdge getInstance(String pFunctionName) {
      DummyEdge edge = cache.get(pFunctionName);
      if (edge == null) {
        edge = new DummyEdge(pFunctionName);
        cache.put(pFunctionName, edge);
      }
      return edge;
    }

    @Override
    public CFAEdgeType getEdgeType() {
      return CFAEdgeType.BlankEdge;
    }

    @Override
    public CFANode getPredecessor() {
      return dummyNode;
    }

    @Override
    public CFANode getSuccessor() {
      return dummyNode;
    }

    @Override
    public Optional<? extends AAstNode> getRawAST() {
      return Optional.absent();
    }

    @Override
    public int getLineNumber() {
      return 0;
    }

    @Override
    public FileLocation getFileLocation() {
      return DUMMY_LOCATION;
    }

    @Override
    public String getRawStatement() {
      return UNKNOWN;
    }

    @Override
    public String getCode() {
      return UNKNOWN;
    }

    @Override
    public String getDescription() {
      return UNKNOWN;
    }
  }

}
