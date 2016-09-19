/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.DPTreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.SpectralParser;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.SiriusResultWriter;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.elementpred.ElementPrediction;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CLI<Options extends SiriusOptions> {

    protected Sirius sirius;
    protected final boolean shellMode;
    protected ShellProgress progress;

    public static void main(String[] args) {
        final CLI cli = new CLI();
        cli.parseArgsAndInit(args);
        cli.compute();
    }

    public CLI() {
        this.shellMode = System.console()!=null;
        this.progress = new ShellProgress(System.out, shellMode);
    }

    Options options;

    public void compute() {
        try {
            final SiriusResultWriter siriusResultWriter;
            if (isUsingSiriusFormat()) {
                File output = options.getOutput();
                if (output == null) {
                    if (options.getInput().size()==1) {
                        output = new File(".",fileNameWithoutExtension(new File(options.getInput().get(0))) + ".sirius");
                    } else {
                        output = new File(".",new File(".").getAbsoluteFile().getName() + ".sirius");
                    }
                } else if (output.isDirectory()) {
                    output = new File(output, "results.sirius");
                }
                final FileOutputStream fout = new FileOutputStream(output);
                siriusResultWriter = new SiriusResultWriter(fout);
            } else {
                siriusResultWriter = null;
            }
            sirius.setProgress(progress);
            final Iterator<Instance> instances = handleInput(options);
            while (instances.hasNext()) {
                final Instance i = instances.next();
                progress.info("Compute '" + i.file.getName() + "'");
                final boolean doIdentify;
                final List<IdentificationResult> results;

                final List<String> whitelist = options.getFormula();
                final Set<MolecularFormula> whiteset = getFormulaWhiteset(i, whitelist);
                if ((whiteset==null) && options.isAutoCharge() && i.experiment.getPrecursorIonType().isIonizationUnknown()) {
                    results = sirius.identifyPrecursorAndIonization(i.experiment, getNumberOfCandidates(), !options.isNotRecalibrating(), options.getIsotopes());
                    doIdentify = true;
                } else if (whiteset!=null && whiteset.isEmpty()) {
                    results = new ArrayList<>();
                    doIdentify=true;
                } else if (whiteset==null || whiteset.size()!=1) {
                    results = sirius.identify(i.experiment, getNumberOfCandidates(), !options.isNotRecalibrating(), options.getIsotopes(), whiteset);
                    doIdentify=true;
                } else {
                    doIdentify=false;
                    results = Arrays.asList(sirius.compute(i.experiment, whiteset.iterator().next(), !options.isNotRecalibrating()));
                }

                if (options.isIonTree()) {
                    for (IdentificationResult result : results) {
                        result.transformToIonTree();
                    }
                } else {
                    for (IdentificationResult result : results) {
                        result.resolveIonizationInTree();
                    }
                }

                if (doIdentify) {
                    int rank=1;
                    int n = Math.max(1,(int)Math.ceil(Math.log10(results.size())));
                    for (IdentificationResult result : results) {
                        printf("%" + n + "d.) %s\tscore: %.2f\ttree: %+.2f\tiso: %.2f\tpeaks: %d\t%.2f %%\n", rank++, result.getMolecularFormula().toString(), result.getScore(), result.getTreeScore(), result.getIsotopeScore(), result.getTree().numberOfVertices(), sirius.getMs2Analyzer().getIntensityRatioOfExplainedPeaks(result.getTree()) * 100);
                    }
                    if (siriusResultWriter==null) output(i, results);
                } else {
                    if (siriusResultWriter==null) outputSingle(i, results.get(0), whiteset.iterator().next());
                }
                handleResults(i, results);
                if (siriusResultWriter!=null) {
                    siriusResultWriter.add(i.experiment, results);
                }
            }
            if (siriusResultWriter!=null) siriusResultWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void handleResults(Instance i, List<IdentificationResult> results) {

    }

    protected Set<MolecularFormula> getFormulaWhiteset(Instance i, List<String> whitelist) {
        final Set<MolecularFormula> whiteset = new HashSet<MolecularFormula>();
        if (whitelist==null && (options.getNumberOfCandidates()==null) && i.experiment.getMolecularFormula()!=null) {
            whiteset.add(i.experiment.getMolecularFormula());
        } else if (whitelist!=null) for (String s :whitelist) whiteset.add(MolecularFormula.parse(s));
        return whiteset.isEmpty() ? null : whiteset;
    }

    private boolean isUsingSiriusFormat() {
        return (options.getFormat()!=null && options.getFormat().toLowerCase().contains("sirius")) || (options.getOutput()!=null && options.getOutput().getName().toLowerCase().endsWith(".sirius"));
    }

    private Integer getNumberOfCandidates() {
        return options.getNumberOfCandidates()!=null ? options.getNumberOfCandidates() : 5;
    }

    private void output(Instance instance, List<IdentificationResult> results) throws IOException {
        final int c = getNumberOfCandidates();
        File target = options.getOutput();
        String format = options.getFormat();
        if (format==null) format = "dot";
        for (IdentificationResult result : results) {
            if (target!=null) {
                final File name = getTargetName(target, instance, result, format,c);
                if (format.equalsIgnoreCase("json")) {
                    new FTJsonWriter().writeTreeToFile(name, result.getTree());
                } else if (format.equalsIgnoreCase("dot")) {
                    new FTDotWriter(!options.isNoHTML(), !options.isIonTree()).writeTreeToFile(name, result.getTree());
                } else {
                    throw new RuntimeException("Unknown format '" + format + "'");
                }
            }
            if (options.isAnnotating()) {
                final File anoName = getTargetName(target!=null ? target : new File("."), instance, result, "csv",c);
                new AnnotatedSpectrumWriter().writeFile(anoName, result.getTree());
            }
        }

    }

    protected void cite() {
        System.out.println("Please cite the following paper when using our method:");
        System.out.println(ApplicationCore.CITATION);
    }

    protected void outputSingle(Instance instance, IdentificationResult result, MolecularFormula formula) throws IOException {
        if (result==null || result.getTree()==null) {
            System.out.println("Cannot find valid tree with molecular formula '" + formula + "' that supports the data. You might try to increase the allowed mass deviation with parameter --ppm-max");
            return;
        }
        result.getTree().normalizeStructure();
        File target = options.getOutput();
        String format = null;

        if (options.getFormat() != null) {
            format = options.getFormat();
        }

        if (target==null) {
            target = getTargetName(new File("."), instance, result, format==null ? "dot" : format, 1);
        } else if (format==null){
            final String n = target.getName();
            final int i = n.lastIndexOf('.');
            if (i >= 0) {
                final String ext = n.substring(i + 1).toLowerCase();
                if (ext.equals("json") || ext.equals("dot")) {
                    format = ext;
                } else format = "dot";
            } else format = "dot";
        }

        if (format==null) format = "dot";

        if (target.isDirectory()) {
            target = getTargetName(target, instance, result, format, 1);
        }

        if (format.equalsIgnoreCase("json")) {
            new FTJsonWriter().writeTreeToFile(target, result.getTree());
        } else if (format.equalsIgnoreCase("dot")) {
            new FTDotWriter(!options.isNoHTML(), !options.isIonTree()).writeTreeToFile(target, result.getTree());
        } else {
            throw new RuntimeException("Unknown format '" + format + "'");
        }
        if (options.isAnnotating()) {
            final File anoName = getTargetName(target, instance, result, "csv", 1);
            new AnnotatedSpectrumWriter().writeFile(anoName, result.getTree());
        }
    }

    protected File getTargetName(File target, Instance i, String format) {
        if (!target.isDirectory()) {
            final String name = target.getName();
            final int j = name.lastIndexOf('.');
            if (j>=0) return new File(target.getParentFile(), name.substring(0, j) + "." + format);
            else return new File(target.getParentFile(), name + "." + format);
        } else {
            final String inputName = i.fileNameWithoutExtension();
            return new File(target, inputName + "." + format);
        }
    }

    protected File getTargetName(File target, Instance i, IdentificationResult result, String format, int n) {
        if (!target.isDirectory()) {
            final String name = target.getName();
            final int j = name.lastIndexOf('.');
            if (j>=0) return new File(target.getParentFile(), name.substring(0, j) + "." + format);
            else return new File(target.getParentFile(), name + "." + format);
        } else {
            final String inputName = i.fileNameWithoutExtension();
            final File name;
            if (n<=1) {
                name = new File(target, inputName + "." + format);
            } else {
                name = new File(target, inputName + "_" + result.getRank() + "_" + result.getMolecularFormula().toString() + "." + format);
            }
            return name;
        }
    }

    protected void parseArgsAndInit(String[] args) {
        parseArgs(args);
        setup();
        validate();
    }

    public void parseArgs(String[] args) {
        parseArgs(args, (Class<Options>) SiriusOptions.class);
    }

    public void parseArgs(String[] args, Class<Options> optionsClass ) {
        if (args.length==0) {
            System.out.println(ApplicationCore.VERSION_STRING);
            System.out.println(CliFactory.createCli(optionsClass).getHelpMessage());
            System.exit(0);
        }
        try {
            this.options = CliFactory.createCli(optionsClass).parseArguments(args);
            if (options.isCite()) {
                cite();
                System.exit(0);
            }
        } catch (HelpRequestedException e) {
            System.out.println(e.getMessage());
            System.out.println("");
            cite();
            System.exit(0);
        }
        if (options.isVersion()) {
            System.out.println(ApplicationCore.VERSION_STRING);
            cite();
            System.exit(0);
        }

    }

    public void setup() {
        try {
            this.sirius = new Sirius(options.getProfile());
            final FragmentationPatternAnalysis ms2 = sirius.getMs2Analyzer();
            final IsotopePatternAnalysis ms1 = sirius.getMs1Analyzer();
            final MutableMeasurementProfile ms1Prof = new MutableMeasurementProfile(ms1.getDefaultProfile());
            final MutableMeasurementProfile ms2Prof = new MutableMeasurementProfile(ms2.getDefaultProfile());

            if (options.getElements()==null) {
                // autodetect and use default set
                ms1Prof.setFormulaConstraints(getDefaultElementSet(options));
                ms2Prof.setFormulaConstraints(getDefaultElementSet(options));
                sirius.setElementPrediction(new ElementPrediction(sirius.getMs1Analyzer()));
            } else {
                ms2Prof.setFormulaConstraints(options.getElements());
                ms1Prof.setFormulaConstraints(options.getElements());
            }

            if (options.isAutoCharge()) {
                sirius.setAutoIonMode(true);
            }

            if (options.getMedianNoise()!=null) {
                ms2Prof.setMedianNoiseIntensity(options.getMedianNoise());
            }
            if (options.getPPMMax() != null) {
                ms2Prof.setAllowedMassDeviation(new Deviation(options.getPPMMax()));
                ms1Prof.setAllowedMassDeviation(new Deviation(options.getPPMMax()));
            }
            final TreeBuilder builder = sirius.getMs2Analyzer().getTreeBuilder();
            if (builder instanceof DPTreeBuilder) {
                System.err.println("Cannot load ILP solver. Please read the installation instructions.");
                System.exit(1);
            }
            System.out.println("Compute trees using " + builder.getDescription());

            sirius.getMs2Analyzer().setDefaultProfile(ms2Prof);
            sirius.getMs1Analyzer().setDefaultProfile(ms1Prof);

            /*
            sirius.getMs2Analyzer().setValidatorWarning(new Warning() {
                @Override
                public void warn(String message) {
                    progress.info(message);
                }
            });
            */
        } catch (IOException e) {
            System.err.println("Cannot load profile '" + options.getProfile() + "':\n");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void validate(){
        // validate
        final File target = options.getOutput();
        if (target != null) {
            if (target.exists() && !target.isDirectory()) {
                System.err.println("Specify a directory name as output directory");
                System.exit(1);
            } else if (target.getName().indexOf('.') < 0){
                target.mkdirs();
            }
        }

        final String format = options.getFormat();
        if (format!=null && !format.equalsIgnoreCase("json") && !format.equalsIgnoreCase("dot") && !format.equalsIgnoreCase("sirius")) {
            System.err.println("Unknown file format '" + format + "'. Available are 'dot' and 'json'");
            System.exit(1);
        }
    }


    protected void println(String msg) {
        System.out.println(msg);
    }
    protected void printf(String msg, Object... args) {
        System.out.printf(Locale.US, msg, args);
    }

    protected Instance setupInstance(Instance inst) {
        final MutableMs2Experiment exp = inst.experiment instanceof MutableMs2Experiment ? (MutableMs2Experiment)inst.experiment : new MutableMs2Experiment(inst.experiment);
        if (exp.getPrecursorIonType()==null || exp.getPrecursorIonType().isIonizationUnknown()) exp.setPrecursorIonType(getIonFromOptions(options));
        if (options.getFormula()!=null && options.getFormula().size()==1) exp.setMolecularFormula(MolecularFormula.parse(options.getFormula().get(0)));
        if (options.getParentMz()!=null) exp.setIonMass(options.getParentMz());
        return new Instance(exp, inst.file);
    }

    public Iterator<Instance> handleInput(final SiriusOptions options) throws IOException {
        final ArrayDeque<Instance> instances = new ArrayDeque<Instance>();
        final MsExperimentParser parser = new MsExperimentParser();
        // two different input modes:
        // general information that should be used if this fields are missing in the file
        final Double defaultParentMass = options.getParentMz();
        PrecursorIonType ion = getIonFromOptions(options);
        if (ion.isIonizationUnknown()) {
            if (!options.isAutoCharge()) {
                ion = (ion.getCharge()>0) ? PeriodicTable.getInstance().ionByName("[M+H]+") : PeriodicTable.getInstance().ionByName("[M-H]-");
            }
        }
        final FormulaConstraints constraints = options.getElements() == null ? null/*getDefaultElementSet(options, ion)*/ : options.getElements();
        // direct input: --ms1 and --ms2 command line options are given
        if (options.getMs2()!=null && !options.getMs2().isEmpty()) {
            final MutableMeasurementProfile profile = new MutableMeasurementProfile();
            profile.setFormulaConstraints(constraints);
            final MutableMs2Experiment exp = new MutableMs2Experiment();
            exp.setPrecursorIonType(ion);
            exp.setMs2Spectra(new ArrayList<MutableMs2Spectrum>());
            for (File f : foreachIn(options.getMs2())) {
                final Iterator<Ms2Spectrum<Peak>> spiter = SpectralParser.getParserFor(f).parseSpectra(f);
                while (spiter.hasNext()) {
                    final Ms2Spectrum<Peak> spec = spiter.next();
                    if (spec.getIonization()==null || spec.getPrecursorMz()==0 || spec.getMsLevel()==0) {
                        final MutableMs2Spectrum ms;
                        if (spec instanceof MutableMs2Spectrum) ms = (MutableMs2Spectrum)spec;
                        else ms = new MutableMs2Spectrum(spec);
                        if (ms.getIonization()==null) ms.setIonization(ion.getIonization());
                        if (ms.getMsLevel()==0) ms.setMsLevel(2);
                        if (ms.getPrecursorMz()==0) {
                            if (defaultParentMass==null) {
                                if (exp.getMs2Spectra().size()>0) {
                                    ms.setPrecursorMz(exp.getMs2Spectra().get(0).getPrecursorMz());
                                } else {
                                    final MolecularFormula formula;
                                    if (exp.getMolecularFormula()!=null) formula = exp.getMolecularFormula();
                                    else if (options.getFormula()!=null && options.getFormula().size()==1) formula = MolecularFormula.parse(options.getFormula().get(0)); else formula=null;
                                    if (formula != null) {
                                        ms.setPrecursorMz(ms.getIonization().addToMass(formula.getMass()));
                                    } else ms.setPrecursorMz(0);
                                }
                            } else {
                                ms.setPrecursorMz(defaultParentMass);
                            }
                        }
                    }
                    exp.getMs2Spectra().add(new MutableMs2Spectrum(spec));
                }
            }
            if (exp.getMs2Spectra().size() <= 0) throw new IllegalArgumentException("SIRIUS expect at least one MS/MS spectrum. Please add a MS/MS spectrum via --ms2 option");

            if (options.getMs2()!=null &&  options.getMs1() != null && !options.getMs1().isEmpty()) {
                exp.setMs1Spectra(new ArrayList<SimpleSpectrum>());
                for (File f : options.getMs1()) {
                    final Iterator<Ms2Spectrum<Peak>> spiter = SpectralParser.getParserFor(f).parseSpectra(f);
                    while (spiter.hasNext()) {
                        exp.getMs1Spectra().add(new SimpleSpectrum(spiter.next()));
                    }
                }
            }

            final double expPrecursor;
            if (options.getParentMz()!=null) {
                expPrecursor = options.getParentMz();
            }else if (exp.getMolecularFormula()!=null) {
                expPrecursor = exp.getPrecursorIonType().neutralMassToPrecursorMass(exp.getMolecularFormula().getMass());
            } else {
                double prec=0d;
                for (int k=1; k < exp.getMs2Spectra().size(); ++k) {
                    final double pmz = exp.getMs2Spectra().get(k).getPrecursorMz();
                    if (pmz!=0 && Math.abs(pmz - exp.getMs2Spectra().get(0).getPrecursorMz()) > 1e-3) {
                        throw new IllegalArgumentException("The given MS/MS spectra have different precursor mass and cannot belong to the same compound");
                    } else if (pmz != 0) prec = pmz;
                }
                if (prec == 0) {
                    if (exp.getMs1Spectra().size()>0) {
                        final SimpleSpectrum patterns = sirius.getMs1Analyzer().extractPattern(exp, exp.getMergedMs1Spectrum().getMzAt(0));
                        if (patterns.size() < exp.getMergedMs1Spectrum().size()) {
                            throw new IllegalArgumentException("SIRIUS cannot infer the parentmass of the measured compound from MS1 spectrum. Please provide it via the -z option.");
                        }
                        expPrecursor = patterns.getMzAt(0);
                    } else throw new IllegalArgumentException("SIRIUS expects the parentmass of the measured compound as parameter. Please provide it via the -z option.");
                } else expPrecursor = prec;
            }


            exp.setIonMass(expPrecursor);
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
                    Instance c = instances.poll();
                    return setupInstance(new Instance(c.experiment, c.file));
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

    private List<File> foreachIn(List<File> ms2) {
        final List<File> queue = new ArrayList<File>();
        for (File f : ms2) {
            if (f.isDirectory()) {
                for (File g : f.listFiles())
                    if (!g.isDirectory())
                        queue.add(g);
            } else queue.add(f);
        }
        return queue;
    }


    private static final Pattern CHARGE_PATTERN = Pattern.compile("(\\d+)[+-]?");
    private static final Pattern CHARGE_PATTERN2 = Pattern.compile("[+-]?(\\d+)");

    protected static PrecursorIonType getIonFromOptions(SiriusOptions opt) {
        String ionStr = opt.getIon();
        if (ionStr==null) {
            if (opt.isAutoCharge()) return PeriodicTable.getInstance().getUnknownPrecursorIonType(1);
            else return PeriodicTable.getInstance().ionByName("[M+H]+");
        }
        final Matcher m1 = CHARGE_PATTERN.matcher(ionStr);
        final Matcher m2 = CHARGE_PATTERN2.matcher(ionStr);
        final Matcher m = m1.matches() ? m1 : (m2.matches() ? m2 : null);
        if (m != null) {
            if (m.group(1)!=null && ionStr.contains("-")) {
                return PeriodicTable.getInstance().getUnknownPrecursorIonType(-Integer.parseInt(m.group(1)));
            } else {
                return PeriodicTable.getInstance().getUnknownPrecursorIonType(Integer.parseInt(m.group(1)));
            }
        } else {
            final PrecursorIonType ion = PeriodicTable.getInstance().ionByName(ionStr);
            if (ion==null)
                throw new IllegalArgumentException("Unknown ionization mode '" + ionStr + "'");
            else return ion;
        }
    }
    private final static FormulaConstraints DEFAULT_ELEMENTS = new FormulaConstraints("CHNOP[5]S");
    public FormulaConstraints getDefaultElementSet(SiriusOptions opts) {
        final FormulaConstraints cf = (opts.getElements()!=null) ? opts.getElements() : DEFAULT_ELEMENTS;
        return cf;
    }

    protected static String fileNameWithoutExtension(File file) {
        final String name = file.getName();
        final int i = name.lastIndexOf('.');
        if (i>=0) return name.substring(0, i);
        else return name;
    }
}
