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
package org.sosy_lab.cpachecker.cpa.shape.graphs.node;

import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;

import java.util.concurrent.atomic.AtomicLong;

public abstract class SGObject implements Comparable<SGObject> {

  protected ShapeExplicitValue size;
  protected final String label;

  protected final boolean zeroInitialized;

  /**
   * Each object has its unique identifier, which is used for comparison purpose
   */
  private static final AtomicLong counter = new AtomicLong(0);
  private final long id;

  private static final SGObject nullObject = new SGObject(KnownExplicitValue.ZERO, "NULL") {
    @Override
    public SGObject copy() {
      return SGObject.getNullObject();
    }

    @Override
    public SGObject join(SGObject pOther) {
      if (pOther == nullObject) {
        return this;
      }
      return new SGAbstract(pOther.getSize());
    }

    @Override
    public String toString() {
      return "NULL";
    }
  };

  /**
   * Void object is an artificially introduced object which is used to represent the target of an
   * abstract pointer value. Such object is allowed to have multiple addresses.
   */
  private static final SGObject voidObject = new SGObject(0, "VOID") {
    @Override
    public SGObject copy() {
      return getVoidObject();
    }

    @Override
    public SGObject join(SGObject pOther) {
      return new SGAbstract(0);
    }
  };

  public static SGObject getNullObject() {
    return nullObject;
  }

  public static SGObject getVoidObject() {
    return voidObject;
  }

  protected SGObject(int pSize, String pLabel) {
    size = KnownExplicitValue.valueOf(pSize);
    label = pLabel;
    zeroInitialized = false;
    id = counter.getAndIncrement();
  }

  protected SGObject(ShapeExplicitValue pSize, String pLabel) {
    size = pSize;
    label = pLabel;
    zeroInitialized = false;
    id = counter.getAndIncrement();
  }

  /**
   * This constructor takes zero-initialization into consideration.
   *
   * @param pZero whether the newly created memory object is zero-initialized
   */
  protected SGObject(ShapeExplicitValue pSize, String pLabel, boolean pZero) {
    size = pSize;
    label = pLabel;
    zeroInitialized = pZero;
    id = counter.getAndIncrement();
  }

  protected SGObject(SGObject pOther) {
    size = pOther.size;
    label = pOther.label;
    zeroInitialized = pOther.zeroInitialized;
    // two objects correspond to the same memory object, and they should have the same identifier
    id = pOther.id;
  }

  public abstract SGObject copy();

  /**
   * Join-merge current shape object with another object.
   */
  public abstract SGObject join(SGObject pOther);

  public String getLabel() {
    return label;
  }

  public ShapeExplicitValue getSize() {
    return size;
  }

  public boolean notNull() {
    return (!equals(nullObject));
  }

  public boolean notVoid() {
    return (!equals(voidObject));
  }

  public boolean isZeroInit() {
    return zeroInitialized;
  }

  public long getId() {
    return id;
  }

  @Override
  public int compareTo(SGObject pObject) {
    return Long.compare(this.id, pObject.id);
  }

  /* ************** */
  /* transformation */
  /* ************** */

  /**
   * Change size of current object. This method is only served for implementation of realloc() only.
   */
  public void changeSize(ShapeExplicitValue pSize) {
    size = pSize;
  }

}
