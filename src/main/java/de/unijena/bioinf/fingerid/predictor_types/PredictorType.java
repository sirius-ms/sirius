package de.unijena.bioinf.fingerid.predictor_types;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        return bitsToTypes(bits).stream()
                .map(PredictorType::name)
                .collect(Collectors.joining(","));
    }

    public static EnumSet<PredictorType> bitsToTypes(long bits) {
        EnumSet<PredictorType> predictors = EnumSet.noneOf(PredictorType.class);
        PredictorType[] val = PredictorType.values();

        for (int i = (int) Long.highestOneBit(bits); i >= 0; i--) {
            if (((bits >> i) & 1) == 1) predictors.add(val[i]);
        }
        return predictors;
    }

    //todo can we do this generic for all type enums
    public static EnumSet<PredictorType> parse(@NotNull String workerTypes, String regexDelimiter) {
        return Arrays.stream(workerTypes.split(regexDelimiter))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter((s) -> !s.isEmpty())
                .map(PredictorType::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(PredictorType.class)));
    }

    public static EnumSet<PredictorType> parse(@NotNull String workerTypes) {
        return parse(workerTypes, ",");
    }

    public static void main(String[] args) {
        List<PredictorType> types = Arrays.asList();
        long bitSet = PredictorType.getBits(types);
        System.out.println(Long.toBinaryString(bitSet)); // show full integer in binary
        System.out.println(bitsToNames(bitSet));
    }
}
