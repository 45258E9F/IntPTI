# IntPTI
Integer error fixing by proper-type inference

Introduction
------------
IntPTI is a tool for automatic integer error (e.g. overflow, sign error) fixing. For now it only supports C programs. The input of IntPTI is a preprocessed C source file or a project after build capture (which will be introduced later), and the output is the modified source files applied with fixes. IntPTI is based on [CPAchecker](https://github.com/sosy-lab/cpachecker) and we have implemented a configurable phase system to schedule multiple static analysis tasks. 

IntPTI is also the prototype from the paper "Fixing Integer Errors by Proper-Type Inference". Generally, for each variable or expression, IntPTI tries to infer a type that can represent all its possible values. Values are derived by static value analysis, while types are derived by solving constraints generated in type inference.

Requirement
-----------
[Z3](https://github.com/Z3Prover/z3): >= 4.5.2, because IntPTI relies on the specific format for models of partial weighted MaxSMT formulae.

Building
--------
Enter the project directory and execute the following commands:

    ant
    ant jar
    
The first command builds the whole project and automatically downloads dependencies. The second command builds three `*.jar` files. `TsmartBuild.jar` is used for build capture and `TsmartAnalyze.jar` is the entry of our tool.

Configuration
-------------
The configuration files used by IntPTI are located in `IntPTI/config/fix_top/`. The entry configuration is [`top.properties`](https://github.com/45258E9F/IntPTI/blob/master/config/fix_top/top.properties), and the first line is as follows.

    phase.manager.config = ...
    
User should fill the absolute path of phase configuration. For benchmark evaluation, we should choose [`top.config.190`](https://github.com/45258E9F/IntPTI/blob/master/config/fix_top/top.config.190), [`top.config.197`](https://github.com/45258E9F/IntPTI/blob/master/config/fix_top/top.config.197) or [`top.config.680`](https://github.com/45258E9F/IntPTI/blob/master/config/fix_top/top.config.680) (Here, `*.197` and `*.680` are used for CWE 197 and 680, respectively. `*.190` is used for other benchmark cases), and change the name of [`rangeAnalysis.properties.bench`](https://github.com/45258E9F/IntPTI/blob/master/config/fix_top/rangeAnalysis.properties.bench) to `rangeAnalysis.properties`. For realistic applications, we choose [`top.config.real`](https://github.com/45258E9F/IntPTI/blob/master/config/fix_top/top.config.real) or [`top.config.real.nomain`](https://github.com/45258E9F/IntPTI/blob/master/config/fix_top/top.config.real.nomain) for projects with no main function, then change the name of [`rangeAnalysis.properties.real`](https://github.com/45258E9F/IntPTI/blob/master/config/fix_top/rangeAnalysis.properties.real) to `rangeAnalysis.properties`.

To specify the call depth of each call graph component for multi-entry analysis, we need to modify the following option in `rangeAnalysis.properties`.

    cpa.boundary.callDepth = 0
    
 `cpa.boundary.callDepth = N` if maximum depth of each call graph component is N.
 
 Usage
 -----
 
 
