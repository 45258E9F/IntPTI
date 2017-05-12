# Guide for Function Adapter

Function adapter is used for specifying semantics of library functions. A function adapter is abstract-state-specific and abstract-domain-specific. To implement an adapter, you should specify how to compute the range given domain values and how to refine domains given range value.

In configuration file, you should specify adapters to be used in analysis. Multiple adapters can be specified for one analysis. Moreover, if no CPA can make use of the given function adapter, then no additional work will be done in analysis. An example configuration item could be `map.adapters = ExampleAdapter`. After specifying function adapters, you should specify a function alias mapping file for each adapter. A mapping file contains: (1) supported library functions; (2) alias of supported functions. Each line of a mapping file is like `func : func1 func2`, where `func` is supported library function and the names after `:` are its aliases. There can be no alias, and in this case nothing follows `:`.

You can refer to implementation for range state for more details.
