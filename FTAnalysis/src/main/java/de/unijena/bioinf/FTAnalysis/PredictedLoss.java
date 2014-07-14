package de.unijena.bioinf.FTAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

public class PredictedLoss {
    // formula of the loss
    final MolecularFormula lossFormula;
    // formula of the fragment at the tail of the arc
    final MolecularFormula fragmentFormula;
    // intensity of the fragment at the tail of the arc
    final double fragmentIntensity;
    // m/z of the fragment at the tail of the arc
    final double fragmentMz;
    // mass of the fragment at the tail of the arc
    final double fragmentNeutralMass;
    // maximum of the intensity of the incoming and outgoing fragment
    final double maxIntensity;

    PredictedLoss(FragmentAnnotation<ProcessedPeak> peak, Loss l, Ionization ion) {
        this.lossFormula = l.getFormula();
        this.fragmentFormula = l.getTarget().getFormula();
        this.fragmentIntensity = peak.get(l.getTarget()).getRelativeIntensity();
        this.fragmentMz = peak.get(l.getTarget()).getOriginalMz();
        this.maxIntensity = Math.max(peak.get(l.getTarget()).getRelativeIntensity(), peak.get(l.getSource()).getRelativeIntensity());
        this.fragmentNeutralMass = ion.subtractFromMass(peak.get(l.getTarget()).getOriginalMz());
    }

    public static String csvHeader() {
        return "fragment,loss,mz,neutralMass,massDeviation,intensity,lossIntensity";
    }

    public String toCSV() {
        return fragmentFormula.toString() + "," + lossFormula.toString() + "," + fragmentMz + "," + fragmentNeutralMass + "," + (fragmentNeutralMass - fragmentFormula.getMass()) + "," + fragmentIntensity + "," + maxIntensity;
    }
}