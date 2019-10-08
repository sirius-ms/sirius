package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.projectspace.ProjectSpaceConfiguration;

public class SiriusProjectSpaceConfiguration {

    public static void configure(ProjectSpaceConfiguration configuration) {

        configuration.registerContainer(CompoundContainer.class, new CompoundContainerSerializer());
        configuration.registerContainer(FormulaResult.class, new FormulaResultSerializer());

        configuration.registerComponent(CompoundContainer.class, Ms2Experiment.class, new MsExperimentSerializer());

        configuration.registerComponent(FormulaResult.class, FTree.class, new TreeSerializer());



    }

}
