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

package de.unijena.bioinf.IsotopePatternAnalysis.isogencli;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FastIsotopePatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.*;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.json.JSONDocumentType;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class NewScorerTest {

    public static void main(String[] args) {
        try {
            new NewScorerTest().test2();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static IsotopePatternAnalysis readProfile() throws IOException {
        final JSONObject json = JSONDocumentType.getJSON("/profiles/orbitrap.json", "/home/kaidu/Documents/temp/test.json");
        final JSONDocumentType document = new JSONDocumentType();
        if (document.hasKeyInDictionary(json, "IsotopePatternAnalysis")) return IsotopePatternAnalysis.loadFromProfile(document, json);
        return null;
    }

    public static void test2() throws IOException {
        int top1 = 0,top5=0;
        final File casmi = new File("/home/kaidu/data/casmi2014/ms");
        final IsotopePatternAnalysis analysis = readProfile();
        final Iterator<IsotopePatternScorer> scorer = analysis.getIsotopePatternScorers().iterator();

        MassDeviationScorer massScorer = null;
        MassDifferenceDeviationScorer massScorer2 = null;
        NormalDistributedIntensityScorer normScorer = new NormalDistributedIntensityScorer();


        while (scorer.hasNext()) {
            final IsotopePatternScorer asc = scorer.next();
            if (asc instanceof LogNormDistributedIntensityScorer) scorer.remove();
            if (asc instanceof MassDeviationScorer) massScorer = (MassDeviationScorer) asc;
            if (asc instanceof MassDifferenceDeviationScorer) massScorer2 = (MassDifferenceDeviationScorer) asc;
        }


        //analysis.getIsotopePatternScorers().add(new LogNormDistributedIntensityScorer());
        analysis.getIsotopePatternScorers().add(normScorer);
        //analysis.getIsotopePatternScorers().add(new MassDifferenceDeviationScorer());
        /*
        analysis.setDefaultProfile(new MutableMeasurementProfile());
        analysis.getDefaultProfile().setAllowedMassDeviation(new Deviation(10, 0.002));
        analysis.getDefaultProfile().setStandardMassDifferenceDeviation(new Deviation(2, 0.0005));
        analysis.getDefaultProfile().setStandardMs1MassDeviation(new Deviation(5, 0.001));
        analysis.getDefaultProfile().setStandardMs2MassDeviation(new Deviation(10, 0.002));
        */

        final FastIsotopePatternGenerator gen = new FastIsotopePatternGenerator();
        final MsExperimentParser parser = new MsExperimentParser();
        int counter=0;
        eachChallenge:
        for (File ms : casmi.listFiles()) {
            if (parser.getParser(ms)==null) continue;
            ++counter;
            final Ms2Experiment experiment = parser.getParser(ms).parseFromFile(ms).get(0);
            analysis.getDefaultProfile().setFormulaConstraints(new FormulaConstraints("CHNOPS").getExtendedConstraints(experiment.getMolecularFormula().elementArray()));
            final IsotopePattern patterns = analysis.deisotope(experiment).get(0);

            final Normalization norm = Normalization.Sum(1d);

            final SimpleSpectrum spectrum = Spectrums.getNormalizedSpectrum(patterns.getPattern(), norm);
            int k=1;
            for (Scored<de.unijena.bioinf.ChemistryBase.chem.MolecularFormula> formula : patterns.getCandidates()) {
                if (formula.getCandidate().equals(experiment.getMolecularFormula())) {
                    gen.setMaximalNumberOfPeaks(patterns.getPattern().size());
                    gen.setMinimalProbabilityThreshold(0d);
                    final SimpleSpectrum theoreticalPattern = Spectrums.getNormalizedSpectrum(gen.simulatePattern(experiment.getMolecularFormula(), experiment.getPrecursorIonType().getIonization()), norm);

                    System.out.print(ms.getName() + "\t" + k);
                    System.out.print("\t" +  massScorer.score(spectrum, theoreticalPattern, Normalization.Sum(1d), experiment, analysis.getDefaultProfile()));
                    if (massScorer2!=null) System.out.print("\t" + massScorer2.score(spectrum, theoreticalPattern, Normalization.Sum(1d), experiment, analysis.getDefaultProfile()));
                    System.out.print("\t" + normScorer.score(spectrum, theoreticalPattern, Normalization.Sum(1d), experiment, analysis.getDefaultProfile()));
                    System.out.print("\n");
                    if (k==1) ++top1;
                    if (k<=5) ++top5;
                    continue eachChallenge;
                }
                ++k;
            }
            System.out.println(ms.getName() +"\t" + "[MISSING]");
        }
        System.out.println("TOP1: " + top1 + " / " + counter);
        System.out.println("TOP1: " + top5 + " / " + counter);
    }

}
