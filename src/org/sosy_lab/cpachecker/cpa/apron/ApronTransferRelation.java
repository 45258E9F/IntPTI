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
package org.sosy_lab.cpachecker.cpa.apron;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.algorithm.summary.computer.ArithSummaryComputer;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.apron.ApronState.Type;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import apron.DoubleScalar;
import apron.Interval;
import apron.Linexpr0;
import apron.Linterm0;
import apron.Scalar;
import apron.Tcons0;
import apron.Texpr0BinNode;
import apron.Texpr0CstNode;
import apron.Texpr0DimNode;
import apron.Texpr0Intern;
import apron.Texpr0Node;
import apron.Texpr0UnNode;

public class ApronTransferRelation extends
                                   ForwardingTransferRelation<Collection<ApronState>, ApronState, VariableTrackingPrecision> {

  /**
   * This is used for making smaller and greater constraint with octagons
   */
  private static final Texpr0CstNode constantMin =
      new Texpr0CstNode(new DoubleScalar(0.00000000000001));

  /**
   * set of functions that may not appear in the source code
   * the value of the map entry is the explanation for the user
   */
  private static final Map<String, String> UNSUPPORTED_FUNCTIONS
      = ImmutableMap.of();

  protected final LogManager logger;
  private final boolean splitDisequalities;

  protected final Set<CFANode> loopHeads;

  protected HashMap<MemoryLocation, List<MemoryLocation>> map;

  private static MachineModel machineModel;

  //  HARRY: getLoopStructure
  private static LoopStructure loopStructure;


  public ApronTransferRelation(LogManager log, LoopStructure loops, boolean pSplitDisequalities) {
    loopStructure = loops;
    logger = log;
    splitDisequalities = pSplitDisequalities;
    map = new HashMap<MemoryLocation, List<MemoryLocation>>();
    machineModel = GlobalInfo.getInstance().getCFAInfo().get().getCFA().getMachineModel();

    Builder<CFANode> builder = new ImmutableSet.Builder<>();
    for (Loop l : loops.getAllLoops()) {
      // function edges do not count as incoming/outgoing edges
      builder.addAll(l.getLoopHeads());
    }
    loopHeads = builder.build();

    // TODO the creation of the additional ApronManager which then is never used
    // should not be necessary, however, without this constructor call the library
    // does not work properly
    try {
      new ApronManager(Configuration.defaultConfiguration());
    } catch (InvalidConfigurationException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  protected Collection<ApronState> postProcessing(Collection<ApronState> successors) {

    successors.removeAll(Collections.singleton(null));

    // remove all states whose constraints cannot be satisfied
    Iterator<ApronState> states = successors.iterator();
    while (states.hasNext()) {
      ApronState st = states.next();
      if (st.isEmpty()) {
        states.remove();
        logger.log(Level.FINER, "removing state because of unsatisfiable constraints:\n" +
            st + "________________\nEdge was:\n" + edge.getDescription());
      }
    }

    if (loopHeads.contains(edge.getSuccessor())) {
      Set<ApronState> newStates = new HashSet<>();
      for (ApronState s : successors) {
        newStates.add(s.asLoopHead());
      }
      return newStates;
    } else {
      return new HashSet<>(successors);
    }
  }

  @Override
  protected Collection<ApronState> handleMultiEdge(MultiEdge cfaEdge)
      throws CPATransferException {
    return super.handleMultiEdgeReturningCollection(cfaEdge);
  }

  @Override
  protected Set<ApronState> handleBlankEdge(BlankEdge cfaEdge) {
    if (cfaEdge.getPredecessor() instanceof FunctionEntryNode) {
      FunctionEntryNode funcEntryNode = (FunctionEntryNode) cfaEdge.getPredecessor();
      List<? extends AParameterDeclaration> dec =
          funcEntryNode.getFunctionDefinition().getParameters();
      MemoryLocation variableName;
      if (ArithSummaryComputer.getUsedVars().get(functionName) == null) {
        return Collections.singleton(state);
      }
      Set<MemoryLocation> funcUsedVarSet =
          ArithSummaryComputer.getUsedVars().get(functionName).keySet();
      funcUsedVarSet = funcUsedVarSet == null ? Collections.EMPTY_SET : funcUsedVarSet;
      for (int i = 0; i < dec.size(); i++) {
        variableName = MemoryLocation.valueOf(functionName, dec.get(i).getName());
        if (!state.existsVariable(variableName) && (funcUsedVarSet.contains(variableName) ||
            variableName.isReference())) {
          if ((CType) dec.get(i).getType() instanceof CSimpleType) {
            state = state.declareVariable(variableName,
                getCorrespondingOctStateType((CType) dec.get(i).getType()),
                (CType) dec.get(i).getType());
          }
        }
      }
      for (MemoryLocation ml : funcUsedVarSet) {
        if (!state.existsVariable(ml)) {
          if (!ml.toString().contains("*") && !ml.toString().contains("->")) {
            if (ArithSummaryComputer.getUsedVars().get(functionName)
                .get(ml) instanceof CSimpleType) {
              state = state.declareVariable(ml, getCorrespondingOctStateType(
                  ArithSummaryComputer.getUsedVars().get(functionName).get(ml)),
                  ArithSummaryComputer.getUsedVars().get(functionName).get(ml));
            }
          }
        }
      }
    }
    return Collections.singleton(state);
  }

  @SuppressWarnings("deprecation")
  @Override
  protected Set<ApronState> handleAssumption(
      CAssumeEdge cfaEdge,
      CExpression expression,
      boolean truthAssumption)
      throws CPATransferException {

    if (expression instanceof CLiteralExpression) {
      if (expression instanceof CIntegerLiteralExpression) {
        return handleLiteralBooleanExpression(((CIntegerLiteralExpression) expression).asLong(),
            truthAssumption, state);

      } else if (expression instanceof CCharLiteralExpression) {
        return handleLiteralBooleanExpression(((CCharLiteralExpression) expression).getCharacter(),
            truthAssumption, state);

      } else if (expression instanceof CFloatLiteralExpression) {
        // only when the float is exactly zero the condition is wrong, for all other float values it is true
        int val = Math.abs(((CFloatLiteralExpression) expression).getValue().signum());
        return handleLiteralBooleanExpression(val, truthAssumption, state);
      } else {
        return Collections.singleton(state);
      }

    } else if (expression instanceof CBinaryExpression) {
      return handleBinaryAssumption(expression, truthAssumption);

    } else {
      Set<Texpr0Node> coeffs = expression.accept(new CApronExpressionVisitor());
      if (coeffs.isEmpty()) {
        state.setTop();
        return Collections.singleton(state);
        //return Collections.EMPTY_SET;
      }
      Set<ApronState> possibleStates = new HashSet<>();
      for (Texpr0Node coeff : coeffs) {
        if (truthAssumption) {
          possibleStates.add(state.addConstraint(new Tcons0(Tcons0.EQ, coeff)));
        } else {
          possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP, coeff)));
          possibleStates.add(state
              .addConstraint(new Tcons0(Tcons0.SUP, new Texpr0UnNode(Texpr0UnNode.OP_NEG, coeff))));
        }
      }
      return possibleStates;
    }
  }

  protected Set<ApronState> handleBinaryAssumption(CExpression expression, boolean truthAssumption)
      throws CPATransferException {
    CBinaryExpression binExp = (CBinaryExpression) expression;

    Double leftVal = binExp.getOperand1().accept(new CLiteralExpressionVisitor());
    Double rightVal = binExp.getOperand2().accept(new CLiteralExpressionVisitor());
    if (leftVal != null && rightVal != null) {
      return handleLiteralBinExpAssumption(leftVal, rightVal, binExp.getOperator(),
          truthAssumption);
    }
    Set<Texpr0Node> leftCoeffs = binExp.getOperand1().accept(new CApronExpressionVisitor());
    Set<Texpr0Node> rightCoeffs = binExp.getOperand2().accept(new CApronExpressionVisitor());

    if (leftCoeffs.isEmpty() || rightCoeffs.isEmpty()) {
      state.setTop();
      return Collections.singleton(state);
      //return Collections.EMPTY_SET;
    }

    Set<ApronState> possibleStates = new HashSet<>();
    for (Texpr0Node left : leftCoeffs) {
      for (Texpr0Node right : rightCoeffs) {
        switch (binExp.getOperator()) {
          case BINARY_AND:
          case BINARY_OR:
          case BINARY_XOR:
          case SHIFT_RIGHT:
          case SHIFT_LEFT:
            return Collections.singleton(state);

          case EQUALS: {
            if (truthAssumption) {
              possibleStates.add(state.addConstraint(new Tcons0(Tcons0.EQ,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      left,
                      right)))));
            } else {
              if (left instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode) left).dim)
                  || right instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode) right).dim)) {
                Texpr0BinNode increasedRight =
                    new Texpr0BinNode(Texpr0BinNode.OP_ADD, right, constantMin);
                Texpr0BinNode increasedLeft =
                    new Texpr0BinNode(Texpr0BinNode.OP_ADD, left, constantMin);

                possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                    new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                        left,
                        increasedRight)))));
                possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                    new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                        right,
                        increasedLeft)))));
              } else {

                if (splitDisequalities) {
                  // use same trick as in octagon analysis since disequality does not seem to work
                  possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                      new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                          left,
                          right)))));
                  possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                      new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                          right,
                          left)))));
                } else {
                  possibleStates.add(state.addConstraint(new Tcons0(Tcons0.DISEQ,
                      new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                          left,
                          right)))));
                }
              }
            }
            break;
          }
          case GREATER_EQUAL: {
            if (truthAssumption) {
              Tcons0 act = new Tcons0(Tcons0.SUPEQ,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      left,
                      right)));
              possibleStates.add(state.addConstraint(act));
            } else {
              Tcons0 act = new Tcons0(Tcons0.SUP,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      right,
                      left)));
              possibleStates.add(state.addConstraint(act));
            }
            break;
          }
          case GREATER_THAN: {
            if (truthAssumption) {
              Tcons0 act;
              if (left instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode) left).dim)
                  || right instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode) right).dim)) {
                Texpr0BinNode increasedRight =
                    new Texpr0BinNode(Texpr0BinNode.OP_ADD, right, constantMin);
                act = new Tcons0(Tcons0.SUP,
                    new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                        left,
                        increasedRight)));
              } else {
                act = new Tcons0(Tcons0.SUP,
                    new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                        left,
                        right)));
              }

              possibleStates.add(state.addConstraint(act));
            } else {
              Tcons0 act = new Tcons0(Tcons0.SUPEQ,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      right,
                      left)));
              possibleStates.add(state.addConstraint(act));
            }
            break;
          }
          case LESS_EQUAL: {
            if (truthAssumption) {
              Tcons0 act = new Tcons0(Tcons0.SUPEQ,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      right,
                      left)));
              possibleStates.add(state.addConstraint(act));
            } else {
              Tcons0 act = new Tcons0(Tcons0.SUP,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      left,
                      right)));
              possibleStates.add(state.addConstraint(act));
            }
            break;
          }
          case LESS_THAN: {
            if (truthAssumption) {
              Tcons0 act;
              if (left instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode) left).dim)
                  || right instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode) right).dim)) {
                Texpr0BinNode increasedLeft =
                    new Texpr0BinNode(Texpr0BinNode.OP_ADD, left, constantMin);
                act = new Tcons0(Tcons0.SUP,
                    new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                        right,
                        increasedLeft)));
              } else {
                act = new Tcons0(Tcons0.SUP,
                    new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                        right,
                        left)));
              }

              possibleStates.add(state.addConstraint(act));
            } else {
              Tcons0 act = new Tcons0(Tcons0.SUPEQ,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      left,
                      right)));
              possibleStates.add(state.addConstraint(act));
            }
            break;
          }

          case NOT_EQUALS: {
            if (truthAssumption) {
              if (left instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode) left).dim)
                  || right instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode) right).dim)) {
                Texpr0BinNode increasedRight =
                    new Texpr0BinNode(Texpr0BinNode.OP_ADD, right, constantMin);
                Texpr0BinNode increasedLeft =
                    new Texpr0BinNode(Texpr0BinNode.OP_ADD, left, constantMin);

                possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                    new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                        left,
                        increasedRight)))));
                possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                    new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                        right,
                        increasedLeft)))));
              } else {
                if (splitDisequalities) {
                  // use same trick as in octagon analysis since disequality does not seem to work
                  possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                      new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                          left,
                          right)))));
                  possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                      new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                          right,
                          left)))));
                } else {
                  possibleStates.add(state.addConstraint(new Tcons0(Tcons0.DISEQ,
                      new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                          left,
                          right)))));
                }
              }
            } else {
              possibleStates.add(state.addConstraint(new Tcons0(Tcons0.EQ,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      left,
                      right)))));
            }
            break;
          }

          case DIVIDE:
          case MINUS:
          case MODULO:
          case MULTIPLY:
          case PLUS:
            Texpr0BinNode innerExp = null;
            switch (binExp.getOperator()) {
              case DIVIDE:
                innerExp = new Texpr0BinNode(Texpr0BinNode.OP_DIV, left, right);
                break;
              case MINUS:
                innerExp = new Texpr0BinNode(Texpr0BinNode.OP_SUB, left, right);
                break;
              case MODULO:
                innerExp = new Texpr0BinNode(Texpr0BinNode.OP_MOD, left, right);
                break;
              case MULTIPLY:
                innerExp = new Texpr0BinNode(Texpr0BinNode.OP_MUL, left, right);
                break;
              case PLUS:
                innerExp = new Texpr0BinNode(Texpr0BinNode.OP_ADD, left, right);
                break;

              // this cannot happen, this switch clause checks the same binary operator
              // as the outer switch clause
              default:
                throw new AssertionError();
            }

            if (truthAssumption) {
              possibleStates.add(state.addConstraint(new Tcons0(Tcons0.EQ, innerExp)));
            } else {
              possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      innerExp,
                      constantMin)))));
              possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      constantMin,
                      innerExp)))));
            }

            break;
          default:
            throw new UnrecognizedCCodeException("unknown binary operator", edge, binExp);
        }
      }
    }

    return possibleStates;
  }

  protected Set<ApronState> handleLiteralBinExpAssumption(
      double pLeftVal,
      double pRightVal,
      BinaryOperator pBinaryOperator,
      boolean truthAssumption) {
    boolean result;
    switch (pBinaryOperator) {
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
        return Collections.singleton(state);
      case NOT_EQUALS:
        result = pLeftVal != pRightVal;
        break;
      case EQUALS:
        result = pLeftVal == pRightVal;
        break;
      case GREATER_EQUAL:
        result = pLeftVal >= pRightVal;
        break;
      case GREATER_THAN:
        result = pLeftVal > pRightVal;
        break;
      case LESS_EQUAL:
        result = pLeftVal <= pRightVal;
        break;
      case LESS_THAN:
        result = pLeftVal < pRightVal;
        break;
      case MINUS:
        result = (pLeftVal - pRightVal) != 0;
        break;
      case MODULO:
        result = (pLeftVal % pRightVal) != 0;
        break;
      case MULTIPLY:
        result = (pLeftVal * pRightVal) != 0;
        break;
      case DIVIDE:
        result = (pLeftVal / pRightVal) != 0;
        break;
      case PLUS:
        result = (pLeftVal + pRightVal) != 0;
        break;
      default:
        throw new AssertionError("unhandled binary operator" + pBinaryOperator);
    }
    if ((truthAssumption && result)
        || (!truthAssumption && !result)) {
      return Collections.singleton(state);
    } else {
      return Collections.emptySet();
    }
  }

  /**
   * If only one literal is the complete boolean expression, we only need to check
   * this literal if it is equal to zero, depending on the truth assumption we
   * either return the unchanged state or null if the following branch is not reachable.
   *
   * @param value           The long value of the CLiteralExpression
   * @param truthAssumption indicates if we are in the then or the else branch of an assumption
   * @return an OctState or null
   */
  protected Set<ApronState> handleLiteralBooleanExpression(
      long value,
      boolean truthAssumption,
      ApronState state) {
    if ((value != 0) == truthAssumption) {
      return Collections.singleton(state);
    } else {
      return Collections.emptySet();
    }
  }

  protected ApronState.Type getCorrespondingOctStateType(CType type) {
    if (type instanceof CSimpleType
        && (((CSimpleType) type).getType() == CBasicType.FLOAT
        || ((CSimpleType) type).getType() == CBasicType.DOUBLE)) {
      return Type.FLOAT;
    } else {
      return Type.INT;
    }
  }

  protected boolean isHandleableVariable(CExpression var) {
    if (var instanceof CArraySubscriptExpression
        || (var instanceof CStringLiteralExpression)) {
      return false;
    }
    return isHandleAbleType(var.getExpressionType());
  }

  protected boolean isHandleAbleType(CType type) {
    type = type.getCanonicalType();
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    if (type instanceof CPointerType
        || type instanceof CArrayType) {
      return false;
    }

    return true;
  }

  @Override
  protected Set<ApronState> handleFunctionCallEdge(
      CFunctionCallEdge cfaEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  protected Set<ApronState> handleFunctionReturnEdge(
      CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr, String callerFunctionName)
      throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  protected Set<ApronState> handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl)
      throws CPATransferException {

    return Collections.singleton(state);
  }

  @Override
  protected Set<ApronState> handleStatementEdge(CStatementEdge cfaEdge, CStatement statement)
      throws CPATransferException {
    if (cfaEdge instanceof CFunctionSummaryStatementEdge) {
      ApronState newstate = state;
      CFunctionSummaryStatementEdge funcSummaryStatementEdge =
          (CFunctionSummaryStatementEdge) cfaEdge;
      CFunctionCall exprOnSummary = funcSummaryStatementEdge.getFunctionCall();
      if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement binExp =
            ((CFunctionCallAssignmentStatement) exprOnSummary);
        CLeftHandSide op1 = binExp.getLeftHandSide();
        String callerFunctionName = cfaEdge.getSuccessor().getFunctionName();
        MemoryLocation assignedVarName = buildVarName(op1, callerFunctionName);
        if (assignedVarName == null) {
          return Collections.singleton(state);
        }
        newstate = newstate.forget(assignedVarName);
      }
      return Collections.singleton(newstate);
    }

    // check if there are functioncalls we cannot handle
    if (statement instanceof CFunctionCall) {
      CExpression fn =
          ((CFunctionCall) statement).getFunctionCallExpression().getFunctionNameExpression();
      if (fn instanceof CIdExpression) {
        String func = ((CIdExpression) fn).getName();
        if (UNSUPPORTED_FUNCTIONS.containsKey(func)) {
          throw new UnsupportedCCodeException(UNSUPPORTED_FUNCTIONS.get(func), cfaEdge, fn);
        }
      }
    }

    // expression is a binary operation, e.g. a = b;
    if (statement instanceof CAssignment) {
      CLeftHandSide left = ((CAssignment) statement).getLeftHandSide();
      CRightHandSide right = ((CAssignment) statement).getRightHandSide();
      if (left.toString().contains("*") || left.toString().contains("->") || right.toString()
          .contains("*") || right.toString().contains("->")) {
        return Collections.singleton(state);
      }
      MemoryLocation variableName = buildVarName(left, functionName);
      if (variableName == null) {
        return Collections.singleton(state);
      }
      if (!state.existsVariable(variableName)) {
        return Collections.singleton(state);

      }

      // as pointers do not get declarated in the beginning we can just
      // ignore them here
      if (!isHandleableVariable(left)
          || !precision
          .isTracking(variableName, left.getExpressionType(), cfaEdge.getSuccessor())) {
        assert !state.existsVariable(variableName) : "variablename '" + variableName
            + "' is in map although it can not be handled";
        return Collections.singleton(state);
      } else {
        Set<Texpr0Node> coeffsList = right.accept(new CApronExpressionVisitor());

        if (coeffsList.isEmpty()) {
          state.setTop();
          return Collections.singleton(state);
        }

        Set<ApronState> possibleStates = new HashSet<>();
        for (Texpr0Node coeffs : coeffsList) {
          // if we cannot determine coefficients, we cannot make any assumptions about
          // the value of the assigned variable and reset its value to unknown
          ApronState st = state.makeAssignment(variableName, coeffs);
          assert !st.isEmpty() : "states with assignments / declarations should never be empty";
          possibleStates.add(st);
        }
        return possibleStates;
      }

      // external function call, or p.e. a;
      // => do nothing
    } else if (statement instanceof CFunctionCallStatement
        || statement instanceof CExpressionStatement) {
      return Collections.singleton(state);

    }

    throw new UnrecognizedCCodeException("unknown statement", cfaEdge, statement);
  }

  protected MemoryLocation buildVarName(CLeftHandSide left, String functionName) {
    String variableName = null;
    if (left instanceof CArraySubscriptExpression) {
      variableName = ((CArraySubscriptExpression) left).getArrayExpression().toASTString();
    } else if (left instanceof CPointerExpression) {
      variableName = ((CPointerExpression) left).getOperand().toASTString();
    } else {
      variableName = left.toASTString();
    }
    if (!isGlobal(left)) {
      return MemoryLocation.valueOf(functionName, variableName);
    } else {
      return MemoryLocation.valueOf(variableName);
    }
  }

  /**
   * This is a return statement in a function
   */
  @Override
  protected Set<ApronState> handleReturnStatementEdge(CReturnStatementEdge cfaEdge)
      throws CPATransferException {

    // this is for functions without return value, which just have returns
    // in them to end the function
    if (!cfaEdge.getExpression().isPresent()) {
      return Collections.singleton(state);
    }

//    MemoryLocation tempVarName =
//        MemoryLocation.valueOf(
//            cfaEdge.getPredecessor().getFunctionName(),
//            ((CIdExpression) cfaEdge.asAssignment().get().getLeftHandSide()).getName());

    MemoryLocation tempVarName = buildVarName(cfaEdge.asAssignment().get().getLeftHandSide(),
        cfaEdge.getPredecessor().getFunctionName());

    if (tempVarName == null) {
      return Collections.singleton(state);
    }
    // main function has no __cpa_temp_result_var as the result of the main function
    // is not important for us, we skip here
    if (!state.existsVariable(tempVarName)) {
      return Collections.singleton(state);
    }

    Set<ApronState> possibleStates = new HashSet<>();
    Set<Texpr0Node> coeffsList =
        cfaEdge.getExpression().get().accept(new CApronExpressionVisitor());

    if (coeffsList.isEmpty()) {
      return Collections.singleton(state);
    }

    for (Texpr0Node coeffs : coeffsList) {
      possibleStates.add(state.makeAssignment(tempVarName, coeffs));
    }
    return possibleStates;
  }

  /**
   * This edge is the return edge from a function to the caller
   */
  @Override
  protected Set<ApronState> handleFunctionSummaryEdge(CFunctionSummaryEdge cfaEdge)
      throws CPATransferException {
    ApronState newstate = state;
    CFunctionCall exprOnSummary = cfaEdge.getExpression();
    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement binExp = ((CFunctionCallAssignmentStatement) exprOnSummary);
      CLeftHandSide op1 = binExp.getLeftHandSide();
      String callerFunctionName = cfaEdge.getSuccessor().getFunctionName();
      MemoryLocation assignedVarName = buildVarName(op1, callerFunctionName);
      if (assignedVarName == null) {
        return Collections.singleton(state);
      }
      newstate = newstate.forget(assignedVarName);
    }
    return Collections.singleton(newstate);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    assert pState instanceof ApronState;
    ApronState apronState = (ApronState) pState;
    state = apronState;
    Set<ApronState> s = null;
    functionName = pCfaEdge.getPredecessor().getFunctionName();

    for (PointerState pointerState : FluentIterable.from(pOtherStates).filter(PointerState.class)) {
//      System.out.println(pState.toString());
//      System.out.println(((ApronState)pState).getIntegerVariableToIndexMap().size());
//      System.out.println(((ApronState)pState).getRealVariableToIndexMap().size());
//      System.out.println(pCfaEdge.toString());
      map = resolvePointState(pointerState);
      if (pCfaEdge instanceof CStatementEdge) {
        s = handleCStatementEdgeStrengthening(apronState, pointerState, (CStatementEdge) pCfaEdge,
            pPrecision);
      }
    }
    return s;
  }

  protected ApronState handleCFunctionSummaryStatementEdgeStrengthening(
      ApronState apronState, PointerState pointerState,
      CFunctionSummaryStatementEdge pCfaEdge, Precision precision)
      throws CPATransferException, InterruptedException {
    CFunctionSummaryStatementEdge summaryEdge = pCfaEdge;
    //handle parameters
    List<CExpression> paramExpr =
        summaryEdge.getFunctionCall().getFunctionCallExpression().getParameterExpressions();
    for (int i = 0; i < paramExpr.size(); i++) {
      String calledFunctionName = pCfaEdge.getPredecessor().getFunctionName();
      String callerFunctionName = pCfaEdge.getSuccessor().getFunctionName();
      if (!(paramExpr.get(i) instanceof CLeftHandSide)) {
        continue;
      }
      MemoryLocation paramVarName =
          buildVarName((CLeftHandSide) paramExpr.get(i), callerFunctionName);
      if (paramVarName == null) {
        continue;
      }
      List<MemoryLocation> l = new ArrayList<>();
      getPointingContent(l, map, paramVarName);
      List<MemoryLocation> m1 = apronState.getIntegerVariableToIndexMap();
      List<MemoryLocation> m2 = apronState.getRealVariableToIndexMap();
      for (int k = 0; k < l.size(); k++) {
        for (int j = 0; j < m1.size(); j++) {
          if (m1.get(j).equals(l.get(k))) {
            apronState = apronState.forget(m1.get(j));
          }
        }
      }
      for (int k = 0; k < l.size(); k++) {
        for (int j = 0; j < m2.size(); j++) {
          if (m2.get(j).equals(l.get(k))) {
            apronState = apronState.forget(m2.get(j));
          }
        }
      }
    }
    //handle return assigned variable
    CFunctionCall exprOnSummary = summaryEdge.getFunctionCall();
    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement binExp = ((CFunctionCallAssignmentStatement) exprOnSummary);
      CLeftHandSide op1 = binExp.getLeftHandSide();
      String callerFunctionName = pCfaEdge.getSuccessor().getFunctionName();
      MemoryLocation assignedVarName = buildVarName(op1, callerFunctionName);
      if (assignedVarName == null) {
        return apronState;
      }
      List<MemoryLocation> l = new ArrayList<>();
      getPointingContent(l, map, assignedVarName);
      List<MemoryLocation> m1 = apronState.getIntegerVariableToIndexMap();
      List<MemoryLocation> m2 = apronState.getRealVariableToIndexMap();
      for (int k = 0; k < l.size(); k++) {
        for (int j = 0; j < m1.size(); j++) {
          if (m1.get(j).equals(l.get(k))) {
            apronState = apronState.forget(m1.get(j));
          }
        }
      }
      for (int k = 0; k < l.size(); k++) {
        for (int j = 0; j < m2.size(); j++) {
          if (m2.get(j).equals(l.get(k))) {
            apronState = apronState.forget(m2.get(j));
          }
        }
      }
    }
    return apronState;
  }

  protected Set<ApronState> handleCStatementEdgeStrengthening(
      ApronState apronState, PointerState pointerState,
      CStatementEdge cfaEdge, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    if (!(pPrecision instanceof VariableTrackingPrecision)) {
      return null;
    }
    VariableTrackingPrecision precision = (VariableTrackingPrecision) pPrecision;
    CStatement statement = cfaEdge.getStatement();
    if (cfaEdge instanceof CFunctionSummaryStatementEdge) {
      ApronState newState =
          handleCFunctionSummaryStatementEdgeStrengthening(apronState, pointerState,
              (CFunctionSummaryStatementEdge) cfaEdge, precision);
      return Collections.singleton(newState);
    }

    // check if there are functioncalls we cannot handle
    if (statement instanceof CFunctionCall) {
      CExpression fn =
          ((CFunctionCall) statement).getFunctionCallExpression().getFunctionNameExpression();
      if (fn instanceof CIdExpression) {
        String func = ((CIdExpression) fn).getName();
        if (UNSUPPORTED_FUNCTIONS.containsKey(func)) {
          throw new UnsupportedCCodeException(UNSUPPORTED_FUNCTIONS.get(func), cfaEdge, fn);
        }
      }
    }

    HashMap<MemoryLocation, List<MemoryLocation>> map = resolvePointState(pointerState);
    // expression is a binary operation, e.g. a = b;
    if (statement instanceof CAssignment) {
      CLeftHandSide left = ((CAssignment) statement).getLeftHandSide();
      CRightHandSide right = ((CAssignment) statement).getRightHandSide();

      if (!left.toString().contains("*") && !left.toString().contains("->") && !right.toString()
          .contains("*") && !right.toString().contains("->")) {
        return null;
      }

      if (left.toString().contains("->") || right.toString().contains("->")) {
        apronState.setTop();
        return Collections.singleton(apronState);
      }

      MemoryLocation variableName = buildVarName(left, functionName);
      List<MemoryLocation> leftList = new ArrayList<>();
      getPointingContent(leftList, map, variableName);

      if (leftList.size() == 0) {
        apronState.setTop();
        return Collections.singleton(apronState);
      }

      for (int i = 0; i < leftList.size(); i++) {
        MemoryLocation ml = leftList.get(i);
        if (ArithSummaryComputer.getUsedVars().get(functionName) == null) {
          return Collections.singleton(state);
        }
        Set<MemoryLocation> funcUsedVarSet =
            ArithSummaryComputer.getUsedVars().get(functionName).keySet();
        funcUsedVarSet = funcUsedVarSet == null ? Collections.EMPTY_SET : funcUsedVarSet;
        if (!funcUsedVarSet.contains(ml)) {
          continue;
        }
        // as pointers do not get declarated in the beginning we can just
        // ignore them here
        if (!isHandleableVariable(left) || !precision
            .isTracking(ml, left.getExpressionType(), cfaEdge.getSuccessor())) {
          assert !apronState.existsVariable(variableName) : "variablename '" + variableName
              + "' is in map although it can not be handled";
          return Collections.singleton(apronState);
        } else {
          Set<Texpr0Node> coeffsList = right.accept(new CApronExpressionVisitor());

          if (coeffsList.isEmpty()) {
            apronState.setTop();
            return Collections.singleton(apronState);
          }

          Set<ApronState> possibleStates = new HashSet<>();
          for (Texpr0Node coeffs : coeffsList) {
            // if we cannot determine coefficients, we cannot make any assumptions about
            // the value of the assigned variable and reset its value to unknown
            if (!apronState.existsVariable(ml)) {
              ml = MemoryLocation.valueOf(ml.getIdentifier());
            }

            ApronState st = apronState.makeAssignment(ml, coeffs);
            assert !st.isEmpty() : "states with assignments / declarations should never be empty";
            possibleStates.add(st);
          }
          return possibleStates;
        }
      }
      return Collections.singleton(apronState);
    } else if (statement instanceof CFunctionCallStatement
        || statement instanceof CExpressionStatement) {
      // external function call, or p.e. a;
      // => do nothing
      return Collections.singleton(apronState);

    }

    throw new UnrecognizedCCodeException("unknown statement", cfaEdge, statement);
  }

  /**
   * This Visitor, evaluates all coefficients for a given Expression.
   */
  public class CApronExpressionVisitor
      extends DefaultCExpressionVisitor<Set<Texpr0Node>, CPATransferException>
      implements CRightHandSideVisitor<Set<Texpr0Node>, CPATransferException> {

    @Override
    protected Set<Texpr0Node> visitDefault(CExpression pExp) throws CPATransferException {
      return Collections.EMPTY_SET;
    }

    @Override
    public Set<Texpr0Node> visit(CPointerExpression e) throws CPATransferException {
      return e.getOperand().accept(this);
    }

    @Override
    public Set<Texpr0Node> visit(CFieldReference e) throws CPATransferException {
      MemoryLocation ml = buildVarName(e, functionName);
      if (ml == null) {
        return Collections.EMPTY_SET;
      }
      Set<Texpr0Node> s = new HashSet<>();
      Integer varIndex = state.getVariableIndexFor(ml);
      if (varIndex != -1) {
        s.add(new Texpr0DimNode(varIndex));
      }
      return s;
    }

    @Override
    public Set<Texpr0Node> visit(CBinaryExpression e) throws CPATransferException {
      Set<Texpr0Node> left = e.getOperand1().accept(this);
      Set<Texpr0Node> right = e.getOperand2().accept(this);

      Set<Texpr0Node> returnCoefficients = new HashSet<>();
      for (Texpr0Node leftCoeffs : left) {
        for (Texpr0Node rightCoeffs : right) {
          switch (e.getOperator()) {
            case BINARY_AND:
            case BINARY_OR:
            case BINARY_XOR:
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
              return Collections.emptySet();
            case MODULO:
              returnCoefficients
                  .add(new Texpr0BinNode(Texpr0BinNode.OP_MOD, leftCoeffs, rightCoeffs));
              break;
            case DIVIDE:
              returnCoefficients
                  .add(new Texpr0BinNode(Texpr0BinNode.OP_DIV, leftCoeffs, rightCoeffs));
              break;
            case MULTIPLY:
              returnCoefficients
                  .add(new Texpr0BinNode(Texpr0BinNode.OP_MUL, leftCoeffs, rightCoeffs));
              break;
            case MINUS:
              returnCoefficients
                  .add(new Texpr0BinNode(Texpr0BinNode.OP_SUB, leftCoeffs, rightCoeffs));
              break;
            case PLUS:
              returnCoefficients
                  .add(new Texpr0BinNode(Texpr0BinNode.OP_ADD, leftCoeffs, rightCoeffs));
              break;
            case EQUALS: {
              Tcons0 constraint = new Tcons0(Tcons0.EQ,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      leftCoeffs,
                      rightCoeffs)));

              if (!state.satisfies(constraint)) {
                returnCoefficients.add(new Texpr0CstNode());
              }

              if (!state.addConstraint(constraint).isEmpty()) {
                returnCoefficients.add(new Texpr0CstNode(new Interval(1, 1)));
              }

              break;
            }
            case GREATER_EQUAL: {
              Tcons0 constraint = new Tcons0(Tcons0.SUPEQ,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      leftCoeffs,
                      rightCoeffs)));

              if (!state.satisfies(constraint)) {
                returnCoefficients.add(new Texpr0CstNode());
              }

              if (!state.addConstraint(constraint).isEmpty()) {
                returnCoefficients.add(new Texpr0CstNode(new Interval(1, 1)));
              }

              break;
            }
            case GREATER_THAN: {
              Tcons0 constraint = new Tcons0(Tcons0.SUP,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      leftCoeffs,
                      rightCoeffs)));

              if (!state.satisfies(constraint)) {
                returnCoefficients.add(new Texpr0CstNode());
              }

              if (!state.addConstraint(constraint).isEmpty()) {
                returnCoefficients.add(new Texpr0CstNode(new Interval(1, 1)));
              }

              break;
            }
            case LESS_EQUAL: {
              Tcons0 constraint = new Tcons0(Tcons0.SUPEQ,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      rightCoeffs,
                      leftCoeffs)));

              if (!state.satisfies(constraint)) {
                returnCoefficients.add(new Texpr0CstNode());
              }

              if (!state.addConstraint(constraint).isEmpty()) {
                returnCoefficients.add(new Texpr0CstNode(new Interval(1, 1)));
              }

              break;
            }
            case LESS_THAN: {
              Tcons0 constraint = new Tcons0(Tcons0.SUP,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      rightCoeffs,
                      leftCoeffs)));

              if (!state.satisfies(constraint)) {
                returnCoefficients.add(new Texpr0CstNode());
              }

              if (!state.addConstraint(constraint).isEmpty()) {
                returnCoefficients.add(new Texpr0CstNode(new Interval(1, 1)));
              }

              break;
            }
            case NOT_EQUALS: {
              Tcons0 constraint = new Tcons0(Tcons0.DISEQ,
                  new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                      leftCoeffs,
                      rightCoeffs)));

              if (!state.satisfies(constraint)) {
                returnCoefficients.add(new Texpr0CstNode());
              }

              if (!state.addConstraint(constraint).isEmpty()) {
                returnCoefficients.add(new Texpr0CstNode(new Interval(1, 1)));
              }

              break;
            }

            default:
              throw new AssertionError("Unhandled case statement");
          }
        }
      }
      return returnCoefficients;
    }

    /**
     * Only unpack the cast and continue with the casts operand
     */
    @Override
    public Set<Texpr0Node> visit(CCastExpression e) throws CPATransferException {
      return e.getOperand().accept(this);
    }

    @Override
    public Set<Texpr0Node> visit(CIdExpression e) throws CPATransferException {
      MemoryLocation varName = MemoryLocation.valueOf(e.getDeclaration().getQualifiedName());
      List<MemoryLocation> varNameList = new ArrayList<>();
      getPointingContent(varNameList, map, varName);
      Set<Texpr0Node> s = new HashSet<>();
      for (int i = 0; i < varNameList.size(); i++) {
        MemoryLocation ml = varNameList.get(i);
        Integer varIndex = state.getVariableIndexFor(ml);
        if (varIndex != -1) {
          s.add(new Texpr0DimNode(varIndex));
        }
      }
      return s;
    }

    @Override
    public Set<Texpr0Node> visit(CCharLiteralExpression e) throws CPATransferException {
      return Collections.singleton(Texpr0Node
          .fromLinexpr0(new Linexpr0(new Linterm0[0], new DoubleScalar(e.getCharacter()))));
    }

    @Override
    public Set<Texpr0Node> visit(CFloatLiteralExpression e) throws CPATransferException {
      return Collections.singleton(Texpr0Node.fromLinexpr0(
          new Linexpr0(new Linterm0[0], new DoubleScalar(e.getValue().doubleValue()))));
    }

    @Override
    public Set<Texpr0Node> visit(CIntegerLiteralExpression e) throws CPATransferException {
      return Collections.singleton(Texpr0Node.fromLinexpr0(
          new Linexpr0(new Linterm0[0], new DoubleScalar(e.getValue().doubleValue()))));
    }

    @SuppressWarnings("deprecation")
    @Override
    public Set<Texpr0Node> visit(CUnaryExpression e) throws CPATransferException {
      Set<Texpr0Node> operand = e.getOperand().accept(this);

      switch (e.getOperator()) {
        case AMPER:
        case SIZEOF:
        case TILDE:
          return Collections.emptySet();

        case MINUS:
          Set<Texpr0Node> returnCoefficients = new HashSet<>();
          for (Texpr0Node coeffs : operand) {
            returnCoefficients.add(new Texpr0UnNode(Texpr0UnNode.OP_NEG, coeffs));
          }
          return returnCoefficients;

        default:
          throw new AssertionError("Unhandled case in switch clause.");
      }
    }

    @Override
    public Set<Texpr0Node> visit(CFunctionCallExpression e) throws CPATransferException {
      if (e.getFunctionNameExpression() instanceof CIdExpression) {
        String functionName = ((CIdExpression) e.getFunctionNameExpression()).getName();
        if (functionName.equals("__VERIFIER_nondet_int")) {
          Scalar sup = Scalar.create();
          sup.setInfty(1);
          Scalar inf = Scalar.create();
          inf.setInfty(-1);
          Interval interval = new Interval(inf, sup);
          return Collections.singleton((Texpr0Node) new Texpr0CstNode(interval));
        } else if (functionName.equals("__VERIFIER_nondet_uint")) {
          Interval interval = new Interval();
          Scalar sup = Scalar.create();
          sup.setInfty(1);
          interval.setSup(sup);
          return Collections.singleton((Texpr0Node) new Texpr0CstNode(interval));
        } else if (functionName.equals("__VERIFIER_nondet_bool")) {
          Interval interval = new Interval(0, 1);
          return Collections.singleton((Texpr0Node) new Texpr0CstNode(interval));
        }
      }
      return Collections.emptySet();
    }
  }

  static class CLiteralExpressionVisitor
      extends DefaultCExpressionVisitor<Double, CPATransferException> {

    @Override
    protected Double visitDefault(CExpression pExp) throws CPATransferException {
      return null;
    }

    @Override
    public Double visit(CFloatLiteralExpression e) throws CPATransferException {
      return e.getValue().doubleValue();
    }

    @Override
    public Double visit(CIntegerLiteralExpression e) throws CPATransferException {
      return e.getValue().doubleValue();
    }

    @Override
    public Double visit(CCharLiteralExpression e) throws CPATransferException {
      return (double) e.getCharacter();
    }

    @Override
    public Double visit(CBinaryExpression e) throws CPATransferException {
      Double left = e.getOperand1().accept(this);
      Double right = e.getOperand2().accept(this);
      if (left == null || right == null) {
        return null;
      }
      switch (e.getOperator()) {
        case BINARY_AND:
        case BINARY_OR:
        case BINARY_XOR:
        case SHIFT_LEFT:
        case SHIFT_RIGHT:
          return null;
        case DIVIDE:
          return left / right;
        case EQUALS:
          return left.equals(right) ? 1.0 : 0;
        case GREATER_EQUAL:
          return left >= right ? 1.0 : 0;
        case GREATER_THAN:
          return left > right ? 1.0 : 0;
        case LESS_EQUAL:
          return left <= right ? 1.0 : 0;
        case LESS_THAN:
          return left < right ? 1.0 : 0;
        case NOT_EQUALS:
          break;
        case MINUS:
          return left - right;
        case MODULO:
          return left % right;
        case MULTIPLY:
          return left * right;
        case PLUS:
          return left + right;
        default:
          break;
      }
      return null;
    }

    @Override
    public Double visit(CUnaryExpression e) throws CPATransferException {
      Double op = e.getOperand().accept(this);
      if (op == null) {
        return null;
      }

      switch (e.getOperator()) {
        case ALIGNOF:
        case AMPER:
        case TILDE:
        case SIZEOF:
          return null;
        case MINUS:
          return -op;
        default:
          break;
      }
      return null;
    }

    @Override
    public Double visit(CCastExpression e) throws CPATransferException {
      Double op = e.getOperand().accept(this);
      if (op != null
          && e.getExpressionType() instanceof CSimpleType
          && ((((CSimpleType) e.getExpressionType()).getType() == CBasicType.INT)
          || (((CSimpleType) e.getExpressionType()).getType() == CBasicType.CHAR))) {
        return (double) op.intValue();
      }
      return op;
    }
  }

  protected boolean isStructType(CType t) {
    CComplexType ct = null;
    if (t instanceof CComplexType) {
      ct = (CComplexType) t;
      if (ct instanceof CCompositeType) {
        return true;
      } else if (ct instanceof CElaboratedType) {
        if (((CElaboratedType) ct).getRealType() instanceof CCompositeType) {
          return true;
        }
      }
    }
    return false;
  }

  protected HashMap<MemoryLocation, List<MemoryLocation>> resolvePointState(PointerState state) {
    HashMap<MemoryLocation, List<MemoryLocation>> resultMap = new HashMap<>();
    Map<MemoryLocation, LocationSet> pointMap = state.getPointsToMap();
    Iterator<MemoryLocation> it1 = pointMap.keySet().iterator();

    while (it1.hasNext()) {
      MemoryLocation ml1 = it1.next();
      LocationSet ls = pointMap.get(ml1);
      if (ls instanceof ExplicitLocationSet) {
        ExplicitLocationSet explicitSet = (ExplicitLocationSet) ls;
        Iterator<MemoryLocation> it2 = explicitSet.iterator();
        List<MemoryLocation> l = new ArrayList<>();
        while (it2.hasNext()) {
          l.add(it2.next());
        }
        resultMap.put(ml1, l);
      }
    }
    return resultMap;
  }

  protected void getPointingContent(
      List<MemoryLocation> result,
      HashMap<MemoryLocation, List<MemoryLocation>> map,
      MemoryLocation src) {
    if (src == null) {
      return;
    }
    List<MemoryLocation> s = map.get(src);
    if (s == null) {
      result.add(src);
      return;
    }
    for (int i = 0; i < s.size(); i++) {
      if (map.get(s.get(i)) == null) {
        result.add(s.get(i));
      }
    }
  }

  public static LoopStructure getLoopStructure() {
    return loopStructure;
  }
}
