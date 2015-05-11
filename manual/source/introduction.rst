###############
Introduction
###############

SIRIUS 3 is a *java* library for analyzing metabolites from tandem mass spectrometry data. It combines the analysis of isotope patterns in MS spectra with the analysis of fragmentation patterns in MS/MS spectra.

SIRIUS 3 requires **high mass accuracy** data. The mass deviation of your MS and MS/MS spectra should be within 20 ppm. Mass Spectrometry instruments like TOF, Orbitrap and FTICR usually provide high mass accuracy data, as well as coupled instruments like Q-TOF, IT-TOF or IT-Orbitrap. However, spectra measured with a quadrupole do not provide the high mass accuracy that is necessary for our method.

SIRIUS expects **MS and MS/MS** spectra as input. Although it is possible to omit the MS data, it will make the analysis much more time consuming and might give you worse results.

SIRIUS expects **processed peak lists**. It does not contain routines for peak picking from profiled spectra nor routines for merging spectra in an LC/MS run. There are several tools specialized for this task, e.g. XCMS or MZmine.

The main purpose of SIRIUS is to identify the molecular formula of the measured ion. Beside this, the software also annotates the spectrum providing a molecular formula for each fragment peak as well as detecting noise peaks. A **fragmentation tree** is predicted. This tree contains the predicted fragmentation reaction leading to the fragment peaks.

SIRIUS does not identify the (2D or 3D) structure of compounds, nor does it look up compounds in databases. There are other tools for this purpose, e.g. FingerId, MetFrag, CFM, and MAGMa.

SIRIUS can be used within an analysis pipeline. For example you can identify the molecular formula of the ion and the fragment peaks and use this information as input for other tools like FingerID or MAGMa to identify the 2D structure of the measured compound. For this purpose you can also use the SIRIUS library directly, instead of the command line interface. See :doc:`library`.


Modelling the fragmentation process as tree comes with some flaws: Namely **pull-ups** and **parallelograms**. A pull-up is a fragment which is inserted too deep into the trees. Due to our combinatorial model SIRIUS will always try to generate very deep trees, claiming that there are many small fragmentation steps instead of few larger ones. SIRIUS will for example prefer three consecuting C2H2 losses to a single C6H6 loss. This does not affect the quality of the molecular formula identification. But when interpreting fragmentation trees you should keep this side-effect of the optimization in mind.
**Parallelograms** are consecutive fragmentation processes that might happen in different orders. SIRIUS will always decide for one order of this fragmentation reactions, as this is the only valid way to model the fragmentation as tree.

Literature
***********

Mass Decomposition
""""""""""""""""""""

  * | **Faster mass decomposition.**
    | Kai Dührkop, Marcus Ludwig, Marvin Meusel and Sebastian Böcker
    | *In Proc. of Workshop on Algorithms in Bioinformatics (WABI 2013), volume 8126 of Lect Notes Comput Sci, pages 45-58. Springer, Berlin, 2013.*

  * | **DECOMP – from interpreting Mass Spectrometry peaks to solving the Money Changing Problem**
    | Sebastian Böcker, Zsuzsanna Lipták, Marcel Martin, Anton Pervukhin, and Henner Sudek
    | *Bioinformatics (2008) 24 (4): 591-593*

  * | **A Fast and Simple Algorithm for the Money Changing Problem**
    | Sebastian Böcker and Zsuzsanna Lipták
    | *Algorithmica (2007) 48 (4): 413-432*

Isotope Pattern Analysis
"""""""""""""""""""""""""""

  * | **SIRIUS: decomposing isotope patterns for metabolite identification**
    | Sebastian Böcker, Matthias C. Letzel, Zsuzsanna Lipták and Anton Pervukhin
    | *Bioinformatics (2009) 25 (2): 218-224*


Fragmentation Tree Computation
""""""""""""""""""""""""""""""""

  * | **Fragmentation trees reloaded.**
    | Kai Dührkop and Sebastian Böcker
    | *In Proc. of Research in Computational Molecular Biology (RECOMB 2015), volume 9029 of Lect Notes Comput Sci, pages 65-79. 2015.*

  * | **Speedy Colorful Subtrees.**
    | W. Timothy J. White, Stephan Beyer, Kai Dührkop, Markus Chimani and Sebastian Böcker
    | *In Proc. of Computing and Combinatorics Conference (COCOON 2015), 2015. To be presented.*

  * | **Finding Maximum Colorful Subtrees in practice.**
    | Imran Rauf, Florian Rasche, François Nicolas and Sebastian Böcker
    | *J Comput Biol, 20(4):1-11, 2013.*

  * | **Computing Fragmentation Trees from Tandem Mass Spectrometry Data**
    | Florian Rasche, Aleš Svatoš, Ravi Kumar Maddula, Christoph Böttcher, and Sebastian Böcker
    | *Analytical Chemistry (2011) 83 (4): 1243–1251*

  * | **Towards de novo identification of metabolites by analyzing tandem mass spectra**
    | Sebastian Böcker and Florian Rasche
    | *Bioinformatics (2008) 24 (16): i49-i55*
