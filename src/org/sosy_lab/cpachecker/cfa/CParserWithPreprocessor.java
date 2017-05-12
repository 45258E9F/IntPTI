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
package org.sosy_lab.cpachecker.cfa;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a {@link CParser} instance and processes all files first
 * with a {@link CPreprocessor}.
 */
class CParserWithPreprocessor implements CParser {

  private final CParser realParser;
  private final CPreprocessor preprocessor;

  public CParserWithPreprocessor(CParser pRealParser, CPreprocessor pPreprocessor) {
    realParser = pRealParser;
    preprocessor = pPreprocessor;
  }

  @Override
  public ParseResult parseFile(String pFilename, CSourceOriginMapping sourceOriginMapping)
      throws ParserException, IOException, InvalidConfigurationException, InterruptedException {
    String programCode = preprocessor.preprocess(pFilename);
    if (programCode.isEmpty()) {
      throw new CParserException("Preprocessor returned empty program");
    }
    return realParser.parseString(pFilename, programCode, sourceOriginMapping);
  }

  @Override
  public ParseResult parseString(
      String pFilename,
      String pCode,
      CSourceOriginMapping sourceOriginMapping)
      throws ParserException, InvalidConfigurationException {
    return parseString(pFilename, pCode, sourceOriginMapping, CProgramScope.empty());
  }

  @Override
  public ParseResult parseString(
      String pFilename, String pCode, CSourceOriginMapping pSourceOriginMapping, Scope pScope)
      throws CParserException, InvalidConfigurationException {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public Timer getParseTime() {
    return realParser.getParseTime();
  }

  @Override
  public Timer getCFAConstructionTime() {
    return realParser.getCFAConstructionTime();
  }

  @Override
  public ParseResult parseFile(
      List<FileToParse> pFilenames,
      CSourceOriginMapping sourceOriginMapping) throws CParserException, IOException,
                                                       InvalidConfigurationException,
                                                       InterruptedException {

    List<FileContentToParse> programs = new ArrayList<>(pFilenames.size());
    for (FileToParse p : pFilenames) {
      String programCode = preprocessor.preprocess(p.getFileName());
      if (programCode.isEmpty()) {
        throw new CParserException("Preprocessor returned empty program");
      }
      programs.add(new FileContentToParse(p.getFileName(), programCode));
    }
    return realParser.parseString(programs, sourceOriginMapping);
  }

  @Override
  public ParseResult parseString(
      List<FileContentToParse> pCode,
      CSourceOriginMapping sourceOriginMapping)
      throws CParserException, InvalidConfigurationException {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public CAstNode parseSingleStatement(String pCode, Scope pScope)
      throws CParserException, InvalidConfigurationException {
    return realParser.parseSingleStatement(pCode, pScope);
  }

  @Override
  public List<CAstNode> parseStatements(String pCode, Scope pScope)
      throws CParserException, InvalidConfigurationException {
    return realParser.parseStatements(pCode, pScope);
  }
}