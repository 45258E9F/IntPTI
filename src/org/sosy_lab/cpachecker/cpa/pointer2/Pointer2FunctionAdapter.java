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
package org.sosy_lab.cpachecker.cpa.pointer2;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.function.DirectFunctionAdapter;
import org.sosy_lab.cpachecker.core.interfaces.function.MapParser;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pointer2FunctionAdapter implements DirectFunctionAdapter<Pointer2State, LocationSet> {

  private static Pointer2FunctionAdapter INSTANCE = null;

  private HashMap<String, String> handlerMap;

  private final boolean activeness;

  private Map<FileLocation, Integer> loc2MallocId;

  private Pointer2FunctionAdapter() {
    handlerMap = Maps.newHashMap();
    Path mapFile = GlobalInfo.getInstance().queryMapFilePath(getClass());
    if (mapFile != null) {
      MapParser.loadFromFile(mapFile, handlerMap);
    }
    activeness = GlobalInfo.getInstance().queryActiveness(getClass());
    loc2MallocId = new HashMap<>();
  }

  public static Pointer2FunctionAdapter instance() {
    if (INSTANCE == null) {
      INSTANCE = new Pointer2FunctionAdapter();
    }
    return INSTANCE;
  }

  @Override
  public LocationSet evaluateFunctionCallExpression(
      CFunctionCallExpression pFunctionCallExpression,
      Pointer2State currentState,
      List<AbstractState> currentOtherStates,
      CFAEdge edge) {
    LocationSet defaultLoc = LocationSetTop.INSTANCE;
    if (!activeness) {
      return defaultLoc;
    }
    CExpression nameExp = pFunctionCallExpression.getFunctionNameExpression();
    if (nameExp instanceof CIdExpression) {
      CSimpleDeclaration declaration = ((CIdExpression) nameExp).getDeclaration();
      String funcName;
      if (declaration == null) {
        funcName = ((CIdExpression) nameExp).getName();
      } else {
        funcName = declaration.getName();
      }
      List<CExpression> args = pFunctionCallExpression.getParameterExpressions();
      LocationSet result = handleFunction(funcName, args, currentState, currentOtherStates);
      if (result != null) {
        return result;
      }
    }
    return defaultLoc;
  }

  @Override
  public boolean isRegistered(CFunctionCallExpression pCFunctionCallExpression) {
    CExpression nameExp = pCFunctionCallExpression.getFunctionNameExpression();
    if (nameExp instanceof CIdExpression) {
      CSimpleDeclaration declaration = ((CIdExpression) nameExp).getDeclaration();
      String funcName;
      if (declaration == null) {
        funcName = ((CIdExpression) nameExp).getName();
      } else {
        funcName = declaration.getName();
      }
      String handlerName = handlerMap.get(funcName);
      if (handlerName != null) {
        return true;
      }
      handlerName = funcName;
      boolean isReg = true;
      try {
        getClass().getDeclaredMethod(handlerName, List.class, Pointer2State.class, List.class);
      } catch (NoSuchMethodException ex) {
        isReg = false;
      }
      return isReg;
    }
    return false;
  }

  private LocationSet handleFunction(
      String name, List<CExpression> argList, Pointer2State
      currentState, List<AbstractState> currentOtherStates) {
    String handlerName = handlerMap.get(name);
    if (handlerName == null) {
      handlerName = name;
    }
    try {
      Method targetMethod = getClass().getDeclaredMethod(handlerName, List.class, Pointer2State
          .class, List.class);
      return (LocationSet) targetMethod.invoke(this, argList, currentState, currentOtherStates);
    } catch (NoSuchMethodException ex) {
      return LocationSetTop.INSTANCE;
    } catch (IllegalAccessException | InvocationTargetException ex) {
      return null;
    }
  }

  /* ************************* */
  /* various function handlers */
  /* ************************* */

  static final String MALLOC_PREFIX = "malloc_ID:";

  @SuppressWarnings("unused")
  private LocationSet malloc(
      List<CExpression> args, Pointer2State currentState,
      List<AbstractState> currentOtherStates) {
    if (args.size() != 1) {
      return LocationSetTop.INSTANCE;
    }
    // TODO: consider the case where allocation fails
    CExpression mallocExp = Iterables.getOnlyElement(args);
    FileLocation fileLoc = mallocExp.getFileLocation();
    Integer id;
    if (fileLoc == null) {
      id = 0;
    } else {
      id = loc2MallocId.get(fileLoc);
      if (id == null) {
        id = loc2MallocId.size() + 1;
        loc2MallocId.put(fileLoc, id);
      }
    }
    MemoryLocation allocatedLoc = MemoryLocation.valueOf(MALLOC_PREFIX.concat(String.valueOf(id)));
    return ExplicitLocationSet.from(allocatedLoc);
  }

}
