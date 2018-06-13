*<span style="color: #808080;">SIRIUS and CSI:FingerID are offered to
the public as freely available resources. Use and re-distribution of the
methods, in whole or in part, for commercial purposes requires explicit
permission of the authors and explicit acknowledgment of the source
material and the original publications. We ask that users who use SIRIUS
and CSI:FingerID cite the corresponding papers in any resulting
publications.</span>*

Contact: ![](https://raw.githubusercontent.com/boecker-lab/sirius_frontend/release/manual/source/siriusMailtrans.png)

SIRIUS is a new java-based software framework for discovering a
landscape of de-novo identification of metabolites using single and
tandem mass spectrometry. SIRIUS uses isotope pattern analysis for
detecting the molecular formula and further analyses the fragmentation
pattern of a compound using fragmentation trees. 
<span style="color: #339966;">Fragmentation trees can be uploaded to CSI:FingerID via a web service,
and results can be displayed in the SIRIUS graphical user interface.</span> (This is
also possible using the command line version of SIRIUS.) 
<span style="color: #339966;">This is the recommended way of using
CSI:FingerID.</span>

## Download Links

<!--begin download-->

### Documentation
- [SIRIUS Training material](https://bio.informatik.uni-jena.de/sirius-training/)
- [SIRIUS manual](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/ms/sirius/4.0/sirius-4.0-manual.pdf)
- [demo data](https://bio.informatik.uni-jena.de/wp/wp-content/uploads/2015/05/demo.zip)

### SIRIUS+CSI:FingerID GUI and CLI - Version 4.0 (Build 21 from 2018-06-13)
- for Windows [32bit](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/ms/sirius/4.0/sirius-4.0-win32.zip) / [64bit](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/ms/sirius/4.0/sirius-4.0-win64.zip)
- for Linux [32bit](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/ms/sirius/4.0/sirius-4.0-linux32.zip) / [64bit](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/ms/sirius/4.0/sirius-4.0-linux64.zip)
- for Mac [64bit](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/ms/sirius/4.0/sirius-4.0-osx64.zip)

### SIRIUS+CSI:FingerID Commandline only - Version 4.0 (Build 21 from 2018-06-13)
- for Windows [32bit](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/ms/sirius/4.0/sirius-4.0-win32-headless.zip) / [64bit](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/ms/sirius/4.0/sirius-4.0-win64-headless.zip)
- for Linux [32bit](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/ms/sirius/4.0/sirius-4.0-linux32-headless.zip) / [64bit](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/ms/sirius/4.0/sirius-4.0-linux64-headless.zip)
- for Mac [64bit](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/ms/sirius/4.0/sirius-4.0-osx64-headless.zip)

### Sources on GitHub
- [SIRIUS frontend](https://github.com/boecker-lab/sirius_frontend)
- [SIRIUS library](https://github.com/boecker-lab/sirius)

<!--end download-->

### Integration of CSI:FingerID

Fragmentation trees and spectra can be directly uploaded from SIRIUS to
a CSI:FingerID web service (without the need to access the CSI:FingerID
website). Results are retrieved from the web service and can be
displayed in the SIRIUS graphical user interface. This functionality is
also available for the SIRIUS command-line tool.

### Fragmentation Tree Computation

The manual interpretation of tandem mass spectra is time-consuming and
non-trivial. SIRIUS analyses the fragmentation pattern resulting in
hypothetical fragmentation trees in which nodes are annotated with
molecular formulas of the fragments and arcs represent fragmentation
events. SIRIUS allows for the automated and high-throughput analysis of
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

## Main citations

Kai Dührkop and Sebastian Böcker. [Fragmentation trees
reloaded.](http://dx.doi.org/10.1007/978-3-319-16706-0_10)  *J
Cheminform*, 8:5, 2016. (Cite this for *fragmentation pattern analysis
and fragmentation tree computation*) 

Kai Dührkop, Huibin Shen, Marvin
Meusel, Juho Rousu, and Sebastian Böcker. [Searching molecular structure
databases with tandem mass spectra using
CSI:FingerID](http://dx.doi.org/10.1073/pnas.1509788112). *Proc Natl
Acad Sci U S A*, 112(41):12580-12585, 2015. (cite this when *using
CSI:FingerID*) 

Sebastian Böcker, Matthias C. Letzel, Zsuzsanna Lipták
and Anton Pervukhin. [SIRIUS: decomposing isotope patterns for
metabolite
identification.](http://bioinformatics.oxfordjournals.org/content/25/2/218.full) *Bioinformatics*
(2009) 25 (2): 218-224. (Cite this for *isotope pattern analysis*)

### Additional citations

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
Data](http://pubs.acs.org/doi/abs/10.1021/ac101825k). *Analytical
Chemistry* (2011) 83 (4): 1243–1251. (Cite this for *introduction of
fragmentation trees* as used by SIRIUS)

Sebastian Böcker and Florian Rasche. [Towards de novo identification of metabolites by analyzing
tandem mass
spectra](http://bioinformatics.oxfordjournals.org/content/24/16/i49.abstract).
*Bioinformatics* (2008) 24 (16): i49-i55. (The very *first paper to
mention fragmentation trees* as used by SIRIUS)

## License

Starting with version 3.4, SIRIUS is licensed under the [GNU General
Public License (GPL)](https://www.gnu.org/licenses/gpl.html). If you integrate SIRIUS into other software, we
strongly encourage you to make the usage of SIRIUS as well as the
literature to cite transparent to the user.
