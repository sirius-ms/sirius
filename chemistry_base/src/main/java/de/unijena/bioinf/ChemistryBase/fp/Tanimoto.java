package de.unijena.bioinf.ChemistryBase.fp;

public class Tanimoto {

    public interface ProbabilisticTanimoto {
        double expectationValue();
        double variance();
        double standardDeviation();
    }

    public static ProbabilisticTanimoto probabilisticTanimotoFixedLength(ProbabilityFingerprint left, Fingerprint right) {
        return new ExactDP(left, right, true);
    }

    public static ProbabilisticTanimoto probabilisticTanimoto(AbstractFingerprint left, AbstractFingerprint right) {
        if (left instanceof ProbabilityFingerprint) {
            if (right instanceof ProbabilityFingerprint) {
                return new ExactDP2((ProbabilityFingerprint) left, (ProbabilityFingerprint)right);
            } else {
                return new ExactDP((ProbabilityFingerprint) left, (Fingerprint)right);
            }
        } else {
            if (right instanceof ProbabilityFingerprint) {
                return new ExactDP((ProbabilityFingerprint) right, (Fingerprint)left);
            } else {
                final double tanimoto = deterministicJaccard((Fingerprint) left, (Fingerprint) right);
                return new ProbabilisticTanimoto() {
                    @Override
                    public double expectationValue() {
                        return tanimoto;
                    }

                    @Override
                    public double variance() {
                        return 0;
                    }

                    @Override
                    public double standardDeviation() {
                        return 0;
                    }
                };
            }
        }
    }

    /**
     * returns the Tanimoto/Jaccard Index of two sets of integers
     * which not necessarily have to be fingerprints
     * @param as
     * @param bs
     * @return
     */
    public static double tanimoto(int[] as, int[] bs) {
        int a=0, b=0, intersection=0;
        while(a < as.length && b < bs.length) {
            if (as[a]==bs[b]) {
                ++intersection;
                ++a; ++b;
            } else if (as[a] > bs[b]) {
                ++b;
            } else {
                ++a;
            }
        }

        // |A n B| = (|A| + |B|) - 2|A u B|
        final int union = as.length + bs.length - intersection;
        if (union==0) return 0;
        return ((double)intersection)/(union);
    }
    public static double tanimoto(short[] as, short[] bs) {
        int a=0, b=0, intersection=0;
        while(a < as.length && b < bs.length) {
            if (as[a]==bs[b]) {
                ++intersection;
                ++a; ++b;
            } else if (as[a] > bs[b]) {
                ++b;
            } else {
                ++a;
            }
        }

        // |A n B| = (|A| + |B|) - 2|A u B|
        final int union = as.length + bs.length - intersection;
        if (union==0) return 0;
        return ((double)intersection)/(union);
    }

    public static double tanimoto(AbstractFingerprint left, AbstractFingerprint right) {
        if (left instanceof ProbabilityFingerprint) {
            if (right instanceof ProbabilityFingerprint) {
                return probabilisticJaccard2((ProbabilityFingerprint) left, (ProbabilityFingerprint)right);
            } else {
                return probabilisticJaccard1((Fingerprint)right, (ProbabilityFingerprint) left);
            }
        } else {
            if (right instanceof ProbabilityFingerprint) {
                return probabilisticJaccard1((Fingerprint)left, (ProbabilityFingerprint) right);
            } else {
                return deterministicJaccard((Fingerprint) left, (Fingerprint) right);
            }
        }
    }

    public static double nonProbabilisticTanimoto(AbstractFingerprint left, AbstractFingerprint right)  {
        return deterministicJaccard(left, right);
    }

    private static double deterministicJaccard(AbstractFingerprint left, AbstractFingerprint right) {
        left.enforceCompatibility(right);
        short union=0, intersection=0;
        for (FPIter2 pairwise : left.foreachPair(right)) {
            final boolean a = pairwise.isLeftSet();
            final boolean b = pairwise.isRightSet();

            if (a || b) ++union;
            if (a && b) ++intersection;
        }
        if (union==0) return 0d;
        return ((double)intersection)/union;
    }

    private static double probabilisticJaccard1(Fingerprint left, ProbabilityFingerprint right) {
        left.enforceCompatibility(right);
        double Q  = 0d, R = 0d;
        FPIter probFp = right.iterator();
        for (FPIter eachFp : left) {
            probFp = probFp.next();
            if (eachFp.isSet()) {
                Q += probFp.getProbability();
                R += 1d;
            } else R += probFp.getProbability();
        }
        if (Q==0) return 0d;
        return Q / R;
    }

    private static double probabilisticJaccard2(ProbabilityFingerprint left, ProbabilityFingerprint right) {
        return new ExactDP2(left,right).expectationValue();
    }

    protected static class ExactDP implements ProbabilisticTanimoto {

        protected double exp, var;

        public ExactDP(ProbabilityFingerprint left, Fingerprint right) {
            this(left, right, false);
        }

        public ExactDP(ProbabilityFingerprint left, Fingerprint right, boolean fixedLength) {
            final int N = right.getFingerprintVersion().size();
            final int NPOS = right.cardinality();
            final int NNEG = right.getFingerprintVersion().size() - NPOS;

            final double[] m = new double[NNEG+1];
            final double[] p = new double[NPOS+1];
            m[0] = 1d; p[0] = 1d;

            computeDP(m, p, left.iterator(), right.iterator());

            // calculate expectation value
            var = 0d;
            exp = 0;
            if (fixedLength) {
                double norm = 0d;
                for (int Q=0; Q <= NPOS; ++Q) {
                    final int R = 2*NPOS - Q;
                    if (R < NPOS) break;
                    norm += m[R-NPOS]*p[Q];
                    exp += (m[R-NPOS] * p[Q] * ((double)Q)/R);
                }
                if (norm > 0) exp /= norm;
                for (int Q=0; Q <= NPOS; ++Q) {
                    final int R = 2*NPOS - Q;
                    if(R < NPOS) break;
                    var += (m[R-NPOS] * p[Q] * ((double)Q*Q)/((double)R*R));
                }
                if (norm > 0)  var /= norm;
            } else {
                for (int Q=0; Q <= NPOS; ++Q) {
                    for (int R=NPOS; R <= N; ++R) {
                        exp += (m[R-NPOS] * p[Q] * ((double)Q)/R);
                    }
                }
                for (int Q=0; Q <= NPOS; ++Q) {
                    for (int R=NPOS; R <= N; ++R) {
                        var += (m[R-NPOS] * p[Q] * ((double)Q*Q)/((double)R*R));
                    }
                }
            }

            var -= exp*exp;
        }

        private void computeDP(double[] m, double[] p, FPIter l, FPIter r) {
            int psize=1, msize=1;
            while (l.hasNext()) {
                l = l.next();
                r = r.next();
                final double isset = l.getProbability(), isnotset = 1d-l.getProbability();
                if (r.isSet()) {
                    // change all other entries
                    for (int k=psize; k > 0; --k) {
                        p[k] = p[k-1] * isset + p[k] * isnotset;
                    }
                    // change 0 entry
                    p[0] *= isnotset;
                    ++psize;
                } else {
                    // change all other entries
                    for (int k=msize; k > 0; --k) {
                        m[k] = m[k-1]*isset + m[k]*isnotset;
                    }
                    // change 0 entry
                    m[0] *= isnotset;
                    ++msize;
                }
            }
        }

        @Override
        public double expectationValue() {
            return exp;
        }

        @Override
        public double variance() {
            return var;
        }

        @Override
        public double standardDeviation() {
            return Math.sqrt(var);
        }

        @Override
        public String toString() {
            return "tanimoto = " + exp + " (σ² = " + var + ")";
        }
    }

    protected static class ExactDP2 implements ProbabilisticTanimoto {

        protected double exp, var;

        public ExactDP2(ProbabilityFingerprint left, ProbabilityFingerprint right) {
            final int N = left.getFingerprintVersion().size();
            double[][] D = new double[N+1][N+1], F = new double[N+1][N+1];
            F[0][0] = 1d;
            int M = 1;

            for (final FPIter2 iter : left.foreachPair(right)) {
                final double A = iter.getLeftProbability(), B = iter.getRightProbability();
                for (int Q = 0; Q <= M; ++Q) {
                    for (int R = 0; R <= M; ++R) {

                        D[Q][R] = (Q < 1 || R < 1) ? 0 : F[Q-1][R-1] * A * B;
                        D[Q][R] +=(R < 1) ? 0 : F[ Q ][R-1] * (((1d-A) * B) + (A * (1d-B)));
                        D[Q][R] +=  F[ Q ][ R ] * (1d-A) * (1d - B);

                    }
                }
                double[][] swap = F;
                F = D;
                D = swap;
                ++M;
            }

            D=F;
            // calculate expectation value and variance
            this.exp = 0d;
            for (int Q=1; Q <= N; ++Q) {
                for (int R=1; R <= N; ++R) {
                    exp += D[Q][R] * (((double)Q)/R);
                }
            }

            this.var = 0d;
            for (int Q=1; Q <= N; ++Q) {
                for (int R=1; R <= N; ++R) {
                    var += D[Q][R] * (((double)Q*Q)/R*R);
                }
            }
            var -= exp*exp;


        }

        @Override
        public double expectationValue() {
            return exp;
        }

        @Override
        public double variance() {
            return var;
        }

        @Override
        public double standardDeviation() {
            return Math.sqrt(var);
        }

        @Override
        public String toString() {
            return "tanimoto = " + exp + " (σ² = " + var + ")";
        }
    }


}
