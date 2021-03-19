This package is **not** intended to contain any source code. 
It just holds different gradle configurations to build different packages/installer/distribution of SIRIUS.

##### Run Main class with CLP ILP-Solver support in the IDE
* Run gradle task to download CLP libs (`downloadCLP`)
* Run the main with vm option: `-Djava.library.path=<PROJECT_ROOT>/sirius_dist/build/clp/l64` and configure
  the following environment variable either System wide or in den Run configuration of the IDE.
  * Linux: Add environment variable `LD_LIBRARY_PATH` with  `<PROJECT_ROOT>/sirius_dist/build/clp/l64`.
  * Mac: Add environment variable `DYLD_LIBRARY_PATH` with  `<PROJECT_ROOT>/sirius_dist/build/clp/l64`.
  * Windows: Add `<PROJECT_ROOT>/sirius_dist/build/clp/l64` to the global `PATH`.

##### Run Main class with CPLEX/Gurobi ILP-Solver support in the IDE
* Note that the IDE vm options do not resolve environment variables! So `<GUROBI_HOME>` and `<CPLEX_HOME>` are just placeholders
* Linux:
  * Run the main with vm option: `-Djava.library.path="<GUROBI_HOME>/lib:<CPLEX_HOME>/bin/x86-64_linux"`
  * Add environment variable `LD_LIBRARY_PATH` with  `<GUROBI_HOME>/lib:<CPLEX_HOME>/bin/x86-64_linux`.
* Mac:
  * Run the main with vm option: `-Djava.library.path="<GUROBI_HOME>/lib:<CPLEX_HOME>/bin/x86-64_osx"`
  * Add environment variable `DYLD_LIBRARY_PATH` with  `<GUROBI_HOME>/lib:<CPLEX_HOME>/bin/x86-64_osx`.
* Windows:
  * Run the main with vm option: `-Djava.library.path="<GUROBI_HOME>/lib:<CPLEX_HOME>/bin/x86-64_win"`
  * Add `<GUROBI_HOME>/lib;<CPLEX_HOME>/bin/x86-64_win` to the global `PATH` (is usually part of the installation on Windows).
  