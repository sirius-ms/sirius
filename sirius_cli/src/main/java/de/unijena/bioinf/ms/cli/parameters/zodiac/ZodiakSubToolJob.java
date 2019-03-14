package de.unijena.bioinf.ms.cli.parameters.zodiac;

import de.unijena.bioinf.ms.cli.parameters.DataSetJob;
import de.unijena.bioinf.sirius.ExperimentResult;

import java.util.List;
import java.util.stream.Collectors;

public class ZodiakSubToolJob extends DataSetJob {

    @Override
    protected Iterable<ExperimentResult> compute() throws Exception {
        List<ExperimentResult> exps = awaitInputs();
        System.out.println("I am Zodiac and run on all instances: " + exps.stream().map(ExperimentResult::getSimplyfiedExperimentName).collect(Collectors.joining(",")));
        return exps;
    }
}
