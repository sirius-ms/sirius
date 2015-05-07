###############
Introduction
###############

SIRIUS 3 is a *java* library for analyzing metabolites from tandem mass spectrometry data. It combines the analysis of isotope patterns in MS spectra with the analysis of fragmentation patterns in MS/MS spectra.

SIRIUS 3 requires **high mass accuracy** data. The mass deviation of your MS and MS/MS spectra should be within 20 ppm. Mass Spectrometry instruments like TOF, Orbitrap and FTICR usually provide high mass accuracy data, as well as coupled instruments like Q-TOF, IT-TOF or IT-Orbitrap. However, spectra measured with a quadrupole do not provide the high mass accuracy that is necessary for our method.

SIRIUS expects **MS and MS/MS** spectra as input. Although it is possible to omit the MS data, it will make the analysis much more time consuming and might give you worse results.

SIRIUS expects **processed peak lists**. It does not contain routines for peak picking from profiled spectra nor routines for merging spectra in an LC/MS run. There are several tools specialized for this task, e.g. XMCS or Mzmine.

The main purpose of SIRIUS is to identify the molecular formula of the measured ion. Beside this, the software also annotates the spectrum providing a molecular formula for each fragment peak as well as detecting noise peaks. A **fragmentation tree** is predicted. This tree contains the predicted fragmentation reaction leading to the fragment peaks.

SIRIUS does not identify the (2D or 3D) structure of compounds, nor does it look up compounds in databases. There are other tools for this purpose, e.g. FingerId, MetFrag, CFM, and MAGMa.

SIRIUS can be used within an analysis pipeline. For example you can identify the molecular formula of the ion and the fragment peaks and use this information as input for other tools like FingerID or MAGMa to identify the 2D structure of the measured compound. For this purpose you can also use the SIRIUS library directly, instead of the command line interface. See :doc:`library`.


Modelling the fragmentation process as tree comes with some flaws: Namely **pull-ups** and **parallelograms**. A pull-up is a fragment which is inserted too deep into the trees. Due to our combinatorial model SIRIUS will always try to generate very deep trees, claiming that there are many small fragmentation steps instead of few larger ones. SIRIUS will for example prefer three consecuting C2H2 losses to a single C6H6 loss. This does not affect the quality of the molecular formula identification. But when interpreting fragmentation trees you should keep this side-effect of the optimization in mind.
**Parallelograms** are consecutive fragmentation processes that might happen in different orders. SIRIUS will always decide for one order of this fragmentation reactions, as this is the only valid way to model the fragmentation as tree.
