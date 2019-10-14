package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.BeautificationScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.MultimereLossScorer;

/**
 * Initializes SIRIUS.
 * We wanna go away from the JSON files.
 */
public class DefaultPlugin extends SiriusPlugin {

    @Override
    public void initializePlugin(PluginInitializer initializer) {
        initializer.addGeneralGraphScorer(new BeautificationScorer());
        initializer.addLossScorer(new MultimereLossScorer());
    }
}
