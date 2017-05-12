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
package org.sosy_lab.cpachecker.cpa.value;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.LValueAssignmentVisitor;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


/**
 * Called communicator and not Evaluator because in the future
 * this will only contain the necessary methods to extract the
 * values and addresses from the respective States, evaluation
 * will be done in another class.
 */
public class ValueAnalysisSMGCommunicator {

  private final CFAEdge cfaEdge;
  private final LogManagerWithoutDuplicates logger;
  private final MachineModel machineModel;
  private final SMGState smgState;
  private final ValueAnalysisState valueAnalysisState;
  private final String functionName;

  public ValueAnalysisSMGCommunicator(
      ValueAnalysisState pValueAnalysisState,
      String pFunctionName,
      SMGState pSmgState,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      CFAEdge pCfaEdge) {
    valueAnalysisState = pValueAnalysisState;
    smgState = pSmgState;
    machineModel = pMachineModel;
    logger = pLogger;
    cfaEdge = pCfaEdge;
    functionName = pFunctionName;
  }

  public Value evaluateExpression(CRightHandSide rValue) throws UnrecognizedCCodeException {

    ExplicitExpressionValueVisitor evv =
        new ExplicitExpressionValueVisitor();
    return rValue.accept(evv);
  }

  public SMGSymbolicValue evaluateSMGExpression(CRightHandSide rValue)
      throws CPATransferException {

    SMGExplicitExpressionEvaluator eee =
        new SMGExplicitExpressionEvaluator();
    return eee.evaluateExpressionValueV2(smgState, cfaEdge, rValue);
  }

  public SMGAddressValue evaluateSMGAddressExpression(CRightHandSide rValue)
      throws CPATransferException {

    SMGExplicitExpressionEvaluator eee =
        new SMGExplicitExpressionEvaluator();
    return eee.evaluateAddressV2(smgState, cfaEdge, rValue);
  }

  public MemoryLocation evaluateLeftHandSide(CExpression lValue)
      throws UnrecognizedCCodeException {
    ExplicitExpressionValueVisitor evv =
        new ExplicitExpressionValueVisitor();
    return evv.evaluateMemloc(lValue);
  }

  public SMGAddress evaluateSMGLeftHandSide(CExpression lValue)
      throws UnrecognizedCCodeException {

    ExplicitExpressionValueVisitor evv =
        new ExplicitExpressionValueVisitor();
    return evv.evaluateAddress(lValue);
  }

  private class ExplicitExpressionValueVisitor
      extends org.sosy_lab.cpachecker.cpa.value.ExpressionValueVisitor {

    private final SMGExplicitExpressionEvaluator smgEvaluator;

    public ExplicitExpressionValueVisitor() {
      super(valueAnalysisState, functionName, machineModel, logger);
      smgEvaluator = new SMGExplicitExpressionEvaluator(this);
    }

    public ExplicitExpressionValueVisitor(SMGExplicitExpressionEvaluator pSmgEvaluator) {
      super(valueAnalysisState, functionName, machineModel, logger);
      smgEvaluator = pSmgEvaluator;
    }

    public SMGAddress evaluateAddress(CExpression pOperand) throws UnrecognizedCCodeException {
      SMGAddress value;

      LValueAssignmentVisitor visitor = smgEvaluator.getLValueAssignmentVisitor(cfaEdge, smgState);

      try {
        value = pOperand.accept(visitor).get(0).getObject();
      } catch (CPATransferException e) {
        if (e instanceof UnrecognizedCCodeException) {
          throw (UnrecognizedCCodeException) e;
        } else {
          logger.logDebugException(e);
          throw new UnrecognizedCCodeException(
              "Could not evluate Address of " + pOperand.toASTString(),
              cfaEdge, pOperand);
        }
      }

      return value;
    }

    @Override
    public Value visit(CPointerExpression pPointerExpression) throws UnrecognizedCCodeException {

      MemoryLocation memloc = evaluateMemloc(pPointerExpression);
      return getValueFromLocation(memloc, pPointerExpression);
    }

    @Override
    public Value visit(CFieldReference pFieldReferenceExpression)
        throws UnrecognizedCCodeException {

      MemoryLocation memloc = evaluateMemloc(pFieldReferenceExpression);
      return getValueFromLocation(memloc, pFieldReferenceExpression);
    }

    @Override
    public Value visit(CBinaryExpression pE) throws UnrecognizedCCodeException {
      Value result = super.visit(pE);
      if (!result.isUnknown()) {
        return result;
      }

      CExpression op1 = pE.getOperand1();
      CExpression op2 = pE.getOperand2();

      if (!(op1.getExpressionType().getCanonicalType() instanceof CPointerType)) {
        return Value.UnknownValue.getInstance();
      }
      if (!(op2.getExpressionType().getCanonicalType() instanceof CPointerType)) {
        return Value.UnknownValue.getInstance();
      }

      try {
        SMGAddressValue op1Value = evaluateSMGAddressExpression(op1);
        SMGAddressValue op2Value = evaluateSMGAddressExpression(op2);
        if (op1Value.isUnknown() || op2Value.isUnknown()) {
          return Value.UnknownValue.getInstance();
        }

        if (op1Value.getOffset().isUnknown() || op2Value.getOffset().isUnknown()) {
          return Value.UnknownValue.getInstance();
        }
        long op1Offset = op1Value.getOffset().getAsLong();
        long op2Offset = op2Value.getOffset().getAsLong();

        switch (pE.getOperator()) {
          case EQUALS:
            return booleanAsLong(op1Value.getObject().equals(op2Value.getObject())
                && op1Offset == op2Offset);

          case NOT_EQUALS:
            return booleanAsLong(!(op1Value.getObject().equals(op2Value.getObject())
                && op1Offset == op2Offset));

          case GREATER_EQUAL:
          case GREATER_THAN:
          case LESS_EQUAL:
          case LESS_THAN:
            if (!op1Value.getObject().equals(op2Value.getObject())) {
              return Value.UnknownValue.getInstance();
            }
            switch (pE.getOperator()) {
              case GREATER_EQUAL:
                return booleanAsLong(op1Offset >= op2Offset);
              case GREATER_THAN:
                return booleanAsLong(op1Offset > op2Offset);
              case LESS_EQUAL:
                return booleanAsLong(op1Offset <= op2Offset);
              case LESS_THAN:
                return booleanAsLong(op1Offset < op2Offset);
              default:
                throw new AssertionError();
            }

          default:
            return Value.UnknownValue.getInstance();
        }
      } catch (CPATransferException e) {
        UnrecognizedCCodeException e2 =
            new UnrecognizedCCodeException("Could not symbolically evaluate expression", pE);
        e2.initCause(e);
        throw e2;
      }
    }

    private Value booleanAsLong(boolean b) {
      return b ? new NumericValue(1L) : new NumericValue(0L);
    }

    @Override
    public Value visit(CArraySubscriptExpression pE) throws UnrecognizedCCodeException {
      MemoryLocation memloc = evaluateMemloc(pE);
      return getValueFromLocation(memloc, pE);
    }

    private Value getValueFromLocation(MemoryLocation pMemloc, CExpression rValue)
        throws UnrecognizedCCodeException {

      if (pMemloc == null) {
        return Value.UnknownValue.getInstance();
      }

      ValueAnalysisState explState = getState();

      if (explState.contains(pMemloc)) {
        return explState.getValueFor(pMemloc);
      } else {
        // In rare cases, through reinterpretations of nullified blocks, smgState can evaluate an
        // explicit value while explicit state cannot.
        // TODO Erase when null reinterpretations an memset is implemented for explicit state
        SMGSymbolicValue value;

        try {
          value = smgEvaluator.evaluateExpressionValueV2(smgState, cfaEdge, rValue);
        } catch (CPATransferException e) {
          logger.logDebugException(e);
          throw new UnrecognizedCCodeException("Rvalue Could not be evaluated by smgEvaluator",
              cfaEdge, rValue);
        }

        if (value.isUnknown() || value.getAsInt() != 0) {
          return Value.UnknownValue.getInstance();
        } else {
          return new NumericValue(0L);
        }
      }
    }

    public MemoryLocation evaluateMemloc(CExpression pOperand) throws UnrecognizedCCodeException {

      SMGAddress value = evaluateAddress(pOperand);

      if (value == null || value.isUnknown()) {
        return null;
      } else {
        return smgState.resolveMemLoc(value, getFunctionName());
      }
    }
  }

  private class SMGExplicitExpressionEvaluator extends SMGExpressionEvaluator {

    ExplicitExpressionValueVisitor evv;

    public SMGExplicitExpressionEvaluator(ExplicitExpressionValueVisitor pEvv) {
      super(logger, machineModel);
      evv = pEvv;
    }

    public SMGExplicitExpressionEvaluator() {
      super(logger, machineModel);
      evv = new ExplicitExpressionValueVisitor(this);
    }

    @Override
    public SMGExplicitValue evaluateExplicitValueV2(
        SMGState pSmgState,
        CFAEdge pCfaEdge,
        CRightHandSide pRValue)
        throws CPATransferException {
      Value value = pRValue.accept(evv);

      if (value.isExplicitlyKnown() && value.isNumericValue()) {
        return SMGKnownExpValue.valueOf(value.asNumericValue().longValue());
      } else {
        return SMGUnknownValue.getInstance();
      }
    }
  }
}
