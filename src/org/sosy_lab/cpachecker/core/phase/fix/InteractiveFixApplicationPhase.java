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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
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
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.bugfix.FixProvider;
import org.sosy_lab.cpachecker.core.bugfix.FixProvider.BugCategory;
import org.sosy_lab.cpachecker.core.bugfix.MutableASTForFix;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFix;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFix.IntegerFixMode;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFixInfo;
import org.sosy_lab.cpachecker.core.phase.CPAPhase;
import org.sosy_lab.cpachecker.core.phase.fix.IntegerFixApplicationPhase.FixCounter;
import org.sosy_lab.cpachecker.core.phase.fix.util.IntegerFixDisplayInfo;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.range.util.Ranges;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.collections.preliminary.Presence;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree.PersistentTreeNode;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.logging.Level;

import javax.annotation.Nullable;

@Options(prefix = "phase.repair.integer")
public class InteractiveFixApplicationPhase extends CPAPhase {

  @Option(secure = true, name = "suffix", description = "suffix for the original .c file")
  private String suffixFixed = "backup";

  @Option(secure = true, description = "whether the program repair runs under benchmark mode")
  private boolean forBenchmark = false;

  // metadata persistence
  // they are fixed and should not be configured
  private static final String metaPath =
      "src/org/sosy_lab/cpachecker/core/phase/fix/display";
  private static final String fixMetaFile = "from_java/fixInfo.json";
  private static final String fileMetaFile = "from_java/fileInfo.json";

  private FixCounter fixCounter = new FixCounter();

  private MachineModel machineModel;

  public InteractiveFixApplicationPhase(
      String pIdentifier,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      ShutdownNotifier pShutdownNotifier,
      MainStatistics pStats)
      throws InvalidConfigurationException {
    super(pIdentifier, pConfig, pLogger, pShutdownManager, pShutdownNotifier, pStats);
    config.inject(this);
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
      return CPAPhaseStatus.FAIL;
    }
    // STEP 1: we pretend to apply fixes, without modifying the source files
    Multimap<FileLocation, IntegerFix> loc2Fix = info.getLoc2Fix();
    Multimap<String, FileLocation> file2Loc = HashMultimap.create();
    for (FileLocation loc : loc2Fix.keySet()) {
      file2Loc.put(loc.getFileName(), loc);
    }
    // summarized data structures for real fixes
    Map<String, List<IntegerFixDisplayInfo>> totalFixDisplay = new HashMap<>();
    Map<String, Map<String, CSimpleType>> totalNewDecls = new HashMap<>();
    Map<String, Map<MutableASTForFix, CSimpleType>> totalNewCasts = new HashMap<>();
    Map<String, MutableASTForFix> file2AST = new HashMap<>();

    List<String> fixJSON = new ArrayList<>();
    for (String fileName : file2Loc.keySet()) {
      Collection<FileLocation> locations = file2Loc.get(fileName);
      List<IntegerFixDisplayInfo> displayInfo = new ArrayList<>();
      Map<String, CSimpleType> newDecls = new HashMap<>();
      Map<MutableASTForFix, CSimpleType> newCasts = new HashMap<>();
      pretendFix(fileName, locations, loc2Fix, newDecls, newCasts, file2AST, displayInfo);
      // summarize the results
      totalFixDisplay.put(fileName, displayInfo);
      totalNewDecls.put(fileName, newDecls);
      totalNewCasts.put(fileName, newCasts);
      // STEP 2: output display info into the JSON
      // Note: fixes are organized in a hierarchical manner
      // sorting the display info by the starting offsets
      List<IntegerFixDisplayInfo> ascend = new ArrayList<>(displayInfo);
      List<IntegerFixDisplayInfo> descend = new ArrayList<>(displayInfo);
      Collections.sort(ascend, new Comparator<IntegerFixDisplayInfo>() {
        @Override
        public int compare(
            IntegerFixDisplayInfo pT1, IntegerFixDisplayInfo pT2) {
          IASTFileLocation loc1 = pT1.getLocation();
          IASTFileLocation loc2 = pT2.getLocation();
          int delta = loc1.getNodeOffset() - loc2.getNodeOffset();
          if (delta != 0) {
            return delta;
          }
          // the shorter node should have larger offset (inclusion relation)
          delta = loc1.getNodeLength() - loc2.getNodeLength();
          if (delta != 0) {
            return -delta;
          }
          // then we compare the fixing mode, sanity check should have larger enclosing range
          int priority1 = pT1.getFixMode().getPriority();
          int priority2 = pT2.getFixMode().getPriority();
          if (priority1 > priority2) {
            return -1;
          } else if (priority1 < priority2) {
            return 1;
          }
          return 0;
        }
      });
      Collections.sort(descend, new Comparator<IntegerFixDisplayInfo>() {
        @Override
        public int compare(
            IntegerFixDisplayInfo pT1, IntegerFixDisplayInfo pT2) {
          IASTFileLocation loc1 = pT1.getLocation();
          IASTFileLocation loc2 = pT2.getLocation();
          int ends1 = loc1.getNodeOffset() + loc1.getNodeLength();
          int ends2 = loc2.getNodeOffset() + loc2.getNodeLength();
          int delta = ends1 - ends2;
          if (delta != 0) {
            return delta;
          }
          delta = loc1.getNodeOffset() - loc2.getNodeOffset();
          if (delta != 0) {
            return -delta;
          }
          // then we compare the fixing mode, sanity check should have larger enclosing range
          int priority1 = pT1.getFixMode().getPriority();
          int priority2 = pT2.getFixMode().getPriority();
          if (priority1 > priority2) {
            return 1;
          } else if (priority1 < priority2) {
            return -1;
          }
          return 0;
        }
      });
      assert (ascend.size() == descend.size());
      List<IntegerFixDisplayInfo> hierInfo = new ArrayList<>();
      computeHierarchy(ascend, descend, 0, ascend.size() - 1, 0, descend.size() - 1, null,
          hierInfo);
      // replace the absolute node offset with the relative one (the offset on the certain line)
      // offset --> the offset in the starting line; length --> the offset in the ending line
      String sep = System.getProperty("line.separator");
      int sepLength = sep.length();
      String source = file2AST.get(fileName).synthesize();
      List<String> srcByLine = Lists.newArrayList(Splitter.on(sep).split(source));
      Stack<Integer> columnByLine = new Stack<>();
      // line(0) = 0, line(1) = length of line_1, line(2) = line(1) + length of line_2, ...
      columnByLine.push(0);
      for (String line : srcByLine) {
        columnByLine.push(columnByLine.peek() + line.length() + sepLength);
      }
      // the last line should not contain line-break
      if (!columnByLine.isEmpty()) {
        int lastSize = columnByLine.pop();
        lastSize -= sepLength;
        columnByLine.push(lastSize);
      }
      for (IntegerFixDisplayInfo singleInfo : displayInfo) {
        IASTFileLocation loc = singleInfo.getLocation();
        int startLine = loc.getStartingLineNumber();
        int nodeOffset = loc.getNodeOffset();
        int startOffset = nodeOffset - columnByLine.get(startLine - 1);
        // this is exclusive
        int endLine = loc.getEndingLineNumber();
        int nodeTail = nodeOffset + loc.getNodeLength();
        int endOffset = nodeTail - columnByLine.get(endLine - 1);
        singleInfo.setStartAndEnd(startOffset, endOffset);
      }
      StringBuilder sb = new StringBuilder();
      sb.append("\"");
      sb.append(fileName);
      sb.append("\":[");
      List<String> singleJSON = new ArrayList<>();
      for (IntegerFixDisplayInfo pFixInfo : hierInfo) {
        singleJSON.add(pFixInfo.toString());
      }
      sb.append(Joiner.on(',').join(singleJSON));
      sb.append("]");
      fixJSON.add(sb.toString());
    }
    writeFixToJSON(fixJSON);
    // STEP 4: generate file explore guide
    // Note: only files to be analyzed are displayed in the trees, while other irrelevant files
    // should not be shown
    writeFileToJSON(file2Loc.keySet());
    // STEP 5: startup the python server
    try {
      ProcessBuilder pb = new ProcessBuilder("python", "server.py");
      pb.directory(Paths.get(GlobalInfo.getInstance().getIoManager().getRootDirectory(),
          metaPath).toFile());
      Process proc = pb.start();
      // redirect output of subprocess to the output of this program
      BufferedReader bread = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
      StringBuilder bout = new StringBuilder();
      String outline;
      while ((outline = bread.readLine()) != null) {
        bout.append(outline);
        bout.append(System.getProperty("line.separator"));
      }
      System.out.println(bout.toString());
    } catch (IOException e) {
      throw new IllegalStateException("Fatal: error in setting up the server");
    }
    // STEP 6: check the output of server and filter out some fixes

    return CPAPhaseStatus.SUCCESS;
  }

  /**
   * Pretend to apply fixes to the specific program file.
   *
   * @param fileName    file name
   * @param locations   file location set
   * @param loc2Fix     map from file location to integer fix
   * @param pNewDecls   map from qualified name to its new type
   * @param pNewCasts   map from AST to its new casted type
   * @param pFile2AST   map from file name to its total AST
   * @param displayInfo display info set
   */
  private void pretendFix(
      String fileName, Collection<FileLocation> locations, Multimap<FileLocation, IntegerFix>
      loc2Fix, Map<String, CSimpleType> pNewDecls, Map<MutableASTForFix, CSimpleType> pNewCasts,
      Map<String, MutableASTForFix> pFile2AST, List<IntegerFixDisplayInfo> displayInfo)
      throws Exception {
    File programFile = new File(fileName);
    if (!programFile.exists()) {
      logger.log(Level.SEVERE, "Cannot locate the program file: " + fileName);
      return;
    }
    FileContent content = FileContent.createForExternalFileLocation(fileName);
    IScannerInfo scanner = new ScannerInfo();
    IParserLogService logService = new DefaultLogService();
    IncludeFileContentProvider includeProvider = IncludeFileContentProvider.getEmptyFilesProvider();
    IASTTranslationUnit unit = GCCLanguage.getDefault().getASTTranslationUnit(content, scanner,
        includeProvider, null, GCCLanguage.OPTION_IS_SOURCE_UNIT, logService);
    MutableASTForFix ast = MutableASTForFix.createMutableASTFromTranslationUnit(unit);
    // add the association between file name and the AST
    pFile2AST.put(fileName, ast);
    // build the relation between file location and AST node
    Map<FileLocation, MutableASTForFix> loc2Ast = new HashMap<>();
    IntegerFixApplicationPhase.createMapFromLocationToASTNode(ast, loc2Ast, locations);
    // triage fixes
    Map<FileLocation, IntegerFix> castFix = new HashMap<>();
    Map<FileLocation, IntegerFix> specFix = new HashMap<>();
    Map<FileLocation, IntegerFix> checkFix = new HashMap<>();
    for (FileLocation loc : locations) {
      Collection<IntegerFix> fixes = loc2Fix.get(loc);
      for (IntegerFix singleFix : fixes) {
        switch (singleFix.getFixMode()) {
          case CAST:
            castFix.put(loc, singleFix);
            break;
          case CHECK_CONV:
            checkFix.put(loc, singleFix);
            break;
          case SPECIFIER:
            specFix.put(loc, singleFix);
        }
      }
    }
    // apply fixes
    Iterator<Entry<FileLocation, IntegerFix>> iterator = Iterators.concat(castFix.entrySet()
        .iterator(), checkFix.entrySet().iterator(), specFix.entrySet().iterator());
    IntegerFixInfo intInfo = (IntegerFixInfo) FixProvider.getFixInfo(BugCategory.INTEGER);
    assert (intInfo != null);
    for (Entry<FileLocation, IntegerFix> entry : specFix.entrySet()) {
      FileLocation loc = entry.getKey();
      String name = intInfo.getQualifiedName(loc);
      if (name != null) {
        CSimpleType targetType = entry.getValue().getTargetType();
        pNewDecls.put(name, targetType);
      }
    }
    while (iterator.hasNext()) {
      Entry<FileLocation, IntegerFix> entry = iterator.next();
      FileLocation location = entry.getKey();
      MutableASTForFix astNode = loc2Ast.get(location);
      if (astNode != null) {
        pretendFix(astNode, entry.getValue(), pNewCasts, pNewDecls, displayInfo);
      }
    }
  }

  private void pretendFix(MutableASTForFix pAstNode, IntegerFix pFix, Map<MutableASTForFix,
      CSimpleType> pNewCasts, Map<String, CSimpleType> pNewDecls, List<IntegerFixDisplayInfo>
      pDisplayInfo) {
    CSimpleType newType = pFix.getTargetType();
    if (newType == null || newType.getType() == CBasicType.BOOL) {
      return;
    }
    pAstNode = IntegerFixApplicationPhase.skipPrimaryBrackets(pAstNode);
    switch (pFix.getFixMode()) {
      case SPECIFIER: {
        IASTNode node = pAstNode.getWrappedNode();
        if (node instanceof IASTDeclarator) {
          MutableASTForFix possibleDeclarationStmt = pAstNode.getParent(2);
          if (possibleDeclarationStmt != null) {
            IASTNode wrappedNode = possibleDeclarationStmt.getWrappedNode();
            if (wrappedNode instanceof IASTDeclarationStatement) {
              MutableASTForFix declaration = pAstNode.getParent(1);
              assert (declaration != null);
              IASTNode declarationNode = declaration.getWrappedNode();
              if (declarationNode instanceof IASTSimpleDeclaration) {
                // increment the count of valid fixes
                pDisplayInfo.add(IntegerFixDisplayInfo.of(UUID.randomUUID(), pFix, pAstNode));
                fixCounter.specInc(forBenchmark, pAstNode);
              }
            } else if (wrappedNode instanceof CASTKnRFunctionDeclarator) {
              pDisplayInfo.add(IntegerFixDisplayInfo.of(UUID.randomUUID(), pFix, pAstNode));
              fixCounter.specInc(forBenchmark, pAstNode);
            }
          }
        } else if (node instanceof IASTParameterDeclaration) {
          pDisplayInfo.add(IntegerFixDisplayInfo.of(UUID.randomUUID(), pFix, pAstNode));
          fixCounter.specInc(forBenchmark, pAstNode);
        }
        break;
      }
      case CHECK_CONV: {
        // the original type may be changed by other accepted fixes
        CSimpleType type = IntegerFixApplicationPhase.deriveTypeForASTNode(machineModel, pAstNode,
            pNewCasts, pNewDecls);
        if (type != null) {
          type = type.getCanonicalType();
          // check if the current AST is the array subscript
          if (IntegerFixApplicationPhase.checkIfArraySubscript(pAstNode)) {
            Range oldTypeRange = Ranges.getTypeRange(type, machineModel);
            Range newTypeRange = Ranges.getTypeRange(newType, machineModel);
            if (newTypeRange.contains(oldTypeRange) && !newTypeRange.equals(oldTypeRange)) {
              pretendArithCheck(pAstNode, type, pNewCasts, pNewDecls, pDisplayInfo);
            }
          } else {
            if (!Types.isIntegralType(newType)) {
              break;
            }
            Range oldTypeRange = Ranges.getTypeRange(type, machineModel);
            Range newTypeRange = Ranges.getTypeRange(newType, machineModel);
            if (newTypeRange.equals(oldTypeRange)) {
              pretendArithCheck(pAstNode, type, pNewCasts, pNewDecls, pDisplayInfo);
            } else if (newTypeRange.contains(oldTypeRange)) {
              break;
            } else {
              pretendArithCheck(pAstNode, type, pNewCasts, pNewDecls, pDisplayInfo);
              // check the sub-expression
              pDisplayInfo.add(IntegerFixDisplayInfo.of(UUID.randomUUID(), pFix, pAstNode));
              fixCounter.checkInc(forBenchmark, pAstNode);
            }
          }
        }
        break;
      }
      case CAST: {
        // derive the original type which possibly has been affected by accepted fixes
        CSimpleType type = IntegerFixApplicationPhase.deriveTypeForASTNode(machineModel,
            pAstNode, pNewCasts, pNewDecls);
        boolean shouldIgnore = false;
        if (type != null) {
          shouldIgnore = Types.canHoldAllValues(type, newType, machineModel);
        }
        MutableASTForFix parentNode = pAstNode.getParent(1);
        if (parentNode != null) {
          IASTNode wrappedParent = parentNode.getWrappedNode();
          if (wrappedParent instanceof IASTCastExpression) {
            if (shouldIgnore) {
              pNewCasts.put(pAstNode, type);
            } else {
              pNewCasts.put(pAstNode, newType);
              pDisplayInfo.add(IntegerFixDisplayInfo.of(UUID.randomUUID(), pFix, pAstNode));
              fixCounter.castInc(forBenchmark, pAstNode);
            }
            break;
          }
        }
        IASTNode wrappedSelf = pAstNode.getWrappedNode();
        if (wrappedSelf instanceof IASTCastExpression) {
          Range oldTypeRange = Ranges.getTypeRange(type, machineModel);
          Range newTypeRange = Ranges.getTypeRange(newType, machineModel);
          if (!newTypeRange.equals(oldTypeRange)) {
            pNewCasts.put(pAstNode, newType);
            pDisplayInfo.add(IntegerFixDisplayInfo.of(UUID.randomUUID(), pFix, pAstNode));
            fixCounter.castInc(forBenchmark, pAstNode);
          }
          break;
        }
        // ordinary case
        if (!shouldIgnore) {
          pNewCasts.put(pAstNode, newType);
          pDisplayInfo.add(IntegerFixDisplayInfo.of(UUID.randomUUID(), pFix, pAstNode));
          fixCounter.castInc(forBenchmark, pAstNode);
        }
        break;
      }
      default:
        logger.log(Level.WARNING, "Unsupported fix mode: " + pFix.getFixMode());
    }
  }

  private void pretendArithCheck(MutableASTForFix pAstNode, CSimpleType pNewType,
                                 Map<MutableASTForFix, CSimpleType> pNewCasts,
                                 Map<String, CSimpleType> pNewDecls,
                                 List<IntegerFixDisplayInfo> pInfo) {
    IASTNode wrappedNode = pAstNode.getWrappedNode();
    if (wrappedNode instanceof IASTUnaryExpression) {
      int unaryOp = ((IASTUnaryExpression) wrappedNode).getOperator();
      if (unaryOp == IASTUnaryExpression.op_bracketedPrimary) {
        List<MutableASTForFix> children = pAstNode.getChildren();
        if (children.size() == 1) {
          pretendArithCheck(children.get(0), pNewType, pNewCasts, pNewDecls, pInfo);
        }
      }
    } else if (wrappedNode instanceof IASTBinaryExpression) {
      int binaryOp = ((IASTBinaryExpression) wrappedNode).getOperator();
      List<MutableASTForFix> children = pAstNode.getChildren();
      if (children.size() == 2) {
        MutableASTForFix op1 = children.get(0);
        MutableASTForFix op2 = children.get(1);
        CSimpleType t1 = IntegerFixApplicationPhase.deriveTypeForASTNode(machineModel, op1,
            pNewCasts, pNewDecls);
        CSimpleType t2 = IntegerFixApplicationPhase.deriveTypeForASTNode(machineModel, op2,
            pNewCasts, pNewDecls);
        if (t1 != null) {
          pretendArithCheck(op1, t1, pNewCasts, pNewDecls, pInfo);
          if (!Types.canHoldAllValues(pNewType, t1, machineModel)) {
            IntegerFix newCheck = new IntegerFix(IntegerFixMode.CHECK_CONV, pNewType);
            pInfo.add(IntegerFixDisplayInfo.of(UUID.randomUUID(), newCheck, op1));
            fixCounter.checkInc(forBenchmark, op1);
          }
        }
        if (t2 != null) {
          pretendArithCheck(op2, t2, pNewCasts, pNewDecls, pInfo);
          if (!Types.canHoldAllValues(pNewType, t2, machineModel)) {
            IntegerFix newCheck = new IntegerFix(IntegerFixMode.CHECK_CONV, pNewType);
            pInfo.add(IntegerFixDisplayInfo.of(UUID.randomUUID(), newCheck, op2));
            fixCounter.checkInc(forBenchmark, op2);
          }
        }
        switch (binaryOp) {
          case IASTBinaryExpression.op_plus:
          case IASTBinaryExpression.op_minus:
          case IASTBinaryExpression.op_multiply: {
            // add sanity check here
            IntegerFix newFix = new IntegerFix(IntegerFixMode.CHECK_ARITH, pNewType);
            pInfo.add(IntegerFixDisplayInfo.of(UUID.randomUUID(), newFix, pAstNode));
            fixCounter.checkInc(forBenchmark, pAstNode);
          }
        }
      }
    }
  }

  /**
   * Compute the hierarchical relations in display info s.
   *
   * @param ascend display info list sorted by the starting offset
   * @param descend display info list sorted by the ending offset
   * @param aStart start index (inclusive) of ascend list
   * @param aEnd end index (inclusive) of ascend list
   * @param dStart start index (inclusive) of descend list
   * @param dEnd end index (inclusive) of descend list
   * @param top when it is not null, we add the current element as the child of the top
   * @param result the result display info list
   */
  private void computeHierarchy(List<IntegerFixDisplayInfo> ascend, List<IntegerFixDisplayInfo>
      descend, int aStart, int aEnd, int dStart, int dEnd, @Nullable IntegerFixDisplayInfo top,
      List<IntegerFixDisplayInfo> result) {
    if (aStart > aEnd || dStart > dEnd) {
      // recursion stops
      return;
    }
    int aPos, dPos;
    for (aPos = aStart, dPos = dStart; aPos <= aEnd && dPos <= dEnd; ) {
      IntegerFixDisplayInfo aCurrent = ascend.get(aPos);
      IntegerFixDisplayInfo dCurrent = descend.get(dPos);
      if (aCurrent == dCurrent) {
        if (top == null) {
          result.add(aCurrent);
        } else {
          top.addChild(aCurrent);
        }
        aPos++;
        dPos++;
      } else {
        // find the element that equals to aCurrent in the descend list
        int dHit;
        for (dHit = dPos + 1; dHit <= dEnd; dHit++) {
          if (descend.get(dHit) == aCurrent) {
            break;
          }
        }
        // invariant: sorting does not drop items
        assert (descend.get(dHit) == aCurrent);
        if (top == null) {
          result.add(aCurrent);
        } else {
          top.addChild(aCurrent);
        }
        // recursively address the remaining sequences
        computeHierarchy(ascend, descend, aPos + 1, aPos + (dHit - dPos), dPos, dHit - 1,
            aCurrent, result);
        aPos += (dHit - dPos + 1);
        dPos = dHit + 1;
      }
    }
  }

  private void writeFixToJSON(List<String> pFixJSON) {
    Path infoPath = Paths.get(GlobalInfo.getInstance().getIoManager().getRootDirectory(), metaPath,
        fixMetaFile);
    File infoFile = infoPath.toFile();
    if (infoFile.exists()) {
      if (!infoFile.delete()) {
        throw new IllegalStateException("Failed to clear the previous file: permission denied");
      }
    }
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append(Joiner.on(',').join(pFixJSON));
    sb.append("}");
    // write to the specified file
    try {
      BufferedWriter bout = new BufferedWriter(new FileWriter(infoFile.getAbsolutePath()));
      bout.write(sb.toString());
      bout.flush();
    } catch (IOException e) {
      throw new IllegalStateException("IO Error: permission denied");
    }
  }

  private void writeFileToJSON(Set<String> pFileNames) {
    // STEP 1: find the common prefix of paths
    PathCopyingPersistentTree<String, Presence> fileTree = PathCopyingPersistentTree.of();
    for (String fileName : pFileNames) {
      Path filePath = new File(fileName).toPath();
      List<String> fileSegments = new ArrayList<>();
      // no NPE will be thrown because the input file names are all absolute
      fileSegments.add(filePath.getRoot().toString());
      for (int i = 0; i < filePath.getNameCount(); i++) {
        fileSegments.add(filePath.getName(i).toString());
      }
      fileTree = fileTree.setElementAndCopy(fileSegments, Presence.INSTANCE);
    }
    // the file list must be non-empty, otherwise there is no files for analysis
    assert (!fileTree.isEmpty());
    PersistentTreeNode<String, Presence> node = fileTree.getRoot();
    List<String> commonPrefix = new ArrayList<>();
    do {
      Set<String> keys = node.branches();
      if (keys.size() != 1) {
        break;
      }
      String onlyKey = Iterables.getOnlyElement(keys);
      commonPrefix.add(onlyKey);
      node = node.getChild(onlyKey);
    } while (node != null);
    // STEP 2: compute hierarchy of paths
    StringBuilder sb = new StringBuilder();
    if (!commonPrefix.isEmpty()) {
      // prefix exists
      PersistentTreeNode<String, Presence> subNode = fileTree.getSubtreeRoot(commonPrefix);
      String rootDir = commonPrefix.get(0);
      commonPrefix.remove(0);
      String[] prefixArray = commonPrefix.toArray(new String[commonPrefix.size()]);
      Path commonPath = Paths.get(rootDir, prefixArray);
      sb.append("[");
      sb.append("{");
      sb.append("\"name\":").append("\"").append(commonPath).append("\"").append(",");
      sb.append("\"children\":").append(writeFileToJSON0(subNode));
      sb.append("}");
      sb.append("]");
    } else {
      // in some systems such as Windows, there is no unified root directory, thus it is possible
      // that two files in C:\ and D:| respectively have no common prefix path
      // root node must be non-null because the file tree is non-empty
      sb.append(writeFileToJSON0(fileTree.getRoot()));
    }
    // STEP 3: write the JSON to the file
    Path filePath = Paths.get(GlobalInfo.getInstance().getIoManager().getRootDirectory(),
        metaPath, fileMetaFile);
    File fileFile = filePath.toFile();
    if (fileFile.exists()) {
      if (!fileFile.delete()) {
        throw new IllegalStateException("Failed to clear the previous file: permission denied");
      }
    }
    try {
      BufferedWriter bout = new BufferedWriter(new FileWriter(fileFile.getAbsolutePath()));
      bout.write(sb.toString());
      bout.flush();
    } catch (IOException e) {
      throw new IllegalStateException("IO Error: permission denied");
    }
  }

  private String writeFileToJSON0(@Nullable PersistentTreeNode<String, Presence> pNode) {
    if (pNode == null) {
      return "[]";
    } else {
      Set<String> keys = pNode.branches();
      int keySize = keys.size();
      if (keySize == 0) {
        return "[]";
      } else {
        List<String> subJSON = new ArrayList<>(keySize);
        for (String key : keys) {
          StringBuilder subSb = new StringBuilder();
          subSb.append("{");
          subSb.append("\"name\":").append("\"").append(key).append("\"").append(",");
          PersistentTreeNode<String, Presence> child = pNode.getChild(key);
          subSb.append("\"children\":").append(writeFileToJSON0(child));
          subSb.append("}");
          subJSON.add(subSb.toString());
        }
        return "[" + Joiner.on(',').join(subJSON) + "]";
      }
    }
  }

}
