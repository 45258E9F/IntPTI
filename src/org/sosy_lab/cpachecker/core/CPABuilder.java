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
package org.sosy_lab.cpachecker.core;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidComponentException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Options
public class CPABuilder {

  private static final String CPA_OPTION_NAME = "cpa";
  private static final String CPA_CLASS_PREFIX = "org.sosy_lab.cpachecker";

  private static final Splitter LIST_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  @Option(secure = true, name = CPA_OPTION_NAME,
      description = "CPA to use (see doc/Configuration.txt for more documentation on this)")
  private String cpaName = CompositeCPA.class.getCanonicalName();

  @Option(secure = true, name = "specification",
      description = "comma-separated list of files with specifications that should be checked"
          + "\n(see config/specification/ for examples)")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> specificationFiles = null;

  @Option(secure = true, name = "backwardSpecification",
      description = "comma-separated list of files with specifications that should be used "
          + "\nin a backwards analysis; used if the full analysis consists of a forward AND a backward part!"
          + "\n(see config/specification/ for examples)")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> backwardSpecificationFiles = null;

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final ReachedSetFactory reachedSetFactory;

  public CPABuilder(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      ReachedSetFactory pReachedSetFactory) throws InvalidConfigurationException {
    this.config = pConfig;
    this.logger = pLogger;
    this.shutdownNotifier = pShutdownNotifier;
    this.reachedSetFactory = pReachedSetFactory;
    config.inject(this);
  }

  public ConfigurableProgramAnalysis buildCPAWithSpecAutomatas(final CFA cfa)
      throws InvalidConfigurationException, CPAException {

    // create automata cpas for the specification files given in "specification"
    return buildCPAs(cfa, specificationFiles);
  }

  public ConfigurableProgramAnalysis buildCPAWithBackwardSpecAutomatas(final CFA cfa)
      throws InvalidConfigurationException, CPAException {
    // create automata cpas for the specification files given in "backwardSpecification"
    return buildCPAs(cfa, backwardSpecificationFiles);
  }

  public ConfigurableProgramAnalysis buildsCPAWithWitnessAutomataAndSpecification(
      final CFA cfa,
      @Nonnull List<Automaton> automata) throws InvalidConfigurationException, CPAException {
    Set<String> usedAliases = new HashSet<>();

    List<ConfigurableProgramAnalysis> cpas = null;

    if (specificationFiles != null) {
      cpas = createSpecificationCPAs(cfa, specificationFiles, usedAliases);
    }

    if (!automata.isEmpty()) {
      if (cpas == null) {
        cpas = new ArrayList<>();
      }

      for (Automaton automaton : automata) {
        String cpaAlias = automaton.getName();

        if (!usedAliases.add(cpaAlias)) {
          throw new InvalidConfigurationException(
              "Name " + cpaAlias + " used twice for an automaton.");
        }

        CPAFactory factory = ControlAutomatonCPA.factory();
        factory.setConfiguration(Configuration.copyWithNewPrefix(config, cpaAlias));
        factory.setLogger(logger.withComponentName(cpaAlias));
        factory.set(cfa, CFA.class);
        factory.set(automaton, Automaton.class);

        cpas.add(factory.createInstance());

        logger.log(Level.FINER, "Loaded Automaton\"" + automaton.getName() + "\"");
      }
    }

    return buildCPAs(cpaName, CPA_OPTION_NAME, usedAliases, cpas, cfa);
  }

  public ConfigurableProgramAnalysis buildCPAs(
      final CFA cfa,
      @Nullable final List<Path> specAutomatonFiles)
      throws InvalidConfigurationException, CPAException {
    Set<String> usedAliases = new HashSet<>();

    List<ConfigurableProgramAnalysis> cpas = null;

    if (specAutomatonFiles != null) {
      cpas = createSpecificationCPAs(cfa, specAutomatonFiles, usedAliases);
    }

    return buildCPAs(cpaName, CPA_OPTION_NAME, usedAliases, cpas, cfa);
  }

  /**
   * create automata cpas for the specification files given as argument
   */
  private List<ConfigurableProgramAnalysis> createSpecificationCPAs(
      final CFA cfa, final List<Path> specAutomatonFiles,
      Set<String> usedAliases)
      throws InvalidConfigurationException, CPAException {

    List<ConfigurableProgramAnalysis> cpas = new ArrayList<>();

    for (Path specFile : specAutomatonFiles) {
      List<Automaton> automata = Collections.emptyList();
      Scope scope = createScope(cfa);

      // Check that the automaton file exists and is not empty
      try {
        if (specFile.asCharSource(StandardCharsets.UTF_8).isEmpty()) {
          throw new InvalidConfigurationException("The specification file is empty: " + specFile);
        }
      } catch (IOException e) {
        throw new InvalidConfigurationException(
            "Could not load automaton from file " + e.getMessage(), e);
      }

      if (AutomatonGraphmlParser.isGraphmlAutomaton(specFile, logger)) {
        AutomatonGraphmlParser graphmlParser =
            new AutomatonGraphmlParser(config, logger, cfa, cfa.getMachineModel(), scope);
        automata = graphmlParser.parseAutomatonFile(specFile);

      } else {
        automata = AutomatonParser
            .parseAutomatonFile(specFile, config, logger, cfa.getMachineModel(), scope,
                cfa.getLanguage());
      }

      if (automata.isEmpty()) {
        throw new InvalidConfigurationException(
            "Specification file contains no automata: " + specFile);
      }

      for (Automaton automaton : automata) {
        String cpaAlias = automaton.getName();

        if (!usedAliases.add(cpaAlias)) {
          throw new InvalidConfigurationException(
              "Name " + cpaAlias + " used twice for an automaton.");
        }

        CPAFactory factory = ControlAutomatonCPA.factory();
        factory.setConfiguration(Configuration.copyWithNewPrefix(config, cpaAlias));
        factory.setLogger(logger.withComponentName(cpaAlias));
        factory.set(cfa, CFA.class);
        factory.set(automaton, Automaton.class);

        cpas.add(factory.createInstance());

        logger.log(Level.FINER, "Loaded Automaton\"" + automaton.getName() + "\"");
      }
    }

    return cpas;
  }

  private Scope createScope(CFA cfa) {
    Language usedLanguage = cfa.getLanguage();

    switch (usedLanguage) {
      case C:
        return new CProgramScope(cfa, logger);

      default:
        return DummyScope.getInstance();
    }
  }

  private ConfigurableProgramAnalysis buildCPAs(
      String optionValue,
      String optionName,
      Set<String> usedAliases,
      List<ConfigurableProgramAnalysis> cpas,
      final CFA cfa) throws InvalidConfigurationException, CPAException {
    Preconditions.checkNotNull(optionValue);

    // parse option (may be of syntax "classname alias"
    String[] optionParts = optionValue.trim().split("\\s+");
    String cpaName = optionParts[0];
    String cpaAlias = getCPAAlias(optionValue, optionName, optionParts, cpaName);

    if (!usedAliases.add(cpaAlias)) {
      throw new InvalidConfigurationException("Alias " + cpaAlias + " used twice for a CPA.");
    }

    // first get instance of appropriate factory

    Class<?> cpaClass = getCPAClass(optionName, cpaName);

    logger.log(Level.FINER, "Instantiating CPA " + cpaClass.getName() + " with alias " + cpaAlias);

    CPAFactory factory = getFactoryInstance(cpaName, cpaClass);

    // now use factory to get an instance of the CPA

    factory.setConfiguration(Configuration.copyWithNewPrefix(config, cpaAlias));
    factory.setLogger(logger.withComponentName(cpaAlias));
    factory.setShutdownNotifier(shutdownNotifier);
    if (reachedSetFactory != null) {
      factory.set(reachedSetFactory, ReachedSetFactory.class);
    }
    if (cfa != null) {
      factory.set(cfa, CFA.class);
    }

    boolean hasChildren =
        createAndSetChildrenCPAs(cpaName, cpaAlias, factory, usedAliases, cpas, cfa);

    if (cpas != null && !cpas.isEmpty()) {
      throw new InvalidConfigurationException(
          "Option specification gave specification automata, but no CompositeCPA was used");
    }
    if (optionName.equals(CPA_OPTION_NAME)
        && cpaClass.equals(CompositeCPA.class)
        && !hasChildren) {
      // This is the top-level CompositeCPA that is the default,
      // but without any children. This means that the user did not specify any
      // meaningful configuration.
      throw new InvalidConfigurationException(
          "Please specify a configuration with '-config CONFIG_FILE' or '-CONFIG' (for example, '-valueAnalysis' or '-predicateAnalysis'). See README.txt for more details.");
    }

    // finally call createInstance
    ConfigurableProgramAnalysis cpa;
    try {
      cpa = factory.createInstance();
    } catch (IllegalStateException e) {
      throw new InvalidComponentException(cpaClass, "CPA", e);
    }
    if (cpa == null) {
      throw new InvalidComponentException(cpaClass, "CPA", "Factory returned null.");
    }
    logger.log(Level.FINER,
        "Sucessfully instantiated CPA " + cpa.getClass().getName() + " with alias " + cpaAlias);
    return cpa;
  }

  private String getCPAAlias(
      String optionValue, String optionName,
      String[] optionParts, String cpaName) throws InvalidConfigurationException {

    if (optionParts.length == 1) {
      // no user-specified alias, use last part of class name
      int dotIndex = cpaName.lastIndexOf('.');
      return (dotIndex >= 0 ? cpaName.substring(dotIndex + 1) : cpaName);

    } else if (optionParts.length == 2) {
      return optionParts[1];

    } else {
      throw new InvalidConfigurationException(
          "Option " + optionName + " contains invalid CPA specification \"" + optionValue + "\"!");
    }
  }

  private Class<?> getCPAClass(String optionName, String cpaName)
      throws InvalidConfigurationException {
    Class<?> cpaClass;
    try {
      cpaClass = Classes.forName(cpaName, CPA_CLASS_PREFIX);
    } catch (ClassNotFoundException e) {
      throw new InvalidConfigurationException(
          "Option " + optionName + " is set to unknown CPA " + cpaName, e);
    }

    if (!ConfigurableProgramAnalysis.class.isAssignableFrom(cpaClass)) {
      throw new InvalidConfigurationException(
          "Option " + optionName
              + " has to be set to a class implementing the ConfigurableProgramAnalysis interface!");
    }

    Classes.produceClassLoadingWarning(logger, cpaClass, ConfigurableProgramAnalysis.class);

    return cpaClass;
  }

  private CPAFactory getFactoryInstance(String cpaName, Class<?> cpaClass) throws CPAException {

    // get factory method
    Method factoryMethod;
    try {
      factoryMethod = cpaClass.getMethod("factory", (Class<?>[]) null);
    } catch (NoSuchMethodException e) {
      throw new InvalidComponentException(cpaClass, "CPA",
          "No public static method \"factory\" with zero parameters.");
    }

    // verify signature
    if (!Modifier.isStatic(factoryMethod.getModifiers())) {
      throw new InvalidComponentException(cpaClass, "CPA", "Factory method is not static.");
    }

    String exception = Classes.verifyDeclaredExceptions(factoryMethod, CPAException.class);
    if (exception != null) {
      throw new InvalidComponentException(cpaClass, "CPA",
          "Factory method declares the unsupported checked exception " + exception + " .");
    }

    // invoke factory method
    Object factoryObj;
    try {
      factoryObj = factoryMethod.invoke(null, (Object[]) null);

    } catch (IllegalAccessException e) {
      throw new InvalidComponentException(cpaClass, "CPA", "Factory method is not public.");

    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      Throwables.propagateIfPossible(cause, CPAException.class);

      throw new UnexpectedCheckedException("instantiation of CPA " + cpaName, cause);
    }

    if ((factoryObj == null) || !(factoryObj instanceof CPAFactory)) {
      throw new InvalidComponentException(cpaClass, "CPA",
          "Factory method did not return a CPAFactory instance.");
    }

    return (CPAFactory) factoryObj;
  }

  private boolean createAndSetChildrenCPAs(
      String cpaName, String cpaAlias,
      CPAFactory factory, Set<String> usedAliases, List<ConfigurableProgramAnalysis> cpas,
      final CFA cfa) throws InvalidConfigurationException, CPAException {
    String childOptionName = cpaAlias + ".cpa";
    String childrenOptionName = cpaAlias + ".cpas";
    String childCpaName = config.getProperty(childOptionName);
    String childrenCpaNames = config.getProperty(childrenOptionName);

    if (childrenCpaNames == null && childCpaName == null && cpaAlias.equals("CompositeCPA")
        && cpas != null && !cpas.isEmpty()) {
      // if a specification was given, but no CPAs, insert a LocationCPA
      childrenCpaNames = LocationCPA.class.getCanonicalName();
    }

    if (childCpaName != null) {
      // only one child CPA
      if (childrenCpaNames != null) {
        throw new InvalidConfigurationException("Ambiguous configuration: both "
            + childOptionName + " and " + childrenOptionName + " are specified!");
      }

      ConfigurableProgramAnalysis child =
          buildCPAs(childCpaName, childOptionName, usedAliases, cpas, cfa);
      try {
        factory.setChild(child);
      } catch (UnsupportedOperationException e) {
        throw new InvalidConfigurationException(
            cpaName + " is no wrapper CPA, but option " + childOptionName + " was specified!", e);
      }
      logger.log(Level.FINER, "CPA " + cpaAlias + " got child " + childCpaName);
      return true;

    } else if (childrenCpaNames != null) {
      // several children CPAs
      ImmutableList.Builder<ConfigurableProgramAnalysis> childrenCpas = ImmutableList.builder();

      for (String currentChildCpaName : LIST_SPLITTER.split(childrenCpaNames)) {
        childrenCpas
            .add(buildCPAs(currentChildCpaName, childrenOptionName, usedAliases, null, cfa));
      }
      if (cpas != null) {
        childrenCpas.addAll(cpas);
        cpas.clear();
      }

      try {
        factory.setChildren(childrenCpas.build());
      } catch (UnsupportedOperationException e) {
        throw new InvalidConfigurationException(
            cpaName + " is no wrapper CPA, but option " + childrenOptionName + " was specified!",
            e);
      }
      logger.log(Level.FINER, "CPA " + cpaAlias + " got children " + childrenCpaNames);
      return true;
    }
    return false;
  }
}
