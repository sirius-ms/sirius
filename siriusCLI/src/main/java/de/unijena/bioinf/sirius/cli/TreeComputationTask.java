package de.unijena.bioinf.sirius.cli;

import com.sun.xml.xsom.impl.scd.Iterators;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.SpectralParser;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.TreeOptions;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class TreeComputationTask implements Task {

    protected Sirius sirius;
    protected final boolean shellMode;
    protected ShellProgress progress;

    public TreeComputationTask() {
        this.shellMode = System.console()!=null;
        this.progress = new ShellProgress(System.out, shellMode);
    }

    public void setup(TreeOptions opts) {
        try {
            this.sirius = new Sirius(opts.getProfile());
            final FragmentationPatternAnalysis ms2 = sirius.getMs2Analyzer();
            final IsotopePatternAnalysis ms1 = sirius.getMs1Analyzer();
            final MutableMeasurementProfile ms1Prof = new MutableMeasurementProfile(ms1.getDefaultProfile());
            final MutableMeasurementProfile ms2Prof = new MutableMeasurementProfile(ms2.getDefaultProfile());

            if (opts.getMedianNoise()!=null) {
                ms2Prof.setMedianNoiseIntensity(opts.getMedianNoise());
            }
            if (opts.getPPMMax() != null) {
                ms2Prof.setAllowedMassDeviation(new Deviation(opts.getPPMMax()));
                ms1Prof.setAllowedMassDeviation(new Deviation(opts.getPPMMax()));
            }
            if (opts.getPPMSd() != null) {
                ms2Prof.setStandardMs2MassDeviation(new Deviation(opts.getPPMSd()));
                ms1Prof.setStandardMs1MassDeviation(new Deviation(opts.getPPMSd()));
                ms1Prof.setStandardMassDifferenceDeviation(ms1Prof.getStandardMs1MassDeviation().multiply(0.66d));
            }
        } catch (IOException e) {
            System.err.println("Cannot load profile '" + opts.getProfile() + "':\n");
            e.printStackTrace();
            System.exit(1);
        }
    }


    protected void println(String msg) {
        System.out.println(msg);
    }
    protected void printf(String msg, Object... args) {
        System.out.printf(Locale.US, msg, args);
    }

    public Iterator<Instance> handleInput(TreeOptions options) throws IOException {
        final ArrayDeque<Instance> instances = new ArrayDeque<Instance>();
        final MsExperimentParser parser = new MsExperimentParser();
        // two different input modes:
        // general information that should be used if this fields are missing in the file
        final Double defaultParentMass = options.getParentMz();
        final Ionization ion = getIonFromOptions(options);
        final FormulaConstraints constraints = options.getElements() == null ? getDefaultElementSet(ion, options.isNoIon()) : options.getElements();
        // direct input: --ms1 and --ms2 command line options are given
        if (options.getMs2()!=null && !options.getMs2().isEmpty()) {
            final MutableMeasurementProfile profile = new MutableMeasurementProfile();
            profile.setFormulaConstraints(constraints);
            final MutableMs2Experiment exp = new MutableMs2Experiment();
            exp.setMeasurementProfile(profile);
            exp.setIonization(ion);
            exp.setMs2Spectra(new ArrayList<Ms2Spectrum<Peak>>());
            for (File f : options.getMs2()) {
                final Iterator<Ms2Spectrum<Peak>> spiter = SpectralParser.getParserFor(f).parseSpectra(f);
                while (spiter.hasNext()) {
                    final Ms2Spectrum<Peak> spec = spiter.next();
                    if (spec.getIonization()==null || spec.getPrecursorMz()==0 || spec.getMsLevel()==0) {
                        final MutableMs2Spectrum ms;
                        if (spec instanceof MutableMs2Spectrum) ms = (MutableMs2Spectrum)spec;
                        else ms = new MutableMs2Spectrum(spec);
                        if (ms.getIonization()==null) ms.setIonization(ion);
                        if (ms.getMsLevel()==0) ms.setMsLevel(2);
                        if (ms.getPrecursorMz()==0) {
                            if (defaultParentMass==null) {
                                if (exp.getMs2Spectra().size()>0) {
                                    ms.setPrecursorMz(exp.getMs2Spectra().get(0).getPrecursorMz());
                                } else {
                                    final MolecularFormula formula;
                                    if (exp.getMolecularFormula()!=null) formula = exp.getMolecularFormula();
                                    else if (options instanceof ComputeOptions && ((ComputeOptions) options).getMolecularFormula() != null) formula = MolecularFormula.parse(((ComputeOptions) options).getMolecularFormula()); else formula=null;
                                    if (formula != null) {
                                        ms.setPrecursorMz(ms.getIonization().addToMass(formula.getMass()));
                                    } else throw new IllegalArgumentException("Input MS/MS spectra do not contain the precursor mass of the measured ion. Please provide this information via --parentmass option");
                                }
                            } else {
                                ms.setPrecursorMz(defaultParentMass);
                            }
                        }
                    }
                    exp.getMs2Spectra().add(spec);
                }
            }
            if (exp.getMs2Spectra().size() <= 0) throw new IllegalArgumentException("SIRIUS expect at least one MS/MS spectrum. Please add a MS/MS spectrum via --ms2 option");
            for (int k=1; k < exp.getMs2Spectra().size(); ++k) {
                if (Math.abs(exp.getMs2Spectra().get(k).getPrecursorMz() - exp.getMs2Spectra().get(0).getPrecursorMz()) > 1e-3) {
                    throw new IllegalArgumentException("The given MS/MS spectra have different precursor mass and cannot belong to the same compound");
                }
            }
            if (options.getMs2()!=null &&  options.getMs1() != null && !options.getMs1().isEmpty()) {
                exp.setMs1Spectra(new ArrayList<Spectrum<Peak>>());
                for (File f : options.getMs1()) {
                    final Iterator<Ms2Spectrum<Peak>> spiter = SpectralParser.getParserFor(f).parseSpectra(f);
                    while (spiter.hasNext()) {
                        exp.getMs1Spectra().add(new SimpleSpectrum(spiter.next()));
                    }
                }
            }
            exp.setIonMass(exp.getMs2Spectra().get(0).getPrecursorMz());
            instances.add(new Instance(exp, options.getMs2().get(0)));
        } else if (options.getMs1()!=null && !options.getMs1().isEmpty()) {
            throw new IllegalArgumentException("SIRIUS expect at least one MS/MS spectrum. Please add a MS/MS spectrum via --ms2 option");
        }
        // batch input: files containing ms1 and ms2 data are given
        if (options.getInput()!=null && !options.getInput().isEmpty()) {
            final Iterator<File> fileIter;
            final ArrayList<File> infiles = new ArrayList<File>();
            for (String f : options.getInput()) {
                final File g = new File(f);
                if (g.isDirectory()) {infiles.addAll(Arrays.asList(g.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isFile();
                    }
                })));} else {
                    infiles.add(g);
                }
            }
            fileIter=infiles.iterator();
            return new Iterator<Instance>() {
                Iterator<Ms2Experiment> experimentIterator = fetchNext();
                File currentFile;
                @Override
                public boolean hasNext() {
                    return !instances.isEmpty();
                }

                @Override
                public Instance next() {
                    fetchNext();
                    return instances.poll();
                }

                private Iterator<Ms2Experiment> fetchNext() {
                    while (true) {
                        if (experimentIterator==null || !experimentIterator.hasNext()) {
                            if (fileIter.hasNext()) {
                                currentFile = fileIter.next();
                                try {
                                    GenericParser<Ms2Experiment> p = parser.getParser(currentFile);
                                    if (p==null) {
                                        System.err.println("Unknown file format: '" + currentFile + "'");
                                    } else experimentIterator = p.parseFromFileIterator(currentFile);
                                } catch (IOException e) {
                                    System.err.println("Cannot parse file '" + currentFile + "':\n");
                                    e.printStackTrace();
                                }
                            } else return null;
                        } else {
                            instances.push(new Instance(experimentIterator.next(), currentFile));
                            return experimentIterator;
                        }
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        } else {
            return instances.iterator();
        }
    }

    private static final Pattern CHARGE_PATTERN = Pattern.compile("(\\d+)([+-])?");

    protected static Ionization getIonFromOptions(InputOptions opt) {
        final String ionStr = opt.getIon();
        final Ionization ion = PeriodicTable.getInstance().ionByName(ionStr);
        if (ion != null) return ion;
        else {
            final Matcher m = CHARGE_PATTERN.matcher(ionStr);
            if (m.matches()) {
                if (m.group(2)!=null && m.group(2).equals("-")) {
                    return new Charge(-Integer.parseInt(m.group(1)));
                } else {
                    return new Charge(Integer.parseInt(m.group(1)));
                }
            } else {
                throw new IllegalArgumentException("Unknown ionization mode '" + ionStr + "'");
            }
        }
    }
    private final static FormulaConstraints DEFAULT_ELEMENTS = new FormulaConstraints("CHNOP[5]S");
    private final static FormulaConstraints DEFAULT_ELEMENTS_INCL_ADDP = new FormulaConstraints("CHNOP[5]SNa[1]K[1]");
    private final static FormulaConstraints DEFAULT_ELEMENTS_INCL_ADDN = new FormulaConstraints("CHNOP[5]SCl[1]");
    public FormulaConstraints getDefaultElementSet(Ionization ion, boolean nocharge) {
        if (ion instanceof Charge && nocharge) {
            return (ion.getCharge()>0) ? DEFAULT_ELEMENTS_INCL_ADDP : DEFAULT_ELEMENTS_INCL_ADDN;
        } else return DEFAULT_ELEMENTS;
    }
}
