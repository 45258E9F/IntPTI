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
package org.sosy_lab.cpachecker.cpa.taint;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.taint.TaintState.Taint;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.ArrayAccessSegment;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.FieldAccessSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

@Options(prefix = "cpa.taint")
public class TaintTransferRelation
    extends ForwardingTransferRelation<TaintState, TaintState, Precision> {

  private CFANode main;

  private final LogManagerWithoutDuplicates logger;

  public TaintTransferRelation(final Configuration pConfig, final LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = new LogManagerWithoutDuplicates(pLogger);
  }

  void setMainFunctionNode(CFANode pMain) {
    main = pMain;
  }

  /**
   * Everything is done in strengthen phase
   * (In order to make use of other components of states)
   */
  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      List<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision) throws CPATransferException, InterruptedException {
    return null;
  }


  @Override
  protected TaintState handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean
      truthAssumption) throws CPATransferException {
    TaintState newState = TaintState.copyOf(state);
    // For now, we apply an assumption that operands of assumption expression are cleansed.
    // After pre-processing, all assumption conditions are regularized as binary operations
    CBinaryExpression assumption = (CBinaryExpression) expression;
    CExpression op1 = assumption.getOperand1();
    CExpression op2 = assumption.getOperand2();
    // If the operand is left-hand-side, we directly compute access path and set the taint status;
    // otherwise, we find the most top left-hand-side and update the taint status.
    Set<AccessPath> paths = new HashSet<>();
    paths.addAll(op1.accept(new MultipleAccessPathVisitorForTaint(otherStates)));
    paths.addAll(op2.accept(new MultipleAccessPathVisitorForTaint(otherStates)));
    for (AccessPath path : paths) {
      newState = newState.updateTaint(path, Taint.CLEAN);
    }
    return newState;
  }

  @Override
  protected TaintState handleFunctionCallEdge(
      CFunctionCallEdge cfaEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> parameters,
      String calledFunctionName)
      throws CPATransferException {
    return state.pushCallStack(parameters, arguments, otherStates);
  }

  @Override
  protected TaintState handleFunctionReturnEdge(
      CFunctionReturnEdge returnEdge,
      CFunctionSummaryEdge summaryEdge,
      CFunctionCall callExpression,
      String callerName)
      throws CPATransferException {
    Optional<CVariableDeclaration> returnVar = summaryEdge.getFunctionEntry().getReturnVariable();
    Taint value = null;
    CLeftHandSide leftHandSide = null;
    if (callExpression instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement callAssignment = (CFunctionCallAssignmentStatement)
          callExpression;
      leftHandSide = callAssignment.getLeftHandSide();
      if (returnVar.isPresent()) {
        value = state.queryTaint(new AccessPath(returnVar.get()));
      }
    }
    TaintState newState = state.popCallStack();
    if (leftHandSide != null) {
      AccessPath leftPath = leftHandSide.accept(new AccessPathVisitorForTaint(otherStates));
      if (value != null) {
        newState = newState.updateTaint(leftPath, value);
      } else {
        newState = newState.updateTaint(leftPath, Taint.CLEAN);
      }
    }
    return newState;
  }

  @Override
  protected TaintState handleDeclarationEdge(
      CDeclarationEdge cfaEdge,
      CDeclaration declaration)
      throws CPATransferException {
    if (declaration instanceof CVariableDeclaration) {
      CVariableDeclaration varDecl = (CVariableDeclaration) declaration;
      AccessPath path = new AccessPath(varDecl);
      CInitializer initializer = varDecl.getInitializer();
      if (initializer != null) {
        // it is possible that initializer expression contains taint values
        // NOTE: the prefix access path is non-empty
        return handleInitializer(Lists.newArrayList(path), initializer, state, varDecl.getType());
      }
      return state.updateTaint(path, Taint.CLEAN);
    }
    return state;
  }

  private TaintState handleInitializer(
      List<AccessPath> prefixPaths, CInitializer pInitializer,
      TaintState pState, CType declaredType)
      throws UnrecognizedCCodeException {
    if (pInitializer instanceof CInitializerExpression) {
      CExpression exp = ((CInitializerExpression) pInitializer).getExpression();
      Taint taint = pState.evaluateTaint(exp, otherStates);
      TaintState newState = TaintState.copyOf(pState);
      for (AccessPath prefix : prefixPaths) {
        newState = newState.updateTaint(prefix, taint);
      }
      return newState;
    } else if (pInitializer instanceof CDesignatedInitializer) {
      CDesignatedInitializer designatedInitializer = (CDesignatedInitializer) pInitializer;
      List<CDesignator> designators = designatedInitializer.getDesignators();
      CInitializer rightHandSide = designatedInitializer.getRightHandSide();
      List<AccessPath> accumulatedPaths = new ArrayList<>(prefixPaths);
      for (CDesignator designator : designators) {
        List<AccessPath> resultPaths = new ArrayList<>();
        if (designator instanceof CArrayDesignator) {
          CExpression indexExp = ((CArrayDesignator) designator).getSubscriptExpression();
          PathSegment newSegment;
          if (indexExp instanceof CIntegerLiteralExpression) {
            long index = ((CIntegerLiteralExpression) indexExp).getValue().longValue();
            newSegment = new ArrayConstIndexSegment(index);
          } else {
            // TODO: use value analysis to derive concrete index value
            newSegment = new ArrayAccessSegment();
          }
          for (AccessPath path : accumulatedPaths) {
            AccessPath newPath = AccessPath.copyOf(path);
            newPath.appendSegment(newSegment);
            resultPaths.add(newPath);
          }
          CArrayType arrayType = Types.extractArrayType(declaredType);
          if (arrayType == null) {
            throw new UnsupportedOperationException("Unsupported type for array " + declaredType);
          }
          declaredType = arrayType.getType();
        } else if (designator instanceof CFieldDesignator) {
          String fieldName = ((CFieldDesignator) designator).getFieldName();
          FieldAccessSegment newSegment = new FieldAccessSegment(fieldName);
          for (AccessPath path : accumulatedPaths) {
            AccessPath newPath = AccessPath.copyOf(path);
            newPath.appendSegment(newSegment);
            resultPaths.add(newPath);
          }
          CCompositeType compositeType = Types.extractCompositeType(declaredType);
          if (compositeType == null) {
            throw new UnsupportedOperationException("Unsupported type for structure " +
                declaredType);
          }
          CCompositeTypeMemberDeclaration targetMember = Types.retrieveMemberByName
              (compositeType, fieldName);
          if (targetMember == null) {
            throw new UnsupportedOperationException("Specified field " + fieldName + " not found");
          }
          declaredType = targetMember.getType();
        } else {
          // otherwise, the designator should be an instance of {@link CArrayRangeDesignator}
          CArrayRangeDesignator rangeDesignator = (CArrayRangeDesignator) designator;
          CExpression floor = rangeDesignator.getFloorExpression();
          CExpression ceil = rangeDesignator.getCeilExpression();
          if (floor instanceof CIntegerLiteralExpression && ceil instanceof
              CIntegerLiteralExpression) {
            long floorValue = ((CIntegerLiteralExpression) floor).getValue().longValue();
            long ceilValue = ((CIntegerLiteralExpression) ceil).getValue().longValue();
            for (AccessPath path : accumulatedPaths) {
              for (long i = floorValue; i <= ceilValue; i++) {
                AccessPath newPath = AccessPath.copyOf(path);
                newPath.appendSegment(new ArrayConstIndexSegment(i));
                resultPaths.add(newPath);
              }
            }
          } else {
            // TODO: use value analysis to derive concrete index value
            PathSegment newSegment = new ArrayAccessSegment();
            for (AccessPath path : accumulatedPaths) {
              AccessPath newPath = AccessPath.copyOf(path);
              newPath.appendSegment(newSegment);
              resultPaths.add(newPath);
            }
          }
          CArrayType arrayType = Types.extractArrayType(declaredType);
          if (arrayType == null) {
            throw new UnsupportedOperationException("Unsupported type for array " + declaredType);
          }
          declaredType = arrayType.getType();
        }
        accumulatedPaths.clear();
        accumulatedPaths.addAll(resultPaths);
      }
      return handleInitializer(accumulatedPaths, rightHandSide, pState, declaredType);
    } else {
      // pInitializer is an instance of {@link CInitializerList}
      CCompositeType compositeType = Types.extractCompositeType(declaredType);
      CArrayType arrayType = Types.extractArrayType(declaredType);
      if ((compositeType == null) == (arrayType == null)) {
        // unexpected case, we are supposed to do nothing
        return pState;
      }
      TaintState newState = TaintState.copyOf(pState);
      CInitializerList initializerList = (CInitializerList) pInitializer;
      List<CInitializer> initializers = initializerList.getInitializers();
      if (arrayType != null) {
        CType elementType = arrayType.getType();
        // the negative index indicates that the current index is undetermined
        long index = -1;
        for (CInitializer initializer : initializers) {
          if (initializer instanceof CDesignatedInitializer) {
            List<CDesignator> designatorList = ((CDesignatedInitializer) initializer)
                .getDesignators();
            if (designatorList.size() > 0) {
              CDesignator firstDesignator = designatorList.get(0);
              if (firstDesignator instanceof CArrayDesignator) {
                CExpression indexExp = ((CArrayDesignator) firstDesignator)
                    .getSubscriptExpression();
                if (indexExp instanceof CIntegerLiteralExpression) {
                  index = ((CIntegerLiteralExpression) indexExp).getValue().longValue() + 1;
                } else {
                  // TODO: use value analysis to derive concrete index value
                  index = -1;
                }
              } else if (firstDesignator instanceof CArrayRangeDesignator) {
                CExpression ceilExp = ((CArrayRangeDesignator) firstDesignator).getCeilExpression();
                if (ceilExp instanceof CIntegerLiteralExpression) {
                  index = ((CIntegerLiteralExpression) ceilExp).getValue().longValue() + 1;
                } else {
                  // TODO: use value analysis too derive concrete index value
                  index = -1;
                }
              }
            }
            newState = handleInitializer(prefixPaths, initializer, newState, arrayType);
          } else {
            List<AccessPath> newPaths = new ArrayList<>(prefixPaths.size());
            PathSegment newSegment;
            if (index < 0) {
              newSegment = new ArrayAccessSegment();
            } else {
              newSegment = new ArrayConstIndexSegment(index + 1);
              index++;
            }
            for (AccessPath path : prefixPaths) {
              AccessPath newPath = AccessPath.copyOf(path);
              newPath.appendSegment(newSegment);
              newPaths.add(newPath);
            }
            newState = handleInitializer(newPaths, initializer, newState, elementType);
          }
        }
      } else {
        Preconditions.checkNotNull(compositeType);
        List<CCompositeTypeMemberDeclaration> members = compositeType.getMembers();
        int index = 0;
        for (CInitializer initializer : initializers) {
          if (initializer instanceof CDesignatedInitializer) {
            List<CDesignator> designatorList = ((CDesignatedInitializer) initializer)
                .getDesignators();
            if (designatorList.size() > 0) {
              CDesignator firstDesignator = designatorList.get(0);
              if (firstDesignator instanceof CFieldDesignator) {
                String fieldName = ((CFieldDesignator) firstDesignator).getFieldName();
                for (int i = 0; i < members.size(); i++) {
                  String memberName = members.get(i).getName();
                  if (memberName.equals(fieldName)) {
                    index = i + 1;
                    break;
                  }
                }
              }
            }
            newState = handleInitializer(prefixPaths, initializer, newState, compositeType);
          } else {
            CCompositeTypeMemberDeclaration targetMember = members.get(index);
            index++;
            String targetName = targetMember.getName();
            FieldAccessSegment newSegment = new FieldAccessSegment(targetName);
            List<AccessPath> newPaths = new ArrayList<>(prefixPaths.size());
            for (AccessPath path : prefixPaths) {
              AccessPath newPath = AccessPath.copyOf(path);
              newPath.appendSegment(newSegment);
              newPaths.add(newPath);
            }
            newState = handleInitializer(newPaths, initializer, newState, targetMember.getType());
          }
        }
      }
      return newState;
    }
  }

  @Override
  protected TaintState handleStatementEdge(CStatementEdge stmtEdge, CStatement statement)
      throws CPATransferException {
    TaintState newState;
    if (statement instanceof CAssignment) {
      newState = handleAssignment((CAssignment) statement, state);
    } else {
      newState = state;
    }
    // if the statement contains call of library function, we should carefully set taint status
    // for its argument expressions
    if (statement instanceof CFunctionCall) {
      CFunctionCallExpression callExpression = ((CFunctionCall) statement)
          .getFunctionCallExpression();
      Taint taint = callExpression.accept(new ExpressionTaintVisitor(state, otherStates));
      if (taint == Taint.TAINT) {
        // that means, we have no knowledge on how argument values are to be changed
        List<CExpression> args = callExpression.getParameterExpressions();
        for (CExpression arg : args) {
          CExpression argExp = arg;
          if (arg instanceof CStringLiteralExpression) {
            continue;
          }
          CPointerType pointerType;
          while ((pointerType = Types.extractPointerType(argExp.getExpressionType())) != null) {
            argExp = new CPointerExpression(FileLocation.DUMMY, pointerType.getType(), argExp);
          }
          if (argExp instanceof CLeftHandSide) {
            AccessPath path = ((CLeftHandSide) argExp).accept(new AccessPathVisitorForTaint
                (otherStates));
            if (path != null) {
              newState = newState.updateTaint(path, Taint.TAINT);
            }
          }
        }
      }
    }
    return newState;
  }

  private TaintState handleAssignment(CAssignment assignment, TaintState pState)
      throws UnrecognizedCCodeException {
    TaintState newState = TaintState.copyOf(pState);
    CLeftHandSide leftHandSide = assignment.getLeftHandSide();
    CRightHandSide rightHandSide = assignment.getRightHandSide();
    AccessPath path = leftHandSide.accept(new AccessPathVisitorForTaint(otherStates));
    Taint taint = state.evaluateTaint(rightHandSide, otherStates);
    return newState.updateTaint(path, taint);
  }

  @Override
  protected TaintState handleReturnStatementEdge(CReturnStatementEdge retEdge)
      throws CPATransferException {
    Optional<CAssignment> orAssign = retEdge.asAssignment();
    if (orAssign.isPresent()) {
      CAssignment assign = orAssign.get();
      return handleAssignment(assign, state);
    }
    return state;
  }

  @Override
  protected TaintState handleBlankEdge(BlankEdge pBlankEdge) {
    if (pBlankEdge.getPredecessor() == main && state.getLocalState().isEmpty()) {
      logger.log(Level.FINE, "Begin to analysis main (or entry) function.");
      CFANode node = pBlankEdge.getPredecessor();
      CFunctionEntryNode entry = (CFunctionEntryNode) node;
      return state.pushEntryCallStack(entry.getFunctionParameters());
    }
    return state;
  }

  @Override
  protected TaintState handleFunctionSummaryEdge(CFunctionSummaryEdge summaryEdge) {
    return state;
  }

}
