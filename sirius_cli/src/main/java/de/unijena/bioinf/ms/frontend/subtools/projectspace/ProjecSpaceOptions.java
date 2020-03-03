package de.unijena.bioinf.ms.frontend.subtools.projectspace;

import de.unijena.bioinf.ms.frontend.subtools.*;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import picocli.CommandLine;

import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@CommandLine.Command(name = "project-space", aliases = {"PS"}, description = "<STANDALONE> Modify a given project Space: Read project(s) with --input, apply modification and write the result via --output. If either onl --input or --output is give the modifications will be made in-place.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class ProjecSpaceOptions implements StandaloneTool<ProjectSpaceWorkflow> {


    @CommandLine.Option(names = {"--delete-by-idx", "--di", "-d"}, description = "Delete all compounds that match the given indices from the given project-space")
    private void makeDeleteIdxFilter(Set<Integer> idxs) {
        deleteIdxFilter = (c) -> !idxs.contains(c.getCompoundIndex());
    }

    Predicate<CompoundContainerId> deleteIdxFilter = (c) -> true;


    @CommandLine.Option(names = {"--delete-by-mass", "--dm"}, description = "Delete all compound that are not within the given mass window. User mass1:mass2 to match compounds with  mass1 <= mass <= mass2. Leave a value empty to set no bound.")
    private void makeDeleteMassFilter(String mass) {
        String[] masses = mass.split(":");
        final double gt = !masses[0].strip().isBlank() ? 0d : Double.parseDouble(masses[0]);
        final double lt = masses.length < 2 || !masses[1].strip().isBlank() ? Double.POSITIVE_INFINITY : Double.parseDouble(masses[0]);
        deleteIdxFilter = (c) -> !(c.getIonMass().map(m -> gt <= m && m <= lt).orElse(false));
    }

    Predicate<CompoundContainerId> deleteMassFilter = (c) -> true;

    @CommandLine.Option(names = {"--delete-by-name", "--dn"}, description = "Delete all compounds where the identifier (Dir name, ID) matches the given regex (JAVA).")
    private void makeDeleteIdxFilter(String regex) {
        final Pattern m = Pattern.compile(regex);
        deleteIdxFilter = (c) -> !m.matcher(c.getDirectoryName()).matches();
    }

    Predicate<CompoundContainerId> deleteNameFilter = (c) -> true;


    @CommandLine.Option(names = {"--keep-by-idx", "--ki", "-k"}, description = "Keep all compounds that match the given indices from the given project-space")
    private void makeKeepIdxFilter(Set<Integer> idxs) {
        keepIdxFilter = (c)-> idxs.contains(c.getCompoundIndex());
    }

    Predicate<CompoundContainerId> keepIdxFilter = (c) -> true;


    @CommandLine.Option(names = {"--keep-by-mass", "--km"}, description = "Keep all compound that are not within the given mass window. User mass1:mass2 to match compounds with  mass1 <= mass <= mass2. Leave a value empty to set no bound.")
    private void makeKeepMassFilter(String mass) {
        String[] masses = mass.split(":");
        final double gt = !masses[0].strip().isBlank() ? 0d : Double.parseDouble(masses[0]);
        final double lt = masses.length < 2 || !masses[1].strip().isBlank() ? Double.POSITIVE_INFINITY : Double.parseDouble(masses[0]);
        keepMassFilter = (c) -> (c.getIonMass().map(m -> gt <= m && m <= lt).orElse(true));
    }

    Predicate<CompoundContainerId> keepMassFilter = (c) -> true;


    @CommandLine.Option(names = {"--keep-by-name", "--kn"}, description = "Keep all compounds where the identifier (Dir name, ID) matches the given regex (JAVA).")
    private void makeKeepNameFilter(String regex) {
        final Pattern m = Pattern.compile(regex);
        keepNameFilter = (c) -> m.matcher(c.getDirectoryName()).matches();

    }

    Predicate<CompoundContainerId> keepNameFilter = (c) -> true;


    public Predicate<CompoundContainerId> getCombinedFilter() {
        return (c) -> deleteIdxFilter.test(c)
                && deleteMassFilter.test(c)
                && deleteNameFilter.test(c)
                && keepIdxFilter.test(c)
                && keepMassFilter.test(c)
                && keepNameFilter.test(c)
                ;
    }

    @CommandLine.ArgGroup(exclusive = false, heading = "@|bold Split the project into chunks: %n|@")
    SplitProject splitOptions = new SplitProject();

    static class SplitProject {
        enum Strategy {SHUFFLE, MASS, NAME}

        @CommandLine.Option(names = {"--split, -s"}, description = "Split the output into <--split> batches.", defaultValue = "1")
        int batches = 1;
        @CommandLine.Option(names = {"--split-order, -o"}, description = "Order Compounds before putting them into batches.", defaultValue = "SHUFFLE", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
        Strategy strategy = Strategy.SHUFFLE;


    }

    public ProjectSpaceWorkflow makeWorkflow(RootOptions<?, ?> rootOptions, ParameterConfig config) {
        return new ProjectSpaceWorkflow(rootOptions, this);
    }
}

