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
package org.sosy_lab.cpachecker.core.interfaces.checker;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.weakness.Weakness;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * This is a manager class for checkers. You should not create checkers by directly calling their
 * constructors.
 */
@Options(prefix = "checker")
public class CheckerManager<T extends AbstractState> {

  private final Configuration config;
  private final Class<T> servedAbstractStateClass;

  private List<ExpressionChecker<T, ?>> expressionCheckers;
  private List<StateChecker<T>> stateCheckers;

  private List<ErrorReport> errorReports;

  @Option(secure = true, name = "weaknessForCheck", description = "specified weakness to be "
      + "checked")
  private Set<Weakness> weaknessForCheck = Sets.newHashSet();

  public CheckerManager(Configuration pConfig, Class<T> pClass)
      throws InvalidConfigurationException {
    config = pConfig;
    servedAbstractStateClass = pClass;
    // inject information about specified weakness
    config.inject(this);
    expressionCheckers = Lists.newArrayList();
    stateCheckers = Lists.newArrayList();
    errorReports = Lists.newArrayList();
  }

  public void register(Class<? extends GeneralChecker> checkerClass) {
    if (weaknessForCheck.isEmpty()) {
      // no need to register any checkers
      return;
    }
    Constructor<?>[] constructors = checkerClass.getDeclaredConstructors();
    if (constructors.length > 1) {
      throw new UnsupportedOperationException("Cannot automatically create the checker with more "
          + "than one constructor");
    }
    Constructor<?> constructor = constructors[0];
    constructor.setAccessible(true);
    Class<?> formalParameters[] = constructor.getParameterTypes();
    try {
      if (formalParameters.length == 1) {
        if (formalParameters[0].equals(Configuration.class)) {
          GeneralChecker checker = checkerClass.cast(constructor.newInstance(config));
          // the checker created could be discarded if certain precondition check does not pass
          if (checker instanceof ExpressionChecker) {
            ExpressionChecker singleChecker = (ExpressionChecker) checker;
            if (!weaknessForCheck.contains(singleChecker.getOrientedWeakness())) {
              return;
            }
            if (!singleChecker.getServedStateType().equals(servedAbstractStateClass)) {
              return;
            }
            expressionCheckers.add(singleChecker);
          } else if (checker instanceof StateChecker) {
            StateChecker singleChecker = (StateChecker) checker;
            if (!weaknessForCheck.contains(singleChecker.getOrientedWeakness())) {
              return;
            }
            if (!singleChecker.getServedStateType().equals(servedAbstractStateClass)) {
              return;
            }
            stateCheckers.add(singleChecker);
          }
        } else {
          throw new UnsupportedOperationException("Invalid constructor for checker: only one "
              + "Configuration object is allowed");
        }
      } else {
        throw new UnsupportedOperationException("Cannot automatically create the checker whose "
            + "constructor has more than one parameter");
      }
    } catch (InvocationTargetException e) {
      Throwable t = e.getCause();
      throw new UnexpectedCheckedException("instantiation of checker " + checkerClass
          .getSimpleName(), t);
    } catch (InstantiationException e) {
      throw new UnsupportedOperationException("Cannot instantiate checker " + checkerClass
          .toString());
    } catch (IllegalAccessException e) {
      throw new UnsupportedOperationException("Cannot instantiate checker without an "
          + "accessible constructor");
    }
  }

  /**
   * This method makes polling on all expression checkers work on abstract domain {@param
   * domainClass}.
   * NOTE: only pre-state could be refined and the updated post-states result from changing of
   * pre-state
   *
   * @param rightHand   as checkAndRefine
   * @param domainClass the class of abstract domain
   * @param cell        expression cell (including values and states)
   * @return the resultant expression cell
   */
  public <S> ExpressionCell<T, S> checkExpression(
      CRightHandSide rightHand,
      Class<S> domainClass,
      ExpressionCell<T, S> cell,
      CFAEdge cfaEdge)
      throws CPATransferException {

    ExpressionCell<T, S> testCell = cell;
    for (ExpressionChecker<T, ?> singleChecker : expressionCheckers) {
      if (!singleChecker.getAbstractDomainClass().equals(domainClass)) {
        continue;
      }
      ExpressionChecker<T, S> castedChecker = (ExpressionChecker<T, S>) singleChecker;
      testCell = castedChecker.checkAndRefine(rightHand, testCell, cfaEdge);
      if (castedChecker instanceof CheckerWithInstantErrorReport) {
        Collection<ErrorReport> newErrors = ((CheckerWithInstantErrorReport) castedChecker)
            .getErrorReport();
        errorReports.addAll(newErrors);
        ((CheckerWithInstantErrorReport) castedChecker).resetErrorReport();
      }
    }
    return testCell;

  }

  /**
   * @param assignment  the assignment to be analyzed
   * @param domainClass the class of abstract domain
   * @param cell        the assignment cell (including the value of right-hand-side and state before
   *                    assignment)
   * @return the resultant assignment cell
   */
  public <S> AssignmentCell<T, S> checkAssignment(
      CAssignment assignment,
      Class<S> domainClass,
      AssignmentCell<T, S> cell,
      CFAEdge cfaEdge)
      throws CPATransferException {

    AssignmentCell<T, S> testCell = cell;
    for (ExpressionChecker<T, ?> singleChecker : expressionCheckers) {
      if (!singleChecker.getAbstractDomainClass().equals(domainClass)) {
        continue;
      }
      ExpressionChecker<T, S> castedChecker = (ExpressionChecker<T, S>) singleChecker;
      testCell = castedChecker.checkAndRefine(assignment, testCell, cfaEdge);
      if (castedChecker instanceof CheckerWithInstantErrorReport) {
        Collection<ErrorReport> newErrors =
            ((CheckerWithInstantErrorReport) castedChecker).getErrorReport();
        errorReports.addAll(newErrors);
        ((CheckerWithInstantErrorReport) castedChecker).resetErrorReport();
      }
    }
    return testCell;
  }

  public void checkState(
      T postState,
      List<AbstractState> postOtherStates,
      CFAEdge pCFAEdge,
      Collection<T> resultStates)
      throws CPATransferException {
    resultStates.add(postState);
    for (StateChecker<T> singleChecker : stateCheckers) {
      Collection<T> totalSuccessorStates = new ArrayList<>();
      for (T onePostState : resultStates) {
        Collection<T> successorStates = new ArrayList<>();
        singleChecker.checkAndRefine(onePostState, postOtherStates, pCFAEdge, successorStates);
        totalSuccessorStates.addAll(successorStates);
        if (singleChecker instanceof CheckerWithInstantErrorReport) {
          Collection<ErrorReport> newErrors = ((CheckerWithInstantErrorReport) singleChecker)
              .getErrorReport();
          errorReports.addAll(newErrors);
          ((CheckerWithInstantErrorReport) singleChecker).resetErrorReport();
        }
      }
      resultStates.clear();
      resultStates.addAll(totalSuccessorStates);
    }
  }

  /**
   * Dump delayed errors from checker manager. This method should be invoked after all analyses
   * have finished
   *
   * @return the collection of delayed error reports
   */
  public Collection<ErrorReport> dumpErrors() {
    List<ErrorReport> newDumpedErrors = new ArrayList<>();
    for (StateChecker<T> singleChecker : stateCheckers) {
      if (singleChecker instanceof CheckerWithDelayedErrorReport) {
        newDumpedErrors.addAll(((CheckerWithDelayedErrorReport) singleChecker).getErrorReport());
      }
    }
    return newDumpedErrors;
  }

  public Collection<ErrorReport> getErrorReportInChecker() {
    return errorReports;
  }

  public void resetErrorReportInChecker() {
    errorReports.clear();
  }

}
