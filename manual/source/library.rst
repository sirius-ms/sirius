####################
SIRIUS Java Library
####################

You can integrate the SIRIUS library in your java project, either by using Maven [#maven]_ or by including the jar file directly. The latter is not recommendet, as the SIRIUS jar contains also dependencies to other external libraries.

Maven Integration
----------------------
.. highlight:: xml

Add the following repository to your pom file::

  <distributionManagement>
    <repository>
        <id>bioinf-jena</id>
        <name>bioinf-jena-releases</name>
        <url>http://bio.informatik.uni-jena.de/artifactory/libs-releases-local</url>
    </repository>
  </distributionManagement>

Now you can integrate SIRIUS in your project by adding the following dependency::

  <dependency>
    <groupId>de.unijena.bioinf</groupId>
    <artifactId>SiriusCLI</artifactId>
    <version>3.0.0</version>
  </dependency>

.. highlight:: text

Main API
----------------------

The main class in SIRIUS is **de.unijena.bioinf.sirius.Sirius**. It is basically a wrapper around the important functionalities of the library. Although there are special classes for all parts of the analysis pipeline it is recommended to only use the Sirius class as the API of all other classes might change in future releases. The Sirius class also provides factory methods for the most important data structures. Although, for many of this data structures you could also use their constructors directly, it is recommended to use the methods in the Sirius class.

.. java:package:: de.unijena.bioinf.sirius

.. java:type:: public class Sirius

  The main class in SIRIUS. Provides the basic functionality of the method.

  :param profile: the profile name. Can be one of 'qtof', 'orbitrap' or 'fticr'. If ommited, the default profile ('qtof') is used.

The main functions of SIRIUS are either identifying the molecular formula of a given MS/MS experiment or computing a tree for a given molecular formula and MS/MS spectrum. The Sirius class provides two methods for this purpose: identify and compute. The basic input type is an Ms2Experiment. It can be seen as a set of MS/MS spectra derived from the same precursor as well as a MS spectrum containing this precursor peak. The output of Sirius is an instance of IdentificationResult, containing the score and the corresponding fragmentation tree for the candidate molecular formula.

Create Datastructures
**********************

Sirius provides the following functions to create the basic data structures:

.. java:method:: public Spectrum<Peak> wrapSpectrum(double[] mz, double[] intensities)

  Wraps an array of m/z values and and array of intensity values into a spectrum object that can be used by the SIRIUS library. The resulting spectrum is a lightweight view on the array, so changes in the array are reflected in the spectrum. The spectrum object itself is immutable.

  :param mz: mass to charge ratios
  :param intensities: intensity values. Can be normalized or absolute values - SIRIUS will normalize them itself if necessary
  :return: view on the arrays implementing the Spectrum interface

.. java:method:: public Element getElement(String symbol)

  Lookup the symbol in the periodic table and returns the corresponding Element object or null if no element with this symbol exists.

  :param symbol: symbol of the element, e.g. H for hydrogen or Cl for chlorine
  :return: instance of Element class

.. java:method:: public Ionization getIonization(String name)

  Lookup the ionization name and returns the corresponding ionization object or null if no ionization with this name is registered. The name of an ionization has the syntax [M+ADDUCT]CHARGE, for example [M+H]+ or [M-H]-.

  :param name: name of the ionization
  :return: Adduct instance

.. java:method:: public Charge getCharge(int charge)

  Charges are subclasses of Ionization. So they can be used everywhere as replacement for ionizations. A charge is very similar to the [M]+ and [M]- ionizations. However, the difference is that [M]+ describes an intrinsically charged compound where the Charge +1 describes an compound with unknown adduct.

  :param charge: either 1 for positive or -1 for negative charges.
  :return: a Charge instance which is also a subclass of Ionization

.. java:method:: public Deviation getMassDeviation(int ppm, double abs)

  Creates a Deviation object that describes a mass deviation as maximum of a relative term (in ppm) and an absolute term. Usually, mass accuracy is given as relative term in ppm, as measurement errors increase with higher masses. However, for very small compounds (and fragments!) these relative values might overestimate the mass accurary. Therefore, an absolute value have to be given.

  :param ppm: mass deviation as relative value (in ppm)
  :param abs: mass deviation as absolute value (m/z)
  :return: Deviation object

.. java:method:: MolecularFormula parseFormula(String f)

  Parses a molecular formula from the given string

  :param f: molecular formula (e.g. in Hill notation)
  :return: immutable molecular formula object

.. java:method::  public Ms2Experiment getMs2Experiment(MolecularFormula formula, Ionization ion, Spectrum<Peak> ms1, Spectrum... ms2)
                  public Ms2Experiment getMs2Experiment(double parentmass, Ionization ion, Spectrum<Peak> ms1, Spectrum... ms2)

  Creates a Ms2Experiment object from the given MS and MS/MS spectra. A Ms2Experiment is NOT a single run or measurement, but a measurement of a concrete compound. So a MS spectrum might contain several Ms2Experiments. However, each MS/MS spectrum should have on precursor or parent mass. All MS/MS spectra with the same precursor together with the MS spectrum containing this precursor peak can be seen as one Ms2Experiment.

  :param formula: neutral molecular formula of the compound
  :param parentmass: if neutral molecular formula is unknown, you have to provide the ion mass
  :param ion: ionization mode (can be an instance of Charge if the exact adduct is unknown)
  :param ms1: the MS spectrum containing the isotope pattern of the measured compound. Might be null
  :param ms2: a list of MS/MS spectra containing the fragmentation pattern of the measured compound
  :return: a MS2Experiment instance, ready to be analyzed by SIRIUS

.. java:method:: public FormulaConstraints getFormulaConstraints(String constraints)

  Formula Constraints consist of a chemical alphabet (a subset of the periodic table, determining which elements might occur in the measured compounds) and upperbounds for each of this elements. A formula constraint can be given like a molecular formula. Upperbounds are written in square brackets or omitted, if any number of this element should be allowed.

  :param constraints: string representation of the constraint, e.g. "CHNOP[5]S[20]"
  :return: formula constraint object


Provided Algorithms
**********************

.. java:method:: List<IdentificationResult> identify(Ms2Experiment uexperiment, int numberOfCandidates, boolean recalibrating, IsotopePatternHandling deisotope, Set<MolecularFormula> whiteList)

  Identify the molecular formula of the measured compound by combining an isotope pattern analysis on MS data with a fragmentation pattern analysis on MS/MS data

  :param uexperiment: input data
  :param numberOfCandidates: number of candidates to output
  :param recalibrating: true if spectra should be recalibrated during tree computation
  :param deisotope: set this to 'omit' to ignore isotope pattern, 'filter' to use it for selecting molecular formula candidates or 'score' to rerank the candidates according to their isotope pattern
  :param whiteList: restrict the analysis to this subset of molecular formulas. If this set is empty, consider all possible molecular formulas
  :return: a list of identified molecular formulas together with their tree


.. java:method:: public IdentificationResult compute(Ms2Experiment experiment, MolecularFormula formula, boolean recalibrating)

  Compute a fragmentation tree for the given MS/MS data using the given neutral molecular formula as explanation for the measured compound

  :param experiment: input data
  :param formula: neutral molecular formula of the measured compound
  :param recalibrating: true if spectra should be recalibrated during tree computation
  :return: A single instance of IdentificationResult containing the computed fragmentation tree

.. java:method::  public List<MolecularFormula> decompose(double mass, Ionization ion, FormulaConstraints constr, Deviation dev)
                  public List<MolecularFormula> decompose(double mass, Ionization ion, FormulaConstraints constr)

  Decomposes a mass and return a list of all molecular formulas which ionized mass is near the measured mass.
  The maximal distance between the neutral mass of the measured ion and the theoretical mass of the decomposed formula depends on the chosen profile. For qtof it is 10 ppm, for Orbitrap and FTICR it is 5 ppm.

  :param mass: mass of the measured ion
  :param ion: ionization mode (might be a Charge, in which case the decomposer will enumerate the ion formulas instead of the neutral formulas)
  :param constr: the formula constraints, defining the allowed elements and their upperbounds
  :param dev: the allowed mass deviation of the measured ion from the theoretical ion masses
  :return: list of molecular formulas which theoretical ion mass is near the given mass

.. java:method:: public Spectrum<Peak> simulateIsotopePattern(MolecularFormula compound, Ionization ion)

  Simulates an isotope pattern for the given molecular formula and the chosen ionization

  :param compound: neutral molecular formula
  :param ion: ionization mode (might be a Charge)
  :return: spectrum containing the theoretical isotope pattern of this compound

Output Type
**********************

.. java:package:: de.unijena.bioinf.sirius

.. java:type:: public class IdentificationResult

  The compute and identify methods return instances of IdentificationResult. This class wraps a tree and its scores. You can write the tree to a file using the writeTreeToFile method.


.. java:method:: public void writeTreeToFile(File target) throws IOException

  Writes the tree into a file. The file format is determined by the file ending (either '.dot' or '.json')

  :param target: file name

.. java:method:: public void writeAnnotatedSpectrumToFile(File target) throws IOException

  Writes the annotated spectrum into a csv file.

  :param target: file name


.. rubric:: Footnotes

.. [#maven] https://maven.apache.org/
