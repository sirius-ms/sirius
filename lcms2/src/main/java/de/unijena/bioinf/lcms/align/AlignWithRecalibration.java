package de.unijena.bioinf.lcms.align;

public interface AlignWithRecalibration {

    public static AlignWithRecalibration noRecalibration() {
        return NO;
    }

    public static class NoRecalibration implements AlignWithRecalibration {

        @Override
        public RecalibrationFunction getRecalibrationFor(MoI moi) {
            return RecalibrationFunction.identity();
        }
    }
    static NoRecalibration NO = new NoRecalibration();

    public default double getRecalibratedRt(MoI moi) {
        return getRecalibrationFor(moi).value(moi.getRetentionTime());
    }

    public RecalibrationFunction getRecalibrationFor(MoI moi);

}
