/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.fingerprint;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainOptions;
import de.unijena.bioinf.ms.frontend.subtools.canopus.CanopusOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.fingerblast.FingerblastOptions;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.FormulaResult;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This is for CSI:FingerID (fingerprint prediction) specific parameters.
 * <p>
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "fingerprint", description = "<COMPOUND_TOOL> Predict molecular fingerprint from MS/MS and fragmentation trees for each compound Individually using CSI:FingerID fingerprint prediction.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class FingerprintOptions implements ToolChainOptions<FingerprintSubToolJob, InstanceJob.Factory<FingerprintSubToolJob>> {
    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public FingerprintOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }

    @Option(names = {"--no-threshold"},
            description = "Disable score threshold for formula candidates. CSI:FingerID fingerprints will be predicted for all formula candidates")
    public void setNoThreshold(boolean noThreshold) throws Exception {
        defaultConfigOptions.changeOption("FormulaResultThreshold", !noThreshold);
    }

    @Override
    public InstanceJob.Factory<FingerprintSubToolJob> call() throws Exception {
        return new InstanceJob.Factory<>(
                FingerprintSubToolJob::new,
                getInvalidator()
        );
    }

    @Override
    public Consumer<Instance> getInvalidator() {
        return inst -> {
            final Set<FormulaResultId> toRemove = inst.loadFormulaResults(FTree.class).stream().map(SScored::getCandidate)
                    .filter(res -> !res.hasAnnotation(FTree.class) ||
                            res.getAnnotationOrThrow(FTree.class).getAnnotation(IonTreeUtils.ExpandedAdduct.class)
                                    .orElse(IonTreeUtils.ExpandedAdduct.RAW) == IonTreeUtils.ExpandedAdduct.EXPANDED) //remove if tree is missing
                    .map(FormulaResult::getId).collect(Collectors.toSet());

            inst.deleteFormulaResults(toRemove); //delete expanded results
            inst.deleteFromFormulaResults(FingerprintResult.class); // remove Fingerprints from remaining
        };
    }

    @Override
    public List<Class<? extends ToolChainOptions<?, ?>>> getDependentSubCommands() {
        return List.of(FingerblastOptions.class, CanopusOptions.class);
    }
}
