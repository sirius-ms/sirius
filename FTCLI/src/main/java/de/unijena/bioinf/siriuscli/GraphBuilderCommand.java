package de.unijena.bioinf.siriuscli;

import de.unijena.bioinf.FragmentationTree.GraphBuilder;
import de.unijena.bioinf.FragmentationTree.Main;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;

/**
 * TEMPORARY
 */
public class GraphBuilderCommand implements Command{
    @Override
    public String getDescription() {
        return "Build fragmentation graphs for the given instance. Is for debugging purpose only.";
    }

    @Override
    public String getName() {
        return "graph";
    }

    @Override
    public void run(String[] args) {
        GraphBuilder.main(args);
    }

    @Override
    public String getVersion() {
        return Main.VERSION_STRING;
    }
}
