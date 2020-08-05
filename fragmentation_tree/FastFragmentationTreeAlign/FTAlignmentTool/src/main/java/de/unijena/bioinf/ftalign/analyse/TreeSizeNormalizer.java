
package de.unijena.bioinf.ftalign.analyse;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.treealign.scoring.Scoring;

/**
 * @author Kai Dührkop
 */
public class TreeSizeNormalizer implements Normalizer {

    private final double c;


    public TreeSizeNormalizer(double c) {
        this.c = c;
    }

    @Override
    public double normalize(FTree left, FTree right, Scoring<Fragment> scoring, float score) {
        final double f = Math.min(score(left, scoring), score(right, scoring));
        assert f > 0d;
        assert Math.pow(f, c) > 0d;
        return ((double) score / Math.pow(f, c));
    }

    public double score(FTree tree, final Scoring<Fragment> scoring) {
        return scoring.selfAlignScore(tree.getRoot());
    }
}
