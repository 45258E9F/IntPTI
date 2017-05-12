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
package org.sosy_lab.cpachecker.cpa.shape.visitors.value;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SEs;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.DeclaredTypeData;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicExpressionAndState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A type visitor for deriving the size of a given type. This visitor is designed for deriving
 * size of type at declaration.
 */
public class CSizeofDeclarationVisitor
    implements CTypeVisitor<List<DeclaredTypeData>, CPATransferException> {

  private final CFAEdge cfaEdge;
  private final ShapeState readableState;
  private final List<AbstractState> otherStates;
  private final MachineModel machineModel;

  private static final CProblemType negArray = new CProblemType("neg_index");

  public CSizeofDeclarationVisitor(
      ShapeState pState, List<AbstractState> pOtherStates, CFAEdge
      pEdge, MachineModel pModel) {
    readableState = pState;
    otherStates = pOtherStates;
    cfaEdge = pEdge;
    machineModel = pModel;
  }

  @Override
  public List<DeclaredTypeData> visit(CArrayType pArrayType) throws CPATransferException {
    CType elementType = pArrayType.getType();
    CExpression length = pArrayType.getLength();
    List<DeclaredTypeData> subTypeData = CoreShapeAdapter.getInstance().evaluateDeclaredSizeof
        (readableState, otherStates, cfaEdge, elementType);
    List<DeclaredTypeData> results = new ArrayList<>();
    for (DeclaredTypeData typeData : subTypeData) {
      ShapeState newState = typeData.getState();
      CType type = typeData.getTrueType();
      boolean isVLA = typeData.isContainsVLA();
      SymbolicExpression size = typeData.getSize();
      if (length instanceof CIntegerLiteralExpression) {
        BigInteger value = ((CIntegerLiteralExpression) length).getValue();
        if (value.signum() < 0) {
          newState = newState.setInvalidWrite();
          results.add(DeclaredTypeData.of(newState, isVLA, SEs.toUnknown(negArray), negArray));
        } else {
          CType newType = new CArrayType(pArrayType.isConst(), pArrayType.isVolatile(), type,
              length);
          SymbolicExpression newSize = SEs.multiply(size, SEs.toConstant(KnownExplicitValue.valueOf
                  (((CIntegerLiteralExpression) length).getValue()), machineModel.getSizeTType()),
              machineModel.getSizeTType(), machineModel);
          results.add(DeclaredTypeData.of(newState, isVLA, newSize, newType));
        }
        continue;
      } else {
        if (length == null) {
          // char[] x = "string"
          // int[] y = {1,2,3}
          // Note: unspecified dimension is only allowed on the first dimension. If we write the
          // code like int[][] x = {{1,2},{2,3}}, the compiler would return an error.
          Integer elementSize = null;
          assert (cfaEdge instanceof CDeclarationEdge);
          CVariableDeclaration declaration = (CVariableDeclaration) ((CDeclarationEdge) cfaEdge).
              getDeclaration();
          CInitializer initializer = declaration.getInitializer();
          if (initializer instanceof CInitializerExpression) {
            CExpression initExp = ((CInitializerExpression) initializer).getExpression();
            if (initExp instanceof CStringLiteralExpression) {
              elementSize = ((CStringLiteralExpression) initExp).getContentString().length() + 1;
            }
          } else if (initializer instanceof CInitializerList) {
            elementSize = ((CInitializerList) initializer).getInitializers().size();
          }
          if (elementSize != null) {
            CType newType = new CArrayType(pArrayType.isConst(), pArrayType.isVolatile(), type,
                CIntegerLiteralExpression.createDummyLiteral(elementSize, machineModel
                    .getSizeTType()));
            SymbolicExpression newSize = SEs.multiply(size, SEs.toConstant(KnownExplicitValue
                    .valueOf(elementSize), machineModel.getSizeTType()), machineModel.getSizeTType(),
                machineModel);
            results.add(DeclaredTypeData.of(newState, true, newSize, newType));
          } else {
            CType newType = new CArrayType(pArrayType.isConst(), pArrayType.isVolatile(), type,
                null);
            SymbolicExpression newSize = SEs.toUnknown(machineModel.getSizeTType());
            results.add(DeclaredTypeData.of(newState, true, newSize, newType));
          }
        } else {
          // T[m] x = ...
          // We attempt to evaluate the value of m at first. If the value is not explicit, the
          // corresponding type should be T[].
          List<SymbolicExpressionAndState> indexAndStates = CoreShapeAdapter.getInstance()
              .evaluateSymbolicExpression(newState, otherStates, cfaEdge, length);
          for (SymbolicExpressionAndState indexAndState : indexAndStates) {
            SymbolicExpression indexSe = indexAndState.getObject();
            ShapeState nState = indexAndState.getShapeState();
            if (SEs.isExplicit(indexSe)) {
              KnownExplicitValue expIndex = (KnownExplicitValue) indexSe.getValue();
              CType newType = new CArrayType(pArrayType.isConst(), pArrayType.isVolatile(), type,
                  CIntegerLiteralExpression.createDummyLiteral(expIndex.getAsLong(), machineModel
                      .getSizeTType()));
              SymbolicExpression newSize = SEs.multiply(size, SEs.convertTo(indexSe, machineModel
                  .getSizeTType(), machineModel), machineModel.getSizeTType(), machineModel);
              results.add(DeclaredTypeData.of(nState, true, newSize, newType));
            } else {
              CType newType = new CArrayType(pArrayType.isConst(), pArrayType.isVolatile(), type,
                  null);
              SymbolicExpression newSize = SEs.multiply(size, indexSe, machineModel.getSizeTType
                  (), machineModel);
              results.add(DeclaredTypeData.of(nState, true, newSize, newType));
            }
          }
        }
      }
    }
    return results;
  }

  @Override
  public List<DeclaredTypeData> visit(CCompositeType pCompositeType) throws CPATransferException {
    // In general, members should have complete type (which does not apply for VLA).
    // Since C99, flexible array member is supported. Typically, a flexible array member is the
    // last member of a structure and in general it is ignored.
    // Consider a structure struct S {int x; int y[];}. To allocate a space for this structure,
    // we use malloc() as follows:
    // malloc(sizeof(struct S) + 40 * sizeof(int));
    // which allocates space for struct S {int x; int y[40];}.
    SymbolicExpression size = ofSize(machineModel.getSizeof(pCompositeType));
    return Collections.singletonList(DeclaredTypeData.of(readableState, false, size,
        pCompositeType));
  }

  @Override
  public List<DeclaredTypeData> visit(CElaboratedType pElaboratedType) throws CPATransferException {
    CType def = pElaboratedType.getRealType();
    if (def != null) {
      return def.accept(this);
    }
    // refer to the implementation of {@link BaseSizeofVisitor}
    SymbolicExpression size = ofSize(machineModel.getSizeofInt());
    return Collections.singletonList(DeclaredTypeData.of(readableState, false, size,
        pElaboratedType));
  }

  @Override
  public List<DeclaredTypeData> visit(CEnumType pEnumType) throws CPATransferException {
    SymbolicExpression size = ofSize(machineModel.getSizeofInt());
    return Collections.singletonList(DeclaredTypeData.of(readableState, false, size, pEnumType));
  }

  @Override
  public List<DeclaredTypeData> visit(CFunctionType pFunctionType) throws CPATransferException {
    SymbolicExpression size = ofSize(machineModel.getSizeofPtr());
    return Collections
        .singletonList(DeclaredTypeData.of(readableState, false, size, pFunctionType));
  }

  @Override
  public List<DeclaredTypeData> visit(CPointerType pPointerType) throws CPATransferException {
    SymbolicExpression size = ofSize(machineModel.getSizeofPtr());
    return Collections.singletonList(DeclaredTypeData.of(readableState, false, size, pPointerType));
  }

  @Override
  public List<DeclaredTypeData> visit(CProblemType pProblemType) throws CPATransferException {
    return Collections.singletonList(DeclaredTypeData.of(readableState, false, SEs.toUnknown
        (pProblemType), pProblemType));
  }

  @Override
  public List<DeclaredTypeData> visit(CSimpleType pSimpleType) throws CPATransferException {
    SymbolicExpression size = ofSize(machineModel.getSizeof(pSimpleType));
    return Collections.singletonList(DeclaredTypeData.of(readableState, false, size, pSimpleType));
  }

  @Override
  public List<DeclaredTypeData> visit(CTypedefType pTypedefType) throws CPATransferException {
    return pTypedefType.getRealType().accept(this);
  }

  @Override
  public List<DeclaredTypeData> visit(CVoidType pVoidType) throws CPATransferException {
    SymbolicExpression size = ofSize(machineModel.getSizeofVoid());
    return Collections.singletonList(DeclaredTypeData.of(readableState, false, size, pVoidType));
  }

  private SymbolicExpression ofSize(int size) {
    return SEs.toConstant(KnownExplicitValue.valueOf(size), machineModel.getSizeTType());
  }
}
