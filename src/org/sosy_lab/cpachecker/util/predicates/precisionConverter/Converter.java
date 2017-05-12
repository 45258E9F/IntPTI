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
package org.sosy_lab.cpachecker.util.predicates.precisionConverter;

import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.SymbolEncoding.Type;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.SymbolEncoding.UnknownFormulaSymbolException;
import org.sosy_lab.solver.api.FormulaType;

import java.util.List;
import java.util.Set;

/**
 * This is a dummy converter
 * that returns the identity of the given terms and types.
 */
public class Converter {

  protected final Set<String> binBooleanOps = Sets.newHashSet("and", "or");
  protected final SymbolEncoding symbolEncoding;
  protected final LogManager logger;

  public Converter(LogManager logger, CFA cfa) {
    this.symbolEncoding = new SymbolEncoding(cfa);
    this.logger = logger;
  }

  @VisibleForTesting
  public Converter() {
    this.symbolEncoding = null;
    this.logger = null;
  }

  /**
   * @throws UnknownFormulaSymbolException may be thrown in subclasses
   */
  public String convertFunctionDeclaration(String symbol, Type<String> pFt)
      throws UnknownFormulaSymbolException {
    return format("%s (%s) %s",
        symbol, Joiner.on(' ').join(pFt.getParameterTypes()), pFt.getReturnType());
  }

  /**
   * @throws UnknownFormulaSymbolException may be thrown in subclasses
   */
  public String convertFunctionDefinition(
      String symbol,
      Type<String> type, Pair<String, Type<FormulaType<?>>> initializerTerm)
      throws UnknownFormulaSymbolException {
    return format("%s (%s) %s %s",
        symbol, Joiner.on(' ').join(type.getParameterTypes()),
        type.getReturnType(), initializerTerm.getFirst());
  }

  public Pair<String, Type<FormulaType<?>>> convertNumeral(String num) {
    return wrap(num);
  }

  /**
   * @throws UnknownFormulaSymbolException may be thrown in subclasses
   */
  public Pair<String, Type<FormulaType<?>>> convertSymbol(String symbol)
      throws UnknownFormulaSymbolException {
    return wrap(symbol);
  }

  /**
   * @throws UnknownFormulaSymbolException may be thrown in subclasses
   */
  public Pair<String, Type<FormulaType<?>>> convertTerm(
      Pair<String, Type<FormulaType<?>>> op, List<Pair<String, Type<FormulaType<?>>>> terms)
      throws UnknownFormulaSymbolException {
    if (terms.isEmpty()) {
      return wrap("(" + op.getFirst() + ")"); // should not happen?
    } else {
      return wrap("(" + op.getFirst() + " " +
          Joiner.on(' ').join(Lists.transform(terms, Pair.getProjectionToFirst())) + ")");
    }
  }

  private static Pair<String, Type<FormulaType<?>>> wrap(String s) {
    // return dummy type with size 0
    return Pair.of(s, new Type<FormulaType<?>>(FormulaType.getBitvectorTypeWithSize(0)));
  }

  public enum PrecisionConverter {
    DISABLE,
    INT2BV,
    BV2INT
  }

  public static Converter getConverter(
      PrecisionConverter encodePredicates,
      CFA cfa,
      LogManager logger) {
    switch (encodePredicates) {
      case INT2BV: {
        return new BVConverter(cfa, logger);
      }
      case BV2INT: {
        return new IntConverter(cfa, logger);
      }
      case DISABLE: {
        return null;
      }
      default:
        throw new AssertionError("invalid value for option");
    }
  }
}
