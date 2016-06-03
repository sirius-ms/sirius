package de.unijena.bioinf.sirius.cli;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms2IsotopePattern;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.DynamicBaselineFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.IsotopePeakWithKnownParentFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.MissingValueValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.Warning;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.PeakAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

public class Test {

    public static void main(String[] args) {
        for (File f : new File(args[0]).listFiles()) {
            if (!f.getName().endsWith(".ms")) continue;
            final Ms2Experiment exp;
            try {
                final Sirius s = new Sirius("qtof");

                s.getMs2Analyzer().enableIsotopesInMs2(true);

                final PeakScorer noIsotopePenalizer = new PeakScorer() {
                    @Override
                    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
                        for (int k=0; k < peaks.size(); ++k) {
                            scores[k] -= 2*peaks.get(k).getRelativeIntensity();
                        }
                    }

                    @Override
                    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

                    }

                    @Override
                    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

                    }
                };
                s.getMs2Analyzer().getFragmentPeakScorers().add(noIsotopePenalizer);

                s.getMs1Analyzer().getDefaultProfile().setAllowedMassDeviation(new Deviation(20, 0.002));
                exp = new MsExperimentParser().getParser(f).parseFromFile(f).get(0);
                MutableMs2Experiment mexp = new MissingValueValidator().validate(exp, new Warning.Noop(),true);
                System.out.println(f.getName());

                /*
                mexp = new IsotopePeakWithKnownParentFilter(true).process(mexp, s.getMs2Analyzer().getDefaultProfile());
                try (final BufferedWriter bw = Files.newBufferedWriter(new File("proc", f.getName()).toPath(), Charset.forName("UTF-8"))) {
                    new JenaMsWriter().write(bw, mexp);
                }
                */



                final IdentificationResult result = s.compute(mexp, mexp.getMolecularFormula());
                result.writeTreeToFile(new File("test", f.getName().substring(0,f.getName().lastIndexOf('.'))+".dot"));
                result.writeTreeToFile(new File("test", f.getName().substring(0,f.getName().lastIndexOf('.'))+".json"));

                int count=0, tc=0, ca=0,tca=0; double scount=0d,ts=0;
                final FragmentAnnotation<Ms2IsotopePattern> iso = result.getTree().getFragmentAnnotationOrNull(Ms2IsotopePattern.class);
                if (iso!=null) {
                    final FragmentAnnotation<AnnotatedPeak> peakAno = result.getTree().getFragmentAnnotationOrNull(AnnotatedPeak.class);
                    for (Fragment ff : result.getTree()) {
                        if (iso.get(ff)!=null) {
                            ++count;
                            scount += peakAno.get(ff).getRelativeIntensity();
                            if (peakAno.get(ff).getMaximalIntensity() >= 3000) ++ca;
                        }
                        if (peakAno.get(ff).getMaximalIntensity() >= 3000) ++tca;
                        tc+=1;
                        ts +=peakAno.get(ff).getRelativeIntensity();
                    }
                    System.out.printf(Locale.US, "%d of %d peaks (%d of %d intensive ones) have isotope patterns (%d %% of intensity)\n",
                            count, tc, ca, tca, (int)(100*scount/ts));
                }

                /*
                try (final BufferedWriter bw = Files.newBufferedWriter(new File("basel", f.getName()).toPath(), Charset.forName("UTF-8"))) {
                    new JenaMsWriter().write(bw, new NoiseThresholdFilter(500).process(mexp, s.getMs2Analyzer().getDefaultProfile()));
                }
                */
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

}
