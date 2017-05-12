# How to use Phase system?

Phase system organizes workflow of CPAchecker.
The default workflow of CPAchecker is linear and
consists of 3 steps: (1) CFA creation phase, (2)
algorithm run phase, and (3) statistics collection
phase. However, such workflow is insufficient for
complicated and highly-customized analysis. We design
a phase system in CPAchecker that supports customization
of analysis workflow. Tasks are organized as phases which
could even be executed in parallel if possible.

The current version of code (commit 3849c8f2)
finally supports phase system! This document introduces
how to make this phase system run.

**1.** Configuration Guide

The entry configuration locates at 
`config/phase/top_config.properties`. This file
comprises: (1) the location of phase configuration,
(2) the execution strategy employed. By default and for
now, we support sequential phase execution only.

The phase configuration is described by our own
configuration language. You can find an example in
`config/phase/default.config`. Typically such configuration
has the extention `.config` and the code can be automatically
highlighted in Vim or Emacs. 

There are 4 main syntactic elements in phase configuration.
The first is comment, which occupies a line and starts with
`#`. The second is package alias definition, enclosed by
`.SET` and `.TES`. You can define a prefix of package name
by an alias. The third is definition of a phase, enclosed by
`.DEF` and `.FED`. A compulsory field is `.TYPE` which denotes
the full package name of phase class. You can use prefix alias
to make the class name concise. Following `.TYPE` assignment
you can specify configuration for this phase and this is 
completely optional, by the design of corresponding phase.
The last is `.RUN` section where the dependency of phases is
defined in a syntax similar to `Makefile`. A dependency 
`x: y,z;` denotes that to run phase `x` requires the 
result of `y` and `z`, hence `y,z` are executed prior to the 
execution of `x`. For now the detailed constraint between 
phases should be carefully maintained by phase designers.

**2.** Make CPAchecker run in IDE

This section introduces the run configuration in IntelliJ. 
This guide, however, should also work for Eclipse.

Main class: `org.sosy_lab.cpachecker.cmdline.CPAMain`

VM options: for example, `-ea -Xmx10000m`, please refer
to the documentation of JVM for more details

Arguments: `-preprocess -config config/phase/top_config.properties /home/cxcfan/dev/testbench/cex.c`.
You should specify a top configuration contains the location
of phase configuration.

Working dir: the directory of CPAchecker by default

**3.** Issues

If any unexpected issue happens, please refer to the detail
of thrown exception for more information. Please feel free
to contact me (`chengxi09@gmail.com`) if you find any bug
or have suggestions.
