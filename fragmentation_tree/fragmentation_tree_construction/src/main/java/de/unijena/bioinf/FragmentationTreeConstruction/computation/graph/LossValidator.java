package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.util.List;

public interface LossValidator {

    public static class Combination implements LossValidator {

        protected final LossValidator[] validators;

        public Combination(List<LossValidator> validators) {
            this.validators = validators.toArray(new LossValidator[validators.size()]);
        }

        @Override
        public boolean isForbidden(ProcessedInput input, FGraph graph, Fragment a, Fragment b) {
            for (LossValidator v : validators) {
                if (v.isForbidden(input,graph,a,b))
                    return true;
            }
            return false;
        }
    }

    public boolean isForbidden(ProcessedInput input, FGraph graph, Fragment a, Fragment b);

}
