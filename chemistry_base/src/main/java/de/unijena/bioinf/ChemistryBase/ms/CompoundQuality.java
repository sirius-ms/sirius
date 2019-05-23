package de.unijena.bioinf.ChemistryBase.ms;

import com.google.common.base.Joiner;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Keywords that can be assigned to a input spectrum to judge its quality. Available keywords are: Good, LowIntensity, NoMS1Peak, FewPeaks, Chimeric, NotMonoisotopicPeak, PoorlyExplained
 */
public class CompoundQuality implements Ms2ExperimentAnnotation  {

    private final EnumSet<CompoundQualityFlag> flags;

    public static CompoundQuality fromString(String s) {
        return fromKeywords(Arrays.asList(s.split("\\s*,\\s*")));
    }

    @DefaultInstanceProvider
    public static CompoundQuality fromKeywords(@DefaultProperty(propertyKey = "quality") List<String> properties) {
        final EnumSet<CompoundQualityFlag> props = EnumSet.noneOf(CompoundQualityFlag.class);
        for (String property : properties) {
            try {
                props.add(CompoundQualityFlag.valueOf(property));
            } catch (IllegalArgumentException e) {
                LoggerFactory.getLogger(CompoundQuality.class).error("Unknown spectrum property with name '" + property + "'");
            }
        }
        return new CompoundQuality(props);
    }

    public CompoundQuality() {
        this.flags = EnumSet.of(CompoundQualityFlag.Good);
    }

    public CompoundQuality(CompoundQualityFlag first, CompoundQualityFlag... other) {
        this.flags = EnumSet.of(first, other);
    }

    private CompoundQuality(EnumSet<CompoundQualityFlag> flags) {
        this.flags = flags;
    }

    public boolean is(CompoundQualityFlag flag) {
        return flags.contains(flag);
    }

    public boolean isNot(CompoundQualityFlag flag) {
        return !flags.contains(flag);
    }

    public static enum CompoundQualityFlag {
        Good, LowIntensity, NoMS1Peak, FewPeaks, Chimeric, NotMonoisotopicPeak, PoorlyExplained;
    }

    public String toString() {
        return Joiner.on(',').join(flags);
    }

}
