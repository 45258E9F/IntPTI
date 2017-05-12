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
package org.sosy_lab.cpachecker.util.ci;

import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AppliedCustomInstructionParserTest {

  private CFAInfo cfaInfo;
  private CFA cfa;
  private AppliedCustomInstructionParser aciParser;
  private List<CLabelNode> labelNodes;

  @Before
  public void init() throws IOException, ParserException, InterruptedException {
    String testProgram = ""
        + "extern int test3(int);"
        + "int test(int p) {"
        + "return p+1;"
        + "}"
        + "int test2(int p) {"
        + "start_ci: return p+2;"
        + "}"
        + "void ci(int var) {"
        + "var = var + 39;"
        + "int globalVar;"
        + "int u;"
        + "int x = globalVar + 5;"
        + "int y;"
        + "int z;"
        + "start_ci:"
        + "if (z>0) {"
        + "y = y + 1;"
        + "} else {"
        + "var = var + 1;"
        + "}"
        + "test(u);"
        + "z = test(globalVar);"
        + "end_ci_1: x = x + 1;"
        + "}"
        + "void main() {"
        + "int m;"
        + "int n;"
        + "int o;"
        + "start_ci:"
        + "if (m>o) {"
        + "ci(m);"
        + "}"
        + "test3(n);"
        + "n = test3(o);"
        + "end_ci_2:"
        + "test2(4);"
        + "}";
    cfa = TestDataTools.makeCFA(testProgram);
    aciParser =
        new AppliedCustomInstructionParser(
            ShutdownNotifier.createDummy(),
            TestLogManager.getInstance(),
            cfa);
    GlobalInfo.getInstance().storeCFA(cfa);
    cfaInfo = GlobalInfo.getInstance().getCFAInfo().get();
    labelNodes = getLabelNodes(cfa);
  }

  @Test
  public void testGetCFANode() throws AppliedCustomInstructionParsingFailedException {
    try {
      aciParser.getCFANode("N57", cfaInfo);
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(AppliedCustomInstructionParsingFailedException.class);
    }

    try {
      aciParser.getCFANode("-1", cfaInfo);
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(AppliedCustomInstructionParsingFailedException.class);
    }

    Truth
        .assertThat(aciParser.getCFANode(cfa.getFunctionHead("main").getNodeNumber() + "", cfaInfo))
        .isEqualTo(cfa.getMainFunction());
  }

  @Test
  public void testReadCustomInstruction()
      throws AppliedCustomInstructionParsingFailedException, InterruptedException,
             SecurityException {
    try {
      aciParser.readCustomInstruction("test4");
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(AppliedCustomInstructionParsingFailedException.class);
      Truth.assertThat(e.getMessage()).matches("Function unknown in program");
    }

    try {
      aciParser.readCustomInstruction("test");
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(AppliedCustomInstructionParsingFailedException.class);
      Truth.assertThat(e.getMessage()).matches("Missing label for start of custom instruction");
    }

    try {
      aciParser.readCustomInstruction("test2");
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(AppliedCustomInstructionParsingFailedException.class);
      Truth.assertThat(e.getMessage()).matches("Missing label for end of custom instruction");
    }

    CustomInstruction ci = aciParser.readCustomInstruction("ci");
    CFANode expectedStart = null;
    Collection<CFANode> expectedEnds = new ArrayList<>(2);
    for (CLabelNode n : labelNodes) {
      if (n.getLabel().startsWith("start_ci") && n.getFunctionName().equals("ci")) {
        expectedStart = n;
      }
      if (n.getLabel().startsWith("end_ci") && n.getFunctionName().equals("ci")) {
        for (CFANode e : CFAUtils.predecessorsOf(n)) {
          expectedEnds.add(e);
        }
      }
    }
    Truth.assertThat(ci.getStartNode()).isEqualTo(expectedStart);
    Truth.assertThat(ci.getEndNodes()).containsExactlyElementsIn(expectedEnds);
    List<String> list = new ArrayList<>();
    list.add("ci::globalVar");
    list.add("ci::u");
    list.add("ci::var");
    list.add("ci::y");
    list.add("ci::z");
    Truth.assertThat(ci.getInputVariables()).containsExactlyElementsIn(list).inOrder();
    list = new ArrayList<>();
    list.add("ci::var");
    list.add("ci::y");
    list.add("ci::z");
    Truth.assertThat(ci.getOutputVariables()).containsExactlyElementsIn(list).inOrder();

    ci = aciParser.readCustomInstruction("main");
    expectedStart = null;
    expectedEnds = new ArrayList<>(1);
    for (CLabelNode n : labelNodes) {
      if (n.getLabel().startsWith("start_ci") && n.getFunctionName().equals("main")) {
        expectedStart = n;
      }
      if (n.getLabel().startsWith("end_ci") && n.getFunctionName().equals("main")) {
        for (CFANode e : CFAUtils.predecessorsOf(n)) {
          expectedEnds.add(e);
        }
      }
    }
    Truth.assertThat(ci.getStartNode()).isEqualTo(expectedStart);
    Truth.assertThat(ci.getEndNodes()).containsExactlyElementsIn(expectedEnds);
    list = new ArrayList<>();
    list.add("main::m");
    list.add("main::n");
    list.add("main::o");
    Truth.assertThat(ci.getInputVariables()).containsExactlyElementsIn(list).inOrder();
    list = new ArrayList<>();
    list.add("main::n");
    Truth.assertThat(ci.getOutputVariables()).containsExactlyElementsIn(list).inOrder();
  }

  private List<CLabelNode> getLabelNodes(CFA cfa) {
    List<CLabelNode> result = new ArrayList<>();
    for (CFANode n : cfa.getAllNodes()) {
      if (n instanceof CLabelNode) {
        result.add((CLabelNode) n);
      }
    }
    return result;
  }

  @Test
  public void testParse()
      throws AppliedCustomInstructionParsingFailedException, IOException, InterruptedException,
             SecurityException, ParserException {
    String testProgram = ""
        + "void main() {"
        + "int x;"
        + "int y;"
        + "start_ci: x = x + y;"
        + "end_ci_1:"
        + "x = x + x;"
        + "y = y + y;"
        + "y = y + x;"
        + "}";

    CFA cfa = TestDataTools.makeCFA(testProgram);
    GlobalInfo.getInstance().storeCFA(cfa);
    aciParser =
        new AppliedCustomInstructionParser(
            ShutdownNotifier.createDummy(),
            TestLogManager.getInstance(),
            cfa);
    Path p = Paths.createTempPath("test_acis", null);
    try (Writer file = Files.openOutputFile(p)) {
      file.append("main\n");
      CFANode node;
      Deque<CFANode> toVisit = new ArrayDeque<>();
      toVisit.add(cfa.getMainFunction());
      while (!toVisit.isEmpty()) {
        node = toVisit.pop();

        for (CFANode succ : CFAUtils.allSuccessorsOf(node)) {
          toVisit.add(succ);
          if (node.getEdgeTo(succ).getEdgeType().equals(CFAEdgeType.StatementEdge)) {
            file.append(node.getNodeNumber() + "\n");
          }
        }
      }
      file.flush();
    }

    CFANode expectedStart = null;
    for (CLabelNode n : getLabelNodes(cfa)) {
      if (n.getLabel().startsWith("start_ci") && n.getFunctionName().equals("main")) {
        expectedStart = n;
      }
    }
    int startNodeNr = expectedStart.getNodeNumber();

    CustomInstructionApplications cia = aciParser.parse(p, Paths.createTempPath("ci_spec", "txt"));
    Map<CFANode, AppliedCustomInstruction> cis = cia.getMapping();
    Truth.assertThat(cis.size()).isEqualTo(4);
    List<CFANode> aciNodes = new ArrayList<>(2);

    for (Entry<CFANode, AppliedCustomInstruction> entry : cis.entrySet()) {
      Collection<String> inputVars = new ArrayList<>();
      Collection<String> outputVars = new ArrayList<>();
      Pair<List<String>, String> fakeSMTDescription;
      List<String> list = new ArrayList<>();
      List<String> variables = new ArrayList<>();
      SSAMap ssaMap;
      aciNodes.clear();
      aciNodes.add(entry.getKey());
      aciNodes.add(entry.getKey().getLeavingEdge(0).getSuccessor());

      if (entry.getKey().getNodeNumber() == startNodeNr) {
        inputVars.add("main::x");
        inputVars.add("main::y");
        Truth.assertThat(entry.getValue().getInputVariables()).containsExactlyElementsIn(inputVars);
        outputVars.add("main::x");
        Truth.assertThat(entry.getValue().getOutputVariables())
            .containsExactlyElementsIn(outputVars);

        fakeSMTDescription = entry.getValue().getFakeSMTDescription();
        list.add("(declare-fun |main::x| () Int)");
        list.add("(declare-fun |main::y| () Int)");
        list.add("(declare-fun |main::x@1| () Int)");
        Truth.assertThat(fakeSMTDescription.getFirst()).containsExactlyElementsIn(list);
        Truth.assertThat(fakeSMTDescription.getSecond()).isEqualTo(
            "(define-fun ci() Bool(and (= |main::x| 0)(and (= |main::y| 0) (= |main::x@1| 0))))");

        ssaMap = entry.getValue().getIndicesForReturnVars();
        variables.add("main::x");
        Truth.assertThat(ssaMap.allVariables()).containsExactlyElementsIn(variables);
        Truth.assertThat(ssaMap.getIndex(variables.get(0))).isEqualTo(1);

        Truth.assertThat(entry.getValue().getStartAndEndNodes())
            .containsExactlyElementsIn(aciNodes);

      } else if (entry.getKey().getNodeNumber() == startNodeNr + 2) {
        inputVars.add("main::x");
        inputVars.add("main::x");
        Truth.assertThat(entry.getValue().getInputVariables()).containsExactlyElementsIn(inputVars);
        outputVars.add("main::x");
        Truth.assertThat(entry.getValue().getOutputVariables())
            .containsExactlyElementsIn(outputVars);

        fakeSMTDescription = entry.getValue().getFakeSMTDescription();
        list.clear();
        list.add("(declare-fun |main::x| () Int)");
        list.add("(declare-fun |main::x| () Int)");
        list.add("(declare-fun |main::x@1| () Int)");
        Truth.assertThat(fakeSMTDescription.getFirst()).containsExactlyElementsIn(list);
        Truth.assertThat(fakeSMTDescription.getSecond()).isEqualTo(
            "(define-fun ci() Bool(and (= |main::x| 0)(and (= |main::x| 0) (= |main::x@1| 0))))");

        ssaMap = entry.getValue().getIndicesForReturnVars();
        variables.add("main::x");
        Truth.assertThat(ssaMap.allVariables()).containsExactlyElementsIn(variables);
        Truth.assertThat(ssaMap.getIndex(variables.get(0))).isEqualTo(1);

        Truth.assertThat(entry.getValue().getStartAndEndNodes())
            .containsExactlyElementsIn(aciNodes);

      } else if (entry.getKey().getNodeNumber() == startNodeNr + 3) {
        inputVars.add("main::y");
        inputVars.add("main::y");
        Truth.assertThat(entry.getValue().getInputVariables()).containsExactlyElementsIn(inputVars);
        outputVars.add("main::y");
        Truth.assertThat(entry.getValue().getOutputVariables())
            .containsExactlyElementsIn(outputVars);

        fakeSMTDescription = entry.getValue().getFakeSMTDescription();
        list.clear();
        list.add("(declare-fun |main::y| () Int)");
        list.add("(declare-fun |main::y| () Int)");
        list.add("(declare-fun |main::y@1| () Int)");
        Truth.assertThat(fakeSMTDescription.getFirst()).containsExactlyElementsIn(list);
        Truth.assertThat(fakeSMTDescription.getSecond()).isEqualTo(
            "(define-fun ci() Bool(and (= |main::y| 0)(and (= |main::y| 0) (= |main::y@1| 0))))");

        ssaMap = entry.getValue().getIndicesForReturnVars();
        variables.add("main::y");
        Truth.assertThat(ssaMap.allVariables()).containsExactlyElementsIn(variables);
        Truth.assertThat(ssaMap.getIndex(variables.get(0))).isEqualTo(1);

        Truth.assertThat(entry.getValue().getStartAndEndNodes())
            .containsExactlyElementsIn(aciNodes);

      } else if (entry.getKey().getNodeNumber() == startNodeNr + 4) {
        inputVars.add("main::y");
        inputVars.add("main::x");
        Truth.assertThat(entry.getValue().getInputVariables()).containsExactlyElementsIn(inputVars);
        outputVars.add("main::y");
        Truth.assertThat(entry.getValue().getOutputVariables())
            .containsExactlyElementsIn(outputVars);

        fakeSMTDescription = entry.getValue().getFakeSMTDescription();
        list.clear();
        list.add("(declare-fun |main::y| () Int)");
        list.add("(declare-fun |main::x| () Int)");
        list.add("(declare-fun |main::y@1| () Int)");
        Truth.assertThat(fakeSMTDescription.getFirst()).containsExactlyElementsIn(list);
        Truth.assertThat(fakeSMTDescription.getSecond()).isEqualTo(
            "(define-fun ci() Bool(and (= |main::y| 0)(and (= |main::x| 0) (= |main::y@1| 0))))");

        ssaMap = entry.getValue().getIndicesForReturnVars();
        variables.add("main::y");
        Truth.assertThat(ssaMap.allVariables()).containsExactlyElementsIn(variables);
        Truth.assertThat(ssaMap.getIndex(variables.get(0))).isEqualTo(1);

        Truth.assertThat(entry.getValue().getStartAndEndNodes())
            .containsExactlyElementsIn(aciNodes);

      } else {
        Truth.assertThat(false).isTrue();
      }
    }
  }

}
