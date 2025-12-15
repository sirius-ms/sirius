/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;

/**
 * Initializes SIRIUS.
 * We wanna go away from the JSON files.
 */
public class DefaultPlugin extends SiriusPlugin {

    @Override
    public void initializePlugin(PluginInitializer initializer) {
        initializer.addGeneralGraphScorer(new BeautificationScorer());
        initializer.addLossScorer(new MultimereLossScorer());

        initializer.addRootScorer(new CarbohydrogenScorer.CarbohydrogenRootScorer());
        initializer.addFragmentScorer(new CarbohydrogenScorer.CarbohydrogenFragmentScorer());
        //initializer.addLossScorer(new CarbohydrogenScorer.CarbohydrogenLossScorer());

        /*
         * This fixes various problems with LossSizeScorer uses the masses instead of the formula's exact mass for scoring
         * formulas
         */
        LossSizeScorer scorer = null;
        for (PeakPairScorer s : initializer.getAnalysis().getPeakPairScorers()) {
            if (s instanceof LossSizeScorer) {
                scorer=(LossSizeScorer) s;
            }
        }
        CommonLossEdgeScorer commonLosses = null;
        for (LossScorer s : initializer.getAnalysis().getLossScorers()) {
            if (s instanceof CommonLossEdgeScorer) {
                commonLosses=(CommonLossEdgeScorer) s;
            }
        }
        if (scorer!=null && commonLosses!=null) {
            initializer.getAnalysis().getPeakPairScorers().remove(scorer);
            commonLosses.setLossSizeScorer(scorer);
        }

    }
}
