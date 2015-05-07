##########################
SIRIUS Commandline Tool
##########################

The SIRIUS commandline tool can be either called via the binary by simply running the command ``sirius`` in your commandline. Alternatively, you can run the sirius jar file using java with the command::

  java -jar sirius.jar

You can always use the ``--help`` option to get a documentation about the available commands and options. Assuming you want to analyze the example data given in the CASMI [#casmi]_ contest, you would execute the following on the commandline:

  sirius -1 MSpos_Challenge0.txt -2 MSMSpos_Challenge0.txt

*************************
Supported Input Formats
*************************

---------------------------
Mass Spectra
---------------------------

The input of SIRIUS are MS and MS/MS spectra as simple peak lists. SIRIUS can read csv files which contain on each line a m/z and an intensity value separated by either a whitespace, a comma or a TAB character. For example::

  185.041199 4034.674316
  203.052597 12382.624023
  245.063171 50792.085938
  275.073975 124088.046875
  305.084106 441539.125
  335.094238 4754.061035
  347.09494 13674.210938
  365.105103 55487.472656

The intensity values can be arbitrary floating point values. SIRIUS will transform the intensities into relative intensities, so only the ratio between the intensity values is important.

SIRIUS also supports the mgf (mascot generic format). This file format was developed for peptide spectra for the mascot search engine. Each spectrum in a mgf file can contain many spectra each starting with ``BEGIN IONS`` and ending with ``END IONS``. Peaks are again written as pairs of m/z and intensity values separated by whitespaces with one peak per line. Further meta information can be given as NAME=VALUE pairs. SIRIUS recognizes the following meta information:
* PEPMASS: contains the measured mass of the ion (e.g. the parent peak)
* CHARGE: contains the charge of the ion. As SIRIUS supports only single charged ions, this value can be either 1+ or 1-.
* MSLEVEL: should be 1 for MS spectra and 2 for MS/MS spectra. SIRIUS will treat higher values automatically as MS/MS spectra, although, it might be that it supports MSn spectra in future versions.

This is an example for a mgf file::

  BEGIN IONS
  PEPMASS=438.32382
  CHARGE=1+
  MSLEVEL=2
  185.041199 4034.674316
  203.052597 12382.624023
  245.063171 50792.085938
  275.073975 124088.046875
  305.084106 441539.125
  335.094238 4754.061035
  347.09494 13674.210938
  365.105103 55487.472656
  END IONS

See also the GNPS [#gnps]_ database for other examples of mgf files.

A disadvantage of these data formats is that they do not contain all information necessary for SIRIUS to perform the computation. Missing meta information have to be provided via the commandline. Therefore, SIRIUS supports also an own file format very similar to the mgf format above. The file ending of this format is **.ms**. Each file contains one measured compound (but arbitrary many spectra). Each line may contain a peak (given as m/z and intensity separated by a whitespace), meta information (starting with the **>** symbol followed by the information type, a whitespace and the value) or comments (starting with the **#** symbol). The following fields are recognized by SIRIUS:

* >compound: The name of the measured compound (or any placeholder). This field is **mandatory**.
* >parentmass: the mass of the parent peak
* >formula: The molecular formula of the compound. This information is helpful if you already know the correct molecular formula and just want to compute a tree or recalibrate the spectrum
* >ion: the ionization mode. See :ref:`ions` for the format of ion modes.
* >charge: is redundant if you already provided the ion mode. Otherwise, it gives the charge of the ion (1 or -1).
* >ms1: All peaks after this line are interpreted as MS peaks
* >ms2: All peaks after this line are interpreted as MS/MS peaks
* >collision: The same as >ms2, just that you can provide a collision energy

An example for a .ms file::

  >compound Gentiobiose
  >formula C12H22O11
  >ionization [M+Na]+
  >parentmass 365.10544

  >ms1
  365.10543 85.63
  366.10887 11.69
  367.11041 2.67

  >collision 20
  185.041199 4034.674316
  203.052597 12382.624023
  245.063171 50792.085938
  275.073975 124088.046875
  305.084106 441539.125
  335.094238 4754.061035
  347.09494 13674.210938
  365.105103 55487.472656

.. _ions:

Ion Modes
---------------------------

Whenever SIRIUS requires the ion mode, it should be given in the following format::

  [M+ADDUCT]+ for positive ions
  [M+ADDUCT]- for negative ions
  [M-ADDUCT]- for losses
  [M]+ for instrinsically charged compounds

ADDUCT is the molecular formula of the adduct. The most common ionization modes are ``[M+H]+``, ``[M+Na]+``, ``[M-H]-``, ``[M+Cl]-``. Currently, SIRIUS supports only single-charged compounds, so ``[M+2H]2+`` is not valid. For intrinsic charged compounds ``[M]+`` and ``[M]-`` should be used.

.. _formulas:

Molecular Formulas
---------------------------

Molecular Formulas in SIRIUS must not contain brackets. So ``2(C2H2)`` is not a valid molecular formula. Write ``C4H4`` instead. Furthermore, all molecular formulas in SIRIUS are always neutral, there is no possibility to add a charge on a molecular formula (instead, charges are given separately). So ``CH3+`` is not a valid molecular formula. Write ``CH3`` instead and provide the charge separately via commandline option.

.. _alphabets:

Chemical Alphabets
---------------------------

Whenever SIRIUS requires the chemical alphabet, you have to provide which elements should be considered and what is the maximum amount for each element. Chemical alphabets are written like molecular formulas. The maximum amount of an element is written in square brackets behind the element. If no square brackets are given, the element might occur arbitrary often. The standard alphabet is CHNOP[5]S, allowing the elements C, H, N O and S as well as up to five times the element P.

********************************
Identifying Molecular Formulas
********************************

The main purpose of SIRIUS is identifying the molecular formula of the measured ion. The syntax for this command is::

  sirius [OPTIONS] -z <PARENTMASS> -i <IONIZATION> -1 <MS FILE> -2 <MS/MS FILE>

Where MS FILE and MS/MS FILE are either csv or mgf files. If mgf files are used, you might omit the PARENTMASS option. If you omit the IONIZATION option, [M+H]+ is used as default. It is also possible to give a list of MS/MS files if you have several measurements of the same compound with different collision energies. SIRIUS will merge these MS/MS spectra into one spectrum.


If your input files are in *.ms* format, you can omit the -1 and -2 flag. For example::

  sirius [OPTIONS] someDirectory/

SIRIUS will pick the meta information (parentmass, ionization etc.) from the *.ms* files in the given directory. This allows SIRIUS to run in batch mode (analyzing multiple compounds without starting a new jvm process every time).

SIRIUS will output a candidate list containing the **rank**, **overall score**, **fragmentation pattern score**, **isotope pattern score**, the number of **explained peaks** and the relative amount of **explained intensity**. See the following example output::

  sirius -z 217.04954 -1 bergapten_ms.csv -2 bergapten_msms.csv

  1.) C12H8O4 score: 60.60  tree: 52.26 iso: 8.34 peaks: 14   98.82 %
  2.) C8H11NO4P score: 46.32  tree: 38.95 iso: 7.37 peaks: 14   98.82 %
  3.) C4H12N2O6S score: -0.77  tree: -0.77 iso: 0.00 peaks: 3   80.29 %
  4.) C6H9N4O3P score: -3.76  tree: -3.76 iso: 0.00 peaks: 5   83.14 %
  5.) C10H6N3O3 score: -4.97  tree: -4.97 iso: 0.00 peaks: 4    2.70 %

The overall score is the sum of the fragmentation pattern score and the isotope pattern score. If the isotope pattern score is negative, it is set to zero. If at least one isotope pattern score is greater than 10, the isotope pattern is considered to have *good quality* and only the candidates with best isotope pattern scores are selected for further fragmentation pattern analysis.

If you want to analyze spectra measured with Orbitrap or FTICR, you should specify the appropiated analysis profile. A profile is a set of configuration options and scoring functions SIRIUS will use for its analysis. For example, the Orbitrap and FTICR profiles having tighter constraints for the allowed mass deviation but do not rely so much on the intensity of isotope peaks. You can set the profile with the ``-p <name>`` option. By default, qtof is used as profile.

SIRIUS recognizes the following options:

.. option:: -p <name>, --profile <name>

  Specify the used analysis profile. Choose either **qtof**, **orbitrap** or **fticr**. By default, **qtof** is selected.

.. option:: -o <dirname>, --output <dirname>

  Specify the output directory. If given, SIRIUS will write the computed trees into this directory.

.. option:: -O <format>, --format <format>

  Specify the format of the output of the fragmentation trees. This can be either json (machine readable) or dot (visualizable)

.. option:: -f [list of formulas], --formula [list of formulas]

  Specify a list of candidate formulas (separated by whitespaces) that should be considered during analysis. This option is helpful if you performed a database search beforehand and only want to consider molecular formulas found in the database. It is recommendet to first consider all molecular formulas (and omit this option) and filter the candidate list afterwards. However, specifying a subset of molecular formulas with this option might greatly improve the speed of the analysis especially for large molecules.

.. option:: -a, --annotate

  If set, SIRIUS will write the annotated spectrum containing the explanations (molecular formulas) for all identified peaks in a csv file within the specified output directory.

.. option:: -c <num>, --candidates <num>

  The number of candidates in the output. By default, SIRIUS will only write the five best candidates.

.. option:: -s <val>, --isotope <val>

  This option specifies the way SIRIUS will handle the isotope patterns. If it is set to **omit**, SIRIUS will omit the isotope pattern analysis. If it is set to **filter**, SIRIUS will use the isotope pattern to select a subset of candidates before starting the fragmentation pattern analysis (this will improve the speed of the analysis). Only if it is set to **score**, SIRIUS will use it for filtering and scoring the candidates. The default setting is **score**.

.. option:: -e <alphabet>, --elements <alphabet>

  Specify the used chemical alphabet. See :ref:`alphabets`. By default, ``CHNOP[5]S`` is used.

.. option:: -i <ion>, --ion <ion>

  Specify the used ionization. See :ref:`ions`. By default, ``[M+H]+`` is used.

.. option:: -z <mz>, --parentmass <mz>

  Specify the parentmass of the input spectra. You have to give the exact measured value, not the selected ion mass.

.. option:: -1 <file>, --ms1 <file>

  Specify the file path to the MS spectrum of the measured compound.

.. option:: -2 <file>, --ms2 <file>

  Specify one or multiple file paths to the MS/MS spectra of the measured compound

.. option:: --ppm-max <value>

  Specify the allowed mass deviation of the fragment peaks in ppm. By default, Q-TOF instruments use 10 ppm and Orbitrap instruments use 5 ppm.

.. option:: --auto-charge

  If this option is set, SIRIUS will annotate the fragment peaks with ion formulas instead of neutral molecular formulas. Use this option if you do not know the correct ionization.

.. option:: --no-recalibrate

  If this option is set, SIRIUS will not recalibrate the spectrum during the analysis.

.. option:: -h, --help

  display help


See the following examples for running SIRIUS commandline tool::

  sirius -p orbitrap -z 239.0315 -i [M+Na]+ -1 bergapten_ms.csv
                  -2 bergapten_msms1.csv bergapten_msms2.csv
  sirius -p fticr -z 215.0350 -i [M-H]- -e CHNOPSCl[2] -c 10 -s omit
                  -1 unknown_ms1.csv -2 unknown_ms2.csv
  sirius -p qtof -z 215.035 -i 1- --auto-charge -2 unknown_ms2.csv
  sirius -c 10 -o trees -O json msdir
  sirius -f C6H12O6 C5H6N7O C7H16OS2 -i [M+H]+ -1 ms.csv -2 msms.csv


********************************
Computing Fragmentation Trees
********************************

If you already know the correct molecular formula and just want to compute a tree, you can specify a single molecular formula with the ``-f`` option. SIRIUS will then only compute a tree for this molecular formula. If your input data is in ``.ms`` format, the molecular formula might be already specified within the file. If a molecular formula is specified, the parentmass can be omitted. However, you still have to specify the ionization (except for default value ``[M+H]+``)::

  sirius -f C6H12O6 -2 msms.csv
  sirius -f C6H12O6 -i [M-H]- -2 msms_neg.csv

********************************
Visualizing Fragmentation Trees
********************************

SIRIUS supports two output formats for fragmentation trees: dot (graphviz format) and json (machine readable format). The commandline tool Graphviz [#graphviz]_ can transform dot files into image formats (pdf, svg, png etc.). After installing Graphviz you can display tree files as follows::

  sirius -o trees -1 demo/ms1.txt -2 demo/15eV.txt demo/25eV.txt demo/40eV.txt demo/55eV.txt
  graphviz -Tpdf -O trees/15eV_1_C14H19NO4.dot

This creates a file 15eV_1_C14H19NO4.dot.pdf (:ref:`Fig.1 <treeimg>`). Remark that SIRIUS uses automatically the file name of the first MS/MS spectrum to name the output file.

.. _treeimg:

.. figure:: images/tree.pdf

  The output of the dot program to visualize the computed fragmentation tree

********************************
Demo Data
********************************

You can download some sample spectrum from the SIRIUS website at http://bio.informatik.uni-jena.de/sirius2/wp-content/uploads/2015/05/demo.zip

In the zip file measurements of two compounds are contained. The files cafeoyl_choline_ms1.txt, cafeoyl_choline_15eV.txt, cafeoyl_choline_25eV.txt, cafeoyl_choline_40eV.txt and cafeoyl_choline_55eV.txt are measurements of the compound cafeoyl choline with different collision energies. To identify this compound with SIRIUS execute the following in your commandline::

  sirius identify -1 cafeoyl_choline_ms1.txt -2 cafeoyl_choline_15eV.txt cafeoyl_choline_25eV.txt
                                                cafeoyl_choline_40eV.txt and cafeoyl_choline_55eV.txt



The console output should be something like this::

  Compute 'cafeoyl_choline_15eV.txt'
  |==========100 %==========|	   computation finished

  recalibrate trees
  |==========100 %==========|	   computation finished
  1.) C14H19NO4	score: 21.62	tree: +10.09	iso: 11.53	peaks: 6	66.95 %
  2.) C10H22N2O4P	score: 19.61	tree: +8.89	iso: 10.71	peaks: 8	98.92 %
  3.) C12H17N4O3	score: 19.01	tree: +6.43	iso: 12.57	peaks: 5	39.89 %
  4.) C11H18N6P	score: 2.97	tree: -8.41	iso: 11.38	peaks: 1	0.00 %

The ranklist shows the correct molecular formula of cafeoyl choline (C14H19NO4) in the top position, explaining 6 peaks (and 66.95% of the intensity) with an overall score of 21.62.


.. rubric:: Footnotes


.. [#gnps] http://gnps.ucsd.edu/

.. [#casmi] http://casmi-contest.org/2014/example/MSpos_Challenge0.txt

.. [#graphviz] http://www.graphviz.org/
