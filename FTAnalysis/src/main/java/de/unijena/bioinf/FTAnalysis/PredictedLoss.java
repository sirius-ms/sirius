package de.unijena.bioinf.FTAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;

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

    PredictedLoss(Loss l, Ionization ion) {
        this.lossFormula = l.getFormula();
        this.fragmentFormula = l.getTail().getFormula();
        this.fragmentIntensity = l.getTail().getRelativePeakIntensity();
        this.fragmentMz = l.getTail().getPeak().getOriginalMz();
        this.maxIntensity = Math.max(l.getTail().getRelativePeakIntensity(), l.getHead().getRelativePeakIntensity());
        this.fragmentNeutralMass = ion.subtractFromMass(l.getTail().getPeak().getOriginalMz());
    }

    public String toCSV() {
        return fragmentFormula.toString() + "," + lossFormula.toString() + "," + fragmentMz + "," + fragmentNeutralMass + "," + (fragmentNeutralMass - fragmentFormula.getMass()) + "," + fragmentIntensity + "," + maxIntensity;
    }

    public static String csvHeader() {
        return "fragment,loss,mz,neutralMass,massDeviation,intensity,lossIntensity";
    }
}