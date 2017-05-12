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
package org.sosy_lab.cpachecker.cpa.range;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.FieldAccessSegment;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A visitor to decompose and traverse complex types
 */
public final class TypeRangeVisitor
    extends
    DefaultCTypeVisitor<PathCopyingPersistentTree<String, Range>, UnrecognizedCCodeException> {

  private final int maxTrackedArrays;
  private PathCopyingPersistentTree<String, Range> generatedMap;
  private final List<String> prefix;
  private final MachineModel model;

  private final boolean zeroInit;

  public TypeRangeVisitor(
      AccessPath pPrefix, int pMaxTrackedArrays, MachineModel pModel, boolean
      pZeroInit) {
    prefix = new ArrayList<>(AccessPath.toStrList(pPrefix));
    maxTrackedArrays = pMaxTrackedArrays;
    model = pModel;
    generatedMap = PathCopyingPersistentTree.of();
    zeroInit = pZeroInit;
  }

  @Override
  public PathCopyingPersistentTree<String, Range> visitDefault(CType t)
      throws UnrecognizedCCodeException {
    // nothing to do
    return generatedMap;
  }

  @Override
  public PathCopyingPersistentTree<String, Range> visit(CArrayType t)
      throws UnrecognizedCCodeException {
    CExpression length = t.getLength();
    CType elementType = t.getType();
    if (length instanceof CIntegerLiteralExpression) {
      // for most cases, the length of array should be a constant
      Long lengthValue = ((CIntegerLiteralExpression) length).asLong();
      // therefore, the range of array index is in [0, lengthValue - 1]
      for (long index = 0; index < lengthValue && index < maxTrackedArrays; index++) {
        ArrayConstIndexSegment segment = new ArrayConstIndexSegment(index);
        prefix.add(segment.getName());
        generatedMap = elementType.accept(this);
        // remove the last one and continue
        prefix.remove(prefix.size() - 1);
      }
    }
    return generatedMap;
  }

  @Override
  public PathCopyingPersistentTree<String, Range> visit(CCompositeType t)
      throws UnrecognizedCCodeException {
    if (t.getKind() == ComplexTypeKind.ENUM) {
      // when this case could be reached
      return generatedMap;
    }
    for (CCompositeTypeMemberDeclaration member : t.getMembers()) {
      CType memberType = member.getType();
      FieldAccessSegment segment = new FieldAccessSegment(member.getName());
      prefix.add(segment.getName());
      generatedMap = memberType.accept(this);
      prefix.remove(prefix.size() - 1);
    }
    return generatedMap;
  }

  @Override
  public PathCopyingPersistentTree<String, Range> visit(CElaboratedType t)
      throws UnrecognizedCCodeException {
    CComplexType realType = t.getRealType();
    if (realType == null) {
      // if no type definition is available
      return generatedMap;
    }
    return realType.accept(this);
  }

  @Override
  public PathCopyingPersistentTree<String, Range> visit(CEnumType t)
      throws UnrecognizedCCodeException {
    ImmutableList<CEnumerator> enumerators = t.getEnumerators();
    List<Long> values = new ArrayList<>();
    for (CEnumerator enumerator : enumerators) {
      values.add(enumerator.getValue());
    }
    Long min = Collections.min(values);
    Long max = Collections.max(values);
    Range newRange = new Range(min, max);
    generatedMap = generatedMap.setElementAndCopy(prefix, newRange);
    return generatedMap;
  }

  @Override
  public PathCopyingPersistentTree<String, Range> visit(CFunctionType t)
      throws UnrecognizedCCodeException {
    return generatedMap;
  }

  @Override
  public PathCopyingPersistentTree<String, Range> visit(CPointerType t)
      throws UnrecognizedCCodeException {
    return generatedMap;
  }

  @Override
  public PathCopyingPersistentTree<String, Range> visit(CProblemType t)
      throws UnrecognizedCCodeException {
    return generatedMap;
  }

  @Override
  public PathCopyingPersistentTree<String, Range> visit(CSimpleType t)
      throws UnrecognizedCCodeException {
    if (zeroInit) {
      generatedMap = generatedMap.setElementAndCopy(prefix, Range.ZERO);
    } else {
      BigInteger min = model.getMinimalIntegerValue(t);
      BigInteger max = model.getMaximalIntegerValue(t);
      Range newRange = new Range(new CompInteger(min), new CompInteger(max));
      generatedMap = generatedMap.setElementAndCopy(prefix, newRange);
    }
    return generatedMap;
  }

  @Override
  public PathCopyingPersistentTree<String, Range> visit(CTypedefType t)
      throws UnrecognizedCCodeException {
    CType resultType = t;
    while (resultType instanceof CTypedefType) {
      resultType = ((CTypedefType) resultType).getRealType();
    }
    return resultType.accept(this);
  }

  @Override
  public PathCopyingPersistentTree<String, Range> visit(CVoidType t)
      throws UnrecognizedCCodeException {
    return generatedMap;
  }
}
