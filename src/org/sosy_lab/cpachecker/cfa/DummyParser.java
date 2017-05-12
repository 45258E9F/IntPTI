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
package org.sosy_lab.cpachecker.cfa;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import java.io.IOException;
import java.util.List;

/**
 * For all languages, where parsing of single or blocks of statements is not yet implemented,
 * use this dummy scope when parsing an automaton {@link AutomatonParser}.
 */
public class DummyParser implements CParser {

  private static final DummyParser DUMMYPARSER = new DummyParser();

  private DummyParser() {
  } // Private constructor to insure one instance.

  public static DummyParser getInstance() {
    return DUMMYPARSER;
  }

  @Override
  public ParseResult parseFile(String pFilename, CSourceOriginMapping pSourceOriginMapping)
      throws ParserException,
             IOException, InvalidConfigurationException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ParseResult parseString(
      String pFilename,
      String pCode,
      CSourceOriginMapping pSourceOriginMapping)
      throws ParserException, InvalidConfigurationException {
    return parseString(pFilename, pCode, pSourceOriginMapping, CProgramScope.empty());
  }

  @Override
  public ParseResult parseString(
      String pFileName, String pCode, CSourceOriginMapping pSourceOriginMapping, Scope pScope)
      throws CParserException, InvalidConfigurationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Timer getParseTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Timer getCFAConstructionTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ParseResult parseFile(
      List<FileToParse> pFilenames,
      CSourceOriginMapping pSourceOriginMapping)
      throws CParserException, IOException, InvalidConfigurationException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ParseResult parseString(
      List<FileContentToParse> pCode,
      CSourceOriginMapping pSourceOriginMapping)
      throws CParserException, InvalidConfigurationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public CAstNode parseSingleStatement(String pCode, Scope pScope) throws CParserException,
                                                                          InvalidConfigurationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CAstNode> parseStatements(String pCode, Scope pScope) throws CParserException,
                                                                           InvalidConfigurationException {
    throw new UnsupportedOperationException();
  }
}