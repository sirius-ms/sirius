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
package de.unijena.bioinf.FragmentationTree;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeIterator;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.ms.JenaMsParser;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ExampleMain {

    public static void computeTrees(String fileName) throws IOException {

        // load profile
        final Profile profile = new Profile("default");
        // hint: do this only once for all files ;)

        // load ms file
        final Ms2Experiment experiment = new GenericParser<Ms2Experiment>(new JenaMsParser()).parseFile(new File(fileName));

        final MutableMs2Experiment exp = new MutableMs2Experiment(experiment);

        // analyze ms/ms with SIRIUS
        final ProcessedInput preprocessing = profile.fragmentationPatternAnalysis.preprocessing(experiment);
        // compute all trees
        final List<FTree> trees = profile.fragmentationPatternAnalysis.computeTrees(preprocessing).list();
        // compute trees one by one (e.g. in a loop)
        final TreeIterator iterator = profile.fragmentationPatternAnalysis.computeTrees(preprocessing).iterator();
        // iterator behaves like a normal iterator
        // compute only optimal tree
        final FTree optimal = profile.fragmentationPatternAnalysis.computeTrees(preprocessing).optimalTree();
        // compute tree for a certain molecular formula
        final FTree special = profile.fragmentationPatternAnalysis.computeTrees(preprocessing).onlyWith(Arrays.asList(MolecularFormula.parse("C6H12O6"))).optimalTree();

        // analyze ms1 with SIRIUS (IN DEVELOPMENT! API MAY CHANGE!)
        final List<IsotopePattern> patterns = profile.isotopePatternAnalysis.deisotope(experiment);

        // list contains a IsotopePattern for each compound in the MS1 spectrum (may not work correct currently)
        final IsotopePattern onePattern = patterns.get(0);
        // get candidate list
        final List<ScoredMolecularFormula> candidates = onePattern.getCandidates();
        // get best hit
        final ScoredMolecularFormula best = candidates.get(0);
        // compute tree for best hit
        final FTree bestHitTree = profile.fragmentationPatternAnalysis.computeTrees(preprocessing).onlyWith(Arrays.asList(best.getFormula())).optimalTree();

        // annotate tree
        final TreeAnnotation annotation = new TreeAnnotation(bestHitTree, profile.fragmentationPatternAnalysis, preprocessing);
        // add additional information to annotation
        annotation.getAdditionalProperties().put(bestHitTree.getRoot(), Arrays.asList("IsotopeScore: " + best.getScore()));

    }

}
