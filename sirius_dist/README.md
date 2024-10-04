# SIRIUS Distribution
This package is **not** intended to contain any source code. 
It just holds different gradle configurations to build different packages/installer/distributions of SIRIUS.

### Run in IDE 
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

### Build a Distribution locally
Our build scripts create distributions for the OS they are executed on. So running it on linux will build a Linux version 
of SIRIUS. Each sub directory of `sirius_dist` corresponds to a specific SIRIUS type. The most common verisions are 
the GUI (`sirius_gui_single_os`) and the CLI (`sirius_cli_single_os`). The following tasks of the `distribution` group
are available to create an application image. Running `gradle clean` on the specific sub dir might be necessary beforehand.
Results will be available in `build/distributions/`

1. `distImage`:  application image as plain directory including start script, jars, native libs and jre which is ready to run 
and to be packaged.
```shell
gradle clean distImage
```   
2. `distImageZip`: packages the app image created by `distImage` as ZIP archive. 
```shell
gradle clean distImage
```

3. `distInstaller`: packages the app image created by `distImage` in an OS specific installer. This needs additional developer
tools to be installed on the system.
```shell
gradle clean distInstaller
```

### CI/CD
#### Gitlab (private)
Pushes to `master`, and maybe some additional branches (see [*.gitlab-ci.yml*](../.gitlab-ci.yml)), will be built and 
published ([Artifactory](https://bio.informatik.uni-jena.de/repository/webapp/#/artifacts/browse/tree/General/dist/de/unijena/bioinf/ms/sirius))
by our internal Linux build pipeline. These builds are intended as internal infrastructure for the development. Not for building releases.

#### Github (public)
Pushes to  `stable` are automatically mirrored to our [Github repo](https://github.com/sirius-ms/sirius/tree/stable)
and will be build for Linux/Mac/Win and published to our 
[Artifactory](https://bio.informatik.uni-jena.de/repository/webapp/#/artifacts/browse/tree/General/dist/de/unijena/bioinf/ms/sirius)
by [Github actions](https://github.com/sirius-ms/sirius/actions) (see, [*.distribute.yml*](../.github/workflows/distribute.yaml)).

#### IMPORTANT: 
Take care about version numbers on branches that are built by CI/CD pipelines to not override versions from
other branches. **Use Merge Requests to merge in such branches if you are unsure** 


### Make a Release
Releases are created from a specific branch (`stable`) that is automatically mirrored to our 
[Github repo](https://github.com/sirius-ms/sirius/tree/stable) (see above).

The workflow contains of two pushes to `stable` and assumes that a **non SNAPSHOT** version is immutable and will be built only once:

1. Push and Build Release
 * Change version (`de.unijena.bioinf.siriusFrontend.version`) in [*sirius_frontend.build.properties*](../sirius_cli/src/main/resources/sirius_frontend.build.properties)
   from `<VERSION>-SNAPSHOT` to `<VERSION>`.
 * Create Tag `<VERSION>` (Optional but strongly recommended)
 * Commit and Push to `stable`

**Check if the build on [Github](https://github.com/sirius-ms/sirius/actions) worked and Test the release builds**

2. Publish Release and bump to new Dev version (SNAPSHOT)
 * Refresh main [*README.md*](../README.md) (which is also the SIRIUS web page) by executing the following task
   from `sirius_frontend` in group `publishing`:
   ```shell
   gradle refreshReadMe
   ```
   
 * Change version (`de.unijena.bioinf.siriusFrontend.version`) in [*sirius_frontend.build.properties*](../sirius_cli/src/main/resources/sirius_frontend.build.properties)
  from `<VERSION>` to `<NU_VERSION>-SNAPSHOT`.
 * Commit and Push to `stable`


#### Bug fixes for releases
 * push changes to `stable` using a *SNAPSHOT version* until you are ready for a new release. Follow "**Make a Release**".
 * merge fixes back to `master` if needed.

#### Major Changes
 * Merge a stable *SNAPSHOT version* from `master` into `stable`. If we are ready to release, follow "**Make a Release**".


### Update Website
The SIRIUS website is the [README.md](../README.md) of this Repo.
Changes to the Website/Readme should be made on the `stable` branch of our [internal Gitlab Repo](https://git.bio.informatik.uni-jena.de/bioinf-mit/ms/sirius_frontend/-/tree/stable) 
and will be synced automatically to GitHub. Changing only `README.md` files **will not** trigger
the build pipelines. So no worries about side effects when just changing documentation.