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
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpressionCollectingVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializers;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.VariableClassification.Partition;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Options(prefix = "cfa.variableClassification")
public class VariableClassificationBuilder {

  @Option(secure = true, name = "logfile", description = "Dump variable classification to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dumpfile = Paths.get("VariableClassification.log");

  @Option(secure = true, description = "Dump variable type mapping to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path typeMapFile = Paths.get("VariableTypeMapping.txt");

  @Option(secure = true, description = "Dump domain type statistics to a CSV file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path domainTypeStatisticsFile = null;

  @Option(secure = true, description = "Print some information about the variable classification.")
  private boolean printStatsOnStartup = false;

  /**
   * Use {@link FunctionEntryNode#getReturnVariable()} and
   * {@link AReturnStatement#asAssignment()} instead.
   */
  @Deprecated
  public static final String FUNCTION_RETURN_VARIABLE = "__retval__";

  private static final String SCOPE_SEPARATOR = "::";

  /**
   * normally a boolean value would be 0 or 1,
   * however there are cases, where the values are only 0 and 1,
   * but the variable is not boolean at all: "int x; if(x!=0 && x!= 1){}".
   * so we allow only 0 as boolean value, and not 1.
   */
  private boolean allowOneAsBooleanValue = false;

  private final Set<String> allVars = new HashSet<>();

  private final Set<String> nonIntBoolVars = new HashSet<>();
  private final Set<String> nonIntEqVars = new HashSet<>();
  private final Set<String> nonIntAddVars = new HashSet<>();

  private final Dependencies dependencies = new Dependencies();

  /**
   * These sets contain all variables even ones of array, pointer or structure types.
   * Such variables cannot be classified even as Int, so they are only kept in these sets in order
   * not to break the classification of Int variables.
   */
  // Initially contains variables used in assumes and assigned to pointer dereferences,
  // then all essential variables (by propagation)
  private final Set<String> relevantVariables = new HashSet<>();
  private final Set<String> addressedVariables = new HashSet<>();

  // Variables and fields used in the right hand side
  private final Multimap<VariableOrField, VariableOrField> assignments =
      LinkedHashMultimap.create();

  /**
   * Fields information doesn't take any aliasing information into account,
   * fields are considered per type, not per composite instance
   */
  // Initially contains fields used in assumes and assigned to pointer dereferences,
  // then all essential fields (by propagation)
  private final Multimap<CCompositeType, String> relevantFields = LinkedHashMultimap.create();

  private final CollectingLHSVisitor collectingLHSVisitor = new CollectingLHSVisitor();

  private final LogManager logger;

  public VariableClassificationBuilder(Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    logger = checkNotNull(pLogger);
    config.inject(this);
  }

  /**
   * This function does the whole work:
   * creating all maps, collecting vars, solving dependencies.
   * The function runs only once, after that it does nothing.
   */
  public VariableClassification build(CFA cfa) throws UnrecognizedCCodeException {
    checkArgument(cfa.getLanguage() == Language.C,
        "VariableClassification currently only supports C");

    // fill maps
    collectVars(cfa);

    // if a value is not boolean, all dependent vars are not boolean and viceversa
    dependencies.solve(nonIntBoolVars);
    dependencies.solve(nonIntEqVars);
    dependencies.solve(nonIntAddVars);

    // Now build the opposites of each non-x-vars-collection.
    // This is responsible for the hierarchy of the variables.
    final Set<String> intBoolVars = new HashSet<>();
    final Set<String> intEqualVars = new HashSet<>();
    final Set<String> intAddVars = new HashSet<>();
    final Set<Partition> intBoolPartitions = new HashSet<>();
    final Set<Partition> intEqualPartitions = new HashSet<>();
    final Set<Partition> intAddPartitions = new HashSet<>();

    for (final String var : allVars) {
      // we have this hierarchy of classes for variables:
      //        IntBool < IntEqBool < IntAddEqBool < AllInt
      // we define and build:
      //        IntBool = IntBool
      //        IntEq   = IntEqBool - IntBool
      //        IntAdd  = IntAddEqBool - IntEqBool
      //        Other   = IntAll - IntAddEqBool

      if (!nonIntBoolVars.contains(var)) {
        intBoolVars.add(var);
        intBoolPartitions.add(dependencies.getPartitionForVar(var));

      } else if (!nonIntEqVars.contains(var)) {
        intEqualVars.add(var);
        intEqualPartitions.add(dependencies.getPartitionForVar(var));

      } else if (!nonIntAddVars.contains(var)) {
        intAddVars.add(var);
        intAddPartitions.add(dependencies.getPartitionForVar(var));
      }
    }

    propagateRelevancy();

    // add last vars to dependencies,
    // this allows to get partitions for all vars,
    // otherwise only dependent vars are in the partitions
    for (String var : allVars) {
      dependencies.addVar(var);
    }

    boolean hasRelevantNonIntAddVars =
        !Sets.intersection(relevantVariables, nonIntAddVars).isEmpty();

    VariableClassification result = new VariableClassification(
        hasRelevantNonIntAddVars,
        intBoolVars,
        intEqualVars,
        intAddVars,
        relevantVariables,
        addressedVariables,
        relevantFields,
        dependencies.partitions,
        intBoolPartitions,
        intEqualPartitions,
        intAddPartitions,
        dependencies.edgeToPartition,
        extractAssumedVariables(cfa.getAllNodes()),
        extractAssignedVariables(cfa.getAllNodes()),
        logger);

    if (printStatsOnStartup) {
      printStats(result);
    }

    if (dumpfile != null) { // option -noout
      try (Writer w = Files.openOutputFile(dumpfile)) {
        w.append("IntBool\n\n");
        w.append(intBoolVars.toString());
        w.append("\n\nIntEq\n\n");
        w.append(intEqualVars.toString());
        w.append("\n\nIntAdd\n\n");
        w.append(intAddVars.toString());
        w.append("\n\nALL\n\n");
        w.append(allVars.toString());
      } catch (IOException e) {
        logger
            .logUserException(Level.WARNING, e, "Could not write variable classification to file");
      }
    }

    if (typeMapFile != null) {
      dumpVariableTypeMapping(typeMapFile, result);
    }

    if (domainTypeStatisticsFile != null) {
      dumpDomainTypeStatistics(domainTypeStatisticsFile, result);
    }

    return result;
  }

  private void dumpDomainTypeStatistics(Path pDomainTypeStatisticsFile, VariableClassification vc) {
    try (Writer w = Files.openOutputFile(pDomainTypeStatisticsFile)) {
      try (PrintWriter p = new PrintWriter(w)) {
        Object[][] statMapping = {
            {"intBoolVars", vc.getIntBoolVars().size()},
            {"intEqualVars", vc.getIntEqualVars().size()},
            {"intAddVars", vc.getIntAddVars().size()},
            {"allVars", allVars.size()},
            {"intBoolVarsRelevant", countNumberOfRelevantVars(vc.getIntBoolVars())},
            {"intEqualVarsRelevant", countNumberOfRelevantVars(vc.getIntEqualVars())},
            {"intAddVarsRelevant", countNumberOfRelevantVars(vc.getIntAddVars())},
            {"allVarsRelevant", countNumberOfRelevantVars(allVars)}
        };
        // Write header
        for (int col = 0; col < statMapping.length; col++) {
          p.print(statMapping[col][0]);
          if (col != statMapping.length - 1) {
            p.print("\t");
          }
        }
        p.print("\n");
        // Write data
        for (int col = 0; col < statMapping.length; col++) {
          p.print(statMapping[col][1]);
          if (col != statMapping.length - 1) {
            p.print("\t");
          }
        }
        p.print("\n");
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e,
          "Could not write variable classification statistics to file");
    }
  }

  private void dumpVariableTypeMapping(Path target, VariableClassification vc) {
    try (Writer w = Files.openOutputFile(target)) {
      for (String var : allVars) {
        int type = 0;
        if (vc.getIntBoolVars().contains(var)) {
          type += 1 + 2 + 4; // IntBool is subset of IntEqualBool and IntAddEqBool
        } else if (vc.getIntEqualVars().contains(var)) {
          type += 2 + 4; // IntEqual is subset of IntAddEqBool
        } else if (vc.getIntAddVars().contains(var)) {
          type += 4;
        }
        w.append(String.format("%s\t%d%n", var, type));
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write variable type mapping to file");
    }
  }

  private void printStats(VariableClassification vc) {
    int numOfBooleans = 0;
    for (Partition p : vc.getIntEqualPartitions()) {
      numOfBooleans += p.getVars().size();
    }
    assert numOfBooleans == vc.getIntBoolVars().size();

    int numOfIntEquals = 0;
    for (Partition p : vc.getIntEqualPartitions()) {
      numOfIntEquals += p.getVars().size();
    }
    assert numOfIntEquals == vc.getIntEqualVars().size();

    int numOfIntAdds = 0;
    for (Partition p : vc.getIntAddPartitions()) {
      numOfIntAdds += p.getVars().size();
    }
    assert numOfIntAdds == vc.getIntAddVars().size();

    final String prefix = "\nVC ";
    StringBuilder str = new StringBuilder("VariableClassification Statistics\n");
    Joiner.on(prefix).appendTo(str, new String[]{
        "---------------------------------",
        "number of boolean vars:  " + numOfBooleans,
        "number of intEq vars:    " + numOfIntEquals,
        "number of intAdd vars:   " + numOfIntAdds,
        "number of all vars:      " + allVars.size(),
        "number of addr. vars:    " + addressedVariables.size(),
        "number of intBool partitions:  " + vc.getIntBoolPartitions().size(),
        "number of intEq partitions:    " + vc.getIntEqualPartitions().size(),
        "number of intAdd partitions:   " + vc.getIntAddPartitions().size(),
        "number of all partitions:      " + dependencies.partitions.size(),
    });
    str.append("\n---------------------------------\n");

    logger.log(Level.INFO, str.toString());
  }

  private int countNumberOfRelevantVars(Set<String> ofVars) {
    return Sets.intersection(ofVars, relevantVariables).size();
  }

  /**
   * This function iterates over all edges of the cfa, collects all variables
   * and orders them into different sets, i.e. nonBoolean and nonIntEuqalNumber.
   */
  private void collectVars(CFA cfa) throws UnrecognizedCCodeException {
    Collection<CFANode> nodes = cfa.getAllNodes();
    for (CFANode node : nodes) {
      for (CFAEdge edge : leavingEdges(node)) {
        handleEdge(edge, cfa);
      }
    }
  }

  /**
   * This method extracts all variables (i.e., their qualified name), that occur in an assumption.
   */
  private Multiset<String> extractAssumedVariables(Collection<CFANode> nodes) {
    Multiset<String> assumeVariables = HashMultiset.create();

    for (CFANode node : nodes) {
      for (CAssumeEdge edge : Iterables.filter(leavingEdges(node), CAssumeEdge.class)) {
        for (CIdExpression identifier : edge.getExpression()
            .accept(new CIdExpressionCollectingVisitor())) {
          assumeVariables.add(identifier.getDeclaration().getQualifiedName());
        }
      }
    }

    return assumeVariables;
  }

  /**
   * This method extracts all variables (i.e., their qualified name), that occur
   * as left-hand side in an assignment.
   */
  private Multiset<String> extractAssignedVariables(Collection<CFANode> nodes) {
    Multiset<String> assignedVariables = HashMultiset.create();

    for (CFANode node : nodes) {
      for (CFAEdge leavingEdge : leavingEdges(node)) {
        Set<CFAEdge> edges = new HashSet<>(Collections.singleton(leavingEdge));

        if (leavingEdge.getEdgeType() == CFAEdgeType.MultiEdge) {
          edges.addAll(((MultiEdge) leavingEdge).getEdges());
        }

        for (AStatementEdge edge : Iterables.filter(edges, AStatementEdge.class)) {
          if (!(edge.getStatement() instanceof CAssignment)) {
            continue;
          }

          CAssignment assignment = (CAssignment) edge.getStatement();
          for (CIdExpression id : assignment.getLeftHandSide()
              .accept(new CIdExpressionCollectingVisitor())) {
            assignedVariables.add(id.getDeclaration().getQualifiedName());
          }
        }
      }
    }

    return assignedVariables;
  }

  private void propagateRelevancy() {
    // Propagate relevant variables from assumes and assignments to pointer dereferences to
    // other variables up to a fix-point (actually as the direction of dependency doesn't matter
    // it's just a BFS)
    Queue<VariableOrField> queue =
        new ArrayDeque<>(relevantVariables.size() + relevantFields.size());
    for (final String relevantVariable : relevantVariables) {
      queue.add(VariableOrField.newVariable(relevantVariable));
    }
    for (final Map.Entry<CCompositeType, String> relevantField : relevantFields.entries()) {
      queue.add(VariableOrField.newField(relevantField.getKey(), relevantField.getValue()));
    }
    while (!queue.isEmpty()) {
      final VariableOrField relevantVariableOrField = queue.poll();
      for (VariableOrField variableOrField : assignments.get(relevantVariableOrField)) {
        final VariableOrField.Variable variable = variableOrField.asVariable();
        final VariableOrField.Field field = variableOrField.asField();
        assert variable != null || field != null
            : "Sum type match failure: neither variable nor field!";
        if (variable != null && !relevantVariables.contains(variable.getScopedName())) {
          relevantVariables.add(variable.getScopedName());
          queue.add(variable);
        } else if (field != null && !relevantFields
            .containsEntry(field.getCompositeType(), field.getName())) {
          relevantFields.put(field.getCompositeType(), field.getName());
          queue.add(field);
        }
      }
    }
  }

  private static CCompositeType getCanonicalFieldOwnerType(CFieldReference fieldReference) {
    CType fieldOwnerType = fieldReference.getFieldOwner().getExpressionType().getCanonicalType();

    while (fieldOwnerType instanceof CArrayType ||
        fieldOwnerType instanceof CPointerType) {
      if (fieldOwnerType instanceof CArrayType) {
        fieldOwnerType = ((CArrayType) fieldOwnerType).getType().getCanonicalType();
      } else if (fieldOwnerType instanceof CPointerType) {
        fieldOwnerType = ((CPointerType) fieldOwnerType).getType().getCanonicalType();
      } else {
        try {
          throw new Exception("can not handle this type!");
        } catch (Exception pE) {
          pE.printStackTrace();
        }
      }
    }

    assert fieldOwnerType instanceof CCompositeType
        : "Field owner should have composite type, but the field-owner type of expression "
        + fieldReference
        + " in " + fieldReference.getFileLocation()
        + " is " + fieldOwnerType + ", which is a " + fieldOwnerType.getClass().getSimpleName()
        + ".";
    final CCompositeType compositeType = (CCompositeType) fieldOwnerType;
    // Currently we don't pay attention to possible const and volatile modifiers
    if (compositeType.isConst() || compositeType.isVolatile()) {
      return new CCompositeType(false,
          false,
          compositeType.getKind(),
          compositeType.getMembers(),
          compositeType.getName(),
          compositeType.getOrigName());
    } else {
      return compositeType;
    }
  }

  /**
   * switch to edgeType and handle all expressions, that could be part of the edge.
   */
  private void handleEdge(CFAEdge edge, CFA cfa) throws UnrecognizedCCodeException {
    switch (edge.getEdgeType()) {

      case AssumeEdge: {
        CExpression exp = ((CAssumeEdge) edge).getExpression();
        CFANode pre = edge.getPredecessor();

        VariablesCollectingVisitor dcv = new VariablesCollectingVisitor(pre);
        Set<String> vars = exp.accept(dcv);
        if (vars != null) {
          allVars.addAll(vars);
          dependencies.addAll(vars, dcv.getValues(), edge, 0);
        }

        exp.accept(new BoolCollectingVisitor(pre));
        exp.accept(new IntEqualCollectingVisitor(pre));
        exp.accept(new IntAddCollectingVisitor(pre));

        exp.accept(new CollectingRHSVisitor(null));
        break;
      }

      case DeclarationEdge: {
        handleDeclarationEdge((CDeclarationEdge) edge);
        break;
      }

      case StatementEdge: {
        final CStatement statement = ((CStatementEdge) edge).getStatement();

        // normal assignment of variable, rightHandSide can be expression or (external) functioncall
        if (statement instanceof CAssignment) {
          handleAssignment(edge, (CAssignment) statement, cfa);

          // pure external functioncall
        } else if (statement instanceof CFunctionCallStatement) {
          handleExternalFunctionCall(edge, ((CFunctionCallStatement) statement).
              getFunctionCallExpression().getParameterExpressions());

          ((CFunctionCallStatement) statement).getFunctionCallExpression()
              .accept(new CollectingRHSVisitor(null));
        }

        break;
      }

      case FunctionCallEdge: {
        handleFunctionCallEdge((CFunctionCallEdge) edge);
        break;
      }

      case FunctionReturnEdge: {
        Optional<CVariableDeclaration> returnVar =
            ((CFunctionReturnEdge) edge).getFunctionEntry().getReturnVariable();
        if (returnVar.isPresent()) {
          String scopedVarName = returnVar.get().getQualifiedName();
          dependencies.addVar(scopedVarName);
          Partition partition = dependencies.getPartitionForVar(scopedVarName);
          partition.addEdge(edge, 0);
        }
        break;
      }

      case ReturnStatementEdge: {
        // this is the 'x' from 'return (x);
        // adding a new temporary FUNCTION_RETURN_VARIABLE, that is not global (-> false)
        CReturnStatementEdge returnStatement = (CReturnStatementEdge) edge;
        if (returnStatement.asAssignment().isPresent()) {
          handleAssignment(edge, returnStatement.asAssignment().get(), cfa);
        }
        break;
      }

      case MultiEdge:
        for (CFAEdge innerEdge : (MultiEdge) edge) {
          handleEdge(innerEdge, cfa);
        }
        break;

      case BlankEdge:
      case CallToReturnEdge:
        // other cases are not interesting
        break;

      default:
        throw new UnrecognizedCCodeException("Unknown edgeType: " + edge.getEdgeType(), edge);
    }
  }

  /**
   * This function handles a declaration with an optional initializer.
   * Only simple types are handled.
   */
  private void handleDeclarationEdge(final CDeclarationEdge edge)
      throws UnrecognizedCCodeException {
    CDeclaration declaration = edge.getDeclaration();
    if (!(declaration instanceof CVariableDeclaration)) {
      return;
    }

    CVariableDeclaration vdecl = (CVariableDeclaration) declaration;
    String varName = vdecl.getQualifiedName();
    allVars.add(varName);

    // "connect" the edge with its partition
    Set<String> var = Sets.newHashSetWithExpectedSize(1);
    var.add(varName);
    dependencies.addAll(var, new HashSet<BigInteger>(), edge, 0);

    // only simple types (int, long) are allowed for booleans, ...
    if (!(vdecl.getType() instanceof CSimpleType)) {
      nonIntBoolVars.add(varName);
      nonIntEqVars.add(varName);
      nonIntAddVars.add(varName);
    }

    final CInitializer initializer = vdecl.getInitializer();
    List<CExpressionAssignmentStatement> l = CInitializers.convertToAssignments(vdecl, edge);

    for (CExpressionAssignmentStatement init : l) {
      final CLeftHandSide lhsExpression = init.getLeftHandSide();
      final VariableOrField lhs = lhsExpression.accept(collectingLHSVisitor);

      final CExpression rhs = init.getRightHandSide();
      rhs.accept(new CollectingRHSVisitor(lhs));
    }

    if ((initializer == null) || !(initializer instanceof CInitializerExpression)) {
      return;
    }

    CExpression exp = ((CInitializerExpression) initializer).getExpression();
    if (exp == null) {
      return;
    }

    handleExpression(edge, exp, varName, VariableOrField.newVariable(varName));
  }

  /**
   * This function handles normal assignments of vars.
   */
  private void handleAssignment(
      final CFAEdge edge, final CAssignment assignment,
      final CFA cfa) throws UnrecognizedCCodeException {
    CRightHandSide rhs = assignment.getRightHandSide();
    CExpression lhs = assignment.getLeftHandSide();
    String function = isGlobal(lhs) ? null : edge.getPredecessor().getFunctionName();
    String varName = scopeVar(function, lhs.toASTString());

    // only simple types (int, long) are allowed for booleans, ...
    if (!(lhs instanceof CIdExpression && lhs.getExpressionType() instanceof CSimpleType)) {
      nonIntBoolVars.add(varName);
      nonIntEqVars.add(varName);
      nonIntAddVars.add(varName);
    }

    dependencies.addVar(varName);

    final VariableOrField lhsVariableOrField = lhs.accept(collectingLHSVisitor);

    if (rhs instanceof CExpression) {
      handleExpression(edge, ((CExpression) rhs), varName, lhsVariableOrField);

    } else if (rhs instanceof CFunctionCallExpression) {
      // use FUNCTION_RETURN_VARIABLE for RIGHT SIDE
      CFunctionCallExpression func = (CFunctionCallExpression) rhs;
      String functionName = func.getFunctionNameExpression().toASTString(); // TODO correct?

      if (cfa.getAllFunctionNames().contains(functionName)) {
        Optional<? extends AVariableDeclaration> returnVariable =
            cfa.getFunctionHead(functionName).getReturnVariable();
        if (!returnVariable.isPresent()) {
          throw new UnrecognizedCCodeException(
              "Void function " + functionName + " used in assignment", edge, assignment);
        }
        String returnVar = returnVariable.get().getQualifiedName();
        allVars.add(returnVar);
        allVars.add(varName);
        dependencies.add(returnVar, varName);

      } else {
        // external function
        Partition partition = dependencies.getPartitionForVar(varName);
        partition.addEdge(edge, -1); // negative value, because all positives are used for params
      }

      rhs.accept(new CollectingRHSVisitor(lhsVariableOrField));

      handleExternalFunctionCall(edge, func.getParameterExpressions());

    } else {
      throw new UnrecognizedCCodeException("unhandled assignment", edge, assignment);
    }
  }

  /**
   * This function handles the call of an external function
   * without an assignment of the result.
   * example: "printf("%d", output);" or "assert(exp);"
   */
  private void handleExternalFunctionCall(final CFAEdge edge, final List<CExpression> params) {
    for (int i = 0; i < params.size(); i++) {
      final CExpression param = params.get(i);

      /* special case: external functioncall with possible side-effect!
       * this is the only statement, where a pointer-operation is allowed
       * and the var can be boolean, intEqual or intAdd,
       * because we know, the variable can have a random (unknown) value after the functioncall.
       * example: "scanf("%d", &input);" */
      if (param instanceof CUnaryExpression &&
          UnaryOperator.AMPER == ((CUnaryExpression) param).getOperator() &&
          ((CUnaryExpression) param).getOperand() instanceof CIdExpression) {
        final CIdExpression id = (CIdExpression) ((CUnaryExpression) param).getOperand();
        final String varName = id.getDeclaration().getQualifiedName();

        dependencies.addVar(varName);
        Partition partition = dependencies.getPartitionForVar(varName);
        partition.addEdge(edge, i);

      } else {
        // "printf("%d", output);" or "assert(exp);"
        // TODO do we need the edge? ignore it?

        CFANode pre = edge.getPredecessor();
        VariablesCollectingVisitor dcv = new VariablesCollectingVisitor(pre);
        Set<String> vars = param.accept(dcv);
        if (vars != null) {
          allVars.addAll(vars);
          dependencies.addAll(vars, dcv.getValues(), edge, i);
        }

        param.accept(new BoolCollectingVisitor(pre));
        param.accept(new IntEqualCollectingVisitor(pre));
        param.accept(new IntAddCollectingVisitor(pre));
      }
    }
  }

  /**
   * This function puts each param in same partition than its arg.
   * If there the functionresult is assigned, it is also handled.
   */
  private void handleFunctionCallEdge(CFunctionCallEdge edge) {

    // overtake arguments from last functioncall into function,
    // get args from functioncall and make them equal with params from functionstart
    final List<CExpression> args = edge.getArguments();
    final List<CParameterDeclaration> params = edge.getSuccessor().getFunctionParameters();

    // functions can have more args than params used in the call
    assert args.size() >= params.size();

    for (int i = 0; i < params.size(); i++) {
      CParameterDeclaration param = params.get(i);
      String varName = param.getQualifiedName();

      // only simple types (int, long) are allowed for booleans, ...
      if (!(param.getType() instanceof CSimpleType)) {
        nonIntBoolVars.add(varName);
        nonIntEqVars.add(varName);
        nonIntAddVars.add(varName);
      }

      // build name for param and evaluate it
      // this variable is not global (->false)
      handleExpression(edge, args.get(i), varName, i, VariableOrField.newVariable(varName));
    }

    // create dependency for functionreturn
    CFunctionSummaryEdge func = edge.getSummaryEdge();
    CFunctionCall statement = func.getExpression();
    Optional<CVariableDeclaration> returnVar = edge.getSuccessor().getReturnVariable();
    if (returnVar.isPresent()) {
      String scopedRetVal = returnVar.get().getQualifiedName();
      if (statement instanceof CFunctionCallAssignmentStatement) {
        // a=f();
        CFunctionCallAssignmentStatement call = (CFunctionCallAssignmentStatement) statement;
        CExpression lhs = call.getLeftHandSide();
        String function = isGlobal(lhs) ? null : edge.getPredecessor().getFunctionName();
        String varName = scopeVar(function, lhs.toASTString());
        allVars.add(scopedRetVal);
        allVars.add(varName);
        dependencies.add(scopedRetVal, varName);

        final VariableOrField lhsVariableOrField = lhs.accept(collectingLHSVisitor);

        assignments.put(lhsVariableOrField, VariableOrField.newVariable(scopedRetVal));

      } else if (statement instanceof CFunctionCallStatement) {
        // f(); without assignment
        // next line is not necessary, but we do it for completeness, TODO correct?
        dependencies.addVar(scopedRetVal);
      }
    }
  }

  /**
   * evaluates an expression and adds containing vars to the sets.
   */
  private void handleExpression(
      CFAEdge edge,
      CExpression exp,
      String varName,
      final VariableOrField lhs) {
    handleExpression(edge, exp, varName, 0, lhs);
  }

  /**
   * evaluates an expression and adds containing vars to the sets.
   * the id is the position of the expression in the edge,
   * it is 0 for all edges except a FuntionCallEdge.
   */
  private void handleExpression(
      CFAEdge edge,
      CExpression exp,
      String varName,
      int id,
      final VariableOrField lhs) {
    CFANode pre = edge.getPredecessor();

    VariablesCollectingVisitor dcv = new VariablesCollectingVisitor(pre);
    Set<String> vars = exp.accept(dcv);
    if (vars == null) {
      vars = Sets.newHashSetWithExpectedSize(1);
    }

    vars.add(varName);
    allVars.addAll(vars);
    dependencies.addAll(vars, dcv.getValues(), edge, id);

    BoolCollectingVisitor bcv = new BoolCollectingVisitor(pre);
    Set<String> possibleBoolean = exp.accept(bcv);
    handleResult(varName, possibleBoolean, nonIntBoolVars);

    IntEqualCollectingVisitor ncv = new IntEqualCollectingVisitor(pre);
    Set<String> possibleIntEqualVars = exp.accept(ncv);
    handleResult(varName, possibleIntEqualVars, nonIntEqVars);

    IntAddCollectingVisitor icv = new IntAddCollectingVisitor(pre);
    Set<String> possibleIntAddVars = exp.accept(icv);
    handleResult(varName, possibleIntAddVars, nonIntAddVars);

    exp.accept(new CollectingRHSVisitor(lhs));
  }

  /**
   * adds the variable to notPossibleVars, if possibleVars is null.
   */
  private void handleResult(
      String varName,
      Collection<String> possibleVars,
      Collection<String> notPossibleVars) {
    if (possibleVars == null) {
      notPossibleVars.add(varName);
    }
  }

  private static String scopeVar(@Nullable final String function, final String var) {
    return (function == null) ? (var) : (function + SCOPE_SEPARATOR + var);
  }

  private static boolean isGlobal(CExpression exp) {
    if (exp instanceof CIdExpression) {
      CSimpleDeclaration decl = ((CIdExpression) exp).getDeclaration();
      if (decl instanceof CDeclaration) {
        return ((CDeclaration) decl).isGlobal();
      }
    }
    return false;
  }

  /**
   * returns the value of a (nested) IntegerLiteralExpression
   * or null for everything else.
   */
  public static BigInteger getNumber(CExpression exp) {
    if (exp instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression) exp).getValue();

    } else if (exp instanceof CUnaryExpression) {
      CUnaryExpression unExp = (CUnaryExpression) exp;
      BigInteger value = getNumber(unExp.getOperand());
      if (value == null) {
        return null;
      }
      switch (unExp.getOperator()) {
        case MINUS:
          return value.negate();
        default:
          return null;
      }

    } else if (exp instanceof CCastExpression) {
      return getNumber(((CCastExpression) exp).getOperand());

    } else {
      return null;
    }
  }

  /**
   * returns true, if the expression contains a casted binaryExpression.
   */
  private static boolean isNestedBinaryExp(CExpression exp) {
    if (exp instanceof CBinaryExpression) {
      return true;

    } else if (exp instanceof CCastExpression) {
      return isNestedBinaryExp(((CCastExpression) exp).getOperand());

    } else {
      return false;
    }
  }

  /**
   * This Visitor evaluates an Expression. It collects all variables.
   * a visit of IdExpression or CFieldReference returns a collection containing the varName,
   * other visits return the inner visit-results.
   * The Visitor also collects all numbers used in the expression.
   */
  private static class VariablesCollectingVisitor implements
                                                  CExpressionVisitor<Set<String>, RuntimeException> {

    private CFANode predecessor;
    private Set<BigInteger> values = new TreeSet<>();

    public VariablesCollectingVisitor(CFANode pre) {
      this.predecessor = pre;
    }

    public Set<BigInteger> getValues() {
      return values;
    }

    @Override
    public Set<String> visit(CArraySubscriptExpression exp) {
      return null;
    }

    @Override
    public Set<String> visit(CBinaryExpression exp) {

      // for numeral values
      BigInteger val1 = getNumber(exp.getOperand1());
      Set<String> operand1;
      if (val1 == null) {
        operand1 = exp.getOperand1().accept(this);
      } else {
        values.add(val1);
        operand1 = null;
      }

      // for numeral values
      BigInteger val2 = getNumber(exp.getOperand2());
      Set<String> operand2;
      if (val2 == null) {
        operand2 = exp.getOperand2().accept(this);
      } else {
        values.add(val2);
        operand2 = null;
      }

      // handle vars from operands
      if (operand1 == null) {
        return operand2;
      } else if (operand2 == null) {
        return operand1;
      } else {
        operand1.addAll(operand2);
        return operand1;
      }
    }

    @Override
    public Set<String> visit(CCastExpression exp) {
      BigInteger val = getNumber(exp.getOperand());
      if (val == null) {
        return exp.getOperand().accept(this);
      } else {
        values.add(val);
        return null;
      }
    }

    @Override
    public Set<String> visit(CComplexCastExpression exp) {
      // TODO complex numbers are not supported for evaluation right now, this
      // way of handling the variables my be wrong

      BigInteger val = getNumber(exp.getOperand());
      if (val == null) {
        return exp.getOperand().accept(this);
      } else {
        values.add(val);
        return null;
      }
    }

    @Override
    public Set<String> visit(CFieldReference exp) {
      String varName = exp.toASTString(); // TODO "(*p).x" vs "p->x"
      String function = isGlobal(exp) ? "" : predecessor.getFunctionName();
      Set<String> ret = Sets.newHashSetWithExpectedSize(1);
      ret.add(scopeVar(function, varName));
      return ret;
    }

    @Override
    public Set<String> visit(CIdExpression exp) {
      Set<String> ret = Sets.newHashSetWithExpectedSize(1);
      ret.add(exp.getDeclaration().getQualifiedName());
      return ret;
    }

    @Override
    public Set<String> visit(CCharLiteralExpression exp) {
      return null;
    }

    @Override
    public Set<String> visit(CFloatLiteralExpression exp) {
      return null;
    }

    @Override
    public Set<String> visit(CImaginaryLiteralExpression exp) {
      return exp.getValue().accept(this);
    }

    @Override
    public Set<String> visit(CIntegerLiteralExpression exp) {
      values.add(exp.getValue());
      return null;
    }

    @Override
    public Set<String> visit(CStringLiteralExpression exp) {
      return null;
    }

    @Override
    public Set<String> visit(CTypeIdExpression exp) {
      return null;
    }

    @Override
    public Set<String> visit(CUnaryExpression exp) {
      BigInteger val = getNumber(exp);
      if (val == null) {
        return exp.getOperand().accept(this);
      } else {
        values.add(val);
        return null;
      }
    }

    @Override
    public Set<String> visit(CPointerExpression exp) {
      BigInteger val = getNumber(exp);
      if (val == null) {
        return exp.getOperand().accept(this);
      } else {
        values.add(val);
        return null;
      }
    }

    @Override
    public Set<String> visit(CAddressOfLabelExpression exp) {
      return null;
    }
  }


  /**
   * This class stores dependencies between variables.
   * It sorts vars into partitions.
   * Dependent vars are in the same partition. Partitions are independent.
   */
  private static class Dependencies {

    /**
     * partitions, each of them contains vars
     */
    private final List<Partition> partitions = Lists.newArrayList();

    /**
     * map to get partition of a var
     */
    private final Map<String, Partition> varToPartition = Maps.newHashMap();

    /**
     * table to get a partition for a edge.
     */
    private final Map<Pair<CFAEdge, Integer>, Partition> edgeToPartition = Maps.newHashMap();

    /**
     * This function returns a partition containing all vars,
     * that are dependent with the given variable.
     */
    public Partition getPartitionForVar(String var) {
      return varToPartition.get(var);
    }

    /**
     * This function creates a dependency between function1::var1 and function2::var2.
     */
    public void add(String var1, String var2) {

      // if both vars exists in some dependencies,
      // either ignore them or merge their partitions
      Partition partition1 = varToPartition.get(var1);
      Partition partition2 = varToPartition.get(var2);
      if (partition1 != null && partition2 != null) {

        // swap partitions, we create partitions in the order they are used
        if (partitions.lastIndexOf(partition1) > partitions.lastIndexOf(partition2)) {
          Partition tmp = partition2;
          partition2 = partition1;
          partition1 = tmp;
        }

        if (!partition1.equals(partition2)) {
          partition1.merge(partition2);
          partitions.remove(partition2);
        }

        // if only left side of dependency exists, add right side into same partition
      } else if (partition1 != null) {
        partition1.add(var2);

        // if only right side of dependency exists, add left side into same partition
      } else if (partition2 != null) {
        partition2.add(var1);

        // if none side is in any existing partition, create new partition
      } else {
        Partition partition = new Partition(varToPartition, edgeToPartition);
        partition.add(var1);
        partition.add(var2);
        partitions.add(partition);
      }
    }

    /**
     * This function adds a group of vars to exactly one partition.
     * The values are stored in the partition.
     * The partition is "connected" with the expression.
     *
     * @param vars   group of variables tobe added
     * @param values numbers, with are used in an expression together with the variables
     * @param edge   where is the expression
     * @param index  if an edge has several expressions, this index is the position ofthe
     *               expression
     */
    public void addAll(
        Collection<String> vars, Set<BigInteger> values,
        CFAEdge edge, int index) {
      if (vars == null || vars.isEmpty()) {
        return;
      }

      Iterator<String> iter = vars.iterator();

      // we use same varName for all other vars --> dependency
      String var = iter.next();

      // first add one single var
      addVar(var);

      // then add all other vars, they are dependent from the first var
      while (iter.hasNext()) {
        add(var, iter.next());
      }

      Partition partition = getPartitionForVar(var);
      partition.addValues(values);
      partition.addEdge(edge, index);
    }

    /**
     * This function adds one single variable to the partitions.
     * This is the only method to create a partition with only one element.
     */
    public void addVar(String var) {

      // if var exists, we can ignore it, otherwise create new partition for var
      if (!varToPartition.containsKey(var)) {
        Partition partition = new Partition(varToPartition, edgeToPartition);
        partition.add(var);
        partitions.add(partition);
      }
    }

    /**
     * This function adds all depending vars to the set, if necessary.
     * If A depends on B and A is part of the set, B is added to the set, and vice versa.
     * Example: If A is not boolean, B is not boolean.
     */
    public void solve(final Set<String> vars) {
      for (Partition partition : partitions) {

        // is at least one var from the partition part of vars
        if (!Sets.intersection(partition.getVars(), vars).isEmpty()) {
          // add all dependend vars to vars
          vars.addAll(partition.getVars());
        }
      }
    }

    @Override
    public String toString() {
      StringBuilder str = new StringBuilder("[");
      Joiner.on(",\n").appendTo(str, partitions);
      str.append("]\n\n");

      //      for (Pair<CFAEdge, Integer> edge : edgeToPartition.keySet()) {
      //        str.append(edge.getFirst().getRawStatement() + " :: "
      //            + edge.getSecond() + " --> " + edgeToPartition.get(edge) + "\n");
      //      }
      return str.toString();
    }
  }


  /**
   * This Visitor evaluates an Expression. It also collects all variables.
   * Each visit-function returns
   * - null, if the expression is not boolean
   * - a collection, if the expression is boolean.
   * The collection contains all boolean vars.
   */
  private class BoolCollectingVisitor extends VariablesCollectingVisitor {

    public BoolCollectingVisitor(CFANode pre) {
      super(pre);
    }

    @Override
    public Set<String> visit(CFieldReference exp) {
      nonIntBoolVars.addAll(super.visit(exp));
      return null;
    }

    @Override
    public Set<String> visit(CBinaryExpression exp) {
      Set<String> operand1 = exp.getOperand1().accept(this);
      Set<String> operand2 = exp.getOperand2().accept(this);

      if (operand1 == null || operand2 == null) { // a+123 --> a is not boolean
        if (operand1 != null) {
          nonIntBoolVars.addAll(operand1);
        }
        if (operand2 != null) {
          nonIntBoolVars.addAll(operand2);
        }
        return null;
      }

      switch (exp.getOperator()) {

        case EQUALS:
        case NOT_EQUALS: // ==, != work with boolean operands
          if (operand1.isEmpty() || operand2.isEmpty()) {
            // one operand is Zero (or One, if allowed)
            operand1.addAll(operand2);
            return operand1;
          }
          // We compare 2 variables. There is no guarantee, that they are boolean!
          // Example: (a!=b) && (b!=c) && (c!=a)
          // -> FALSE for boolean, but TRUE for {1,2,3}

          //$FALL-THROUGH$

        default: // +-*/ --> no boolean operators, a+b --> a and b are not boolean
          nonIntBoolVars.addAll(operand1);
          nonIntBoolVars.addAll(operand2);
          return null;
      }
    }

    @Override
    public Set<String> visit(CIntegerLiteralExpression exp) {
      BigInteger value = exp.getValue();
      if (BigInteger.ZERO.equals(value)
          || (allowOneAsBooleanValue && BigInteger.ONE.equals(value))) {
        return new HashSet<>(0);
      } else {
        return null;
      }
    }

    @Override
    public Set<String> visit(CUnaryExpression exp) {
      Set<String> inner = exp.getOperand().accept(this);

      if (inner == null) {
        return null;
      } else { // PLUS, MINUS, etc --> not boolean
        nonIntBoolVars.addAll(inner);
        return null;
      }
    }

    @Override
    public Set<String> visit(CPointerExpression exp) {
      Set<String> inner = exp.getOperand().accept(this);

      if (inner == null) {
        return null;
      } else {
        nonIntBoolVars.addAll(inner);
        return null;
      }
    }
  }


  /**
   * This Visitor evaluates an Expression.
   * Each visit-function returns
   * - null, if the expression contains calculations
   * - a collection, if the expression is a number, unaryExp, == or !=
   */
  private class IntEqualCollectingVisitor extends VariablesCollectingVisitor {

    public IntEqualCollectingVisitor(CFANode pre) {
      super(pre);
    }

    @Override
    public Set<String> visit(CCastExpression exp) {
      BigInteger val = getNumber(exp.getOperand());
      if (val == null) {
        return exp.getOperand().accept(this);
      } else {
        return new HashSet<>(0);
      }
    }

    @Override
    public Set<String> visit(CFieldReference exp) {
      nonIntEqVars.addAll(super.visit(exp));
      return null;
    }

    @Override
    public Set<String> visit(CBinaryExpression exp) {

      // for numeral values
      BigInteger val1 = getNumber(exp.getOperand1());
      Set<String> operand1;
      if (val1 == null) {
        operand1 = exp.getOperand1().accept(this);
      } else {
        operand1 = new HashSet<>(0);
      }

      // for numeral values
      BigInteger val2 = getNumber(exp.getOperand2());
      Set<String> operand2;
      if (val2 == null) {
        operand2 = exp.getOperand2().accept(this);
      } else {
        operand2 = new HashSet<>(0);
      }

      // handle vars from operands
      if (operand1 == null || operand2 == null) { // a+0.2 --> no simple number
        if (operand1 != null) {
          nonIntEqVars.addAll(operand1);
        }
        if (operand2 != null) {
          nonIntEqVars.addAll(operand2);
        }
        return null;
      }

      switch (exp.getOperator()) {

        case EQUALS:
        case NOT_EQUALS: // ==, != work with numbers
          operand1.addAll(operand2);
          return operand1;

        default: // +-*/ --> no simple operators
          nonIntEqVars.addAll(operand1);
          nonIntEqVars.addAll(operand2);
          return null;
      }
    }

    @Override
    public Set<String> visit(CIntegerLiteralExpression exp) {
      return new HashSet<>(0);
    }

    @Override
    public Set<String> visit(CUnaryExpression exp) {

      // if exp is numeral
      BigInteger val = getNumber(exp);
      if (val != null) {
        return new HashSet<>(0);
      }

      // if exp is binary expression
      Set<String> inner = exp.getOperand().accept(this);
      if (isNestedBinaryExp(exp)) {
        return inner;
      }

      if (inner != null) {
        nonIntEqVars.addAll(inner);
      }
      return null;
    }

    @Override
    public Set<String> visit(CPointerExpression exp) {

      // if exp is numeral
      BigInteger val = getNumber(exp);
      if (val != null) {
        return new HashSet<>(0);
      }

      // if exp is binary expression
      Set<String> inner = exp.getOperand().accept(this);
      if (isNestedBinaryExp(exp)) {
        return inner;
      }

      // if exp is unknown
      if (inner == null) {
        return null;
      }

      nonIntEqVars.addAll(inner);
      return null;
    }
  }


  /**
   * This Visitor evaluates an Expression.
   * Each visit-function returns
   * - a collection, if the expression is a var or a simple mathematical
   * calculation (add, sub, <, >, <=, >=, ==, !=, !),
   * - else null
   */
  private class IntAddCollectingVisitor extends VariablesCollectingVisitor {

    public IntAddCollectingVisitor(CFANode pre) {
      super(pre);
    }

    @Override
    public Set<String> visit(CCastExpression exp) {
      return exp.getOperand().accept(this);
    }

    @Override
    public Set<String> visit(CFieldReference exp) {
      nonIntAddVars.addAll(super.visit(exp));
      return null;
    }

    @Override
    public Set<String> visit(CBinaryExpression exp) {
      Set<String> operand1 = exp.getOperand1().accept(this);
      Set<String> operand2 = exp.getOperand2().accept(this);

      if (operand1 == null || operand2 == null) { // a+0.2 --> no simple number
        if (operand1 != null) {
          nonIntAddVars.addAll(operand1);
        }
        if (operand2 != null) {
          nonIntAddVars.addAll(operand2);
        }
        return null;
      }

      switch (exp.getOperator()) {

        case PLUS:
        case MINUS:
        case LESS_THAN:
        case LESS_EQUAL:
        case GREATER_THAN:
        case GREATER_EQUAL:
        case EQUALS:
        case NOT_EQUALS:
        case BINARY_AND:
        case BINARY_XOR:
        case BINARY_OR:
          // this calculations work with all numbers
          operand1.addAll(operand2);
          return operand1;

        default: // *, /, %, shift --> no simple calculations
          nonIntAddVars.addAll(operand1);
          nonIntAddVars.addAll(operand2);
          return null;
      }
    }

    @Override
    public Set<String> visit(CIntegerLiteralExpression exp) {
      return new HashSet<>(0);
    }

    @Override
    public Set<String> visit(CUnaryExpression exp) {
      Set<String> inner = exp.getOperand().accept(this);
      if (inner == null) {
        return null;
      }
      if (exp.getOperator() == UnaryOperator.MINUS) {
        return inner;
      }

      // *, ~, etc --> not simple
      nonIntAddVars.addAll(inner);
      return null;
    }

    @Override
    public Set<String> visit(CPointerExpression exp) {
      Set<String> inner = exp.getOperand().accept(this);
      if (inner == null) {
        return null;
      }

      nonIntAddVars.addAll(inner);
      return null;
    }
  }

  private class CollectingLHSVisitor
      extends DefaultCExpressionVisitor<VariableOrField, RuntimeException> {

    @Override
    public VariableOrField visit(final CArraySubscriptExpression e) {
      final VariableOrField result = e.getArrayExpression().accept(this);
      e.getSubscriptExpression().accept(new CollectingRHSVisitor(result));
      return result;
    }

    @Override
    public VariableOrField visit(final CFieldReference e) {
      final CCompositeType compositeType = getCanonicalFieldOwnerType(e);
      final VariableOrField result = VariableOrField.newField(compositeType, e.getFieldName());
      if (e.isPointerDereference()) {
        e.getFieldOwner().accept(new CollectingRHSVisitor(result));
      } else {
        e.getFieldOwner().accept(this);
      }
      return result;
    }

    @Override
    public VariableOrField visit(final CPointerExpression e) {
      e.getOperand().accept(new CollectingRHSVisitor(null));
      return null;
    }

    @Override
    public VariableOrField visit(final CComplexCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public VariableOrField visit(final CCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public VariableOrField visit(final CIdExpression e) {
      return VariableOrField.newVariable(e.getDeclaration().getQualifiedName());
    }

    @Override
    protected VariableOrField visitDefault(final CExpression e) {
      if (e instanceof CBinaryExpression) {
        CExpression operand1 = ((CBinaryExpression) e).getOperand1();
        CExpression operand2 = ((CBinaryExpression) e).getOperand2();
        if (operand2 instanceof CLiteralExpression) {
          return operand1.accept(this);
        } else if (operand1 instanceof CLiteralExpression) {
          return operand2.accept(this);
        }
        return null;
      } else if (e instanceof CUnaryExpression) {
        if (((CUnaryExpression) e).getOperator() == UnaryOperator.AMPER) {
          return ((CUnaryExpression) e).getOperand().accept(this);
        }
      } else if (e instanceof CLiteralExpression) {
        return null;
      }
      throw new IllegalArgumentException(
          e.getFileLocation() + " " + e.toString() + "The expression "
              + "should not occur in the left hand side");
    }
  }

  private void addVariableOrField(final @Nullable VariableOrField lhs, final VariableOrField rhs) {
    if (lhs != null) {
      assignments.put(lhs, rhs);
    } else {
      final VariableOrField.Variable variable = rhs.asVariable();
      final VariableOrField.Field field = rhs.asField();
      if (variable != null) {
        relevantVariables.add(variable.getScopedName());
      } else {
        relevantFields.put(field.getCompositeType(), field.getName());
      }
    }
  }

  private class CollectingRHSVisitor extends DefaultCExpressionVisitor<Void, RuntimeException>
      implements CRightHandSideVisitor<Void, RuntimeException> {

    private final
    @Nullable
    VariableOrField lhs;
    private boolean addressed = false;

    CollectingRHSVisitor(@Nullable VariableOrField pLhs) {
      lhs = pLhs;
    }

    @Override
    public Void visit(final CArraySubscriptExpression e) {
      CollectingRHSVisitor arrayExprVisitor = new CollectingRHSVisitor(null);
      arrayExprVisitor.addressed = true;
      e.getArrayExpression().accept(arrayExprVisitor);
      return e.getSubscriptExpression().accept(this);
    }

    @Override
    public Void visit(final CFieldReference e) {
      final CCompositeType compositeType = getCanonicalFieldOwnerType(e);
      addVariableOrField(lhs, VariableOrField.newField(compositeType, e.getFieldName()));
      return e.getFieldOwner().accept(this);
    }

    @Override
    public Void visit(final CBinaryExpression e) {
      e.getOperand1().accept(this);
      return e.getOperand2().accept(this);
    }

    @Override
    public Void visit(final CUnaryExpression e) {
      if (e.getOperator() != UnaryOperator.AMPER) {
        return e.getOperand().accept(this);
      } else {
        addressed = true;
        e.getOperand().accept(this);
        addressed = false;
        return null;
      }
    }

    @Override
    public Void visit(final CPointerExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public Void visit(final CComplexCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public Void visit(final CCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public Void visit(final CIdExpression e) {
      final VariableOrField.Variable variable =
          VariableOrField.newVariable(e.getDeclaration().getQualifiedName());
      addVariableOrField(lhs, variable);
      if (addressed) {
        addressedVariables.add(variable.getScopedName());
      }
      return null;
    }

    @Override
    public Void visit(CFunctionCallExpression e) {
      for (CExpression param : e.getParameterExpressions()) {
        param.accept(this);
      }
      return null;
    }

    @Override
    protected Void visitDefault(final CExpression e) {
      return null;
    }
  }

  private static class VariableOrField {
    private static class Variable extends VariableOrField {
      private Variable(final @Nonnull String scopedName) {
        this.scopedName = scopedName;
      }

      public
      @Nonnull
      String getScopedName() {
        return scopedName;
      }

      @Override
      public String toString() {
        return getScopedName();
      }

      @Override
      public boolean equals(final Object o) {
        if (o == this) {
          return true;
        } else if (!(o instanceof Variable)) {
          return false;
        } else {
          final Variable other = (Variable) o;
          return this.scopedName.equals(other.scopedName);
        }
      }

      @Override
      public int hashCode() {
        return scopedName.hashCode();
      }

      private final
      @Nonnull
      String scopedName;
    }

    private static class Field extends VariableOrField {
      private Field(final CCompositeType composite, final String name) {
        this.composite = composite;
        this.name = name;
      }

      public CCompositeType getCompositeType() {
        return composite;
      }

      public String getName() {
        return name;
      }

      @Override
      public String toString() {
        return composite + SCOPE_SEPARATOR + name;
      }

      @Override
      public boolean equals(final Object o) {
        if (o == this) {
          return true;
        } else if (!(o instanceof Field)) {
          return false;
        } else {
          final Field other = (Field) o;
          return this.composite.equals(other.composite) && this.name.equals(other.name);
        }
      }

      @Override
      public int hashCode() {
        final int prime = 67;
        return prime * composite.hashCode() + name.hashCode();
      }

      private
      @Nonnull
      CCompositeType composite;
      private
      @Nonnull
      String name;
    }

    public static Variable newVariable(final String scopedName) {
      return new Variable(scopedName);
    }

    public static Field newField(
        final @Nonnull CCompositeType composite,
        final @Nonnull String name) {
      return new Field(composite, name);
    }

    public
    @Nullable
    Variable asVariable() {
      if (this instanceof Variable) {
        return (Variable) this;
      } else {
        return null;
      }
    }

    public
    @Nullable
    Field asField() {
      if (this instanceof Field) {
        return (Field) this;
      } else {
        return null;
      }
    }
  }
}
