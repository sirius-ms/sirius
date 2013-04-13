package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.LogNormDistributedIntensityScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDeviationScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDifferenceDeviationScorer;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: Marcus
 * Date: 19.12.12
 * Time: 11:42
 * To change this template use File | Settings | File Templates.
 */
public class IsotopePatternScorerTest {

    @Test
    public void testScorer() {
        //todo theoretical intensity. in std.deviation, but paper describes measured?

        IsotopePatternScorer<Peak, Spectrum<Peak>> scorer;


        //Test MassDifferenceDeviationScorer
        final double[] theoMasses  = new double[]{100, 101, 102, 103, 104};
        final double[] theoIntensities = new double[]{0.4, 0.3, 0.15, 0.1, 0.05};
        Spectrum<Peak> theoreticalSpectrum = new SimpleSpectrum(theoMasses, theoIntensities);

        final double[] mMasses  = new double[]{100.0001, 101.0002, 102.0003, 103.003, 104.005};
        final double[] mIntensities = new double[]{0.39, 0.31, 0.15001, 0.10001, 0.04998};

        Spectrum<Peak> measuredSpectrum = new SimpleSpectrum(mMasses, mIntensities);

        scorer = new MassDeviationScorer<Peak,Spectrum<Peak>>(3, 10, 10); //alph0 = alpha1 don't cares whether Intensities are normalized
        double score = scorer.score(measuredSpectrum , theoreticalSpectrum, Normalization.Sum(1));

        assertTrue("MassDeviationScorer: \n expected: -141.9282 \n actual score: "+score, Math.round(score*10000)/10000d==-141.9282);

        scorer = new MassDeviationScorer<Peak,Spectrum<Peak>>(3, 5, 10);
        score = scorer.score(measuredSpectrum , theoreticalSpectrum, Normalization.Sum(1));

        assertTrue("MassDeviationScorer: \n expected: -151.2311 \n actual score: "+score, Math.round(score*10000)/10000d==-151.2311); //depending on whether it's right to use theoretical or measured intensity to calculate deviation, score is 151.2332 (theor) or 151.2311 (measured) (same story for following scores)

        scorer = new MassDifferenceDeviationScorer<Peak,Spectrum<Peak>>(3, 10, 10);
        score = scorer.score(measuredSpectrum , theoreticalSpectrum, Normalization.Sum(1));

        assertTrue("MassDeviationScorer: \n expected: -52.66849 \n actual score: "+score, Math.round(score*100000)/100000d==-52.66849);


        measuredSpectrum = new SimpleSpectrum(mMasses, mIntensities);
        scorer = new LogNormDistributedIntensityScorer(3, 0.1, 0.9);
        score = scorer.score(measuredSpectrum , theoreticalSpectrum, Normalization.Sum(1));
        assertTrue("LogNormDistributedIntensityScorer: \n expected: -0.3108457 \n actual score: "+score, Math.round(score*10000000)/10000000d==-0.3108457);


//        final ChemicalAlphabet alphabet = new ChemicalAlphabet(MolecularFormula.parse("CHNOPSFe").elementArray());
//        final MassDecomposer<Element> decomposer = new MassDecomposer<Element>(1e-5, 5, 0.001, alphabet);
//        decomposer.setValidator(new ValenceValidator<Element>());
//
//
//        MolecularFormula molecularFormula = MolecularFormula.parse("C7H10FeN2O4");
//        final PatternGenerator gen = new PatternGenerator(PeriodicTable.getInstance().ionByName("[M+H+]+"), Normalization.Max(1));
//        SimpleMutableSpectrum mutableSpectrum = new SimpleMutableSpectrum(gen.generatePattern(molecularFormula));
//        Spectrums.normalize(mutableSpectrum, Normalization.Sum(1)); //ist nicht schon normalisiert?
//
//        int q=mutableSpectrum.size()-1;
//        while (mutableSpectrum.getIntensityAt(q) < 0.001) {
//            mutableSpectrum.removePeakAt(q--);
//        }
//        ChargedSpectrum chargedSpectrum1 = new ChargedSpectrum(mutableSpectrum, PeriodicTable.getInstance().ionByName("[M+H+]+"));
//
//        molecularFormula = MolecularFormula.parse("C2N12OS");
//        mutableSpectrum = new SimpleMutableSpectrum(gen.generatePattern(molecularFormula));
//        Spectrums.normalize(mutableSpectrum, Normalization.Sum(1)); //ist nicht schon normalisiert?
//
//        q=mutableSpectrum.size()-1;
//        while (mutableSpectrum.getIntensityAt(q) < 0.001) {
//            mutableSpectrum.removePeakAt(q--);
//        }
//        ChargedSpectrum chargedSpectrum2 = new ChargedSpectrum(mutableSpectrum, PeriodicTable.getInstance().ionByName("[M+H+]+"));
//                                                                s
//        System.out.println("chargedSpec1: "+chargedSpectrum1);
//        System.out.println("chargedSpec2: "+chargedSpectrum2);
//
//        MassDeviationScorer<ChargedPeak, ChargedSpectrum> scorer2 = new MassDeviationScorer<ChargedPeak, ChargedSpectrum>(3, 10, 10);
//        score = scorer2.score(chargedSpectrum2 , chargedSpectrum1, Normalization.Sum(1));
//
//
//        System.out.println("x vs y: "+score);
//
//
//        molecularFormula = MolecularFormula.parse("C8H10FeN2O4");
//        mutableSpectrum = new SimpleMutableSpectrum(gen.generatePattern(molecularFormula));
//        Spectrums.normalize(mutableSpectrum, Normalization.Sum(1)); //ist nicht schon normalisiert?
//
//        q=mutableSpectrum.size()-1;
//        while (mutableSpectrum.getIntensityAt(q) < 0.001) {
//            mutableSpectrum.removePeakAt(q--);
//        }
//
//        chargedSpectrum2 = new ChargedSpectrum(mutableSpectrum, PeriodicTable.getInstance().ionByName("[M+H+]+"));
//
//        System.out.println("chargedSpec2: "+chargedSpectrum2);
//        scorer2 = new MassDeviationScorer<ChargedPeak, ChargedSpectrum>(3, 10, 10); //alph0 = alpha1 don't cares whether Intensities are normalized
//        score = scorer2.score(chargedSpectrum1 , chargedSpectrum2, Normalization.Sum(1));
//        System.out.println("z vs y: "+score);

    }

}
