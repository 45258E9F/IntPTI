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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.EclipseParsers;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.CParserException;

import java.io.IOException;
import java.util.List;

/**
 * Abstraction of a C parser that creates CFAs from C code.
 *
 * A C parser should be state-less and therefore thread-safe as well as reusable.
 *
 * It may offer timing of it's operations. If present, this is not expected to
 * be thread-safe.
 */
public interface CParser extends Parser {

  public static class FileToParse {
    private final String fileName;

    public FileToParse(String pFileName) {
      this.fileName = pFileName;
    }

    public String getFileName() {
      return fileName;
    }
  }

  public static class FileContentToParse extends FileToParse {
    private final String fileContent;

    public FileContentToParse(String pFileName, String pFileContent) {
      super(pFileName);
      this.fileContent = pFileContent;
    }

    public String getFileContent() {
      return fileContent;
    }
  }

  /**
   * Parse the content of files into a single CFA.
   *
   * @param filenames           The List of files to parse. The first part of the pair should be the
   *                            filename, the second part should be the prefix which will be
   *                            appended to static variables
   * @param sourceOriginMapping A mapping from real input file locations to original file locations
   *                            (before pre-processing).
   * @return The CFA.
   * @throws IOException      If file cannot be read.
   * @throws CParserException If parser or CFA builder cannot handle the C code.
   */
  ParseResult parseFile(List<FileToParse> filenames, CSourceOriginMapping sourceOriginMapping)
      throws CParserException, IOException, InvalidConfigurationException, InterruptedException;

  /**
   * Parse the content of Strings into a single CFA.
   *
   * @param code                The List of code fragments to parse. The first part of the pair
   *                            should be the code, the second part should be the prefix which will
   *                            be appended to static variables
   * @param sourceOriginMapping A mapping from real input file locations to original file locations
   *                            (before pre-processing).
   * @return The CFA.
   * @throws CParserException If parser or CFA builder cannot handle the C code.
   */
  ParseResult parseString(List<FileContentToParse> code, CSourceOriginMapping sourceOriginMapping)
      throws CParserException, InvalidConfigurationException;

  /**
   * Parse the content of a String into a CFA.
   *
   * @param pFileName            the file name.
   * @param pCode                the code to parse.
   * @param pSourceOriginMapping a mapping from real input file locations to original file locations
   *                             (before pre-processing).
   * @param pScope               an optional external scope to be used.
   * @return the parse result.
   * @throws CParserException              if the parser cannot handle the C code.
   * @throws InvalidConfigurationException if the configuration is invalid.
   */
  ParseResult parseString(
      String pFileName, String pCode, CSourceOriginMapping pSourceOriginMapping, Scope pScope)
      throws CParserException, InvalidConfigurationException;

  /**
   * Method for parsing a string that contains exactly one function with exactly
   * one statement. Only the AST for the statement is returned, the function
   * declaration is stripped.
   *
   * Example input:
   * void foo() { bar(); }
   * Example output:
   * AST for "bar();"
   *
   * This method guarantees that the AST does not contain CProblem nodes.
   *
   * @param code  The code snippet as described above.
   * @param scope The scope is needed to resolve the type bindings in the statement.
   * @return The AST for the statement.
   * @throws CParserException If parsing fails.
   */
  CAstNode parseSingleStatement(String code, Scope scope)
      throws CParserException, InvalidConfigurationException;

  /**
   * Method for parsing a block of statements that contains exactly one function with exactly one
   * block of statements. Only the List of ASTs for the block of statement is returned, the function
   * declaration is stripped.
   *
   * Example input: void foo() { bar();a = 2; } Example output: AST for "<bar();, a = 2;>"
   *
   * This method guarantees that the AST does not contain CProblem nodes.
   *
   * @param code  The code snippet as described above.
   * @param scope The scope is needed to resolve the type bindings in the statement.
   * @return The list of ASTs for the statement.
   * @throws CParserException If parsing fails.
   */
  List<CAstNode> parseStatements(String code, Scope scope)
      throws CParserException, InvalidConfigurationException;

  /**
   * Enum for clients of this class to choose the C dialect the parser uses.
   */
  public static enum Dialect {
    C99,
    GNUC,;
  }

  @Options(prefix = "parser")
  public final static class ParserOptions {

    @Option(secure = true, description = "C dialect for parser")
    private Dialect dialect = Dialect.GNUC;

    private ParserOptions() {
    }
  }

  /**
   * Factory that tries to create a parser based on available libraries
   * (e.g. Eclipse CDT).
   */
  public static class Factory {


    public static ParserOptions getOptions(Configuration config)
        throws InvalidConfigurationException {
      ParserOptions result = new ParserOptions();
      config.inject(result);
      return result;
    }

    public static ParserOptions getDefaultOptions() {
      return new ParserOptions();
    }

    public static CParser getParser(
        Configuration config,
        LogManager logger,
        ParserOptions options,
        MachineModel machine) {
      return EclipseParsers.getCParser(config, logger, options.dialect, machine);
    }
  }
}
