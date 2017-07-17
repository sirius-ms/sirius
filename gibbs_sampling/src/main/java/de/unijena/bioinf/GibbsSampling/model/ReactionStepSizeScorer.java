package de.unijena.bioinf.GibbsSampling.model;

public abstract class ReactionStepSizeScorer {
    public ReactionStepSizeScorer() {
    }

    public abstract double multiplier(int var1);

    public static class ConstantReactionStepSizeScorer extends ReactionStepSizeScorer {
        final double value;

        public ConstantReactionStepSizeScorer() {
            this(1.0D);
        }

        public ConstantReactionStepSizeScorer(double value) {
            this.value = value;
        }

        public double multiplier(int stepSize) {
            return 1.0D;
        }
    }

    public static class ExponentialReactionStepSizeScorer extends ReactionStepSizeScorer {
        final double startValue;

        public ExponentialReactionStepSizeScorer(double startValue) {
            this.startValue = startValue;
        }

        public double multiplier(int stepSize) {
            return this.startValue / Math.pow(2.0D, (double)(stepSize - 1));
        }
    }
}
