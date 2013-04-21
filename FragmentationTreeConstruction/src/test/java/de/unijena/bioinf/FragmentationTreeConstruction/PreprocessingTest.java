package de.unijena.bioinf.FragmentationTreeConstruction;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection.Detection;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2ExperimentImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2SpectrumImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProfileImpl;

/**
 * Created with IntelliJ IDEA.
 * User: kaidu
 * Date: 21.04.13
 * Time: 12:18
 * To change this template use File | Settings | File Templates.
 */
public class PreprocessingTest {

    public Ms2ExperimentImpl testData() {
        final Ms2ExperimentImpl experiment = new Ms2ExperimentImpl();
        experiment.setIonization(new Charge(1));
        experiment.setIonMass(180.0633881184 + new Charge(1).getMass());
        experiment.setMoleculeNeutralMass(180.0633881184);
        experiment.setMolecularFormula(MolecularFormula.parse("C6H12O6"));
        final ProfileImpl profile = new ProfileImpl();
        profile.setChemicalAlphabet(new ChemicalAlphabet());
        profile.setExpectedFragmentMassDeviation(new Deviation(10, 1e-3, 1e-4));
        profile.setExpectedIonMassDeviation(new Deviation(5, 1e-3, 1e-4));
        profile.setExpectedMassDifferenceDeviation(new Deviation(2, 5e-4, 1e-4));
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

    public testNormalization() {
        final Ms2Experiment exp = testData();
        final Detection correctParentPeak = new Detection(new Peak(exp.getIonMass(), 320), false);
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
        final ProcIanalysis.normalize(exp, correctParentPeak);

    }

}
