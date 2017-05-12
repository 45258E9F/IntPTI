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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.ast.c.AdaptingCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.UnaliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.solver.api.Formula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

class CExpressionVisitorWithPointerAliasing
    extends DefaultCExpressionVisitor<Expression, UnrecognizedCCodeException>
    implements CRightHandSideVisitor<Expression, UnrecognizedCCodeException> {

  private static class AdaptingExpressionToFormulaVisitor
      extends AdaptingCExpressionVisitor<Formula, Expression, UnrecognizedCCodeException>
      implements CRightHandSideVisitor<Formula, UnrecognizedCCodeException> {

    private AdaptingExpressionToFormulaVisitor(CExpressionVisitorWithPointerAliasing pDelegate) {
      super(pDelegate);
    }

    @Override
    protected Formula convert(Expression value, CExpression rhs) throws UnrecognizedCCodeException {
      return convert0(value, rhs);
    }

    private Formula convert0(Expression value, CRightHandSide rhs) {
      CType type = CTypeUtils.simplifyType(rhs.getExpressionType());
      return ((CExpressionVisitorWithPointerAliasing) delegate).asValueFormula(value, type);
    }

    @Override
    public Formula visit(CFunctionCallExpression e) throws UnrecognizedCCodeException {
      return convert0(((CExpressionVisitorWithPointerAliasing) delegate).visit(e), e);
    }
  }

  public CExpressionVisitorWithPointerAliasing(
      final CToFormulaConverterWithPointerAliasing cToFormulaConverter,
      final CFAEdge cfaEdge,
      final String function,
      final SSAMapBuilder ssa,
      final Constraints constraints,
      final ErrorConditions errorConditions,
      final PointerTargetSetBuilder pts) {

    delegate =
        new ExpressionToFormulaVisitor(cToFormulaConverter, cToFormulaConverter.fmgr, cfaEdge,
            function, ssa, constraints) {
          @Override
          protected Formula toFormula(CExpression e) throws UnrecognizedCCodeException {
            // recursive application of pointer-aliasing.
            return asValueFormula(e.accept(CExpressionVisitorWithPointerAliasing.this),
                CTypeUtils.simplifyType(e.getExpressionType()));
          }
        };

    this.conv = cToFormulaConverter;
    this.edge = cfaEdge;
    this.ssa = ssa;
    this.constraints = constraints;
    this.errorConditions = errorConditions;
    this.pts = pts;

    this.baseVisitor = new BaseVisitor(cfaEdge, pts);
  }

  public CRightHandSideVisitor<Formula, UnrecognizedCCodeException> asFormulaVisitor() {
    return new AdaptingExpressionToFormulaVisitor(this);
  }

  private void addEqualBaseAdressConstraint(final Formula p1, final Formula p2) {
    if (errorConditions.isEnabled()) {
      // Constraint is only necessary for correct error conditions
      constraints.addConstraint(conv.fmgr.makeEqual(conv.makeBaseAddressOfTerm(p1),
          conv.makeBaseAddressOfTerm(p2)));
    }
  }

  Formula asValueFormula(final Expression e, final CType type, final boolean isSafe) {
    if (e.isValue()) {
      return e.asValue().getValue();
    } else if (e.asLocation().isAliased()) {
      return !isSafe ? conv
          .makeDereference(type, e.asLocation().asAliased().getAddress(), ssa, errorConditions) :
             conv.makeSafeDereference(type, e.asLocation().asAliased().getAddress(), ssa);
    } else { // Unaliased location
      return conv.makeVariable(e.asLocation().asUnaliased().getVariableName(), type, ssa);
    }
  }

  Formula asValueFormula(final Expression e, final CType type) {
    return asValueFormula(e, type, false);
  }

  Formula asSafeValueFormula(final Expression e, final CType type) {
    return asValueFormula(e, type, true);
  }

  @Override
  public AliasedLocation visit(final CArraySubscriptExpression e)
      throws UnrecognizedCCodeException {
    Expression base = e.getArrayExpression().accept(this);
    // There are two distinct kinds of arrays in C:
    // -- fixed-length arrays for which the aliased location of the first element is returned here
    // -- pointers implicitly converted to arrays for which either the aliased or unaliased location of the *pointer*
    //    is returned
    final CType baseType = CTypeUtils.simplifyType(e.getArrayExpression().getExpressionType());
    // Fixed-length arrays
    // TODO: Check if fixed-sized arrays and pointers can be clearly distinguished this way
    if (baseType instanceof CArrayType && ((CArrayType) baseType).getLength() != null) {
      assert base.isAliasedLocation();
    } else {
      // The address of the first element is needed i.e. the value of the pointer in the array expression
      base = AliasedLocation
          .ofAddress(asValueFormula(base, CTypeUtils.implicitCastToPointer(baseType)));
    }
    // Now we should always have the aliased location of the first array element
    assert base.isAliasedLocation();

    final CType elementType = CTypeUtils.simplifyType(e.getExpressionType());
    final CExpression subscript = e.getSubscriptExpression();
    final CType subscriptType = CTypeUtils.simplifyType(subscript.getExpressionType());
    final Formula index = conv.makeCast(subscriptType,
        CPointerType.POINTER_TO_VOID,
        asValueFormula(subscript.accept(this), subscriptType),
        constraints,
        edge);

    final Formula coeff =
        conv.fmgr.makeNumber(conv.voidPointerFormulaType, conv.getSizeof(elementType));
    final Formula baseAddress = base.asAliasedLocation().getAddress();
    final Formula address = conv.fmgr.makePlus(baseAddress, conv.fmgr.makeMultiply(coeff, index));
    addEqualBaseAdressConstraint(baseAddress, address);
    return AliasedLocation.ofAddress(address);
  }

  @Override
  public Location visit(CFieldReference e) throws UnrecognizedCCodeException {

    e = CToFormulaConverterWithPointerAliasing.eliminateArrow(e, edge);

    final Variable variable = e.accept(baseVisitor);
    if (variable != null) {
      final String variableName = variable.getName();
      if (pts.isDeferredAllocationPointer(variableName)) {
        usedDeferredAllocationPointers.put(variableName, CPointerType.POINTER_TO_VOID);
      }
      return UnaliasedLocation.ofVariableName(variableName);
    } else {
      final CType fieldOwnerType = CTypeUtils.simplifyType(e.getFieldOwner().getExpressionType());
      if (fieldOwnerType instanceof CCompositeType) {
        final AliasedLocation base = e.getFieldOwner().accept(this).asAliasedLocation();

        final String fieldName = e.getFieldName();
        usedFields.add(Pair.of((CCompositeType) fieldOwnerType, fieldName));
        final Formula offset = conv.fmgr.makeNumber(conv.voidPointerFormulaType,
            conv.ptsMgr.getOffset((CCompositeType) fieldOwnerType, fieldName));

        final Formula address = conv.fmgr.makePlus(base.getAddress(), offset);
        addEqualBaseAdressConstraint(base.getAddress(), address);
        return AliasedLocation.ofAddress(address);
      } else {
        throw new UnrecognizedCCodeException("Field owner of a non-composite type", edge, e);
      }
    }
  }

  static boolean isUnaliasedLocation(final CExpression e) {
    if (e instanceof CIdExpression) {
      return true;
    } else if (e instanceof CFieldReference && !((CFieldReference) e).isPointerDereference()) {
      return isUnaliasedLocation(((CFieldReference) e).getFieldOwner());
    } else {
      return false;
    }
  }

  static boolean isRevealingType(final CType type) {
    return (type instanceof CPointerType || type instanceof CArrayType) &&
        !type.equals(CPointerType.POINTER_TO_VOID);
  }

  @Override
  public Expression visit(final CCastExpression e) throws UnrecognizedCCodeException {
    final CType resultType = CTypeUtils.simplifyType(e.getExpressionType());
    final CExpression operand =
        conv.makeCastFromArrayToPointerIfNecessary(e.getOperand(), resultType);

    final Expression result = operand.accept(this);

    // TODO: is the second isUnaliasedLocation() check really needed?
    if (isRevealingType(resultType) && isUnaliasedLocation(operand) && result
        .isUnaliasedLocation()) {
      final String variableName = result.asLocation().asUnaliased().getVariableName();
      if (pts.isDeferredAllocationPointer(variableName)) {
        assert usedDeferredAllocationPointers.containsKey(variableName) &&
            usedDeferredAllocationPointers.get(variableName).equals(CPointerType.POINTER_TO_VOID) :
            "Wrong assumptions on deferred allocations tracking: unknown pointer encountered";
        usedDeferredAllocationPointers.put(variableName, resultType);
      }
    }

    final CType operandType = CTypeUtils.simplifyType(operand.getExpressionType());
    if (CTypeUtils.isSimpleType(resultType)) {
      return Value.ofValue(
          conv.makeCast(operandType, resultType, asValueFormula(result, operandType), constraints,
              edge));
    } else if (CTypes.withoutConst(resultType).equals(CTypes.withoutConst(operandType))) {
      // Special case: conversion of non-scalar type to itself is allowed (and ignored)
      // Change of const modifier is ignored, too.
      return result;
    } else {
      throw new UnrecognizedCCodeException("Conversion to non-scalar type requested", edge, e);
    }
// TODO: The following heuristic should be implemented in more generally in the assignment to p
//    if (operand instanceof CPointerExpression
//        && !(resultType instanceof CFunctionType)) {
//      // Heuristic:
//      // When there is (t)*p, we treat it like *((*t)p)
//      // This means the UF for type t get's used instead of the UF for actual type of p.
//    }
  }

  @Override
  public Expression visit(final CIdExpression e) throws UnrecognizedCCodeException {
    Variable variable = e.accept(baseVisitor);
    final CType resultType = CTypeUtils.simplifyType(e.getExpressionType());
    if (variable != null) {
      if (!(e.getDeclaration() instanceof CFunctionDeclaration)) {
        final String variableName = variable.getName();
        if (pts.isDeferredAllocationPointer(variableName)) {
          usedDeferredAllocationPointers.put(variableName, CPointerType.POINTER_TO_VOID);
        }
        return UnaliasedLocation.ofVariableName(variableName);
      } else {
        return Value.ofValue(conv.makeConstant(variable.getName(), variable.getType()));
      }
    } else {
      final Formula address =
          conv.makeConstant(PointerTargetSet.getBaseName(e.getDeclaration().getQualifiedName()),
              CTypeUtils.getBaseType(resultType));
      return AliasedLocation.ofAddress(address);
    }
  }

  @Override
  public Value visit(final CUnaryExpression e) throws UnrecognizedCCodeException {
    if (e.getOperator() == UnaryOperator.AMPER) {
      final CExpression operand = e.getOperand();

      final Variable baseVariable = operand.accept(baseVisitor);
      if (baseVariable == null) {
        AliasedLocation addressExpression = null;

        // addressedFields is used to treat structure assignment and field addressing separately:
        // assuming s1 and s2 both have substructure ss1, which in its turn has fields f1 and f2,
        // and there is also outer structure ss2 of the same type as ss1, in
        // s1.ss1 = s2.ss1;
        // ss2.f1 = 0;
        // it isn't necessary to start tracking
        // either s1.ss1.f{1,2} or s2.ss1.f{1,2}, because as s{1,2}.ss1 itself is not tracked,
        // it's known that their values remain undefined and only some other outer structure field is assigned.
        // But in
        // p2 = &(s2.ss1);
        // p2->f1 = 0;
        // the fields f1 and f2 along with the field s{1,2}.ss1 should be tracked from the first line onwards, because
        // it's too hard to determine (without the help of some alias analysis)
        // whether f1 assigned in the second line is an outer or inner structure field.
        final List<Pair<CCompositeType, String>> alreadyUsedFields =
            ImmutableList.copyOf(usedFields);
        usedFields.clear();

        if (errorConditions.isEnabled() && operand instanceof CFieldReference) {
          // for &(s->f) and &((*s).f) do special case because the pointer is
          // not actually dereferenced and thus we don't want to add error conditions
          // for invalid-deref
          final CFieldReference field = (CFieldReference) operand;
          CExpression fieldOwner = field.getFieldOwner();
          boolean isDeref = field.isPointerDereference();
          if (!isDeref && fieldOwner instanceof CPointerExpression) {
            fieldOwner = ((CPointerExpression) fieldOwner).getOperand();
            isDeref = true;
          }
          if (isDeref) {
            final CPointerType pointerType =
                (CPointerType) CTypeUtils.simplifyType(fieldOwner.getExpressionType());
            final Formula base = asSafeValueFormula(fieldOwner.accept(this), pointerType);
            final String fieldName = field.getFieldName();
            final CCompositeType compositeType =
                (CCompositeType) CTypeUtils.simplifyType(pointerType.getType());
            usedFields.add(Pair.of(compositeType, fieldName));
            final Formula offset = conv.fmgr.makeNumber(conv.voidPointerFormulaType,
                conv.ptsMgr.getOffset(compositeType, fieldName));
            addressExpression = AliasedLocation.ofAddress(conv.fmgr.makePlus(base, offset));
            addEqualBaseAdressConstraint(base, addressExpression.getAddress());
          }
        }

        if (addressExpression == null) {
          addressExpression = operand.accept(this).asAliasedLocation();
        }

        addressedFields.addAll(usedFields);
        usedFields.addAll(alreadyUsedFields);

        return Value.ofValue(addressExpression.getAddress());
      } else {
        final Variable base = baseVisitor.getLastBase();
        final Formula baseAddress = conv.makeConstant(PointerTargetSet.getBaseName(base.getName()),
            CTypeUtils.getBaseType(base.getType()));
        conv.addValueImportConstraints(edge,
            baseAddress,
            base,
            initializedFields,
            ssa,
            constraints,
            pts);
        if (conv.hasIndex(base.getName(), base.getType(), ssa)) {
          ssa.deleteVariable(base.getName());
        }
        conv.addPreFilledBase(base.getName(),
            base.getType(),
            pts.isPreparedBase(base.getName()),
            false,
            constraints,
            pts);
        return visit(e);
      }
    } else {
      return visitDefault(e);
    }
  }

  @Override
  public AliasedLocation visit(final CPointerExpression e) throws UnrecognizedCCodeException {
    final CExpression operand = e.getOperand();
    final CType operandType = CTypeUtils.simplifyType(operand.getExpressionType());
    final Expression operandExpression = operand.accept(this);
    if (operandType instanceof CArrayType && ((CArrayType) operandType).getLength() != null) {
      return operandExpression.asAliasedLocation();
    } else {
      return AliasedLocation.ofAddress(asValueFormula(operandExpression, operandType));
    }
  }

  @Override
  public Value visit(final CBinaryExpression exp) throws UnrecognizedCCodeException {
    final CType returnType = exp.getExpressionType();
    final CType calculationType = exp.getCalculationType();
    final Formula f1 = delegate.processOperand(exp.getOperand1(), calculationType, returnType);
    final Formula f2 = delegate.processOperand(exp.getOperand2(), calculationType, returnType);

    final Formula result = delegate.handleBinaryExpression(exp, f1, f2);

    final CType t1 = CTypeUtils.simplifyType(exp.getOperand1().getExpressionType());
    final CType t2 = CTypeUtils.simplifyType(exp.getOperand2().getExpressionType());
    final BinaryOperator op = exp.getOperator();

    switch (op) {
      case PLUS:
        if (t1 instanceof CPointerType) {
          addEqualBaseAdressConstraint(result, f1);
        }
        if (t2 instanceof CPointerType) {
          addEqualBaseAdressConstraint(result, f2);
        }
        break;
      case MINUS:
        // TODO addEqualBaseAddressConstraints here, too?
      default:
        // Does not occur for pointers
        break;
    }

    return Value.ofValue(result);
  }

  @Override
  protected Value visitDefault(final CExpression e) throws UnrecognizedCCodeException {
    return Value.ofValue(e.accept(delegate));
  }

  @SuppressWarnings("deprecation")
  @Override
  public Value visit(final CFunctionCallExpression e) throws UnrecognizedCCodeException {
    final CExpression functionNameExpression = e.getFunctionNameExpression();

    // First let's handle special cases such as allocations
    if (functionNameExpression instanceof CIdExpression) {
      final String functionName = ((CIdExpression) functionNameExpression).getName();

      if (conv.options.isDynamicMemoryFunction(functionName)) {
        DynamicMemoryHandler memoryHandler =
            new DynamicMemoryHandler(conv, edge, ssa, pts, constraints, errorConditions);
        try {
          return memoryHandler.handleDynamicMemoryFunction(e, functionName, this);
        } catch (InterruptedException exc) {
          // Throwing two checked exception from this visitor is not possible directly.
          // The following does the same although it is not recommended to do so.
          // However, we are sure that an InterrupedException from this visitor
          // will be handled correctly outside.
          Thread.currentThread().stop(exc);
        }
      }
    }

    // Pure functions returning composites are unsupported, return a nondet value
    final CType resultType = CTypeUtils.simplifyType(conv.getReturnType(e, edge));
    if (resultType instanceof CCompositeType ||
        CTypeUtils.containsArray(resultType)) {
      conv.logger.logfOnce(Level.WARNING,
          "Extern function %s returning a composite is treated as nondet.", e);
      return Value.nondetValue();
    }

    // Delegate
    return Value.ofValue(checkNotNull(delegate.visit(e)));
  }

  List<Pair<CCompositeType, String>> getUsedFields() {
    return Collections.unmodifiableList(usedFields);
  }

  List<Pair<CCompositeType, String>> getInitializedFields() {
    return Collections.unmodifiableList(initializedFields);
  }

  List<Pair<CCompositeType, String>> getAddressedFields() {
    return Collections.unmodifiableList(addressedFields);
  }

  Map<String, CType> getUsedDeferredAllocationPointers() {
    return Collections.unmodifiableMap(usedDeferredAllocationPointers);
  }

  private final CToFormulaConverterWithPointerAliasing conv;
  private final CFAEdge edge;
  private final SSAMapBuilder ssa;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;
  private final PointerTargetSetBuilder pts;

  private final BaseVisitor baseVisitor;
  private final ExpressionToFormulaVisitor delegate;

  private final List<Pair<CCompositeType, String>> usedFields = new ArrayList<>(1);
  private final List<Pair<CCompositeType, String>> initializedFields = new ArrayList<>();
  private final List<Pair<CCompositeType, String>> addressedFields = new ArrayList<>();
  private final Map<String, CType> usedDeferredAllocationPointers =
      Maps.newHashMapWithExpectedSize(1);
}
