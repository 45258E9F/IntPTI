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
package org.sosy_lab.cpachecker.core.algorithm.summary.computer;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
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
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory.SpecAutomatonCompositionType;
import org.sosy_lab.cpachecker.core.algorithm.summary.CPABasedSummaryComputer;
import org.sosy_lab.cpachecker.core.algorithm.summary.SummarySubject;
import org.sosy_lab.cpachecker.core.algorithm.summary.SummaryType;
import org.sosy_lab.cpachecker.core.algorithm.summary.subjects.FunctionSubject;
import org.sosy_lab.cpachecker.core.algorithm.summary.subjects.LoopSubject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ArithFunctionSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ArithLoopSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ArithSummaryStore;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider.SummaryName;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryStore;
import org.sosy_lab.cpachecker.cpa.apron.ApronState;
import org.sosy_lab.cpachecker.cpa.apron.ApronTransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

@Options(prefix = "summary.arith")
public class ArithSummaryComputer extends CPABasedSummaryComputer {

  @Option(name = "threshold", secure = true, description = "the complexity bound of summary subjects for arith")
  private int complexityThreshold = 30;

  private static final HashMap<String, Map<MemoryLocation, CType>> usedVars = new HashMap<>();

  private Map<CFAEdge, Loop> loopEntryEdges = null;
  private Map<CFAEdge, Loop> loopExitEdges = null;


  public ArithSummaryComputer(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier shutdownNotifier) throws Exception {
    super(pConfig, pLogger, shutdownNotifier);
    pConfig.inject(this);
    initSummary();

    calculateUsedVars(cfaInfo.getCFA());
  }

  @Override
  public void preAction() {
  }

  @Override
  public void initSummary() {
    CFA cfa = cfaInfo.getCFA();
    for (FunctionEntryNode entry : cfa.getAllFunctionHeads()) {
      FunctionSubject subject = FunctionSubject.of(entry);
      update(subject, ArithFunctionSummaryInstance.of(subject.getFunctionName()));
    }
  }

  /*
     * build all summarys(function or loop), each summary type has a summary store
     */
  @Override
  public List<Triple<SummaryType, SummaryName, ? extends SummaryStore>> build() {
    ArithSummaryStore store = new ArithSummaryStore();

    for (SummarySubject sub : summary.keySet()) {
      SummaryInstance instance = summary.get(sub);
      if (sub instanceof FunctionSubject) {
        store.updateFunctionSummary(((FunctionSubject) sub).getFunctionEntry(),
            (ArithFunctionSummaryInstance) instance);
      } else if (sub instanceof LoopSubject) {
        store.updateLoopSummary(((LoopSubject) sub).getLoop(), (ArithLoopSummaryInstance) instance);
      }
    }

    // both function and loop summaries are stored in the same instance of SummaryStore
    List<Triple<SummaryType, SummaryName, ? extends SummaryStore>> summarized =
        Lists.newArrayList();
    summarized.add(Triple.of(SummaryType.FUNCTION_SUMMARY, SummaryName.ARITH_SUMMARY, store));
    summarized.add(Triple.of(SummaryType.LOOP_SUMMARY, SummaryName.ARITH_SUMMARY, store));
    summarized.add(Triple.of(SummaryType.LOOP_SUMMARY, SummaryName.ARITH_INVARIANT, store));
    return summarized;
  }

  @Override
  protected ConfigurableProgramAnalysis createCPA(
      Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {
    ConfigurableProgramAnalysis cpa = null;
    try {
      CoreComponentsFactory factory = new CoreComponentsFactory(config, logger, pShutdownNotifier);
      CFA cfa = GlobalInfo.getInstance().getCFAInfo().get().getCFA();
      cpa = factory.createCPA(cfa, null, SpecAutomatonCompositionType.NONE);

    } catch (InvalidConfigurationException | CPAException e) {
      e.printStackTrace();
    }
    return cpa;
  }

  /*
   * each function/loop has a reachedSet
   */
  @Override
  protected ReachedSet initReachedSetForSubject(
      SummarySubject pSubject,
      ConfigurableProgramAnalysis pCpa) {
    // System.out.println("we are in function: " + pSubject.toString());
    Preconditions.checkArgument(pSubject instanceof FunctionSubject);
    FunctionSubject fSubject = (FunctionSubject) pSubject;
    ReachedSet reached = null;
    ReachedSetFactory reachedSetFactory;
    try {
      reachedSetFactory = new ReachedSetFactory(
          config);// for bfs WaitlistFactory waitlistFactory = traversalMethod;
      reached = reachedSetFactory.create();

      // function without definition, it should never happen
      if (fSubject.getFunctionEntry() == null) {
        return reached;
      }
      CFANode loc = fSubject.getFunctionEntry();
      StateSpacePartition partition = StateSpacePartition.getDefaultPartition();
      AbstractState initialState = pCpa.getInitialState(loc, partition);
      Precision initialPrecision = pCpa.getInitialPrecision(loc, partition);
      reached.add(initialState, initialPrecision);
    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
    return reached;
  }

  //we use this function to prevent arith computing summaries for complex functions
  public int cyclomaticComplex(SummarySubject pSubject) {
    Set<CFANode> nodes = new HashSet<>();
    Set<CFAEdge> edges = new HashSet<>();
    if (pSubject instanceof FunctionSubject) {
      CFANode entry = ((FunctionSubject) pSubject).getFunctionEntry();
      Stack<CFANode> stack = new Stack<>();
      stack.push(entry);
      while (!stack.isEmpty()) {
        CFANode temp = stack.pop();
        nodes.add(temp);
        for (int i = 0; i < temp.getNumLeavingEdges(); i++) {
          CFAEdge leavingEdge = temp.getLeavingEdge(i);
          CFANode successor = leavingEdge.getSuccessor();
          if (temp.getFunctionName().equals(successor.getFunctionName())) {
            edges.add(leavingEdge);
            if (!nodes.contains(successor) && !(successor instanceof FunctionExitNode)) {
              stack.push(successor);
            }
          }
        }
      }
    } else if (pSubject instanceof LoopSubject) {
      nodes = ((LoopSubject) pSubject).getLoop().getLoopNodes();
      edges = ((LoopSubject) pSubject).getLoop().getInnerLoopEdges();
    }
    return (edges.size() + 2 - nodes.size());
  }

  @Override
  public Set<SummarySubject> computeFor(SummarySubject subject) throws Exception {
    long start = System.currentTimeMillis();
    logger.log(Level.INFO, "Start computation for: " + subject);

    SummaryInstance oldInstance = summary.get(subject);

    // Pick next state using strategy
    // BFS, DFS or top sort according to the configuration

    ReachedSet reached = initReachedSetForSubject(subject, cpa);
    Preconditions.checkNotNull(reached);

    Map<? extends SummarySubject, ? extends SummaryInstance> partialSummary;
    int cc = cyclomaticComplex(subject);
    if (cc <= complexityThreshold) {
      partialSummary = computeFor0(reached, subject, oldInstance);
    } else {
      logger.log(Level.WARNING, "Skipping this subject with cyclomatic complexity " + cc);
      partialSummary = new HashMap<>();
    }

    // clear the reached set (for GC)
    reached.clear();

    // update the set of summary entries that should be modified
    // collect their dependers (which will trigger re-computation for them)
    Set<SummarySubject> influenced = Sets.newHashSet();
    for (Map.Entry<? extends SummarySubject, ? extends SummaryInstance> entry : partialSummary
        .entrySet()) {
      SummarySubject s = entry.getKey();
      SummaryInstance inst = entry.getValue();
      if (update(s, inst)) {
        influenced.addAll(getDepender(s));
      }
    }

    logger.log(Level.INFO, "triggers: " + influenced);
    logger.log(Level.INFO, "time=" + (System.currentTimeMillis() - start) / 1000.0);
    logger.log(Level.INFO, "Finish computation for: " + subject);
    return influenced;
  }

  /*
   * each function/loop has a SummaryInstance, Summarize will be called multiple times
   * update SummaryInstance of pSubject, according to the information from pReachedSet
   */
  @Override
  protected Map<? extends SummarySubject, ? extends SummaryInstance> summarize(
      SummarySubject pSubject, ReachedSet pReachedSet, SummaryInstance pOld) {
    if (pSubject instanceof FunctionSubject) {
      FunctionSubject fSubject = (FunctionSubject) pSubject;
      String functionName = fSubject.getFunctionName();

      getLoopsForFunction(functionName);

      Iterator<AbstractState> it1 = pReachedSet.iterator();

      Map<CFAEdge, ApronState> exitEdgeToState = new HashMap<>();
      Map<CFAEdge, ApronState> entryEdgeToState = new HashMap<>();

      LocationState functionExitState = null;
      ApronState functionExitApronState = null;

      while (it1.hasNext()) {
        AbstractState abstractState = it1.next();

        ARGState s1 = (ARGState) abstractState;
        LocationState locState = null;
        ApronState apronState = null;
        CompositeState s2 = (CompositeState) (s1).getWrappedState();
        List<AbstractState> stateList2 = s2.getWrappedStates();
        for (int j = 0; j < stateList2.size(); j++) {
          if (stateList2.get(j) instanceof LocationState) {
            locState = (LocationState) stateList2.get(j);
          }
          if (stateList2.get(j) instanceof ApronState) {
            apronState = (ApronState) stateList2.get(j);
          }
        }
        if (locState != null && apronState != null) {
//        initialize loop. edge. state while traverse the reached set
          CFANode locNode = locState.getLocationNode();
          for (int i = 0; i < locNode.getNumEnteringEdges(); ++i) {
            CFAEdge edge = locNode.getEnteringEdge(i);
            if (loopExitEdges.get(edge) != null) {
              exitEdgeToState.put(edge, apronState);
            }
            if (loopEntryEdges.get(edge) != null) {
              entryEdgeToState.put(edge, apronState);
            }
          }
//          loop entry edge
          if (locState.getLocationNode() instanceof FunctionExitNode) {
            functionExitState = locState;
            functionExitApronState = apronState;
          }
        }
      }

//      get all summary instances
      if (functionExitState != null) {
        FunctionExitNode functionExitNode = (FunctionExitNode) functionExitState.getLocationNode();
        if (functionExitNode.getFunctionName().equals(functionName)) {
//          add function summary instance
          Map<SummarySubject, SummaryInstance> result = new HashMap<>();
          ArithFunctionSummaryInstance newInstance = ArithFunctionSummaryInstance
              .from(functionExitNode.getFunctionName(), functionExitApronState);
          if (!newInstance.isEqualTo(pOld)) {
            result.put(pSubject, newInstance);
          }
//          add loop summary instance
          addLoopSummaryInstance(result, exitEdgeToState, entryEdgeToState);
          return result;
        }
      }
    }
    return null;
  }

  /*
   * add loop summary instance, each loop has multiple loop exit edges and loop entry edges
   * each loop exit edge to a list of constraints, same to entry edge
   * the last element of reached set is function exit node
   */
  private void addLoopSummaryInstance(
      Map<SummarySubject, SummaryInstance> result,
      Map<CFAEdge, ApronState> exitEdgeToState, Map<CFAEdge, ApronState> entryEdgeToState) {
    Map<Loop, ArithLoopSummaryInstance> loopToSummary = Maps.newHashMap();
    for (CFAEdge edge : exitEdgeToState.keySet()) {
      Loop loop = loopExitEdges.get(edge);
      ArithLoopSummaryInstance loopInstance = loopToSummary.get(loop);
      if (loopInstance == null) {
        loopInstance = ArithLoopSummaryInstance.of(loop);
        loopInstance.addConstraintsForExitEdge(edge, exitEdgeToState.get(edge));
        loopToSummary.put(loop, loopInstance);
      } else {
        loopInstance.addConstraintsForExitEdge(edge, exitEdgeToState.get(edge));
      }
    }
    for (CFAEdge edge : entryEdgeToState.keySet()) {
      Loop loop = loopEntryEdges.get(edge);
      ArithLoopSummaryInstance loopInstance = loopToSummary.get(loop);
      if (loopInstance == null) {
        loopInstance = ArithLoopSummaryInstance.of(loop);
        loopInstance.addConstraintsForEntryEdge(edge, entryEdgeToState.get(edge));
        loopToSummary.put(loop, loopInstance);
      } else {
        loopInstance.addConstraintsForEntryEdge(edge, entryEdgeToState.get(edge));
      }
    }
    for (Loop loop : loopToSummary.keySet()) {
      LoopSubject loopSubject = LoopSubject.of(loop);
      update(loopSubject, ArithLoopSummaryInstance.of(loopSubject.getLoop()));
      result.put(loopSubject, loopToSummary.get(loop));
    }

  }


  public void getLoopsForFunction(String functionName) {
    ImmutableMap.Builder<CFAEdge, Loop> entryEdges = ImmutableMap.builder();
    ImmutableMap.Builder<CFAEdge, Loop> exitEdges = ImmutableMap.builder();

    for (Loop l : ApronTransferRelation.getLoopStructure().getLoopsForFunction(functionName)) {
      // function edges do not count as incoming/outgoing edges
      Iterable<CFAEdge> incomingEdges = filter(l.getIncomingEdges(),
          not(instanceOf(CFunctionReturnEdge.class)));
      Iterable<CFAEdge> outgoingEdges = filter(l.getOutgoingEdges(),
          not(instanceOf(CFunctionCallEdge.class)));

      for (CFAEdge e : incomingEdges) {
        entryEdges.put(e, l);
      }
      for (CFAEdge e : outgoingEdges) {
        exitEdges.put(e, l);
      }
    }
    loopEntryEdges = entryEdges.build();
    loopExitEdges = exitEdges.build();
  }

  public class CApronPrevActionExpressionVisitor
      extends DefaultCExpressionVisitor<Map<MemoryLocation, CType>, CPATransferException>
      implements CRightHandSideVisitor<Map<MemoryLocation, CType>, CPATransferException> {

    @Override
    protected Map<MemoryLocation, CType> visitDefault(CExpression pExp)
        throws CPATransferException {
      return new HashMap<>();
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
    public Map<MemoryLocation, CType> visit(CFieldReference pIastFieldReference)
        throws CPATransferException {
      if (pIastFieldReference.isPointerDereference()) {
        return new HashMap<>();
      }

      if (!(pIastFieldReference.getFieldOwner() instanceof CLeftHandSide)) {
        return new HashMap<>();
      }

      CLeftHandSide fieldOwner = (CLeftHandSide) pIastFieldReference.getFieldOwner();

      Map<MemoryLocation, CType> memLocOfFieldOwner = fieldOwner.accept(this);

      if (memLocOfFieldOwner.size() == 0) {
        return new HashMap<>();
      }

      Map<MemoryLocation, CType> result = new HashMap<>();
      Iterator<MemoryLocation> it = memLocOfFieldOwner.keySet().iterator();
      while (it.hasNext()) {
        MemoryLocation s = getStructureFieldLocationFromRelativePoint(it.next(),
            pIastFieldReference.getFieldName(),
            fieldOwner.getExpressionType());
        result.put(s, pIastFieldReference.getExpressionType());
      }
      return result;
    }

    @Override
    public Map<MemoryLocation, CType> visit(CBinaryExpression e) throws CPATransferException {
      Map<MemoryLocation, CType> left = e.getOperand1().accept(this);
      Map<MemoryLocation, CType> right = e.getOperand2().accept(this);

      Map<MemoryLocation, CType> returnMemoryLocation = new HashMap<>();
      returnMemoryLocation.putAll(left);
      returnMemoryLocation.putAll(right);
      return returnMemoryLocation;
    }

    /**
     * Only unpack the cast and continue with the casts operand
     */
    @Override
    public Map<MemoryLocation, CType> visit(CCastExpression e) throws CPATransferException {
      return e.getOperand().accept(this);
    }

    @Override
    public Map<MemoryLocation, CType> visit(CIdExpression e) throws CPATransferException {
      String name = e.getDeclaration().getQualifiedName();
      MemoryLocation ml = MemoryLocation.valueOf(name);
      Map<MemoryLocation, CType> returnMemoryLocation = new HashMap<>();
      returnMemoryLocation.put(ml, e.getExpressionType());
      return returnMemoryLocation;
    }

    @Override
    public Map<MemoryLocation, CType> visit(CCharLiteralExpression e) throws CPATransferException {
      return new HashMap<>();
    }

    @Override
    public Map<MemoryLocation, CType> visit(CFloatLiteralExpression e) throws CPATransferException {
      return new HashMap<>();
    }

    @Override
    public Map<MemoryLocation, CType> visit(CIntegerLiteralExpression e)
        throws CPATransferException {
      return new HashMap<>();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Map<MemoryLocation, CType> visit(CUnaryExpression e) throws CPATransferException {
      return e.getOperand().accept(this);
    }

    @Override
    public Map<MemoryLocation, CType> visit(CFunctionCallExpression e) throws CPATransferException {
      List<CExpression> paramExpressions = e.getParameterExpressions();
      Map<MemoryLocation, CType> returnMemoryLocation = new HashMap<>();
      for (int i = 0; i < paramExpressions.size(); i++) {
        Map<MemoryLocation, CType> tempMemoryLocation = paramExpressions.get(i).accept(this);
        returnMemoryLocation.putAll(tempMemoryLocation);
      }
      return returnMemoryLocation;
    }
  }

  private void calculateUsedVars(CFA cfa) {
    for (CFANode node : cfa.getAllNodes()) {
      int edgeNum = node.getNumLeavingEdges();
      for (int j = 0; j < edgeNum; j++) {
        CFAEdge edge = node.getLeavingEdge(j);
        if (edge instanceof CStatementEdge) {
          calculateCStatementEdgeUsedVars((CStatementEdge) edge);
        } else if (edge instanceof CDeclarationEdge) {
          calculateCDeclarationEdgeUsedVars((CDeclarationEdge) edge);
        } else if (edge instanceof CAssumeEdge) {
          calculateCAssumeEdgeUsedVars((CAssumeEdge) edge);
        }
      }
    }
  }

  private void calculateCStatementEdgeUsedVars(CStatementEdge cfaEdge) {
    try {
      Map<MemoryLocation, CType> returnMemoryLocation = new HashMap<>();
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

      String functionName = cfaEdge.getPredecessor().getFunctionName();
      if (usedVars.get(functionName) == null) {
        usedVars.put(functionName, returnMemoryLocation);
      } else {
        usedVars.get(functionName).putAll(returnMemoryLocation);
      }
    } catch (CPATransferException e) {
      e.printStackTrace();
    }
  }

  private void calculateCDeclarationEdgeUsedVars(CDeclarationEdge cfaEdge) {
    try {
      Map<MemoryLocation, CType> returnMemoryLocation = new HashMap<>();
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
      String functionName = cfaEdge.getPredecessor().getFunctionName();
      if (usedVars.get(functionName) == null) {
        usedVars.put(functionName, returnMemoryLocation);
      } else {
        usedVars.get(functionName).putAll(returnMemoryLocation);
        ;
      }
    } catch (CPATransferException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void calculateCAssumeEdgeUsedVars(CAssumeEdge cfaEdge) {
    try {
      Map<MemoryLocation, CType> returnMemoryLocation = new HashMap<>();
      CExpression expression = cfaEdge.getExpression();
      if (expression instanceof CLiteralExpression) {
        //do nothing
      } else {
        returnMemoryLocation = expression.accept(new CApronPrevActionExpressionVisitor());
      }
      String functionName = cfaEdge.getPredecessor().getFunctionName();
      if (usedVars.get(functionName) == null) {
        usedVars.put(functionName, returnMemoryLocation);
      } else {
        usedVars.get(functionName).putAll(returnMemoryLocation);
      }
    } catch (CPATransferException e) {
      e.printStackTrace();
    }
  }

  public static HashMap<String, Map<MemoryLocation, CType>> getUsedVars() {
    return usedVars;
  }

}
