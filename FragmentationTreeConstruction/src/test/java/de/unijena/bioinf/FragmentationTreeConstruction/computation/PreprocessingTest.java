package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.PostProcessor;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.InputValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.MissingValueValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.HighIntensityMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PreprocessingTest {

    public Ms2Experiment getExperimentData() {
        final JenaMsParser parser = new JenaMsParser();
        final GenericParser<Ms2Experiment> genericParser = new GenericParser<Ms2Experiment>(parser);
        try {
            return genericParser.parse(PreprocessingTest.class.getResourceAsStream("/testfile.ms"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Ms2ExperimentImpl testData() {
        final Ms2ExperimentImpl experiment = new Ms2ExperimentImpl();
        experiment.setIonization(new Charge(1));
        experiment.setIonMass(180.0633881184 + new Charge(1).getMass());
        experiment.setMoleculeNeutralMass(180.0633881184);
        experiment.setMolecularFormula(MolecularFormula.parse("C6H12O6"));
        final ProfileImpl profile = new ProfileImpl();
        profile.setFormulaConstraints(new FormulaConstraints(new ChemicalAlphabet()));
        profile.setExpectedFragmentMassDeviation(new Deviation(10, 1e-3));
        profile.setExpectedIonMassDeviation(new Deviation(5, 1e-3));
        profile.setExpectedMassDifferenceDeviation(new Deviation(2, 5e-4));
        experiment.setMeasurementProfile(profile);
        final SimpleMutableSpectrum sp = new SimpleMutableSpectrum();
        final double c = new Charge(1).getMass();
        final double parent = 180.0633881184 + c;
        sp.addPeak(new Peak(parent, 1.0));
        experiment.getMs1Spectra().add(sp);
        experiment.setMergedMs1Spectrum(sp);
        {
            final SimpleMutableSpectrum sp2 = new SimpleMutableSpectrum();
            sp2.addPeak(new Peak(parent, 320));
            sp2.addPeak(new Peak(MolecularFormula.parse("C6H10O5").getMass() + c, 100));
            sp2.addPeak(new Peak(MolecularFormula.parse("C5H12O4").getMass() + c, 75));
            final Ms2SpectrumImpl ms2 = new Ms2SpectrumImpl(sp2, new CollisionEnergy(0, 10), 2, parent);
            experiment.getMs2Spectra().add(ms2);
        }
        {
            final SimpleMutableSpectrum sp2 = new SimpleMutableSpectrum();
            sp2.addPeak(new Peak(parent, 21));
            sp2.addPeak(new Peak(MolecularFormula.parse("C5H6O5").getMass() + c, 22));
            sp2.addPeak(new Peak(MolecularFormula.parse("C4H6O3").getMass() + c, 11));
            final Ms2SpectrumImpl ms2 = new Ms2SpectrumImpl(sp2, new CollisionEnergy(0, 10), 2, parent);
            experiment.getMs2Spectra().add(ms2);
        }
        return experiment;
    }

    /**
     * As the preprocessing is a pipeline consisting of many (more or less) complex steps,
     * it is hard to "UNIT" test it. An quick'n dirty approach is to test the whole preprocessing
     * pipeline in one method.
     */
    @Test
    public void testPreprocessing() {
        /*
        Ms2ExperimentImpl experiment = new Ms2ExperimentImpl(getExperimentData());
        experiment.setMeasurementProfile(new ProfileImpl(new Deviation(10), new Deviation(5), new Deviation(20),
                FormulaConstraints.create(new ValenceFilter(), "C", "H", "N", "O", "P", "S")));
        // configure analysis
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
        analysis.setInitial();
        analysis.setPostProcessors(Arrays.asList((PostProcessor)new NoiseThresholdFilter(0.015d)));
        analysis.setPeakMerger(new HighIntensityMerger());
        analysis.setInputValidators(Arrays.asList((InputValidator)new MissingValueValidator()));
        analysis.setNormalizationType(NormalizationType.GLOBAL);
        // input validation
        experiment = new Ms2ExperimentImpl(analysis.validate(experiment));

        // input normalization
        List<ProcessedPeak> normalizedPeaks = analysis.normalize(experiment);

        // input merging
        List<ProcessedPeak> mergedPeaks = analysis.mergePeaks(experiment, normalizedPeaks);

        // postprocessing
        mergedPeaks = new ArrayList<ProcessedPeak>(analysis.postProcess(PostProcessor.Stage.AFTER_MERGING, new ProcessedInput(experiment, mergedPeaks, null, null)).getMergedPeaks());

        Collections.sort(mergedPeaks, new ProcessedPeak.MassComparator());
        assertEquals(mergedPeaks.size(), 25);

        // first peak is parent peak. Take parent mass from ms1
        assertEquals(mergedPeaks.get(24).getMz(), 132.076815319352d, 1e-6);
        // second peak is at 114.066386424197 Da
        assertEquals(mergedPeaks.get(23).getMz(), 115.049976864731, 1e-6);
        assertEquals(mergedPeaks.get(23).getRelativeIntensity(), 0.0250236, 1e-3);
        /*
        // 90.0553049770099 // should be preferred, as it has the lowest collision energy
        assertEquals(mergedPeaks.get(11).getMz(), 90.0553049770099d, 1e-6);
        assertEquals(mergedPeaks.get(11).getRelativeIntensity(), 210.18379000000002, 1e-3);
        // 115.049976864731 (1.08147 + 1.42089 = 2.50236) // has highest intensity
        assertEquals(mergedPeaks.get(10).getMz(), 115.049976864731, 1e-6);
        assertEquals(mergedPeaks.get(10).getRelativeIntensity(), 2.50236, 1e-3);
        // 114.066386424197 (2.88662 + 3.2572 + 0.626885 = 6.7707049999999995)
        assertEquals(mergedPeaks.get(9).getMz(),114.066386424197, 1e-6);
        assertEquals(mergedPeaks.get(9).getRelativeIntensity(), 6.7707049999999995, 1e-3);


        // parent peak detection
        ProcessedPeak parentPeak = analysis.selectParentPeakAndCleanSpectrum(experiment, mergedPeaks);
        */

    }

}
