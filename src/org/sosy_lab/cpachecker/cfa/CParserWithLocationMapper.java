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

import com.google.common.collect.Lists;

import org.eclipse.cdt.core.parser.OffsetLimitReachedException;
import org.eclipse.cdt.internal.core.parser.scanner.ILexerLog;
import org.eclipse.cdt.internal.core.parser.scanner.Lexer;
import org.eclipse.cdt.internal.core.parser.scanner.Lexer.LexerOptions;
import org.eclipse.cdt.internal.core.parser.scanner.Token;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Encapsulates a {@link CParser} instance and tokenizes all files first.
 */

@Options
public class CParserWithLocationMapper implements CParser {

  private final CParser realParser;

  private final LogManager logger;

  private final boolean readLineDirectives;

  @Option(secure = true, name = "locmapper.dumpTokenizedProgramToFile",
      description = "Write the tokenized version of the input program to this file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dumpTokenizedProgramToFile = null;

  @Option(secure = true, name = "parser.transformTokensToLines",
      description =
          "Preprocess the given C files before parsing: Put every single token onto a new line. "
              + "Then the line number corresponds to the token number.")
  private boolean tokenizeCode = false;

  public CParserWithLocationMapper(
      Configuration pConfig,
      LogManager pLogger,
      CParser pRealParser,
      boolean pReadLineDirectives) throws InvalidConfigurationException {

    pConfig.inject(this);

    this.logger = pLogger;
    this.realParser = pRealParser;
    readLineDirectives = pReadLineDirectives;
  }

//  public static void main(String[] args) throws CParserException {
//    String sourceFileName = args[0];
//    CParserWithLocationExtractor t = new CParserWithLocationExtractor(null);
//    StringBuilder tokenized = t.tokenizeSourcefile(sourceFileName);
//    System.out.append(tokenized.toString());
//  }

  @Override
  public ParseResult parseFile(String pFilename, CSourceOriginMapping sourceOriginMapping)
      throws ParserException, IOException, InvalidConfigurationException, InterruptedException {
    String tokenizedCode = tokenizeSourcefile(pFilename, sourceOriginMapping);
    return realParser.parseString(pFilename, tokenizedCode, sourceOriginMapping);
  }

  private String tokenizeSourcefile(
      String pFilename,
      CSourceOriginMapping sourceOriginMapping) throws CParserException, IOException {
    String code = Paths.get(pFilename).asCharSource(Charset.defaultCharset()).read();
    return processCode(pFilename, code, sourceOriginMapping);
  }

  private String processCode(
      final String fileName, String pCode,
      CSourceOriginMapping sourceOriginMapping) throws CParserException {
    StringBuilder tokenizedCode = new StringBuilder();

    LexerOptions options = new LexerOptions();
    ILexerLog log = ILexerLog.NULL;
    Object source = null;
    Lexer lx = new Lexer(pCode.toCharArray(), options, log, source);

    try {
      int absoluteLineNumber = 1;
      int relativeLineNumber = absoluteLineNumber;

      String rangeLinesOriginFilename = fileName;
      int includeStartedWithAbsoluteLine = 0;

      Token token;
      while ((token = lx.nextToken()).getType() != Token.tEND_OF_INPUT) {
        if (token.getType() == Lexer.tNEWLINE) {
          absoluteLineNumber += 1;
          relativeLineNumber += 1;
        }

        if (token.getType() == Token.tPOUND) { // match #
          // Read the complete line containing the directive...
          ArrayList<Token> directiveTokens = Lists.newArrayList();
          token = lx.nextToken();
          while (token.getType() != Lexer.tNEWLINE && token.getType() != Token.tEND_OF_INPUT) {
            directiveTokens.add(token);
            token = lx.nextToken();
          }
          absoluteLineNumber += 1;
          relativeLineNumber += 1;

          // Evaluate the preprocessor directive...
          if (readLineDirectives && directiveTokens.size() > 0) {
            String firstTokenImage = directiveTokens.get(0).getImage().trim();

            final int lineNumberTokenIndex;

            if (directiveTokens.size() > 1
                && firstTokenImage.equals("line")
                && directiveTokens.get(1).getImage().matches("[0-9]+")) {
              lineNumberTokenIndex = 1;
            } else if (firstTokenImage.matches("[0-9]+")) {
              lineNumberTokenIndex = 0;
            } else {
              lineNumberTokenIndex = -1;
            }
            if (lineNumberTokenIndex >= 0) {

              sourceOriginMapping.mapInputLineRangeToDelta(fileName, rangeLinesOriginFilename,
                  includeStartedWithAbsoluteLine, absoluteLineNumber,
                  relativeLineNumber - absoluteLineNumber);

              final String lineNumberToken =
                  directiveTokens.get(lineNumberTokenIndex).getImage().trim();
              includeStartedWithAbsoluteLine = absoluteLineNumber;
              relativeLineNumber = Integer.parseInt(lineNumberToken);
              if (directiveTokens.size() > lineNumberTokenIndex + 1) {
                String file = directiveTokens.get(lineNumberTokenIndex + 1).getImage().trim();
                if (file.charAt(0) == '"' && file.charAt(file.length() - 1) == '"') {
                  file = file.substring(1, file.length() - 1);
                }
                rangeLinesOriginFilename = file;
              }
            }
          }
        } else if (!token.getImage().trim().isEmpty()) {
          if (tokenizeCode) {
            tokenizedCode.append(token.toString());
            tokenizedCode.append(System.lineSeparator());
          }
        }
      }

      if (readLineDirectives) {
        sourceOriginMapping.mapInputLineRangeToDelta(fileName, rangeLinesOriginFilename,
            includeStartedWithAbsoluteLine + 1, absoluteLineNumber,
            relativeLineNumber - absoluteLineNumber);
      }
    } catch (OffsetLimitReachedException e) {
      throw new CParserException("Tokenizing failed", e);
    }

    String code = tokenizeCode ? tokenizedCode.toString() : pCode;
    if (tokenizeCode && dumpTokenizedProgramToFile != null) {
      try (Writer out = Files
          .openOutputFile(dumpTokenizedProgramToFile, StandardCharsets.US_ASCII)) {
        out.append(code);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write tokenized program to file");
      }
    }
    return code;
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
    String tokenizedCode = processCode(pFilename, pCode, pSourceOriginMapping);

    return realParser.parseString(pFilename, tokenizedCode, pSourceOriginMapping, pScope);
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

    List<FileContentToParse> programFragments = new ArrayList<>(pFilenames.size());
    for (FileToParse f : pFilenames) {
      String programCode = tokenizeSourcefile(f.getFileName(), sourceOriginMapping);
      if (programCode.isEmpty()) {
        throw new CParserException("Tokenizer returned empty program");
      }
      programFragments.add(new FileContentToParse(f.getFileName(), programCode));
    }
    return realParser.parseString(programFragments, sourceOriginMapping);
  }

  @Override
  public ParseResult parseString(
      List<FileContentToParse> pCode,
      CSourceOriginMapping sourceOriginMapping) throws CParserException,
                                                       InvalidConfigurationException {

    List<FileContentToParse> tokenizedFragments = new ArrayList<>(pCode.size());
    for (FileContentToParse f : pCode) {
      String programCode = processCode(f.getFileName(), f.getFileContent(), sourceOriginMapping);
      if (programCode.isEmpty()) {
        throw new CParserException("Tokenizer returned empty program");
      }
      tokenizedFragments.add(new FileContentToParse(f.getFileName(), programCode));
    }

    return realParser.parseString(tokenizedFragments, sourceOriginMapping);
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