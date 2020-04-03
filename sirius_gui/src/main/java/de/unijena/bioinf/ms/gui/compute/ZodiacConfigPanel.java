package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;

public class ZodiacConfigPanel extends ConfigPanel {

    public ZodiacConfigPanel() {
        final TwoColumnPanel general = new TwoColumnPanel();
        add(new TextHeaderBoxPanel("General", general));
        general.addNamed("Considered candidates", makeIntParameterSpinner("ZodiacNumberOfConsideredCandidates", -1, 10000, 1));
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

        final TwoColumnPanel libraryHits = new TwoColumnPanel();
        add(new TextHeaderBoxPanel("Library Hits (Anchors)", libraryHits));
        //todo library file input
        libraryHits.addNamed("Minimal Cosine", makeDoubleParameterSpinner("ZodiacLibraryScoring.minCosine", 0, 1, .02));
        libraryHits.addNamed("Lamda", makeIntParameterSpinner("ZodiacLibraryScoring.lambda", 0, 99999, 1));
    }
}
