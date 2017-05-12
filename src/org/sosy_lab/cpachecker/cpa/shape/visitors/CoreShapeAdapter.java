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
package org.sosy_lab.cpachecker.cpa.shape.visitors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstraintRepresentation;
import org.sosy_lab.cpachecker.cpa.shape.constraint.LogicalOrContainer;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SEs;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.util.AssumeEvaluator;
import org.sosy_lab.cpachecker.cpa.shape.util.ReducerResult;
import org.sosy_lab.cpachecker.cpa.shape.util.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.shape.util.UnknownTypes;
import org.sosy_lab.cpachecker.cpa.shape.values.Address;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeField;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.address.ArrayVisitor;
import org.sosy_lab.cpachecker.cpa.shape.visitors.address.LValueVisitor;
import org.sosy_lab.cpachecker.cpa.shape.visitors.address.StructAndUnionVisitor;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.SymbolicAssumeInfo;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.SymbolicCollector;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.SymbolicMerger;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.Symbolizer;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.TypedSymbolicCollector;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.DeclaredTypeData;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.ExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicExpressionAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndStateList;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.ValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.value.CSizeofDeclarationVisitor;
import org.sosy_lab.cpachecker.cpa.shape.visitors.value.CSizeofVisitor;
import org.sosy_lab.cpachecker.cpa.shape.visitors.value.ExplicitValueVisitor;
import org.sosy_lab.cpachecker.cpa.shape.visitors.value.PointerVisitor;
import org.sosy_lab.cpachecker.cpa.shape.visitors.value.SymbolicExpressionVisitor;
import org.sosy_lab.cpachecker.cpa.shape.visitors.value.SymbolicValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the core adapter of various expression visitors. We should invoke visitors via this
 * adapter, instead of directly create the instance of specific visitor.
 */
@Options(prefix = "cpa.shape")
public final class CoreShapeAdapter {

  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;

  @Option(secure = true, toUppercase = true, description = "The direction of allocating function "
      + "frames")
  private AddressDirection functionDirection = AddressDirection.DOWNWARDS;

  @Option(secure = true, toUppercase = true, description = "The direction of allocating variable "
      + "in one function frame")
  private AddressDirection variableDirection = AddressDirection.DOWNWARDS;

  /**
   * Directions of function frame and stack objects are machine-dependent. We should specify them
   * carefully before analyzing the program.
   */
  public enum AddressDirection {
    DOWNWARDS,
    UPWARDS
  }

  @Option(secure = true, name = "merge.guard", description = "the maximum length for guard "
      + "condition")
  private int maxGuardDepth = 0;

  @Option(secure = true, name = "merge.interpretation", description = "the maximum number of "
      + "interpretations for abstract value")
  private int maxInterpretations = 0;

  private CoreShapeAdapter(
      Configuration pConfig,
      MachineModel pModel,
      LogManagerWithoutDuplicates pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    machineModel = pModel;
    logger = pLogger;
  }

  private static CoreShapeAdapter instance;

  public static CoreShapeAdapter getInstance() {
    if (instance == null) {
      throw new UnsupportedOperationException("CoreShapeAdapter should be initialized first");
    }
    return instance;
  }

  public static void initialize(
      Configuration pConfig, MachineModel pModel,
      LogManagerWithoutDuplicates pLogger)
      throws InvalidConfigurationException {
    instance = new CoreShapeAdapter(pConfig, pModel, pLogger);
  }

  /* ***************************** */
  /* expression evaluation methods */
  /* ***************************** */

  public SymbolicValueAndStateList evaluateSymbolicValue
      (ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge, CRightHandSide rE)
      throws CPATransferException {
    CType type = getType(rE);
    if (type instanceof CPointerType || type instanceof CArrayType ||
        isStructOrUnion(type) || type instanceof CFunctionType) {
      return evaluateAddressValue(pState, pOtherStates, pEdge, rE);
    } else {
      return evaluateNonAddressValue(pState, pOtherStates, pEdge, rE);
    }
  }

  /**
   * Derive symbolic value for right-hand-side expression. Consider a binary expression X + Y,
   * we generate a fresh value Z and a constraint Z = X + Y to denote the constraint in assignment.
   * This method should only be used when processing assignment.
   */
  public List<SymbolicExpressionAndState> evaluateSymbolicExpression
  (ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge, CRightHandSide rE)
      throws CPATransferException {
    CType type = getType(rE);
    if (type instanceof CPointerType || type instanceof CArrayType ||
        isStructOrUnion(type) || type instanceof CFunctionType) {
      SymbolicValueAndStateList valueAndStates = evaluateAddressValue(pState, pOtherStates, pEdge,
          rE);
      List<SymbolicExpressionAndState> results = new ArrayList<>(valueAndStates.size());
      for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
        ShapeState newState = valueAndState.getShapeState();
        ShapeSymbolicValue value = valueAndState.getObject();
        results.add(SymbolicExpressionAndState.of(newState, SEs.toConstant(value, rE)));
      }
      return results;
    } else {
      return evaluateNonAddressValueAsSymbolicExpression(pState, pOtherStates, pEdge, rE);
    }
  }

  public List<ExplicitValueAndState> evaluateExplicitValue
      (ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge, CRightHandSide rE)
      throws CPATransferException {
    List<ExplicitValueAndState> results = new ArrayList<>();
    CType rightType = CoreShapeAdapter.getType(rE);
    ExplicitValueVisitor visitor = new ExplicitValueVisitor(pState, pOtherStates, pEdge,
        machineModel, logger);
    List<ValueAndState> vResults = rE.accept(visitor);
    for (ValueAndState valueAndState : vResults) {
      Value value = valueAndState.getObject();
      ShapeState newState = valueAndState.getShapeState();
      if (!value.isExplicitlyKnown() || !value.isNumericValue()) {
        // derive explicit value from symbolic value
        SymbolicValueAndStateList symValueAndStates = evaluateSymbolicValue(newState,
            pOtherStates, pEdge, rE);
        for (SymbolicValueAndState symValueAndState : symValueAndStates
            .asSymbolicValueAndStateList()) {
          results.add(deriveExplicitValueFromSymbolicValue(symValueAndState));
        }
      } else {
        // if value has an explicit value, it should have a simple type
        Preconditions.checkArgument(rightType instanceof CSimpleType);
        results.add(ExplicitValueAndState.of(newState, convertFromValue(value.asNumericValue(),
            (CSimpleType) rightType)));
      }
    }
    return results;
  }

  public KnownExplicitValue convertFromValue(NumericValue pValue, CSimpleType targetType) {
    switch (targetType.getType()) {
      case INT:
      case BOOL:
      case CHAR:
        return KnownExplicitValue.valueOf(pValue.longValue());
      case FLOAT:
        return KnownExplicitValue.valueOf(pValue.floatValue());
      case DOUBLE:
        return KnownExplicitValue.valueOf(pValue.doubleValue());
      default:
        throw new IllegalArgumentException("unsupported type: " + targetType.toASTString(""));
    }
  }

  /**
   * This function evaluate a right-hand-side expression as an explicit value. If there are
   * multiple evaluation results, we just return unknown value.
   */
  public ShapeExplicitValue evaluateSingleExplicitValue
  (ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge, CRightHandSide rE)
      throws CPATransferException {
    List<ExplicitValueAndState> result = evaluateExplicitValue(pState, pOtherStates, pEdge, rE);
    if (result.size() == 1) {
      // the only determined value is returned
      return result.get(0).getObject();
    } else {
      return UnknownValue.getInstance();
    }
  }

  /**
   * This method is "raw" because we directly extract the list of {@link ValueAndState} instances
   * as evaluation result.
   */
  public List<ValueAndState> evaluateRawExplicitValue
  (ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge, CRightHandSide rE)
      throws CPATransferException {
    ExplicitValueVisitor visitor = new ExplicitValueVisitor(pState, pOtherStates, pEdge,
        machineModel, logger);
    List<ValueAndState> results = rE.accept(visitor);
    if (!results.isEmpty()) {
      return results;
    }
    // not likely to occur?
    return Lists.newArrayList(ValueAndState.of(pState));
  }

  /**
   * Evaluate a right-hand-side expression as address values.
   */
  public AddressValueAndStateList evaluateAddressValue
  (ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge, CRightHandSide rE)
      throws CPATransferException {
    CType type = getType(rE);
    if (type instanceof CPointerType || type instanceof CFunctionType) {
      // if the specified expression has pointer type (value/function pointer)
      PointerVisitor visitor = getPointerVisitor(pEdge, pState, pOtherStates);
      SymbolicValueAndStateList valueAndStateList = rE.accept(visitor);
      return getAddressFromSymbolicValues(valueAndStateList);
    } else if (isStructOrUnion(type)) {
      StructAndUnionVisitor visitor = getStructAndUnionVisitor(pEdge, pState, pOtherStates);
      List<AddressAndState> addressAndStates = rE.accept(visitor);
      return convertAddressesFrom(addressAndStates);
    } else if (type instanceof CArrayType) {
      ArrayVisitor visitor = getArrayVisitor(pEdge, pState, pOtherStates);
      List<AddressAndState> addressAndStates = rE.accept(visitor);
      return convertAddressesFrom(addressAndStates);
    } else {
      throw new IllegalArgumentException("value of type " + type.toASTString("foo") + " cannot be"
          + " evaluated by address evaluator");
    }
  }

  /**
   * Evaluate a left-hand-side expression as addresses.
   * cf. {@link LValueVisitor} for the difference between address and address values.
   */
  public List<AddressAndState> evaluateAddress(
      ShapeState pState, List<AbstractState> pOtherStates,
      CFAEdge pEdge, CLeftHandSide lE)
      throws CPATransferException {
    LValueVisitor visitor = getLValueVisitor(pEdge, pState, pOtherStates);
    return lE.accept(visitor);
  }

  /**
   * Evaluate a right-hand-side expression as symbolic values.
   * Note: ordinary symbolic values do not have memory object or offset information.
   */
  private SymbolicValueAndStateList evaluateNonAddressValue
  (ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge, CRightHandSide rE)
      throws CPATransferException {
    SymbolicValueVisitor visitor = getExpressionValueVisitor(pEdge, pState, pOtherStates);
    return rE.accept(visitor);
  }

  public List<SymbolicExpressionAndState> evaluateNonAddressValueAsSymbolicExpression
      (ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge, CRightHandSide rE)
      throws CPATransferException {
    SymbolicExpressionVisitor visitor = getSymbolicExpressionVisitor(pEdge, pState, pOtherStates);
    return rE.accept(visitor);
  }

  /**
   * Evaluate an array subscript as address.
   */
  public List<AddressAndState> evaluateArraySubscriptAddress
  (
      ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge,
      CArraySubscriptExpression exp) throws CPATransferException {
    List<AddressAndState> results = new ArrayList<>();
    CExpression array = exp.getArrayExpression();
    CExpression index = exp.getSubscriptExpression();
    AddressValueAndStateList arrayAddresses = evaluateAddressValue(pState, pOtherStates, pEdge,
        array);
    for (AddressValueAndState arrayAddress : arrayAddresses.asAddressValueAndStateList()) {
      ShapeAddressValue aAddress = arrayAddress.getObject();
      ShapeState newState = arrayAddress.getShapeState();
      if (aAddress.isUnknown()) {
        // ROB-1: a possible memory issue
        results.add(AddressAndState.of(newState));
        continue;
      }
      SGObject arrayObject = aAddress.getObject();
      // the object offset is always known, otherwise it should not appear in the shape graph
      int arrayOffset = aAddress.getOffset().getAsInt();
      ShapeExplicitValue arraySize = arrayObject.getSize();
      boolean isKnownArraySize = !arraySize.isUnknown();
      int elementSize = evaluateSizeof(newState, pOtherStates, pEdge, getType(exp), exp);
      SymbolicExpression offsetSet = SEs.toConstant(KnownExplicitValue.valueOf(arrayOffset),
          machineModel.getSizeTType());
      SymbolicExpression sizeSe = SEs.toConstant(KnownExplicitValue.valueOf(elementSize),
          machineModel.getSizeTType());
      List<SymbolicExpressionAndState> indexValues = evaluateSymbolicExpression(newState,
          pOtherStates, pEdge, index);
      for (SymbolicExpressionAndState indexValue : indexValues) {
        SymbolicExpression value = indexValue.getObject();
        newState = indexValue.getShapeState();
        if (SEs.isUnknown(value)) {
          // ROB-1: a possible memory issue
          results.add(AddressAndState.of(newState));
          continue;
        } else if (!SEs.isExplicit(value)) {
          List<ConstraintRepresentation> errorConditions = new ArrayList<>(2);
          CType indexType = value.getType();

          // Constraint I: array index should be non-negative
          SymbolicExpression nonNegative = SEs.lessThan(value, SEs.toConstant(KnownExplicitValue
              .ZERO, indexType));
          errorConditions.add(nonNegative);

          // Constraint II: array index should not exceed its upper bound (aka. buffer size)
          SymbolicExpression bound;
          if (isKnownArraySize) {
            bound = SEs.toConstantWithConversion((KnownExplicitValue) arraySize,
                machineModel.getSizeTType(), machineModel);
          } else {
            bound = newState.getSizeForObject(arrayObject);
          }
          if (bound != null) {
            SymbolicExpression delta = SEs.multiply(value, sizeSe, machineModel.getSizeTType(),
                machineModel);
            SymbolicExpression position = SEs.plus(offsetSet, delta, machineModel.getSizeTType(),
                machineModel);
            SymbolicExpression outBound = SEs.greaterEqual(position, bound);
            errorConditions.add(outBound);
          }
          ConstraintRepresentation errorCondition = LogicalOrContainer.of(errorConditions);
          try {
            boolean sat = newState.checkSatWithAssumptions(Lists.newArrayList(errorCondition));
            if (sat) {
              newState = newState.setInvalidRead(exp);
            }
            results.add(AddressAndState.of(newState));
          } catch (Exception ex) {
            results.add(AddressAndState.of(newState));
          }
          continue;
        } else {
          // If the index value is known, we can derive an explicit target address (though it is
          // possible to exceed the allocated buffer space)
          KnownExplicitValue expIndex = (KnownExplicitValue) value.getValue();
          ShapeExplicitValue delta = expIndex.multiply(KnownExplicitValue.valueOf(elementSize));
          AddressAndState result = AddressAndState.of(newState, Address.valueOf(arrayObject,
              aAddress.getOffset().add(delta)));
          results.add(result);
        }
      }
    }
    return results;
  }

  /**
   * Evaluate a field reference as addresses.
   */
  public List<AddressAndState> evaluateFieldAddress
  (ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge, CFieldReference e)
      throws CPATransferException {
    CExpression owner = e.getFieldOwner();
    CType ownerType = CoreShapeAdapter.getType(owner);
    List<AddressAndState> results = new ArrayList<>();
    AddressValueAndStateList ownerAddresses = evaluateAddressValue(pState, pOtherStates, pEdge,
        owner);
    for (AddressValueAndState ownerAddress : ownerAddresses.asAddressValueAndStateList()) {
      ShapeAddressValue oAddress = ownerAddress.getObject();
      ShapeState newState = ownerAddress.getShapeState();
      if (oAddress.isUnknown()) {
        results.add(AddressAndState.of(newState));
        continue;
      }
      String fieldName = e.getFieldName();
      ShapeField field = getField(pState, pOtherStates, pEdge, ownerType, fieldName);
      if (field.isUnknown()) {
        results.add(AddressAndState.of(newState));
        continue;
      }
      Address addressOfOwner = oAddress.getAddress();
      ShapeExplicitValue fieldOffset = addressOfOwner.getOffset().add(field.getOffset());
      SGObject object = addressOfOwner.getObject();
      Address newAddress = Address.valueOf(object, fieldOffset);
      results.add(AddressAndState.of(newState, newAddress));
    }
    return results;
  }

  /**
   * Evaluate pointer arithmetic as address value (the symbolic value).
   *
   * @param pState    shape state
   * @param pEdge     CFA edge
   * @param pAddress  the address expression of array or pointer type
   * @param pOffset   the offset expression, should be of numerical type
   * @param pType     the type of pointer arithmetic expression
   * @param pOperator the binary operator
   * @return the resultant address value(s)
   */
  public AddressValueAndStateList evaluatePointerArithmetic(
      ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge, CExpression pAddress,
      CExpression pOffset, CPointerType pType, BinaryOperator pOperator)
      throws CPATransferException {
    CType elementType = pType.getType();
    switch (pOperator) {
      case PLUS:
      case MINUS: {
        List<AddressValueAndState> results = new ArrayList<>();
        AddressValueAndStateList addressValues = evaluateAddressValue(pState, pOtherStates, pEdge,
            pAddress);
        for (AddressValueAndState addressValue : addressValues.asAddressValueAndStateList()) {
          ShapeAddressValue address = addressValue.getObject();
          ShapeState newState = addressValue.getShapeState();
          if (address.isUnknown()) {
            // unnecessary to compute offset in the following
            results.add(AddressValueAndState.of(newState));
            continue;
          }
          List<ExplicitValueAndState> offsetValues = evaluateExplicitValue(pState,
              pOtherStates, pEdge, pOffset);
          for (ExplicitValueAndState offsetValue : offsetValues) {
            ShapeExplicitValue offset = offsetValue.getObject();
            newState = offsetValue.getShapeState();
            if (offset.isUnknown()) {
              results.add(AddressValueAndState.of(newState));
              continue;
            }
            ShapeExplicitValue elementSize = KnownExplicitValue.valueOf(evaluateSizeof(newState,
                pOtherStates, pEdge, elementType));
            ShapeExplicitValue delta = elementSize.multiply(offset);
            SGObject targetObject = address.getObject();
            ShapeExplicitValue innerOffset = address.getOffset();
            switch (pOperator) {
              case PLUS: {
                AddressValueAndState newAddress = createAddress(newState, targetObject,
                    innerOffset.add(delta));
                results.add(newAddress);
                break;
              }
              case MINUS: {
                AddressValueAndState newAddress = createAddress(newState, targetObject,
                    innerOffset.subtract(delta));
                results.add(newAddress);
                break;
              }
              default:
                throw new UnsupportedOperationException("unsupported binary operator " +
                    pOperator);
            }
          }
        }
        return AddressValueAndStateList.copyOfAddressList(results);
      }
      default:
        // case of unsupported binary operator, which should not occur
        return AddressValueAndStateList.of(pState);
    }
  }

  public int evaluateSizeof(
      ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge,
      CType pType) throws UnrecognizedCCodeException {
    CSizeofVisitor visitor = new CSizeofVisitor(machineModel, pEdge, pState, pOtherStates);
    try {
      return pType.accept(visitor);
    } catch (IllegalArgumentException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("unable to resolve the type.", pEdge);
    }
  }

  /**
   * A constraint: the type of specified "exp" should be consistent with "pType". Otherwise,
   * please use the above method instead.
   */
  public int evaluateSizeof(
      ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge,
      CType pType, CExpression exp)
      throws UnrecognizedCCodeException {
    CSizeofVisitor visitor = new CSizeofVisitor(machineModel, pEdge, pState, pOtherStates, exp);
    try {
      return pType.accept(visitor);
    } catch (IllegalArgumentException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("unable to resolve the type.", pEdge);
    }
  }

  public List<DeclaredTypeData> evaluateDeclaredSizeof(
      ShapeState pState, List<AbstractState>
      pOtherStates, CFAEdge pEdge, CType pType) throws CPATransferException {
    CSizeofDeclarationVisitor visitor = new CSizeofDeclarationVisitor(pState, pOtherStates, pEdge,
        machineModel);
    return pType.accept(visitor);
  }

  /**
   * This evaluation method transforms assumption into symbolic expression, while necessary
   * reduction and simplification are applied.
   *
   * @param pState a shape state
   * @param pEdge  the CFA edge for assumption
   * @param e      the assumption expression
   * @return symbolic assumption info, consisting of SE, evaluator and new shape state
   */
  public SymbolicAssumeInfo symbolizeAssumption(
      ShapeState pState, List<AbstractState>
      pOtherStates, CFAEdge pEdge, CExpression e, boolean isTop, boolean isEager)
      throws CPATransferException {
    ShapeState newState = new ShapeState(pState);
    Symbolizer symbolizer = new Symbolizer(newState, pOtherStates, pEdge, machineModel, logger,
        isTop, isEager);
    return e.accept(symbolizer);
  }

  /* *************** */
  /* visitor getters */
  /* *************** */

  private SymbolicValueVisitor getExpressionValueVisitor(
      CFAEdge pEdge, ShapeState pState,
      List<AbstractState> pOtherStates) {
    return new SymbolicValueVisitor(pEdge, pState, pOtherStates);
  }

  private SymbolicExpressionVisitor getSymbolicExpressionVisitor(
      CFAEdge pEdge, ShapeState
      pState, List<AbstractState> pOtherStates) {
    return new SymbolicExpressionVisitor(pEdge, pState, pOtherStates, machineModel, logger);
  }

  private StructAndUnionVisitor getStructAndUnionVisitor(
      CFAEdge pEdge, ShapeState pState,
      List<AbstractState> pOtherStates) {
    return new StructAndUnionVisitor(pEdge, pState, pOtherStates);
  }

  private PointerVisitor getPointerVisitor(
      CFAEdge pEdge, ShapeState pState, List<AbstractState>
      pOtherStates) {
    return new PointerVisitor(pEdge, pState, pOtherStates);
  }

  public AssumeEvaluator getAssumeEvaluator() {
    return new AssumeEvaluator();
  }

  private ArrayVisitor getArrayVisitor(
      CFAEdge pEdge, ShapeState pState, List<AbstractState>
      pOtherStates) {
    return new ArrayVisitor(pEdge, pState, pOtherStates);
  }

  private LValueVisitor getLValueVisitor(
      CFAEdge pEdge, ShapeState pState, List<AbstractState>
      pOtherStates) {
    return new LValueVisitor(pEdge, pState, pOtherStates);
  }

  /* ********************** */
  /* shape state read/write */
  /* ********************** */

  /**
   * Invariant: readValue() returns at least one value-state result.
   */
  public SymbolicValueAndStateList readValue(
      ShapeState pState, List<AbstractState> pOtherStates,
      CFAEdge pEdge, SGObject pObject,
      ShapeExplicitValue pOffset, CType pType, CExpression exp)
      throws UnrecognizedCCodeException {
    if (pOffset.isUnknown() || pObject == null) {
      // nothing could be read
      return SymbolicValueAndStateList.of(pState);
    }
    int cOffset = pOffset.getAsInt();
    ShapeExplicitValue oSize = pObject.getSize();
    boolean notFit = cOffset < 0 ||
        (!pObject.equals(SGObject.getNullObject()) && !oSize.isUnknown() &&
            cOffset + evaluateSizeof(pState, pOtherStates, pEdge, pType) > oSize.getAsInt());
    if (notFit) {
      // given offset and type does not fit the memory object
      ShapeState newState = pState.setInvalidRead(exp);
      return SymbolicValueAndStateList.of(newState);
    }
    return pState.readValue(pObject, cOffset, pType, exp);
  }

  public ShapeState writeValue(
      ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge,
      SGObject pObject, ShapeExplicitValue pOffset, long pLength,
      ShapeSymbolicValue pValue) throws UnrecognizedCCodeException {
    return writeValue(pState, pOtherStates, pEdge, pObject, pOffset, UnknownTypes
        .createTypeWithLength(pLength), pValue);
  }

  /**
   * Write a symbolic value into the specified memory region. If the requested memory region
   * exceeds the size of current memory object, we set the INVALID_WRITE flag as true.
   */
  public ShapeState writeValue(
      ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge,
      SGObject pObject, ShapeExplicitValue pOffset, CType pType,
      ShapeSymbolicValue pValue)
      throws UnrecognizedCCodeException {
    if (pOffset.isUnknown() || pObject == null) {
      // nothing is to be changed
      return pState;
    }
    int offset = pOffset.getAsInt();
    ShapeExplicitValue oSize = pObject.getSize();
    boolean notFit = offset < 0 || (!pObject.equals(SGObject.getNullObject()) && !oSize.isUnknown
        () && offset + evaluateSizeof(pState, pOtherStates, pEdge, pType) > oSize.getAsInt());
    if (notFit) {
      // try to perform an invalid memory write
      return pState.setInvalidWrite();
    }
    if (pValue.isUnknown()) {
      // removal of has-value edge is equivalent to assign an undetermined value
      return pState.removeValue(pObject, offset, pType);
    }
    return pState.writeValue(pObject, offset, pType, pValue).getState();
  }

  /* ********************* */
  /* symbolic manipulation */
  /* ********************* */

  /**
   * Get the symbolic value given an explicit value. If such symbolic value does not exist, we
   * create a new one and associate it with the given explicit value.
   *
   * @param pState a shape state
   * @param pValue an explicit value
   * @return symbolic value along with a possibly updated state
   */
  public SymbolicValueAndState createSymbolic(ShapeState pState, KnownExplicitValue pValue) {
    ShapeSymbolicValue value = getSymbolic(pState, pValue);
    if (!value.isUnknown()) {
      return SymbolicValueAndState.of(pState, value);
    }
    KnownSymbolicValue newValue = KnownSymbolicValue.valueOf(SymbolicValueFactory.getNewValue());
    newValue = pState.updateExplicitValue(newValue, pValue);
    return SymbolicValueAndState.of(pState, newValue);
  }

  private ShapeSymbolicValue getSymbolic(ShapeState pState, KnownExplicitValue pValue) {
    return pState.getSymbolic(pValue);
  }

  /* ******************** */
  /* address manipulation */
  /* ******************** */

  /**
   * Get the existing address value or create a new address value for the specified memory region
   *
   * @param pState  shape state
   * @param pObject memory object
   * @param pOffset offset in the memory object
   * @return existing or new-created address value
   */
  public AddressValueAndState createAddress(
      ShapeState pState, SGObject pObject,
      ShapeExplicitValue pOffset) {
    AddressValueAndState address = getAddress(pState, pObject, pOffset);
    ShapeAddressValue addressValue = address.getObject();
    ShapeState newState = address.getShapeState();
    if (addressValue.isUnknown()) {
      if (pOffset.isUnknown()) {
        return AddressValueAndState.of(newState);
      }
      // generate a new value pointing to the specified memory region
      KnownSymbolicValue newValue = KnownSymbolicValue.valueOf(SymbolicValueFactory.getNewValue());
      KnownAddressValue newAddress = KnownAddressValue.valueOf(pObject, (KnownExplicitValue)
          pOffset, newValue);
      // FIX: associate new generated symbolic value with the memory object
      newState = newState.writeAddress(pObject, pOffset.getAsInt(), newValue.getAsLong());
      return AddressValueAndState.of(newState, newAddress);
    } else {
      return address;
    }
  }

  /**
   * Convert a list of addresses to the list of address values.
   */
  private AddressValueAndStateList convertAddressesFrom(List<AddressAndState> pAddresses) {
    List<AddressValueAndState> results = new ArrayList<>(pAddresses.size());
    for (AddressAndState address : pAddresses) {
      results.add(convertAddressFrom(address));
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  /**
   * Convert a single address to address value.
   */
  private AddressValueAndState convertAddressFrom(AddressAndState pAddress) {
    // for each known address, we should create a new symbolic value to represent it
    Address address = pAddress.getObject();
    ShapeState pState = pAddress.getShapeState();
    if (address.isUnknown()) {
      return AddressValueAndState.of(pState);
    }
    return createAddress(pState, address.getObject(), address.getOffset());
  }

  /**
   * Get the address value for the specified memory region
   *
   * @param pState  shape state
   * @param pObject memory object
   * @param pOffset offset in the memory object
   * @return address value
   */
  public AddressValueAndState getAddress(
      ShapeState pState, SGObject pObject,
      ShapeExplicitValue pOffset) {
    if (pObject == null || pOffset.isUnknown()) {
      return AddressValueAndState.of(pState);
    }
    Long address = pState.getAddress(pObject, pOffset.getAsInt());
    if (address == null) {
      return AddressValueAndState.of(pState);
    }
    return pState.getPointToForAddress(address);
  }

  /**
   * Given a symbolic value, we look into the shape graph to determine if the symbolic value
   * represents a pointer, and then interpret it as a {@link ShapeAddressValue} (which is also
   * a symbolic value, but has address semantics).
   *
   * @param pValueAndState a symbolic value, which is contained in shape graph
   * @return the address value, or unknown otherwise
   */
  public AddressValueAndState getAddressFromSymbolicValue(SymbolicValueAndState pValueAndState) {
    if (pValueAndState instanceof AddressValueAndState) {
      return (AddressValueAndState) pValueAndState;
    }
    ShapeSymbolicValue value = pValueAndState.getObject();
    ShapeState state = pValueAndState.getShapeState();
    if (value instanceof ShapeAddressValue) {
      return AddressValueAndState.of(state, (ShapeAddressValue) value);
    }
    if (value.isUnknown()) {
      return AddressValueAndState.of(state);
    }
    if (!state.isAddress(value.getAsLong())) {
      // such symbolic value is not a pointer (address)
      return AddressValueAndState.of(state);
    }
    return state.getPointToForAddress(value.getAsLong());
  }

  /**
   * Convert a symbolic value into an address value (which is also symbolic).
   *
   * @param valueAndStates symbolic value and shape state
   * @return address value and shape state
   */
  public AddressValueAndStateList getAddressFromSymbolicValues(
      SymbolicValueAndStateList valueAndStates) {
    if (valueAndStates instanceof AddressValueAndStateList) {
      return (AddressValueAndStateList) valueAndStates;
    }
    List<AddressValueAndState> results = new ArrayList<>(valueAndStates.size());
    for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
      results.add(getAddressFromSymbolicValue(valueAndState));
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  /**
   * Derive the field information given shape state and field reference expression.
   *
   * @param pState     Specified shape state
   * @param pEdge      CFA edge
   * @param pOwnerType type of field owner
   * @param pFieldName name of field
   * @return field information, an instance of {@link ShapeField}
   */
  private ShapeField getField(
      ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge,
      CType pOwnerType, String pFieldName)
      throws UnrecognizedCCodeException {
    if (pOwnerType instanceof CElaboratedType) {
      CType realType = ((CElaboratedType) pOwnerType).getRealType();
      if (realType == null) {
        return ShapeField.getUnknownInstance();
      }
      return getField(pState, pOtherStates, pEdge, realType, pFieldName);
    } else if (pOwnerType instanceof CCompositeType) {
      return getFieldForCompositeType(pState, pOtherStates, pEdge, (CCompositeType) pOwnerType,
          pFieldName);
    } else if (pOwnerType instanceof CPointerType) {
      CType type = ((CPointerType) pOwnerType).getType();
      return getField(pState, pOtherStates, pEdge, type, pFieldName);
    }
    // we should not reach here
    return ShapeField.getUnknownInstance();
  }

  /**
   * Derive the field information from a field reference where the type of owner is composite type.
   */
  private ShapeField getFieldForCompositeType(
      ShapeState pState, List<AbstractState> pOtherStates,
      CFAEdge pEdge, CCompositeType pOwnerType,
      String pFieldName)
      throws UnrecognizedCCodeException {
    List<CCompositeTypeMemberDeclaration> members = pOwnerType.getMembers();
    int offset = 0;
    for (CCompositeTypeMemberDeclaration member : members) {
      String memberName = member.getName();
      int padding = machineModel.getPadding(offset, member.getType());
      if (memberName.equals(pFieldName)) {
        offset += padding;
        return new ShapeField(KnownExplicitValue.valueOf(offset), getType(member.getType()));
      }
      // otherwise, we continue to visit the next member
      if (pOwnerType.getKind() != ComplexTypeKind.UNION) {
        offset += padding;
        offset += evaluateSizeof(pState, pOtherStates, pEdge, getType(member.getType()));
      }
      // if the owner has union type, the offset always keeps zero
    }
    return new ShapeField(UnknownValue.getInstance(), pOwnerType);
  }

  /* ******************* */
  /* string manipulation */
  /* ******************* */

  /**
   * Check whether the given memory address value is DEFINITELY NOT null-terminated.
   * If the size of memory object is uncertain, it is hard to say if the corresponding memory
   * region is DEFINITELY NOT null-terminated.
   *
   * @param pState        a shape state
   * @param pAddressValue a memory address value
   * @return whether the given address value is null-terminated (i.e. having NULL within its memory
   * region)
   */
  public boolean isNullTerminated(ShapeState pState, ShapeAddressValue pAddressValue) {
    if (pAddressValue.isUnknown()) {
      return true;
    }
    SGObject object = pAddressValue.getObject();
    ShapeExplicitValue objectSize = object.getSize();
    if (objectSize.isUnknown()) {
      return true;
    }
    int length = objectSize.getAsInt();
    BitSet bits;
    int offset = pAddressValue.getOffset().getAsInt();
    int nextBit;
    if (object.isZeroInit()) {
      bits = pState.getNonNullBytesFor(object);
      // set bits are non-null bits
      nextBit = bits.nextClearBit(offset);
    } else {
      bits = pState.getNullBytesFor(object);
      // set bits are null bits
      nextBit = bits.nextSetBit(offset);
    }
    return nextBit >= 0 && nextBit <= offset + length;
  }

  /* ***************** */
  /* important getters */
  /* ***************** */

  public AddressDirection getFunctionDirection() {
    return functionDirection;
  }

  public AddressDirection getVariableDirection() {
    return variableDirection;
  }

  /* ***************** */
  /* auxiliary methods */
  /* ***************** */

  public static CType getType(CRightHandSide rE) {
    CType type = rE.getExpressionType();
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    return type.getCanonicalType();
  }

  public static CType getType(CType pType) {
    CType type = pType;
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    return type.getCanonicalType();
  }

  public static boolean isCompatible(CType pType1, CType pType2) {
    return getType(pType1).equals(getType(pType2));
  }

  public static boolean isStructOrUnion(CType pType) {
    if (pType instanceof CElaboratedType) {
      CElaboratedType eType = (CElaboratedType) pType;
      return eType.getKind() != ComplexTypeKind.ENUM;
    }
    if (pType instanceof CCompositeType) {
      CCompositeType cType = (CCompositeType) pType;
      return cType.getKind() != ComplexTypeKind.ENUM;
    }
    return false;
  }

  private ExplicitValueAndState deriveExplicitValueFromSymbolicValue(
      SymbolicValueAndState valueAndState) {
    ShapeSymbolicValue value = valueAndState.getObject();
    ShapeState state = valueAndState.getShapeState();
    if (!value.isUnknown()) {
      if (value.equals(KnownSymbolicValue.ZERO)) {
        return ExplicitValueAndState.of(state, KnownExplicitValue.ZERO);
      }
      if (value instanceof ShapeAddressValue) {
        ShapeAddressValue address = (ShapeAddressValue) value;
        SGObject object = address.getObject();
        if (object.equals(SGObject.getNullObject())) {
          return ExplicitValueAndState.of(state, KnownExplicitValue.valueOf(address.getOffset()
              .getAsLong()));
        }
      }
    }
    // for other cases, we cannot derive an explicit value
    return ExplicitValueAndState.of(state);
  }

  public int getSizeofChar() {
    return machineModel.getSizeofChar();
  }

  public int getMaxGuardDepth() {
    return maxGuardDepth;
  }

  public int getMaxInterpretations() {
    return maxInterpretations;
  }

  /* *********************** */
  /* constraint manipulation */
  /* *********************** */

  /**
   * The computed symbol set should be a sorted set (i.e. tree set).
   */
  public Set<Long> extractSymbols(ConstraintRepresentation cr) {
    return cr.accept(new SymbolicCollector());
  }

  /**
   * The computed symbol-type mapping should be a sorted map (i.e. tree map).
   */
  public Map<Long, CType> extractTypedSymbols(ConstraintRepresentation cr) {
    return cr.accept(new TypedSymbolicCollector());
  }

  private SymbolicMerger getSymbolicMerger(KnownSymbolicValue pNV, Set<KnownSymbolicValue> pOVs) {
    return new SymbolicMerger(pNV, pOVs);
  }

  public ReducerResult mergeValues(
      SymbolicExpression se, KnownSymbolicValue pNV,
      Set<KnownSymbolicValue> pOVs) {
    SymbolicMerger merger = getSymbolicMerger(pNV, pOVs);
    return se.accept(merger);
  }

}
