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
package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class combines a AutomatonInternal State with a variable Configuration.
 * Instances of this class are passed to the CPAchecker as AbstractState.
 */
public class AutomatonState
    implements AbstractQueryableState, Targetable, Serializable, AbstractStateWithAssumptions,
               Graphable {

  private static final long serialVersionUID = -4665039439114057346L;
  private static final String AutomatonAnalysisNamePrefix = "AutomatonAnalysis_";

  static final String INTERNAL_STATE_IS_TARGET_PROPERTY = "internalStateIsTarget";

  static class TOP extends AutomatonState {
    private static final long serialVersionUID = -7848577870312049023L;

    public TOP(ControlAutomatonCPA pAutomatonCPA) {
      super(
          Collections.<String, AutomatonVariable>emptyMap(),
          new AutomatonInternalState(
              "_predefinedState_TOP", Collections.<AutomatonTransition>emptyList()),
          pAutomatonCPA,
          ImmutableList.<AStatement>of(),
          ExpressionTrees.<AExpression>getTrue(),
          0,
          0,
          null);
    }

    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      return pProperty.toLowerCase().equals("state == top");
    }

    @Override
    public String toString() {
      return "AutomatonState.TOP";
    }
  }

  static class BOTTOM extends AutomatonState {
    private static final long serialVersionUID = -401794748742705212L;

    public BOTTOM(ControlAutomatonCPA pAutomatonCPA) {
      super(
          Collections.<String, AutomatonVariable>emptyMap(),
          AutomatonInternalState.BOTTOM,
          pAutomatonCPA,
          ImmutableList.<AStatement>of(),
          ExpressionTrees.<AExpression>getTrue(),
          0,
          0,
          null);
    }

    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      return pProperty.toLowerCase().equals("state == bottom");
    }

    @Override
    public String toString() {
      return "AutomatonState.BOTTOM";
    }
  }

  private transient ControlAutomatonCPA automatonCPA;
  private final Map<String, AutomatonVariable> vars;
  private transient AutomatonInternalState internalState;
  private final ImmutableList<AStatement> assumptions;
  private transient final ExpressionTree<AExpression> candidateInvariants;
  private int matches = 0;
  private int failedMatches = 0;
  private final AutomatonSafetyProperty violatedPropertyDescription;

  static AutomatonState automatonStateFactory(
      Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState,
      ControlAutomatonCPA pAutomatonCPA,
      ImmutableList<AStatement> pAssumptions,
      ExpressionTree<AExpression> pCandidateInvariants,
      int successfulMatches,
      int failedMatches,
      AutomatonSafetyProperty violatedPropertyDescription) {

    if (pInternalState == AutomatonInternalState.BOTTOM) {
      return pAutomatonCPA.getBottomState();
    } else {
      return new AutomatonState(
          pVars,
          pInternalState,
          pAutomatonCPA,
          pAssumptions,
          pCandidateInvariants,
          successfulMatches,
          failedMatches,
          violatedPropertyDescription);
    }
  }

  static AutomatonState automatonStateFactory(
      Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState,
      ControlAutomatonCPA pAutomatonCPA,
      int successfulMatches,
      int failedMatches,
      AutomatonSafetyProperty violatedPropertyDescription) {
    return automatonStateFactory(
        pVars,
        pInternalState,
        pAutomatonCPA,
        ImmutableList.<AStatement>of(),
        ExpressionTrees.<AExpression>getTrue(),
        successfulMatches,
        failedMatches,
        violatedPropertyDescription);
  }

  private AutomatonState(
      Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState,
      ControlAutomatonCPA pAutomatonCPA,
      ImmutableList<AStatement> pAssumptions,
      ExpressionTree<AExpression> pCandidateInvariants,
      int successfulMatches,
      int failedMatches,
      AutomatonSafetyProperty pViolatedPropertyDescription) {

    this.vars = checkNotNull(pVars);
    this.internalState = checkNotNull(pInternalState);
    this.automatonCPA = checkNotNull(pAutomatonCPA);
    this.matches = successfulMatches;
    this.failedMatches = failedMatches;
    this.assumptions = pAssumptions;
    this.candidateInvariants = pCandidateInvariants;

    if (isTarget()) {
      checkNotNull(pViolatedPropertyDescription);
      violatedPropertyDescription = pViolatedPropertyDescription;
    } else {
      violatedPropertyDescription = null;
    }
  }

  @Override
  public boolean isTarget() {
    return this.automatonCPA.isTreatingErrorsAsTargets() && internalState.isTarget();
  }

  @Override
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    checkState(isTarget());
    return ImmutableSet.<Property>of(violatedPropertyDescription);
  }

  Optional<AutomatonSafetyProperty> getOptionalViolatedPropertyDescription() {
    return Optional.<AutomatonSafetyProperty>fromNullable(violatedPropertyDescription);
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj == null) {
      return false;
    }
    if (!pObj.getClass().equals(this.getClass())) {
      return false;
    }
    AutomatonState otherState = (AutomatonState) pObj;

    return this.internalState.equals(otherState.internalState)
        && this.vars.equals(otherState.vars);
  }

  @Override
  public int hashCode() {
    // Important: we cannot use vars.hashCode(), because the hash code of a map
    // depends on the hash code of its values, and those may change.
    return internalState.hashCode();
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }

  public List<CStatementEdge> getAsStatementEdges(String cFunctionName) {
    if (assumptions.isEmpty()) {
      return ImmutableList.of();
    }

    List<CStatementEdge> result = new ArrayList<>(assumptions.size());
    for (AStatement statement : assumptions) {

      if (statement instanceof CAssignment) {
        CAssignment assignment = (CAssignment) statement;

        if (assignment.getRightHandSide() instanceof CExpression) {


          result.add(
              new CStatementEdge(assignment.toASTString(), assignment, assignment.getFileLocation(),
                  new CFANode(cFunctionName), new CFANode(cFunctionName)));
        } else if (assignment.getRightHandSide() instanceof CFunctionCall) {
          //TODO FunctionCalls, ExpressionStatements etc
        }
      }
    }
    return result;
  }

  @Override
  public List<AssumeEdge> getAsAssumeEdges(String cFunctionName) {
    if (assumptions.isEmpty()) {
      return ImmutableList.of();
    }

    List<AssumeEdge> result = new ArrayList<>(assumptions.size());
    CBinaryExpressionBuilder expressionBuilder =
        new CBinaryExpressionBuilder(automatonCPA.getMachineModel(), automatonCPA.getLogManager());
    for (AStatement statement : assumptions) {

      if (statement instanceof CAssignment) {
        CAssignment assignment = (CAssignment) statement;

        if (assignment.getRightHandSide() instanceof CExpression) {

          CExpression expression = (CExpression) assignment.getRightHandSide();
          CBinaryExpression assumeExp =
              expressionBuilder.buildBinaryExpressionUnchecked(assignment.getFileLocation(),
                  assignment.getLeftHandSide(),
                  expression,
                  CBinaryExpression.BinaryOperator.EQUALS);

          result.add(new CAssumeEdge(assignment.toASTString(), assignment.getFileLocation(),
              new CFANode(cFunctionName), new CFANode(cFunctionName), assumeExp, true));
        } else if (assignment.getRightHandSide() instanceof CFunctionCall) {
          //TODO FunctionCalls, ExpressionStatements etc
        }
      }

      if (statement instanceof CExpressionStatement) {
        if (((CExpressionStatement) statement).getExpression()
            .getExpressionType() instanceof CSimpleType
            && ((CSimpleType) (((CExpressionStatement) statement).getExpression()
            .getExpressionType())).getType().isIntegerType()) {
          result.add(new CAssumeEdge(statement.toASTString(), statement.getFileLocation(),
              new CFANode(cFunctionName), new CFANode(cFunctionName),
              ((CExpressionStatement) statement).getExpression(), true));
        }
      }
    }
    return result;
  }

  /**
   * returns the name of the automaton, to whom this state belongs to (the name is specified in the
   * automaton file) forwards to <code>automatonCPA.getAutomaton().getName()</code>.
   *
   * @return name of automaton
   */
  public String getOwningAutomatonName() {
    return automatonCPA.getAutomaton().getName();
  }

  public Automaton getOwningAutomaton() {
    return automatonCPA.getAutomaton();
  }

  @Override
  public String toString() {
    return (automatonCPA != null ? automatonCPA.getAutomaton().getName() + ": " : "")
        + internalState.getName() + ' ' + Joiner.on(' ').withKeyValueSeparator("=").join(vars);
  }

  @Override
  public String toDOTLabel() {
    if (!internalState.getName().equals("Init")) {
      return (automatonCPA != null ? automatonCPA.getAutomaton().getName() + ": " : "")
          + internalState.getName();
    }
    return "";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  /**
   * The UnknownState represents one of the States following a normal State of the Automaton. Which
   * State is the correct following state could not be determined so far. This Class is used if
   * during a "getAbstractSuccessor" call the abstract successor could not be determined. During the
   * subsequent "strengthen" call enough information should be available to determine a normal
   * AutomatonState as following State.
   */
  static class AutomatonUnknownState extends AutomatonState {
    private static final long serialVersionUID = -2010032222354565037L;
    private final AutomatonState previousState;

    AutomatonUnknownState(AutomatonState pPreviousState) {
      super(
          pPreviousState.getVars(),
          pPreviousState.getInternalState(),
          pPreviousState.automatonCPA,
          pPreviousState.getAssumptions(),
          pPreviousState.getCandidateInvariants(),
          -1,
          -1,
          null);
      previousState = pPreviousState;
    }

    AutomatonState getPreviousState() {
      return previousState;
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj == null) {
        return false;
      }
      if (!pObj.getClass().equals(this.getClass())) {
        return false;
      }
      AutomatonUnknownState otherState = (AutomatonUnknownState) pObj;
      return previousState.equals(otherState.previousState);
    }

    @Override
    public int hashCode() {
      return this.previousState.hashCode() + 724;
    }

    @Override
    public String toString() {
      return "AutomatonUnknownState<" + previousState.toString() + ">";
    }
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    /*
     * Check properties of the state, which are either:
     * a) "internalStateIsTarget", to check if the internal state is a target
     *    state.
     * b) "state == name-of-state" where name-of-state is the name of the
     *    internal state, e.g. _predefinedState_ERROR, _predefinedState_BOTTOM,
     *    _predefinedState_BREAK.
     * c) "name-of-variable == int-value" where name-of-variable is the name of
     *    an automaton variable and int-value is an integer value.
     */
    if (pProperty.equalsIgnoreCase(INTERNAL_STATE_IS_TARGET_PROPERTY)) {
      return getInternalState().isTarget();
    }
    String[] parts = pProperty.split("==");
    if (parts.length != 2) {
      throw new InvalidQueryException("The Query \"" + pProperty
          + "\" is invalid. Could not split the property string correctly.");
    } else {
      String left = parts[0].trim();
      String right = parts[1].trim();
      if (left.equalsIgnoreCase("state")) {
        return this.getInternalState().getName().equals(right);
      } else {
        AutomatonVariable var = vars.get(left);
        if (var != null) {
          // is a local variable
          try {
            int val = Integer.parseInt(right);
            return var.getValue() == val;
          } catch (NumberFormatException e) {
            throw new InvalidQueryException(
                "The Query \"" + pProperty + "\" is invalid. Could not parse the int \"" + right
                    + "\".");
          }
        } else {
          throw new InvalidQueryException("The Query \"" + pProperty
              + "\" is invalid. Only accepting \"State == something\" and \"varname = something\" queries so far.");
        }
      }
    }
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    // allows to set values of Automaton variables like "x:=6"
    String[] parts = pModification.split(":=");
    if (parts.length != 2) {
      throw new InvalidQueryException(
          "The Query \"" + pModification + "\" is invalid. Could not split the string correctly.");
    } else {
      String left = parts[0].trim();
      String right = parts[1].trim();
      AutomatonVariable var = this.vars.get(left);
      if (var != null) {
        try {
          int val = Integer.parseInt(right);
          var.setValue(val);
        } catch (NumberFormatException e) {
          throw new InvalidQueryException(
              "The Query \"" + pModification + "\" is invalid. Could not parse the int \"" + right
                  + "\".");
        }
      } else {
        throw new InvalidQueryException(
            "Could not modify the variable \"" + left + "\" (Variable not found)");
      }
    }
  }

  @Override
  public Boolean evaluateProperty(String pProperty) throws InvalidQueryException {
    return Boolean.valueOf(checkProperty(pProperty));
  }

  @Override
  public String getCPAName() {
    return AutomatonState.AutomatonAnalysisNamePrefix + automatonCPA.getAutomaton().getName();
  }

  @Override
  public ImmutableList<AStatement> getAssumptions() {
    return assumptions;
  }

  public ExpressionTree<AExpression> getCandidateInvariants() {
    return candidateInvariants;
  }

  AutomatonInternalState getInternalState() {
    return internalState;
  }

  public String getInternalStateName() {
    return internalState.getName();
  }

  Map<String, AutomatonVariable> getVars() {
    return vars;
  }

  ControlAutomatonCPA getAutomatonCPA() {
    return automatonCPA;
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeInt(internalState.getStateId());
    out.writeObject(automatonCPA.getAutomaton().getName());
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    int stateId = in.readInt();
    internalState = GlobalInfo.getInstance().getAutomatonInfo().getStateById(stateId);
    automatonCPA =
        GlobalInfo.getInstance().getAutomatonInfo().getCPAForAutomaton((String) in.readObject());
  }

  public int getMatches() {
    return matches;
  }

  public int getFailedMatches() {
    return failedMatches;
  }

  public void setFailedMatches(int pFailedMatches) {
    failedMatches = pFailedMatches;
  }

  public void setMatches(int pMatches) {
    matches = pMatches;
  }
}
