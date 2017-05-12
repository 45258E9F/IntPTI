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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;

import java.util.HashMap;
import java.util.Map;


public class TypeHandlerWithPointerAliasing extends CtoFormulaTypeHandler {

  private final CSizeofVisitor sizeofVisitor;

  /*
   * Use Multiset<String> instead of Map<String, Integer> because it is more
   * efficient. The integer value is stored as the number of instances of any
   * element in the Multiset. So instead of calling map.get(key) we just use
   * Multiset.count(key). This is better because the Multiset internally uses
   * modifiable integers instead of the immutable Integer class.
   */
  private final Multiset<CCompositeType> sizes = HashMultiset.create();
  private final Map<CCompositeType, Multiset<String>> offsets = new HashMap<>();

  public TypeHandlerWithPointerAliasing(
      LogManager pLogger, MachineModel pMachineModel,
      FormulaEncodingWithPointerAliasingOptions pOptions) {
    super(pLogger, pMachineModel);

    sizeofVisitor = new CSizeofVisitor(pMachineModel, pOptions);
  }

  /**
   * The method is used to speed up {@code sizeof} computation by caching sizes of declared
   * composite types.
   *
   * @param cType the type of which the size should be retrieved
   */
  @Override
  public int getSizeof(CType cType) {
    cType = CTypeUtils.simplifyType(cType);
    if (cType instanceof CCompositeType) {
      if (sizes.contains(cType)) {
        return sizes.count(cType);
      } else {
        return cType.accept(sizeofVisitor);
      }
    } else {
      return cType.accept(sizeofVisitor);
    }
  }


  /**
   * The method is used to speed up member offset computation for declared composite types.
   */
  public int getOffset(CCompositeType compositeType, final String memberName) {
    compositeType = (CCompositeType) CTypeUtils.simplifyType(compositeType);
    assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: "
        + compositeType;
    Multiset<String> multiset = offsets.get(compositeType);
    if (multiset == null) {
      addCompositeTypeToCache(compositeType);
      multiset = offsets.get(compositeType);
      assert multiset != null : "Failed adding composite type to cache: " + compositeType;
    }
    return multiset.count(memberName);
  }

  /**
   * Adds the declared composite type to the cache saving its size as well as the offset of every
   * member of the composite.
   */
  public void addCompositeTypeToCache(CCompositeType compositeType) {
    compositeType = (CCompositeType) CTypeUtils.simplifyType(compositeType);
    if (offsets.containsKey(compositeType)) {
      // Support for empty structs though it's a GCC extension
      assert sizes.contains(compositeType) || Integer.valueOf(0)
          .equals(compositeType.accept(sizeofVisitor)) :
          "Illegal state of PointerTargetSet: no size for type:" + compositeType;
      return; // The type has already been added
    }

    final Integer size = compositeType.accept(sizeofVisitor);

    assert size != null : "Can't evaluate size of a composite type: " + compositeType;

    assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: "
        + compositeType;

    final Multiset<String> members = HashMultiset.create();
    int offset = 0;
    for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
      members.setCount(memberDeclaration.getName(), offset);
      final CType memberType = CTypeUtils.simplifyType(memberDeclaration.getType());
      final CCompositeType memberCompositeType;
      if (memberType instanceof CCompositeType) {
        memberCompositeType = (CCompositeType) memberType;
        if (memberCompositeType.getKind() == ComplexTypeKind.STRUCT ||
            memberCompositeType.getKind() == ComplexTypeKind.UNION) {
          if (!offsets.containsKey(memberCompositeType)) {
            assert !sizes.contains(memberCompositeType) :
                "Illegal state of PointerTargetSet: size for type:" + memberCompositeType;
            addCompositeTypeToCache(memberCompositeType);
          }
        }
      } else {
        memberCompositeType = null;
      }
      if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
        if (memberCompositeType != null) {
          offset += machineModel.getPadding(offset, memberCompositeType);
          offset += sizes.count(memberCompositeType);
        } else {
          offset += machineModel.getPadding(offset, memberDeclaration.getType());
          offset += memberDeclaration.getType().accept(sizeofVisitor);
        }
      }
    }
    offset += machineModel.getPadding(offset, compositeType);

    assert compositeType.getKind() != ComplexTypeKind.STRUCT || offset == size :
        "Incorrect sizeof or offset of the last member: " + compositeType;

    sizes.setCount(compositeType, size);
    offsets.put(compositeType, members);
  }
}
