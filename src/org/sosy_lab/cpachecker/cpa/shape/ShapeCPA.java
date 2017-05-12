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
package org.sosy_lab.cpachecker.cpa.shape;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisMultiInitials;
import org.sosy_lab.cpachecker.core.interfaces.FunctionRegistrant;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState.ShapeStateJoinOperator;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SEs;
import org.sosy_lab.cpachecker.cpa.shape.function.ShapePointerAdapter;
import org.sosy_lab.cpachecker.cpa.shape.function.ShapeValueAdapter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.util.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Options(prefix = "cpa.shape")
public class ShapeCPA implements ConfigurableProgramAnalysisMultiInitials, FunctionRegistrant {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ShapeCPA.class);
  }

  @Option(secure = true, name = "merge", values = {"SEP", "JOIN"}, description = "merge "
      + "operator used for shape analysis")
  private String mergeType = "SEP";

  @Option(secure = true, name = "stop", values = {"SEP", "NEVER"}, description = "stop operator "
      + "used for shape analysis")
  private String stopType = "SEP";

  @Option(secure = true, name = "initializePointerAsNULL", description = "whether we initialize "
      + "a pointer as NULL value")
  private boolean initPointerAsNull = false;

  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;

  private final MachineModel machineModel;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final CFA cfa;

  private ShapeCPA(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCFA) throws InvalidConfigurationException {
    config = pConfig;
    config.inject(this);
    cfa = pCFA;
    machineModel = cfa.getMachineModel();
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    abstractDomain = DelegateAbstractDomain.<ShapeState>getInstance();
    if (mergeType.equals("JOIN")) {
      mergeOperator = ShapeStateJoinOperator.getInstance();
    } else {
      mergeOperator = MergeSepOperator.getInstance();
    }
    if (stopType.equals("NEVER")) {
      stopOperator = new StopNeverOperator();
    } else {
      stopOperator = new StopSepOperator(abstractDomain);
    }
    precisionAdjustment = StaticPrecisionAdjustment.getInstance();

    Solver solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    transferRelation = new ShapeTransferRelation(config, logger, machineModel, shutdownNotifier,
        solver, mergeType);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) {
    ShapeState initState = new ShapeState(logger, machineModel);
    if (node instanceof CFunctionEntryNode) {
      // for the function entry, we create a new function call frame
      CFunctionEntryNode functionNode = (CFunctionEntryNode) node;
      initState.addStackFrame(functionNode.getFunctionDefinition());
    }
    return initState;
  }

  @Override
  public Collection<AbstractState> getInitialStates(
      CFANode node, StateSpacePartition partition) {
    // STEP 1: initializing work
    CFunctionEntryNode functionNode = (CFunctionEntryNode) node;
    ShapeState initState = new ShapeState(logger, machineModel);
    initState.addStackFrame(functionNode.getFunctionDefinition());
    // STEP 2: create initial objects for each parameter value
    List<CParameterDeclaration> parameters = functionNode.getFunctionParameters();
    Set<ShapeState> states = new HashSet<>();
    states.add(initState);
    Set<ShapeState> newStates = new HashSet<>();
    for (CParameterDeclaration parameter : parameters) {
      CType type = CoreShapeAdapter.getType(parameter.getType());
      String name = parameter.getName();
      // parameter value should not be VLA
      int size = machineModel.getSizeof(type);
      boolean isPtr = type instanceof CPointerType;
      for (ShapeState state : states) {
        SGObject newObject = state.addLocalVariable(type, KnownExplicitValue.valueOf(size), name);
        if (isPtr) {
          // create an object pointing to NULL object
          if (initPointerAsNull) {
            ShapeState newStateNull = new ShapeState(state);
            newStateNull =
                newStateNull.writeValue(newObject, 0, type, KnownSymbolicValue.ZERO).getState();
            newStates.add(newStateNull);
          }
          // create an object pointing to non-null object
          ShapeState newStateNonNull = new ShapeState(state);
          CPointerType pointerType = (CPointerType) type;
          CType innerType = CoreShapeAdapter.getType(pointerType.getType());
          String objectName = "parameter_ID:" + parameter.getQualifiedName();
          // TODO: use symbolic size for memory block
          int objectSize = machineModel.getSizeof(innerType);
          ShapeAddressValue address = newStateNonNull.addHeapAllocation(objectName, innerType, SEs
                  .toConstant(KnownExplicitValue.valueOf(objectSize), CNumericTypes.UNSIGNED_INT),
              false, functionNode.getLeavingEdge(0));
          newStateNonNull = newStateNonNull.writeValue(newObject, 0, type, address).getState();
          newStates.add(newStateNonNull);
        } else {
          // create an object with fresh value
          KnownSymbolicValue freshValue = KnownSymbolicValue.valueOf(SymbolicValueFactory
              .getNewValue());
          state = state.writeValue(newObject, 0, type, freshValue).getState();
          newStates.add(state);
        }
      }
      states.clear();
      states.addAll(newStates);
      newStates.clear();
    }
    // STEP 3: return the initialized states
    ImmutableSet.Builder<AbstractState> result = ImmutableSet.builder();
    result.addAll(states);
    return result.build();
  }

  @Override
  public Precision getInitialPrecision(
      CFANode node, StateSpacePartition partition) {
    return SingletonPrecision.getInstance();
  }

  public Configuration getConfig() {
    return config;
  }

  public CFA getCFA() {
    return cfa;
  }

  public ShutdownNotifier getShutdownNotifier() {
    return shutdownNotifier;
  }

  public LogManager getLogger() {
    return logger;
  }

  @Override
  public boolean retrieveCall(CFunctionCallExpression pCFunctionCallExpression) {
    return ShapePointerAdapter.instance().isRegistered(pCFunctionCallExpression) ||
        ShapeValueAdapter.instance().isRegistered(pCFunctionCallExpression);
  }
}
