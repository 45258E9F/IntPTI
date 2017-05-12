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
package org.sosy_lab.cpachecker.core.phase.config;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhaseConfigParser {

  private PhaseCollection collection;
  private PhaseDependency dependency;

  private final static Splitter DEPEND_SPLITTER = Splitter.on(';').omitEmptyStrings().trimResults();
  private final static Splitter TARGET_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

  public PhaseConfigParser() {
    collection = new PhaseCollection();
    dependency = new PhaseDependency();
  }

  private static final Pattern setPattern = Pattern.compile("^\\.SET([\\S\\s]*)\\.TES");
  private static final Pattern phasePattern = Pattern.compile("^\\.DEF\\s+(\\w+)\\s+\\"
      + ".TYPE\\s*=\\s*(\\S+)\\s+([\\S\\s]*?)\\.FED");
  private static final Pattern runPattern = Pattern.compile("^\\.RUN\\s+([\\S\\s]*)$");

  private static final Pattern dependPattern = Pattern.compile("^(\\w+)\\s*:([\\S\\s]+)$");
  private static final Pattern aliasPattern = Pattern.compile("^(\\w+)\\s*=\\s*(\\S*)\\s*$");

  /**
   * Format for alias in configuration. This is similar to alias in
   * Makefile!
   */
  private static final String aliasFormat = "$(%s)";

  /**
   * Parse the configuration file content
   *
   * @param content of configuration
   * @return true if succeeded, false if failed
   */
  public boolean parse(String content) {
    content = content.trim();
    while (!content.isEmpty()) {
      Matcher match;

      match = setPattern.matcher(content);
      if (match.find()) {
        String alias = match.group(1).trim();
        Map<String, String> aliasMap = new HashMap<>();
        if (!parseAlias(alias, aliasMap)) {
          // failed to parse alias
          return false;
        }
        content = match.replaceAll("").trim();
        // replace all $(PATTERN) to actual char sequence
        for (Entry<String, String> aliasEntry : aliasMap.entrySet()) {
          String aliasKey = String.format(aliasFormat, aliasEntry.getKey());
          content = content.replace(aliasKey, aliasEntry.getValue());
        }
        continue;
      }

      match = phasePattern.matcher(content);
      if (match.find()) {
        String phaseName = match.group(1);
        String phaseType = match.group(2);
        String phaseConfig = match.group(3);

        PhaseInfo info = new PhaseInfo(phaseType, phaseName, phaseConfig);
        collection.addPhaseInfo(info);

        content = match.replaceAll("").trim();
        continue;
      }

      match = runPattern.matcher(content);
      if (match.find()) {
        String dependencies = match.group(1).trim();
        if (!parseDependencies(dependencies, dependency)) {
          // failed to parse dependency
          return false;
        }

        content = match.replaceAll("").trim();
        continue;
      }

      // none of these patterns succeed in matching
      return false;
    }
    return true;
  }

  private boolean parseAlias(String aliasContent, Map<String, String> aliasMap) {
    Iterable<String> aliasItems = DEPEND_SPLITTER.split(aliasContent);
    for (String line : aliasItems) {
      String trimmedLine = line.trim();
      if (trimmedLine.isEmpty()) {
        // the empty line is usually derived from split operation
        continue;
      }
      Matcher match = aliasPattern.matcher(trimmedLine);
      if (match.find()) {
        String subject = match.group(1);
        String target = match.group(2);
        aliasMap.put(subject, target);
      } else {
        // parse error
        return false;
      }
    }
    return true;
  }

  private boolean parseDependencies(String content, PhaseDependency pDepend) {
    // we can guarantee that the {@code content} is trimmed off
    if (content.isEmpty()) {
      // nothing to do, return true directly
      return true;
    }
    List<String> depends = DEPEND_SPLITTER.splitToList(content);
    Set<String> visited = new HashSet<>();
    boolean firstDepend = true;
    for (String depend : depends) {
      String item = depend.trim();
      if (item.isEmpty()) {
        continue;
      }
      Matcher match = dependPattern.matcher(item);
      if (match.find()) {
        firstDepend = false;
        String subject = match.group(1);
        String target = match.group(2);
        if (visited.contains(subject)) {
          // duplicated item to be discarded
          continue;
        }
        visited.add(subject);
        // parse targets
        List<String> targets = TARGET_SPLITTER.splitToList(target.trim());
        pDepend.insertDependency(subject, targets);
      } else {
        if (firstDepend) {
          // then no dependency is to be built
          return true;
        }
        // syntax error in configuration file
        return false;
      }
    }
    return true;
  }

  /**
   * This function checks whether all phases in dependency are declared in
   * phase collection
   *
   * @return {@code true} if and only if no new phase is used in dependency
   */
  public boolean isConsistent() {
    // we just enumerate all elements.
    // in practice the phase flow could not be so complicated
    Set<String> phaseInDepend = dependency.keySet();
    for (String phase : phaseInDepend) {
      if (!collection.contains(phase)) {
        return false;
      }
      Set<String> relevants = dependency.queryFor(phase);
      // null value should not occur here
      Preconditions.checkNotNull(relevants);
      for (String relevant : relevants) {
        if (!collection.contains(relevant)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * This function checks whether the dependency relation is valid.
   * A valid dependency should be modelled as a connected graph
   *
   * @return {@code true} if and only if phases are well-connected
   */
  public boolean checkIfValidDependency() {
    // we use {@link HashMultimap} structure to construct an equivalence relation
    // that reflects the dependency derived from configuration file
    // TODO: how to implement an efficient checking algorithm?
    // otherwise, all nodes in dependency graph are well-connected
    return true;
  }

  public PhaseCollection getPhases() {
    return collection;
  }

  public PhaseDependency getDependency() {
    return dependency;
  }
}
