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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.sosy_lab.common.ProcessExecutor;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.CParserException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Options(prefix = "parser")
public class CPreprocessor {

  @Option(description = "The command line for calling the preprocessor. " +
      "May contain binary name and arguments, but won't be expanded by a shell. " +
      "The source file name will be appended to this string. " +
      "The preprocessor needs to print the output to stdout.")

  private String preprocessor = "cpp";

  private final LogManager logger;

  public CPreprocessor(Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
  }

  public String preprocess(String file) throws CParserException, InterruptedException {
    // create command line
    List<String> argList =
        Lists.newArrayList(
            Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().split(preprocessor));
    argList.add(file);
    String[] args = argList.toArray(new String[argList.size()]);

    logger.log(Level.FINE, "Running preprocessor", argList);
    try {
      CPreprocessorExecutor executor = new CPreprocessorExecutor(logger, args);
      executor.sendEOF();
      int exitCode = executor.join();
      logger.log(Level.FINE, "Preprocessor finished");

      if (exitCode != 0) {
        throw new CParserException("Preprocessor failed with exit code " + exitCode);
      }

      if (executor.errorOutputCount > 0) {
        logger.log(Level.WARNING,
            "Preprocessor returned successfully, but printed warnings. Please check the log above!");
      }

      if (executor.buffer == null) {
        return "";
      }
      return executor.buffer.toString();

    } catch (IOException e) {
      throw new CParserException("Preprocessor failed", e);
    }
  }


  private static class CPreprocessorExecutor extends ProcessExecutor<IOException> {

    private static final int MAX_ERROR_OUTPUT_SHOWN = 10;
    private static final Map<String, String> ENV_VARS = ImmutableMap.of("LANG", "C");

    @SuppressFBWarnings(value = "VO_VOLATILE_INCREMENT",
        justification = "Written only by one thread")
    private volatile int errorOutputCount = 0;
    private volatile StringBuffer buffer;

    public CPreprocessorExecutor(LogManager logger, String[] args) throws IOException {
      super(logger, IOException.class, ENV_VARS, args);
    }

    @Override
    @SuppressWarnings("NonAtomicVolatileUpdate") // errorOutputCount written only by one thread
    protected void handleErrorOutput(String pLine) throws IOException {
      if (errorOutputCount == MAX_ERROR_OUTPUT_SHOWN) {
        logger.log(Level.WARNING, "Skipping further preprocessor error output...");
        errorOutputCount++;

      } else if (errorOutputCount < MAX_ERROR_OUTPUT_SHOWN) {
        errorOutputCount++;
        super.handleErrorOutput(pLine);
      }
    }

    @Override
    protected void handleOutput(String pLine) throws IOException {
      if (buffer == null) {
        buffer = new StringBuffer();
      }
      buffer.append(pLine);
      buffer.append('\n');
    }

    @Override
    protected void handleExitCode(int pCode) throws IOException {
    }
  }
}
