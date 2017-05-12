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
import org.sosy_lab.cpachecker.exceptions.ParserException;

import java.io.IOException;

/**
 * Abstraction of a  parser that creates CFAs from  code.
 *
 * A parser should be state-less and therefore thread-safe as well as reusable.
 *
 * It may offer timing of it's operations. If present, this is not expected to
 * be thread-safe.
 */
public interface Parser {

  /**
   * Parse the content of a file into a CFA.
   *
   * @param filename            The file to parse.
   * @param sourceOriginMapping A mapping from real input file locations to original file locations
   *                            (before pre-processing).
   * @return The CFA.
   * @throws IOException     If file cannot be read.
   * @throws ParserException If parser or CFA builder cannot handle the  code.
   */
  ParseResult parseFile(String filename, CSourceOriginMapping sourceOriginMapping)
      throws ParserException, IOException, InvalidConfigurationException, InterruptedException;

  /**
   * Parse the content of a String into a CFA.
   *
   * @param code                The code to parse.
   * @param sourceOriginMapping A mapping from real input file locations to original file locations
   *                            (before pre-processing).
   * @return The CFA.
   * @throws ParserException If parser or CFA builder cannot handle the C code.
   */
  ParseResult parseString(String filename, String code, CSourceOriginMapping sourceOriginMapping)
      throws ParserException, InvalidConfigurationException;

  /**
   * Return a timer that measured the time needed for parsing.
   * Optional method: may return null.
   */
  Timer getParseTime();

  /**
   * Return a timer that measured the time need for CFA construction.
   * Optional method: may return null.
   */
  Timer getCFAConstructionTime();

}
