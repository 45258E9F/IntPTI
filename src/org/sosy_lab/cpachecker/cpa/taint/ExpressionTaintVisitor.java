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

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.FunctionRegistrant;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.taint.TaintState.Taint;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.List;

public class ExpressionTaintVisitor
    extends DefaultCExpressionVisitor<Taint, UnrecognizedCCodeException>
    implements CRightHandSideVisitor<Taint, UnrecognizedCCodeException> {

  private final TaintState innerState;
  private final List<AbstractState> otherStates;

  ExpressionTaintVisitor(TaintState pState, List<AbstractState> pOtherStates) {
    innerState = pState;
    otherStates = pOtherStates;
  }

  @Override
  public Taint visit(CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    AccessPath path = e.accept(new AccessPathVisitorForTaint(otherStates));
    return innerState.queryTaint(path);
  }

  @Override
  public Taint visit(CBinaryExpression e) throws UnrecognizedCCodeException {
    CExpression op1 = e.getOperand1();
    CExpression op2 = e.getOperand2();
    Taint taint = op1.accept(this);
    if (taint == Taint.TAINT) {
      return Taint.TAINT;
    }
    taint = op2.accept(this);
    return taint;
  }

  @Override
  public Taint visit(CCastExpression e) throws UnrecognizedCCodeException {
    CExpression op = e.getOperand();
    return op.accept(this);
  }

  @Override
  public Taint visit(CFieldReference e) throws UnrecognizedCCodeException {
    AccessPath path = e.accept(new AccessPathVisitorForTaint(otherStates));
    return innerState.queryTaint(path);
  }

  @Override
  public Taint visit(CIdExpression e) throws UnrecognizedCCodeException {
    AccessPath path = new AccessPath(e.getDeclaration());
    return innerState.queryTaint(path);
  }

  @Override
  public Taint visit(CUnaryExpression e) throws UnrecognizedCCodeException {
    CExpression operand = e.getOperand();
    return operand.accept(this);
  }

  @Override
  public Taint visit(CPointerExpression e) throws UnrecognizedCCodeException {
    AccessPath path = e.accept(new AccessPathVisitorForTaint(otherStates));
    return innerState.queryTaint(path);
  }

  @Override
  public Taint visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCCodeException {
    // for non-registered library functions, their return values are regarded as taint
    // traverse CPAs
    Optional<ConfigurableProgramAnalysis> cpa = GlobalInfo.getInstance().getCPA();
    if (cpa.isPresent()) {
      ConfigurableProgramAnalysis tCPA = cpa.get();
      return handleFunctionCall(tCPA, pIastFunctionCallExpression);
    }
    return Taint.TAINT;
  }

  private Taint handleFunctionCall(
      ConfigurableProgramAnalysis cpa,
      CFunctionCallExpression callExpression) {
    if (cpa instanceof WrapperCPA) {
      Iterable<ConfigurableProgramAnalysis> cpas = ((WrapperCPA) cpa).getWrappedCPAs();
      for (ConfigurableProgramAnalysis singleCPA : cpas) {
        if (handleFunctionCall(singleCPA, callExpression) == Taint.CLEAN) {
          return Taint.CLEAN;
        }
      }
      return Taint.TAINT;
    } else {
      if (cpa instanceof FunctionRegistrant) {
        boolean hit = ((FunctionRegistrant) cpa).retrieveCall(callExpression);
        if (hit) {
          return Taint.CLEAN;
        }
        return Taint.TAINT;
      }
      // if current CPA is not a function registrant, it cannot analyze any library function
      // specifically
      return Taint.TAINT;
    }
  }

  @Override
  protected Taint visitDefault(CExpression exp) throws UnrecognizedCCodeException {
    // including cases such as integer literal
    return Taint.CLEAN;
  }
}
