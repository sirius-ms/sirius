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

public class MainIsoTest {
/*
    final static NormalDistribution rdbe = new NormalDistribution(6.151312, 4.541604);
    final static NormalDistribution het2carb = new NormalDistribution(0.5886335, 0.5550574);
    final static NormalDistribution hy2carb = new NormalDistribution(1.435877, 0.4960778);


    public static void main(String[] args) {
        final PeriodicTable P = PeriodicTable.getInstance();
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(P.getAllByName("C", "H", "N", "O", "P",
                "F", "I", "Na"));
        final HashMap<Element, Interval> boundaries = new HashMap<Element, Interval>();

        final MolecularFormula right = MolecularFormula.parse("C33H47NO13");
        boundaries.put(P.getByName("I"), new Interval(0, 5));
        boundaries.put(P.getByName("F"), new Interval(0, 10));
        //boundaries.put(P.getByName("S"), new Interval(0, 4));
        boundaries.put(P.getByName("P"), new Interval(0, 6));
        boundaries.put(P.getByName("Na"), new Interval(0, 1));
        final MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer(1e-5, 10, 0.001, alphabet);
        final MolecularFormula[] formulasX = decomposer.decomposeToFormulas(665.3047405966, boundaries).toArray(new MolecularFormula[0]);
        System.out.println(formulasX.length + " decompositions");
        final int N = 100;
        final int[] ranks = new int[N];
        for (int i=0; i < N; ++i) {
            final ScoredMolecularFormula[] scored = foo(alphabet, formulasX);
            // compute rank
            int rank = 0;
            boolean found = false;
            for (ScoredMolecularFormula s : scored) {
                ++rank;
                if (s.getName().equals(right)) {
                    found = true;
                    break;
                }
            }
            if (found) ranks[i] = rank;
        }
        Arrays.sort(ranks);
        System.out.println(Arrays.toString(ranks));


    }

    private static ScoredMolecularFormula[] foo(ChemicalAlphabet alphabet, MolecularFormula[] formulasX) {
        // isotopenanalyse
        final PatternGenerator generator = new PatternGenerator();
        ChargedSpectrum spec = generator.generatePatternWithTreshold(MolecularFormula.parse("C33H47NO13"), 4e-3);
        final MutableSpectrum<Peak> ms = new SimpleMutableSpectrum(spec);
        final FixedBagIntensityDependency depInt = new FixedBagIntensityDependency(
                new double[]{0.8, 0.5, 0.2, 0.1, 0.05, 0.01, 0.0001},
                new double[]{0.008, 0.06, 0.12, 0.2, 0.9, 1.5d, 6d}
        );
        final double isoPPm = 5d;
        final FixedBagIntensityDependency depMass = new FixedBagIntensityDependency(
                new double[]{0.8, 0.5, 0.2, 0.1, 0.05, 0.01, 0.0001},
                new double[]{isoPPm*1, isoPPm*1.1, isoPPm*1.2, isoPPm*2, isoPPm*2.5, isoPPm*10, isoPPm*30}
        );
        Spectrums.performNormalization(ms, Normalization.Sum(1));
        final Random r = new Random();
        for (int i=0; i < ms.size(); ++i) {
            // mass deviation
            final double in = ms.getIntensityAt(i);
            final NormalDistribution massDist = new NormalDistribution(ms.getMzAt(i), (1/3.0) * ms.getMzAt(i)*depMass.getValueAt(in)*1e-6);
            final NormalDistribution inDist = new NormalDistribution(0, (1/3.0) * (Math.log(1+depInt.getValueAt(in))));
            ms.setMzAt(i, massDist.sample());
            // intensity deviation:
            ms.setIntensityAt(i, in * Math.exp(inDist.sample()));
            Spectrums.performNormalization(ms, Normalization.Sum(1));
        }
        Spectrums.performNormalization(ms, Normalization.Sum(1));
        spec = new ChargedSpectrum(ms, spec.getIonization());
        final DeIsotope deiso = new DeIsotope(isoPPm, isoPPm*100d*1e-6, alphabet, PeriodicTable.getInstance().getDistribution(),
                3, 3, 1, 1, 1, 1, 50, 0d);
        deiso.setIntensityTreshold(4e-3);
        final IsotopePatternScorer scorer = new PatternScoreList(
                new AlternativeMassScorer(3, depMass)
                ,
                new LogNormDistributedIntensityScorer(3, depInt)
                //new MissingPeakScorer(50)
        );
        deiso.setIntensityOffset(0);
        deiso.setIsotopePatternScorer(scorer);
        final ScoredMolecularFormula[] scored = new ScoredMolecularFormula[formulasX.length];
        int k=0;
        for (MolecularFormula f : formulasX) {
            scored[k++] = new ScoredMolecularFormula(f, deiso.scoreFormula(spec, f) +
                    Math.log(rdbe.density(f.rdbe())) +  Math.log(het2carb.density(f.hetero2CarbonRatio())) +
                    Math.log(hy2carb.density(f.hydrogen2CarbonRatio())));
        }
        Arrays.sort(scored, Collections.reverseOrder());
        return scored;
    }
*/
}
