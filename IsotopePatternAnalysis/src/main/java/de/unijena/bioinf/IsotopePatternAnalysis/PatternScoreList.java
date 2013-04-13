package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

import java.io.PrintStream;
import java.util.*;

public class PatternScoreList<P extends Peak, T extends Spectrum<P>> implements IsotopePatternScorer<P, T> {


    private final List<IsotopePatternScorer<P, T>> scorers;
    private Normalization requirement;

    public PatternScoreList(IsotopePatternScorer<P, T>... scorers) {
        this(Arrays.asList(scorers));
    }

    public PatternScoreList(Collection<IsotopePatternScorer<P, T>> scorers) {
        this.scorers = new ArrayList<IsotopePatternScorer<P, T>>(scorers);
    }

    public List<IsotopePatternScorer<P, T>> getScorers() {
        return Collections.unmodifiableList(scorers);
    }

    public void addScorer(IsotopePatternScorer<P, T> scorer) {
        this.scorers.add(scorer);
    }

    public double report(T measuredSpectrum, T theoreticalSpectrum, Normalization norm, PrintStream out) {
        double score = 0d;
        int k=0;
        for (IsotopePatternScorer<P, T> scorer : scorers) {
            final double sc = scorer.score(measuredSpectrum, theoreticalSpectrum, norm);
            score += score;
            out.print(scorer.getClass().getSimpleName() + ": " + sc);
            if (k++ < scorers.size()) out.print(",\t");
        }
        out.print("\t=>\t" + score);
        return score;
    }

    @Override
    public double score(T measuredSpectrum, T theoreticalSpectrum, Normalization norm) {
        double score = 0d;
        for (IsotopePatternScorer<P, T> scorer : scorers) {
            score += scorer.score(measuredSpectrum, theoreticalSpectrum, norm);
        }
        return score;
    }
}
