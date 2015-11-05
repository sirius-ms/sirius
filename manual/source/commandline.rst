##########################
SIRIUS Commandline Tool
##########################

The SIRIUS commandline tool can be either called via the binary by simply running the command ``sirius`` in your commandline. Alternatively, you can run the sirius jar file using java with the command::

  java -jar sirius.jar

You can always use the ``--help`` option to get a documentation about the available commands and options. Assuming you want to analyze the example data given in the CASMI [#casmi]_ contest, you would execute the following on the commandline:

  sirius -1 MSpos_Challenge0.txt -2 MSMSpos_Challenge0.txt

.. _inputFormats:
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
* >collision: The same as >ms2 with the difference that you can provide a collision energy

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


If your input files are in *.ms* or *.mgf* format (containing MSLEVEL and PEPMASS meta information), you can omit the -1 and -2 flag. For example::

  sirius [OPTIONS] demo-data/ms

SIRIUS will pick the meta information (parentmass, ionization etc.) from the *.ms* files in the given directory. This allows SIRIUS to run in batch mode (analyzing multiple compounds without starting a new jvm process every time).

SIRIUS will output a candidate list containing the **rank**, **overall score**, **fragmentation pattern score**, **isotope pattern score**, the number of **explained peaks** and the relative amount of **explained intensity**. See the following example output::

  sirius  -z 354.1347 -p orbitrap  -1 demo-data/txt/chelidonine_ms.txt
          -2 demo-data/txt/chelidonine_msms1.txt demo-data/txt/chelidonine_msms2.txt

  1.) C20H19NO5         score: 33.17	tree: +27.48	iso: 5.69	peaks: 13	95.44 %
  2.) C16H22N2O5P	score: 32.35	tree: +26.77	iso: 5.58	peaks: 13	95.44 %
  3.) C12H23N3O7S	score: 24.62	tree: +24.62	iso: 0.00	peaks: 13	95.44 %
  4.) C18H17N4O4	score: 23.28	tree: +23.28	iso: 0.00	peaks: 14	95.79 %
  5.) C14H20N5O4P	score: 21.61	tree: +21.61	iso: 0.00	peaks: 14	95.79 %


The overall score is the sum of the fragmentation pattern score and the isotope pattern score. If the isotope pattern score is negative, it is set to zero. If at least one isotope pattern score is greater than 10, the isotope pattern is considered to have *good quality* and only the candidates with best isotope pattern scores are selected for further fragmentation pattern analysis.

If you want to analyze spectra measured with Orbitrap or FTICR, you should specify the appropiated analysis profile. A profile is a set of configuration options and scoring functions SIRIUS will use for its analysis. For example, the Orbitrap and FTICR profiles having tighter constraints for the allowed mass deviation but do not rely so much on the intensity of isotope peaks. You can set the profile with the ``-p <name>`` option. By default, qtof is used as profile.

SIRIUS recognizes the following options:

.. option:: -p <name>, --profile <name>

  Specify the used analysis profile. Choose either **qtof**, **orbitrap** or **fticr**. By default, **qtof** is selected.

.. option:: -o <dirname>, --output <dirname>

  Specify the output directory. If given, SIRIUS will write the computed trees into this directory.

.. option:: -O <format>, --format <format>

  Specify the format of the output of the fragmentation trees. This can be either **json** (machine readable), **dot** (visualizable) or **sirius** (can be viewed with the Sirius User Interface).

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

  sirius -f C20H19NO5 -2 demo-data/txt/chelidonine_msms2.txt demo-data/txt/chelidonine_msms2.txt

********************************
Visualizing Fragmentation Trees
********************************

SIRIUS supports three output formats for fragmentation trees: dot (graphviz format), json (machine readable format), and sirius (can be viewed with the Sirius User Interface). The commandline tool Graphviz [#graphviz]_ can transform dot files into image formats (pdf, svg, png etc.). After installing Graphviz you can display tree files as follows::

  sirius -p orbitrap -f C20H17NO6 -o trees demo-data/ms/Bicuculline.ms
  dot -Tpdf -O trees/Bicuculline.dot

This creates a file Bicuculline.dot.pdf (:ref:`Fig.1 <treeimg>`). Remark that SIRIUS uses automatically the file name of the input spectrum to name the output file. You can specify another filename with the **-o** option (as long as only one tree is computed).

  sirius -p orbitrap -f C20H17NO6 -o compound.dot demo-data/ms/Bicuculline.ms
  dot -Tpdf -O compound.dot

.. _treeimg:

.. figure:: images/tree.pdf

  The output of the dot program to visualize the computed fragmentation tree

********************************
Demo Data
********************************

You can download some sample spectra from the SIRIUS website at http://bio.informatik.uni-jena.de/sirius2/wp-content/uploads/2015/05/demo.zip

The demo-data contain examples for three different data formats readable by SIRIUS. The mgf folder contain an example for a mgf file containing a single compound with several MS/MS spectra measured on an Orbitrap instrument. SIRIUS recognizes that these MS/MS spectra belong to the same compound because they have the same parent mass. To analyze this compound, run::

  sirius -p orbitrap demo-data/mgf/laudanosine.mgf

The output is::

  1.) C21H27NO4	        score: 25.41	tree: +17.55	iso: 7.86	peaks: 12	97.94 %
  2.) C17H30N2O4P	score: 21.46	tree: +13.97	iso: 7.49	peaks: 12	97.94 %
  3.) C15H28N5O3P	score: 15.00	tree: +15.00	iso: 0.00	peaks: 11	87.04 %
  4.) C19H25N4O3	score: 14.66	tree: +14.66	iso: 0.00	peaks: 11	87.16 %
  5.) C14H27N7O2S	score: 13.69	tree: +13.69	iso: 0.00	peaks: 11	97.38 %

This is a ranking list of the top molecular formula candidates. The best candidate is C21H27NO4 with a overall score of 25.41. This score is the sum of the fragmentation pattern scoring (17.55) and the isotope pattern scoring (7.86). For the last three candidates, the isotope pattern scoring is 0. In fact, this score can never fall below zero. If all isotope pattern scores are zero, you can assume that the isotope pattern has very low quality and cannot be used to determine the molecular formula. If the isotope pattern score of the top candidate is over 10, it is assumed to be a high quality isotope pattern. In this case, the isotope pattern is also used to filter out unlikely candidates and speed up the analysis.

The last two columns contain the number of explained peaks in MS/MS spectrum as well as the relative amount of explained intensity. The last value should usually be over 80 % or even 90 %. If this value is very low you either have strange high intensive noise in your spectrum or the allowed mass deviation might be too low to explain all the peaks.

If you want to look at the trees, you have to add the output option::

  sirius -p orbitrap -o outputdir demo-data/mgf/laudanosine.mgf

Now, SIRIUS will write the computed trees into the *outputdir* directory. You can visualize this trees in pdf format using Graphviz::

  dot -Tpdf -O outputdir/laudanosine_1_C21H27NO4.dot

This creates a pdf file *outputdir/laudanosine_1_C21H27NO4.dot.pdf*.

The directory *ms* contains two examples of the ms format. Each file contains a single compound measured with an Orbitrap instrument. To analyze this compound run::

  sirius -p orbitrap -o outputdir demo-data/ms/Bicuculline.ms

As the ms file already contains the correct molecular formula, SIRIUS will directly compute the tree. For such cases (as well as when you specify exactly one molecular formula via *-f* option) you can also specify the concrete filename of the output file::

  sirius -p orbitrap -o mycompound.dot demo-data/ms/Bicuculline.ms

If you want to enforce a molecular formula analysis and ranking (although the correct molecular formula is given within the file) you can specify the number of candidates with the *-c* option::

  sirius -p orbitrap -c 5 demo-data/ms/Bicuculline.ms

SIRIUS will now ignore the correct molecular formula in the file and output the 5 best candidates.


The txt folder contains simple peaklist files. Such file formats can be easily extracted from Excel spreadsheets. However, they do not contain meta information like the MS level and the parent mass. So you have to specify this information via commandline options::

  sirius  -p orbitrap  -z 354.134704589844 -1 demo-data/txt/chelidonine_ms.txt
          -2 demo-data/txt/chelidonine_msms1.txt demo-data/txt/chelidonine_msms2.txt

The demo data contain a clean MS spectrum (e.g. there is only one isotope pattern contained in the MS spectrum). In such cases, SIRIUS can infer the correct parent mass from the MS data (by simply using the monoisotopic mass of the isotope pattern as parent mass). So you can omit the *-z* option in this cases.

.. rubric:: Footnotes

.. [#gnps] http://gnps.ucsd.edu/

.. [#casmi] http://casmi-contest.org/2014/example/MSpos_Challenge0.txt

.. [#graphviz] http://www.graphviz.org/
