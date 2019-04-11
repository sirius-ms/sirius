package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.SearchStructureByFormula;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            results.add(new Scored<FingerprintCandidate>(new FingerprintCandidate(fp, fpm), scorer.score(fingerprint, fpm)));
        }
        Collections.sort(results, Scored.<FingerprintCandidate>desc());
        return results;
    }


}
