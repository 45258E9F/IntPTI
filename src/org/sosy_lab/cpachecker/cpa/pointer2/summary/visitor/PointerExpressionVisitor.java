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
package org.sosy_lab.cpachecker.cpa.pointer2.summary.visitor;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.List;

/**
 * Created by landq on 11/28/16.
 */
public class PointerExpressionVisitor extends
                                      DefaultCExpressionVisitor<MemoryLocation, UnrecognizedCCodeException> {

  String functionName;
  MachineModel machineModel;

  public PointerExpressionVisitor(String pFunctionName, MachineModel pMachineModel) {
    functionName = pFunctionName;
    machineModel = pMachineModel;
  }

  @Override
  protected MemoryLocation visitDefault(CExpression pExp) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public MemoryLocation visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws UnrecognizedCCodeException {

    CExpression arrayExpression = pIastArraySubscriptExpression.getArrayExpression();

    CType arrayExpressionType = arrayExpression.getExpressionType().getCanonicalType();

      /* A subscript Expression can also include an Array Expression.
      In that case, it is a dereference*/
    if (arrayExpressionType instanceof CPointerType) {
      //return null;
    }

    CExpression subscript = pIastArraySubscriptExpression.getSubscriptExpression();

    CType elementType = pIastArraySubscriptExpression.getExpressionType();

    MemoryLocation arrayLoc = arrayExpression.accept(this);

    if (arrayLoc == null) {
      return null;
    }

    if (!(subscript instanceof CIntegerLiteralExpression)) {
      return null;
    }

    long typeSize = machineModel.getSizeof(elementType);

    long subscriptOffset;
    subscriptOffset = ((CIntegerLiteralExpression) subscript).getValue().longValue() * typeSize;

    //YX, for array int a[2][3], a[1][0] first get a[1]'s offset is 12, then 12 should be added when calculating the offset of a[1][0]
    if (arrayLoc.isReference()) {
      subscriptOffset = subscriptOffset + arrayLoc.getOffset();
    }

    if (arrayLoc.isOnFunctionStack()) {

      return MemoryLocation.valueOf(arrayLoc.getFunctionName(),
          arrayLoc.getIdentifier(),
          subscriptOffset);
    } else {

      return MemoryLocation.valueOf(arrayLoc.getIdentifier(),
          subscriptOffset);
    }
  }

  @Override
  public MemoryLocation visit(CFieldReference pIastFieldReference)
      throws UnrecognizedCCodeException {

    if (pIastFieldReference.isPointerDereference()) {
      //return null;
    }

    if (!(pIastFieldReference.getFieldOwner() instanceof CLeftHandSide)) {
      return null;
    }

    CLeftHandSide fieldOwner = (CLeftHandSide) pIastFieldReference.getFieldOwner();

    MemoryLocation memLocOfFieldOwner = fieldOwner.accept(this);

    if (memLocOfFieldOwner == null) {
      return null;
    }

    return getStructureFieldLocationFromRelativePoint(memLocOfFieldOwner,
        pIastFieldReference.getFieldName(),
        fieldOwner.getExpressionType());
  }

  protected MemoryLocation getStructureFieldLocationFromRelativePoint(
      MemoryLocation pStartLocation,
      String pFieldName, CType pOwnerType) throws UnrecognizedCCodeException {

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

  private Integer getFieldOffset(CType ownerType, String fieldName)
      throws UnrecognizedCCodeException {

    if (ownerType instanceof CElaboratedType) {
      return getFieldOffset(((CElaboratedType) ownerType).getRealType(), fieldName);
    } else if (ownerType instanceof CCompositeType) {
      return getFieldOffset((CCompositeType) ownerType, fieldName);
    } else if (ownerType instanceof CPointerType) {
      //return null;
      return getFieldOffset(((CPointerType) ownerType).getType(), fieldName);
    } else if (ownerType instanceof CArrayType) {
      return getFieldOffset(((CArrayType) ownerType).getType(), fieldName);
    }

    throw new AssertionError();
  }

  private Integer getFieldOffset(CCompositeType ownerType, String fieldName) {

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

  protected MemoryLocation getArraySlotLocationFromArrayStart(
      final MemoryLocation pArrayStartLocation,
      final int pSlotNumber,
      final CArrayType pArrayType) {

    long typeSize = machineModel.getSizeof(pArrayType.getType());
    long offset = typeSize * pSlotNumber;
    long baseOffset = pArrayStartLocation.isReference() ? pArrayStartLocation.getOffset() : 0;

    if (pArrayStartLocation.isOnFunctionStack()) {

      return MemoryLocation.valueOf(
          pArrayStartLocation.getFunctionName(),
          pArrayStartLocation.getIdentifier(),
          baseOffset + offset);
    } else {
      return MemoryLocation.valueOf(pArrayStartLocation.getIdentifier(), baseOffset + offset);
    }
  }

  @Override
  public MemoryLocation visit(CIdExpression idExp) throws UnrecognizedCCodeException {

    if (idExp.getDeclaration() != null) {
      return MemoryLocation.valueOf(idExp.getDeclaration().getQualifiedName());
    }

    boolean isGlobal = ForwardingTransferRelation.isGlobal(idExp);

    if (isGlobal) {
      return MemoryLocation.valueOf(idExp.getName());
    } else {
      return MemoryLocation.valueOf(functionName, idExp.getName());
    }
  }

  @Override
  public MemoryLocation visit(CPointerExpression pPointerExpression)
      throws UnrecognizedCCodeException {
    return pPointerExpression.getOperand().accept(this);
  }

  @Override
  public MemoryLocation visit(CCastExpression pE) throws UnrecognizedCCodeException {
    // TODO reinterpretations for ValueAnalysis
    return pE.getOperand().accept(this);
  }
}
