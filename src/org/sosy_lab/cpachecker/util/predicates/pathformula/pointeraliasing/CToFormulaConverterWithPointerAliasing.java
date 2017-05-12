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

import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.isSimpleType;

import com.google.common.base.Optional;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializers;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl.MergeResult;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder.RealPointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class CToFormulaConverterWithPointerAliasing extends CtoFormulaConverter {

  // Overrides just for visibility in other classes of this package

  @SuppressWarnings("hiding")
  final LogManagerWithoutDuplicates logger = super.logger;
  @SuppressWarnings("hiding")
  final FormulaManagerView fmgr = super.fmgr;
  @SuppressWarnings("hiding")
  final BooleanFormulaManagerView bfmgr = super.bfmgr;
  @SuppressWarnings("hiding")
  final FunctionFormulaManagerView ffmgr = super.ffmgr;
  @SuppressWarnings("hiding")
  final MachineModel machineModel = super.machineModel;
  @SuppressWarnings("hiding")
  final ShutdownNotifier shutdownNotifier = super.shutdownNotifier;

  final TypeHandlerWithPointerAliasing typeHandler;
  final PointerTargetSetManager ptsMgr;

  final FormulaType<?> voidPointerFormulaType;
  final Formula nullPointer;

  public CToFormulaConverterWithPointerAliasing(
      final FormulaEncodingWithPointerAliasingOptions pOptions,
      final FormulaManagerView formulaManagerView,
      final MachineModel pMachineModel,
      final Optional<VariableClassification> pVariableClassification,
      final LogManager logger,
      final ShutdownNotifier pShutdownNotifier,
      final TypeHandlerWithPointerAliasing pTypeHandler,
      final AnalysisDirection pDirection) {
    super(pOptions, formulaManagerView, pMachineModel, pVariableClassification, logger,
        pShutdownNotifier, pTypeHandler, pDirection);
    variableClassification = pVariableClassification;
    options = pOptions;
    typeHandler = pTypeHandler;
    ptsMgr = new PointerTargetSetManager(options, fmgr, typeHandler, shutdownNotifier);

    voidPointerFormulaType = typeHandler.getFormulaTypeFromCType(CPointerType.POINTER_TO_VOID);
    nullPointer = fmgr.makeNumber(voidPointerFormulaType, 0);
  }

  public static String getUFName(final CType type) {
    String result = ufNameCache.get(type);
    if (result != null) {
      return result;
    } else {
      result = UF_NAME_PREFIX + CTypeUtils.typeToString(type).replace(' ', '_');
      ufNameCache.put(type, result);
      return result;
    }
  }

  public static boolean isUF(final String symbol) {
    return symbol.startsWith(UF_NAME_PREFIX);
  }

  Formula makeBaseAddressOfTerm(final Formula address) {
    return ffmgr.declareAndCallUF("__BASE_ADDRESS_OF__", voidPointerFormulaType, address);
  }

  static CFieldReference eliminateArrow(final CFieldReference e, final CFAEdge edge)
      throws UnrecognizedCCodeException {
    if (e.isPointerDereference()) {
      final CType fieldOwnerType = CTypeUtils.simplifyType(e.getFieldOwner().getExpressionType());
      if (fieldOwnerType instanceof CPointerType) {
        return new CFieldReference(e.getFileLocation(),
            e.getExpressionType(),
            e.getFieldName(),
            new CPointerExpression(e.getFieldOwner().getFileLocation(),
                ((CPointerType) fieldOwnerType).getType(),
                e.getFieldOwner()),
            false);
      } else {
        throw new UnrecognizedCCodeException(
            "Can't dereference a non-pointer in the field reference", edge, e);
      }
    } else {
      return e;
    }
  }

  @Override
  protected void checkSsaSavedType(final String name, final CType type, CType ssaSavedType) {
    if (ssaSavedType != null) {
      ssaSavedType = CTypeUtils.simplifyType(ssaSavedType);
    }
    if (ssaSavedType != null &&
        !ssaSavedType.equals(CTypeUtils.simplifyType(type))) {
      logger.logf(Level.FINEST,
          "Variable %s was found with multiple types! (Type1: %s, Type2: %s)",
          name,
          ssaSavedType,
          type);
    }
  }

  boolean hasIndex(final String name, final CType type, final SSAMapBuilder ssa) {
    checkSsaSavedType(name, type, ssa.getType(name));
    return ssa.getIndex(name) > 0;
  }

  Formula makeDereference(
      CType type,
      final Formula address,
      final SSAMapBuilder ssa,
      final ErrorConditions errorConditions) {
    if (errorConditions.isEnabled()) {
      errorConditions.addInvalidDerefCondition(fmgr.makeEqual(address, nullPointer));
      errorConditions.addInvalidDerefCondition(
          fmgr.makeLessThan(address, makeBaseAddressOfTerm(address), false));
    }
    return makeSafeDereference(type, address, ssa);
  }

  Formula makeSafeDereference(
      CType type,
      final Formula address,
      final SSAMapBuilder ssa) {
    type = CTypeUtils.simplifyType(type);
    final String ufName = getUFName(type);
    final int index = getIndex(ufName, type, ssa);
    final FormulaType<?> returnType = getFormulaTypeFromCType(type);
    return ffmgr.declareAndCallUninterpretedFunction(ufName, index, returnType, address);
  }

  @Override
  protected boolean isRelevantField(
      final CCompositeType compositeType,
      final String fieldName) {
    return super.isRelevantField(compositeType, fieldName)
        || getSizeof(compositeType) <= options.maxPreFilledAllocationSize();
  }

  boolean isAddressedVariable(CDeclaration var) {
    return !variableClassification.isPresent() ||
        variableClassification.get().getAddressedVariables().contains(var.getQualifiedName());
  }

  private void addAllFields(final CType type, final PointerTargetSetBuilder pts) {
    if (type instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) type;
      for (CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        if (isRelevantField(compositeType, memberDeclaration.getName())) {
          pts.addField(compositeType, memberDeclaration.getName());
          final CType memberType = CTypeUtils.simplifyType(memberDeclaration.getType());
          addAllFields(memberType, pts);
        }
      }
    } else if (type instanceof CArrayType) {
      final CType elementType = CTypeUtils.simplifyType(((CArrayType) type).getType());
      addAllFields(elementType, pts);
    }
  }

  void addPreFilledBase(
      final String base,
      final CType type,
      final boolean prepared,
      final boolean forcePreFill,
      final Constraints constraints,
      final PointerTargetSetBuilder pts) {
    if (!prepared) {
      constraints.addConstraint(pts.addBase(base, type));
    } else {
      pts.shareBase(base, type);
    }
    if (forcePreFill ||
        (options.maxPreFilledAllocationSize() > 0 && getSizeof(type) <= options
            .maxPreFilledAllocationSize())) {
      addAllFields(type, pts);
    }
  }

  private void declareSharedBase(
      final CDeclaration declaration, final boolean shareImmediately,
      final Constraints constraints, final PointerTargetSetBuilder pts) {
    if (shareImmediately) {
      addPreFilledBase(declaration.getQualifiedName(), declaration.getType(), false, false,
          constraints, pts);
    } else if (isAddressedVariable(declaration) ||
        CTypeUtils.containsArray(declaration.getType())) {
      constraints.addConstraint(pts.prepareBase(declaration.getQualifiedName(),
          CTypeUtils.simplifyType(declaration.getType())));
    }
  }

  void addValueImportConstraints(
      final CFAEdge cfaEdge,
      final Formula address,
      final Variable base,
      final List<Pair<CCompositeType, String>> fields,
      final SSAMapBuilder ssa,
      final Constraints constraints,
      final PointerTargetSetBuilder pts) throws UnrecognizedCCodeException {
    final CType baseType = CTypeUtils.simplifyType(base.getType());
    if (baseType instanceof CArrayType) {
      throw new UnrecognizedCCodeException("Array access can't be encoded as a varaible", cfaEdge);
    } else if (baseType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) baseType;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: "
          + compositeType;
      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        final String memberName = memberDeclaration.getName();
        final CType memberType = CTypeUtils.simplifyType(memberDeclaration.getType());
        final Variable newBase = Variable.create(base.getName() + FIELD_NAME_SEPARATOR + memberName,
            memberType);
        if (hasIndex(newBase.getName(), newBase.getType(), ssa) &&
            isRelevantField(compositeType, memberName)) {
          fields.add(Pair.of(compositeType, memberName));
          addValueImportConstraints(cfaEdge,
              fmgr.makePlus(address, fmgr.makeNumber(voidPointerFormulaType, offset)),
              newBase,
              fields,
              ssa,
              constraints,
              pts);
        }
        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          offset += getSizeof(memberType);
        }
      }
    } else if (!(baseType instanceof CFunctionType)) {
      // This adds a constraint *a = a for the case where we previously tracked
      // a variable directly and now via its address (we do not want to loose
      // the value previously stored in the variable).
      // Make sure to not add invalid-deref constraints for this dereference
      constraints.addConstraint(fmgr.makeEqual(makeSafeDereference(baseType, address, ssa),
          makeVariable(base.getName(), baseType, ssa)));
    }
  }

  /**
   * Expand a string literal to a array of characters.
   *
   * http://stackoverflow.com/a/6915917
   * As the C99 Draft Specification's 32nd Example in §6.7.8 (p. 130) states
   * char s[] = "abc", t[3] = "abc";
   * is identical to:
   * char s[] = { 'a', 'b', 'c', '\0' }, t[] = { 'a', 'b', 'c' };
   *
   * @param e    The string that has to be expanded
   * @param type the type of the array
   * @return List of character-literal expressions
   */
  private static List<CCharLiteralExpression> expandStringLiteral(
      final CStringLiteralExpression e,
      final CArrayType type) {
    // The string is either NULL terminated, or not.
    // If the length is not provided explicitly, NULL termination is used
    Integer length = CTypeUtils.getArrayLength(type);
    final String s = e.getContentString();
    if (length == null) {
      length = s.length() + 1;
    }
    assert length >= s.length();

    // create one CharLiteralExpression for each character of the string
    final List<CCharLiteralExpression> result = new ArrayList<>();
    for (int i = 0; i < s.length(); i++) {
      result.add(
          new CCharLiteralExpression(e.getFileLocation(), CNumericTypes.SIGNED_CHAR, s.charAt(i)));
    }


    // http://stackoverflow.com/questions/10828294/c-and-c-partial-initialization-of-automatic-structure
    // C99 Standard 6.7.8.21
    // If there are ... fewer characters in a string literal
    // used to initialize an array of known size than there are elements in the array,
    // the remainder of the aggregate shall be initialized implicitly ...
    for (int i = s.length(); i < length; i++) {
      result.add(new CCharLiteralExpression(e.getFileLocation(), CNumericTypes.SIGNED_CHAR, '\0'));
    }

    return result;
  }

  private static List<CExpressionAssignmentStatement> expandStringLiterals(
      final List<CExpressionAssignmentStatement> assignments)
      throws UnrecognizedCCodeException {
    final List<CExpressionAssignmentStatement> result = new ArrayList<>();
    for (CExpressionAssignmentStatement assignment : assignments) {
      final CExpression rhs = assignment.getRightHandSide();
      if (rhs instanceof CStringLiteralExpression) {
        final CExpression lhs = assignment.getLeftHandSide();
        final CType lhsType = lhs.getExpressionType();
        final CArrayType lhsArrayType;
        if (lhsType instanceof CArrayType) {
          lhsArrayType = (CArrayType) lhsType;
        } else if (lhsType instanceof CPointerType) {
          lhsArrayType = new CArrayType(false, false, ((CPointerType) lhsType).getType(), null);
        } else {
          throw new UnrecognizedCCodeException("Assigning string literal to " + lhsType.toString(),
              assignment);
        }

        List<CCharLiteralExpression> chars =
            expandStringLiteral((CStringLiteralExpression) rhs, lhsArrayType);

        int offset = 0;
        for (CCharLiteralExpression e : chars) {
          result.add(new CExpressionAssignmentStatement(
              assignment.getFileLocation(),
              new CArraySubscriptExpression(lhs.getFileLocation(),
                  lhsArrayType.getType(),
                  lhs,
                  new CIntegerLiteralExpression(lhs.getFileLocation(),
                      CNumericTypes.INT,
                      BigInteger.valueOf(offset))),
              e));
          offset++;
        }
      } else {
        result.add(assignment);
      }
    }
    return result;
  }

  private List<CExpressionAssignmentStatement> expandAssignmentList(
      final CVariableDeclaration declaration,
      final List<CExpressionAssignmentStatement> explicitAssignments) {
    final CType variableType = CTypeUtils.simplifyType(declaration.getType());
    final CLeftHandSide lhs = new CIdExpression(declaration.getFileLocation(),
        variableType,
        declaration.getName(),
        declaration);
    final Set<String> alreadyAssigned = new HashSet<>();
    for (CExpressionAssignmentStatement statement : explicitAssignments) {
      alreadyAssigned.add(statement.getLeftHandSide().toString());
    }
    final List<CExpressionAssignmentStatement> defaultAssignments = new ArrayList<>();
    expandAssignmentList(variableType, lhs, alreadyAssigned, defaultAssignments);
    defaultAssignments.addAll(explicitAssignments);
    return defaultAssignments;
  }

  private void expandAssignmentList(
      CType type,
      final CLeftHandSide lhs,
      final Set<String> alreadyAssigned,
      final List<CExpressionAssignmentStatement> defaultAssignments) {
    if (alreadyAssigned.contains(lhs.toString())) {
      return;
    }

    type = CTypeUtils.simplifyType(type);
    if (type instanceof CArrayType) {
      final CArrayType arrayType = (CArrayType) type;
      final CType elementType = CTypeUtils.simplifyType(arrayType.getType());
      final Integer length = CTypeUtils.getArrayLength(arrayType);
      if (length != null) {
        for (int i = 0; i < Math.min(length, options.maxArrayLength()); i++) {
          final CLeftHandSide newLhs = new CArraySubscriptExpression(
              lhs.getFileLocation(),
              elementType,
              lhs,
              new CIntegerLiteralExpression(lhs.getFileLocation(),
                  CNumericTypes.INT,
                  BigInteger.valueOf(i)));
          expandAssignmentList(elementType, newLhs, alreadyAssigned, defaultAssignments);
        }
      }
    } else if (type instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) type;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        final CType memberType = memberDeclaration.getType();
        final CLeftHandSide newLhs = new CFieldReference(lhs.getFileLocation(),
            memberType,
            memberDeclaration.getName(),
            lhs, false);
        expandAssignmentList(memberType, newLhs, alreadyAssigned, defaultAssignments);
      }
    } else {
      assert isSimpleType(type);
      CExpression initExp =
          ((CInitializerExpression) CDefaults.forType(type, lhs.getFileLocation())).getExpression();
      defaultAssignments
          .add(new CExpressionAssignmentStatement(lhs.getFileLocation(), lhs, initExp));
    }
  }

  @Override
  protected PointerTargetSetBuilder createPointerTargetSetBuilder(PointerTargetSet pts) {
    return new RealPointerTargetSetBuilder(pts, fmgr, ptsMgr, options);
  }

  @Override
  public MergeResult<PointerTargetSet> mergePointerTargetSets(
      PointerTargetSet pPts1,
      PointerTargetSet pPts2, SSAMapBuilder pResultSSA) throws InterruptedException {
    return ptsMgr.mergePointerTargetSets(pPts1, pPts2, pResultSSA, this);
  }

  @Override
  protected CRightHandSideVisitor<Formula, UnrecognizedCCodeException> createCRightHandSideVisitor(
      CFAEdge pEdge, String pFunction,
      SSAMapBuilder pSsa, PointerTargetSetBuilder pPts,
      Constraints pConstraints, ErrorConditions pErrorConditions) {

    CExpressionVisitorWithPointerAliasing rhsVisitor =
        new CExpressionVisitorWithPointerAliasing(this, pEdge, pFunction, pSsa, pConstraints,
            pErrorConditions, pPts);
    return rhsVisitor.asFormulaVisitor();
  }

  @Override
  protected BooleanFormula makeReturn(
      final Optional<CAssignment> assignment,
      final CReturnStatementEdge returnEdge,
      final String function,
      final SSAMapBuilder ssa,
      final PointerTargetSetBuilder pts,
      final Constraints constraints,
      final ErrorConditions errorConditions)
      throws UnrecognizedCCodeException, InterruptedException {
    BooleanFormula result =
        super.makeReturn(assignment, returnEdge, function, ssa, pts, constraints, errorConditions);

    if (assignment.isPresent()) {
      final CVariableDeclaration returnVariableDeclaraton =
          ((CFunctionEntryNode) returnEdge.getSuccessor().getEntryNode()).getReturnVariable().get();
      final boolean containsArray = CTypeUtils.containsArray(returnVariableDeclaraton.getType());

      declareSharedBase(returnVariableDeclaraton, containsArray, constraints, pts);
    }
    return result;
  }

  @Override
  protected BooleanFormula makeAssignment(
      final CLeftHandSide lhs, final CLeftHandSide lhsForChecking, CRightHandSide rhs,
      final CFAEdge edge, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
      throws UnrecognizedCCodeException, InterruptedException {

    if (rhs instanceof CExpression) {
      rhs = makeCastFromArrayToPointerIfNecessary((CExpression) rhs, lhs.getExpressionType());
    }

    AssignmentHandler assignmentHandler =
        new AssignmentHandler(this, edge, function, ssa, pts, constraints, errorConditions);
    return assignmentHandler.handleAssignment(lhs, lhsForChecking, rhs, false, null);
  }

  private static String getLogMessage(final String msg, final CFAEdge edge) {
    return edge.getFileLocation()
        + ": " + msg
        + ": " + edge.getDescription();
  }

  private void logDebug(final String msg, final CFAEdge edge) {
    if (logger.wouldBeLogged(Level.ALL)) {
      logger.log(Level.ALL, getLogMessage(msg, edge));
    }
  }

  @Override
  protected BooleanFormula makeDeclaration(
      final CDeclarationEdge declarationEdge, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
      throws UnrecognizedCCodeException, InterruptedException {

    // TODO merge with super-class method

    if (declarationEdge.getDeclaration() instanceof CTypeDeclaration) {
      final CType declarationType = CTypeUtils.simplifyType(
          (declarationEdge.getDeclaration()).getType());
      if (declarationType instanceof CCompositeType) {
        typeHandler.addCompositeTypeToCache((CCompositeType) declarationType);
      }
    }

    if (!(declarationEdge.getDeclaration() instanceof CVariableDeclaration)) {
      // function declaration, typedef etc.
      logDebug("Ignoring declaration", declarationEdge);
      return bfmgr.makeBoolean(true);
    }

    CVariableDeclaration declaration = (CVariableDeclaration) declarationEdge.getDeclaration();

    // makeFreshIndex(variableName, declaration.getType(), ssa); // TODO: Make sure about
    // correctness of SSA indices without this trick!

    CType declarationType = CTypeUtils.simplifyType(declaration.getType());

    if (!isRelevantVariable(declaration) &&
        !isAddressedVariable(declaration)) {
      // The variable is unused
      logDebug("Ignoring declaration of unused variable", declarationEdge);
      return bfmgr.makeBoolean(true);
    }

    if (declarationType instanceof CArrayType) {
      CType elementType = ((CArrayType) declarationType).getType();
      if (elementType instanceof CSimpleType && ((CSimpleType) elementType).getType()
          .isFloatingPointType()) {

        CExpression length = ((CArrayType) declarationType).getLength();
        if (length instanceof CIntegerLiteralExpression) {
          if (((CIntegerLiteralExpression) length).getValue().longValue() > 100) {
            throw new UnsupportedCCodeException("large floating-point array", declarationEdge);
          }
        }
      }

      if (elementType instanceof CSimpleType
          && ((CSimpleType) elementType).getType() == CBasicType.INT) {

        CExpression length = ((CArrayType) declarationType).getLength();
        if (length instanceof CIntegerLiteralExpression) {
          if (((CIntegerLiteralExpression) length).getValue().longValue() >= 10000) {
            throw new UnsupportedCCodeException("large integer array", declarationEdge);
          }
        }
      }
    }

    if (errorConditions.isEnabled()) {
      final Formula address =
          makeConstant(PointerTargetSet.getBaseName(declaration.getQualifiedName()),
              CTypeUtils.getBaseType(declarationType));
      constraints.addConstraint(fmgr.makeEqual(makeBaseAddressOfTerm(address), address));
    }

    // if there is an initializer associated to this variable,
    // take it into account
    final CInitializer initializer = declaration.getInitializer();

    // Fixing unsized array declarations
    if (declarationType instanceof CArrayType
        && ((CArrayType) declarationType).getLength() == null) {
      final Integer actualLength;
      if (initializer instanceof CInitializerList) {
        actualLength = ((CInitializerList) initializer).getInitializers().size();
      } else if (initializer instanceof CInitializerExpression &&
          ((CInitializerExpression) initializer)
              .getExpression() instanceof CStringLiteralExpression) {
        actualLength =
            ((CStringLiteralExpression) ((CInitializerExpression) initializer).getExpression())
                .getContentString()
                .length() + 1;
      } else {
        actualLength = null;
      }

      if (actualLength != null) {
        declarationType = new CArrayType(declarationType.isConst(),
            declarationType.isVolatile(),
            ((CArrayType) declarationType).getType(),
            new CIntegerLiteralExpression(declaration.getFileLocation(),
                machineModel.getPointerDiffType(),
                BigInteger.valueOf(actualLength)));

        declaration = new CVariableDeclaration(declaration.getFileLocation(),
            declaration.isGlobal(),
            declaration.getCStorageClass(),
            declarationType,
            declaration.getName(),
            declaration.getOrigName(),
            declaration.getQualifiedName(),
            initializer);
      }
    }

    declareSharedBase(declaration, false, constraints, pts);
    if (CTypeUtils.containsArray(declarationType)) {
      addPreFilledBase(declaration.getQualifiedName(), declarationType, true, false, constraints,
          pts);
    }

    if (options.useParameterVariablesForGlobals() && declaration.isGlobal()) {
      globalDeclarations.add(declaration);
    }

    final CIdExpression lhs =
        new CIdExpression(declaration.getFileLocation(), declaration);
    final AssignmentHandler assignmentHandler =
        new AssignmentHandler(this, declarationEdge, function, ssa, pts, constraints,
            errorConditions);
    final BooleanFormula result;
    if (initializer instanceof CInitializerExpression || initializer == null) {

      if (initializer != null) {
        result = assignmentHandler
            .handleAssignment(lhs, lhs, ((CInitializerExpression) initializer).getExpression(),
                false, null);
      } else if (isRelevantVariable(declaration)) {
        result = assignmentHandler.handleAssignment(lhs, lhs, null, false, null);
      } else {
        result = bfmgr.makeBoolean(true);
      }

    } else if (initializer instanceof CInitializerList) {

      List<CExpressionAssignmentStatement> assignments =
          CInitializers.convertToAssignments(declaration, declarationEdge);
      if (options.handleStringLiteralInitializers()) {
        // Special handling for string literal initializers -- convert them into character arrays
        assignments = expandStringLiterals(assignments);
      }
      if (options.handleImplicitInitialization()) {
        assignments = expandAssignmentList(declaration, assignments);
      }

      result = assignmentHandler.handleInitializationAssignments(lhs, assignments);

    } else {
      throw new UnrecognizedCCodeException("Unrecognized initializer", declarationEdge,
          initializer);
    }

    return result;
  }

  @Override
  protected BooleanFormula makePredicate(
      final CExpression e, final boolean truthAssumtion,
      final CFAEdge edge, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
      throws UnrecognizedCCodeException, InterruptedException {
    final CType expressionType = CTypeUtils.simplifyType(e.getExpressionType());
    CExpressionVisitorWithPointerAliasing ev =
        new CExpressionVisitorWithPointerAliasing(this, edge, function, ssa, constraints,
            errorConditions, pts);
    BooleanFormula result = toBooleanFormula(ev.asValueFormula(e.accept(ev), expressionType));

    if (options.deferUntypedAllocations()) {
      DynamicMemoryHandler memoryHandler =
          new DynamicMemoryHandler(this, edge, ssa, pts, constraints, errorConditions);
      memoryHandler.handleDeferredAllocationsInAssume(e, ev.getUsedDeferredAllocationPointers());
    }

    if (!truthAssumtion) {
      result = bfmgr.not(result);
    }

    pts.addEssentialFields(ev.getInitializedFields());
    pts.addEssentialFields(ev.getUsedFields());
    return result;
  }

  @Override
  protected BooleanFormula makeFunctionCall(
      final CFunctionCallEdge edge, final String callerFunction,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
      throws UnrecognizedCCodeException, InterruptedException {

    final CFunctionEntryNode entryNode = edge.getSuccessor();
    BooleanFormula result =
        super.makeFunctionCall(edge, callerFunction, ssa, pts, constraints, errorConditions);

    for (CParameterDeclaration formalParameter : entryNode.getFunctionParameters()) {
      final CType parameterType = CTypeUtils.simplifyType(formalParameter.getType());
      final CVariableDeclaration formalDeclaration = formalParameter.asVariableDeclaration();
      final CVariableDeclaration declaration;
      if (options.useParameterVariables()) {
        CParameterDeclaration tmpParameter = new CParameterDeclaration(
            formalParameter.getFileLocation(), formalParameter.getType(),
            formalParameter.getName() + PARAM_VARIABLE_NAME);
        tmpParameter.setQualifiedName(formalParameter.getQualifiedName() + PARAM_VARIABLE_NAME);
        declaration = tmpParameter.asVariableDeclaration();
      } else {
        declaration = formalDeclaration;
      }
      declareSharedBase(declaration, CTypeUtils.containsArray(parameterType), constraints, pts);
    }

    return result;
  }

  @Override
  protected BooleanFormula makeExitFunction(
      final CFunctionSummaryEdge summaryEdge, final String calledFunction,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
      throws UnrecognizedCCodeException, InterruptedException {

    final BooleanFormula result =
        super.makeExitFunction(summaryEdge, calledFunction, ssa, pts, constraints, errorConditions);

    DynamicMemoryHandler memoryHandler =
        new DynamicMemoryHandler(this, summaryEdge, ssa, pts, constraints, errorConditions);
    memoryHandler.handleDeferredAllocationInFunctionExit(calledFunction);

    return result;
  }

  @SuppressWarnings("hiding") // same instance with narrower type
  final FormulaEncodingWithPointerAliasingOptions options;

  private final Optional<VariableClassification> variableClassification;

  static final String UF_NAME_PREFIX = "*";

  static final String FIELD_NAME_SEPARATOR = "$";

  private static final Map<CType, String> ufNameCache = new IdentityHashMap<>();


  // Overrides just for visibility in other classes of this package

  @Override
  protected CType getReturnType(CFunctionCallExpression pFuncCallExp, CFAEdge pEdge)
      throws UnrecognizedCCodeException {
    return super.getReturnType(pFuncCallExp, pEdge);
  }

  @Override
  protected CExpression makeCastFromArrayToPointerIfNecessary(CExpression pExp, CType pTargetType) {
    return super.makeCastFromArrayToPointerIfNecessary(pExp, pTargetType);
  }

  @Override
  protected Formula makeCast(
      CType pFromType,
      CType pToType,
      Formula pFormula,
      Constraints constraints,
      CFAEdge pEdge)
      throws UnrecognizedCCodeException {
    return super.makeCast(pFromType, pToType, pFormula, constraints, pEdge);
  }

  @Override
  protected Formula makeConstant(String pName, CType pType) {
    return super.makeConstant(pName, pType);
  }

  @Override
  protected Formula makeVariable(String pName, CType pType, SSAMapBuilder pSsa) {
    return super.makeVariable(pName, pType, pSsa);
  }

  @Override
  protected Formula makeFreshVariable(String pName, CType pType, SSAMapBuilder pSsa) {
    return super.makeFreshVariable(pName, pType, pSsa);
  }

  @Override
  public int makeFreshIndex(String pName, CType pType, SSAMapBuilder pSsa) {
    return super.makeFreshIndex(pName, pType, pSsa);
  }

  @Override
  protected int getIndex(String pName, CType pType, SSAMapBuilder pSsa) {
    return super.getIndex(pName, pType, pSsa);
  }

  @Override
  protected int getFreshIndex(String pName, CType pType, SSAMapBuilder pSsa) {
    return super.getFreshIndex(pName, pType, pSsa);
  }

  @Override
  protected int getSizeof(CType pType) {
    return super.getSizeof(pType);
  }

  @Override
  protected boolean isRelevantLeftHandSide(CLeftHandSide pLhs) {
    return super.isRelevantLeftHandSide(pLhs);
  }
}
