/*
 * Tsmart-BD: The static analysis component of Tsmart platform
 *
 * Copyright (C) 2013-2017  Tsinghua University
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
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.FieldAccessSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;
import org.sosy_lab.cpachecker.util.access.PointerDereferenceSegment;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class providing util methods for the family of {@link Type} classes.
 */
public class Types {

  /**
   * Returns whether the first given type can hold all values the second given typ can hold.
   * The types have to belong to the same language, for example {@link CType}.
   *
   * <p>If only Java types have to be checked, no {@link MachineModel} is necessary.
   * Use {@link #canHoldAllValues(JType, JType)} instead.</p>
   *
   * @param pHoldingType  the first type
   * @param pInnerType    the second type
   * @param pMachineModel the machine model types are based on
   * @return <code>true</code> if the first given type can hold all values the second given type can
   * hold
   */
  public static boolean canHoldAllValues(
      Type pHoldingType,
      Type pInnerType,
      MachineModel pMachineModel) {

    if (pHoldingType instanceof CType) {
      checkArgument(pInnerType instanceof CType);
      CSimpleType toType;
      CSimpleType fromType;

      if (pHoldingType instanceof CPointerType) {
        toType = pMachineModel.getPointerEquivalentSimpleType();
      } else if (pHoldingType instanceof CSimpleType) {
        toType = (CSimpleType) pHoldingType;
      } else {
        return pHoldingType.equals(pInnerType);
      }

      if (pInnerType instanceof CPointerType) {
        fromType = pMachineModel.getPointerEquivalentSimpleType();
      } else if (pInnerType instanceof CSimpleType) {
        fromType = (CSimpleType) pInnerType;
      } else {
        return pHoldingType.equals(pInnerType);
      }

      return canHoldAllValues(toType, fromType, pMachineModel);

    } else {
      assert pHoldingType instanceof JType && pInnerType instanceof JType;
      return canHoldAllValues((JType) pHoldingType, (JType) pInnerType);
    }
  }

  public static boolean canHoldAllValues(
      CSimpleType pHoldingType, CSimpleType pInnerType,
      MachineModel pMachineModel) {
    final boolean isHoldingTypeSigned = pMachineModel.isSigned(pHoldingType);
    final boolean isInnerTypeSigned = pMachineModel.isSigned(pInnerType);
    final boolean isHoldingTypeFloat = pHoldingType.getType().isFloatingPointType();
    final boolean isInnerTypeFloat = pInnerType.getType().isFloatingPointType();

    if (isInnerTypeSigned && !isHoldingTypeSigned) {
      return false;
    } else if (isInnerTypeFloat && !isHoldingTypeFloat) {
      return false;
    }

    int holdingBitSize =
        pMachineModel.getSizeof(pHoldingType) * pMachineModel.getSizeofCharInBits();
    int innerBitSize = pMachineModel.getSizeof(pInnerType) * pMachineModel.getSizeofCharInBits();

    if (innerBitSize > holdingBitSize) {
      return false;
    }

    // if inner type is not signed, but holding type is, add a relevant bit to the inner type
    // (adding one to both/removing the sign bit for both does not change anything, so we only
    // change the bits in this case)
    if (isHoldingTypeSigned && !isInnerTypeSigned) {
      innerBitSize++;
    }

    if (isHoldingTypeFloat && !isInnerTypeFloat) {
      holdingBitSize = holdingBitSize / 2;
    }

    return holdingBitSize >= innerBitSize;
  }

  /**
   * Returns whether the first given type can hold all values the second given typ can hold.
   *
   * @param pHoldingType the first type
   * @param pInnerType   the second type
   * @return <code>true</code> if the first given type can hold all values the second given type can
   * hold
   */
  public static boolean canHoldAllValues(JType pHoldingType, JType pInnerType) {
    if (pHoldingType instanceof JSimpleType) {
      checkArgument(pInnerType instanceof JSimpleType);

      return canHoldAllValues((JSimpleType) pHoldingType, (JSimpleType) pInnerType);

    } else {
      return pHoldingType.equals(pInnerType);
    }
  }

  private static boolean canHoldAllValues(JSimpleType pHoldingType, JSimpleType pInnerType) {
    JBasicType holdingType = pHoldingType.getType();
    JBasicType innerType = pInnerType.getType();
    boolean canHold = false;


    switch (innerType) {
      case BOOLEAN:
        return holdingType == JBasicType.BOOLEAN;

      case CHAR:
        return holdingType == JBasicType.CHAR;

      case BYTE:
        canHold |= holdingType == JBasicType.BYTE;
        // $FALL-THROUGH$
      case SHORT:
        canHold |= holdingType == JBasicType.SHORT;
        canHold |= holdingType == JBasicType.FLOAT;
        // $FALL-THROUGH$
      case INT:
        canHold |= holdingType == JBasicType.INT;
        canHold |= holdingType == JBasicType.DOUBLE;
        // $FALL-THROUGH$
      case LONG:
        canHold |= holdingType == JBasicType.LONG;
        break;

      case FLOAT:
        canHold |= holdingType == JBasicType.FLOAT;
        // $FALL-THROUGH$
      case DOUBLE:
        canHold |= holdingType == JBasicType.DOUBLE;
        break;
      default:
        throw new AssertionError("Unhandled type " + pInnerType.getType());
    }

    return canHold;
  }

  /**
   * Check if the specified type can hold floating point values.
   *
   * @param type the specified C type
   * @return whether this type is float-relevant type
   */
  public static boolean isFloatType(CType type) {
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    if (type instanceof CSimpleType) {
      switch (((CSimpleType) type).getType()) {
        case FLOAT:
        case DOUBLE:
          return true;
        default:
          return false;
      }
    }
    return false;
  }

  public static boolean isIntegralType(CType type) {
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    if (type instanceof CSimpleType) {
      switch (((CSimpleType) type).getType()) {
        case BOOL:
        case INT:
        case CHAR:
          return true;
        default:
          return false;
      }
    }
    return false;
  }

  /**
   * Check if the specified type can hold integer values, and return a valid type if the check
   * passes.
   */
  @Nullable
  public static CSimpleType toIntegerType(CType type) {
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    if (type instanceof CSimpleType) {
      switch (((CSimpleType) type).getType()) {
        case BOOL:
        case CHAR:
        case INT:
          return (CSimpleType) type;
        default:
          return null;
      }
    }
    return null;
  }

  @Nullable
  public static CSimpleType toSimpleType(CType type) {
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    if (type instanceof CSimpleType) {
      return (CSimpleType) type;
    } else {
      return null;
    }
  }

  public static CSimpleType toUnsignedType(CSimpleType type) {
    if (type.isUnsigned()) {
      return type;
    }
    switch (type.getType()) {
      case BOOL:
      case CHAR:
      case INT:
        return new CSimpleType(type.isConst(), type.isVolatile(), type.getType(), type.isLong(),
            type.isShort(), false, true, type.isComplex(), type.isImaginary(), type.isLongLong());
      default:
        return type;
    }
  }

  /**
   * Check if the specified type can hold numerical values.
   *
   * @param type the specified C type
   * @return whether this type is numerical type
   */
  public static boolean isNumericalType(CType type) {
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    if (type instanceof CSimpleType || type instanceof CEnumType) {
      return true;
    }
    return false;
  }

  /**
   * Compute the dereferenced type of given (possibly) pointer type
   *
   * @param type a C type
   * @return its dereferenced type
   */
  public static CType dereferenceType(CType type) {
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    if (type instanceof CPointerType) {
      return ((CPointerType) type).getType();
    }
    if (type instanceof CArrayType) {
      return ((CArrayType) type).getType();
    }
    throw new UnsupportedOperationException(type.toString() + " cannot be dereferenced");
  }

  @Nullable
  public static CType unwrapType(CType type) {
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    if (type instanceof CPointerType) {
      return ((CPointerType) type).getType();
    } else if (type instanceof CArrayType) {
      return ((CArrayType) type).getType();
    } else {
      return null;
    }
  }

  /**
   * Check if the given type is pointer type
   *
   * @param type a C type
   * @return whether this type is a pointer type
   */
  public static boolean isPointerType(CType type) {
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    if (type instanceof CPointerType) {
      return true;
    }
    return false;
  }

  /**
   * Compute the offset and type of given field name in (possibly) composite type
   *
   * @param type      a C type
   * @param fieldName field name
   * @return field information, including offset and field type
   */
  public static
  @Nonnull
  Pair<Long, CType> getFieldInfo(
      CType type, String fieldName,
      MachineModel model) {
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    // the real type of an elaborated type could only be a complex type
    while (type != null && type instanceof CElaboratedType) {
      type = ((CElaboratedType) type).getRealType();
    }
    if (type == null) {
      return Pair.of(null, null);
    }
    if (type instanceof CCompositeType) {
      switch (((CCompositeType) type).getKind()) {
        case STRUCT: {
          int offset = 0;
          for (CCompositeTypeMemberDeclaration member : ((CCompositeType) type).getMembers()) {
            String memberName = member.getName();
            CType memberType = member.getType();
            // add appropriate padding according to machine model
            offset += model.getPadding(offset, memberType);
            if (memberName.equals(fieldName)) {
              return Pair.of((long) offset, memberType);
            }
            offset += model.getSizeof(member.getType());
          }
          break;
        }
        case UNION: {
          for (CCompositeTypeMemberDeclaration member : ((CCompositeType) type).getMembers()) {
            String memberName = member.getName();
            CType memberType = member.getType();
            if (memberName.equals(fieldName)) {
              return Pair.of((long) 0, memberType);
            }
          }
          break;
        }
        default: {
          // no such case, including ENUM
          return Pair.of(null, null);
        }
      }
    } else if (type instanceof CEnumType) {
      ImmutableList<CEnumerator> enumerators = ((CEnumType) type).getEnumerators();
      for (CEnumerator enumerator : enumerators) {
        String name = enumerator.getName();
        CType eType = enumerator.getType();
        if (name.equals(fieldName)) {
          return Pair.of((long) 0, eType);
        }
      }
    }
    // if we reach here, then no matching member is found
    return Pair.of(null, null);
  }

  @Nullable
  public static CType getFieldType(CType type, String fieldName) {
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    // the real type of an elaborated type could only be a complex type
    while (type != null && type instanceof CElaboratedType) {
      type = ((CElaboratedType) type).getRealType();
    }
    if (type == null) {
      return null;
    }
    if (type instanceof CCompositeType) {
      for (CCompositeTypeMemberDeclaration member : ((CCompositeType) type).getMembers()) {
        String memberName = member.getName();
        if (fieldName.equals(memberName)) {
          return member.getType();
        }
      }
    } else if (type instanceof CEnumType) {
      ImmutableList<CEnumerator> enumerators = ((CEnumType) type).getEnumerators();
      for (CEnumerator enumerator : enumerators) {
        String name = enumerator.getName();
        if (fieldName.equals(name)) {
          return enumerator.getType();
        }
      }
    }
    return null;
  }

  public static
  @Nullable
  CCompositeType extractCompositeType(CType type) {
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    while (type != null && type instanceof CElaboratedType) {
      type = ((CElaboratedType) type).getRealType();
    }
    if (type == null) {
      return null;
    }
    if (type instanceof CCompositeType) {
      return (CCompositeType) type;
    }
    return null;
  }

  public static
  @Nullable
  CArrayType extractArrayType(CType type) {
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    if (type instanceof CArrayType) {
      return (CArrayType) type;
    }
    return null;
  }

  public static
  @Nullable
  CPointerType extractPointerType(CType type) {
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    if (type instanceof CPointerType) {
      return (CPointerType) type;
    }
    return null;
  }

  public static
  @Nullable
  CCompositeTypeMemberDeclaration retrieveMemberByName
      (CCompositeType type, String name) {
    for (CCompositeTypeMemberDeclaration member : type.getMembers()) {
      if (member.getName().equals(name)) {
        return member;
      }
    }
    return null;
  }

  /**
   * Check if two types are equivalent.
   *
   * @param type1 The first type
   * @param type2 The second type
   * @return whether two types are equivalent
   */
  public static boolean isEquivalent(CType type1, CType type2) {
    while (type1 instanceof CTypedefType) {
      type1 = ((CTypedefType) type1).getRealType();
    }
    while (type2 instanceof CTypedefType) {
      type2 = ((CTypedefType) type2).getRealType();
    }
    return type1.getCanonicalType().equals(type2.getCanonicalType());
  }

  /**
   * Extract type of inner member by designators
   *
   * @param rootType     A C type (possibly a composite type)
   * @param pDesignators designators
   * @return the type corresponding to specified designators
   */
  public static CType extractInnerTypeByDesignators(
      CType rootType, List<CDesignator>
      pDesignators) {
    while (rootType instanceof CTypedefType) {
      rootType = ((CTypedefType) rootType).getRealType();
    }
    while (rootType != null && rootType instanceof CElaboratedType) {
      rootType = ((CElaboratedType) rootType).getRealType();
    }
    if (rootType == null) {
      throw new AssertionError("Cannot parse compound type");
    }
    CType finalType = rootType;
    for (CDesignator designator : pDesignators) {
      if (designator instanceof CArrayDesignator || designator instanceof CArrayRangeDesignator) {
        if (finalType instanceof CArrayType) {
          finalType = ((CArrayType) finalType).getType();
        } else {
          throw new AssertionError("Array designator in non-array type");
        }
      } else {
        String fieldName = ((CFieldDesignator) designator).getFieldName();
        finalType = getFieldType(finalType, fieldName);
        if (finalType == null) {
          throw new AssertionError("Specified field not found");
        }
      }
    }
    return finalType;
  }

  /**
   * Parse the type of access path
   *
   * @param path given access path
   * @return C type of this path
   */
  public static CType parseTypeFromAccessPath(AccessPath path) {
    List<PathSegment> segments = path.path();
    CType currentType = path.getType();
    for (int i = 1; i < segments.size(); i++) {
      PathSegment segment = segments.get(i);
      if (segment instanceof PointerDereferenceSegment) {
        currentType = dereferenceType(currentType);
      } else if (segment instanceof FieldAccessSegment) {
        String fieldName = segment.getName();
        currentType = getFieldType(currentType, fieldName);
        if (currentType == null) {
          throw new AssertionError("No compatible field name. Can the code actually be compiled "
              + "successfully?");
        }
      } else if (segment instanceof ArrayConstIndexSegment) {
        if (currentType instanceof CArrayType) {
          currentType = ((CArrayType) currentType).getType();
        } else {
          Preconditions.checkArgument(currentType instanceof CPointerType);
          currentType = ((CPointerType) currentType).getType();
        }
      } else {
        throw new UnsupportedOperationException("Unsupported segment: " + segment);
      }
    }
    return currentType;
  }

}
