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
package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.checker.ExpressionCell;
import org.sosy_lab.cpachecker.core.interfaces.function.FunctionAdapter;
import org.sosy_lab.cpachecker.core.interfaces.function.MapParser;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

public class ValueFunctionAdapter implements FunctionAdapter<ValueAnalysisState, Value> {

  private static ValueFunctionAdapter INSTANCE = null;

  private MachineModel model;
  private HashMap<String, String> handlerMap;

  private final boolean activeness;

  private String functionName;
  private LogManagerWithoutDuplicates logger;

  private ValueFunctionAdapter(String pFunctionName, LogManager pLogger) {
    Optional<CFAInfo> cfaInfo = GlobalInfo.getInstance().getCFAInfo();
    if (cfaInfo.isPresent()) {
      model = cfaInfo.get().getCFA().getMachineModel();
    } else {
      throw new AssertionError("Valid CFA is required for function analysis");
    }

    handlerMap = Maps.newHashMap();
    Path mapFile = GlobalInfo.getInstance().queryMapFilePath(getClass());
    if (mapFile != null) {
      MapParser.loadFromFile(mapFile, handlerMap);
    }
    activeness = GlobalInfo.getInstance().queryActiveness(getClass());

    functionName = pFunctionName;
    logger = new LogManagerWithoutDuplicates(pLogger);
  }

  public static ValueFunctionAdapter instance(String pFunctionName, LogManager pLogger) {
    if (INSTANCE == null) {
      INSTANCE = new ValueFunctionAdapter(pFunctionName, pLogger);
    } else {
      // no need to modify log manager
      INSTANCE.functionName = pFunctionName;
    }
    return INSTANCE;
  }

  @Override
  public ExpressionCell<ValueAnalysisState, Value> evaluateFunctionCallExpression(
      CFunctionCallExpression pFunctionCallExpression,
      List<Value> arguments,
      ValueAnalysisState currentState,
      List<AbstractState> currentOtherStates) {
    Value defaultValue = UnknownValue.getInstance();
    ExpressionCell<ValueAnalysisState, Value> defaultCell = new ExpressionCell<>(currentState,
        currentOtherStates, arguments, defaultValue);
    if (!activeness) {
      return defaultCell;
    }
    if (pFunctionCallExpression.getParameterExpressions().size() == arguments.size()) {
      CExpression name = pFunctionCallExpression.getFunctionNameExpression();
      if (name instanceof CIdExpression) {
        CSimpleDeclaration funcDecl = ((CIdExpression) name).getDeclaration();
        String funcName;
        if (funcDecl == null) {
          funcName = ((CIdExpression) name).getName();
        } else {
          funcName = funcDecl.getName();
        }
        List<CExpression> parameters = pFunctionCallExpression.getParameterExpressions();
        ExpressionCell<ValueAnalysisState, Value> cell = handleFunction(funcName, parameters,
            arguments, currentState, currentOtherStates);
        if (cell != null) {
          return cell;
        }
      }
    }
    return defaultCell;
  }

  @Override
  public ValueAnalysisState refineFunctionCallExpression(
      CFunctionCallExpression pCFunctionCallExpression,
      Value restriction,
      ValueAnalysisState currentState,
      List<AbstractState> currentOtherStates) {
    // for now we make no reverse refinement
    return currentState;
  }

  @Override
  public boolean isRegistered(CFunctionCallExpression pCFunctionCallExpression) {
    CExpression name = pCFunctionCallExpression.getFunctionNameExpression();
    if (name instanceof CIdExpression) {
      CSimpleDeclaration decl = ((CIdExpression) name).getDeclaration();
      String funcName;
      if (decl == null) {
        funcName = ((CIdExpression) name).getName();
      } else {
        funcName = decl.getName();
      }
      String handlerName = handlerMap.get(funcName);
      if (handlerName != null) {
        return true;
      }
      handlerName = funcName;
      boolean isReg = true;
      try {
        getClass().getDeclaredMethod(handlerName, List.class, List.class, ValueAnalysisState
            .class, List.class);
      } catch (NoSuchMethodException ex) {
        isReg = false;
      }
      return isReg;
    }
    return false;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private ExpressionCell<ValueAnalysisState, Value> handleFunction(
      String name, List<CExpression> parameters, List<Value> arguments, ValueAnalysisState
      currentState, List<AbstractState> currentOtherStates) {

    String handlerName = handlerMap.get(name);
    if (handlerName == null) {
      handlerName = name;
    }
    try {
      Method targetMethod = getClass().getDeclaredMethod(handlerName, List.class, List.class,
          ValueAnalysisState.class, List.class);
      return (ExpressionCell<ValueAnalysisState, Value>) targetMethod.invoke(this, parameters,
          arguments, currentState, currentOtherStates);
    } catch (NoSuchMethodException ex) {
      return null;
    } catch (IllegalAccessException ex) {
      return null;
    } catch (InvocationTargetException ex) {
      return null;
    }
  }

  private ExpressionCell<ValueAnalysisState, Value> scanf(
      List<CExpression> parameters, List<Value> arguments, ValueAnalysisState currentState,
      List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    // int fscanf( FILE * stream, const char * format, ... );
    ValueAnalysisState newState = currentState;
    for (int i = 1; i < parameters.size(); i++) {
      CExpression parameter = parameters.get(i);
      newState = relaxExpression(parameter, newState);
    }
    // since the matching can be partially failed, the return value should be undetermined
    Value resultValue = UnknownValue.getInstance();
    return new ExpressionCell<>(newState, currentOtherStates, arguments, resultValue);
  }

  private ExpressionCell<ValueAnalysisState, Value> fscanf(
      List<CExpression> parameters, List<Value> arguments, ValueAnalysisState currentState,
      List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    // int fscanf( FILE * stream, const char * format, ... );
    ValueAnalysisState newState = currentState;
    for (int i = 2; i < parameters.size(); i++) {
      CExpression parameter = parameters.get(i);
      newState = relaxExpression(parameter, newState);
    }
    // since the matching can be partially failed, the return value should be undetermined
    Value resultValue = UnknownValue.getInstance();
    return new ExpressionCell<>(newState, currentOtherStates, arguments, resultValue);
  }

  private ValueAnalysisState relaxExpression(CExpression pExpression, ValueAnalysisState state)
      throws UnrecognizedCCodeException {
    if (pExpression instanceof CUnaryExpression) {
      if (((CUnaryExpression) pExpression).getOperator() == UnaryOperator.AMPER) {
        CExpression possibleNum = ((CUnaryExpression) pExpression).getOperand();
        if (Types.isNumericalType(possibleNum.getExpressionType())) {
          ExpressionValueVisitor visitor = getVisitor(state);
          MemoryLocation location = visitor.evaluateMemoryLocation(possibleNum);
          if (location != null) {
            // we just forget the concrete value of this operand
            state.forget(location);
          }
        }
      }
    }
    return state;
  }

  private ExpressionValueVisitor getVisitor(ValueAnalysisState state) {
    return new ExpressionValueVisitor(state, functionName, model, logger);
  }

}
