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
package org.sosy_lab.cpachecker.core.phase.fix;

import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_assign;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_binaryAnd;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_binaryAndAssign;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_binaryOr;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_binaryOrAssign;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_binaryXor;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_binaryXorAssign;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_divide;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_divideAssign;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_minusAssign;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_modulo;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_moduloAssign;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_multiply;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_multiplyAssign;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_plusAssign;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_shiftLeft;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_shiftLeftAssign;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_shiftRight;
import static org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.op_shiftRightAssign;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_bracketedPrimary;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_minus;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_plus;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_postFixDecr;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_postFixIncr;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_prefixDecr;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_prefixIncr;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_tilde;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTConditionalExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.ASTTypeConverter;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.bugfix.MutableASTForFix;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class DeriveASTTypeVisitor {

  private final MachineModel machineModel;

  private final Map<MutableASTForFix, CSimpleType> newCasts;
  private final Map<String, CSimpleType> newDecls;

  DeriveASTTypeVisitor(
      MachineModel pMachineModel, Map<MutableASTForFix, CSimpleType> pNewCasts,
      Map<String, CSimpleType> pNewDecls) {
    machineModel = pMachineModel;
    newCasts = pNewCasts;
    newDecls = pNewDecls;
  }

  /**
   * Visit AST node to infer type information
   *
   * @return expression type. NULL if the certain type is not of simple type
   */
  @Nullable
  CSimpleType visit(MutableASTForFix astNode) {
    if (newCasts.containsKey(astNode)) {
      return newCasts.get(astNode);
    }
    IASTNode wrappedNode = astNode.getWrappedNode();
    if (wrappedNode instanceof IASTUnaryExpression) {
      MutableASTForFix subNode = astNode.getChildren().get(0);
      CSimpleType subType = visit(subNode);
      if (subType != null) {
        int operator = ((IASTUnaryExpression) wrappedNode).getOperator();
        switch (operator) {
          case op_bracketedPrimary:
            return subType;
          case op_minus:
          case op_plus:
          case op_postFixDecr:
          case op_postFixIncr:
          case op_prefixDecr:
          case op_prefixIncr:
          case op_tilde:
            subType = machineModel.getPromotedCType(subType);
            return subType;
        }
      }
    } else if (wrappedNode instanceof IASTBinaryExpression) {
      MutableASTForFix op1 = astNode.getChildren().get(0);
      MutableASTForFix op2 = astNode.getChildren().get(1);
      CSimpleType subType1 = visit(op1);
      CSimpleType subType2 = visit(op2);
      if (subType1 != null && subType2 != null) {
        int operator = ((IASTBinaryExpression) wrappedNode).getOperator();
        switch (operator) {
          case op_assign:
          case op_binaryAndAssign:
          case op_binaryOrAssign:
          case op_binaryXorAssign:
          case op_divideAssign:
          case op_minusAssign:
          case op_moduloAssign:
          case op_multiplyAssign:
          case op_plusAssign:
          case op_shiftLeftAssign:
          case op_shiftRightAssign:
            return subType1;
          case op_binaryAnd:
          case op_binaryOr:
          case op_binaryXor:
          case op_divide:
          case IASTBinaryExpression.op_minus:
          case op_modulo:
          case IASTBinaryExpression.op_plus:
          case op_multiply:
          case op_shiftLeft:
          case op_shiftRight:
            return CBinaryExpressionBuilder.getCommonSimpleTypeForBinaryOperation(machineModel,
                subType1, subType2);
        }
      }
    } else if (wrappedNode instanceof IASTConditionalExpression) {
      MutableASTForFix opPos = astNode.getChildren().get(1);
      MutableASTForFix opNeg = astNode.getChildren().get(2);
      CSimpleType type1 = visit(opPos);
      CSimpleType type2 = visit(opNeg);
      if (type1 != null && type2 != null) {
        return CBinaryExpressionBuilder.getCommonSimpleTypeForBinaryOperation(machineModel,
            type1, type2);
      }
    } else if (wrappedNode instanceof IASTIdExpression) {
      String name = ((IASTIdExpression) wrappedNode).getName().getRawSignature();
      // However, we do not know if current name is enclosed by certain function definition
      IASTFunctionDefinition parentFunc = getParentFunction(wrappedNode);
      if (parentFunc != null) {
        IASTFunctionDeclarator declarator = parentFunc.getDeclarator();
        String funcName = declarator.getName().getRawSignature();
        String transformedName = GlobalInfo.getInstance().getPreInfoManager().getTransformedName
            (parentFunc.getContainingFilename(), funcName);
        if (transformedName != null) {
          funcName = transformedName;
        }
        name = funcName.concat("::").concat(name);
      }
      if (newDecls.containsKey(name)) {
        return newDecls.get(name);
      }
    } else if (wrappedNode instanceof IASTExpressionList) {
      List<MutableASTForFix> children = astNode.getChildren();
      MutableASTForFix lastNode = children.get(children.size() - 1);
      CSimpleType type = visit(lastNode);
      if (type != null) {
        return type;
      }
    }
    // we infer its original type, by default
    if (wrappedNode instanceof IASTExpression) {
      IType rawType = ((IASTExpression) wrappedNode).getExpressionType();
      while (rawType instanceof ITypedef) {
        rawType = ((ITypedef) rawType).getType();
      }
      if (rawType instanceof IBasicType) {
        CType convType = ASTTypeConverter.conv((IBasicType) rawType);
        if (convType instanceof CSimpleType) {
          return (CSimpleType) convType;
        }
      }
    }
    return null;
  }

  @Nullable
  private IASTFunctionDefinition getParentFunction(IASTNode node) {
    IASTNode currentNode = node;
    while (currentNode != null) {
      if (currentNode instanceof IASTFunctionDefinition) {
        return (IASTFunctionDefinition) currentNode;
      }
      currentNode = currentNode.getParent();
    }
    return null;
  }

}
