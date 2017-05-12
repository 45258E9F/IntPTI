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
package org.sosy_lab.cpachecker.cpa.smg.join;

import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

final public class SMGJoin {
  static public void performChecks(boolean pOn) {
    SMGJoinSubSMGs.performChecks(pOn);
  }

  private boolean defined = false;
  private SMGJoinStatus status = SMGJoinStatus.EQUAL;
  private final CLangSMG smg;

  public SMGJoin(CLangSMG pSMG1, CLangSMG pSMG2) throws SMGInconsistentException {
    CLangSMG opSMG1 = new CLangSMG(pSMG1);
    CLangSMG opSMG2 = new CLangSMG(pSMG2);
    smg = new CLangSMG(opSMG1.getMachineModel());

    SMGNodeMapping mapping1 = new SMGNodeMapping();
    SMGNodeMapping mapping2 = new SMGNodeMapping();

    Map<String, SMGRegion> globals_in_smg1 = opSMG1.getGlobalObjects();
    Deque<CLangStackFrame> stack_in_smg1 = opSMG1.getStackFrames();
    Map<String, SMGRegion> globals_in_smg2 = opSMG2.getGlobalObjects();
    Deque<CLangStackFrame> stack_in_smg2 = opSMG2.getStackFrames();

    Set<String> globalVars = new HashSet<>();
    globalVars.addAll(globals_in_smg1.keySet());
    globalVars.addAll(globals_in_smg2.keySet());

    for (String globalVar : globalVars) {
      SMGRegion globalInSMG1 = globals_in_smg1.get(globalVar);
      SMGRegion globalInSMG2 = globals_in_smg2.get(globalVar);
      if (globalInSMG1 == null || globalInSMG2 == null) {
        // This weird situation happens with function static variables, which are created
        // as globals when a declaration is met. So if one path goes through function and other
        // does not, then one SMG will have that global and the other one won't.
        // TODO: We could actually just add that object, as that should not influence the result of
        // the join. For now, we will treat this situation as unjoinable.
        return;
      }
      SMGRegion finalObject = globalInSMG1;
      smg.addGlobalObject(finalObject);
      mapping1.map(globalInSMG1, finalObject);
      mapping2.map(globalInSMG2, finalObject);
    }

    Iterator<CLangStackFrame> smg1stackIterator = stack_in_smg1.descendingIterator();
    Iterator<CLangStackFrame> smg2stackIterator = stack_in_smg2.descendingIterator();

    //TODO assert stack smg1 == stack smg2

    while (smg1stackIterator.hasNext() && smg2stackIterator.hasNext()) {
      CLangStackFrame frameInSMG1 = smg1stackIterator.next();
      CLangStackFrame frameInSMG2 = smg2stackIterator.next();

      smg.addStackFrame(frameInSMG1.getFunctionDeclaration());

      Set<String> localVars = new HashSet<>();
      localVars.addAll(frameInSMG1.getVariables().keySet());
      localVars.addAll(frameInSMG2.getVariables().keySet());

      for (String localVar : localVars) {
        // two stack frames should have the same set of variables, otherwise two states are
        // unable to be joined
        if ((!frameInSMG1.containsVariable(localVar)) || (!frameInSMG2
            .containsVariable(localVar))) {
          return;
        }
        SMGRegion localInSMG1 = frameInSMG1.getVariable(localVar);
        SMGRegion localInSMG2 = frameInSMG2.getVariable(localVar);
        SMGRegion finalObject = localInSMG1;
        smg.addStackObject(finalObject);
        mapping1.map(localInSMG1, finalObject);
        mapping2.map(localInSMG2, finalObject);
      }
    }

    // if we reach here, two states share the same set of global variables
    for (Entry<String, SMGRegion> entry : globals_in_smg1.entrySet()) {
      SMGObject globalInSMG1 = entry.getValue();
      SMGObject globalInSMG2 = globals_in_smg2.get(entry.getKey());
      SMGObject destinationGlobal = mapping1.get(globalInSMG1);
      // the core function of SMGJoin, which joins two memory objects by joining their values and
      // target objects of value nodes representing addresses
      SMGJoinSubSMGs jss =
          new SMGJoinSubSMGs(status, opSMG1, opSMG2, smg, mapping1, mapping2, globalInSMG1,
              globalInSMG2, destinationGlobal, 0, false, false);
      if (!jss.isDefined()) {
        // thus, two states cannot be joined
        return;
      }
      // note: there are 4 join status
      // (1) two states are EQUIVALENT, (2) one state includes information of another state (LEFT
      // or RIGHT), (3) two states are semantically incomparable
      status = jss.getStatus();
    }

    smg1stackIterator = stack_in_smg1.iterator();
    smg2stackIterator = stack_in_smg2.iterator();

    while (smg1stackIterator.hasNext() && smg2stackIterator.hasNext()) {
      CLangStackFrame frameInSMG1 = smg1stackIterator.next();
      CLangStackFrame frameInSMG2 = smg2stackIterator.next();

      for (String localVar : frameInSMG1.getVariables().keySet()) {
        SMGObject localInSMG1 = frameInSMG1.getVariable(localVar);
        SMGObject localInSMG2 = frameInSMG2.getVariable(localVar);
        SMGObject destinationLocal = mapping1.get(localInSMG1);
        SMGJoinSubSMGs jss =
            new SMGJoinSubSMGs(status, opSMG1, opSMG2, smg, mapping1, mapping2, localInSMG1,
                localInSMG2, destinationLocal, 0, false, false);
        if (!jss.isDefined()) {
          return;
        }
        status = jss.getStatus();
      }
    }

    // WHY we do not directly merge heap objects here? That is because heap objects are target
    // objects of some address values which are reachable from either stack or global object.
    // Note that we can never directly access heap objects in C program.

    defined = true;
  }

  public boolean isDefined() {
    return defined;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public CLangSMG getJointSMG() {
    return smg;
  }
}
