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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTKnRFunctionDeclarator;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.bugfix.FixProvider;
import org.sosy_lab.cpachecker.core.bugfix.FixProvider.BugCategory;
import org.sosy_lab.cpachecker.core.bugfix.MutableASTForFix;
import org.sosy_lab.cpachecker.core.bugfix.SimpleFileLocation;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFix;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFixInfo;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerTypeConstraint;
import org.sosy_lab.cpachecker.core.phase.CPAPhase;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.range.util.Ranges;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

@Options(prefix = "phase.repair.integer")
public class IntegerFixApplicationPhase extends CPAPhase {

  @Option(secure = true, name = "suffix", description = "suffix for the original .c file")
  private String suffixFixed = "backup";

  @Option(secure = true, description = "whether the program repair runs under benchmark mode")
  private boolean forBenchmark = false;

  private static final String renamePrefix = "_";
  // [1] signedness of input value
  // [2] the sanitized type of input value
  private static final String checkTemplate = "pti_%s_%s";

  private static final String parameterElevateTemplate = "%s %s = %s;\n";
  private static final String variableElevateTemplate = "%s %s;\n";
  private static final String callTemplate = "%s(%s)";
  private static final String convertPattern = "(%s) %s";
  private static final String castPattern = "(%s)";

  private static String libraryDeclaration = null;

  private FixCounter fixCounter = new FixCounter();

  private MachineModel machineModel;

  public IntegerFixApplicationPhase(
      String pIdentifier,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      ShutdownNotifier pShutdownNotifier,
      MainStatistics pStats)
      throws InvalidConfigurationException {

    super(pIdentifier, pConfig, pLogger, pShutdownManager, pShutdownNotifier, pStats);
    config.inject(this);
    // initialize machine model
    CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().orNull();
    if (cfaInfo == null) {
      throw new InvalidConfigurationException("CFA information required for analysis");
    }
    machineModel = cfaInfo.getCFA().getMachineModel();
  }

  @Override
  protected CPAPhaseStatus prevAction() throws Exception {
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus postAction() throws Exception {
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus runPhase() throws Exception {
    IntegerFixInfo info = (IntegerFixInfo) FixProvider.getFixInfo(BugCategory.INTEGER);
    if (info == null) {
      // fix information must exist (though could be empty)
      return CPAPhaseStatus.FAIL;
    }
    Multimap<FileLocation, IntegerFix> loc2Fix = info.getLoc2Fix();
    Multimap<String, FileLocation> file2Loc = HashMultimap.create();
    for (FileLocation loc : loc2Fix.keySet()) {
      file2Loc.put(loc.getFileName(), loc);
    }
    // we visit each file containing generated fixes and then apply fixes sequentially
    for (String fileName : file2Loc.keySet()) {
      Collection<FileLocation> locations = file2Loc.get(fileName);
      runFix(fileName, locations, loc2Fix);
    }
    System.out.println("FIXES: " + fixCounter.getTotal());
    System.out.println("CAST: " + fixCounter.castFix);
    System.out.println("CHECK: " + fixCounter.checkFix);
    System.out.println("SPEC: " + fixCounter.specFix);
    System.out.println("CRITICAL: " + fixCounter.getFixedCriticalSites());
    // for benchmark evaluation
    if (forBenchmark) {
      System.out.println("ON_GOOD: " + fixCounter.fixOnGood);
      System.out.println("ON_BAD: " + fixCounter.fixOnBad);
      System.out.println("GOOD_FUNC: " + fixCounter.getFixedGoodFunctionSize());
      System.out.println("BAD_FUNC: " + fixCounter.getFixedBadFunctionSize());
    }
    return CPAPhaseStatus.SUCCESS;
  }

  /**
   * Apply fixes to the specified program file.
   *
   * @param fileName program file
   */
  private void runFix(
      String fileName, Collection<FileLocation> locations, Multimap<FileLocation, IntegerFix>
      loc2Fix) throws Exception {
    // STEP 1: check if the specified file exists
    File programFile = new File(fileName);
    if (!programFile.exists()) {
      logger.log(Level.SEVERE, "Cannot locate the program file: " + fileName);
      return;
    }
    // STEP 2: load the translation unit and create a mutable AST for code refactoring
    FileContent content = FileContent.createForExternalFileLocation(fileName);
    IScannerInfo scanner = new ScannerInfo();
    IParserLogService logService = new DefaultLogService();
    IncludeFileContentProvider includeProvider = IncludeFileContentProvider.getEmptyFilesProvider();
    IASTTranslationUnit unit = GCCLanguage.getDefault().getASTTranslationUnit(content, scanner,
        includeProvider, null, GCCLanguage.OPTION_IS_SOURCE_UNIT, logService);
    MutableASTForFix ast = MutableASTForFix.createMutableASTFromTranslationUnit(unit);
    // STEP 3: build the relation between file location and AST node
    // Note: it is possible that multiple AST nodes have the same file location. For example, in
    // most cases an identifier and its AST name share the same location. Generally, we only keep
    // the AST node of the highest level in the mapping.
    Map<FileLocation, MutableASTForFix> loc2Ast = new HashMap<>();
    createMapFromLocationToASTNode(ast, loc2Ast, locations);
    // STEP 4: triage fixes
    Map<FileLocation, IntegerFix> castFix = new HashMap<>();
    Map<FileLocation, IntegerFix> specFix = new HashMap<>();
    Map<FileLocation, IntegerFix> convFix = new HashMap<>();
    for (FileLocation loc : locations) {
      Collection<IntegerFix> fixes = loc2Fix.get(loc);
      for (IntegerFix singleFix : fixes) {
        switch (singleFix.getFixMode()) {
          case CAST:
            castFix.put(loc, singleFix);
            break;
          case CHECK_CONV:
            convFix.put(loc, singleFix);
            break;
          case SPECIFIER:
            specFix.put(loc, singleFix);
        }
      }
    }
    // STEP 5: apply fixes
    // fixes are applied by priority of fix kind: CAST > CHECK_ARITH > CHECK_CONV > SPECIFIER
    Iterator<Entry<FileLocation, IntegerFix>> iterator = Iterators.concat(castFix.entrySet()
        .iterator(), convFix.entrySet().iterator(), specFix.entrySet().iterator());
    // expressions the types of which change after applying (cast) fixes
    Map<MutableASTForFix, CSimpleType> newCasts = new HashMap<>();
    // variables the type of which change after applying (specifier) fixes
    Map<String, CSimpleType> newDecls = new HashMap<>();
    IntegerFixInfo intInfo = (IntegerFixInfo) FixProvider.getFixInfo(BugCategory.INTEGER);
    assert (intInfo != null);
    for (Entry<FileLocation, IntegerFix> entry : specFix.entrySet()) {
      FileLocation loc = entry.getKey();
      String name = intInfo.getQualifiedName(loc);
      if (name != null) {
        CSimpleType targetType = entry.getValue().getTargetType();
        newDecls.put(name, targetType);
      }
    }
    boolean isChanged = false;
    while (iterator.hasNext()) {
      Entry<FileLocation, IntegerFix> entry = iterator.next();
      FileLocation location = entry.getKey();
      MutableASTForFix astNode = loc2Ast.get(location);
      if (astNode != null) {
        long oldCastFix = fixCounter.castFix;
        long oldCheckFix = fixCounter.checkFix;
        long oldSpecFix = fixCounter.specFix;
        applyFix(astNode, entry.getValue(), newCasts, newDecls);
        if (oldCastFix != fixCounter.castFix || oldCheckFix != fixCounter.checkFix || oldSpecFix
            != fixCounter.specFix) {
          isChanged = true;
        }
      }
    }
    // STEP 6: write back fixes into .i file
    if (isChanged) {
      String newFileName = checkNotNull(backUpFileNameFunction.apply(fileName));
      File backupFile = new File(newFileName);
      Files.copy(programFile, backupFile);
      // overwrite the existing program file
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
      String fixedCode = ast.synthesize();
      writer.write(getLibraryDeclarations());
      writer.newLine();
      writer.newLine();
      writer.write(fixedCode);
      writer.flush();
      writer.close();
    }
  }

  private String getLibraryDeclarations() {
    if (libraryDeclaration == null) {
      List<String> declarations = new ArrayList<>();
      // the template of declaration contains three fields: (1) return type, (2) function name,
      // (3) type of input value
      String template = "extern %s %s(%s x);";
      CSimpleType[] intTypes = {CNumericTypes.CHAR, CNumericTypes.SIGNED_CHAR, CNumericTypes
          .UNSIGNED_CHAR, CNumericTypes.SHORT_INT, CNumericTypes.UNSIGNED_SHORT_INT,
          CNumericTypes.INT, CNumericTypes.UNSIGNED_INT, CNumericTypes.LONG_INT, CNumericTypes
          .UNSIGNED_LONG_INT, CNumericTypes.LONG_LONG_INT, CNumericTypes.UNSIGNED_LONG_LONG_INT};
      String[] signs = {"s", "u"};
      for (CSimpleType intType : intTypes) {
        String returnType = intType.toString();
        String functionName;
        String paramType;
        for (String sign : signs) {
          if (sign.equals("s")) {
            paramType = "long long int";
          } else {
            paramType = "long long unsigned int";
          }
          functionName = String.format(checkTemplate, sign, IntegerTypeConstraint.toMethodString
              (intType));
          declarations.add(String.format(template, returnType, functionName, paramType));
        }
      }
      // moreover, we add safe arithmetic functions here
      String arithTemplate = "extern %s %s(%s x, %s y, int mode);";
      String operation[] = { "add", "minus", "multiply" };
      for (String op : operation) {
        for (CSimpleType intType : intTypes) {
          if (machineModel.needPromotion(intType)) {
            continue;
          }
          String typeName = IntegerTypeConstraint.toMethodString(intType);
          String functionName = String.format(checkTemplate, op, typeName);
          declarations.add(String.format(arithTemplate, intType.toString(), functionName, "long long int",
              "long long int"));
        }
      }
      // combine declarations into a text segment
      libraryDeclaration = Joiner.on("\n\n").join(declarations);
    }
    return libraryDeclaration;
  }

  private void applyFix(
      MutableASTForFix pAstNode, IntegerFix pFix, Map<MutableASTForFix,
      CSimpleType> pNewCasts, Map<String, CSimpleType> pNewDecls) {
    CSimpleType newType = pFix.getTargetType();
    if (newType == null || newType.getType() == CBasicType.BOOL) {
      // it is unnecessary to sanitize boolean value
      // this is because, boolean value is false iff the value assigned is zero
      // and zero is always in the legal range of integer type
      return;
    }
    // skip useless brackets
    pAstNode = skipPrimaryBrackets(pAstNode);
    switch (pFix.getFixMode()) {
      case SPECIFIER: {
        // 2 cases:
        // (1) variable declaration: we have the declarator
        // (2) function parameter declaration:
        // 2.1 K&R style: we have declarator
        // 2.2 ANSI style: we have parameter declaration
        // For variable declaration, we only need to change the declared type. For parameter
        // declaration, we need to rename the parameter name and add a cast statement at the top
        // of function body.
        IASTNode node = pAstNode.getWrappedNode();
        if (node instanceof IASTDeclarator) {
          MutableASTForFix possibleDeclarationStmt = pAstNode.getParent(2);
          if (possibleDeclarationStmt != null) {
            IASTNode wrappedNode = possibleDeclarationStmt.getWrappedNode();
            if (wrappedNode instanceof IASTDeclarationStatement) {
              // case 1:
              // * refactor operation is trivial if there is only one declarator
              // * if there are multiple declarators, we clean the target declarator and append the
              // new declarator in the last template of declaration statement
              MutableASTForFix declaration = pAstNode.getParent(1);
              assert (declaration != null);
              IASTNode declarationNode = declaration.getWrappedNode();
              if (declarationNode instanceof IASTSimpleDeclaration) {
                List<MutableASTForFix> children = declaration.getChildren();
                int childrenSize = children.size();
                int declaratorSize = ((IASTSimpleDeclaration) declarationNode)
                    .getDeclarators().length;
                if (declaratorSize == 1) {
                  // trivial
                  MutableASTForFix specifierNode = children.get(0);
                  MutableASTForFix specifierLeaf = specifierNode.getOnlyLeaf();
                  String newTypeStr = updateTypeString(specifierLeaf.synthesize(), newType);
                  specifierLeaf.writeToLeaf(newTypeStr);
                } else if (declaratorSize > 1) {
                  // non-trivial
                  // which declarator should we refactor?
                  int hit = 0;
                  for (MutableASTForFix childOfDeclaration : children) {
                    if (childOfDeclaration.getWrappedNode() == node) {
                      break;
                    }
                    hit++;
                  }
                  assert (hit < childrenSize);
                  MutableASTForFix targetDeclarator = children.get(hit);
                  String newDeclaration = String.format(variableElevateTemplate, newType
                      .toString(), targetDeclarator.synthesize());
                  if (hit == childrenSize - 1) {
                    // the last one
                    targetDeclarator.cleanText();
                    targetDeclarator.setPrecedentText("; ");
                    targetDeclarator.setSuccessorText(newDeclaration);
                  } else {
                    // not the last one
                    targetDeclarator.cleanText();
                    targetDeclarator.setSuccessorText("");
                    MutableASTForFix lastDeclarator = children.get(childrenSize - 1);
                    String successorText = lastDeclarator.getSuccessorText();
                    if (successorText.charAt(successorText.length() - 1) == '\n') {
                      successorText = successorText.substring(0, successorText.length() - 1);
                    }
                    successorText = successorText.concat(" ").concat(newDeclaration);
                    lastDeclarator.setSuccessorText(successorText);
                  }
                }
                fixCounter.specInc(forBenchmark, pAstNode);
              }
            } else if (wrappedNode instanceof CASTKnRFunctionDeclarator) {
              // case 2.1
              MutableASTForFix leaf = pAstNode.getOnlyLeaf();
              String oldName = leaf.synthesize();
              String newName = renamePrefix.concat(oldName);
              leaf.writeToLeaf(newName);
              // update parameter name consistently
              boolean foundParamName = false;
              List<MutableASTForFix> stmtChildren = possibleDeclarationStmt.getChildren();
              for (int i = 1; i < stmtChildren.size(); i++) {
                MutableASTForFix possibleName = stmtChildren.get(i);
                IASTNode wrappedName = possibleName.getWrappedNode();
                if (wrappedName instanceof IASTName) {
                  String paramName = possibleName.synthesize();
                  if (oldName.equals(paramName)) {
                    MutableASTForFix leafForName = possibleName.getOnlyLeaf();
                    leafForName.writeToLeaf(newName);
                    foundParamName = true;
                    break;
                  }
                }
              }
              if (!foundParamName) {
                throw new IllegalArgumentException("Inconsistent parameter name in KnR function "
                    + "definition");
              }
              // insert a new declaration for the elevated parameter variable
              MutableASTForFix possibleFuncDef = possibleDeclarationStmt.getParent(1);
              if (possibleFuncDef == null) {
                throw new IllegalArgumentException("Function definition not found");
              }
              List<MutableASTForFix> funcChildren = possibleFuncDef.getChildren();
              MutableASTForFix funcBody = funcChildren.get(funcChildren.size() - 1);
              String marginalText = funcBody.getMarginalText();
              if (marginalText.isEmpty()) {
                throw new IllegalArgumentException("Missing function body");
              }
              int endPos = 1;
              while (endPos < marginalText.length()) {
                if (!Character.isWhitespace(marginalText.charAt(endPos))) {
                  break;
                }
                endPos++;
              }
              String spacePrefix = marginalText.substring(1, endPos);

              String paramElevate = String.format(parameterElevateTemplate, newType.toString(),
                  oldName, newName);
              marginalText = insertString(marginalText, spacePrefix.concat(paramElevate), 1);
              funcBody.writeToMarginalText(marginalText);
              fixCounter.specInc(forBenchmark, pAstNode);
            }
          }
        } else if (node instanceof IASTParameterDeclaration) {
          // case 2.2
          List<MutableASTForFix> children = pAstNode.getChildren();
          if (children.size() < 2) {
            throw new IllegalArgumentException("At least 2 ast nodes required for parameter "
                + "declaration");
          }
          MutableASTForFix declarator = children.get(children.size() - 1);
          MutableASTForFix leaf = declarator.getOnlyLeaf();
          String oldName = leaf.synthesize();
          String newName = renamePrefix.concat(oldName);
          leaf.writeToLeaf(newName);
          // add a new assignment at the top of function body
          MutableASTForFix possibleFuncDef = pAstNode.getParent(2);
          if (possibleFuncDef == null) {
            throw new IllegalArgumentException("Function definition not found");
          }
          List<MutableASTForFix> funcChildren = possibleFuncDef.getChildren();
          MutableASTForFix funcBody = funcChildren.get(funcChildren.size() - 1);
          String marginalText = funcBody.getMarginalText();
          if (marginalText.isEmpty()) {
            throw new IllegalArgumentException("Missing function body");
          }
          // collect empty characters
          int endPos = 1;
          while (endPos < marginalText.length()) {
            if (!Character.isWhitespace(marginalText.charAt(endPos))) {
              break;
            }
            endPos++;
          }
          String spacePrefix = marginalText.substring(1, endPos);
          String paramElevate = String.format(parameterElevateTemplate, newType.toString(),
              oldName, newName);
          marginalText = insertString(marginalText, spacePrefix.concat(paramElevate), 1);
          funcBody.writeToMarginalText(marginalText);
          fixCounter.specInc(forBenchmark, pAstNode);
        }
        break;
      }
      case CHECK_CONV: {
        // derive the real type (i.e. the type after applying fixes) of target AST node
        CSimpleType type = deriveTypeForASTNode(machineModel, pAstNode, pNewCasts, pNewDecls);
        if (type != null) {
          type = type.getCanonicalType();
          // check if the current AST is the array subscript
          if (checkIfArraySubscript(pAstNode)) {
            Range oldTypeRange = Ranges.getTypeRange(type, machineModel);
            Range newTypeRange = Ranges.getTypeRange(newType, machineModel);
            if (newTypeRange.contains(oldTypeRange) && !newTypeRange.equals(oldTypeRange)) {
              addArithmeticCheck(pAstNode, type, pNewCasts, pNewDecls);
            }
          } else {
            if (!Types.isIntegralType(newType)) {
              break;
            }
            // check if sanitization is necessary
            Range oldTypeRange = Ranges.getTypeRange(type, machineModel);
            Range newTypeRange = Ranges.getTypeRange(newType, machineModel);
            if (newTypeRange.equals(oldTypeRange)) {
              // then we need to further check if the sanitized expression contains binary/negate
              // operation
              addArithmeticCheck(pAstNode, type, pNewCasts, pNewDecls);
            } else if (newTypeRange.contains(oldTypeRange)) {
              break;
            } else {
              // ensure that the inner operation does not overflow
              addArithmeticCheck(pAstNode, type, pNewCasts, pNewDecls);
              addSanityCheck(pAstNode, type, newType);
            }
          }
        }
        break;
      }
      case CAST: {
        // derive the cast operand after fix application
        CSimpleType type = deriveTypeForASTNode(machineModel, pAstNode, pNewCasts, pNewDecls);
        boolean shouldIgnore = false;
        if (type != null) {
          shouldIgnore = Types.canHoldAllValues(type, newType, machineModel);
        }
        // check if the target expression is the operand of a cast expression
        // Note: this case occurs only when the flag "treat truncation as error" is ON
        MutableASTForFix parentNode = pAstNode.getParent(1);
        if (parentNode != null) {
          IASTNode wrappedParent = parentNode.getWrappedNode();
          if (wrappedParent instanceof IASTCastExpression) {
            // we just modify the type-id of the existing cast expression here
            MutableASTForFix typeIdNode = parentNode.getChildren().get(0);
            if (shouldIgnore) {
              typeIdNode.cleanText();
              typeIdNode.setPrecedentText("");
              typeIdNode.setSuccessorText("");
              pNewCasts.put(pAstNode, type);
            } else {
              typeIdNode.cleanText();
              typeIdNode.writeToMarginalText(newType.toString());
              pNewCasts.put(pAstNode, newType);
            }
            fixCounter.castInc(forBenchmark, pAstNode);
            break;
          }
        }
        if (!shouldIgnore) {
          if (pAstNode.isLeaf()) {
            String oldContent = pAstNode.synthesize();
            String withCast = String.format(convertPattern, newType.toString(), oldContent);
            pAstNode.writeToLeaf(withCast);
          } else {
            String firstCode = pAstNode.getMarginalText();
            String finalCode = pAstNode.getTailText();
            firstCode = insertString(firstCode, String.format(castPattern, newType.toString())
                .concat("("), 0);
            finalCode = insertString(finalCode, ")", finalCode.length());
            pAstNode.writeToMarginalText(firstCode);
            pAstNode.writeToTailText(finalCode);
          }
          pNewCasts.put(pAstNode, newType);
          fixCounter.castInc(forBenchmark, pAstNode);
        }
        break;
      }
      default:
        logger.log(Level.WARNING, "illegal fix mode: " + pFix.getFixMode());
    }
  }

  static boolean checkIfArraySubscript(MutableASTForFix pASTNode) {
    IASTNode wrapped = pASTNode.getWrappedNode();
    IASTNode parent = wrapped.getParent();
    if (parent != null && parent instanceof IASTArraySubscriptExpression) {
      IASTNode argument = ((IASTArraySubscriptExpression) parent).getArgument();
      return argument == wrapped;
    }
    return false;
  }

  private void addArithmeticCheck(MutableASTForFix pASTNode, CSimpleType pNewType,
                                  Map<MutableASTForFix, CSimpleType> pNewCasts,
                                  Map<String, CSimpleType> pNewDecls) {
    if (machineModel.needPromotion(pNewType)) {
      // binary expression has integer-promotion principle
      return;
    }
    IASTNode wrappedNode = pASTNode.getWrappedNode();
    if (wrappedNode instanceof IASTUnaryExpression) {
      int unaryOperator = ((IASTUnaryExpression) wrappedNode).getOperator();
      if (unaryOperator == IASTUnaryExpression.op_bracketedPrimary) {
        List<MutableASTForFix> children = pASTNode.getChildren();
        if (children.size() == 1) {
          addArithmeticCheck(children.get(0), pNewType, pNewCasts, pNewDecls);
        }
      }
    } else if (wrappedNode instanceof IASTBinaryExpression) {
      int binaryOperator = ((IASTBinaryExpression) wrappedNode).getOperator();
      List<MutableASTForFix> children = pASTNode.getChildren();
      if (children.size() == 2) {
        MutableASTForFix op1 = children.get(0);
        MutableASTForFix op2 = children.get(1);
        CSimpleType t1 = deriveTypeForASTNode(machineModel, op1, pNewCasts, pNewDecls);
        CSimpleType t2 = deriveTypeForASTNode(machineModel, op2, pNewCasts, pNewDecls);
        String checkName = "";
        String typeString = checkNotNull(IntegerTypeConstraint.toMethodString(pNewType));
        switch (binaryOperator) {
          case IASTBinaryExpression.op_plus:
            checkName = String.format(checkTemplate, "add", typeString);
            break;
          case IASTBinaryExpression.op_minus:
            checkName = String.format(checkTemplate, "minus", typeString);
            break;
          case IASTBinaryExpression.op_multiply:
            checkName = String.format(checkTemplate, "multiply", typeString);
        }
        if (!checkName.isEmpty() && t1 != null && t2 != null) {
          int signedMode = (t1.isGeneralSigned() ? 2 : 0) + (t2.isGeneralSigned() ? 1 : 0);
          String firstCode = pASTNode.getMarginalText();
          String finalCode = pASTNode.getTailText();
          firstCode = insertString(firstCode, checkName.concat("("), 0);
          finalCode = insertString(finalCode, ", " + String.valueOf(signedMode) + ")", finalCode
              .length());
          op1.setSuccessorText(", ");
          pASTNode.writeToMarginalText(firstCode);
          pASTNode.writeToTailText(finalCode);
          fixCounter.checkInc(forBenchmark, pASTNode);
        }
      }
    }
  }

  /**
   * Add sanity check enclosing the given AST node.
   *
   * @param pAstNode the AST node
   * @param oldType the old type of AST node (though it can be derived given the AST node)
   * @param newType the target type
   */
  private void addSanityCheck(MutableASTForFix pAstNode, CSimpleType oldType, CSimpleType newType) {
    String checkName = String.format(checkTemplate,
        machineModel.isSigned(oldType.getCanonicalType()) ? "s" : "u",
        checkNotNull(IntegerTypeConstraint.toMethodString(newType)));
    if (pAstNode.isLeaf()) {
      String oldContent = pAstNode.synthesize();
      String withCheck = String.format(callTemplate, checkName, oldContent);
      pAstNode.writeToLeaf(withCheck);
    } else {
      String firstCode = pAstNode.getMarginalText();
      String finalCode = pAstNode.getTailText();
      firstCode = insertString(firstCode, checkName.concat("("), 0);
      finalCode = insertString(finalCode, ")", finalCode.length());
      pAstNode.writeToMarginalText(firstCode);
      pAstNode.writeToTailText(finalCode);
    }
    fixCounter.checkInc(forBenchmark, pAstNode);
  }

  static MutableASTForFix skipPrimaryBrackets(MutableASTForFix pASTNode) {
    MutableASTForFix parentNode = pASTNode.getParent(1);
    MutableASTForFix currentNode = pASTNode;
    while (parentNode != null) {
      IASTNode wrappedParent = parentNode.getWrappedNode();
      if (wrappedParent instanceof IASTUnaryExpression) {
        int operator = ((IASTUnaryExpression) wrappedParent).getOperator();
        if (operator != IASTUnaryExpression.op_bracketedPrimary) {
          break;
        }
        currentNode = parentNode;
        parentNode = currentNode.getParent(1);
      } else {
        break;
      }
    }
    return currentNode;
  }

  private static String getContainingFunctionName(MutableASTForFix pASTNode) {
    IASTNode currentNode = pASTNode.getWrappedNode();
    IASTNode parentNode = currentNode.getParent();
    while (parentNode != null) {
      if (parentNode instanceof IASTFunctionDefinition) {
        return ((IASTFunctionDefinition) parentNode).getDeclarator().getName().toString();
      }
      currentNode = parentNode;
      parentNode = currentNode.getParent();
    }
    // if we reach here, the specified AST node is probably not belong to any function scope
    return "";
  }

  @Nullable
  static CSimpleType deriveTypeForASTNode(
      MachineModel pModel, MutableASTForFix pNode, Map<MutableASTForFix, CSimpleType> pNewCasts,
      Map<String, CSimpleType> pNewDecls) {
    IASTNode wrappedNode = pNode.getWrappedNode();
    if (wrappedNode instanceof IASTExpression) {
      DeriveASTTypeVisitor visitor = new DeriveASTTypeVisitor(pModel, pNewCasts, pNewDecls);
      CSimpleType type = visitor.visit(pNode);
      if (type != null) {
        return type;
      }
    }
    return null;
  }

  private final Function<String, String> backUpFileNameFunction = new Function<String, String>() {
    @Override
    public String apply(String pFileName) {
      int dotPos = pFileName.lastIndexOf('.');
      String baseName;
      if (dotPos == -1) {
        baseName = pFileName;
      } else {
        baseName = pFileName.substring(0, dotPos);
      }
      baseName = baseName.concat("_").concat(suffixFixed);
      // append extension name if exists
      String extName = pFileName.substring(dotPos);
      return baseName.concat(extName);
    }
  };

  /**
   * Update type string with the specified new type.
   */
  static String updateTypeString(String oldTypeString, CSimpleType newType) {
    Iterable<String> words = Splitter.on(' ').omitEmptyStrings().split(oldTypeString);
    List<String> newWords = new ArrayList<>();
    for (String word : words) {
      switch (word) {
        case "auto":
        case "extern":
        case "register":
        case "const":
        case "volatile":
        case "static":
        case "typedef":
        case "inline":
        case "restrict":
          newWords.add(word);
          break;
        default:
      }
    }
    newWords.add(newType.toString());
    return Joiner.on(' ').join(newWords);
  }

  /**
   * Insert new content to the specified position of the base content. The index of the first
   * character of the new content is on the `pos`-th position.
   */
  static String insertString(String baseStr, String newStr, int pos) {
    if (pos < 0) {
      pos = 0;
    } else if (pos > baseStr.length()) {
      pos = baseStr.length();
    }
    return baseStr.substring(0, pos) +
        newStr +
        baseStr.substring(pos);
  }

  static void createMapFromLocationToASTNode(
      MutableASTForFix astNode, Map<FileLocation,
      MutableASTForFix> loc2Ast, Collection<FileLocation> locations) {
    Map<SimpleFileLocation, FileLocation> targets = new HashMap<>();
    for (FileLocation location : locations) {
      targets.put(SimpleFileLocation.from(location), location);
    }
    createMapFromLocationToAstNode0(astNode, loc2Ast, targets);
  }

  private static void createMapFromLocationToAstNode0(
      MutableASTForFix astNode, Map<FileLocation,
      MutableASTForFix> loc2Ast, Map<SimpleFileLocation, FileLocation> locationMap) {
    if (locationMap.isEmpty()) {
      return;
    }
    IASTNode currentNode = astNode.getWrappedNode();
    IASTFileLocation currentLoc = currentNode.getFileLocation();
    SimpleFileLocation simpleLoc = SimpleFileLocation.from(currentLoc);
    if (locationMap.containsKey(simpleLoc)) {
      FileLocation normalLoc = locationMap.get(simpleLoc);
      locationMap.remove(simpleLoc);
      loc2Ast.put(normalLoc, astNode);
    }
    for (MutableASTForFix child : astNode.getChildren()) {
      createMapFromLocationToAstNode0(child, loc2Ast, locationMap);
    }
  }

  static class FixCounter {

    private long checkFix = 0;
    private long castFix = 0;
    private long specFix = 0;

    // the following counters are designed for benchmark evaluation only
    private long fixOnGood = 0;
    private long fixOnBad = 0;

    private Set<String> goodFuncSet = new HashSet<>();
    private Set<String> badFuncSet = new HashSet<>();

    private Set<IASTNode> criticalSites = new HashSet<>();

    void checkInc(boolean pForBenchmark, MutableASTForFix pASTNode) {
      checkFix += 1;
      triageOnFunction(pForBenchmark, getContainingFunctionName(pASTNode));
      collectFixOnCriticalSite(pASTNode.getWrappedNode());
    }

    void castInc(boolean pForBenchmark, MutableASTForFix pASTNode) {
      castFix += 1;
      triageOnFunction(pForBenchmark, getContainingFunctionName(pASTNode));
      // check whether the AST node is on the critical site
      collectFixOnCriticalSite(pASTNode.getWrappedNode());
    }

    void castDec(boolean pForBenchmark, MutableASTForFix pASTNode) {
      castFix -= 1;
      if (pForBenchmark) {
        String funcName = getContainingFunctionName(pASTNode);
        if (funcName.contains("good")) {
          fixOnGood -= 1;
        } else if (funcName.contains("bad")) {
          fixOnBad -= 1;
        }
      }
    }

    void specInc(boolean pForBenchmark, MutableASTForFix pASTNode) {
      specFix += 1;
      triageOnFunction(pForBenchmark, getContainingFunctionName(pASTNode));
    }

    long getTotal() {
      return checkFix + castFix + specFix;
    }

    long getFixedCriticalSites() {
      return criticalSites.size();
    }

    long getTotalChecks() {
      return checkFix;
    }

    long getTotalCasts() {
      return castFix;
    }

    long getTotalSpecs() {
      return specFix;
    }

    int getFixedGoodFunctionSize() {
      return goodFuncSet.size();
    }

    int getFixedBadFunctionSize() {
      return badFuncSet.size();
    }

    private void triageOnFunction(boolean pForBenchmark, String pFunctionName) {
      if (pForBenchmark) {
        if (pFunctionName.contains("good")) {
          fixOnGood += 1;
          goodFuncSet.add(pFunctionName);
        } else if (pFunctionName.contains("bad")) {
          fixOnBad += 1;
          badFuncSet.add(pFunctionName);
        }
      }
    }

    private void collectFixOnCriticalSite(IASTNode pASTNode) {
      // 4 critical sites:
      // (1) condition,
      // (2) library call argument,
      // (3) array index,
      // (4) return expression.
      if (pASTNode == null) {
        return;
      }
      IASTNode current = pASTNode;
      IASTNode parent = pASTNode.getParent();
      while (parent != null) {
        // case 1
        if (parent instanceof IASTIfStatement) {
          if (((IASTIfStatement) parent).getConditionExpression() == current) {
            // then current node is critical
            criticalSites.add(current);
          }
          return;
        } else if (parent instanceof IASTWhileStatement) {
          if (((IASTWhileStatement) parent).getCondition() == current) {
            criticalSites.add(current);
          }
          return;
        } else if (parent instanceof IASTDoStatement) {
          if (((IASTDoStatement) parent).getCondition() == current) {
            criticalSites.add(current);
          }
          return;
        } else if (parent instanceof IASTForStatement) {
          if (((IASTForStatement) parent).getConditionExpression() == current) {
            criticalSites.add(current);
          }
          return;
        }
        // case 2
        else if (parent instanceof IASTFunctionCallExpression) {
          IASTInitializerClause[] arguments = ((IASTFunctionCallExpression) parent).getArguments();
          for (IASTInitializerClause argument : arguments) {
            if (current == argument) {
              criticalSites.add(current);
            }
          }
          return;
        }
        // case 3
        else if (parent instanceof IASTArraySubscriptExpression) {
          if (((IASTArraySubscriptExpression) parent).getArgument() == current) {
            criticalSites.add(current);
          }
          return;
        }
        // case 4:
        else if (parent instanceof IASTReturnStatement) {
          if (((IASTReturnStatement) parent).getReturnArgument() == current) {
            criticalSites.add(current);
          }
          return;
        }
        // other cases: we just escalate the AST level
        current = parent;
        parent = current.getParent();
      }
    }

  }
}
