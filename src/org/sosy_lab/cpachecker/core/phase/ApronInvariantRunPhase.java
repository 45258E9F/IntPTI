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
package org.sosy_lab.cpachecker.core.phase;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.CPAchecker.InitialStatesFor;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory.SpecAutomatonCompositionType;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.ExternalCBMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.impact.ImpactAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.phase.result.AlgorithmPhaseResult;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProviderImpl;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Algorithm phase is one of the most frequently used phase.
 * In this phase, we create a new algorithm and initial reached set with respect of configuration
 *
 * There are two levels of configurations: one is for this phase and another one is for the
 * wrapped algorithm. Configurations for this phase comprise i) whether it should stop after the
 * first error; ii) how initial states are specified; iii) whether we use external CBMC tool
 * rather than CPA; iv) configuration file for wrapped algorithm.
 */
@Options(prefix = "phase.apronInvariant")
public class ApronInvariantRunPhase extends CPAPhase {

  private Algorithm algorithm;
  private ReachedSet reached;

  private ShutdownManager subShutdownManager;
  private ConfigurableProgramAnalysis subCpa;
  private final String programDenotation;
  private static final HashMap<String, Set<MemoryLocation>> usedVars =
      new HashMap<String, Set<MemoryLocation>>();
  private String functionName;

  // NOTE: the following configurations are upon the algorithm level, thus we should carefully
  // specify them in the phase of algorithm run

  @Option(secure = true, name = "stopAfterError", description = "stop after the first error has "
      + "been found")
  private boolean stopAfterError = true;

  @Option(secure = true, name = "initialStatesFor", description = "start point of analysis")
  private Set<InitialStatesFor> initialStatesFor = Sets.newHashSet(InitialStatesFor.ENTRY);

  @Option(secure = true, description =
      "Partition the initial states based on the containing function "
          + "of the initial location. This option suppresses partitionInitialStates")
  private boolean functionWisePartition = false;

  @Option(secure = true, description = "partition initial states based on the type of location")
  private boolean partitionInitialStates = false;

  @Option(secure = true, name = "useCBMC", description = "use CBMC as external verifier")
  private boolean runCBMCTool = false;

  @Option(secure = true, name = "analysis", description = "the configuration of main analysis")
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path mainAnalyisisConfigFile = Paths.get("config/valueAnalysis-symbolic.properties");

  public ApronInvariantRunPhase(
      String pIdentifier,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      ShutdownNotifier pShutdownNotifier,
      MainStatistics pStats)
      throws InvalidConfigurationException {
    super(pIdentifier, pConfig, pLogger, pShutdownManager, pShutdownNotifier, pStats);
    config.inject(this);
    // this should be lazily initialized
    subShutdownManager = null;
    subCpa = null;
    functionName = null;
    algorithm = null;
    reached = null;
    // load program denotation from IO manager
    programDenotation = GlobalInfo.getInstance().getIoManager().getProgramNames();
  }

  @Override
  protected CPAPhaseStatus prevAction() throws Exception {

    CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().orNull();
    if (cfaInfo == null) {
      throw new InvalidConfigurationException("Invalid CFA set-up");
    }
    CFA cfa = cfaInfo.getCFA();

    if (cfa instanceof ImmutableCFA) {
      calculateUsedVars((ImmutableCFA) cfa);
    }

    ConfigurationBuilder configBuilder = Configuration.builder();
    // to inherit from the global configuration
    configBuilder.copyFrom(config);
    configBuilder.loadFromFile(mainAnalyisisConfigFile);
    Configuration subConfig = configBuilder.build();
    subShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
    LogManager subLogger = logger.withComponentName("ApronInvariantRunPhase");
    CoreComponentsFactory factory =
        new CoreComponentsFactory(subConfig, subLogger, subShutdownManager
            .getNotifier());
    reached = factory.createReachedSet();
    if (runCBMCTool) {
      checkIfOneValidFile(programDenotation);
      algorithm = new ExternalCBMCAlgorithm(programDenotation, subConfig, subLogger);
    } else {
      final SpecAutomatonCompositionType specComposition = initialStatesFor.contains
          (InitialStatesFor.TARGET) ? SpecAutomatonCompositionType.BACKWARD_TO_ENTRY_SPEC :
                                                           SpecAutomatonCompositionType.TARGET_SPEC;
      subCpa = factory.createCPA(cfa, stats, specComposition);
      // set-up CPA info on global since we are about to run this CPA
      GlobalInfo.getInstance().setUpInfoFromCPA(subCpa);
      algorithm = factory.createAlgorithm(subCpa, programDenotation, cfa, stats);
      // initialize reached set
      if (algorithm instanceof ImpactAlgorithm) {
        ImpactAlgorithm impactAlgo = (ImpactAlgorithm) algorithm;
        reached.add(impactAlgo.getInitialState(cfa.getMainFunction()), impactAlgo
            .getInitialPrecision(cfa.getMainFunction()));
      } else {
        initializeReachedSet(reached, subCpa, cfa.getMainFunction(), cfa, factory,
            subShutdownManager
                .getNotifier(), subLogger, subConfig);
      }
    }

    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus postAction() throws Exception {
    // the same as {@link prevAction}
    if (subCpa != null) {
      CPAs.closeCpaIfPossible(subCpa, logger);
    }
    CPAs.closeIfPossible(algorithm, logger);
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus runPhase() throws Exception {
    AlgorithmStatus runStat = AlgorithmStatus.SOUND_AND_PRECISE;

    try {
      do {
        runStat = runStat.update(algorithm.run(reached));
      } while (!stopAfterError && reached.hasWaitingState());
      currResult = runStat;
    } catch (Exception ex) {
      throw ex;
    } finally {
      subShutdownManager.requestShutdown("ApronInvariantRunPhase terminated");
      // store the algorithm status and reached set as the phase result
      currResult = new AlgorithmPhaseResult(runStat, reached);
    }
    return CPAPhaseStatus.SUCCESS;
  }

  private void checkIfOneValidFile(String fileDenote) throws InvalidConfigurationException {
    if (fileDenote.contains(",")) {
      throw new InvalidConfigurationException("Multiple files not supported");
    }
  }

  private void initializeReachedSet(
      final ReachedSet pReached, final ConfigurableProgramAnalysis
      pCpa, final FunctionEntryNode pEntry, final CFA pCfa, final CoreComponentsFactory pFactory,
      final ShutdownNotifier pNotifier, final LogManager pLogger, final Configuration pConfig)
      throws InvalidConfigurationException {
    for (InitialStatesFor isf : initialStatesFor) {
      final ImmutableSet<? extends CFANode> initialLocations;
      switch (isf) {
        case ENTRY:
          initialLocations = ImmutableSet.of(pEntry);
          break;
        case EXIT:
          initialLocations = ImmutableSet.of(pEntry.getExitNode());
          break;
        case FUNCTION_ENTRIES:
          initialLocations = ImmutableSet.copyOf(pCfa.getAllFunctionHeads());
          break;
        case FUNCTION_SINKS:
          // TODO: why adding loop heads?
          initialLocations = ImmutableSet.<CFANode>builder().addAll(getAllEndlessLoopHeads(pCfa
              .getLoopStructure().get()))
              .addAll(getAllFunctionExitNodes(pCfa))
              .build();
          break;
        case PROGRAM_SINKS:
          Builder builder = ImmutableSet.builder().addAll(getAllEndlessLoopHeads(pCfa
              .getLoopStructure().get()));
          if (pCfa.getAllNodes().contains(pEntry.getExitNode())) {
            builder.add(pEntry.getExitNode());
          }
          initialLocations = builder.build();
          break;
        case TARGET:
          TargetLocationProvider provider = new TargetLocationProviderImpl(pFactory
              .getReachedSetFactory(), pNotifier, pLogger, pConfig, pCfa);
          initialLocations = provider.tryGetAutomatonTargetLocations(pEntry);
          break;
        default:
          throw new AssertionError("Unhandled case statement: " + initialStatesFor);
      }
      // finally, add these locations to the reached set
      addToInitialReachedSet(initialLocations, isf, pReached, pCpa);
    }

    if (!pReached.hasWaitingState()) {
      throw new InvalidConfigurationException("No analysis target found");
    }
  }

  private Set<CFANode> getAllFunctionExitNodes(CFA cfa) {
    Set<CFANode> funcExitNodes = new HashSet<>();
    for (FunctionEntryNode node : cfa.getAllFunctionHeads()) {
      FunctionExitNode exit = node.getExitNode();
      if (cfa.getAllNodes().contains(exit)) {
        funcExitNodes.add(exit);
      }
    }
    return funcExitNodes;
  }

  private Set<CFANode> getAllEndlessLoopHeads(LoopStructure structure) {
    ImmutableCollection<Loop> loops = structure.getAllLoops();
    Set<CFANode> loopHeads = new HashSet<>();
    for (Loop loop : loops) {
      if (loop.getOutgoingEdges().isEmpty()) {
        for (CFANode head : loop.getLoopHeads()) {
          loopHeads.add(head);
        }
      }
    }

    return loopHeads;
  }

  private void addToInitialReachedSet(
      final Set<? extends CFANode> pLocations, final Object
      pPartitionKey, final ReachedSet pReached, final ConfigurableProgramAnalysis pCpa) {
    for (CFANode loc : pLocations) {
      StateSpacePartition partition = partitionInitialStates ? StateSpacePartition
          .getPartitionWithKey(pPartitionKey) : StateSpacePartition.getDefaultPartition();

      if (functionWisePartition && (pPartitionKey == InitialStatesFor.FUNCTION_ENTRIES ||
          pPartitionKey == InitialStatesFor.FUNCTION_SINKS)) {
        partition = StateSpacePartition.getPartitionWithKey(loc.getFunctionName());
      }

      AbstractState initialState = pCpa.getInitialState(loc, partition);
      Precision initialPrecision = pCpa.getInitialPrecision(loc, partition);
      pReached.add(initialState, initialPrecision);
    }
  }

  public class CApronPrevActionExpressionVisitor
      extends DefaultCExpressionVisitor<Set<MemoryLocation>, CPATransferException>
      implements CRightHandSideVisitor<Set<MemoryLocation>, CPATransferException> {

    @Override
    protected Set<MemoryLocation> visitDefault(CExpression pExp) throws CPATransferException {
      MemoryLocation ml = MemoryLocation.valueOf(functionName + "::" + pExp.toString());
      return Collections.singleton(ml);
    }

    protected MemoryLocation getStructureFieldLocationFromRelativePoint(
        MemoryLocation pStartLocation,
        String pFieldName, CType pOwnerType) throws CPATransferException {
      CType canonicalOwnerType = pOwnerType.getCanonicalType();

      Integer offset = getFieldOffset(canonicalOwnerType, pFieldName);

      if (offset == null) {
        return null;
      }

      long baseOffset = pStartLocation.isReference() ? pStartLocation.getOffset() : 0;

      if (pStartLocation.isOnFunctionStack()) {

        return MemoryLocation.valueOf(
            pStartLocation.getFunctionName(), pStartLocation.getIdentifier(), baseOffset + offset);
      } else {

        return MemoryLocation.valueOf(pStartLocation.getIdentifier(), baseOffset + offset);
      }
    }

    private Integer getFieldOffset(CCompositeType ownerType, String fieldName) {

      MachineModel machineModel =
          GlobalInfo.getInstance().getCFAInfo().get().getCFA().getMachineModel();
      List<CCompositeTypeMemberDeclaration> membersOfType = ownerType.getMembers();

      int offset = 0;

      for (CCompositeTypeMemberDeclaration typeMember : membersOfType) {
        String memberName = typeMember.getName();

        if (memberName.equals(fieldName)) {
          return offset;
        }

        if (!(ownerType.getKind() == ComplexTypeKind.UNION)) {

          CType fieldType = typeMember.getType().getCanonicalType();

          offset = offset + machineModel.getSizeof(fieldType);
        }
      }

      return null;
    }


    private Integer getFieldOffset(CType ownerType, String fieldName) throws CPATransferException {

      if (ownerType instanceof CElaboratedType) {
        return getFieldOffset(((CElaboratedType) ownerType).getRealType(), fieldName);
      } else if (ownerType instanceof CCompositeType) {
        return getFieldOffset((CCompositeType) ownerType, fieldName);
      } else if (ownerType instanceof CPointerType) {
        //return null;
        return getFieldOffset(((CPointerType) ownerType).getType(), fieldName);
      }

      throw new AssertionError();
    }

    @Override
    public Set<MemoryLocation> visit(CFieldReference pIastFieldReference)
        throws CPATransferException {
      if (pIastFieldReference.isPointerDereference()) {
        //return null;
      }

      CLeftHandSide fieldOwner = (CLeftHandSide) pIastFieldReference.getFieldOwner();

      Set<MemoryLocation> memLocOfFieldOwner = fieldOwner.accept(this);

      if (memLocOfFieldOwner == null) {
        return null;
      }
      Set<MemoryLocation> result = new HashSet<>();
      Iterator<MemoryLocation> it = memLocOfFieldOwner.iterator();
      while (it.hasNext()) {
        MemoryLocation s = getStructureFieldLocationFromRelativePoint(it.next(),
            pIastFieldReference.getFieldName(),
            fieldOwner.getExpressionType());
        result.add(s);
      }
      return result;
    }

    @Override
    public Set<MemoryLocation> visit(CBinaryExpression e) throws CPATransferException {
      Set<MemoryLocation> left = e.getOperand1().accept(this);
      Set<MemoryLocation> right = e.getOperand2().accept(this);

      Set<MemoryLocation> returnMemoryLocation = new HashSet<>();
      returnMemoryLocation.addAll(left);
      returnMemoryLocation.addAll(right);
      return returnMemoryLocation;
    }

    /**
     * Only unpack the cast and continue with the casts operand
     */
    @Override
    public Set<MemoryLocation> visit(CCastExpression e) throws CPATransferException {
      return e.getOperand().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CIdExpression e) throws CPATransferException {
      String name = e.getDeclaration().getQualifiedName();
      MemoryLocation ml = MemoryLocation.valueOf(name);
      return Collections.singleton(ml);
    }

    @Override
    public Set<MemoryLocation> visit(CCharLiteralExpression e) throws CPATransferException {
      return Collections.EMPTY_SET;
    }

    @Override
    public Set<MemoryLocation> visit(CFloatLiteralExpression e) throws CPATransferException {
      return Collections.EMPTY_SET;
    }

    @Override
    public Set<MemoryLocation> visit(CIntegerLiteralExpression e) throws CPATransferException {
      return Collections.EMPTY_SET;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Set<MemoryLocation> visit(CUnaryExpression e) throws CPATransferException {
      Set<MemoryLocation> operand = e.getOperand().accept(this);
      return operand;
    }

    @Override
    public Set<MemoryLocation> visit(CFunctionCallExpression e) throws CPATransferException {
      List<CExpression> paramExpressions = e.getParameterExpressions();
      Set<MemoryLocation> returnMemoryLocation = new HashSet<>();
      for (int i = 0; i < paramExpressions.size(); i++) {
        Set<MemoryLocation> tempMemoryLocation = paramExpressions.get(i).accept(this);
        returnMemoryLocation.addAll(tempMemoryLocation);
      }
      return returnMemoryLocation;
    }
  }

  private void calculateUsedVars(ImmutableCFA cfa) {
    ImmutableSortedSet<CFANode> allNodes = cfa.getAllNodes();
    ImmutableList<CFANode> allNodesList = allNodes.asList();
    for (int i = 0; i < allNodesList.size(); i++) {
      CFANode node = allNodesList.get(i);
      int edgeNum = node.getNumLeavingEdges();
      for (int j = 0; j < edgeNum; j++) {
        CFAEdge edge = node.getLeavingEdge(j);
        functionName = edge.getPredecessor().getFunctionName();
        if (edge instanceof CStatementEdge) {
          calculateCStatementEdgeUsedVars((CStatementEdge) edge);
        } else if (edge instanceof CDeclarationEdge) {
          calculateCDeclarationEdgeUsedVars((CDeclarationEdge) edge);
        } else if (edge instanceof CAssumeEdge) {
          calculateCAssumeEdgeUsedVars((CAssumeEdge) edge);
        }
      }
    }

    /*
    //handle field reference in local function
    Iterator<Map.Entry<String, Set<MemoryLocation>>> it1 = usedVars.entrySet().iterator();
    while(it1.hasNext()) {
      Map.Entry<String, Set<MemoryLocation>> e = it1.next();
      if (!e.getKey().equals("")) {
        Set<MemoryLocation> set = e.getValue();
        Iterator<MemoryLocation> it2 = set.iterator();
        while(it2.hasNext()) {
          MemoryLocation ml = it2.next();
          if (ml.toString().contains("struct")) {
            MemoryLocation newml = MemoryLocation.valueOf(ml.getIdentifier());
            usedVars.get("").add(newml);
          }
        }
      }
    }
    */
    Iterator<Map.Entry<String, Set<MemoryLocation>>> it = usedVars.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Set<MemoryLocation>> e = it.next();
      System.out.println(e.getKey());
      System.out.println(e.getValue());
    }

  }

  private void calculateCStatementEdgeUsedVars(CStatementEdge cfaEdge) {
    try {
      Set<MemoryLocation> returnMemoryLocation = new HashSet<>();
      CStatement statement = cfaEdge.getStatement();
      if (cfaEdge instanceof CFunctionSummaryStatementEdge) {
        CFunctionSummaryStatementEdge funcSummaryStatementEdge =
            (CFunctionSummaryStatementEdge) cfaEdge;
        CFunctionCall fc = funcSummaryStatementEdge.getFunctionCall();
        returnMemoryLocation =
            fc.getFunctionCallExpression().accept(new CApronPrevActionExpressionVisitor());
      }
      if (statement instanceof CFunctionCall) {
        CFunctionCall fc = (CFunctionCall) statement;
        returnMemoryLocation =
            fc.getFunctionCallExpression().accept(new CApronPrevActionExpressionVisitor());
      } else if (statement instanceof CAssignment) {
        CRightHandSide right = ((CAssignment) statement).getRightHandSide();
        returnMemoryLocation = right.accept(new CApronPrevActionExpressionVisitor());
      }
      Iterator<MemoryLocation> it = returnMemoryLocation.iterator();
      while (it.hasNext()) {
        MemoryLocation ml = it.next();
        String functionName = cfaEdge.getPredecessor().getFunctionName();
        if (usedVars.get(functionName) == null) {
          Set<MemoryLocation> mlSet = new HashSet<>();
          mlSet.add(ml);
          usedVars.put(functionName, mlSet);
        } else {
          usedVars.get(functionName).add(ml);
        }
      }

    } catch (CPATransferException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void calculateCDeclarationEdgeUsedVars(CDeclarationEdge cfaEdge) {
    try {
      Set<MemoryLocation> returnMemoryLocation = new HashSet<>();
      CDeclaration declaration = cfaEdge.getDeclaration();
      if (declaration instanceof CVariableDeclaration) {
        CVariableDeclaration decl = (CVariableDeclaration) declaration;
        CInitializer initializer = decl.getInitializer();
        if (initializer instanceof CInitializerExpression) {
          CInitializerExpression initialExpr = (CInitializerExpression) initializer;
          CExpression expr = initialExpr.getExpression();
          returnMemoryLocation = expr.accept(new CApronPrevActionExpressionVisitor());
        }
      }
      Iterator<MemoryLocation> it = returnMemoryLocation.iterator();
      while (it.hasNext()) {
        MemoryLocation ml = it.next();
        String functionName = cfaEdge.getPredecessor().getFunctionName();
        if (usedVars.get(functionName) == null) {
          Set<MemoryLocation> mlSet = new HashSet<>();
          mlSet.add(ml);
          usedVars.put(functionName, mlSet);
        } else {
          usedVars.get(functionName).add(ml);
        }
      }
    } catch (CPATransferException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void calculateCAssumeEdgeUsedVars(CAssumeEdge cfaEdge) {
    try {
      Set<MemoryLocation> returnMemoryLocation = new HashSet<>();
      CExpression expression = cfaEdge.getExpression();
      if (expression instanceof CLiteralExpression) {
        //do nothing
      } else {
        returnMemoryLocation = expression.accept(new CApronPrevActionExpressionVisitor());
      }
      Iterator<MemoryLocation> it = returnMemoryLocation.iterator();
      while (it.hasNext()) {
        MemoryLocation ml = it.next();
        String functionName = cfaEdge.getPredecessor().getFunctionName();
        if (usedVars.get(functionName) == null) {
          Set<MemoryLocation> mlSet = new HashSet<>();
          mlSet.add(ml);
          usedVars.put(functionName, mlSet);
        } else {
          usedVars.get(functionName).add(ml);
        }
      }
    } catch (CPATransferException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public ReachedSet getReachedSet() {
    return reached;
  }

  public static HashMap<String, Set<MemoryLocation>> getUsedVars() {
    return usedVars;
  }
}
