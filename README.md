[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blueviolet.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Generic badge](https://img.shields.io/badge/Version-6.3.5-informational.svg)](https://shields.io/)
[![Build and Publish](https://github.com/sirius-ms/sirius/actions/workflows/distribute.yaml/badge.svg?branch=release-4-pre)](https://github.com/sirius-ms/sirius/actions/workflows/distribute.yaml)
[![Join community chat at https://gitter.im/sirius-ms/general](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/sirius-ms/general?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

*<span style="color: #808080;">Our methods are offered to the scientific community as freely available resources. (Re-)distribution of the
methods, in whole or in part, for commercial purposes is prohibited.
The SIRIUS web services (CSI:FingerID, CANOPUS, MSNovelist and others) hosted by the [Böcker group](https://bio.informatik.uni-jena.de/) are for academic research and education use only.
Please review the [terms of service](https://bio.informatik.uni-jena.de/terms-of-service-fsu-csi) of the academic version for details.
For non-academic users, the [Bright Giant GmbH](https://bright-giant.com) provides licenses and all related services.
We ask that users of our tools cite the corresponding papers in any resulting publications.</span>*

SIRIUS is a java-based software framework for the analysis of LC-MS/MS data of metabolites and other "small molecules of biological interest".
SIRIUS integrates a collection of our tools, including CSI:FingerID (with [COSMIC](https://bio.informatik.uni-jena.de/software/cosmic/)), [ZODIAC](https://bio.informatik.uni-jena.de/software/zodiac/),
[CANOPUS](https://bio.informatik.uni-jena.de/software/canopus/). In particular, both the
graphical user interface and the command line version of SIRIUS seamlessly integrate the CSI:FingerID, CANOPUS and MSNovelist web services.

Main developers of SIRIUS are the [Böcker group](https://bio.informatik.uni-jena.de/) and the [Bright Giant GmbH](https://bright-giant.com)

## Download Links

### Documentation
- [Online Documentation](https://v6.docs.sirius-ms.io/)
- [Video tutorials](https://www.youtube.com/playlist?list=PL8R4DKiWsw-vBpvrhtKaZT8UX5Zt08x9h)
- [Videos - Behind the scenes](https://www.youtube.com/playlist?list=PL57Jv_39fTddhuGqIlX6OIfzhtMBabrGi)
- [Demo data](https://github.com/sirius-ms/sirius/raw/refs/heads/stable/data/demo-data.zip?download=)
- [Logos for publications and presentations](https://bio.informatik.uni-jena.de/software/sirius/sirius-logos/)

<!--begin download-->

### SIRIUS+CSI:FingerID GUI and CLI - Version 6.3.5 (2026-05-11)
##### These versions include the Java Runtime Environment, so there is no need to install Java separately! Just download, install/unpack and execute.
- for Windows (x86-64/amd64/x64): [msi](https://github.com/sirius-ms/sirius/releases/download/v6.3.5/sirius-6.3.5-win-x64.msi) / [zip](https://github.com/sirius-ms/sirius/releases/download/v6.3.5/sirius-6.3.5-win-x64.zip)
- for Mac (x86-64/amd64/x64): [pkg](https://github.com/sirius-ms/sirius/releases/download/v6.3.5/sirius-6.3.5-macos-x64.pkg) / [zip](https://github.com/sirius-ms/sirius/releases/download/v6.3.5/sirius-6.3.5-macos-x64.zip)
- for Mac (arm64/aarch64/apple silicon): [pkg](https://github.com/sirius-ms/sirius/releases/download/v6.3.5/sirius-6.3.5-macos-arm64.pkg) / [zip](https://github.com/sirius-ms/sirius/releases/download/v6.3.5/sirius-6.3.5-macos-arm64.zip)
- for Linux (x86-64/amd64/x64): [zip](https://github.com/sirius-ms/sirius/releases/download/v6.3.5/sirius-6.3.5-linux-x64.zip)
- for Linux (arm64/aarch64): [zip](https://github.com/sirius-ms/sirius/releases/download/v6.3.5/sirius-6.3.5-linux-arm64.zip)


<!--end download-->

All (including previous) releases can be found [here](https://github.com/sirius-ms/sirius/releases).

### [Installation](https://v6.docs.sirius-ms.io/install)
For Windows and MacOS, the installer version of SIRIUS (msi/pkg) should be preferred but might require administrator permissions.
These installer packages are signed by [Bright Giant](https://bright-giant.com) to verify the package provider’s identity, and should therefore trigger no or only mild security warnings from the operating system during installation.
See the [documentation](https://v6.docs.sirius-ms.io/install) for details.


### [Creating a user account](https://v6.docs.sirius-ms.io/account-and-license/)
User accounts can be created directly via the SIRIUS GUI. Please, use your **institutional email address**. SIRIUS
web services are free for academic use. Usually academic institutions are identified by their
email domain and access will be granted automatically. In some cases, further validation of your academic may be required.
[See also SIRIUS Documentation – Account and License](https://v6.docs.sirius-ms.io/account-and-license/).

### [Sources on GitHub](https://github.com/sirius-ms)
- [SIRIUS](https://github.com/sirius-ms/sirius)
- [SIRIUS-API SDKs](https://github.com/sirius-ms/sirius-client-openAPI)
- [SIRIUS-API Java SDK](sirius-sdk/README.md)

### [Changelog](#changelog)
Also available [online](https://v6.docs.sirius-ms.io/changelog/) and as [CHANGELOG.md](CHANGELOG.md).

### Contact
- To get news, help or ask questions please join our [Gitter Community `#sirius-ms:gitter.im`](https://matrix.to/#/#sirius-ms:gitter.im).
- For bug reports or feature request please use the issues on our [GitHub](https://github.com/sirius-ms/sirius/issues). Or check the [documentation](https://v6.docs.sirius-ms.io/bugs/) for further information about this topic.

### Integration of CSI:FingerID, CANOPUS and MSNovelist

Fragmentation trees and spectra can be directly uploaded from SIRIUS to the CSI:FingerID, CANOPUS and MSNovelist web services.
Results are retrieved from the web service and can be displayed in the SIRIUS graphical user interface. This functionality is
also available for the SIRIUS command-line tool. Training structures for CSI:FingerID's predictors are available through the CSI:FingerID web API:
<!--begin training-->

- https://www.csi-fingerid.uni-jena.de/v3.0/api/fingerid/trainingstructures?predictor=1 (training structures for positive ion mode)
- https://www.csi-fingerid.uni-jena.de/v3.0/api/fingerid/trainingstructures?predictor=2 (training structures for negative ion mode)

<!--end training-->

### Fragmentation Tree Computation

The manual interpretation of tandem mass spectra is time-consuming and
non-trivial. SIRIUS analyses the fragmentation pattern resulting in
a hypothetical fragmentation tree, in which nodes are annotated with
molecular formulas of the fragments and arcs (edges) represent fragmentation
events (losses). SIRIUS allows for the automated and high-throughput analysis of
small-compound MS data beyond elemental composition without requiring
compound structures or a mass spectral database.

### Isotope Pattern Analysis

SIRIUS deduces molecular formulas of small compounds by ranking isotope
patterns from mass spectra of high resolution. After preprocessing, the
output of a mass spectrometer is a list of peaks which corresponds to
the masses of the sample molecules and their abundance. In principle,
elemental compositions of small molecules can be identified using only
accurate masses. However, even with very high mass accuracy, many
formulas are obtained in higher mass regions. High resolution mass
spectrometry allows us to determine the isotope pattern of sample
molecule with outstanding accuracy and apply this information to
identify the elemental composition of the sample molecule. SIRIUS can be
downloaded either as graphical user interface (see Sirius GUI) or as
command-line tool.

<!--begin cite-->
## Main citations

Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Alexander A. Aksenov, Alexey V. Melnik, Marvin Meusel, Pieter C. Dorrestein, Juho Rousu and Sebastian Böcker.
[SIRIUS 4: Turning tandem mass spectra into metabolite structure information.](https://doi.org/10.1038/s41592-019-0344-8)
*Nature Methods* 16, 299–302, 2019.

---
Michael A. Stravs and Kai Dührkop, Sebastian Böcker and Nicola Zamboni.
[MSNovelist: De novo structure generation from mass spectra.](https://doi.org/10.1038/s41592-022-01486-3)
*Nature Methods* 19, 865–870, 2022. (Cite if you are using: MSNovelist)

Martin A. Hoffmann, Louis-Félix Nothias, Marcus Ludwig, Markus Fleischauer, Emily C. Gentry, Michael Witting, Pieter C. Dorrestein, Kai Dührkop and Sebastian Böcker.
[High-confidence structural annotation of metabolites absent from spectral libraries.](https://doi.org/10.1038/s41587-021-01045-9)
*Nature Biotechnology* 40, 411–421, 2022. (Cite if you are using: *CSI:FingerID*, *COSMIC*)

Kai Dührkop, Louis-Félix Nothias, Markus Fleischauer, Raphael Reher, Marcus Ludwig, Martin A. Hoffmann, Daniel Petras, William H. Gerwick, Juho Rousu, Pieter C. Dorrestein and Sebastian Böcker.
[Systematic classification of unknown metabolites using high-resolution fragmentation mass spectra.](https://doi.org/10.1038/s41587-020-0740-8)
*Nature Biotechnology*, 2021. (Cite if you are using *CANOPUS*)

Yannick Djoumbou Feunang, Roman Eisner, Craig Knox, Leonid Chepelev, Janna Hastings, Gareth Owen, Eoin Fahy, Christoph Steinbeck, Shankar Subramanian, Evan Bolton, Russell Greiner, David S. Wishart.
[ClassyFire: automated chemical classification with a comprehensive, computable taxonomy.](https://doi.org/10.1186/s13321-016-0174-y)
*Journal of Cheminformatics* 8, 61, 2016. (*ClassyFire* publication; cite this if you are using *CANOPUS*)

Marcus Ludwig, Louis-Félix Nothias, Kai Dührkop, Irina Koester, Markus Fleischauer, Martin A. Hoffmann, Daniel Petras, Fernando Vargas, Mustafa Morsy, Lihini Aluwihare, Pieter C. Dorrestein, Sebastian Böcker.
[Database-independent molecular formula annotation using Gibbs sampling through ZODIAC.](https://doi.org/10.1038/s42256-020-00234-6)
*Nature Machine Intelligence* 2, 629–641, 2020. (Cite if you are using *ZODIAC*)

Kai Dührkop and Sebastian Böcker.
[Fragmentation trees reloaded.](http://dx.doi.org/10.1007/978-3-319-16706-0_10)
*Journal of Cheminformatics* 8, 5, 2016. (Cite this for *fragmentation pattern analysis and fragmentation tree computation*)

Kai Dührkop, Huibin Shen, Marvin Meusel, Juho Rousu, and Sebastian Böcker.
[Searching molecular structure databases with tandem mass spectra using CSI:FingerID](http://dx.doi.org/10.1073/pnas.1509788112).
*Proceedings of the National Academy of Sciences U S A* 112(41), 12580-12585, 2015. (cite this when *using CSI:FingerID*)

Sebastian Böcker, Matthias C. Letzel, Zsuzsanna Lipták and Anton Pervukhin.
[SIRIUS: decomposing isotope patterns for metabolite identification.](http://bioinformatics.oxfordjournals.org/content/25/2/218.full)
*Bioinformatics* 25(2), 218-224, 2009. (Cite this for *isotope pattern analysis*)

### Additional citations

David S Wishart , Siyang Tian , Dana Allen , Eponine Oler , Harrison Peters , Vicki W Lui , Vasuk Gautam , Yannick Djoumbou-Feunang , Russell Greiner , Thomas O Metz.
[BioTransformer 3.0—a web server for accurately predicting metabolic transformation products](https://doi.org/10.1093/nar/gkac313)
Nucleic Acids Research, 50(W1), W115–W123, 2022. (Cite if you are using: Biotransformer for structure database generation)

Shipei Xing, Sam Shen, Banghua Xu, Xiaoxiao Li and Tao Huan.
[BUDDY: molecular formula discovery via bottom-up MS/MS interrogation.](https://doi.org/10.1038/s41592-023-01850-x)
*Nature Methods* 20, 881–890, 2023. (Cite if you are using: Bottom-up molecular formula generation)

Marcus Ludwig, Kai Dührkop and Sebastian and Böcker.
[Bayesian networks for mass spectrometric metabolite identification via molecular fingerprints.](http://doi.org/10.1093/bioinformatics/bty245)
*Bioinformatics*, 34(13): i333-i340. 2018. Proc. of Intelligent Systems for Molecular Biology (ISMB 2018). (Cite for CSI:FingerID Scoring)

W. Timothy J. White, Stephan Beyer, Kai Dührkop, Markus Chimani and
Sebastian Böcker. [Speedy Colorful
Subtrees.](http://dx.doi.org/10.1007/978-3-319-16706-0_10) In *Proc. of
Computing and Combinatorics Conference (COCOON 2015)*, volume 9198 of
*Lect Notes Comput Sci*, pages 310-322. Springer, Berlin, 2015. (cite
this on *why computations are swift*, even on a laptop computer)

Huibin Shen, Kai Dührkop, Sebastian Böcker and Juho Rousu. [Metabolite
Identification through Multiple Kernel Learning on Fragmentation
Trees.](http://dx.doi.org/10.1093/bioinformatics/btu275)
*Bioinformatics*, 30(12):i157-i164, 2014. Proc. of *Intelligent Systems
for Molecular Biology* (ISMB 2014). (Introduces *the machinery behind
CSI:FingerID*)

Imran Rauf, Florian Rasche, François Nicolas and
Sebastian Böcker. [Finding Maximum Colorful Subtrees in
practice.](http://dx.doi.org/10.1089/cmb.2012.0083) *J Comput Biol*,
20(4):1-11, 2013. (More, earlier work on *why computations are swift*
today)

Heinonen, M.; Shen, H.; Zamboni, N.; Rousu, J. [Metabolite
identification and molecular fingerprint prediction through machine
learning](http://dx.doi.org/10.1093/bioinformatics/bts437).
*Bioinformatics*, 2012. Vol. 28, nro 18, pp. 2333-2341. (Introduces the
*idea of predicting molecular fingerprints* from tandem MS data)

Florian Rasche, Aleš Svatoš, Ravi Kumar Maddula, Christoph Böttcher, and
Sebastian Böcker. [Computing Fragmentation Trees from Tandem Mass
Spectrometry
Data](http://pubs.acs.org/doi/abs/10.1021/ac101825k). *Analytical
Chemistry* (2011) 83 (4): 1243–1251. (Cite this for *introduction of
fragmentation trees* as used by SIRIUS)

Sebastian Böcker and Florian Rasche. [Towards de novo identification of metabolites by analyzing
tandem mass
spectra](http://bioinformatics.oxfordjournals.org/content/24/16/i49.abstract).
*Bioinformatics* (2008) 24 (16): i49-i55. (The very *first paper to
mention fragmentation trees* as used by SIRIUS)

<!--end cite-->

## License

Starting with version 4.4.27, SIRIUS is licensed under the [GNU Affero General
Public License (GPL)](https://www.gnu.org/licenses/agpl-3.0.txt). If you integrate SIRIUS into other software, we
strongly encourage you to make the usage of SIRIUS as well as the literature to cite transparent to the user.

## Acknowledgements
#### Thanks for supporting the development of SIRIUS!
[![MSCJ Logo](https://www.mscj.uni-jena.de/wp-content/uploads/2015/05/logo-svg-text-horizontal.svg)](https://www.mscj.uni-jena.de)

# Changelog

### SIRIUS 6
#### 6.5 (6.5.4) (2026-08-16)
- feature: **searchable projects**. Projects now carry a full text search index over their features,
  which is the basis for the new query and filter capabilities:
  - a chip based query bar above the feature list. Fields, operators and values are suggested while
    typing, panel filters and the typed query are combined into a single query, and the filter
    dialog stays reachable from the funnel icon in the bar (GUI)
  - all features that do not match the current filter can be deleted from the project in one step
    (GUI)
  - the index is built while data is imported and can be rebuilt from the project settings if it
    ever gets out of sync (GUI)
  - the paged listing endpoints accept a `searchQuery` in lucene syntax, supporting full text
    search, field specific terms, phrases, ranges, regular expressions and boolean operators (API)
  - a `searchable-fields` endpoint reports which fields can be queried, their value type, whether
    they support word based search and whether results can be sorted by them (API)
  - features can be deleted by query in a single request, instead of paging the matching ids to the
    client and sending them back (API)
  - quantification tables can be filtered by search query (API)
  - projects created with older SIRIUS versions are migrated and indexed on first use. The feature
    filter options of the CLI (`--mzmin`, `--rtmax`, `--quality`, ...) are unchanged and do not use
    the index (CLI)

- feature: tagging and statistics. Objects in a project can be tagged, grouped by tag, and fold
  changes can be computed per tag group for features and for compound classes. SIRIUS uses this
  internally for blank subtraction and to mark PFAS candidates. Defining and assigning custom tags
  is available through the API only, and these endpoints are still experimental and may change (API,
  experimental)
- feature: reaction editor to build custom structure databases from reaction rules, with a reaction
  library, reactions yielding several products, and intermediate products included in the result
  (GUI, public beta)
- feature: viewer for the contents of custom structure databases (GUI, public beta)
- feature: EAD QTOF profile for instrument specific fragmentation tree computation (GUI and CLI)
- feature: PFAS detection and PFAS specific scoring in fragmentation trees (GUI and CLI,
  experimental)
- feature: rewritten element detection. Elements are now predicted both for de novo formula
  generation and as elements to be excluded from database and bottom up search, and the detected
  elements are stored in the project (GUI and CLI)
- feature: custom sample and run names can be provided when importing LC-MS data, so results can be
  related back to the input (GUI and API)
- feature: restore defaults action for the feature filters, and a single adduct preset (GUI)

- improvement: the GUI stays responsive while a computation uses all cores, since background
  computations now yield to the user interface (GUI)
- improvement: several smaller interface refinements: a loading animation while an embedded web view
  is prepared, web views respecting a minimum size, borderless icon buttons, and faster loading of
  the feature list and its annotations (GUI)
- improvement: quantification tables load faster and their run columns are optional; the
  quantification table API is now stable (GUI and API)
- improvement: large result lists (features, compounds, formula candidates, structure candidates,
  spectral library matches and jobs) are served page by page. The unpaginated endpoints still work
  but are deprecated and will be removed in the next major version (API)
- improvement: the API documentation now describes the errors an endpoint can return (API)
- improvement: name lookup against PubChem during search is cached and bounded by a timeout, so a
  slow or unreachable service no longer stalls the search (GUI and API)

- change: the reaction editor and the structure sketcher belong to the licensed feature set and are
  only shown if your subscription includes them (GUI)

- fixed: `formula --database` now disables de novo and bottom up formula generation, as documented.
  Before, the selected databases were used in addition to them (CLI)
- fixed: disabling element detection had no effect (CLI)
- fixed: wrong citation parameter in the `login` command (CLI)
- fixed: jumping to a related feature could freeze the GUI (GUI)
- fixed: layout of the LC-MS view in the integrated web panels (GUI)
- fixed: pop-up windows opened from web based panels (reaction editor, database contents) could end
  up behind the dialog and could not be brought to the front again (GUI)
- fixed: the blank filter checkbox was not reset together with the other filters (GUI)
- fixed: quantifying features that do not belong to an LC/MS run failed with an error (API)
- fixed: projects created by older SIRIUS versions are now migrated to the current schema before the
  search index is built (GUI and API)

#### 6.3.7 (2026-05-23)
- fixed: CEF file parser end parsing too early when certain features are skipped.

#### 6.3.6 (2026-05-20)
- fixed: License tester (Agilent Explorer) failed to register when the software was installed in
  paths containing whitespaces.

#### 6.3.5 (2026-05-11)
- fixed: Prevented MSNovelist jobs from crashing due to specific CDK exceptions during structure
  generation.
- fixed: Ensured that redundantly specified adducts do not generate duplicate compound formulas
  during annotation, which previously caused database search errors.
- fixed: Improved robustness of the custom database SDF importer; it now continues processing even
  if individual molecules fail to read.

#### 6.3.4 (2026-03-21)
- feature: download and import curated spectral library with public spectra from GNPS, MassBank and
  MsnLib
- feature: custom database exporter to export transformation products generated using BioTransformer
  as molecular structure database into `.tsv` and `.sdf` formats, including database links.
- feature: export substructure annotations as SVG
- feature: direct navigation (jump) to related features via right-click in LC-MS and KMD views.

- improvement: immediate feedback in the Structure Sketcher if the generated structure is already in
  the candidate list, preventing the creation of redundant entries and enhanced error reporting
- improvement: manually override automatic tool selection in the compute panel to prevent redundant
  re-computation of heavy jobs.
- improvement: instrument filter for spectral library importer that automatically selects
  high-resolution spectra (Orbitrap, Q-TOF, FTICR) while filtering out low-resolution or GC-MS data.

- fixed: "unexpected error" in custom DB import on Mac
- fixed: MgfWriter incorrectly using locale-specific formats (e.g., using commas instead of dots for
  decimals), which previously caused parsing issues in downstream tools
- fixed: improved handling and rejection of multiple charged ions during early preprocessing phases.
- fixed: GUI minimized after closing the software tour guide on Windows.
- fixed: missing 'M' prefix in intrinsic charge string generation.
- fixed: CORS issues blocking PUT and DELETE requests in the REST API.


#### 6.3.3 (2025-10-15)
- fix: application starting issue due to faulty logging config
- fix: content-type of Multipart endpoints in SIRIUS API/SDKs.
- fix: missing LC-MS view after initial data import

#### 6.3.2 (2025-09-25)
- fix: accept-terms button not show when internet-check url not reachable
- fix: issues with Windows system scaling
- fix: fractional scaling on Windows
- fix: disable unsupported scaling options on linux (e.g. fractional scaling)
- fix: disable custom scaling on Mac since it's handled/overwritten by the OS anyway

#### 6.3.1 (2025-09-18)
- improvement: option to add a `SIRIUS_` prefix to summary columns in the summary files
- improvement: increased the maximum value for the element filters
- improvement: enabled export of "Formula only" data to ChemVista

- fixed: Internal Server Error when using the API to create a custom database from an existing file.
- fixed: recurring spectra view rendering bug on Linux
- fixed: add self-testing feature to fix conda-forge builds

#### 6.3.0 (2025-08-17)
- feature: Structure Sketcher to manually modify candidate structures

- improvement: improved LCMS pre-alignment using only high-quality features
- improvement: mouseover zoom/preview for structures in "Structures" and "De Novo Structures" views
- improvement: improved isotope detection
- improvement: preventing multiple API instance from being executed

- fixed: SIRIUS failed to start if an outdated subscription was active for the user account
- fixed: fragmentation tree export was not working
- fixed: adducts were not shown in filter view
- fixed: missing MacOS builds
- fixed: incorrect parsing from .cef files

#### 6.2.2 (2025-06-03)
- fixed: Missing AWT initialization error that prevented the REST API from starting correctly.

#### 6.2.0 (2025-05-30)
- feature: BioTransformer integration
- feature: analog spectral library search
- feature: quick software tour guides users through welcome screen, compute dialog and the different
  results views to explain the most important components when first opening sirius
- feature: Kendrick Mass Defect (KMD) plots to visualize groups of related molecules, that share the
  same basic structure but differ by a consistent building block

- improvement: spectral library search can be (de)selected in the `Compute Dialog`
- improvement: database selection is now a global setting to ensure consistent database use
  throughout all selected workflow steps
- improvement: better `LC-MS view` including adduct network, improved sample sorting, improved color
  highlighting
- improvement: better `Library Matches view` listing all identity and analog matches
- improvement: mirror plots for substructure annotations for identity and analog spectral library
  matches, including neutral loss highlighting
- improvement: tags to group and easily filter features via API; still under development, i.e.
  endpoints and their behavior may change in future versions of SIRIUS
- improvement: default timeout for fragmentation tree computation
- improvement: for MGF files with no `NAME` or `FEATURE_ID`, the value of `SCANS` will be used as
  the feature identifier
- improvement: auto-detected element filters are always considered for bottom-up search
- improvement: parameters for preprocessing can be changed via CLI/API
- improvement: `Reference Spectra` in the `Structures` view will open the `Substructure Annotations`
  view
- improvement: Changed behavior of paramter `--elements-extended-organic` in the formula CLI subtool
  (detectable elements set to `SBBrClSe` and fallback `S`)
- improvement: consistent generation of merged MS/MS spectra across multiple usages (file import,
  feature import via API, preprocessing for molecular formula computation)
- improvement: import for multiple charged ions and dimers (analysis not possible yet)

- fixed: progress bar when importing custom databases
- fixed: the timeout for tree computations with non-commercial CLP solver was falsely measuring the
  time from program initiation
- fixed: minor bugs in the feature list filter
- fixed: adducts were not detected in single mzML/mzXML import
- fixed: missing ion mass when importing certain `.cef` files
- fixed: missing isotope pattern when importing via API

#### 6.1.1 (2025-01-22)

- fixed: parameters for molecular formula annotation were read-only for MS1 data only
- fixed: landing page connection info was not updated after license change
- fixed: decimal character in the summaries files was influenced by region settings
- fixed: internal browser for custom DB links was not working
- fixed: when importing multiple `.mzml`  files without aligning, the project space displayed only
  features from the last file
- fixed: ZODIAC parameters were not deactivated after clicking "No" in a question dialog
- fixed: Hidden parameter warning for El Gordo lipid identification
- fixed: inefficient index search for spectral library search

- improvement: batch compute dialog can be opened via `Settings` dialog to view and edit computation
  presets
- improvement: three predefined computation presets for different applications are now available
- improvement: proper error handling for exceeding query quota
- improvement: performance improvement of custom databases in CLI
- improvement: CANOPUS summary exports for "all" hits and "top k" added.


#### 6.1.0 (2025-01-04)

- fixed: unavailable `Compute all` button after finishing computations
- fixed: GUI freezing when submitting many jobs
- fixed: element-selection-panel not using the currently selected elements as initialization state
  in molecular formula computation panel
- fixed: incorrect relative intensities in the MS1 mirror plot in the `Formulas` view
- fixed: missing peak coloring for some peaks explained by the fragmentation tree
- fixed: ZODIAC score incorrectly displayed
- fixed: incorrect column name `median absolute mass error` in `Formulas` view
- fixed: fragmentation tree viewer resolves molecular formula for adducts incorrectly
- fixed: missing parent information in the `Compound Classes` view
- fixed: linked database labels in the `Structures` and `De Novo Structures` views not recognised as
  links on hover
- fixed: duplicated DB labels for compound candidates that are part of de novo and structure
  database search results
- fixed: `De Novo Structures` view not shown if structure database search was not performed
- fixed: sorting by columns in `Library Matches` view not working
- fixed: crashing SDF import if a record cannot be parsed
- fixed: incorrect intensities in FBMN export
- fixed: removed non-functioning `Similarity` CLI tool

- improvement: new color scheme with more consistent coloring throughout the whole identification
  process
- improvement: option to enable/disable CPK colors for all structures in the settings
- improvement: consistent top hit highlighting: the best structure database search hit (and its
  similar structures) is highlighted green in all views.
- improvement: new welcome page
- improvement: three decimal places of precursor masses of the aligned features in the feature list
  (full decimal number displayed in tooltip)
- improvement: 'Da' renamed to 'm/z' in feature list and fragmenation tree viewer
- improvement: confidence filter moved to `Results` tab in filter panel
- improvement: `LC-MS` view is hidden if no LC-MS data was loaded
- improvement: adding data to LC-MS projects after initial import is prohibited
- improvement: new ordering of runs in `LC-MS` view
- improvement: computation setup can be saved and reloaded as a preset for the next computation
- improvement: automated enabling/disabling of tools in compute dialog to match the
  [workflow principles](https://v6.docs.sirius-ms.io/cli/#basic-principles)
- improvement: easy setting of molecular formula to run formula annotation in single-feature
  computation mode
- improvement: formula annotation results in the `Formulas` view are sorted by ZODIAC score if
  performed
- improvement: normalized SIRIUS score added to summary files
- improvement: table obtained via the `fingerid-data` endpoint includes information about the
  fingerprint type
- improvement: additional column in the `Compound Classes` view showing the level of the class
- improvement: databases that have been used for the structure database search are highlighted (in
  the filter menu in the `Structures` view)
- improvement: `Library Matches` tab needs to be activated in `Settings` to ensure understanding of
  the library matching workflow
- improvement: polarity is checked for spectral library search: negative ion mode spectra are only
  compared to negative; positive to positive
- improvement: added metadata to spectral library hits in the `Library Matches` view
- improvement: collision energy sorting in the `Library Matches` view is now number based.

- **Known Issue:** Adduct/isotope assignment view in `LC-MS` tab deactivated for a short time until
  the next release.

#### 6.0.6 (2024-09-28)

- fixed: When importing large mzml/mzxml runs, the feature list filter is not refreshed properly
  after import.
- fixed: Problem when deleting feature via GUI filtering
- fixed: Empty list of affected Compound and feature IDs when importing mzML/mzXML files via
  /import/ms-data-files API endpoint.
- fixed: Error handling for fiels with empty values for given key in .mat/.msp parser
- fixed: Number of explained peaks incorrect for in-source losses.
- fixed: Enforced adducts ignored during formula computation
- fixed: GNPS FBMN does not work with SIRIUS files (https://github.com/sirius-ms/sirius/issues/167)
- fixed: Cannot enforce adducts for features detected as multimere
- fixed: Missing Ids in API fragtree export (https://github.com/sirius-ms/sirius/issues/199)
- fixed: ZODIAC score is missing from the `formula_identifications` file.
- fixed: MGF parser cannot parse `CHARGE=-`` or `CHARGE=+`
- fixed: Formula candidates and adducts introduced by spectral library search are not reset
  correctly during recompute
- fixed: Adducts from LCMS preprocessing may be ignored if not part of detectable adducts.
- fixed: SDF custom database import with spectra cancels the import if one compound in the file has
  invalid spectrum data.

- improvement Show all detected adducts in the GUI
- improvement Export summary in ChemVista compatible csv format.
- improvement Export summary files as `CSV` and `XLXS`
- improvement Scrollable Quality assessment panel
- improvement Temprorary unavailable custom-dbs are no longer permanently removed from SIRIUS.
- improvement Sort features by RT in summary files
- improvement: `data quality summary` renamed to `feature quality summary`
- improvement https://github.com/sirius-ms/sirius/issues/159
- improvement Renamed SIRIUS java sdk package to `sirius-sdk`

- **Known Issues:** Sometimes all jobs of a computation are finished but the Gui compute button and
  feature list stays in compute state. This issue has **not*- yet been resolved but a workarount has
  been added to reset the state and make the gui controllable again when clicking `Cancel all`.
  However, after doing so SIRIUS needs to be restarted to work properly again.

#### 6.0.5 (2024-09-02)
- improvement: new export options and file formats for writing summaries in the CLI  and GUI
- improvement: export of data quality summaries
- improvement: `lcms-align` is automatically executed in the CLI when importing `.mzML` / `.mzXML`
  files


#### 6.0.0 (2024-06-03)
- feature: *de novo* generation of candidate structures through MSNovelist
- feature: spectral library matching
- feature: expansive search allows for structure database searches in user-selected databases with a
  confidence score-based fallback on PubChem
- feature: additional molecular formula generation strategy via bottom-up serach
- feature: SIRIUS service via REST API including Python SDK

- improvement: CANOPUS is automatically executed together with the fingerprint prediction
- improvement: entire adduct is now used already for the first step of the annotation workflow
- improvement: updated fingerprint model; now consistent between positive and negative ion mode
- improvement: LCMS data preprocessing also include MS1-only features
- improvement: LCMS quality tags for features
- improvement: compute dialog has been streamlined to improve clarity
- improvement: transition from a file-based project space to a Nitrite database

### SIRIUS 5
#### 5.8.4 (2023-11-04)
- fixed problem with summary files writing of CANOPUS predictions
- fixed bug where filtering of features would result in omitting features with no available
  confidence score
- added option to ignore molecular formulas during file import in the GUI

#### 5.8.0 (2023-07-01)
- breaking: rankings and ranking cloumn names of summary files have changed
- improvement: added feature id support from several input formats.
- improvement: added feature id as separate fields to project wide summary files
- improvement: change display name of ConfidenceScore from COSMIC to Confidence
- improvement: added support for .mat PEAKID field
- improvement: added support of 1-/1+ charge notation in mgf parser

- fix wrong ranks in summary files
- fix: missing adducts in CanopusSummaryWriter
- fix: missing adducts in FormulaSummaryWriter
- fix: bug with wrong NPC classes in Canopus summary files
- fix: null value problem with MS-Dial msp/mat files

#### 5.7.3 (2023-06-14)
- improvement: Buffered parallelization for Fingerprinter subtool
- improvement: use internal browser as fallback for User Portal if system browser not available.
  - fix: fixes bug where account creation and management not possible with conda version of SIRIUS

#### 5.7.2 (2023-05-26)
- build: SIRIUS docker Image not on dockerhub
- fix: Fixed maxRetentionError getting too small, introduced minError

#### 5.7.1 (2023-05-20)
- feature: Added compound quality flags to Scripting API
- improvement: db import dialog, error handling and description
- fixed: MS1 <-> MS2 switch bug

#### 5.7.0 (2023-05-19)
- (no entries recorded for this release)

#### 5.6.3 (2023-01-12)
- improvement: compounds can now be filtered by cosmic/confidence score (GUI)
- improvement: added `.version` file to project-space that contains SIRIUS version information.
- fix: unreachable availability check url for authentication server which prevented the login
  request message from occurring.
- fix: fixed  bug when selecting `all` database flag in cli. I behaves now like selecting all
  databases in the GUI.
- fix: fixed some missing default values in the GUI.
- fix: fixed handling of preset molecular formulas. `.ms` and `.mgf` inputs should now behave
  equally.
- fix: fixed custom db handling in sirius/formula step. custom database can now be used as formula
  candidate list instead of denovo.
- fix: fixed custom database import which did crash in 5.6.2
- fix: fixed some typos
#### 5.6.2 (2022-11-03)
- fix: bug that prevents accepting license terms caused by mising content-length header in the
  request.
#### 5.6.1 (2022-10-28)
- feature: improved progress information for background computations.
- feature: new scheduling of remote jobs that reduces computation time and improves local cpu
  utilization.
- **feature: A Beta version of the new SIRIUS background service is now included in the respective
  build (_service_ suffix).**
- feature: support for MS1 only data in GUI (Isotope Pattern analysis only).
  - MS1 only data from Agilent CEF format can now be imported
- feature: command line autocompletion support for Linux and MacOS. See `sirius --help` for details.
- feature: allow to write uncompressed (legacy) project-spaces via `--no-compression` pearameter
- improvement: improvements on feature finding and feature alignment
- improvement: more robust custom database importer. Support to import existing DBs into the GUI.
- fix: fixed bug in feature finding that changed negative ion mode data to positive ion mode
- fix: fixed inconsistent indexing of aligned and not aligned mzml/mzxml data that caused an invalid
  FBMN output.
   - **possibly breaking change**: SIRIUS internal compound index does now start at 1 instead of 0
- fix: several smaller fixes and improvements

#### 5.5.2
- fix: Collsion energy parsing bug
- feature: Update checker

#### 5.4.1
- **breaking**: User Authentication. A user account and license is now needed to user the online
  features of SIRIUS.
The license is free and automatically available for non-commercial use, details
[here](https://v6.docs.sirius-ms.io/install/#creating-a-user-account-since-v410).
- **breaking**: New [project-space](https://v6.docs.sirius-ms.io/io/#output) compression. Method
  level directories are now compressed archives to reduce number of files and save storage.
- **breaking**: Summary writing has been moved to a separate sub-tool (`write-summaries`). Som
  summary files have slightly changed. Usually just additional columns if at all.
- **breaking**: The `fingerid`/`structure` sub-tool has been split into a  `fingerprint`
  (fingerprint prediction)
and a `structure` (structure db search) sub-tool. This allows the user to recompute the database
search without having to recompute the fingerprint and compound class predictions. It further allows
to compute `canopus` compound class prediction without having to perform structure db search.
- **breaking**: Updated Fingerprint vector. Fingerprint related results of SIRIUS 4 projects might
  have to be recomputed
to perform some modification (e.g. recompute db-search). Reading the projects is still possible and
formula results do not affected.
- **breaking**: Custom database format has changed. Custom databases need to be re-imported

- feature: **Tool** - **El Gordo** lipid class annotation.
- feature: **GUI Tool** - **Epimetheus** sub-structure annotation. Combinatorial fragmentation of
  CSI:FingerID candidates to assign fragment peaks to sub-structures of the candidate.
- feature: **GUI View** - New feature rich **spectrum viewer**. Mirror-plot to compare Isotope
  pattern and Simulated isotope pattern (e.g. ).
- feature: **GUI View** - **LC-MS** view to review chromatographic peak and data quality report for
  a given feature/compound
- feature: CANOPUS now fully supports **NPC** classes (prediction, GUI and output)
- feature: **GUI** advanced filtering options
- feature: **GUI** scaling factor can be defined by the user (setting panel)
- feature: Additional file formats for spectrum import supported (`.msp`, massbank, `.mat`)

### SIRIUS 4

#### 4.9.3
- fix: prevent Null-pointer in CANOPUS View filter box
- improvement: allow storing project-space copy as directory (not compressed)

#### 4.9.2
- fix: error when using external path for custom dbs in the CLI
  ([#4](https://github.com/boecker-lab/sirius/issues/44))

#### 4.9.1
- fix: wrong log directory that might prevent SIRIUS from starting.

#### 4.9.0
- feature: improved filtering options for the compound list in the GUI
- feature: MS1 only data can now be imported using the CLI (`--allow-ms1-only`) to perform isotope
  pattern based molecular formula identification
  [#28](https://github.com/boecker-lab/sirius/issues/25).
- change: changed tanimoto algorithm form probabilistic to rounded to dramatically reduce running
  time for large structure candidate lists, see
  [#43](https://github.com/boecker-lab/sirius/issues/43)
- change: `--db=BIO` is now the default in the CLI, see
  [#43](https://github.com/boecker-lab/sirius/issues/43)
- fix: wrong adduct for adduct switch annotated spectra in project-space output.

- dev feature: hidden parameter to skip project validation for testing purposes see,
  [#42](https://github.com/boecker-lab/sirius/issues/42)

- minor bug fixes and improvementss

#### 4.8.2
- critical-fix: custom database importer wrote cache files to working directory instead to the usual
  casche dir

#### 4.8.1
- fix: problems with the deletion and creation of custom databases (GUI)
- improvement: local database cached can be cleare from the settings panel (GUI)

#### 4.8.0
- feature: **COSMIC - confidence score**
- improvement: new Object storage baackend
- improvement: resizeable compound list (GUI)
- improvement: CSI:FingerID and SIRIUS/ZODIAC scores in formula selector (GUI)
- fix: new CLP dll should improve compatibility on windows (NO Solver found problem)

#### 4.7.4
- fix: intensity bug in FBMN export

##### 4.7.3
- fix: CONFIG already exists error during background computations (FBMN export, custom-db import)
- fix: `.cef` file extension missing in file import dialog

#### 4.7.2
- fix: `java.util.ConcurrentModificationException` when computing subtool separately with the CLI

#### 4.7.1
- fix: fixed missing jar/zip provider in bundled jre -> **Zipped project-spaces are now working
  again**
- fix: fixed ignored `custom.config` and config inheritance

#### 4.7.0
- feature: Heuristic computation for fragmentation trees to improve running times for high mass
  compounds (no ILP has to be computed)
- feature/improvement: Summaries
    - new `formula_identifications_adducts.tsv` summary file
    - new `canopus_summary_adducts.tsv` summary file
    - added `fromulaRank` to `compound_identifications.tsv` and
      `compound_identifications_adducts.tsv`
    - fix: inconsistent ranking in `compound_identifications.tsv` and
      `compound_identifications_adducts.tsv`
- improvement: more information about running jobs (compound information), improved log panel
- improvement: All SIRIUS distros ship with start scripts that should handle env variables properly
- improvement: rounded values for similarity matrix output
- improvement: better progress information during import
- improvement: CSI:FingerID adn CANOPUS summaries with multiple adducts (like the formula summary)
- improvement/fix: Multithreading and performance issues of integrated CLP ILP solver
- improvement/fix: GUI Job cancelling now works properly, even under high load
- improvement/fix: improved caching and update mechanisms prevents GUI freezes and  reduces GUI
  memory consumption when computing large data sets
- improvement/fix: much lower memory consumption when writing summaries


- fix: memory leak in jjob job manager lib - dramatically improves performance on large datasets.
- fix: correct handling of `CHARGE=-0` in mgf
- fix: corrupted project-space caused by empty adduct detection results after mgf import
- fix: empty project dir after wrong command
- fix: wrong compound name in compound edit panel
- fix: invalid valence filter error when computing positive and negative ion mode together fix:
  compound timeout not working reliable
- fix: Commercial ILP solver not detected correctly even if the correct env variable was set
- fix: CEF format import - "No proper interval given" error
- fix: multiOS build architecture now OpenMS compatible
- several minor bug fixes

- upgrade: SIRIUS ships now with JRE-15 which should fix jvm crashes during heavy multi threading on
  linux

#### 4.6.1
- fix: CSI:FingerID results were not refreshed correctly after recomputing with different parameters
  in the GUI
- fix: Parameters were not always handled correctly when recomputing with the GUI
- fix: Index bug in fragmentation tree scoring

#### 4.6.0
- feature: standalone subtool (`ftree-export`) to export fragmentation trees from CLI
- feature: `--noCite` command to disable bibliography print in CLI
- improvement: bibliography is not printed when showing help message or command parsing error
- fix: another problem with un-importable projects due to detected adducts

#### 4.5.3
- fix: ZODIAC crash caused by empty spectra

#### 4.5.2
- fix: invalid project-space (not importable) due to empty detected adducts
- fix: uncatched exception during adduct resolution (GitHub issue
  [#9](https://github.com/boecker-lab/sirius/issues/19))
- Merry X-Mas

#### 4.5.1
- improvement: CLP native libs are now compatible with glibc 2.12+ (instead of 2.18+)
- fix: project-space with outdated fingeprint versions (e.g. from SIRIUS 4.4) are now handled
  correctly and can be converted.
- fix: database formulas could be used if candidates even if they were incompatible with the adduct
- fix: mzml/mzxml files are now shown in input file selector

#### 4.5.0
- **feature: [CANOPUS:](https://www.biorxiv.org/content/10.1101/2020.04.17.046672v1) for negative
  ion mode data**
- feature: [Bayesian (individual tree)](https://doi.org/10.1093/bioinformatics/bty245) scoring is
  now the default for ranking structure candidates
- **update: Structure DB update due to major changes in PubChem standardization since the last
  one.**
  - feature: COCONUT, NORMAN and Super Natural are now officially supported
- feature: Custom-DB importer View (GUI)

- feature: mgf export for Feature Based Molecular Networking is now available in the GUI

- **breaking:**  additional columns (`ionMass`, `retentionTimeInSeconds`) have been added to project
  wide summary files
such as `formula_identifications.tsv`, `compound_identifications.tsv` and
`compound_identifications_adducts.tsv`
- **breaking:** column names in `formula_candidates.tsv` have changed: `massError(ppm)` to
  `massErrorPrecursor(ppm)`, `explainedPeaks` to `numExplainedPeaks`, `medianAbsoluteMassError(ppm)`
  to `medianAbsoluteMassErrorFragmentPeaks(ppm)`
- **breaking:** column names describing scores now use camel case instead of underscores:
  `ConfidenceScore`, `SiriusScore`, `ZodiacScore`,`TreeScore`,`IsotopeScore`, `CSI:FingerIDScore`

- fix: incompatibility with recent MaOSX version caused by gatekeeper. We now provide an installable
  packages.
- fix: missing SCANS annotation in mgf-export subtool - creates now a valid input for FBMN
- fix: un-parsed retention times in CEF format.
- fix: Structure DB linking (wrong ids, missing link flags, duplicate entries, etc.)
- fix: reduced memory consumption of CLI and GUI

- JRE is now included in all version of SIRIUS
- Many more bug fixes and performance improvements

**NOTE: SIRIUS versions will now follow semantic versioning (all upcoming releases)** regarding the
command line interface and project-space output.

#### 4.4.29
- fix: Error when parsing FragTree json with non numeric double values
- fix: layout of screener progress bar on Mac

#### 4.4.28
- feature: Retention time will now be imported by SIRIUS
  - RT is shown in the Compound list in the SIRUS GUI and the list can be sorted by RT
  - RT is part of the compound.info file in the project-space
- feature: Loglevel can now be changed from CLI
- feature: Summaries can not be written without closing SIRIUS GUI
  - Improvement: Better progress reporting when Summary writing summaries (GUI)
- fix: Agilent CEF files without CE can now be imported

#### 4.4.27
- feature: coin-or ilp solver (CLP) is now included. This allows parallel computation of FragTrees
  without the need for a commercial solver.
- improvement: Compounds without given charge are can now be imported. SIRIUS tries to guess the
  charge from the name (keyword: pos/neg) or falls back to positive.
- improvement: additional parameters in compute dialog
- improvement: commands of the 'show command' dialog can now be copied
- fix: error when writing/reading fragmentation trees with new Jackson parser
- fix: mgf exporter (CLI) now outputs feature name properly
- fix: deadlock during connection check without internet connection
- fix: tree rendering bug on non linux systems
- fix: crash when aborting recompute dialog
- upgrade (GUI): included JRE to `zulu11.41.23-ca-fx-jre11.0.8`

#### 4.4.26
- fix: deadlock and waiting time due to webservice connections
- fix/improvement: Adduct Settings and Adduct detection
- fix: memory leak in third party json lib -> Zodiac memory consumption has been reduced
  dramatically
- fix: several minor bug fixes in the sirius libs

#### 4.4.25
- fix: removed spring boot packaging to
  - solve several class not found issues,
  - solve github issue [#7](https://github.com/boecker-lab/sirius/issues/7)
  - errors when importing and aligning mzml files.
  - improve startup time
- fix: cosine similarity tool ignores instances without spectra (failed before)
- fix: mgf-export tool skips invalid instances if possible (failed before)
- instance validation after lcms-align tool

#### 4.4.24
- feature: ms2 istotope scorer now available in cli and gui

#### 4.4.23
- fix: wrong missing value handling in xlogp filter (some candidates were invisible)
- improvement: less cores for computations if gui is running to have mor cpu time for GUI tasks
- improvement:  show deviation to target ion in FragTree root if precursor is missing in MS/MS

#### 4.4.22
- fix: Classloader exceptions when using CLI from the GUI version
- fix: Wrong mass deviation for trees with adducts
- fix: misplaced labels when exporting svg/pdf fragtrees
- fix: some minor GUI bugs

#### 4.4.21
- fix: incompatibilities with existing configs from previous versions (.sirius)
- fix: CANOPUS detail view has size zero
- fix: failing CSI:FingerID computation with Zodiac re-ranking and existing Adducts
- improvement: errors that occur before GUI is started are now reported
- improvement: minor GUI improvements

#### 4.4.20
- fix: some more fixes on MacOS GUI freezes

#### 4.4.18
- fix: GUI Deadlock on MacOS X fixed. **Mac version is now available**.
- improvement: Character separated files in project-space have now .tsv extension for better excel
  compatibility.
- feature: Windows headless executable respects `%JAVA_HOME%` as JRE location.
- improvement: Improved packaging and startup of the GUI version
- fixes GitHub issues: [#4](https://github.com/boecker-lab/sirius/issues/4) and
  [#6](https://github.com/boecker-lab/sirius/issues/6)

#### 4.4.16
- feature: **CSI:FingerID for negative ion mode is available**
  - NOTE: CANOPUS for negative mode data is not ready yet and will still take some time.
- fix: Too small Heapsize on Windows
- improvement: better GUI performance

#### 4.4.15
- feature: CLI Sub-Tool to export projects to mgf.
- feature: multiple candidate number for Zodiac.
- fix: zodiac score rendering.
- fix: deadlock project-space import
- fixes: tree rendering
- improvement: import and deletion performance
- improvement: import progress now shown

#### 4.4.14
- fix: MacOS included JRE not found.
- fix: ignored parameters.
- fix: recompute does not correctly invalidate and delete previous results.
- fix: UI now correctly update when data will by deleted by the computations.

#### 4.4.(0-13)
- **New (and newly integrated) tools:**
  - [**CANOPUS:**](https://www.biorxiv.org/content/10.1101/2020.04.17.046672v1): A tool for the
    comprehensive annotation of compound classes from MS/MS data.
  - [**ZODIAC:**](https://www.biorxiv.org/content/10.1101/842740v1) Builds upon the SIRIUS molecular
    formula identifications and uses, say, its top 50 molecular formula  annotations as candidates
    for one compound. It then re-ranks molecular formula candidates using Bayesian statistics.
  - [**PASSATUTTO:**](https://www.nature.com/articles/s41467-017-01318-5) Is now part of SIRIUS and
    allows you to generate dataset specific decoy databases from computed fragmentation trees.
  - Other handy standalone tools e.g. compound similarity calculation, mass decomposition, custom-db
    creation and project-space manipulation.

- [**Project-Space:**](https://link.springer.com/protocol/10.1007/978-1-0716-0239-3_11) A
  standardized persistence layer shared by CLI and GUI that makes both fully compatible.
  - Save and reimport your projects with all previously calculated results.
  - Review your results computed with the CLI in the GUI.
  - Handy project-space summary CSV and mzTab-M files for downstream analysis.
  - Preojects can be stored and modified as directory structure or as compressed archive.

- **LCMS-Runs:** SIRIUS can now handle full LCMS-Runs given in mzML/mzXML format and performs
  automatic feature detection.
  - The **lcms-align** preprocessing tool performs feature detection and feature alignment for
    multiple LCMS-Runs based on the available the MS/MS spectra.

- Redesigned **Command line interface**: SIRIUS is now a toolbox containing many
subtools that may be combined to ToolChains based on the project-space.

- **CSI:FingerID** had some massive updates, including more and larger molecular properties.
  - **Structure DBs** New version of the CSI:FingerID PubChem copy that now uses **PubChem
    standardized structures**.
  - [**NORMAN**](https://www.norman-network.com/nds/common/) is now available as search DB
  - All available database filters can now be combined to arbitrary subsets for searching (even with
    custom databases).

- **Interactive fragmentation tree viewer** with vector graphics export in the GUI.
- New REST service with [openAPI](https://www.csi-fingerid.uni-jena.de/v1.4.2-SNAPSHOT/v2/api-docs)
  specification and
  [Swagger-UI](https://www.csi-fingerid.uni-jena.de/v1.4.2-SNAPSHOT/swagger-ui.html).
- **Java 11** or higher is now mandatory
  - **GUI** version ships with an **integrated JRE**
- Many minor improvements and Bugfixes

#### 4.0.1
-   **Java 9 and higher are now supported**
-   **CSI:FingerID trainings structures available**
    - Trainings structures available via WebAPI.
    - Trainings structures are flagged in CSI:FingerID candidate list.
-   **SMARTS filter for candidate list (GUI)**
-   **Molecular Property filter for candidate list (GUI)**
-   **Available prediction workers of the CSI:FingerID webservice can be listed from SIRIUS**
-   Improved connection handling and auto reconnect to Webservice
-   Improved error messaged
-   Improved stability and load balancing of the CSI:FingerID webservice
-   Several bug fixes

#### 4.0
-   **Fragmentation tree heuristics**
-   **Negative ion mode data is now supported**
-   **Polished and more informative GUI**
    - **Sirius Overview:** Explained intensity, number of explained peaks, median mass deviation
    - **Fragmentation trees:** Color coding of nodes by intensity/mass deviation, more informative
      Fragmentation tree nodes
    - **CSI:FingerID Overview:** Number of Pubmed publication with pubmed linking for each
      Candidate, Visualization of CSI:FingerID score.
    - **Predicted Fingerprints:** Visualisation of prediction (posterior probability), predictor
      quality (F1) and number of training examples.
    - Several small improvements
-   **CPLEX** ILP solver support
-   Consider a specific list of **ionizations for Sirius**
-   Consider a specific list of **adducts for CSI:FingerID**
-   Custom ionizations/adducts can be specified (CLI and GUI)
-   **Full-featured** standalone **command line version** (headless version)
-   Improved **parallelization** and task management
-   Improved stability of the CSI:FingerID webservice
-   Time limit for fragmentation tree computations
-   Specify fields to import name and ID from .sdf into a custom database (GUI).
-   CSI:FingerID results can be **filtered by Custom databases** (GUI).
-   Better filtering performance (GUI)
-   Bug fix in Database filtering view (GUI)
-   Error Reporter bug fixed (GUI)
-   Logging bugs fixed
-   Many minor bug fixes
### SIRIUS 3
#### 3.5
-   **Custom databases** can be imported by hand or via csv file. You can manage multiple databases
    within Sirius.
-   New **Bayesian Network scoring** for CSI:FingerID which takes dependencies between molecular
    properties into account.
-   **CSI:FingerID Overview** which lists results for all molecular formulas.
-   **Visualization of the predicted fingerprints**.
-   **ECFP fingerprints** are now also in the CSI:FingerID database and do no longer have to be
    computed on the users side.
-   Connection error detection and refresh feature. No restart required to apply Sirius internal
    proxy settings anymore.
-   **System wide proxy** settings are now supported.
-   Many minor bug fixes and small improvements of the GUI

#### 3.4
-   **element prediction** using isotope pattern
-   CSI:FingerID now predicts **more molecular properties** which improves structure identification
-   improved structure of the result output generated by the command line tool **to its final
    version**

#### 3.3
-   fix missing MS2 data error
-   MacOSX compatible start script
-   add proxy settings, bug reporter, feature request
-   new GUI look

#### 3.2
-   integration of CSI:FingerID and structure identification into SIRIUS
-   it is now possible to search formulas or structures in molecular databases
-   isotope pattern analysis is now rewritten and hopefully more stable than before

#### 3.1.3
-   fix bug with penalizing molecular formulas on intrinsically charged mode
-   fix critical bug in CSV reader

#### 3.1.0
-   Sirius User Interface
-   new output type *-O sirius*. The .sirius format can be imported into the User Interface.
-   Experimental support for in-source fragmentations and adducts

#### 3.0.3
-   fix crash when using GLPK solver

#### 3.0.2
-   fix bug: SIRIUS uses the old scoring system by default when *-p* parameter is not given
-   fix some minor bugs

#### 3.0.1
-   if MS1 data is available, SIRIUS will now always use the parent peak from MS1 to decompose the
    parent ion, instead of using the peak from an MS/MS spectrum
-   fix bugs in isotope pattern selection
-   SIRIUS ships now with the correct version of the GLPK binary

#### 3.0.0
-   release version
