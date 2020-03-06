package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.SearchStructureByFormula;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.Partition;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Fingerblast {

    private SearchStructureByFormula searchEngine;
    private FingerblastScoringMethod scoringMethod;

    public Fingerblast(FingerblastScoringMethod method, SearchStructureByFormula searchEngine) {
        this.searchEngine = searchEngine;
        this.scoringMethod = method;
    }

    public SearchStructureByFormula getSearchEngine() {
        return searchEngine;
    }

    public void setSearchEngine(SearchStructureByFormula searchEngine) {
        this.searchEngine = searchEngine;
    }

    public FingerblastScoringMethod getScoringMethod() {
        return scoringMethod;
    }

    public void setScoringMethod(FingerblastScoringMethod scoringMethod) {
        this.scoringMethod = scoringMethod;
    }

    public List<Scored<FingerprintCandidate>> search(@NotNull MolecularFormula formula, @NotNull ProbabilityFingerprint fingerprint) throws ChemicalDatabaseException {
        return search(searchEngine, scoringMethod, formula, fingerprint);
    }

    public List<Scored<FingerprintCandidate>> score(@NotNull List<FingerprintCandidate> candidates, @NotNull ProbabilityFingerprint fingerprint) throws ChemicalDatabaseException {
        return score(scoringMethod, candidates, fingerprint);
    }

    public static List<Scored<FingerprintCandidate>> search(@NotNull final SearchStructureByFormula searchEngine, @NotNull final FingerblastScoringMethod scoringMethod, @NotNull final MolecularFormula formula, @NotNull final ProbabilityFingerprint fingerprint) throws ChemicalDatabaseException {
        final List<FingerprintCandidate> candidates = searchEngine.lookupStructuresAndFingerprintsByFormula(formula);
        return score(scoringMethod, candidates, fingerprint);
    }

    public static List<Scored<FingerprintCandidate>> score(@NotNull final FingerblastScoringMethod scoringMethod, @NotNull final List<FingerprintCandidate> candidates, @NotNull final ProbabilityFingerprint fingerprint) {
        final ArrayList<Scored<FingerprintCandidate>> results = new ArrayList<>();
        MaskedFingerprintVersion mask = null;
        if (fingerprint.getFingerprintVersion() instanceof MaskedFingerprintVersion) mask = (MaskedFingerprintVersion)fingerprint.getFingerprintVersion();
        final FingerblastScoring scorer = scoringMethod.getScoring();
        scorer.prepare(fingerprint);
        for (FingerprintCandidate fp : candidates) {
            final Fingerprint fpm = (mask==null || fp.getFingerprint().getFingerprintVersion().equals(mask)) ? fp.getFingerprint() : mask.mask(fp.getFingerprint());
            results.add(new Scored<>(new FingerprintCandidate(fp, fpm), scorer.score(fingerprint, fpm)));
        }
        results.sort(Comparator.reverseOrder());
        return results;
    }


    public static List<JJob<List<Scored<FingerprintCandidate>>>> makeScoringJobs(@NotNull final FingerblastScoringMethod scoringMethod, @NotNull final List<FingerprintCandidate> candidates, @NotNull final ProbabilityFingerprint fingerprint) {
        final List<List<FingerprintCandidate>> inputs = Partition.ofNumber(candidates, PropertyManager.getNumberOfThreads());

        return inputs.stream().map(can ->
                new BasicJJob<List<Scored<FingerprintCandidate>>>(JJob.JobType.CPU) {
                    @Override
                    protected List<Scored<FingerprintCandidate>> compute() throws Exception {
                        return score(scoringMethod, can, fingerprint);
                    }
                }
        ).collect(Collectors.toList());
    }

    /*public static class FingeriblastScoringJJob extends BasicJJob<Scored<FingerprintCandidate>> {
        private final FingerblastScoringMethod scoringMethod;
        private final List<FingerprintCandidate> candidates;
        private final ProbabilityFingerprint fingerprint;

        public FingeriblastScoringJJob(@NotNull final FingerblastScoringMethod scoringMethod, @NotNull final FingerprintCandidate candidate, @NotNull final ProbabilityFingerprint fingerprint) {
            this.scoringMethod = scoringMethod;
            this.candidates = Collections.singletonList(candidate);
            this.fingerprint = fingerprint;
        }

        @Override
        protected Scored<FingerprintCandidate> compute() throws Exception {
            return score(scoringMethod, candidates, fingerprint).get(0);
        }
    }*/
}
