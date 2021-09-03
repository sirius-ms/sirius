[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blueviolet.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Generic badge](https://img.shields.io/badge/Version-5.0.1--SNAPSHOT-informational.svg)](https://shields.io/)
[![Build and Pubish](https://github.com/boecker-lab/sirius/actions/workflows/distribute.yaml/badge.svg?branch=release-4-pre)](https://github.com/boecker-lab/sirius/actions/workflows/distribute.yaml)

*<span style="color: #808080;">Our methods are offered to the scientific community as freely available resources. (Re-)distribution of the
methods, in whole or in part, for commercial purposes is prohibited.
CSI:FingerID and CANOPUS web services hosted by the [Böcker group](https://bio.informatik.uni-jena.de/) are for non-commercial use only.
For commercial users, the [Bright Giant GmbH](https://bright-giant.com) provides all related services.
We ask that users of our tools cite the corresponding papers in any resulting publications.</span>*

Contact: ![](https://raw.githubusercontent.com/boecker-lab/sirius_frontend/release/manual/source/siriusMailtrans.png)

SIRIUS is a java-based software framework for the analysis of LC-MS/MS data of metabolites and other "small molecules of biological interest".
SIRIUS integrates a collection of our tools, including CSI:FingerID (with [COSMIC](https://bio.informatik.uni-jena.de/software/cosmic/)), [ZODIAC](https://bio.informatik.uni-jena.de/software/zodiac/) and
[CANOPUS](https://bio.informatik.uni-jena.de/software/canopus/). In particular, both the
graphical user interface and the command line version of SIRIUS seamlessly integrate the CSI:FingerID and CANOPUS web services.

## Download Links

### Documentation
- [Online Documentation](https://boecker-lab.github.io/docs.sirius.github.io/)
- [Video tutorials](https://www.youtube.com/channel/UCIbW_ZFSADRUQ-T5nmgU4VA/featured)
- [Bookchapter on using SIRIUS 4](https://doi.org/10.1007/978-1-0716-0239-3_11) ([Preprint](https://bio.informatik.uni-jena.de/wp/wp-content/uploads/2020/12/SIRIUS4_book_chapter_preprint-2.pdf)) -- does not cover the new LC-MS/MS processing option
- [Demo data](https://bio.informatik.uni-jena.de/wp/wp-content/uploads/2015/05/demo.zip)

<!--begin download-->

### SIRIUS+CSI:FingerID GUI and CLI - Version 5.0.1-SNAPSHOT (2021-09-03)
##### These versions include the Java Runtime Environment, so there is no need to install Java separately! Just download, install/unpack and execute.
- for Windows (64bit): [msi](https://github.com/boecker-lab/sirius/releases/download/v5.0.1-SNAPSHOT/sirius-5.0.1-SNAPSHOT-win64.msi) / [zip](https://github.com/boecker-lab/sirius/releases/download/v5.0.1-SNAPSHOT/sirius-5.0.1-SNAPSHOT-win64.zip)
- for Linux (64bit): [zip](https://github.com/boecker-lab/sirius/releases/download/v5.0.1-SNAPSHOT/sirius-5.0.1-SNAPSHOT-linux64.zip)
- for Mac (64bit): [pkg](https://github.com/boecker-lab/sirius/releases/download/v5.0.1-SNAPSHOT/sirius-5.0.1-SNAPSHOT-osx64.pkg) / [zip](https://github.com/boecker-lab/sirius/releases/download/v5.0.1-SNAPSHOT/sirius-5.0.1-SNAPSHOT-osx64.zip)

### SIRIUS+CSI:FingerID Command-Line Interface only - Version 5.0.1-SNAPSHOT (2021-09-03)
##### These versions include the Java Runtime Environment, so there is no need to install Java separately! Just download, install/unpack and execute.
- for Windows (64bit): [msi](https://github.com/boecker-lab/sirius/releases/download/v5.0.1-SNAPSHOT/sirius-5.0.1-SNAPSHOT-win64-headless.msi) / [zip](https://github.com/boecker-lab/sirius/releases/download/v5.0.1-SNAPSHOT/sirius-5.0.1-SNAPSHOT-win64-headless.zip)
- for Linux (64bit): [zip](https://github.com/boecker-lab/sirius/releases/download/v5.0.1-SNAPSHOT/sirius-5.0.1-SNAPSHOT-linux64-headless.zip)
- for Mac (64bit): [pkg](https://github.com/boecker-lab/sirius/releases/download/v5.0.1-SNAPSHOT/sirius-5.0.1-SNAPSHOT-osx64-headless.pkg) / [zip](https://github.com/boecker-lab/sirius/releases/download/v5.0.1-SNAPSHOT/sirius-5.0.1-SNAPSHOT-osx64-headless.zip)


<!--end download-->

### [Installation](https://boecker-lab.github.io/docs.sirius.github.io/install)
For  Windows and MacOS, the installer version of SIRIUS (msi/pkg) should be preferred but might require administrator permissions.
Since we do not pay Microsoft/Apple for certification, you might have to confirm that you want to trust "software from an unknown source" on Windows/MacOS.
See the [documenntation](https://boecker-lab.github.io/docs.sirius.github.io/install) for details.

### [Sources on GitHub](https://github.com/boecker-lab)
- [SIRIUS frontend](https://github.com/boecker-lab/sirius)
- [SIRIUS library](https://github.com/boecker-lab/sirius-libs)

### [Changelog](https://boecker-lab.github.io/docs.sirius.github.io/changelog/)

### Integration of CSI:FingerID

Fragmentation trees and spectra can be directly uploaded from SIRIUS to a CSI:FingerID web service, without the need to access the (deprecated) CSI:FingerID
website. Results are retrieved from the web service and can be displayed in the SIRIUS graphical user interface. This functionality is
also available for the SIRIUS command-line tool. Training structures for CSI:FingerID's predictors are available through the CSI:FingerID web API:
<!--begin training-->

- https://www.csi-fingerid.uni-jena.de/v1.7.1-SNAPSHOT/api/fingerid/trainingstructures?predictor=1 (training structures for positive ion mode)
- https://www.csi-fingerid.uni-jena.de/v1.7.1-SNAPSHOT/api/fingerid/trainingstructures?predictor=2 (training structures for negative ion mode)

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

Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Alexander A. Aksenov, Alexey V. Melnik, Marvin Meusel, Pieter C. Dorrestein, Juho Rousu, and Sebastian Böcker, 
[SIRIUS 4: Turning tandem mass spectra into metabolite structure information.](https://doi.org/10.1038/s41592-019-0344-8)
*Nature Methods* 16, 299–302, 2019.

---
Martin A. Hoffmann and Louis-Félix Nothias and Marcus Ludwig and Markus Fleischauer and Emily C. Gentry and Michael Witting and Pieter C. Dorrestein and Kai Dührkop and Sebastian Böcker
[Assigning confidence to structural annotations from mass spectra with COSMIC](https://doi.org/10.1101/2021.03.18.435634)
bioRxiv, 2021. (Cite if you are using: *CSI:FingerID*, *COSMIC*)

Kai Dührkop, Louis-Félix Nothias, Markus Fleischauer, Raphael Reher, Marcus Ludwig, Martin A. Hoffmann, Daniel Petras, William H. Gerwick, Juho Rousu, Pieter C. Dorrestein and Sebastian Böcker.
[Systematic classification of unknown metabolites using high-resolution fragmentation mass spectra.](https://doi.org/10.1038/s41587-020-0740-8)
*Nature Biotechnology*, 2020. (Cite if you are using *CANOPUS*)

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


