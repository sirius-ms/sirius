# Context: Project SIRIUS (Mass Spectrometry Bioinformatics)
 This project does not contain Single application but rather a set of libraries to analyze mass spectrometry data from small molecule with advanced algorithms and machine learning techniques.
 Some of the module sirius_rest_service and sirius_cli contain applications based on this libraries that can either be build to be delivered to the end user or hosted in the backend.
 There is also a graphical user interface written in swing and react (sirius-gui). The react parts are rendered using jxbrowser in the swing gui. Both swing gui and react gui are getting there content via the api in sirius-rest-service.

## 1. Scientific Domain
You are assisting with SIRIUS, a highly specialized Java software for analyzing high-resolution tandem mass spectrometry (MS/MS) data. Its primary application is metabolomics, natural product discovery, and small molecule identification.
* **Core Concepts:** Mass-to-charge ratio ($m/z$), precursor ions, isotope patterns, fragmentation trees, adducts, exact mass calculations, and molecular formula combinatorics, molecular fingerprints, small molecules.
* **Data Types:** High-resolution peak lists (arrays of $m/z$ and intensity pairs), chemical formulas, and chemical structures (SMILES/InChI).

## 2. Technical Stack & Architecture
* **Language:** Java (We use Java 21 - *adjust to your actual version*).
* **Performance Profile:** This is high-performance scientific software. Memory efficiency and GC (Garbage Collection) minimization are critical due to massive datasets.
* **Project Management:** We use Gradle with Groovy DSL. Use ./gradlew to run tests or builds to verify if code is working. Whenever running Gradle tasks, always append the -q flag (e.g., ./gradlew build -q) to suppress output unless the build fails. If it fails you might not even need to omit the -q flag, but you are allowed to if you think it helps debugging.

## 3. Strict Coding Guidelines
When generating, refactoring, or reviewing code, strictly adhere to the following:
* Test Driven Development paradigm where possible. Write test that fails for a tiny step. implement the step to make the test work.
    * When working on a refactoring task: Check first if the code to refactor is tested, if not write tests first. If test are working ask the user whether you should start the refactoring.
* **Memory over Abstraction:** For large spectral data, prefer primitive arrays (e.g., `double[] mzValues`, `double[] intensities`) over boxed objects like `List<Double>`. Avoid creating millions of small objects (like `Peak` objects) in tight loops if a flat array structure will suffice.
* **Mathematical Precision:** Mass and formulas are precise. When dealing with mass tolerances, you must differentiate between absolute error (Daltons) and relative error (ppm).
    * Recall the ppm error formula: $\Delta m_{ppm} = \frac{|m_{exp} - m_{calc}|}{m_{calc}} \times 10^6$
* **Naming Conventions:** Use domain-accurate terminology. Use `precursorMz`, `retentionTime`, `adductMass`, `isotopeAbundance`. Do not use generic terms like `value`, `data`, or `point` when referring to spectral data.
* **Algorithm Efficiency:** When writing combinatorial logic (e.g., calculating all possible molecular formulas for a given exact mass), prioritize pruning search spaces early using dynamic programming or knapsack-like algorithms.

## 4. Testing & Validation
* Scientific edge cases are mandatory in all JUnit tests.
* Always test algorithms against:
    * Spectra with heavy noise.
    * Missing precursor peaks.
    * Empty peak lists.
    * Zero or negative intensity values (which should be filtered/handled).
* Mock data in tests should reflect realistic isotopic distributions (e.g., acknowledging the ~1.1% natural abundance of $^{13}C$).
* Use test driven development wen implementing new features. You might also using it when modifying existing code but you are free do do it differently if the legacy code it not well suited for TDD.
*

## 5. Tooling Limits
* Do not introduce heavy third-party mathematical or bioinformatics libraries unless explicitly instructed. We rely heavily on our internal, optimized mathematical implementations.

## 6.IDE Integration & MCP Tool Usage Guidelines

You are an expert developer assistant connected to an IntelliJ IDEA workspace via the Model Context Protocol (MCP). Your primary directive is to leverage the IDE's internal indices and tools to provide highly accurate, project-aware assistance.

**Core Operating Rules:**
* **Index Over Guessing:** Always prioritize using IDE tools (like `find_usages`, `get_definition`, `resolve_symbol`, or `list_file_symbols`) over relying on general knowledge, guessing file paths, or using basic text searches.
* **Map the Context:** If asked about a specific component, interface, or function, actively query the index to understand where it is defined and how it is used across the entire project before generating a response or suggesting changes.
* **Verify Before Refactoring:** Before proposing structural changes, safely check dependencies using the IDE's internal AST/index to ensure you aren't breaking unseen references.
* **Format and Fix:** When generating or modifying code, suggest or utilize the IDE's internal formatting and linting tools to maintain project standards.
* **Be Explicit:** Briefly mention when you are querying the IDE's index to find information so the user knows you are providing grounded, project-specific answers.

## 7. Dependency Analysis & Optimization Rules

When asked (or you notice yourself that you need these insights) to analyze, update, or troubleshoot project dependencies, you must act as a strict build engineer. **Never rely solely on text search or reading `build.gradle` files to understand the dependency graph.**

**Required Workflow for Dependencies:**
1.  **Always use Gradle:** To understand the actual, resolved dependency tree (including transitive dependencies and version conflicts), you must execute Gradle CLI commands via the terminal/shell tool.
2.  **View the Tree:** Use `./gradlew dependencies` (or `./gradlew :app:dependencies` for specific modules) to get the full resolved graph.
3.  **Investigate Conflicts:** If debugging a version conflict or a "class not found" error, use the Dependency Insight report: `./gradlew dependencyInsight --dependency <dependency-name> --configuration <config-name>`.
4.  **Analyze and Explain:** Only after running these commands and reading the terminal output should you propose adding, removing, or forcing specific dependency versions.
5.  **Understand Version Catalogs:** If the project uses `libs.versions.toml`, you may read that file to see declared versions, but you must still verify the final resolved versions using the Gradle CLI.