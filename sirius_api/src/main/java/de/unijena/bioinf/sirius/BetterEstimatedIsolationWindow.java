package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;

import java.util.List;

/**
 * Created by ge28quv on 13/07/17.
 * //TODO test!!
 * this {@link IsolationWindow} tests how reasonable the patterns are using IPA while extracting them for estimation
 */
public class BetterEstimatedIsolationWindow extends EstimatedIsolationWindow {

    private Sirius sirius;
    private PrecursorIonType[] precursorIonTypes;

    public BetterEstimatedIsolationWindow(double maxWindowSize, PrecursorIonType[] precursorIonTypes) {
        super(maxWindowSize);
        sirius = new Sirius();
        this.precursorIonTypes = precursorIonTypes;
    }

    public BetterEstimatedIsolationWindow(double maxWindowSize, double massShift, boolean estimateSize, PrecursorIonType[] precursorIonTypes, Deviation findMs1PeakDeviation) {
        super(maxWindowSize, massShift, estimateSize, findMs1PeakDeviation);
        sirius = new Sirius();
        this.precursorIonTypes = precursorIonTypes;
    }

    @Override
    public ChargedSpectrum extractPatternMs1(Spectrum<Peak> ms1Spec, MeasurementProfile profile, double targetMz) {
        //test charge
        ChargedSpectrum bestSpec = null;
        for (int charge : charges) {
            final ChargedSpectrum current = extractPattern(ms1Spec, profile, targetMz, charge);
            double longestLength = 0;
            for (final PrecursorIonType ionType : precursorIonTypes) {
                List<MolecularFormula> formulas = sirius.decompose(current.getMzAt(0), ionType.getIonization(), profile.getFormulaConstraints(), profile.getAllowedMassDeviation());
                SimpleSpectrum spectrum;
                if (current.getAbsCharge()==1){
                    spectrum = new SimpleSpectrum(current);
                } else {
                    spectrum = Spectrums.map(current, new Spectrums.Transformation<Peak, Peak>() {
                        @Override
                        public Peak transform(Peak input) {
                            double newMass = ionType.getIonization().addToMass(current.getAbsCharge()*ionType.getIonization().subtractFromMass(input.getMass()));
                            return new Peak(newMass, input.getIntensity());
                        }
                    });
                }
                List<IsotopePattern> patterns = sirius.getMs1Analyzer().scoreFormulas(spectrum, formulas, null, profile, ionType);
                for (IsotopePattern pattern : patterns) longestLength = Math.max(longestLength, pattern.getPattern().size());
            }
            if (current.size()>longestLength){
                for (int i = current.size() - 1; i >= longestLength; i--) {
                     current.removePeakAt(i);

                }
            }

            if (bestSpec==null) bestSpec = current;
            else if (current.size()>bestSpec.size()) bestSpec = current; //todo rather take best scoreing not longest?
        }
        return bestSpec;
    }
}
