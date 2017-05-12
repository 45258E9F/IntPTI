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
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import org.sosy_lab.common.ChildFirstPatternClassLoader;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CParser.Dialect;
import org.sosy_lab.cpachecker.cfa.Parser;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;


/**
 * We load the parser in its own class loader, so both all Eclipse objects
 * and all Eclipse classes can be garbage collected when they are not needed anymore.
 * Without this, nothing could be garbage collected because all the parser objects
 * are referenced statically inside their classes.
 */
public class EclipseParsers {

  private EclipseParsers() {
  }

  private static final Pattern OUR_CLASSES =
      Pattern.compile("^(org\\.eclipse|org\\.sosy_lab\\.cpachecker\\.cfa\\.parser\\.eclipse)\\..*");

  private static final String C_PARSER_CLASS =
      "org.sosy_lab.cpachecker.cfa.parser.eclipse.c.EclipseCParser";
  private static final String JAVA_PARSER_CLASS =
      "org.sosy_lab.cpachecker.cfa.parser.eclipse.java.EclipseJavaParser";

  private static WeakReference<ClassLoader> loadedClassLoader = new WeakReference<>(null);

  private static WeakReference<Constructor<? extends CParser>> loadedCParser =
      new WeakReference<>(null);
  private static WeakReference<Constructor<? extends Parser>> loadedJavaParser =
      new WeakReference<>(null);

  private static final AtomicInteger loadingCount = new AtomicInteger(0);

  private static ClassLoader getClassLoader(LogManager logger) {
    ClassLoader classLoader = loadedClassLoader.get();
    if (classLoader != null) {
      return classLoader;
    }

    // garbage collected or first time we come here
    if (loadingCount.incrementAndGet() > 1) {
      logger.log(Level.INFO, "Repeated loading of Eclipse source parser");
    }

    classLoader = EclipseParsers.class.getClassLoader();
    if (classLoader instanceof URLClassLoader) {
      classLoader = new ChildFirstPatternClassLoader(OUR_CLASSES,
          ((URLClassLoader) classLoader).getURLs(), classLoader);
    }
    loadedClassLoader = new WeakReference<>(classLoader);
    return classLoader;
  }

  public static CParser getCParser(
      Configuration config,
      LogManager logger,
      CParser.Dialect dialect,
      MachineModel machine) {

    try {
      Constructor<? extends CParser> parserConstructor = loadedCParser.get();

      if (parserConstructor == null) {
        ClassLoader classLoader = getClassLoader(logger);

        @SuppressWarnings("unchecked")
        Class<? extends CParser> parserClass =
            (Class<? extends CParser>) classLoader.loadClass(C_PARSER_CLASS);
        parserConstructor = parserClass.getConstructor(
            new Class<?>[]{Configuration.class, LogManager.class, Dialect.class, MachineModel.class});
        parserConstructor.setAccessible(true);
        loadedCParser = new WeakReference<Constructor<? extends CParser>>(parserConstructor);
      }

      return parserConstructor.newInstance(config, logger, dialect, machine);
    } catch (ReflectiveOperationException e) {
      throw new Classes.UnexpectedCheckedException("Failed to create Eclipse CDT parser", e);
    }
  }

  public static Parser getJavaParser(LogManager logger, Configuration config)
      throws InvalidConfigurationException {

    try {
      Constructor<? extends Parser> parserConstructor = loadedJavaParser.get();

      if (parserConstructor == null) {
        ClassLoader classLoader = getClassLoader(logger);

        @SuppressWarnings("unchecked")
        Class<? extends CParser> parserClass =
            (Class<? extends CParser>) classLoader.loadClass(JAVA_PARSER_CLASS);
        parserConstructor =
            parserClass.getConstructor(new Class<?>[]{LogManager.class, Configuration.class});
        parserConstructor.setAccessible(true);
        loadedJavaParser = new WeakReference<Constructor<? extends Parser>>(parserConstructor);
      }

      try {
        return parserConstructor.newInstance(logger, config);
      } catch (InvocationTargetException e) {
        if (e.getCause() instanceof InvalidConfigurationException) {
          throw (InvalidConfigurationException) e.getCause();
        }
        throw e;
      }
    } catch (ReflectiveOperationException e) {
      throw new Classes.UnexpectedCheckedException("Failed to create Eclipse Java parser", e);
    }
  }
}
