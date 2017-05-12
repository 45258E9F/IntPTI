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
package org.sosy_lab.cpachecker.cmdline;

import static org.sosy_lab.cpachecker.cmdline.CPAMain.ERROR_EXIT_CODE;
import static org.sosy_lab.cpachecker.cmdline.CPAMain.ERROR_OUTPUT;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import org.sosy_lab.common.configuration.OptionCollector;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.util.PropertyFileParser;
import org.sosy_lab.cpachecker.util.PropertyFileParser.InvalidPropertyFileException;
import org.sosy_lab.cpachecker.util.PropertyFileParser.PropertyType;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This classes parses the CPAchecker command line arguments.
 * To add a new argument, handle it in {@link #processArguments(String[])}
 * and list it in {@link #printHelp(PrintStream)}.
 */
class CmdLineArguments {

  private static final Splitter SETPROP_OPTION_SPLITTER = Splitter.on('=').trimResults().limit(2);

  /**
   * Exception thrown when something invalid is specified on the command line.
   */
  public static class InvalidCmdlineArgumentException extends Exception {

    private static final long serialVersionUID = -6526968677815416436L;

    private InvalidCmdlineArgumentException(final String msg) {
      super(msg);
    }

    public InvalidCmdlineArgumentException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }

  private CmdLineArguments() {
  } // prevent instantiation, this is a static helper class

  private static final Pattern DEFAULT_CONFIG_FILES_PATTERN = Pattern.compile("^[a-zA-Z0-9-+]+$");


  /**
   * The directory where to look for configuration files for options like
   * "-predicateAbstraction" that get translated into a config file name.
   */
  private static final String DEFAULT_CONFIG_FILES_DIR = "config/%s.properties";

  static final String CONFIGURATION_FILE_OPTION = "configuration.file";

  private static final String CMC_CONFIGURATION_FILES_OPTION = "restartAlgorithm.configFiles";

  private static final Pattern SPECIFICATION_FILES_PATTERN = DEFAULT_CONFIG_FILES_PATTERN;
  private static final String SPECIFICATION_FILES_TEMPLATE = "config/specification/%s.spc";
  private static final String REACHABILITY_LABEL_SPECIFICATION_FILE =
      "config/specification/sv-comp-errorlabel.spc";
  private static final String REACHABILITY_SPECIFICATION_FILE =
      "config/specification/sv-comp-reachability.spc";
  private static final String MEMORYSAFETY_SPECIFICATION_FILE_DEREF =
      "config/specification/memorysafety-deref.spc";
  private static final String MEMORYSAFETY_SPECIFICATION_FILE_FREE =
      "config/specification/memorysafety-free.spc";
  private static final String MEMORYSAFETY_SPECIFICATION_FILE_MEMTRACK =
      "config/specification/memorysafety-memtrack.spc";
  private static final String OVERFLOW_SPECIFICATION_FILE = "config/specification/overflow.spc";

  private static final Pattern PROPERTY_FILE_PATTERN = Pattern.compile("(.)+\\.prp");

  /**
   * Option name for hack to allow witness export to access specified property files.
   * DO NOT USE otherwise.
   */
  private static final String PROPERTY_OPTION = "properties";

  static final String SECURE_MODE_OPTION = "secureMode";

  /**
   * Reads the arguments and process them.
   *
   * In some special cases this method may terminate the VM.
   *
   * @param args commandline arguments
   * @return a map with all options found in the command line
   * @throws InvalidCmdlineArgumentException if there is an error in the command line
   */
  static Map<String, String> processArguments(final String[] args)
      throws InvalidCmdlineArgumentException {
    Map<String, String> properties = new HashMap<>();
    List<String> programs = new ArrayList<>();

    Iterator<String> argsIt = Arrays.asList(args).iterator();

    while (argsIt.hasNext()) {
      String arg = argsIt.next();
      if (handleArgument0("-stats", "statistics.print", "true", arg, properties)
          || handleArgument0("-noout", "output.disable", "true", arg, properties)
          || handleArgument0("-java", "language", "JAVA", arg, properties)
          || handleArgument0("-32", "analysis.machineModel", "Linux32", arg, properties)
          || handleArgument0("-64", "analysis.machineModel", "Linux64", arg, properties)
          || handleArgument0("-preprocess", "parser.usePreprocessor", "true", arg, properties)
          || handleArgument0("-secureMode", SECURE_MODE_OPTION, "true", arg, properties)
          || handleArgument1("-outputpath", "output.path", arg, argsIt, properties)
          || handleArgument1("-logfile", "log.file", arg, argsIt, properties)
          || handleArgument1("-entryfunction", "analysis.entryFunction", arg, argsIt, properties)
          || handleArgument1("-config", CONFIGURATION_FILE_OPTION, arg, argsIt, properties)
          || handleArgument1("-timelimit", "limits.time.cpu", arg, argsIt, properties)
          || handleArgument1("-sourcepath", "java.sourcepath", arg, argsIt, properties)
          || handleArgument1("-cp", "java.classpath", arg, argsIt, properties)
          || handleArgument1("-classpath", "java.classpath", arg, argsIt, properties)
          || handleMultipleArgument1("-spec", "specification", arg, argsIt, properties)
          ) {
        // nothing left to do
      } else if (arg.equals("-cmc")) {
        handleCmc(argsIt, properties);

      } else if (arg.equals("-cpas")) {
        if (argsIt.hasNext()) {
          properties.put("cpa", CompositeCPA.class.getName());
          properties.put(CompositeCPA.class.getSimpleName() + ".cpas", argsIt.next());
        } else {
          throw new InvalidCmdlineArgumentException("-cpas argument missing.");
        }

      } else if (arg.equals("-cbmc")) {
        putIfNotExistent(properties, "analysis.checkCounterexamples", "true");
        putIfNotExistent(properties, "counterexample.checker", "CBMC");

      } else if (arg.equals("-nolog")) {
        putIfNotExistent(properties, "log.level", "off");
        putIfNotExistent(properties, "log.consoleLevel", "off");

      } else if (arg.equals("-skipRecursion")) {
        putIfNotExistent(properties, "analysis.summaryEdges", "true");
        putIfNotExistent(properties, "cpa.callstack.skipRecursion", "true");

      } else if (arg.equals("-setprop")) {
        if (argsIt.hasNext()) {
          String s = argsIt.next();
          List<String> bits = Lists.newArrayList(SETPROP_OPTION_SPLITTER.split(s));
          if (bits.size() != 2) {
            throw new InvalidCmdlineArgumentException(
                "-setprop argument must be a key=value pair, but \"" + s + "\" is not.");
          }
          putIfNotExistent(properties, bits.get(0), bits.get(1));
        } else {
          throw new InvalidCmdlineArgumentException("-setprop argument missing.");
        }

      } else if ("-printOptions".equals(arg)) {
        boolean verbose = false;
        if (argsIt.hasNext()) {
          final String nextArg = argsIt.next();
          verbose = ("-v".equals(nextArg) || ("-verbose".equals(nextArg)));
        }
        PrintStream out = System.out;
        OptionCollector.collectOptions(verbose, true, out);
        System.exit(0);

      } else if ("-printUsedOptions".equals(arg)) {
        putIfNotExistent(properties, "log.usedOptions.export", "true");
        putIfNotExistent(properties, "analysis.disable", "true");

        // this will disable all other output
        properties.put("log.consoleLevel", "SEVERE");

      } else if (arg.equals("-help") || arg.equals("-h")) {
        printHelp(System.out);
        System.exit(0);

      } else if (arg.startsWith("-") && !(Paths.get(arg).exists())) {
        String argName = arg.substring(1); // remove "-"
        if (DEFAULT_CONFIG_FILES_PATTERN.matcher(argName).matches()) {
          Path configFile = findFile(DEFAULT_CONFIG_FILES_DIR, argName);

          if (configFile != null) {
            try {
              Files.checkReadableFile(configFile);
              putIfNotExistent(properties, CONFIGURATION_FILE_OPTION, configFile.toString());
            } catch (FileNotFoundException e) {
              ERROR_OUTPUT
                  .println("Invalid configuration " + argName + " (" + e.getMessage() + ")");
              System.exit(ERROR_EXIT_CODE);
            }
          } else {
            ERROR_OUTPUT.println("Invalid option " + arg);
            ERROR_OUTPUT.println("If you meant to specify a configuration file, the file "
                + String.format(DEFAULT_CONFIG_FILES_DIR, argName) + " does not exist.");
            ERROR_OUTPUT.println("");
            printHelp(ERROR_OUTPUT);
            System.exit(ERROR_EXIT_CODE);
          }
        } else {
          ERROR_OUTPUT.println("Invalid option " + arg);
          ERROR_OUTPUT.println("");
          printHelp(ERROR_OUTPUT);
          System.exit(ERROR_EXIT_CODE);
        }

      } else {
        programs.add(arg);
      }
    }

    // arguments with non-specified options are considered as file names
    if (!programs.isEmpty()) {
      putIfNotExistent(properties, "analysis.programNames", Joiner.on(", ").join(programs));
    }

    return properties;
  }

  private static void handleCmc(final Iterator<String> argsIt, final Map<String, String> properties)
      throws InvalidCmdlineArgumentException {
    properties.put("analysis.restartAfterUnknown", "true");

    if (argsIt.hasNext()) {
      String newValue = argsIt.next();

      // replace "predicateAnalysis" with config/predicateAnalysis.properties etc.
      if (DEFAULT_CONFIG_FILES_PATTERN.matcher(newValue).matches() && !(Paths.get(newValue)
          .exists())) {
        Path configFile = findFile(DEFAULT_CONFIG_FILES_DIR, newValue);

        if (configFile != null) {
          newValue = configFile.toString();
        }
      }

      String value = properties.get(CMC_CONFIGURATION_FILES_OPTION);
      if (value != null) {
        value += "," + newValue;
      } else {
        value = newValue;
      }
      properties.put(CMC_CONFIGURATION_FILES_OPTION, value);

    } else {
      throw new InvalidCmdlineArgumentException("-cmc argument missing.");
    }
  }

  private static void printHelp(PrintStream out) {
    out.println("CPAchecker " + CPAchecker.getVersion());
    out.println();
    out.println("OPTIONS:");
    out.println(" -config");
    out.println(" -cpas");
    out.println(" -spec");
    out.println(" -outputpath");
    out.println(" -logfile");
    out.println(" -entryfunction");
    out.println(" -timelimit");
    out.println(" -cbmc");
    out.println(" -stats");
    out.println(" -nolog");
    out.println(" -noout");
    out.println(" -java");
    out.println(" -32");
    out.println(" -64");
    out.println(" -secureMode");
    out.println(" -skipRecursion");
    out.println(" -setprop");
    out.println(" -printOptions [-v|-verbose]");
    out.println(" -printUsedOptions");
    out.println(" -help");
    out.println();
    out.println("You can also specify any of the configuration files in the directory config/");
    out.println(
        "with -CONFIG_FILE, e.g., -predicateAnalysis for config/predicateAnalysis.properties.");
    out.println();
    out.println(
        "More information on how to configure CPAchecker can be found in 'doc/Configuration.txt'.");
  }

  private static void putIfNotExistent(
      final Map<String, String> properties,
      final String key,
      final String value)
      throws InvalidCmdlineArgumentException {

    if (properties.containsKey(key)) {
      throw new InvalidCmdlineArgumentException(
          "Duplicate option " + key + " specified on command-line.");
    }

    properties.put(key, value);
  }

  private static void putIfNotDifferent(
      final Map<String, String> properties,
      final String key,
      final String value)
      throws InvalidCmdlineArgumentException {

    if (properties.containsKey(key) && !properties.get(key).equals(value)) {
      throw new InvalidCmdlineArgumentException(
          "Duplicate option " + key + " specified on command-line, "
              + "with different values " + properties.get(key) + " and " + value + ".");
    }

    properties.put(key, value);
  }

  private static void appendOptionValue(
      final Map<String, String> options,
      final String option,
      String newValue) {
    if (newValue != null) {
      String value = options.get(option);
      if (value != null) {
        value = value + "," + newValue;
      } else {
        value = newValue;
      }
      options.put(option, value);
    }
  }

  /**
   * Handle a command line argument with no value.
   */
  private static boolean handleArgument0(
      final String arg, final String option, final String value, final String currentArg,
      final Map<String, String> properties) throws InvalidCmdlineArgumentException {
    if (currentArg.equals(arg)) {
      putIfNotExistent(properties, option, value);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Handle a command line argument with one value.
   */
  private static boolean handleArgument1(
      final String arg, final String option, final String currentArg,
      final Iterator<String> args, final Map<String, String> properties)
      throws InvalidCmdlineArgumentException {
    if (currentArg.equals(arg)) {
      if (args.hasNext()) {
        putIfNotExistent(properties, option, args.next());
      } else {
        throw new InvalidCmdlineArgumentException(currentArg + " argument missing.");
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Handle a command line argument with one value that may appear several times.
   */
  private static boolean handleMultipleArgument1(
      final String arg, final String option, final String currentArg,
      final Iterator<String> args, final Map<String, String> options)
      throws InvalidCmdlineArgumentException {
    if (currentArg.equals(arg)) {
      if (args.hasNext()) {

        String newValue = args.next();
        if (arg.equals("-spec")) {
          // handle normal specification definitions
          if (SPECIFICATION_FILES_PATTERN.matcher(newValue).matches()) {
            Path specFile = findFile(SPECIFICATION_FILES_TEMPLATE, newValue);
            if (specFile != null) {
              newValue = specFile.toString();
            } else {
              ERROR_OUTPUT.println("Checking for property " + newValue
                  + " is currently not supported by CPAchecker.");
              System.exit(ERROR_EXIT_CODE);
            }
          }

          // handle property files, as demanded by SV-COMP, which are just mapped to an explicit entry function and
          // the respective specification definition
          else if (PROPERTY_FILE_PATTERN.matcher(newValue).matches()) {
            Path propertyFile = Paths.get(newValue);
            if (propertyFile.toFile().exists()) {
              PropertyFileParser parser = new PropertyFileParser(propertyFile);
              try {
                parser.parse();
              } catch (InvalidPropertyFileException e) {
                throw new InvalidCmdlineArgumentException(
                    "Invalid property file: " + e.getMessage(), e);
              }
              putIfNotExistent(options, "analysis.entryFunction", parser.getEntryFunction());
              appendOptionValue(options, PROPERTY_OPTION, newValue);

              // set the file from where to read the specification automaton
              Set<PropertyType> properties = parser.getProperties();
              assert !properties.isEmpty();

              newValue = getSpecifications(options, properties);

            } else {
              ERROR_OUTPUT.println("The property file " + newValue + " does not exist.");
              System.exit(ERROR_EXIT_CODE);
            }
          }
        }
        appendOptionValue(options, option, newValue);

      } else {
        throw new InvalidCmdlineArgumentException(currentArg + " argument missing.");
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * This method returns all specifications for the given properties.
   * If needed for the analysis, some options can be set.
   */
  private static String getSpecifications(
      final Map<String, String> options,
      Set<PropertyType> properties) throws InvalidCmdlineArgumentException {
    final List<String> specifications = new ArrayList<>();
    for (PropertyType property : properties) {
      String newSpec = null;
      switch (property) {
        case VALID_DEREF:
          putIfNotDifferent(options, "memorysafety.check", "true");
          newSpec = MEMORYSAFETY_SPECIFICATION_FILE_DEREF;
          break;
        case VALID_FREE:
          putIfNotDifferent(options, "memorysafety.check", "true");
          newSpec = MEMORYSAFETY_SPECIFICATION_FILE_FREE;
          break;
        case VALID_MEMTRACK:
          putIfNotDifferent(options, "memorysafety.check", "true");
          newSpec = MEMORYSAFETY_SPECIFICATION_FILE_MEMTRACK;
          break;
        case REACHABILITY_LABEL:
          newSpec = REACHABILITY_LABEL_SPECIFICATION_FILE;
          break;
        case OVERFLOW:
          putIfNotExistent(options, "overflow.check", "true");
          newSpec = OVERFLOW_SPECIFICATION_FILE;
          break;
        case REACHABILITY:
          newSpec = REACHABILITY_SPECIFICATION_FILE;
          break;
        default:
          ERROR_OUTPUT.println("Checking for the property " + property
              + " is currently not supported by CPAchecker.");
          System.exit(ERROR_EXIT_CODE);
      }
      assert newSpec != null;
      specifications.add(newSpec);
    }
    return Joiner.on(",").join(specifications);
  }

  /**
   * Try to locate a file whose (partial) name is given by the user,
   * using a file name template which is filled with the user given name.
   *
   * If the path is relative, it is first looked up in the current directory,
   * and (if the file does not exist there), it is looked up in the parent directory
   * of the code base.
   *
   * If the file cannot be found, null is returned.
   *
   * @param template The string template for the path.
   * @param name     The value for filling in the template.
   * @return An absolute Path object pointing to an existing file or null.
   */
  private static Path findFile(final String template, final String name) {
    final String fileName = String.format(template, name);

    Path file = Paths.get(fileName);

    // look in current directory first
    if (file.toFile().exists()) {
      return file;
    }

    // look relative to code location second
    Path codeLocation = Paths
        .get(CmdLineArguments.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    Path baseDir = codeLocation.getParent();

    file = baseDir.resolve(fileName);
    if (file.toFile().exists()) {
      return file;
    }

    return null;
  }
}
