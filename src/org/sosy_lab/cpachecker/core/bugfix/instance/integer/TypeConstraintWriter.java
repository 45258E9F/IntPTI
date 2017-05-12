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
package org.sosy_lab.cpachecker.core.bugfix.instance.integer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerTypeConstraint.IntegerTypePredicate;
import org.sosy_lab.cpachecker.util.Types;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Output type constraint as SMTLIB2 format.
 */
public final class TypeConstraintWriter {

  private String fileName;
  private Set<IntegerTypeConstraint> constraints;

  private int coverWeight;
  private int declarationCoverWeight;
  private int equalWeight;

  private final MachineModel machineModel;

  public TypeConstraintWriter(
      String pFileName, Set<IntegerTypeConstraint> pConstraints, int
      pCoverWeight, int pDeclarationCoverWeight, int pEqualWeight, MachineModel pModel) {
    fileName = pFileName;
    constraints = pConstraints;
    coverWeight = pCoverWeight;
    declarationCoverWeight = pDeclarationCoverWeight;
    equalWeight = pEqualWeight;

    // generate type relations on-the-fly according to the specified machine model
    if (typeRelation == null) {
      generateTypeRelation(pModel);
    }
    machineModel = pModel;
  }

  private void generateTypeRelation(MachineModel pMachineModel) {
    String predicateTemplate = "(define-fun P ((x!1 I) (x!2 I)) Bool %s)";
    String relationTemplate = "(ite (and (= x!1 %s) (= x!2 %s)) true %s)";
    CSimpleType typeList[] = {CNumericTypes.CHAR, CNumericTypes.SIGNED_CHAR, CNumericTypes
        .UNSIGNED_CHAR, CNumericTypes.SHORT_INT, CNumericTypes.UNSIGNED_SHORT_INT, CNumericTypes
        .INT, CNumericTypes.UNSIGNED_INT, CNumericTypes.LONG_INT, CNumericTypes
        .UNSIGNED_LONG_INT, CNumericTypes.LONG_LONG_INT, CNumericTypes.UNSIGNED_LONG_LONG_INT};
    Multimap<String, String> covers = HashMultimap.create();
    for (int i = 0; i < typeList.length; i++) {
      for (int j = i; j < typeList.length; j++) {
        if (i == j) {
          String typeStr = IntegerTypeConstraint.toTypeString(typeList[i]);
          assert (typeStr != null);
          covers.put(typeStr, typeStr);
        } else {
          String typeStr1 = IntegerTypeConstraint.toTypeString(typeList[i]);
          String typeStr2 = IntegerTypeConstraint.toTypeString(typeList[j]);
          assert (typeStr1 != null);
          assert (typeStr2 != null);
          if (Types.canHoldAllValues(typeList[i], typeList[j], pMachineModel)) {
            covers.put(typeStr1, typeStr2);
          } else if (Types.canHoldAllValues(typeList[j], typeList[i], pMachineModel)) {
            covers.put(typeStr2, typeStr1);
          }
        }
      }
    }
    String currentTemplate = null;
    for (Entry<String, String> coverPair : covers.entries()) {
      String relation = String.format(relationTemplate, coverPair.getKey(), coverPair.getValue(),
          "%s");
      if (currentTemplate == null) {
        currentTemplate = relation;
      } else {
        currentTemplate = String.format(currentTemplate, relation);
      }
    }
    if (currentTemplate != null) {
      // that means, the cover relation is not empty
      currentTemplate = String.format(currentTemplate, "(ite (= x!1 !OVERLONG!) true false)");
    } else {
      // overall, we should specify that !OVERLONG! is the largest integer type
      currentTemplate = "(ite (= x!1 !OVERLONG!) true false)";
    }
    typeRelation = String.format(predicateTemplate, currentTemplate);
  }

  /* ************** */
  /* format control */
  /* ************** */

  // we add `!` on the head and the tail of the type variable, in order to prevent name crash
  private static final String dataType = "(declare-datatypes () ((I !CHAR! !SCHAR! !UCHAR! "
      + "!SHORT! !USHORT! !INT! !UINT! !LINT! !ULINT! !LLINT! !ULLINT! !OVERLONG!)))";
  private static String typeRelation = null;
  private static final String assertHard = "(assert %s)";
  private static final String assertSoft = "(assert-soft %s :weight %d)";
  private static final String coverTemplate = "(P %s %s)";
  private static final String eqTemplate = "(= %s %s)";
  private static final String declareTemplate = "(declare-fun %s () I)";
  private static final String checkSat = "(check-sat)";
  private static final String getModel = "(get-model)";
  private static final String exit = "(exit)";
  private static final String notOverlong = "(not (= %s !OVERLONG!))";

  private Set<String> varSet = new HashSet<>();
  // declarations of variables
  private List<String> declarations = new ArrayList<>();
  // hard or soft assertions for type constraints
  private List<String> assertions = new ArrayList<>();
  // restrictions that all the variables should not have !OVERLONG! type (which is a virtual type
  // for the convenience of type derivation)
  private List<String> restrictions = new ArrayList<>();

  public void output() {
    for (IntegerTypeConstraint assertion : constraints) {
      IntegerTypePredicate predicate = assertion.getPredicate();
      String relationStr;
      String name1 = preProcess(assertion.getName1());
      String name2 = preProcess(assertion.getName2());
      switch (predicate) {
        case COVER:
        case COVER_DECLARE:
          relationStr = String.format(coverTemplate, name1, name2);
          break;
        default:
          // EQUAL
          relationStr = String.format(eqTemplate, name1, name2);
      }
      boolean isSoft = assertion.getSoftness();
      String assertionStr;
      if (isSoft) {
        switch (predicate) {
          case COVER:
            assertionStr = String.format(assertSoft, relationStr, coverWeight);
            break;
          case COVER_DECLARE: {
            if (isMaximumLengthTypeName(name2)) {
              assertionStr = String.format(assertSoft, relationStr, declarationCoverWeight);
            } else {
              assertionStr = String.format(assertHard, relationStr);
            }
            break;
          }
          default:
            // EQUAL
            assertionStr = String.format(assertSoft, relationStr, equalWeight);
        }
      } else {
        assertionStr = String.format(assertHard, relationStr);
      }
      assertions.add(assertionStr);
    }

    for (String var : varSet) {
      declarations.add(String.format(declareTemplate, var));
      restrictions.add(String.format(assertHard, String.format(notOverlong, var)));
    }

    // output constraint text into the specified file
    try {
      BufferedWriter bout = new BufferedWriter(new FileWriter(fileName));
      bout.write(dataType);
      bout.newLine();
      bout.write(typeRelation);
      bout.newLine();
      for (String declaration : declarations) {
        bout.write(declaration);
        bout.newLine();
      }
      bout.flush();
      for (String assertion : assertions) {
        bout.write(assertion);
        bout.newLine();
      }
      bout.flush();
      for (String restriction : restrictions) {
        bout.write(restriction);
        bout.newLine();
      }
      bout.flush();
      bout.write(checkSat);
      bout.newLine();
      bout.write(getModel);
      bout.newLine();
      bout.write(exit);
      bout.flush();
      bout.close();
    } catch (IOException e) {
      // error in handling IO operation
      System.err.println("invalid I/O operation in 7generating MAXSMT");
    }

  }

  private String preProcess(final String name) {
    // constant type variable should start and end with a single '!'
    // if the original name starts with number, then we should append `!!` at the head of name
    if (!name.startsWith("!")) {
      String newName = name;
      if (Character.isDigit(name.charAt(0))) {
        newName = "!!".concat(name);
      }
      newName = newName.replace("::", "!!");
      varSet.add(newName);
      return newName;
    }
    return name;
  }

  private boolean isMaximumLengthTypeName(String typeName) {
    CSimpleType type = IntegerTypeConstraint.fromTypeString(typeName);
    return type != null && (machineModel.getSizeof(type) >= machineModel
        .getSizeof(CNumericTypes.LONG_LONG_INT));
  }

}
