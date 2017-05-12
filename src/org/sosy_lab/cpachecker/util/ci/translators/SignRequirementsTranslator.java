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
package org.sosy_lab.cpachecker.util.ci.translators;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.sign.SIGN;
import org.sosy_lab.cpachecker.cpa.sign.SignState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;


public class SignRequirementsTranslator extends CartesianRequirementsTranslator<SignState> {

  public SignRequirementsTranslator(final LogManager pLog) {
    super(SignState.class, pLog);
  }

  @Override
  protected List<String> getVarsInRequirements(final SignState pRequirement) {
    return new ArrayList<>(pRequirement.getSignMapView().keySet());
  }

  @Override
  protected List<String> getListOfIndependentRequirements(
      final SignState pRequirement, final SSAMap pIndices,
      final @Nullable Collection<String> pRequiredVars) {
    List<String> list = new ArrayList<>();
    for (String var : pRequirement.getSignMapView().keySet()) {
      if (pRequiredVars == null || pRequiredVars.contains(var)) {
        list.add(
            getRequirement(getVarWithIndex(var, pIndices), pRequirement.getSignMapView().get(var)));
      }
    }
    return list;
  }

  private String getRequirement(final String var, final SIGN sign) {
    StringBuilder sb = new StringBuilder();
    Preconditions.checkArgument(sign != SIGN.EMPTY);
    Preconditions.checkArgument(sign != SIGN.ALL);

    switch (sign) {
      case PLUS:
        sb.append("(> ");
        sb.append(var);
        sb.append(" 0)");
        break;
      case MINUS:
        sb.append("(< ");
        sb.append(var);
        sb.append(" 0)");
        break;
      case ZERO:
        sb.append("(= ");
        sb.append(var);
        sb.append(" 0)");
        break;
      case PLUSMINUS:
        sb.append("(or (> ");
        sb.append(var);
        sb.append(" 0) (< ");
        sb.append(var);
        sb.append(" 0))");
        break;
      case PLUS0:
        sb.append("(>= ");
        sb.append(var);
        sb.append(" 0)");
        break;
      case MINUS0:
        sb.append("(<= ");
        sb.append(var);
        sb.append(" 0)");
        break;
      default:
        // should never happen
        assert (false);
    }

    return sb.toString();
  }

}
