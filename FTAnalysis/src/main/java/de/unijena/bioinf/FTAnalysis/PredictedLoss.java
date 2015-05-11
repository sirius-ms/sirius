/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
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
