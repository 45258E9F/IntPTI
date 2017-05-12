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
package org.sosy_lab.cpachecker.cpa.shape.function;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.function.DirectFunctionAdapter;
import org.sosy_lab.cpachecker.core.interfaces.function.MapParser;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackState;
import org.sosy_lab.cpachecker.cpa.range.util.CompIntegers;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdgeFilter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGResource;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.NumberKind;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.ExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndStateList;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.ValueAndState;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The function handles functions with numeric return type.
 * The type of abstract domain element is the list of {@link ValueAndState}.
 * Note: we do not use expression checker to capture property violation, thus we directly
 * evaluate the actual argument value in handling each function call.
 */
public class ShapeValueAdapter implements DirectFunctionAdapter<ShapeState, List<ValueAndState>> {

  private static ShapeValueAdapter INSTANCE = null;

  private MachineModel machineModel;
  private HashMap<String, String> handlerMap;

  private final boolean activeness;

  /**
   * This value is used as a dummy character denoting unknown character.
   */
  private static final char UNKNOWN_CHAR = Character.MAX_VALUE;

  private ShapeValueAdapter() {
    Optional<CFAInfo> cfaInfo = GlobalInfo.getInstance().getCFAInfo();
    if (cfaInfo.isPresent()) {
      machineModel = cfaInfo.get().getCFA().getMachineModel();
    } else {
      throw new AssertionError("Valid CFA is required for function analysis");
    }
    handlerMap = Maps.newHashMap();
    Path mapFile = GlobalInfo.getInstance().queryMapFilePath(getClass());
    if (mapFile != null) {
      MapParser.loadFromFile(mapFile, handlerMap);
    }
    activeness = GlobalInfo.getInstance().queryActiveness(getClass());
  }

  public static ShapeValueAdapter instance() {
    if (INSTANCE == null) {
      INSTANCE = new ShapeValueAdapter();
    }
    return INSTANCE;
  }

  @Override
  public List<ValueAndState> evaluateFunctionCallExpression(
      CFunctionCallExpression pFunctionCallExpression,
      ShapeState currentState,
      List<AbstractState> currentOtherStates,
      CFAEdge edge) {
    List<ValueAndState> defaultResult = new ArrayList<>();
    defaultResult.add(ValueAndState.of(currentState));
    if (!activeness) {
      return defaultResult;
    }
    CExpression name = pFunctionCallExpression.getFunctionNameExpression();
    if (name instanceof CIdExpression) {
      CSimpleDeclaration declaration = ((CIdExpression) name).getDeclaration();
      String funcName;
      if (declaration == null) {
        funcName = ((CIdExpression) name).getName();
      } else {
        funcName = declaration.getName();
      }
      List<CExpression> parameters = pFunctionCallExpression.getParameterExpressions();
      List<ValueAndState> results = handleFunction(funcName, parameters, currentState,
          currentOtherStates, edge);
      if (results != null) {
        return results;
      }
    }
    return defaultResult;
  }

  @Override
  public boolean isRegistered(CFunctionCallExpression pCFunctionCallExpression) {
    CExpression name = pCFunctionCallExpression.getFunctionNameExpression();
    if (name instanceof CIdExpression) {
      CSimpleDeclaration declaration = ((CIdExpression) name).getDeclaration();
      String funcName;
      if (declaration == null) {
        funcName = ((CIdExpression) name).getName();
      } else {
        funcName = declaration.getName();
      }
      String handlerName = handlerMap.get(funcName);
      if (handlerName != null) {
        return true;
      }
      handlerName = funcName;
      boolean isReg = true;
      try {
        getClass().getDeclaredMethod(handlerName, List.class, ShapeState.class, List.class,
            CFAEdge.class);
      } catch (NoSuchMethodException ex) {
        isReg = false;
      }
      return isReg;
    }
    return false;
  }

  /* ****************************** */
  /* methods for function semantics */
  /* ****************************** */

  @Nullable
  @SuppressWarnings("unchecked")
  private List<ValueAndState> handleFunction(
      String name, List<CExpression> parameters, ShapeState currentState, List<AbstractState>
      currentOtherStates, CFAEdge cfaEdge) {
    String handlerName = handlerMap.get(name);
    if (handlerName == null) {
      handlerName = name;
    }
    try {
      Method targetMethod = getClass().getDeclaredMethod(handlerName, List.class, ShapeState
          .class, List.class, CFAEdge.class);
      return (List<ValueAndState>) targetMethod.invoke(this, parameters, currentState,
          currentOtherStates, cfaEdge);
    } catch (NoSuchMethodException ex) {
      try {
        return handleUnknownFunction(parameters, currentState, currentOtherStates, cfaEdge);
      } catch (CPATransferException ex2) {
        return null;
      }
    } catch (IllegalAccessException | InvocationTargetException ex) {
      return null;
    }
  }

  /**
   * For unknown functions, we evaluate its arguments to detect bugs in argument expressions such
   * as invalid read.
   */
  @SuppressWarnings("unused")
  private List<ValueAndState> handleUnknownFunction(
      List<CExpression> parameters,
      ShapeState currentState,
      List<AbstractState> currentOtherStates,
      CFAEdge cfaEdge) throws CPATransferException {
    List<ShapeState> states = new ArrayList<>();
    states.add(currentState);
    for (CExpression parameter : parameters) {
      List<ShapeState> newStates = new ArrayList<>();
      for (ShapeState state : states) {
        SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance()
            .evaluateSymbolicValue(state, currentOtherStates, cfaEdge, parameter);
        newStates.addAll(FluentIterable.from(valueAndStates.asSymbolicValueAndStateList())
            .transform(new Function<SymbolicValueAndState, ShapeState>() {
              @Override
              public ShapeState apply(SymbolicValueAndState pSymbolicValueAndState) {
                return pSymbolicValueAndState.getShapeState();
              }
            }).toList());
      }
      states = newStates;
    }
    return FluentIterable.from(states).transform(new Function<ShapeState, ValueAndState>() {
      @Override
      public ValueAndState apply(ShapeState pShapeState) {
        return ValueAndState.of(pShapeState);
      }
    }).toList();
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> asin(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        if (dArg >= -1.0 && dArg <= 1.0) {
          results.add(ValueAndState.of(newState, new NumericValue(Math.asin(dArg))));
          continue;
        }
      }
      // otherwise, we append an unknown value here
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> acos(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        if (dArg >= -1.0 && dArg <= 1.0) {
          results.add(ValueAndState.of(newState, new NumericValue(Math.acos(dArg))));
          continue;
        }
      }
      // otherwise, we append an unknown value here
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> atan(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.atan(dArg))));
        continue;
      }
      // otherwise, we append an unknown value here
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> atan2(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 2) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg1 = arguments.get(0);
    CExpression arg2 = arguments.get(1);
    List<ValueAndState> results = new ArrayList<>();
    List<ValueAndState> arg1Results = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg1);
    for (ValueAndState arg1Result : arg1Results) {
      Value arg1Value = arg1Result.getObject();
      ShapeState newState = arg1Result.getShapeState();
      if (!arg1Value.isNumericValue()) {
        results.add(ValueAndState.of(newState));
        continue;
      }
      double dArg1 = arg1Value.asNumericValue().doubleValue();
      List<ValueAndState> arg2Results = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
          (newState, currentOtherStates, cfaEdge, arg2);
      for (ValueAndState arg2Result : arg2Results) {
        Value arg2Value = arg2Result.getObject();
        newState = arg2Result.getShapeState();
        if (!arg2Value.isNumericValue()) {
          results.add(ValueAndState.of(newState));
          continue;
        }
        double dArg2 = arg2Value.asNumericValue().doubleValue();
        if (CompIntegers.isAlmostZero(dArg2)) {
          results.add(ValueAndState.of(newState));
          continue;
        }
        results.add(ValueAndState.of(newState, new NumericValue(Math.atan2(dArg1, dArg2))));
      }
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> sin(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.sin(dArg))));
        continue;
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> cos(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.cos(dArg))));
        continue;
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> tan(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.tan(dArg))));
        continue;
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> exp(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.exp(dArg))));
        continue;
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> exp2(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.pow(2.0, dArg))));
        continue;
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> expm1(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.expm1(dArg))));
        continue;
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> log(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        if (dArg > 0) {
          results.add(ValueAndState.of(newState, new NumericValue(Math.log(dArg))));
          continue;
        }
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> log10(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        if (dArg > 0) {
          results.add(ValueAndState.of(newState, new NumericValue(Math.log10(dArg))));
          continue;
        }
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> log1p(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        if (dArg > 0) {
          results.add(ValueAndState.of(newState, new NumericValue(Math.log1p(dArg))));
          continue;
        }
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> log2(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        if (dArg > 0) {
          results.add(ValueAndState.of(newState, new NumericValue(Math.log(dArg) / Math.log(2))));
          continue;
        }
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> cbrt(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.cbrt(dArg))));
        continue;
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> sqrt(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        if (dArg >= 0.0) {
          results.add(ValueAndState.of(newState, new NumericValue(Math.sqrt(dArg))));
          continue;
        }
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> abs(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.abs(dArg))));
        continue;
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> ceil(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.ceil(dArg))));
        continue;
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> floor(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.floor(dArg))));
        continue;
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> round(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg = arguments.get(0);
    List<ValueAndState> argResults = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg);
    List<ValueAndState> results = new ArrayList<>(argResults.size());
    for (ValueAndState argResult : argResults) {
      Value argValue = argResult.getObject();
      ShapeState newState = argResult.getShapeState();
      if (argValue.isNumericValue()) {
        double dArg = argValue.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.round(dArg))));
        continue;
      }
      results.add(ValueAndState.of(newState));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> pow(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 2) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg1 = arguments.get(0);
    CExpression arg2 = arguments.get(1);
    List<ValueAndState> results = new ArrayList<>();
    List<ValueAndState> arg1Results = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg1);
    for (ValueAndState arg1Result : arg1Results) {
      Value arg1Value = arg1Result.getObject();
      ShapeState newState = arg1Result.getShapeState();
      if (!arg1Value.isNumericValue()) {
        results.add(ValueAndState.of(newState));
        continue;
      }
      double dArg1 = arg1Value.asNumericValue().doubleValue();
      List<ValueAndState> arg2Results = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
          (newState, currentOtherStates, cfaEdge, arg2);
      for (ValueAndState arg2Result : arg2Results) {
        Value arg2Value = arg2Result.getObject();
        newState = arg2Result.getShapeState();
        if (!arg2Value.isNumericValue()) {
          results.add(ValueAndState.of(newState));
          continue;
        }
        double dArg2 = arg2Value.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.pow(dArg1, dArg2))));
      }
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> fmax(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 2) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg1 = arguments.get(0);
    CExpression arg2 = arguments.get(1);
    List<ValueAndState> results = new ArrayList<>();
    List<ValueAndState> arg1Results = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg1);
    for (ValueAndState arg1Result : arg1Results) {
      Value arg1Value = arg1Result.getObject();
      ShapeState newState = arg1Result.getShapeState();
      if (!arg1Value.isNumericValue()) {
        results.add(ValueAndState.of(newState));
        continue;
      }
      double dArg1 = arg1Value.asNumericValue().doubleValue();
      List<ValueAndState> arg2Results = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
          (newState, currentOtherStates, cfaEdge, arg2);
      for (ValueAndState arg2Result : arg2Results) {
        Value arg2Value = arg2Result.getObject();
        newState = arg2Result.getShapeState();
        if (!arg2Value.isNumericValue()) {
          results.add(ValueAndState.of(newState));
          continue;
        }
        double dArg2 = arg2Value.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.max(dArg1, dArg2))));
      }
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> fmin(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 2) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression arg1 = arguments.get(0);
    CExpression arg2 = arguments.get(1);
    List<ValueAndState> results = new ArrayList<>();
    List<ValueAndState> arg1Results = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
        (currentState, currentOtherStates, cfaEdge, arg1);
    for (ValueAndState arg1Result : arg1Results) {
      Value arg1Value = arg1Result.getObject();
      ShapeState newState = arg1Result.getShapeState();
      if (!arg1Value.isNumericValue()) {
        results.add(ValueAndState.of(newState));
        continue;
      }
      double dArg1 = arg1Value.asNumericValue().doubleValue();
      List<ValueAndState> arg2Results = CoreShapeAdapter.getInstance().evaluateRawExplicitValue
          (newState, currentOtherStates, cfaEdge, arg2);
      for (ValueAndState arg2Result : arg2Results) {
        Value arg2Value = arg2Result.getObject();
        newState = arg2Result.getShapeState();
        if (!arg2Value.isNumericValue()) {
          results.add(ValueAndState.of(newState));
          continue;
        }
        double dArg2 = arg2Value.asNumericValue().doubleValue();
        results.add(ValueAndState.of(newState, new NumericValue(Math.min(dArg1, dArg2))));
      }
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> scanf(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() < 2) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression formatString = arguments.get(0);
    Preconditions.checkArgument(formatString instanceof CStringLiteralExpression);
    String format = ((CStringLiteralExpression) formatString).getContentString();
    // FIX: the definition of format string for scanf() is different from that for printf().
    List<CType> targetTypes = extractTypesFromScanfFormatString(format);
    int targetNum = targetTypes.size();
    for (int i = targetNum; i < arguments.size() - 1; i++) {
      targetTypes.add(new CProblemType("unknown"));
    }
    ShapeState newState = new ShapeState(currentState);
    // create a state queue for iterative state updating
    List<ShapeState> stateQueue = new ArrayList<>();
    stateQueue.add(newState);
    for (int i = 1; i < arguments.size(); i++) {
      List<ShapeState> newStateQueue = new ArrayList<>(stateQueue.size());
      CExpression arg = arguments.get(i);
      CType targetType = targetTypes.get(i - 1);
      if (targetType instanceof CProblemType) {
        continue;
      }
      for (ShapeState singleState : stateQueue) {
        AddressValueAndStateList addressValues = CoreShapeAdapter.getInstance()
            .evaluateAddressValue(singleState, currentOtherStates, cfaEdge, arg);
        for (AddressValueAndState addressValue : addressValues.asAddressValueAndStateList()) {
          ShapeAddressValue address = addressValue.getObject();
          ShapeState newerState = addressValue.getShapeState();
          if (address.isUnknown()) {
            newStateQueue.add(newerState);
            continue;
          }
          SGObject object = address.getObject();
          int offset = address.getOffset().getAsInt();
          // FIX: if the target type is const char*, then we should remove all the has-value
          // edges in the specified object
          newerState = newerState.removeValue(object, offset, targetType);
          newStateQueue.add(newerState);
        }
      }
      // if we reach here, the target type should not the problem type
      stateQueue.clear();
      stateQueue.addAll(newStateQueue);
    }
    // finally, we create ValueAndState instances given derived states
    // TODO: how about the format string specifier and the type of given argument do not match
    int numberOfMatching = arguments.size() - 1;
    Value resultValue = new NumericValue(numberOfMatching);
    List<ValueAndState> results = new ArrayList<>(stateQueue.size());
    for (ShapeState sState : stateQueue) {
      results.add(ValueAndState.of(sState, resultValue));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> fscanf(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() < 3) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression formatString = arguments.get(1);
    Preconditions.checkArgument(formatString instanceof CStringLiteralExpression);
    String format = ((CStringLiteralExpression) formatString).getContentString();
    List<CType> targetTypes = extractTypesFromScanfFormatString(format);
    int targetNum = targetTypes.size();
    for (int i = targetNum; i < arguments.size() - 2; i++) {
      targetTypes.add(new CProblemType("unknown"));
    }
    ShapeState newState = new ShapeState(currentState);
    // create a state queue for iterative state updating
    List<ShapeState> stateQueue = new ArrayList<>();
    stateQueue.add(newState);
    for (int i = 2; i < arguments.size(); i++) {
      List<ShapeState> newStateQueue = new ArrayList<>(stateQueue.size());
      CExpression arg = arguments.get(i);
      CType targetType = targetTypes.get(i - 2);
      if (targetType instanceof CProblemType) {
        continue;
      }
      for (ShapeState singleState : stateQueue) {
        AddressValueAndStateList addressValues = CoreShapeAdapter.getInstance()
            .evaluateAddressValue(singleState, currentOtherStates, cfaEdge, arg);
        for (AddressValueAndState addressValue : addressValues.asAddressValueAndStateList()) {
          ShapeAddressValue address = addressValue.getObject();
          ShapeState newerState = addressValue.getShapeState();
          if (address.isUnknown()) {
            newStateQueue.add(newerState);
            continue;
          }
          SGObject object = address.getObject();
          int offset = address.getOffset().getAsInt();
          // FIX: is it appropriate to simply remove values in this memory region?
          newerState = newerState.removeValue(object, offset, targetType);
          newStateQueue.add(newerState);
        }
      }
      // if we reach here, the target type should not the problem type
      stateQueue.clear();
      stateQueue.addAll(newStateQueue);
    }
    int numberOfMatching = arguments.size() - 2;
    Value resultValue = new NumericValue(numberOfMatching);
    List<ValueAndState> results = new ArrayList<>(stateQueue.size());
    for (ShapeState sState : stateQueue) {
      results.add(ValueAndState.of(sState, resultValue));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> printf(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() < 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression formatString = arguments.get(0);
    Preconditions.checkArgument(formatString instanceof CStringLiteralExpression);
    String format = ((CStringLiteralExpression) formatString).getContentString();
    List<CType> targetTypes = extractTypesFromPrintfFormatString(format);
    int targetNum = targetTypes.size();
    // TODO: undefined behavior when there are more format specifiers than to-be-filled arguments
    for (int i = targetNum; i < arguments.size() - 1; i++) {
      // additional arguments will also be evaluated
      targetTypes.add(new CProblemType("unknown"));
    }
    ShapeState newState = new ShapeState(currentState);
    List<ShapeState> states = new ArrayList<>();
    states.add(newState);
    for (int i = 1; i < arguments.size(); i++) {
      List<ShapeState> newStates = new ArrayList<>(states.size());
      CExpression arg = arguments.get(i);
      CType targetType = targetTypes.get(i - 1);
      if (targetType.equals(CPointerType.POINTER_TO_CONST_CHAR)) {
        // check if the string to be read is null-terminated
        for (ShapeState state : states) {
          AddressValueAndStateList stringValueAndStates = CoreShapeAdapter.getInstance()
              .evaluateAddressValue(state, currentOtherStates, cfaEdge, arg);
          for (AddressValueAndState stringValueAndState : stringValueAndStates
              .asAddressValueAndStateList()) {
            ShapeAddressValue stringValue = stringValueAndState.getObject();
            ShapeState newerState = stringValueAndState.getShapeState();
            if (!CoreShapeAdapter.getInstance().isNullTerminated(newerState, stringValue)) {
              newerState = newerState.setInvalidRead(arg);
            }
            newStates.add(newerState);
          }
        }
      } else {
        for (ShapeState state : states) {
          SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance()
              .evaluateSymbolicValue(state, currentOtherStates, cfaEdge, arg);
          newStates.addAll(FluentIterable.from(valueAndStates.asSymbolicValueAndStateList())
              .transform(new Function<SymbolicValueAndState, ShapeState>() {
                @Override
                public ShapeState apply(SymbolicValueAndState pSymbolicValueAndState) {
                  return pSymbolicValueAndState.getShapeState();
                }
              }).toList());
        }
      }
      states = newStates;
    }
    // printf() returns the length of output string, this is, generally, uncertain in compile time
    return FluentIterable.from(states).transform(new Function<ShapeState, ValueAndState>() {
      @Override
      public ValueAndState apply(ShapeState pShapeState) {
        return ValueAndState.of(pShapeState);
      }
    }).toList();
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> snprintf(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // int snprintf (char * s, size_t n, const char * format, ...)
    // Compose a string with the same text that would be printed if format was used on printf(),
    // but instead of being printed, the content is stored as a C string n the buffer pointed by
    // s (while taking n as the maximum buffer capacity to fill)
    // ** The return value is the number of characters to be written if n HAD BEEN sufficiently
    // large, NOT counting the terminating null character.
    // ** The generated string has a length of at most n-1, leaving space for the additional
    // terminating null character.
    // ** The buffer should have the size of at least n characters, otherwise buffer overflow
    // could occur.
    if (arguments.size() < 3) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression result, size, formatString;
    result = arguments.get(0);
    size = arguments.get(1);
    formatString = arguments.get(2);
    List<CExpression> formattedValues = new ArrayList<>();
    for (int i = 3; i < arguments.size(); i++) {
      formattedValues.add(arguments.get(i));
    }
    Preconditions.checkArgument(formatString instanceof CStringLiteralExpression);
    String format = ((CStringLiteralExpression) formatString).getContentString();
    List<FormatSpecifier> specifiers = extractFormatSpecifierFromPrintfFormatString(format);
    ShapeState newState = new ShapeState(currentState);
    List<ValueAndState> results = new ArrayList<>();
    AddressValueAndStateList resultValueAndStates = CoreShapeAdapter.getInstance()
        .evaluateAddressValue(newState, currentOtherStates, cfaEdge, result);
    for (AddressValueAndState resultValueAndState : resultValueAndStates
        .asAddressValueAndStateList()) {
      ShapeAddressValue resultValue = resultValueAndState.getObject();
      newState = resultValueAndState.getShapeState();
      if (resultValue.isUnknown()) {
        // we do not know which buffer should be updated
        results.add(ValueAndState.of(newState));
        continue;
      }
      List<ExplicitValueAndState> sizeValueAndStates = CoreShapeAdapter.getInstance()
          .evaluateExplicitValue(newState, currentOtherStates, cfaEdge, size);
      for (ExplicitValueAndState sizeValueAndState : sizeValueAndStates) {
        ShapeExplicitValue sizeValue = sizeValueAndState.getObject();
        newState = sizeValueAndState.getShapeState();
        if (sizeValue.isUnknown()) {
          // we do not know how many bits are allowed to be written
          // by default, buffer overflow could occur
          results.add(ValueAndState.of(newState.setInvalidWrite()));
          continue;
        }
        // derive the concrete limiting size for convenience
        int limitedSize = sizeValue.getAsInt();
        // evaluate the remaining arguments and convert their values into strings
        List<StringOutput> outputs = new ArrayList<>();
        outputs.add(new StringOutput(newState, "", false));
        Iterator<CExpression> valueVisitor = formattedValues.iterator();
        int workingPos = 0;
        for (FormatSpecifier specifier : specifiers) {
          // First, we evaluate external width or/and precision if necessary
          if (specifier.needWidthFromArg()) {
            // WARNING: Iterator consumption should not be put in the enumeration of states
            if (!valueVisitor.hasNext()) {
              // the number of formatted values is not consistent with the number of format
              // specifiers, this is undefined behavior
              return forceStateDerivation(outputs);
            }
            CExpression visitedValue = valueVisitor.next();
            for (int i = 0; i < outputs.size(); i++) {
              StringOutput nextOutput = outputs.get(i);
              ShapeState nextState = nextOutput.state;
              ShapeExplicitValue extWidth = CoreShapeAdapter.getInstance()
                  .evaluateSingleExplicitValue(nextState, currentOtherStates, cfaEdge,
                      visitedValue);
              if (!extWidth.isUnknown()) {
                nextOutput.setExternalWidth(extWidth.getAsInt());
                outputs.set(i, nextOutput);
              }
            }
          }
          if (specifier.needPrecisionFromArg()) {
            if (!valueVisitor.hasNext()) {
              return forceStateDerivation(outputs);
            }
            CExpression visitedValue = valueVisitor.next();
            for (int i = 0; i < outputs.size(); i++) {
              StringOutput nextOutput = outputs.get(i);
              ShapeState nextState = nextOutput.state;
              ShapeExplicitValue extPrecision = CoreShapeAdapter.getInstance()
                  .evaluateSingleExplicitValue(nextState, currentOtherStates, cfaEdge,
                      visitedValue);
              if (!extPrecision.isUnknown()) {
                nextOutput.setExternalPrecision(extPrecision.getAsInt());
                outputs.set(i, nextOutput);
              }
            }
          }
          // SECOND, we evaluate the argument according to the parsed specifier
          if (!valueVisitor.hasNext()) {
            return forceStateDerivation(outputs);
          }
          CExpression visitedValue = valueVisitor.next();
          List<StringOutput> innerOutputs = new ArrayList<>();
          switch (specifier.specifier) {
            case "s": {
              // string value should be evaluated using an address evaluator
              for (StringOutput output : outputs) {
                ShapeState nextState = output.state;
                String nextContent = output.content;
                boolean fuzzyMode = output.fuzzyMode;
                nextContent = nextContent.concat(format.substring(workingPos, specifier.start));
                workingPos = specifier.end;
                // By generating java format specifier, we also update derived width and precision
                Pair<String, CType> jSpecifier = specifier.toJavaFormatSpecifier(output
                    .getExternalWidth(), output.getExternalPrecision());
                if (jSpecifier == null) {
                  ShapeState neuState = handleEndlessString(nextState, currentOtherStates, cfaEdge,
                      resultValue, nextContent, limitedSize);
                  results.add(ValueAndState.of(neuState));
                  continue;
                }
                // evaluate the address of string
                AddressValueAndStateList valueAndStates = CoreShapeAdapter.getInstance()
                    .evaluateAddressValue(nextState, currentOtherStates, cfaEdge, visitedValue);
                for (AddressValueAndState valueAndState : valueAndStates
                    .asAddressValueAndStateList()) {
                  ShapeAddressValue value = valueAndState.getObject();
                  ShapeState neuState = valueAndState.getShapeState();
                  String subResult;
                  if (value.isUnknown()) {
                    // check if precision is specified
                    Integer length = specifier.getPrecision();
                    if (length != null) {
                      // If the formatted string value is shorter, then it keeps unchanged. In
                      // this case, the length of string cannot be determined but has a
                      // theoretical maximum value. We should set the fuzzy mode for this case.
                      subResult = new String(new char[length]).replace('\0', UNKNOWN_CHAR);
                      fuzzyMode = true;
                    } else {
                      // then, the string could have infinite size
                      neuState = handleEndlessString(neuState, currentOtherStates, cfaEdge,
                          resultValue, nextContent, limitedSize);
                      results.add(ValueAndState.of(neuState));
                      continue;
                    }
                  } else {
                    // the value is known, then we try to read string from memory
                    Pair<String, ShapeState> readResult = readStringFromState(neuState,
                        currentOtherStates, cfaEdge, value, visitedValue);
                    neuState = Preconditions.checkNotNull(readResult.getSecond());
                    // if buffer overflow occurs in reading the string, we directly return the
                    // faulty state
                    if (neuState.getInvalidReadStatus()) {
                      results.add(ValueAndState.of(neuState));
                      continue;
                    }
                    subResult = Preconditions.checkNotNull(readResult.getFirst());
                    // if the fuzzy mode is off and the sub-result contains unknown byte, then we
                    // set the fuzzy mode flag
                    Integer length = specifier.getPrecision();
                    if (length != null && subResult.length() > length) {
                      subResult = subResult.substring(0, length);
                    }
                    if (!fuzzyMode) {
                      if (subResult.indexOf(UNKNOWN_CHAR) >= 0) {
                        fuzzyMode = true;
                      }
                    } else {
                      subResult = new String(new char[subResult.length()]).replace('\0',
                          UNKNOWN_CHAR);
                    }
                  }
                  // generate content using Java formatter
                  String subSpecifier = jSpecifier.getFirst();
                  assert (subSpecifier != null);
                  subResult = nextContent.concat(String.format(subSpecifier, subResult));
                  innerOutputs.add(new StringOutput(neuState, subResult, fuzzyMode));
                }
              }
              break;
            }
            case "p":
            case "n": {
              // we evaluate the corresponding argument by symbolic evaluator, but the concrete
              // pointer value is unknown
              for (StringOutput output : outputs) {
                ShapeState nextState = output.state;
                String nextContent = output.content;
                nextContent = nextContent.concat(format.substring(workingPos, specifier.start));
                workingPos = specifier.end;
                Pair<String, CType> jSpecifier = specifier.toJavaFormatSpecifier(output
                    .getExternalWidth(), output.getExternalPrecision());
                if (jSpecifier == null) {
                  ShapeState neuState = handleEndlessString(nextState, currentOtherStates, cfaEdge,
                      resultValue, nextContent, limitedSize);
                  results.add(ValueAndState.of(neuState));
                  continue;
                }
                SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance()
                    .evaluateSymbolicValue(nextState, currentOtherStates, cfaEdge, visitedValue);
                for (SymbolicValueAndState valueAndState : valueAndStates
                    .asSymbolicValueAndStateList()) {
                  ShapeState neuState = valueAndState.getShapeState();
                  String subResult;
                  if (specifier.specifier.equals("p")) {
                    // 1 byte = 2 hex digits, 2 additional bytes for "0x"
                    int length = machineModel.getSizeofPtr() * 2;
                    if (specifier.flag.equals("+")) {
                      length++;
                    }
                    subResult = new String(new char[length]).replace('\0', UNKNOWN_CHAR);
                    String subSpecifier = jSpecifier.getFirst();
                    assert (subSpecifier != null);
                    subResult = nextContent.concat(String.format(subSpecifier, subResult));
                    // we always set the fuzzy mode, since the length of address value is
                    // uncertain in general (depending on the machine architecture and OS)
                    innerOutputs.add(new StringOutput(neuState, subResult, true));
                  } else {
                    // we do not alter the content string
                    innerOutputs.add(new StringOutput(neuState, nextContent, output.fuzzyMode));
                  }
                }
              }
              break;
            }
            default: {
              // we evaluate the corresponding argument by explicit evaluator
              for (StringOutput output : outputs) {
                ShapeState nextState = output.state;
                String nextContent = output.content;
                boolean fuzzyMode = output.fuzzyMode;
                nextContent = nextContent.concat(format.substring(workingPos, specifier.start));
                workingPos = specifier.end;
                Pair<String, CType> jSpecifier = specifier.toJavaFormatSpecifier(output
                    .getExternalWidth(), output.getExternalPrecision());
                if (jSpecifier == null) {
                  ShapeState neuState = handleEndlessString(nextState, currentOtherStates, cfaEdge,
                      resultValue, nextContent, limitedSize);
                  results.add(ValueAndState.of(neuState));
                  continue;
                }
                String subSpecifier = Preconditions.checkNotNull(jSpecifier.getFirst());
                CType subType = Preconditions.checkNotNull(jSpecifier.getSecond());
                assert (subType instanceof CSimpleType);
                CSimpleType simpleType = (CSimpleType) subType;
                List<ValueAndState> valueAndStates = CoreShapeAdapter.getInstance()
                    .evaluateRawExplicitValue(nextState, currentOtherStates, cfaEdge, visitedValue);
                for (ValueAndState valueAndState : valueAndStates) {
                  Value value = valueAndState.getObject();
                  ShapeState neuState = valueAndState.getShapeState();
                  String subResult;
                  if (value.isUnknown()) {
                    // in this case, the fuzzy mode should be set, and we need to compute the
                    // maximum length of output string
                    fuzzyMode = true;
                    // TODO: we treat long double as double (which is imprecise though)
                    if (simpleType.equals(CNumericTypes.DOUBLE) || simpleType.equals(CNumericTypes
                        .LONG_DOUBLE)) {
                      subResult = String.format(subSpecifier, Double.MIN_VALUE);
                    } else {
                      Integer precision = specifier.getPrecision();
                      if (precision != null) {
                        if (simpleType.isUnsigned()) {
                          subResult = stringifyNumber(machineModel.getMaximalIntegerValue
                              (simpleType), precision, specifier.specifier);
                        } else {
                          subResult = stringifyNumber(machineModel.getMinimalIntegerValue
                              (simpleType), precision, specifier.specifier);
                        }
                        subResult = String.format(subSpecifier, subResult);
                      } else {
                        // the integral value is directly formatted by Java formatter
                        if (simpleType.isUnsigned()) {
                          subResult = String.format(subSpecifier, machineModel
                              .getMaximalIntegerValue(simpleType));
                        } else {
                          subResult = String.format(subSpecifier, machineModel
                              .getMinimalIntegerValue(simpleType));
                        }
                      }
                    }
                    subResult = new String(new char[subResult.length()]).replace('\0',
                        UNKNOWN_CHAR);
                  } else {
                    Value castedValue = AbstractExpressionValueVisitor.castCValue(value,
                        simpleType, machineModel, null, FileLocation.DUMMY);
                    if (simpleType.equals(CNumericTypes.DOUBLE) || simpleType.equals(CNumericTypes
                        .LONG_DOUBLE)) {
                      subResult = String.format(subSpecifier, castedValue.asNumericValue()
                          .doubleValue());
                    } else {
                      Integer precision = specifier.getPrecision();
                      if (precision != null) {
                        subResult = stringifyNumber(BigInteger.valueOf(castedValue.asNumericValue
                            ().longValue()), precision, specifier.specifier);
                        subResult = String.format(subSpecifier, subResult);
                      } else {
                        subResult = String.format(subSpecifier, BigInteger.valueOf(castedValue
                            .asNumericValue().longValue()));
                      }
                    }
                    if (fuzzyMode) {
                      subResult = new String(new char[subResult.length()]).replace('\0',
                          UNKNOWN_CHAR);
                    }
                  }
                  subResult = nextContent.concat(subResult);
                  innerOutputs.add(new StringOutput(neuState, subResult, fuzzyMode));
                }
              }
              break;
            }
          }
          // update intermediate outputs using inner outputs
          outputs.clear();
          outputs = innerOutputs;
        }
        final String remContent = format.substring(workingPos);
        if (!remContent.isEmpty()) {
          outputs = FluentIterable.from(outputs).transform(new Function<StringOutput, StringOutput>
              () {
            @Override
            public StringOutput apply(StringOutput pStringOutput) {
              return new StringOutput(pStringOutput.state, pStringOutput.content.concat
                  (remContent), pStringOutput.fuzzyMode);
            }
          }).toList();
        }
        // write resultant string into the memory
        for (StringOutput output : outputs) {
          ShapeState neuState = output.state;
          String content = output.content;
          Integer origLength = output.fuzzyMode ? null : content.length();
          if (limitedSize == 0) {
            results.add(origLength == null ? ValueAndState.of(neuState) : ValueAndState.of
                (neuState, new NumericValue(origLength)));
          }
          if (content.length() > limitedSize - 1) {
            content = content.substring(0, limitedSize - 1);
          }
          ShapeState nouveauState = writeStringIntoState(neuState, currentOtherStates, cfaEdge,
              resultValue, content);
          results.add(origLength == null ? ValueAndState.of(nouveauState) : ValueAndState.of(
              nouveauState, new NumericValue(origLength)));
        }
      }
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> memcmp(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    if (arguments.size() != 3) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression memOne = arguments.get(0);
    CExpression memTwo = arguments.get(1);
    CExpression num = arguments.get(2);
    List<ValueAndState> results = new ArrayList<>();
    AddressValueAndStateList memOneAddresses = CoreShapeAdapter.getInstance()
        .evaluateAddressValue(currentState, currentOtherStates, cfaEdge, memOne);
    for (AddressValueAndState memOneAddress : memOneAddresses.asAddressValueAndStateList()) {
      ShapeAddressValue addressValue1 = memOneAddress.getObject();
      ShapeState newState = memOneAddress.getShapeState();
      if (addressValue1.isUnknown()) {
        results.add(ValueAndState.of(newState));
        continue;
      }
      AddressValueAndStateList memTwoAddresses = CoreShapeAdapter.getInstance()
          .evaluateAddressValue(newState, currentOtherStates, cfaEdge, memTwo);
      for (AddressValueAndState memTwoAddress : memTwoAddresses.asAddressValueAndStateList()) {
        ShapeAddressValue addressValue2 = memTwoAddress.getObject();
        newState = memTwoAddress.getShapeState();
        if (addressValue2.isUnknown()) {
          results.add(ValueAndState.of(newState));
          continue;
        }
        List<ExplicitValueAndState> numValues = CoreShapeAdapter.getInstance()
            .evaluateExplicitValue(currentState, currentOtherStates, cfaEdge, num);
        for (ExplicitValueAndState numValue : numValues) {
          ShapeExplicitValue numVal = numValue.getObject();
          newState = numValue.getShapeState();
          if (numVal.isUnknown()) {
            results.add(ValueAndState.of(newState));
            continue;
          }
          ValueAndState result = evaluateMemcmp(newState, addressValue1, addressValue2, numVal,
              memOne, memTwo);
          results.add(result);
        }
      }
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> strlen(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // size_t strlen(const char * str);
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    List<ValueAndState> results = new ArrayList<>();
    CExpression string = arguments.get(0);
    AddressValueAndStateList stringAddresses = CoreShapeAdapter.getInstance()
        .evaluateAddressValue(currentState, currentOtherStates, cfaEdge, string);
    for (AddressValueAndState stringAddress : stringAddresses.asAddressValueAndStateList()) {
      ShapeAddressValue addressValue = stringAddress.getObject();
      ShapeState newState = stringAddress.getShapeState();
      if (addressValue.isUnknown()) {
        results.add(ValueAndState.of(newState));
        continue;
      }
      SGObject object = addressValue.getObject();
      int offset = addressValue.getOffset().getAsInt();
      int sizeofChar = machineModel.getSizeofChar();
      // then we read the value from the buffer by character
      // * value reading finishes when the '\0' or undetermined character is encountered
      boolean stopFlag = false;
      int length = 0;
      while (!stopFlag) {
        SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance().readValue
            (newState, currentOtherStates, cfaEdge, object, KnownExplicitValue.valueOf(offset),
                CNumericTypes.CHAR, string);
        ShapeState chosenState = null;
        for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
          ShapeState state = valueAndState.getShapeState();
          if (state.getInvalidReadStatus()) {
            chosenState = state;
            break;
          }
        }
        // return the invalid read state as soon as possible
        if (chosenState != null) {
          results.add(ValueAndState.of(newState));
          break;
        }
        if (valueAndStates.size() > 1) {
          chosenState = Preconditions.checkNotNull(valueAndStates.getOneElement().orNull())
              .getShapeState();
        } else {
          SymbolicValueAndState valueAndState = Iterables.getOnlyElement(valueAndStates
              .asSymbolicValueAndStateList());
          ShapeSymbolicValue value = valueAndState.getObject();
          if (value.isUnknown()) {
            chosenState = valueAndState.getShapeState();
          } else if (value.equals(KnownSymbolicValue.ZERO)) {
            stopFlag = true;
            continue;
          }
        }
        if (chosenState != null) {
          results.add(ValueAndState.of(newState));
          break;
        }
        length++;
        offset += sizeofChar;
      }
      if (stopFlag) {
        results.add(ValueAndState.of(newState, new NumericValue(length)));
      }
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> strcmp(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // int strcmp(const char * str1, const char * str2);
    // Note: two string values should have null termination, otherwise buffer overflow will occur.
    // (refer to CWE 170)
    if (arguments.size() != 2) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    List<ValueAndState> results = new ArrayList<>();
    CExpression string1 = arguments.get(0);
    CExpression string2 = arguments.get(1);
    AddressValueAndStateList addresses1 = CoreShapeAdapter.getInstance().evaluateAddressValue
        (currentState, currentOtherStates, cfaEdge, string1);
    for (AddressValueAndState address1 : addresses1.asAddressValueAndStateList()) {
      ShapeAddressValue addressValue1 = address1.getObject();
      ShapeState newState = address1.getShapeState();
      if (addressValue1.isUnknown()) {
        results.add(ValueAndState.of(newState));
        continue;
      }
      AddressValueAndStateList addresses2 = CoreShapeAdapter.getInstance().evaluateAddressValue
          (newState, currentOtherStates, cfaEdge, string2);
      for (AddressValueAndState address2 : addresses2.asAddressValueAndStateList()) {
        ShapeAddressValue addressValue2 = address2.getObject();
        newState = address2.getShapeState();
        if (addressValue2.isUnknown()) {
          results.add(ValueAndState.of(newState));
          continue;
        }
        // if we reach here, two string object exists
        SGObject object1 = addressValue1.getObject();
        SGObject object2 = addressValue2.getObject();
        int offset1 = addressValue1.getOffset().getAsInt();
        int offset2 = addressValue2.getOffset().getAsInt();
        int sizeofChar = machineModel.getSizeofChar();
        boolean canStop = false;
        while (true) {
          SymbolicValueAndStateList valueAndStates1 = CoreShapeAdapter.getInstance().readValue
              (newState, currentOtherStates, cfaEdge, object1, KnownExplicitValue.valueOf(offset1),
                  CNumericTypes.CHAR, string1);
          ShapeState chosenState = null;
          for (SymbolicValueAndState valueAndState1 : valueAndStates1
              .asSymbolicValueAndStateList()) {
            ShapeState state = valueAndState1.getShapeState();
            if (state.getInvalidReadStatus()) {
              chosenState = state;
              break;
            }
          }
          if (chosenState != null) {
            results.add(ValueAndState.of(chosenState));
            break;
          }
          ShapeSymbolicValue value1 = null;
          if (valueAndStates1.size() > 1) {
            chosenState = Preconditions.checkNotNull(valueAndStates1.getOneElement().orNull())
                .getShapeState();
          } else {
            SymbolicValueAndState valueAndState1 = Iterables.getOnlyElement
                (valueAndStates1.asSymbolicValueAndStateList());
            value1 = valueAndState1.getObject();
            if (value1.isUnknown()) {
              chosenState = valueAndState1.getShapeState();
            }
          }
          if (chosenState != null) {
            // any comparison result is possible
            results.add(ValueAndState.of(chosenState));
            break;
          }
          SymbolicValueAndStateList valueAndStates2 = CoreShapeAdapter.getInstance().readValue
              (newState, currentOtherStates, cfaEdge, object2, KnownExplicitValue.valueOf(offset2),
                  CNumericTypes.CHAR, string2);
          for (SymbolicValueAndState valueAndState2 : valueAndStates2
              .asSymbolicValueAndStateList()) {
            ShapeState state = valueAndState2.getShapeState();
            if (state.getInvalidReadStatus()) {
              chosenState = state;
              break;
            }
          }
          if (chosenState != null) {
            results.add(ValueAndState.of(chosenState));
            break;
          }
          ShapeSymbolicValue value2 = null;
          if (valueAndStates2.size() > 1) {
            chosenState = Preconditions.checkNotNull(valueAndStates2.getOneElement().orNull())
                .getShapeState();
          } else {
            SymbolicValueAndState valueAndState2 = Iterables.getOnlyElement
                (valueAndStates2.asSymbolicValueAndStateList());
            value2 = valueAndState2.getObject();
            if (value2.isUnknown()) {
              chosenState = valueAndState2.getShapeState();
            }
          }
          if (chosenState != null) {
            results.add(ValueAndState.of(chosenState));
            break;
          }

          assert (value1 != null);
          assert (value2 != null);
          ShapeExplicitValue expValue1, expValue2;
          if (value1.equals(KnownSymbolicValue.ZERO)) {
            expValue1 = KnownExplicitValue.ZERO;
            canStop = true;
          } else {
            expValue1 = newState.getExplicit((KnownSymbolicValue) value1);
          }
          if (value2.equals(KnownSymbolicValue.ZERO)) {
            expValue2 = KnownExplicitValue.ZERO;
            canStop = true;
          } else {
            expValue2 = newState.getExplicit((KnownSymbolicValue) value2);
          }
          ShapeExplicitValue delta = expValue1.subtract(expValue2);
          if (delta.isUnknown()) {
            results.add(ValueAndState.of(newState));
            break;
          }
          int intDelta = delta.getAsInt();
          if (intDelta == 0) {
            if (canStop) {
              results.add(ValueAndState.of(newState, new NumericValue(0)));
              break;
            }
          } else {
            results.add(ValueAndState.of(newState, new NumericValue(intDelta)));
            break;
          }
          offset1 += sizeofChar;
          offset2 += sizeofChar;
        }
      }
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> recv(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // ssize_t recv(int socket, void *buffer, size_t length, int flags);
    // recv() could return: (1) length, (2) 0, (3) -1.
    // Note: if the size of buffer is smaller than the specified length, we should mark an
    // invalid write here for the FIRST return case
    if (arguments.size() != 4) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression buffer = arguments.get(1);
    CExpression length = arguments.get(2);
    List<ValueAndState> results = new ArrayList<>();
    AddressValueAndStateList bufferAddress = CoreShapeAdapter.getInstance().evaluateAddressValue
        (currentState, currentOtherStates, cfaEdge, buffer);
    for (AddressValueAndState bufferAddr : bufferAddress.asAddressValueAndStateList()) {
      ShapeAddressValue address = bufferAddr.getObject();
      ShapeState newState = bufferAddr.getShapeState();
      if (address.isUnknown()) {
        results.add(ValueAndState.of(newState));
        continue;
      }
      SGObject bufferObject = address.getObject();
      int offset = address.getOffset().getAsInt();
      List<ExplicitValueAndState> lengthValues = CoreShapeAdapter.getInstance()
          .evaluateExplicitValue(newState, currentOtherStates, cfaEdge, length);
      for (ExplicitValueAndState lengthValue : lengthValues) {
        ShapeExplicitValue lengthExp = lengthValue.getObject();
        newState = lengthValue.getShapeState();
        if (!lengthExp.isUnknown()) {
          // the length is explicitly known, then we can get the exact value for the first case,
          // and check the possible buffer overflow issue
          int intLength = lengthExp.getAsInt();
          ShapeState newerState = new ShapeState(newState);
          ShapeExplicitValue bufferSize = bufferObject.getSize();
          if (!bufferSize.isUnknown()) {
            int intBuffer = bufferSize.getAsInt();
            if (offset + intLength > intBuffer) {
              newerState = newerState.setInvalidWrite();
            }
          }
          results.add(ValueAndState.of(newerState, new NumericValue(intLength)));
        } else {
          // the exact length of successful written bytes is unknown
          results.add(ValueAndState.of(newState));
        }
        // branching states should have deep copy of the original state
        // otherwise, execution of one trace may influence the execution of another trace
        // starting from the same branching point
        //results.add(ValueAndState.of(new ShapeState(newState), new NumericValue(0)));
        //results.add(ValueAndState.of(new ShapeState(newState), new NumericValue(-1)));
      }
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> fclose(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // int fclose(FILE * file);
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression file = arguments.get(0);
    List<ValueAndState> results = new ArrayList<>();
    AddressValueAndStateList fileValueAndStates = CoreShapeAdapter.getInstance()
        .evaluateAddressValue(currentState, currentOtherStates, cfaEdge, file);
    for (AddressValueAndState fileValueAndState : fileValueAndStates.asAddressValueAndStateList()) {
      ShapeAddressValue fileValue = fileValueAndState.getObject();
      ShapeState newState = fileValueAndState.getShapeState();
      if (fileValue.isUnknown()) {
        // we cannot determine the file to be closed
        // however, we have no sufficient evidence to show that this is an error
        results.add(ValueAndState.of(newState, new NumericValue(0)));
        ShapeState nState = new ShapeState(newState);
        results.add(ValueAndState.of(nState.setInvalidFree(), new NumericValue(-1)));
        continue;
      }
      long addressValue = fileValue.getAsLong();
      if (addressValue == CShapeGraph.getNullAddress()) {
        // closing a null pointer is invalid free
        results.add(ValueAndState.of(newState.setInvalidFree(), new NumericValue(-1)));
        continue;
      }
      SGObject fileObject = fileValue.getObject();
      if (!(fileObject instanceof SGResource)) {
        // try to close a non-file pointer
        results.add(ValueAndState.of(newState.setInvalidFree(), new NumericValue(-1)));
        continue;
      }
      SGResource fileRes = (SGResource) fileObject;
      newState = newState.freeResource(fileRes, "fopen");
      // if the resultant state has invalid-free set, this path should be pruned in analysis
      results.add(ValueAndState.of(newState, new NumericValue(0)));
    }
    return results;
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> open(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // int open(const char *path, int oflag, ...);
    if (arguments.size() < 2) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    String resName = "resource_ID_" + getLoopStackIdentifier(currentOtherStates) + "_Line:" +
        cfaEdge.getFileLocation().getStartingLineNumber();
    SGObject resource = currentState.getHeapObject(resName);
    if (resource == null) {
      return Lists.newArrayList(ValueAndState.of(currentState, new NumericValue(-1)));
    }
    assert (resource instanceof SGResource);
    AddressValueAndState addressAndState = CoreShapeAdapter.getInstance().getAddress(currentState,
        resource, KnownExplicitValue.ZERO);
    ShapeAddressValue addressValue = addressAndState.getObject();
    ShapeState newState = addressAndState.getShapeState();
    if (addressValue.isUnknown()) {
      return Lists.newArrayList(ValueAndState.of(newState, new NumericValue(-1)));
    }
    return Lists.newArrayList(ValueAndState.of(newState, new NumericValue(addressValue.getAsLong
        ())));
  }

  @SuppressWarnings("unused")
  private List<ValueAndState> close(
      List<CExpression> arguments, ShapeState currentState,
      List<AbstractState> currentOtherStates, CFAEdge cfaEdge)
      throws CPATransferException {
    // int close(int fildes);
    // Note: the value of file descriptor is the explicit value of address value of file resource
    if (arguments.size() != 1) {
      return Lists.newArrayList(ValueAndState.of(currentState));
    }
    CExpression desc = arguments.get(0);
    List<ValueAndState> results = new ArrayList<>();
    List<ExplicitValueAndState> descValueAndStates = CoreShapeAdapter.getInstance()
        .evaluateExplicitValue(currentState, currentOtherStates, cfaEdge, desc);
    for (ExplicitValueAndState descValueAndState : descValueAndStates) {
      ShapeExplicitValue descValue = descValueAndState.getObject();
      ShapeState newState = descValueAndState.getShapeState();
      if (descValue.isUnknown()) {
        results.add(ValueAndState.of(newState, new NumericValue(0)));
        ShapeState nState = new ShapeState(newState);
        results.add(ValueAndState.of(nState, new NumericValue(-1)));
        continue;
      }
      long symDescValue = descValue.getAsLong();
      ShapeAddressValue addrDesc = newState.getPointToForAddressValue(symDescValue);
      if (!addrDesc.isUnknown()) {
        SGObject object = addrDesc.getObject();
        if (object instanceof SGResource) {
          newState = newState.freeResource((SGResource) object, "open");
          results.add(ValueAndState.of(newState, new NumericValue(0)));
          continue;
        }
      }
      // if we reach here, the specified file descriptor is known but invalid
      results.add(ValueAndState.of(newState.setInvalidFree(), new NumericValue(-1)));
    }
    return results;
  }

  /* ***************** */
  /* auxiliary methods */
  /* ***************** */

  private static final Pattern PRINTF_FORMAT_STRING = Pattern.compile("%((?:\\+|-| |#|0)?)("
      + "(?:\\d+|\\*)?)((?:\\.(?:\\d+|\\*))?)((?:hh|h|l|ll|j|z|t|L)?)(d|i|u|o|x|X|f|F|e|E|g|G|a|A|c|s|p|n)");

  private static final Pattern SCANF_FORMAT_STRING = Pattern.compile("%((?:\\*)?)((?:\\d+)?)"
      + "((?:hh|h|l|ll|j|z|t|L)?)(d|i|u|o|x|X|f|F|e|E|g|G|a|A|c|s|p|n|\\[(?:(?:\\^)*[\\w\\\\]+)\\])");

  private List<CType> extractTypesFromPrintfFormatString(String inputString) {
    List<CType> types = Lists.newArrayList();
    Matcher matcher = PRINTF_FORMAT_STRING.matcher(inputString);
    while (matcher.find()) {
      // for now, we are not interested in other groups
      String length = matcher.group(4);
      String specifier = matcher.group(5);
      CType type = restoreTypeFromPrintfSpecifier(length, specifier);
      types.add(type);
    }
    return types;
  }

  /**
   * Convert the format string specifier to the type of the data to be modified pointed by the
   * corresponding pointer argument. For example, we convert %d into SIGNED_INT. However, we
   * convert %s to const char *, which means the whole target object should be reset.
   */
  private List<CType> extractTypesFromScanfFormatString(String inputString) {
    List<CType> types = Lists.newArrayList();
    Matcher matcher = SCANF_FORMAT_STRING.matcher(inputString);
    while (matcher.find()) {
      String ignored = matcher.group(1);
      if (ignored.equals("*")) {
        // we read a string segment from input stream but ignore it (i.e. do not store the
        // converted string to the space pointed by the corresponding argument)
        continue;
      }
      String length = matcher.group(3);
      String specifier = matcher.group(4);
      CType type = restoreTypeFromScanfSpecifier(length, specifier);
      types.add(type);
    }
    return types;
  }

  private List<FormatSpecifier> extractFormatSpecifierFromPrintfFormatString(String inputString) {
    List<FormatSpecifier> specifiers = Lists.newArrayList();
    Matcher matcher = PRINTF_FORMAT_STRING.matcher(inputString);
    while (matcher.find()) {
      FormatSpecifier specifier = new FormatSpecifier(matcher.start(), matcher.end(), matcher
          .group(1), matcher.group(2), matcher.group(3), matcher.group(4),
          matcher.group(5));
      specifiers.add(specifier);
    }
    return specifiers;
  }

  /**
   * The structure for format specifier.
   */
  private class FormatSpecifier {

    int start;
    int end;

    String flag;
    String width;
    String precision;
    String length;
    String specifier;

    FormatSpecifier(
        int pStart, int pEnd, String pFlag, String pWidth, String pPrecision,
        String pLength, String pSpecifier) {
      start = pStart;
      end = pEnd;
      flag = pFlag;
      width = pWidth;
      precision = pPrecision;
      length = pLength;
      assert (!pSpecifier.isEmpty());
      specifier = pSpecifier;
    }

    @Nullable
    Integer getPrecision() {
      if (precision.isEmpty()) {
        return null;
      }
      try {
        return Integer.parseInt(precision.substring(1));
      } catch (NumberFormatException ex) {
        return null;
      }
    }

    @Nullable
    Integer getWidth() {
      if (width.isEmpty()) {
        return null;
      }
      try {
        return Integer.parseInt(width);
      } catch (NumberFormatException ex) {
        return null;
      }
    }

    boolean needWidthFromArg() {
      return width.equals("*");
    }

    boolean needPrecisionFromArg() {
      return precision.equals(".*");
    }

    /**
     * Rebuild the format specifier for Java string formatter.
     *
     * @param pWidth width from argument, if necessary
     * @param pPrec  precision from argument, if necessary
     * @return (Java format specifier, parsed type): (1) if the whole value is NULL, then the length
     * of target value could be infinite; (2) otherwise, we have the specifier for Java formatter
     * plus a parsed type.
     */
    Pair<String, CType> toJavaFormatSpecifier(@Nullable Integer pWidth, @Nullable Integer pPrec) {
      CType resultType = restoreTypeFromPrintfSpecifier(length, specifier);
      StringBuilder specBuilder = new StringBuilder();
      specBuilder = specBuilder.append("%").append(flag);

      if (needWidthFromArg()) {
        if (pWidth == null) {
          return null;
        } else {
          // update the format specifier for evaluating length
          width = String.valueOf(pWidth);
        }
      }
      // a concrete width or nothing
      specBuilder = specBuilder.append(width);

      if (needPrecisionFromArg()) {
        if (pPrec == null) {
          // FIX: if the specifier is "s", then the maximum length can be determined
          if (specifier.equals("s")) {
            precision = "";
          } else {
            return null;
          }
        } else {
          precision = ".".concat(String.valueOf(pPrec));
        }
      }
      // a concrete precision or nothing
      // FIX: Java specifiers of integral, string and pointer do not support precision
      if (!precision.isEmpty()) {
        if ("diuoxXcspn".contains(specifier)) {
          specBuilder = specBuilder.append("s");
          String jSpecifier = specBuilder.toString();
          // remove unsupported flags
          if (" 0+".indexOf(jSpecifier.charAt(1)) >= 0) {
            jSpecifier = jSpecifier.substring(0, 1).concat(jSpecifier.substring(2));
          }
          return Pair.of(jSpecifier, resultType);
        }
      }
      // otherwise, we append the precision segment
      specBuilder = specBuilder.append(precision);

      // conversion specifier:
      // (1) p,n: the specifier is null but the type is of pointer type;
      // (2) d,i,u: they are all converted into 'd'
      switch (specifier) {
        case "p":
        case "n":
          // remove unsupported flags
          specBuilder = specBuilder.append("s");
          String jSpecifier = specBuilder.toString();
          if (" 0+".indexOf(jSpecifier.charAt(1)) >= 0) {
            jSpecifier = jSpecifier.substring(0, 1).concat(jSpecifier.substring(2));
          }
          return Pair.of(jSpecifier, resultType);
        case "i":
        case "u":
          specBuilder = specBuilder.append("d");
          return Pair.of(specBuilder.toString(), resultType);
        default:
          specBuilder = specBuilder.append(specifier);
          return Pair.of(specBuilder.toString(), resultType);
      }
    }
  }

  /**
   * The intermediate structure for analyzing formatted output content.
   */
  private class StringOutput {

    private final ShapeState state;
    private final String content;
    private final boolean fuzzyMode;
    private Integer extWidth = null;
    private Integer extPrecision = null;

    StringOutput(@Nonnull ShapeState pState, @Nonnull String pContent, boolean pMode) {
      state = pState;
      content = pContent;
      fuzzyMode = pMode;
    }

    void setExternalWidth(int pWidth) {
      extWidth = pWidth;
    }

    void setExternalPrecision(int pPrec) {
      extPrecision = pPrec;
    }

    @Nullable
    Integer getExternalWidth() {
      return extWidth;
    }

    @Nullable
    Integer getExternalPrecision() {
      return extPrecision;
    }
  }

  /**
   * Restore data type from specifier of format string. For example, %d is restored to int
   * Note: for now some corner cases are not considered
   *
   * @param length    the length part, could be empty
   * @param specifier the specifier part, must be non-empty
   * @return the restored C type
   */
  private CType restoreTypeFromPrintfSpecifier(String length, String specifier) {
    if (length.isEmpty()) {
      if (specifier.equals("c")) {
        // TODO: However, %c actually denotes int type
        return CNumericTypes.CHAR;
      } else if (specifier.equals("d") || specifier.equals("i")) {
        return CNumericTypes.INT;
      } else if (specifier.equals("u") || specifier.equals("o") || specifier
          .equalsIgnoreCase("x")) {
        return CNumericTypes.UNSIGNED_INT;
      } else if (specifier.equalsIgnoreCase("f") || specifier.equalsIgnoreCase("e") || specifier
          .equalsIgnoreCase("g") || specifier.equalsIgnoreCase("a")) {
        return CNumericTypes.DOUBLE;
      } else if (specifier.equals("s")) {
        return CPointerType.POINTER_TO_CONST_CHAR;
      } else if (specifier.equals("p")) {
        return CPointerType.POINTER_TO_VOID;
      } else if (specifier.equals("n")) {
        // %n specifier leads to buffer write operation
        // we return (int *) to denote denote this specifier
        return new CPointerType(false, false, CNumericTypes.INT);
      }
    } else if (length.equals("hh")) {
      if (specifier.equals("d") || specifier.equals("i")) {
        return CNumericTypes.SIGNED_CHAR;
      } else if (specifier.equals("u") || specifier.equals("o") || specifier
          .equalsIgnoreCase("x")) {
        return CNumericTypes.UNSIGNED_CHAR;
      } else if (specifier.equals("n")) {
        return new CPointerType(false, false, CNumericTypes.SIGNED_CHAR);
      }
    } else if (length.equals("h")) {
      if (specifier.equals("d") || specifier.equals("i")) {
        return CNumericTypes.SHORT_INT;
      } else if (specifier.equals("u") || specifier.equals("o") || specifier
          .equalsIgnoreCase("x")) {
        return CNumericTypes.UNSIGNED_SHORT_INT;
      } else if (specifier.equals("n")) {
        return new CPointerType(false, false, CNumericTypes.SHORT_INT);
      }
    } else if (length.equals("l")) {
      if (specifier.equals("d") || specifier.equals("i")) {
        return CNumericTypes.LONG_INT;
      } else if (specifier.equals("u") || specifier.equals("o") || specifier
          .equalsIgnoreCase("x")) {
        return CNumericTypes.UNSIGNED_LONG_INT;
      } else if (specifier.equals("n")) {
        return new CPointerType(false, false, CNumericTypes.LONG_INT);
      }
    } else if (length.equals("ll")) {
      if (specifier.equals("d") || specifier.equals("i")) {
        return CNumericTypes.LONG_LONG_INT;
      } else if (specifier.equals("u") || specifier.equals("o") || specifier
          .equalsIgnoreCase("x")) {
        return CNumericTypes.UNSIGNED_LONG_LONG_INT;
      } else if (specifier.equals("n")) {
        return new CPointerType(false, false, CNumericTypes.LONG_LONG_INT);
      }
    } else if (length.equals("L")) {
      if (specifier.equalsIgnoreCase("f") || specifier.equalsIgnoreCase("e") || specifier
          .equalsIgnoreCase("g") || specifier.equalsIgnoreCase("a")) {
        return CNumericTypes.LONG_DOUBLE;
      }
    }
    return new CProblemType("unknown");
  }

  private CType restoreTypeFromScanfSpecifier(String length, String specifier) {
    if (length.isEmpty()) {
      if (specifier.equals("c")) {
        return CNumericTypes.CHAR;
      } else if (specifier.equals("d") || specifier.equals("i") || specifier.equals("n")) {
        return CNumericTypes.INT;
      } else if (specifier.equals("u") || specifier.equals("o") || specifier.equals("x")) {
        return CNumericTypes.UNSIGNED_INT;
      } else if (specifier.equals("f") || specifier.equals("e") || specifier.equals("g") ||
          specifier.equals("a")) {
        return CNumericTypes.FLOAT;
      } else if (specifier.equals("p")) {
        return CPointerType.POINTER_TO_VOID;
      } else if (specifier.equals("s") || specifier.startsWith("[")) {
        return CPointerType.POINTER_TO_CONST_CHAR;
      }
    } else if (length.equals("hh")) {
      if (specifier.equals("d") || specifier.equals("i") || specifier.equals("n")) {
        return CNumericTypes.SIGNED_CHAR;
      } else if (specifier.equals("u") || specifier.equals("o") || specifier.equals("x")) {
        return CNumericTypes.UNSIGNED_CHAR;
      }
    } else if (length.equals("h")) {
      if (specifier.equals("d") || specifier.equals("i") || specifier.equals("n")) {
        return CNumericTypes.SHORT_INT;
      } else if (specifier.equals("u") || specifier.equals("o") || specifier.equals("x")) {
        return CNumericTypes.UNSIGNED_SHORT_INT;
      }
    } else if (length.equals("l")) {
      switch (specifier) {
        case "d":
        case "i":
        case "n":
          return CNumericTypes.LONG_INT;
        case "u":
        case "o":
        case "x":
          return CNumericTypes.UNSIGNED_LONG_INT;
        case "f":
        case "e":
        case "g":
        case "a":
          return CNumericTypes.DOUBLE;
      }
    } else if (length.equals("ll")) {
      if (specifier.equals("d") || specifier.equals("i") || specifier.equals("n")) {
        return CNumericTypes.LONG_LONG_INT;
      } else if (specifier.equals("u") || specifier.equals("o") || specifier.equals("x")) {
        return CNumericTypes.UNSIGNED_LONG_LONG_INT;
      }
    } else if (length.equals("L")) {
      if (specifier.equals("f") || specifier.equals("e") || specifier.equals("g") || specifier
          .equals("a")) {
        return CNumericTypes.LONG_DOUBLE;
      }
    }
    return new CProblemType("unknown");
  }

  /**
   * Convert current intermediate string outputs into resultant shape states. This method is
   * called when there are incorrect number of target values (which leads to undefined behavior).
   */
  private ImmutableList<ValueAndState> forceStateDerivation(List<StringOutput> pOutputs) {
    return FluentIterable.from(pOutputs).transform(new Function<StringOutput, ValueAndState>() {
      @Override
      public ValueAndState apply(StringOutput pStringOutput) {
        return ValueAndState.of(pStringOutput.state);
      }
    }).toList();
  }

  /**
   * Write the string of possibly infinite size into the memory.
   *
   * @param pState      a shape state
   * @param pEdge       a CFA edge
   * @param buffer      the buffer to be updated
   * @param pContent    the partial string content to be written
   * @param limitedSize specified size of bytes to be written
   * @return new state with updated buffer
   */
  private ShapeState handleEndlessString(
      ShapeState pState, List<AbstractState> pOtherStates,
      CFAEdge pEdge, ShapeAddressValue buffer,
      String pContent, int limitedSize)
      throws CPATransferException {
    if (limitedSize == 0) {
      // nothing to be changed when the maximum written length is 0
      return pState;
    }
    String result;
    int delta = limitedSize - 1 - pContent.length();
    if (delta > 0) {
      result = new String(new char[delta]).replace('\0', UNKNOWN_CHAR);
      result = pContent.concat(result);
    } else {
      result = pContent.substring(0, limitedSize - 1);
    }
    return writeStringIntoState(pState, pOtherStates, pEdge, buffer, result);
  }

  /**
   * Since Java formatter does not support precision for integral values, we simulate precision
   * specifier in C by stringify operation.
   *
   * @param value     integral value
   * @param precision precision value
   * @param specifier format specifier
   * @return output of number with precision specification
   */
  private String stringifyNumber(BigInteger value, Integer precision, String specifier) {
    String subSpecifier = "%";
    String format = specifier;
    if (format.equals("i") || format.equals("u")) {
      format = "d";
    }
    subSpecifier = subSpecifier.concat("0").concat(String.format("%d%s", precision, format));
    return String.format(subSpecifier, value);
  }

  /**
   * Load string content from the state
   *
   * @param newState      a shape state
   * @param cfaEdge       a CFA edge
   * @param pAddressValue address value of target buffer
   * @param stringExp     string expression, for the purpose of error marking
   * @return string content along with resultant state
   */
  private Pair<String, ShapeState> readStringFromState(
      ShapeState newState,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge,
      ShapeAddressValue pAddressValue,
      CExpression stringExp)
      throws CPATransferException {
    assert (!pAddressValue.isUnknown());
    SGObject bufferObject = pAddressValue.getObject();
    boolean unknownSize = bufferObject.getSize().isUnknown();
    int bufferOffset = pAddressValue.getOffset().getAsInt();
    int sizeofChar = machineModel.getSizeofChar();
    StringBuilder builder = new StringBuilder();
    int visitOffset = bufferOffset;
    while (true) {
      // prevent infinite loop on processing memory object of uncertain size
      if (unknownSize && newState.getHasValueEdgesFor(SGHasValueEdgeFilter
          .objectFilter(bufferObject).filterByRange(visitOffset, null)).isEmpty()) {
        newState = newState.setInvalidRead(stringExp);
        return Pair.of("", newState);
      }
      SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance().readValue(newState,
          otherStates, cfaEdge, bufferObject, KnownExplicitValue.valueOf(visitOffset),
          CNumericTypes.CHAR, stringExp);
      ShapeState chosenState = null;
      for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
        ShapeState state = valueAndState.getShapeState();
        if (state.getInvalidReadStatus()) {
          chosenState = state;
          break;
        }
      }
      if (chosenState != null) {
        return Pair.of("", chosenState);
      }
      ShapeSymbolicValue value;
      if (valueAndStates.size() > 1) {
        value = UnknownValue.getInstance();
      } else {
        SymbolicValueAndState valueAndState = Iterables.getOnlyElement(valueAndStates
            .asSymbolicValueAndStateList());
        value = valueAndState.getObject();
        chosenState = valueAndState.getShapeState();
      }
      if (value.isUnknown()) {
        builder.append(UNKNOWN_CHAR);
      } else {
        assert (chosenState != null);
        if (value.equals(KnownSymbolicValue.ZERO)) {
          return Pair.of(builder.toString(), chosenState);
        }
        // otherwise, we try to derive the explicit value
        ShapeExplicitValue expValue = chosenState.getExplicit((KnownSymbolicValue) value);
        if (expValue.isUnknown()) {
          builder.append(UNKNOWN_CHAR);
        } else {
          builder.append((char) expValue.getAsInt());
        }
      }
      visitOffset += sizeofChar;
    }
  }

  /**
   * Write string content into the specified buffer.
   *
   * @param newState     a shape state
   * @param cfaEdge      a CFA edge
   * @param addressValue the address value of specified buffer
   * @param content      string content to be written
   * @return the resultant state after string writing
   */
  private ShapeState writeStringIntoState(
      ShapeState newState, List<AbstractState>
      otherStates, CFAEdge cfaEdge, ShapeAddressValue addressValue, String content)
      throws CPATransferException {
    assert (!addressValue.isUnknown());
    SGObject bufferObject = addressValue.getObject();
    int bufferOffset = addressValue.getOffset().getAsInt();
    int sizeofChar = machineModel.getSizeofChar();
    ShapeState nextState = new ShapeState(newState);
    for (int i = 0; i < content.length(); i++) {
      char ch = content.charAt(i);
      if (ch == UNKNOWN_CHAR) {
        // we do not write anything if unknown character is encountered
        bufferOffset += sizeofChar;
        continue;
      }
      KnownExplicitValue chValue = KnownExplicitValue.valueOf(ch);
      SymbolicValueAndState symValueAndState = CoreShapeAdapter.getInstance().createSymbolic
          (nextState, chValue);
      ShapeSymbolicValue symValue = symValueAndState.getObject();
      nextState = symValueAndState.getShapeState();
      nextState = CoreShapeAdapter.getInstance().writeValue(nextState, otherStates, cfaEdge,
          bufferObject, KnownExplicitValue.valueOf(bufferOffset), CNumericTypes.CHAR, symValue);
      if (nextState.getInvalidWriteStatus()) {
        // it is unnecessary to continue writing values
        return nextState;
      }
      bufferOffset += sizeofChar;
    }
    // write the terminating null character
    return CoreShapeAdapter.getInstance().writeValue(nextState, otherStates, cfaEdge, bufferObject,
        KnownExplicitValue.valueOf(bufferOffset), CNumericTypes.CHAR, KnownSymbolicValue.ZERO);

  }

  /**
   * Since it is difficult to do bit-precise approximation in many practical cases, we just
   * return -1 for "less-than", 1 for "greater-than" and 0 for "equal". This does not match the
   * definition of memcmp() though, but is useful in practice. Generally, we only use the sign of
   * return value to check their comparison result.
   */
  private ValueAndState evaluateMemcmp(
      ShapeState newState, ShapeAddressValue memOne,
      ShapeAddressValue memTwo, ShapeExplicitValue size,
      CExpression oneExpression, CExpression twoExpression)
      throws CPATransferException {
    if (memOne.isUnknown()) {
      // ROB-1: a possible memory read issue
      return ValueAndState.of(newState);
    }
    if (memTwo.isUnknown()) {
      // ROB-1: a possible memory read issue
      return ValueAndState.of(newState);
    }
    if (size.isUnknown()) {
      // an over-approximation of buffer overrun error flag status
      return ValueAndState.of(newState.setInvalidRead(oneExpression).setInvalidRead(twoExpression));
    }
    SGObject objectOne = memOne.getObject();
    SGObject objectTwo = memTwo.getObject();
    int length = size.getAsInt();
    int startOne = memOne.getOffset().getAsInt();
    int startTwo = memTwo.getOffset().getAsInt();
    // check if invalid read would occur
    ShapeExplicitValue sizeOne = objectOne.getSize();
    ShapeExplicitValue sizeTwo = objectTwo.getSize();
    if (sizeOne.isUnknown() || startOne < 0 || startOne + length > sizeOne.getAsInt()) {
      newState = newState.setInvalidRead(oneExpression);
    }
    if (sizeTwo.isUnknown() || startTwo < 0 || startTwo + length > sizeTwo.getAsInt()) {
      newState = newState.setInvalidRead(twoExpression);
    }
    Map<Integer, SGHasValueEdge> edgeMap1 = newState.getHasValueEdgesInRange(objectOne, startOne,
        startOne + length);
    Map<Integer, SGHasValueEdge> edgeMap2 = newState.getHasValueEdgesInRange(objectTwo, startTwo,
        startTwo + length);
    Iterator<Entry<Integer, SGHasValueEdge>> mapIt1 = edgeMap1.entrySet().iterator();
    Iterator<Entry<Integer, SGHasValueEdge>> mapIt2 = edgeMap2.entrySet().iterator();
    while (mapIt1.hasNext() && mapIt2.hasNext()) {
      Entry<Integer, SGHasValueEdge> entry1 = mapIt1.next();
      Entry<Integer, SGHasValueEdge> entry2 = mapIt2.next();
      // regularize the start position for comparison purpose
      int start1 = entry1.getKey() - startOne;
      int start2 = entry2.getKey() - startTwo;
      int length1 = entry1.getValue().getSizeInBytes(machineModel);
      int length2 = entry2.getValue().getSizeInBytes(machineModel);
      if (start1 == start2 && length1 == length2) {
        long symValue1 = entry1.getValue().getValue();
        long symValue2 = entry2.getValue().getValue();
        if (symValue1 == symValue2) {
          // two symbolic values are equal (though they do not have explicit values)
          continue;
        }
        ShapeExplicitValue expValue1 = newState.getExplicit(KnownSymbolicValue.valueOf(symValue1));
        ShapeExplicitValue expValue2 = newState.getExplicit(KnownSymbolicValue.valueOf(symValue2));
        if (!expValue1.isUnknown() && !expValue2.isUnknown()) {
          int result =
              compareNumber((KnownExplicitValue) expValue1, (KnownExplicitValue) expValue2);
          if (result == 0) {
            continue;
          } else {
            return ValueAndState.of(newState, new NumericValue(result));
          }
        }
      } else {
        // if two memory blocks does not "overlap", the comparison result is obvious
        if (start1 < start2 && start1 + length1 < start2) {
          return ValueAndState.of(newState, new NumericValue(1));
        } else if (start1 > start2 && start2 + length2 < start1) {
          return ValueAndState.of(newState, new NumericValue(-1));
        } else {
          // we cannot determine the comparison result statically in the overlapping case
          return ValueAndState.of(newState);
        }
      }
    }
    // if two regions have different number of values
    if (mapIt1.hasNext()) {
      return ValueAndState.of(newState, new NumericValue(1));
    } else if (mapIt2.hasNext()) {
      return ValueAndState.of(newState, new NumericValue(-1));
    } else {
      return ValueAndState.of(newState, new NumericValue(0));
    }
  }

  /**
   * Compare two instances of {@link KnownExplicitValue}.
   *
   * @return -1 for <, +1 for > and 0 for =.
   */
  private int compareNumber(KnownExplicitValue pValue1, KnownExplicitValue pValue2) {
    if (pValue1.getKind() == NumberKind.DOUBLE || pValue2.getKind() == NumberKind.DOUBLE) {
      double val1 = pValue1.getAsDouble();
      double val2 = pValue2.getAsDouble();
      return (val1 > val2) ? 1 : ((val1 < val2) ? -1 : 0);
    } else if (pValue1.getKind() == NumberKind.FLOAT || pValue2.getKind() == NumberKind.FLOAT) {
      float val1 = pValue1.getAsFloat();
      float val2 = pValue2.getAsFloat();
      return (val1 > val2) ? 1 : ((val1 < val2) ? -1 : 0);
    } else {
      BigInteger val1 = pValue1.getAsBigInteger();
      BigInteger val2 = pValue2.getAsBigInteger();
      return val1.compareTo(val2);
    }
  }

  private String getLoopStackIdentifier(List<AbstractState> otherStates) {
    LoopstackState loopState = AbstractStates.extractStateByType(otherStates, LoopstackState.class);
    if (loopState == null) {
      return "";
    }
    return loopState.getLoopStackIdentifier();
  }

}
