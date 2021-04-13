package de.unijena.bioinf.networks.de.unijena.bioinf.pvalues;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.math.Probability;

import java.util.Arrays;

/**
 * just counts the number of common bits in two fingerprints
 * computes p-values
 */
public class SimpleIndepDP {

    private final Probability[][] probabilities;
    private final Probability[] p;
    private static final Probability One = Probability.ONE, Zero = Probability.ZERO;

    public SimpleIndepDP(double[] p) {
        this.p = Arrays.stream(p).mapToObj(Probability::new).toArray(Probability[]::new);
        this.probabilities = new Probability[p.length][p.length];
    }

    public PValueCalculation compute(ProbabilityFingerprint inputFingerprint) {
        final Probability[] fingerprint = Arrays.stream(inputFingerprint.toProbabilityArray()).mapToObj(Probability::new).toArray(Probability[]::new);

        // first entry
        probabilities[0][0] = fingerprint[0].multiply(p[0]).add(One.subtract(fingerprint[0]).multiply(One.subtract(p[0])));
        probabilities[0][1] = One.subtract(probabilities[0][0]);
        // remaining entries
        dp(fingerprint);
        // return last row of the DP
        return new PValueCalculation(probabilities[probabilities.length-1]);
    }

    private void dp(final Probability[] fingerprint) {
        for (int fid = 1; fid < p.length; ++fid) {
            final Probability matchProb = fingerprint[fid].multiply(p[fid]).add(One.subtract(fingerprint[fid]).multiply(One.subtract(p[fid])));
            final Probability missMatchProb = One.subtract(matchProb);
            probabilities[fid][0] = probabilities[fid - 1][0].multiply(matchProb);
            for (int penalty = 1; penalty < probabilities[0].length; ++penalty) {
                probabilities[fid][penalty] = probabilities[fid - 1][penalty - 1].multiply(missMatchProb).add(probabilities[fid - 1][penalty].multiply(matchProb));
            }
        }
    }

    public static class PValueCalculation {
        private final Probability[] probabilities;

        PValueCalculation(Probability[] probabilities) {
            this.probabilities = probabilities;
        }

        public Probability pvalue(int missmatches) {
            Probability prob = Zero;
            for (int i=0; i <= missmatches; ++i) {
                prob = prob.add(probabilities[i]);
            }
            return prob;
        }

        public int logScore(int missmatches) {
            return -pvalue(missmatches).getExp();
        }
    }

}
