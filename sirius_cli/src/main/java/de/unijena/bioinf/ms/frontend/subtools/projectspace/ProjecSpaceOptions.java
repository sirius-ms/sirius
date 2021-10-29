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

package de.unijena.bioinf.ms.frontend.subtools.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.io.lcms.CVUtils;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandLine.Command(name = "project-space", aliases = {"PS"}, description = "<STANDALONE> Modify a given project Space: Read project(s) with --input, apply modification and write the result via --output. If either --input or --output is given, the modifications will be made in-place.",  versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class ProjecSpaceOptions implements StandaloneTool<ProjectSpaceWorkflow> {

    @CommandLine.Option(names = {"--repair-scores"},
            description = "Remove CSI scores that have no candidates anymore", hidden = true)
    boolean repairScores;

    @CommandLine.Option(names = {"--merge-compounds-top"}, description = "Merge compounds which have the same spectra and m/z into one. For each such cluster of compounds, keep only the top k most abundant ones. The criteria for merging can be specified with --merge-compounds-cosine and --merge-compounds-rtdiff.")
    Integer mergeCompoundsTopK;

    @CommandLine.Option(names = {"--merge-compounds-cosine"}, description = "Merge compounds which have the same spectra and m/z into one. The criteria for merging can be specified with --merge-compounds-cosine and --merge-compounds-rtdiff.")
    Double mergeCompoundsCosine;

    @CommandLine.Option(names = {"--merge-compounds-rtdiff"}, description = "Merge compounds which have the same spectra and m/z into one. The criteria for merging can be specified with --merge-compounds-cosine and --merge-compounds-rtdiff. Retention time difference for merging is either specified in seconds, or by adding a unit after the number (e.g., '5 min'")
    private void setMergeCompoundsRtDiff(String txt) {
        Matcher m = TMUNIT.matcher(txt);
        if (m.find()) {
            final double rtval = Double.parseDouble(m.group(1));
            double multiplier = 1;
            if (m.group(2)!= null && !m.group(2).isEmpty()) {
                for (CVUtils.TimeUnit t : CVUtils.TimeUnit.values()) {
                    if (m.group(2).equalsIgnoreCase(t.unitName)) {
                        multiplier = t.inSeconds;
                        break;
                    }
                }
            }
            this.mergeCompoundsRtDiff = Math.round(rtval*multiplier);
        } else{
            throw new CommandLine.TypeConversionException("'" + txt + "' is not a valid value for the retention time difference. Specify the time either in seconds or by adding one of the following units: ms, s, min, h.");
        }
    }
    Long mergeCompoundsRtDiff;
    private static Pattern TMUNIT = Pattern.compile("([0-9]+(?:\\.[0-9]+))\\s*(ms|s|min|h)?");

    @CommandLine.Option(names = {"--delete-by-idx", "--di", "-d"}, split = ",",
            description = {"Delete all compounds that match the given indices from the given project-space."})
    private void makeDeleteIdxFilter(Set<Integer> idxs) {
        deleteIdxFilter = (c) -> !idxs.contains(c.getCompoundIndex());
    }

    Predicate<CompoundContainerId> deleteIdxFilter = (c) -> true;


    @CommandLine.Option(names = {"--delete-by-mass", "--dm"},
            description = {"Delete all compounds that are within the given mass window."
                    ,"Example: Use 'mass1:mass2' to match compounds with mass1 <= mass <= mass2. Leave a value empty to set no bound."})
    private void makeDeleteMassFilter(String mass) {
        String[] masses = mass.split(":");
        final double gt = masses[0].strip().isBlank() ? 0d : Double.parseDouble(masses[0]);
        final double lt = masses.length < 2 || masses[1].strip().isBlank() ? Double.POSITIVE_INFINITY : Double.parseDouble(masses[1]);
        deleteMassFilter = (c) -> !(c.getIonMass().map(m -> gt <= m && m <= lt).orElse(false));
        deleteMassFilterExp = (c) -> !(gt <= c.getIonMass() && c.getIonMass() <= lt);
    }

    Predicate<CompoundContainerId> deleteMassFilter = (c) -> true;
    Predicate<Ms2Experiment> deleteMassFilterExp = (c) -> true;

    @CommandLine.Option(names = {"--delete-by-name", "--dn"},
            description = "Delete all compounds where the 'identifier' (dir name, ID) matches the given regex (JAVA).")
    private void makeDeleteIdxFilter(String regex) {
        final Pattern m = Pattern.compile(regex);
        deleteNameFilter = (c) -> !m.matcher(c.getDirectoryName()).find();
    }

    Predicate<CompoundContainerId> deleteNameFilter = (c) -> true;


    @CommandLine.Option(names = {"--keep-by-idx", "--ki", "-k"}, split = ",",
            description = {"Keep all compounds that match the given indices from the given project-space"})
    private void makeKeepIdxFilter(Set<Integer> idxs) {
        keepIdxFilter = (c)-> idxs.contains(c.getCompoundIndex());
    }

    Predicate<CompoundContainerId> keepIdxFilter = (c) -> true;

    @CommandLine.Option(names = {"--keep-by-mass", "--km"},
            description = {"Keep all compounds that are within the given mass window."
                    ,"Example: Use 'mass1:mass2' to match compounds with mass1 <= mass <= mass2. Leave a value empty to set no bound."})
    public void makeKeepMassFilter(String mass) {
        String[] masses = mass.split(":");
        final double gt = masses[0].strip().isBlank() ? 0d : Double.parseDouble(masses[0]);
        final double lt = masses.length < 2 || masses[1].strip().isBlank() ? Double.POSITIVE_INFINITY : Double.parseDouble(masses[1]);
        keepMassFilter = (c) -> (c.getIonMass().map(m -> gt <= m && m <= lt).orElse(true));
        keepMassFilterExp = (c) -> (gt <= c.getIonMass() && c.getIonMass() <= lt);
    }

    Predicate<CompoundContainerId> keepMassFilter = (c) -> true;
    Predicate<Ms2Experiment> keepMassFilterExp = (c) -> true;


    @CommandLine.Option(names = {"--keep-by-name", "--kn"},
            description = "Keep all compounds where the 'identifier' (Dir name, ID) matches the given regex (JAVA).")
    private void makeKeepNameFilter(String regex) {
        final Pattern m = Pattern.compile(regex);
        keepNameFilter = (c) -> m.matcher(c.getDirectoryName()).find();
    }

    Predicate<CompoundContainerId> keepNameFilter = (c) -> true;


    @CommandLine.Option(names = {"--keep-by-confidence", "--kc"},
            description = {"Keep all compounds that have a valid confidence score greater or equal than the given minimum confidence."})
    private void makeKeepConfidenceFilter(double minConfidence) {
        keepConfidenceFilter = (inst) -> inst.loadTopFormulaResult(List.of(ConfidenceScore.class))
                .flatMap(f -> f.getAnnotation(FormulaScoring.class)).flatMap(it -> it.getAnnotation(ConfidenceScore.class))
                .map(s -> !s.isNa() && s.score() >= minConfidence).orElse(false);
    }

    Predicate<Instance> keepConfidenceFilter = null;


    @CommandLine.Option(names = {"--keep-by-tree-size", "--kts"},
            description = {"Keep all compounds that have at least one fragmentation tree with number of fragments (including precursor) greater or equal than the given minimum."})
    private void makeKeepTreeSizeFilter(double minTreeSize) {
        keepTreeSizeFilter = (inst) -> inst.loadFormulaResults(FTree.class).stream().map(SScored::getCandidate).map(res ->  res.getAnnotation(FTree.class))
                .filter(Optional::isPresent).map(Optional::get).mapToDouble(FTree::numberOfVertices).anyMatch(n->n>=minTreeSize);
    }

    Predicate<Instance> keepTreeSizeFilter = null;

    @CommandLine.Option(names = {"--keep-by-explained-intensity", "--kei"},
            description = {"Keep all compounds that have at least one fragmentation tree that explains at least a minimum total intensity of the spectrum. Value between 0 to 1."})
    private void makeKeepExplainedIntensityFilter(double minIntensityRatio) {
        keepExplainedIntensityFilter = (inst) -> inst.loadFormulaResults(FTree.class).stream().map(SScored::getCandidate).map(res ->  res.getAnnotation(FTree.class))
                .filter(Optional::isPresent).map(Optional::get).mapToDouble(FTreeMetricsHelper::getExplainedIntensityRatio).anyMatch(r->r>=minIntensityRatio);
    }

    Predicate<Instance> keepExplainedIntensityFilter = null;

    @Nullable
    public Predicate<Instance> getCombinedInstanceilter() {
        Predicate<Instance> it = (inst) -> true; //always true as a start
        // combine
        if (keepConfidenceFilter != null)
            it = it.and(keepConfidenceFilter);
        if (keepTreeSizeFilter != null)
            it = it.and(keepTreeSizeFilter);
        if (keepExplainedIntensityFilter != null)
            it = it.and(keepExplainedIntensityFilter);
        return it;
    }

    public Predicate<CompoundContainerId> getCombinedCIDFilter() {
        return (cid) -> deleteIdxFilter.test(cid)
                && deleteMassFilter.test(cid)
                && deleteNameFilter.test(cid)
                && keepIdxFilter.test(cid)
                && keepMassFilter.test(cid)
                && keepNameFilter.test(cid)
                ;
    }

    public Predicate<Ms2Experiment> getCombinedMS2ExpFilter() {
        return (cc) -> deleteMassFilterExp.test(cc)
                && keepMassFilterExp.test(cc)
                ;
    }


    @CommandLine.ArgGroup(exclusive = false, heading = "@|bold Split the project into chunks: %n|@")
    protected SplitProject splitOptions = new SplitProject();

    static class SplitProject {
        enum Order {SHUFFLE, MASS, NAME}
        enum SplitType {NO, NUMBER, SIZE}

        @CommandLine.Option(names = {"--split", "-s"}, defaultValue = "NO", description = "Split the output into batches. Either in a specific number of batches or in batches of specific size!")
        SplitType type;

        @CommandLine.Option(names = {"--count", "-c"}, defaultValue = "1", description = "Sets batch number or batch size depending on <--split>")
        int count = 1;

        @CommandLine.Option(names = {"--split-order", "-o"}, defaultValue = "SHUFFLE", description = "Specify the order of Compounds before putting them into batches.")
        Order order = Order.SHUFFLE;


    }

    @CommandLine.Option(names = {"--move", "-m"}, description = "DANGERZONE: Move instead of copy data (where possible) when merging or splitting projects to save time. Be aware of the risk that you may end up with corrupted input or output data when the program crashes.")
    public boolean move = false;

    public ProjectSpaceWorkflow makeWorkflow(RootOptions<?,?,?> rootOptions, ParameterConfig config) {
        return new ProjectSpaceWorkflow(rootOptions, this, config);
    }
}

