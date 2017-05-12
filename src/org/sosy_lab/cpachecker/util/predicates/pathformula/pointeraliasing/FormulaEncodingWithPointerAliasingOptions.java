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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;

@Options(prefix = "cpa.predicate")
public class FormulaEncodingWithPointerAliasingOptions extends FormulaEncodingOptions {

  @Option(secure = true, description = "Memory allocation functions of which all parameters but the first should be ignored.")
  private ImmutableSet<String> memoryAllocationFunctionsWithSuperfluousParameters = ImmutableSet.of(
      "__kmalloc", "kmalloc", "kzalloc");

  @Option(secure = true, description =
      "The function used to model successful heap object allocation. " +
          "This is only used, when pointer analysis with UFs is enabled.")
  private String successfulAllocFunctionName = "__VERIFIER_successful_alloc";

  @Option(secure = true, description =
      "The function used to model successful heap object allocation with zeroing. " +
          "This is only used, when pointer analysis with UFs is enabled.")
  private String successfulZallocFunctionName = "__VERIFIER_successful_zalloc";

  @Option(secure = true, description = "Setting this to true makes memoryAllocationFunctions always return a valid pointer.")
  private boolean memoryAllocationsAlwaysSucceed = false;

  @Option(secure = true, description =
      "Enable the option to allow detecting the allocation type by type " +
          "of the LHS of the assignment, e.g. char *arr = malloc(size) is detected as char[size]")
  private boolean revealAllocationTypeFromLhs = true;

  @Option(secure = true, description =
      "Use deferred allocation heuristic that tracks void * variables until the actual type " +
          "of the allocation is figured out.")
  private boolean deferUntypedAllocations = true;

  @Option(secure = true, description =
      "Maximum size of allocations for which all structure fields are regarded always essential, " +
          "regardless of whether they were ever really used in code.")
  private int maxPreFilledAllocationSize = 0;

  @Option(secure = true, description = "The default size in bytes for memory allocations when the value cannot be determined.")
  private int defaultAllocationSize = 4;

  @Option(secure = true, description = "The default length for arrays when the real length cannot be determined.")
  private int defaultArrayLength = 20;

  @Option(secure = true, description = "The maximum length for arrays (elements beyond this will be ignored).")
  private int maxArrayLength = 20;

  @Option(secure = true, description = "Function that is used to free allocated memory.")
  private String memoryFreeFunctionName = "free";

  @Option(secure = true, description =
      "When a string literal initializer is encountered, initialize the contents of the char array "
          + "with the contents of the string literal instead of just assigning a fresh non-det address "
          + "to it")
  private boolean handleStringLiteralInitializers = false;

  @Option(secure = true, description = "If disabled, all implicitly initialized fields and elements are treated as non-dets")
  private boolean handleImplicitInitialization = true;

  public FormulaEncodingWithPointerAliasingOptions(Configuration config)
      throws InvalidConfigurationException {
    super(config);
    config.inject(this, FormulaEncodingWithPointerAliasingOptions.class);
  }

  public boolean hasSuperfluousParameters(final String name) {
    return memoryAllocationFunctionsWithSuperfluousParameters.contains(name);
  }

  public boolean isDynamicMemoryFunction(final String name) {
    return isSuccessfulAllocFunctionName(name)
        || isSuccessfulZallocFunctionName(name)
        || isMemoryAllocationFunction(name)
        || isMemoryAllocationFunctionWithZeroing(name)
        || isMemoryFreeFunction(name);
  }

  public boolean isSuccessfulAllocFunctionName(final String name) {
    return successfulAllocFunctionName.equals(name);
  }

  public boolean isSuccessfulZallocFunctionName(final String name) {
    return successfulZallocFunctionName.equals(name);
  }

  public boolean isDynamicAllocVariableName(final String name) {
    return isSuccessfulAllocFunctionName(name) || isSuccessfulZallocFunctionName(name);
  }

  public String getSuccessfulAllocFunctionName() {
    return successfulAllocFunctionName;
  }

  public String getSuccessfulZallocFunctionName() {
    return successfulZallocFunctionName;
  }

  public boolean makeMemoryAllocationsAlwaysSucceed() {
    return memoryAllocationsAlwaysSucceed;
  }

  public boolean revealAllocationTypeFromLHS() {
    return revealAllocationTypeFromLhs;
  }

  public boolean deferUntypedAllocations() {
    return deferUntypedAllocations;
  }

  public int maxPreFilledAllocationSize() {
    return maxPreFilledAllocationSize;
  }

  public int defaultAllocationSize() {
    return defaultAllocationSize;
  }

  public int defaultArrayLength() {
    return defaultArrayLength;
  }

  public int maxArrayLength() {
    return maxArrayLength;
  }

  public boolean isMemoryFreeFunction(final String name) {
    return memoryFreeFunctionName.equals(name);
  }

  public boolean handleStringLiteralInitializers() {
    return handleStringLiteralInitializers;
  }

  public boolean handleImplicitInitialization() {
    return handleImplicitInitialization;
  }
}
