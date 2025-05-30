[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blueviolet.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Generic badge](https://img.shields.io/badge/Version-6.2.0-informational.svg)](https://shields.io/)
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
- [Video tutorials](https://www.youtube.com/channel/UCIbW_ZFSADRUQ-T5nmgU4VA/featured)
- [Bookchapter on using SIRIUS 4](https://doi.org/10.1007/978-1-0716-0239-3_11) ([Preprint](https://bio.informatik.uni-jena.de/wp/wp-content/uploads/2020/12/SIRIUS4_book_chapter_preprint-2.pdf)) -- does not cover the new LC-MS/MS processing option
- [Demo data](https://github.com/sirius-ms/sirius/raw/refs/heads/stable/data/demo-data.zip?download=)
- [Logos for publications and presentations](https://bio.informatik.uni-jena.de/software/sirius/sirius-logos/)

<!--begin download-->

### SIRIUS+CSI:FingerID GUI and CLI - Version 6.2.0 (2025-05-30)
##### These versions include the Java Runtime Environment, so there is no need to install Java separately! Just download, install/unpack and execute.
- for Windows (x86-64/amd64/x64): [msi](https://github.com/sirius-ms/sirius/releases/download/v6.2.0/sirius-6.2.0-win-x64.msi) / [zip](https://github.com/sirius-ms/sirius/releases/download/v6.2.0/sirius-6.2.0-win-x64.zip)
- for Mac (x86-64/amd64/x64): [pkg](https://github.com/sirius-ms/sirius/releases/download/v6.1.1/sirius-6.1.1-macos-x64.pkg) / [zip](https://github.com/sirius-ms/sirius/releases/download/v6.1.1/sirius-6.1.1-macos-x64.zip)
- for Linux (x86-64/amd64/x64): [zip](https://github.com/sirius-ms/sirius/releases/download/v6.2.0/sirius-6.2.0-linux-x64.zip)
- for Linux (arm64/aarch64): [zip](https://github.com/sirius-ms/sirius/releases/download/v6.2.0/sirius-6.2.0-linux-arm64.zip)


<!--end download-->

All (including previous) releases can be found [here](https://github.com/sirius-ms/sirius/releases).

### [Installation](https://v6.docs.sirius-ms.io/install)
For Windows and MacOS, the installer version of SIRIUS (msi/pkg) should be preferred but might require administrator permissions.
These installer packages are signed by [Bright Giant](https://bright-giant.com) to verify the package provider’s identity, and should therefore trigger no or only mild security warnings from the operating system during installation.
See the [documentation](https://v6.docs.sirius-ms.io/install) for details.


### [Creating a user account](https://v6.docs.sirius-ms.io/account-and-license/)
User accounts can be created directly via the SIRIUS GUI. Please, use your **institutional email address**. SIRIUS
web services are free for academic/non-commercial use. Usually academic institutions are identified by their
email domain and access will be granted automatically. In some cases, further validation of your academic/non-commercial
may be required.
[See also SIRIUS Documentation – Account and License](https://v6.docs.sirius-ms.io/account-and-license/).

### [Sources on GitHub](https://github.com/sirius-ms)
- [SIRIUS](https://github.com/sirius-ms/sirius)
- [SIRIUS-API SDKs](https://github.com/sirius-ms/sirius-client-openAPI)
- [SIRIUS-API Java SDK](sirius-sdk/README.md)

### [Changelog](https://v6.docs.sirius-ms.io/changelog/)

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
