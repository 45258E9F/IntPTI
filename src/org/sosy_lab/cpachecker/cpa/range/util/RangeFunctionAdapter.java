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
package org.sosy_lab.cpachecker.cpa.range.util;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.checker.ExpressionCell;
import org.sosy_lab.cpachecker.core.interfaces.function.ADCombinator;
import org.sosy_lab.cpachecker.core.interfaces.function.FunctionAdapter;
import org.sosy_lab.cpachecker.core.interfaces.function.MapParser;
import org.sosy_lab.cpachecker.cpa.range.CompInteger;
import org.sosy_lab.cpachecker.cpa.range.ExpressionRangeVisitor;
import org.sosy_lab.cpachecker.cpa.range.LeftHandAccessPathVisitor;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.cpa.range.checker.RangeRefineVisitor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

@SuppressWarnings("unused")
public class RangeFunctionAdapter implements FunctionAdapter<RangeState, Range> {

  private static RangeFunctionAdapter INSTANCE = null;

  private MachineModel model;
  private HashMap<String, String> handlerMap;

  /**
   * The regular expression for C format string.
   * a format string has the form of %[flags][width][.precision][length]specifier
   * see http://www.cplusplus.com/reference/cstdio/printf/ for more details
   */
  private static final Pattern FORMAT_STRING = Pattern.compile("%((?:\\+|-| |#|0)?)((?:\\d+|\\*)?)("
      + "(?:\\.(?:\\d+|\\*))?)((?:hh|h|l|ll|j|z|t|L)?)(d|i|u|o|x|X|f|F|e|E|g|G|a|A|c|s|p|n)");

  /**
   * If current function adapter is not specified in configuration, we just bypass everything!
   */
  private final boolean activeness;

  private static final String INVERSE = "Inverse";

  private RangeFunctionAdapter() {
    Optional<CFAInfo> cfaInfo = GlobalInfo.getInstance().getCFAInfo();
    if (cfaInfo.isPresent()) {
      model = cfaInfo.get().getCFA().getMachineModel();
    } else {
      throw new AssertionError("Valid CFA is required for function analysis");
    }
    // create mapping from function name to corresponding handling method
    // NOTE: a hash map can accelerate searching of handler function
    handlerMap = Maps.newHashMap();
    Path mapFile = GlobalInfo.getInstance().queryMapFilePath(getClass());
    if (mapFile != null) {
      MapParser.loadFromFile(mapFile, handlerMap);
    }
    activeness = GlobalInfo.getInstance().queryActiveness(getClass());
  }

  public static RangeFunctionAdapter instance() {
    if (INSTANCE == null) {
      INSTANCE = new RangeFunctionAdapter();
    }
    return INSTANCE;
  }

  private static boolean forSummary = false;

  /**
   * Some operations are different under summary/non-summary modes.
   */
  public static RangeFunctionAdapter instance(boolean pForSummary) {
    if (INSTANCE == null) {
      INSTANCE = new RangeFunctionAdapter();
    }
    forSummary = pForSummary;
    return INSTANCE;
  }

  /**
   * If other components of state are unavailable, we just pass NULL into it.
   */

  @Override
  public ExpressionCell<RangeState, Range> evaluateFunctionCallExpression(
      CFunctionCallExpression pFunctionCallExpression,
      List<Range> arguments,
      RangeState currentState,
      @Nullable List<AbstractState> currentOtherStates) {
    // a function call expression is carefully handled only when:
    // the function name is id expression and matches one supported function
    Range typeRange = Ranges.getTypeRange(pFunctionCallExpression, model);
    ExpressionCell<RangeState, Range> defaultCell = new ExpressionCell<>(currentState,
        currentOtherStates, arguments, typeRange);
    if (!activeness) {
      return defaultCell;
    }
    if (pFunctionCallExpression.getParameterExpressions().size() == arguments.size()) {
      CExpression name = pFunctionCallExpression.getFunctionNameExpression();
      if (name instanceof CIdExpression) {
        CSimpleDeclaration funcDecl = ((CIdExpression) name).getDeclaration();
        String funcName;
        if (funcDecl == null) {
          // if we reach here, that means necessary declaration is missing
          funcName = ((CIdExpression) name).getName();
        } else {
          funcName = funcDecl.getName();
        }
        List<CExpression> parameters = pFunctionCallExpression.getParameterExpressions();
        ExpressionCell<RangeState, Range> cell = handleFunction(funcName, parameters, arguments,
            currentState, currentOtherStates);
        if (cell != null) {
          return cell;
        }
      }
    }
    // otherwise, we just return its type range as result
    return defaultCell;
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
        // that is to say, we can add as many custom functions as possible into the map file in
        // order to support various built-in functions
        return true;
      }
      handlerName = funcName;
      boolean isReg = true;
      try {
        getClass().getDeclaredMethod(handlerName, List.class, List.class, RangeState.class, List
            .class);
      } catch (NoSuchMethodException ex) {
        isReg = false;
      }
      return isReg;
    }
    return false;
  }

  @Override
  public RangeState refineFunctionCallExpression(
      CFunctionCallExpression pCFunctionCallExpression,
      Range restriction,
      RangeState currentState,
      @Nullable List<AbstractState> currentOtherStates) {
    if (!activeness) {
      return currentState;
    }
    CExpression name = pCFunctionCallExpression.getFunctionNameExpression();
    if (name instanceof CIdExpression) {
      CSimpleDeclaration declaration = ((CIdExpression) name).getDeclaration();
      if (declaration != null) {
        String funcName = declaration.getName();
        return handleFunctionInverse(funcName, pCFunctionCallExpression.getParameterExpressions(),
            restriction, currentState, currentOtherStates);
      }
    }
    // otherwise, we perform no refinement on state
    return currentState;
  }

  @SuppressWarnings("unchecked")
  private
  @Nullable
  ExpressionCell<RangeState, Range> handleFunction
      (
          String name, List<CExpression> parameters, List<Range> arguments, RangeState currentState,
          List<AbstractState> currentOtherStates) {

    String handlerName = handlerMap.get(name);
    if (handlerName == null) {
      // use the original name as function name
      handlerName = name;
    }
    // use reflection API to automatically find handler
    try {
      Method targetMethod = getClass().getDeclaredMethod(handlerName, List.class, List.class,
          RangeState.class, List.class);
      return (ExpressionCell<RangeState, Range>) targetMethod.invoke(this, parameters, arguments,
          currentState, currentOtherStates);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
      // no matching handler
      return null;
    }
  }

  /**
   * Refine values of domains by restricting the value of range
   *
   * @param name        Function name
   * @param arguments   Argument expressions of function call
   * @param restriction Range restriction of function range
   * @param state       Current range state
   * @param otherStates Other components of abstract state
   * @return The refined range state
   */
  private RangeState handleFunctionInverse(
      String name, List<CExpression> arguments, Range
      restriction, RangeState state, List<AbstractState> otherStates) {
    // theoretically, only a small portion of functions support refinement based on function inverse
    String handlerName = handlerMap.get(name);
    if (handlerName == null) {
      handlerName = name;
    }
    handlerName = handlerName.concat(INVERSE);
    try {
      Method targetMethod = getClass().getDeclaredMethod(handlerName, List.class, Range.class,
          RangeState.class, List.class);
      return (RangeState) targetMethod.invoke(this, arguments, restriction, state, otherStates);
    } catch (Exception ex) {
      // for other exceptions, such as unrecognized C code exception
      return state;
    }
  }

  /**
   * The followings are detailed implementations of library functions
   */

  private ExpressionCell<RangeState, Range> asin(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).asin();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> acos(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).acos();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> atan(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).atan();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> atan2(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).atan2(arguments.get(1));
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> sin(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).sin();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> cos(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).sin();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> tan(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).tan();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> exp(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).exp();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> exp2(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).exp2();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> expm1(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).expm1();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> log(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).log();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> log10(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).log10();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> log1p(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).log1p();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> log2(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).log2();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> cbrt(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).cbrt();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> sqrt(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).sqrt();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> abs(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).abs();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> ceil(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).ceil();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> floor(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).floor();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> trunc(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).trunc();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> round(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).round();
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> hypot(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).hypot(arguments.get(1));
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> pow(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).pow(arguments.get(1));
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> fdim(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).fdim(arguments.get(1));
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> fmax(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).max(arguments.get(1));
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> fmin(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).min(arguments.get(1));
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> fma(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).fma(arguments.get(1), arguments.get(2));
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> fmod(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    Range resultRange = arguments.get(0).modulo(arguments.get(1));
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> scanf(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    // we check the arguments (from the second one) and relax pointers that point to integer
    // variables
    RangeState newState = currentState;
    CExpression formatString = parameters.get(0);
    Preconditions.checkArgument(formatString instanceof CStringLiteralExpression);
    String format = ((CStringLiteralExpression) formatString).getContentString();
    List<CType> targetTypes = extractTypesFromFormatString(format);
    int initialSize = targetTypes.size();
    for (int i = initialSize; i < parameters.size() - 1; i++) {
      targetTypes.add(new CProblemType("unknown"));
    }
    for (int i = 1; i < parameters.size(); i++) {
      CExpression parameter = parameters.get(i);
      newState = relaxExpression(parameter, targetTypes.get(i - 1), newState,
          currentOtherStates);
    }
    int numberOfMatching = parameters.size() - 1;
    Range resultRange = new Range(0, numberOfMatching);
    return new ExpressionCell<>(newState, currentOtherStates, arguments, resultRange);
  }

  private ExpressionCell<RangeState, Range> fscanf(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    // int fscanf( FILE * stream, const char * format, ... );
    RangeState newState = currentState;
    // to relax expressions, we should parse the format string from the second parameter
    CExpression formatString = parameters.get(1);
    Preconditions.checkArgument(formatString instanceof CStringLiteralExpression);
    String format = ((CStringLiteralExpression) formatString).getContentString();
    List<CType> targetTypes = extractTypesFromFormatString(format);
    int initialSize = targetTypes.size();
    for (int i = initialSize; i < parameters.size() - 2; i++) {
      targetTypes.add(new CProblemType("unknown"));
    }
    for (int i = 2; i < parameters.size(); i++) {
      CExpression parameter = parameters.get(i);
      newState = relaxExpression(parameter, targetTypes.get(i - 2), newState,
          currentOtherStates);
    }
    Range resultRange = new Range(0, parameters.size() - 2);
    return new ExpressionCell<>(newState, currentOtherStates, arguments, resultRange);
  }

  private List<CType> extractTypesFromFormatString(String inputString) {
    List<CType> types = Lists.newArrayList();
    Matcher matcher = FORMAT_STRING.matcher(inputString);
    while (matcher.find()) {
      // for now, we are not interested in other groups
      String length = matcher.group(4);
      String specifier = matcher.group(5);
      CType type = restoreTypeFromSpecifier(length, specifier);
      types.add(type);
    }
    return types;
  }

  /**
   * Restore data type from specifier of format string. For example, %d is restored to int
   * Note: for now some corner cases are not considered
   *
   * @param length    the length part, could be empty
   * @param specifier the specifier part, must be non-empty
   * @return the restored C type
   */
  private CType restoreTypeFromSpecifier(String length, String specifier) {
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
      }
    } else if (length.equals("hh")) {
      if (specifier.equals("d") || specifier.equals("i")) {
        return CNumericTypes.SIGNED_CHAR;
      } else if (specifier.equals("u") || specifier.equals("o") || specifier
          .equalsIgnoreCase("x")) {
        return CNumericTypes.UNSIGNED_CHAR;
      }
    } else if (length.equals("h")) {
      if (specifier.equals("d") || specifier.equals("i")) {
        return CNumericTypes.SHORT_INT;
      } else if (specifier.equals("u") || specifier.equals("o") || specifier
          .equalsIgnoreCase("x")) {
        return CNumericTypes.UNSIGNED_SHORT_INT;
      }
    } else if (length.equals("l")) {
      if (specifier.equals("d") || specifier.equals("i")) {
        return CNumericTypes.LONG_INT;
      } else if (specifier.equals("u") || specifier.equals("o") || specifier
          .equalsIgnoreCase("x")) {
        return CNumericTypes.UNSIGNED_LONG_INT;
      }
    } else if (length.equals("ll")) {
      if (specifier.equals("d") || specifier.equals("i")) {
        return CNumericTypes.LONG_LONG_INT;
      } else if (specifier.equals("u") || specifier.equals("o") || specifier
          .equalsIgnoreCase("x")) {
        return CNumericTypes.UNSIGNED_LONG_LONG_INT;
      }
    } else if (length.equals("L")) {
      if (specifier.equalsIgnoreCase("f") || specifier.equalsIgnoreCase("e") || specifier
          .equalsIgnoreCase("g") || specifier.equalsIgnoreCase("a")) {
        return CNumericTypes.LONG_DOUBLE;
      }
    }
    return new CProblemType("unknown");
  }

  private ExpressionCell<RangeState, Range> strcmp(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    // int strcmp( const char * str1, const char * str2 );
    ADCombinator<Range> string1 = Ranges.createADCombinatorFromExpression(parameters.get(0),
        currentState, currentOtherStates, model);
    ADCombinator<Range> string2 = Ranges.createADCombinatorFromExpression(parameters.get(1),
        currentState, currentOtherStates, model);
    long index = 0;
    boolean isTerminate = false;
    Range resultRange = Range.EMPTY;
    while (true) {
      Range r1, r2;
      r1 = string1.queryValueByIndex(index);
      r2 = string2.queryValueByIndex(index);
      if (r1 == null) {
        // when the queried range is null, then corresponding array element is undetermined (and
        // this is possibly a buffer overflow issue!)
        r1 = Ranges.getTypeRange(CNumericTypes.CHAR, model);
        isTerminate = true;
      }
      if (r2 == null) {
        r2 = Ranges.getTypeRange(CNumericTypes.CHAR, model);
        isTerminate = true;
      }
      if (isTerminate) {
        resultRange = mergeCompareResult(resultRange, r1.minus(r2));
        return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
      }
      Range currentDelta = r1.minus(r2);
      resultRange = mergeCompareResult(resultRange, currentDelta);
      if (r1.equals(Range.ZERO) || r2.equals(Range.ZERO)) {
        // '\0' denotes the end of string
        resultRange = mergeCompareResult(resultRange, currentDelta);
        return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
      }
      if (currentDelta.isGreaterThan(Range.ZERO) || Range.ZERO.isGreaterThan(currentDelta)) {
        return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
      }
      // otherwise, we cannot say if the comparison should continue
      index++;
    }
  }

  private ExpressionCell<RangeState, Range> rand(
      List<CExpression> parameters, List<Range>
      arguments, RangeState currentState, List<AbstractState> currentOtherStates) {
    // int rand(), the return value ranges in [0, RAND_MAX]
    BigInteger max = model.getMaximalIntegerValue(CNumericTypes.SIGNED_INT);
    Range resultRange = new Range(CompInteger.ZERO, new CompInteger(max));
    return new ExpressionCell<>(currentState, currentOtherStates, arguments, resultRange);
  }

  private Range mergeCompareResult(Range origRange, Range newRange) {
    if (origRange.equals(Range.ZERO)) {
      return newRange;
    }
    return origRange.union(newRange);
  }

  private RangeState relaxExpression(
      CExpression expression, CType targetType, RangeState state,
      List<AbstractState> otherStates) {
    // for now we only consider expression with addressing (e.g. &p)
    if (expression instanceof CUnaryExpression) {
      if (((CUnaryExpression) expression).getOperator() == UnaryOperator.AMPER) {
        CExpression possibleNum = ((CUnaryExpression) expression).getOperand();
        Range typeRange;
        if (targetType instanceof CProblemType) {
          CType possibleNumType = possibleNum.getExpressionType();
          typeRange = Ranges.getTypeRange(possibleNumType, model);
        } else {
          typeRange = Ranges.getTypeRange(targetType, model);
          Range expRange = Ranges.getTypeRange(possibleNum, model);
          if (expRange.isUnbound() || !expRange.contains(typeRange)) {
            typeRange = expRange;
          }
        }
        if (!typeRange.equals(Range.UNBOUND)) {
          // this expression has numerical type
          if (possibleNum instanceof CLeftHandSide) {
            try {
              Optional<AccessPath> accessPath = ((CLeftHandSide) possibleNum).accept(new
                  LeftHandAccessPathVisitor(new ExpressionRangeVisitor(state, model, false)));
              if (!accessPath.isPresent()) {
                return state;
              }
              AccessPath truePath = accessPath.get();
              // relax this access path with unbounded range
              // NOTE: other abstract domain should have its own implementation
              state.addRange(truePath, typeRange, forSummary);
              return state;
            } catch (UnrecognizedCCodeException ex) {
              // nothing to be changed
              return state;
            }
          }
        }
      }
    }
    // for other cases, there is no reason to change the range state
    return state;
  }

  /**
   * The following functions are implementations of inverse refinement for numerical functions
   */

  private RangeState asinInverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range restrict = restriction.intersect(Range.TRIGONOMETRIC_VALUES);
    // otherwise, the refinement is non-trivial
    Range domainRestrict = restrict.sin();
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domainRestrict, model, forSummary));
  }

  private RangeState acosInverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range restrict = restriction.intersect(Range.TRIGONOMETRIC_VALUES);
    Range domain = restrict.cos();
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  private RangeState atanInverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range restrict = restriction.intersect(Range.TRIGONOMETRIC_SYM_DOMAIN);
    Range domain = restrict.tan();
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  /* We cannot refine domains of atan2() since division cannot be refined under the range domain */

  private RangeState expInverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range restrict = restriction.intersect(Range.NONNEGATIVE);
    Range domain = restrict.log();
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  private RangeState exp2Inverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range restrict = restriction.intersect(Range.NONNEGATIVE);
    Range domain = restrict.log2();
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  private RangeState expm1Inverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range restrict = restriction.plus((long) 1);
    restrict = restrict.intersect(Range.NONNEGATIVE);
    Range domain = restrict.log();
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  private RangeState logInverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range domain = restriction.exp();
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  private RangeState log2Inverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range domain = restriction.exp2();
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  private RangeState log1pInverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range domain = restriction.exp();
    domain = domain.minus((long) 1);
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  private RangeState log10Inverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range domain = Range.singletonRange(10).pow(restriction);
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  private RangeState cbrtInverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range domain = restriction.times(restriction).times(restriction);
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  private RangeState sqrtInverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range restrict = restriction.intersect(Range.NONNEGATIVE);
    Range domain = restrict.times(restrict);
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  private RangeState absInverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range restrict = restriction.intersect(Range.NONNEGATIVE);
    Range domain;
    if (restrict.contains(Range.ZERO)) {
      domain = new Range(restrict.getHigh().negate(), restrict.getHigh());
    } else {
      // FIXME: here is an incomplete and unsound fix, but useful in many cases
      domain = new Range(restrict.getLow(), restrict.getHigh());
    }
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  /**
   * for a preliminary prototype, we do not implement hypot and fma, though refinement is
   * completely possible for them
   */

  private RangeState ceilInverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range domain = new Range(restriction.getLow().subtract(CompInteger.ONE), restriction.getHigh());
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  private RangeState floorInverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range domain = new Range(restriction.getLow(), restriction.getHigh().add(CompInteger.ONE));
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  private RangeState roundInverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    Range domain = new Range(restriction.getLow().subtract(CompInteger.ONE), restriction.getHigh()
        .add(CompInteger.ONE));
    return arguments.get(0).accept(new RangeRefineVisitor(currentState, currentOtherStates,
        domain, model, forSummary));
  }

  private RangeState fdimInverse(
      List<CExpression> arguments, Range restriction, RangeState
      currentState, List<AbstractState> currentOtherStates)
      throws UnrecognizedCCodeException {
    CExpression x = arguments.get(0);
    CExpression y = arguments.get(1);
    CBinaryExpressionBuilder builder = new CBinaryExpressionBuilder(model, null);
    CBinaryExpression xy = builder.buildBinaryExpression(FileLocation.DUMMY, x, y, BinaryOperator
        .MINUS);
    Range restrict = restriction.intersect(Range.NONNEGATIVE);
    Range domain;
    if (restrict.getLow().equals(CompInteger.ZERO)) {
      domain = new Range(CompInteger.NEGATIVE_INF, restrict.getHigh());
    } else {
      domain = restrict;
    }
    return xy.accept(new RangeRefineVisitor(currentState, currentOtherStates, domain, model,
        forSummary));
  }

}
