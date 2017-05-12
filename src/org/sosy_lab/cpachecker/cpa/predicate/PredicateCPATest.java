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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.reflect.Invokable;

import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.util.test.LoggingClassLoader;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import java.net.URLClassLoader;
import java.util.regex.Pattern;

public class PredicateCPATest {

  private static final Pattern BDD_CLASS_PATTERN = Pattern.compile("(BDD|bdd)");

  /**
   * This tests that the BDD library is NOT loaded by PredicateCPA if it is not necessary
   * (loading the library might occupy a lot of memory).
   */
  @Test
  public void dontLoadBDDLibraryIfNotNecessary() throws Exception {
    Configuration config = TestDataTools.configurationForTest()
        .setOption("cpa.predicate.blk.alwaysAtFunctions", "false")
        .setOption("cpa.predicate.blk.alwaysAtLoops", "false")
        .build();

    FluentIterable<String> loadedClasses = loadPredicateCPA(config);
    assertThat(loadedClasses.filter(Predicates.contains(BDD_CLASS_PATTERN))).isEmpty();
  }

  /**
   * This tests that the BDD library is loaded by PredicateCPA if it is necessary
   * (if this test fails, the {@link #loadBDDLibraryIfNecessary} test
   * won't work).
   */
  @Test
  public void loadBDDLibraryIfNecessary() throws Exception {
    Configuration config = TestDataTools.configurationForTest().build();

    FluentIterable<String> loadedClasses = loadPredicateCPA(config);
    assertThat(loadedClasses.filter(Predicates.contains(BDD_CLASS_PATTERN))).isNotEmpty();
  }

  private FluentIterable<String> loadPredicateCPA(Configuration config) throws Exception {
    ClassLoader myClassLoader = PredicateCPATest.class.getClassLoader();
    assume().that(myClassLoader).isInstanceOf(URLClassLoader.class);
    LogManager logger = TestLogManager.getInstance();

    try (LoggingClassLoader cl = new LoggingClassLoader(
        Pattern.compile(
            "(org\\.sosy_lab\\.cpachecker\\..*(predicate|bdd|BDD|formulaslicing).*)|(org\\.sosy_lab\\.solver\\..*)"),
        ((URLClassLoader) myClassLoader).getURLs(), myClassLoader
    )) {
      Class<?> cpaClass =
          cl.loadClass(PredicateCPATest.class.getPackage().getName() + ".PredicateCPA");
      Invokable<?, CPAFactory> factoryMethod =
          Invokable.from(cpaClass.getDeclaredMethod("factory")).returning(CPAFactory.class);
      CPAFactory factory = factoryMethod.invoke(null);

      factory.setConfiguration(config);
      factory.setLogger(logger);
      factory.setShutdownNotifier(ShutdownNotifier.createDummy());
      factory.set(TestDataTools.makeCFA("void main() { }", config), CFA.class);
      factory.set(new ReachedSetFactory(config), ReachedSetFactory.class);

      ConfigurableProgramAnalysis cpa = factory.createInstance();
      if (cpa instanceof AutoCloseable) {
        ((AutoCloseable) cpa).close();
      }
      return from(cl.getLoadedClasses()).transform(new Function<Class<?>, String>() {
        @Override
        public String apply(Class<?> pInput) {
          return pInput.getName();
        }
      });
    }
  }
}
