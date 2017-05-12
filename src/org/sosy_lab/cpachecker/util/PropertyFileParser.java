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
package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableMap;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.cfa.CFACreator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple class that reads a property, i.e. basically an entry function and a proposition, from a
 * given property, and maps the proposition to a file from where to read the specification
 * automaton.
 */
public class PropertyFileParser {

  public static class InvalidPropertyFileException extends Exception {

    private static final long serialVersionUID = -5880923544560903123L;

    public InvalidPropertyFileException(String msg) {
      super(msg);
    }

    public InvalidPropertyFileException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }

  private final Path propertyFile;

  private String entryFunction;
  private final EnumSet<PropertyType> properties = EnumSet.noneOf(PropertyType.class);

  private static final Pattern PROPERTY_PATTERN =
      Pattern.compile("CHECK\\( init\\((" + CFACreator.VALID_C_FUNCTION_NAME_PATTERN
          + ")\\(\\)\\), LTL\\((.+)\\) \\)");

  public PropertyFileParser(final Path pPropertyFile) {
    propertyFile = pPropertyFile;
  }

  public void parse() throws InvalidPropertyFileException {
    String rawProperty = null;
    try (BufferedReader br = propertyFile.asCharSource(Charset.defaultCharset())
        .openBufferedStream()) {
      while ((rawProperty = br.readLine()) != null) {
        if (!rawProperty.isEmpty()) {
          properties.add(parsePropertyLine(rawProperty));
        }
      }
    } catch (IOException e) {
      throw new InvalidPropertyFileException("Could not read file: " + e.getMessage(), e);
    }

    if (properties.isEmpty()) {
      throw new InvalidPropertyFileException("No property in file.");
    }
  }

  private PropertyType parsePropertyLine(String rawProperty) throws InvalidPropertyFileException {
    Matcher matcher = PROPERTY_PATTERN.matcher(rawProperty);

    if (rawProperty == null || !matcher.matches() || matcher.groupCount() != 2) {
      throw new InvalidPropertyFileException(String.format(
          "The given property '%s' is not well-formed!", rawProperty));
    }

    if (entryFunction == null) {
      entryFunction = matcher.group(1);
    } else if (!entryFunction.equals(matcher.group(1))) {
      throw new InvalidPropertyFileException(String.format(
          "Specifying two different entry functions %s and %s is not supported.", entryFunction,
          matcher.group(1)));
    }

    PropertyType property = PropertyType.AVAILABLE_PROPERTIES.get(matcher.group(2));
    if (property == null) {
      throw new InvalidPropertyFileException(String.format(
          "The property '%s' is not supported.", matcher.group(2)));
    }
    return property;
  }

  public String getEntryFunction() {
    return entryFunction;
  }

  public EnumSet<PropertyType> getProperties() {
    return properties;
  }

  public enum PropertyType {
    REACHABILITY_LABEL,
    REACHABILITY,
    VALID_FREE,
    VALID_DEREF,
    VALID_MEMTRACK,
    OVERFLOW,;

    private static ImmutableMap<String, PropertyType> AVAILABLE_PROPERTIES =
        ImmutableMap.<String, PropertyType>builder()
            .put("G ! label(ERROR)", PropertyType.REACHABILITY_LABEL)
            .put("G ! call(__VERIFIER_error())", PropertyType.REACHABILITY)
            .put("G valid-free", PropertyType.VALID_FREE)
            .put("G valid-deref", PropertyType.VALID_DEREF)
            .put("G valid-memtrack", PropertyType.VALID_MEMTRACK)
            .put("G ! overflow", PropertyType.OVERFLOW).build();
  }
}