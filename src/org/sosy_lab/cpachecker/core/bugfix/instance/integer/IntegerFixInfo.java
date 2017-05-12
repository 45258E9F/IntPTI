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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.bugfix.FixInformation;
import org.sosy_lab.cpachecker.core.bugfix.FixProvider.BugCategory;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFix.IntegerFixMode;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerTypeConstraint.IntegerTypePredicate;
import org.sosy_lab.cpachecker.util.access.AccessPath;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

public class IntegerFixInfo implements FixInformation {

  private BiMap<String, FileLocation> id2Loc = HashBiMap.create();
  private Multimap<FileLocation, IntegerFix> loc2Fix = ArrayListMultimap.create();

  // inferred type constraint: should be cleared after finishing each entry
  private Set<IntegerTypeConstraint> constraint = Sets.newHashSet();
  // qualified names appearing at the left-hand-side of assignment
  private Set<String> leftNames = Sets.newHashSet();
  // qualified name to the original declared type
  private Map<String, CSimpleType> oldVarTypeMap = Maps.newHashMap();

  // a counter for the fixes that fail to give a new type (which corresponds to the large
  // punishment)
  private long noNewTypeCounter = 0;

  public void addNameLocationBinding(String identifier, FileLocation location) {
    // normally, declared name and its location have one-by-one matching relation
    if (identifier != null && location != null) {
      id2Loc.forcePut(identifier, location);
    }
  }

  public void addNameTypeBinding(String identifier, CSimpleType declaredType) {
    if (identifier != null && declaredType != null) {
      oldVarTypeMap.put(identifier, declaredType);
    }
  }

  public void addCandidateFix(
      FileLocation location, IntegerFixMode pMode,
      @Nullable CSimpleType pType) {
    if (location != null && !location.equals(FileLocation.DUMMY) && pType != null) {
      IntegerFix newFix = new IntegerFix(pMode, pType);
      loc2Fix.put(location, newFix);
    }
  }

  public void addTypeConstraint(
      IntegerTypePredicate pPredicate, AccessPath pPath, CSimpleType
      pType, boolean pSoft) {
    IntegerTypeConstraint clause = IntegerTypeConstraint.of(pPredicate, pPath, pType, pSoft);
    if (clause != null) {
      constraint.add(clause);
    }
  }

  public void addTypeConstraint(
      IntegerTypePredicate pPredicate, String pQualifiedName,
      CSimpleType pType, boolean pSoft) {
    IntegerTypeConstraint clause = IntegerTypeConstraint.of(pPredicate, pQualifiedName, pType,
        pSoft);
    if (clause != null) {
      constraint.add(clause);
    }
  }

  public void addLeftName(@Nullable AccessPath pPath) {
    if (pPath != null && pPath.isDeclarationPath()) {
      leftNames.add(pPath.getQualifiedName());
    }
  }

  public void mergeFixes(MachineModel pMachineModel) {
    Multimap<FileLocation, IntegerFix> refinedLoc2Fix = ArrayListMultimap.create();
    for (FileLocation keyLoc : loc2Fix.keySet()) {
      IntegerFix specifierFix = null;
      IntegerFix checkFix = null;
      IntegerFix castFix = null;
      // the number of fixes is not less than 1
      Collection<IntegerFix> fixes = loc2Fix.get(keyLoc);
      if (fixes.size() == 1) {
        refinedLoc2Fix.put(keyLoc, Iterables.getOnlyElement(fixes));
        continue;
      }
      // if there are multiple candidate fixes, we should merge them properly
      for (IntegerFix fix : fixes) {
        IntegerFixMode mode = fix.getFixMode();
        switch (mode) {
          case SPECIFIER:
            if (specifierFix == null) {
              specifierFix = fix;
            } else {
              specifierFix = specifierFix.merge(fix, pMachineModel);
            }
            break;
          case SANITYCHECK:
            if (checkFix == null) {
              checkFix = fix;
            } else {
              checkFix = checkFix.merge(fix, pMachineModel);
            }
            break;
          case CAST:
            if (castFix == null) {
              castFix = fix;
            } else {
              castFix = castFix.merge(fix, pMachineModel);
            }
            break;
          default:
            throw new AssertionError("unrecognized fix mode: " + mode);
        }
      }
      if (castFix != null && specifierFix != null) {
        // Cast and specifier fixes cannot co-exist, because the former applies to the expression
        // while the latter applies to the declaration. For the sake of sanity, we deal with this
        // almost impossible case here.
        // We leave it to fix applicator to decide whether to deal with the fix the target type
        // of which is null.
        refinedLoc2Fix.put(keyLoc, specifierFix);
      } else if (castFix != null) {
        // only cast fix
        refinedLoc2Fix.put(keyLoc, castFix);
      } else if (specifierFix != null) {
        refinedLoc2Fix.put(keyLoc, specifierFix);
      }
      if (checkFix != null) {
        // Sanity check fix only applies to the expression, and it could co-exist with cast fix.
        refinedLoc2Fix.put(keyLoc, checkFix);
      }
    }
    loc2Fix.clear();
    loc2Fix.putAll(refinedLoc2Fix);
  }

  public FileLocation getLocation(String varName) {
    return id2Loc.get(varName);
  }

  public String getQualifiedName(FileLocation loc) {
    return id2Loc.inverse().get(loc);
  }

  public CSimpleType getDeclaredType(String varName) {
    return oldVarTypeMap.get(varName);
  }

  /**
   * Generate SMTLIB2 file for type constraints.
   *
   * @param fileName               file path to be written
   * @param coverWeight            weight for COVER relation
   * @param declarationCoverWeight weight for DECLARATION_COVER relation
   * @param equalWeight            weight for EQUAL relation
   */
  public void generateTypeConstraint(
      String fileName, int coverWeight, int declarationCoverWeight,
      int equalWeight, MachineModel machineModel) {
    TypeConstraintWriter writer = new TypeConstraintWriter(fileName, constraint, coverWeight,
        declarationCoverWeight, equalWeight, machineModel);
    writer.output();
  }

  public Multimap<FileLocation, IntegerFix> getLoc2Fix() {
    return loc2Fix;
  }

  public boolean containsLeftName(String name) {
    return leftNames.contains(name);
  }

  public void incPunishCount() {
    noNewTypeCounter++;
  }

  public long getPunishCount() {
    return noNewTypeCounter;
  }

  @Override
  public BugCategory getCategory() {
    return BugCategory.INTEGER;
  }

  @Override
  public void reset() {
    constraint.clear();
    leftNames.clear();
    oldVarTypeMap.clear();
  }
}
