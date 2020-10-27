This package is **not** intended to contain any source code. 
It just holds different gradle configurations to build different packages/installer/distribution of SIRIUS.

##### Run Main class with CLP ILP-Solver support
* Run gradle task to download CLP libs (`downloadCLP`)
* Run the main with vm option: `-Djava.library.path=<PROJECT_ROOT>/sirius_dist/build/clp/l64`
  * Linux/Mac: libs are build with *rpath*, so **no** other path than `java.library.path` is needed.
  * Windows: Windows doe not support *rpath*. You have to add the libs additionally to the global `PATH`.

##### Run Main class with CPLEX/Gurobi ILP-Solver support
* Configure `CPLEX_HOME`/ `GUROBI_HOME` environment variable. Can be either on the System or in den Run configuration if the IDE.
* Run the main with vm option: `-javaagent:<PATH_TO_AGENT>/agents-4.4.7-SNAPSHOT.jar`
  * The `agents` is a module in `sirius` that resolves native libraries and environment variables at runtime
  * you can just publish it to maven local and point to its location in the `~/.m2` repo.