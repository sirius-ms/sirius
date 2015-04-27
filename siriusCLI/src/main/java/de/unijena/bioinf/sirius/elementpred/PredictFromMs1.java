package de.unijena.bioinf.sirius.elementpred;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.extraction.PatternExtractor;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.List;

/**
 * Vote for Br and Cl from the isotope pattern
 */
public class PredictFromMs1 implements Judge {

    private final PatternExtractor extractor;

    public PredictFromMs1(PatternExtractor extractor) {
        this.extractor = extractor;
    }

    @Override
    public void vote(TObjectIntHashMap<Element> votes, Ms2Experiment experiment, MeasurementProfile profile) {
        final Element Cl =PeriodicTable.getInstance().getByName("Cl");
        final Element Br = PeriodicTable.getInstance().getByName("Br");
        if (experiment.getMs1Spectra().size() > 0) {
            for (Spectrum<Peak> spec : experiment.getMs1Spectra()) {
                final List<IsotopePattern> extracted = extractor.extractPattern(profile, spec, experiment.getIonMass(), false);
                for (IsotopePattern pattern : extracted) {
                    final SimpleSpectrum ms1spec = pattern.getPattern();
                    final double mono = pattern.getMonoisotopicMass();
                    final int plus1 = (int)Math.round(mono+1);
                    final int plus2 = (int)Math.round(mono+2);
                    // find +1 peak
                    double int1 = 0, int2 = 0;
                    for (int k=0; k < ms1spec.size(); ++k) {
                        if (Math.round(ms1spec.getMzAt(k)) == plus1) {
                            int1 = Math.max(int1, ms1spec.getIntensityAt(k));
                        } else if (Math.round(ms1spec.getMzAt(k))==plus2) {
                            int2 = Math.max(int2, ms1spec.getIntensityAt(k));
                        }
                    }
                    if (int1==0 && int2 == 0) continue;
                    if (int1 > 0 && int2 == 0) {
                        votes.adjustValue(Cl, -10);
                        votes.adjustValue(Br, -10);
                    } else if (int2 > int1) {
                        if ((int2 / int1) <= 2 ) {
                            votes.adjustValue(Cl, 10);
                            votes.adjustValue(Br, 3);
                        } else if ((int2 / int1) > 2) {
                            votes.adjustValue(Cl, 5);
                            votes.adjustValue(Br, 6);
                        }
                    } else {
                        votes.adjustValue(Cl, -10);
                        votes.adjustValue(Br, -10);
                    }

                }

            }
        }
    }
}
