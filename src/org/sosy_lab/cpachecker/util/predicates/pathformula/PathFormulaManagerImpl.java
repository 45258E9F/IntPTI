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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.MapsDifference.collectMapsDifferenceTo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.MapsDifference;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.arrays.CToFormulaConverterWithArrays;
import org.sosy_lab.cpachecker.util.predicates.pathformula.arrays.CtoFormulaTypeHandlerWithArrays;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.heaparray.CToFormulaConverterWithHeapArray;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTarget;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.QuantifiedFormulaManagerView;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.Model.ValueAssignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Class implementing the FormulaManager interface,
 * providing some commonly used stuff which is independent from specific libraries.
 *
 * This class inherits from CtoFormulaConverter to import the stuff there.
 */
@Options(prefix = "cpa.predicate")
public class PathFormulaManagerImpl implements PathFormulaManager {

  @Option(secure = true, description = "Handle aliasing of pointers. "
      + "This adds disjunctions to the formulas, so be careful when using cartesian abstraction.")
  private boolean handlePointerAliasing = true;

  @Option(secure = true, description = "Handle arrays using the theory of arrays.")
  private boolean handleArrays = false;

  @Option(secure = true, description = "Call 'simplify' on generated formulas.")
  private boolean simplifyGeneratedPathFormulas = false;

  @Option(secure = true, description = "Use the theory of arrays for heap memory abstraction. "
      + "This supports pointer aliasing and replaces the option \"handlePointerAliasing\".")
  private boolean handleHeapArray = false;

  @Option(secure = true, description = "Use quantifiers together with the heap-array converter. "
      + "This requires the option \"handleHeapArray=true\" and a SMT solver that is capable of the "
      + "theory of arrays and quantifiers (e.g. Z3 or PRINCESS). Universal quantifiers will only "
      + "be introduced for array initializer statements.")
  private boolean useQuantifiersOnArrays = false;

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

  private static final String NONDET_VARIABLE = "__nondet__";
  private static final String NONDET_FLAG_VARIABLE = NONDET_VARIABLE + "flag__";
  private static final CType NONDET_TYPE = CNumericTypes.INT;
  private final FormulaType<?> NONDET_FORMULA_TYPE;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final FunctionFormulaManagerView ffmgr;
  private final ArrayFormulaManagerView afmgr;
  private final CtoFormulaConverter converter;
  private final CtoFormulaTypeHandler typeHandler;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  @Option(secure = true, description = "add special information to formulas about non-deterministic functions")
  private boolean useNondetFlags = false;

  private final AnalysisDirection direction;

  @Deprecated
  public PathFormulaManagerImpl(
      FormulaManagerView pFmgr,
      Configuration config, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      MachineModel pMachineModel, AnalysisDirection pDirection)
      throws InvalidConfigurationException {

    this(pFmgr, config, pLogger, pShutdownNotifier,
        pMachineModel, Optional.<VariableClassification>absent(), pDirection);
  }

  public PathFormulaManagerImpl(
      FormulaManagerView pFmgr,
      Configuration config, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      CFA pCfa, AnalysisDirection pDirection)
      throws InvalidConfigurationException {

    this(pFmgr, config, pLogger, pShutdownNotifier, pCfa.getMachineModel(),
        pCfa.getVarClassification(), pDirection);
  }

  @VisibleForTesting
  PathFormulaManagerImpl(
      FormulaManagerView pFmgr,
      Configuration config, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      MachineModel pMachineModel,
      Optional<VariableClassification> pVariableClassification, AnalysisDirection pDirection)
      throws InvalidConfigurationException {

    config.inject(this, PathFormulaManagerImpl.class);

    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    ffmgr = fmgr.getFunctionFormulaManager();
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    direction = pDirection;

    if (handleArrays) {
      afmgr = fmgr.getArrayFormulaManager();
      final FormulaEncodingOptions options = new FormulaEncodingOptions(config);
      typeHandler = new CtoFormulaTypeHandlerWithArrays(pLogger, pMachineModel);
      converter = new CToFormulaConverterWithArrays(options, fmgr, pMachineModel,
          pVariableClassification, logger, shutdownNotifier, typeHandler, direction);

      logger.log(Level.WARNING,
          "Handling of pointer aliasing is disabled, analysis is unsound if aliased pointers exist.");

    } else if (handleHeapArray) {
      afmgr = fmgr.getArrayFormulaManager();
      final FormulaEncodingWithPointerAliasingOptions options =
          new FormulaEncodingWithPointerAliasingOptions(config);
      TypeHandlerWithPointerAliasing aliasingTypeHandler =
          new TypeHandlerWithPointerAliasing(pLogger, pMachineModel, options);
      typeHandler = aliasingTypeHandler;

      if (useQuantifiersOnArrays) {
        QuantifiedFormulaManagerView qfmgr = fmgr.getQuantifiedFormulaManager();
        assert qfmgr != null : "To use the analysis with option \"cpa.predicate"
            + ".useQuantifiersOnArrays=true\", you have to use a solver supporting quantifier "
            + "theories!";

        converter = new CToFormulaConverterWithHeapArray(options, fmgr, pMachineModel,
            pVariableClassification, logger, shutdownNotifier, aliasingTypeHandler, direction,
            qfmgr);
      } else {
        converter = new CToFormulaConverterWithHeapArray(options, fmgr, pMachineModel,
            pVariableClassification, logger, shutdownNotifier, aliasingTypeHandler, direction);
      }
    } else if (handlePointerAliasing) {
      afmgr = null;
      final FormulaEncodingWithPointerAliasingOptions options =
          new FormulaEncodingWithPointerAliasingOptions(config);
      TypeHandlerWithPointerAliasing aliasingTypeHandler =
          new TypeHandlerWithPointerAliasing(pLogger, pMachineModel, options);
      typeHandler = aliasingTypeHandler;
      converter = new CToFormulaConverterWithPointerAliasing(options, fmgr,
          pMachineModel, pVariableClassification, logger, shutdownNotifier,
          aliasingTypeHandler, direction);

    } else {
      afmgr = null;
      final FormulaEncodingOptions options = new FormulaEncodingOptions(config);
      typeHandler = new CtoFormulaTypeHandler(pLogger, pMachineModel);
      converter = new CtoFormulaConverter(options, fmgr, pMachineModel,
          pVariableClassification, logger, shutdownNotifier, typeHandler, direction);

      logger.log(Level.WARNING,
          "Handling of pointer aliasing is disabled, analysis is unsound if aliased pointers exist.");
    }

    NONDET_FORMULA_TYPE = converter.getFormulaTypeFromCType(NONDET_TYPE);
  }

  @Override
  public Pair<PathFormula, ErrorConditions> makeAndWithErrorConditions(
      PathFormula pOldFormula,
      final CFAEdge pEdge) throws CPATransferException, InterruptedException {
    ErrorConditions errorConditions = new ErrorConditions(bfmgr);
    PathFormula pf = makeAnd(pOldFormula, pEdge, errorConditions);

    return Pair.of(pf, errorConditions);
  }

  private PathFormula makeAnd(
      PathFormula pOldFormula,
      final CFAEdge pEdge,
      ErrorConditions errorConditions)
      throws UnrecognizedCCodeException, UnrecognizedCFAEdgeException, InterruptedException {
    PathFormula pf = converter.makeAnd(pOldFormula, pEdge, errorConditions);

    if (useNondetFlags) {
      SSAMapBuilder ssa = pf.getSsa().builder();

      int lNondetIndex = ssa.getIndex(NONDET_VARIABLE);
      int lFlagIndex = ssa.getIndex(NONDET_FLAG_VARIABLE);

      if (lNondetIndex != lFlagIndex) {
        if (lFlagIndex < 0) {
          lFlagIndex =
              1; // ssa indices start with 2, so next flag that is generated also uses index 2
        }

        BooleanFormula edgeFormula = pf.getFormula();

        for (int lIndex = lFlagIndex + 1; lIndex <= lNondetIndex; lIndex++) {
          Formula nondetVar = fmgr.makeVariable(NONDET_FORMULA_TYPE, NONDET_FLAG_VARIABLE, lIndex);
          BooleanFormula lAssignment =
              fmgr.assignment(nondetVar, fmgr.makeNumber(NONDET_FORMULA_TYPE, 1));
          edgeFormula = bfmgr.and(edgeFormula, lAssignment);
        }

        // update ssa index of nondet flag
        //setSsaIndex(ssa, Variable.create(NONDET_FLAG_VARIABLE, getNondetType()), lNondetIndex);
        ssa.setIndex(NONDET_FLAG_VARIABLE, NONDET_TYPE, lNondetIndex);

        pf = new PathFormula(edgeFormula, ssa.build(), pf.getPointerTargetSet(), pf.getLength());
      }
    }
    if (simplifyGeneratedPathFormulas) {
      pf = pf.updateFormula(fmgr.simplify(pf.getFormula()));
    }
    return pf;
  }

  @Override
  public PathFormula makeAnd(PathFormula pOldFormula, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    ErrorConditions errorConditions = ErrorConditions.dummyInstance(bfmgr);
    return makeAnd(pOldFormula, pEdge, errorConditions);
  }

  @Override
  public PathFormula makeEmptyPathFormula() {
    return new PathFormula(bfmgr.makeBoolean(true),
        SSAMap.emptySSAMap(),
        PointerTargetSet.emptyPointerTargetSet(),
        0);
  }

  @Override
  public PathFormula makeEmptyPathFormula(PathFormula oldFormula) {
    return new PathFormula(bfmgr.makeBoolean(true),
        oldFormula.getSsa(),
        oldFormula.getPointerTargetSet(),
        0);
  }

  @Override
  public PathFormula makeNewPathFormula(PathFormula oldFormula, SSAMap m) {
    return new PathFormula(oldFormula.getFormula(),
        m,
        oldFormula.getPointerTargetSet(),
        oldFormula.getLength());
  }

  @Override
  public PathFormula makeOr(final PathFormula pathFormula1, final PathFormula pathFormula2)
      throws InterruptedException {

    final BooleanFormula formula1 = pathFormula1.getFormula();
    final BooleanFormula formula2 = pathFormula2.getFormula();
    final SSAMap ssa1 = pathFormula1.getSsa();
    final SSAMap ssa2 = pathFormula2.getSsa();

    final PointerTargetSet pts1 = pathFormula1.getPointerTargetSet();
    final PointerTargetSet pts2 = pathFormula2.getPointerTargetSet();

    final MergeResult<SSAMap> mergeSSAResult = mergeSSAMaps(ssa1, pts1, ssa2, pts2);
    final SSAMapBuilder newSSA = mergeSSAResult.getResult().builder();

    final MergeResult<PointerTargetSet> mergePtsResult =
        converter.mergePointerTargetSets(pts1, pts2, newSSA);

    // (?) Do not swap these two lines, that makes a huge difference in performance (?) !
    final BooleanFormula newFormula1 = bfmgr.and(formula1,
        bfmgr.and(mergeSSAResult.getLeftConjunct(), mergePtsResult.getLeftConjunct()));
    final BooleanFormula newFormula2 = bfmgr.and(formula2,
        bfmgr.and(mergeSSAResult.getRightConjunct(), mergePtsResult.getRightConjunct()));
    final BooleanFormula newFormula = bfmgr.and(bfmgr.or(newFormula1, newFormula2),
        bfmgr.and(mergeSSAResult.getFinalConjunct(), mergePtsResult.getFinalConjunct()));
    final PointerTargetSet newPTS = mergePtsResult.getResult();
    final int newLength = Math.max(pathFormula1.getLength(), pathFormula2.getLength());

    PathFormula out = new PathFormula(newFormula, newSSA.build(), newPTS, newLength);
    if (simplifyGeneratedPathFormulas) {
      out = out.updateFormula(fmgr.simplify(out.getFormula()));
    }
    return out;
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, BooleanFormula pOtherFormula) {
    SSAMap ssa = pPathFormula.getSsa();
    BooleanFormula otherFormula = fmgr.instantiate(pOtherFormula, ssa);
    BooleanFormula resultFormula = bfmgr.and(pPathFormula.getFormula(), otherFormula);
    final PointerTargetSet pts = pPathFormula.getPointerTargetSet();
    return new PathFormula(resultFormula, ssa, pts, pPathFormula.getLength());
  }

  /**
   * Class representing the result of the operation of merging (disjuncting)
   * additional parts of {@link PathFormula}s beyond the actual formula.
   */
  public static class MergeResult<T> {

    private final BooleanFormula leftConjunct;
    private final BooleanFormula rightConjunct;
    private final BooleanFormula finalConjunct;

    private final T result;

    public MergeResult(
        T pResult, BooleanFormula pLeftConjunct,
        BooleanFormula pRightConjunct, BooleanFormula pFinalConjunct) {
      result = checkNotNull(pResult);
      leftConjunct = checkNotNull(pLeftConjunct);
      rightConjunct = checkNotNull(pRightConjunct);
      finalConjunct = checkNotNull(pFinalConjunct);
    }

    public static <T> MergeResult<T> trivial(T result, BooleanFormulaManagerView bfmgr) {
      BooleanFormula trueFormula = bfmgr.makeBoolean(true);
      return new MergeResult<>(result, trueFormula, trueFormula, trueFormula);
    }

    /**
     * This is a formula that needs to be conjuncted to the left formula
     * before it is used in the disjunction.
     */
    BooleanFormula getLeftConjunct() {
      return leftConjunct;
    }

    /**
     * This is a formula that needs to be conjuncted to the right formula
     * before it is used in the disjunction.
     */
    BooleanFormula getRightConjunct() {
      return rightConjunct;
    }

    /**
     * This is a formula that needs to be conjuncted to the result of the disjunction.
     */
    BooleanFormula getFinalConjunct() {
      return finalConjunct;
    }

    T getResult() {
      return result;
    }
  }

  /**
   * builds a formula that represents the necessary variable assignments
   * to "merge" the two ssa maps. That is, for every variable X that has two
   * different ssa indices i and j in the maps, creates a new formula
   * (X_k = X_i) | (X_k = X_j), where k is a fresh ssa index.
   * Returns the formula described above, plus a new SSAMap that is the merge
   * of the two.
   *
   * @param ssa1 an SSAMap
   * @param pts1 the PointerTargetSet for ssa1
   * @param ssa2 an SSAMap
   * @param pts2 the PointerTargetSet for ssa1
   * @return The new SSAMap and the formulas that need to be added to the path formulas before
   * disjuncting them.
   */
  private MergeResult<SSAMap> mergeSSAMaps(
      final SSAMap ssa1,
      final PointerTargetSet pts1,
      final SSAMap ssa2,
      final PointerTargetSet pts2) throws InterruptedException {
    final List<MapsDifference.Entry<String, Integer>> symbolDifferences = new ArrayList<>();
    final SSAMap resultSSA = SSAMap.merge(ssa1, ssa2, collectMapsDifferenceTo(symbolDifferences));

    BooleanFormula mergeFormula1 = bfmgr.makeBoolean(true);
    BooleanFormula mergeFormula2 = bfmgr.makeBoolean(true);

    for (final MapsDifference.Entry<String, Integer> symbolDifference : symbolDifferences) {
      shutdownNotifier.shutdownIfNecessary();
      final String symbolName = symbolDifference.getKey();
      final CType symbolType = resultSSA.getType(symbolName);
      final int index1 = symbolDifference.getLeftValue().or(1);
      final int index2 = symbolDifference.getRightValue().or(1);

      assert symbolName != null;
      if (index1 > index2 && index1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2
        BooleanFormula mergeFormula = makeSsaMerger(symbolName, symbolType, index2, index1, pts2);

        mergeFormula2 = bfmgr.and(mergeFormula2, mergeFormula);

      } else if (index2 > 1) {
        assert index1 < index2;
        // i1:smaller, i2:bigger
        // => need correction term for i1
        BooleanFormula mergeFormula = makeSsaMerger(symbolName, symbolType, index1, index2, pts1);

        mergeFormula1 = bfmgr.and(mergeFormula1, mergeFormula);
      }
    }

    return new MergeResult<>(resultSSA, mergeFormula1, mergeFormula2, bfmgr.makeBoolean(true));
  }

  /**
   * Create the necessary equivalence terms for adjusting the SSA indices
   * of a given symbol (of any type) from oldIndex to newIndex.
   */
  private BooleanFormula makeSsaMerger(
      final String symbolName, final CType symbolType,
      final int oldIndex, final int newIndex,
      final PointerTargetSet oldPts) throws InterruptedException {
    assert oldIndex > 0;
    assert newIndex > oldIndex;

    // Important note:
    // we need to use fmgr.assignment in these methods,
    // because fmgr.equal has undesired semantics for floating points.

    if (useNondetFlags && symbolName.equals(NONDET_FLAG_VARIABLE)) {
      return makeSsaNondetFlagMerger(oldIndex, newIndex);
    } else if (handleHeapArray && CToFormulaConverterWithHeapArray.isSMTArray(symbolName)) {
      assert symbolName.equals(CToFormulaConverterWithHeapArray.getArrayName(symbolType));
      return makeSsaArrayMerger(symbolName, symbolType, oldIndex, newIndex);
    } else if (CToFormulaConverterWithPointerAliasing.isUF(symbolName)) {
      assert symbolName.equals(CToFormulaConverterWithPointerAliasing.getUFName(symbolType));
      return makeSsaUFMerger(symbolName, symbolType, oldIndex, newIndex, oldPts);
    } else {
      return makeSsaVariableMerger(symbolName, symbolType, oldIndex, newIndex);
    }
  }

  private BooleanFormula makeSsaVariableMerger(
      final String variableName,
      final CType variableType,
      final int oldIndex,
      final int newIndex) {
    assert oldIndex < newIndex;

    // TODO Previously we called makeMerger,
    // which creates the terms (var@oldIndex = var@oldIndex+1; ...; var@oldIndex = var@newIndex).
    // Now we only create a single term (var@oldIndex = var@newIndex).
    // This should not make a difference except maybe for the model,
    // but this could be investigated to be sure.

    final FormulaType<?> variableFormulaType = converter.getFormulaTypeFromCType(variableType);
    final Formula oldVariable = fmgr.makeVariable(variableFormulaType, variableName, oldIndex);
    final Formula newVariable = fmgr.makeVariable(variableFormulaType, variableName, newIndex);

    return fmgr.assignment(newVariable, oldVariable);
  }

  private BooleanFormula makeSsaArrayMerger(
      final String pFunctionName,
      final CType pReturnType,
      final int pOldIndex,
      final int pNewIndex) {
    assert pOldIndex < pNewIndex;
    final FormulaType<?> returnFormulaType = converter.getFormulaTypeFromCType(pReturnType);
    final ArrayFormula<?, ?> newArray =
        afmgr.makeArray(
            pFunctionName + "@" + pNewIndex, FormulaType.IntegerType, returnFormulaType);
    final ArrayFormula<?, ?> oldArray =
        afmgr.makeArray(
            pFunctionName + "@" + pOldIndex, FormulaType.IntegerType, returnFormulaType);
    return fmgr.makeEqual(newArray, oldArray);
  }

  private BooleanFormula makeSsaUFMerger(
      final String functionName,
      final CType returnType,
      final int oldIndex,
      final int newIndex,
      final PointerTargetSet pts) throws InterruptedException {
    assert oldIndex < newIndex;

    final FormulaType<?> returnFormulaType = converter.getFormulaTypeFromCType(returnType);
    BooleanFormula result = bfmgr.makeBoolean(true);
    for (final PointerTarget target : pts.getAllTargets(returnType)) {
      shutdownNotifier.shutdownIfNecessary();
      final Formula targetAddress =
          fmgr.makePlus(fmgr.makeVariable(typeHandler.getPointerType(), target.getBaseName()),
              fmgr.makeNumber(typeHandler.getPointerType(), target.getOffset()));

      final BooleanFormula retention = fmgr.assignment(
          ffmgr.declareAndCallUninterpretedFunction(functionName,
              newIndex,
              returnFormulaType,
              targetAddress),
          ffmgr.declareAndCallUninterpretedFunction(functionName,
              oldIndex,
              returnFormulaType,
              targetAddress));
      result = fmgr.makeAnd(result, retention);
    }

    return result;
  }

  private BooleanFormula makeSsaNondetFlagMerger(int iSmaller, int iBigger) {
    Formula pInitialValue = fmgr.makeNumber(NONDET_FORMULA_TYPE, 0);
    assert iSmaller < iBigger;

    BooleanFormula lResult = bfmgr.makeBoolean(true);
    FormulaType<Formula> type = fmgr.getFormulaType(pInitialValue);

    for (int i = iSmaller + 1; i <= iBigger; ++i) {
      Formula currentVar = fmgr.makeVariable(type, NONDET_FLAG_VARIABLE, i);
      BooleanFormula e = fmgr.assignment(currentVar, pInitialValue);
      lResult = bfmgr.and(lResult, e);
    }

    return lResult;
  }

  private BooleanFormula addMergeAssumptions(
      final BooleanFormula pFormula, final SSAMap ssa1,
      final PointerTargetSet pts1, final SSAMap ssa2) throws InterruptedException {
    final List<MapsDifference.Entry<String, Integer>> symbolDifferences = new ArrayList<>();
    final SSAMap resultSSA = SSAMap.merge(ssa1, ssa2, collectMapsDifferenceTo(symbolDifferences));

    BooleanFormula mergeFormula1 = pFormula;

    for (final MapsDifference.Entry<String, Integer> symbolDifference : symbolDifferences) {
      shutdownNotifier.shutdownIfNecessary();
      final String symbolName = symbolDifference.getKey();
      final CType symbolType = resultSSA.getType(symbolName);
      final int index1 = symbolDifference.getLeftValue().or(1);
      final int index2 = symbolDifference.getRightValue().or(1);

      assert symbolName != null;
      if (index1 > index2 && index1 > 1) {
        return bfmgr.makeBoolean(true);

      } else if (index2 > 1) {
        assert index1 < index2;
        // i1:smaller, i2:bigger
        // => need correction term for i1
        BooleanFormula mergeFormula;

        for (int i = index1; i < index2; i++) {
          mergeFormula = makeSsaMerger(symbolName, symbolType, i, i + 1, pts1);
          mergeFormula1 = bfmgr.and(mergeFormula1, mergeFormula);
        }
      }
    }

    return mergeFormula1;
  }

  @Override
  public PathFormula makeFormulaForPath(List<CFAEdge> pPath)
      throws CPATransferException, InterruptedException {
    PathFormula pathFormula = makeEmptyPathFormula();
    for (CFAEdge edge : pPath) {
      pathFormula = makeAnd(pathFormula, edge);
    }
    return pathFormula;
  }


  /**
   * Build a formula containing a predicate for all branching situations in the
   * ARG. If a satisfying assignment is created for this formula, it can be used
   * to find out which paths in the ARG are feasible.
   *
   * This method may be called with an empty set, in which case it does nothing
   * and returns the formula "true".
   *
   * @param elementsOnPath The ARG states that should be considered.
   * @return A formula containing a predicate for each branching.
   */
  @Override
  public BooleanFormula buildBranchingFormula(Iterable<ARGState> elementsOnPath)
      throws CPATransferException, InterruptedException {
    // build the branching formula that will help us find the real error path
    BooleanFormula branchingFormula = bfmgr.makeBoolean(true);
    for (final ARGState pathElement : elementsOnPath) {

      if (pathElement.getChildren().size() > 1) {
        if (pathElement.getChildren().size() > 2) {
          // can't create branching formula
          if (from(pathElement.getChildren()).anyMatch(AbstractStates.IS_TARGET_STATE)) {
            // We expect this situation of one of the children is a target state created by PredicateCPA.
            continue;
          } else {
            logger.log(Level.WARNING,
                "ARG branching with more than two outgoing edges at ARG node " + pathElement
                    .getStateId() + ".");
            return bfmgr.makeBoolean(true);
          }
        }

        FluentIterable<CFAEdge> outgoingEdges = from(pathElement.getChildren()).transform(
            new Function<ARGState, CFAEdge>() {
              @Override
              public CFAEdge apply(ARGState child) {
                return pathElement.getEdgeToChild(child);
              }
            });
        if (!outgoingEdges.allMatch(Predicates.instanceOf(AssumeEdge.class))) {
          if (from(pathElement.getChildren()).anyMatch(AbstractStates.IS_TARGET_STATE)) {
            // We expect this situation of one of the children is a target state created by PredicateCPA.
            continue;
          } else {
            logger.log(Level.WARNING,
                "ARG branching without AssumeEdge at ARG node " + pathElement.getStateId() + ".");
            return bfmgr.makeBoolean(true);
          }
        }

        assert outgoingEdges.size() == 2;

        // We expect there to be exactly one positive and one negative edge
        AssumeEdge positiveEdge = null;
        AssumeEdge negativeEdge = null;
        for (AssumeEdge currentEdge : outgoingEdges.filter(AssumeEdge.class)) {
          if (currentEdge.getTruthAssumption()) {
            positiveEdge = currentEdge;
          } else {
            negativeEdge = currentEdge;
          }
        }
        if (positiveEdge == null || negativeEdge == null) {
          logger.log(Level.WARNING,
              "Ambiguous ARG branching at ARG node " + pathElement.getStateId() + ".");
          return bfmgr.makeBoolean(true);
        }

        BooleanFormula pred =
            bfmgr.makeVariable(BRANCHING_PREDICATE_NAME + pathElement.getStateId());

        // create formula by edge, be sure to use the correct SSA indices!
        // TODO the class PathFormulaManagerImpl should not depend on PredicateAbstractState,
        // it is used without PredicateCPA as well.
        PathFormula pf;
        PredicateAbstractState pe =
            AbstractStates.extractStateByType(pathElement, PredicateAbstractState.class);
        if (pe == null) {
          logger.log(Level.WARNING,
              "Cannot find precise error path information without PredicateCPA");
          return bfmgr.makeBoolean(true);
        } else {
          pf = pe.getPathFormula();
        }
        pf = this.makeEmptyPathFormula(pf); // reset everything except SSAMap
        pf = this.makeAnd(pf, positiveEdge);        // conjunct with edge

        BooleanFormula equiv = bfmgr.equivalence(pred, pf.getFormula());
        branchingFormula = bfmgr.and(branchingFormula, equiv);
      }
    }
    return branchingFormula;
  }

  /**
   * Extract the information about the branching predicates created by
   * {@link #buildBranchingFormula(Iterable)} from a satisfying assignment.
   *
   * A map is created that stores for each ARGState (using its element id as
   * the map key) which edge was taken (the positive or the negated one).
   *
   * @param model A satisfying assignment that should contain values for branching predicates.
   * @return A map from ARG state id to a boolean value indicating direction.
   */
  @Override
  public Map<Integer, Boolean> getBranchingPredicateValuesFromModel(Iterable<ValueAssignment> model) {
    if (!model.iterator().hasNext()) {
      logger.log(Level.WARNING, "No satisfying assignment given by solver!");
      return Collections.emptyMap();
    }

    Map<Integer, Boolean> preds = Maps.newHashMap();
    for (ValueAssignment entry : model) {
      String canonicalName = entry.getName();

      if (fmgr.getFormulaType(entry.getKey()).isBooleanType()) {
        String name = BRANCHING_PREDICATE_NAME_PATTERN.matcher(canonicalName).replaceFirst("");
        if (!name.equals(canonicalName)) {
          // pattern matched, so it's a variable with __ART__ in it

          // no NumberFormatException because of RegExp match earlier
          Integer nodeId = Integer.parseInt(name);

          assert !preds.containsKey(nodeId);

          preds.put(nodeId, (Boolean) entry.getValue());
        }
      }
    }
    return preds;
  }

  @Override
  public Formula expressionToFormula(
      PathFormula pFormula,
      CIdExpression expr,
      CFAEdge edge) throws UnrecognizedCCodeException {
    return converter.buildTermFromPathFormula(pFormula, expr, edge);
  }

  @Override
  public BooleanFormula buildImplicationTestAsUnsat(PathFormula pF1, PathFormula pF2)
      throws InterruptedException {
    BooleanFormula bF = pF2.getFormula();
    bF = bfmgr.not(bF);
    bF = bfmgr.and(addMergeAssumptions(pF1.getFormula(), pF1.getSsa(), pF1.getPointerTargetSet(),
        pF2.getSsa()), bF);

    return bF;
  }

}
