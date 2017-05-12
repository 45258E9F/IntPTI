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
package org.sosy_lab.cpachecker.cpa.shape.util;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph.StackObjectInfo;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGPointToEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * This visitor evaluates assumptions as symbolic values.
 * Note: this visitor has inner status to assist derivation of further information. Therefore we
 * should carefully use this visitor.
 */
public class AssumeEvaluator {

  private Map<ShapeState, RelationInfo> relations = new HashMap<>();

  /* ****************** */
  /* evaluation methods */
  /* ****************** */

  public ShapeSymbolicValue evaluateAssumption(
      ShapeState pState, ShapeSymbolicValue pV1,
      ShapeSymbolicValue pV2, BinaryOperator operator) {
    RelationInfo relation = new RelationInfo(pState, operator, pV1, pV2);
    if (relation.isFalse()) {
      return KnownSymbolicValue.FALSE;
    } else if (relation.isTrue()) {
      return KnownSymbolicValue.TRUE;
    }
    // otherwise, we cannot determine the truth instantly, but we can store the relation for
    // further mining
    relations.put(pState, relation);
    return null;
  }

  /* ****************** */
  /* derivation methods */
  /* ****************** */

  /**
   * Given a shape state and truth setting, if this implies that two symbolic values equal.
   *
   * @param pTruth the truth setting
   * @param pState the shape state
   * @return whether the equality of two symbolic values is implied
   */
  public boolean impliesEqOn(boolean pTruth, ShapeState pState) {
    return relations.containsKey(pState) && relations.get(pState).impliesEq(pTruth);
  }

  /**
   * Given a shape state and truth setting, if this implies that two symbolic values are not equal.
   *
   * @param pTruth the truth setting
   * @param pState the shape state
   * @return whether the inequality of two symbolic values is implied
   */
  public boolean impliesNeqOn(boolean pTruth, ShapeState pState) {
    return relations.containsKey(pState) && relations.get(pState).impliesNeq(pTruth);
  }

  /**
   * This method is called when the {@code pState} corresponds to a valid {@link RelationInfo}
   * instance.
   */
  public ShapeSymbolicValue getImpliedValue1(ShapeState pState) {
    return relations.get(pState).getValue1();
  }

  public ShapeSymbolicValue getImpliedValue2(ShapeState pState) {
    return relations.get(pState).getValue2();
  }

  /* ************************** */
  /* relation information class */
  /* ************************** */

  /**
   * This class stores relation information between two symbolic values, given a specific shape
   * state. One can determine the value of assumption, or derive further information by this info.
   */
  private class RelationInfo {

    private boolean isTrue = false;
    private boolean isFalse = false;

    private boolean isEqWhenTrue = false;
    private boolean isNeqWhenTrue = false;

    private boolean isEqWhenFalse = false;
    private boolean isNeqWhenFalse = false;

    private final ShapeSymbolicValue value1;
    private final ShapeSymbolicValue value2;
    private final ShapeState state;

    RelationInfo(
        ShapeState pState, BinaryOperator pOperator, ShapeSymbolicValue pValue1,
        ShapeSymbolicValue pValue2) {
      state = pState;
      value1 = pValue1;
      value2 = pValue2;

      /* ************************** */
      /* derive further information */
      /* ************************** */

      if (value1.isUnknown() || value2.isUnknown()) {
        return;
      }
      long v1 = value1.getAsLong();
      long v2 = value2.getAsLong();
      boolean isPtr1 = isPointer(value1);
      boolean isPtr2 = isPointer(value2);
      // Here `isEq` and `isNeq` are weak result. For example, `isEq` is TRUE when v1 and v2 are
      // confirmed to be equal. When the relation between v1 and v2 is undetermined, its value
      // should be FALSE.
      boolean isEq = (v1 == v2);
      boolean isNeq = isNeq(value1, value2, isPtr1, isPtr2);
      switch (pOperator) {
        case NOT_EQUALS:
          isTrue = isNeq;
          isFalse = isEq;
          isEqWhenFalse = true;
          isNeqWhenTrue = true;
          break;
        case EQUALS:
          isTrue = isEq;
          isFalse = isNeq;
          isEqWhenTrue = true;
          isNeqWhenFalse = true;
          break;
        // for the following cases, we could only handle limited cases
        case GREATER_EQUAL:
        case LESS_EQUAL:
          if (isEq) {
            isTrue = true;
            isEqWhenTrue = true;
            isNeqWhenFalse = true;
          } else {
            isNeqWhenFalse = true;
            // a more refined pointer comparison
            if (isPtr1 && isPtr2) {
              comparePointer(value1, value2, pOperator);
            }
          }
          break;
        case GREATER_THAN:
        case LESS_THAN:
          if (isEq) {
            isFalse = true;
          } else {
            // a more refined pointer comparison
            if (isPtr1 && isPtr2) {
              comparePointer(value1, value2, pOperator);
            }
          }
          isNeqWhenTrue = true;
          break;
        default:
          throw new IllegalArgumentException("unsupported relational operator " + pOperator);
      }
    }

    /* ***************** */
    /* auxiliary methods */
    /* ***************** */

    private boolean isPointer(ShapeSymbolicValue pValue) {
      if (pValue.isUnknown()) {
        return false;
      }
      if (state.isAddress(pValue.getAsLong())) {
        return true;
      }
      return false;
    }

    private boolean isNeq(
        ShapeSymbolicValue pV1, ShapeSymbolicValue pV2,
        boolean isPtr1, boolean isPtr2) {
      long v1 = pV1.getAsLong();
      long v2 = pV2.getAsLong();
      if (isPtr1 && isPtr2) {
        // an address is associated with memory location, thus two different address values
        // should be unequal (otherwise they point to the same memory location)
        return (v1 != v2);
      } else if ((isPtr1 && v2 == 0) || (isPtr2 && v1 == 0)) {
        return (v1 != v2);
      } else {
        return !((pV1.equals(KnownSymbolicValue.TRUE) && v2 == 0) || (pV2.equals(KnownSymbolicValue
            .TRUE) && v1 == 0)) && state.isNeq(pV1, pV2);
      }
    }

    /**
     * Compare two pointers. Two pointers are comparable only when: (1) they are in the same
     * memory object; (2) they are stack variables [Note: two arbitrary stack variables can be
     * compared with their addresses].
     *
     * @param pValue1 address value 1
     * @param pValue2 address value 2
     * @param op      binary operator
     */
    private void comparePointer(
        ShapeSymbolicValue pValue1, ShapeSymbolicValue
        pValue2, BinaryOperator op) {
      SGPointToEdge pointer1 = state.getPointer(pValue1.getAsLong());
      SGPointToEdge pointer2 = state.getPointer(pValue2.getAsLong());
      SGObject object1 = pointer1.getObject();
      SGObject object2 = pointer2.getObject();
      if (object1 == object2) {
        // they are the same memory object
        int offset1 = pointer1.getOffset();
        int offset2 = pointer2.getOffset();
        switch (op) {
          case GREATER_EQUAL:
            isTrue = offset1 >= offset2;
            isFalse = !isTrue;
            break;
          case GREATER_THAN:
            isTrue = offset1 > offset2;
            isFalse = !isTrue;
            break;
          case LESS_EQUAL:
            isTrue = offset1 <= offset2;
            isFalse = !isTrue;
            break;
          case LESS_THAN:
            isTrue = offset1 < offset2;
            isFalse = !isTrue;
            break;
          default:
            throw new IllegalArgumentException("unsupported relational operation " + op);
        }
      } else {
        StackObjectInfo objectInfo1 = state.getStackObjectInfo(object1);
        StackObjectInfo objectInfo2 = state.getStackObjectInfo(object2);
        if (objectInfo1 != null && objectInfo2 != null) {
          // two objects are both on the stack
          Integer comparison = objectInfo1.compare(objectInfo2, CoreShapeAdapter.getInstance()
              .getFunctionDirection(), CoreShapeAdapter.getInstance().getVariableDirection());
          if (comparison == null) {
            // they are incomparable
            return;
          }
          switch (op) {
            case GREATER_EQUAL:
              isTrue = comparison >= 0;
              isFalse = !isTrue;
              break;
            case GREATER_THAN:
              isTrue = comparison > 0;
              isFalse = !isTrue;
              break;
            case LESS_EQUAL:
              isTrue = comparison <= 0;
              isFalse = !isTrue;
              break;
            case LESS_THAN:
              isTrue = comparison < 0;
              isFalse = !isTrue;
              break;
            default:
              throw new IllegalArgumentException("unsupported relational operation " + op);
          }
        }
      }
    }

    /* ************ */
    /* Some getters */
    /* ************ */

    public boolean isTrue() {
      return isTrue;
    }

    public boolean isFalse() {
      return isFalse;
    }

    boolean impliesEq(boolean pTruth) {
      return pTruth ? isEqWhenTrue : isEqWhenFalse;
    }

    boolean impliesNeq(boolean pTruth) {
      return pTruth ? isNeqWhenTrue : isNeqWhenFalse;
    }

    public ShapeSymbolicValue getValue1() {
      return value1;
    }

    public ShapeSymbolicValue getValue2() {
      return value2;
    }

  }

}
