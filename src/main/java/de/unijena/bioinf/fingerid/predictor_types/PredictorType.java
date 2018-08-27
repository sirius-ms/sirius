package de.unijena.bioinf.fingerid.predictor_types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public enum PredictorType {
    CSI_FINGERID_POSITIVE,//CSI for negative ionization
    IOKR_POSITIVE,
    CSI_FINGERID_NEGATIVE, //CSI for negative ionization
    IOKR_NEGATIVE;


    public String toBitsAsString() {
        return String.valueOf(toBits());
    }

    public long toBits() {
        return (1L << ordinal());
    }

    public boolean isBitSet(final long bits) {
        return ((bits & (1L << ordinal())) != 0L);
    }

    public static String getBitsAsString(Iterable<PredictorType> types) {
        return String.valueOf(getBits(types));
    }

    public static long getBits(Iterable<PredictorType> types) {
        long bits = 0L;
        for (PredictorType type : types) {
            bits = setBit(bits, type.ordinal());
        }
        return bits;
    }

    public static String getBitsAsString(PredictorType... types) {
        return String.valueOf(getBits(types));
    }

    public static long getBits(PredictorType... types) {
        long bits = 0L;
        for (PredictorType type : types) {
            bits = setBit(bits, type.ordinal());
        }
        return bits;
    }

    private static long setBit(long source, long posToAdd) {
        return source | (1L << posToAdd);
    }

    public static boolean contains(long bits, PredictorType predictor) {
        return predictor.isBitSet(bits);
    }

    public static long allBitsSet() {
        return ~0L;
    }

    public static long noBitsSet() {
        return 0L;
    }

    public static EnumSet<PredictorType> defaultPredictorSet() {
        return EnumSet.of(CSI_FINGERID_POSITIVE);
    }

    public static EnumSet<PredictorType> makeValid(final EnumSet<PredictorType> predictors) {
        if (predictors.contains(CSI_FINGERID_POSITIVE) && predictors.contains(CSI_FINGERID_NEGATIVE)) {
            predictors.remove(CSI_FINGERID_NEGATIVE);
        }
        return predictors;
    }

    public static String bitsToNames(long bits) {
        List<PredictorType> predictors = new ArrayList<>();
        PredictorType[] val = PredictorType.values();

        for (int i = (int) Long.highestOneBit(bits); i >= 0; i--) {
            if (((bits >> i) & 1) == 1) predictors.add(val[i]);
        }
        return predictors.toString().substring(1, predictors.size() - 1);
    }

    public static void main(String[] args) {
        List<PredictorType> types = Arrays.asList();
        long bitSet = PredictorType.getBits(types);
        System.out.println(Long.toBinaryString(bitSet)); // show full integer in binary
        System.out.println(bitsToNames(bitSet));
    }
}
