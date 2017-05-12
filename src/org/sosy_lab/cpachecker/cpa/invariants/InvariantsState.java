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
package org.sosy_lab.cpachecker.cpa.invariants;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanConstant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectFormulasVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundIntervalFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Constant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ContainsVarVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ContainsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Equal;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Exclusion;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaAbstractionVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaDepthCountVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.IsLinearVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.LogicalNot;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PartialEvaluator;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PushAssumptionToEnvironmentVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ReplaceVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SplitConjunctionsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.StateEqualsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToBitvectorFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToCodeFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Union;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.VariableSelection;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.solver.api.BooleanFormulaManager;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Instances of this class represent states in the light-weight invariants analysis.
 */
public class InvariantsState implements AbstractState,
                                        ExpressionTreeReportingState, FormulaReportingState,
                                        LatticeAbstractState<InvariantsState>,
                                        AbstractQueryableState {

  private static final String PROPERTY_OVERFLOW = "overflow";

  private static final FormulaDepthCountVisitor<CompoundInterval> FORMULA_DEPTH_COUNT_VISITOR =
      new FormulaDepthCountVisitor<>();

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR =
      new CollectVarsVisitor<>();

  /**
   * A visitor used to split boolean conjunction formulas up into the conjuncted clauses
   */
  private static final SplitConjunctionsVisitor<CompoundInterval> SPLIT_CONJUNCTIONS_VISITOR =
      new SplitConjunctionsVisitor<>();

  private static final Predicate<? super MemoryLocation> IS_UNSUPPORTED_VARIABLE_NAME =
      new Predicate<MemoryLocation>() {

        @Override
        public boolean apply(MemoryLocation pMemoryLocation) {
          return pMemoryLocation == null || pMemoryLocation.getIdentifier().contains("[");
        }
      };

  private final Predicate<BooleanFormula<CompoundInterval>> implies =
      new Predicate<BooleanFormula<CompoundInterval>>() {

        @Override
        public boolean apply(BooleanFormula<CompoundInterval> pArg0) {
          return definitelyImplies(pArg0);
        }

      };

  /**
   * A visitor used to evaluate formulas as exactly as possible.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  /**
   * A visitor that, like the formula evaluation visitor, is used to evaluate formulas, but far less
   * exact to allow for convergence.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> abstractionVisitor;

  /**
   * The environment currently known to the state.
   */
  private final NonRecursiveEnvironment environment;

  /**
   * The variables selected for this analysis.
   */
  private final VariableSelection<CompoundInterval> variableSelection;

  private final PersistentSortedMap<MemoryLocation, CType> variableTypes;

  private final PartialEvaluator partialEvaluator;

  private final MachineModel machineModel;

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  private final CompoundIntervalFormulaManager compoundIntervalFormulaManager;

  private final AbstractionState abstractionState;

  private final boolean overflowDetected;

  private final boolean includeTypeInformation;

  private Iterable<BooleanFormula<CompoundInterval>> environmentAsAssumptions;

  private volatile int hash = 0;

  public InvariantsState(
      VariableSelection<CompoundInterval> pVariableSelection,
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel,
      InvariantsState pInvariant,
      AbstractionState pAbstractionState,
      boolean pIncludeTypeInformation) {
    this.environment = pInvariant.environment;
    this.partialEvaluator = pInvariant.partialEvaluator;
    this.variableSelection = pVariableSelection;
    this.variableTypes = pInvariant.variableTypes;
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.machineModel = pMachineModel;
    this.abstractionState = pAbstractionState;
    this.compoundIntervalFormulaManager =
        new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
    this.evaluationVisitor =
        new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory);
    this.abstractionVisitor = new FormulaAbstractionVisitor(compoundIntervalManagerFactory);
    this.overflowDetected = false;
    this.includeTypeInformation = pIncludeTypeInformation;
  }

  /**
   * Creates a new invariants state with a selection of
   * variables, and the machine model used.
   *
   * @param pVariableSelection      the selected variables.
   * @param pMachineModel           the machine model used.
   * @param pAbstractionState       the abstraction information.
   * @param pOverflowDetected       if an overflow has been detected.
   * @param pIncludeTypeInformation whether or not to include type information for exports.
   */
  public InvariantsState(
      VariableSelection<CompoundInterval> pVariableSelection,
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel,
      AbstractionState pAbstractionState,
      boolean pOverflowDetected,
      boolean pIncludeTypeInformation) {
    this.environment = NonRecursiveEnvironment.of(pCompoundIntervalManagerFactory);
    this.partialEvaluator = new PartialEvaluator(pCompoundIntervalManagerFactory, this.environment);
    this.variableSelection = pVariableSelection;
    this.variableTypes = PathCopyingPersistentTreeMap.of();
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.machineModel = pMachineModel;
    this.abstractionState = pAbstractionState;
    this.compoundIntervalFormulaManager =
        new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
    this.evaluationVisitor =
        new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory);
    this.abstractionVisitor = new FormulaAbstractionVisitor(compoundIntervalManagerFactory);
    this.overflowDetected = pOverflowDetected;
    this.includeTypeInformation = pIncludeTypeInformation;
  }

  /**
   * Creates a new invariants state with the given data, reusing the given
   * instance of the environment without copying.
   *
   * @param pVariableSelection      the selected variables.
   * @param pMachineModel           the machine model used.
   * @param pAbstractionState       the abstraction information.
   * @param pEnvironment            the environment. This instance is reused and not copied.
   * @param pVariableTypes          the variable types.
   * @param pOverflowDetected       if an overflow has been detected.
   * @param pIncludeTypeInformation whether or not to include type information for exports.
   */
  private InvariantsState(
      VariableSelection<CompoundInterval> pVariableSelection,
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel,
      AbstractionState pAbstractionState,
      NonRecursiveEnvironment pEnvironment,
      PersistentSortedMap<MemoryLocation, CType> pVariableTypes,
      boolean pOverflowDetected,
      boolean pIncludeTypeInformation) {
    this.environment = pEnvironment;
    this.partialEvaluator = new PartialEvaluator(pCompoundIntervalManagerFactory, this.environment);
    this.variableSelection = pVariableSelection;
    this.variableTypes = pVariableTypes;
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.machineModel = pMachineModel;
    this.abstractionState = pAbstractionState;
    this.compoundIntervalFormulaManager =
        new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
    this.evaluationVisitor =
        new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory);
    this.abstractionVisitor = new FormulaAbstractionVisitor(compoundIntervalManagerFactory);
    this.overflowDetected = pOverflowDetected;
    this.includeTypeInformation = pIncludeTypeInformation;
  }

  /**
   * Creates a new invariants state with a selection of variables, the machine
   * model used, the given variable types and the given abstraction state.
   *
   * @param pVariableSelection      the selected variables.
   * @param pMachineModel           the machine model used.
   * @param pVariableTypes          the variable types.
   * @param pAbstractionState       the abstraction state.
   * @param pOverflowDetected       if an overflow has been detected.
   * @param pIncludeTypeInformation whether or not to include type information for exports.
   */
  private InvariantsState(
      Map<MemoryLocation, NumeralFormula<CompoundInterval>> pEnvironment,
      VariableSelection<CompoundInterval> pVariableSelection,
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel,
      PersistentSortedMap<MemoryLocation, CType> pVariableTypes,
      AbstractionState pAbstractionState,
      boolean pOverflowDetected,
      boolean pIncludeTypeInformation) {
    this.environment =
        NonRecursiveEnvironment.copyOf(pCompoundIntervalManagerFactory, pEnvironment);
    this.partialEvaluator = new PartialEvaluator(pCompoundIntervalManagerFactory, pEnvironment);
    this.variableSelection = pVariableSelection;
    this.variableTypes = pVariableTypes;
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.machineModel = pMachineModel;
    this.abstractionState = pAbstractionState;
    this.compoundIntervalFormulaManager =
        new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
    this.evaluationVisitor =
        new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory);
    this.abstractionVisitor = new FormulaAbstractionVisitor(compoundIntervalManagerFactory);
    this.overflowDetected = pOverflowDetected;
    this.includeTypeInformation = pIncludeTypeInformation;
  }

  private AbstractionState determineAbstractionState(AbstractionState pMasterState) {
    AbstractionState state = pMasterState;
    if (state.getClass() == abstractionState.getClass()) {
      state = abstractionState.join(state);
    }
    return state;
  }

  public AbstractionState determineAbstractionState(InvariantsPrecision pPrecision) {
    return determineAbstractionState(
        pPrecision.getAbstractionStrategy()
            .from(abstractionState));
  }

  public InvariantsState updateAbstractionState(InvariantsPrecision pPrecision, CFAEdge pEdge) {
    AbstractionState state =
        pPrecision.getAbstractionStrategy()
            .getSuccessorState(abstractionState);
    state = state.addEnteringEdge(pEdge);
    if (state.equals(abstractionState)) {
      return this;
    }
    return new InvariantsState(environment, variableSelection, compoundIntervalManagerFactory,
        machineModel, variableTypes, state, overflowDetected, includeTypeInformation);
  }

  public Type getType(MemoryLocation pMemoryLocation) {
    return variableTypes.get(pMemoryLocation);
  }

  public InvariantsState setType(MemoryLocation pMemoryLocation, CType pType) {
    if (pType.equals(variableTypes.get(pMemoryLocation))) {
      return this;
    }
    return new InvariantsState(variableSelection, compoundIntervalManagerFactory, machineModel,
        abstractionState, environment, variableTypes.putAndCopy(pMemoryLocation, pType),
        overflowDetected, includeTypeInformation);
  }

  public InvariantsState setTypes(Map<MemoryLocation, CType> pVarTypes) {
    boolean allContained = true;
    for (Map.Entry<MemoryLocation, CType> entry : pVarTypes.entrySet()) {
      if (!entry.getValue().equals(variableTypes.get(entry.getKey()))) {
        allContained = false;
        break;
      }
    }
    if (allContained) {
      return this;
    }
    PersistentSortedMap<MemoryLocation, CType> variableTypes = this.variableTypes;
    for (Map.Entry<MemoryLocation, CType> entry : pVarTypes.entrySet()) {
      MemoryLocation memoryLocation = entry.getKey();
      if (!entry.getValue().equals(variableTypes.get(memoryLocation))) {
        variableTypes = variableTypes.putAndCopy(memoryLocation, entry.getValue());
      }
    }
    return new InvariantsState(variableSelection, compoundIntervalManagerFactory, machineModel,
        abstractionState, environment, variableTypes, overflowDetected, includeTypeInformation);
  }

  public InvariantsState assignArray(
      MemoryLocation pArray,
      NumeralFormula<CompoundInterval> pSubscript,
      NumeralFormula<CompoundInterval> pValue) {
    FormulaEvaluationVisitor<CompoundInterval> fev = getFormulaResolver();
    CompoundInterval value = pSubscript.accept(fev, this.environment);
    if (value.isSingleton()) { // Exact subscript value is known
      return assignInternal(
          MemoryLocation.valueOf(pArray.getAsSimpleString() + "[" + value.getValue() + "]"),
          pValue);
    } else { // Multiple subscript values are possible: All possible subscript targets are now unknown
      InvariantsState result = this;
      for (MemoryLocation memoryLocation : this.environment.keySet()) {
        String prefix = pArray.getAsSimpleString() + "[";
        if (memoryLocation.getAsSimpleString().startsWith(prefix)) {
          String subscriptValueStr =
              memoryLocation.getAsSimpleString().replace(prefix, "").replaceAll("].*", "");
          if (subscriptValueStr.equals("*") || value.contains(new BigInteger(subscriptValueStr))) {
            result = result.assignInternal(memoryLocation,
                allPossibleValuesFormula(pValue.getBitVectorInfo()));
          }
        }
      }
      return result;
    }
  }

  private CompoundIntervalManager getCompoundIntervalManager(BitVectorInfo pBitVectorInfo) {
    return compoundIntervalManagerFactory.createCompoundIntervalManager(pBitVectorInfo);
  }

  private CompoundInterval allPossibleValues(BitVectorInfo pBitVectorInfo) {
    return getCompoundIntervalManager(pBitVectorInfo).allPossibleValues();
  }

  private NumeralFormula<CompoundInterval> allPossibleValuesFormula(BitVectorInfo pBitVectorInfo) {
    return InvariantsFormulaManager.INSTANCE.asConstant(
        pBitVectorInfo,
        allPossibleValues(pBitVectorInfo));
  }

  public InvariantsState assign(
      MemoryLocation pMemoryLocation,
      NumeralFormula<CompoundInterval> pValue) {
    InvariantsState result = this;
    Type variableType = variableTypes.get(pMemoryLocation);
    if (variableType == null) {
      return this;
    }
    BitVectorInfo bitVectorInfo = BitVectorInfo.from(machineModel, variableType);
    NumeralFormula<CompoundInterval> value =
        compoundIntervalFormulaManager.cast(bitVectorInfo, pValue);
    for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry : this.environment
        .entrySet()) {
      MemoryLocation memoryLocation = entry.getKey();
      BitVectorInfo varBitVectorInfo = BitVectorInfo.from(machineModel, getType(memoryLocation));
      if (memoryLocation.getAsSimpleString().startsWith(pMemoryLocation.getAsSimpleString() + "->")
          || memoryLocation.getAsSimpleString()
          .startsWith(pMemoryLocation.getAsSimpleString() + ".")) {
        result = result.assign(memoryLocation, allPossibleValuesFormula(varBitVectorInfo));
      }
    }
    if (value instanceof Variable<?>) {
      MemoryLocation valueMemoryLocation = ((Variable<?>) value).getMemoryLocation();
      if (valueMemoryLocation.getAsSimpleString()
          .startsWith(pMemoryLocation.getAsSimpleString() + "->")
          || valueMemoryLocation.getAsSimpleString()
          .startsWith(pMemoryLocation.getAsSimpleString() + ".")) {
        return assign(pMemoryLocation, allPossibleValuesFormula(bitVectorInfo));
      }
      String pointerDerefPrefix = valueMemoryLocation.getAsSimpleString() + "->";
      String nonPointerDerefPrefix = valueMemoryLocation.getAsSimpleString() + ".";
      for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry : this.environment
          .entrySet()) {
        final String suffix;
        if (entry.getKey().getAsSimpleString().startsWith(pointerDerefPrefix)) {
          suffix = entry.getKey().getAsSimpleString().substring(pointerDerefPrefix.length());
        } else if (entry.getKey().getAsSimpleString().startsWith(nonPointerDerefPrefix)) {
          suffix = entry.getKey().getAsSimpleString().substring(nonPointerDerefPrefix.length());
        } else {
          suffix = null;
        }
        if (suffix != null) {
          MemoryLocation memoryLocation =
              MemoryLocation.valueOf(pMemoryLocation.getAsSimpleString() + "->" + suffix);
          NumeralFormula<CompoundInterval> previous = this.environment.get(memoryLocation);
          if (previous != null) {
            result = result.assign(
                memoryLocation,
                InvariantsFormulaManager.INSTANCE.<CompoundInterval>asVariable(
                    previous.getBitVectorInfo(),
                    entry.getKey()));
          }
        }
      }
      return result.assignInternal(pMemoryLocation, value);
    }
    return result.assignInternal(pMemoryLocation, value);
  }

  /**
   * Creates a new state representing the given assignment applied to the current state.
   *
   * @param pMemoryLocation the memory location of the variable being assigned.
   * @param pValue          the new value of the variable.
   * @return a new state representing the given assignment applied to the current state.
   */
  private InvariantsState assignInternal(
      MemoryLocation pMemoryLocation,
      NumeralFormula<CompoundInterval> pValue) {
    Preconditions.checkNotNull(pValue);
    // Only use information from supported variables
    if (IS_UNSUPPORTED_VARIABLE_NAME.apply(pMemoryLocation)) {
      return this;
    }
    if (FluentIterable.from(pValue.accept(COLLECT_VARS_VISITOR))
        .anyMatch(IS_UNSUPPORTED_VARIABLE_NAME)) {
      return assignInternal(pMemoryLocation, allPossibleValuesFormula(pValue.getBitVectorInfo()));
    }

    // Check if the assigned variable is selected (newVariableSelection != null)
    VariableSelection<CompoundInterval> newVariableSelection =
        this.variableSelection.acceptAssignment(pMemoryLocation, pValue);
    if (newVariableSelection == null) {
      // Ensure that no information about the irrelevant assigned variable is retained
      NonRecursiveEnvironment newEnvironment = this.environment;
      if (this.environment.containsKey(pMemoryLocation)) {
        newEnvironment = newEnvironment.removeAndCopy(pMemoryLocation);
      }
      if (this.environment == newEnvironment) {
        return this;
      }
      return new InvariantsState(newEnvironment,
          variableSelection,
          compoundIntervalManagerFactory,
          machineModel,
          variableTypes,
          abstractionState,
          overflowDetected,
          includeTypeInformation);
    }

    BitVectorInfo bitVectorInfo = pValue.getBitVectorInfo();
    Variable<CompoundInterval> variable = InvariantsFormulaManager.INSTANCE.asVariable(
        bitVectorInfo,
        pMemoryLocation);

    // Optimization: If the value being assigned is equivalent to the value already stored, do nothing
    if (getEnvironmentValue(bitVectorInfo, pMemoryLocation).equals(pValue)
        && (pValue instanceof Variable<?>
        || pValue instanceof Constant<?> && ((Constant<CompoundInterval>) pValue).getValue()
        .isSingleton())
        || variable.accept(new StateEqualsVisitor(getFormulaResolver(), this.environment,
        compoundIntervalManagerFactory), pValue)) {
      return this;
    }

    // Compute the assignment
    InvariantsState result =
        assignInternal(pMemoryLocation, pValue, newVariableSelection, evaluationVisitor);

    if (equals(result)) {
      return this;
    }
    return result;
  }

  private InvariantsState assignInternal(
      final MemoryLocation pMemoryLocation, NumeralFormula<CompoundInterval> pValue,
      VariableSelection<CompoundInterval> newVariableSelection,
      FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor) {
    NonRecursiveEnvironment resultEnvironment = this.environment;

    ContainsVarVisitor<CompoundInterval> containsVarVisitor = new ContainsVarVisitor<>();

    /*
     * A variable is newly assigned, so the appearances of this variable
     * in any previously collected assumptions (including its new value)
     * have to be resolved with the variable's previous value.
     */
    BitVectorInfo bitVectorInfo = pValue.getBitVectorInfo();
    Variable<CompoundInterval> variable = InvariantsFormulaManager.INSTANCE.asVariable(
        bitVectorInfo,
        pMemoryLocation);
    NumeralFormula<CompoundInterval> previousValue =
        getEnvironmentValue(bitVectorInfo, pMemoryLocation);
    ReplaceVisitor<CompoundInterval> replaceVisitor = new ReplaceVisitor<>(variable, previousValue);
    resultEnvironment = resultEnvironment.putAndCopy(pMemoryLocation,
        pValue.accept(replaceVisitor).accept(partialEvaluator, evaluationVisitor));
    if (pValue.accept(new IsLinearVisitor<CompoundInterval>(), variable) && pValue
        .accept(containsVarVisitor, pMemoryLocation)) {
      CompoundInterval zero =
          compoundIntervalManagerFactory.createCompoundIntervalManager(bitVectorInfo).singleton(0);
      previousValue = pValue.accept(new ReplaceVisitor<>(variable,
          InvariantsFormulaManager.INSTANCE.asConstant(bitVectorInfo, zero)));
      previousValue = compoundIntervalFormulaManager.subtract(variable, previousValue);
    }
    replaceVisitor = new ReplaceVisitor<>(variable, previousValue);

    for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> environmentEntry : this.environment
        .entrySet()) {
      if (!environmentEntry.getKey().equals(pMemoryLocation)) {
        NumeralFormula<CompoundInterval> prevEnvValue = environmentEntry.getValue();
        if (prevEnvValue.accept(containsVarVisitor, pMemoryLocation)) {
          NumeralFormula<CompoundInterval> newEnvValue =
              prevEnvValue.accept(replaceVisitor).accept(partialEvaluator, evaluationVisitor);
          resultEnvironment = resultEnvironment.putAndCopy(environmentEntry.getKey(), newEnvValue);
        }
      }
    }
    return new InvariantsState(newVariableSelection, compoundIntervalManagerFactory, machineModel,
        abstractionState, resultEnvironment, variableTypes, overflowDetected,
        includeTypeInformation);
  }

  /**
   * Gets a state that has no information about the program and the same
   * information about the analysis as this state.
   *
   * @return a state that has no information about the program and the same information about the
   * analysis as this state.
   */
  public InvariantsState clear() {
    if (environment.isEmpty()) {
      return this;
    }
    return new InvariantsState(variableSelection, compoundIntervalManagerFactory, machineModel,
        abstractionState, overflowDetected, includeTypeInformation);
  }

  /**
   * Removes the value stored for the given variable.
   *
   * @param pMemoryLocation the variable to remove.
   * @return the new state.
   */
  public InvariantsState clear(MemoryLocation pMemoryLocation) {
    NumeralFormula<CompoundInterval> previous = environment.get(pMemoryLocation);
    final BitVectorInfo bitVectorInfo;

    if (previous == null) {
      Type type = variableTypes.get(pMemoryLocation);
      if (type == null) {
        return this;
      }
      bitVectorInfo = BitVectorInfo.from(machineModel, type);
    } else {
      bitVectorInfo = previous.getBitVectorInfo();
    }

    NumeralFormula<CompoundInterval> allPossibleValues = allPossibleValuesFormula(bitVectorInfo);
    InvariantsState result =
        assignInternal(pMemoryLocation, allPossibleValues, variableSelection, evaluationVisitor);
    NonRecursiveEnvironment resultEnvironment = result.environment.removeAndCopy(pMemoryLocation);
    result = new InvariantsState(
        result.variableSelection,
        result.compoundIntervalManagerFactory,
        result.machineModel,
        result.abstractionState,
        resultEnvironment,
        result.variableTypes,
        overflowDetected,
        includeTypeInformation);
    if (equals(result)) {
      return this;
    }
    return result;
  }

  public InvariantsState clearAll(Predicate<MemoryLocation> pMemoryLocationPredicate) {
    final Set<Variable<CompoundInterval>> toClear = getVariables(pMemoryLocationPredicate);
    ContainsVisitor<CompoundInterval> containsVisitor = new ContainsVisitor<>();
    ContainsVarVisitor<CompoundInterval> containsVarVisitor = new ContainsVarVisitor<>();
    Predicate<NumeralFormula<CompoundInterval>> toClearPredicate =
        new Predicate<NumeralFormula<CompoundInterval>>() {

          @Override
          public boolean apply(NumeralFormula<CompoundInterval> pFormula) {
            return toClear.contains(pFormula);
          }

        };
    Queue<MemoryLocation> potentialReferrers = new ArrayDeque<>();
    for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry : environment
        .entrySet()) {
      if (entry.getValue().accept(containsVisitor, toClearPredicate)) {
        potentialReferrers.add(entry.getKey());
      }
    }

    NonRecursiveEnvironment resultEnvironment = environment;

    Iterator<Variable<CompoundInterval>> toClearIterator = toClear.iterator();
    while (toClearIterator.hasNext()) {
      Variable<CompoundInterval> variable = toClearIterator.next();
      MemoryLocation memoryLocation = variable.getMemoryLocation();
      NumeralFormula<CompoundInterval> previous = resultEnvironment.get(memoryLocation);
      final BitVectorInfo bitVectorInfo;

      if (previous == null) {
        Type type = variableTypes.get(memoryLocation);
        if (type == null) {
          continue;
        }
        bitVectorInfo = BitVectorInfo.from(machineModel, type);
      } else {
        bitVectorInfo = previous.getBitVectorInfo();
      }

      NumeralFormula<CompoundInterval> allPossibleValues = allPossibleValuesFormula(bitVectorInfo);
      ReplaceVisitor<CompoundInterval> replaceVisitor = new ReplaceVisitor<>(
          variable,
          previous == null ? allPossibleValues : previous);

      Iterator<MemoryLocation> potentialReferrerIterator = potentialReferrers.iterator();
      while (potentialReferrerIterator.hasNext()) {
        MemoryLocation key = potentialReferrerIterator.next();
        if (key.equals(memoryLocation)) {
          potentialReferrerIterator.remove();
        } else {
          NumeralFormula<CompoundInterval> previousValue = resultEnvironment.get(key);
          if (previousValue.accept(containsVarVisitor, memoryLocation)) {
            NumeralFormula<CompoundInterval> newEnvValue =
                previousValue.accept(replaceVisitor).accept(partialEvaluator, evaluationVisitor);
            resultEnvironment = resultEnvironment.putAndCopy(key, newEnvValue);
            if (!newEnvValue.accept(containsVisitor, toClearPredicate)) {
              potentialReferrerIterator.remove();
            }
          }
        }
      }
      resultEnvironment = resultEnvironment.removeAndCopy(memoryLocation);
      toClearIterator.remove();
    }

    InvariantsState result = new InvariantsState(
        variableSelection,
        compoundIntervalManagerFactory,
        machineModel,
        abstractionState,
        resultEnvironment,
        variableTypes,
        overflowDetected,
        includeTypeInformation);
    if (equals(result)) {
      return this;
    }
    return result;
  }

  /**
   * Gets the environment as a set equations of the variables with their values.
   *
   * @return the environment as a set equations of the variables with their values.
   */
  public Iterable<BooleanFormula<CompoundInterval>> getEnvironmentAsAssumptions() {
    if (this.environmentAsAssumptions == null) {
      environmentAsAssumptions =
          getEnvironmentAsAssumptions(compoundIntervalManagerFactory, environment);
    }
    return environmentAsAssumptions;
  }

  private Iterable<BooleanFormula<CompoundInterval>> getTypeInformationAsAssumptions() {
    List<BooleanFormula<CompoundInterval>> assumptions = new ArrayList<>();
    for (Map.Entry<? extends MemoryLocation, ? extends Type> typeEntry : variableTypes.entrySet()) {
      MemoryLocation memoryLocation = typeEntry.getKey();
      Type type = typeEntry.getValue();
      if (BitVectorInfo.isSupported(type)) {
        BitVectorInfo bitVectorInfo = BitVectorInfo.from(machineModel, typeEntry.getValue());
        CompoundIntervalManager cim =
            compoundIntervalManagerFactory.createCompoundIntervalManager(bitVectorInfo);
        CompoundInterval range = cim.allPossibleValues();
        Variable<CompoundInterval> variable = InvariantsFormulaManager.INSTANCE.asVariable(
            bitVectorInfo,
            memoryLocation);
        NumeralFormula<CompoundInterval> value = environment.get(memoryLocation);
        if (value == null || value.accept(evaluationVisitor, environment)
            .containsAllPossibleValues()) {
          if (range.hasLowerBound()) {
            assumptions.add(compoundIntervalFormulaManager.greaterThanOrEqual(
                variable,
                InvariantsFormulaManager.INSTANCE
                    .asConstant(bitVectorInfo, cim.singleton(range.getLowerBound()))));
          }
          if (range.hasUpperBound()) {
            assumptions.add(compoundIntervalFormulaManager.lessThanOrEqual(
                variable,
                InvariantsFormulaManager.INSTANCE
                    .asConstant(bitVectorInfo, cim.singleton(range.getUpperBound()))));
          }
        }
      }
    }
    return assumptions;
  }

  private static Iterable<BooleanFormula<CompoundInterval>> getEnvironmentAsAssumptions(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {

    CompoundIntervalFormulaManager compoundIntervalFormulaManager =
        new CompoundIntervalFormulaManager(pCompoundIntervalManagerFactory);

    Set<BooleanFormula<CompoundInterval>> environmentalAssumptions = new HashSet<>();

    List<NumeralFormula<CompoundInterval>> atomic = new ArrayList<>(1);
    Deque<NumeralFormula<CompoundInterval>> toCheck = new ArrayDeque<>(1);
    for (Entry<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> entry : pEnvironment
        .entrySet()) {
      NumeralFormula<CompoundInterval> variable =
          InvariantsFormulaManager.INSTANCE.asVariable(
              entry.getValue().getBitVectorInfo(),
              entry.getKey());

      NumeralFormula<CompoundInterval> value = entry.getValue();

      boolean isExclusion = false;
      if (value instanceof Exclusion) {
        isExclusion = true;
        value = ((Exclusion<CompoundInterval>) value).getExcluded();
      }

      atomic.clear();
      toCheck.clear();

      toCheck.add(value);
      while (!toCheck.isEmpty()) {
        NumeralFormula<CompoundInterval> current = toCheck.poll();
        if (current instanceof Union<?>) {
          Union<CompoundInterval> union = (Union<CompoundInterval>) current;
          toCheck.add(union.getOperand1());
          toCheck.add(union.getOperand2());
        } else {
          atomic.add(current);
        }
      }
      assert !atomic.isEmpty();
      Iterator<NumeralFormula<CompoundInterval>> iterator = atomic.iterator();
      BooleanFormula<CompoundInterval> assumption = null;
      while (iterator.hasNext()) {
        BooleanFormula<CompoundInterval> equation =
            compoundIntervalFormulaManager.equal(variable, iterator.next());
        if (isExclusion) {
          equation = compoundIntervalFormulaManager.logicalNot(equation);
        }
        assumption = assumption == null
                     ? equation
                     : compoundIntervalFormulaManager.logicalOr(assumption, equation);
      }
      if (assumption != null) {
        environmentalAssumptions.add(assumption);
      }
    }
    return environmentalAssumptions;
  }

  /**
   * Gets the value of the variable with the given memory location from the environment.
   *
   * @param pBitVectorInfo  the bit vector information of the variable.
   * @param pMemoryLocation the memory location of the variable.
   * @return the value of the variable with the given memory location from the environment.
   */
  private NumeralFormula<CompoundInterval> getEnvironmentValue(
      BitVectorInfo pBitVectorInfo,
      MemoryLocation pMemoryLocation) {
    NumeralFormula<CompoundInterval> environmentValue = this.environment.get(pMemoryLocation);
    if (environmentValue == null) {
      return allPossibleValuesFormula(pBitVectorInfo);
    }
    return environmentValue;
  }

  /**
   * Gets an exact formula evaluation visitor.
   *
   * @return an exact formula evaluation visitor.
   */
  public FormulaEvaluationVisitor<CompoundInterval> getFormulaResolver() {
    return evaluationVisitor;
  }

  /**
   * Makes the given assumptions for this state and checks if this state is still valid.
   *
   * @param pAssumptions          the assumptions to be made.
   * @param pEvaluationVisitor    the evaluation visitor to use for evaluating the assumptions'
   *                              correctness.
   * @param pNewVariableSelection the new variable selection
   * @return <code>true</code> if the state is still valid after the assumptions are made,
   * <code>false</code> otherwise.
   */
  private InvariantsState assumeInternal(
      Collection<? extends BooleanFormula<CompoundInterval>> pAssumptions,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor,
      VariableSelection<CompoundInterval> pNewVariableSelection) {
    InvariantsState result = this;
    for (BooleanFormula<CompoundInterval> assumption : pAssumptions) {
      result = assumeInternal(assumption, pEvaluationVisitor, pNewVariableSelection);
      if (result == null) {
        return null;
      }
    }
    return result;
  }

  /**
   * Makes the given assumption for this state and checks if this state is still valid.
   *
   * @param pAssumption           the assumption to be made.
   * @param pEvaluationVisitor    the evaluation visitor to use for evaluating the assumptions'
   *                              correctness.
   * @param pNewVariableSelection the new variable selection
   * @return <code>true</code> if the state is still valid after the assumptions are made,
   * <code>false</code> otherwise.
   */
  private InvariantsState assumeInternal(
      BooleanFormula<CompoundInterval> pAssumption,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor,
      VariableSelection<CompoundInterval> pNewVariableSelection) {
    BooleanFormula<CompoundInterval> assumption =
        pAssumption.accept(this.partialEvaluator, pEvaluationVisitor);
    // If there are multiple assumptions combined with &&, split them up
    List<BooleanFormula<CompoundInterval>> assumptionParts =
        assumption.accept(SPLIT_CONJUNCTIONS_VISITOR);
    if (assumptionParts.size() > 1) {
      return assumeInternal(assumptionParts, pEvaluationVisitor, pNewVariableSelection);
    }

    if (assumption instanceof BooleanConstant) {
      return BooleanConstant.isTrue(assumption) ? this : null;
    }

    // Only use information from supported variables
    if (FluentIterable.from(assumption.accept(COLLECT_VARS_VISITOR))
        .anyMatch(IS_UNSUPPORTED_VARIABLE_NAME)) {
      return this;
    }

    BooleanConstant<CompoundInterval> assumptionEvaluation =
        assumption.accept(pEvaluationVisitor, getEnvironment());
    // If the invariant evaluates to false or is bottom, it represents an invalid state
    if (BooleanConstant.isFalse(assumptionEvaluation)) {
      return null;
    }
    // If the invariant evaluates to true, it adds no value for now
    if (BooleanConstant.isTrue(assumptionEvaluation)) {
      return this;
    }

    NonRecursiveEnvironment.Builder environmentBuilder =
        new NonRecursiveEnvironment.Builder(this.environment);
    PushAssumptionToEnvironmentVisitor patev =
        new PushAssumptionToEnvironmentVisitor(compoundIntervalManagerFactory, pEvaluationVisitor,
            environmentBuilder);
    if (!assumption.accept(patev, BooleanConstant.<CompoundInterval>getTrue())) {
      assert !BooleanConstant.isTrue(assumptionEvaluation);
      return null;
    }
    // Check all the assumption once more after the environment changed
    if (isDefinitelyFalse(assumption, pEvaluationVisitor)) {
      return null;
    }
    return new InvariantsState(environmentBuilder.build(), pNewVariableSelection,
        compoundIntervalManagerFactory, machineModel, variableTypes, abstractionState,
        overflowDetected, includeTypeInformation);
  }

  /**
   * Checks if the given assumption is definitely false for this state.
   *
   * @param pAssumption        the assumption to evaluate.
   * @param pEvaluationVisitor the evaluation visitor used to evaluate the assumption within this
   *                           state's environment.
   * @return <code>true</code> if the given assumption does definitely not hold for this state's
   * environment, <code>false</code> if it might.
   */
  private boolean isDefinitelyFalse(
      BooleanFormula<CompoundInterval> pAssumption,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    return BooleanConstant.isFalse(pAssumption.accept(pEvaluationVisitor, getEnvironment()));
  }

  public InvariantsState assume(BooleanFormula<CompoundInterval> pAssumption) {
    // Check if at least one of the involved variables is selected (newVariableSelection != null)
    VariableSelection<CompoundInterval> newVariableSelection =
        this.variableSelection.acceptAssumption(pAssumption);
    if (newVariableSelection == null) {
      return this;
    }
    FormulaEvaluationVisitor<CompoundInterval> evaluator = getFormulaResolver();
    BooleanFormula<CompoundInterval> assumption =
        pAssumption.accept(this.partialEvaluator, evaluator);
    if (assumption instanceof BooleanConstant) {
      // An assumption evaluating to false represents an unreachable state; it can never be fulfilled
      if (BooleanConstant.isFalse(assumption)) {
        return null;
      }
      // An assumption representing nothing more than "true" or "maybe true" adds no information
      return this;
    }

    InvariantsState result = assumeInternal(assumption, evaluator, newVariableSelection);
    if (equalsState(result)) {
      return this;
    }
    return result;
  }

  @Override
  public org.sosy_lab.solver.api.BooleanFormula getFormulaApproximation(
      FormulaManagerView pManager,
      PathFormulaManager pfmgr) {

    BooleanFormulaManager bfmgr = pManager.getBooleanFormulaManager();
    org.sosy_lab.solver.api.BooleanFormula result = bfmgr.makeBoolean(true);
    FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor = getFormulaResolver();
    ToBitvectorFormulaVisitor toBooleanFormulaVisitor =
        new ToBitvectorFormulaVisitor(pManager, evaluationVisitor);

    for (BooleanFormula<CompoundInterval> assumption : getApproximationFormulas()) {
      org.sosy_lab.solver.api.BooleanFormula assumptionFormula =
          assumption.accept(toBooleanFormulaVisitor, getEnvironment());
      if (assumptionFormula != null) {
        result = bfmgr.and(result, assumptionFormula);
      }
    }
    return result;
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximation(
      final FunctionEntryNode pFunctionEntryNode, final CFANode pReferenceNode) {
    final Predicate<MemoryLocation> isExportable = new Predicate<MemoryLocation>() {

      @Override
      public boolean apply(MemoryLocation pMemoryLocation) {
        if (pMemoryLocation
            .getIdentifier()
            .startsWith("__CPAchecker_TMP_")) {
          return false;
        }
        if (pFunctionEntryNode.getReturnVariable().isPresent()
            && pMemoryLocation.isOnFunctionStack()
            && pMemoryLocation
            .getIdentifier()
            .equals(
                pFunctionEntryNode
                    .getReturnVariable()
                    .get()
                    .getName())) {
          return false;
        }
        if (!isExportable(pMemoryLocation)) {
          return false;
        }
        String functionName = pFunctionEntryNode.getFunctionName();
        return !pMemoryLocation.isOnFunctionStack()
            || pMemoryLocation.getFunctionName().equals(functionName);
      }

    };
    final Predicate<MemoryLocation> isPointerOrArray = new Predicate<MemoryLocation>() {

      @Override
      public boolean apply(MemoryLocation pMemoryLocation) {
        Type type = getType(pMemoryLocation);
        if (type instanceof CPointerType) {
          return true;
        }
        if (type instanceof CArrayType) {
          return true;
        }
        return false;
      }

    };
    final Predicate<MemoryLocation> isValidMemLoc =
        Predicates.and(isExportable, Predicates.not(isPointerOrArray));
    Predicate<NumeralFormula<CompoundInterval>> isInvalidVar =
        new Predicate<NumeralFormula<CompoundInterval>>() {

          @Override
          public boolean apply(NumeralFormula<CompoundInterval> pFormula) {
            if (pFormula instanceof Variable) {
              return !isValidMemLoc.apply(((Variable<?>) pFormula).getMemoryLocation());
            }
            return FluentIterable.from(pFormula.accept(COLLECT_VARS_VISITOR))
                .anyMatch(isPointerOrArray);
          }

        };
    ReplaceVisitor<CompoundInterval> evaluateInvalidVars =
        getInvalidReplacementVisitor(isInvalidVar);
    Function<BooleanFormula<CompoundInterval>, BooleanFormula<CompoundInterval>> replaceInvalid =
        getInvalidReplacer(isInvalidVar, evaluateInvalidVars);
    Function<BooleanFormula<CompoundInterval>, ExpressionTree<Object>> toCode =
        new Function<BooleanFormula<CompoundInterval>, ExpressionTree<Object>>() {

          @Override
          public ExpressionTree<Object> apply(BooleanFormula<CompoundInterval> pFormula) {
            ExpressionTree<String> asCode = pFormula
                .accept(new ToCodeFormulaVisitor(evaluationVisitor, machineModel),
                    getEnvironment());
            return ExpressionTrees.cast(asCode);
          }
        };
    ExpressionTree<Object> result = And.of(
        getApproximationFormulas()
            .transform(replaceInvalid)
            .filter(
                new Predicate<BooleanFormula<CompoundInterval>>() {

                  @Override
                  public boolean apply(BooleanFormula<CompoundInterval> pFormula) {
                    if (pFormula.equals(BooleanConstant.getTrue())) {
                      return false;
                    }
                    Set<MemoryLocation> memLocs =
                        pFormula.accept(new CollectVarsVisitor<CompoundInterval>());
                    if (memLocs.isEmpty()) {
                      return false;
                    }
                    return FluentIterable.from(memLocs).allMatch(isValidMemLoc);
                  }
                })
            .transform(toCode).filter(Predicates.notNull()));

    final Set<MemoryLocation> safePointers = Sets.newHashSet();
    isInvalidVar = new Predicate<NumeralFormula<CompoundInterval>>() {

      @Override
      public boolean apply(NumeralFormula<CompoundInterval> pFormula) {
        if (pFormula instanceof Variable) {
          return !isExportable.apply(((Variable<?>) pFormula).getMemoryLocation());
        }
        return false;
      }

    };
    isInvalidVar = Predicates.or(isInvalidVar, new Predicate<NumeralFormula<CompoundInterval>>() {

      @Override
      public boolean apply(NumeralFormula<CompoundInterval> pFormula) {
        if (pFormula instanceof Variable) {
          return !safePointers.contains(((Variable<?>) pFormula).getMemoryLocation());
        }
        return !FluentIterable.from(pFormula.accept(COLLECT_VARS_VISITOR))
            .anyMatch(isPointerOrArray);
      }

    });
    evaluateInvalidVars = getInvalidReplacementVisitor(isInvalidVar);
    replaceInvalid = getInvalidReplacer(isInvalidVar, evaluateInvalidVars);

    for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry : environment
        .entrySet()) {
      MemoryLocation memoryLocation = entry.getKey();
      CType type = variableTypes.get(memoryLocation);
      if (!(type instanceof CPointerType)) {
        continue;
      }
      if (!isExportable.apply(memoryLocation)) {
        continue;
      }
      NumeralFormula<CompoundInterval> value = entry.getValue();
      Predicate<NumeralFormula<CompoundInterval>> isNonSingletonConstant =
          new Predicate<NumeralFormula<CompoundInterval>>() {

            @Override
            public boolean apply(NumeralFormula<CompoundInterval> pFormula) {
              if (pFormula instanceof Constant) {
                return !((Constant<CompoundInterval>) pFormula).getValue().isSingleton();
              }
              return false;
            }

          };
      ContainsVisitor<CompoundInterval> containsVisitor = new ContainsVisitor<>();
      if (value.accept(containsVisitor, isNonSingletonConstant)) {
        continue;
      }
      NumeralFormula<CompoundInterval> var =
          InvariantsFormulaManager.INSTANCE.asVariable(value.getBitVectorInfo(), memoryLocation);
      safePointers.add(memoryLocation);
      for (MemoryLocation otherSafePointer : safePointers) {
        if (otherSafePointer == memoryLocation) {
          continue;
        }
        CType otherType = variableTypes.get(otherSafePointer);
        if (!type.equals(otherType)) {
          continue;
        }
        NumeralFormula<CompoundInterval> otherValue = environment.get(otherSafePointer);
        NumeralFormula<CompoundInterval> otherVar = InvariantsFormulaManager.INSTANCE
            .asVariable(otherValue.getBitVectorInfo(), otherSafePointer);
        BooleanFormula<CompoundInterval> equality =
            InvariantsFormulaManager.INSTANCE.equal(otherVar, var);
        if (definitelyImplies(equality)) {
          ExpressionTree<Object> code = toCode.apply(replaceInvalid.apply(equality));
          if (code != null) {
            result = And.of(result, code);
          }
        }
      }
    }
    return result;
  }

  private ReplaceVisitor<CompoundInterval> getInvalidReplacementVisitor(
      final Predicate<NumeralFormula<CompoundInterval>> isInvalidVar) {
    return new ReplaceVisitor<>(
        isInvalidVar,
        new Function<NumeralFormula<CompoundInterval>, NumeralFormula<CompoundInterval>>() {

          @Override
          public NumeralFormula<CompoundInterval> apply(NumeralFormula<CompoundInterval> pFormula) {
            return replaceOrEvaluateInvalid(pFormula, isInvalidVar);
          }

        });
  }

  private Function<BooleanFormula<CompoundInterval>, BooleanFormula<CompoundInterval>> getInvalidReplacer(
      final Predicate<NumeralFormula<CompoundInterval>> pIsInvalid,
      final ReplaceVisitor<CompoundInterval> pEvaluateInvalidVars) {
    return new Function<BooleanFormula<CompoundInterval>, BooleanFormula<CompoundInterval>>() {

      @Override
      public BooleanFormula<CompoundInterval> apply(BooleanFormula<CompoundInterval> pFormula) {
        if (pFormula instanceof Equal) {
          Equal<CompoundInterval> eq = (Equal<CompoundInterval>) pFormula;
          NumeralFormula<CompoundInterval> op1 = eq.getOperand1().accept(pEvaluateInvalidVars);
          final Set<MemoryLocation> op1Vars = op1.accept(COLLECT_VARS_VISITOR);
          final Predicate<NumeralFormula<CompoundInterval>> isInvalid =
              Predicates.or(pIsInvalid, new Predicate<NumeralFormula<CompoundInterval>>() {

                @Override
                public boolean apply(NumeralFormula<CompoundInterval> pFormula) {
                  return !Sets.intersection(op1Vars, pFormula.accept(COLLECT_VARS_VISITOR))
                      .isEmpty();
                }

              });
          ReplaceVisitor<CompoundInterval> evaluateInvalid = new ReplaceVisitor<>(
              isInvalid,
              new Function<NumeralFormula<CompoundInterval>, NumeralFormula<CompoundInterval>>() {

                @Override
                public NumeralFormula<CompoundInterval> apply(NumeralFormula<CompoundInterval> pFormula) {
                  return replaceOrEvaluateInvalid(pFormula, isInvalid);
                }

              });
          NumeralFormula<CompoundInterval> op2 = eq.getOperand2().accept(evaluateInvalid);
          return InvariantsFormulaManager.INSTANCE.equal(op1, op2);
        }
        if (pFormula instanceof LogicalNot) {
          return InvariantsFormulaManager.INSTANCE
              .logicalNot(apply(((LogicalNot<CompoundInterval>) pFormula).getNegated()));
        }
        return pFormula.accept(pEvaluateInvalidVars);
      }

    };
  }

  private NumeralFormula<CompoundInterval> replaceOrEvaluateInvalid(
      NumeralFormula<CompoundInterval> pFormula,
      final Predicate<NumeralFormula<CompoundInterval>> pIsInvalid) {
    if (!pIsInvalid.apply(pFormula)) {
      return pFormula;
    }
    CompoundInterval evaluated = pFormula.accept(evaluationVisitor, environment);
    if (!evaluated.isSingleton() && pFormula instanceof Variable) {
      // Try and replace the variable by a fitting value
      ReplaceVisitor<CompoundInterval> evaluateInvalidVars = new ReplaceVisitor<>(
          pIsInvalid,
          new Function<NumeralFormula<CompoundInterval>, NumeralFormula<CompoundInterval>>() {

            @Override
            public NumeralFormula<CompoundInterval> apply(NumeralFormula<CompoundInterval> pFormula) {
              return replaceOrEvaluateInvalid(pFormula, pIsInvalid);
            }

          });

      MemoryLocation memoryLocation = ((Variable<?>) pFormula).getMemoryLocation();
      NumeralFormula<CompoundInterval> value =
          getEnvironmentValue(pFormula.getBitVectorInfo(), memoryLocation);
      value = value.accept(evaluateInvalidVars);
      if (value instanceof Variable) {
        return value;
      }
      CompoundIntervalManager cim =
          compoundIntervalManagerFactory.createCompoundIntervalManager(pFormula.getBitVectorInfo());
      if (value instanceof Constant && cim
          .contains(evaluated, ((Constant<CompoundInterval>) value).getValue())) {
        evaluated = ((Constant<CompoundInterval>) value).getValue();
      }
      if (!evaluated.isSingleton()) {
        // Try and find a variable referring to this variable
        Set<Variable<CompoundInterval>> visited = Sets.newHashSet();
        Queue<Variable<CompoundInterval>> waitlist = Queues.newArrayDeque();
        visited.add((Variable<CompoundInterval>) pFormula);
        waitlist.addAll(visited);
        while (!waitlist.isEmpty()) {
          Variable<CompoundInterval> currentVar = waitlist.poll();
          if (!pIsInvalid.apply(currentVar)) {
            return currentVar;
          }
          for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry : environment
              .entrySet()) {
            if (entry.getValue().equals(currentVar)) {
              Variable<CompoundInterval> entryVar = InvariantsFormulaManager.INSTANCE
                  .asVariable(entry.getValue().getBitVectorInfo(), entry.getKey());
              if (visited.add(entryVar)) {
                waitlist.offer(entryVar);
              }
            }
          }
        }
      }
    }
    return InvariantsFormulaManager.INSTANCE.asConstant(pFormula.getBitVectorInfo(), evaluated);
  }

  private FluentIterable<BooleanFormula<CompoundInterval>> getApproximationFormulas() {

    final Predicate<MemoryLocation> acceptVariable =
        new Predicate<MemoryLocation>() {

          @Override
          public boolean apply(@Nullable MemoryLocation pInput) {
            return isExportable(pInput);
          }
        };

    final Predicate<BooleanFormula<CompoundInterval>> acceptFormula =
        new Predicate<BooleanFormula<CompoundInterval>>() {

          @Override
          public boolean apply(@Nullable BooleanFormula<CompoundInterval> pInput) {
            return pInput != null
                && FluentIterable.from(CompoundIntervalFormulaManager.collectVariableNames(pInput))
                .allMatch(acceptVariable);
          }

        };
    Iterable<BooleanFormula<CompoundInterval>> formulas = getEnvironmentAsAssumptions();
    if (includeTypeInformation) {
      formulas = Iterables.concat(formulas, getTypeInformationAsAssumptions());
    }
    return FluentIterable.from(formulas).filter(acceptFormula);
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    }
    if (!(pObj instanceof InvariantsState)) {
      return false;
    }
    return equalsState((InvariantsState) pObj);
  }

  private boolean equalsState(InvariantsState pOther) {
    return pOther != null
        && environment.equals(pOther.environment)
        && abstractionState.equals(pOther.abstractionState);
  }

  @Override
  public int hashCode() {
    int result = hash;
    if (result == 0) {
      result = 17;
      result = 31 * result + environment.hashCode();
      result = 31 * result + abstractionState.hashCode();
      hash = result;
    }
    return result;
  }

  @Override
  public String toString() {
    return FluentIterable.from(environment.entrySet()).transform(
        new Function<Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>>, String>() {

          @Override
          public String apply(Entry<MemoryLocation, NumeralFormula<CompoundInterval>> pInput) {
            MemoryLocation memoryLocation = pInput.getKey();
            NumeralFormula<?> value = pInput.getValue();
            if (value instanceof Exclusion) {
              return String
                  .format("%s\u2260%s", memoryLocation, ((Exclusion<?>) value).getExcluded());
            }
            return String.format("%s=%s", memoryLocation, value);
          }

        }).join(Joiner.on(", "));
  }

  public AbstractionState getAbstractionState() {
    return abstractionState;
  }

  /**
   * Gets the environment of this state.
   *
   * @return the environment of this state.
   */
  public Map<MemoryLocation, NumeralFormula<CompoundInterval>> getEnvironment() {
    return Collections.unmodifiableMap(environment);
  }

  public MachineModel getMachineModel() {
    return machineModel;
  }

  @Override
  public boolean isLessOrEqual(InvariantsState pState2) {
    if (equals(pState2)) {
      return true;
    }
    if (pState2 == null) {
      return false;
    }
    if (!abstractionState.isLessThanOrEqualTo(pState2.abstractionState)) {
      return false;
    }
    // Perform the implication check (if this state definitely implies the other one, it is less than or equal to it)
    for (BooleanFormula<CompoundInterval> rightAssumption : pState2.getEnvironmentAsAssumptions()) {
      if (!definitelyImplies(rightAssumption)) {
        return false;
      }
    }
    return true;
  }

  public boolean definitelyImplies(BooleanFormula<CompoundInterval> pFormula) {
    return compoundIntervalFormulaManager.definitelyImplies(this.environment, pFormula);
  }

  public InvariantsState widen(
      InvariantsState pOlderState,
      InvariantsPrecision pPrecision,
      @Nullable Set<MemoryLocation> pWideningTargets,
      Set<BooleanFormula<CompoundInterval>> pWideningHints) {

    final Set<MemoryLocation> wideningTargets = pWideningTargets == null
                                                ? environment.keySet()
                                                : Sets.intersection(pWideningTargets,
                                                    environment.keySet());

    if (wideningTargets.isEmpty()) {
      return this;
    }

    // Prepare result environment
    NonRecursiveEnvironment resultEnvironment = this.environment;

    // Find entries that require widening
    Map<MemoryLocation, NumeralFormula<CompoundInterval>> toDo = new HashMap<>();
    for (MemoryLocation memoryLocation : wideningTargets) {
      NumeralFormula<CompoundInterval> oldFormula = pOlderState.environment.get(memoryLocation);
      if (oldFormula == null) {
        continue;
      }
      NumeralFormula<CompoundInterval> currentFormula = environment.get(memoryLocation);
      BitVectorInfo bitVectorInfo = oldFormula.getBitVectorInfo();
      CompoundIntervalManager compoundIntervalManager =
          compoundIntervalManagerFactory.createCompoundIntervalManager(bitVectorInfo);
      currentFormula =
          currentFormula == null ? allPossibleValuesFormula(bitVectorInfo) : currentFormula;
      assert currentFormula.getBitVectorInfo().equals(bitVectorInfo);
      if (!currentFormula.equals(oldFormula)) {
        NumeralFormula<CompoundInterval> newValueFormula =
            compoundIntervalFormulaManager.union(
                currentFormula.accept(this.partialEvaluator, evaluationVisitor),
                oldFormula.accept(pOlderState.partialEvaluator, evaluationVisitor))
                .accept(new PartialEvaluator(compoundIntervalManagerFactory), evaluationVisitor);

        // Trim formulas that exceed the maximum depth
        if (currentFormula.accept(FORMULA_DEPTH_COUNT_VISITOR) > pPrecision
            .getMaximumFormulaDepth()) {
          CompoundInterval value = compoundIntervalManager.union(
              currentFormula.accept(evaluationVisitor, environment),
              oldFormula.accept(evaluationVisitor, pOlderState.getEnvironment()));
          if (!value.isSingleton()) {
            value = compoundIntervalManager.allPossibleValues();
          }
          newValueFormula = InvariantsFormulaManager.INSTANCE.asConstant(
              currentFormula.getBitVectorInfo(),
              value);
        }

        resultEnvironment = resultEnvironment.putAndCopy(memoryLocation, newValueFormula);
        toDo.put(memoryLocation, newValueFormula);
      }
    }
    if (toDo.isEmpty()) {
      return this;
    }
    for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry : toDo.entrySet()) {
      MemoryLocation memoryLocation = entry.getKey();
      NumeralFormula<CompoundInterval> newValueFormula = entry.getValue();
      BitVectorInfo bitVectorInfo = entry.getValue().getBitVectorInfo();
      CompoundInterval simpleExactValue =
          newValueFormula.accept(evaluationVisitor, resultEnvironment);
      if (simpleExactValue.isSingleton()) {
        resultEnvironment = resultEnvironment.putAndCopy(memoryLocation,
            InvariantsFormulaManager.INSTANCE.asConstant(bitVectorInfo, simpleExactValue));
      } else {
        CompoundIntervalManager compoundIntervalManager = getCompoundIntervalManager(bitVectorInfo);
        NumeralFormula<CompoundInterval> oldFormula =
            pOlderState.getEnvironmentValue(bitVectorInfo, memoryLocation);
        NumeralFormula<CompoundInterval> currentFormula =
            getEnvironmentValue(bitVectorInfo, memoryLocation);
        CompoundInterval oldExactValue =
            oldFormula.accept(evaluationVisitor, pOlderState.environment);
        CompoundInterval currentExactValue = currentFormula.accept(evaluationVisitor, environment);
        final CompoundInterval newValue;
        if (compoundIntervalManager.contains(oldExactValue, currentExactValue)) {
          newValue = oldExactValue;
        } else if (
            compoundIntervalManager.lessEqual(oldExactValue, currentExactValue).isDefinitelyTrue()
                || oldExactValue.hasUpperBound() && (!currentExactValue.hasUpperBound()
                || oldExactValue.getUpperBound().compareTo(currentExactValue.getUpperBound())
                < 0)) {
          newValue =
              compoundIntervalManager.union(oldExactValue, currentExactValue).extendToMaxValue();
        } else if (compoundIntervalManager.greaterEqual(oldExactValue, currentExactValue)
            .isDefinitelyTrue()
            || oldExactValue.hasLowerBound() && (!currentExactValue.hasLowerBound()
            || oldExactValue.getLowerBound().compareTo(currentExactValue.getLowerBound()) > 0)) {
          newValue =
              compoundIntervalManager.union(oldExactValue, currentExactValue).extendToMinValue();
        } else {
          NumeralFormula<CompoundInterval> newFormula = resultEnvironment.get(memoryLocation);
          if (newFormula == null) {
            newFormula = allPossibleValuesFormula(bitVectorInfo);
          }
          newValue = newFormula.accept(abstractionVisitor, resultEnvironment);
        }
        resultEnvironment = resultEnvironment.putAndCopy(
            memoryLocation,
            InvariantsFormulaManager.INSTANCE.asConstant(
                bitVectorInfo,
                newValue));
      }
    }
    final NonRecursiveEnvironment resEnv = resultEnvironment;
    InvariantsState result =
        new InvariantsState(resEnv, variableSelection, compoundIntervalManagerFactory, machineModel,
            variableTypes, abstractionState, overflowDetected, includeTypeInformation);

    for (BooleanFormula<CompoundInterval> hint : FluentIterable
        .from(pWideningHints)
        .filter(new Predicate<BooleanFormula<CompoundInterval>>() {

          @Override
          public boolean apply(BooleanFormula<CompoundInterval> pHint) {
            return wideningTargets.containsAll(pHint.accept(COLLECT_VARS_VISITOR));
          }
        })
        .filter(implies)) {
      result = result.assume(hint);
    }
    if (equals(result)) {
      return this;
    }

    return result;
  }

  @Override
  public InvariantsState join(InvariantsState state2) {
    return join(state2, InvariantsPrecision.getEmptyPrecision(
        AbstractionStrategyFactories.ALWAYS.createStrategy(
            compoundIntervalManagerFactory,
            machineModel)));
  }

  public InvariantsState join(InvariantsState pState2, InvariantsPrecision pPrecision) {

    InvariantsState result;

    InvariantsState state1 = this;
    InvariantsState state2 = pState2;

    if (state1.isLessOrEqual(state2)) {
      result = state2;
    } else if (state2.isLessOrEqual(state1)) {
      result = state1;
    } else {
      NonRecursiveEnvironment resultEnvironment =
          NonRecursiveEnvironment.of(compoundIntervalManagerFactory);

      // Get some basic information by joining the environments
      {
        Set<MemoryLocation> todo = new HashSet<>();

        // Join the easy ones first (both values equal or one value top)
        for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry : state1.environment
            .entrySet()) {
          MemoryLocation memoryLocation = entry.getKey();
          NumeralFormula<CompoundInterval> rightFormula = state2.environment.get(memoryLocation);
          if (rightFormula != null) {
            NumeralFormula<CompoundInterval> leftFormula =
                getEnvironmentValue(rightFormula.getBitVectorInfo(), memoryLocation);
            if (leftFormula.equals(rightFormula)) {
              resultEnvironment = resultEnvironment.putAndCopy(memoryLocation, leftFormula);
            } else {
              todo.add(memoryLocation);
            }
          }
        }

        // Compute the union of the types
        PersistentSortedMap<MemoryLocation, CType> variableTypes = state1.variableTypes;
        for (Map.Entry<MemoryLocation, CType> entry : state2.variableTypes.entrySet()) {
          if (!variableTypes.containsKey(entry.getKey())) {
            variableTypes = variableTypes.putAndCopy(entry.getKey(), entry.getValue());
          }
        }

        // Join the harder ones by constructing the union of left and right value for each variable
        for (MemoryLocation memoryLocation : todo) {
          NumeralFormula<CompoundInterval> leftFormula = environment.get(memoryLocation);
          NumeralFormula<CompoundInterval> rightFormula = state2.environment.get(memoryLocation);
          assert leftFormula != null && rightFormula != null;
          CompoundIntervalManager cim = compoundIntervalManagerFactory
              .createCompoundIntervalManager(leftFormula.getBitVectorInfo());
          NumeralFormula<CompoundInterval> evaluated =
              InvariantsFormulaManager.INSTANCE.asConstant(leftFormula.getBitVectorInfo(),
                  cim.union(
                      leftFormula.accept(evaluationVisitor, environment),
                      rightFormula.accept(state2.evaluationVisitor, state2.environment)));
          resultEnvironment = resultEnvironment.putAndCopy(memoryLocation, evaluated);
        }

      }

      VariableSelection<CompoundInterval> resultVariableSelection =
          state1.variableSelection.join(state2.variableSelection);

      AbstractionState abstractionState1 = determineAbstractionState(pPrecision);
      AbstractionState abstractionState2 = pState2.determineAbstractionState(pPrecision);
      AbstractionState abstractionState = abstractionState1.join(abstractionState2);

      result =
          new InvariantsState(resultVariableSelection, compoundIntervalManagerFactory, machineModel,
              abstractionState, resultEnvironment, variableTypes, overflowDetected,
              includeTypeInformation);

      if (result.equalsState(state1)) {
        result = state1;
      }
    }
    return result;
  }

  public BooleanFormula<CompoundInterval> asFormula() {
    BooleanFormula<CompoundInterval> result = BooleanConstant.<CompoundInterval>getTrue();
    for (BooleanFormula<CompoundInterval> assumption : getEnvironmentAsAssumptions()) {
      result = compoundIntervalFormulaManager.logicalAnd(result, assumption);
    }
    return result;
  }

  public Set<MemoryLocation> getVariables() {
    Set<MemoryLocation> result = environment.keySet();
    for (NumeralFormula<CompoundInterval> value : environment.values()) {
      Set<MemoryLocation> valueVars = value.accept(COLLECT_VARS_VISITOR);
      if (!valueVars.isEmpty()) {
        result = Sets.union(result, valueVars);
      }
    }
    return result;
  }

  private Set<Variable<CompoundInterval>> getVariables(final Predicate<MemoryLocation> pMemoryLocationPredicate) {
    final Set<Variable<CompoundInterval>> result = new HashSet<>();
    Predicate<NumeralFormula<CompoundInterval>> pCondition =
        new Predicate<NumeralFormula<CompoundInterval>>() {

          @Override
          public boolean apply(NumeralFormula<CompoundInterval> pFormula) {
            if (pFormula instanceof Variable) {
              Variable<?> variable = (Variable<?>) pFormula;
              MemoryLocation memoryLocation = variable.getMemoryLocation();
              return pMemoryLocationPredicate.apply(memoryLocation)
                  && !result.contains(variable);
            }
            return false;
          }

        };
    CollectFormulasVisitor<CompoundInterval> collectVisitor =
        new CollectFormulasVisitor<>(pCondition);
    for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry : environment
        .entrySet()) {
      MemoryLocation memoryLocation = entry.getKey();
      if (pMemoryLocationPredicate.apply(memoryLocation)) {
        result.add(InvariantsFormulaManager.INSTANCE.<CompoundInterval>asVariable(
            entry.getValue().getBitVectorInfo(), memoryLocation));
      }
      for (NumeralFormula<CompoundInterval> formula : entry.getValue().accept(collectVisitor)) {
        Variable<CompoundInterval> variable = (Variable<CompoundInterval>) formula;
        result.add(variable);
      }
    }
    return result;
  }

  public InvariantsState overflowDetected() {
    if (overflowDetected) {
      return this;
    }
    return new InvariantsState(
        variableSelection,
        compoundIntervalManagerFactory,
        machineModel,
        abstractionState,
        environment,
        variableTypes,
        true,
        includeTypeInformation);
  }

  @Override
  public String getCPAName() {
    return InvariantsCPA.class.getSimpleName();
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.equals(PROPERTY_OVERFLOW)) {
      return overflowDetected;
    }
    throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    return checkProperty(pProperty);
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    throw new InvalidQueryException("Cannot modify properties.");
  }

  private static boolean isExportable(@Nullable MemoryLocation pMemoryLocation) {
    return pMemoryLocation != null
        && !pMemoryLocation.getIdentifier().contains("*")
        && !pMemoryLocation.getIdentifier().contains("->")
        && !pMemoryLocation.getIdentifier().contains(".")
        && !pMemoryLocation.getIdentifier().contains("[");
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }
}
