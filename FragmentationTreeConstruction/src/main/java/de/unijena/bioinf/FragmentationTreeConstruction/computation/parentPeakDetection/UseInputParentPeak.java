package de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Spectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kai Dührkop
 */
public class UseInputParentPeak implements ParentPeakDetector {
    @Override
    public Detection detectParentPeak(ProcessedInput input, List<ProcessedPeak> peaks) {
        // multiplication is necessary, because overlapping of two mass windows is forbidden
        Deviation deviation = input.getExperimentInformation().getMassError().multiply(2);
        final List<MS2Spectrum> spectra = input.getOriginalInput().getMs2Spectra();
        double parentMass = 0d;
        for (MS2Spectrum s : spectra) {
            if (!Double.isNaN(s.getParentMass()) && s.getParentMass() > 0) {
                parentMass = s.getParentMass();
                break;
            }
        }
        if (parentMass <= 0d && input.getOriginalInput().getFormula() != null) {
            final MolecularFormula formula = input.getOriginalInput().getFormula();
            // is formula charged?
            //if (formula.maybeCharged()) {
            //    parentMass = formula.getMass(); // TODO: Macht das Sinn?
            //} else
            if (input.getOriginalInput().getStandardIon() != null){
                parentMass = input.getOriginalInput().getStandardIon().addToMass(formula.getMass());
            } else {
                return null;
            }
            if (input.getOriginalInput().getMs1Spectrum() != null) {
                double minDifference = Double.POSITIVE_INFINITY;
                double pm = parentMass;
                double intensityTreshold = Spectrums.getMaximalIntensity(input.getOriginalInput().getMs1Spectrum())*0.1;
                for (Peak p : input.getOriginalInput().getMs1Spectrum()) {
                    if (deviation.inErrorWindow(parentMass, p.getMass()) && p.getIntensity() >= intensityTreshold) {
                        final double diff = parentMass-p.getMass();
                        if (diff < minDifference) {
                            minDifference = diff;
                            pm = p.getMass();
                        }
                    }
                }
                parentMass=pm;
            }
        }
        if (parentMass <= 0d) {
            parentMass = input.getOriginalInput().getFormulaChargedMass();
        }
        if (parentMass <= 0d) return null;
        // search for peak with this mass
        ArrayList<ProcessedPeak> pps = new ArrayList<ProcessedPeak>();
        for (ProcessedPeak p : peaks) {
            if (deviation.inErrorWindow(parentMass, p.getMz())) {
                pps.add(p);
            }
        }
        // use peak with best "score" (-> trade-off between intensity and mass precision)
        // the concrete parameters of this distributions are not so important
        double opt = Double.NEGATIVE_INFINITY;
        ProcessedPeak pp = null;
        final ExponentialDistribution exp = new ExponentialDistribution(40);
        for (ProcessedPeak p : pps) {
            final double score =
                    new NormalDistribution(0, deviation.getAbsolute()/3).density(p.getMz()-parentMass)/exp.density(p.getRelativeIntensity());
            if (score > opt) {
                pp = p;
                opt = score;
            }
        }

        if (pp != null) {
            return new Detection(pp, false);
        } else {
            // create own one
            // TODO: Ion
            final ProcessedPeak ppp = new ProcessedPeak();
            ppp.setGlobalRelativeIntensity(1);
            ppp.setRelativeIntensity(1);
            ppp.setIon(input.getOriginalInput().getStandardIon());
            ppp.setMz(parentMass);
            ppp.setIndex(-1);
            return new Detection(ppp);
        }
    }
}
