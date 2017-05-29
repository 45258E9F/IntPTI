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
package org.sosy_lab.cpachecker.core.phase.fix.util;

import com.google.common.base.Joiner;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.bugfix.MutableASTForFix;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFix;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFix.IntegerFixMode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class IntegerFixDisplayInfo {

  private final UUID id;
  private final IntegerFix fix;
  private final MutableASTForFix ast;
  private final List<IntegerFixDisplayInfo> children = new ArrayList<>();

  private IntegerFixDisplayInfo(UUID pID, IntegerFix pFix, MutableASTForFix pAST) {
    id = pID;
    fix = pFix;
    ast = pAST;
  }

  public static IntegerFixDisplayInfo of(UUID pID, IntegerFix pFix, MutableASTForFix pAST) {
    return new IntegerFixDisplayInfo(pID, pFix, pAST);
  }

  public void addChild(IntegerFixDisplayInfo pInfo) {
    children.add(pInfo);
  }

  public IASTFileLocation getLocation() {
    return ast.getWrappedNode().getFileLocation();
  }

  public IntegerFixMode getFixMode() {
    return fix.getFixMode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"UUID\":").append("\"").append(id.toString()).append("\"").append(",");
    sb.append("\"mode\":").append("\"").append(fix.getFixMode().getName()).append("\"").append(",");
    CSimpleType targetType = fix.getTargetType();
    assert (targetType != null);
    sb.append("\"type\":").append("\"").append(targetType.toString()).append("\"").append(",");
    // file location info
    IASTFileLocation loc = ast.getWrappedNode().getFileLocation();
    sb.append("\"startLine\":").append(loc.getStartingLineNumber()).append(",");
    sb.append("\"endLine\":").append(loc.getEndingLineNumber()).append(",");
    sb.append("\"offset\":").append(loc.getNodeOffset()).append(",");
    sb.append("\"length\":").append(loc.getNodeLength()).append(",");
    // add children
    sb.append("\"children\":");
    if (children.isEmpty()) {
      sb.append("[]");
    } else {
      List<String> childList = new ArrayList<>(children.size());
      StringBuilder subSb = new StringBuilder();
      subSb.append("[");
      for (IntegerFixDisplayInfo child : children) {
        childList.add(child.toString());
      }
      subSb.append(Joiner.on(',').join(childList));
      subSb.append("]");
      sb.append(subSb.toString());
    }
    sb.append("}");
    return sb.toString();
  }
}
