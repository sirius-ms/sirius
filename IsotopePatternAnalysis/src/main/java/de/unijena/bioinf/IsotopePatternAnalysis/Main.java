package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.utils.ChargedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FastIsotopePatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FinestructurePatternGenerator;

public class Main {

    public static void main(String[] args) {
        /*
        final SimpleMutableSpectrum ms1 = new SimpleMutableSpectrum();
        ms1.addPeak(new Peak(174.0883, 16304.0));
        ms1.addPeak(new Peak(175.0901, 1192.0));
        ms1.addPeak(new Peak(176.0929, 170.0));
        final MsExperiment exp = new MutableMsExperiment(new MutableMeasurementProfile(), PeriodicTable.getInstance().ionByName("[M-H]-"), Arrays.asList(ms1), ms1);
        final IsotopePatternAnalysis iso = IsotopePatternAnalysis.defaultAnalyzer();
        iso.setCutoff(0.005);
        iso.setIntensityOffset(0d);
        List<IsotopePattern> deisotope = iso.deisotope(exp);
        System.out.println(deisotope.get(0).getCandidates().get(0).getScore());
        */
        Ionization hplus = PeriodicTable.getInstance().ionByName("[M]+");
        final MolecularFormula example = MolecularFormula.parse("C2H2O18S15");
        final ChargedSpectrum spec = new PatternGenerator(hplus).generatePattern(example, 8);
        final FastIsotopePatternGenerator gen = new FastIsotopePatternGenerator();
        gen.setMaximalNumberOfPeaks(8);
        final SimpleSpectrum spec2 = gen.simulatePattern(example, hplus);
        final FinestructurePatternGenerator gen2 = new FinestructurePatternGenerator();
        gen2.setMaximalNumberOfPeaks(8);
        gen2.setMinimalProbabilityThreshold(1e-8);
        final SimpleSpectrum spec3 = gen2.simulatePattern(example, hplus);
        System.out.println(spec);
        System.out.println(spec2);
        System.out.println(spec3);
        System.out.println("FOO!");

    }

}
