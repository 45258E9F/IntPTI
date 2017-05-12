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
package org.sosy_lab.cpachecker.cpa.pointer2;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.pointer2.summary.PointerFunctionSummary;
import org.sosy_lab.cpachecker.cpa.pointer2.summary.PointerLoopSummary;
import org.sosy_lab.cpachecker.cpa.pointer2.summary.visitor.PointerExpressionVisitor;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetBot;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

public class PointerTransferRelation extends SingleEdgeTransferRelation {

  private static PointerExpressionVisitor visitor;
  private static MachineModel machineModel;
  private static LoopStructure loopStructure;
  private Map<CFAEdge, Loop> loopEntryEdges = null;
  private Map<CFAEdge, Loop> loopExitEdges = null;

  public PointerTransferRelation() throws InvalidConfigurationException {
    CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().orNull();
    if (cfaInfo == null) {
      throw new IllegalArgumentException("CFA info required for creating pointer transfer "
          + "relation");
    }
    machineModel = cfaInfo.getCFA().getMachineModel();
    Optional<LoopStructure> loopStruct = cfaInfo.getCFA().getLoopStructure();
    if (!loopStruct.isPresent()) {
      throw new IllegalArgumentException("Loop structure required for loop summary computation");
    }
    loopStructure = loopStruct.get();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, List<AbstractState> otherStates, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    PointerState pointerState = (PointerState) pState;
    PointerState resultState = getAbstractSuccessor(pointerState, pPrecision, pCfaEdge);
    return resultState == null ? Collections.<AbstractState>emptySet()
                               : Collections.<AbstractState>singleton(resultState);
  }

  public void setLoopSummary(PointerState pState, CFAEdge pCFAEdge) {
    if (loopEntryEdges == null) {
      return;
    }
    Loop newLoop = loopEntryEdges.get(pCFAEdge);
    if (newLoop != null) {
      if (!pState.getLoopSummaries().containsKey(newLoop)) {
        Stack<Loop> stack;
        if (pState.getCurLoop() == null) {
          stack = new Stack<>();
        } else {
          stack = pState.getCurLoopSummary().getStack();
        }
        PointerLoopSummary loopSummary = new PointerLoopSummary(stack, newLoop);
        pState.getLoopSummaries().put(newLoop, loopSummary);
      }
      pState.setCurLoop(newLoop);
    }
    Loop oldLoop = loopExitEdges.get(pCFAEdge);
    if (oldLoop != null) {
      Loop curLoop = pState.getCurLoop();
      if (curLoop != null) {
        Stack<Loop> curStack = pState.getCurLoopSummary().getStack();
        int size = curStack.size();
        if (curStack.elementAt(size - 1).equals(curLoop)) {
          Loop loop = null;
          if (size >= 2) {
            loop = curStack.elementAt(curStack.size() - 2);
          }
          pState.setCurLoop(loop);
        }
      }
    }
  }

  public void getLoopsForFunction(String functionName) {
    ImmutableMap.Builder<CFAEdge, Loop> entryEdges = ImmutableMap.builder();
    ImmutableMap.Builder<CFAEdge, Loop> exitEdges = ImmutableMap.builder();

    for (Loop l : loopStructure.getLoopsForFunction(functionName)) {
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

  public PointerState initialize(PointerState pState, CFAEdge pCfaEdge) {
    pState.clear();
    getLoopsForFunction(pCfaEdge.getSuccessor().getFunctionName());
    CFANode entry = pCfaEdge.getPredecessor();
    while (!(entry instanceof FunctionEntryNode)) {
      entry = entry.getEnteringEdge(0).getPredecessor();
    }
    pState.setCurFunctionSummary(new PointerFunctionSummary(pCfaEdge.getSuccessor()));
    pState = initParas(pState, pCfaEdge);
    visitor = new PointerExpressionVisitor(pCfaEdge.getSuccessor().getFunctionName(),
        machineModel);
    return pState;
  }

  private PointerState getAbstractSuccessor(
      PointerState pState, Precision pPrecision,
      CFAEdge pCfaEdge) throws CPATransferException {
    PointerState resultState = pState;
    setLoopSummary(pState, pCfaEdge);
    switch (pCfaEdge.getEdgeType()) {
      case AssumeEdge:
        resultState = pState.copyOf();
        break;
      case BlankEdge:
        if (pCfaEdge.toString().contains("default return")) {
          resultState = handleSummary(pState, pCfaEdge);
          //return null;
        } else if (pCfaEdge.toString().contains("Function start dummy edge")) {
          resultState = initialize(pState, pCfaEdge);
        }
        break;
      case CallToReturnEdge:
        break;
      case DeclarationEdge:
        resultState = handleDeclarationEdge(pState, (CDeclarationEdge) pCfaEdge);
        break;
      case FunctionCallEdge:
        return null;
      case FunctionReturnEdge:
        break;
      case MultiEdge:
        for (CFAEdge edge : ((MultiEdge) pCfaEdge)) {
          resultState = getAbstractSuccessor(resultState, pPrecision, edge);
        }
        break;
      case ReturnStatementEdge:
        resultState = handleSummary(pState, pCfaEdge);
        //return null;
        resultState = handleReturnStatementEdge(resultState, (CReturnStatementEdge) pCfaEdge);
        break;
      case StatementEdge:
        resultState = handleStatementEdge(pState, (CStatementEdge) pCfaEdge);
        break;
      default:
        throw new UnrecognizedCCodeException("Unrecognized CFA edge.", pCfaEdge);
    }
    return resultState;
  }

  private LocationSet getGlobalLocationSet(LocationSet locationSet) {
    if (!(locationSet instanceof ExplicitLocationSet)) {
      return locationSet;
    }
    Set<MemoryLocation> newLocMem = new HashSet<>();
    for (MemoryLocation locMem : ((ExplicitLocationSet) locationSet)) {
      if (!locMem.isOnFunctionStack()) {
        newLocMem.add(locMem);
      }
    }
    return ExplicitLocationSet.from(newLocMem);
  }

  private void handleGlobals(PointerState pState, PointerFunctionSummary pFunctionSummary) {
    for (MemoryLocation mem : pState.getPointsToMap().keySet()) {
      if (!mem.isOnFunctionStack()) {
        LocationSet locationSet = pState.getPointsToMap().get(mem);
        pFunctionSummary.addChangedGlobal(mem, getGlobalLocationSet(locationSet));
      }
    }
  }

  private PointerState initParas(PointerState pState, CFAEdge pCFAEdge) {
    PointerState newState = pState;
    CFunctionEntryNode entryNode;
    while (!(pCFAEdge.getPredecessor() instanceof CFunctionEntryNode)) {
      pCFAEdge = pCFAEdge.getPredecessor().getEnteringEdge(0);
    }
    entryNode = (CFunctionEntryNode) pCFAEdge.getPredecessor();
    List<CParameterDeclaration> formalParams = entryNode.getFunctionParameters();
    int limit = formalParams.size();
    formalParams = FluentIterable.from(formalParams).limit(limit).toList();

    for (int i = 0; i < limit; i++) {
      CParameterDeclaration formalParam = formalParams.get(i);
      if (formalParam.getType() instanceof CPointerType) {
        pState.getPointerParamNames().put(formalParam.getName(), i);
        MemoryLocation location = toLocation(formalParam);
        newState = handleAssignment(pState, location, LocationSetTop.INSTANCE);
      }
    }

    return newState;
  }

  private void handleReturned(
      PointerState pState, CReturnStatementEdge pEdge, PointerFunctionSummary
      pFunctionSummary) throws UnrecognizedCCodeException {
    if (pEdge.getExpression().isPresent()) {
      LocationSet locationSet = getGlobalLocationSet(asLocations(pEdge.getExpression().get(),
          pState, 1));
      pFunctionSummary.setReturnedSet(locationSet);
    }
  }

  private PointerState handleSummary(PointerState pState, CFAEdge pEdge)
      throws UnrecognizedCCodeException {
    PointerFunctionSummary curFunctionSummary = pState.getCurFunctionSummary();
    handleGlobals(pState, curFunctionSummary);
    if (pEdge instanceof CReturnStatementEdge) {
      handleReturned(pState, (CReturnStatementEdge) pEdge, curFunctionSummary);
    }
    return pState;
  }

  private PointerState handleReturnStatementEdge(PointerState pState, CReturnStatementEdge pCfaEdge)
      throws UnrecognizedCCodeException {
    if (!pCfaEdge.getExpression().isPresent()) {
      return pState;
    }
    Optional<? extends AVariableDeclaration> returnVariable =
        pCfaEdge.getSuccessor().getEntryNode().getReturnVariable();
    if (!returnVariable.isPresent()) {
      return pState;
    }
    return handleAssignment(pState,
        MemoryLocation.valueOf(returnVariable.get().getQualifiedName()),
        pCfaEdge.getExpression().get());
  }

  private MemoryLocation toLocation(AbstractSimpleDeclaration pDeclaration) {
    return MemoryLocation.valueOf(pDeclaration.getQualifiedName());
  }

  public void addCalledFunction(PointerState pState, CFAEdge calledFunction) {
    pState.getCurFunctionSummary().addCalledFunctions(calledFunction);
    if (pState.getCurLoop() != null) {
      Stack<Loop> curLoopStack = pState.getCurLoopSummary().getStack();
      for (int i = 0; i < curLoopStack.size(); i++) {
        Loop loop = curLoopStack.get(i);
        pState.getLoopSummaries().get(loop).addCalledFunction(calledFunction);
      }
    }
  }

  private PointerState handleStatementEdge(PointerState pState, CStatementEdge pCfaEdge)
      throws UnrecognizedCCodeException {
    if (pCfaEdge.getStatement() instanceof CAssignment) {
      CAssignment assignment = (CAssignment) pCfaEdge.getStatement();
      if (assignment.getRightHandSide() instanceof CFunctionCallExpression) {
        addCalledFunction(pState, pCfaEdge);
      }
      return handleAssignment(pState, assignment.getLeftHandSide(), assignment.getRightHandSide());
    } else if (pCfaEdge instanceof CFunctionSummaryStatementEdge) {
      addCalledFunction(pState, pCfaEdge);
    }
    return pState;
  }

  private PointerState handleAssignment(
      PointerState pState,
      CExpression pLeftHandSide,
      CRightHandSide pRightHandSide) throws UnrecognizedCCodeException {
    LocationSet locations = asLocations(pLeftHandSide, pState, 0);
    return handleAssignment(pState, locations, pRightHandSide);
  }

  private PointerState handleAssignment(
      PointerState pState,
      LocationSet pLocationSet,
      CRightHandSide pRightHandSide) throws UnrecognizedCCodeException {
    final Iterable<MemoryLocation> locations;
    if (pLocationSet.isTop()) {
      locations = pState.getKnownLocations();
    } else if (pLocationSet instanceof ExplicitLocationSet) {
      locations = (ExplicitLocationSet) pLocationSet;
    } else {
      locations = Collections.<MemoryLocation>emptySet();
    }
    PointerState result = pState;
    for (MemoryLocation location : locations) {
      result = handleAssignment(result, location, pRightHandSide);
    }
    return result;
  }

  public void addChangedParas(
      PointerState pState,
      MemoryLocation pMemoryLocation,
      LocationSet pLocationSet) {
    if (pMemoryLocation.isOnFunctionStack() &&
        pState.getPointerParamNames().containsKey(pMemoryLocation.getIdentifier())) {
      pState.getCurFunctionSummary().addChangedFormalPointerParas(
          pState.getPointerParamNames().get(pMemoryLocation.getIdentifier()), pLocationSet);
    }
  }

  public void setChangedVarsInLoop(
      PointerState pState, MemoryLocation pMemoryLocation,
      LocationSet pLocationSet) {
    if (pState.getCurLoop() != null) {
      Stack<Loop> curLoopStack = pState.getCurLoopSummary().getStack();
      for (int i = 0; i < curLoopStack.size(); i++) {
        Loop loop = curLoopStack.get(i);
        pState.getLoopSummaries().get(loop).addChangedVars(pMemoryLocation, pLocationSet);
      }
    }
  }

  private PointerState handleAssignment(
      PointerState pState,
      MemoryLocation pLhsLocation,
      CRightHandSide pRightHandSide) throws UnrecognizedCCodeException {
    LocationSet rightHandSide = asLocations(pRightHandSide, pState, 1);
    if (!(rightHandSide instanceof LocationSetBot)) {
      addChangedParas(pState, pLhsLocation, rightHandSide);
      setChangedVarsInLoop(pState, pLhsLocation, rightHandSide);
    }
    return pState.addPointsToInformation(pLhsLocation, rightHandSide);
  }

  private PointerState handleAssignment(
      PointerState pState,
      MemoryLocation pLeftHandSide,
      LocationSet pRightHandSide) {
    if (!(pRightHandSide instanceof LocationSetBot)) {
      addChangedParas(pState, pLeftHandSide, pRightHandSide);
      setChangedVarsInLoop(pState, pLeftHandSide, pRightHandSide);
    }
    return pState.addPointsToInformation(pLeftHandSide, pRightHandSide);
  }

  private PointerState handleDeclarationEdge(
      final PointerState pState,
      final CDeclarationEdge pCfaEdge) throws UnrecognizedCCodeException {
    if (!(pCfaEdge.getDeclaration() instanceof CVariableDeclaration)) {
      return pState;
    }
    CVariableDeclaration declaration = (CVariableDeclaration) pCfaEdge.getDeclaration();
    CInitializer initializer = declaration.getInitializer();
    PointerState newState = pState;
    MemoryLocation location = toLocation(declaration);
    LocationSet rhs = null;
    if (initializer != null) {
      rhs = initializer.accept(new CInitializerVisitor<LocationSet, UnrecognizedCCodeException>() {

        @Override
        public LocationSet visit(CInitializerExpression pInitializerExpression)
            throws UnrecognizedCCodeException {
          return asLocations(pInitializerExpression.getExpression(), pState, 1);
        }

        //TODO, these two situations can be handled more precise -> partly resolved
        @Override
        public LocationSet visit(CInitializerList pInitializerList)
            throws UnrecognizedCCodeException {
          if (pInitializerList.getInitializers().size() != 0) {
            return LocationSetTop.INSTANCE;
          }
          return LocationSetBot.INSTANCE;
        }

        @Override
        public LocationSet visit(CDesignatedInitializer pCStructInitializerPart)
            throws UnrecognizedCCodeException {
          if (pCStructInitializerPart.getDesignators().size() != 0) {
            return LocationSetTop.INSTANCE;
          }
          return LocationSetBot.INSTANCE;
        }

      });

      newState = handleAssignment(pState, location, rhs);
    }
    if (declaration.getType() instanceof CArrayType) {
      MemoryLocation pointsTo;
      String name = location.getIdentifier();
      if (location.isOnFunctionStack()) {
        pointsTo = MemoryLocation.valueOf(location.getFunctionName(), name, 0);
      } else {
        pointsTo = MemoryLocation.valueOf(name, 0);
      }
      LocationSet locationSet = ExplicitLocationSet.from(pointsTo);
      newState = handleAssignment(newState, location, locationSet);
    }
    return newState;
  }

  private static LocationSet toLocationSet(Iterable<? extends MemoryLocation> pLocations) {
    if (pLocations == null) {
      return LocationSetTop.INSTANCE;
    }
    Iterator<? extends MemoryLocation> locationIterator = pLocations.iterator();
    if (!locationIterator.hasNext()) {
      return LocationSetBot.INSTANCE;
    }
    return ExplicitLocationSet.from(pLocations);
  }

  public static LocationSet asLocations(
      final CRightHandSide pExpression, final PointerState
      pState, final int pDerefCounter) throws UnrecognizedCCodeException {
    return pExpression.accept(new CRightHandSideVisitor<LocationSet, UnrecognizedCCodeException>() {

      //TODO, now it can handle only arrays whose subscript is 0 -> resolved
      //TODO, if subscript is a symbolic variable (lefthandside or right) -> resolved
      @Override
      public LocationSet visit(CArraySubscriptExpression pIastArraySubscriptExpression)
          throws UnrecognizedCCodeException {
        if (pIastArraySubscriptExpression.getSubscriptExpression() instanceof CLiteralExpression) {
          CLiteralExpression literal =
              (CLiteralExpression) pIastArraySubscriptExpression.getSubscriptExpression();
          if (literal instanceof CIntegerLiteralExpression) {
            //TODO, array[1], if array is pointer type, we should return all the possible targets
            MemoryLocation location = pIastArraySubscriptExpression.accept(visitor);
            if (location == null) {
              return LocationSetTop.INSTANCE;
            }
            return visit(location);
          }
          return LocationSetTop.INSTANCE;
        } else {
          MemoryLocation expMem = pIastArraySubscriptExpression.getArrayExpression().accept
              (visitor);
          if (expMem == null) {
            return LocationSetTop.INSTANCE;
          }
          Set<MemoryLocation> result = new HashSet<>();
          final Iterable<MemoryLocation> locations;
          locations = pState.getKnownLocations();
          for (MemoryLocation location : locations) {
            if (expMem.isOnFunctionStack() == location.isOnFunctionStack() &&
                Objects.equals(expMem.getIdentifier(), location.getIdentifier()) &&
                location.isReference()) {
              LocationSet temp = visit(location);
              if (temp instanceof LocationSetTop) {
                return LocationSetTop.INSTANCE;
              }
              if (temp instanceof ExplicitLocationSet) {
                result.addAll(((ExplicitLocationSet) temp).getExplicitSet());
              }
            }
          }
          return toLocationSet(result);
        }
      }

      @Override
      public LocationSet visit(final CFieldReference pIastFieldReference)
          throws UnrecognizedCCodeException {
        //return toLocationSet(Collections.singleton(pIastFieldReference.accept(visitor)));
        MemoryLocation location = pIastFieldReference.accept(visitor);
        if (location == null) {
          return LocationSetTop.INSTANCE;
        }
        return visit(location);
      }

      @Override
      public LocationSet visit(CIdExpression pIastIdExpression) throws UnrecognizedCCodeException {
        Type type = pIastIdExpression.getExpressionType();
        final MemoryLocation location;
        CSimpleDeclaration declaration = pIastIdExpression.getDeclaration();
        if (declaration != null) {
          location = MemoryLocation.valueOf(declaration.getQualifiedName());
        } else {
          location = MemoryLocation.valueOf(pIastIdExpression.getName());
        }
        return visit(location);
      }

      private LocationSet visit(MemoryLocation pLocation) {
        Collection<MemoryLocation> result = Collections.singleton(pLocation);
        for (int deref = pDerefCounter; deref > 0 && !result.isEmpty(); --deref) {
          Collection<MemoryLocation> newResult = new HashSet<>();
          for (MemoryLocation location : result) {
            LocationSet targets = pState.getPointsToSet(location);
            if (targets.isBot()) {
              if (location.toString().contains("/")) {
                if (location.isOnFunctionStack()) {
                  location = MemoryLocation.valueOf(location.getFunctionName(), location
                      .getIdentifier());
                } else {
                  location = MemoryLocation.valueOf(location.getIdentifier());
                }
              }
              targets = pState.getPointsToSet(location);
            }
            if (targets.isTop() || targets.isBot()) {
              return targets;
            }
            if (!(targets instanceof ExplicitLocationSet)) {
              return LocationSetTop.INSTANCE;
            }
            Iterables.addAll(newResult, ((ExplicitLocationSet) targets));
          }
          result = newResult;
        }
        return toLocationSet(result);
      }

      @Override
      public LocationSet visit(CPointerExpression pPointerExpression)
          throws UnrecognizedCCodeException {
        return asLocations(pPointerExpression.getOperand(), pState, pDerefCounter + 1);
      }

      @Override
      public LocationSet visit(CComplexCastExpression pComplexCastExpression)
          throws UnrecognizedCCodeException {
        return asLocations(pComplexCastExpression.getOperand(), pState, pDerefCounter);
      }

      @Override
      public LocationSet visit(CBinaryExpression pIastBinaryExpression)
          throws UnrecognizedCCodeException {
        return toLocationSet(Iterables.concat(
            toNormalSet(pState,
                asLocations(pIastBinaryExpression.getOperand1(), pState, pDerefCounter)),
            toNormalSet(pState,
                asLocations(pIastBinaryExpression.getOperand2(), pState, pDerefCounter))));
      }

      @Override
      public LocationSet visit(CCastExpression pIastCastExpression)
          throws UnrecognizedCCodeException {
        return asLocations(pIastCastExpression.getOperand(), pState, pDerefCounter);
      }

      @Override
      public LocationSet visit(CCharLiteralExpression pIastCharLiteralExpression)
          throws UnrecognizedCCodeException {
        return LocationSetBot.INSTANCE;
      }

      @Override
      public LocationSet visit(CFloatLiteralExpression pIastFloatLiteralExpression)
          throws UnrecognizedCCodeException {
        return LocationSetBot.INSTANCE;
      }

      @Override
      public LocationSet visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
          throws UnrecognizedCCodeException {
        return LocationSetBot.INSTANCE;
      }

      @Override
      public LocationSet visit(CStringLiteralExpression pIastStringLiteralExpression)
          throws UnrecognizedCCodeException {
        return LocationSetBot.INSTANCE;
      }

      @Override
      public LocationSet visit(CTypeIdExpression pIastTypeIdExpression)
          throws UnrecognizedCCodeException {
        return LocationSetBot.INSTANCE;
      }

      @Override
      public LocationSet visit(CUnaryExpression pIastUnaryExpression)
          throws UnrecognizedCCodeException {
        if (pDerefCounter > 0 && pIastUnaryExpression.getOperator() == UnaryOperator.AMPER) {
          return asLocations(pIastUnaryExpression.getOperand(), pState, pDerefCounter - 1);
        }
        return LocationSetBot.INSTANCE;
      }

      @Override
      public LocationSet visit(CImaginaryLiteralExpression PIastLiteralExpression)
          throws UnrecognizedCCodeException {
        return LocationSetBot.INSTANCE;
      }

      @Override
      public LocationSet visit(CFunctionCallExpression pIastFunctionCallExpression)
          throws UnrecognizedCCodeException {
        CFunctionDeclaration declaration = pIastFunctionCallExpression.getDeclaration();
        if (declaration == null) {
          LocationSet result = pIastFunctionCallExpression.getFunctionNameExpression().accept(this);
          if (result.isTop() || result.isBot()) {
            return result;
          }
          return toLocationSet(
              FluentIterable.from(toNormalSet(pState, result)).filter(Predicates.notNull()));
        }
        return visit(MemoryLocation.valueOf(declaration.getQualifiedName()));
      }

      @Override
      public LocationSet visit(CAddressOfLabelExpression pAddressOfLabelExpression)
          throws UnrecognizedCCodeException {
        throw new UnrecognizedCCodeException("Address of labels not supported by pointer analysis",
            pAddressOfLabelExpression);
      }
    });
  }

  /**
   * Gets the set of possible locations of the given expression. For the expression 'x', the
   * location is the identifier x. For the expression 's.a' the location is the identifier
   * t.a, where t is the type of s. For the expression '*p', the possible locations are the
   * points-to set of locations the expression 'p'.
   */
  public static LocationSet asLocations(CExpression pExpression, final PointerState pState)
      throws UnrecognizedCCodeException {
    return asLocations(pExpression, pState, 0);
  }

  /**
   * Gets the locations represented by the given location set considering the
   * context of the given state. The returned iterable is guaranteed to be free
   * of duplicates.
   *
   * @param pState       the context.
   * @param pLocationSet the location set.
   * @return the locations represented by the given location set.
   */
  public static Iterable<MemoryLocation> toNormalSet(
      PointerState pState,
      LocationSet pLocationSet) {
    if (pLocationSet.isBot()) {
      return Collections.emptySet();
    }
    if (pLocationSet.isTop() || !(pLocationSet instanceof ExplicitLocationSet)) {
      return pState.getKnownLocations();
    }
    return (ExplicitLocationSet) pLocationSet;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return Collections.singleton(pState);
  }

}
