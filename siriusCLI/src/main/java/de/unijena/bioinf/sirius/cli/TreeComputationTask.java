package de.unijena.bioinf.sirius.cli;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.TreeOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

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

    public abstract void compute(List<File> input);

    public Iterator<Instance> handleInput(List<File> files) throws IOException {
        final MsExperimentParser parser = new MsExperimentParser();
        final ArrayDeque<File> queue = new ArrayDeque<File>(files);
        return new Iterator<Instance>() {

            private Instance currentInstance = nextInstance();
            private Iterator<Ms2Experiment> innerIterator = null;
            private File currentFile;

            @Override
            public boolean hasNext() {
                return currentInstance != null;
            }

            @Override
            public Instance next() {
                final Instance i = currentInstance;
                nextInstance();
                return i;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private Instance nextInstance() {
                currentInstance=null;
                if (innerIterator!=null && innerIterator.hasNext()) {
                    currentInstance = new Instance(innerIterator.next(), currentFile);
                    return currentInstance;
                } else {
                    innerIterator=null;
                }
                while (!queue.isEmpty()) {
                    File f = queue.poll();
                    if (f.isDirectory()) {
                        for (File g : f.listFiles()) {
                            if (!g.isDirectory()) queue.push(g);
                        }
                    } else {
                        final GenericParser<Ms2Experiment> fileParser = parser.getParser(f);
                        currentFile=f;
                        if (fileParser!=null) {
                            try {
                                innerIterator = fileParser.parseFromFileIterator(currentFile);
                                if (innerIterator.hasNext()) {
                                    currentInstance = new Instance(innerIterator.next(), currentFile);
                                    break;
                                }
                            } catch (IOException e) {
                                System.err.println("Error while reading file '" + currentFile.getAbsolutePath() + "':");
                                e.printStackTrace();
                            }
                        }
                    }
                }
                return currentInstance;
            }
        };

    }

}
