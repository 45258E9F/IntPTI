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
package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.Converter;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.Converter.PrecisionConverter;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.FormulaParser;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * This is a parser for files which contain initial predicates for the analysis. The file format is
 * based on SMTLIB2 and defines a map from location to sets of predicates. The keys of the map (the
 * locations) may either be CFA nodes (N1 etc.), function names of the analyzed program (which stand
 * for all locations in the respective function), or the special identifier "*" (which stands for
 * all locations in the program).
 *
 * Detailed format description:
 *
 * - Lines starting with "//" are comments. - Blank lines separates sections in the file. - The
 * first section (starting in the first line) consists of an arbitrary number of lines of the format
 * "(declare-fun ...)" or "(define-fun ...)" with definitions in SMTLIB2 format. - Every section
 * except the first one starts with a line of the format "key:", where key is either "*", "<FUNC>",
 * or "<FUNC> N<ID>", with <FUNC> being a function name of the program, and <ID> being a CFA node
 * id. This line defines where the following predicates are to be used. - The following lines of the
 * section contain SMTLIB2 statements of the form "(assert ...)". Each asserted term will be used as
 * one predicate.
 */
@Options(prefix = "cpa.predicate.abstraction.initialPredicates")
public class PredicateMapParser {

  private static final String FUNCTION_NAME_REGEX = "([_a-zA-Z][_a-zA-Z0-9]*)";
  private static final String CFA_NODE_REGEX = "N([0-9][0-9]*)";
  private static final Pattern FUNCTION_NAME_PATTERN =
      Pattern.compile("^" + FUNCTION_NAME_REGEX + "$");
  private static final Pattern CFA_NODE_PATTERN =
      Pattern.compile("^" + FUNCTION_NAME_REGEX + " " + CFA_NODE_REGEX + "$");

  @Option(secure = true, description = "Apply location-specific predicates to all locations in their function")
  private boolean applyFunctionWide = false;

  @Option(secure = true, description = "Apply location- and function-specific predicates globally (to all locations in the program)")
  private boolean applyGlobally = false;

  @Option(secure = true, description = "when reading predicates from file, convert them from Integer- to BV-theory or reverse.")
  private PrecisionConverter encodePredicates = PrecisionConverter.DISABLE;

  private final CFA cfa;

  private final LogManagerWithoutDuplicates logger;
  private final FormulaManagerView fmgr;
  private final AbstractionManager amgr;

  private final Map<Integer, CFANode> idToNodeMap = Maps.newHashMap();

  public PredicateMapParser(
      Configuration pConfig, CFA pCfa,
      LogManager pLogger,
      FormulaManagerView pFmgr, AbstractionManager pAmgr) throws InvalidConfigurationException {
    pConfig.inject(this);

    cfa = pCfa;
    logger = new LogManagerWithoutDuplicates(pLogger);
    fmgr = pFmgr;
    amgr = pAmgr;
  }

  /**
   * Parse a file in the format described above and create a PredicatePrecision
   * object with all the predicates.
   *
   * @param file The file to parse.
   * @return A PredicatePrecision containing all the predicates from the file.
   * @throws IOException                     If the file cannot be read.
   * @throws PredicateParsingFailedException If there is a syntax error in the file.
   */
  public PredicatePrecision parsePredicates(Path file)
      throws IOException, PredicateParsingFailedException {

    Files.checkReadableFile(file);

    try (BufferedReader reader = file.asCharSource(StandardCharsets.US_ASCII)
        .openBufferedStream()) {
      return parsePredicates(reader, file.getName());
    }
  }

  /**
   * @see #parsePredicates(Path) Instead of reading from a file, this method reads from a
   * BufferedReader (available primarily for testing).
   */
  PredicatePrecision parsePredicates(BufferedReader reader, String source)
      throws IOException, PredicateParsingFailedException {

    // first, read first section with initial set of function definitions
    Pair<Integer, String> defParsingResult =
        PredicatePersistenceUtils.parseCommonDefinitions(reader, source);
    int lineNo = defParsingResult.getFirst();
    String commonDefinitions = defParsingResult.getSecond();

    final Converter converter = Converter.getConverter(encodePredicates, cfa, logger);
    if (encodePredicates != PrecisionConverter.DISABLE) {
      final StringBuilder str = new StringBuilder();
      for (String line : commonDefinitions.split("\n")) {
        final String converted = convertFormula(converter, line);
        if (converted != null) {
          str.append(converted).append("\n");
        }
      }
      commonDefinitions = str.toString();
    }

    // second, read map of predicates
    Set<AbstractionPredicate> globalPredicates = Sets.newHashSet();
    SetMultimap<String, AbstractionPredicate> functionPredicates = HashMultimap.create();
    SetMultimap<CFANode, AbstractionPredicate> localPredicates = HashMultimap.create();

    Set<AbstractionPredicate> currentSet = null;
    String currentLine;
    while ((currentLine = reader.readLine()) != null) {
      lineNo++;
      currentLine = currentLine.trim();
      if (currentLine.isEmpty()) {
        // blank lines separates sections
        currentSet = null;
        continue;
      }

      if (currentLine.startsWith("//")) {
        // comment
        continue;
      }

      if (currentSet == null) {
        // we expect a new section header
        if (!currentLine.endsWith(":")) {
          throw new PredicateParsingFailedException(currentLine + " is not a valid section header",
              source, lineNo);
        }
        currentLine = currentLine.substring(0, currentLine.length() - 1).trim(); // strip off ":"
        if (currentLine.isEmpty()) {
          throw new PredicateParsingFailedException("empty key is not allowed", source, lineNo);
        }

        if (currentLine.equals("*") || applyGlobally) {
          // the section "*"
          currentSet = globalPredicates;

        } else if (FUNCTION_NAME_PATTERN.matcher(currentLine).matches()) {
          // a section with a function name

          if (!cfa.getAllFunctionNames().contains(currentLine)) {
            logger.log(Level.WARNING, "Cannot use predicates for function",
                currentLine + ", this function does not exist.");
            currentSet = new HashSet<>(); // temporary set which will be thrown away and ignored

          } else {
            currentSet = functionPredicates.get(currentLine);
          }

        } else {
          Matcher matcher = CFA_NODE_PATTERN.matcher(currentLine);
          if (matcher.matches()) {
            // a section with a CFA node

            String function = matcher.group(1);
            int nodeId =
                Integer.parseInt(matcher.group(2)); // does not fail, we checked with regexp

            if (applyFunctionWide) {
              if (!cfa.getAllFunctionNames().contains(function)) {
                logger.log(Level.WARNING, "Cannot use predicates for function",
                    function + ", this function does not exist.");
                currentSet = new HashSet<>(); // temporary set which will be thrown away and ignored
              } else {
                currentSet = functionPredicates.get(function);
              }

            } else {
              CFANode node = getCFANodeWithId(nodeId);
              if (node == null) {
                logger.log(Level.WARNING, "Cannot use predicates for CFANode",
                    nodeId + ", this node does not exist.");
                currentSet = new HashSet<>(); // temporary set which will be thrown away and ignored
              } else {
                currentSet = localPredicates.get(node);
              }
            }

          } else {
            throw new PredicateParsingFailedException(currentLine + " is not a valid key", source,
                lineNo);
          }
        }

      } else {
        // we expect a predicate
        if (currentLine.startsWith("(assert ") && currentLine.endsWith(")")) {

          if (encodePredicates != PrecisionConverter.DISABLE) {
            currentLine = convertFormula(converter, currentLine);
            if (currentLine == null) {
              // ignore formula, if converting fails.
              // We parse only predicates for a precision,
              // so we can ignore some predicates without losing too much information.
              continue;
            }
          }

          BooleanFormula f;
          try {
            f = fmgr.parse(commonDefinitions + currentLine);
          } catch (IllegalArgumentException e) {
            throw new PredicateParsingFailedException(e, source, lineNo);
          }

          currentSet.add(amgr.makePredicate(f));

        } else {
          throw new PredicateParsingFailedException("unexpected line " + currentLine, source,
              lineNo);
        }
      }
    }

    return new PredicatePrecision(
        ImmutableSetMultimap.<Pair<CFANode, Integer>, AbstractionPredicate>of(),
        localPredicates, functionPredicates, globalPredicates);
  }

  private
  @Nullable
  String convertFormula(final Converter converter, final String line) {
    return FormulaParser.convertFormula(checkNotNull(converter), line, logger);
  }

  private CFANode getCFANodeWithId(int id) {
    if (idToNodeMap.isEmpty()) {
      for (CFANode n : cfa.getAllNodes()) {
        idToNodeMap.put(n.getNodeNumber(), n);
      }
    }
    return idToNodeMap.get(id);
  }
}
