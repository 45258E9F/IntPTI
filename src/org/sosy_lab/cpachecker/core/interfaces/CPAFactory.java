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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.List;

/**
 * Interface for classes which know how to create an instance of one specific
 * CPA.
 *
 * Each class implementing the ConfigurableProgramAnalysis interface has to
 * provide a factory implementing this interface. The CPA class has to have a
 * public static method "factory()" which takes no arguments, returns an instance
 * of CPAFactory and never fails (that is, it never returns null or throws an
 * exception).
 *
 * If the CPA cannot be instantiated for one reason or another (e.g. because
 * not all necessary objects have been provided to the factory), this has to be
 * signaled by an exception returned from the createInstance() method. The only
 * other way in which creating a CPA may fail is when a CPA or its CPAFactory
 * don't support an optional feature (like wrapping another CPA or several of
 * them). The methods of this related to such optional features are marked with
 * an {@link UnsupportedOperationException}. A CPA which does not support a
 * feature should throw such an exception if the method is called (which might
 * not always be the case).
 *
 * All methods except the {@link #createInstance()} method return an instance
 * of CPAFactory. Implementations have to return "this" (that is the very same
 * object on which the method was called) in all cases, to support a programming
 * style like
 * {@code CPA.factory().setLogger(logger).setConfiguration(config).createInstance()}
 */
public interface CPAFactory {

  /**
   * Provides a LogManager to the CPA. If it does not need it, this method
   * should do nothing.
   *
   * @return this
   */
  public CPAFactory setLogger(LogManager logger);

  /**
   * Provides a configuration object to the CPA. If it does not need it, this
   * method should do nothing.
   *
   * @return this
   */
  public CPAFactory setConfiguration(Configuration configuration);

  /**
   * Provides a ShutdownNotifier instance to the CPA. If it does not need it, this
   * method should do nothing.
   *
   * @return this
   */
  public CPAFactory setShutdownNotifier(ShutdownNotifier shutdownNotifier);

  /**
   * Provides exactly one child to the CPA. If the CPA does not support wrapping
   * other CPAs, it should throw an {@link UnsupportedOperationException}.
   * If the CPA supports wrapping more than one CPA, it may also throw an
   * exception for this method for fail-fast behavior.
   *
   * @param child the CPA to be wrapped
   * @return this
   * @throws UnsupportedOperationException if this is no wrapper CPA
   */
  public CPAFactory setChild(ConfigurableProgramAnalysis child)
      throws UnsupportedOperationException;

  /**
   * Provides at least one child to the CPA. If the CPA does not support wrapping
   * several other CPAs, it should throw an {@link UnsupportedOperationException}.
   *
   * @param children the CPAs to be wrapped
   * @return this
   * @throws UnsupportedOperationException if this is no wrapper CPA
   */
  public CPAFactory setChildren(List<ConfigurableProgramAnalysis> children)
      throws UnsupportedOperationException;

  /**
   * Provides an object of arbitrary type to the CPA.
   *
   * @param <T>    the type of the object
   * @param object the object to be given to the CPA
   * @param cls    the class object for the type
   * @return this
   * @throws UnsupportedOperationException if this factory does not support storing objects of this
   *                                       type
   */
  public <T> CPAFactory set(T object, Class<T> cls) throws UnsupportedOperationException;

  /**
   * Returns a new instance of the CPA belonging to this CPAFactory, using the
   * objects passed to the other methods of this CPAFactory before. If this is not
   * possible, an exception (either a CPAException or a RuntimeException) may be thrown.
   * If the CPA is something it needs in order to be created (e.g. one or more
   * children), it should indicate this by a {@link IllegalStateException}.
   *
   * @return a new ConfigurableProgramAnalysis instance
   * @throws CPAException If the CPA cannot be instantiated.
   */
  public ConfigurableProgramAnalysis createInstance()
      throws InvalidConfigurationException, CPAException;
}
