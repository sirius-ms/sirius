
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.chem.utils.biotransformation.BioTransformation;
import de.unijena.bioinf.ChemistryBase.chem.utils.biotransformation.BioTransformer;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FasterTreeComputationInstance;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.ExtractedIsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.IsotopePatternGenerator;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.plugins.*;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.treemotifs.model.TreeMotifPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


//todo we should cleanup the api methods, proof which should be private and which are no longer needed, or at least change them, so that they use the identification job
public class Sirius {
    protected Profile profile;
    protected PeriodicTable table;

    public Sirius(@NotNull String profile) {
        this(Profile.fromString(profile));
    }

    public Sirius(@NotNull Profile profile) {
        this(profile, PeriodicTable.getInstance());
    }

    public Sirius(ParameterConfig config) {
        this(config.createInstanceWithDefaults(Profile.class));
    }
    public Sirius() {
        this(PropertyManager.DEFAULTS.createInstanceWithDefaults(Profile.class));

    }

    private void addDefaultPlugins() {
        this.getMs2Analyzer().registerPlugin(new DefaultPlugin());
        this.getMs2Analyzer().registerPlugin(new TreeStatisticPlugin());
        this.getMs2Analyzer().registerPlugin(new AdductSwitchPlugin());
        this.getMs2Analyzer().registerPlugin(new IsotopePatternInMs2Plugin());
        this.getMs2Analyzer().registerPlugin(new UseLossMassDeviationScoringPlugin());
        this.getMs2Analyzer().registerPlugin(new TreeMotifPlugin());

        this.getMs2Analyzer().registerPlugin(new PredefinedPeakPlugin());
        this.getMs2Analyzer().registerPlugin(new AminoAcidPlugin());
        this.getMs2Analyzer().registerPlugin(new AdductNeutralizationPlugin());

        this.getMs2Analyzer().registerPlugin(new BottomUpSearch());

        this.getMs2Analyzer().registerPlugin(new IsotopePatternInMs1Plugin()); //must be executed after BottomUpSearch, so it can also filter these formulas
        this.getMs2Analyzer().registerPlugin(new ElGordoPlugin()); //must be executed after IsotopePatternInMs1Plugin, so it is not filtered out
    }

    public Sirius(@NotNull Profile profile, @NotNull PeriodicTable table) {
        this.profile = profile;
        this.table = table;
        addDefaultPlugins();
    }

    public FragmentationPatternAnalysis getMs2Analyzer() {
        return profile.fragmentationPatternAnalysis;
    }

    public IsotopePatternAnalysis getMs1Analyzer() {
        return profile.isotopePatternAnalysis;
    }

    public void enableAutomaticElementDetection(@NotNull Ms2Experiment experiment, boolean enabled) {
        FormulaSettings current = experiment.getAnnotationOrDefault(FormulaSettings.class);
        if (enabled) {
            experiment.setAnnotation(FormulaSettings.class, current.autoDetect(profile.ms1Preprocessor.getSetOfPredictableElements().toArray(new Element[0])));
        } else {
            disableElementDetection(experiment, current);
        }
    }

    protected FasterTreeComputationInstance getTreeComputationImplementation(FragmentationPatternAnalysis analyzer, ProcessedInput input) {
        return new FasterTreeComputationInstance(analyzer, input);
    }


    public Ms1Preprocessor getMs1Preprocessor() {
        return profile.ms1Preprocessor;
    }

    public Ms2Preprocessor getMs2Preprocessor() {
        return profile.ms2Preprocessor;
    }

    /**
     * Perform all preprocessing steps for MS1 analysis
     */
    public ProcessedInput preprocessForMs1Analysis(Ms2Experiment experiment) {
        return getMs1Preprocessor().preprocess(experiment);
    }

    /**
     * Perform all preprocessing steps for MS/MS analysis
     */
    public ProcessedInput preprocessForMs2Analysis(Ms2Experiment experiment) {
        return getMs2Preprocessor().preprocess(experiment);
    }

    protected ProcessedInput preprocess(Ms2Experiment experiment) {
        if (experiment.getMs2Spectra().size()>0 && experiment.getMs2Spectra().get(0).size()>1) {
            return preprocessForMs2Analysis(experiment);
        } else return preprocessForMs1Analysis(experiment);
    }

    /**
     * Identify the molecular formula of the measured compound using the provided MS and MSMS data
     *
     * TODO: find a better solution which does not block if Job queue is full
     *
     * @param experiment input data
     * @return the top tree
     */
    public List<IdentificationResult> identify(Ms2Experiment experiment) {
        return SiriusJobs.getGlobalJobManager().submitJob(makeIdentificationJob(experiment)).takeResult();
    }


    @Deprecated
    public FormulaConstraints predictElementsFromMs1(Ms2Experiment experiment) {
        return preprocessForMs1Analysis(experiment).getAnnotationOrNull(FormulaConstraints.class);
    }

    public IdentificationResult compute(@NotNull Ms2Experiment experiment, MolecularFormula formula) {
        final MutableMs2Experiment copy = new MutableMs2Experiment(experiment);
        copy.setMolecularFormula(formula);
        Set<MolecularFormula> wh = Collections.singleton(formula);
        copy.setAnnotation(Whiteset.class, Whiteset.ofNeutralizedFormulas(wh, Sirius.class));
        final List<IdentificationResult> irs = identify(copy);
        if (irs.isEmpty()) return null;
        else return irs.get(0);
    }

    public BasicJJob<IdentificationResult> makeComputeJob(@NotNull Ms2Experiment experiment, MolecularFormula
            formula) {
        final MutableMs2Experiment copy = new MutableMs2Experiment(experiment);
        copy.setMolecularFormula(formula);
        copy.setAnnotation(Whiteset.class, Whiteset.ofNeutralizedFormulas(Collections.singleton(formula), Sirius.class));
        return new SiriusIdentificationJob(copy).wrap(x->x.get(0));

    }


    // region STATIC API METHODS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void setAnnotations(@NotNull Ms2Experiment
                                              experiment, @NotNull Annotated<Ms2ExperimentAnnotation> annotations) {
        experiment.setAnnotationsFrom(annotations);
    }


    public static MutableMs2Experiment makeMutable(@NotNull Ms2Experiment experiment) {
        if (experiment instanceof MutableMs2Experiment) return (MutableMs2Experiment) experiment;
        else return new MutableMs2Experiment(experiment);
    }

    public static void setAllowedMassDeviationMs1(@NotNull MutableMs2Experiment experiment, Deviation fragmentMassDeviation) {
        setAllowedMassDeviation(experiment, fragmentMassDeviation, MS1MassDeviation.class);
    }

    public static void setAllowedMassDeviationMs2(@NotNull MutableMs2Experiment experiment, Deviation fragmentMassDeviation) {
        setAllowedMassDeviation(experiment, fragmentMassDeviation, MS2MassDeviation.class);
    }

    public static <T extends MassDeviation> void setAllowedMassDeviation(@NotNull MutableMs2Experiment experiment, Deviation fragmentMassDeviation, Class<T> deviationType) {
        experiment.setAnnotation(deviationType, experiment.getAnnotationOrDefault(deviationType).withAllowedMassDeviation(fragmentMassDeviation));
    }

    public static void setFormulaSearchList(@NotNull Ms2Experiment experiment, MolecularFormula... formulas) {
        setFormulaSearchList(experiment, Arrays.asList(formulas));
    }

    public static void setFormulaSearchList(@NotNull Ms2Experiment
                                                    experiment, Iterable<MolecularFormula> formulas) {
        final HashSet<MolecularFormula> fs = new HashSet<MolecularFormula>();
        for (MolecularFormula f : formulas) fs.add(f);
        final Whiteset whiteset = Whiteset.ofNeutralizedFormulas(fs, Sirius.class);
        experiment.setAnnotation(Whiteset.class, whiteset);
    }

    public static void enableRecalibration(@NotNull MutableMs2Experiment experiment, boolean enabled) {
        experiment.setAnnotation(ForbidRecalibration.class, enabled ? ForbidRecalibration.ALLOWED : ForbidRecalibration.FORBIDDEN);
    }

    public static void setIsotopeMode(@NotNull MutableMs2Experiment experiment, IsotopeSettings isotopeSettings) {
        experiment.setAnnotation(IsotopeSettings.class, isotopeSettings);
    }

    public static void setAutomaticElementDetectionFor(@NotNull Ms2Experiment experiment, Element elements) {
        FormulaSettings current = experiment.getAnnotationOrDefault(FormulaSettings.class);
        experiment.setAnnotation(FormulaSettings.class, current.withoutAutoDetect().autoDetect(elements));
    }

    @Deprecated
    public static void setFormulaConstraints(@NotNull Ms2Experiment experiment, FormulaConstraints constraints) {
        //experiment.setAnnotation(FormulaConstraints.class, constraints);
        setFormulaSettings(experiment, experiment.getAnnotationOrDefault(FormulaSettings.class).enforce(constraints));
    }

    public static void setFormulaSettings(@NotNull Ms2Experiment experiment, FormulaSettings settings) {
        experiment.setAnnotation(FormulaSettings.class, settings);
    }

    public static void setTimeout(@NotNull MutableMs2Experiment experiment, int timeoutPerInstanceInSeconds,
                                  int timeoutPerDecompositionInSeconds) {
        experiment.setAnnotation(Timeout.class, Timeout.newTimeout(timeoutPerInstanceInSeconds, timeoutPerDecompositionInSeconds));
    }

    public static void disableTimeout(@NotNull MutableMs2Experiment experiment) {
        experiment.setAnnotation(Timeout.class, Timeout.NO_TIMEOUT);
    }

    public static void disableElementDetection(@NotNull Ms2Experiment experiment) {
        disableElementDetection(experiment, experiment.getAnnotationOrDefault(FormulaSettings.class));
    }

    public static void disableElementDetection(@NotNull Ms2Experiment experiment, FormulaSettings current) {
        experiment.setAnnotation(FormulaSettings.class, current.withoutAutoDetect());
    }

    public static void setNumberOfCandidates(@NotNull Ms2Experiment experiment, NumberOfCandidates value) {
        experiment.setAnnotation(NumberOfCandidates.class, value);
    }

    public static void setNumberOfCandidatesPerIon(@NotNull Ms2Experiment experiment, NumberOfCandidatesPerIonization value) {
        experiment.setAnnotation(NumberOfCandidatesPerIonization.class, value);
    }

    /*
    remove all but the most intense ms2
    todo this is more a hack for bad data. maybe remove if data quality stuff is done
     */
    public static void onlyKeepMostIntenseMS2(MutableMs2Experiment experiment) {
        if (experiment == null || experiment.getMs2Spectra().size() == 0) return;
        double precursorMass = experiment.getIonMass();
        int mostIntensiveIdx = -1;
        double maxIntensity = -1d;
        int pos = -1;
        if (experiment.getMs1Spectra().size() == experiment.getMs2Spectra().size()) {
            //one ms1 corresponds to one ms2. we take ms2 with most intense ms1 precursor peak
            for (Spectrum<Peak> spectrum : experiment.getMs1Spectra()) {
                ++pos;
                Deviation dev = new Deviation(100);
                int idx = Spectrums.mostIntensivePeakWithin(spectrum, precursorMass, dev);
                if (idx < 0) continue;
                double intensity = spectrum.getIntensityAt(idx);
                if (intensity > maxIntensity) {
                    maxIntensity = intensity;
                    mostIntensiveIdx = pos;
                }
            }
        }
        if (mostIntensiveIdx < 0) {
            //take ms2 with highest summed intensity
            pos = -1;
            for (Spectrum<Peak> spectrum : experiment.getMs2Spectra()) {
                ++pos;
                final int n = spectrum.size();
                double sumIntensity = 0d;
                for (int i = 0; i < n; ++i) {
                    sumIntensity += spectrum.getIntensityAt(i);
                }
                if (sumIntensity > maxIntensity) {
                    maxIntensity = sumIntensity;
                    mostIntensiveIdx = pos;
                }
            }
        }

        List<SimpleSpectrum> ms1List = new ArrayList<>();
        List<MutableMs2Spectrum> ms2List = new ArrayList<>();
        if (experiment.getMs1Spectra().size() == experiment.getMs2Spectra().size()) {
            ms1List.add(experiment.getMs1Spectra().get(mostIntensiveIdx));
        } else {
            ms1List.addAll(experiment.getMs1Spectra());
        }
        ms2List.add(experiment.getMs2Spectra().get(mostIntensiveIdx));
        experiment.setMs1Spectra(ms1List);
        experiment.setMs2Spectra(ms2List);
    }
    //endregion
    ////////////////////////////////////////////////////////////////////////////////



    //region DATA STRUCTURES API CALLS

    /**
     * Wraps an array of m/z values and and array of intensity values into a spectrum object that can be used by the SIRIUS library. The resulting spectrum is a lightweight view on the array, so changes in the array are reflected in the spectrum. The spectrum object itself is immutable.
     *
     * @param mz          mass to charge ratios
     * @param intensities intensity values. Can be normalized or absolute - SIRIUS will performNormalization them itself at later point
     * @return view on the arrays implementing the Spectrum interface
     */
    public Spectrum<Peak> wrapSpectrum(double[] mz, double[] intensities) {
        return Spectrums.wrap(mz, intensities);
    }

    /**
     * Lookup the symbol in the periodic table and returns the corresponding Element object or null if no element with this symbol exists.
     *
     * @param symbol symbol of the element, e.g. H for hydrogen or Cl for chlorine
     * @return instance of Element class
     */
    public Element getElement(String symbol) {
        return table.getByName(symbol);
    }

    /**
     * Lookup the ionization name and returns the corresponding ionization object or null if no ionization with this name is registered. The name of an ionization has the syntax [M+ADDUCT]CHARGE, for example [M+H]+ or [M-H]-.
     * <p>
     * Deprecated: Ionization is now for the ion-mode (protonation or deprotonation, number of charges, ...). Use
     * getPrecursorIonType to get a PrecursorIonType object that contains adducts and in-source fragmentation as well as
     * the ion mode of the precursor ion
     *
     * @param name name of the ionization
     * @return adduct object
     *//*
    @Deprecated
    public Ionization getIonization(String name) {
        return getPrecursorIonType(name).getIonization();
    }*/

    /**
     * Lookup the ionization name and returns the corresponding ionization object or null if no ionization with this name is registered. The name of an ionization has the syntax [M+ADDUCT]CHARGE, for example [M+H]+ or [M-H]-.
     *
     * @param name name of the ionization
     * @return adduct object
     */
    public PrecursorIonType getPrecursorIonType(String name) throws UnknownElementException {
        return table.ionByName(name);
    }


    /**
     * Charges are subclasses of Ionization. So they can be used everywhere as replacement for ionizations. A charge is very similar to the [M]+ and [M]- ionizations. However, the difference is that [M]+ describes an intrinsically charged compound where the Charge +1 describes an compound with unknown adduct.
     *
     * @param charge either 1 for positive or -1 for negative charges.
     * @return
     */
    public Charge getCharge(int charge) {
        if (charge != -1 && charge != 1)
            throw new IllegalArgumentException("SIRIUS does not support multiple charged compounds");
        return new Charge(charge);
    }

    /**
     * Creates a Deviation object that describes a mass deviation as maximum of a relative term (in ppm) and an absolute term. Usually, mass accuracy is given as relative term in ppm, as measurement errors increase with higher masses. However, for very small compounds (and fragments!) these relative values might overestimate the mass accurary. Therefore, an absolute value have to be given.
     *
     * @param ppm mass deviation as relative value (in ppm)
     * @param abs mass deviation as absolute value (m/z)
     * @return Deviation object
     */
    public Deviation getMassDeviation(int ppm, double abs) {
        return new Deviation(ppm, abs);
    }

    /**
     * Creates a Deviation object with the given relative term. The absolute term is implicitly given by applying the relative term on m/z 100.
     *
     * @param ppm
     * @return
     */
    public Deviation getMassDeviation(int ppm) {
        return new Deviation(ppm);
    }

    /**
     * Parses a molecular formula from the given string
     *
     * @param f molecular formula (e.g. in Hill notation)
     * @return immutable molecular formula object
     */
    public MolecularFormula parseFormula(String f) throws UnknownElementException {
        return MolecularFormula.parse(f);
    }

    /**
     * Creates a Ms2Experiment object from the given MS and MS/MS spectra. A Ms2Experiment is NOT a single run or measurement, but a measurement of a concrete compound. So a MS spectrum might contain several Ms2Experiments. However, each MS/MS spectrum should have on precursor or parent mass. All MS/MS spectra with the same precursor together with the MS spectrum containing this precursor peak can be seen as one Ms2Experiment.
     *
     * @param formula neutral molecular formula of the compound
     * @param ion     ionization mode (can be an instance of Charge if the exact adduct is unknown)
     * @param ms1     the MS spectrum containing the isotope pattern of the measured compound. Might be null
     * @param ms2     a list of MS/MS spectra containing the fragmentation pattern of the measured compound
     * @return a MS2Experiment instance, ready to be analyzed by SIRIUS
     */
    public Ms2Experiment getMs2Experiment(MolecularFormula formula, Ionization ion, Spectrum<Peak> ms1, List<Spectrum<Peak>> ms2) {
        return getMs2Experiment(formula, PrecursorIonType.getPrecursorIonType(ion), ms1, ms2);
    }

    /**
     * Creates a Ms2Experiment object from the given MS and MS/MS spectra. A Ms2Experiment is NOT a single run or measurement, but a measurement of a concrete compound. So a MS spectrum might contain several Ms2Experiments. However, each MS/MS spectrum should have on precursor or parent mass. All MS/MS spectra with the same precursor together with the MS spectrum containing this precursor peak can be seen as one Ms2Experiment.
     *
     * @param formula neutral molecular formula of the compound
     * @param ion     PrecursorIonType (contains ion mode as well as adducts and in-source fragmentations of the precursor ion)
     * @param ms1     the MS spectrum containing the isotope pattern of the measured compound. Might be null
     * @param ms2     a list of MS/MS spectra containing the fragmentation pattern of the measured compound
     * @return a MS2Experiment instance, ready to be analyzed by SIRIUS
     */
    public Ms2Experiment getMs2Experiment(MolecularFormula formula, PrecursorIonType
            ion, Spectrum<Peak> ms1, List<Spectrum<Peak>>  ms2) {
        final MutableMs2Experiment exp = (MutableMs2Experiment) getMs2Experiment(ion.neutralMassToPrecursorMass(formula.getMass()), ion, ms1, ms2);
        exp.setMolecularFormula(formula);
        return exp;
    }

    /**
     * Creates a Ms2Experiment object from the given MS and MS/MS spectra. A Ms2Experiment is NOT a single run or measurement, but a measurement of a concrete compound. So a MS spectrum might contain several Ms2Experiments. However, each MS/MS spectrum should have on precursor or parent mass. All MS/MS spectra with the same precursor together with the MS spectrum containing this precursor peak can be seen as one Ms2Experiment.
     *
     * @param parentMass the measured mass of the precursor ion. Can be either the MS peak or (if present) a MS/MS peak
     * @param ion        PrecursorIonType (contains ion mode as well as adducts and in-source fragmentations of the precursor ion)
     * @param ms1        the MS spectrum containing the isotope pattern of the measured compound. Might be null
     * @param ms2        a list of MS/MS spectra containing the fragmentation pattern of the measured compound
     * @return a MS2Experiment instance, ready to be analyzed by SIRIUS
     */
    public Ms2Experiment getMs2Experiment(double parentMass, PrecursorIonType ion, Spectrum<Peak> ms1, List<Spectrum<Peak>> ms2) {
        final MutableMs2Experiment mexp = new MutableMs2Experiment();
        mexp.setPrecursorIonType(ion);
        mexp.setIonMass(parentMass);
        mexp.setMergedMs1Spectrum(new SimpleSpectrum(ms1));
        for (Spectrum<Peak> spec : ms2) {
            mexp.getMs2Spectra().add(new MutableMs2Spectrum(spec, mexp.getIonMass(), CollisionEnergy.none(), 2));
        }
        return mexp;
    }

    /**
     * Creates a Ms2Experiment object from the given MS and MS/MS spectra. A Ms2Experiment is NOT a single run or measurement, but a measurement of a concrete compound. So a MS spectrum might contain several Ms2Experiments. However, each MS/MS spectrum should have on precursor or parent mass. All MS/MS spectra with the same precursor together with the MS spectrum containing this precursor peak can be seen as one Ms2Experiment.
     *
     * @param parentMass the measured mass of the precursor ion. Can be either the MS peak or (if present) a MS/MS peak
     * @param ion        ionization mode (can be an instance of Charge if the exact adduct is unknown)
     * @param ms1        the MS spectrum containing the isotope pattern of the measured compound. Might be null
     * @param ms2        a list of MS/MS spectra containing the fragmentation pattern of the measured compound
     * @return a MS2Experiment instance, ready to be analyzed by SIRIUS
     */
    public Ms2Experiment getMs2Experiment(double parentMass, Ionization ion, Spectrum<Peak> ms1, List<Spectrum<Peak>> ms2) {
        return getMs2Experiment(parentMass, PrecursorIonType.getPrecursorIonType(ion), ms1, ms2);
    }

    /**
     * Formula Constraints consist of a chemical alphabet (a subset of the periodic table, determining which elements might occur in the measured compounds) and upperbounds for each of this elements. A formula constraint can be given like a molecular formula. Upperbounds are written in square brackets or omitted, if any number of this element should be allowed.
     *
     * @param constraints string representation of the constraint, e.g. "CHNOP[5]S[20]"
     * @return formula constraint object
     */
    public FormulaConstraints getFormulaConstraints(String constraints) {
        return new FormulaConstraints(constraints);
    }

    /**
     * Decomposes a mass and return a list of all molecular formulas which ionized mass is near the measured mass.
     * The maximal distance between the neutral mass of the measured ion and the theoretical mass of the decomposed formula depends on the chosen profile. For qtof it is 10 ppm, for Orbitrap and FTICR it is 5 ppm.
     *
     * @param mass   mass of the measured ion
     * @param ion    ionization mode (might be a Charge, in which case the decomposer will enumerate the ion formulas instead of the neutral formulas)
     * @param constr the formula constraints, defining the allowed elements and their upperbounds
     * @return list of molecular formulas which theoretical ion mass is near the given mass
     */
    public List<MolecularFormula> decompose(double mass, Ionization ion, FormulaConstraints constr) {
        return decompose(mass, ion, constr, PropertyManager.DEFAULTS.createInstanceWithDefaults(MS2MassDeviation.class).allowedMassDeviation);
    }

    /**
     * Decomposes a mass and return a list of all molecular formulas which ionized mass is near the measured mass
     *
     * @param mass   mass of the measured ion
     * @param ion    ionization mode (might be a Charge, in which case the decomposer will enumerate the ion formulas instead of the neutral formulas)
     * @param constr the formula constraints, defining the allowed elements and their upperbounds
     * @param dev    the allowed mass deviation of the measured ion from the theoretical ion masses
     * @return
     */
    public List<MolecularFormula> decompose(double mass, Ionization ion, FormulaConstraints constr, Deviation dev) {
        return getMs2Analyzer().getDecomposerFor(constr.getChemicalAlphabet()).decomposeToFormulas(mass, ion, dev, constr);
    }

    /**
     * Applies a given biotransformation on a given Molecular formular and return the transformed formula(s)
     *
     * @param source         source formula for transformation
     * @param transformation to that will be applied to given Formula    ionization mode (might be a Charge, in which case the decomposer will enumerate the ion formulas instead of the neutral formulas)
     * @return transformed MolecularFormulas
     */
    public List<MolecularFormula> bioTransform(MolecularFormula source, BioTransformation transformation) {
        return BioTransformer.transform(source, transformation);
    }


    /**
     * Applies all known biotransformation on a given Molecular formular and returns the transformed formula(s)
     *
     * @param source source formula for transformation
     * @return transformed MolecularFormulas
     */
    public List<MolecularFormula> bioTransform(MolecularFormula source) {
        return BioTransformer.getAllTransformations(source);
    }

    /**
     * Simulates an isotope pattern for the given molecular formula and the chosen ionization
     *
     * @param compound neutral molecular formula
     * @param ion      ionization mode (might be a Charge)
     * @return spectrum containing the theoretical isotope pattern of this compound
     */
    public SimpleSpectrum simulateIsotopePattern(MolecularFormula compound, Ionization ion) {
        return getMs1Analyzer().getPatternGenerator().simulatePattern(compound, ion);
    }

    /**
     * Simulates an isotope pattern for the given molecular formula and the chosen ionization
     *
     * @param compound      neutral molecular formula
     * @param ion           ionization mode (might be a Charge)
     * @param numberOfPeaks number of peaks in simulated pattern
     * @return spectrum containing the theoretical isotope pattern of this compound
     */
    public SimpleSpectrum simulateIsotopePattern(MolecularFormula compound, Ionization ion, int numberOfPeaks) {
        IsotopePatternGenerator gen = getMs1Analyzer().getPatternGenerator();
        gen.setMaximalNumberOfPeaks(numberOfPeaks);
        return gen.simulatePattern(compound, ion);
    }

    public ExtractedIsotopePattern extractedIsotopePattern(@NotNull ProcessedInput pinput) {
        ExtractedIsotopePattern pat = pinput.getAnnotationOrNull(ExtractedIsotopePattern.class);
        if (pat == null) {
            final MutableMs2Experiment experiment = pinput.getExperimentInformation();
            pat = extractedIsotopePattern(experiment);
            pinput.setAnnotation(ExtractedIsotopePattern.class, pat);
        }
        return pat;
    }

    public ExtractedIsotopePattern extractedIsotopePattern(@NotNull Ms2Experiment experiment) {
        MS1MassDeviation ms1dev = experiment.getAnnotationOrDefault(MS1MassDeviation.class);

        SimpleSpectrum mergedMS1Pattern = null;
        if (experiment.getMergedMs1Spectrum() != null) {
            mergedMS1Pattern = Spectrums.extractIsotopePattern(experiment.getMergedMs1Spectrum(), ms1dev, experiment.getIonMass(), experiment.getPrecursorIonType().getCharge(), true);
        }

        SimpleSpectrum ms1SpectraPattern = null;
        if (experiment.getMs1Spectra().size() > 0) {
            ms1SpectraPattern = Spectrums.extractIsotopePatternFromMultipleSpectra(experiment.getMs1Spectra(), ms1dev, experiment.getIonMass(), experiment.getPrecursorIonType().getCharge(), true, 0.66);
        }


        if (mergedMS1Pattern != null) {
            if (ms1SpectraPattern != null) {
                final SimpleSpectrum extendedPattern = Spectrums.extendPattern(mergedMS1Pattern, ms1SpectraPattern, 0.02);
                return new ExtractedIsotopePattern(extendedPattern);
            } else {
                return new ExtractedIsotopePattern(mergedMS1Pattern);
            }
        } else if (ms1SpectraPattern != null) {
            return new ExtractedIsotopePattern(ms1SpectraPattern);
        }

        return null;
    }


    public Sirius.SiriusIdentificationJob makeIdentificationJob(final Ms2Experiment experiment) {
        return new SiriusIdentificationJob(experiment);
    }
    //endregion

    //region CLASSES
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////CLASSES/////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  /*  public class SiriusMS1IdentificationJob extends BasicMasterJJob<List<IdentificationResult>> {

        @Override
        protected List<IdentificationResult> compute() throws Exception {
            return null;
        }
    }*/

    public class SiriusIdentificationJob extends BasicMasterJJob<List<IdentificationResult>> {
        private final Ms2Experiment experiment;

        public SiriusIdentificationJob(Ms2Experiment experiment) {
            super(JobType.CPU);
            this.experiment = experiment;
        }

        @Override
        protected List<IdentificationResult> compute() throws Exception {
            try {
                final ProcessedInput input = preprocessForMs2Analysis(experiment);
                if (experiment.getAnnotationOrDefault(IsotopeSettings.class).isEnabled())
                    computeIsotopeScoresAndMs1Decompositions(input);
                final FasterTreeComputationInstance instance = getTreeComputationImplementation(getMs2Analyzer(), input);
                instance.addJobProgressListener(evt -> updateProgress(0, 105,  evt.getProgress()));
                submitSubJob(instance);
                FasterTreeComputationInstance.FinalResult fr = instance.awaitResult();

                List<IdentificationResult> r = createIdentificationResultsAndResolveAdducts(fr, instance);//postprocess results
                return r;
            } catch (RuntimeException e) {
                LoggerFactory.getLogger(Sirius.class).error("Error in instance " + experiment.getSourceString() + ": " + e.getMessage());
                throw e;
            }
        }

        private void computeIsotopeScoresAndMs1Decompositions(ProcessedInput input) {
            FormulaSearchSettings settings = input.getAnnotation(FormulaSearchSettings.class, FormulaSearchSettings::deNovoOnly);
            if (getMs2Analyzer().hasPlugin(BottomUpSearch.class)) {
                if (settings.useBottomUpFor(input.getExperimentInformation().getIonMass())) {
                    BottomUpSearch.generateDecompositionsAndSaveToWhiteset(input);
                }
            }
            getMs1Analyzer().computeAndScoreIsotopePattern(input);
        }

        /**
         * resolves adduct, current trees are still only based on ionizations without adducts
         */
        private List<IdentificationResult> createIdentificationResultsAndResolveAdducts(FasterTreeComputationInstance.FinalResult fr, FasterTreeComputationInstance computationInstance) {
            //clear scores for none MS/MS results
            if (experiment.getMs2Spectra().isEmpty()){
                logWarn("Instance has no MS/MS data. Tree score will be unreliable. Setting TreeScore to 0");
                for (FTree ftree : fr.getResults())
                    ftree.setTreeWeight(new FTreeMetricsHelper(ftree).getIsotopeMs1Score());
            }

            List<IdentificationResult> irs = fr.getResults().stream()
                    .map(tree -> new IdentificationResult(tree, new SiriusScore(FTreeMetricsHelper.getSiriusScore(tree))))
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());

            ProcessedInput pinput = computationInstance.getProcessedInput();
            irs = irs.stream()
                    .flatMap(idr->  resolveAdductIfPossible(idr, pinput).stream())
                    .collect(Collectors.toList());
            return irs;
        }

        /**
         *     This resolve all trees based on the {@link PossibleAdducts}. For some formulas, not every adduct will be possible based on formula filters such as  RDBE.
         *     //todo is this the correct position to do that? The same should hold for Isotope pattern Analysis. do we have to do at 2 positions? We also support MS1 only
         * @return
         */
        private List<IdentificationResult> resolveAdductIfPossible(IdentificationResult identificationResult, ProcessedInput pinput) {
            try {
                FTree tree = identificationResult.getTree();
                SiriusScore siriusScore = (SiriusScore) identificationResult.getScoreObject();
                PrecursorIonType ionType = tree.getAnnotation(PrecursorIonType.class).orElseThrow();
                final MolecularFormula measuredMF = tree.getRoot().getFormula();

                final FormulaConstraints constraints = pinput.getAnnotationOrThrow(FormulaConstraints.class);
                final PossibleAdducts possibleAdducts = pinput.getAnnotationOrThrow(PossibleAdducts.class);
                final Whiteset ws = pinput.getAnnotationOrThrow(Whiteset.class);

                List<IdentificationResult> resolvedTrees = new ArrayList<>();
                for (PrecursorIonType pa : possibleAdducts.getAdducts(ionType.getIonization())) {
                    boolean checkElementFilter = false; //should be sufficient to check when adding to whiteset. since filter is not applied to all formulas. E.g. bottom up may be excluded.
                    if(isValidNeutralFormula(measuredMF, pa, ws, constraints, checkElementFilter)) {
                        resolvedTrees.add(new IdentificationResult(new IonTreeUtils().treeToNeutralTree(tree, pa, true), siriusScore));
                    }
                }
                if (resolvedTrees.size()==0) {
                    //should not happen anymore
                    LoggerFactory.getLogger(getClass()).warn("No valid adducts found for ionization " + ionType.getIonization() + " for compound " + experiment.getName());
                    //return Collections.singletonList(identificationResult);
                }
                return resolvedTrees;


            } catch (Exception e) {
                //should not happen anymore
                LoggerFactory.getLogger(getClass()).error("Exception in 'resolveAdducts' code for compound " + experiment.getName() + ". Please report this problem. Using unmodified tree!", e);
                return Collections.singletonList(identificationResult);
            }
        }

        /*
         * assumes that whiteset contains EVERY allowed parent molecular formula - eiter in measured or neutral form. Even de novo and bottup-up formulas must be included!
         */
        private static boolean isValidNeutralFormula(MolecularFormula measuredMF, PrecursorIonType ionType, Whiteset whiteset, FormulaConstraints constraints,  boolean checkElementFilter) {
            if (!whiteset.containsMeasuredFormula(measuredMF, ionType)) return false;
            if (checkElementFilter) return constraints.isSatisfied(ionType.measuredNeutralMoleculeToNeutralMolecule(measuredMF), ionType.getIonization());
            for (FormulaFilter filter : constraints.getFilters()) {
                if (!filter.isValid(measuredMF, ionType)){
                    return false;
                }
            }
            return true;
        }

        private FTree treeWithInSourceIfNotEmpty(FTree tree, Ionization ionization, MolecularFormula inSource) {
            if (inSource.isEmpty()) return tree;
            else return new IonTreeUtils().treeToNeutralTree(tree, PrecursorIonType.getPrecursorIonType(ionization).substituteInsource(inSource));
        }

        public Ms2Experiment getExperiment() {
            return experiment;
        }
    }
    //endregion
}
