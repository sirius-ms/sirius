/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.frontend.subtools.zodiac.ZodiacOptions;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;

public class ZodiacConfigPanel extends SubToolConfigPanel<ZodiacOptions> {

    public ZodiacConfigPanel() {
        super(ZodiacOptions.class);
        final TwoColumnPanel general = new TwoColumnPanel();
        add(new TextHeaderBoxPanel("General", general));
        general.addNamed("Considered candidates 300m/z", makeIntParameterSpinner("ZodiacNumberOfConsideredCandidatesAt300Mz", -1, 10000, 1));
        general.addNamed("Considered candidates 800m/z", makeIntParameterSpinner("ZodiacNumberOfConsideredCandidatesAt800Mz", -1, 10000, 1));
        general.addNamed("Use  2-step approach", makeParameterCheckBox("ZodiacRunInTwoSteps"));

        final TwoColumnPanel edgeFilter = new TwoColumnPanel();
        add(new TextHeaderBoxPanel("Edge Filters", edgeFilter));
        edgeFilter.addNamed("Edge Threshold", makeDoubleParameterSpinner("ZodiacEdgeFilterThresholds.thresholdFilter", .5, 1, .01));
        edgeFilter.addNamed("Min Local Connections", makeIntParameterSpinner("ZodiacEdgeFilterThresholds.minLocalConnections", 0, 10000, 1));

        final TwoColumnPanel gibbsSampling = new TwoColumnPanel();
        add(new TextHeaderBoxPanel("Gibbs Sampling", gibbsSampling));
        gibbsSampling.addNamed("Iterations", makeIntParameterSpinner("ZodiacEpochs.iterations", 100, 9999999, 1));
        gibbsSampling.addNamed("Burn-In", makeIntParameterSpinner("ZodiacEpochs.burnInPeriod", 0, 9999, 1));
        gibbsSampling.addNamed("Separate Runs", makeIntParameterSpinner("ZodiacEpochs.numberOfMarkovChains", 1, 1000, 1));

//        final TwoColumnPanel libraryHits = new TwoColumnPanel();
//        add(new TextHeaderBoxPanel("Library Hits (Anchors)", libraryHits));
//        //todo library file input
//        libraryHits.addNamed("Minimal Cosine", makeDoubleParameterSpinner("ZodiacLibraryScoring.minCosine", 0, 1, .02));
//        libraryHits.addNamed("Lamda", makeIntParameterSpinner("ZodiacLibraryScoring.lambda", 0, 99999, 1));
    }
}
